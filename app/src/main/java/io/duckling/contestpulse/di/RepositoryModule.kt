package io.duckling.contestpulse.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.IntoSet
import dagger.hilt.components.SingletonComponent
import io.duckling.contestpulse.data.remote.ContestRemoteDataSource
import io.duckling.contestpulse.data.remote.codeforces.CodeforcesContestDataSource
import io.duckling.contestpulse.data.remote.atcoder.AtCoderContestDataSource
import io.duckling.contestpulse.data.remote.luogu.LuoguContestDataSource
import io.duckling.contestpulse.data.remote.nowcoder.NowcoderContestDataSource
import io.duckling.contestpulse.data.repository.OfflineFirstContestRepository
import io.duckling.contestpulse.domain.repository.ContestRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindContestRepository(
        repository: OfflineFirstContestRepository,
    ): ContestRepository

    @Binds
    @IntoSet
    abstract fun bindCodeforcesDataSource(
        dataSource: CodeforcesContestDataSource,
    ): ContestRemoteDataSource

    @Binds
    @IntoSet
    abstract fun bindAtCoderDataSource(
        dataSource: AtCoderContestDataSource,
    ): ContestRemoteDataSource

    @Binds
    @IntoSet
    abstract fun bindLuoguDataSource(
        dataSource: LuoguContestDataSource,
    ): ContestRemoteDataSource

    @Binds
    @IntoSet
    abstract fun bindNowcoderDataSource(
        dataSource: NowcoderContestDataSource,
    ): ContestRemoteDataSource
}
