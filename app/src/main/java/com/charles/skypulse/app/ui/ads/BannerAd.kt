package com.charles.skypulse.app.ui.ads

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.charles.skypulse.app.data.ads.AdConfig
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

    val context = LocalContext.current
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
    return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidthDp)
}
