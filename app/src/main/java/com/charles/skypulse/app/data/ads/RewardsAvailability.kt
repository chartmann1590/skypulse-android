package com.charles.skypulse.app.data.ads

import com.charles.skypulse.app.BuildConfig

/**
 * Whether the optional "watch an ad for ad-free time" rewards feature exists in this build.
 *
 * Amazon Appstore (Fire) builds turn it off: Fire devices have no Google Play services, so a
 * rewarded ad may never load there and the "Watch an ad" button would do nothing. Amazon review
 * flagged exactly that, so the Amazon build hides every rewards entry point instead.
 */
object RewardsAvailability {
    val enabled: Boolean = BuildConfig.STORE != "amazon"
}
