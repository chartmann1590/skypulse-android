package com.charles.skypulse.app.ui.screens.airports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charles.skypulse.app.data.location.LocationProvider
import com.charles.skypulse.app.data.repository.AircraftRepository
import com.charles.skypulse.app.data.repository.AirportRepository
import com.charles.skypulse.app.data.repository.SavedRepository
import com.charles.skypulse.app.data.settings.SettingsDataStore
import com.charles.skypulse.app.data.settings.SkySettings
import com.charles.skypulse.app.domain.model.Aircraft
import com.charles.skypulse.app.domain.model.Airport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
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

@HiltViewModel
class AirportViewModel @Inject constructor(
    private val airportRepository: AirportRepository,
    private val aircraftRepository: AircraftRepository,
    private val savedRepository: SavedRepository,
    private val locationProvider: LocationProvider,
    settingsDataStore: SettingsDataStore,
) : ViewModel() {

    private val _state = MutableStateFlow(AirportsUiState())
    val state: StateFlow<AirportsUiState> = _state.asStateFlow()

    private val _focusAirport = MutableStateFlow<Airport?>(null)
    val focusAirport: StateFlow<Airport?> = _focusAirport.asStateFlow()

    val aircraftAtFocus: StateFlow<List<Aircraft>> = aircraftRepository.feed
        .map { it.aircraft }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun viewAircraftNear(airport: Airport) {
        _focusAirport.value = airport
        viewModelScope.launch {
            aircraftRepository.refresh(airport.latitude, airport.longitude, 60, force = true)
        }
    }

    fun clearFocus() { _focusAirport.value = null }

    fun saveAirport(airport: Airport) {
        viewModelScope.launch { savedRepository.saveAirport(airport) }
    }

    /** Retry the import-dependent load (e.g. if the DB was still importing on first open). */
    fun retryLoad() = loadNearbyAndFeatured()
}
