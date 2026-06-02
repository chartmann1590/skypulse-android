package com.charles.skypulse.app.data.ads

import android.app.Activity
import android.util.Log
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Google's User Messaging Platform (UMP). On launch it refreshes consent info and shows the
 * consent form when required (e.g. GDPR regions), then reports whether ads may be requested.
 *
 * [onCanRequestAds] may be invoked more than once; callers must make ad initialization idempotent.
 */
@Singleton
class ConsentManager @Inject constructor() {

    fun gatherConsent(activity: Activity, onCanRequestAds: () -> Unit) {
        val consentInformation: ConsentInformation =
            UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "Consent form error: ${formError.message}")
                    }
                    if (consentInformation.canRequestAds()) onCanRequestAds()
                }
            },
            { requestError ->
                Log.w(TAG, "Consent info update failed: ${requestError.message}")
                // Fall back to any consent already on record.
                if (consentInformation.canRequestAds()) onCanRequestAds()
            },
        )

        // If consent was already resolved on a previous launch, ads can start right away.
        if (consentInformation.canRequestAds()) onCanRequestAds()
    }

    private companion object {
        const val TAG = "ConsentManager"
    }
}
