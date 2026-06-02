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
                "No account required",
                "SkyPulse never asks you to sign in or create an account. There is no user profile and no password.",
            )
            Section(
                "Your data stays on your device",
                "Saved aircraft, saved airports, saved areas, and alert rules are stored locally in an on-device database. They are never uploaded to a server by this app.",
            )
            Section(
                "Location",
                "Your location is used only on-device to find aircraft and airports near you and to evaluate local alerts. It is not sent to any SkyPulse backend (there isn't one).",
            )
            Section(
                "Open data sources",
                "Live aircraft come from the free ADSB.lol API and, as a fallback, OpenSky Network's anonymous API. Airport and airline reference data is bundled from OpenFlights. These are public, open data sources and require no key or login.",
            )
            Section(
                "Diagnostics (Firebase, free tier)",
                "Anonymous crash reports (Crashlytics), aggregate non-personal usage analytics (Google Analytics for Firebase), and app performance metrics (Firebase Performance Monitoring) help improve the app. Remote Config delivers feature flags. Optional push (FCM) may be used for announcements. None of these require a login or collect personally identifying information.",
            )
            Section(
                "Ads (Google AdMob)",
                "SkyPulse is free and shows ads via Google AdMob (a banner, occasional full-screen ads, and optional rewarded ads). Google may use a device advertising identifier to serve and measure ads. On first launch you'll see a consent choice (Google's User Messaging Platform) where required by law; you can change it later. See Google's policies for how ad data is handled.",
            )
            Section(
                "Ad-free rewards",
                "You can watch optional rewarded ads to earn credits and unlock ad-free time. Your credits and ad-free timer are stored only on your device and reset daily — they are not tied to any account or sent anywhere.",
            )
            Section(
                "Limits of free ADS-B data",
                "Not every aircraft is guaranteed to appear. Coverage varies by area and time, data can be delayed or missing, and some military or private aircraft may be hidden or incomplete. Gate, boarding, and route status are not provided by free feeds.",
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
