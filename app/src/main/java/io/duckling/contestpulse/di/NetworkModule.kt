package io.duckling.contestpulse.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.duckling.contestpulse.data.remote.codeforces.CodeforcesApi
import io.duckling.contestpulse.data.update.GitHubReleaseApi
import io.duckling.contestpulse.data.remote.atcoder.AtCoderContestPageApi
import io.duckling.contestpulse.data.remote.luogu.LuoguContestPageApi
import io.duckling.contestpulse.data.remote.nowcoder.NowcoderContestPageApi
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .retryOnConnectionFailure(true)
        .connectTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .build(),
            )
        }
        .build()

    @Provides
    @Singleton
    fun provideCodeforcesApi(
        json: Json,
        okHttpClient: OkHttpClient,
    ): CodeforcesApi = Retrofit.Builder()
        .baseUrl("https://codeforces.com/")
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE.toMediaType()))
        .build()
        .create(CodeforcesApi::class.java)

    @Provides
    @Singleton
    fun provideAtCoderContestPageApi(
        okHttpClient: OkHttpClient,
    ): AtCoderContestPageApi = Retrofit.Builder()
        .baseUrl("https://atcoder.jp/")
        .client(okHttpClient)
        .build()
        .create(AtCoderContestPageApi::class.java)

    @Provides
    @Singleton
    fun provideLuoguContestPageApi(
        okHttpClient: OkHttpClient,
    ): LuoguContestPageApi = Retrofit.Builder()
        .baseUrl("https://www.luogu.com.cn/")
        .client(okHttpClient)
        .build()
        .create(LuoguContestPageApi::class.java)

    @Provides
    @Singleton
    fun provideNowcoderContestPageApi(
        okHttpClient: OkHttpClient,
    ): NowcoderContestPageApi = Retrofit.Builder()
        .baseUrl("https://ac.nowcoder.com/")
        .client(okHttpClient)
        .build()
        .create(NowcoderContestPageApi::class.java)

    @Provides
    @Singleton
    fun provideGitHubReleaseApi(
        json: Json,
        okHttpClient: OkHttpClient,
    ): GitHubReleaseApi = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE.toMediaType()))
        .build()
        .create(GitHubReleaseApi::class.java)
}

private const val JSON_MEDIA_TYPE = "application/json"
private const val USER_AGENT = "ContestPulse/0.9 (Android; local-first contest schedule)"
private const val NETWORK_TIMEOUT_SECONDS = 15L
