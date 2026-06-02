package com.charles.skypulse.app

import com.charles.skypulse.app.domain.model.AltitudeUnit
import com.charles.skypulse.app.domain.model.DistanceUnit
import com.charles.skypulse.app.domain.model.SpeedUnit
import com.charles.skypulse.app.domain.util.UnitConverters
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitConvertersTest {

    @Test
    fun meters_to_feet() {
        assertEquals(3280.84, UnitConverters.metersToFeet(1000.0), 0.1)
    }

    @Test
    fun ms_to_knots() {
        assertEquals(194.384, UnitConverters.msToKnots(100.0), 0.01)
    }

    @Test
    fun ms_to_fpm() {
        assertEquals(196.85, UnitConverters.msToFpm(1.0), 0.1)
    }

    @Test
    fun altitude_display_metersConversion() {
        assertEquals(304.8, UnitConverters.altitude(1000.0, AltitudeUnit.METERS), 0.5)
        assertEquals(1000.0, UnitConverters.altitude(1000.0, AltitudeUnit.FEET), 0.0)
    }

    @Test
    fun speed_display_mph() {
        assertEquals(115.078, UnitConverters.speed(100.0, SpeedUnit.MPH), 0.01)
        assertEquals(100.0, UnitConverters.speed(100.0, SpeedUnit.KNOTS), 0.0)
    }

    @Test
    fun distance_from_nm() {
        assertEquals(100.0, UnitConverters.distanceFromNm(100.0, DistanceUnit.NAUTICAL), 0.0)
        assertEquals(185.2, UnitConverters.distanceFromNm(100.0, DistanceUnit.KILOMETERS), 0.1)
        // 100 NM ~ 115.08 miles
        assertEquals(115.08, UnitConverters.distanceFromNm(100.0, DistanceUnit.MILES), 0.1)
    }
}
