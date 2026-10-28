"""Regenerates the launcher icon backgrounds and the pre-Android-8 icons (needs Pillow).

Run from the repository root: python3 docs/images/make_launcher_icons.py

The icon is the panda on a diagonal gradient (blue for the commercial build, green for foss). The
adaptive foreground and monochrome layers are not touched; this script only rebuilds the gradient
backgrounds (the same stops as ic_launcher_background.xml) and the flat legacy icons that stack the
foreground on top of them. It also writes res/drawable-nodpi/panda_mark.png, the panda cropped out of
the foreground with a small margin, which the splash screen draws.
"""
import os
from PIL import Image, ImageDraw

RES = "app/src"
DENSITY_BG = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}
DENSITY_LEGACY = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
# Same stops as the vector backgrounds: dark at the lower left, light at the upper right.
STOPS = {
    "main": [(0.0, "#0B1736"), (0.55, "#1D4ED8"), (0.75, "#38BDF8"), (1.0, "#EAF6FF")],
    "foss": [(0.0, "#0B361C"), (0.55, "#1DD869"), (0.75, "#3DF838"), (1.0, "#EAFFEC")],
}
# The adaptive icon shows the middle 72 of its 108 units; legacy icons use the same middle area.
VISIBLE_FRACTION = 72 / 108


def rgb(hex_color):
    return tuple(int(hex_color[i:i + 2], 16) for i in (1, 3, 5))


def gradient(size, stops):
    image = Image.new("RGB", (size, size))
    pixels = image.load()
    for y in range(size):
        for x in range(size):
            t = (x + (size - 1 - y)) / (2 * (size - 1))  # 0 at the lower left, 1 at the upper right
            for (a, ca), (b, cb) in zip(stops, stops[1:]):
                if t <= b:
                    f = (t - a) / (b - a)
                    pixels[x, y] = tuple(round(rgb(ca)[i] + (rgb(cb)[i] - rgb(ca)[i]) * f) for i in range(3))
                    break
    return image


def legacy(background, foreground, size, round_shape):
    big = size * 4
    canvas = background.resize((big, big), Image.LANCZOS).convert("RGBA")
    crop = int(foreground.width * VISIBLE_FRACTION)
    offset = (foreground.width - crop) // 2
    panda = foreground.crop((offset, offset, offset + crop, offset + crop)).resize((big, big), Image.LANCZOS)
    canvas.alpha_composite(panda)
    mask = Image.new("L", (big, big), 0)
    draw = ImageDraw.Draw(mask)
    if round_shape:
        draw.ellipse((0, 0, big - 1, big - 1), fill=255)
    else:
        draw.rounded_rectangle((0, 0, big - 1, big - 1), radius=int(big * 0.16), fill=255)
    canvas.putalpha(mask)
    return canvas.resize((size, size), Image.LANCZOS)


foreground = Image.open(f"{RES}/main/res/drawable-xxxhdpi/ic_launcher_foreground.png").convert("RGBA")
for variant, source_set in (("main", "main"), ("foss", "foss")):
    for density, size in DENSITY_BG.items():
        bg = gradient(size, STOPS[variant])
        bg.save(f"{RES}/{source_set}/res/drawable-{density}/ic_launcher_background.png")
    master = gradient(432, STOPS[variant])
    for density, size in DENSITY_LEGACY.items():
        legacy(master, foreground, size, False).save(f"{RES}/{source_set}/res/mipmap-{density}/ic_launcher.png")
        legacy(master, foreground, size, True).save(f"{RES}/{source_set}/res/mipmap-{density}/ic_launcher_round.png")
box = foreground.getbbox()
margin = 8
panda_mark = foreground.crop((box[0] - margin, box[1] - margin, box[2] + margin, box[3] + margin))
os.makedirs(f"{RES}/main/res/drawable-nodpi", exist_ok=True)
panda_mark.save(f"{RES}/main/res/drawable-nodpi/panda_mark.png", optimize=True)
print("done")
