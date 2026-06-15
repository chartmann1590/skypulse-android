"""Shared SkyPulse brand toolkit for Play Store assets.

Colors and the plane glyph are taken directly from the app:
  - launcher background #0E131E  (colors.xml ic_launcher_background)
  - accent cyan        #00DBE7  (ic_launcher_foreground path fill)
"""
import math
from PIL import Image, ImageDraw, ImageFont, ImageFilter

# ---- palette -------------------------------------------------------------
NAVY_TOP = (8, 12, 20)
NAVY_MID = (14, 19, 30)      # #0E131E launcher background
NAVY_BOT = (18, 27, 44)
CYAN = (0, 219, 231)         # #00DBE7
CYAN_DIM = (0, 150, 165)
WHITE = (240, 245, 250)
GREY = (150, 165, 185)

FONTS = r"C:\Windows\Fonts"
def font(name, size):
    return ImageFont.truetype(f"{FONTS}\\{name}", size)
BLACK = "seguibl.ttf"     # Segoe UI Black
BOLD = "segoeuib.ttf"     # Segoe UI Bold
SEMI = "seguisb.ttf"      # Segoe UI Semibold
REG = "segoeui.ttf"       # Segoe UI

# ---- plane glyph (same path as the app launcher) -------------------------
import cairosvg, io
def plane_png(px, color="#00DBE7"):
    svg = f'''<svg xmlns="http://www.w3.org/2000/svg" width="{px}" height="{px}" viewBox="0 0 24 24">
      <path fill="{color}" d="M21,16v-2l-8,-5V3.5C13,2.67 12.33,2 11.5,2S10,2.67 10,3.5V9l-8,5v2l8,-2.5V19l-2,1.5V22l3.5,-1 3.5,1v-1.5L13,19v-5.5L21,16z"/>
    </svg>'''
    return Image.open(io.BytesIO(cairosvg.svg2png(bytestring=svg.encode(), output_width=px, output_height=px))).convert("RGBA")

# ---- backgrounds ---------------------------------------------------------
def vgradient(w, h, top=NAVY_TOP, mid=NAVY_MID, bot=NAVY_BOT):
    img = Image.new("RGB", (w, h))
    px = img.load()
    for y in range(h):
        t = y / (h - 1)
        if t < 0.5:
            k = t / 0.5; c = tuple(int(top[i] + (mid[i]-top[i])*k) for i in range(3))
        else:
            k = (t-0.5)/0.5; c = tuple(int(mid[i] + (bot[i]-mid[i])*k) for i in range(3))
        for x in range(w):
            px[x, y] = c
    return img

def radar_rings(img, cx, cy, rings=5, step=None, color=CYAN, base_alpha=26):
    """Faint concentric radar rings + crosshair, drawn onto an RGB image."""
    w, h = img.size
    step = step or int(min(w, h) * 0.12)
    overlay = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(overlay)
    for i in range(1, rings + 1):
        r = step * i
        a = max(6, base_alpha - i * 3)
        d.ellipse([cx-r, cy-r, cx+r, cy+r], outline=color + (a,), width=2)
    d.line([cx, cy-step*rings, cx, cy+step*rings], fill=color + (12,), width=1)
    d.line([cx-step*rings, cy, cx+step*rings, cy], fill=color + (12,), width=1)
    img.paste(Image.alpha_composite(img.convert("RGBA"), overlay).convert("RGB"), (0, 0))
    return img

def scatter_planes(img, specs, color=CYAN):
    """specs = list of (x, y, size, rot_deg, alpha)."""
    w, h = img.size
    layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    for (x, y, s, rot, a) in specs:
        p = plane_png(s, color=f"rgb({color[0]},{color[1]},{color[2]})")
        if a < 255:
            al = p.split()[3].point(lambda v: int(v * a / 255))
            p.putalpha(al)
        p = p.rotate(rot, expand=True, resample=Image.BICUBIC)
        layer.alpha_composite(p, (int(x - p.width/2), int(y - p.height/2)))
    img.paste(Image.alpha_composite(img.convert("RGBA"), layer).convert("RGB"), (0, 0))
    return img

# ---- helpers -------------------------------------------------------------
def rounded(img, radius):
    mask = Image.new("L", img.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, img.size[0]-1, img.size[1]-1], radius, fill=255)
    out = img.convert("RGBA"); out.putalpha(mask); return out

def drop_shadow(card, radius, blur=40, alpha=160, grow=24):
    w, h = card.size
    sh = Image.new("RGBA", (w + grow*2, h + grow*2), (0, 0, 0, 0))
    m = Image.new("L", (w, h), 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, w-1, h-1], radius, fill=alpha)
    sh.paste((0, 0, 0, alpha), (grow, grow), m)
    return sh.filter(ImageFilter.GaussianBlur(blur))

def paste_card(bg, card, x, y, radius=48, border=CYAN, shadow=True):
    rc = rounded(card, radius)
    if shadow:
        sh = drop_shadow(card, radius, blur=46, alpha=150, grow=30)
        bg.paste(sh, (x-30, y-22), sh)
    bg.paste(rc, (x, y), rc)
    if border:
        ImageDraw.Draw(bg).rounded_rectangle([x, y, x+card.width-1, y+card.height-1],
                                             radius, outline=border, width=2)
    return bg

def text_center(draw, cx, y, s, fnt, fill=WHITE):
    bb = draw.textbbox((0, 0), s, font=fnt)
    draw.text((cx - (bb[2]-bb[0])/2, y), s, font=fnt, fill=fill)
    return y + (bb[3]-bb[1])

def wordmark(draw, x, y, size=40):
    """Draws 'Sky' + cyan 'Pulse' wordmark, returns end x."""
    f = font(BLACK, size)
    draw.text((x, y), "Sky", font=f, fill=WHITE)
    w1 = draw.textbbox((0, 0), "Sky", font=f)[2]
    draw.text((x + w1, y), "Pulse", font=f, fill=CYAN)
    w2 = draw.textbbox((0, 0), "SkyPulse", font=f)[2]
    return x + w2
