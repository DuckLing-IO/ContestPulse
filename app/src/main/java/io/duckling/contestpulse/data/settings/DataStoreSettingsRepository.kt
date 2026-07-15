package io.duckling.contestpulse.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import io.duckling.contestpulse.domain.model.ContestSource
import io.duckling.contestpulse.domain.settings.DEFAULT_ENABLED_SOURCES
import io.duckling.contestpulse.domain.settings.DEFAULT_REMINDER_OFFSETS_MINUTES
import io.duckling.contestpulse.domain.settings.DEFAULT_SYNC_INTERVAL_HOURS
import io.duckling.contestpulse.domain.settings.MAX_REMINDER_OFFSET_MINUTES
import io.duckling.contestpulse.domain.settings.MIN_REMINDER_OFFSET_MINUTES
import io.duckling.contestpulse.domain.settings.SUPPORTED_SYNC_INTERVALS
import io.duckling.contestpulse.domain.settings.SettingsRepository
import io.duckling.contestpulse.domain.settings.SyncPreferences
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) : SettingsRepository {
    private val dataStore = context.settingsDataStore

    override val preferences: Flow<SyncPreferences> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { values ->
            val interval = values[INTERVAL_HOURS]
                ?.takeIf { it in SUPPORTED_SYNC_INTERVALS }
                ?: DEFAULT_SYNC_INTERVAL_HOURS
            val enabledSources = values[ENABLED_SOURCES]
                ?.mapNotNull { stored ->
                    ContestSource.entries.firstOrNull { source -> source.name == stored }
                }
                ?.toSet()
                ?: DEFAULT_ENABLED_SOURCES
            val defaultReminderOffsets = values[DEFAULT_REMINDER_OFFSETS]
                ?.mapNotNull(String::toIntOrNull)
                ?.filter { it in MIN_REMINDER_OFFSET_MINUTES..MAX_REMINDER_OFFSET_MINUTES }
                ?.toSet()
                ?: values[DEFAULT_REMINDER_OFFSET]
                    ?.takeIf { it in MIN_REMINDER_OFFSET_MINUTES..MAX_REMINDER_OFFSET_MINUTES }
                    ?.let(::setOf)
                ?: DEFAULT_REMINDER_OFFSETS_MINUTES
            SyncPreferences(
                backgroundSyncEnabled = values[BACKGROUND_ENABLED] ?: true,
                wifiOnly = values[WIFI_ONLY] ?: false,
                intervalHours = interval,
                enabledSources = enabledSources,
                defaultReminderOffsetsMinutes = defaultReminderOffsets,
            )
        }

    override suspend fun setBackgroundSyncEnabled(enabled: Boolean) {
        dataStore.edit { values -> values[BACKGROUND_ENABLED] = enabled }
    }

    override suspend fun setWifiOnly(enabled: Boolean) {
        dataStore.edit { values -> values[WIFI_ONLY] = enabled }
    }

    override suspend fun setIntervalHours(hours: Int) {
        require(hours in SUPPORTED_SYNC_INTERVALS) { "Unsupported sync interval: $hours" }
        dataStore.edit { values -> values[INTERVAL_HOURS] = hours }
    }

    override suspend fun setDefaultReminderOffsetsMinutes(offsetsMinutes: Set<Int>) {
        require(offsetsMinutes.all { it in MIN_REMINDER_OFFSET_MINUTES..MAX_REMINDER_OFFSET_MINUTES }) {
            "Unsupported default reminder offsets: $offsetsMinutes"
        }
        dataStore.edit { values ->
            values[DEFAULT_REMINDER_OFFSETS] = offsetsMinutes.map(Int::toString).toSet()
            values.remove(DEFAULT_REMINDER_OFFSET)
        }
    }

    override suspend fun setSourceEnabled(
        source: ContestSource,
        enabled: Boolean,
    ) {
        dataStore.edit { values ->
            val current = values[ENABLED_SOURCES]?.toMutableSet()
                ?: DEFAULT_ENABLED_SOURCES.mapTo(mutableSetOf(), ContestSource::name)
            if (enabled) current += source.name else current -= source.name
            values[ENABLED_SOURCES] = current
        }
    }
}

private val BACKGROUND_ENABLED = booleanPreferencesKey("background_sync_enabled")
private val WIFI_ONLY = booleanPreferencesKey("wifi_only")
private val INTERVAL_HOURS = intPreferencesKey("sync_interval_hours")
private val DEFAULT_REMINDER_OFFSET = intPreferencesKey("default_reminder_offset_minutes")
private val DEFAULT_REMINDER_OFFSETS = stringSetPreferencesKey("default_reminder_offsets_minutes")
private val ENABLED_SOURCES = stringSetPreferencesKey("enabled_sources")
