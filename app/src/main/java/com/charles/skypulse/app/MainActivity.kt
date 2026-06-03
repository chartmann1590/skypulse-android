package com.charles.skypulse.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.charles.skypulse.app.data.ads.AdManager
import com.charles.skypulse.app.data.ads.ConsentManager
import com.charles.skypulse.app.ui.navigation.SkyPulseNavHost
import com.charles.skypulse.app.ui.theme.SkyColors
import com.charles.skypulse.app.ui.theme.SkyPulseTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var consentManager: ConsentManager
    @Inject lateinit var adManager: AdManager

    // Holds the shared-flight id from a deep link so Compose can react to it (incl. onNewIntent).
    private var pendingShareId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        pendingShareId = extractShareId(intent)

        // Gather UMP consent, then initialize ads. AdManager.initialize() is idempotent.
        consentManager.gatherConsent(this) { adManager.initialize() }

        setContent {
            SkyPulseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SkyColors.PitchBlack,
                ) {
                    SkyPulseNavHost(sharedFlightId = pendingShareId)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractShareId(intent)?.let { pendingShareId = it }
    }

    /** Pulls the share id from a skypulse://flight?id=… (or website ?id=…) deep link. */
    private fun extractShareId(intent: Intent?): String? {
        val uri: Uri = intent?.data ?: return null
        return uri.getQueryParameter("id")?.takeIf { it.isNotBlank() }
    }
}
