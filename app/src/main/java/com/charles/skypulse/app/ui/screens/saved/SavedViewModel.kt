package com.charles.skypulse.app.ui.screens.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charles.skypulse.app.data.local.entity.SavedAircraftEntity
import com.charles.skypulse.app.data.local.entity.SavedAirportEntity
import com.charles.skypulse.app.data.local.entity.SavedAreaEntity
import com.charles.skypulse.app.data.repository.SavedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SavedTab { AIRCRAFT, AIRPORTS, AREAS }

@HiltViewModel
class SavedViewModel @Inject constructor(
    private val savedRepository: SavedRepository,
) : ViewModel() {

    val aircraft: StateFlow<List<SavedAircraftEntity>> = savedRepository.savedAircraft
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val airports: StateFlow<List<SavedAirportEntity>> = savedRepository.savedAirports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val areas: StateFlow<List<SavedAreaEntity>> = savedRepository.savedAreas
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun removeAircraft(id: String) = viewModelScope.launch { savedRepository.removeAircraft(id) }
    fun removeAirport(id: Int) = viewModelScope.launch { savedRepository.removeAirport(id) }
    fun removeArea(id: Long) = viewModelScope.launch { savedRepository.removeArea(id) }
}
