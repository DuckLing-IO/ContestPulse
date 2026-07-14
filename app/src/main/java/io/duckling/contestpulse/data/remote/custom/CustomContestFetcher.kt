package io.duckling.contestpulse.data.remote.custom

import io.duckling.contestpulse.data.remote.ContestSourceFetchOutcome
import io.duckling.contestpulse.domain.customsource.CustomSourceRepository
import io.duckling.contestpulse.domain.model.ContestSource
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.supervisorScope

class CustomContestFetcher @Inject constructor(
    private val repository: CustomSourceRepository,
    private val previewer: CustomSourcePreviewer,
) {
    suspend fun fetchEnabled(): List<ContestSourceFetchOutcome> = supervisorScope {
        repository.sources.first()
            .filter { source -> source.enabled }
            .map { source ->
                async {
                    try {
                        ContestSourceFetchOutcome.Success(
                            source = ContestSource.OTHER,
                            sourceKey = source.sourceKey,
                            contests = previewer.preview(source).contests,
                        )
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (throwable: Throwable) {
                        ContestSourceFetchOutcome.Failure(
                            source = ContestSource.OTHER,
                            sourceKey = source.sourceKey,
                            throwable = throwable,
                        )
                    }
                }
            }
            .map { task -> task.await() }
    }
}
