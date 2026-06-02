package com.charles.skypulse.app.ui.screens.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.charles.skypulse.app.domain.model.AltitudeUnit
import com.charles.skypulse.app.domain.model.DistanceUnit
import com.charles.skypulse.app.domain.model.SpeedUnit
import com.charles.skypulse.app.ui.components.GhostButton
import com.charles.skypulse.app.ui.theme.SkyColors
import com.charles.skypulse.app.ui.theme.SkyType

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenPrivacy: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val cacheCleared by viewModel.cacheCleared.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 40.dp, start = 4.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = SkyColors.OnSurface)
            }
            Text("Settings", style = SkyType.HeadlineLgMobile, color = SkyColors.TextHigh)
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Data sources card
            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.Wifi, null, tint = SkyColors.PrimaryFixedDim)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Connection Status", style = SkyType.DataLg, color = SkyColors.TextHigh)
                        Text("Open ADS-B sources", style = SkyType.LabelSm, color = SkyColors.PrimaryFixedDim)
                    }
                }
                StatusRow("ADSB.lol", "Primary")
                StatusRow("OpenSky", "Fallback")
                StatusRow("OpenFlights DB", "Active")
            }

            Text("Preferences", style = SkyType.TitleMd, color = SkyColors.OnSurface)
            SettingsCard {
                SegLabel("Refresh Interval")
                SegmentedRow(
                    options = listOf(5, 10, 30),
                    selectedIndex = listOf(5, 10, 30).indexOf(settings.refreshIntervalSeconds).coerceAtLeast(0),
                    labels = listOf("5s", "10s", "30s"),
                    onSelect = { viewModel.setRefreshInterval(listOf(5, 10, 30)[it]) },
                )
                SegLabel("Altitude Units")
                SegmentedRow(
                    options = AltitudeUnit.entries,
                    selectedIndex = AltitudeUnit.entries.indexOf(settings.altitudeUnit),
                    labels = listOf("Feet", "Meters"),
                    onSelect = { viewModel.setAltitudeUnit(AltitudeUnit.entries[it]) },
                )
                SegLabel("Speed Units")
                SegmentedRow(
                    options = SpeedUnit.entries,
                    selectedIndex = SpeedUnit.entries.indexOf(settings.speedUnit),
                    labels = listOf("Knots", "MPH"),
                    onSelect = { viewModel.setSpeedUnit(SpeedUnit.entries[it]) },
                )
                SegLabel("Distance Units")
                SegmentedRow(
                    options = DistanceUnit.entries,
                    selectedIndex = DistanceUnit.entries.indexOf(settings.distanceUnit),
                    labels = listOf("Miles", "Km", "NM"),
                    onSelect = { viewModel.setDistanceUnit(DistanceUnit.entries[it]) },
                )
            }

            // Privacy
            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.PrivacyTip, null, tint = SkyColors.PrimaryFixedDim)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Privacy Focus", style = SkyType.TitleMd, color = SkyColors.OnSurface)
                        Text(
                            "Your alerts and saved data stay local to your device.",
                            style = SkyType.LabelSm,
                            color = SkyColors.OnSurfaceVariant,
                        )
                    }
                }
                GhostButton("Read privacy details", onClick = onOpenPrivacy, modifier = Modifier.fillMaxWidth())
            }

            GhostButton(
                text = if (cacheCleared) "Cache Cleared ✓" else "Clear Cache",
                onClick = viewModel::clearCache,
                modifier = Modifier.fillMaxWidth(),
            )

            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("SkyPulse", style = SkyType.TitleMd, color = SkyColors.OnSurface)
                Text("Powered by open ADS-B data.", style = SkyType.LabelSm, color = SkyColors.OnSurfaceVariant)
                Text("Version 1.0.0", style = SkyType.LabelSm, color = SkyColors.Outline, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SkyColors.SurfaceContainerLow.copy(alpha = 0.6f), shape)
            .border(1.dp, SkyColors.GlassStroke, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) { content() }
}

@Composable
private fun StatusRow(name: String, tag: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, style = SkyType.BodyMd, color = SkyColors.OnSurface)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(SkyColors.SurfaceContainerHigh, RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text(tag, style = SkyType.LabelSm, color = SkyColors.OnSurfaceVariant)
        }
    }
}

@Composable
private fun SegLabel(text: String) {
    Text(text, style = SkyType.TitleMd, color = SkyColors.OnSurface, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun <T> SegmentedRow(
    options: List<T>,
    selectedIndex: Int,
    labels: List<String>,
    onSelect: (Int) -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SkyColors.SurfaceContainerLowest, shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.indices.forEach { i ->
            val selected = i == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (selected) SkyColors.SurfaceContainerHigh else androidx.compose.ui.graphics.Color.Transparent,
                        RoundedCornerShape(8.dp),
                    )
                    .clickable { onSelect(i) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    labels.getOrElse(i) { options[i].toString() },
                    style = SkyType.BodyMd,
                    color = if (selected) SkyColors.PrimaryFixedDim else SkyColors.OnSurfaceVariant,
                )
            }
        }
    }
}
