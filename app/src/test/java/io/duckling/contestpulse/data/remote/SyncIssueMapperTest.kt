package io.duckling.contestpulse.data.remote

import io.duckling.contestpulse.domain.model.SyncErrorType
import java.io.IOException
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Test
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

class SyncIssueMapperTest {
    @Test
    fun mapsSafeErrorCategories() {
        assertEquals(SyncErrorType.NETWORK, IOException("offline").toSyncIssue().type)
        assertEquals(
            SyncErrorType.PARSING,
            SerializationException("bad payload").toSyncIssue().type,
        )
        assertEquals(
            SyncErrorType.REMOTE_API,
            RemoteApiException("failed").toSyncIssue().type,
        )
        assertEquals(SyncErrorType.UNKNOWN, IllegalStateException().toSyncIssue().type)
        assertEquals(
            SyncErrorType.RATE_LIMIT,
            HttpException(Response.error<Unit>(429, "rate limited".toResponseBody()))
                .toSyncIssue().type,
        )
    }
}
