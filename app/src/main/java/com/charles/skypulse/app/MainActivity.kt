package com.charles.skypulse.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
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

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Gather UMP consent, then initialize ads. AdManager.initialize() is idempotent.
        consentManager.gatherConsent(this) { adManager.initialize() }

        setContent {
            SkyPulseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SkyColors.PitchBlack,
                ) {
                    SkyPulseNavHost()
                }
            }
        }
    }
}
