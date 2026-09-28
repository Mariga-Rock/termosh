"""Termosh icon: geometric '>_' prompt on near-black. No fonts."""
from pathlib import Path
from PIL import Image, ImageDraw
import sys

BG_COLOR   = (13, 17, 23, 255)      # #0D1117
SYMBOL     = (125, 207, 255, 255)   # #7DCFFF
BG_HEX     = "#0D1117"
MONO_COLOR = (255, 255, 255, 255)

LEGACY = [
    ("mipmap-mdpi", 48), ("mipmap-hdpi", 72), ("mipmap-xhdpi", 96),
    ("mipmap-xxhdpi", 144), ("mipmap-xxxhdpi", 192),
]
ADAPTIVE = [
    ("mipmap-mdpi", 108), ("mipmap-hdpi", 162), ("mipmap-xhdpi", 216),
    ("mipmap-xxhdpi", 324), ("mipmap-xxxhdpi", 432),
]

def draw_prompt(d, size, color):
    """Geometric '>_' centered on the canvas. Box height ~ 46% of size."""
    box_h  = size * 0.46
    box_w  = size * 0.62
    cx, cy = size / 2, size / 2

    stroke = max(2, int(size * 0.075))   # line thickness
    r      = stroke / 2

    # chevron ">" : apex on right side of the chevron box, opening to the left
    ch_w = box_h * 0.55                  # width of the chevron
    ch_x = cx - box_w / 2                # left edge of chevron
    ch_y = cy - box_h / 2
    apex = (ch_x + ch_w, cy)
    top  = (ch_x, ch_y)
    bot  = (ch_x, ch_y + box_h)

    d.line([top, apex, bot], fill=color, width=stroke, joint="curve")
    # round the caps manually (PIL's line caps are square)
    for p in (top, apex, bot):
        d.ellipse([p[0]-r, p[1]-r, p[0]+r, p[1]+r], fill=color)

    # cursor "_" : rounded rectangle on the baseline, right of the chevron
    cur_w = box_w * 0.36
    cur_h = stroke * 0.55
    gap   = box_w * 0.14
    x0    = apex[0] + gap
    x1    = x0 + cur_w
    y1    = ch_y + box_h + stroke * 0.25  # sit just below baseline
    y0    = y1 - cur_h
    d.rounded_rectangle([x0, y0, x1, y1], radius=cur_h/2, fill=color)


def draw_legacy(size, shape, color=SYMBOL, bg=BG_COLOR):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d   = ImageDraw.Draw(img)
    if shape == "circle":
        d.ellipse([(0, 0), (size - 1, size - 1)], fill=bg)
    else:
        d.rounded_rectangle([(0, 0), (size - 1, size - 1)],
                            radius=int(size * 0.22), fill=bg)
    draw_prompt(d, size, color)
    return img


def draw_foreground(size, color=SYMBOL):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d   = ImageDraw.Draw(img)
    # shrink to fit the 66% adaptive safe zone
    inner = int(size * 0.66)
    pad   = (size - inner) // 2
    sub   = Image.new("RGBA", (inner, inner), (0, 0, 0, 0))
    draw_prompt(ImageDraw.Draw(sub), inner, color)
    img.alpha_composite(sub, (pad, pad))
    return img


def draw_release(size):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d   = ImageDraw.Draw(img)
    d.rounded_rectangle([(0, 0), (size - 1, size - 1)],
                        radius=int(size * 0.22), fill=BG_COLOR)
    draw_prompt(d, size, SYMBOL)
    return img


def main():
    base = Path.home() / "termosh"
    res  = base / "app" / "src" / "main" / "res"
    if not res.exists():
        print(f"ERROR: {res} не найден"); return 1

    for folder, size in LEGACY:
        out = res / folder; out.mkdir(parents=True, exist_ok=True)
        draw_legacy(size, "square").save(out / "ic_launcher.png", "PNG")
        draw_legacy(size, "circle").save(out / "ic_launcher_round.png", "PNG")
        print(f"  {folder}: {size}x{size}")

    for folder, size in ADAPTIVE:
        out = res / folder; out.mkdir(parents=True, exist_ok=True)
        draw_foreground(size, SYMBOL).save(out / "ic_launcher_foreground.png", "PNG")
        draw_foreground(size, MONO_COLOR).save(out / "ic_launcher_monochrome.png", "PNG")
        print(f"  foreground {folder}: {size}x{size}")

    anydpi = res / "mipmap-anydpi-v26"; anydpi.mkdir(parents=True, exist_ok=True)
    xml = (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
        '    <background android:drawable="@color/ic_launcher_background"/>\n'
        '    <foreground android:drawable="@mipmap/ic_launcher_foreground"/>\n'
        '    <monochrome android:drawable="@mipmap/ic_launcher_monochrome"/>\n'
        '</adaptive-icon>\n'
    )
    (anydpi / "ic_launcher.xml").write_text(xml)
    (anydpi / "ic_launcher_round.xml").write_text(xml)

    values = res / "values"; values.mkdir(parents=True, exist_ok=True)
    (values / "colors.xml").write_text(
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<resources>\n'
        f'    <color name="ic_launcher_background">{BG_HEX}</color>\n'
        '</resources>\n'
    )

    release = base / "release" / "icon"; release.mkdir(parents=True, exist_ok=True)
    for s in (1024, 512, 192):
        draw_release(s).save(release / f"termosh_icon_{s}.png", "PNG")

    print(f"\nOK. res: {res}")
    print(f"    release: {release}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
