package com.charles.skypulse.app.data.firebase

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper over Firebase Analytics for key, non-PII product events. No user login or
 * personal data is ever logged (Spark tier, privacy-first).
 */
@Singleton
class Analytics @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics,
) {
    fun logScreenView(screen: String) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screen)
        })
    }

    fun logAircraftTapped(source: String) =
        log("aircraft_tapped", "source" to source)

    fun logAlertCreated(type: String) =
        log("alert_created", "type" to type)

    fun logDataSourceFallback(from: String, to: String) =
        log("data_source_fallback", "from" to from, "to" to to)

    fun logRewardEarned() = log("ad_reward_earned")

    fun logAdFreeActivated() = log("ad_free_activated")

    fun logInterstitialShown() = log("interstitial_shown")

    fun logRewardsOpened() = log("rewards_opened")

    private fun log(event: String, vararg params: Pair<String, String>) {
        firebaseAnalytics.logEvent(event, Bundle().apply {
            params.forEach { (k, v) -> putString(k, v) }
        })
    }
}
