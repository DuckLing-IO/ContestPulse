package io.duckling.contestpulse.domain

import io.duckling.contestpulse.domain.logic.fallbackContestId
import io.duckling.contestpulse.domain.logic.stableContestId
import io.duckling.contestpulse.domain.model.ContestSource
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ContestIdentityTest {
    @Test
    fun sourceId_isNamespacedBySource() {
        assertEquals(
            "codeforces:123",
            stableContestId(ContestSource.CODEFORCES, "123"),
        )
    }

    @Test
    fun fallbackId_isStableAcrossWhitespaceAndCase() {
        val start = Instant.parse("2026-07-14T12:00:00Z")

        val first = fallbackContestId(ContestSource.ATCODER, " Demo   Contest ", start)
        val second = fallbackContestId(ContestSource.ATCODER, "demo contest", start)
        val changed = fallbackContestId(ContestSource.ATCODER, "demo contest", start.plusSeconds(1))

        assertEquals(first, second)
        assertNotEquals(first, changed)
    }
}
