package io.duckling.contestpulse.core.database

import io.duckling.contestpulse.core.database.entity.ContestEntity
import io.duckling.contestpulse.core.database.model.ContestWithFavorite
import io.duckling.contestpulse.domain.model.Contest
import io.duckling.contestpulse.domain.model.ContestSource
import io.duckling.contestpulse.domain.model.ContestStatus
import io.duckling.contestpulse.domain.model.ReminderDefinition
import io.duckling.contestpulse.domain.model.ReminderDeliveryStatus
import io.duckling.contestpulse.domain.model.ReminderFailureReason
import io.duckling.contestpulse.domain.model.ReminderMode
import io.duckling.contestpulse.domain.model.ReminderRule
import io.duckling.contestpulse.domain.model.ReminderScheduleStatus
import io.duckling.contestpulse.domain.model.ScheduledReminder
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant

fun Contest.toEntity(): ContestEntity = ContestEntity(
    id = id,
    source = source.name,
    sourceContestId = sourceContestId,
    title = title,
    startTimeEpochMillis = startTime.toEpochMilli(),
    endTimeEpochMillis = endTime?.toEpochMilli(),
    durationMinutes = duration?.toMinutes(),
    registrationUrl = registrationUrl,
    contestUrl = contestUrl,
    status = status.name,
    category = category,
    difficultyLabel = difficultyLabel,
    ratedRange = ratedRange,
    isRated = isRated,
    lastUpdatedAtEpochMillis = lastUpdatedAt.toEpochMilli(),
    remoteFingerprint = remoteFingerprint(),
)

fun ContestWithFavorite.toDomain(): Contest = Contest(
    id = contest.id,
    source = enumValueOrDefault(contest.source, ContestSource.OTHER),
    sourceContestId = contest.sourceContestId,
    title = contest.title,
    startTime = Instant.ofEpochMilli(contest.startTimeEpochMillis),
    endTime = contest.endTimeEpochMillis?.let(Instant::ofEpochMilli),
    duration = contest.durationMinutes?.let(Duration::ofMinutes),
    registrationUrl = contest.registrationUrl,
    contestUrl = contest.contestUrl,
    status = enumValueOrDefault(contest.status, ContestStatus.UNKNOWN),
    category = contest.category,
    difficultyLabel = contest.difficultyLabel,
    ratedRange = contest.ratedRange,
    isRated = contest.isRated,
    isFavorite = favorite != null,
    reminderOffsets = reminders
        .asSequence()
        .filter { reminder -> reminder.isEnabled }
        .map { reminder -> Duration.ofMinutes(reminder.offsetMinutes) }
        .toSet(),
    lastUpdatedAt = Instant.ofEpochMilli(contest.lastUpdatedAtEpochMillis),
    reminders = reminders
        .asSequence()
        .filter { it.isEnabled }
        .mapNotNull { it.toDomainReminder() }
        .sortedWith(compareBy<ScheduledReminder> { it.triggerAt }.thenBy { it.id })
        .toList(),
    reminderMode = favorite?.reminderMode?.let { enumValueOrDefault(it, ReminderMode.CUSTOM) },
)

private fun io.duckling.contestpulse.core.database.entity.ReminderEntity.toDomainReminder(): ScheduledReminder? {
    val rule = when (ruleType) {
        io.duckling.contestpulse.core.database.entity.ReminderEntity.RULE_FIXED_TIME -> {
            ReminderRule.FixedTime(
                dayOffset = fixedDayOffset ?: return null,
                hour = fixedHour ?: return null,
                minute = fixedMinute ?: return null,
            )
        }
        else -> ReminderRule.Relative(offsetMinutes.toInt())
    }
    return ScheduledReminder(
        definition = ReminderDefinition(
            id = id,
            rule = rule,
            createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
        ),
        triggerAt = triggerAtEpochMillis?.let(Instant::ofEpochMilli),
        scheduleStatus = enumValueOrDefault(scheduleStatus, ReminderScheduleStatus.UNSCHEDULED),
        deliveryStatus = enumValueOrDefault(deliveryStatus, ReminderDeliveryStatus.NOT_ATTEMPTED),
        failureReason = failureReason
            ?.let { enumValueOrDefault(it, ReminderFailureReason.NONE) }
            ?: ReminderFailureReason.NONE,
    )
}

private fun Contest.remoteFingerprint(): String {
    val input = listOf(
        title,
        startTime.epochSecond.toString(),
        endTime?.epochSecond?.toString().orEmpty(),
        duration?.seconds?.toString().orEmpty(),
        contestUrl,
        status.name,
        category.orEmpty(),
        difficultyLabel.orEmpty(),
        ratedRange.orEmpty(),
        isRated?.toString().orEmpty(),
    ).joinToString(separator = "|")
    return MessageDigest.getInstance("SHA-256")
        .digest(input.toByteArray())
        .joinToString(separator = "") { byte ->
            "%02x".format(byte.toInt() and BYTE_MASK)
        }
}

private inline fun <reified T : Enum<T>> enumValueOrDefault(
    value: String,
    default: T,
): T = enumValues<T>().firstOrNull { it.name == value } ?: default

private const val BYTE_MASK = 0xff
