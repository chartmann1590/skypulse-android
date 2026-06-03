package com.charles.skypulse.app.ui.screens.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.charles.skypulse.app.domain.model.Aircraft
import com.charles.skypulse.app.domain.model.AltitudeUnit
import com.charles.skypulse.app.domain.model.FlightRoute
import com.charles.skypulse.app.domain.model.RouteProgress
import com.charles.skypulse.app.domain.model.SpeedUnit
import com.charles.skypulse.app.domain.util.FormatUtils
import com.charles.skypulse.app.ui.components.GhostButton
import com.charles.skypulse.app.ui.components.PrimaryButton
import com.charles.skypulse.app.ui.components.StatusBadge
import com.charles.skypulse.app.ui.theme.SkyColors
import com.charles.skypulse.app.ui.theme.SkyType

private data class DataCell(val icon: ImageVector, val label: String, val value: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AircraftDetailSheet(
    aircraft: Aircraft,
    altitudeUnit: AltitudeUnit,
    speedUnit: SpeedUnit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    route: FlightRoute? = null,
    progress: RouteProgress? = null,
    isSharing: Boolean = false,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SkyColors.SurfaceContainer,
        contentColor = SkyColors.OnSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text(
                        aircraft.displayName,
                        style = SkyType.HeadlineLgMobile,
                        color = SkyColors.TextHigh,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "ICAO: ${aircraft.hex?.uppercase() ?: "—"}",
                            style = SkyType.DataLg.copy(fontSize = SkyType.LabelSm.fontSize),
                            color = SkyColors.OnSurfaceVariant,
                        )
                        aircraft.originCountry?.let {
                            Text("• $it", style = SkyType.LabelSm, color = SkyColors.OnSurfaceVariant)
                        }
                    }
                }
                StatusBadge(
                    text = if (aircraft.onGround) "GROUND" else "LIVE",
                    dotColor = if (aircraft.onGround) SkyColors.TextMed else SkyColors.PrimaryFixedDim,
                )
            }

            val cells = listOf(
                DataCell(Icons.Filled.Height, "Altitude", FormatUtils.altitude(aircraft.altitudeFeet, altitudeUnit)),
                DataCell(Icons.Filled.Speed, "Speed", FormatUtils.speed(aircraft.speedKnots, speedUnit)),
                DataCell(Icons.Filled.Explore, "Heading", FormatUtils.heading(aircraft.headingDegrees)),
                DataCell(Icons.Filled.SwapVert, "Vert. Rate", FormatUtils.verticalRate(aircraft.verticalRate)),
                DataCell(Icons.Filled.Radar, "Last Seen", FormatUtils.relativeTime(aircraft.lastSeenEpochSeconds)),
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(cells) { cell -> CockpitCell(cell) }
            }

            if (route != null) {
                RouteStrip(route, progress)
            } else {
                Text(
                    "Route data unavailable for this flight on free feeds.",
                    style = SkyType.LabelSm,
                    color = SkyColors.OnSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GhostButton(
                    text = "Save",
                    onClick = onSave,
                    leadingIcon = Icons.Filled.Star,
                    modifier = Modifier.weight(1f),
                )
                GhostButton(
                    text = "Notify",
                    onClick = onSave,
                    leadingIcon = Icons.Filled.NotificationsActive,
                    modifier = Modifier.weight(1f),
                )
            }

            // Share a live link to this flight that friends can open on the web — or in the app.
            PrimaryButton(
                text = if (isSharing) "Generating link…" else "Share flight",
                onClick = { if (!isSharing) onShare() },
                leadingIcon = Icons.Filled.Share,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun RouteStrip(route: FlightRoute, progress: RouteProgress?) {
    val shape = RoundedCornerShape(16.dp)
    val fraction = (progress?.fractionComplete ?: 0.0).toFloat().coerceIn(0f, 1f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SkyColors.SurfaceContainerLowest, shape)
            .border(1.dp, SkyColors.GlassStroke, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            RouteEnd(route.origin.code, route.origin.city ?: route.origin.name, Alignment.Start)
            route.airlineName?.let {
                Text(it, style = SkyType.LabelSm, color = SkyColors.OnSurfaceVariant)
            }
            RouteEnd(route.destination.code, route.destination.city ?: route.destination.name, Alignment.End)
        }
        // Progress track with plane marker.
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(50))
                    .background(SkyColors.OutlineVariant, RoundedCornerShape(50)),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(3.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(50))
                    .background(SkyColors.PrimaryFixedDim, RoundedCornerShape(50)),
            )
            Box(modifier = Modifier.fillMaxWidth(fraction), contentAlignment = Alignment.CenterEnd) {
                Icon(
                    Icons.Filled.Flight,
                    contentDescription = null,
                    tint = SkyColors.PrimaryFixedDim,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        val etaText = when {
            progress?.etaMinutes != null -> "~${progress.etaMinutes} min left · ${progress.remainingNm.toInt()} NM (est.)"
            progress != null -> "${progress.remainingNm.toInt()} NM to go (est.)"
            else -> "Estimated from live position"
        }
        Text(etaText, style = SkyType.LabelSm, color = SkyColors.PrimaryFixedDim)
        Text(
            "Route from live flight data (free sources) — arrival time is estimated from the current position.",
            style = SkyType.LabelSm,
            color = SkyColors.Outline,
        )
    }
}

@Composable
private fun RouteEnd(code: String, name: String?, align: Alignment.Horizontal) {
    Column(horizontalAlignment = align) {
        Text(code, style = SkyType.HeadlineLgMobile, color = SkyColors.TextHigh)
        name?.let { Text(it, style = SkyType.LabelSm, color = SkyColors.Outline) }
    }
}

@Composable
private fun CockpitCell(cell: DataCell) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .background(SkyColors.SurfaceContainerLow.copy(alpha = 0.5f), shape)
            .border(1.dp, SkyColors.GlassStroke, shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(cell.icon, null, tint = SkyColors.Outline, modifier = Modifier.size(14.dp))
            Text(cell.label, style = SkyType.LabelSm, color = SkyColors.Outline)
        }
        Text(cell.value, style = SkyType.DataLg, color = SkyColors.PrimaryFixedDim)
    }
}
