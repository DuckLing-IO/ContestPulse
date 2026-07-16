package io.duckling.contestpulse.data.settings

import io.duckling.contestpulse.domain.model.ReminderDefinition
import io.duckling.contestpulse.domain.model.ReminderRule
import java.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal object ReminderSettingsCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(reminders: List<ReminderDefinition>): String = json.encodeToString(
        reminders.map(ReminderDefinition::toStored),
    )

    fun decode(value: String): List<ReminderDefinition>? = runCatching {
        json.decodeFromString<List<StoredReminder>>(value)
            .mapNotNull(StoredReminder::toDomain)
            .distinctBy(ReminderDefinition::id)
    }.getOrNull()

    fun fromLegacyOffsets(offsets: Collection<Int>): List<ReminderDefinition> = offsets
        .distinct()
        .sorted()
        .map { offset ->
            ReminderDefinition(
                id = "legacy-relative-$offset",
                rule = ReminderRule.Relative(offset),
                createdAt = Instant.EPOCH,
            )
        }
}

@Serializable
private data class StoredReminder(
    val id: String,
    val type: String,
    val offsetMinutes: Int? = null,
    val dayOffset: Int? = null,
    val hour: Int? = null,
    val minute: Int? = null,
    val createdAtEpochMillis: Long,
)

private fun ReminderDefinition.toStored(): StoredReminder = when (val value = rule) {
    is ReminderRule.Relative -> StoredReminder(
        id = id,
        type = "RELATIVE",
        offsetMinutes = value.offsetMinutes,
        createdAtEpochMillis = createdAt.toEpochMilli(),
    )
    is ReminderRule.FixedTime -> StoredReminder(
        id = id,
        type = "FIXED_TIME",
        dayOffset = value.dayOffset,
        hour = value.hour,
        minute = value.minute,
        createdAtEpochMillis = createdAt.toEpochMilli(),
    )
}

private fun StoredReminder.toDomain(): ReminderDefinition? {
    if (id.isBlank()) return null
    val rule = when (type) {
        "RELATIVE" -> ReminderRule.Relative(offsetMinutes?.takeIf { it >= 0 } ?: return null)
        "FIXED_TIME" -> ReminderRule.FixedTime(
            dayOffset = dayOffset ?: return null,
            hour = hour ?: return null,
            minute = minute ?: return null,
        )
        else -> return null
    }
    return ReminderDefinition(id, rule, Instant.ofEpochMilli(createdAtEpochMillis))
}
