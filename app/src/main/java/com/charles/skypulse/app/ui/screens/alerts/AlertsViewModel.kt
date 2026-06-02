package com.charles.skypulse.app.ui.screens.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charles.skypulse.app.data.firebase.Analytics
import com.charles.skypulse.app.data.repository.AlertRepository
import com.charles.skypulse.app.data.settings.SettingsDataStore
import com.charles.skypulse.app.domain.model.AlertRule
import com.charles.skypulse.app.domain.model.AlertType
import com.charles.skypulse.app.worker.AlertScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlertsUiState(
    val areaEnabled: Boolean = true,
    val radiusNm: Float = 15f,
    val callsignEnabled: Boolean = false,
    val callsign: String = "",
    val lowAltEnabled: Boolean = false,
    val altitudeThresholdFeet: Float = 5000f,
    val airportEnabled: Boolean = false,
    val saved: Boolean = false,
)

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val alertRepository: AlertRepository,
    private val settingsDataStore: SettingsDataStore,
    private val alertScheduler: AlertScheduler,
    private val analytics: Analytics,
) : ViewModel() {

    private val _state = MutableStateFlow(AlertsUiState())
    val state: StateFlow<AlertsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val rules = alertRepository.getAll().associateBy { it.type }
            _state.value = AlertsUiState(
                areaEnabled = rules[AlertType.AIRCRAFT_ENTERS_AREA]?.enabled ?: false,
                radiusNm = (rules[AlertType.AIRCRAFT_ENTERS_AREA]?.radiusNm ?: 15.0).toFloat(),
                callsignEnabled = rules[AlertType.SPECIFIC_CALLSIGN]?.enabled ?: false,
                callsign = rules[AlertType.SPECIFIC_CALLSIGN]?.callsign ?: "",
                lowAltEnabled = rules[AlertType.LOW_ALTITUDE_NEARBY]?.enabled ?: false,
                altitudeThresholdFeet = (rules[AlertType.LOW_ALTITUDE_NEARBY]?.altitudeThresholdFeet ?: 5000.0).toFloat(),
                airportEnabled = rules[AlertType.AIRPORT_ACTIVITY_NEARBY]?.enabled ?: false,
            )
        }
    }

    fun setAreaEnabled(v: Boolean) = update { it.copy(areaEnabled = v, saved = false) }
    fun setRadius(v: Float) = update { it.copy(radiusNm = v, saved = false) }
    fun setCallsignEnabled(v: Boolean) = update { it.copy(callsignEnabled = v, saved = false) }
    fun setCallsign(v: String) = update { it.copy(callsign = v, saved = false) }
    fun setLowAltEnabled(v: Boolean) = update { it.copy(lowAltEnabled = v, saved = false) }
    fun setAltitude(v: Float) = update { it.copy(altitudeThresholdFeet = v, saved = false) }
    fun setAirportEnabled(v: Boolean) = update { it.copy(airportEnabled = v, saved = false) }

    fun savePreferences() {
        val s = _state.value
        val rules = listOf(
            AlertRule(type = AlertType.AIRCRAFT_ENTERS_AREA, enabled = s.areaEnabled, radiusNm = s.radiusNm.toDouble()),
            AlertRule(type = AlertType.SPECIFIC_CALLSIGN, enabled = s.callsignEnabled, callsign = s.callsign.trim()),
            AlertRule(type = AlertType.LOW_ALTITUDE_NEARBY, enabled = s.lowAltEnabled, altitudeThresholdFeet = s.altitudeThresholdFeet.toDouble(), radiusNm = s.radiusNm.toDouble()),
            AlertRule(type = AlertType.AIRPORT_ACTIVITY_NEARBY, enabled = s.airportEnabled, radiusNm = s.radiusNm.toDouble()),
        )
        viewModelScope.launch {
            alertRepository.saveAll(rules)
            val anyEnabled = rules.any { it.enabled }
            settingsDataStore.setBackgroundAlerts(anyEnabled)
            alertScheduler.setEnabled(anyEnabled)
            rules.filter { it.enabled }.forEach { analytics.logAlertCreated(it.type.name) }
            _state.value = _state.value.copy(saved = true)
        }
    }

    private inline fun update(block: (AlertsUiState) -> AlertsUiState) {
        _state.value = block(_state.value)
    }
}
