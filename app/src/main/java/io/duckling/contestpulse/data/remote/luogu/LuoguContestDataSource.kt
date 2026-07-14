package io.duckling.contestpulse.data.remote.luogu

import io.duckling.contestpulse.data.remote.ContestRemoteDataSource
import io.duckling.contestpulse.domain.model.Contest
import io.duckling.contestpulse.domain.model.ContestSource
import java.time.Clock
import javax.inject.Inject

class LuoguContestDataSource @Inject constructor(
    private val api: LuoguContestPageApi,
    private val parser: LuoguContestParser,
    private val clock: Clock,
) : ContestRemoteDataSource {
    override val source: ContestSource = ContestSource.LUOGU

    override suspend fun fetchUpcomingContests(): List<Contest> =
        api.getContestsPage().use { responseBody ->
            parser.parse(
                html = responseBody.string(),
                fetchedAt = clock.instant(),
            )
        }
}
