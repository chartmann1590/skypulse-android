package com.charles.skypulse.app.ui.screens.nearby

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.charles.skypulse.app.domain.model.AircraftSort
import com.charles.skypulse.app.ui.components.AircraftListItem
import com.charles.skypulse.app.ui.components.EmptyState
import com.charles.skypulse.app.ui.components.LoadingState
import com.charles.skypulse.app.ui.components.PillChip
import com.charles.skypulse.app.ui.components.SkyTopAppBar
import com.charles.skypulse.app.ui.screens.map.AircraftDetailSheet
import com.charles.skypulse.app.ui.theme.SkyColors
import com.charles.skypulse.app.ui.theme.SkyType

@Composable
fun NearbyScreen(viewModel: NearbyViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        SkyTopAppBar()
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            androidx.compose.material3.Text(
                "Nearby Aircraft",
                style = SkyType.HeadlineLgMobile,
                color = SkyColors.TextHigh,
            )
            androidx.compose.material3.Text(
                "(${state.aircraft.size})",
                style = SkyType.TitleMd,
                color = SkyColors.OnSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AircraftSort.entries.forEach { option ->
                PillChip(
                    label = option.name.lowercase().replaceFirstChar { it.uppercase() },
                    selected = sort == option,
                    onClick = { viewModel.setSort(option) },
                )
            }
        }

        when {
            state.isLoading && state.aircraft.isEmpty() -> LoadingState()
            state.aircraft.isEmpty() -> EmptyState(
                icon = Icons.Filled.FlightTakeoff,
                title = "No aircraft nearby",
                subtitle = "We couldn't find aircraft around you right now. Free ADS-B coverage varies by area and time.",
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.aircraft, key = { it.id }) { ac ->
                    AircraftListItem(
                        aircraft = ac,
                        distanceUnit = settings.distanceUnit,
                        altitudeUnit = settings.altitudeUnit,
                        speedUnit = settings.speedUnit,
                        onClick = { viewModel.select(ac) },
                    )
                }
            }
        }
    }

    selected?.let { aircraft ->
        AircraftDetailSheet(
            aircraft = aircraft,
            altitudeUnit = settings.altitudeUnit,
            speedUnit = settings.speedUnit,
            onDismiss = viewModel::clearSelection,
            onSave = viewModel::toggleSaveSelected,
        )
    }
}
