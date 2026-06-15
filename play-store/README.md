# SkyPulse — Play Store Asset Package

Everything needed to publish **SkyPulse** (`com.charles.skypulse.app`) on Google
Play, generated from the live app. Listing copy is in **[LISTING.md](LISTING.md)**.

All graphics use the app's own brand: launcher background `#0E131E`, accent cyan
`#00DBE7`, and the exact plane glyph from `ic_launcher_foreground.xml`.

---

## What's here

| Folder / file | Play Console field | Spec | Status |
|---|---|---|---|
| `LISTING.md` | Title, descriptions, data safety, contact | text | ✅ |
| `icon/icon-512.png` | App icon | 512×512, 32-bit PNG | ✅ |
| `feature-graphic/feature-graphic-1024x500.png` | Feature graphic | 1024×500 PNG | ✅ |
| `screenshots/phone/*.png` | Phone screenshots | 1080×1920 (9:16) ×5 | ✅ |
| `screenshots/tablet-7/*.png` | 7‑inch tablet screenshots | 1920×1200 ×3 | ✅ |
| `screenshots/tablet-10/*.png` | 10‑inch tablet screenshots | 2560×1600 ×3 | ✅ |
| `video/skypulse-promo.mp4` | Promo video (via YouTube) | 1920×1080, ~50s | ✅ |

The icon and store graphics **match the on-device app icon** (same path data,
same colors, rasterized from the vector drawable via `assets/make_icon.py`).

---

## Screenshots — these are REAL

Captured live from the connected **Pixel 8 Pro** (`adb serial 37220DLJG001ML`)
running the installed app, showing real ADS‑B traffic:

| File | Screen | Real content shown |
|---|---|---|
| `phone/01_map.png` | Live map | Real aircraft over Albany NY area, "23 planes" |
| `phone/02_nearby.png` | Nearby list | Live callsigns ranked by distance |
| `phone/03_detail.png` | Aircraft detail | SWA172 — altitude, speed, heading, vert. rate |
| `phone/04_airports.png` | Airports | KSCH / KALB / KGFL with arrivals & departures |
| `phone/05_alerts.png` | Flight alerts | Alert types & toggles |

The raw, unframed captures are kept in `screenshots/phone-raw/` for reference.
The store-ready files add a branded background + marketing caption around the
real screenshot (a raw screenshot is 1008×2244 ≈ 9:20, which is **taller than
Play's 9:16 limit** and would be rejected, so framing is required).

**Tablets:** per the chosen approach, the tablet images re-frame the real phone
captures into landscape tablet showcases (no native 10″ tablet was connected).

---

## Promo video

- **`video/skypulse-promo.mp4`** — 1920×1080, 30 fps, H.264 + AAC, ~49.6 s.
- Contains: branded title card, all five real screenshots with subtle Ken‑Burns
  motion, an outro with the icon + "Free on Google Play" CTA.
- **Voiceover:** natural neural voice (`en-US-AriaNeural`, edge‑tts).
- **Captions:** burned into every scene (bottom caption bar).
- Upload to YouTube, then paste the URL into the listing's **Video** field
  (Play pulls the promo video from YouTube). Suggested title/description are in
  `LISTING.md`.

Source pieces: `video/scenes/` (frames), `video/voiceover/` (per-scene MP3s).

---

## Regenerating (optional)

Scripts live in `assets/` (Python 3 + Pillow, cairosvg, edge‑tts; ffmpeg on PATH):

```bash
cd play-store/assets
python make_icon.py            # 512 icon
python make_feature.py         # feature graphic
python make_screens.py         # phone screenshots  (needs screenshots/phone-raw)
python make_tablets.py         # tablet screenshots
python make_video_frames.py    # video scene frames
python -c "import asyncio,edge_tts;..."   # (voiceover — see history)
python build_video.py          # assemble promo  (add --music for a soft bed)
```

`assets/brand.py` holds the shared palette, plane glyph and layout helpers;
`assets/scenes.py` holds the video script + voiceover lines.

---

## Pre-publish checklist

- [ ] Paste title / short / full description from `LISTING.md`
- [ ] Upload icon, feature graphic, phone + tablet screenshots
- [ ] Upload `skypulse-promo.mp4` to YouTube → add link to listing
- [ ] Complete **Data safety** (see `LISTING.md` — location, analytics, ads)
- [ ] Complete content-rating (IARC) questionnaire → expect *Everyone*
- [ ] Set category **Travel & Local**, mark **Contains ads = Yes**
- [ ] Add privacy policy URL: `https://chartmann1590.github.io/skypulse-android/privacy.html`
- [ ] Upload the signed release **AAB** (App Bundle) and roll out
