/* app.js — SkyPulse PWA entry point */

import * as firebase from './firebase.js';
import * as api from './api.js';
import * as mapMod from './map.js';
import * as nearby from './nearby.js';
import * as airports from './airports.js';
import * as alerts from './alerts.js';
import * as saved from './saved.js';
import * as auth from './auth.js';
import * as settings from './settings.js';
import { esc, haversineNm, formatAlt, formatSpeed, formatDist, relTime, vrLabel, headingToCard } from './utils.js';
import { fetchRouteInfo } from './api.js';

/* ── State ── */
let _location = { lat: 39.8283, lon: -98.5795 }; // default: center of continental US
let _hasRealLocation = false;
let _currentUser = null;
let _activeTab = 'map';
let _activeAircraft = null;    // currently shown in detail sheet
let _allAircraft = [];

/* ── 1. Service Worker ── */
if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('./sw.js').catch(err => {
    console.warn('[SW] Registration failed:', err);
  });
}

/* ── 2. Firebase init ── */
const { auth: fbAuth, db: fbDb } = firebase.initFirebase();

/* ── 3. Geolocation ── */
function getLocation() { return _location; }

if ('geolocation' in navigator) {
  navigator.geolocation.watchPosition(
    pos => {
      _location = { lat: pos.coords.latitude, lon: pos.coords.longitude };
      _hasRealLocation = true;
      mapMod.setUserLocation(_location.lat, _location.lon);
      _updateLastUpdateLabel();
    },
    err => {
      console.warn('[Geo]', err.message);
      _updateLastUpdateLabel();
    },
    { enableHighAccuracy: true, maximumAge: 10000 }
  );
}

/* ── 4. Settings ── */
settings.init('settings-container', newSettings => {
  // restart polling with new interval
  api.stopPolling();
  api.startPolling(getLocation, settings.getSettings);
});

/* ── 5. Auth ── */
auth.init('auth-modal', firebase, handleAuthChange);
firebase.handleGoogleRedirect().then(user => {
  if (user) handleAuthChange(user);
});
firebase.onAuthChange(handleAuthChange);

function handleAuthChange(user) {
  _currentUser = user;
  auth.renderUserState(user);
  auth.updateModalForUser(user);
  saved.onAuthChanged(user);
}

/* ── 6. Map ── */
mapMod.initMap('map-container');

/* ── 7. Tab modules ── */
nearby.init('nearby-list');
airports.init('airports-container');
alerts.init('alerts-container');
saved.init('saved-container', firebase);

/* ── 8. Start polling ── */
api.startPolling(getLocation, settings.getSettings);

/* ── 9. aircraft-updated event ── */
document.addEventListener('aircraft-updated', e => {
  _allAircraft = e.detail.aircraft || [];

  const s = settings.getSettings();
  const loc = getLocation();

  mapMod.updateMarkers(_allAircraft, loc, s);
  nearby.render(_allAircraft, loc?.lat ?? null, loc?.lon ?? null, s);
  alerts.checkAlerts(_allAircraft, loc?.lat ?? null, loc?.lon ?? null, []);

  // Update aircraft count badge
  const countEl = document.getElementById('aircraft-count');
  if (countEl) {
    countEl.textContent = `${_allAircraft.length} plane${_allAircraft.length !== 1 ? 's' : ''}`;
  }

  // Status indicator
  const statusEl = document.getElementById('status-indicator');
  if (statusEl) {
    const src = api.lastStatus.source === 'opensky' ? 'OpenSky' : 'ADSB.lol';
    statusEl.textContent = `${_allAircraft.length} aircraft · ${src}`;
  }

  _updateLastUpdateLabel();
});

function _updateLastUpdateLabel() {
  const el = document.getElementById('last-update-label');
  if (!el) return;
  if (!_hasRealLocation) {
    el.innerHTML = `<span class="w-1.5 h-1.5 rounded-full bg-on-surface-variant/30"></span> Default location — enable GPS for local view`;
    return;
  }
  const ts = api.lastStatus.timestamp;
  const timeStr = ts ? relTime(Math.floor(ts / 1000)) : 'updating...';
  const dot = api.lastStatus.stale
    ? `<span class="w-1.5 h-1.5 rounded-full bg-error animate-pulse"></span>`
    : `<span class="w-1.5 h-1.5 rounded-full bg-primary animate-pulse"></span>`;
  el.innerHTML = `${dot} Updated ${timeStr}`;
}

