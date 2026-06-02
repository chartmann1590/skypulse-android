package com.charles.skypulse.app.ui.screens.airports

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.charles.skypulse.app.domain.model.Airport
import com.charles.skypulse.app.domain.util.FormatUtils
import com.charles.skypulse.app.ui.components.AircraftListItem
import com.charles.skypulse.app.ui.components.EmptyState
import com.charles.skypulse.app.ui.components.PrimaryButton
import com.charles.skypulse.app.ui.theme.SkyColors
import com.charles.skypulse.app.ui.theme.SkyType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AirportLookupScreen(viewModel: AirportViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val focus by viewModel.focusAirport.collectAsStateWithLifecycle()
    val focusAircraft by viewModel.aircraftAtFocus.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf(TextFieldValue("")) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                "Airports",
                style = SkyType.HeadlineLgMobile,
                color = SkyColors.TextHigh,
                modifier = Modifier.padding(top = 56.dp),
            )
        }
        item {
            val shape = RoundedCornerShape(12.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SkyColors.SurfaceContainerLow, shape)
                    .border(1.dp, SkyColors.GlassStroke, shape)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Filled.Search, null, tint = SkyColors.OnSurfaceVariant)
                BasicTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        viewModel.onQueryChange(it.text)
                    },
                    singleLine = true,
                    textStyle = SkyType.BodyMd.copy(color = SkyColors.OnSurface),
                    cursorBrush = SolidColor(SkyColors.PrimaryFixedDim),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (query.text.isEmpty()) {
                            Text(
                                "Search city, airport, IATA/ICAO",
                                style = SkyType.BodyMd,
                                color = SkyColors.OnSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                        inner()
                    },
                )
            }
        }

        if (state.query.isNotBlank()) {
            items(state.results.size) { i ->
                AirportRow(state.results[i], settings.distanceUnit, viewModel::viewAircraftNear, viewModel::saveAirport)
            }
            if (state.results.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.Search,
                        title = "No matches",
                        subtitle = "Try an airport name, city, or IATA/ICAO code.",
                    )
                }
            }
        } else {
            item { SectionHeader(Icons.Filled.LocationOn, "Airports Near You") }
            items(state.nearby.size) { i ->
                AirportRow(state.nearby[i], settings.distanceUnit, viewModel::viewAircraftNear, viewModel::saveAirport)
            }
            item { SectionHeader(Icons.Filled.Star, "Featured Airports") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.featured.chunked(2).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            rowItems.forEach { airport ->
                                FeaturedCard(
                                    airport = airport,
                                    modifier = Modifier.weight(1f),
                                    onClick = { viewModel.viewAircraftNear(airport) },
                                )
                            }
                            if (rowItems.size == 1) Box(modifier = Modifier.weight(1f)) {}
                        }
                    }
                }
            }
            item { androidx.compose.foundation.layout.Spacer(Modifier.padding(40.dp)) }
        }
    }

    if (focus != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = viewModel::clearFocus,
            sheetState = sheetState,
            containerColor = SkyColors.SurfaceContainer,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Aircraft near ${focus!!.code}",
                    style = SkyType.TitleMd,
                    color = SkyColors.TextHigh,
                )
                if (focusAircraft.isEmpty()) {
                    EmptyState(
                        icon = Icons.Filled.FlightTakeoff,
                        title = "No aircraft right now",
                        subtitle = "Open ADS-B coverage varies. Try again shortly.",
                    )
                } else {
                    focusAircraft.take(20).forEach { ac ->
                        AircraftListItem(
                            aircraft = ac,
                            distanceUnit = settings.distanceUnit,
                            altitudeUnit = settings.altitudeUnit,
                            speedUnit = settings.speedUnit,
                            onClick = {},
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = SkyColors.PrimaryFixedDim, modifier = Modifier.padding(2.dp))
        Text(title, style = SkyType.TitleMd, color = SkyColors.OnSurface)
    }
}

@Composable
private fun AirportRow(
    airport: Airport,
    distanceUnit: com.charles.skypulse.app.domain.model.DistanceUnit,
    onViewAircraft: (Airport) -> Unit,
    onSave: (Airport) -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SkyColors.SurfaceContainerLow.copy(alpha = 0.6f), shape)
            .border(1.dp, SkyColors.GlassStroke, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(airport.code, style = SkyType.DataLg, color = SkyColors.TextHigh)
                airport.distanceKm?.let {
                    Text(
                        FormatUtils.distanceFromNm(it / 1.852, distanceUnit),
                        style = SkyType.LabelSm,
                        color = SkyColors.OnSurfaceVariant,
                    )
                }
            }
            Icon(
                Icons.Filled.Star,
                "Save",
                tint = SkyColors.OnSurfaceVariant,
                modifier = Modifier.clickable { onSave(airport) },
            )
        }
        Text(airport.name, style = SkyType.BodyMd, color = SkyColors.OnSurface)
        airport.city?.let { Text(it, style = SkyType.LabelSm, color = SkyColors.OnSurfaceVariant) }
        PrimaryButton(
            text = "View nearby aircraft",
            onClick = { onViewAircraft(airport) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FeaturedCard(airport: Airport, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .background(SkyColors.SurfaceContainerLow.copy(alpha = 0.6f), shape)
            .border(1.dp, SkyColors.GlassStroke, shape)
            .clickable { onClick() }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(airport.code, style = SkyType.DataLg, color = SkyColors.TextHigh)
        Text(airport.name, style = SkyType.BodyMd, color = SkyColors.OnSurface)
        airport.country?.let { Text(it, style = SkyType.LabelSm, color = SkyColors.OnSurfaceVariant) }
    }
}
