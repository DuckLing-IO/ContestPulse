package io.duckling.contestpulse.data.remote.codeforces

import kotlinx.serialization.Serializable

@Serializable
data class CodeforcesContestResponse(
    val status: String,
    val result: List<CodeforcesContestDto>? = null,
    val comment: String? = null,
)

@Serializable
data class CodeforcesContestDto(
    val id: Long? = null,
    val name: String? = null,
    val type: String? = null,
    val phase: String? = null,
    val durationSeconds: Long? = null,
    val startTimeSeconds: Long? = null,
)
