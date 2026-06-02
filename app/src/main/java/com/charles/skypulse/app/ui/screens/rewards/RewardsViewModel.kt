package com.charles.skypulse.app.ui.screens.rewards

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charles.skypulse.app.data.ads.AdManager
import com.charles.skypulse.app.data.firebase.Analytics
import com.charles.skypulse.app.domain.ads.AdRewardCalculator
import com.charles.skypulse.app.domain.ads.AdRewardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RewardsUiState(
    val credits: Int = 0,
    val creditsEarnedToday: Int = 0,
    val adFree: Boolean = false,
    val adFreeRemainingMs: Long = 0L,
    val canEarn: Boolean = false,
    val canSpend: Boolean = false,
    val introSeen: Boolean = true,
)

@HiltViewModel
class RewardsViewModel @Inject constructor(
    private val rewardRepository: AdRewardRepository,
    private val adManager: AdManager,
    private val analytics: Analytics,
) : ViewModel() {

    // Emits every second so the ad-free countdown stays live.
    private val ticker = flow {
        while (true) {
            emit(Unit)
            delay(1000)
        }
    }

    val ui: StateFlow<RewardsUiState> = combine(rewardRepository.state, ticker) { state, _ ->
        val now = System.currentTimeMillis()
        val today = AdRewardCalculator.dayKey(now)
        RewardsUiState(
            credits = state.credits,
            creditsEarnedToday = state.creditsEarnedToday,
            adFree = AdRewardCalculator.isAdFree(state, now),
            adFreeRemainingMs = AdRewardCalculator.adFreeRemainingMs(state, now),
            canEarn = AdRewardCalculator.canEarn(state, today),
            canSpend = AdRewardCalculator.canSpend(state, today),
            introSeen = state.rewardsIntroSeen,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RewardsUiState())

    fun onScreenOpened() = analytics.logRewardsOpened()

    val rewardedReady: Boolean get() = adManager.rewardedReady

    fun watchAd(activity: Activity, onResult: (earned: Boolean) -> Unit) {
        adManager.showRewarded(
            activity = activity,
            onEarned = { onResult(true) },
            onUnavailable = { onResult(false) },
        )
    }

    fun spendCredit() {
        viewModelScope.launch { rewardRepository.spendCredit() }
    }

    fun markIntroSeen() {
        viewModelScope.launch { rewardRepository.markRewardsIntroSeen() }
    }
}
