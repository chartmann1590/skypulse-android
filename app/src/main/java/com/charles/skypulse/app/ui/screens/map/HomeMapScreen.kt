package com.charles.skypulse.app.ui.screens.map

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.charles.skypulse.app.domain.model.DataSource
import com.charles.skypulse.app.domain.util.FormatUtils
import com.charles.skypulse.app.ui.components.AircraftListItem
import com.charles.skypulse.app.ui.components.StatusBadge
import com.charles.skypulse.app.ui.theme.SkyColors
import com.charles.skypulse.app.ui.theme.SkyType
import com.charles.skypulse.app.ui.theme.glassPanel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeMapScreen(
    onOpenSettings: () -> Unit,
    viewModel: HomeMapViewModel = hiltViewModel(),
) {
    val feed by viewModel.feed.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val userLocation by viewModel.userLocation.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val selectedRoute by viewModel.selectedRoute.collectAsStateWithLifecycle()
    val selectedProgress by viewModel.selectedProgress.collectAsStateWithLifecycle()
    val recenterTrigger by viewModel.recenterTrigger.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize().background(SkyColors.PitchBlack)) {
        OsmMapView(
            aircraft = feed.aircraft,
            userLat = userLocation?.lat,
            userLon = userLocation?.lon,
            selectedId = selected?.id,
            onAircraftClick = viewModel::select,
            modifier = Modifier.fillMaxSize(),
            recenterTrigger = recenterTrigger,
        )

        // Floating search bar (decorative entry — search lives on Airports tab).
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .glassPanel(RoundedCornerShape(50), fill = SkyColors.PitchBlack.copy(alpha = 0.6f))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Filled.Search, null, tint = SkyColors.OnSurfaceVariant)
            Text(
                "Search flight, airport, aircraft…",
                style = SkyType.BodyMd,
                color = SkyColors.OnSurfaceVariant.copy(alpha = 0.7f),
            )
        }

        // Right-side FABs.
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MapFab(Icons.Filled.MyLocation, "Recenter", viewModel::recenter)
            MapFab(Icons.Filled.Tune, "Settings", onOpenSettings)
        }

        // Bottom summary card.
        SummaryCard(
            count = feed.aircraft.size,
            lastUpdated = feed.lastUpdatedEpochMs,
            source = feed.activeSource,
            isOffline = feed.isOffline,
            closest = feed.aircraft.minByOrNull { it.distanceNm ?: Double.MAX_VALUE },
            altitudeUnit = settings.altitudeUnit,
            distanceUnit = settings.distanceUnit,
            speedUnit = settings.speedUnit,
            onClosestClick = { ac -> viewModel.select(ac) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }

    selected?.let { aircraft ->
        AircraftDetailSheet(
            aircraft = aircraft,
            altitudeUnit = settings.altitudeUnit,
            speedUnit = settings.speedUnit,
            onDismiss = viewModel::clearSelection,
            onSave = viewModel::toggleSaveSelected,
            route = selectedRoute,
            progress = selectedProgress,
        )
    }
}

@Composable
private fun MapFab(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .glassPanel(CircleShape, fill = SkyColors.PitchBlack.copy(alpha = 0.6f))
            .border(1.dp, SkyColors.GlassStroke, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, desc, tint = SkyColors.PrimaryFixedDim)
    }
}

@Composable
private fun SummaryCard(
    count: Int,
    lastUpdated: Long?,
    source: DataSource?,
    isOffline: Boolean,
    closest: com.charles.skypulse.app.domain.model.Aircraft?,
    altitudeUnit: com.charles.skypulse.app.domain.model.AltitudeUnit,
    distanceUnit: com.charles.skypulse.app.domain.model.DistanceUnit,
    speedUnit: com.charles.skypulse.app.domain.model.SpeedUnit,
    onClosestClick: (com.charles.skypulse.app.domain.model.Aircraft) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .glassPanel(shape, fill = SkyColors.SurfaceContainerLow.copy(alpha = 0.75f))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Aircraft Nearby", style = SkyType.TitleMd, color = SkyColors.OnSurface)
            Text("$count planes", style = SkyType.DataLg, color = SkyColors.PrimaryFixedDim)
        }
        if (closest != null) {
            AircraftListItem(
                aircraft = closest,
                distanceUnit = distanceUnit,
                altitudeUnit = altitudeUnit,
                speedUnit = speedUnit,
                onClick = { onClosestClick(closest) },
                accent = if (isOffline) SkyColors.AlertRed else SkyColors.PrimaryFixedDim,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val label = when {
                isOffline -> "Cached"
                source == DataSource.OPENSKY -> "OpenSky • ${FormatUtils.relativeTime(lastUpdated?.div(1000))}"
                else -> "Updated ${FormatUtils.relativeTime(lastUpdated?.div(1000))}"
            }
            StatusBadge(
                text = label,
                dotColor = if (isOffline) SkyColors.AlertRed else SkyColors.PrimaryFixedDim,
                textColor = if (isOffline) SkyColors.AlertRed else SkyColors.OnSurfaceVariant,
            )
        }
    }
}
