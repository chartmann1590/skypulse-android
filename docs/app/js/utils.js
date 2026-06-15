/* utils.js — shared utility functions */

const R_NM = 3440.065; // Earth radius in nautical miles

/**
 * Haversine distance in nautical miles.
 */
export function haversineNm(lat1, lon1, lat2, lon2) {
  const toRad = d => d * Math.PI / 180;
  const dLat = toRad(lat2 - lat1);
  const dLon = toRad(lon2 - lon1);
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon / 2) ** 2;
  return 2 * R_NM * Math.asin(Math.sqrt(a));
}

/**
 * Haversine distance in statute miles.
 */
export function haversineMi(lat1, lon1, lat2, lon2) {
  return haversineNm(lat1, lon1, lat2, lon2) * 1.15078;
}

/**
 * Format altitude with unit preference.
 * @param {number|null} ft  altitude in feet
 * @param {object} settings  { altUnit: 'ft'|'m' }
 */
export function formatAlt(ft, settings = {}) {
  if (ft == null || ft === '' || isNaN(ft)) return '—';
  const unit = settings.altUnit || 'ft';
  if (unit === 'm') {
    const m = Math.round(ft * 0.3048);
    return m.toLocaleString() + ' m';
  }
  return Math.round(ft).toLocaleString() + ' ft';
}

/**
 * Format speed with unit preference.
 * @param {number|null} kts  speed in knots
 * @param {object} settings  { speedUnit: 'kts'|'mph'|'kmh' }
 */
export function formatSpeed(kts, settings = {}) {
  if (kts == null || kts === '' || isNaN(kts)) return '—';
  const unit = settings.speedUnit || 'kts';
  if (unit === 'mph') return Math.round(kts * 1.15078) + ' mph';
  if (unit === 'kmh') return Math.round(kts * 1.852) + ' km/h';
  return Math.round(kts) + ' kts';
}

/**
 * Format distance with unit preference.
 * @param {number} nm  distance in nautical miles
 * @param {object} settings  { distUnit: 'nm'|'mi'|'km' }
 */
export function formatDist(nm, settings = {}) {
  if (nm == null || isNaN(nm)) return '—';
  const unit = settings.distUnit || 'nm';
  if (unit === 'mi') return (nm * 1.15078).toFixed(1) + ' mi';
  if (unit === 'km') return (nm * 1.852).toFixed(1) + ' km';
  return nm.toFixed(1) + ' nm';
}

/**
 * HTML-escape a string.
 */
export function esc(str) {
  if (str == null) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

/**
 * Relative time label from epoch seconds.
 */
export function relTime(epochSec) {
  if (!epochSec) return 'unknown';
  const diffSec = Math.floor(Date.now() / 1000) - epochSec;
  if (diffSec < 10) return 'just now';
  if (diffSec < 60) return `${diffSec}s ago`;
  const m = Math.floor(diffSec / 60);
  if (m < 60) return `${m} min ago`;
  const h = Math.floor(m / 60);
  return `${h}h ago`;
}

/**
 * Vertical rate label.
 */
export function vrLabel(vr) {
  if (vr == null || isNaN(vr)) return 'Level';
  if (vr > 100) return 'Climbing';
  if (vr < -100) return 'Descending';
  return 'Level';
}

/**
 * Heading to cardinal string (N, NE, E, etc.)
 */
export function headingToCard(deg) {
  if (deg == null) return '';
  const dirs = ['N','NE','E','SE','S','SW','W','NW'];
  return dirs[Math.round(deg / 45) % 8];
}
