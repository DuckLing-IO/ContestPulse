package io.duckling.contestpulse.domain.model

import java.time.Duration
import java.time.Instant

data class Contest(
    val id: String,
    val source: ContestSource,
    val sourceContestId: String,
    val title: String,
    val startTime: Instant,
    val endTime: Instant?,
    val duration: Duration?,
    val registrationUrl: String?,
    val contestUrl: String,
    val status: ContestStatus,
    val category: String?,
    val difficultyLabel: String?,
    val ratedRange: String?,
    val isRated: Boolean?,
    val isFavorite: Boolean,
    val reminderOffsets: Set<Duration>,
    val lastUpdatedAt: Instant,
    val reminders: List<ScheduledReminder> = emptyList(),
    val reminderMode: ReminderMode? = null,
) {
    init {
        require(id.isNotBlank()) { "Contest id must not be blank" }
        require(sourceContestId.isNotBlank()) { "Source contest id must not be blank" }
        require(title.isNotBlank()) { "Contest title must not be blank" }
        require(duration == null || !duration.isNegative) { "Duration must not be negative" }
        require(endTime == null || !endTime.isBefore(startTime)) {
            "Contest end time must not be before start time"
        }
        require(reminderOffsets.none { it.isNegative }) {
            "Reminder offsets must not be negative"
        }
    }
}
