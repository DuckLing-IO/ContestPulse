package io.duckling.contestpulse.domain

import io.duckling.contestpulse.domain.model.ContestSource
import io.duckling.contestpulse.domain.settings.SyncPreferences
import io.duckling.contestpulse.domain.model.ReminderDefinition
import io.duckling.contestpulse.domain.model.ReminderRule
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SyncPreferencesTest {
    @Test
    fun newInstall_enablesAllProductionSources() {
        assertEquals(
            setOf(
                ContestSource.CODEFORCES,
                ContestSource.ATCODER,
                ContestSource.LUOGU,
                ContestSource.NOWCODER,
            ),
            SyncPreferences().enabledSources,
        )
    }

    @Test
    fun newInstall_usesOneHourForAutomaticFavoriteReminders() {
        assertEquals(setOf(60), SyncPreferences().defaultReminderOffsetsMinutes)
    }

    @Test
    fun automaticFavoriteReminders_acceptMultipleCustomOffsets() {
        val offsets = setOf(0, 75, 1_590)

        assertEquals(
            offsets,
            SyncPreferences(defaultReminders = offsets.map(::reminder)).defaultReminderOffsetsMinutes,
        )
    }

    @Test
    fun automaticFavoriteReminders_canBeDisabled() {
        assertEquals(
            emptySet<Int>(),
            SyncPreferences(defaultReminders = emptyList()).defaultReminderOffsetsMinutes,
        )
    }

    @Test
    fun automaticFavoriteReminders_rejectNegativeOffsets() {
        assertThrows(IllegalArgumentException::class.java) {
            reminder(-1)
        }
    }

    private fun reminder(offset: Int): ReminderDefinition = ReminderDefinition(
        id = "relative-$offset",
        rule = ReminderRule.Relative(offset),
        createdAt = Instant.EPOCH,
    )
}
