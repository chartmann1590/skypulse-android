package com.charles.skypulse.app.data.repository

import com.charles.skypulse.app.domain.model.Aircraft
import com.charles.skypulse.app.domain.model.DataSource
import kotlinx.coroutines.flow.StateFlow

/** Snapshot of the live aircraft feed exposed to the UI. */
data class AircraftFeedState(
    val aircraft: List<Aircraft> = emptyList(),
    val activeSource: DataSource? = null,
    val lastUpdatedEpochMs: Long? = null,
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val errorMessage: String? = null,
)

interface AircraftRepository {
    val feed: StateFlow<AircraftFeedState>

    /**
     * Refresh aircraft around a point. Honors a throttle window unless [force] is true,
     * so repeated calls within the window return the cached snapshot instead of hitting
     * the network (Plan §3). Never throws — failures surface via [AircraftFeedState].
     */
    suspend fun refresh(lat: Double, lon: Double, radiusNm: Int, force: Boolean = false)

    fun aircraftById(id: String): Aircraft?
}
