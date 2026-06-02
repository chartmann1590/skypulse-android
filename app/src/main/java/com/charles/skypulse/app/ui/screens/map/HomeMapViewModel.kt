package com.charles.skypulse.app.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charles.skypulse.app.data.firebase.Analytics
import com.charles.skypulse.app.data.firebase.RemoteConfigProvider
import com.charles.skypulse.app.data.location.LocationProvider
import com.charles.skypulse.app.data.repository.AircraftFeedState
import com.charles.skypulse.app.data.repository.AircraftRepository
import com.charles.skypulse.app.data.repository.SavedRepository
import com.charles.skypulse.app.data.settings.SettingsDataStore
import com.charles.skypulse.app.data.settings.SkySettings
import com.charles.skypulse.app.domain.model.Aircraft
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserLocation(val lat: Double, val lon: Double, val isReal: Boolean)

@HiltViewModel
class HomeMapViewModel @Inject constructor(
    private val aircraftRepository: AircraftRepository,
    private val locationProvider: LocationProvider,
    private val savedRepository: SavedRepository,
    private val analytics: Analytics,
    private val remoteConfig: RemoteConfigProvider,
    settingsDataStore: SettingsDataStore,
) : ViewModel() {

    val feed: StateFlow<AircraftFeedState> = aircraftRepository.feed

    val settings: StateFlow<SkySettings> = settingsDataStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SkySettings())

    private val _userLocation = MutableStateFlow<UserLocation?>(null)
    val userLocation: StateFlow<UserLocation?> = _userLocation.asStateFlow()

    private val _selected = MutableStateFlow<Aircraft?>(null)
    val selected: StateFlow<Aircraft?> = _selected.asStateFlow()

    private val radiusNm: Int get() = remoteConfig.defaultRadiusNm.coerceIn(10, 250)

    init {
        startRefreshLoop()
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
            aircraftRepository.refresh(loc.lat, loc.lon, radiusNm, force = true)
        }
    }

    fun select(aircraft: Aircraft) {
        _selected.value = aircraft
        analytics.logAircraftTapped(aircraft.source.name)
    }

    fun clearSelection() {
        _selected.value = null
    }

    fun toggleSaveSelected() {
        val ac = _selected.value ?: return
        viewModelScope.launch {
            val saved = savedRepository.isAircraftSaved(ac.id).first()
            savedRepository.toggleAircraft(ac, saved)
        }
    }
}
