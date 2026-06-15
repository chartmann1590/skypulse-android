/* SkyPulse Service Worker */
const SHELL_CACHE = 'skypulse-shell-v2';
const SHELL_URLS = [
  '/skypulse-android/app/',
  '/skypulse-android/app/index.html',
  '/skypulse-android/app/css/app.css',
  '/skypulse-android/app/js/app.js',
  '/skypulse-android/app/js/firebase.js',
  '/skypulse-android/app/js/api.js',
  '/skypulse-android/app/js/map.js',
  '/skypulse-android/app/js/nearby.js',
  '/skypulse-android/app/js/airports.js',
  '/skypulse-android/app/js/alerts.js',
  '/skypulse-android/app/js/saved.js',
  '/skypulse-android/app/js/auth.js',
  '/skypulse-android/app/js/settings.js',
  '/skypulse-android/app/js/utils.js',
];

const API_ORIGINS = [
  'api.adsb.lol',
  'opensky-network.org',
  'api.adsbdb.com',
];

const FIREBASE_ORIGINS = [
  'firestore.googleapis.com',
  'identitytoolkit.googleapis.com',
  'securetoken.googleapis.com',
  'www.gstatic.com',
];

/* ── Install: pre-cache shell ── */
self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(SHELL_CACHE).then(cache => {
      return cache.addAll(SHELL_URLS).catch(err => {
        // Non-fatal: might fail in dev with localhost redirects
        console.warn('[SW] Shell pre-cache partial failure:', err);
      });
    }).then(() => self.skipWaiting())
  );
});

/* ── Activate: delete old caches ── */
self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys().then(keys =>
      Promise.all(
        keys
          .filter(k => k !== SHELL_CACHE)
          .map(k => caches.delete(k))
      )
    ).then(() => self.clients.claim())
  );
});

/* ── Fetch strategy ── */
self.addEventListener('fetch', event => {
  const url = new URL(event.request.url);

  // Skip non-GET and browser-extension requests
  if (event.request.method !== 'GET') return;
  if (!url.protocol.startsWith('http')) return;

  // ADS-B APIs → don't intercept; let browser fetch directly so CORS and
  // network errors are handled by api.js rather than the SW context
  if (API_ORIGINS.some(o => url.hostname.includes(o))) {
    return;
  }

  // Firebase / gstatic CDN → network-first with cache fallback
  if (FIREBASE_ORIGINS.some(o => url.hostname.includes(o))) {
    event.respondWith(
      fetch(event.request)
        .then(response => {
          const clone = response.clone();
          caches.open(SHELL_CACHE).then(c => c.put(event.request, clone));
          return response;
        })
        .catch(() => caches.match(event.request))
    );
    return;
  }

  // Shell URLs (our own origin) → cache-first
  if (SHELL_URLS.some(p => url.pathname === p || url.pathname.startsWith(p.replace('/index.html', '')))) {
    event.respondWith(
      caches.match(event.request).then(cached => {
        if (cached) return cached;
        return fetch(event.request).then(response => {
          const clone = response.clone();
          caches.open(SHELL_CACHE).then(c => c.put(event.request, clone));
          return response;
        });
      })
    );
    return;
  }

  // Everything else (CDN assets, fonts) → network-first with cache fallback
  event.respondWith(
    fetch(event.request)
      .then(response => {
        if (response.ok) {
          const clone = response.clone();
          caches.open(SHELL_CACHE).then(c => c.put(event.request, clone));
        }
        return response;
      })
      .catch(() => caches.match(event.request))
  );
});
