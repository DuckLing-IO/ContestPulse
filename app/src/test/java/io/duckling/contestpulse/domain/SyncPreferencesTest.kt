package io.duckling.contestpulse.domain

import io.duckling.contestpulse.domain.model.ContestSource
import io.duckling.contestpulse.domain.settings.SyncPreferences
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
            SyncPreferences(defaultReminderOffsetsMinutes = offsets).defaultReminderOffsetsMinutes,
        )
    }

    @Test
    fun automaticFavoriteReminders_canBeDisabled() {
        assertEquals(
            emptySet<Int>(),
            SyncPreferences(defaultReminderOffsetsMinutes = emptySet()).defaultReminderOffsetsMinutes,
        )
    }

    @Test
    fun automaticFavoriteReminders_rejectNegativeOffsets() {
        assertThrows(IllegalArgumentException::class.java) {
            SyncPreferences(defaultReminderOffsetsMinutes = setOf(-1))
        }
    }
}
