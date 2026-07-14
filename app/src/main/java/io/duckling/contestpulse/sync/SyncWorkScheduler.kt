package io.duckling.contestpulse.sync

interface SyncWorkScheduler {
    suspend fun applyPreferences()
}
