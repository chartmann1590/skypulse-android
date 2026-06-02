package com.charles.skypulse.app.data.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.charles.skypulse.app.data.firebase.Analytics
import com.charles.skypulse.app.domain.ads.AdRewardCalculator
import com.charles.skypulse.app.domain.ads.AdRewardRepository
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Central orchestrator for AdMob. Initializes the SDK (only after UMP consent), preloads the
 * interstitial and rewarded ads, and exposes show methods. Every show path is a no-op while the
 * user has active ad-free time, or before the SDK is initialized.
 */
@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rewardRepository: AdRewardRepository,
    private val analytics: Analytics,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val initialized = AtomicBoolean(false)

    private var interstitial: InterstitialAd? = null
    private var rewarded: RewardedAd? = null
    private var lastInterstitialShownAt = 0L

    /** Idempotent — safe to call multiple times (UMP may report "can request ads" more than once). */
    fun initialize() {
        if (!initialized.compareAndSet(false, true)) return
        MobileAds.initialize(context) {}
        loadInterstitial()
        loadRewarded()
    }

    private fun isAdFree(): Boolean =
        AdRewardCalculator.isAdFree(rewardRepository.state.value, System.currentTimeMillis())

    // ── Interstitial ──────────────────────────────────────────────────────────

    private fun loadInterstitial() {
        if (interstitial != null) return
        InterstitialAd.load(
            context,
            AdConfig.interstitialId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Interstitial failed to load: ${error.message}")
                    interstitial = null
                }
            },
        )
    }

    /** Randomly shows an interstitial, throttled and suppressed while ad-free. */
    fun maybeShowInterstitial(activity: Activity) {
        if (!initialized.get() || isAdFree()) return
        val now = System.currentTimeMillis()
        if (now - lastInterstitialShownAt < MIN_INTERSTITIAL_GAP_MS) return
        if (Random.nextFloat() > INTERSTITIAL_PROBABILITY) return
        val ad = interstitial ?: run { loadInterstitial(); return }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { interstitial = null; loadInterstitial() }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitial = null; loadInterstitial()
            }
        }
        lastInterstitialShownAt = now
        analytics.logInterstitialShown()
        ad.show(activity)
    }

    // ── Rewarded ──────────────────────────────────────────────────────────────

    private fun loadRewarded() {
        if (rewarded != null) return
        RewardedAd.load(
            context,
            AdConfig.rewardedId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) { rewarded = ad }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Rewarded failed to load: ${error.message}")
                    rewarded = null
                }
            },
        )
    }

    val rewardedReady: Boolean get() = rewarded != null

    /**
     * Shows a rewarded ad. [onEarned] runs after the credit has been persisted; [onUnavailable]
     * runs if no ad is loaded yet or it fails to show.
     */
    fun showRewarded(activity: Activity, onEarned: () -> Unit, onUnavailable: () -> Unit) {
        if (!initialized.get()) { onUnavailable(); return }
        val ad = rewarded ?: run { loadRewarded(); onUnavailable(); return }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { rewarded = null; loadRewarded() }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "Rewarded failed to show: ${error.message}")
                rewarded = null; loadRewarded(); onUnavailable()
            }
        }
        ad.show(activity) { _ ->
            scope.launch {
                rewardRepository.awardCredit()
                onEarned()
            }
        }
    }

    private companion object {
        const val TAG = "AdManager"
        const val INTERSTITIAL_PROBABILITY = 0.15f
        const val MIN_INTERSTITIAL_GAP_MS = 3 * 60 * 1000L
    }
}
