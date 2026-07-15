package io.duckling.contestpulse.domain.update

import java.io.File

data class AppVersion(
    val name: String,
    val code: Long,
)

data class AppUpdate(
    val versionName: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val apkFileName: String,
    val checksumDownloadUrl: String?,
)

enum class AppUpdateError {
    NETWORK,
    RELEASE,
    DOWNLOAD,
    TIMEOUT,
    STORAGE,
    INTEGRITY,
    UNKNOWN,
}

sealed interface AppUpdateCheckResult {
    data object UpToDate : AppUpdateCheckResult

    data class UpdateAvailable(
        val update: AppUpdate,
    ) : AppUpdateCheckResult

    data class Failure(
        val error: AppUpdateError,
        val detail: String? = null,
    ) : AppUpdateCheckResult
}

sealed interface AppUpdateDownloadResult {
    data class Success(
        val apkFile: File,
        val isChecksumVerified: Boolean,
    ) : AppUpdateDownloadResult

    data class Failure(
        val error: AppUpdateError,
        val detail: String? = null,
    ) : AppUpdateDownloadResult
}

interface AppUpdateRepository {
    suspend fun checkForUpdate(currentVersion: AppVersion): AppUpdateCheckResult

    suspend fun downloadUpdate(
        update: AppUpdate,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
    ): AppUpdateDownloadResult
}

interface AppVersionProvider {
    fun currentVersion(): AppVersion
}
