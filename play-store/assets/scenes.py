"""Scene definitions for the SkyPulse promo video (shared by frame + audio build)."""

VOICE = "en-US-AriaNeural"

SCENES = [
    dict(id=1, type="title",
         head="SkyPulse", sub="Live flight tracking, right above you",
         caption="Live flight tracking, right above you",
         vo="This is SkyPulse. Live flight tracking, right above you."),
    dict(id=2, type="shot", file="01_map.png",
         head="Live flights overhead", sub="Real-time aircraft on an interactive map",
         caption="Watch real aircraft move across a live map",
         vo="Open the app and watch real aircraft move across an interactive map, updated live."),
    dict(id=3, type="shot", file="02_nearby.png",
         head="See what's flying nearby", sub="Ranked by distance, altitude and speed",
         caption="Every plane near you, ranked instantly",
         vo="See every plane near you, ranked by distance, altitude, and speed."),
    dict(id=4, type="shot", file="06_detail.png",
         head="Live aircraft details", sub="Altitude, speed, heading and more",
         caption="Tap any aircraft for live flight details",
         vo="Tap any aircraft for live details. Altitude, speed, heading, and vertical rate."),
    dict(id=5, type="shot", file="03_airports.png",
         head="Airports around you", sub="Arrivals, departures and live activity",
         caption="Explore airports, arrivals and departures",
         vo="Explore the airports around you, with arrivals, departures, and live activity."),
    dict(id=6, type="shot", file="04_alerts.png",
         head="Smart flight alerts", sub="Never miss the flights that matter",
         caption="Smart alerts for the flights that matter",
         vo="And set smart alerts, so you never miss the flights that matter to you."),
    dict(id=7, type="outro",
         head="SkyPulse", sub="Free on Google Play",
         caption="Download free on Google Play",
         vo="SkyPulse. Look up, and discover the world flying above you."),
]
