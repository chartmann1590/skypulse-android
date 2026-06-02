package com.charles.skypulse.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.charles.skypulse.app.data.location.LocationProvider
import com.charles.skypulse.app.data.repository.AircraftRepository
import com.charles.skypulse.app.data.repository.AlertRepository
import com.charles.skypulse.app.data.repository.SavedRepository
import com.charles.skypulse.app.domain.model.Aircraft
import com.charles.skypulse.app.domain.model.AlertRule
import com.charles.skypulse.app.domain.model.AlertType
import com.charles.skypulse.app.domain.util.GeoUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Periodically evaluates enabled local alert rules against a fresh aircraft snapshot and
 * posts on-device notifications. Runs entirely locally — no backend (Plan §8).
 */
@HiltWorker
class AlertCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val alertRepository: AlertRepository,
    private val aircraftRepository: AircraftRepository,
    private val savedRepository: SavedRepository,
    private val locationProvider: LocationProvider,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val rules = alertRepository.getEnabled()
        if (rules.isEmpty()) return Result.success()

        val location = locationProvider.currentLocation()
        val lat = location?.latitude ?: rules.firstNotNullOfOrNull { it.anchorLat } ?: return Result.success()
        val lon = location?.longitude ?: rules.firstNotNullOfOrNull { it.anchorLon } ?: return Result.success()

        val maxRadius = rules.maxOf { it.radiusNm }.coerceIn(5.0, 250.0)
        aircraftRepository.refresh(lat, lon, maxRadius.toInt(), force = true)
        val aircraft = aircraftRepository.feed.value.aircraft
        if (aircraft.isEmpty()) return Result.success()

        rules.forEach { rule -> evaluate(rule, aircraft, lat, lon) }
        return Result.success()
    }

    private suspend fun evaluate(rule: AlertRule, aircraft: List<Aircraft>, lat: Double, lon: Double) {
        when (rule.type) {
            AlertType.AIRCRAFT_ENTERS_AREA -> {
                val anchorLat = rule.anchorLat ?: lat
                val anchorLon = rule.anchorLon ?: lon
                val inArea = aircraft.filter { ac ->
                    val aLat = ac.latitude; val aLon = ac.longitude
                    aLat != null && aLon != null &&
                        GeoUtils.haversineNm(anchorLat, anchorLon, aLat, aLon) <= rule.radiusNm
                }
                if (inArea.isNotEmpty()) {
                    notify(
                        rule, "Aircraft in your area",
                        "${inArea.size} aircraft within ${rule.radiusNm.toInt()} NM right now.",
                    )
                }
            }

            AlertType.SPECIFIC_CALLSIGN -> {
                val target = rule.callsign?.trim()?.uppercase().orEmpty()
                if (target.isNotEmpty()) {
                    val match = aircraft.firstOrNull {
                        it.callsign?.trim()?.uppercase()?.contains(target) == true
                    }
                    if (match != null) {
                        notify(rule, "${match.displayName} is nearby", "Matched your watched callsign.")
                    }
                }
            }

            AlertType.LOW_ALTITUDE_NEARBY -> {
                val low = aircraft.filter { ac ->
                    val alt = ac.altitudeFeet; val aLat = ac.latitude; val aLon = ac.longitude
                    alt != null && aLat != null && aLon != null && !ac.onGround &&
                        alt <= rule.altitudeThresholdFeet &&
                        GeoUtils.haversineNm(lat, lon, aLat, aLon) <= rule.radiusNm
                }
                if (low.isNotEmpty()) {
                    notify(
                        rule, "Low-altitude flight nearby",
                        "${low.first().displayName} below ${rule.altitudeThresholdFeet.toInt()} ft.",
                    )
                }
            }

            AlertType.AIRPORT_ACTIVITY_NEARBY -> {
                val airports = savedRepository.savedAirports.first()
                airports.forEach { airport ->
                    val near = aircraft.count { ac ->
                        val aLat = ac.latitude; val aLon = ac.longitude
                        aLat != null && aLon != null &&
                            GeoUtils.haversineNm(airport.lat, airport.lon, aLat, aLon) <= rule.radiusNm
                    }
                    if (near > 0) {
                        notify(
                            rule, "Activity near ${airport.code}",
                            "$near aircraft within ${rule.radiusNm.toInt()} NM of ${airport.name}.",
                            extraId = airport.airportId,
                        )
                    }
                }
            }
        }
    }

    private fun notify(rule: AlertRule, title: String, body: String, extraId: Int = 0) {
        val id = (rule.type.name.hashCode() * 31 + extraId)
        notificationHelper.notify(id, title, body)
    }
}
