package com.charles.skypulse.app.data.firebase

import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remote Config (Spark tier) for safe-to-tune values and feature flags. Always falls back
 * to in-code defaults, so the app behaves correctly offline or before the first fetch.
 */
@Singleton
class RemoteConfigProvider @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig,
) {
    init {
        remoteConfig.setConfigSettingsAsync(
            FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600) // 1h cache, conservative on Spark
                .build(),
        )
        remoteConfig.setDefaultsAsync(
            mapOf(
                KEY_MIN_REFRESH_SECONDS to 8L,
                KEY_OPENSKY_FALLBACK_ENABLED to true,
                KEY_MAX_FREE_ALERTS to 4L,
                KEY_DEFAULT_RADIUS_NM to 50L,
            ),
        )
    }

    fun fetchAndActivate() {
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                Log.i(TAG, "Remote Config fetch success=${task.isSuccessful}")
            }
    }

    val minRefreshSeconds: Int get() = remoteConfig.getLong(KEY_MIN_REFRESH_SECONDS).toInt()
    val openSkyFallbackEnabled: Boolean get() = remoteConfig.getBoolean(KEY_OPENSKY_FALLBACK_ENABLED)
    val maxFreeAlerts: Int get() = remoteConfig.getLong(KEY_MAX_FREE_ALERTS).toInt()
    val defaultRadiusNm: Int get() = remoteConfig.getLong(KEY_DEFAULT_RADIUS_NM).toInt()

    companion object {
        private const val TAG = "RemoteConfigProvider"
        const val KEY_MIN_REFRESH_SECONDS = "min_refresh_seconds"
        const val KEY_OPENSKY_FALLBACK_ENABLED = "opensky_fallback_enabled"
        const val KEY_MAX_FREE_ALERTS = "max_free_alerts"
        const val KEY_DEFAULT_RADIUS_NM = "default_radius_nm"
    }
}
