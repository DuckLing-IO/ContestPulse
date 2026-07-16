package io.duckling.contestpulse.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ReminderPendingIntentIdentityTest {
    @Test
    fun reminderIdIsPartOfTheStableIdentityEvenWhenRequestCodesCouldCollide() {
        val first = reminderPendingIntentData("contest-a:reminder", 2)
        val second = reminderPendingIntentData("contest-b:reminder", 2)

        assertNotEquals(first, second)
        assertEquals(first, reminderPendingIntentData("contest-a:reminder", 2))
    }

    @Test
    fun legacyIdentityCanBeReconstructedForCancellation() {
        assertEquals(
            "contestpulse://reminder/contest%3Alegacy",
            reminderPendingIntentData("contest:legacy", 1),
        )
    }
}
