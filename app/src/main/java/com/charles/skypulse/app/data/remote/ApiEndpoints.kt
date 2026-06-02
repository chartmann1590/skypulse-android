package com.charles.skypulse.app.data.remote

/**
 * ★ Single source of truth for all external endpoint paths (Plan §5).
 *
 * If a free public API changes its routes, adjust them HERE only. Base URLs are consumed
 * by the Retrofit builders in NetworkModule; the relative paths are used by the API
 * interfaces below.
 */
object ApiEndpoints {

    // ---- ADSB.lol (primary) ----
    // Docs: https://api.adsb.lol/docs  — v2 radius search returns aircraft within
    // `dist` nautical miles (max 250) of a lat/lon.
    const val ADSB_LOL_BASE_URL = "https://api.adsb.lol/"

    /** Relative path for ADSB.lol radius search: v2/lat/{lat}/lon/{lon}/dist/{nm}. */
    fun adsbRadiusPath(lat: Double, lon: Double, distNm: Int): String =
        "v2/lat/$lat/lon/$lon/dist/$distNm"

    /** ADSB.lol single-aircraft lookups (locate a specific flight anywhere). */
    fun adsbHexPath(hex: String): String = "v2/hex/$hex"
    fun adsbCallsignPath(callsign: String): String = "v2/callsign/$callsign"

    // ---- OpenSky Network (fallback, anonymous) ----
    // Docs: https://openskynetwork.github.io/opensky-api/rest.html
    const val OPENSKY_BASE_URL = "https://opensky-network.org/api/"
    const val OPENSKY_STATES_PATH = "states/all"

    // ---- adsbdb (free, no-key) route lookup: callsign -> origin/destination ----
    // Docs: https://www.adsbdb.com/  — no API key, no login required.
    const val ADSBDB_BASE_URL = "https://api.adsbdb.com/"

    // ---- Flightradar24 live feed (unofficial, keyless) ----
    // Returns per-aircraft position AND origin/destination airports in a bounding box. Used to
    // build accurate airport arrival/departure boards. Unofficial endpoint; may change.
    const val FR24_BASE_URL = "https://data-cloud.flightradar24.com/"
    const val FR24_FEED_PATH = "zones/fcgi/feed.js"

    /** ADSB.lol caps radius at 250 NM. */
    const val MAX_RADIUS_NM = 250
}
