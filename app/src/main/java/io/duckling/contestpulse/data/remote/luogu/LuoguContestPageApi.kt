package io.duckling.contestpulse.data.remote.luogu

import okhttp3.ResponseBody
import retrofit2.http.GET

interface LuoguContestPageApi {
    @GET("contest/list")
    suspend fun getContestsPage(): ResponseBody
}
