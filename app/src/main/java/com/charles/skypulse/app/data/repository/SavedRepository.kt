package com.charles.skypulse.app.data.repository

import com.charles.skypulse.app.data.auth.AuthRepository
import com.charles.skypulse.app.data.local.dao.SavedDao
import com.charles.skypulse.app.data.local.entity.SavedAircraftEntity
import com.charles.skypulse.app.data.local.entity.SavedAirportEntity
import com.charles.skypulse.app.data.local.entity.SavedAreaEntity
import com.charles.skypulse.app.di.ApplicationScope
import com.charles.skypulse.app.domain.model.Aircraft
import com.charles.skypulse.app.domain.model.Airport
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope

@Singleton
class SavedRepository @Inject constructor(
    private val savedDao: SavedDao,
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
) {
    val savedAircraft: Flow<List<SavedAircraftEntity>> = savedDao.observeAircraft()
    val savedAirports: Flow<List<SavedAirportEntity>> = savedDao.observeAirports()
    val savedAreas: Flow<List<SavedAreaEntity>> = savedDao.observeAreas()

    init {
        applicationScope.launch {
            authRepository.authState
                .map { it?.uid }
                .distinctUntilChanged()
                .collect { uid ->
                    if (uid != null) syncSavedItems(uid)
                }
        }
    }

    fun isAircraftSaved(id: String): Flow<Boolean> = savedDao.isAircraftSaved(id)

    suspend fun toggleAircraft(aircraft: Aircraft, currentlySaved: Boolean) {
        if (currentlySaved) {
            removeAircraft(aircraft.id)
        } else {
            saveAircraftEntity(
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

    suspend fun removeAircraft(id: String) {
        savedDao.deleteAircraft(id)
        currentUserCollection("saved_aircraft")?.document(id)?.delete()?.await()
    }

    suspend fun saveAirport(airport: Airport) {
        saveAirportEntity(
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

    suspend fun removeAirport(id: Int) {
        savedDao.deleteAirport(id)
        currentUserCollection("saved_airports")?.document(id.toString())?.delete()?.await()
    }

    suspend fun saveArea(label: String, lat: Double, lon: Double, radiusNm: Double) {
        val provisional = SavedAreaEntity(
            label = label,
            lat = lat,
            lon = lon,
            radiusNm = radiusNm,
            savedAtEpochMs = System.currentTimeMillis(),
        )
        val id = savedDao.saveArea(provisional)
        saveAreaRemote(provisional.copy(id = id))
    }

    suspend fun removeArea(id: Long) {
        savedDao.deleteArea(id)
        currentUserCollection("saved_areas")?.document(id.toString())?.delete()?.await()
    }

    private suspend fun syncSavedItems(uid: String) {
        val userDoc = firestore.collection("users").document(uid)

        savedDao.getAircraft().forEach { entity ->
            userDoc.collection("saved_aircraft").document(entity.id).set(entity.toFirestore(), SetOptions.merge()).await()
        }
        savedDao.getAirports().forEach { entity ->
            userDoc.collection("saved_airports").document(entity.airportId.toString()).set(entity.toFirestore(), SetOptions.merge()).await()
        }
        savedDao.getAreas().forEach { entity ->
            userDoc.collection("saved_areas").document(entity.id.toString()).set(entity.toFirestore(), SetOptions.merge()).await()
        }

        userDoc.collection("saved_aircraft").get().await().documents
            .mapNotNull { it.toSavedAircraft() }
            .forEach { savedDao.saveAircraft(it) }
        userDoc.collection("saved_airports").get().await().documents
            .mapNotNull { it.toSavedAirport() }
            .forEach { savedDao.saveAirport(it) }
        userDoc.collection("saved_areas").get().await().documents
            .mapNotNull { it.toSavedArea() }
            .forEach { savedDao.saveArea(it) }
    }

    private suspend fun saveAircraftEntity(entity: SavedAircraftEntity) {
        savedDao.saveAircraft(entity)
        currentUserCollection("saved_aircraft")?.document(entity.id)?.set(entity.toFirestore(), SetOptions.merge())?.await()
    }

    private suspend fun saveAirportEntity(entity: SavedAirportEntity) {
        savedDao.saveAirport(entity)
        currentUserCollection("saved_airports")?.document(entity.airportId.toString())?.set(entity.toFirestore(), SetOptions.merge())?.await()
    }

    private suspend fun saveAreaRemote(entity: SavedAreaEntity) {
        currentUserCollection("saved_areas")?.document(entity.id.toString())?.set(entity.toFirestore(), SetOptions.merge())?.await()
    }

    private fun currentUserCollection(name: String) =
        authRepository.currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid).collection(name)
        }
}

private fun SavedAircraftEntity.toFirestore(): Map<String, Any?> = mapOf(
    "id" to id,
    "callsign" to callsign,
    "hex" to hex,
    "typeLabel" to typeLabel,
    "savedAtEpochMs" to savedAtEpochMs,
)

private fun SavedAirportEntity.toFirestore(): Map<String, Any?> = mapOf(
    "airportId" to airportId,
    "code" to code,
    "name" to name,
    "city" to city,
    "country" to country,
    "lat" to lat,
    "lon" to lon,
    "savedAtEpochMs" to savedAtEpochMs,
)

private fun SavedAreaEntity.toFirestore(): Map<String, Any?> = mapOf(
    "id" to id,
    "label" to label,
    "lat" to lat,
    "lon" to lon,
    "radiusNm" to radiusNm,
    "savedAtEpochMs" to savedAtEpochMs,
)

private fun com.google.firebase.firestore.DocumentSnapshot.toSavedAircraft(): SavedAircraftEntity? {
    val savedId = getString("id") ?: id
    return SavedAircraftEntity(
        id = savedId,
        callsign = getString("callsign"),
        hex = getString("hex"),
        typeLabel = getString("typeLabel"),
        savedAtEpochMs = getLong("savedAtEpochMs") ?: 0L,
    )
}

private fun com.google.firebase.firestore.DocumentSnapshot.toSavedAirport(): SavedAirportEntity? {
    val airportId = getLong("airportId")?.toInt() ?: id.toIntOrNull() ?: return null
    return SavedAirportEntity(
        airportId = airportId,
        code = getString("code") ?: return null,
        name = getString("name") ?: return null,
        city = getString("city"),
        country = getString("country"),
        lat = getDouble("lat") ?: return null,
        lon = getDouble("lon") ?: return null,
        savedAtEpochMs = getLong("savedAtEpochMs") ?: 0L,
    )
}

private fun com.google.firebase.firestore.DocumentSnapshot.toSavedArea(): SavedAreaEntity? {
    val areaId = getLong("id") ?: id.toLongOrNull() ?: return null
    return SavedAreaEntity(
        id = areaId,
        label = getString("label") ?: return null,
        lat = getDouble("lat") ?: return null,
        lon = getDouble("lon") ?: return null,
        radiusNm = getDouble("radiusNm") ?: return null,
        savedAtEpochMs = getLong("savedAtEpochMs") ?: 0L,
    )
}
