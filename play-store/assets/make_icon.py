"""Rasterize the SkyPulse adaptive launcher icon to a 512x512 Play Store icon.

Reproduces app/src/main/res/drawable/ic_launcher_foreground.xml (the Material
"flight" glyph, cyan #00DBE7) on the launcher background color #0E131E, using the
exact same vector path + group transform so the store icon matches the device icon.
"""
import cairosvg

# Foreground vector: viewport 108x108, group scale 2.6 translate 20.5,
# path fill #00DBE7. Background color from colors.xml (#FF0E131E).
SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 108 108">
  <rect x="0" y="0" width="108" height="108" fill="#0E131E"/>
  <g transform="translate(20.5,20.5) scale(2.6)">
    <path fill="#00DBE7" d="M21,16v-2l-8,-5V3.5C13,2.67 12.33,2 11.5,2S10,2.67 10,3.5V9l-8,5v2l8,-2.5V19l-2,1.5V22l3.5,-1 3.5,1v-1.5L13,19v-5.5L21,16z"/>
  </g>
</svg>"""

cairosvg.svg2png(bytestring=SVG.encode("utf-8"),
                 write_to=r"h:\flight-tracker\play-store\icon\icon-512.png",
                 output_width=512, output_height=512)
print("wrote icon-512.png")
