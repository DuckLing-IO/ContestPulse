package io.duckling.contestpulse.domain.logic

import io.duckling.contestpulse.domain.model.ContestSource
import java.security.MessageDigest
import java.time.Instant
import java.util.Locale

fun stableContestId(
    source: ContestSource,
    sourceContestId: String,
): String = "${source.name.lowercase(Locale.ROOT)}:$sourceContestId"

fun fallbackContestId(
    source: ContestSource,
    title: String,
    startTime: Instant,
): String {
    val normalizedTitle = title
        .trim()
        .lowercase(Locale.ROOT)
        .replace(Regex("\\s+"), " ")
    val input = "${source.name}|$normalizedTitle|${startTime.epochSecond}"
    val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    val shortHash = digest.take(HASH_BYTES).joinToString(separator = "") { byte ->
        "%02x".format(byte)
    }
    return "${source.name.lowercase(Locale.ROOT)}:$shortHash"
}

private const val HASH_BYTES = 12
