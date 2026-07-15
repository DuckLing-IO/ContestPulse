package io.duckling.contestpulse.data.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path

interface GitHubReleaseApi {
    @Headers(
        "Accept: application/vnd.github+json",
        "X-GitHub-Api-Version: 2022-11-28",
    )
    @GET("repos/{owner}/{repository}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repository") repository: String,
    ): Response<GitHubReleaseDto>
}

@Serializable
data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String,
    val body: String? = null,
    val assets: List<GitHubReleaseAssetDto> = emptyList(),
)

@Serializable
data class GitHubReleaseAssetDto(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
)
