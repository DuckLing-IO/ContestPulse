package io.duckling.contestpulse.data.remote

import io.duckling.contestpulse.domain.model.Contest
import io.duckling.contestpulse.domain.model.ContestSource

interface ContestRemoteDataSource {
    val source: ContestSource

    suspend fun fetchUpcomingContests(): List<Contest>
}
