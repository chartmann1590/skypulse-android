package com.charles.skypulse.app.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charles.skypulse.app.data.firebase.Analytics
import com.charles.skypulse.app.data.firebase.RemoteConfigProvider
import com.charles.skypulse.app.data.location.LocationProvider
import com.charles.skypulse.app.data.repository.AircraftFeedState
import com.charles.skypulse.app.data.repository.AircraftRepository
import com.charles.skypulse.app.data.repository.RouteRepository
import com.charles.skypulse.app.data.repository.SavedRepository
import com.charles.skypulse.app.data.repository.ShareRepository
import com.charles.skypulse.app.data.settings.SettingsDataStore
import com.charles.skypulse.app.data.settings.SkySettings
import com.charles.skypulse.app.domain.model.Aircraft
import com.charles.skypulse.app.domain.model.FlightRoute
import com.charles.skypulse.app.domain.model.RouteProgress
import com.charles.skypulse.app.domain.model.SharedFlight
import com.charles.skypulse.app.domain.util.RouteEstimator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserLocation(val lat: Double, val lon: Double, val isReal: Boolean)

/** One-shot results from sharing a flight, surfaced to the screen to launch the share sheet. */
sealed interface ShareEvent {
    data class Ready(val url: String) : ShareEvent
    data class Failed(val message: String) : ShareEvent
}

@HiltViewModel
class HomeMapViewModel @Inject constructor(
    private val aircraftRepository: AircraftRepository,
    private val locationProvider: LocationProvider,
    private val savedRepository: SavedRepository,
    private val routeRepository: RouteRepository,
    private val shareRepository: ShareRepository,
    private val analytics: Analytics,
    private val remoteConfig: RemoteConfigProvider,
    settingsDataStore: SettingsDataStore,
) : ViewModel() {

    val feed: StateFlow<AircraftFeedState> = aircraftRepository.feed

    val settings: StateFlow<SkySettings> = settingsDataStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SkySettings())

    private val _userLocation = MutableStateFlow<UserLocation?>(null)
    val userLocation: StateFlow<UserLocation?> = _userLocation.asStateFlow()

    /** Incremented each time the user taps the location button, to drive a map re-center. */
    private val _recenterTrigger = MutableStateFlow(0)
    val recenterTrigger: StateFlow<Int> = _recenterTrigger.asStateFlow()

    private val _selected = MutableStateFlow<Aircraft?>(null)
    val selected: StateFlow<Aircraft?> = _selected.asStateFlow()

    private val _selectedRoute = MutableStateFlow<FlightRoute?>(null)
    val selectedRoute: StateFlow<FlightRoute?> = _selectedRoute.asStateFlow()

    private val _selectedProgress = MutableStateFlow<RouteProgress?>(null)
    val selectedProgress: StateFlow<RouteProgress?> = _selectedProgress.asStateFlow()

    /** A flight opened from a share link — drawn + centered on the map even if it's not in the feed. */
    private val _focusShared = MutableStateFlow<Aircraft?>(null)
    val focusShared: StateFlow<Aircraft?> = _focusShared.asStateFlow()

    private val _isSharing = MutableStateFlow(false)
    val isSharing: StateFlow<Boolean> = _isSharing.asStateFlow()

    private val _shareEvents = MutableSharedFlow<ShareEvent>(extraBufferCapacity = 1)
    val shareEvents: SharedFlow<ShareEvent> = _shareEvents.asSharedFlow()

    private val radiusNm: Int get() = remoteConfig.defaultRadiusNm.coerceIn(10, 250)

    init {
        startRefreshLoop()
        syncSelectionWithFeed()
    }

    /** Keep the open detail sheet's aircraft fresh (altitude/speed/last-seen) as the feed refreshes. */
    private fun syncSelectionWithFeed() {
        viewModelScope.launch {
            aircraftRepository.feed.collect { feed ->
                val current = _selected.value ?: return@collect
                val fresh = feed.aircraft.firstOrNull {
                    it.id == current.id || (current.hex != null && it.hex == current.hex)
                } ?: return@collect
                _selected.value = fresh
                _selectedRoute.value?.let { r ->
                    _selectedProgress.value = RouteEstimator.estimate(r, fresh.latitude, fresh.longitude, fresh.speedKnots)
                }
            }
        }
    }

    private fun startRefreshLoop() {
        viewModelScope.launch {
            while (isActive) {
                val loc = resolveLocation()
                aircraftRepository.refresh(loc.lat, loc.lon, radiusNm)
                val intervalMs = settings.value.refreshIntervalSeconds.coerceAtLeast(5) * 1000L
                delay(intervalMs)
            }
        }
    }

    private suspend fun resolveLocation(): UserLocation {
        val fix = locationProvider.currentLocation()
        val resolved = if (fix != null) {
            UserLocation(fix.latitude, fix.longitude, true)
        } else {
            UserLocation(LocationProvider.FALLBACK_LAT, LocationProvider.FALLBACK_LON, false)
        }
        _userLocation.value = resolved
        return resolved
    }

    fun recenter() {
        viewModelScope.launch {
            val loc = resolveLocation()
            _recenterTrigger.value += 1 // tell the map to animate to the fresh location
            aircraftRepository.refresh(loc.lat, loc.lon, radiusNm, force = true)
        }
    }

    fun select(aircraft: Aircraft) {
        _selected.value = aircraft
        _selectedRoute.value = null
        _selectedProgress.value = null
        analytics.logAircraftTapped(aircraft.source.name)
        viewModelScope.launch {
            val route = routeRepository.routeForAircraft(aircraft)
            if (route != null && _selected.value?.id == aircraft.id) {
                _selectedRoute.value = route
                _selectedProgress.value = RouteEstimator.estimate(
                    route, aircraft.latitude, aircraft.longitude, aircraft.speedKnots,
                )
            }
        }
    }

    fun clearSelection() {
        _selected.value = null
        _selectedRoute.value = null
        _selectedProgress.value = null
    }

    fun toggleSaveSelected() {
        val ac = _selected.value ?: return
        viewModelScope.launch {
            val saved = savedRepository.isAircraftSaved(ac.id).first()
            savedRepository.toggleAircraft(ac, saved)
        }
    }

    /** Publish the selected flight to Firestore and emit a shareable link for the OS share sheet. */
    fun shareSelected() {
        val ac = _selected.value ?: return
        if (_isSharing.value) return
        if (ac.latitude == null || ac.longitude == null) {
            _shareEvents.tryEmit(ShareEvent.Failed("This flight has no position to share yet."))
            return
        }
        _isSharing.value = true
        viewModelScope.launch {
            try {
                val url = shareRepository.shareFlight(
                    SharedFlight(ac, _selectedRoute.value, _selectedProgress.value),
                )
                analytics.logFlightShared(ac.source.name)
                _shareEvents.tryEmit(ShareEvent.Ready(url))
            } catch (e: Exception) {
                _shareEvents.tryEmit(ShareEvent.Failed("Couldn't create a share link. Check your connection and try again."))
            } finally {
                _isSharing.value = false
            }
        }
    }

    /** Open a flight that someone shared (from a deep link): show its details and center the map. */
    fun openSharedFlight(shareId: String) {
        viewModelScope.launch {
            val shared = try {
                shareRepository.loadSharedFlight(shareId)
            } catch (e: Exception) {
                null
            } ?: return@launch
            analytics.logSharedFlightOpened()
            _selected.value = shared.aircraft
            _selectedRoute.value = shared.route
            _selectedProgress.value = shared.progress
            _focusShared.value = shared.aircraft
        }
    }
}