/* ── 10. Tab switching ── */
const TAB_PANELS = ['map', 'nearby', 'airports', 'alerts', 'saved'];

function switchTab(tabName) {
  _activeTab = tabName;

  TAB_PANELS.forEach(t => {
    const panel = document.getElementById(`tab-${t}`);
    if (panel) panel.classList.toggle('hidden', t !== tabName);
  });

  // Update bottom nav
  document.querySelectorAll('.tab-btn').forEach(btn => {
    const active = btn.dataset.tab === tabName;
    btn.classList.toggle('text-primary', active);
    btn.classList.toggle('text-on-surface-variant', !active);
    const icon = btn.querySelector('.material-symbols-outlined');
    if (icon) {
      icon.style.fontVariationSettings = active ? "'FILL' 1" : "'FILL' 0";
    }
  });

  // Update desktop nav
  document.querySelectorAll('#desktop-nav a').forEach(a => {
    const active = a.dataset.tab === tabName;
    a.className = active
      ? 'font-title-md text-title-md text-primary glow-active'
      : 'font-title-md text-title-md text-on-surface-variant hover:text-primary/80 transition-all';
  });

  // Tab-specific activation
  if (tabName === 'nearby') {
    const loc = getLocation();
    const s = settings.getSettings();
    nearby.render(_allAircraft, loc?.lat ?? null, loc?.lon ?? null, s);
  }
  if (tabName === 'airports') {
    const loc = getLocation();
    airports.onActivate(loc?.lat ?? null, loc?.lon ?? null, settings.getSettings());
  }
}

// Bottom nav clicks
document.querySelectorAll('.tab-btn').forEach(btn => {
  btn.addEventListener('click', () => switchTab(btn.dataset.tab));
});

// Build desktop nav
const desktopNav = document.getElementById('desktop-nav');
if (desktopNav) {
  const tabs = [
    { tab: 'map', label: 'Map' },
    { tab: 'nearby', label: 'Nearby' },
    { tab: 'airports', label: 'Airports' },
    { tab: 'alerts', label: 'Alerts' },
    { tab: 'saved', label: 'Saved' },
  ];
  desktopNav.innerHTML = tabs.map(t =>
    `<a href="#" data-tab="${t.tab}" class="font-title-md text-title-md text-on-surface-variant hover:text-primary/80 transition-all">${esc(t.label)}</a>`
  ).join('');
  desktopNav.querySelectorAll('a').forEach(a => {
    a.addEventListener('click', e => { e.preventDefault(); switchTab(a.dataset.tab); });
  });
}

/* ── 11. aircraft-selected → detail sheet ── */
document.addEventListener('aircraft-selected', e => {
  _openDetailSheet(e.detail);
});

async function _openDetailSheet(ac) {
  _activeAircraft = ac;
  mapMod.highlightAircraft(ac.hex);

  const sheet = document.getElementById('detail-sheet');
  if (!sheet) return;

  const s = settings.getSettings();
  const isSaved = saved.isAircraftSaved(ac.hex);

  // Populate static fields immediately
  _populateSheet(ac, s, isSaved, null);
  sheet.classList.remove('hidden');

  // Fetch route info async
  if (ac.callsign) {
    const route = await fetchRouteInfo(ac.callsign).catch(() => null);
    if (_activeAircraft?.hex === ac.hex) {
      _populateSheet(ac, s, isSaved, route);
    }
  }
}

