package com.charles.skypulse.app.ui.screens.onboarding

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.charles.skypulse.app.ui.components.PrimaryButton
import com.charles.skypulse.app.ui.theme.SkyColors
import com.charles.skypulse.app.ui.theme.SkyType
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val permissions = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val permissionState = rememberMultiplePermissionsState(permissions) { onDone() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(180.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Flight,
                contentDescription = null,
                tint = SkyColors.PrimaryContainer,
                modifier = Modifier.size(72.dp),
            )
        }
        Text(
            "See aircraft around you",
            style = SkyType.HeadlineLg,
            color = SkyColors.TextHigh,
            textAlign = TextAlign.Center,
        )
        Text(
            "Track live flights in real-time using free, open ADS-B data. No account, no API key — your data stays on your device.",
            style = SkyType.BodyMd,
            color = SkyColors.OnSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )

        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PermissionRow(Icons.Filled.MyLocation, "Location", "Find aircraft and airports near you.")
            PermissionRow(Icons.Filled.Notifications, "Notifications", "Get local alerts when aircraft enter your area.")
        }

        PrimaryButton(
            text = "Get Started",
            onClick = { permissionState.launchMultiplePermissionRequest() },
            modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
        )
    }
}

@Composable
private fun PermissionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(icon, null, tint = SkyColors.PrimaryFixedDim, modifier = Modifier.size(28.dp))
        Column {
            Text(title, style = SkyType.TitleMd, color = SkyColors.OnSurface)
            Text(body, style = SkyType.LabelSm, color = SkyColors.OnSurfaceVariant)
        }
    }
}
