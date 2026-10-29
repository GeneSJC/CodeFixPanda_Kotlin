"""Draws the README's launch-screen image (the splash's final frame in a phone) with Pillow.

Run from this folder:  python3 make_splash_still.py
It reuses the panda and fonts from make_art.py and writes splash.png next to this file. The frame is an
illustration drawn from the animation's design (the layout in TitleWordmark), not a capture of the running
app, so the README labels it as one; replace the image with a real screenshot if one is taken. The font is a
stand-in for the app's own, so letter shapes differ slightly.
"""
import math
import os
from PIL import Image, ImageDraw
import make_art as A

HERE = os.path.dirname(os.path.abspath(__file__))
S = 2
W, H = 540, 1100
BG = (245, 242, 252)  # the splash's purple palette: background, primary, text colors
PRIMARY = (110, 95, 209)
INK = (28, 27, 31)
GREY = (73, 69, 79)
F, B = A.font, A.FONT_BOLD
WORD_Y = 660
PANDA_Y = 440


def text_layer(txt, size, fill, spacing=0):
    f = F(B, size * S)
    if spacing:
        w = sum(f.getlength(c) for c in txt) + spacing * S * (len(txt) - 1)
        im = Image.new("RGBA", (int(w) + 8, int(size * S * 1.4)), (0, 0, 0, 0))
        d, x = ImageDraw.Draw(im), 4
        for c in txt:
            d.text((x, 0), c, font=f, fill=fill)
            x += f.getlength(c) + spacing * S
        return im
    bbox = f.getbbox(txt)
    im = Image.new("RGBA", (bbox[2] + 8, int(size * S * 1.4)), (0, 0, 0, 0))
    ImageDraw.Draw(im).text((4, 0), txt, font=f, fill=fill)
    return im


def paste_center(canvas, layer, cx, cy, rot=0, alpha=1.0):
    if rot:
        layer = layer.rotate(rot, expand=True, resample=Image.BICUBIC)
    if alpha < 1:
        layer = layer.copy()
        layer.putalpha(layer.split()[-1].point(lambda v: int(v * alpha)))
    canvas.alpha_composite(layer, (int(cx * S - layer.width / 2), int(cy * S - layer.height / 2)))


def burst(canvas, cx, cy, radius, alpha):
    size = int(radius * 2 * S)
    g = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = g.load()
    for y in range(size):
        for x in range(size):
            r = math.hypot(x - size / 2, y - size / 2) / (size / 2)
            if r >= 1:
                continue
            a = 0.6 + (0.18 - 0.6) * (r / 0.6) if r < 0.6 else 0.18 * (1 - (r - 0.6) / 0.4)
            px[x, y] = PRIMARY + (int(max(a, 0) * 255 * alpha),)
    canvas.alpha_composite(g, (int(cx * S - size / 2), int(cy * S - size / 2)))


def frame(open_dx=0, open_rot=0, src_dx=0, src_a=1, open_a=1, panda_dy=None, burst_a=0, burst_r=0,
          panda_sx=1, panda_sy=1, text_dy=0, text_a=1):
    img = Image.new("RGBA", (W * S, H * S), BG + (255,))
    if burst_a:
        burst(img, 270, PANDA_Y, burst_r, burst_a)
    ow, sw = text_layer("Open", 58, INK + (255,)), text_layer("Source", 58, PRIMARY + (255,))
    total = (ow.width + sw.width) / S + 14
    ox, sx = 270 - total / 2 + ow.width / S / 2, 270 + total / 2 - sw.width / S / 2
    if open_a:
        paste_center(img, ow, ox + open_dx, WORD_Y, open_rot, open_a)
    if src_a:
        paste_center(img, sw, sx + src_dx, WORD_Y, 0, src_a)
    if text_a:
        paste_center(img, text_layer("PANDA", 26, GREY + (255,), spacing=9), 270, WORD_Y + 78 + text_dy, 0, text_a)
    if panda_dy is not None:
        p = A.panda(250 * S)
        p = p.resize((int(p.width * panda_sx), int(p.height * panda_sy)), Image.LANCZOS)
        img.alpha_composite(p, (int(270 * S - p.width / 2), int((PANDA_Y + panda_dy) * S - p.height / 2)))
    return img.convert("RGB").resize((W, H), Image.LANCZOS)


def phone_still():
    """The final frame inside the same dark phone bezel the placeholder screens use."""
    pw, ph, bezel = 540 * S, 1100 * S, 14 * S
    out = Image.new("RGBA", (pw, ph), (0, 0, 0, 0))
    ImageDraw.Draw(out).rounded_rectangle((0, 0, pw - 1, ph - 1), radius=70 * S, fill=A.INK)
    screen = frame(panda_dy=0).resize((pw - 2 * bezel, ph - 2 * bezel), Image.LANCZOS).convert("RGBA")
    mask = Image.new("L", screen.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, screen.width - 1, screen.height - 1), radius=58 * S, fill=255)
    out.paste(screen, (bezel, bezel), mask)
    return out.resize((540, 1100), Image.LANCZOS)


if __name__ == "__main__":
    phone_still().save(os.path.join(HERE, "splash.png"), optimize=True)
    print("done")
