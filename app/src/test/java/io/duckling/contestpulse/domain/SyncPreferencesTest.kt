package io.duckling.contestpulse.domain

import io.duckling.contestpulse.domain.model.ContestSource
import io.duckling.contestpulse.domain.settings.SyncPreferences
import org.junit.Assert.assertEquals
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
}
