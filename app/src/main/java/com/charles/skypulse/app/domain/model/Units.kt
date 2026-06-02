package com.charles.skypulse.app.domain.model

enum class DistanceUnit(val label: String) { MILES("mi"), KILOMETERS("km"), NAUTICAL("NM") }
enum class AltitudeUnit(val label: String) { FEET("ft"), METERS("m") }
enum class SpeedUnit(val label: String) { KNOTS("kts"), MPH("mph") }

/** Sort options on the Nearby screen. */
enum class AircraftSort { CLOSEST, HIGHEST, FASTEST, RECENT }

/** Type of a saved item on the Saved screen. */
enum class SavedKind { AIRCRAFT, AIRPORT, AREA }
