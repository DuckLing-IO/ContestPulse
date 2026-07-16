package io.duckling.contestpulse.data.settings

import io.duckling.contestpulse.domain.model.ReminderDefinition
import io.duckling.contestpulse.domain.model.ReminderRule
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderSettingsCodecTest {
    @Test
    fun roundTripPreservesStableIdsRulesAndCreationTimes() {
        val reminders = listOf(
            ReminderDefinition("relative", ReminderRule.Relative(30), Instant.ofEpochMilli(10)),
            ReminderDefinition(
                "fixed",
                ReminderRule.FixedTime(1, 20, 5),
                Instant.ofEpochMilli(20),
            ),
        )

        assertEquals(reminders, ReminderSettingsCodec.decode(ReminderSettingsCodec.encode(reminders)))
    }

    @Test
    fun malformedPayloadFallsBackWithoutCrashing() {
        assertNull(ReminderSettingsCodec.decode("not-json"))
    }

    @Test
    fun legacyOffsetsHaveDeterministicIdsAndAreDeduplicated() {
        val first = ReminderSettingsCodec.fromLegacyOffsets(listOf(30, 10, 30))
        val second = ReminderSettingsCodec.fromLegacyOffsets(listOf(30, 10))

        assertEquals(first, second)
        assertEquals(listOf("legacy-relative-10", "legacy-relative-30"), first.map { it.id })
    }
}
