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
    const aircraft = await fetchOnce(loc.lat, loc.lon, 100);
    lastStatus.source = aircraft._source || 'airplanes.live';
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
    const data = await _fetchAirplanesLive(lat, lon, radiusNm);
    data._source = 'airplanes.live';
    return data;
  } catch (err) {
    console.warn('[API] airplanes.live unavailable:', err.message);
    const empty = [];
    empty._source = 'airplanes.live';
    return empty;
  }
}

async function _fetchAirplanesLive(lat, lon, radiusNm) {
  const url = `https://api.airplanes.live/v2/point/${lat}/${lon}/${Math.round(radiusNm)}`;
  const res = await fetch(url, { signal: AbortSignal.timeout(15000) });
  if (!res.ok) throw new Error(`airplanes.live HTTP ${res.status}`);
  const json = await res.json();
  if (json.error) throw new Error(`airplanes.live: ${json.error}`);
  const ac = json.ac || json.aircraft || [];
  return ac.map(_normalizeAc).filter(a => a.lat != null && a.lon != null);
}

function _normalizeAc(ac) {
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
