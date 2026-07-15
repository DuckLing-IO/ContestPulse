package io.duckling.contestpulse.domain.settings

import io.duckling.contestpulse.domain.model.ContestSource
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val preferences: Flow<SyncPreferences>

    suspend fun setBackgroundSyncEnabled(enabled: Boolean)

    suspend fun setWifiOnly(enabled: Boolean)

    suspend fun setIntervalHours(hours: Int)

    suspend fun setDefaultReminderOffsetsMinutes(offsetsMinutes: Set<Int>)

    suspend fun setSourceEnabled(
        source: ContestSource,
        enabled: Boolean,
    )
}
