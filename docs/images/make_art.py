"""README tooling only: generates the GitHub front page's artwork (banner, divider, placeholder screens, panda)
with Pillow. It is not part of the Android app and nothing here ships in either build flavor.

Run from this folder:  python3 make_art.py
It reads the mascot from panda-source.png (next to this file) and writes PNGs next to this file.
Real screenshots and GIFs replace the placeholder-*.png files; keep the file names, or change the README to match. Fonts: DejaVu Sans Bold and Liberation Sans (Linux paths); edit FONT_* for another machine.
"""
import os
from PIL import Image, ImageDraw, ImageFilter, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
PANDA = os.path.join(HERE, "panda-source.png")
FONT_BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FONT_TEXT = "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf"
FONT_TEXT_BOLD = "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf"

# The app's palette (Color.kt), so the artwork looks like the app: the launcher icon's orange gradient, the sun
# yellow of the glyph, the navy of the panda's outline and the seafoam of its scrubs, and the soft tangerine
# backdrop with its cards and chips.
ORANGE_TOP, ORANGE_MID, ORANGE_BOTTOM = (0xCF, 0x66, 0x57), (0xBC, 0x52, 0x4A), (0xA5, 0x40, 0x3E)
SUN, NAVY, SEAFOAM = (0xFF, 0xD9, 0x4D), (0x0F, 0x2A, 0x52), (0x4D, 0xBB, 0x92)
TANGERINE_LIGHT, TANGERINE_MID = (0xFF, 0xE2, 0xCC), (0xFF, 0xCD, 0xAA)
CARD, CHIP, SOFT = (0xFF, 0xF7, 0xF1), (0xFF, 0xDC, 0xC9), (0xFF, 0xEC, 0xE0)

def font(path, size): return ImageFont.truetype(path, size)

def gradient(w, h, stops, diagonal=True):
    """Linear gradient from lower-left to upper-right (or left to right)."""
    img = Image.new("RGB", (w, h))
    px = img.load()
    for y in range(h):
        for x in range(w):
            t = ((x / w) + (1 - y / h)) / 2 if diagonal else x / w
            for i in range(len(stops) - 1):
                (t0, c0), (t1, c1) = stops[i], stops[i + 1]
                if t0 <= t <= t1:
                    k = (t - t0) / (t1 - t0) if t1 > t0 else 0
                    px[x, y] = tuple(int(c0[j] + (c1[j] - c0[j]) * k) for j in range(3))
                    break
    return img

def vgradient(w, h, stops):
    """Linear gradient from the top to the bottom."""
    img = Image.new("RGB", (w, h))
    d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / h
        for i in range(len(stops) - 1):
            (t0, c0), (t1, c1) = stops[i], stops[i + 1]
            if t0 <= t <= t1:
                k = (t - t0) / (t1 - t0) if t1 > t0 else 0
                d.line([(0, y), (w, y)], fill=tuple(int(c0[j] + (c1[j] - c0[j]) * k) for j in range(3)))
                break
    return img

def panda(height):
    img = Image.open(PANDA).convert("RGBA")
    img = img.crop(img.getbbox())
    return img.resize((int(img.width * height / img.height), height), Image.LANCZOS)

def shadowed(base, layer, xy, blur=18, offset=(0, 14), opacity=90):
    sh = Image.new("RGBA", base.size, (0, 0, 0, 0))
    mask = layer.split()[-1].point(lambda v: opacity if v > 0 else 0)
    sh.paste((0, 0, 0, 255), (xy[0] + offset[0], xy[1] + offset[1]), mask)
    sh = sh.filter(ImageFilter.GaussianBlur(blur))
    base.alpha_composite(sh)
    base.alpha_composite(layer, xy)

