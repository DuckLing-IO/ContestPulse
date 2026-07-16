package io.duckling.contestpulse.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.ZoneId
import io.duckling.contestpulse.core.time.SystemTimeZoneProvider
import io.duckling.contestpulse.core.time.TimeZoneProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()

    @Provides
    @Singleton
    fun provideTimeZoneProvider(): TimeZoneProvider = SystemTimeZoneProvider()

    @Provides
    fun provideZoneId(provider: TimeZoneProvider): ZoneId = provider.currentZoneId()
}