function _populateSheet(ac, s, isSaved, route) {
  const sheet = document.getElementById('detail-sheet');
  if (!sheet) return;

  const vr = ac.vertRateFpm;
  const vrStr = vr != null ? (vr > 0 ? '+' : '') + Math.round(vr).toLocaleString() + ' fpm' : '—';
  const cardClass = heading => `font-data-lg text-data-lg ${heading}`;

  const routeSection = route
    ? `<div class="relative h-20 rounded-xl border border-glass-stroke overflow-hidden bg-surface-container-lowest flex items-center px-4">
        <div class="absolute inset-0 bg-gradient-to-r from-surface-container-lowest via-transparent to-surface-container-lowest"></div>
        <div class="relative z-10 w-full flex items-center justify-between">
          <div class="flex flex-col items-center">
            <span class="font-headline-lg-mobile text-headline-lg-mobile text-text-high">${esc(route.originCode || '?')}</span>
            <span class="font-label-sm text-label-sm text-outline mt-1">${esc(route.originCity || route.originName || '')}</span>
          </div>
          <div class="flex-1 px-4 flex items-center relative">
            <div class="w-full h-[2px] bg-glass-stroke">
              <div class="h-full bg-primary rounded-full w-1/3" style="box-shadow: 0 0 10px rgba(225,253,255,0.8);"></div>
            </div>
            <span class="material-symbols-outlined text-primary absolute left-1/3 -translate-x-1/2 text-[20px] rotate-90"
              style="text-shadow: 0 0 8px rgba(225,253,255,1);">flight</span>
          </div>
          <div class="flex flex-col items-center">
            <span class="font-headline-lg-mobile text-headline-lg-mobile text-text-high">${esc(route.destCode || '?')}</span>
            <span class="font-label-sm text-label-sm text-outline mt-1">${esc(route.destCity || route.destName || '')}</span>
          </div>
        </div>
      </div>`
    : '';

  sheet.innerHTML = `
    <div class="bg-glass-surface backdrop-blur-[20px] border border-glass-stroke rounded-t-3xl md:rounded-2xl p-card-padding flex flex-col gap-4 shadow-2xl">
      <!-- Drag handle -->
      <div class="w-12 h-1.5 bg-outline-variant rounded-full mx-auto -mt-1 cursor-grab"></div>

      <!-- Header -->
      <div class="flex flex-col gap-1 border-b border-glass-stroke pb-3">
        <div class="flex justify-between items-start">
          <h2 id="detail-callsign" class="font-headline-lg-mobile text-headline-lg-mobile text-text-high tracking-tight">${esc(ac.callsign || ac.hex)}</h2>
          <div class="flex items-center gap-2">
            <span class="px-3 py-1 bg-surface-container-high border border-glass-stroke rounded-full font-label-sm text-label-sm text-primary flex items-center gap-1">
              <span class="w-2 h-2 rounded-full bg-primary animate-pulse"></span>LIVE
            </span>
            <button id="detail-close" class="w-8 h-8 rounded-full hover:bg-primary/10 flex items-center justify-center transition-colors">
              <span class="material-symbols-outlined text-on-surface-variant text-sm">close</span>
            </button>
          </div>
        </div>
        <div class="flex items-center gap-2 font-label-sm text-label-sm text-text-med">
          <span id="detail-type" class="font-data-lg text-data-lg text-on-surface-variant opacity-80 text-[13px]">${esc(ac.typeCode || 'Unknown type')}</span>
          <span class="w-1 h-1 bg-outline-variant rounded-full"></span>
          <span class="opacity-70">${esc(ac.hex.toUpperCase())}</span>
          ${ac.originCountry ? `<span class="w-1 h-1 bg-outline-variant rounded-full"></span><span>${esc(ac.originCountry)}</span>` : ''}
        </div>
      </div>

      <!-- Cockpit grid -->
      <div class="grid grid-cols-2 md:grid-cols-3 gap-3">
        <div class="bg-surface-container-low/50 backdrop-blur-sm border border-glass-stroke rounded-xl p-3 flex flex-col gap-1 hover:bg-surface-container-high/50 group transition-all">
          <span class="font-label-sm text-label-sm text-outline flex items-center gap-1">
            <span class="material-symbols-outlined text-[14px]">height</span> Altitude
          </span>
          <span id="detail-alt" class="font-data-lg text-data-lg text-primary drop-shadow-[0_0_8px_rgba(225,253,255,0.2)]">${formatAlt(ac.altFt, s)}</span>
        </div>
        <div class="bg-surface-container-low/50 backdrop-blur-sm border border-glass-stroke rounded-xl p-3 flex flex-col gap-1 hover:bg-surface-container-high/50 group transition-all">
          <span class="font-label-sm text-label-sm text-outline flex items-center gap-1">
            <span class="material-symbols-outlined text-[14px]">speed</span> Speed
          </span>
          <span id="detail-speed" class="font-data-lg text-data-lg text-primary drop-shadow-[0_0_8px_rgba(225,253,255,0.2)]">${formatSpeed(ac.speedKts, s)}</span>
        </div>
        <div class="bg-surface-container-low/50 backdrop-blur-sm border border-glass-stroke rounded-xl p-3 flex flex-col gap-1 hover:bg-surface-container-high/50 group transition-all">
          <span class="font-label-sm text-label-sm text-outline flex items-center gap-1">
            <span class="material-symbols-outlined text-[14px]">explore</span> Heading
          </span>
          <span id="detail-heading" class="font-data-lg text-data-lg text-primary drop-shadow-[0_0_8px_rgba(225,253,255,0.2)]">
            ${ac.headingDeg != null ? Math.round(ac.headingDeg) + '° ' + headingToCard(ac.headingDeg) : '—'}
          </span>
        </div>
        <div class="bg-surface-container-low/50 backdrop-blur-sm border border-glass-stroke rounded-xl p-3 flex flex-col gap-1 hover:bg-surface-container-high/50 group transition-all">
          <span class="font-label-sm text-label-sm text-outline flex items-center gap-1">
            <span class="material-symbols-outlined text-[14px]">swap_vert</span> Vert. Rate
          </span>
          <span id="detail-vr" class="font-data-lg text-data-lg text-primary drop-shadow-[0_0_8px_rgba(225,253,255,0.2)]">${vrStr}</span>
        </div>
        <div class="bg-surface-container-low/50 backdrop-blur-sm border border-glass-stroke rounded-xl p-3 flex flex-col gap-1 hover:bg-surface-container-high/50 group transition-all col-span-2 md:col-span-1">
          <span class="font-label-sm text-label-sm text-outline flex items-center gap-1">
            <span class="material-symbols-outlined text-[14px]">radar</span> Status
          </span>
          <span class="font-data-lg text-data-lg text-on-surface-variant">${ac.onGround ? 'On Ground' : vrLabel(vr)}</span>
        </div>
      </div>

      <!-- Route (shown when available) -->
      ${routeSection}

      <!-- Action buttons -->
      <div class="flex items-center gap-3">
        <button id="detail-save-btn"
          class="flex-1 py-3 px-4 rounded-full border border-glass-stroke bg-surface-container-low hover:bg-surface-container-high transition-colors flex items-center justify-center gap-2 text-on-surface hover:text-primary group">
          <span class="material-symbols-outlined text-[20px]" style="font-variation-settings:'FILL' ${isSaved ? 1 : 0};">star</span>
          <span class="font-title-md text-title-md text-[14px]">${isSaved ? 'Saved' : 'Save'}</span>
        </button>
        <button id="detail-share-btn"
          class="flex-1 py-3 px-4 rounded-full border border-glass-stroke bg-surface-container-low hover:bg-surface-container-high transition-colors flex items-center justify-center gap-2 text-on-surface hover:text-primary group">
          <span class="material-symbols-outlined text-[20px]">share</span>
          <span class="font-title-md text-title-md text-[14px]">Share</span>
        </button>
        <button id="detail-track-btn"
          class="flex-1 py-3 px-4 rounded-full bg-primary text-pitch-black hover:bg-primary-fixed transition-colors flex items-center justify-center gap-2 shadow-[0_4px_20px_rgba(225,253,255,0.15)] active:scale-95">
          <span class="material-symbols-outlined text-[20px]">my_location</span>
          <span class="font-title-md text-title-md text-[14px]">Track</span>
        </button>
      </div>
    </div>`;

  // Bind sheet events
  document.getElementById('detail-close')?.addEventListener('click', _closeDetailSheet);

  document.getElementById('detail-save-btn')?.addEventListener('click', async () => {
    if (!_activeAircraft) return;
    if (saved.isAircraftSaved(_activeAircraft.hex)) {
      await saved.unsaveAircraft(_activeAircraft.hex);
    } else {
      await saved.saveAircraft(_activeAircraft);
    }
    _populateSheet(_activeAircraft, settings.getSettings(), saved.isAircraftSaved(_activeAircraft.hex), route);
  });

  document.getElementById('detail-share-btn')?.addEventListener('click', async () => {
    if (!_activeAircraft) return;
    const shareData = {
      title: `SkyPulse — ${_activeAircraft.callsign || _activeAircraft.hex}`,
      text: `Track ${_activeAircraft.callsign || _activeAircraft.hex} — ${formatAlt(_activeAircraft.altFt, s)} at ${formatSpeed(_activeAircraft.speedKts, s)}`,
      url: window.location.href,
    };
    if (navigator.share) {
      try { await navigator.share(shareData); } catch {}
    } else {
      // Create shared flight in Firestore
      try {
        const id = await firebase.createSharedFlight({
          hex: _activeAircraft.hex,
          callsign: _activeAircraft.callsign,
          altFt: _activeAircraft.altFt,
          speedKts: _activeAircraft.speedKts,
          headingDeg: _activeAircraft.headingDeg,
          lat: _activeAircraft.lat,
          lon: _activeAircraft.lon,
        });
        const shareUrl = `${window.location.origin}/skypulse-android/app/?share=${id}`;
        await navigator.clipboard.writeText(shareUrl).catch(() => {});
        alert(`Share link copied: ${shareUrl}`);
      } catch (err) {
        alert('Could not create share link: ' + err.message);
      }
    }
  });

  document.getElementById('detail-track-btn')?.addEventListener('click', () => {
    if (_activeAircraft?.lat && _activeAircraft?.lon) {
      mapMod.centerOnUser(_activeAircraft.lat, _activeAircraft.lon);
      switchTab('map');
    }
  });

  // Touch swipe to close
  _addSwipeToClose(sheet);
}

