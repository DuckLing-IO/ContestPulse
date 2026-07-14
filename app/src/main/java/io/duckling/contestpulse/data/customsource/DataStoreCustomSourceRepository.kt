package io.duckling.contestpulse.data.customsource

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import io.duckling.contestpulse.domain.customsource.CustomContestSource
import io.duckling.contestpulse.domain.customsource.CustomSourceRepository
import io.duckling.contestpulse.domain.customsource.MAX_CUSTOM_SOURCES
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.customSourcesDataStore by preferencesDataStore(name = "custom_sources")

@Singleton
class DataStoreCustomSourceRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val json: Json,
) : CustomSourceRepository {
    private val dataStore = context.customSourcesDataStore

    override val sources: Flow<List<CustomContestSource>> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map(::decodeSources)

    override suspend fun save(source: CustomContestSource) {
        source.requireValid()
        dataStore.edit { values ->
            val current = decodeSources(values).toMutableList()
            val existingIndex = current.indexOfFirst { stored -> stored.id == source.id }
            require(current.none { stored ->
                stored.id != source.id && stored.name.equals(source.name, ignoreCase = true)
            }) {
                "A custom source with the same name already exists"
            }
            if (existingIndex >= 0) {
                current[existingIndex] = source
            } else {
                require(current.size < MAX_CUSTOM_SOURCES) {
                    "At most $MAX_CUSTOM_SOURCES custom sources can be saved"
                }
                current += source
            }
            values[SOURCES_JSON] = encodeSources(current)
        }
    }

    override suspend fun setEnabled(id: String, enabled: Boolean) {
        dataStore.edit { values ->
            val current = decodeSources(values)
            require(current.any { source -> source.id == id }) { "Custom source was not found" }
            values[SOURCES_JSON] = encodeSources(
                current.map { source ->
                    if (source.id == id) source.copy(enabled = enabled) else source
                },
            )
        }
    }

    override suspend fun delete(id: String) {
        dataStore.edit { values ->
            values[SOURCES_JSON] = encodeSources(
                decodeSources(values).filterNot { source -> source.id == id },
            )
        }
    }

    private fun decodeSources(values: Preferences): List<CustomContestSource> {
        val encoded = values[SOURCES_JSON] ?: return emptyList()
        return runCatching {
            json.decodeFromString(SOURCE_LIST_SERIALIZER, encoded)
        }.getOrDefault(emptyList())
    }

    private fun encodeSources(sources: List<CustomContestSource>): String =
        json.encodeToString(SOURCE_LIST_SERIALIZER, sources.sortedBy { it.name.lowercase() })
}

private val SOURCES_JSON = stringPreferencesKey("sources_json")
private val SOURCE_LIST_SERIALIZER = ListSerializer(CustomContestSource.serializer())
