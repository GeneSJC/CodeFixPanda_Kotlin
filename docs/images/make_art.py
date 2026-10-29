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

NAVY, BLUE, BLUE2, SKY = (11, 23, 54), (29, 78, 216), (37, 99, 235), (56, 189, 248)
PURPLE, GREEN, AMBER = (112, 87, 255), (0, 134, 114), (245, 158, 11)
INK, SOFT, PAPER = (15, 23, 42), (203, 213, 225), (243, 242, 250)

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

def banner():
    S, W, H = 2, 1600, 520
    w, h = W * S, H * S
    img = gradient(w, h, [(0, NAVY), (0.5, BLUE), (1, BLUE2)]).convert("RGBA")
    d = ImageDraw.Draw(img, "RGBA")
    # faint dot grid
    for gx in range(0, w, 64 * S):
        for gy in range(0, h, 64 * S):
            d.ellipse((gx, gy, gx + 4 * S, gy + 4 * S), fill=(255, 255, 255, 10))
    # soft glows
    glow = Image.new("RGBA", img.size, (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse((int(w * 0.55), -int(h * 0.55), int(w * 1.1), int(h * 0.7)), fill=SKY + (120,))
    gd.ellipse((-int(w * 0.1), int(h * 0.55), int(w * 0.3), int(h * 1.3)), fill=PURPLE + (70,))
    img.alpha_composite(glow.filter(ImageFilter.GaussianBlur(90 * S)))
    d = ImageDraw.Draw(img, "RGBA")
    # panda
    p = panda(380 * S)
    shadowed(img, p, (64 * S, (h - p.height) // 2 + 6 * S), blur=22 * S, offset=(0, 16 * S), opacity=110)
    # title
    x0 = 470 * S
    d.text((x0, 96 * S), "Open Source", font=font(FONT_BOLD, 80 * S), fill=(255, 255, 255))
    d.text((x0, 182 * S), "Panda", font=font(FONT_BOLD, 80 * S), fill=(255, 255, 255))
    d.rounded_rectangle((x0, 290 * S, x0 + 140 * S, 298 * S), radius=4 * S, fill=AMBER)
    d.text((x0, 318 * S), "Find the open-source issue", font=font(FONT_TEXT, 31 * S), fill=SOFT)
    d.text((x0, 358 * S), "worth your next hour.", font=font(FONT_TEXT, 31 * S), fill=SOFT)
    cx = x0
    for text, fill in (("beginner-friendly", PURPLE), ("asking for help", GREEN), ("kotlin", (14, 165, 233))):
        c = chip(text, fill, size=20 * S, pad=(15 * S, 7 * S))
        img.alpha_composite(c, (cx, 432 * S))
        cx += c.width + 14 * S
    # floating issue cards on the right
    specs = [((1190, 44), 300, 160, 190, ("beginner-friendly",), 3, PURPLE, 3),
             ((1262, 190), 300, 160, 160, ("asking for help",), 7, GREEN, -2),
             ((1160, 330), 300, 160, 210, ("beginner-friendly", "kotlin"), 2, PURPLE, 2)]
    for (x, y), cw, ch, tw, labels, comments, _c, rot in specs:
        chips = [chip(l, PURPLE if "good" in l else (GREEN if "help" in l else (14, 165, 233)), size=17 * S, pad=(12 * S, 6 * S)) for l in labels]
        card = issue_card(cw * S, ch * S, tw * S, chips, comments).rotate(rot, expand=True, resample=Image.BICUBIC)
        shadowed(img, card, (x * S, y * S), blur=16 * S, offset=(0, 12 * S), opacity=80)
    return img.convert("RGB").resize((W, H), Image.LANCZOS)

def divider():
    W, H = 1600, 6
    img = gradient(W, H, [(0, NAVY), (0.35, BLUE), (0.65, SKY), (1, BLUE)], diagonal=False)
    return img

def phone(name, caption, kind):
    S, W, H = 2, 540, 1100
    w, h = W * S, H * S
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((0, 0, w - 1, h - 1), radius=70 * S, fill=INK)
    inner = (14 * S, 14 * S, w - 14 * S, h - 14 * S)
    if kind == "welcome":
        screen = gradient(inner[2] - inner[0], inner[3] - inner[1], [(0, NAVY), (0.55, BLUE), (1, BLUE2)])
    else:
        screen = Image.new("RGB", (inner[2] - inner[0], inner[3] - inner[1]), PAPER)
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
        d.rectangle((ox, oy, ox + 512 * S, oy + 110 * S), fill=(255, 255, 255))
        d.rounded_rectangle((ox + 90 * S, oy + 38 * S, ox + 300 * S, oy + 80 * S), radius=21 * S, fill=PURPLE)
        for i in range(5):
            top = oy + (140 + i * 120) * S
            d.rounded_rectangle((ox + 24 * S, top, ox + 488 * S, top + 104 * S), radius=24 * S, fill=(255, 255, 255))
            d.rounded_rectangle((ox + 48 * S, top + 24 * S, ox + 230 * S, top + 44 * S), radius=10 * S, fill=(30, 41, 59))
            d.rounded_rectangle((ox + 48 * S, top + 62 * S, ox + 380 * S, top + 76 * S), radius=7 * S, fill=(203, 213, 225))
            d.rounded_rectangle((ox + 384 * S, top + 24 * S, ox + 464 * S, top + 48 * S), radius=12 * S, fill=(232, 228, 252))
    # caption pill
    f1, f2 = font(FONT_BOLD, 30 * S), font(FONT_TEXT, 24 * S)
    pw = 430 * S
    px, py = (w - pw) // 2, h // 2 - 70 * S
    d.rounded_rectangle((px, py, px + pw, py + 150 * S), radius=30 * S, fill=(15, 23, 42, 235))
    d.text((w // 2, py + 50 * S), "Screenshot coming", font=f1, fill=(255, 255, 255), anchor="mm")
    d.text((w // 2, py + 100 * S), caption, font=f2, fill=SOFT, anchor="mm")
    return img.resize((W, H), Image.LANCZOS)

def demo_placeholder():
    S, W, H = 2, 1200, 675
    img = gradient(W * S, H * S, [(0, NAVY), (0.55, BLUE), (1, BLUE2)]).convert("RGBA")
    d = ImageDraw.Draw(img)
    cx, cy = W * S // 2, H * S // 2 - 30 * S
    d.ellipse((cx - 90 * S, cy - 90 * S, cx + 90 * S, cy + 90 * S), fill=(255, 255, 255, 235))
    d.polygon([(cx - 26 * S, cy - 48 * S), (cx - 26 * S, cy + 48 * S), (cx + 54 * S, cy)], fill=BLUE)
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
