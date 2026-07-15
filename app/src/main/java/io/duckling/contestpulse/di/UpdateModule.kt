package io.duckling.contestpulse.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.duckling.contestpulse.core.update.AndroidAppVersionProvider
import io.duckling.contestpulse.data.update.GitHubAppUpdateRepository
import io.duckling.contestpulse.domain.update.AppUpdateRepository
import io.duckling.contestpulse.domain.update.AppVersionProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UpdateModule {
    @Binds
    @Singleton
    abstract fun bindAppUpdateRepository(
        repository: GitHubAppUpdateRepository,
    ): AppUpdateRepository

    @Binds
    @Singleton
    abstract fun bindAppVersionProvider(
        provider: AndroidAppVersionProvider,
    ): AppVersionProvider
}
