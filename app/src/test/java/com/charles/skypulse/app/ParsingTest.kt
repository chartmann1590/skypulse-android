package com.charles.skypulse.app

import com.charles.skypulse.app.data.local.OpenFlightsImporter
import com.charles.skypulse.app.data.remote.dto.parseAltBaro
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParsingTest {

    // --- ADSB.lol alt_baro (number OR "ground") ---

    @Test
    fun altBaro_number_parsedAsFeet() {
        val result = parseAltBaro(JsonPrimitive(34000))
        assertEquals(34000.0, result.feet)
        assertFalse(result.onGround)
    }

    @Test
    fun altBaro_groundString_marksOnGround() {
        val result = parseAltBaro(JsonPrimitive("ground"))
        assertEquals(0.0, result.feet)
        assertTrue(result.onGround)
    }

    @Test
    fun altBaro_null_isNullFeet() {
        val result = parseAltBaro(null)
        assertNull(result.feet)
        assertFalse(result.onGround)
    }

    // --- OpenFlights CSV parsing ---

    @Test
    fun csv_parsesQuotedFieldsWithCommas() {
        val line = "1,\"Goroka Airport\",\"Goroka\",\"Papua New Guinea\",\"GKA\",\"AYGA\",-6.08,145.39,5282,10,\"U\",\"Pacific/Port_Moresby\",\"airport\",\"OurAirports\""
        val cols = OpenFlightsImporter.parseCsvLine(line)
        assertEquals("1", cols[0])
        assertEquals("Goroka Airport", cols[1])
        assertEquals("GKA", cols[4])
        assertEquals("AYGA", cols[5])
        assertEquals(-6.08, cols[6].toDouble(), 0.001)
    }

    @Test
    fun csv_convertsBackslashNToEmpty() {
        val line = "1,\"Private flight\",\\N,\"-\",\"N/A\",\"\",\"\",\"Y\""
        val cols = OpenFlightsImporter.parseCsvLine(line)
        assertEquals("", cols[2]) // \N -> ""
        assertEquals("Y", cols[7])
    }
}
