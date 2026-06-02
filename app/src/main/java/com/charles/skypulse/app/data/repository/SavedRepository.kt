package com.charles.skypulse.app.data.repository

import com.charles.skypulse.app.data.local.dao.SavedDao
import com.charles.skypulse.app.data.local.entity.SavedAircraftEntity
import com.charles.skypulse.app.data.local.entity.SavedAirportEntity
import com.charles.skypulse.app.data.local.entity.SavedAreaEntity
import com.charles.skypulse.app.domain.model.Aircraft
import com.charles.skypulse.app.domain.model.Airport
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavedRepository @Inject constructor(
    private val savedDao: SavedDao,
) {
    val savedAircraft: Flow<List<SavedAircraftEntity>> = savedDao.observeAircraft()
    val savedAirports: Flow<List<SavedAirportEntity>> = savedDao.observeAirports()
    val savedAreas: Flow<List<SavedAreaEntity>> = savedDao.observeAreas()

    fun isAircraftSaved(id: String): Flow<Boolean> = savedDao.isAircraftSaved(id)

    suspend fun toggleAircraft(aircraft: Aircraft, currentlySaved: Boolean) {
        if (currentlySaved) {
            savedDao.deleteAircraft(aircraft.id)
        } else {
            savedDao.saveAircraft(
                SavedAircraftEntity(
                    id = aircraft.id,
                    callsign = aircraft.callsign,
                    hex = aircraft.hex,
                    typeLabel = aircraft.typeCode,
                    savedAtEpochMs = System.currentTimeMillis(),
                ),
            )
        }
    }

    suspend fun removeAircraft(id: String) = savedDao.deleteAircraft(id)

    suspend fun saveAirport(airport: Airport) {
        savedDao.saveAirport(
            SavedAirportEntity(
                airportId = airport.id,
                code = airport.code,
                name = airport.name,
                city = airport.city,
                country = airport.country,
                lat = airport.latitude,
                lon = airport.longitude,
                savedAtEpochMs = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun removeAirport(id: Int) = savedDao.deleteAirport(id)

    suspend fun saveArea(label: String, lat: Double, lon: Double, radiusNm: Double) {
        savedDao.saveArea(
            SavedAreaEntity(
                label = label,
                lat = lat,
                lon = lon,
                radiusNm = radiusNm,
                savedAtEpochMs = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun removeArea(id: Long) = savedDao.deleteArea(id)
}