def chip(text, fill, size=26, pad=(18, 9), text_fill=(255, 255, 255)):
    f = font(FONT_BOLD, size)
    bbox = f.getbbox(text)
    w, h = bbox[2] - bbox[0] + pad[0] * 2, bbox[3] - bbox[1] + pad[1] * 2 + 4
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((0, 0, w - 1, h - 1), radius=h // 2, fill=fill)
    d.text((pad[0] - bbox[0], pad[1] - bbox[1] + 1), text, font=f, fill=text_fill)
    return img

def issue_card(w, h, title_w, chips, comments):
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((0, 0, w - 1, h - 1), radius=26, fill=(255, 255, 255, 245))
    d.rounded_rectangle((28, 30, 28 + title_w, 52), radius=11, fill=(30, 41, 59))
    d.rounded_rectangle((28, 68, 28 + int(title_w * 0.7), 82), radius=7, fill=(203, 213, 225))
    x = 28
    for c in chips:
        img.alpha_composite(c, (x, h - c.height - 26))
        x += c.width + 12
    f = font(FONT_TEXT_BOLD, 24)
    d.text((w - 28 - f.getlength(str(comments)) - 38, h - 52), "● " + str(comments), font=f, fill=(100, 116, 139))
    return img

# The glyph behind the panda in the icon.
GLYPH_TRUNK = [(76.57, 79.80), (40.43, 28.20)]
GLYPH_BRANCH = [(51.62, 44.17), (54.51, 46.17), (57.20, 47.97), (60.01, 49.60), (62.61, 50.65), (65.25, 51.39),
                (67.92, 51.86), (70.53, 52.05), (73.03, 51.89), (75.68, 51.23), (78.18, 50.34), (80.86, 49.33)]
GLYPH_NODES = [(76.57, 79.80), (40.43, 28.20), (80.86, 49.33)]

def glyph(height, outline=0):
    """The icon's branch glyph (design units on a 108 grid) drawn as a transparent layer of the given height.
    With outline > 0 the layer gains a black outline that many pixels wide, and grows by that much on every side."""
    k = height / 69.0                       # the glyph spans about 67 units top to bottom, plus a margin
    ox, oy = 31.5, 19.5                     # its top-left corner in design units, with the margin
    w = int(58 * k)
    img = Image.new("RGBA", (w + 2 * outline, height + 2 * outline), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    def pt(q): return ((q[0] - ox) * k + outline, (q[1] - oy) * k + outline)
    width = int(4.6 * k)
    for line in (GLYPH_TRUNK, GLYPH_BRANCH):
        pts = [pt(q) for q in line]
        d.line(pts, fill=SUN + (255,), width=width, joint="curve")
        for x, y in (pts[0], pts[-1]):
            d.ellipse((x - width / 2, y - width / 2, x + width / 2, y + width / 2), fill=SUN + (255,))
    for q in GLYPH_NODES:
        x, y = pt(q)
        r = 7.6 * k
        d.ellipse((x - r, y - r, x + r, y + r), fill=SUN + (255,))
    if outline:
        edge = img.split()[-1].filter(ImageFilter.MaxFilter(2 * outline + 1))
        out = Image.new("RGBA", img.size, (8, 8, 12, 0))
        out.putalpha(edge)
        out.alpha_composite(img)
        img = out
    return img

def banner():
    S, W, H = 2, 1600, 520
    w, h = W * S, H * S
    img = Image.new("RGB", (w, h))
    bg = ImageDraw.Draw(img)
    for y in range(h):
        t = y / h
        lo, hi, u = (ORANGE_TOP, ORANGE_MID, t * 2) if t < 0.5 else (ORANGE_MID, ORANGE_BOTTOM, t * 2 - 1)
        bg.line([(0, y), (w, y)], fill=tuple(int(lo[i] + (hi[i] - lo[i]) * u) for i in range(3)))
    img = img.convert("RGBA")
    d = ImageDraw.Draw(img, "RGBA")
    # faint dot grid
    for gx in range(0, w, 64 * S):
        for gy in range(0, h, 64 * S):
            d.ellipse((gx, gy, gx + 4 * S, gy + 4 * S), fill=(255, 255, 255, 22))
    # soft warm glow behind the glyph and panda
    glow = Image.new("RGBA", img.size, (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse((-int(w * 0.1), -int(h * 0.2), int(w * 0.4), int(h * 1.0)), fill=(255, 190, 120, 75))
    img.alpha_composite(glow.filter(ImageFilter.GaussianBlur(150 * S)))
    d = ImageDraw.Draw(img, "RGBA")
    # branch glyph on the left, then the panda beside it, then the title: the branch leads to the panda
    edge = 5 * S                             # black outline so the glyph pops off the orange
    g = glyph(410 * S, outline=edge)
    shadowed(img, g, (100 * S - edge, (h - g.height) // 2), blur=18 * S, offset=(0, 14 * S), opacity=70)
    p = panda(380 * S)
    shadowed(img, p, (100 * S + g.width - 2 * edge + 36 * S, (h - p.height) // 2 + 6 * S), blur=22 * S, offset=(0, 16 * S), opacity=110)
    # title
    x0 = 835 * S
    d.text((x0, 96 * S), "Open Source", font=font(FONT_BOLD, 80 * S), fill=(255, 255, 255))
    d.text((x0, 182 * S), "Panda", font=font(FONT_BOLD, 80 * S), fill=(255, 255, 255))
    d.rounded_rectangle((x0, 290 * S, x0 + 140 * S, 298 * S), radius=4 * S, fill=SUN)
    d.text((x0, 318 * S), "Find the open-source issue", font=font(FONT_TEXT, 31 * S), fill=(255, 236, 224))
    d.text((x0, 358 * S), "worth your next hour.", font=font(FONT_TEXT, 31 * S), fill=(255, 236, 224))
    cx = x0
    for text, fill, ink in (("beginner-friendly", NAVY, (255, 255, 255)), ("asking for help", SEAFOAM, NAVY), ("kotlin", SUN, NAVY)):
        c = chip(text, fill, size=20 * S, pad=(15 * S, 7 * S), text_fill=ink)
        img.alpha_composite(c, (cx, 432 * S))
        cx += c.width + 14 * S
    return img.convert("RGB").resize((W, H), Image.LANCZOS)

def divider():
    W, H = 1600, 6
    img = gradient(W, H, [(0, ORANGE_BOTTOM), (0.35, ORANGE_TOP), (0.65, SUN), (1, ORANGE_TOP)], diagonal=False)
    return img

def phone(name, caption, kind):
    S, W, H = 2, 540, 1100
    w, h = W * S, H * S
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((0, 0, w - 1, h - 1), radius=70 * S, fill=NAVY)
    inner = (14 * S, 14 * S, w - 14 * S, h - 14 * S)
    if kind == "welcome":
        screen = vgradient(inner[2] - inner[0], inner[3] - inner[1], [(0, ORANGE_TOP), (0.5, ORANGE_MID), (1, ORANGE_BOTTOM)])
    else:
        screen = vgradient(inner[2] - inner[0], inner[3] - inner[1], [(0, TANGERINE_LIGHT), (1, TANGERINE_MID)])
        # The header band and its see-through title pill are drawn on the screen before it is rounded off.
        screen.paste(vgradient(screen.width, 110 * S, [(0, ORANGE_TOP), (1, ORANGE_MID)]), (0, 0))
        screen = screen.convert("RGBA")
        pill = Image.new("RGBA", screen.size, (0, 0, 0, 0))
        ImageDraw.Draw(pill).rounded_rectangle((90 * S, 38 * S, 300 * S, 80 * S), radius=12 * S, fill=(255, 255, 255, 51))
        screen = Image.alpha_composite(screen, pill).convert("RGB")
    mask = Image.new("L", screen.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, screen.width - 1, screen.height - 1), radius=58 * S, fill=255)
    img.paste(screen, inner[:2], mask)
    d = ImageDraw.Draw(img)
    ox, oy = inner[0], inner[1]
    if kind == "welcome":
        pd = panda(190 * S)
        img.alpha_composite(pd, (ox + (512 * S - pd.width) // 2, oy + 130 * S))
        d = ImageDraw.Draw(img)
        for i, wd in enumerate((260, 200, 300)):
            d.rounded_rectangle((ox + (512 - wd) // 2 * S, oy + (380 + i * 60) * S, ox + ((512 + wd) // 2) * S, oy + (420 + i * 60) * S), radius=20 * S, fill=(255, 255, 255, 200 if i == 0 else 90))
        d.rounded_rectangle((ox + 56 * S, oy + 700 * S, ox + 456 * S, oy + 770 * S), radius=35 * S, outline=(255, 255, 255), width=3 * S)
        d.rounded_rectangle((ox + 56 * S, oy + 800 * S, ox + 456 * S, oy + 870 * S), radius=35 * S, fill=(255, 255, 255))
    else:
        for i in range(5):
            top = oy + (140 + i * 120) * S
            d.rounded_rectangle((ox + 24 * S, top, ox + 488 * S, top + 104 * S), radius=24 * S, fill=CARD)
            d.rounded_rectangle((ox + 48 * S, top + 24 * S, ox + 230 * S, top + 44 * S), radius=10 * S, fill=(42, 27, 24))
            d.rounded_rectangle((ox + 48 * S, top + 62 * S, ox + 380 * S, top + 76 * S), radius=7 * S, fill=(92, 69, 63))
            d.rounded_rectangle((ox + 384 * S, top + 24 * S, ox + 464 * S, top + 48 * S), radius=12 * S, fill=CHIP)
    # caption pill
    f1, f2 = font(FONT_BOLD, 30 * S), font(FONT_TEXT, 24 * S)
    pw = 430 * S
    px, py = (w - pw) // 2, h // 2 - 70 * S
    d.rounded_rectangle((px, py, px + pw, py + 150 * S), radius=30 * S, fill=NAVY + (235,))
    d.text((w // 2, py + 50 * S), "Screenshot coming", font=f1, fill=(255, 255, 255), anchor="mm")
    d.text((w // 2, py + 100 * S), caption, font=f2, fill=SOFT, anchor="mm")
    return img.resize((W, H), Image.LANCZOS)

def demo_placeholder():
    S, W, H = 2, 1200, 675
    img = vgradient(W * S, H * S, [(0, ORANGE_TOP), (0.5, ORANGE_MID), (1, ORANGE_BOTTOM)]).convert("RGBA")
    d = ImageDraw.Draw(img)
    cx, cy = W * S // 2, H * S // 2 - 30 * S
    d.ellipse((cx - 90 * S, cy - 90 * S, cx + 90 * S, cy + 90 * S), fill=(255, 255, 255, 235))
    d.polygon([(cx - 26 * S, cy - 48 * S), (cx - 26 * S, cy + 48 * S), (cx + 54 * S, cy)], fill=ORANGE_MID)
    d.text((cx, cy + 150 * S), "Demo GIF coming soon", font=font(FONT_BOLD, 44 * S), fill=(255, 255, 255), anchor="mm")
    d.text((cx, cy + 205 * S), "swap this file for docs/images/demo.gif and update the README line", font=font(FONT_TEXT, 26 * S), fill=SOFT, anchor="mm")
    return img.convert("RGB").resize((W, H), Image.LANCZOS)

if __name__ == "__main__":
    out = lambda n: os.path.join(HERE, n)
    banner().save(out("banner.png"), optimize=True)
    divider().save(out("divider.png"), optimize=True)
    phone("welcome", "Welcome / token gate", "welcome").save(out("placeholder-welcome.png"), optimize=True)
    phone("top", "Top Repos", "list").save(out("placeholder-top-repos.png"), optimize=True)
    phone("issues", "Repo issues", "list").save(out("placeholder-repo-issues.png"), optimize=True)
    demo_placeholder().save(out("placeholder-demo.png"), optimize=True)
    panda(160).save(out("panda.png"), optimize=True)
    print("done")
