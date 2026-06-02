package com.charles.skypulse.app.data.ads

import com.charles.skypulse.app.BuildConfig

/**
 * Ad unit IDs, sourced from [BuildConfig] (which is populated from local.properties / CI secrets,
 * falling back to Google's official test IDs in unconfigured builds). Never hardcode IDs elsewhere.
 */
object AdConfig {
    val bannerId: String = BuildConfig.ADMOB_BANNER_ID
    val interstitialId: String = BuildConfig.ADMOB_INTERSTITIAL_ID
    val rewardedId: String = BuildConfig.ADMOB_REWARDED_ID
}
