/* airports.js — Airport lookup and nearby airports */

import { esc, haversineNm, formatDist } from './utils.js';

const AIRPORTS_DAT_URL = 'https://raw.githubusercontent.com/jpatokal/openflights/master/data/airports.dat';
const LS_KEY = 'sp_airports_cache';
const LS_VERSION_KEY = 'sp_airports_cache_ver';
const CACHE_VERSION = '1';

let _airports = null; // parsed array
let _containerId = null;
let _searchInput = null;
let _userLat = null;
let _userLon = null;
let _settings = {};

/**
 * Initialize the airports tab.
 */
export function init(containerId) {
  _containerId = containerId;
  _renderLoading();
  loadAirportData().then(() => _renderDefault());
}

export function onActivate(userLat, userLon, settings) {
  _userLat = userLat;
  _userLon = userLon;
  _settings = settings || {};
  if (_airports) _renderDefault();
}

function _renderLoading() {
  const c = document.getElementById(_containerId);
  if (!c) return;
  c.innerHTML = `
    <div class="py-8 text-center text-on-surface-variant">
      <span class="material-symbols-outlined text-4xl text-primary mb-2 block" style="font-variation-settings:'FILL' 0;">local_airport</span>
      Loading airport database...
    </div>`;
}

function _renderDefault() {
  const c = document.getElementById(_containerId);
  if (!c) return;

  const nearby = (_userLat != null && _airports)
    ? nearbyAirports(_userLat, _userLon, 6)
    : [];

  c.innerHTML = `
    <h2 class="font-headline-lg-mobile text-headline-lg-mobile text-text-high mb-4">Airports</h2>
    <div class="relative w-full mb-6">
      <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-on-surface-variant">search</span>
      <input id="airport-search-input"
        class="w-full bg-surface-container/50 border-0 border-b-2 border-glass-stroke focus:border-primary focus:ring-0
               text-text-high font-body-md text-body-md py-4 pl-12 pr-4 transition-colors
               placeholder:text-on-surface-variant rounded-t-lg outline-none"
        placeholder="Search city, airport, IATA/ICAO" type="text"/>
    </div>
    <div id="airport-search-results" class="mb-6 hidden"></div>
    ${nearby.length ? _renderNearbySection(nearby) : ''}
    ${_renderFeaturedSection()}`;

  _searchInput = document.getElementById('airport-search-input');
  if (_searchInput) {
    let _debounce;
    _searchInput.addEventListener('input', e => {
      clearTimeout(_debounce);
      _debounce = setTimeout(() => _handleSearch(e.target.value.trim()), 200);
    });
  }
}

function _handleSearch(query) {
  const resultsDiv = document.getElementById('airport-search-results');
  if (!resultsDiv) return;
  if (!query) {
    resultsDiv.classList.add('hidden');
    resultsDiv.innerHTML = '';
    return;
  }
  const results = searchAirports(query, 15);
  if (!results.length) {
    resultsDiv.classList.remove('hidden');
    resultsDiv.innerHTML = `<p class="text-on-surface-variant py-4 text-center">No airports found for "${esc(query)}"</p>`;
    return;
  }
  resultsDiv.classList.remove('hidden');
  resultsDiv.innerHTML = `
    <h3 class="font-title-md text-title-md text-text-high mb-3">Search Results</h3>
    <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
      ${results.map(a => _airportCard(a)).join('')}
    </div>`;
  _bindAirportCardClicks(resultsDiv);
}

function _renderNearbySection(airports) {
  return `
    <section class="mb-6">
      <div class="flex items-center gap-2 mb-3">
        <span class="material-symbols-outlined text-primary text-sm" style="font-variation-settings:'FILL' 1;">my_location</span>
        <h3 class="font-title-md text-title-md text-text-high">Airports Near You</h3>
      </div>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
        ${airports.map(a => _airportCard(a, true)).join('')}
      </div>
    </section>`;
}

function _renderFeaturedSection() {
  const featured = [
    { iata: 'LHR', name: 'London Heathrow', country: 'United Kingdom' },
    { iata: 'DXB', name: 'Dubai International', country: 'UAE' },
    { iata: 'HND', name: 'Tokyo Haneda', country: 'Japan' },
    { iata: 'JFK', name: 'John F. Kennedy Intl', country: 'United States' },
    { iata: 'SYD', name: 'Sydney Kingsford Smith', country: 'Australia' },
    { iata: 'CDG', name: 'Paris Charles de Gaulle', country: 'France' },
  ];

  return `
    <section>
      <h3 class="font-title-md text-title-md text-text-high flex items-center gap-2 mb-3">
        <span class="material-symbols-outlined text-sm" style="font-variation-settings:'FILL' 1; color: #ffe173;">star</span>
        Featured Airports
      </h3>
      <div class="grid grid-cols-2 md:grid-cols-3 gap-3">
        ${featured.map(f => `
          <div class="glass-card rounded-xl p-card-padding flex flex-col justify-between hover:-translate-y-1 hover:border-primary/30 transition-all cursor-pointer group"
               onclick="document.getElementById('airport-search-input').value='${f.iata}'; document.getElementById('airport-search-input').dispatchEvent(new Event('input'))">
            <div class="flex justify-between items-start">
              <span class="font-data-lg text-data-lg text-text-high group-hover:text-primary transition-colors">${esc(f.iata)}</span>
              <span class="material-symbols-outlined text-on-surface-variant text-sm group-hover:text-primary transition-colors">public</span>
            </div>
            <div class="mt-2">
              <p class="font-body-md text-body-md text-on-surface">${esc(f.name)}</p>
              <p class="font-label-sm text-label-sm text-on-surface-variant mt-1">${esc(f.country)}</p>
            </div>
          </div>`).join('')}
      </div>
    </section>`;
}

