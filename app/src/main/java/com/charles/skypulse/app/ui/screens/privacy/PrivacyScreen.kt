package com.charles.skypulse.app.ui.screens.privacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.charles.skypulse.app.ui.theme.SkyColors
import com.charles.skypulse.app.ui.theme.SkyType

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 40.dp, start = 4.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = SkyColors.OnSurface)
            }
            Text("Privacy", style = SkyType.HeadlineLgMobile, color = SkyColors.TextHigh)
        }
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Section(
                "Accounts and saved data",
                "You can use basic flight tracking without an account. If you choose email or Google sign-in, Firebase Authentication processes account information such as your email address and account ID. When you are signed in, saved aircraft, airports and areas (including area labels and coordinates) are synced to Cloud Firestore under your account. Local alert rules and cached flight data are not part of that account sync.",
            )
            Section(
                "Location and live data",
                "If you grant location permission, SkyPulse uses your device location to center the map, find nearby aircraft, and check location-based alerts. A nearby-aircraft request can send your precise device coordinates to ADS-B.lol or the surrounding search area to OpenSky. Map providers receive tile requests for the visible area. These services also receive network information such as your IP address. If you deny location permission, SkyPulse can still show aircraft around a default or selected area. Flightradar24 and adsbdb receive aircraft-related route lookups.",
            )
            Section(
                "Public flight sharing",
                "If you share a flight, the app stores a snapshot of the aircraft and related flight information in Cloud Firestore and creates a public link. It is not a live feed. The snapshot is associated with a Firebase account or anonymous app ID.",
            )
            Section(
                "Delete account and data",
                "Open Profile, then Delete account and data to remove your SkyPulse sign-in, synced saved items, and associated share links in your browser. Deletion does not remove data stored only on this device or public feedback posted separately on GitHub.",
            )
            Section(
                "Public feedback",
                "If you post a report or comment from the app, the content passes through a Cloudflare-hosted service to GitHub. If you choose to attach a screenshot or system diagnostics, those can also appear publicly on GitHub. Do not include private information. System diagnostics are off by default in this version.",
            )
            Section(
                "Diagnostics and app services",
                "Firebase services support app analytics, crash reports, performance measurements, configuration and push notifications. These services can process app usage, device and app identifiers, diagnostic information and push registration data. See Google’s Firebase privacy information for how it processes this data.",
            )
            Section(
                "Ads",
                "AdMob serves banner, interstitial and optional rewarded ads. Google and its partners may process advertising identifiers, IP-based location, ad interactions and technical data to serve and measure ads. The app uses Google’s consent form where required. SkyPulse rewards and the ad-free timer are stored on your device.",
            )
            Section(
                "Limits of free ADS-B data",
                "Not every aircraft is guaranteed to appear. Coverage varies by area and time, data can be delayed or missing, and some aircraft may be hidden or incomplete. Free feeds may not provide reliable gate, boarding or route status.",
            )
        }
    }
}

@Composable
private fun Section(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = SkyType.TitleMd, color = SkyColors.PrimaryFixedDim)
        Text(body, style = SkyType.BodyMd, color = SkyColors.OnSurfaceVariant)
    }
}
