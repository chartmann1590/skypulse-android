package com.charles.skypulse.app.ui.screens.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlightLand
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.LocalAirport
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.charles.skypulse.app.ui.components.PrimaryButton
import com.charles.skypulse.app.ui.components.SkyTopAppBar
import com.charles.skypulse.app.ui.theme.SkyColors
import com.charles.skypulse.app.ui.theme.SkyType

@Composable
fun AlertsScreen(viewModel: AlertsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SkyTopAppBar()
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Flight Alerts", style = SkyType.HeadlineLgMobile, color = SkyColors.TextHigh)
            Text(
                "Create local alerts based on live ADS-B data. No account required — checks run on your device.",
                style = SkyType.BodyMd,
                color = SkyColors.OnSurfaceVariant,
            )

            // Aircraft enters area + radius slider
            AlertCard(
                icon = Icons.Filled.TrackChanges,
                title = "Aircraft enters my area",
                enabled = state.areaEnabled,
                onToggle = viewModel::setAreaEnabled,
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Radius", style = SkyType.LabelSm, color = SkyColors.OnSurfaceVariant)
                        Text(
                            "${state.radiusNm.toInt()} NM",
                            style = SkyType.DataLg,
                            color = SkyColors.PrimaryFixedDim,
                        )
                    }
                    Slider(
                        value = state.radiusNm,
                        onValueChange = viewModel::setRadius,
                        valueRange = 5f..50f,
                        colors = SliderDefaults.colors(
                            thumbColor = SkyColors.PrimaryFixedDim,
                            activeTrackColor = SkyColors.PrimaryFixedDim,
                            inactiveTrackColor = SkyColors.OutlineVariant,
                        ),
                    )
                }
            }

            // Specific callsign
            AlertCard(
                icon = Icons.Filled.FlightTakeoff,
                title = "Specific callsign appears",
                enabled = state.callsignEnabled,
                onToggle = viewModel::setCallsignEnabled,
            ) {
                val shape = RoundedCornerShape(8.dp)
                BasicTextField(
                    value = state.callsign,
                    onValueChange = viewModel::setCallsign,
                    singleLine = true,
                    textStyle = SkyType.DataLg.copy(color = SkyColors.PitchBlack),
                    cursorBrush = SolidColor(SkyColors.OnPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SkyColors.Primary, shape)
                        .padding(12.dp),
                )
            }

            // Low altitude nearby
            AlertCard(
                icon = Icons.Filled.TrendingDown,
                title = "Low altitude flight nearby",
                subtitle = "Below ${state.altitudeThresholdFeet.toInt()} ft",
                enabled = state.lowAltEnabled,
                onToggle = viewModel::setLowAltEnabled,
            ) {
                Slider(
                    value = state.altitudeThresholdFeet,
                    onValueChange = viewModel::setAltitude,
                    valueRange = 1000f..15000f,
                    colors = SliderDefaults.colors(
                        thumbColor = SkyColors.PrimaryFixedDim,
                        activeTrackColor = SkyColors.PrimaryFixedDim,
                        inactiveTrackColor = SkyColors.OutlineVariant,
                    ),
                )
            }

            // Airport activity nearby
            AlertCard(
                icon = Icons.Filled.LocalAirport,
                title = "Airport activity nearby",
                subtitle = "Uses your saved airports",
                enabled = state.airportEnabled,
                onToggle = viewModel::setAirportEnabled,
            )

            // Saved flight departs
            AlertCard(
                icon = Icons.Filled.FlightTakeoff,
                title = "Saved flight departs",
                subtitle = "When a saved flight takes off (estimated)",
                enabled = state.departedEnabled,
                onToggle = viewModel::setDepartedEnabled,
            )

            // Saved flight landing soon
            AlertCard(
                icon = Icons.Filled.FlightLand,
                title = "Saved flight is landing",
                subtitle = "Near destination & descending (estimated)",
                enabled = state.landingEnabled,
                onToggle = viewModel::setLandingEnabled,
            )

            PrimaryButton(
                text = if (state.saved) "Preferences Saved ✓" else "Save Preferences",
                onClick = viewModel::savePreferences,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )

            Text(
                "Background checks run about every 15 minutes to respect free data limits and your battery.",
                style = SkyType.LabelSm,
                color = SkyColors.OnSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AlertCard(
    icon: ImageVector,
    title: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    subtitle: String? = null,
    content: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SkyColors.SurfaceContainerLow.copy(alpha = 0.6f), shape)
            .border(1.dp, SkyColors.GlassStroke, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, null, tint = SkyColors.PrimaryFixedDim)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = SkyType.TitleMd, color = SkyColors.OnSurface)
                subtitle?.let { Text(it, style = SkyType.LabelSm, color = SkyColors.OnSurfaceVariant) }
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SkyColors.PitchBlack,
                    checkedTrackColor = SkyColors.PrimaryFixedDim,
                    uncheckedTrackColor = SkyColors.SurfaceContainerHigh,
                ),
            )
        }
        if (enabled && content != null) content()
    }
}
