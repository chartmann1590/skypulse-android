package com.charles.skypulse.app.domain.ads

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.min

/**
 * Pure, deterministic transitions for the ad-reward / ad-free system. Every function takes the
 * current [RewardState] plus an explicit `now` (epoch millis) and/or `today` (local day key), so
 * the logic is fully unit-testable with no Android, time, or storage dependencies.
 *
 * Economy (product decision):
 *  - Watching a rewarded ad earns 1 credit, up to [MAX_CREDITS_PER_DAY] per day.
 *  - Spending 1 credit grants [MINUTES_PER_CREDIT] minutes of ad-free time.
 *  - A hard ceiling of [MAX_AD_FREE_PER_DAY_SECONDS] of ad-free time may be granted per day.
 *  - Daily counters (and the credit bank) reset at local midnight.
 */
object AdRewardCalculator {

    const val MAX_CREDITS_PER_DAY = 6
    const val MINUTES_PER_CREDIT = 30
    const val SECONDS_PER_CREDIT = MINUTES_PER_CREDIT * 60
    const val MAX_AD_FREE_PER_DAY_SECONDS = 4 * 60 * 60 // 4 hours

    private val DAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /** Local day key (`yyyy-MM-dd`) for the given instant. */
    fun dayKey(nowEpochMs: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        DAY_FORMAT.format(Instant.ofEpochMilli(nowEpochMs).atZone(zoneId).toLocalDate())

    /**
     * Returns [state] reset for [today] if a new day has begun, otherwise [state] unchanged.
     * Resets the credit bank and both daily counters; preserves any active [RewardState.adFreeUntilEpochMs].
     */
    fun rolledOver(state: RewardState, today: String): RewardState =
        if (state.dayKey == today) {
            state
        } else {
            state.copy(
                credits = 0,
                creditsEarnedToday = 0,
                adFreeSecondsGrantedToday = 0,
                dayKey = today,
            )
        }

    fun canEarn(state: RewardState, today: String): Boolean =
        rolledOver(state, today).creditsEarnedToday < MAX_CREDITS_PER_DAY

    fun canSpend(state: RewardState, today: String): Boolean {
        val s = rolledOver(state, today)
        return s.credits >= 1 && s.adFreeSecondsGrantedToday < MAX_AD_FREE_PER_DAY_SECONDS
    }

    /** Award one credit for watching a rewarded ad, respecting the daily earning cap. */
    fun earnCredit(state: RewardState, today: String): RewardState {
        val s = rolledOver(state, today)
        if (s.creditsEarnedToday >= MAX_CREDITS_PER_DAY) return s
        return s.copy(
            credits = s.credits + 1,
            creditsEarnedToday = s.creditsEarnedToday + 1,
        )
    }

    /**
     * Spend one credit to extend ad-free time by up to [MINUTES_PER_CREDIT] minutes, clamped so the
     * total granted today never exceeds [MAX_AD_FREE_PER_DAY_SECONDS]. No-op if there are no credits
     * or the daily ceiling is already reached. Ad-free time stacks onto any still-active window.
     */
    fun spendCredit(state: RewardState, nowEpochMs: Long, today: String): RewardState {
        val s = rolledOver(state, today)
        if (s.credits < 1) return s
        val remainingCapSeconds = MAX_AD_FREE_PER_DAY_SECONDS - s.adFreeSecondsGrantedToday
        if (remainingCapSeconds <= 0) return s
        val grantSeconds = min(SECONDS_PER_CREDIT, remainingCapSeconds)
        val base = max(nowEpochMs, s.adFreeUntilEpochMs)
        return s.copy(
            credits = s.credits - 1,
            adFreeUntilEpochMs = base + grantSeconds * 1000L,
            adFreeSecondsGrantedToday = s.adFreeSecondsGrantedToday + grantSeconds,
        )
    }

    fun isAdFree(state: RewardState, nowEpochMs: Long): Boolean =
        state.adFreeUntilEpochMs > nowEpochMs

    fun adFreeRemainingMs(state: RewardState, nowEpochMs: Long): Long =
        max(0L, state.adFreeUntilEpochMs - nowEpochMs)
}
