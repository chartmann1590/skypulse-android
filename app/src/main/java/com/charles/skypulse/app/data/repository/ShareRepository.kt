package com.charles.skypulse.app.data.repository

import com.charles.skypulse.app.domain.model.Aircraft
import com.charles.skypulse.app.domain.model.DataSource
import com.charles.skypulse.app.domain.model.FlightRoute
import com.charles.skypulse.app.domain.model.RoutePoint
import com.charles.skypulse.app.domain.model.RouteProgress
import com.charles.skypulse.app.domain.model.SharedFlight
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Backs the "Share a flight" feature. Writes a one-off, immutable snapshot of a flight to
 * Cloud Firestore (after an anonymous sign-in) and hands back a public link to the website
 * viewer; also reads a shared snapshot back when the app is opened from a share link.
 *
 * Stays entirely on the Firebase Spark (free) tier: anonymous auth + a single small document.
 */
@Singleton
class ShareRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) {
    /**
     * Persists [flight] and returns a public web URL that renders it. Requires a known position
     * (the website plots the aircraft on a map). Throws on failure so callers can surface an error.
     */
    suspend fun shareFlight(flight: SharedFlight): String {
        val ac = flight.aircraft
        val lat = ac.latitude
        val lon = ac.longitude
        require(lat != null && lon != null) { "Cannot share a flight without a known position." }

        val uid = ensureSignedIn()

        val data = HashMap<String, Any?>()
        data["schema"] = SCHEMA_VERSION
        data["sharedBy"] = uid
        data["createdAt"] = FieldValue.serverTimestamp()
        // Aircraft snapshot
        data["callsign"] = ac.callsign
        data["displayName"] = ac.displayName
        data["hex"] = ac.hex
        data["lat"] = lat
        data["lon"] = lon
        data["altitudeFeet"] = ac.altitudeFeet
        data["speedKnots"] = ac.speedKnots
        data["headingDegrees"] = ac.headingDegrees
        data["verticalRate"] = ac.verticalRate
        data["originCountry"] = ac.originCountry
        data["typeCode"] = ac.typeCode
        data["onGround"] = ac.onGround
        data["lastSeenEpochSeconds"] = ac.lastSeenEpochSeconds
        data["source"] = ac.source.name
        // Route (optional)
        flight.route?.let { r ->
            data["airlineName"] = r.airlineName
            data["originCode"] = r.origin.code
            data["originName"] = r.origin.name
            data["originCity"] = r.origin.city
            data["originLat"] = r.origin.latitude
            data["originLon"] = r.origin.longitude
            data["destCode"] = r.destination.code
            data["destName"] = r.destination.name
            data["destCity"] = r.destination.city
            data["destLat"] = r.destination.latitude
            data["destLon"] = r.destination.longitude
        }
        // Progress (optional)
        flight.progress?.let { p ->
            data["fractionComplete"] = p.fractionComplete
            data["remainingNm"] = p.remainingNm
            data["etaMinutes"] = p.etaMinutes
        }

        val doc = firestore.collection(COLLECTION).document()
        doc.set(data).await()
        return shareUrl(doc.id)
    }

    /** Reads back a shared snapshot (e.g. when opened from a deep link). Returns null if missing. */
    suspend fun loadSharedFlight(shareId: String): SharedFlight? {
        val snap = firestore.collection(COLLECTION).document(shareId).get().await()
        if (!snap.exists()) return null

        val lat = snap.getDouble("lat") ?: return null
        val lon = snap.getDouble("lon") ?: return null
        val hex = snap.getString("hex")

        val aircraft = Aircraft(
            id = hex ?: shareId,
            callsign = snap.getString("callsign"),
            hex = hex,
            latitude = lat,
            longitude = lon,
            altitudeFeet = snap.getDouble("altitudeFeet"),
            speedKnots = snap.getDouble("speedKnots"),
            headingDegrees = snap.getDouble("headingDegrees"),
            verticalRate = snap.getDouble("verticalRate"),
            originCountry = snap.getString("originCountry"),
            lastSeenEpochSeconds = snap.getLong("lastSeenEpochSeconds"),
            source = DataSource.CACHE,
            typeCode = snap.getString("typeCode"),
            onGround = snap.getBoolean("onGround") ?: false,
            distanceNm = null,
        )

        val originCode = snap.getString("originCode")
        val route = if (originCode != null) {
            FlightRoute(
                callsign = aircraft.callsign?.trim().orEmpty(),
                origin = RoutePoint(
                    iata = null,
                    icao = originCode,
                    name = snap.getString("originName"),
                    city = snap.getString("originCity"),
                    latitude = snap.getDouble("originLat") ?: 0.0,
                    longitude = snap.getDouble("originLon") ?: 0.0,
                    countryName = null,
                ),
                destination = RoutePoint(
                    iata = null,
                    icao = snap.getString("destCode"),
                    name = snap.getString("destName"),
                    city = snap.getString("destCity"),
                    latitude = snap.getDouble("destLat") ?: 0.0,
                    longitude = snap.getDouble("destLon") ?: 0.0,
                    countryName = null,
                ),
                airlineName = snap.getString("airlineName"),
            )
        } else {
            null
        }

        val progress = snap.getDouble("remainingNm")?.let { rem ->
            RouteProgress(
                fractionComplete = snap.getDouble("fractionComplete") ?: 0.0,
                remainingNm = rem,
                etaMinutes = snap.getLong("etaMinutes")?.toInt(),
            )
        }

        return SharedFlight(aircraft, route, progress)
    }

    /** Ensures we have an anonymous Firebase user (required by the Firestore create rule). */
    private suspend fun ensureSignedIn(): String {
        auth.currentUser?.let { return it.uid }
        val result = auth.signInAnonymously().await()
        return result.user?.uid ?: error("Anonymous sign-in failed")
    }

    companion object {
        const val COLLECTION = "shared_flights"
        const val SCHEMA_VERSION = 1
        const val SHARE_BASE_URL = "https://chartmann1590.github.io/skypulse-android/flight.html"

        fun shareUrl(shareId: String): String = "$SHARE_BASE_URL?id=$shareId"
    }
}
