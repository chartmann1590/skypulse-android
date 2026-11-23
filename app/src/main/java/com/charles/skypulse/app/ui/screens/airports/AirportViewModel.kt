package com.charles.skypulse.app.ui.screens.airports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charles.skypulse.app.data.firebase.Analytics
import com.charles.skypulse.app.data.location.LocationProvider
import com.charles.skypulse.app.data.remote.RemoteAircraftDataSource
import com.charles.skypulse.app.data.repository.AirportRepository
import com.charles.skypulse.app.data.repository.RouteRepository
import com.charles.skypulse.app.data.repository.SavedRepository
import com.charles.skypulse.app.data.repository.ShareRepository
import com.charles.skypulse.app.data.settings.SettingsDataStore
import com.charles.skypulse.app.data.settings.SkySettings
import com.charles.skypulse.app.domain.model.Aircraft
import com.charles.skypulse.app.domain.model.Airport
import com.charles.skypulse.app.domain.model.DataSource
import com.charles.skypulse.app.domain.model.FlightRoute
import com.charles.skypulse.app.domain.model.RouteProgress
import com.charles.skypulse.app.domain.model.SharedFlight
import com.charles.skypulse.app.domain.util.FetchResult
import com.charles.skypulse.app.domain.util.RouteEstimator
import com.charles.skypulse.app.ui.screens.map.ShareEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AirportsUiState(
    val query: String = "",
    val results: List<Airport> = emptyList(),
    val nearby: List<Airport> = emptyList(),
    val featured: List<Airport> = emptyList(),
    val isImporting: Boolean = true,
)

/** ARRIVING/DEPARTING are routed to/from the airport; NEARBY is in the area (route unknown). */
enum class FlightRole { ARRIVING, DEPARTING, NEARBY }

/** A flight shown for a focused airport. [route] is non-null only when routed to/from it. */
data class AirportFlight(
    val aircraft: Aircraft,
    val role: FlightRole,
    val route: FlightRoute?,
)

@HiltViewModel
class AirportViewModel @Inject constructor(
    private val airportRepository: AirportRepository,
    private val remoteDataSource: RemoteAircraftDataSource,
    private val savedRepository: SavedRepository,
    private val routeRepository: RouteRepository,
    private val locationProvider: LocationProvider,
    private val shareRepository: ShareRepository,
    private val analytics: Analytics,
    settingsDataStore: SettingsDataStore,
) : ViewModel() {

    private val _state = MutableStateFlow(AirportsUiState())
    val state: StateFlow<AirportsUiState> = _state.asStateFlow()

    private val _isSharing = MutableStateFlow(false)
    val isSharing: StateFlow<Boolean> = _isSharing.asStateFlow()

    private val _shareEvents = MutableSharedFlow<ShareEvent>(extraBufferCapacity = 1)
    val shareEvents: SharedFlow<ShareEvent> = _shareEvents.asSharedFlow()

    private val _focusAirport = MutableStateFlow<Airport?>(null)
    val focusAirport: StateFlow<Airport?> = _focusAirport.asStateFlow()

    private val _focusFlights = MutableStateFlow<List<AirportFlight>>(emptyList())
    val focusFlights: StateFlow<List<AirportFlight>> = _focusFlights.asStateFlow()

    private val _focusLoading = MutableStateFlow(false)
    val focusLoading: StateFlow<Boolean> = _focusLoading.asStateFlow()

    private val _selected = MutableStateFlow<Aircraft?>(null)
    val selected: StateFlow<Aircraft?> = _selected.asStateFlow()

    private val _selectedRoute = MutableStateFlow<FlightRoute?>(null)
    val selectedRoute: StateFlow<FlightRoute?> = _selectedRoute.asStateFlow()

    private val _selectedProgress = MutableStateFlow<RouteProgress?>(null)
    val selectedProgress: StateFlow<RouteProgress?> = _selectedProgress.asStateFlow()

    val settings: StateFlow<SkySettings> = settingsDataStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SkySettings())

    private val featuredCodes = listOf("LHR", "DXB", "HND", "JFK", "LAX", "SIN")

    init {
        loadNearbyAndFeatured()
    }

    private fun loadNearbyAndFeatured() {
        viewModelScope.launch {
            // Featured.
            val featured = featuredCodes.mapNotNull { airportRepository.byCode(it) }
            // Nearby.
            val fix = locationProvider.currentLocation()
            val lat = fix?.latitude ?: LocationProvider.FALLBACK_LAT
            val lon = fix?.longitude ?: LocationProvider.FALLBACK_LON
            val nearby = airportRepository.airportsNearMe(lat, lon)
            _state.value = _state.value.copy(
                nearby = nearby,
                featured = featured,
                isImporting = featured.isEmpty() && nearby.isEmpty(),
            )
        }
    }

    fun onQueryChange(q: String) {
        _state.value = _state.value.copy(query = q)
        viewModelScope.launch {
            _state.value = _state.value.copy(results = airportRepository.search(q))
        }
    }

    /**
     * Show flights near this airport from ADS-B (ADSB.lol). Route labels are resolved
     * per flight via adsbdb when one is selected; the board itself lists nearby aircraft.
     */
    fun viewAircraftNear(airport: Airport) {
        _focusAirport.value = airport
        _focusFlights.value = emptyList()
        _focusLoading.value = true
        viewModelScope.launch {
            val flights = runCatching { loadAirportFlights(airport) }.getOrDefault(emptyList())
            if (_focusAirport.value?.id == airport.id) {
                _focusFlights.value = flights
                _focusLoading.value = false
            }
        }
    }

    private suspend fun loadAirportFlights(airport: Airport): List<AirportFlight> =
        fallbackNearby(airport)

    /** Nearest aircraft from ADS-B around the airport, no route labels. */
    private suspend fun fallbackNearby(airport: Airport): List<AirportFlight> {
        val result = remoteDataSource.fetchFromAdsbLol(airport.latitude, airport.longitude, AIRPORT_RADIUS_NM)
        return (result as? FetchResult.Success)?.aircraft.orEmpty()
            .sortedBy { it.distanceNm ?: Double.MAX_VALUE }
            .take(MAX_DISPLAY)
            .map { AirportFlight(it, FlightRole.NEARBY, null) }
    }

    fun clearFocus() {
        _focusAirport.value = null
        _focusFlights.value = emptyList()
        _focusLoading.value = false
    }

    fun select(aircraft: Aircraft) {
        _selected.value = aircraft
        _selectedRoute.value = null
        _selectedProgress.value = null
        viewModelScope.launch {
            // Prefer the route already resolved for this flight on the airport board
            // (re-fetching can come back empty once a flight lands).
            val known = _focusFlights.value.firstOrNull { it.aircraft.id == aircraft.id }?.route
            val route = known ?: routeRepository.routeForAircraft(aircraft)
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

    fun saveAirport(airport: Airport) {
        viewModelScope.launch { savedRepository.saveAirport(airport) }
    }

    /** Retry the import-dependent load (e.g. if the DB was still importing on first open). */
    fun retryLoad() = loadNearbyAndFeatured()

    companion object {
        /** Radius searched around an airport for inbound/outbound flights (NM). */
        private const val AIRPORT_RADIUS_NM = 250
        /** Max flights shown in the sheet. */
        private const val MAX_DISPLAY = 200
        /** Always show at least this many nearby flights as context. */
        private const val MIN_NEARBY = 15
    }
}
