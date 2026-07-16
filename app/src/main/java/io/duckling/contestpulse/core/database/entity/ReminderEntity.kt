package io.duckling.contestpulse.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = ContestEntity::class,
            parentColumns = ["id"],
            childColumns = ["contestId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["contestId"]),
        Index(value = ["contestId", "ruleKey"], unique = true),
        Index(value = ["triggerAtEpochMillis"]),
    ],
)
data class ReminderEntity(
    @PrimaryKey val id: String,
    val contestId: String,
    val triggerAtEpochMillis: Long?,
    val offsetMinutes: Long,
    val ruleType: String = RULE_RELATIVE,
    val ruleKey: String = "relative:$offsetMinutes",
    val fixedDayOffset: Int? = null,
    val fixedHour: Int? = null,
    val fixedMinute: Int? = null,
    val isEnabled: Boolean,
    val schedulerType: String,
    val scheduleStatus: String = STATUS_UNSCHEDULED,
    val deliveryStatus: String = DELIVERY_NOT_ATTEMPTED,
    val failureReason: String? = null,
    val lastDeliveryAttemptAtEpochMillis: Long? = null,
    val systemRequestCode: Int,
    val createdAtEpochMillis: Long,
) {
    companion object {
        const val RULE_RELATIVE = "RELATIVE"
        const val RULE_FIXED_TIME = "FIXED_TIME"
        const val STATUS_UNSCHEDULED = "UNSCHEDULED"
        const val DELIVERY_NOT_ATTEMPTED = "NOT_ATTEMPTED"
    }
}
