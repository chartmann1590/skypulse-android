package com.charles.skypulse.app.domain.ads

import com.charles.skypulse.app.data.ads.AdRewardDataStore
import com.charles.skypulse.app.data.firebase.Analytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for ad-reward state. Wraps [AdRewardDataStore] (persistence) and
 * [AdRewardCalculator] (pure logic), always applying daily rollover so callers see today's values.
 */
@Singleton
class AdRewardRepository @Inject constructor(
    private val store: AdRewardDataStore,
    private val analytics: Analytics,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Live state, rolled over to the current day. */
    val state: StateFlow<RewardState> = store.state
        .map { AdRewardCalculator.rolledOver(it, today()) }
        .stateIn(scope, SharingStarted.Eagerly, RewardState())

    private fun now(): Long = System.currentTimeMillis()
    private fun today(): String = AdRewardCalculator.dayKey(now())

    suspend fun currentState(): RewardState =
        AdRewardCalculator.rolledOver(store.state.first(), today())

    suspend fun isAdFreeNow(): Boolean =
        AdRewardCalculator.isAdFree(currentState(), now())

    /** Grant a credit for a completed rewarded ad. Returns true if a credit was actually added. */
    suspend fun awardCredit(): Boolean {
        val current = store.state.first()
        if (!AdRewardCalculator.canEarn(current, today())) return false
        store.save(AdRewardCalculator.earnCredit(current, today()))
        analytics.logRewardEarned()
        return true
    }

    /** Spend a credit for ad-free time. Returns true if the spend succeeded. */
    suspend fun spendCredit(): Boolean {
        val current = store.state.first()
        if (!AdRewardCalculator.canSpend(current, today())) return false
        store.save(AdRewardCalculator.spendCredit(current, now(), today()))
        analytics.logAdFreeActivated()
        return true
    }

    suspend fun markRewardsIntroSeen() {
        store.save(store.state.first().copy(rewardsIntroSeen = true))
    }
}
