package com.charles.skypulse.app.domain.entitlement

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Gate for premium features. Billing is intentionally NOT implemented yet (see Plan §12);
 * this interface lets the rest of the app code against entitlements so a future
 * Google Play Billing implementation can drop in without touching feature code.
 *
 * TODO(monetization): add a BillingEntitlementRepository backed by Play Billing to unlock
 * premium themes, more saved alerts, custom alert sounds, and extra map styles.
 */
interface EntitlementRepository {
    val isPremium: Flow<Boolean>

    /** Max number of enabled alert rules on the current tier. */
    fun maxSavedAlerts(): Int

    /** Whether a named premium feature is currently unlocked. */
    fun isFeatureUnlocked(feature: PremiumFeature): Boolean
}

enum class PremiumFeature {
    PREMIUM_THEMES,
    EXTRA_MAP_STYLES,
    CUSTOM_ALERT_SOUNDS,
    UNLIMITED_ALERTS,
}

/** Free-tier implementation: everything premium is locked. */
class FreeEntitlementRepository(
    private val freeMaxAlerts: Int = DEFAULT_FREE_MAX_ALERTS,
) : EntitlementRepository {

    override val isPremium: Flow<Boolean> = flowOf(false)

    override fun maxSavedAlerts(): Int = freeMaxAlerts

    override fun isFeatureUnlocked(feature: PremiumFeature): Boolean = false

    companion object {
        const val DEFAULT_FREE_MAX_ALERTS = 4
    }
}
