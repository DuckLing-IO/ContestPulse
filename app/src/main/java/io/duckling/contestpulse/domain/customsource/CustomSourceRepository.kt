package io.duckling.contestpulse.domain.customsource

import kotlinx.coroutines.flow.Flow

interface CustomSourceRepository {
    val sources: Flow<List<CustomContestSource>>

    suspend fun save(source: CustomContestSource)

    suspend fun setEnabled(id: String, enabled: Boolean)

    suspend fun delete(id: String)
}
