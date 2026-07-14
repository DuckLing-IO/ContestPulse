package io.duckling.contestpulse.data.remote.atcoder

import okhttp3.ResponseBody
import retrofit2.http.GET

interface AtCoderContestPageApi {
    @GET("contests/?lang=en")
    suspend fun getContestsPage(): ResponseBody
}
