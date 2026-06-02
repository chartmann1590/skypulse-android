package com.charles.skypulse.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.charles.skypulse.app.domain.model.AltitudeUnit
import com.charles.skypulse.app.domain.model.DistanceUnit
import com.charles.skypulse.app.domain.model.SpeedUnit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "skypulse_settings")

data class SkySettings(
    val refreshIntervalSeconds: Int = 10,
    val distanceUnit: DistanceUnit = DistanceUnit.MILES,
    val altitudeUnit: AltitudeUnit = AltitudeUnit.FEET,
    val speedUnit: SpeedUnit = SpeedUnit.KNOTS,
    val backgroundAlertsEnabled: Boolean = false,
)

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val REFRESH = intPreferencesKey("refresh_interval_s")
        val DISTANCE = stringPreferencesKey("distance_unit")
        val ALTITUDE = stringPreferencesKey("altitude_unit")
        val SPEED = stringPreferencesKey("speed_unit")
        val BG_ALERTS = booleanPreferencesKey("bg_alerts")
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val OPENFLIGHTS_IMPORTED = booleanPreferencesKey("openflights_imported")
    }

    val settings: Flow<SkySettings> = context.dataStore.data.map { p ->
        SkySettings(
            refreshIntervalSeconds = p[Keys.REFRESH] ?: 10,
            distanceUnit = p[Keys.DISTANCE]?.let { runCatching { DistanceUnit.valueOf(it) }.getOrNull() }
                ?: DistanceUnit.MILES,
            altitudeUnit = p[Keys.ALTITUDE]?.let { runCatching { AltitudeUnit.valueOf(it) }.getOrNull() }
                ?: AltitudeUnit.FEET,
            speedUnit = p[Keys.SPEED]?.let { runCatching { SpeedUnit.valueOf(it) }.getOrNull() }
                ?: SpeedUnit.KNOTS,
            backgroundAlertsEnabled = p[Keys.BG_ALERTS] ?: false,
        )
    }

    val onboarded: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDED] ?: false }

    suspend fun setRefreshInterval(seconds: Int) =
        context.dataStore.edit { it[Keys.REFRESH] = seconds }

    suspend fun setDistanceUnit(unit: DistanceUnit) =
        context.dataStore.edit { it[Keys.DISTANCE] = unit.name }

    suspend fun setAltitudeUnit(unit: AltitudeUnit) =
        context.dataStore.edit { it[Keys.ALTITUDE] = unit.name }

    suspend fun setSpeedUnit(unit: SpeedUnit) =
        context.dataStore.edit { it[Keys.SPEED] = unit.name }

    suspend fun setBackgroundAlerts(enabled: Boolean) =
        context.dataStore.edit { it[Keys.BG_ALERTS] = enabled }

    suspend fun setOnboarded(value: Boolean) =
        context.dataStore.edit { it[Keys.ONBOARDED] = value }

    suspend fun isOpenFlightsImported(): Boolean =
        context.dataStore.data.first()[Keys.OPENFLIGHTS_IMPORTED] ?: false

    suspend fun setOpenFlightsImported(value: Boolean) =
        context.dataStore.edit { it[Keys.OPENFLIGHTS_IMPORTED] = value }
}