function _closeDetailSheet() {
  const sheet = document.getElementById('detail-sheet');
  if (sheet) sheet.classList.add('hidden');
  _activeAircraft = null;
}

function _addSwipeToClose(el) {
  let startY = 0;
  el.addEventListener('touchstart', e => { startY = e.touches[0].clientY; }, { passive: true });
  el.addEventListener('touchend', e => {
    const diff = e.changedTouches[0].clientY - startY;
    if (diff > 80) _closeDetailSheet();
  }, { passive: true });
}

/* ── 12. Map search ── */
let _searchDebounce;
document.getElementById('map-search')?.addEventListener('input', e => {
  clearTimeout(_searchDebounce);
  _searchDebounce = setTimeout(() => {
    const q = e.target.value.trim().toUpperCase();
    if (!q) return;
    const match = _allAircraft.find(a =>
      (a.callsign || '').toUpperCase().includes(q) ||
      (a.hex || '').toUpperCase().includes(q) ||
      (a.typeCode || '').toUpperCase().includes(q)
    );
    if (match) {
      document.dispatchEvent(new CustomEvent('aircraft-selected', { detail: match }));
    }
  }, 300);
});

/* ── 13. My location button ── */
document.getElementById('my-location-btn')?.addEventListener('click', () => {
  const loc = getLocation();
  if (loc) mapMod.centerOnUser(loc.lat, loc.lon);
});

/* ── 14. Auth buttons ── */
['auth-btn', 'auth-btn-mobile'].forEach(id => {
  document.getElementById(id)?.addEventListener('click', () => {
    auth.showModal();
    auth.updateModalForUser(_currentUser);
  });
});

/* ── 15. Settings panel ── */
function openSettings() {
  document.getElementById('settings-panel')?.classList.remove('hidden');
  document.getElementById('settings-overlay')?.classList.remove('hidden');
}
document.getElementById('settings-btn')?.addEventListener('click', openSettings);
document.getElementById('settings-btn-mobile')?.addEventListener('click', openSettings);
document.getElementById('settings-overlay')?.addEventListener('click', () => {
  document.getElementById('settings-panel')?.classList.add('hidden');
  document.getElementById('settings-overlay')?.classList.add('hidden');
});

/* ── 16. aircraft-save event (from detail sheet or other places) ── */
document.addEventListener('aircraft-save', async e => {
  await saved.saveAircraft(e.detail);
});

/* ── Initial tab ── */
switchTab('map');
