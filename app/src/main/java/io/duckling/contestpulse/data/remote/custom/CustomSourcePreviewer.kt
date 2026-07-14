package io.duckling.contestpulse.data.remote.custom

import io.duckling.contestpulse.domain.customsource.CustomContestSource
import java.time.Clock
import java.net.URI
import javax.inject.Inject

class CustomSourcePreviewer @Inject constructor(
    private val httpClient: SafeCustomSourceHttpClient,
    private val parser: CustomContestParser,
    private val clock: Clock,
) {
    suspend fun preview(source: CustomContestSource): CustomSourceParseResult {
        source.requireValid()
        val content = httpClient.get(source.url)
        val result = parser.parse(source, content, clock.instant())
        return if (source.url.isBuiltInContestSource()) {
            result.copy(
                warnings = result.warnings +
                    "该网址属于内置平台；请关闭对应内置来源，避免比赛重复显示",
            )
        } else {
            result
        }
    }
}

private fun String.isBuiltInContestSource(): Boolean {
    val uri = runCatching { URI(this) }.getOrNull() ?: return false
    val host = uri.host?.lowercase() ?: return false
    val path = uri.path.orEmpty()
    return when (host) {
        "codeforces.com", "www.codeforces.com" -> path.startsWith("/api/contest.list") ||
            path.startsWith("/contests")
        "atcoder.jp" -> path.startsWith("/contests")
        "www.luogu.com.cn", "luogu.com.cn" -> path.startsWith("/contest/list")
        "ac.nowcoder.com" -> path.startsWith("/acm/contest/vip-index")
        else -> false
    }
}
