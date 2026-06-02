package com.charles.skypulse.app

import com.charles.skypulse.app.domain.ads.AdRewardCalculator
import com.charles.skypulse.app.domain.ads.RewardState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdRewardCalculatorTest {

    private val today = "2026-06-02"
    private val tomorrow = "2026-06-03"
    private val now = 1_000_000_000_000L

    private fun freshState() = RewardState(dayKey = today)

    @Test
    fun earnCredit_incrementsCreditsAndDailyCounter() {
        val s = AdRewardCalculator.earnCredit(freshState(), today)
        assertEquals(1, s.credits)
        assertEquals(1, s.creditsEarnedToday)
    }

    @Test
    fun earnCredit_capsAtDailyMax() {
        var s = freshState()
        repeat(10) { s = AdRewardCalculator.earnCredit(s, today) }
        assertEquals(AdRewardCalculator.MAX_CREDITS_PER_DAY, s.credits)
        assertEquals(AdRewardCalculator.MAX_CREDITS_PER_DAY, s.creditsEarnedToday)
        assertFalse(AdRewardCalculator.canEarn(s, today))
    }

    @Test
    fun spendCredit_withNoCredits_isNoOp() {
        val s = freshState()
        val result = AdRewardCalculator.spendCredit(s, now, today)
        assertEquals(s, result)
        assertFalse(AdRewardCalculator.canSpend(s, today))
    }

    @Test
    fun spendCredit_grantsThirtyMinutesAndConsumesCredit() {
        val s = AdRewardCalculator.earnCredit(freshState(), today)
        val result = AdRewardCalculator.spendCredit(s, now, today)
        assertEquals(0, result.credits)
        assertEquals(AdRewardCalculator.SECONDS_PER_CREDIT, result.adFreeSecondsGrantedToday)
        assertEquals(now + AdRewardCalculator.SECONDS_PER_CREDIT * 1000L, result.adFreeUntilEpochMs)
    }

    @Test
    fun spendCredit_stacksOntoActiveAdFreeWindow() {
        var s = freshState().copy(credits = 2)
        s = AdRewardCalculator.spendCredit(s, now, today)
        val firstEnd = s.adFreeUntilEpochMs
        // Spend again while still ad-free (a little later, but before the window ends).
        s = AdRewardCalculator.spendCredit(s, now + 1000L, today)
        assertEquals(firstEnd + AdRewardCalculator.SECONDS_PER_CREDIT * 1000L, s.adFreeUntilEpochMs)
        assertEquals(2 * AdRewardCalculator.SECONDS_PER_CREDIT, s.adFreeSecondsGrantedToday)
    }

    @Test
    fun spendCredit_clampsToDailyAdFreeCeiling() {
        // 10 minutes (600s) left before the 4h/day ceiling, with a credit to spend.
        val remaining = 600
        val s = freshState().copy(
            credits = 1,
            adFreeSecondsGrantedToday = AdRewardCalculator.MAX_AD_FREE_PER_DAY_SECONDS - remaining,
        )
        val result = AdRewardCalculator.spendCredit(s, now, today)
        // Only the remaining 600s are granted (not a full 30 min), and the credit is consumed.
        assertEquals(0, result.credits)
        assertEquals(AdRewardCalculator.MAX_AD_FREE_PER_DAY_SECONDS, result.adFreeSecondsGrantedToday)
        assertEquals(now + remaining * 1000L, result.adFreeUntilEpochMs)
        assertFalse(AdRewardCalculator.canSpend(result, today))
    }

    @Test
    fun spendCredit_atCeiling_isNoOp() {
        val s = freshState().copy(
            credits = 3,
            adFreeSecondsGrantedToday = AdRewardCalculator.MAX_AD_FREE_PER_DAY_SECONDS,
        )
        val result = AdRewardCalculator.spendCredit(s, now, today)
        assertEquals(s, result)
    }

    @Test
    fun rollover_resetsDailyCountersAndCredits_preservesAdFree() {
        val adFreeUntil = now + 5 * 60 * 1000L
        val yesterday = RewardState(
            credits = 4,
            adFreeUntilEpochMs = adFreeUntil,
            creditsEarnedToday = 6,
            adFreeSecondsGrantedToday = 3600,
            dayKey = today,
            rewardsIntroSeen = true,
        )
        val rolled = AdRewardCalculator.rolledOver(yesterday, tomorrow)
        assertEquals(0, rolled.credits)
        assertEquals(0, rolled.creditsEarnedToday)
        assertEquals(0, rolled.adFreeSecondsGrantedToday)
        assertEquals(tomorrow, rolled.dayKey)
        assertEquals(adFreeUntil, rolled.adFreeUntilEpochMs)
        assertTrue(rolled.rewardsIntroSeen)
    }

    @Test
    fun rollover_sameDay_isUnchanged() {
        val s = freshState().copy(credits = 3, creditsEarnedToday = 3)
        assertEquals(s, AdRewardCalculator.rolledOver(s, today))
    }

    @Test
    fun earnCredit_appliesRolloverFromPreviousDay() {
        val yesterday = RewardState(credits = 6, creditsEarnedToday = 6, dayKey = today)
        // A new day should reset the cap, allowing earning again.
        val s = AdRewardCalculator.earnCredit(yesterday, tomorrow)
        assertEquals(1, s.credits)
        assertEquals(1, s.creditsEarnedToday)
        assertEquals(tomorrow, s.dayKey)
    }

    @Test
    fun isAdFree_andRemaining_boundaries() {
        val s = RewardState(adFreeUntilEpochMs = now + 60_000L)
        assertTrue(AdRewardCalculator.isAdFree(s, now))
        assertEquals(60_000L, AdRewardCalculator.adFreeRemainingMs(s, now))
        assertFalse(AdRewardCalculator.isAdFree(s, now + 60_000L))
        assertEquals(0L, AdRewardCalculator.adFreeRemainingMs(s, now + 120_000L))
    }

    @Test
    fun sixCreditsYieldsThreeHoursUnderFourHourCeiling() {
        var s = freshState()
        repeat(AdRewardCalculator.MAX_CREDITS_PER_DAY) { s = AdRewardCalculator.earnCredit(s, today) }
        repeat(AdRewardCalculator.MAX_CREDITS_PER_DAY) { s = AdRewardCalculator.spendCredit(s, now, today) }
        // 6 * 30 min = 3 hours, which is under the 4-hour daily ceiling.
        assertEquals(6 * AdRewardCalculator.SECONDS_PER_CREDIT, s.adFreeSecondsGrantedToday)
        assertTrue(s.adFreeSecondsGrantedToday < AdRewardCalculator.MAX_AD_FREE_PER_DAY_SECONDS)
        assertEquals(0, s.credits)
    }
}
