"""Compose polished 1080x1920 Play Store phone screenshots from the real
Pixel 8 Pro captures: brand background + marketing caption + framed screenshot."""
import sys
from PIL import Image, ImageDraw
import brand as B

RAW = r"h:\flight-tracker\play-store\screenshots\phone-raw"
OUT = r"h:\flight-tracker\play-store\screenshots\phone"
W, H = 1080, 1920

# (raw_file, out_name, top_crop, bottom_crop, headline, subhead)
SCREENS = [
    ("01_map.png",     "01_map.png",     66, 1802, "Live flights overhead",       "Real-time aircraft on an interactive map"),
    ("02_nearby.png",  "02_nearby.png",  66, 1802, "See what's flying nearby",    "Ranked by distance, altitude and speed"),
    ("06_detail.png",  "03_detail.png",  66, 2186, "Tap any aircraft for details","Altitude, speed, heading and vertical rate"),
    ("03_airports.png","04_airports.png",66, 1802, "Airports around you",         "Arrivals, departures and live activity"),
    ("04_alerts.png",  "05_alerts.png",  66, 1802, "Smart flight alerts",         "Get notified about the flights that matter"),
]

def fit(size, maxw, maxh):
    w, h = size; k = min(maxw/w, maxh/h)
    return int(w*k), int(h*k)

def build(idx, fn, out_name, top, bot, head, sub):
    bg = B.vgradient(W, H)
    B.radar_rings(bg, W//2, 1180, rings=6, step=150)
    B.scatter_planes(bg, [
        (170, 250, 60, 25, 60), (930, 360, 46, -15, 50),
        (120, 1500, 52, 40, 45), (980, 1620, 64, -30, 55),
    ])
    d = ImageDraw.Draw(bg)
    # centered wordmark at top
    wm_w = d.textbbox((0, 0), "SkyPulse", font=B.font(B.BLACK, 40))[2]
    B.wordmark(d, (W - wm_w)//2, 84, size=40)
    head_f = B.font(B.BLACK, 60)
    sub_f = B.font(B.SEMI, 33)
    # screenshot card
    shot = Image.open(f"{RAW}\\{fn}").convert("RGB").crop((0, top, 1008, bot))
    cw, ch = fit(shot.size, 880, 1470)
    shot = shot.resize((cw, ch), Image.LANCZOS)
    card_y = H - ch - 70
    card_x = (W - cw)//2
    B.paste_card(bg, shot, card_x, card_y, radius=46, border=(0,90,100))
    # caption block centered above the card
    d = ImageDraw.Draw(bg)
    cap_top = 168
    B.text_center(d, W//2, cap_top, head, head_f, fill=B.WHITE)
    B.text_center(d, W//2, cap_top+84, sub, sub_f, fill=B.CYAN)
    bg.save(f"{OUT}\\{out_name}")
    print("wrote", out_name, "card", (cw, ch))
if __name__ == "__main__":
    for i, s in enumerate(SCREENS, 1):
        build(i, *s)
