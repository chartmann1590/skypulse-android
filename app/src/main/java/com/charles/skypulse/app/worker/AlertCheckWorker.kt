package com.charles.skypulse.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.charles.skypulse.app.data.local.dao.WatchedFlightDao
import com.charles.skypulse.app.data.local.entity.WatchedFlightStateEntity
import com.charles.skypulse.app.data.location.LocationProvider
import com.charles.skypulse.app.data.remote.RemoteAircraftDataSource
import com.charles.skypulse.app.data.repository.AircraftRepository
import com.charles.skypulse.app.data.repository.AlertRepository
import com.charles.skypulse.app.data.repository.RouteRepository
import com.charles.skypulse.app.data.repository.SavedRepository
import com.charles.skypulse.app.domain.model.Aircraft
import com.charles.skypulse.app.domain.model.AlertRule
import com.charles.skypulse.app.domain.model.AlertType
import com.charles.skypulse.app.domain.util.GeoUtils
import com.charles.skypulse.app.domain.util.RouteEstimator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Periodically evaluates enabled local alert rules and posts on-device notifications.
 * Runs entirely locally — no backend (Plan §8). Departure/landing detection for saved
 * flights is best-effort (estimated from live ADS-B + free route data).
 */
@HiltWorker
class AlertCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val alertRepository: AlertRepository,
    private val aircraftRepository: AircraftRepository,
    private val savedRepository: SavedRepository,
    private val routeRepository: RouteRepository,
    private val remoteDataSource: RemoteAircraftDataSource,
    private val watchedFlightDao: WatchedFlightDao,
    private val locationProvider: LocationProvider,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val rules = alertRepository.getEnabled().associateBy { it.type }
        if (rules.isEmpty()) return Result.success()

        evaluateAreaRules(rules)
        evaluateSavedFlightRules(rules)
        return Result.success()
    }

    /** Location/area-based rules (entering area, callsign nearby, low altitude, airport). */
    private suspend fun evaluateAreaRules(rules: Map<AlertType, AlertRule>) {
        val locationRules = rules.values.filter {
            it.type in setOf(
                AlertType.AIRCRAFT_ENTERS_AREA,
                AlertType.SPECIFIC_CALLSIGN,
                AlertType.LOW_ALTITUDE_NEARBY,
                AlertType.AIRPORT_ACTIVITY_NEARBY,
            )
        }
        if (locationRules.isEmpty()) return

        val location = locationProvider.currentLocation()
        val lat = location?.latitude ?: locationRules.firstNotNullOfOrNull { it.anchorLat } ?: return
        val lon = location?.longitude ?: locationRules.firstNotNullOfOrNull { it.anchorLon } ?: return

        val maxRadius = locationRules.maxOf { it.radiusNm }.coerceIn(5.0, 250.0)
        aircraftRepository.refresh(lat, lon, maxRadius.toInt(), force = true)
        val aircraft = aircraftRepository.feed.value.aircraft
        if (aircraft.isEmpty()) return

        locationRules.forEach { rule -> evaluateArea(rule, aircraft, lat, lon) }
    }

    private suspend fun evaluateArea(rule: AlertRule, aircraft: List<Aircraft>, lat: Double, lon: Double) {
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
                    notify(rule, "Aircraft in your area", "${inArea.size} aircraft within ${rule.radiusNm.toInt()} NM right now.")
                }
            }

            AlertType.SPECIFIC_CALLSIGN -> {
                val target = rule.callsign?.trim()?.uppercase().orEmpty()
                if (target.isNotEmpty()) {
                    val match = aircraft.firstOrNull {
                        it.callsign?.trim()?.uppercase()?.contains(target) == true
                    }
                    if (match != null) notify(rule, "${match.displayName} is nearby", "Matched your watched callsign.")
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
                    notify(rule, "Low-altitude flight nearby", "${low.first().displayName} below ${rule.altitudeThresholdFeet.toInt()} ft.")
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
                        notify(rule, "Activity near ${airport.code}", "$near aircraft within ${rule.radiusNm.toInt()} NM of ${airport.name}.", extraId = airport.airportId)
                    }
                }
            }

            else -> Unit
        }
    }

    /** Departure / landing detection for SAVED flights (located anywhere via ADSB.lol). */
    private suspend fun evaluateSavedFlightRules(rules: Map<AlertType, AlertRule>) {
        val wantDeparted = rules[AlertType.FLIGHT_DEPARTED]?.enabled == true
        val wantLanding = rules[AlertType.FLIGHT_LANDING_SOON]?.enabled == true
        if (!wantDeparted && !wantLanding) return

        val saved = savedRepository.savedAircraft.first()
        if (saved.isEmpty()) return
        val now = System.currentTimeMillis()

        for (s in saved) {
            val live = s.hex?.let { remoteDataSource.fetchByHex(it) }
                ?: s.callsign?.let { remoteDataSource.fetchByCallsign(it.trim()) }
                ?: continue

            val prev = watchedFlightDao.get(s.id)
            var departedAt = prev?.departedNotifiedAtMs ?: 0L
            var landingAt = prev?.landingNotifiedAtMs ?: 0L

            // Departure: was on ground, now airborne.
            if (wantDeparted && prev?.lastOnGround == true && !live.onGround &&
                now - departedAt > DEDUPE_MS
            ) {
                val route = routeRepository.routeForCallsign(live.callsign)
                val from = route?.origin?.code?.let { " from $it" } ?: ""
                notifyId(("dep" + s.id).hashCode(), "${live.displayName} has departed", "Now airborne$from.")
                departedAt = now
            }

            // Landing soon: near destination and descending.
            if (wantLanding && !live.onGround && now - landingAt > DEDUPE_MS) {
                val route = routeRepository.routeForCallsign(live.callsign)
                val dest = route?.destination
                val lat = live.latitude; val lon = live.longitude
                if (dest != null && lat != null && lon != null) {
                    val remaining = GeoUtils.haversineNm(lat, lon, dest.latitude, dest.longitude)
                    val descending = (live.verticalRate ?: 0.0) < -100.0
                    if (remaining <= LANDING_RADIUS_NM && descending) {
                        val eta = RouteEstimator.estimate(route, lat, lon, live.speedKnots).etaMinutes
                        val etaText = eta?.let { " (~$it min)" } ?: ""
                        notifyId(("land" + s.id).hashCode(), "${live.displayName} is landing soon", "Approaching ${dest.code}$etaText.")
                        landingAt = now
                    }
                }
            }

            watchedFlightDao.upsert(
                WatchedFlightStateEntity(
                    id = s.id,
                    lastOnGround = live.onGround,
                    departedNotifiedAtMs = departedAt,
                    landingNotifiedAtMs = landingAt,
                    updatedAtMs = now,
                ),
            )
        }
    }

    private fun notify(rule: AlertRule, title: String, body: String, extraId: Int = 0) {
        notificationHelper.notify(rule.type.name.hashCode() * 31 + extraId, title, body)
    }

    private fun notifyId(id: Int, title: String, body: String) {
        notificationHelper.notify(id, title, body)
    }

    companion object {
        private const val LANDING_RADIUS_NM = 40.0
        private const val DEDUPE_MS = 2 * 60 * 60 * 1000L // don't repeat within 2h
    }
}
