package com.charles.skypulse.app.ui.screens.nearby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charles.skypulse.app.data.firebase.Analytics
import com.charles.skypulse.app.data.location.LocationProvider
import com.charles.skypulse.app.data.repository.AircraftRepository
import com.charles.skypulse.app.data.repository.RouteRepository
import com.charles.skypulse.app.data.repository.SavedRepository
import com.charles.skypulse.app.data.repository.ShareRepository
import com.charles.skypulse.app.data.settings.SettingsDataStore
import com.charles.skypulse.app.data.settings.SkySettings
import com.charles.skypulse.app.domain.model.Aircraft
import com.charles.skypulse.app.domain.model.AircraftSort
import com.charles.skypulse.app.domain.model.FlightRoute
import com.charles.skypulse.app.domain.model.RouteProgress
import com.charles.skypulse.app.domain.model.SharedFlight
import com.charles.skypulse.app.domain.util.GeoUtils
import com.charles.skypulse.app.domain.util.RouteEstimator
import com.charles.skypulse.app.ui.screens.map.ShareEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NearbyUiState(
    val aircraft: List<Aircraft> = emptyList(),
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class NearbyViewModel @Inject constructor(
    private val aircraftRepository: AircraftRepository,
    private val locationProvider: LocationProvider,
    private val savedRepository: SavedRepository,
    private val routeRepository: RouteRepository,
    private val shareRepository: ShareRepository,
    private val analytics: Analytics,
    settingsDataStore: SettingsDataStore,
) : ViewModel() {

    private val _isSharing = MutableStateFlow(false)
    val isSharing: StateFlow<Boolean> = _isSharing.asStateFlow()

    private val _shareEvents = MutableSharedFlow<ShareEvent>(extraBufferCapacity = 1)
    val shareEvents: SharedFlow<ShareEvent> = _shareEvents.asSharedFlow()

    private val _sort = MutableStateFlow(AircraftSort.CLOSEST)
    val sort: StateFlow<AircraftSort> = _sort.asStateFlow()

    private val _selected = MutableStateFlow<Aircraft?>(null)
    val selected: StateFlow<Aircraft?> = _selected.asStateFlow()

    private val _selectedRoute = MutableStateFlow<FlightRoute?>(null)
    val selectedRoute: StateFlow<FlightRoute?> = _selectedRoute.asStateFlow()

    private val _selectedProgress = MutableStateFlow<RouteProgress?>(null)
    val selectedProgress: StateFlow<RouteProgress?> = _selectedProgress.asStateFlow()

    val settings: StateFlow<SkySettings> = settingsDataStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SkySettings())

    private var userLat: Double? = null
    private var userLon: Double? = null

    val uiState: StateFlow<NearbyUiState> =
        combine(aircraftRepository.feed, _sort) { feed, sort ->
            val withDistance = feed.aircraft.map { ac ->
                if (ac.distanceNm == null && userLat != null && userLon != null &&
                    ac.latitude != null && ac.longitude != null
                ) {
                    ac.copy(distanceNm = GeoUtils.haversineNm(userLat!!, userLon!!, ac.latitude, ac.longitude))
                } else {
                    ac
                }
            }
            NearbyUiState(
                aircraft = withDistance.sortedWith(comparatorFor(sort)),
                isLoading = feed.isLoading,
                isOffline = feed.isOffline,
                errorMessage = feed.errorMessage,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NearbyUiState(isLoading = true))

    init {
        viewModelScope.launch {
            while (isActive) {
                val fix = locationProvider.currentLocation()
                userLat = fix?.latitude ?: LocationProvider.FALLBACK_LAT
                userLon = fix?.longitude ?: LocationProvider.FALLBACK_LON
                aircraftRepository.refresh(userLat!!, userLon!!, 100)
                val intervalMs = settings.value.refreshIntervalSeconds.coerceAtLeast(5) * 1000L
                delay(intervalMs)
            }
        }
        // Keep the open detail sheet fresh (altitude/speed/last-seen) as the feed refreshes.
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

    private fun comparatorFor(sort: AircraftSort): Comparator<Aircraft> = when (sort) {
        AircraftSort.CLOSEST -> compareBy { it.distanceNm ?: Double.MAX_VALUE }
        AircraftSort.HIGHEST -> compareByDescending { it.altitudeFeet ?: -1.0 }
        AircraftSort.FASTEST -> compareByDescending { it.speedKnots ?: -1.0 }
        AircraftSort.RECENT -> compareByDescending { it.lastSeenEpochSeconds ?: 0L }
    }

    fun setSort(sort: AircraftSort) { _sort.value = sort }

    fun select(aircraft: Aircraft) {
        _selected.value = aircraft
        _selectedRoute.value = null
        _selectedProgress.value = null
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
}
