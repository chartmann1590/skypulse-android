/* nearby.js — Nearby aircraft list tab */

import { esc, haversineNm, formatDist, formatAlt, formatSpeed } from './utils.js';

let _containerId = null;
let _activeSort = 'closest';
let _lastData = [];
let _userLat = null;
let _userLon = null;
let _settings = {};

/**
 * Initialize the nearby module.
 * @param {string} containerId  ID of the list container
 */
export function init(containerId) {
  _containerId = containerId;

  // Bind sort chip clicks
  const chips = document.querySelectorAll('.sort-chip');
  chips.forEach(chip => {
    chip.addEventListener('click', () => {
      chips.forEach(c => c.classList.remove('active'));
      chip.classList.add('active');
      _activeSort = chip.dataset.sort || 'closest';
      _renderList();
    });
  });
}

/**
 * Render the aircraft list.
 */
export function render(aircraftArray, userLat, userLon, settings) {
  _lastData = aircraftArray || [];
  _userLat = userLat;
  _userLon = userLon;
  _settings = settings || {};
  _renderList();
}

function _renderList() {
  const container = document.getElementById(_containerId);
  if (!container) return;

  if (!_lastData.length) {
    container.innerHTML = `
      <div class="flex flex-col items-center justify-center py-16 text-center">
        <div class="relative w-32 h-32 rounded-full border border-glass-stroke bg-surface-container-high/30 overflow-hidden flex items-center justify-center mb-6">
          <div class="absolute inset-3 rounded-full border border-glass-stroke/50"></div>
          <div class="absolute inset-8 rounded-full border border-glass-stroke/50"></div>
          <div class="absolute inset-0 radar-sweep rounded-full"></div>
          <div class="w-2 h-2 bg-primary rounded-full z-10" style="box-shadow: 0 0 10px #00dbe7;"></div>
        </div>
        <p class="font-title-md text-title-md text-on-surface mb-2">Searching airspace...</p>
        <p class="font-body-md text-body-md text-on-surface-variant">Acquiring ADS-B signals near you.</p>
      </div>`;
    return;
  }

  const sorted = _sortAircraft([..._lastData]);
  container.innerHTML = sorted.map(ac => _cardHtml(ac)).join('');

  // Bind click handlers
  container.querySelectorAll('[data-hex]').forEach(card => {
    card.addEventListener('click', () => {
      const hex = card.dataset.hex;
      const ac = _lastData.find(a => a.hex === hex);
      if (ac) {
        document.dispatchEvent(new CustomEvent('aircraft-selected', { detail: ac }));
      }
    });
  });
}

function _sortAircraft(arr) {
  switch (_activeSort) {
    case 'closest':
      if (_userLat == null) return arr;
      return arr.sort((a, b) =>
        haversineNm(_userLat, _userLon, a.lat, a.lon) -
        haversineNm(_userLat, _userLon, b.lat, b.lon)
      );
    case 'highest':
      return arr.sort((a, b) => (b.altFt || 0) - (a.altFt || 0));
    case 'fastest':
      return arr.sort((a, b) => (b.speedKts || 0) - (a.speedKts || 0));
    case 'recent':
      return arr.sort((a, b) => (b.lastSeen || 0) - (a.lastSeen || 0));
    default:
      return arr;
  }
}

function _cardHtml(ac) {
  const distNm = (_userLat != null && ac.lat != null)
    ? haversineNm(_userLat, _userLon, ac.lat, ac.lon)
    : null;
  const dist = distNm != null ? formatDist(distNm, _settings) : '—';
  const alt = formatAlt(ac.altFt, _settings);
  const speed = formatSpeed(ac.speedKts, _settings);
  const heading = ac.headingDeg || 0;
  const label = esc(ac.callsign || ac.hex);
  const type = esc(ac.typeCode || 'Unknown type');

  return `
    <article class="glass-card rounded-xl p-card-padding flex items-center gap-4 hover:border-primary/50 transition-colors cursor-pointer group" data-hex="${esc(ac.hex)}">
      <div class="flex-shrink-0 w-12 h-12 rounded-full bg-surface-container-high border border-glass-stroke flex items-center justify-center relative">
        <span class="material-symbols-outlined text-primary"
          style="font-variation-settings:'FILL' 1; transform: rotate(${heading}deg); display: block;">flight</span>
      </div>
      <div class="flex-grow min-w-0">
        <div class="flex justify-between items-baseline mb-1">
          <h3 class="font-data-lg text-data-lg text-text-high truncate">${label}</h3>
          <span class="font-data-lg text-data-lg text-primary ml-3 shrink-0">${dist}</span>
        </div>
        <div class="flex gap-4 font-label-sm text-label-sm text-on-surface-variant">
          <div class="flex items-center gap-1">
            <span class="material-symbols-outlined text-[14px]">altitude</span>
            ${alt}
          </div>
          <div class="flex items-center gap-1">
            <span class="material-symbols-outlined text-[14px]">speed</span>
            ${speed}
          </div>
          <div class="flex items-center gap-1 hidden sm:flex">
            <span class="material-symbols-outlined text-[14px]">airplanemode_active</span>
            ${type}
          </div>
        </div>
      </div>
    </article>`;
}
