package com.charles.skypulse.app.domain.ads

/**
 * Snapshot of the user's ad-reward state. Pure data — all transitions live in
 * [AdRewardCalculator] so they can be unit-tested without Android or DataStore.
 *
 * Daily counters reset at local midnight (see [AdRewardCalculator.rolledOver]). [adFreeUntilEpochMs]
 * intentionally persists across midnight, so ad-free time activated late at night isn't lost.
 */
data class RewardState(
    /** Unspent credits available to convert into ad-free time. Resets daily. */
    val credits: Int = 0,
    /** Epoch millis until which ads are suppressed (0 = not ad-free). */
    val adFreeUntilEpochMs: Long = 0L,
    /** Credits earned so far today (enforces the daily earning cap). */
    val creditsEarnedToday: Int = 0,
    /** Seconds of ad-free time granted so far today (enforces the daily ad-free ceiling). */
    val adFreeSecondsGrantedToday: Int = 0,
    /** Local day this state belongs to, as `yyyy-MM-dd`; used to detect rollover. */
    val dayKey: String = "",
    /** Whether the one-time rewards explanation has been shown. */
    val rewardsIntroSeen: Boolean = false,
)
