/* settings.js — App settings panel */

const LS_KEY = 'sp_settings';

const DEFAULTS = {
  altUnit:         'ft',    // 'ft' | 'm'
  speedUnit:       'kts',   // 'kts' | 'mph' | 'kmh'
  distUnit:        'nm',    // 'nm' | 'mi' | 'km'
  refreshInterval: 10,      // seconds
};

let _settings = { ...DEFAULTS };
let _panelId = null;
let _onChange = null;

/**
 * Initialize settings panel.
 * @param {string} panelId  ID of the settings container element
 * @param {Function} [onChange]  called with new settings when they change
 */
export function init(panelId, onChange) {
  _panelId = panelId;
  _onChange = onChange;
  _load();
  _render();
}

export function getSettings() {
  return { ..._settings };
}

function _load() {
  try {
    const raw = localStorage.getItem(LS_KEY);
    if (raw) _settings = { ...DEFAULTS, ...JSON.parse(raw) };
  } catch {}
}

function _save() {
  try {
    localStorage.setItem(LS_KEY, JSON.stringify(_settings));
    _onChange?.(_settings);
  } catch {}
}

function _render() {
  const c = document.getElementById(_panelId);
  if (!c) return;

  c.innerHTML = `
    <div class="flex items-center justify-between mb-6">
      <h2 class="font-headline-lg-mobile text-headline-lg-mobile text-primary font-bold">Settings</h2>
      <button id="settings-close-btn" class="w-8 h-8 rounded-full hover:bg-primary/10 flex items-center justify-center transition-colors">
        <span class="material-symbols-outlined text-on-surface-variant">close</span>
      </button>
    </div>

    <!-- Data Sources -->
    <section class="mb-6">
      <h3 class="font-title-md text-title-md text-primary mb-3">Data Sources</h3>
      <div class="bg-glass-surface backdrop-blur-xl border border-glass-stroke rounded-xl p-card-padding">
        <ul class="space-y-3">
          <li class="flex items-center justify-between">
            <span class="font-body-md text-body-md text-on-surface-variant">ADSB.lol</span>
            <span class="bg-primary/10 text-primary border border-primary/30 px-3 py-1 rounded-full font-label-sm text-label-sm">Primary</span>
          </li>
          <li class="flex items-center justify-between">
            <span class="font-body-md text-body-md text-on-surface-variant">OpenFlights DB</span>
            <span class="bg-surface-container-high text-text-med border border-glass-stroke px-3 py-1 rounded-full font-label-sm text-label-sm">Active</span>
          </li>
        </ul>
      </div>
    </section>

    <!-- Preferences -->
    <section class="mb-6">
      <h3 class="font-title-md text-title-md text-primary mb-3">Preferences</h3>
      <div class="bg-glass-surface backdrop-blur-xl border border-glass-stroke rounded-xl p-card-padding space-y-5">

        <!-- Refresh Interval -->
        <div>
          <label class="font-title-md text-title-md text-text-high block mb-2">Refresh Interval</label>
          <div class="grid grid-cols-4 gap-1 border border-glass-stroke rounded-lg overflow-hidden bg-surface-container">
            ${[5, 10, 30, 60].map(s => `
              <button class="settings-seg-btn py-3 font-data-lg text-data-lg ${_settings.refreshInterval === s ? 'active' : 'text-on-surface-variant hover:bg-white/5'} transition-colors"
                data-setting="refreshInterval" data-val="${s}">${s}s</button>`).join('')}
          </div>
        </div>

        <!-- Altitude Units -->
        <div>
          <label class="font-title-md text-title-md text-text-high block mb-2">Altitude</label>
          <div class="grid grid-cols-2 gap-1 border border-glass-stroke rounded-lg overflow-hidden bg-surface-container">
            <button class="settings-seg-btn py-3 font-body-md text-body-md ${_settings.altUnit === 'ft' ? 'active' : 'text-on-surface-variant hover:bg-white/5'} transition-colors"
              data-setting="altUnit" data-val="ft">Feet</button>
            <button class="settings-seg-btn py-3 font-body-md text-body-md ${_settings.altUnit === 'm' ? 'active' : 'text-on-surface-variant hover:bg-white/5'} transition-colors"
              data-setting="altUnit" data-val="m">Meters</button>
          </div>
        </div>

        <!-- Speed Units -->
        <div>
          <label class="font-title-md text-title-md text-text-high block mb-2">Speed</label>
          <div class="grid grid-cols-3 gap-1 border border-glass-stroke rounded-lg overflow-hidden bg-surface-container">
            ${[['kts','Knots'],['mph','MPH'],['kmh','km/h']].map(([v, l]) => `
              <button class="settings-seg-btn py-3 font-body-md text-body-md ${_settings.speedUnit === v ? 'active' : 'text-on-surface-variant hover:bg-white/5'} transition-colors"
                data-setting="speedUnit" data-val="${v}">${l}</button>`).join('')}
          </div>
        </div>

        <!-- Distance Units -->
        <div>
          <label class="font-title-md text-title-md text-text-high block mb-2">Distance</label>
          <div class="grid grid-cols-3 gap-1 border border-glass-stroke rounded-lg overflow-hidden bg-surface-container">
            ${[['nm','NM'],['mi','Miles'],['km','km']].map(([v, l]) => `
              <button class="settings-seg-btn py-3 font-body-md text-body-md ${_settings.distUnit === v ? 'active' : 'text-on-surface-variant hover:bg-white/5'} transition-colors"
                data-setting="distUnit" data-val="${v}">${l}</button>`).join('')}
          </div>
        </div>
      </div>
    </section>

    <!-- Privacy notice -->
    <section class="mb-6">
      <div class="bg-glass-surface backdrop-blur-xl border border-glass-stroke rounded-xl p-card-padding flex items-start gap-3">
        <span class="material-symbols-outlined text-primary mt-1" style="font-variation-settings:'FILL' 0;">shield</span>
        <div>
          <h3 class="font-title-md text-title-md text-text-high mb-1">Privacy Focus</h3>
          <p class="font-body-md text-body-md text-on-surface-variant">Your alerts and preferences stay local to your device.</p>
        </div>
      </div>
    </section>

    <!-- About -->
    <section class="text-center">
      <p class="font-label-sm text-label-sm text-text-med uppercase tracking-wider">SkyPulse Web v1.0</p>
      <p class="font-label-sm text-label-sm text-on-surface-variant mt-1">Powered by open ADS-B data</p>
    </section>`;

  _bindEvents();
}

function _bindEvents() {
  // Segmented control buttons
  document.querySelectorAll('.settings-seg-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const setting = btn.dataset.setting;
      const val = btn.dataset.val;
      if (!setting || val === undefined) return;

      // Parse numeric values
      _settings[setting] = isNaN(Number(val)) ? val : Number(val);
      _save();

      // Update sibling button styles
      const siblings = document.querySelectorAll(`[data-setting="${setting}"]`);
      siblings.forEach(sib => {
        const active = sib.dataset.val === val || Number(sib.dataset.val) === _settings[setting];
        sib.className = sib.className
          .replace(/\bactive\b/g, '')
          .replace(/\btext-on-surface-variant\b/g, '')
          .trim();
        if (active) {
          sib.classList.add('active');
        } else {
          sib.classList.add('text-on-surface-variant');
        }
      });
    });
  });

  // Close button
  document.getElementById('settings-close-btn')?.addEventListener('click', () => {
    document.getElementById('settings-panel')?.classList.add('hidden');
    document.getElementById('settings-overlay')?.classList.add('hidden');
  });
}
