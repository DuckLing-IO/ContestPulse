package io.duckling.contestpulse.data.remote.atcoder

import io.duckling.contestpulse.data.remote.ContestRemoteDataSource
import io.duckling.contestpulse.domain.model.Contest
import io.duckling.contestpulse.domain.model.ContestSource
import java.time.Clock
import javax.inject.Inject

class AtCoderContestDataSource @Inject constructor(
    private val api: AtCoderContestPageApi,
    private val parser: AtCoderContestParser,
    private val clock: Clock,
) : ContestRemoteDataSource {
    override val source: ContestSource = ContestSource.ATCODER

    override suspend fun fetchUpcomingContests(): List<Contest> =
        api.getContestsPage().use { responseBody ->
            parser.parse(
                html = responseBody.string(),
                fetchedAt = clock.instant(),
            )
        }
}
