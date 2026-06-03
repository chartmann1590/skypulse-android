package com.charles.skypulse.app.ui.screens.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.charles.skypulse.app.R
import com.charles.skypulse.app.domain.model.Aircraft
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * Full-screen OpenStreetMap (osmdroid) with rotating aircraft markers. No Google Maps,
 * no API key. Markers are rebuilt each update from the live [aircraft] list.
 */
@Composable
fun OsmMapView(
    aircraft: List<Aircraft>,
    userLat: Double?,
    userLon: Double?,
    selectedId: String?,
    onAircraftClick: (Aircraft) -> Unit,
    modifier: Modifier = Modifier,
    recenterTrigger: Int = 0,
    focusAircraft: Aircraft? = null,
) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setUseDataConnection(true)
            controller.setZoom(9.0)
            isHorizontalMapRepetitionEnabled = false
        }
    }
    // Mutable holders that don't trigger recomposition.
    val hasCentered = remember { booleanArrayOf(false) }
    val lastRecenter = remember { intArrayOf(-1) }
    val lastFocusId = remember { arrayOfNulls<String>(1) }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { map ->
            // Center on the user the first time we get a location.
            if (userLat != null && userLon != null && !hasCentered[0]) {
                map.controller.setZoom(9.0)
                map.controller.setCenter(GeoPoint(userLat, userLon))
                hasCentered[0] = true
            }
            // Re-center (animated) whenever the location button is tapped.
            if (recenterTrigger != lastRecenter[0]) {
                lastRecenter[0] = recenterTrigger
                if (userLat != null && userLon != null) {
                    map.controller.animateTo(GeoPoint(userLat, userLon))
                    map.controller.setZoom(10.0)
                }
            }

            // Center on a focused (shared) aircraft when it first arrives.
            if (focusAircraft != null &&
                focusAircraft.latitude != null && focusAircraft.longitude != null &&
                focusAircraft.id != lastFocusId[0]
            ) {
                lastFocusId[0] = focusAircraft.id
                map.controller.animateTo(GeoPoint(focusAircraft.latitude, focusAircraft.longitude))
                map.controller.setZoom(10.0)
                hasCentered[0] = true
            }

            map.overlays.clear()

            // User location marker.
            if (userLat != null && userLon != null) {
                Marker(map).apply {
                    position = GeoPoint(userLat, userLon)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    title = "You"
                    icon = ContextCompat.getDrawable(context, android.R.drawable.presence_online)
                }.also { map.overlays.add(it) }
            }

            // Aircraft markers (cap for performance on dense areas).
            aircraft.asSequence()
                .filter { it.latitude != null && it.longitude != null }
                .take(250)
                .forEach { ac ->
                    val marker = Marker(map).apply {
                        position = GeoPoint(ac.latitude!!, ac.longitude!!)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        rotation = -(ac.headingDegrees?.toFloat() ?: 0f)
                        title = ac.displayName
                        icon = ContextCompat.getDrawable(context, R.drawable.ic_map_plane)
                        isFlat = true
                        setOnMarkerClickListener { _, _ ->
                            onAircraftClick(ac)
                            true
                        }
                    }
                    map.overlays.add(marker)
                }

            // Highlighted marker for a shared flight (may not be in the live feed).
            if (focusAircraft != null &&
                focusAircraft.latitude != null && focusAircraft.longitude != null &&
                aircraft.none { it.id == focusAircraft.id }
            ) {
                Marker(map).apply {
                    position = GeoPoint(focusAircraft.latitude, focusAircraft.longitude)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    rotation = -(focusAircraft.headingDegrees?.toFloat() ?: 0f)
                    title = focusAircraft.displayName
                    icon = ContextCompat.getDrawable(context, R.drawable.ic_map_plane)
                    isFlat = true
                    setOnMarkerClickListener { _, _ ->
                        onAircraftClick(focusAircraft)
                        true
                    }
                }.also { map.overlays.add(it) }
            }

            map.invalidate()
        },
    )
}
