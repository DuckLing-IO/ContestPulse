package io.duckling.contestpulse.domain.logic

import io.duckling.contestpulse.domain.model.Contest
import io.duckling.contestpulse.domain.model.ContestGroup
import io.duckling.contestpulse.domain.model.ContestGroupType
import io.duckling.contestpulse.domain.model.ContestStatus
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

fun buildContestTimeline(
    contests: List<Contest>,
    now: Instant,
    zoneId: ZoneId,
): List<ContestGroup> {
    val today = now.atZone(zoneId).toLocalDate()
    val tomorrow = today.plusDays(1)
    val endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

    return contests
        .asSequence()
        .filter { it.statusAt(now) != ContestStatus.FINISHED }
        .sortedBy(Contest::startTime)
        .groupBy { contest ->
            val status = contest.statusAt(now)
            val localDate = contest.startTime.atZone(zoneId).toLocalDate()
            when {
                status == ContestStatus.RUNNING -> ContestGroupType.RUNNING
                localDate == today -> ContestGroupType.TODAY
                localDate == tomorrow -> ContestGroupType.TOMORROW
                !localDate.isAfter(endOfWeek) -> ContestGroupType.THIS_WEEK
                else -> ContestGroupType.LATER
            }
        }
        .let { grouped ->
            ContestGroupType.entries.mapNotNull { type ->
                grouped[type]?.takeIf(List<Contest>::isNotEmpty)?.let { items ->
                    ContestGroup(type = type, contests = items)
                }
            }
        }
}
