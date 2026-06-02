package com.charles.skypulse.app.domain.util

import com.charles.skypulse.app.domain.model.AltitudeUnit
import com.charles.skypulse.app.domain.model.DistanceUnit
import com.charles.skypulse.app.domain.model.SpeedUnit

/** Conversions between API base units and user-selected display units. */
object UnitConverters {

    // --- Raw provider conversions (OpenSky is metric) ---
    const val METERS_TO_FEET = 3.28084
    const val MS_TO_KNOTS = 1.94384
    const val MS_TO_FPM = 196.8504 // metres/sec -> feet/min

    fun metersToFeet(m: Double) = m * METERS_TO_FEET
    fun msToKnots(ms: Double) = ms * MS_TO_KNOTS
    fun msToFpm(ms: Double) = ms * MS_TO_FPM

    // --- Display conversions from base units ---
    fun altitude(feet: Double, unit: AltitudeUnit): Double = when (unit) {
        AltitudeUnit.FEET -> feet
        AltitudeUnit.METERS -> feet / METERS_TO_FEET
    }

    fun speed(knots: Double, unit: SpeedUnit): Double = when (unit) {
        SpeedUnit.KNOTS -> knots
        SpeedUnit.MPH -> knots * 1.15078
    }

    fun distanceFromNm(nm: Double, unit: DistanceUnit): Double = when (unit) {
        DistanceUnit.NAUTICAL -> nm
        DistanceUnit.MILES -> nm * (GeoUtils.KM_PER_NM / GeoUtils.KM_PER_MILE)
        DistanceUnit.KILOMETERS -> nm * GeoUtils.KM_PER_NM
    }

    fun distanceFromKm(km: Double, unit: DistanceUnit): Double = when (unit) {
        DistanceUnit.KILOMETERS -> km
        DistanceUnit.MILES -> km / GeoUtils.KM_PER_MILE
        DistanceUnit.NAUTICAL -> km / GeoUtils.KM_PER_NM
    }
}
