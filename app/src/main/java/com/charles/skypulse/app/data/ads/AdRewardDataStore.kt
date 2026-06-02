package com.charles.skypulse.app.data.ads

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.charles.skypulse.app.domain.ads.RewardState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.rewardsDataStore: DataStore<Preferences> by preferencesDataStore(name = "skypulse_rewards")

/**
 * Persists [RewardState] in its own DataStore (separate from app settings). Mirrors the
 * [com.charles.skypulse.app.data.settings.SettingsDataStore] pattern.
 */
@Singleton
class AdRewardDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val CREDITS = intPreferencesKey("credits")
        val AD_FREE_UNTIL = longPreferencesKey("ad_free_until_ms")
        val CREDITS_EARNED_TODAY = intPreferencesKey("credits_earned_today")
        val AD_FREE_SECONDS_TODAY = intPreferencesKey("ad_free_seconds_today")
        val DAY_KEY = stringPreferencesKey("day_key")
        val INTRO_SEEN = booleanPreferencesKey("rewards_intro_seen")
    }

    val state: Flow<RewardState> = context.rewardsDataStore.data.map { p ->
        RewardState(
            credits = p[Keys.CREDITS] ?: 0,
            adFreeUntilEpochMs = p[Keys.AD_FREE_UNTIL] ?: 0L,
            creditsEarnedToday = p[Keys.CREDITS_EARNED_TODAY] ?: 0,
            adFreeSecondsGrantedToday = p[Keys.AD_FREE_SECONDS_TODAY] ?: 0,
            dayKey = p[Keys.DAY_KEY] ?: "",
            rewardsIntroSeen = p[Keys.INTRO_SEEN] ?: false,
        )
    }

    suspend fun save(state: RewardState) {
        context.rewardsDataStore.edit { p ->
            p[Keys.CREDITS] = state.credits
            p[Keys.AD_FREE_UNTIL] = state.adFreeUntilEpochMs
            p[Keys.CREDITS_EARNED_TODAY] = state.creditsEarnedToday
            p[Keys.AD_FREE_SECONDS_TODAY] = state.adFreeSecondsGrantedToday
            p[Keys.DAY_KEY] = state.dayKey
            p[Keys.INTRO_SEEN] = state.rewardsIntroSeen
        }
    }
}
