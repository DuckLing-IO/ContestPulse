package io.duckling.contestpulse.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseVersionTest {
    @Test
    fun `parses GitHub tags and app version names`() {
        assertEquals(
            ReleaseVersion.parse("1.2.3"),
            ReleaseVersion.parse("v1.2.3"),
        )
        assertEquals(
            ReleaseVersion.parse("1.2"),
            ReleaseVersion.parse("1.2.0"),
        )
        assertEquals(
            ReleaseVersion.parse("1"),
            ReleaseVersion.parse("1.0.0"),
        )
    }

    @Test
    fun `compares numeric segments instead of lexical text`() {
        val newer = requireNotNull(ReleaseVersion.parse("v1.2.10"))
        val older = requireNotNull(ReleaseVersion.parse("1.2.9"))

        assertTrue(newer > older)
    }

    @Test
    fun `rejects tags without a numeric version`() {
        assertNull(ReleaseVersion.parse("latest"))
        assertNull(ReleaseVersion.parse("v1.2.beta"))
    }
}
