package com.charles.skypulse.app.data.remote

import com.charles.skypulse.app.data.remote.dto.OpenSkyResponse
import retrofit2.http.GET
import retrofit2.http.Query

/** OpenSky Network anonymous REST API (rate-limited, no credentials). */
interface OpenSkyApi {

    @GET("states/all")
    suspend fun getStatesInBoundingBox(
        @Query("lamin") latMin: Double,
        @Query("lomin") lonMin: Double,
        @Query("lamax") latMax: Double,
        @Query("lomax") lonMax: Double,
    ): OpenSkyResponse
}
