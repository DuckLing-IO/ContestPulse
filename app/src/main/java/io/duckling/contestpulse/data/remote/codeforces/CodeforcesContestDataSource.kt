package io.duckling.contestpulse.data.remote.codeforces

import io.duckling.contestpulse.data.remote.ContestRemoteDataSource
import io.duckling.contestpulse.data.remote.RemoteApiException
import io.duckling.contestpulse.domain.model.Contest
import io.duckling.contestpulse.domain.model.ContestSource
import java.time.Clock
import javax.inject.Inject

class CodeforcesContestDataSource @Inject constructor(
    private val api: CodeforcesApi,
    private val clock: Clock,
) : ContestRemoteDataSource {
    override val source: ContestSource = ContestSource.CODEFORCES

    override suspend fun fetchUpcomingContests(): List<Contest> {
        val response = api.getContests(includeGymContests = false)
        if (response.status != STATUS_OK) {
            throw RemoteApiException(response.comment ?: "Codeforces API returned ${response.status}")
        }
        val fetchedAt = clock.instant()
        val contests = response.result
            ?: throw RemoteApiException("Codeforces API response did not include a result")
        return contests
            .mapNotNull { contest -> contest.toDomainOrNull(fetchedAt) }
            .distinctBy(Contest::id)
            .sortedBy(Contest::startTime)
    }
}

private const val STATUS_OK = "OK"
