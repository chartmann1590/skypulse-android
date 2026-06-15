"""Tablet Play Store screenshots (7" and 10"): landscape showcase compositions
that feature the real Pixel phone captures side-by-side on the brand background."""
from PIL import Image, ImageDraw
import brand as B

RAW = r"h:\flight-tracker\play-store\screenshots\phone-raw"

# crops: file -> (top, bottom)
CROP = {
    "01_map.png": (66, 1802), "02_nearby.png": (66, 1802),
    "03_airports.png": (66, 1802), "04_alerts.png": (66, 1802),
    "06_detail.png": (66, 2186),
}

# (headline, [left_file, right_file])
PANELS = [
    ("Track every flight in real time", ["01_map.png", "02_nearby.png"]),
    ("Live aircraft details at a glance", ["06_detail.png", "03_airports.png"]),
    ("Smart alerts for flights that matter", ["04_alerts.png", "01_map.png"]),
]

def shot(fn):
    t, b = CROP[fn]
    return Image.open(f"{RAW}\\{fn}").convert("RGB").crop((0, t, 1008, b))

def make(W, H, head, files, outpath):
    k = H / 1600.0
    bg = B.vgradient(W, H)
    B.radar_rings(bg, W//2, int(H*0.58), rings=7, step=int(150*k))
    B.scatter_planes(bg, [
        (int(W*0.10), int(H*0.18), int(70*k), 22, 55),
        (int(W*0.90), int(H*0.20), int(54*k), -18, 50),
        (int(W*0.08), int(H*0.82), int(60*k), 35, 45),
        (int(W*0.93), int(H*0.80), int(74*k), -28, 55),
    ])
    d = ImageDraw.Draw(bg)
    wm = B.font(B.BLACK, int(46*k))
    wm_w = d.textbbox((0, 0), "SkyPulse", font=wm)[2]
    B.wordmark(d, (W-wm_w)//2, int(70*k), size=int(46*k))
    B.text_center(d, W//2, int(150*k), head, B.font(B.BLACK, int(72*k)), fill=B.WHITE)
    # two phone cards, centered, side by side
    card_h = int(H*0.66)
    ims = [shot(f) for f in files]
    cards = []
    for im in ims:
        cw = int(card_h * im.size[0] / im.size[1])
        cards.append(im.resize((cw, card_h), Image.LANCZOS))
    gap = int(90*k)
    total = sum(c.width for c in cards) + gap*(len(cards)-1)
    x = (W - total)//2
    y = int(H*0.30)
    for c in cards:
        B.paste_card(bg, c, x, y, radius=int(40*k), border=(0, 90, 100))
        x += c.width + gap
    bg.save(outpath)
    print("wrote", outpath, (W, H))

if __name__ == "__main__":
    for i, (head, files) in enumerate(PANELS, 1):
        make(2560, 1600, head, files, rf"h:\flight-tracker\play-store\screenshots\tablet-10\{i:02d}.png")
        make(1920, 1200, head, files, rf"h:\flight-tracker\play-store\screenshots\tablet-7\{i:02d}.png")
