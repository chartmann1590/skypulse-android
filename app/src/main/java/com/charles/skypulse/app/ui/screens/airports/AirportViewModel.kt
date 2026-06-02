package com.charles.skypulse.app.ui.screens.airports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charles.skypulse.app.data.location.LocationProvider
import com.charles.skypulse.app.data.remote.RemoteAircraftDataSource
import com.charles.skypulse.app.data.repository.AirportRepository
import com.charles.skypulse.app.data.repository.RouteRepository
import com.charles.skypulse.app.data.repository.SavedRepository
import com.charles.skypulse.app.data.settings.SettingsDataStore
import com.charles.skypulse.app.data.settings.SkySettings
import com.charles.skypulse.app.domain.model.Aircraft
import com.charles.skypulse.app.domain.model.Airport
import com.charles.skypulse.app.domain.model.FlightRoute
import com.charles.skypulse.app.domain.model.RouteProgress
import com.charles.skypulse.app.domain.util.FetchResult
import com.charles.skypulse.app.domain.util.RouteEstimator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    settingsDataStore: SettingsDataStore,
) : ViewModel() {

    private val _state = MutableStateFlow(AirportsUiState())
    val state: StateFlow<AirportsUiState> = _state.asStateFlow()

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
     * Show only flights arriving at / departing from this airport. We pull aircraft within a
     * generous radius (to catch flights still inbound/outbound), look up each one's route via
     * adsbdb (in parallel, capped), and keep those whose origin or destination is this airport.
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

    private suspend fun loadAirportFlights(airport: Airport): List<AirportFlight> {
        val result = remoteDataSource.fetchFromAdsbLol(airport.latitude, airport.longitude, AIRPORT_RADIUS_NM)
        val all = (result as? FetchResult.Success)?.aircraft.orEmpty()
            .sortedBy { it.distanceNm ?: Double.MAX_VALUE }

        // Look up routes for the nearest flights that have a callsign (bounded).
        val lookupTargets = all.filter { !it.callsign.isNullOrBlank() }.take(MAX_ROUTE_LOOKUPS)
        val routeByCallsign: Map<String, FlightRoute> = coroutineScope {
            lookupTargets.map { ac -> async { ac.callsign!!.trim() to routeRepository.routeForCallsign(ac.callsign) } }
                .awaitAll()
                .mapNotNull { (cs, route) -> route?.let { cs.uppercase() to it } }
                .toMap()
        }

        // Classify every nearby flight: arriving/departing if its route matches, else nearby.
        return all.take(MAX_DISPLAY).map { ac ->
            val route = ac.callsign?.trim()?.uppercase()?.let { routeByCallsign[it] }
            val role = when {
                route != null && airport.matches(route.destination.iata, route.destination.icao) -> FlightRole.ARRIVING
                route != null && airport.matches(route.origin.iata, route.origin.icao) -> FlightRole.DEPARTING
                else -> FlightRole.NEARBY
            }
            AirportFlight(ac, role, if (role == FlightRole.NEARBY) null else route)
        }.sortedWith(
            // Routed to/from first (ARRIVING, DEPARTING), then nearby; each by distance.
            compareBy({ it.role.ordinal }, { it.aircraft.distanceNm ?: Double.MAX_VALUE }),
        )
    }

    private fun Airport.matches(iataCode: String?, icaoCode: String?): Boolean {
        val a = iata?.trim()?.uppercase()?.ifEmpty { null }
        val i = icao?.trim()?.uppercase()?.ifEmpty { null }
        val ti = iataCode?.trim()?.uppercase()?.ifEmpty { null }
        val tc = icaoCode?.trim()?.uppercase()?.ifEmpty { null }
        return (a != null && a == ti) || (i != null && i == tc)
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
            val route = routeRepository.routeForCallsign(aircraft.callsign)
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

    fun saveAirport(airport: Airport) {
        viewModelScope.launch { savedRepository.saveAirport(airport) }
    }

    /** Retry the import-dependent load (e.g. if the DB was still importing on first open). */
    fun retryLoad() = loadNearbyAndFeatured()

    companion object {
        /** Radius searched around an airport for inbound/outbound flights (NM). Wider so
         *  arrivals still descending and departures climbing out are caught. */
        private const val AIRPORT_RADIUS_NM = 175
        /** Cap on parallel route lookups per airport to bound network calls. */
        private const val MAX_ROUTE_LOOKUPS = 60
        /** Max flights shown in the sheet. */
        private const val MAX_DISPLAY = 60
    }
}
