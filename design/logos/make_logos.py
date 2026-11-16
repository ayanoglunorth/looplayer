"""Generates the Looplayer logo concepts as SVG (108x108 adaptive-icon canvas, mark inside the 66 dp safe circle)."""
import math
import os

W, B = "#FFFFFF", "#3EA6FF"
OUT = os.path.dirname(os.path.abspath(__file__))
NL = "\n"


def svg(*parts, bg="#000000"):
    body = "".join("  " + p + NL for p in parts)
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="108" height="108">{NL}'
            f'  <rect width="108" height="108" fill="{bg}"/>{NL}{body}</svg>{NL}')


def pt(r, deg, cx=54, cy=54):
    a = math.radians(deg)
    return cx + r * math.cos(a), cy + r * math.sin(a)


logos = {}

# 1. Echo: a white disc with its blue echo behind it, a black halo between them.
logos["1-echo"] = svg(
    f'<circle cx="60" cy="48" r="21" fill="{B}"/>',
    '<circle cx="49" cy="59" r="25" fill="#000000"/>',
    f'<circle cx="49" cy="59" r="19" fill="{W}"/>',
)

# 2. Fold: a squircle cut on the diagonal, the halves slid apart.
sq = "M54,26 C76,26 82,32 82,54 C82,76 76,82 54,82 C32,82 26,76 26,54 C26,32 32,26 54,26Z"
logos["2-fold"] = svg(
    '<defs><clipPath id="a"><path d="M0,0 L108,0 L0,108Z" transform="translate(-1.6,-1.6)"/></clipPath>'
    '<clipPath id="b"><path d="M108,0 L108,108 L0,108Z" transform="translate(1.6,1.6)"/></clipPath></defs>',
    f'<g clip-path="url(#a)"><path d="{sq}" fill="{W}"/></g>',
    f'<g clip-path="url(#b)"><path d="{sq}" fill="{B}"/></g>',
)

# 3. Overlap: two discs, the moment they share cut out in black.
logos["3-overlap"] = svg(
    '<defs><clipPath id="c"><circle cx="64" cy="54" r="19"/></clipPath></defs>',
    f'<circle cx="44" cy="54" r="19" fill="{W}"/>',
    f'<circle cx="64" cy="54" r="19" fill="{B}"/>',
    '<circle cx="44" cy="54" r="19" fill="#000000" clip-path="url(#c)"/>',
)

# 4. Loop point: an open ring with the blue dot sitting in its gap.
gap_c, gap_half = -45, 36
p0 = pt(21, gap_c + gap_half)
p1 = pt(21, gap_c - gap_half + 360)
dot = pt(21, gap_c)
logos["4-loop-point"] = svg(
    f'<path d="M{p0[0]:.2f},{p0[1]:.2f} A21,21 0 1 1 {p1[0]:.2f},{p1[1]:.2f}" fill="none" stroke="{W}" stroke-width="7" stroke-linecap="round"/>',
    f'<circle cx="{dot[0]:.2f}" cy="{dot[1]:.2f}" r="4.5" fill="{B}"/>',
)

# 5. Duality: a filled half-disc against an outlined half-ring.
logos["5-duality"] = svg(
    f'<path d="M51,32 A22,22 0 0 0 51,76Z" fill="{W}"/>',
    f'<path d="M57,32 A22,22 0 0 1 57,76" fill="none" stroke="{B}" stroke-width="6"/>',
)

for name, s in logos.items():
    with open(os.path.join(OUT, f"v2-{name}.svg"), "w", encoding="utf-8") as f:
        f.write(s)

cells = []
for name, s in logos.items():
    big = s.replace('width="108" height="108"', 'width="300" height="300"')
    small = s.replace('width="108" height="108"', 'width="48" height="48"')
    cells.append(
        f'<div><div style="width:300px;height:300px;border-radius:66px;overflow:hidden">{big}</div>'
        f'<div style="display:flex;gap:12px;align-items:center;margin-top:8px">'
        f'<div style="width:48px;height:48px;border-radius:12px;overflow:hidden">{small}</div><span>{name}</span></div></div>')
with open(os.path.join(OUT, "sheet2.html"), "w", encoding="utf-8") as f:
    f.write('<html><body style="margin:0;background:#222;display:flex;flex-wrap:wrap;gap:24px;padding:24px;'
            'font:14px sans-serif;color:#ddd">' + "".join(cells) + "</body></html>")
