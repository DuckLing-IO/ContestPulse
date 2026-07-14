package io.duckling.contestpulse.data.remote.custom

import io.duckling.contestpulse.data.remote.RemoteApiException
import io.duckling.contestpulse.data.remote.RemoteHttpException
import java.io.IOException
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class FetchedCustomSourceContent(
    val body: String,
    val finalUrl: String,
    val contentType: String?,
)

class SafeCustomSourceHttpClient @Inject constructor(
    okHttpClient: OkHttpClient,
) {
    private val client = okHttpClient.newBuilder()
        .followRedirects(false)
        .followSslRedirects(false)
        .callTimeout(CUSTOM_SOURCE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    suspend fun get(url: String): FetchedCustomSourceContent = withContext(Dispatchers.IO) {
        var currentUrl = url.toSafeHttpsUrl()
        repeat(MAX_REDIRECTS + 1) { redirectCount ->
            val addresses = resolvePublicAddresses(currentUrl.host)
            val requestClient = client.newBuilder()
                .dns(PinnedDns(currentUrl.host, addresses))
                .build()
            val request = Request.Builder()
                .url(currentUrl)
                .get()
                .header(
                    "Accept",
                    "text/html, application/json, text/calendar, application/ld+json;q=0.9, */*;q=0.2",
                )
                .build()

            requestClient.newCall(request).execute().use { response ->
                if (response.isRedirectResponse()) {
                    if (redirectCount >= MAX_REDIRECTS) {
                        throw RemoteApiException("Custom source redirected too many times")
                    }
                    val location = response.header("Location")
                        ?: throw RemoteApiException("Custom source redirect did not include a location")
                    currentUrl = currentUrl.resolve(location)?.toString()?.toSafeHttpsUrl()
                        ?: throw RemoteApiException("Custom source redirect URL is invalid")
                    return@repeat
                }
                if (!response.isSuccessful) {
                    throw RemoteHttpException(
                        statusCode = response.code,
                        message = "Custom source returned HTTP ${response.code}",
                    )
                }
                val responseBody = response.body
                    ?: throw RemoteApiException("Custom source returned an empty response")
                val declaredLength = responseBody.contentLength()
                if (declaredLength > MAX_RESPONSE_BYTES) {
                    throw RemoteApiException("Custom source response is larger than 1 MB")
                }
                val source = responseBody.source()
                if (source.request(MAX_RESPONSE_BYTES + 1L)) {
                    throw RemoteApiException("Custom source response is larger than 1 MB")
                }
                val bytes = source.buffer.readByteArray()
                if (bytes.any { byte -> byte == 0.toByte() }) {
                    throw RemoteApiException("Custom source returned binary content")
                }
                val charset = responseBody.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
                return@withContext FetchedCustomSourceContent(
                    body = bytes.toString(charset.safeCharset()),
                    finalUrl = currentUrl.toString(),
                    contentType = responseBody.contentType()?.toString(),
                )
            }
        }
        throw RemoteApiException("Custom source could not be loaded")
    }
}

private class PinnedDns(
    private val expectedHost: String,
    private val addresses: List<InetAddress>,
) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        if (!hostname.equals(expectedHost, ignoreCase = true)) {
            throw UnknownHostException("Unexpected custom source host")
        }
        return addresses
    }
}

private fun String.toSafeHttpsUrl(): HttpUrl {
    val parsed = toHttpUrlOrNull() ?: throw RemoteApiException("Custom source URL is invalid")
    if (!parsed.isHttps) throw RemoteApiException("Only HTTPS custom sources are supported")
    if (parsed.username.isNotEmpty() || parsed.password.isNotEmpty()) {
        throw RemoteApiException("Custom source URL must not contain credentials")
    }
    val normalizedHost = parsed.host.trimEnd('.').lowercase()
    if (normalizedHost == "localhost" || normalizedHost.endsWith(".localhost") ||
        normalizedHost.endsWith(".local")
    ) {
        throw RemoteApiException("Local network addresses are not allowed")
    }
    return parsed
}

private fun resolvePublicAddresses(host: String): List<InetAddress> {
    val addresses = try {
        InetAddress.getAllByName(host).toList()
    } catch (exception: UnknownHostException) {
        throw IOException("Custom source host could not be resolved", exception)
    }
    if (addresses.isEmpty() || addresses.any { address -> !address.isPublicInternetAddress() }) {
        throw RemoteApiException("Local or reserved network addresses are not allowed")
    }
    return addresses
}

internal fun InetAddress.isPublicInternetAddress(): Boolean {
    if (isAnyLocalAddress || isLoopbackAddress || isLinkLocalAddress ||
        isSiteLocalAddress || isMulticastAddress
    ) {
        return false
    }
    return when (this) {
        is Inet4Address -> {
            val octets = address.map { byte -> byte.toInt() and 0xff }
            val carrierGradeNat = octets[0] == 100 && octets[1] in 64..127
            val benchmark = octets[0] == 198 && octets[1] in 18..19
            !carrierGradeNat && !benchmark && octets[0] != 0
        }
        is Inet6Address -> {
            val first = address.first().toInt() and 0xff
            val uniqueLocal = first and 0xfe == 0xfc
            !uniqueLocal
        }
        else -> false
    }
}

private fun Response.isRedirectResponse(): Boolean = code in 300..399

private fun Charset.safeCharset(): Charset = runCatching { Charset.forName(name()) }
    .getOrDefault(Charsets.UTF_8)

private const val CUSTOM_SOURCE_TIMEOUT_SECONDS = 15L
private const val MAX_REDIRECTS = 3
private const val MAX_RESPONSE_BYTES = 1_048_576L
