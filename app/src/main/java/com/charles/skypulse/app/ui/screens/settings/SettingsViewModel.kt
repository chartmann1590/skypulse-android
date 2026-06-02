package com.charles.skypulse.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charles.skypulse.app.data.local.dao.CacheDao
import com.charles.skypulse.app.data.settings.SettingsDataStore
import com.charles.skypulse.app.data.settings.SkySettings
import com.charles.skypulse.app.domain.model.AltitudeUnit
import com.charles.skypulse.app.domain.model.DistanceUnit
import com.charles.skypulse.app.domain.model.SpeedUnit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val cacheDao: CacheDao,
) : ViewModel() {

    val settings: StateFlow<SkySettings> = settingsDataStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SkySettings())

    private val _cacheCleared = MutableStateFlow(false)
    val cacheCleared: StateFlow<Boolean> = _cacheCleared.asStateFlow()

    fun setRefreshInterval(seconds: Int) =
        viewModelScope.launch { settingsDataStore.setRefreshInterval(seconds) }

    fun setAltitudeUnit(unit: AltitudeUnit) =
        viewModelScope.launch { settingsDataStore.setAltitudeUnit(unit) }

    fun setSpeedUnit(unit: SpeedUnit) =
        viewModelScope.launch { settingsDataStore.setSpeedUnit(unit) }

    fun setDistanceUnit(unit: DistanceUnit) =
        viewModelScope.launch { settingsDataStore.setDistanceUnit(unit) }

    fun clearCache() = viewModelScope.launch {
        cacheDao.clear()
        _cacheCleared.value = true
    }
}
