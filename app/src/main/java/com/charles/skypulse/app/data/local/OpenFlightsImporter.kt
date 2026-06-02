package com.charles.skypulse.app.data.local

import android.content.Context
import android.util.Log
import com.charles.skypulse.app.data.local.dao.AirlineDao
import com.charles.skypulse.app.data.local.dao.AirportDao
import com.charles.skypulse.app.data.local.entity.AirlineEntity
import com.charles.skypulse.app.data.local.entity.AirportEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Imports the bundled OpenFlights datasets (assets/airports.dat, assets/airlines.dat) into
 * Room on first launch. CSV format per https://openflights.org/data.php — quoted fields,
 * comma separated, `\N` denotes null.
 */
@Singleton
class OpenFlightsImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val airportDao: AirportDao,
    private val airlineDao: AirlineDao,
) {
    /** Imports both datasets if the tables are empty. Safe to call repeatedly. */
    suspend fun importIfNeeded() = withContext(Dispatchers.IO) {
        runCatching {
            if (airportDao.count() == 0) importAirports()
        }.onFailure { Log.e(TAG, "Airport import failed", it) }
        runCatching {
            if (airlineDao.count() == 0) importAirlines()
        }.onFailure { Log.e(TAG, "Airline import failed", it) }
    }

    private suspend fun importAirports() {
        val batch = ArrayList<AirportEntity>(BATCH)
        readAsset("airports.dat") { cols ->
            if (cols.size < 9) return@readAsset
            val id = cols[0].toIntOrNull() ?: return@readAsset
            val lat = cols[6].toDoubleOrNull() ?: return@readAsset
            val lon = cols[7].toDoubleOrNull() ?: return@readAsset
            batch += AirportEntity(
                id = id,
                name = cols[1].ifBlank { "Unknown" },
                city = cols[2].ifBlankNull(),
                country = cols[3].ifBlankNull(),
                iata = cols[4].ifBlankNull(),
                icao = cols[5].ifBlankNull(),
                lat = lat,
                lon = lon,
                altitudeFeet = cols[8].toIntOrNull(),
            )
            if (batch.size >= BATCH) {
                airportDao.insertAll(batch.toList()); batch.clear()
            }
        }
        if (batch.isNotEmpty()) airportDao.insertAll(batch.toList())
        Log.i(TAG, "Imported ${airportDao.count()} airports")
    }

    private suspend fun importAirlines() {
        val batch = ArrayList<AirlineEntity>(BATCH)
        readAsset("airlines.dat") { cols ->
            if (cols.size < 8) return@readAsset
            val id = cols[0].toIntOrNull() ?: return@readAsset
            batch += AirlineEntity(
                id = id,
                name = cols[1].ifBlank { "Unknown" },
                iata = cols[3].ifBlankNull(),
                icao = cols[4].ifBlankNull(),
                country = cols[6].ifBlankNull(),
                active = cols[7].equals("Y", ignoreCase = true),
            )
            if (batch.size >= BATCH) {
                airlineDao.insertAll(batch.toList()); batch.clear()
            }
        }
        if (batch.isNotEmpty()) airlineDao.insertAll(batch.toList())
        Log.i(TAG, "Imported ${airlineDao.count()} airlines")
    }

    private inline fun readAsset(name: String, onRow: (List<String>) -> Unit) {
        context.assets.open(name).bufferedReader().useLines { lines ->
            lines.forEach { line ->
                if (line.isBlank()) return@forEach
                onRow(parseCsvLine(line))
            }
        }
    }

    companion object {
        private const val TAG = "OpenFlightsImporter"
        private const val BATCH = 500

        /** Parses one OpenFlights CSV line: quoted fields, commas inside quotes, `\N` -> "". */
        fun parseCsvLine(line: String): List<String> {
            val out = ArrayList<String>(14)
            val sb = StringBuilder()
            var inQuotes = false
            var i = 0
            while (i < line.length) {
                val c = line[i]
                when {
                    c == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                        sb.append('"'); i++ // escaped quote
                    }
                    c == '"' -> inQuotes = !inQuotes
                    c == ',' && !inQuotes -> { out.add(normalize(sb.toString())); sb.setLength(0) }
                    else -> sb.append(c)
                }
                i++
            }
            out.add(normalize(sb.toString()))
            return out
        }

        private fun normalize(raw: String): String {
            val t = raw.trim()
            return if (t == "\\N" || t == "NULL") "" else t
        }

        private fun String.ifBlankNull(): String? = ifBlank { null }
    }
}
