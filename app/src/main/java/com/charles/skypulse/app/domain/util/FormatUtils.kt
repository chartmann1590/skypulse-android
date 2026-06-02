package com.charles.skypulse.app.domain.util

import com.charles.skypulse.app.domain.model.AltitudeUnit
import com.charles.skypulse.app.domain.model.DistanceUnit
import com.charles.skypulse.app.domain.model.SpeedUnit
import java.util.Locale
import kotlin.math.roundToInt

/** Display formatting helpers (grouping, units, relative time). */
object FormatUtils {

    private fun grouped(value: Int): String =
        String.format(Locale.US, "%,d", value)

    fun altitude(feet: Double?, unit: AltitudeUnit): String {
        if (feet == null) return "—"
        val v = UnitConverters.altitude(feet, unit).roundToInt()
        return "${grouped(v)} ${unit.label}"
    }

    fun speed(knots: Double?, unit: SpeedUnit): String {
        if (knots == null) return "—"
        val v = UnitConverters.speed(knots, unit).roundToInt()
        return "$v ${unit.label}"
    }

    fun distanceFromNm(nm: Double?, unit: DistanceUnit): String {
        if (nm == null) return "—"
        val v = UnitConverters.distanceFromNm(nm, unit)
        return String.format(Locale.US, "%.1f %s", v, unit.label)
    }

    fun verticalRate(fpm: Double?): String {
        if (fpm == null) return "—"
        val v = fpm.roundToInt()
        val sign = if (v > 0) "+" else ""
        return "$sign$v fpm"
    }

    fun heading(degrees: Double?): String {
        if (degrees == null) return "—"
        return "${degrees.roundToInt()}° ${GeoUtils.compass(degrees)}"
    }

    /** Human relative time from an epoch-seconds last-seen value. */
    fun relativeTime(epochSeconds: Long?, nowMs: Long = System.currentTimeMillis()): String {
        if (epochSeconds == null) return "—"
        val deltaSec = (nowMs / 1000) - epochSeconds
        return when {
            deltaSec < 0 -> "Just now"
            deltaSec < 15 -> "Just now"
            deltaSec < 60 -> "${deltaSec}s ago"
            deltaSec < 3600 -> "${deltaSec / 60}m ago"
            deltaSec < 86400 -> "${deltaSec / 3600}h ago"
            else -> "${deltaSec / 86400}d ago"
        }
    }
}
