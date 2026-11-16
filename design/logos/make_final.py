"""Final mark: two layers (the two subtitle languages) with a hand-drawn character on the front one.
环 (huan: ring, loop; 循环 = loop) or 再 (zai: again). Strokes are round-capped, drawn here, no font involved."""
import os
import sys

OUT = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(OUT, "..", "..", "app", "src", "main", "res")
W, B = "#FFFFFF", "#3EA6FF"


def f(v):
    return f"{v:.2f}".rstrip("0").rstrip(".")


def rr(x, y, w, h, r, ccw=False):
    if not ccw:
        return (f"M{f(x+r)},{f(y)}H{f(x+w-r)}A{f(r)},{f(r)} 0 0 1 {f(x+w)},{f(y+r)}V{f(y+h-r)}"
                f"A{f(r)},{f(r)} 0 0 1 {f(x+w-r)},{f(y+h)}H{f(x+r)}A{f(r)},{f(r)} 0 0 1 {f(x)},{f(y+h-r)}"
                f"V{f(y+r)}A{f(r)},{f(r)} 0 0 1 {f(x+r)},{f(y)}Z")
    return (f"M{f(x+r)},{f(y)}A{f(r)},{f(r)} 0 0 0 {f(x)},{f(y+r)}V{f(y+h-r)}"
            f"A{f(r)},{f(r)} 0 0 0 {f(x+r)},{f(y+h)}H{f(x+w-r)}A{f(r)},{f(r)} 0 0 0 {f(x+w)},{f(y+h-r)}"
            f"V{f(y+r)}A{f(r)},{f(r)} 0 0 0 {f(x+w-r)},{f(y)}Z")


# Characters on a 24 x 24 box, as stroke paths (SVG path syntax).
CHARS = {
    "huan": ("M1,4H9.5 M2,11H8.5 M0.5,19.8L10,18.8 M5.2,4V19.8 "      # 王 (the ring radical)
             "M12.5,4H23.5 M18,4.5Q16.2,10 12.8,13.5 M18,4.5V20.5 M20.5,11L22.7,14.5"),  # 不
    "zai": "M2,3.5H22 M4.5,3.5V20.5 M19.5,3.5V20.5 M12,3.5V15 M8,9H16 M7,15H17",
}

FX, FY, FS, FR = 30, 38, 38, 11           # front card
BX, BY = 38, 30                            # back card, offset up-right
HALO = 3.5
SCALE, STROKE = 1.0, 2.2                  # glyph scale and stroke width on the 108 canvas
cx, cy = FX + FS / 2, FY + FS / 2


def place(path_d):
    """Move the 24-box glyph so its centre sits on the front card centre, scaled."""
    import re
    toks = re.findall(r"[MLHVQ]|-?\d+\.?\d*", path_d)
    out, i, cmd = [], 0, None
    nums_per = {"M": 2, "L": 2, "H": 1, "V": 1, "Q": 4}
    while i < len(toks):
        if toks[i] in nums_per:
            cmd = toks[i]
            out.append(cmd)
            i += 1
            continue
        n = nums_per[cmd]
        vals = [float(t) for t in toks[i:i + n]]
        i += n
        if cmd in ("M", "L", "Q"):
            vals = [cx + (v - 12) * SCALE if k % 2 == 0 else cy + (v - 12) * SCALE for k, v in enumerate(vals)]
        elif cmd == "H":
            vals = [cx + (vals[0] - 12) * SCALE]
        else:
            vals = [cy + (vals[0] - 12) * SCALE]
        out.append(",".join(f(v) for v in vals))
    return "".join(out)


def build(char):
    glyph = place(CHARS[char])
    front = rr(FX, FY, FS, FS, FR)
    back = rr(BX, BY, FS, FS, FR)
    halo_hole = rr(FX - HALO, FY - HALO, FS + 2 * HALO, FS + 2 * HALO, FR + HALO, ccw=True)
    frame = "M0,0H108V108H0Z"
    svg = f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="108" height="108">
  <rect width="108" height="108" fill="#000000"/>
  <defs><clipPath id="gap"><path d="{frame}{halo_hole}"/></clipPath></defs>
  <g clip-path="url(#gap)"><path d="{back}" fill="{B}"/></g>
  <path d="{front}" fill="{W}"/>
  <path d="{glyph}" fill="none" stroke="#000000" stroke-width="{STROKE}" stroke-linecap="round" stroke-linejoin="round"/>
</svg>
"""
    return svg, glyph, front, back, halo_hole, frame


def vector(glyph, front, back, halo_hole, frame):
    return f"""<?xml version="1.0" encoding="utf-8"?>
<!-- Two layers, one per subtitle language. The front one carries a hand-drawn character: 环 (huan: ring, loop). -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <group>
        <clip-path android:pathData="{frame}{halo_hole}" />
        <path
            android:fillColor="@color/layer_back"
            android:pathData="{back}" />
    </group>
    <path
        android:fillColor="@color/paper"
        android:pathData="{front}" />
    <path
        android:pathData="{glyph}"
        android:strokeColor="@color/night_ink"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"
        android:strokeWidth="{STROKE}" />
</vector>
"""


def mono(glyph, front, back, halo_hole, frame):
    """Themed icons keep only alpha, so the cards become outlines and the character a solid stroke."""
    return f"""<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <group>
        <clip-path android:pathData="{frame}{halo_hole}" />
        <path
            android:pathData="{back}"
            android:strokeColor="#FFFFFFFF"
            android:strokeWidth="2.4" />
    </group>
    <path
        android:pathData="{front}"
        android:strokeColor="#FFFFFFFF"
        android:strokeWidth="2.4" />
    <path
        android:pathData="{glyph}"
        android:strokeColor="#FFFFFFFF"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"
        android:strokeWidth="{STROKE}" />
</vector>
"""


cells = []
for name in CHARS:
    svg, *_ = build(name)
    with open(os.path.join(OUT, f"final-{name}.svg"), "w", encoding="utf-8") as fh:
        fh.write(svg)
    big = svg.replace('width="108" height="108"', 'width="320" height="320"')
    mid = svg.replace('width="108" height="108"', 'width="72" height="72"')
    small = svg.replace('width="108" height="108"', 'width="40" height="40"')
    cells.append(f'<div><div style="width:320px;height:320px;border-radius:72px;overflow:hidden">{big}</div>'
                 f'<div style="display:flex;gap:16px;align-items:center;margin-top:10px">'
                 f'<div style="width:72px;height:72px;border-radius:50%;overflow:hidden">{mid}</div>'
                 f'<div style="width:40px;height:40px;border-radius:10px;overflow:hidden">{small}</div><span>{name}</span></div></div>')
with open(os.path.join(OUT, "final.html"), "w", encoding="utf-8") as fh:
    fh.write('<html><body style="margin:0;background:#222;display:flex;gap:32px;padding:24px;font:14px sans-serif;color:#ddd">'
             + "".join(cells) + "</body></html>")

if len(sys.argv) > 1:   # apply the chosen character to the Android icon
    _, glyph, front, back, halo_hole, frame = build(sys.argv[1])
    with open(os.path.join(RES, "drawable", "ic_launcher_foreground.xml"), "w", encoding="utf-8") as fh:
        fh.write(vector(glyph, front, back, halo_hole, frame))
    with open(os.path.join(RES, "drawable", "ic_launcher_monochrome.xml"), "w", encoding="utf-8") as fh:
        fh.write(mono(glyph, front, back, halo_hole, frame))
