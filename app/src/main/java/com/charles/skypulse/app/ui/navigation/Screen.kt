package com.charles.skypulse.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocalAirport
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.LocalAirport
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val MAP = "map"
    const val NEARBY = "nearby"
    const val AIRPORTS = "airports"
    const val ALERTS = "alerts"
    const val SAVED = "saved"
    const val SETTINGS = "settings"
    const val PRIVACY = "privacy"
}

/** Bottom navigation destinations (the five primary tabs). */
enum class BottomTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
) {
    MAP(Routes.MAP, "Map", Icons.Filled.Map, Icons.Outlined.Map),
    NEARBY(Routes.NEARBY, "Nearby", Icons.Filled.Explore, Icons.Outlined.Explore),
    AIRPORTS(Routes.AIRPORTS, "Airports", Icons.Filled.LocalAirport, Icons.Outlined.LocalAirport),
    ALERTS(Routes.ALERTS, "Alerts", Icons.Filled.Notifications, Icons.Outlined.Notifications),
    SAVED(Routes.SAVED, "Saved", Icons.Filled.Bookmark, Icons.Outlined.Bookmark),
}

val BOTTOM_BAR_ROUTES = BottomTab.entries.map { it.route }.toSet()
