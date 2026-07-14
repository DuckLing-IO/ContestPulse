package io.duckling.contestpulse.domain.logic

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.Assert.assertTrue

class ReminderTriggerCalculatorTest {
    private val now = Instant.parse("2026-07-14T10:00:00Z")
    private val start = Instant.parse("2026-07-14T13:00:00Z")

    @Test
    fun subtractsOffsetFromContestStart() {
        assertEquals(
            Instant.parse("2026-07-14T12:00:00Z"),
            calculateReminderTrigger(start, Duration.ofHours(1), now),
        )
    }

    @Test
    fun filtersPastAndCurrentTriggers() {
        assertNull(calculateReminderTrigger(start, Duration.ofHours(4), now))
        assertNull(calculateReminderTrigger(start, Duration.ofHours(3), now))
    }

    @Test
    fun rejectsNegativeOffset() {
        assertThrows(IllegalArgumentException::class.java) {
            calculateReminderTrigger(start, Duration.ofMinutes(-1), now)
        }
    }

    @Test
    fun requestCode_isStableAndNonNegative() {
        val first = stableReminderRequestCode("codeforces:123:60")
        val second = stableReminderRequestCode("codeforces:123:60")

        assertEquals(first, second)
        assertTrue(first >= 0)
    }
}
