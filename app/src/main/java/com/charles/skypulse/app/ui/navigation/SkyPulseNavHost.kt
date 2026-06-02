package com.charles.skypulse.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.charles.skypulse.app.ui.screens.airports.AirportLookupScreen
import com.charles.skypulse.app.ui.screens.alerts.AlertsScreen
import com.charles.skypulse.app.ui.screens.map.HomeMapScreen
import com.charles.skypulse.app.ui.screens.nearby.NearbyScreen
import com.charles.skypulse.app.ui.screens.onboarding.OnboardingScreen
import com.charles.skypulse.app.ui.screens.privacy.PrivacyScreen
import com.charles.skypulse.app.ui.screens.saved.SavedScreen
import com.charles.skypulse.app.ui.screens.settings.SettingsScreen
import com.charles.skypulse.app.ui.screens.splash.SplashScreen

@Composable
fun SkyPulseNavHost(appViewModel: AppViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in BOTTOM_BAR_ROUTES

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (showBottomBar) {
                SkyPulseBottomBar(
                    currentRoute = currentRoute,
                    onTabSelected = { tab ->
                        navController.navigate(tab.route) {
                            popUpTo(Routes.MAP) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(
                    onFinished = {
                        val onboarded = appViewModel.onboarded.value == true
                        val target = if (onboarded) Routes.MAP else Routes.ONBOARDING
                        navController.navigate(target) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onDone = {
                        appViewModel.completeOnboarding()
                        navController.navigate(Routes.MAP) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.MAP) {
                HomeMapScreen(onOpenSettings = { navController.navigate(Routes.SETTINGS) })
            }
            composable(Routes.NEARBY) { NearbyScreen() }
            composable(Routes.AIRPORTS) { AirportLookupScreen() }
            composable(Routes.ALERTS) { AlertsScreen() }
            composable(Routes.SAVED) { SavedScreen() }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                )
            }
            composable(Routes.PRIVACY) {
                PrivacyScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
