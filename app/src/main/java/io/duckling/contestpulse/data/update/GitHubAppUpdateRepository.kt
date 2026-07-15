package io.duckling.contestpulse.data.update

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.duckling.contestpulse.domain.update.AppUpdate
import io.duckling.contestpulse.domain.update.AppUpdateCheckResult
import io.duckling.contestpulse.domain.update.AppUpdateDownloadResult
import io.duckling.contestpulse.domain.update.AppUpdateError
import io.duckling.contestpulse.domain.update.AppUpdateRepository
import io.duckling.contestpulse.domain.update.AppVersion
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class GitHubAppUpdateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gitHubReleaseApi: GitHubReleaseApi,
    private val okHttpClient: OkHttpClient,
) : AppUpdateRepository {
    override suspend fun checkForUpdate(currentVersion: AppVersion): AppUpdateCheckResult =
        withContext(Dispatchers.IO) {
            try {
                val response = gitHubReleaseApi.getLatestRelease(GITHUB_OWNER, GITHUB_REPOSITORY)
                when {
                    response.code() == NO_RELEASE_STATUS_CODE -> {
                        AppUpdateCheckResult.Failure(AppUpdateError.RELEASE)
                    }

                    !response.isSuccessful -> AppUpdateCheckResult.Failure(AppUpdateError.NETWORK)
                    else -> response.body()?.toCheckResult(currentVersion)
                        ?: AppUpdateCheckResult.Failure(AppUpdateError.RELEASE)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: IOException) {
                AppUpdateCheckResult.Failure(AppUpdateError.NETWORK)
            } catch (_: Exception) {
                AppUpdateCheckResult.Failure(AppUpdateError.UNKNOWN)
            }
        }

    override suspend fun downloadUpdate(
        update: AppUpdate,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
    ): AppUpdateDownloadResult = withContext(Dispatchers.IO) {
        val updateDirectory = File(context.cacheDir, UPDATE_DIRECTORY_NAME)
        if (!updateDirectory.exists() && !updateDirectory.mkdirs()) {
            return@withContext AppUpdateDownloadResult.Failure(AppUpdateError.DOWNLOAD)
        }

        val destination = File(updateDirectory, update.apkFileName)
        val temporaryFile = File(updateDirectory, "${update.apkFileName}.part")
        temporaryFile.delete()

        try {
            val expectedChecksum = update.checksumDownloadUrl?.let { checksumUrl ->
                downloadChecksum(checksumUrl, update.apkFileName)
                    ?: return@withContext AppUpdateDownloadResult.Failure(
                    AppUpdateError.INTEGRITY,
                )
            }
            val request = Request.Builder().url(update.apkDownloadUrl).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext AppUpdateDownloadResult.Failure(AppUpdateError.DOWNLOAD)
                }
                val responseBody = response.body
                    ?: return@withContext AppUpdateDownloadResult.Failure(AppUpdateError.DOWNLOAD)
                val totalBytes = responseBody.contentLength()
                var downloadedBytes = 0L
                responseBody.byteStream().use { input ->
                    temporaryFile.outputStream().buffered().use { output ->
                        val buffer = ByteArray(BUFFER_SIZE_BYTES)
                        while (true) {
                            val bytesRead = input.read(buffer)
                            if (bytesRead == END_OF_STREAM) break
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            onProgress(downloadedBytes, totalBytes)
                        }
                    }
                }
            }

            if (expectedChecksum != null && temporaryFile.sha256() != expectedChecksum) {
                temporaryFile.delete()
                return@withContext AppUpdateDownloadResult.Failure(AppUpdateError.INTEGRITY)
            }
            if (!temporaryFile.renameTo(destination)) {
                temporaryFile.copyTo(destination, overwrite = true)
                temporaryFile.delete()
            }
            AppUpdateDownloadResult.Success(
                apkFile = destination,
                isChecksumVerified = expectedChecksum != null,
            )
        } catch (error: CancellationException) {
            temporaryFile.delete()
            throw error
        } catch (_: IOException) {
            temporaryFile.delete()
            AppUpdateDownloadResult.Failure(AppUpdateError.DOWNLOAD)
        } catch (_: Exception) {
            temporaryFile.delete()
            AppUpdateDownloadResult.Failure(AppUpdateError.UNKNOWN)
        }
    }

    private fun GitHubReleaseDto.toCheckResult(currentVersion: AppVersion): AppUpdateCheckResult {
        val latestVersion = ReleaseVersion.parse(tagName)
            ?: return AppUpdateCheckResult.Failure(AppUpdateError.RELEASE)
        val installedVersion = ReleaseVersion.parse(currentVersion.name)
            ?: return AppUpdateCheckResult.Failure(AppUpdateError.RELEASE)
        if (latestVersion <= installedVersion) return AppUpdateCheckResult.UpToDate

        val apkAsset = assets.firstOrNull { asset ->
            asset.name.endsWith(APK_EXTENSION, ignoreCase = true) &&
                !asset.name.contains(DEBUG_APK_MARKER, ignoreCase = true)
        } ?: return AppUpdateCheckResult.Failure(AppUpdateError.RELEASE)
        val checksumAsset = assets.firstOrNull { asset ->
            asset.name.equals("${apkAsset.name}.sha256", ignoreCase = true) ||
                asset.name.equals("${apkAsset.name}.sha256sum", ignoreCase = true) ||
                asset.name.equals(CHECKSUMS_FILE_NAME, ignoreCase = true)
        }
        return AppUpdateCheckResult.UpdateAvailable(
            AppUpdate(
                versionName = tagName.removePrefix("v").removePrefix("V"),
                releaseNotes = body.orEmpty().trim().take(MAX_RELEASE_NOTES_LENGTH),
                apkDownloadUrl = apkAsset.downloadUrl,
                apkFileName = apkAsset.name,
                checksumDownloadUrl = checksumAsset?.downloadUrl,
            ),
        )
    }

    private fun downloadChecksum(
        url: String,
        apkFileName: String,
    ): String? {
        val request = Request.Builder().url(url).build()
        return okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val responseBody = response.body ?: return null
            if (responseBody.contentLength() > MAX_CHECKSUM_FILE_BYTES) return null
            val content = responseBody.string()
            val lines = content.lineSequence().toList()
            val matchingLine = lines.firstOrNull { line ->
                line.trim().endsWith(apkFileName)
            }
            when {
                matchingLine != null -> CHECKSUM_PATTERN.find(matchingLine)?.value?.lowercase()
                lines.size == 1 -> CHECKSUM_PATTERN.find(lines.single())?.value?.lowercase()
                else -> null
            }
        }
    }

    private fun File.sha256(): String = inputStream().buffered().use { input ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(BUFFER_SIZE_BYTES)
        while (true) {
            val bytesRead = input.read(buffer)
            if (bytesRead == END_OF_STREAM) break
            digest.update(buffer, 0, bytesRead)
        }
        digest.digest().joinToString(separator = "") { byte ->
            "%02x".format(byte.toInt() and 0xff)
        }
    }
}

private const val GITHUB_OWNER = "DuckLing-IO"
private const val GITHUB_REPOSITORY = "ContestPulse"
private const val NO_RELEASE_STATUS_CODE = 404
private const val UPDATE_DIRECTORY_NAME = "updates"
private const val APK_EXTENSION = ".apk"
private const val DEBUG_APK_MARKER = "debug"
private const val CHECKSUMS_FILE_NAME = "SHA256SUMS"
private const val MAX_RELEASE_NOTES_LENGTH = 1_200
private const val MAX_CHECKSUM_FILE_BYTES = 64 * 1024L
private const val BUFFER_SIZE_BYTES = 8 * 1024
private const val END_OF_STREAM = -1
private val CHECKSUM_PATTERN = Regex("(?i)\\b[a-f0-9]{64}\\b")
