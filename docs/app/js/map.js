/* map.js — Leaflet map initialization and aircraft markers */

import { esc, haversineNm, formatAlt, formatSpeed } from './utils.js';

let _map = null;
const _markers = new Map(); // hex → L.Marker
let _settings = {};
let _highlightedHex = null;

/**
 * Initialize the Leaflet map.
 * @param {string} containerId  ID of the map container element
 */
export function initMap(containerId) {
  if (_map) return _map;

  _map = L.map(containerId, {
    center: [39.5, -98.35],
    zoom: 5,
    zoomControl: false,
    attributionControl: false,
  });

  // OSM tiles
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 18,
    attribution: '© OpenStreetMap contributors',
  }).addTo(_map);

  // Attribution in corner
  L.control.attribution({ position: 'bottomright', prefix: false }).addTo(_map);

  // Custom zoom control (top-right)
  L.control.zoom({ position: 'topright' }).addTo(_map);

  return _map;
}

/**
 * Get the Leaflet map instance.
 */
export function getMap() {
  return _map;
}

/**
 * Update current settings reference.
 */
export function setSettings(settings) {
  _settings = settings || {};
}

/**
 * Update aircraft markers on the map.
 * @param {Aircraft[]} aircraftArray
 * @param {{ lat, lon }|null} userLocation
 * @param {object} settings
 */
export function updateMarkers(aircraftArray, userLocation, settings) {
  if (!_map) return;
  _settings = settings || _settings;

  const seen = new Set();

  for (const ac of aircraftArray) {
    if (ac.lat == null || ac.lon == null) continue;
    seen.add(ac.hex);

    if (_markers.has(ac.hex)) {
      // Update existing marker
      const marker = _markers.get(ac.hex);
      marker.setLatLng([ac.lat, ac.lon]);
      const icon = _buildIcon(ac);
      marker.setIcon(icon);
      marker._acData = ac;
    } else {
      // Create new marker
      const icon = _buildIcon(ac);
      const marker = L.marker([ac.lat, ac.lon], {
        icon,
        title: ac.callsign || ac.hex,
        zIndexOffset: 100,
      });
      marker._acData = ac;
      marker.on('click', () => {
        document.dispatchEvent(new CustomEvent('aircraft-selected', { detail: ac }));
      });
      marker.addTo(_map);
      _markers.set(ac.hex, marker);
    }
  }

  // Remove stale markers
  for (const [hex, marker] of _markers) {
    if (!seen.has(hex)) {
      marker.remove();
      _markers.delete(hex);
    }
  }

  // Update closest aircraft card on map
  if (userLocation && aircraftArray.length > 0) {
    _updateClosestCard(aircraftArray, userLocation);
  }
}

function _buildIcon(ac) {
  const heading = ac.headingDeg || 0;
  const isHighlighted = ac.hex === _highlightedHex;
  const glowStyle = isHighlighted ? 'filter: drop-shadow(0 0 12px rgba(0, 219, 231, 1));' : '';
  const colorStyle = isHighlighted ? 'color: #00dbe7;' : 'color: #e1fdff;';

  const html = `
    <div class="leaflet-aircraft-marker" style="transform-origin: center;">
      <span class="material-symbols-outlined aircraft-icon"
        style="font-variation-settings: 'FILL' 1; font-size: 20px; transform: rotate(${heading}deg); display: block; ${colorStyle} ${glowStyle} filter: drop-shadow(0 0 4px rgba(225,253,255,0.8));">
        flight
      </span>
      ${ac.callsign ? `<div class="aircraft-label">${esc(ac.callsign)}</div>` : ''}
    </div>`;

  return L.divIcon({
    html,
    className: '',
    iconSize: [48, ac.callsign ? 40 : 24],
    iconAnchor: [24, 12],
  });
}

function _updateClosestCard(aircraftArray, userLocation) {
  const card = document.getElementById('closest-aircraft-card');
  if (!card) return;

  let closest = null;
  let minDist = Infinity;

  for (const ac of aircraftArray) {
    if (ac.lat == null || ac.lon == null) continue;
    const d = haversineNm(userLocation.lat, userLocation.lon, ac.lat, ac.lon);
    if (d < minDist) { minDist = d; closest = ac; }
  }

  if (!closest) { card.classList.add('hidden'); return; }

  card.classList.remove('hidden');
  card.innerHTML = `
    <div class="flex items-center gap-3">
      <span class="material-symbols-outlined text-primary text-xl"
        style="font-variation-settings:'FILL' 1; transform: rotate(${closest.headingDeg || 0}deg); display: block;">flight</span>
      <div>
        <div class="font-data-lg text-data-lg text-on-surface font-bold">${esc(closest.callsign || closest.hex)}</div>
        <div class="font-label-sm text-label-sm text-on-surface-variant">${esc(closest.typeCode || 'Unknown')}</div>
      </div>
    </div>
    <div class="text-right">
      <div class="font-data-lg text-data-lg text-primary">${minDist.toFixed(1)} nm</div>
      <div class="font-label-sm text-label-sm text-on-surface-variant">${formatAlt(closest.altFt, _settings)}</div>
    </div>`;

  card.onclick = () => {
    document.dispatchEvent(new CustomEvent('aircraft-selected', { detail: closest }));
  };
}

/**
 * Center map on user location.
 */
export function centerOnUser(lat, lon) {
  if (!_map) return;
  _map.flyTo([lat, lon], Math.max(_map.getZoom(), 10), { duration: 1 });
}

/**
 * Highlight a specific aircraft marker.
 */
export function highlightAircraft(hex) {
  _highlightedHex = hex;
  for (const [h, marker] of _markers) {
    const ac = marker._acData;
    if (ac) marker.setIcon(_buildIcon(ac));
  }
  const marker = _markers.get(hex);
  if (marker) {
    _map.panTo(marker.getLatLng(), { animate: true, duration: 0.5 });
  }
}

/**
 * Add or update user location marker.
 */
let _userMarker = null;
export function setUserLocation(lat, lon) {
  if (!_map) return;
  if (!_userMarker) {
    const icon = L.divIcon({
      html: `<div style="width:14px;height:14px;background:#00dbe7;border-radius:50%;border:3px solid white;box-shadow:0 0 12px rgba(0,219,231,0.8);"></div>`,
      className: '',
      iconSize: [14, 14],
      iconAnchor: [7, 7],
    });
    _userMarker = L.marker([lat, lon], { icon, zIndexOffset: 1000 }).addTo(_map);
  } else {
    _userMarker.setLatLng([lat, lon]);
  }
}
