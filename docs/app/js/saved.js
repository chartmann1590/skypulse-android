/* saved.js — Saved aircraft, airports, and areas */

import { esc, relTime } from './utils.js';

const LS_AIRCRAFT_KEY = 'sp_saved_aircraft';
const LS_AIRPORTS_KEY = 'sp_saved_airports';
const LS_AREAS_KEY    = 'sp_saved_areas';

let _containerId = null;
let _firebaseMod = null;
let _activeSubTab = 'aircraft';
let _saved = { aircraft: [], airports: [], areas: [] };
let _uid = null;

/**
 * Initialize the saved tab.
 */
export function init(containerId, firebaseMod) {
  _containerId = containerId;
  _firebaseMod = firebaseMod;
  _renderShell();
}

export async function onAuthChanged(user) {
  _uid = user?.uid || null;
  if (_uid) {
    await loadSaved(_uid);
    await syncLocalToCloud(_uid, _firebaseMod);
  } else {
    _loadFromLocalStorage();
  }
  renderAll(_saved);
}

function _renderShell() {
  const c = document.getElementById(_containerId);
  if (!c) return;
  c.innerHTML = `
    <h2 class="font-headline-lg-mobile text-headline-lg-mobile text-text-high mb-2">Saved Items</h2>
    <p class="font-body-md text-body-md text-on-surface-variant mb-6">Quick access to your tracked entities.</p>
    <div class="flex space-x-2 bg-surface-container-low p-1 rounded-xl mb-6 border border-glass-stroke overflow-x-auto no-scrollbar">
      <button class="saved-sub-tab flex-1 min-w-[90px] py-2 text-center rounded-lg font-title-md text-title-md transition-all" data-subtab="aircraft">Aircraft</button>
      <button class="saved-sub-tab flex-1 min-w-[90px] py-2 text-center rounded-lg font-title-md text-title-md transition-all" data-subtab="airports">Airports</button>
      <button class="saved-sub-tab flex-1 min-w-[90px] py-2 text-center rounded-lg font-title-md text-title-md transition-all" data-subtab="areas">Areas</button>
    </div>
    <div id="saved-content"></div>`;

  document.querySelectorAll('.saved-sub-tab').forEach(btn => {
    btn.addEventListener('click', () => {
      _activeSubTab = btn.dataset.subtab;
      _updateSubTabStyles();
      _renderContent();
    });
  });

  _updateSubTabStyles();
  _loadFromLocalStorage();
  renderAll(_saved);
}

function _updateSubTabStyles() {
  document.querySelectorAll('.saved-sub-tab').forEach(btn => {
    const isActive = btn.dataset.subtab === _activeSubTab;
    btn.className = `saved-sub-tab flex-1 min-w-[90px] py-2 text-center rounded-lg font-title-md text-title-md transition-all ${
      isActive
        ? 'glass-panel text-primary neon-glow'
        : 'text-on-surface-variant hover:text-text-high hover:bg-glass-surface/50'
    }`;
  });
}

/**
 * Load saved items from Firestore.
 */
export async function loadSaved(uid) {
  if (!_firebaseMod || !uid) return;
  try {
    const [aircraft, airports, areas] = await Promise.all([
      _firebaseMod.listSavedAircraft(uid),
      _firebaseMod.listSavedAirports(uid),
      _firebaseMod.listSavedAreas(uid),
    ]);
    _saved = { aircraft, airports, areas };
  } catch (err) {
    console.warn('[Saved] Firestore load failed:', err.message);
    _loadFromLocalStorage();
  }
}

function _loadFromLocalStorage() {
  try {
    _saved = {
      aircraft: JSON.parse(localStorage.getItem(LS_AIRCRAFT_KEY) || '[]'),
      airports: JSON.parse(localStorage.getItem(LS_AIRPORTS_KEY) || '[]'),
      areas:    JSON.parse(localStorage.getItem(LS_AREAS_KEY) || '[]'),
    };
  } catch {
    _saved = { aircraft: [], airports: [], areas: [] };
  }
}

