# SkyPulse ✈️

**SkyPulse** is a native Android live aircraft tracker built with Kotlin and Jetpack
Compose. It shows aircraft around you on an OpenStreetMap map using **only free, open,
no-key data sources** — no FlightAware, FlightRadar24, AviationStack, RapidAPI, or paid
Google Maps APIs, and **no account or login**.

<p align="center">
  <em>Futuristic glassmorphism HUD · Map-first · Privacy-first · 100% free data</em>
</p>

---

## No API key required

SkyPulse needs **no API key, no account, and no secret token**. Just build and run.

| Layer | Source | Notes |
|------|--------|-------|
| Primary live aircraft | [**ADSB.lol**](https://www.adsb.lol/) public API | Radius search around your location |
| Fallback live aircraft | [**OpenSky Network**](https://opensky-network.org/) anonymous REST | Rate-limited → cached & throttled |
| Airports & airlines | [**OpenFlights**](https://openflights.org/data.php) | Bundled as assets, imported into Room on first launch |
| Map tiles | **OpenStreetMap** via osmdroid | No Google Maps billing |

### Limits of free ADS-B data (please read)

Free, open ADS-B feeds are best-effort. In particular:

- **Not every aircraft is guaranteed** to appear — coverage depends on nearby receivers.
- **Coverage varies by area and time**; data can be **delayed or missing**.
- **Some military or private aircraft may be hidden or incomplete.**
- **Gate, boarding, and route status are not provided** by free feeds — SkyPulse shows
  position/altitude/speed/heading only, and clearly says when route data is unavailable.
- OpenSky anonymous access is **rate-limited**, so SkyPulse caches results and throttles
  requests (minimum ~8s between network calls, configurable up to 30s).

---

## Features

- **Live map** (osmdroid) with aircraft markers rotated to heading; tap for details.
- **Aircraft detail** "cockpit" sheet: altitude, speed, heading, vertical rate, last seen.
- **Nearby** list with Closest / Highest / Fastest / Recent sorting (Haversine distance).
- **Airport lookup** over the full OpenFlights dataset: search by name, city, country,
  IATA, or ICAO; "airports near you"; "view nearby aircraft".
- **Local alerts** (no backend): aircraft enters radius, specific callsign appears,
  low-altitude flight nearby, activity near a saved airport — evaluated on-device by
  WorkManager and shown as local notifications.
- **Saved** aircraft, airports, and areas (Room).
- **Settings** (DataStore): refresh interval, distance/altitude/speed units, data-source
  status, clear cache.
- **Resilient**: loading / empty / error / offline-cached states everywhere; works with
  cached data when offline.
- **Monetization-ready** (no billing yet): `EntitlementRepository` / `FreeEntitlementRepository`
  stubs for future premium unlocks.

## Architecture

- **Kotlin · Jetpack Compose · Material 3 · MVVM**
- **Hilt** for DI, **Kotlin Coroutines/Flow** for async/state
- **Retrofit + kotlinx.serialization** networking; endpoints centralized in
  `data/remote/ApiEndpoints.kt` (★ adjust here if a public API changes)
- **Room** (saved items, alert rules, cached snapshots, OpenFlights data) + **DataStore**
- **WorkManager** for periodic local alert checks
- **osmdroid** for OpenStreetMap tiles

```
com.charles.skypulse.app
├── data/ (remote, local, repository, settings, firebase, location)
├── domain/ (model, util, entitlement)
├── worker/ (alert worker, scheduler, notifications)
└── ui/ (theme, navigation, components, screens)
```

## Firebase (free Spark plan, no login)

SkyPulse integrates Firebase on the **free Spark tier** — nothing requires a user login
and no Blaze-only resources are used:

- **Crashlytics** — anonymous crash reporting.
- **Analytics** — aggregate, non-personal product events (no PII).
- **Remote Config** — feature flags / tunables (refresh floor, fallback toggle); always
  falls back to in-code defaults offline.
- **Cloud Messaging (FCM)** — optional push for announcements; the primary alerting path
  remains on-device WorkManager.

A working `app/google-services.json` is committed for the demo project. **To use your own
Firebase project:** create a project + Android app (`com.charles.skypulse.app`) in the
Firebase console, download your `google-services.json`, and replace `app/google-services.json`.

## Build & run

### In Android Studio
1. Open the project root in **Android Studio** (Koala or newer).
2. Let Gradle sync (JDK 17). The Android SDK 35 platform is required.
3. Select the **app** configuration and press **Run**.

### From the command line
```bash
./gradlew assembleDebug          # build the debug APK
./gradlew test                   # run JVM unit tests
./gradlew installDebug           # install on a connected device
```

### Run on a real Android phone
1. Enable **Developer options → USB debugging** on the phone.
2. Connect via USB and accept the debugging prompt.
3. `./gradlew installDebug` (or Run from Android Studio).
4. Grant **Location** (and, on Android 13+, **Notifications**) when prompted.

### Required permissions
| Permission | Why |
|-----------|-----|
| `INTERNET`, `ACCESS_NETWORK_STATE` | Fetch live aircraft data |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | Find aircraft/airports near you |
| `POST_NOTIFICATIONS` (Android 13+) | Local alert notifications |

- **Minimum SDK:** 26 (Android 8.0)  ·  **Target/Compile SDK:** 35

## Privacy

No account, no login. Saved items and alert rules stay in an on-device database and are
never uploaded by the app. Location is used only on-device for nearby aircraft and alerts.
See the in-app **Privacy** screen for the full statement.

## Data attribution & licenses

- Aircraft data © [ADSB.lol](https://www.adsb.lol/) and the
  [OpenSky Network](https://opensky-network.org/) (anonymous access).
- Airport/airline data from [OpenFlights](https://openflights.org/data.php).
- Map tiles © [OpenStreetMap](https://www.openstreetmap.org/copyright) contributors.

This project is for educational/enthusiast use. Respect each data provider's terms.
