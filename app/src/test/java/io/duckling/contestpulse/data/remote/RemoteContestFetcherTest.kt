package io.duckling.contestpulse.data.remote

import io.duckling.contestpulse.domain.model.Contest
import io.duckling.contestpulse.domain.model.ContestSource
import io.duckling.contestpulse.domain.testContest
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteContestFetcherTest {
    @Test
    fun sourceFailure_doesNotCancelSuccessfulSource() = runBlocking {
        val expected = testContest(sourceContestId = "success")
        val fetcher = RemoteContestFetcher(
            dataSources = setOf(
                FakeDataSource(ContestSource.CODEFORCES) { listOf(expected) },
                FakeDataSource(ContestSource.ATCODER) { throw IOException("offline") },
            ),
        )

        val outcomes = fetcher.fetchAll()

        assertEquals(2, outcomes.size)
        val success = outcomes.filterIsInstance<ContestSourceFetchOutcome.Success>().single()
        val failure = outcomes.filterIsInstance<ContestSourceFetchOutcome.Failure>().single()
        assertEquals(listOf(expected), success.contests)
        assertEquals(ContestSource.ATCODER, failure.source)
        assertTrue(failure.throwable is IOException)
    }
}

private class FakeDataSource(
    override val source: ContestSource,
    private val result: suspend () -> List<Contest>,
) : ContestRemoteDataSource {
    override suspend fun fetchUpcomingContests(): List<Contest> = result()
}
