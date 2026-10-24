package com.charles.skypulse.app.ui.screens.map

import android.app.Activity

import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import android.content.Intent
import android.widget.Toast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.charles.skypulse.app.domain.model.DataSource
import com.charles.skypulse.app.domain.util.FormatUtils
import com.charles.skypulse.app.review.ReviewPrompter
import com.charles.skypulse.app.ui.components.AircraftListItem
import com.charles.skypulse.app.ui.components.StatusBadge
import com.charles.skypulse.app.ui.screens.rewards.RewardsChip
import com.charles.skypulse.app.ui.theme.SkyColors
import com.charles.skypulse.app.ui.theme.SkyType
import com.charles.skypulse.app.ui.theme.glassPanel
import com.charles.skypulse.app.ui.util.isShortScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeMapScreen(
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenRewards: () -> Unit,
    sharedFlightId: String? = null,
    viewModel: HomeMapViewModel = hiltViewModel(),
) {
    val feed by viewModel.feed.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val userLocation by viewModel.userLocation.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val selectedRoute by viewModel.selectedRoute.collectAsStateWithLifecycle()
    val selectedProgress by viewModel.selectedProgress.collectAsStateWithLifecycle()
    val recenterTrigger by viewModel.recenterTrigger.collectAsStateWithLifecycle()
    val focusShared by viewModel.focusShared.collectAsStateWithLifecycle()
    val isSharing by viewModel.isSharing.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Short phones (e.g. 5" 360x640 dp) get a compact layout so the map stays visible.
    val compact = isShortScreen()

    // Open a shared flight when arriving via deep link.
    LaunchedEffect(sharedFlightId) {
        if (!sharedFlightId.isNullOrBlank()) {
            viewModel.openSharedFlight(sharedFlightId)
        }
    }

    // Tapping an aircraft for live details is the app's core "aha" moment.
    LaunchedEffect(selected) {
        if (selected != null) {
            (context as? Activity)?.let { ReviewPrompter.maybeRequestReview(it) }
        }
    }

    // React to share-link generation: launch the Android share sheet or show an error.
    LaunchedEffect(Unit) {
        viewModel.shareEvents.collect { event ->
            when (event) {
                is ShareEvent.Ready -> {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, event.url)
                    }
                    context.startActivity(
                        Intent.createChooser(sendIntent, "Share flight").apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        },
                    )
                }
                is ShareEvent.Failed ->
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(SkyColors.PitchBlack)) {
        OsmMapView(
            aircraft = feed.aircraft,
            userLat = userLocation?.lat,
            userLon = userLocation?.lon,
            selectedId = selected?.id,
            onAircraftClick = viewModel::select,
            modifier = Modifier.fillMaxSize(),
            recenterTrigger = recenterTrigger,
            focusAircraft = focusShared,
        )

        // Floating search bar — opens the Airports search.
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .glassPanel(RoundedCornerShape(50), fill = SkyColors.PitchBlack.copy(alpha = 0.6f))
                .clickable { onOpenSearch() }
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

        // Right-side FABs. On short screens they sit in a row under the search pill so they never
        // cover the summary card; taller screens keep the original centered column.
        if (compact) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 76.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RewardsChip(onClick = onOpenRewards)
                MapFab(Icons.Filled.MyLocation, "Recenter", viewModel::recenter)
                MapFab(Icons.Filled.Tune, "Settings", onOpenSettings)
            }
        } else {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.End,
            ) {
                RewardsChip(onClick = onOpenRewards)
                MapFab(Icons.Filled.MyLocation, "Recenter", viewModel::recenter)
                MapFab(Icons.Filled.Tune, "Settings", onOpenSettings)
            }
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
            compact = compact,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(if (compact) 12.dp else 16.dp),
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
            onShare = viewModel::shareSelected,
            isSharing = isSharing,
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
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    // On short screens the card starts collapsed to one row; the choice survives rotation and
    // process death. Taller screens always show the full card.
    var expanded by rememberSaveable(compact) { mutableStateOf(!compact) }
    val statusLabel = when {
        isOffline -> "Cached"
        source == DataSource.OPENSKY -> "OpenSky • ${FormatUtils.relativeTime(lastUpdated?.div(1000))}"
        else -> "Updated ${FormatUtils.relativeTime(lastUpdated?.div(1000))}"
    }
    val accent = if (isOffline) SkyColors.AlertRed else SkyColors.PrimaryFixedDim

    if (compact && !expanded) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(shape)
                .glassPanel(shape, fill = SkyColors.SurfaceContainerLow.copy(alpha = 0.75f))
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("$count planes", style = SkyType.TitleMd, color = SkyColors.PrimaryFixedDim, maxLines = 1)
            val detail = buildString {
                if (closest != null) {
                    append("Closest ")
                    append(closest.displayName)
                    append(" ")
                    append(FormatUtils.distanceFromNm(closest.distanceNm, distanceUnit))
                    append(" • ")
                }
                append(statusLabel)
            }
            Text(
                detail,
                style = SkyType.BodyMd,
                color = if (isOffline) SkyColors.AlertRed else SkyColors.OnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Filled.KeyboardArrowUp, "Show details", tint = accent, modifier = Modifier.size(24.dp))
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .clip(shape)
            .glassPanel(shape, fill = SkyColors.SurfaceContainerLow.copy(alpha = 0.75f))
            .padding(if (compact) 16.dp else 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (compact) Modifier.clickable { expanded = false } else Modifier),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Aircraft Nearby", style = SkyType.TitleMd, color = SkyColors.OnSurface)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("$count planes", style = SkyType.DataLg, color = SkyColors.PrimaryFixedDim)
                if (compact) {
                    Icon(Icons.Filled.KeyboardArrowDown, "Hide details", tint = accent, modifier = Modifier.size(24.dp))
                }
            }
        }
        if (closest != null) {
            AircraftListItem(
                aircraft = closest,
                distanceUnit = distanceUnit,
                altitudeUnit = altitudeUnit,
                speedUnit = speedUnit,
                onClick = { onClosestClick(closest) },
                accent = accent,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusBadge(
                text = statusLabel,
                dotColor = accent,
                textColor = if (isOffline) SkyColors.AlertRed else SkyColors.OnSurfaceVariant,
            )
        }
    }
}
