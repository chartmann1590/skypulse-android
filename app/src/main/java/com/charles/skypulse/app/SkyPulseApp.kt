package com.charles.skypulse.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.charles.skypulse.app.data.firebase.RemoteConfigProvider
import com.charles.skypulse.app.data.local.OpenFlightsImporter
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration as OsmConfig
import javax.inject.Inject

@HiltAndroidApp
class SkyPulseApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var openFlightsImporter: OpenFlightsImporter
    @Inject lateinit var remoteConfigProvider: RemoteConfigProvider

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // osmdroid: set a user-agent and cache path before any MapView is created.
        OsmConfig.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = filesDir
            osmdroidTileCache = cacheDir
        }

        // Warm Remote Config (falls back to defaults if offline).
        remoteConfigProvider.fetchAndActivate()

        // Import bundled OpenFlights data into Room on first launch.
        appScope.launch { openFlightsImporter.importIfNeeded() }
    }
}
