package com.charles.skypulse.app.ui.navigation

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charles.skypulse.app.data.ads.AdManager
import com.charles.skypulse.app.data.settings.SettingsDataStore
import com.charles.skypulse.app.domain.ads.AdRewardCalculator
import com.charles.skypulse.app.domain.ads.AdRewardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val settings: SettingsDataStore,
    private val rewardRepository: AdRewardRepository,
    private val adManager: AdManager,
) : ViewModel() {

    val onboarded: StateFlow<Boolean?> = settings.onboarded
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Re-evaluates ad-free state periodically so the banner reappears when ad-free time expires.
    private val ticker = flow {
        while (true) {
            emit(Unit)
            delay(15_000)
        }
    }

    val adFree: StateFlow<Boolean> = combine(rewardRepository.state, ticker) { state, _ ->
        AdRewardCalculator.isAdFree(state, System.currentTimeMillis())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Whether the one-time rewards explanation still needs to be shown. */
    val showRewardsIntro: StateFlow<Boolean> = rewardRepository.state
        .map { !it.rewardsIntroSeen }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun completeOnboarding() {
        viewModelScope.launch { settings.setOnboarded(true) }
    }

    fun markRewardsIntroSeen() {
        viewModelScope.launch { rewardRepository.markRewardsIntroSeen() }
    }

    fun maybeShowInterstitial(activity: Activity) = adManager.maybeShowInterstitial(activity)
}
