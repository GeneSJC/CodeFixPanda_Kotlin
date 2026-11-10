"""
Draws the open (foss) build's launcher icon: the same mascot and branch glyph on a blue background instead of
the terracotta one, so the two builds can be told apart on a home screen.

It reads the main launcher icon from app/src/main/res and writes only the files that differ into
app/src/foss/res, where Gradle overlays them on the main ones for the foss flavor:
  - ic_launcher_background.webp  the adaptive icon's background (the foreground and monochrome layers are shared)
  - ic_launcher.webp, ic_launcher_round.webp  the legacy icons for Android 7.x, rebuilt from the adaptive layers

Run from the repo root: python3 -B _meta/scripts/make_flavor_icons.py   (needs Pillow and numpy)
The mascot is repainted from the main files, so rerun this after changing them.
"""
import os

import numpy as np
from PIL import Image

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
MAIN = os.path.join(ROOT, "app", "src", "main", "res")
FOSS = os.path.join(ROOT, "app", "src", "foss", "res")

# density -> (adaptive layer size, legacy icon size), in pixels
DENSITIES = {"mdpi": (108, 48), "hdpi": (162, 72), "xhdpi": (216, 96), "xxhdpi": (324, 144), "xxxhdpi": (432, 192)}

GLYPH_YELLOW = np.array([255, 217, 77], dtype=float)
BLUE_TOP = np.array([46, 108, 196], dtype=float)
BLUE_BOTTOM = np.array([22, 58, 120], dtype=float)

# The legacy icon shows the middle two thirds of the adaptive canvas (found by matching the existing legacy icons).
LEGACY_CROP = 2 / 3


def luminance(rgb):
    return rgb @ np.array([0.299, 0.587, 0.114])


def blue_background(background):
    """The adaptive background with its terracotta gradient swapped for blue; the yellow glyph is kept as it is."""
    pixels = np.array(background.convert("RGB")).astype(float)
    height, width, _ = pixels.shape

    # The terracotta color of each row, read from the left edge where the glyph and the soft circle never reach.
    row_color = np.median(pixels[:, 2:max(4, width // 20), :], axis=1)[:, None, :]

    # How much of each pixel is the glyph's yellow, from 0 to 1 (this also keeps the glyph's smooth edge).
    to_yellow = GLYPH_YELLOW - row_color
    coverage = np.clip(((pixels - row_color) * to_yellow).sum(axis=2) / (to_yellow ** 2).sum(axis=2), 0, 1)
    coverage = np.where(coverage < 0.22, 0.0, coverage)[:, :, None]  # the soft circle reads up to about 0.2

    # The blue gradient, with the soft lighter circle of the original carried over as a brightness ratio.
    rows = np.linspace(0, 1, height)[:, None, None]
    blue = BLUE_TOP * (1 - rows) + BLUE_BOTTOM * rows
    glow = np.clip(luminance(pixels) / luminance(np.broadcast_to(row_color, pixels.shape)), 0.8, 1.35)[:, :, None]
    glow = np.where(coverage > 0, 1.0, glow)
    background_only = np.clip(blue * glow, 0, 255)

    out = background_only * (1 - coverage) + GLYPH_YELLOW * coverage
    return Image.fromarray(np.clip(out, 0, 255).astype(np.uint8), "RGB")


def legacy_icon(blue, foreground, shape, size):
    """A legacy icon: the middle of the adaptive composite, cut to the shape the main icon already uses."""
    composite = blue.convert("RGBA")
    composite.alpha_composite(foreground)
    span = composite.width * LEGACY_CROP
    start = (composite.width - span) / 2
    cropped = composite.crop((round(start), round(start), round(start + span), round(start + span)))
    icon = cropped.resize((size, size), Image.LANCZOS)
    icon.putalpha(shape.getchannel("A"))
    return icon


def main():
    for density, (_, legacy_size) in DENSITIES.items():
        source = os.path.join(MAIN, f"mipmap-{density}")
        target = os.path.join(FOSS, f"mipmap-{density}")
        os.makedirs(target, exist_ok=True)

        blue = blue_background(Image.open(os.path.join(source, "ic_launcher_background.webp")))
        foreground = Image.open(os.path.join(source, "ic_launcher_foreground.webp")).convert("RGBA")
        blue.save(os.path.join(target, "ic_launcher_background.webp"), lossless=True)

        for name in ("ic_launcher", "ic_launcher_round"):
            shape = Image.open(os.path.join(source, f"{name}.webp")).convert("RGBA")
            assert shape.width == legacy_size, (name, density, shape.size)
            legacy_icon(blue, foreground, shape, legacy_size).save(os.path.join(target, f"{name}.webp"), lossless=True)
        print("wrote", density)


if __name__ == "__main__":
    main()
