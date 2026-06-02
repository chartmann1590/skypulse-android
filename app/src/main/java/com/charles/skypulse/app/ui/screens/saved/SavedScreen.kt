package com.charles.skypulse.app.ui.screens.saved

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.LocalAirport
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.charles.skypulse.app.ui.components.EmptyState
import com.charles.skypulse.app.ui.components.PillChip
import com.charles.skypulse.app.ui.components.SkyTopAppBar
import com.charles.skypulse.app.ui.theme.SkyColors
import com.charles.skypulse.app.ui.theme.SkyType

@Composable
fun SavedScreen(viewModel: SavedViewModel = hiltViewModel()) {
    val aircraft by viewModel.aircraft.collectAsStateWithLifecycle()
    val airports by viewModel.airports.collectAsStateWithLifecycle()
    val areas by viewModel.areas.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(SavedTab.AIRCRAFT) }

    Column(modifier = Modifier.fillMaxSize()) {
        SkyTopAppBar()
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Saved Items", style = SkyType.HeadlineLgMobile, color = SkyColors.TextHigh)
            Text(
                "Quick access to your tracked entities.",
                style = SkyType.BodyMd,
                color = SkyColors.OnSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PillChip("Aircraft", tab == SavedTab.AIRCRAFT) { tab = SavedTab.AIRCRAFT }
            PillChip("Airports", tab == SavedTab.AIRPORTS) { tab = SavedTab.AIRPORTS }
            PillChip("Areas", tab == SavedTab.AREAS) { tab = SavedTab.AREAS }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (tab) {
                SavedTab.AIRCRAFT -> {
                    if (aircraft.isEmpty()) item { EmptyStateItem("aircraft") }
                    items(aircraft.size) { i ->
                        val a = aircraft[i]
                        SavedRow(
                            icon = Icons.Filled.Flight,
                            title = a.callsign ?: a.hex?.uppercase() ?: a.id,
                            subtitle = a.typeLabel ?: a.hex?.uppercase() ?: "Aircraft",
                            onRemove = { viewModel.removeAircraft(a.id) },
                        )
                    }
                }
                SavedTab.AIRPORTS -> {
                    if (airports.isEmpty()) item { EmptyStateItem("airports") }
                    items(airports.size) { i ->
                        val a = airports[i]
                        SavedRow(
                            icon = Icons.Filled.LocalAirport,
                            title = "${a.code} · ${a.name}",
                            subtitle = listOfNotNull(a.city, a.country).joinToString(", "),
                            onRemove = { viewModel.removeAirport(a.airportId) },
                        )
                    }
                }
                SavedTab.AREAS -> {
                    if (areas.isEmpty()) item { EmptyStateItem("areas") }
                    items(areas.size) { i ->
                        val a = areas[i]
                        SavedRow(
                            icon = Icons.Outlined.Map,
                            title = a.label,
                            subtitle = "${a.radiusNm.toInt()} NM radius",
                            onRemove = { viewModel.removeArea(a.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateItem(kind: String) {
    EmptyState(
        icon = Icons.Filled.Bookmark,
        title = "No saved $kind",
        subtitle = "Items you save will appear here for quick access.",
    )
}

@Composable
private fun SavedRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onRemove: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SkyColors.SurfaceContainerLow.copy(alpha = 0.6f), shape)
            .border(1.dp, SkyColors.GlassStroke, shape)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SkyColors.SurfaceContainerHigh, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = SkyColors.PrimaryFixedDim, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = SkyType.TitleMd, color = SkyColors.TextHigh)
            Text(subtitle, style = SkyType.LabelSm, color = SkyColors.OnSurfaceVariant)
        }
        Icon(
            Icons.Filled.Star,
            "Remove",
            tint = SkyColors.PrimaryFixedDim,
            modifier = Modifier.clickable { onRemove() },
        )
    }
}
