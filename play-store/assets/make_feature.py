"""1024x500 Play Store feature graphic for SkyPulse."""
from PIL import Image, ImageDraw, ImageFilter
import brand as B

W, H = 1024, 500
bg = B.vgradient(W, H, top=(8,12,20), mid=(13,18,29), bot=(17,25,41))

# radar rings centered on the right, with a glowing plane
cx, cy = 800, 250
B.radar_rings(bg, cx, cy, rings=6, step=66, base_alpha=46)
# faint scattered planes around the canvas
B.scatter_planes(bg, [
    (150, 110, 40, 22, 55), (300, 400, 30, -16, 45),
    (980, 90, 34, -25, 50), (940, 430, 44, 30, 55), (640, 70, 26, 12, 40),
])

# glowing hero plane inside the rings
glow = Image.new("RGBA", (W, H), (0,0,0,0))
gp = B.plane_png(250, color="rgb(0,219,231)")
glow.alpha_composite(gp, (cx-125, cy-125))
glow_blur = glow.filter(ImageFilter.GaussianBlur(22))
bg.paste(Image.alpha_composite(bg.convert("RGBA"), glow_blur).convert("RGB"), (0,0))
bg.paste(Image.alpha_composite(bg.convert("RGBA"), glow).convert("RGB"), (0,0))

d = ImageDraw.Draw(bg)
# left wordmark + tagline
f_word = B.font(B.BLACK, 92)
x0, y0 = 70, 150
d.text((x0, y0), "Sky", font=f_word, fill=B.WHITE)
w1 = d.textbbox((0,0), "Sky", font=f_word)[2]
d.text((x0+w1, y0), "Pulse", font=f_word, fill=B.CYAN)
# accent underline
d.rounded_rectangle([x0, y0+118, x0+250, y0+126], 4, fill=B.CYAN)
# tagline
d.text((x0, y0+150), "Live flight tracking,", font=B.font(B.SEMI, 38), fill=B.WHITE)
d.text((x0, y0+196), "right above you.", font=B.font(B.SEMI, 38), fill=B.GREY)

bg.save(r"h:\flight-tracker\play-store\feature-graphic\feature-graphic-1024x500.png")
print("wrote feature graphic")
