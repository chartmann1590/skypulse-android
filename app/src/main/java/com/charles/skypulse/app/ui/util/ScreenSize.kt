package com.charles.skypulse.app.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Screens shorter than this (in dp, as reported by Configuration.screenHeightDp) get the compact
 * layout: standard-height banner and a collapsed map summary card.
 *
 * Why 700 dp: a 5" 720x1280 / 1080x1920 phone is 640 dp tall (about 568-616 dp after the system
 * bars), and that is where the large banner + bottom nav + the full summary card left only ~20% of
 * the map visible. Typical modern phones are 780-915 dp tall and keep the original layout. Landscape
 * phones are also below 700 dp, which is the same "little vertical room" case.
 */
const val SHORT_SCREEN_HEIGHT_DP = 700

fun isShortScreen(screenHeightDp: Int): Boolean =
    screenHeightDp in 1 until SHORT_SCREEN_HEIGHT_DP

/** True on short screens (see [SHORT_SCREEN_HEIGHT_DP]). Recomposes on configuration changes. */
@Composable
fun isShortScreen(): Boolean = isShortScreen(LocalConfiguration.current.screenHeightDp)
