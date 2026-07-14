package io.duckling.contestpulse.data.remote.codeforces

import retrofit2.http.GET
import retrofit2.http.Query

interface CodeforcesApi {
    @GET("api/contest.list")
    suspend fun getContests(
        @Query("gym") includeGymContests: Boolean = false,
    ): CodeforcesContestResponse
}
