package io.duckling.contestpulse.data.remote

import io.duckling.contestpulse.domain.model.Contest
import io.duckling.contestpulse.domain.model.ContestSource
import io.duckling.contestpulse.data.remote.custom.CustomContestFetcher
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope

sealed interface ContestSourceFetchOutcome {
    val source: ContestSource
    val sourceKey: String

    data class Success(
        override val source: ContestSource,
        val contests: List<Contest>,
        override val sourceKey: String = source.name,
    ) : ContestSourceFetchOutcome

    data class Failure(
        override val source: ContestSource,
        val throwable: Throwable,
        override val sourceKey: String = source.name,
    ) : ContestSourceFetchOutcome
}

class RemoteContestFetcher @Inject constructor(
    private val dataSources: Set<@JvmSuppressWildcards ContestRemoteDataSource>,
    private val customContestFetcher: CustomContestFetcher? = null,
) {
    suspend fun fetchAll(
        enabledSources: Set<ContestSource> = ContestSource.entries.toSet(),
    ): List<ContestSourceFetchOutcome> = supervisorScope {
        val builtInTasks = dataSources.filter { dataSource -> dataSource.source in enabledSources }
            .map { dataSource ->
                async {
                    try {
                        ContestSourceFetchOutcome.Success(
                            source = dataSource.source,
                            contests = dataSource.fetchUpcomingContests(),
                        )
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (throwable: Throwable) {
                        ContestSourceFetchOutcome.Failure(
                            source = dataSource.source,
                            throwable = throwable,
                        )
                    }
                }
            }
        val customTask = async { customContestFetcher?.fetchEnabled().orEmpty() }
        (builtInTasks.map { task -> task.await() } + customTask.await())
            .sortedBy(ContestSourceFetchOutcome::sourceKey)
    }
}
