/* api.js — ADS-B polling and aircraft data fetching */

export const lastStatus = {
  source: null,
  count: 0,
  timestamp: null,
  stale: false,
};

let _pollingTimer = null;
let _getLocationFn = null;
let _getSettingsFn = null;

/**
 * Start the polling loop.
 * @param {Function} getLocationFn  () => { lat, lon } | null
 * @param {Function} getSettingsFn  () => { refreshInterval: number, ... }
 */
export function startPolling(getLocationFn, getSettingsFn) {
  _getLocationFn = getLocationFn;
  _getSettingsFn = getSettingsFn;
  _poll(); // immediate first fetch
  _scheduleNext();
}

export function stopPolling() {
  if (_pollingTimer) {
    clearTimeout(_pollingTimer);
    _pollingTimer = null;
  }
}

function _scheduleNext() {
  const settings = _getSettingsFn ? _getSettingsFn() : {};
  const intervalMs = ((settings.refreshInterval || 10) * 1000);
  _pollingTimer = setTimeout(() => {
    _poll();
    _scheduleNext();
  }, intervalMs);
}

async function _poll() {
  const loc = _getLocationFn ? _getLocationFn() : null;
  if (!loc) return;

  try {
    const aircraft = await fetchOnce(loc.lat, loc.lon, 50);
    lastStatus.source = aircraft._source || 'adsb.lol';
    lastStatus.count = aircraft.length;
    lastStatus.timestamp = Date.now();
    lastStatus.stale = false;

    document.dispatchEvent(new CustomEvent('aircraft-updated', {
      detail: { aircraft }
    }));
  } catch (err) {
    console.warn('[API] Poll error:', err.message);
    lastStatus.stale = true;
  }
}

/**
 * Fetch aircraft once from ADSB.lol or fall back to OpenSky.
 * @param {number} lat
 * @param {number} lon
 * @param {number} radiusNm
 * @returns {Promise<Aircraft[]>}
 */
export async function fetchOnce(lat, lon, radiusNm = 50) {
  try {
    const data = await _fetchAdsbLol(lat, lon, radiusNm);
    data._source = 'adsb.lol';
    return data;
  } catch (primaryErr) {
    console.warn('[API] ADSB.lol failed, trying OpenSky:', primaryErr.message);
    try {
      const data = await _fetchOpenSky(lat, lon, radiusNm);
      data._source = 'opensky';
      return data;
    } catch (fallbackErr) {
      console.error('[API] Both sources failed:', fallbackErr.message);
      return [];
    }
  }
}

async function _fetchAdsbLol(lat, lon, radiusNm) {
  const url = `https://api.adsb.lol/v2/lat/${lat}/lon/${lon}/dist/${Math.round(radiusNm)}`;
  const res = await fetch(url, { signal: AbortSignal.timeout(8000) });
  if (!res.ok) throw new Error(`ADSB.lol HTTP ${res.status}`);
  const json = await res.json();
  const ac = json.ac || json.aircraft || [];
  return ac.map(_normalizeAdsbLol).filter(a => a.lat != null && a.lon != null);
}

async function _fetchOpenSky(lat, lon, radiusNm) {
  const nmToLat = radiusNm / 60;
  const nmToLon = radiusNm / (60 * Math.cos(lat * Math.PI / 180));
  const lamin = lat - nmToLat, lamax = lat + nmToLat;
  const lomin = lon - nmToLon, lomax = lon + nmToLon;
  const url = `https://opensky-network.org/api/states/all?lamin=${lamin}&lomin=${lomin}&lamax=${lamax}&lomax=${lomax}`;
  const res = await fetch(url, { signal: AbortSignal.timeout(12000) });
  if (!res.ok) throw new Error(`OpenSky HTTP ${res.status}`);
  const json = await res.json();
  const states = json.states || [];
  return states.map(_normalizeOpenSky).filter(a => a.lat != null && a.lon != null);
}

function _normalizeAdsbLol(ac) {
  return {
    hex: (ac.hex || '').toLowerCase(),
    callsign: (ac.flight || ac.r || '').trim() || null,
    lat: ac.lat ?? null,
    lon: ac.lon ?? null,
    altFt: ac.alt_baro != null ? Number(ac.alt_baro) : (ac.alt_geom != null ? Number(ac.alt_geom) : null),
    speedKts: ac.gs != null ? Number(ac.gs) : null,
    headingDeg: ac.track != null ? Number(ac.track) : null,
    vertRateFpm: ac.baro_rate != null ? Number(ac.baro_rate) : (ac.geom_rate != null ? Number(ac.geom_rate) : null),
    onGround: ac.alt_baro === 'ground' || ac.on_ground === true,
    typeCode: ac.t || null,
    originCountry: ac.ownOp || null,
    lastSeen: ac.seen != null ? Math.floor(Date.now() / 1000) - ac.seen : Math.floor(Date.now() / 1000),
    registration: ac.r || null,
  };
}

function _normalizeOpenSky(state) {
  // state = [icao24, callsign, origin_country, time_pos, last_contact, lon, lat, baro_alt, on_ground, velocity, true_track, vert_rate, sensors, geo_alt, squawk, spi, pos_source]
  const altM = state[7] ?? state[13];
  return {
    hex: (state[0] || '').toLowerCase(),
    callsign: (state[1] || '').trim() || null,
    lat: state[6],
    lon: state[5],
    altFt: altM != null ? altM * 3.28084 : null,
    speedKts: state[9] != null ? state[9] * 1.94384 : null,
    headingDeg: state[10],
    vertRateFpm: state[11] != null ? state[11] * 196.85 : null,
    onGround: state[8] === true,
    typeCode: null,
    originCountry: state[2] || null,
    lastSeen: state[4] || Math.floor(Date.now() / 1000),
    registration: null,
  };
}

/**
 * Fetch route info for a callsign from adsbdb.com.
 * @param {string} callsign
 * @returns {Promise<{originCode, originName, originCity, destCode, destName, destCity}|null>}
 */
export async function fetchRouteInfo(callsign) {
  if (!callsign) return null;
  try {
    const url = `https://api.adsbdb.com/v0/callsign/${callsign.trim().toUpperCase()}`;
    const res = await fetch(url, { signal: AbortSignal.timeout(5000) });
    if (!res.ok) return null;
    const json = await res.json();
    const route = json.response?.flightroute;
    if (!route) return null;
    return {
      originCode: route.origin?.iata_code || route.origin?.icao_code || null,
      originName: route.origin?.name || null,
      originCity: route.origin?.municipality || null,
      destCode: route.destination?.iata_code || route.destination?.icao_code || null,
      destName: route.destination?.name || null,
      destCity: route.destination?.municipality || null,
    };
  } catch {
    return null;
  }
}
