package io.duckling.contestpulse.data.remote.nowcoder

import io.duckling.contestpulse.data.remote.ContestRemoteDataSource
import io.duckling.contestpulse.domain.model.Contest
import io.duckling.contestpulse.domain.model.ContestSource
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class NowcoderContestDataSource @Inject constructor(
    private val api: NowcoderContestPageApi,
    private val parser: NowcoderContestParser,
    private val clock: Clock,
) : ContestRemoteDataSource {
    override val source: ContestSource = ContestSource.NOWCODER

    override suspend fun fetchUpcomingContests(): List<Contest> = coroutineScope {
        val fetchedAt = clock.instant()
        NOWCODER_PUBLIC_CATEGORIES.map { category ->
            async {
                api.getContestsPage(category).use { responseBody ->
                    parser.parse(
                        html = responseBody.string(),
                        fetchedAt = fetchedAt,
                    )
                }
            }
        }.awaitAll()
            .flatten()
            .distinctBy(Contest::id)
            .sortedBy(Contest::startTime)
    }
}

private const val NOWCODER_SERIES_CATEGORY = 13
private const val SCHOOL_CONTEST_CATEGORY = 14
private val NOWCODER_PUBLIC_CATEGORIES = listOf(
    NOWCODER_SERIES_CATEGORY,
    SCHOOL_CONTEST_CATEGORY,
)
