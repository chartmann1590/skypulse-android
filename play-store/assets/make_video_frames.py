"""Render 1920x1080 promo-video scene frames with burned-in captions."""
from PIL import Image, ImageDraw, ImageFilter
import brand as B
from scenes import SCENES

RAW = r"h:\flight-tracker\play-store\screenshots\phone-raw"
OUT = r"h:\flight-tracker\play-store\video\scenes"
W, H = 1920, 1080
CROP = {"01_map.png": (66,1802), "02_nearby.png": (66,1802),
        "03_airports.png": (66,1802), "04_alerts.png": (66,1802),
        "06_detail.png": (66,2186)}

def wrap(draw, text, fnt, maxw):
    words, lines, cur = text.split(), [], ""
    for w in words:
        t = (cur + " " + w).strip()
        if draw.textlength(t, font=fnt) <= maxw:
            cur = t
        else:
            lines.append(cur); cur = w
    if cur: lines.append(cur)
    return lines

def base_bg():
    bg = B.vgradient(W, H)
    B.radar_rings(bg, int(W*0.72), int(H*0.5), rings=7, step=130)
    B.scatter_planes(bg, [
        (170, 180, 60, 22, 55), (1780, 150, 46, -18, 48),
        (120, 920, 52, 35, 45), (1820, 940, 64, -28, 52),
    ])
    return bg

def caption_bar(bg, text):
    d = ImageDraw.Draw(bg, "RGBA")
    f = B.font(B.SEMI, 40)
    tw = d.textlength(text, font=f)
    pad = 36; bw = tw + pad*2; bh = 76
    x = (W - bw)//2; y = H - 120
    d.rounded_rectangle([x, y, x+bw, y+bh], 38, fill=(6, 10, 16, 205),
                        outline=(0, 219, 231, 120), width=2)
    # small cyan dot
    d.ellipse([x+22, y+bh//2-7, x+36, y+bh//2+7], fill=B.CYAN)
    d.text((x+pad+26, y+15), text, font=f, fill=B.WHITE)

def draw_shot(bg, fn):
    t, b = CROP[fn]
    shot = Image.open(f"{RAW}\\{fn}").convert("RGB").crop((0, t, 1008, b))
    ch = 820
    cw = int(ch * shot.size[0] / shot.size[1])
    shot = shot.resize((cw, ch), Image.LANCZOS)
    x = W - cw - 150; y = 80
    B.paste_card(bg, shot, x, y, radius=40, border=(0, 90, 100))

def title_like(bg, head, sub, big=True):
    d = ImageDraw.Draw(bg)
    cx = W//2
    # glowing plane behind
    glow = Image.new("RGBA", (W, H), (0,0,0,0))
    gp = B.plane_png(360, color="rgb(0,219,231)")
    glow.alpha_composite(gp, (cx-180, 250-60))
    bg.paste(Image.alpha_composite(bg.convert("RGBA"),
             glow.filter(ImageFilter.GaussianBlur(26))).convert("RGB"), (0,0))
    bg.paste(Image.alpha_composite(bg.convert("RGBA"), glow).convert("RGB"), (0,0))
    d = ImageDraw.Draw(bg)
    f = B.font(B.BLACK, 150)
    w1 = d.textlength("Sky", font=f); wtot = d.textlength("SkyPulse", font=f)
    x0 = cx - wtot/2; y0 = 560
    d.text((x0, y0), "Sky", font=f, fill=B.WHITE)
    d.text((x0+w1, y0), "Pulse", font=f, fill=B.CYAN)
    B.text_center(d, cx, y0+185, sub, B.font(B.SEMI, 48), fill=B.GREY)

def outro(bg, head, sub):
    d = ImageDraw.Draw(bg)
    cx = W//2
    # app icon tile
    tile = B.vgradient(300, 300, top=(14,19,30), mid=(14,19,30), bot=(14,19,30))
    pl = B.plane_png(190, color="rgb(0,219,231)")
    tile.paste(Image.alpha_composite(tile.convert("RGBA"),
        Image.new("RGBA",(300,300),(0,0,0,0))).convert("RGB"),(0,0))
    ti = tile.convert("RGBA"); ti.alpha_composite(pl, (55, 55))
    B.paste_card(bg, ti.convert("RGB"), cx-150, 210, radius=64, border=(0,120,135))
    d = ImageDraw.Draw(bg)
    f = B.font(B.BLACK, 110)
    w1 = d.textlength("Sky", font=f); wtot = d.textlength("SkyPulse", font=f)
    x0 = cx - wtot/2; y0 = 560
    d.text((x0, y0), "Sky", font=f, fill=B.WHITE)
    d.text((x0+w1, y0), "Pulse", font=f, fill=B.CYAN)
    # CTA pill
    cta = "Free on Google Play"
    cf = B.font(B.SEMI, 46)
    tw = d.textlength(cta, font=cf); bw = tw+90
    bx = cx-bw/2; by = 720
    d.rounded_rectangle([bx, by, bx+bw, by+86], 43, fill=B.CYAN)
    d.text((bx+45, by+18), cta, font=cf, fill=(7,12,20))

def shot_text(bg, head, sub):
    d = ImageDraw.Draw(bg)
    x = 130
    wm = B.font(B.BLACK, 40)
    B.wordmark(d, x, 150, size=40)
    hf = B.font(B.BLACK, 78)
    lines = wrap(d, head, hf, 760)
    y = 270
    for ln in lines:
        d.text((x, y), ln, font=hf, fill=B.WHITE); y += 92
    d.rounded_rectangle([x, y+12, x+120, y+20], 4, fill=B.CYAN)
    sf = B.font(B.SEMI, 40)
    for ln in wrap(d, sub, sf, 760):
        y += 40
        d.text((x, y+18), ln, font=sf, fill=B.CYAN); y += 16

def build(s):
    bg = base_bg()
    if s["type"] == "title":
        title_like(bg, s["head"], s["sub"])
    elif s["type"] == "outro":
        outro(bg, s["head"], s["sub"])
    else:
        draw_shot(bg, s["file"])
        shot_text(bg, s["head"], s["sub"])
    caption_bar(bg, s["caption"])
    p = f"{OUT}\\scene_{s['id']:02d}.png"
    bg.save(p); print("wrote", p)

if __name__ == "__main__":
    for s in SCENES:
        build(s)
