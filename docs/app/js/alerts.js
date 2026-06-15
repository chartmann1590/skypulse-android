/* alerts.js — Flight alert configuration and checking */

import { esc, haversineNm } from './utils.js';

const LS_KEY = 'sp_alerts';

const ALERT_DEFAULTS = {
  areaAlert:     { enabled: false, radiusNm: 15 },
  callsignAlert: { enabled: false, callsign: '' },
  altitudeAlert: { enabled: false, thresholdFt: 5000, radiusNm: 5 },
  airportAlert:  { enabled: false },
  departureAlert:{ enabled: false },
  landingAlert:  { enabled: false },
};

let _containerId = null;
let _config = { ...ALERT_DEFAULTS };
let _notifiedHexes = new Set(); // dedupe notifications

/**
 * Initialize the alerts tab.
 */
export function init(containerId) {
  _containerId = containerId;
  _loadConfig();
  _render();
  _requestNotificationPermission();
}

function _loadConfig() {
  try {
    const raw = localStorage.getItem(LS_KEY);
    if (raw) {
      const parsed = JSON.parse(raw);
      _config = { ...ALERT_DEFAULTS, ...parsed };
    }
  } catch {}
}

function _saveConfig() {
  try {
    localStorage.setItem(LS_KEY, JSON.stringify(_config));
  } catch {}
}

function _requestNotificationPermission() {
  if ('Notification' in window && Notification.permission === 'default') {
    Notification.requestPermission();
  }
}

function _render() {
  const c = document.getElementById(_containerId);
  if (!c) return;

  const isIos = /iPhone|iPad/i.test(navigator.userAgent);
  const isStandalone = window.navigator.standalone === true;
  const iosBanner = (isIos && !isStandalone) ? `
    <div class="bg-primary/10 border border-primary/30 rounded-xl p-4 mb-4 flex items-start gap-3">
      <span class="material-symbols-outlined text-primary text-xl shrink-0">ios_share</span>
      <p class="font-body-md text-body-md text-on-surface-variant">
        On iPhone, tap <strong class="text-on-surface">Share → Add to Home Screen</strong> to enable alerts.
      </p>
    </div>` : '';

  c.innerHTML = `
    ${iosBanner}
    <h2 class="font-headline-lg-mobile text-headline-lg-mobile text-text-high mb-2">Flight Alerts</h2>
    <p class="font-body-md text-body-md text-on-surface-variant mb-6">Create local alerts based on live ADS-B data. No account required.</p>
    <div class="flex flex-col gap-4" id="alerts-list">
      ${_alertCard('areaAlert', 'radar', 'Aircraft enters my area', _areaBody())}
      ${_alertCard('callsignAlert', 'flight', 'Specific callsign appears', _callsignBody())}
      ${_alertCard('altitudeAlert', 'vertical_align_bottom', 'Low altitude flight nearby', _altBody())}
      ${_alertCard('airportAlert', 'local_airport', 'Airport activity nearby', '')}
      ${_alertCard('departureAlert', 'flight_takeoff', 'Aircraft departing nearby', '')}
      ${_alertCard('landingAlert', 'flight_land', 'Aircraft landing nearby', '')}
    </div>`;

  _bindEvents();
}

function _alertCard(key, icon, label, body) {
  const enabled = _config[key]?.enabled || false;
  return `
    <div class="bg-glass-surface backdrop-blur-xl border border-glass-stroke rounded-xl p-card-padding" data-alert-key="${esc(key)}">
      <div class="flex justify-between items-center ${body ? 'mb-4' : ''}">
        <div class="flex items-center gap-3">
          <span class="material-symbols-outlined text-primary" style="font-variation-settings:'FILL' 0;">${esc(icon)}</span>
          <h3 class="font-title-md text-title-md text-text-high">${esc(label)}</h3>
        </div>
        <input type="checkbox" class="glass-switch alert-toggle" data-key="${esc(key)}" ${enabled ? 'checked' : ''}/>
      </div>
      ${body ? `<div class="alert-body pl-9 pr-2">${body}</div>` : ''}
    </div>`;
}

function _areaBody() {
  const v = _config.areaAlert.radiusNm;
  return `
    <div class="flex justify-between items-end mb-2">
      <label class="text-on-surface-variant text-sm">Radius</label>
      <span class="font-data-lg text-data-lg text-primary" id="area-radius-val">${v} NM</span>
    </div>
    <input type="range" min="5" max="50" value="${v}"
      class="glass-range w-full" id="area-radius-slider"/>
    <div class="flex justify-between text-xs text-on-surface-variant mt-2 opacity-50">
      <span>5 NM</span><span>50 NM</span>
    </div>`;
}

