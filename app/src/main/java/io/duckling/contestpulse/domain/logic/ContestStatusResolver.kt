package io.duckling.contestpulse.domain.logic

import io.duckling.contestpulse.domain.model.Contest
import io.duckling.contestpulse.domain.model.ContestStatus
import java.time.Instant

fun Contest.statusAt(now: Instant): ContestStatus {
    if (now.isBefore(startTime)) return ContestStatus.UPCOMING

    val calculatedEnd = endTime ?: duration?.let(startTime::plus)
    if (calculatedEnd != null) {
        return if (now.isBefore(calculatedEnd)) {
            ContestStatus.RUNNING
        } else {
            ContestStatus.FINISHED
        }
    }

    return when (status) {
        ContestStatus.RUNNING,
        ContestStatus.FINISHED,
        -> status

        ContestStatus.UPCOMING,
        ContestStatus.UNKNOWN,
        -> ContestStatus.UNKNOWN
    }
}