/**
 * Merge localStorage items to Firestore after login.
 */
export async function syncLocalToCloud(uid, firebaseMod) {
  if (!firebaseMod || !uid) return;
  try {
    const localAircraft = JSON.parse(localStorage.getItem(LS_AIRCRAFT_KEY) || '[]');
    const localAirports = JSON.parse(localStorage.getItem(LS_AIRPORTS_KEY) || '[]');
    const localAreas    = JSON.parse(localStorage.getItem(LS_AREAS_KEY) || '[]');

    await Promise.all([
      ...localAircraft.map(a => firebaseMod.saveSavedAircraft(uid, a)),
      ...localAirports.map(a => firebaseMod.saveSavedAirport(uid, a)),
      ...localAreas.map(a => firebaseMod.saveSavedArea(uid, a)),
    ]);

    // Clear local after sync
    localStorage.removeItem(LS_AIRCRAFT_KEY);
    localStorage.removeItem(LS_AIRPORTS_KEY);
    localStorage.removeItem(LS_AREAS_KEY);
  } catch (err) {
    console.warn('[Saved] Sync to cloud failed:', err.message);
  }
}

/**
 * Save an aircraft to saved list.
 */
export async function saveAircraft(ac) {
  const item = {
    id: ac.hex,
    callsign: ac.callsign || null,
    hex: ac.hex,
    typeLabel: ac.typeCode || null,
    savedAtEpochMs: Date.now(),
  };
  if (_uid && _firebaseMod) {
    await _firebaseMod.saveSavedAircraft(_uid, item);
    await loadSaved(_uid);
  } else {
    const list = JSON.parse(localStorage.getItem(LS_AIRCRAFT_KEY) || '[]');
    const exists = list.findIndex(a => a.hex === ac.hex);
    if (exists >= 0) list[exists] = item; else list.push(item);
    localStorage.setItem(LS_AIRCRAFT_KEY, JSON.stringify(list));
    _loadFromLocalStorage();
  }
  renderAll(_saved);
}

/**
 * Remove an aircraft from saved list.
 */
export async function unsaveAircraft(hex) {
  if (_uid && _firebaseMod) {
    await _firebaseMod.deleteSavedAircraft(_uid, hex);
    await loadSaved(_uid);
  } else {
    const list = JSON.parse(localStorage.getItem(LS_AIRCRAFT_KEY) || '[]').filter(a => a.hex !== hex);
    localStorage.setItem(LS_AIRCRAFT_KEY, JSON.stringify(list));
    _loadFromLocalStorage();
  }
  renderAll(_saved);
}

export function isAircraftSaved(hex) {
  return _saved.aircraft.some(a => a.hex === hex);
}

/**
 * Render all sub-tabs.
 */
export function renderAll(saved) {
  _saved = saved || _saved;
  _renderContent();
}

function _renderContent() {
  const c = document.getElementById('saved-content');
  if (!c) return;

  switch (_activeSubTab) {
    case 'aircraft':  c.innerHTML = _aircraftList(_saved.aircraft);  break;
    case 'airports':  c.innerHTML = _airportList(_saved.airports);   break;
    case 'areas':     c.innerHTML = _areaList(_saved.areas);         break;
  }

  // Bind delete buttons
  c.querySelectorAll('[data-delete-hex]').forEach(btn => {
    btn.addEventListener('click', async e => {
      e.stopPropagation();
      await unsaveAircraft(btn.dataset.deleteHex);
    });
  });
}