function _callsignBody() {
  const v = esc(_config.callsignAlert.callsign || '');
  return `
    <input type="text" id="callsign-input"
      class="glass-input w-full p-2 rounded-t-md font-data-lg text-data-lg uppercase tracking-wider"
      placeholder="e.g., N12345 or UAL123" value="${v}"/>`;
}

function _altBody() {
  const ft = _config.altitudeAlert.thresholdFt;
  return `<p class="text-on-surface-variant text-sm mt-1">Below ${ft.toLocaleString()} ft within ${_config.altitudeAlert.radiusNm} NM</p>`;
}

function _bindEvents() {
  // Toggle switches
  document.querySelectorAll('.alert-toggle').forEach(toggle => {
    toggle.addEventListener('change', e => {
      const key = e.target.dataset.key;
      if (_config[key]) _config[key].enabled = e.target.checked;
      _saveConfig();
    });
  });

  // Area radius slider
  const slider = document.getElementById('area-radius-slider');
  const val = document.getElementById('area-radius-val');
  if (slider && val) {
    slider.addEventListener('input', e => {
      _config.areaAlert.radiusNm = Number(e.target.value);
      val.textContent = e.target.value + ' NM';
      _saveConfig();
    });
  }

  // Callsign input
  const callsignInput = document.getElementById('callsign-input');
  if (callsignInput) {
    let _t;
    callsignInput.addEventListener('input', e => {
      clearTimeout(_t);
      _t = setTimeout(() => {
        _config.callsignAlert.callsign = e.target.value.toUpperCase().trim();
        _saveConfig();
      }, 500);
    });
  }
}

/**
 * Check alerts against current aircraft data.
 */
export function checkAlerts(aircraftArray, userLat, userLon, savedAirports = []) {
  if (!Array.isArray(aircraftArray) || userLat == null) return;

  const now = Date.now();

  for (const ac of aircraftArray) {
    if (!ac.lat || !ac.lon) continue;
    const distNm = haversineNm(userLat, userLon, ac.lat, ac.lon);

    // Area alert
    if (_config.areaAlert.enabled && distNm <= _config.areaAlert.radiusNm) {
      _notify(`aircraft-area-${ac.hex}`, `✈ ${ac.callsign || ac.hex} in your area`, `${ac.typeCode || 'Aircraft'} is ${distNm.toFixed(1)} NM away`);
    }

    // Callsign alert
    if (_config.callsignAlert.enabled && _config.callsignAlert.callsign) {
      const target = _config.callsignAlert.callsign.toUpperCase();
      if ((ac.callsign || '').toUpperCase().includes(target)) {
        _notify(`aircraft-callsign-${ac.hex}`, `✈ ${ac.callsign} spotted!`, `${distNm.toFixed(1)} NM away, ${(ac.altFt || 0).toLocaleString()} ft`);
      }
    }

    // Altitude alert
    if (_config.altitudeAlert.enabled &&
        ac.altFt != null && ac.altFt <= _config.altitudeAlert.thresholdFt &&
        distNm <= _config.altitudeAlert.radiusNm && !ac.onGround) {
      _notify(`aircraft-alt-${ac.hex}`, `⚠ Low altitude flight: ${ac.callsign || ac.hex}`,
        `${Math.round(ac.altFt).toLocaleString()} ft, ${distNm.toFixed(1)} NM away`);
    }

    // Landing alert (very low alt + low speed)
    if (_config.landingAlert.enabled && ac.altFt != null && ac.altFt < 1000 && (ac.speedKts || 0) < 180 && !ac.onGround) {
      _notify(`aircraft-land-${ac.hex}`, `🛬 Landing: ${ac.callsign || ac.hex}`, `${distNm.toFixed(1)} NM, ${Math.round(ac.altFt)} ft`);
    }
  }
}

/** Fire a Web Notification (deduplicated by key, 5-min cooldown). */
const _notifyCooldowns = new Map();
function _notify(key, title, body) {
  if (!('Notification' in window) || Notification.permission !== 'granted') return;
  const now = Date.now();
  const last = _notifyCooldowns.get(key) || 0;
  if (now - last < 5 * 60 * 1000) return; // 5 min cooldown
  _notifyCooldowns.set(key, now);
  try {
    new Notification(title, { body, icon: '../icons/icon-192.png', badge: '../icons/icon-192.png' });
  } catch {}
}

export function getConfig() {
  return { ..._config };
}