function _airportCard(airport, showDist = false) {
  const code = esc(airport.icao || airport.iata || '—');
  const dist = (showDist && _userLat != null)
    ? formatDist(haversineNm(_userLat, _userLon, airport.lat, airport.lon), _settings)
    : null;
  return `
    <div class="glass-card rounded-xl p-card-padding flex flex-col gap-3 relative overflow-hidden group cursor-pointer hover:border-primary/30 transition-all"
         data-airport-id="${esc(String(airport.id))}">
      <div class="absolute -right-8 -top-8 w-24 h-24 bg-primary/10 rounded-full blur-2xl group-hover:bg-primary/20 transition-all duration-500"></div>
      <div class="flex justify-between items-start z-10">
        <div>
          <div class="flex items-baseline gap-2">
            <span class="font-data-lg text-data-lg text-primary tracking-wider">${code}</span>
            ${dist ? `<span class="font-label-sm text-label-sm text-on-surface-variant bg-surface-container-high px-2 py-0.5 rounded-full border border-glass-stroke">${esc(dist)}</span>` : ''}
          </div>
          <p class="font-body-md text-body-md text-on-surface mt-1">${esc(airport.name)}</p>
          <p class="font-label-sm text-label-sm text-on-surface-variant">${esc(airport.city || '')}${airport.city && airport.country ? ', ' : ''}${esc(airport.country || '')}</p>
        </div>
        <span class="material-symbols-outlined text-on-surface-variant">flight_takeoff</span>
      </div>
    </div>`;
}

function _bindAirportCardClicks(container) {
  container.querySelectorAll('[data-airport-id]').forEach(card => {
    card.addEventListener('click', () => {
      const id = card.dataset.airportId;
      const airport = _airports?.find(a => String(a.id) === id);
      if (airport) {
        document.dispatchEvent(new CustomEvent('airport-selected', { detail: airport }));
      }
    });
  });
}

/**
 * Load and cache airport data.
 */
export async function loadAirportData() {
  if (_airports) return _airports;

  // Try localStorage cache
  try {
    const ver = localStorage.getItem(LS_VERSION_KEY);
    const raw = localStorage.getItem(LS_KEY);
    if (ver === CACHE_VERSION && raw) {
      _airports = JSON.parse(raw);
      return _airports;
    }
  } catch {}

  // Fetch from OpenFlights
  try {
    const res = await fetch(AIRPORTS_DAT_URL);
    if (!res.ok) throw new Error('HTTP ' + res.status);
    const text = await res.text();
    _airports = _parseDat(text);
    try {
      localStorage.setItem(LS_VERSION_KEY, CACHE_VERSION);
      localStorage.setItem(LS_KEY, JSON.stringify(_airports));
    } catch {}
  } catch (err) {
    console.warn('[Airports] Failed to load airport data:', err.message);
    _airports = [];
  }
  return _airports;
}

function _parseDat(text) {
  const airports = [];
  const lines = text.split('\n');
  for (const line of lines) {
    if (!line.trim()) continue;
    // CSV fields (some quoted): id,name,city,country,iata,icao,lat,lon,alt,tz,dst,tz_name,type,source
    const fields = _parseCsvLine(line);
    if (fields.length < 8) continue;
    const lat = parseFloat(fields[6]);
    const lon = parseFloat(fields[7]);
    if (isNaN(lat) || isNaN(lon)) continue;
    airports.push({
      id: Number(fields[0]) || 0,
      name: fields[1],
      city: fields[2],
      country: fields[3],
      iata: fields[4] === '\\N' ? null : fields[4],
      icao: fields[5] === '\\N' ? null : fields[5],
      lat,
      lon,
      tz: fields[9] || null,
    });
  }
  return airports;
}

function _parseCsvLine(line) {
  const result = [];
  let cur = '';
  let inQuote = false;
  for (let i = 0; i < line.length; i++) {
    const ch = line[i];
    if (ch === '"') { inQuote = !inQuote; continue; }
    if (ch === ',' && !inQuote) { result.push(cur); cur = ''; continue; }
    cur += ch;
  }
  result.push(cur);
  return result;
}

/**
 * Search airports by query.
 */
export function searchAirports(query, limit = 20) {
  if (!_airports) return [];
  const q = query.toLowerCase();
  return _airports
    .filter(a =>
      a.iata?.toLowerCase().includes(q) ||
      a.icao?.toLowerCase().includes(q) ||
      a.name?.toLowerCase().includes(q) ||
      a.city?.toLowerCase().includes(q)
    )
    .slice(0, limit);
}

/**
 * Get nearest airports to a location.
 */
export function nearbyAirports(lat, lon, limit = 10) {
  if (!_airports) return [];
  return _airports
    .filter(a => a.iata || a.icao) // only real airports with codes
    .map(a => ({ ...a, _dist: haversineNm(lat, lon, a.lat, a.lon) }))
    .sort((a, b) => a._dist - b._dist)
    .slice(0, limit);
}