function _aircraftList(items) {
  if (!items?.length) return _emptyState('flight', 'No saved aircraft', 'Tap the star on any aircraft to save it.');
  return `<div class="space-y-3">
    ${items.map(a => `
      <div class="glass-panel rounded-xl p-card-padding flex items-center justify-between hover:bg-glass-surface/80 transition-all cursor-pointer group"
           onclick="document.dispatchEvent(new CustomEvent('search-aircraft', {detail: '${esc(a.hex)}'}))">
        <div class="flex items-center space-x-4">
          <div class="w-12 h-12 rounded-full bg-surface-container-high flex items-center justify-center border border-outline-variant group-hover:border-primary/50 transition-colors">
            <span class="material-symbols-outlined text-primary text-[24px]" style="font-variation-settings:'FILL' 1;">flight</span>
          </div>
          <div>
            <h3 class="font-data-lg text-data-lg text-text-high">${esc(a.callsign || a.hex)}</h3>
            <p class="font-label-sm text-label-sm text-on-surface-variant">${esc(a.typeLabel || a.hex)}</p>
          </div>
        </div>
        <button class="text-primary hover:text-error drop-shadow-[0_0_8px_rgba(0,219,231,0.5)] transition-all active:scale-90 p-2" data-delete-hex="${esc(a.hex)}">
          <span class="material-symbols-outlined" style="font-variation-settings:'FILL' 1;">star</span>
        </button>
      </div>`).join('')}
  </div>`;
}

function _airportList(items) {
  if (!items?.length) return _emptyState('local_airport', 'No saved airports', 'Browse Airports and save your favourites.');
  return `<div class="space-y-3">
    ${items.map(a => `
      <div class="glass-panel rounded-xl p-card-padding flex items-center justify-between hover:bg-glass-surface/80 transition-all cursor-pointer group">
        <div class="flex items-center space-x-4">
          <div class="w-12 h-12 rounded-full bg-surface-container-high flex items-center justify-center border border-outline-variant group-hover:border-primary/50 transition-colors">
            <span class="material-symbols-outlined text-primary text-[24px]" style="font-variation-settings:'FILL' 1;">local_airport</span>
          </div>
          <div>
            <h3 class="font-data-lg text-data-lg text-text-high">${esc(a.code || a.airportId)}</h3>
            <p class="font-label-sm text-label-sm text-on-surface-variant">${esc(a.name || '')}</p>
          </div>
        </div>
        <span class="material-symbols-outlined text-primary" style="font-variation-settings:'FILL' 1;">star</span>
      </div>`).join('')}
  </div>`;
}

function _areaList(items) {
  if (!items?.length) return _emptyState('radar', 'No saved areas', 'Save watch areas to track activity in specific zones.');
  return `<div class="space-y-3">
    ${items.map(a => `
      <div class="glass-panel rounded-xl p-card-padding flex items-center justify-between hover:bg-glass-surface/80 transition-all cursor-pointer group">
        <div class="flex items-center space-x-4">
          <div class="w-12 h-12 rounded-full bg-surface-container-high flex items-center justify-center border border-outline-variant group-hover:border-primary/50 transition-colors">
            <span class="material-symbols-outlined text-primary text-[24px]">radar</span>
          </div>
          <div>
            <h3 class="font-data-lg text-data-lg text-text-high">${esc(a.label || 'Area')}</h3>
            <p class="font-label-sm text-label-sm text-on-surface-variant">${a.radiusNm ? a.radiusNm + ' NM radius' : ''}</p>
          </div>
        </div>
        <span class="material-symbols-outlined text-primary" style="font-variation-settings:'FILL' 1;">star</span>
      </div>`).join('')}
  </div>`;
}

function _emptyState(icon, title, body) {
  return `
    <div class="flex flex-col items-center justify-center py-16 text-center">
      <div class="w-24 h-24 rounded-full glass-panel flex items-center justify-center mb-6 border border-primary/30">
        <span class="material-symbols-outlined text-[48px] text-primary neon-glow" style="font-variation-settings:'FILL' 1;">${esc(icon)}</span>
      </div>
      <h3 class="font-title-md text-title-md text-text-high mb-2">${esc(title)}</h3>
      <p class="font-body-md text-body-md text-on-surface-variant max-w-[280px]">${esc(body)}</p>
    </div>`;
}
