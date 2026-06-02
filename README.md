# SkyPulse ✈️

**See the aircraft flying around you, live.** SkyPulse is a free Android app that shows
planes near you on a map in real time — tap any aircraft to see its altitude, speed,
heading, and (when available) where it's flying from and to.

No account. No sign-up. No API key. Just open it and look up.

<p align="center">
  <em>Map-first · Privacy-first · Free</em>
</p>

<p align="center">
  ⬇ <a href="https://github.com/chartmann1590/skypulse-android/releases"><strong>Download the latest build</strong></a> ·
  🌐 <a href="https://chartmann1590.github.io/skypulse-android/">Website</a> ·
  📜 <a href="https://chartmann1590.github.io/skypulse-android/privacy.html">Privacy</a> ·
  ☕ <a href="https://buymeacoffee.com/charleshartmann">Buy me a coffee</a>
</p>

> **Google Play — coming soon.** Until then, install the latest signed APK from the
> [Releases page](https://github.com/chartmann1590/skypulse-android/releases).

---

## What you can do

- 🗺️ **Live map** — watch aircraft move in real time, each marker pointed the way it's flying.
- ✈️ **Tap for details** — altitude, speed, heading, climb/descent, and last-seen time.
- 🧭 **Route & ETA** — where a flight is coming from and heading to, with an estimated
  progress bar and time remaining.
- 📍 **Nearby** — a sorted list of the closest, highest, fastest, or most recent aircraft.
- 🛫 **Airport lookup** — search any airport in the world, or find the ones near you.
- 🔔 **Local alerts** — get a notification when a plane enters your area, a specific
  callsign shows up, or a low flight passes nearby. All handled on your device.
- 🔖 **Save** your favorite aircraft, airports, and areas.

## Free, with optional ads — and a way to turn them off

SkyPulse is completely free. A small banner and the occasional full-screen ad help keep it
that way. If you'd rather not see ads for a while, you can **earn ad-free time**:

- Watch a short rewarded ad to earn **1 credit** (up to **6 per day**).
- Spend a credit for **30 minutes** with no ads.
- Enjoy up to a few hours of ad-free time each day. Credits reset daily.

You'll find this on the **Rewards** screen — tap the credits chip on the map or open it from
**Settings**. It's also explained the first time you open the app.

## A note on the data

SkyPulse uses free, community-powered flight data. That data is wonderful, but it's
best-effort:

- Not every aircraft will appear, and coverage varies by area and time of day.
- Data can be delayed, and some military or private aircraft may be hidden.
- Routes, progress, and ETAs are **estimates** — not official schedules. Please don't use
  them for travel planning.

## Privacy

No account, no login. Your saved items and alerts stay on your device. Your location is used
only on your device to find nearby aircraft and evaluate alerts. The app shows ads via Google
AdMob and collects anonymous diagnostics to stay reliable — full details are in the in-app
**Privacy** screen and the
[Privacy Policy](https://chartmann1590.github.io/skypulse-android/privacy.html).

## Support

If SkyPulse is useful to you, you can support development here:
**[☕ Buy me a coffee](https://buymeacoffee.com/charleshartmann)**. Thank you!

## Data attribution

- Aircraft data © [ADSB.lol](https://www.adsb.lol/) and the
  [OpenSky Network](https://opensky-network.org/).
- Flight routes from Flightradar24's public live feed and [adsbdb](https://www.adsbdb.com/).
- Airport/airline data from [OpenFlights](https://openflights.org/data.php).
- Map tiles © [OpenStreetMap](https://www.openstreetmap.org/copyright) contributors.

This project is for educational/enthusiast use. Please respect each data provider's terms.

---

<details>
<summary><strong>For developers</strong></summary>

### Tech stack
Kotlin · Jetpack Compose · Material 3 · MVVM · Hilt · Coroutines/Flow · Retrofit +
kotlinx.serialization · Room · DataStore · WorkManager · osmdroid · Firebase (Spark tier:
Analytics, Crashlytics, Performance Monitoring, Remote Config, FCM) · Google AdMob + UMP.

```
com.charles.skypulse.app
├── data/    (remote, local, repository, settings, firebase, ads, location)
├── domain/  (model, util, entitlement, ads)
├── worker/  (alert worker, scheduler, notifications)
└── ui/      (theme, navigation, components, ads, screens)
```

### Build & run
```bash
./gradlew assembleDebug          # debug APK (uses Google's AdMob TEST ad IDs)
./gradlew testDebugUnitTest      # JVM unit tests
./gradlew installDebug           # install on a connected device
```
- **Min SDK** 26 · **Target/Compile SDK** 35 · **JDK** 17.
- Debug builds need no secrets — they fall back to AdMob test ads automatically.

### Configuration & secrets (`local.properties`)
Real AdMob unit IDs and the release keystore are **never** committed. Copy
[`local.properties.template`](local.properties.template) to `local.properties` and fill in
your values (AdMob app/banner/interstitial/rewarded IDs, keystore path + passwords). When a
value is absent the build falls back to Google's official test IDs / an unsigned release.

### Firebase
A working `app/google-services.json` is committed for the demo project
(`skypulse-tracker-2026`). To use your own, create a Firebase Android app for
`com.charles.skypulse.app`, download your `google-services.json`, and replace the file.
Enable **Performance Monitoring** in the Firebase console.

### CI / Releases (GitHub Actions)
[`.github/workflows/android.yml`](.github/workflows/android.yml):
- **Every push / PR** → unit tests + debug APK.
- **Push to `main`** → bumps `versionCode` (auto-commit), builds a **signed** release APK +
  AAB using the real AdMob IDs, and publishes a **GitHub Release** with both attached.

Required repository **secrets**: `ADMOB_APP_ID`, `ADMOB_BANNER_ID`, `ADMOB_INTERSTITIAL_ID`,
`ADMOB_REWARDED_ID`, `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

</details>
