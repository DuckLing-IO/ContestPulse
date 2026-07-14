package io.duckling.contestpulse.data.remote.codeforces

import io.duckling.contestpulse.domain.logic.stableContestId
import io.duckling.contestpulse.domain.model.Contest
import io.duckling.contestpulse.domain.model.ContestSource
import io.duckling.contestpulse.domain.model.ContestStatus
import java.time.DateTimeException
import java.time.Duration
import java.time.Instant

fun CodeforcesContestDto.toDomainOrNull(fetchedAt: Instant): Contest? {
    val contestId = id?.takeIf { it > 0 } ?: return null
    val contestTitle = name?.trim()?.takeIf(String::isNotEmpty) ?: return null
    val startSeconds = startTimeSeconds?.takeIf { it > 0 } ?: return null
    val contestDurationSeconds = durationSeconds?.takeIf { it > 0 } ?: return null
    val contestPhase = phase.orEmpty()
    if (contestPhase !in VISIBLE_PHASES) return null

    return try {
        val start = Instant.ofEpochSecond(startSeconds)
        val duration = Duration.ofSeconds(contestDurationSeconds)
        Contest(
            id = stableContestId(ContestSource.CODEFORCES, contestId.toString()),
            source = ContestSource.CODEFORCES,
            sourceContestId = contestId.toString(),
            title = contestTitle,
            startTime = start,
            endTime = start.plus(duration),
            duration = duration,
            registrationUrl = null,
            contestUrl = "https://codeforces.com/contest/$contestId",
            status = when (contestPhase) {
                PHASE_BEFORE -> ContestStatus.UPCOMING
                PHASE_CODING -> ContestStatus.RUNNING
                else -> ContestStatus.UNKNOWN
            },
            category = type?.trim()?.takeIf(String::isNotEmpty),
            difficultyLabel = null,
            ratedRange = null,
            isRated = null,
            isFavorite = false,
            reminderOffsets = emptySet(),
            lastUpdatedAt = fetchedAt,
        )
    } catch (_: DateTimeException) {
        null
    } catch (_: ArithmeticException) {
        null
    }
}

private const val PHASE_BEFORE = "BEFORE"
private const val PHASE_CODING = "CODING"
private val VISIBLE_PHASES = setOf(PHASE_BEFORE, PHASE_CODING)
