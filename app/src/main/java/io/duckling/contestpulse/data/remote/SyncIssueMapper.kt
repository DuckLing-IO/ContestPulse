package io.duckling.contestpulse.data.remote

import io.duckling.contestpulse.domain.model.SyncErrorType
import io.duckling.contestpulse.domain.model.SyncIssue
import java.io.IOException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

fun Throwable.toSyncIssue(): SyncIssue {
    val type = when (this) {
        is IOException -> SyncErrorType.NETWORK
        is HttpException -> if (code() == HTTP_TOO_MANY_REQUESTS) {
            SyncErrorType.RATE_LIMIT
        } else {
            SyncErrorType.HTTP
        }
        is RemoteHttpException -> if (statusCode == HTTP_TOO_MANY_REQUESTS) {
            SyncErrorType.RATE_LIMIT
        } else {
            SyncErrorType.HTTP
        }
        is SerializationException -> SyncErrorType.PARSING
        is RemoteParsingException -> SyncErrorType.PARSING
        is RemoteApiException -> SyncErrorType.REMOTE_API
        else -> SyncErrorType.UNKNOWN
    }
    return SyncIssue(
        type = type,
        diagnosticMessage = message?.take(MAX_DIAGNOSTIC_LENGTH),
    )
}

class RemoteApiException(message: String) : IllegalStateException(message)

class RemoteHttpException(
    val statusCode: Int,
    message: String,
) : IllegalStateException(message)

class RemoteParsingException(message: String) : IllegalArgumentException(message)

private const val MAX_DIAGNOSTIC_LENGTH = 240
private const val HTTP_TOO_MANY_REQUESTS = 429
