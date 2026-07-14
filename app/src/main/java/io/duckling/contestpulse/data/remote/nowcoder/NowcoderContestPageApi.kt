package io.duckling.contestpulse.data.remote.nowcoder

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query

interface NowcoderContestPageApi {
    @GET("acm/contest/vip-index")
    suspend fun getContestsPage(
        @Query("topCategoryFilter") topCategoryFilter: Int,
    ): ResponseBody
}
