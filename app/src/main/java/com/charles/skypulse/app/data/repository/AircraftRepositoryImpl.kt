package com.charles.skypulse.app.data.repository

import com.charles.skypulse.app.data.local.dao.CacheDao
import com.charles.skypulse.app.data.local.entity.CachedAircraftEntity
import com.charles.skypulse.app.data.remote.RemoteAircraftDataSource
import com.charles.skypulse.app.data.settings.SettingsDataStore
import com.charles.skypulse.app.domain.model.Aircraft
import com.charles.skypulse.app.domain.model.DataSource
import com.charles.skypulse.app.domain.util.FetchResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ADSB.lol (primary) -> OpenSky (fallback) -> cache, with throttling so the free APIs are
 * never spammed. The current snapshot is exposed via [feed] as a StateFlow.
 */
@Singleton
class AircraftRepositoryImpl @Inject constructor(
    private val remote: RemoteAircraftDataSource,
    private val cacheDao: CacheDao,
    private val settings: SettingsDataStore,
) : AircraftRepository {

    private val _feed = MutableStateFlow(AircraftFeedState())
    override val feed: StateFlow<AircraftFeedState> = _feed.asStateFlow()

    private val refreshMutex = Mutex()
    @Volatile private var lastNetworkFetchMs: Long = 0L

    override suspend fun refresh(lat: Double, lon: Double, radiusNm: Int, force: Boolean) {
        refreshMutex.withLock {
            val now = System.currentTimeMillis()
            val intervalMs = throttleWindowMs()
            val withinWindow = now - lastNetworkFetchMs < intervalMs
            if (!force && withinWindow && _feed.value.aircraft.isNotEmpty()) {
                return // serve existing snapshot; do not spam the network
            }

            _feed.value = _feed.value.copy(isLoading = true, errorMessage = null)

            // 1) Primary: ADSB.lol
            when (val primary = remote.fetchFromAdsbLol(lat, lon, radiusNm)) {
                is FetchResult.Success -> {
                    publishAndCache(primary.aircraft, DataSource.ADSB_LOL, now); return
                }
                is FetchResult.Empty -> {
                    // Reachable but empty — still try fallback in case it has coverage.
                }
                is FetchResult.Error -> { /* fall through to fallback */ }
            }

            // 2) Fallback: OpenSky
            when (val fallback = remote.fetchFromOpenSky(lat, lon, radiusNm)) {
                is FetchResult.Success -> {
                    publishAndCache(fallback.aircraft, DataSource.OPENSKY, now); return
                }
                is FetchResult.Empty -> {
                    lastNetworkFetchMs = now
                    _feed.value = _feed.value.copy(
                        aircraft = emptyList(),
                        activeSource = DataSource.ADSB_LOL,
                        lastUpdatedEpochMs = now,
                        isLoading = false,
                        isOffline = false,
                        errorMessage = null,
                    )
                    return
                }
                is FetchResult.Error -> {
                    serveCache(fallback.message); return
                }
            }
        }
    }

    private suspend fun publishAndCache(list: List<Aircraft>, source: DataSource, now: Long) {
        lastNetworkFetchMs = now
        _feed.value = AircraftFeedState(
            aircraft = list,
            activeSource = source,
            lastUpdatedEpochMs = now,
            isLoading = false,
            isOffline = false,
            errorMessage = null,
        )
        runCatching { cacheDao.replaceAll(list.map { it.toCacheEntity(now) }) }
    }

    private suspend fun serveCache(error: String) {
        val cached = runCatching { cacheDao.getAll() }.getOrDefault(emptyList())
        val lastAt = runCatching { cacheDao.lastCachedAt() }.getOrNull()
        _feed.value = AircraftFeedState(
            aircraft = cached.map { it.toAircraft() },
            activeSource = if (cached.isNotEmpty()) DataSource.CACHE else _feed.value.activeSource,
            lastUpdatedEpochMs = lastAt ?: _feed.value.lastUpdatedEpochMs,
            isLoading = false,
            isOffline = true,
            errorMessage = error,
        )
    }

    override fun aircraftById(id: String): Aircraft? =
        _feed.value.aircraft.firstOrNull { it.id == id || it.hex == id || it.callsign?.trim() == id }

    private suspend fun throttleWindowMs(): Long {
        val intervalSec = runCatching { settings.settings.first().refreshIntervalSeconds }
            .getOrDefault(10)
        // Hard floor of 8s so even an aggressive setting can't spam the free APIs.
        return (intervalSec.coerceAtLeast(8)) * 1000L
    }
}

private fun Aircraft.toCacheEntity(now: Long) = CachedAircraftEntity(
    id = id,
    callsign = callsign,
    hex = hex,
    lat = latitude,
    lon = longitude,
    altitudeFeet = altitudeFeet,
    speedKnots = speedKnots,
    headingDegrees = headingDegrees,
    verticalRate = verticalRate,
    originCountry = originCountry,
    lastSeenEpochSeconds = lastSeenEpochSeconds,
    typeCode = typeCode,
    onGround = onGround,
    cachedAtEpochMs = now,
)

private fun CachedAircraftEntity.toAircraft() = Aircraft(
    id = id,
    callsign = callsign,
    hex = hex,
    latitude = lat,
    longitude = lon,
    altitudeFeet = altitudeFeet,
    speedKnots = speedKnots,
    headingDegrees = headingDegrees,
    verticalRate = verticalRate,
    originCountry = originCountry,
    lastSeenEpochSeconds = lastSeenEpochSeconds,
    source = DataSource.CACHE,
    typeCode = typeCode,
    onGround = onGround,
)
