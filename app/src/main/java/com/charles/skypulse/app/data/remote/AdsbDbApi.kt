package com.charles.skypulse.app.data.remote

import com.charles.skypulse.app.data.remote.dto.AdsbDbResponse
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * adsbdb.com free route API (no key/login). Maps a callsign to its origin/destination
 * airports. Returns HTTP 404 with body {"response":"unknown callsign"} when not found.
 */
interface AdsbDbApi {

    @GET("v0/callsign/{callsign}")
    suspend fun getRoute(@Path("callsign") callsign: String): AdsbDbResponse
}
