package com.charles.skypulse.app.ui.ads

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.charles.skypulse.app.data.ads.AdConfig
import com.charles.skypulse.app.ui.util.isShortScreen
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Anchored adaptive banner shown at the bottom of the app. Renders nothing while the user has
 * active ad-free time (so toggling ad-free reactively adds/removes the banner).
 */
@Composable
fun BannerAd(
    adFree: Boolean,
    modifier: Modifier = Modifier,
) {
    if (adFree) return
    // Don't try to inflate an AdView in Compose previews.
    if (LocalInspectionMode.current) return

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(adaptiveBannerSize(ctx))
                adUnitId = AdConfig.bannerId
                loadAd(AdRequest.Builder().build())
            }
        },
    )
}

private fun adaptiveBannerSize(context: Context): AdSize {
    val metrics = context.resources.displayMetrics
    val adWidthDp = (metrics.widthPixels / metrics.density).toInt()
    val screenHeightDp = context.resources.configuration.screenHeightDp
    return if (isShortScreen(screenHeightDp)) {
        // Short phones (e.g. 5" 360x640 dp): the large banner is ~113 dp there, which together with
        // the bottom nav eats about a quarter of the screen. Use the standard anchored adaptive size
        // (~56 dp at 360 dp wide) so the map stays usable.
        @Suppress("DEPRECATION")
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidthDp)
    } else {
        // SDK 25 replacement for the deprecated getCurrentOrientationAnchoredAdaptiveBannerAdSize.
        AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, adWidthDp)
    }
}
