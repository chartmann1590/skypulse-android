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

    // ---- OpenSky Network (fallback, anonymous) ----
    // Docs: https://openskynetwork.github.io/opensky-api/rest.html
    const val OPENSKY_BASE_URL = "https://opensky-network.org/api/"
    const val OPENSKY_STATES_PATH = "states/all"

    /** ADSB.lol caps radius at 250 NM. */
    const val MAX_RADIUS_NM = 250
}
