package com.charles.skypulse.app.data.firebase

import android.util.Log
import com.charles.skypulse.app.worker.NotificationHelper
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Receives FCM pushes and surfaces them as local notifications. The app's primary alerting
 * path remains the on-device WorkManager checks; FCM is additive (e.g. announcements).
 */
@AndroidEntryPoint
class SkyPulseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var notificationHelper: NotificationHelper

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title
            ?: message.data["title"]
            ?: "SkyPulse"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: return
        notificationHelper.notify(message.messageId.hashCode(), title, body)
    }

    override fun onNewToken(token: String) {
        // No backend to register with on Spark; record only a non-sensitive flag for diagnostics.
        // The token itself is never logged.
        Log.i(TAG, "Received a new FCM registration token")
        FirebaseCrashlytics.getInstance().setCustomKey("has_fcm_token", true)
    }

    companion object {
        private const val TAG = "SkyPulseFCM"
    }
}
