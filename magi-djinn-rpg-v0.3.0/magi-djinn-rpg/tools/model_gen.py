#!/usr/bin/env python3
"""
Magi model generator (kept in the repo so every future version regenerates all art from it).

Usage:  python3 tools/model_gen.py [preview_dir]

- Every 3D item is built from many cuboids (vanilla JSON item model, no extra mod dependency).
- Colours come from a tiny "palette" texture (8 shades x 16 materials); each face samples one palette pixel,
  with the shading baked in (elements use "shade": false).
- Weapons are authored UPRIGHT with the grip at y ~= 1.64 (the sprite handle position of vanilla handheld
  items), which lets the shared parent `magi:item/handheld_3d` reuse vanilla's hand display transforms with the
  z rotation reduced by 45 degrees. Bows use `magi:item/bow_3d` the same way.
- Element rotations are limited by Minecraft to one axis and {-45,-22.5,0,22.5,45}; curves are chains of segments.
"""
import json, math, os, sys
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = f"{ROOT}/src/main/resources"
A = f"{RES}/assets/magi"
PREV = sys.argv[1] if len(sys.argv) > 1 else f"{ROOT}/build_previews"

ALLOWED = [-45, -22.5, 0, 22.5, 45]
def snap(a): return min(ALLOWED, key=lambda v: abs(v - a))
def r3(v): return round(float(v), 4)

# ---------------------------------------------------------------- palette
BASE = {
    "blade": (178, 184, 196), "edge": (225, 230, 240), "gold": (214, 175, 58), "bronze": (150, 100, 48),
    "leather": (104, 64, 38), "red": (168, 34, 46), "turq": (28, 178, 170), "purple": (106, 63, 160),
    "dark": (44, 46, 60), "white": (240, 232, 210), "wood": (122, 82, 48), "gem": (63, 169, 245),
    "fire": (255, 122, 24), "black": (20, 20, 28), "ruby": (194, 30, 58), "ivory": (226, 212, 178),
}
ROWS = list(BASE)
LV = [0.34, 0.5, 0.66, 0.82, 1.0, 1.14, 1.28, 1.45]
def clamp(v): return max(0, min(255, int(round(v))))
def shade(rgb, lv): return tuple(clamp(c * LV[lv]) for c in rgb)
def make_palette(over=None, alpha=None):
    base = dict(BASE); base.update(over or {})
    alpha = alpha or {}
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    for r, name in enumerate(ROWS):
        for lv in range(8):
            c = (*shade(base[name], lv), alpha.get(name, 255))
            for dx in range(4):
                for dy in range(4):
                    img.putpixel((lv * 4 + dx, r * 4 + dy), c)
    return img

TIER_PAL = {
    "iron":    {"blade": (168, 170, 180), "edge": (214, 218, 226), "gem": (28, 178, 170)},
    "steel":   {"blade": (132, 166, 204), "edge": (214, 236, 255), "gem": (63, 169, 245)},
    "dungeon": {"blade": (84, 56, 146), "edge": (176, 146, 238), "gem": (170, 96, 255), "gold": (232, 190, 70)},
}
ARMOR_PAL = {"ivory": (222, 204, 160), "white": (240, 232, 210), "red": (150, 28, 40)}
CORE_COLORS = {"baal": (63, 169, 245), "leraje": (205, 85, 60), "cerberus": (160, 30, 70),
               "zagan": (76, 175, 80), "amon": (255, 122, 24), "talmir": (36, 86, 214), "default": (130, 130, 150)}

# ---------------------------------------------------------------- geometry core
NORMALS = {"north": (0, 0, -1), "south": (0, 0, 1), "east": (1, 0, 0), "west": (-1, 0, 0), "up": (0, 1, 0), "down": (0, -1, 0)}
def norm(v):
    l = math.sqrt(sum(c * c for c in v)); return tuple(c / l for c in v)
LIGHT = norm((-0.35, 0.75, 0.55))
def dot(a, b): return sum(x * y for x, y in zip(a, b))

def rot_axis(p, axis, ang, origin=(0, 0, 0)):
    x, y, z = p[0] - origin[0], p[1] - origin[1], p[2] - origin[2]
    a = math.radians(ang); c, s = math.cos(a), math.sin(a)
    if axis == "z": x, y = x * c - y * s, x * s + y * c
    elif axis == "x": y, z = y * c - z * s, y * s + z * c
    elif axis == "y": x, z = x * c + z * s, -x * s + z * c
    return (x + origin[0], y + origin[1], z + origin[2])

def view_apply(p, rot):
    """Minecraft item display: translate(-pivot), then Z first, then Y, then X."""
    q = (p[0] - 8, p[1] - 8, p[2] - 8)
    q = rot_axis(q, "z", rot[2]); q = rot_axis(q, "y", rot[1]); q = rot_axis(q, "x", rot[0])
    return q

class Model:
    def __init__(self, pal):
        self.pal = pal; self.els = []

    def box(self, cx, cy, cz, sx, sy, sz, mat, rot=None, bias=0.0):
        hx, hy, hz = sx / 2, sy / 2, sz / 2
        axis, ang = ("z", 0) if rot is None else rot
        assert ang in ALLOWED, ang
        row = ROWS.index(mat)
        faces = {}
        for name, n in NORMALS.items():
            nr = rot_axis(n, axis, ang) if ang else n
            lv = max(0, min(7, int(round(3.6 + 3.4 * dot(nr, LIGHT) + bias))))
            faces[name] = (lv, row)
        self.els.append(dict(fr=(cx - hx, cy - hy, cz - hz), to=(cx + hx, cy + hy, cz + hz),
                             axis=axis, ang=ang, origin=(cx, cy, cz), faces=faces))

    def corners(self, e):
        (x0, y0, z0), (x1, y1, z1) = e["fr"], e["to"]
        pts = [(x, y, z) for x in (x0, x1) for y in (y0, y1) for z in (z0, z1)]
        if e["ang"]: pts = [rot_axis(p, e["axis"], e["ang"], e["origin"]) for p in pts]
        return pts

    def bbox_view(self, rot):
        pts = [view_apply(p, rot) for e in self.els for p in self.corners(e)]
        lo = [min(p[i] for p in pts) for i in range(3)]; hi = [max(p[i] for p in pts) for i in range(3)]
        return lo, hi

    def all_points(self):
        return [p for e in self.els for p in self.corners(e)]

    def json(self, tex, parent, display=None):
        els = []
        for e in self.els:
            for p in (e["fr"], e["to"]):
                assert all(-16 <= v <= 32 for v in p), ("out of bounds", p)
            d = {"from": [r3(v) for v in e["fr"]], "to": [r3(v) for v in e["to"]]}
            if e["ang"]:
                d["rotation"] = {"angle": e["ang"], "axis": e["axis"], "origin": [r3(v) for v in e["origin"]]}
            d["shade"] = False
            d["faces"] = {f: {"uv": [lv + 0.25, row + 0.25, lv + 0.75, row + 0.75], "texture": "#p"}
                          for f, (lv, row) in e["faces"].items()}
            els.append(d)
        out = {"parent": parent, "textures": {"p": tex, "particle": tex}, "elements": els}
        if display: out["display"] = display
        return out

def fit_display(m, gui_rot, fixed_rot, ground_rot, maxfit=15.0, maxscale=1.35):
    disp = {}
    def entry(rot, limit, extra=(0, 0, 0), base_scale=1.0):
        lo, hi = m.bbox_view(rot)
        ex, ey = hi[0] - lo[0], hi[1] - lo[1]
        s = min(maxscale, limit / max(ex, ey, 1e-6)) * base_scale
        c = [(lo[i] + hi[i]) / 2 for i in range(3)]
        t = [max(-80, min(80, -s * c[i] + extra[i])) for i in range(3)]
        return {"rotation": list(rot), "translation": [r3(v) for v in t], "scale": [r3(s)] * 3}
    disp["gui"] = entry(gui_rot, maxfit)
    disp["fixed"] = entry(fixed_rot, 14.0)
    disp["ground"] = entry(ground_rot, 15.0, extra=(0, 2, 0), base_scale=0.5)
    return disp

# ---------------------------------------------------------------- shared building blocks
CX, CZ = 8.0, 8.0

def chain(m, x, y, phis, lens, widths, thick, mat, edge=None, edge_side=-1, inlay=(), inlay_mat="gold",
          down=False, z=CZ, edge_thick_pad=0.05):
    """Segmented, tapered, curved bar. phi = tilt from vertical toward +x (degrees, snapped)."""
    px, py = x, y
    for i, (phi, L, w) in enumerate(zip(phis, lens, widths)):
        phi = snap(phi); r = math.radians(phi)
        dx, dy = math.sin(r), (-math.cos(r) if down else math.cos(r))
        cx, cy = px + dx * L / 2, py + dy * L / 2
        ang = phi if down else -phi
        m.box(cx, cy, z, w, L * 1.14, thick, mat, rot=("z", ang))
        if edge:
            nx, ny = math.cos(r), (math.sin(r) if down else -math.sin(r))
            off = edge_side * (w * 0.5 - 0.1)
            m.box(cx + nx * off, cy + ny * off, z, w * 0.3, L * 1.14, thick + edge_thick_pad, edge,
                  rot=("z", ang), bias=1.0)
        if i in inlay:
            m.box(cx, cy, z, max(0.25, w * 0.18), L * 0.55, thick + 0.14, inlay_mat, rot=("z", ang))
        px, py = px + dx * L, py + dy * L
    return px, py

def tassel(m, x, y, mat="red"):
    m.box(x, y - 1.0, CZ, 0.36, 2.2, 0.36, mat, rot=("z", 22.5))
    m.box(x + 0.55, y - 2.15, CZ, 0.75, 0.75, 0.75, "gold")
    m.box(x + 0.95, y - 3.3, CZ, 1.0, 1.9, 1.0, mat, rot=("z", 22.5))
    m.box(x + 0.95, y - 3.3, CZ, 0.4, 2.0, 1.1, "gold", rot=("z", 22.5), bias=-0.5)

def ring(m, y, w, mat="gold", h=0.36):
    m.box(CX, y, CZ, w, h, w, mat)

def grip(m, y0, y1, w=1.3, mat="leather", rings=(), bands=()):
    ln = y1 - y0
    m.box(CX, (y0 + y1) / 2, CZ, w, ln, w * 0.95, mat)
    for yb in bands: m.box(CX, yb, CZ, w + 0.1, 0.28, w * 0.95 + 0.1, mat, bias=-1.8)
    for yr in rings: ring(m, yr, w + 0.4)

def crossguard(m, y, half=2.7, curl=22.5, curl_up=True, gem="gem"):
    m.box(CX, y, CZ, half * 2, 0.8, 1.5, "gold")
    m.box(CX, y, CZ, 2.0, 1.55, 1.9, "gold")
    m.box(CX, y, CZ, 1.15, 0.95, 2.3, gem)
    s = 1 if curl_up else -1
    for side in (1, -1):
        m.box(CX + side * (half + 0.15), y + s * 0.5, CZ, 0.8, 1.7, 1.3, "gold", rot=("z", side * s * curl))
        m.box(CX + side * (half + 0.3 * (1 if curl_up else 1)), y + s * 1.35, CZ, 0.7, 0.7, 0.95, "turq")

def pommel_disc(m, y, w=2.9, gem="gem"):
    m.box(CX, y, CZ, w, 0.8, 1.5, "gold")
    m.box(CX, y - 0.6, CZ, w * 0.62, 0.5, 1.1, "bronze")
    m.box(CX, y, CZ, 0.9, 0.5, 1.95, gem)

# ---------------------------------------------------------------- weapons
GRIP_Y = 1.64

def scimitar(m):
    pommel_disc(m, -0.8); tassel(m, CX + 0.9, -1.4)
    grip(m, -0.4, 3.6, rings=(-0.25, 3.45), bands=(0.5, 1.4, 2.3))
    crossguard(m, 4.15, half=2.7, curl_up=True)
    m.box(CX, 5.3, CZ, 1.7, 1.5, 1.05, "gold")                   # langet
    n = 9; L = 13.2 / n
    phis = [42 * (((i + 0.5) / n) ** 1.25) for i in range(n)]
    widths = [2.1, 2.1, 2.05, 2.0, 1.95, 1.85, 1.6, 1.15, 0.6]
    chain(m, CX, 4.55, phis, [L] * n, widths, 0.85, "blade", edge="edge", edge_side=-1, inlay=(1, 3, 5))

def saber(m):   # kilij: down-curved quillons, flared clipped tip
    m.box(CX, -0.9, CZ, 1.6, 0.9, 1.3, "gold")                  # bird-head pommel
    m.box(CX - 0.7, -1.45, CZ, 1.0, 1.0, 1.1, "ivory", rot=("z", 22.5))
    m.box(CX + 0.75, -1.55, CZ, 0.5, 0.7, 1.0, "bronze")
    m.box(CX + 0.4, -0.45, CZ, 0.55, 0.55, 1.5, "gem")
    tassel(m, CX - 0.9, -1.8)
    grip(m, -0.4, 3.6, w=1.35, mat="ivory", rings=(-0.25, 3.45))
    for yy in (0.3, 1.6, 2.9):
        m.box(CX, yy, CZ, 0.55, 0.42, 1.6, "gold")               # rivets through the horn scales
    m.box(CX, 4.15, CZ, 5.4, 0.8, 1.45, "gold")                  # guard bar
    m.box(CX, 4.15, CZ, 1.8, 1.4, 1.8, "gold"); m.box(CX, 4.15, CZ, 1.0, 0.9, 2.2, "gem")
    for side in (1, -1):                                         # quillons curl DOWN toward the grip
        m.box(CX + side * 2.85, 3.5, CZ, 0.85, 1.8, 1.2, "gold", rot=("z", -side * 22.5))
        m.box(CX + side * 3.05, 2.55, CZ, 0.65, 0.65, 0.9, "turq")
    m.box(CX, 5.2, CZ, 1.6, 1.3, 1.0, "gold")
    n = 10; L = 14.4 / n
    phis = [24 * (((i + 0.5) / n) ** 1.3) for i in range(n)]
    widths = [1.7, 1.7, 1.75, 1.85, 2.0, 2.2, 2.4, 2.5, 2.35, 1.6]
    ex, ey = chain(m, CX, 4.45, phis, [L] * n, widths, 0.8, "blade", edge="edge", edge_side=-1, inlay=(2, 4, 6))
    m.box(ex + 0.25, ey - 0.4, CZ, 0.9, 1.3, 0.75, "edge", rot=("z", -22.5))    # clipped-back tip

def dagger(m):  # khanjar: heavy curve, wide belly, bulbous fan pommel
    m.box(CX, -0.6, CZ, 3.6, 0.7, 1.6, "gold")
    for side in (1, -1):
        m.box(CX + side * 1.85, -0.05, CZ, 0.75, 1.4, 1.3, "gold", rot=("z", -side * 22.5))
    m.box(CX, -0.55, CZ, 1.0, 0.5, 2.0, "gem")
    grip(m, -0.2, 3.4, w=1.45, mat="ivory", rings=(-0.1, 3.3))
    m.box(CX, 1.6, CZ, 1.75, 1.0, 1.75, "gold"); m.box(CX, 1.6, CZ, 1.0, 0.6, 2.05, "turq")   # swell
    m.box(CX, 3.85, CZ, 3.0, 0.9, 1.6, "gold"); m.box(CX, 3.85, CZ, 1.1, 0.9, 2.1, "gem")     # collar
    n = 8; L = 9.4 / n
    phis = [45 * (((i + 0.5) / n) ** 1.15) for i in range(n)]
    widths = [2.05, 2.2, 2.2, 2.1, 1.9, 1.6, 1.15, 0.55]
    chain(m, CX, 4.3, phis, [L] * n, widths, 0.9, "blade", edge="edge", edge_side=-1, inlay=(1, 3))
    tassel(m, CX + 1.2, -1.0, mat="ruby")

def spear(m):   # nizah
    m.box(CX, 6.0, CZ, 1.1, 27.0, 1.1, "wood")                   # shaft y -7.5 .. 19.5
    grip(m, -0.4, 4.0, w=1.3, mat="leather", rings=(-0.3, 4.0), bands=(0.6, 1.6, 2.6, 3.4))
    for yb in (-5.0, 10.5, 13.5): ring(m, yb, 1.6, "bronze", 0.5)
    for i, (w, y) in enumerate(((1.4, -7.9), (1.0, -8.7), (0.6, -9.5))):                      # butt spike
        m.box(CX, y, CZ, w, 0.9, w, "bronze")
    m.box(CX, 19.0, CZ, 1.8, 3.4, 1.8, "gold")                   # socket
    ring(m, 17.4, 2.2, "gold", 0.5); ring(m, 20.6, 2.2, "gold", 0.5)
    m.box(CX, 19.0, CZ, 0.9, 1.6, 2.2, "gem")
    for side in (1, -1):                                         # langets
        m.box(CX + side * 1.15, 21.2, CZ, 0.45, 2.6, 0.7, "gold", rot=("z", -side * 22.5))
    widths = [2.6, 3.1, 3.3, 3.2, 2.9, 2.5, 2.0, 1.5, 1.0, 0.5]
    y = 20.9
    for w in widths:
        m.box(CX, y + 0.35, CZ, w, 0.85, 0.8, "blade")
        m.box(CX - w / 2 + 0.2, y + 0.35, CZ, 0.45, 0.85, 0.85, "edge", bias=1)
        m.box(CX + w / 2 - 0.2, y + 0.35, CZ, 0.45, 0.85, 0.85, "edge", bias=1)
        y += 0.7
    m.box(CX, 24.3, CZ, 0.5, 7.0, 1.15, "dark", bias=-0.5)     # central ridge
    # pennant + tassels under the socket
    m.box(CX + 2.3, 15.6, CZ, 3.6, 2.3, 0.22, "red", rot=("z", -22.5))
    m.box(CX + 2.3, 15.6, CZ, 3.7, 0.4, 0.28, "gold", rot=("z", -22.5))
    for side in (1, -1):
        m.box(CX + side * 0.9, 15.4, CZ, 0.55, 3.2, 0.5, "ruby", rot=("z", -side * 22.5))
        m.box(CX + side * 1.6, 13.5, CZ, 0.7, 0.7, 0.7, "gold")

def axe(m):     # tabar: crescent bit + back spike
    m.box(CX, 8.0, CZ, 1.2, 24.0, 1.2, "wood")                    # haft y -4 .. 20
    grip(m, -0.4, 4.0, w=1.35, rings=(-0.3, 4.0), bands=(0.6, 1.6, 2.6, 3.4))
    m.box(CX, -4.4, CZ, 1.8, 0.9, 1.8, "gold"); m.box(CX, -5.1, CZ, 1.1, 0.7, 1.1, "bronze")
    for yb in (7.0, 11.0): ring(m, yb, 1.7, "bronze", 0.5)
    m.box(CX, 17.0, CZ, 2.1, 5.2, 2.0, "gold")                    # socket / collar
    ring(m, 14.5, 2.5, "gold", 0.5); ring(m, 19.5, 2.5, "gold", 0.5)
    # crescent blade toward +x
    n = 22; y0, y1 = 11.2, 23.0
    for i in range(n):
        t = (i + 0.5) / n
        y = y0 + (y1 - y0) * t
        reach = 5.9 * (math.sin(math.pi * t) ** 0.55) + 0.7
        h = (y1 - y0) / n * 1.12
        core = min(reach * 0.62, 3.6)
        m.box(CX + 1.0 + core / 2, y, CZ, core, h, 1.0, "blade")
        m.box(CX + 1.0 + core + (reach - core) / 2, y, CZ, reach - core, h, 0.45, "edge", bias=1)
    m.box(CX + 2.6, 17.0, CZ, 1.5, 1.5, 1.35, "gem", rot=("z", 45))                 # gem inset
    m.box(CX + 2.6, 17.0, CZ, 2.4, 2.4, 1.05, "gold", rot=("z", 45))
    for yy in (13.6, 20.4): m.box(CX + 2.2, yy, CZ, 0.55, 0.55, 1.2, "gold", rot=("z", 45))
    # back spike toward -x
    x = CX - 1.05
    for (w, h, th) in ((1.6, 1.6, 1.3), (1.5, 1.25, 1.05), (1.2, 0.95, 0.85), (0.9, 0.6, 0.6)):
        m.box(x - w / 2, 17.0, CZ, w, h, th, "dark", bias=0.4); x -= w * 0.92
    m.box(CX + 1.1, 8.5, CZ, 0.6, 3.0, 0.6, "ruby", rot=("z", -22.5))              # haft tassel
    m.box(CX + 1.6, 6.6, CZ, 0.8, 0.8, 0.8, "gold")

def bow(m, pull=None):
    # riser / grip
    m.box(CX, 8.0, CZ, 1.6, 4.4, 1.6, "leather")
    for yy in (5.8, 10.2): ring(m, yy, 2.1, "gold", 0.45)
    for yy in (7.2, 8.0, 8.8): m.box(CX, yy, CZ, 1.7, 0.22, 1.7, "leather", bias=-1.8)
    m.box(CX, 8.0, CZ, 0.8, 0.8, 2.2, "gem")
    m.box(CX - 1.05, 8.0, CZ, 0.7, 2.4, 1.2, "gold")                                 # arrow shelf
    tassel(m, CX + 0.6, 5.4, mat="ruby")
    phis = [22.5, 22.5, 22.5, 0, 0, -22.5]; L = 1.9
    widths = [1.4, 1.35, 1.25, 1.15, 1.0, 0.9]
    ux, uy = chain(m, CX, 10.2, phis, [L] * 6, widths, 1.15, "wood", edge="ivory", edge_side=1, inlay=(1, 4))
    lx, ly = chain(m, CX, 5.8, phis, [L] * 6, widths, 1.15, "wood", edge="ivory", edge_side=1, inlay=(1, 4), down=True)
    for (tx, ty, s) in ((ux, uy, 1), (lx, ly, -1)):
        m.box(tx, ty + s * 0.35, CZ, 0.95, 1.1, 1.0, "gold")
        m.box(tx + 0.1, ty + s * 1.0, CZ, 0.55, 0.55, 0.75, "turq")
    xs = ux + 0.15
    if pull is None:
        m.box(xs, 8.0, CZ, 0.26, uy - ly, 0.26, "white")
        return
    lat = [1.5, 3.0, 4.6][pull]; k = pull + 1; Ld = [4.0, 4.0, 3.6][pull]
    half = uy - 8.0
    vlen = max(0.1, half - k * Ld * math.cos(math.radians(22.5)))
    for sgn in (1, -1):                                          # upper half sgn=1, lower half sgn=-1
        y = 8.0 + sgn * half; x = xs
        m.box(x, y - sgn * vlen / 2, CZ, 0.26, vlen, 0.26, "white"); y -= sgn * vlen
        for _ in range(k):
            dx, dy = Ld * math.sin(math.radians(22.5)), Ld * math.cos(math.radians(22.5))
            m.box(x + dx / 2, y - sgn * dy / 2, CZ, 0.26, Ld * 1.1, 0.26, "white", rot=("z", 22.5 * sgn))
            x += dx; y -= sgn * dy
    nx = xs + lat
    head_x = CX - 6.2
    m.box((nx + head_x) / 2, 8.0, CZ, nx - head_x, 0.36, 0.36, "wood")                     # shaft
    m.box(head_x - 0.4, 8.0, CZ, 1.5, 0.95, 0.4, "edge"); m.box(head_x - 1.3, 8.0, CZ, 1.0, 0.5, 0.36, "blade")
    m.box(nx - 1.1, 8.0, CZ, 1.6, 0.14, 1.0, "red"); m.box(nx - 1.1, 8.0, CZ, 1.6, 1.0, 0.14, "red")   # fletching

WEAPONS = {"scimitar": scimitar, "saber": saber, "dagger": dagger, "spear": spear, "axe": axe}

# ---------------------------------------------------------------- armor icons (upright, front = +z)
def helmet(m):
    m.box(8, 5.0, 8, 8.4, 3.0, 8.4, "ivory")
    m.box(8, 5.9, 12.3, 5.8, 0.85, 0.3, "black")                                      # eye slit
    for sx in (-1.3, 1.3): m.box(8 + sx, 5.9, 12.42, 0.6, 0.35, 0.15, "turq")
    m.box(8, 7.0, 8, 8.8, 0.9, 8.8, "gold"); m.box(8, 7.0, 8, 8.8, 0.9, 8.8, "gold", rot=("y", 45))
    m.box(8, 8.4, 8, 8.0, 1.9, 8.0, "ivory", rot=("y", 22.5))
    m.box(8, 9.2, 8, 8.0, 1.9, 8.0, "white", rot=("y", -22.5), bias=-0.3)
    m.box(8, 10.6, 8, 6.6, 1.8, 6.6, "ivory", rot=("y", 22.5), bias=-0.5)
    m.box(8, 12.0, 8, 4.4, 1.6, 4.4, "white", rot=("y", -22.5))
    m.box(8, 13.2, 8, 2.6, 1.5, 2.6, "red", rot=("y", 22.5))
    m.box(8, 14.4, 8, 0.6, 1.6, 0.6, "gold"); m.box(8, 15.1, 8, 0.9, 0.9, 0.9, "turq")
    m.box(8, 7.0, 12.3, 2.0, 2.0, 0.9, "gold", rot=("z", 45)); m.box(8, 7.0, 12.55, 1.2, 1.2, 1.0, "gem", rot=("z", 45))
    for yy in (9.85, 11.25): m.box(8, yy, 8, 8.3, 0.32, 8.3, "white", rot=("y", 45), bias=0.5)       # wrap seams
    m.box(12.6, 3.6, 7.0, 2.0, 6.8, 1.1, "red", rot=("z", 22.5)); m.box(12.9, 1.5, 7.0, 2.1, 0.5, 1.2, "gold", rot=("z", 22.5))
    m.box(3.6, 4.4, 7.4, 1.6, 5.2, 1.0, "ivory", rot=("z", -22.5), bias=-0.6)

def chestplate(m):
    m.box(8, 7.4, 8, 8.0, 8.4, 4.6, "ivory")
    m.box(8, 12.0, 8, 11.4, 1.6, 5.6, "red"); m.box(8, 11.15, 8, 11.6, 0.4, 5.8, "gold"); m.box(8, 12.95, 8, 11.6, 0.4, 5.8, "gold")
    for side in (1, -1):                                                              # V-neck trim on the mantle
        m.box(8 + side * 0.85, 12.0, 10.9, 0.5, 1.9, 0.3, "gold", rot=("z", -side * 22.5))
    m.box(8, 11.15, 10.95, 0.7, 0.7, 0.35, "turq")
    for side in (1, -1):
        m.box(8 + side * 5.8, 12.3, 8, 3.6, 1.2, 5.0, "gold", rot=("z", -side * 22.5))
        m.box(8 + side * 5.9, 13.2, 8, 1.1, 1.1, 1.1, "gem")
        m.box(8 + side * 6.9, 8.6, 8, 2.7, 6.0, 3.6, "ivory", rot=("z", side * 22.5), bias=-0.3)
        m.box(8 + side * 7.7, 5.4, 8, 3.0, 0.9, 3.9, "gold", rot=("z", side * 22.5))
        m.box(8 + side * 4.0, 7.4, 8, 0.4, 8.2, 4.8, "gold")
    m.box(8, 8.6, 10.5, 4.0, 5.0, 0.5, "gold"); m.box(8, 9.4, 10.8, 2.4, 2.4, 0.7, "gold", rot=("z", 45))
    m.box(8, 9.4, 11.0, 1.4, 1.4, 0.9, "gem", rot=("z", 45))
    for yy in (7.0, 6.4, 5.8): m.box(8, yy, 10.75, 3.2, 0.2, 0.35, "bronze")
    m.box(8, 4.6, 8, 8.6, 1.6, 5.0, "red"); m.box(8, 4.6, 10.5, 1.9, 1.4, 0.7, "gold"); m.box(8, 4.6, 10.8, 0.9, 0.8, 0.7, "gem")
    m.box(9.6, 2.5, 10.4, 1.6, 4.2, 0.5, "red", rot=("z", 22.5)); m.box(6.6, 2.9, 10.4, 1.5, 3.4, 0.5, "ruby", rot=("z", -22.5))
    m.box(8, 2.5, 8, 8.2, 2.2, 4.8, "ivory", bias=-0.4); m.box(8, 1.4, 8, 8.4, 0.4, 5.0, "gold")

def leggings(m):
    for side in (1, -1):
        x = 8 + side * 2.2
        m.box(x, 9.0, 8, 4.2, 4.4, 4.4, "ivory")
        m.box(x, 4.8, 8, 3.5, 4.4, 3.7, "ivory", bias=-0.4)
        m.box(x, 2.8, 8, 3.1, 0.9, 3.5, "gold"); m.box(x, 2.0, 8, 3.2, 0.6, 3.6, "red")
        m.box(x + side * 2.15, 6.8, 8, 0.3, 7.0, 4.5, "gold")
        m.box(x, 6.9, 10.3, 0.35, 5.0, 0.3, "bronze")
    m.box(8, 11.7, 8, 9.6, 1.6, 4.9, "red"); m.box(8, 12.6, 8, 9.8, 0.35, 5.0, "gold")
    m.box(8, 11.7, 10.5, 2.0, 1.5, 0.7, "gold"); m.box(8, 11.7, 10.8, 1.0, 0.8, 0.7, "gem")
    m.box(8, 7.4, 10.5, 3.4, 6.8, 0.45, "red"); m.box(8, 7.4, 10.8, 0.5, 6.6, 0.3, "gold")
    for side in (1, -1): m.box(8 + side * 1.55, 7.4, 10.8, 0.3, 6.6, 0.28, "gold", bias=-0.6)
    m.box(8, 3.9, 10.6, 3.4, 0.7, 0.4, "gold"); m.box(8, 3.5, 10.8, 0.8, 0.8, 0.8, "turq")

def boots(m):
    for side in (1, -1):
        x = 8 + side * 2.7
        m.box(x, 6.6, 7.4, 3.4, 4.6, 3.4, "leather"); m.box(x, 8.7, 7.4, 3.7, 1.0, 3.7, "gold"); m.box(x, 9.5, 7.4, 3.5, 0.7, 3.5, "red")
        m.box(x, 8.7, 9.2, 0.9, 0.9, 0.5, "gem")
        m.box(x, 2.6, 9.2, 3.4, 2.2, 5.8, "leather"); m.box(x, 1.4, 9.2, 3.5, 0.5, 6.1, "dark", bias=-0.5)
        m.box(x, 3.5, 9.2, 3.55, 0.35, 5.3, "gold")
        m.box(x, 3.4, 12.6, 2.8, 1.7, 1.9, "leather", rot=("x", -22.5))
        m.box(x, 4.4, 13.5, 2.3, 1.4, 1.6, "leather", rot=("x", -45), bias=0.3)
        m.box(x, 5.6, 13.8, 0.9, 0.9, 0.9, "gold")
        m.box(x + side * 1.75, 2.6, 9.2, 0.15, 1.2, 4.6, "bronze")

# ---------------------------------------------------------------- Djinn core
def core(m):
    for w, y, mat in ((6.6, 1.5, "bronze"), (5.2, 2.5, "gold"), (4.2, 3.35, "bronze")):
        m.box(8, y, 8, w, 1.0, w, mat); m.box(8, y, 8, w, 1.0, w, mat, rot=("y", 45))
    for i in range(8):
        a = math.radians(45 * i); m.box(8 + 2.5 * math.cos(a), 3.95, 8 + 2.5 * math.sin(a), 0.6, 0.3, 0.6, "white")
    m.box(8, 2.5, 10.7, 1.6, 0.7, 0.4, "gem", bias=1)
    for sx in (-1.9, 1.9):
        for sz in (-1.9, 1.9): m.box(8 + sx, 8.0, 8 + sz, 0.55, 8.0, 0.55, "gold")
    for y in (4.2, 11.9): m.box(8, y, 8, 5.2, 0.7, 5.2, "gold"); m.box(8, y, 8, 5.2, 0.7, 5.2, "gold", rot=("y", 45))
    m.box(8, 8.0, 8, 4.9, 0.45, 4.9, "bronze"); m.box(8, 8.0, 8, 4.9, 0.45, 4.9, "bronze", rot=("y", 45))
    m.box(8, 13.1, 8, 1.7, 1.7, 1.7, "gold", rot=("y", 45)); m.box(8, 14.6, 8, 0.8, 1.9, 0.8, "turq")
    m.box(8, 8.0, 8, 3.4, 3.4, 3.4, "gem", rot=("y", 45), bias=1)
    m.box(8, 8.0, 8, 3.4, 3.4, 3.4, "gem", rot=("x", 45), bias=1)
    m.box(8, 8.0, 8, 3.4, 3.4, 3.4, "gem", rot=("z", 45), bias=1)
    m.box(8, 8.0, 8, 1.5, 1.5, 1.5, "white", bias=2)
    for (dx, dy, dz) in ((3.6, 9.6, 1.0), (-3.4, 6.6, -1.5), (1.2, 10.9, -3.4), (-1.6, 5.6, 3.5), (3.0, 5.4, -2.6), (-3.2, 10.2, 2.2)):
        m.box(8 + dx, dy, 8 + dz, 0.55, 0.55, 0.55, "gem", rot=("y", 45), bias=1)

# ---------------------------------------------------------------- Baal's Extreme Magic blade
SPECTRAL_PAL = {"blade": (90, 170, 255), "edge": (215, 242, 255), "gold": (240, 205, 95), "gem": (130, 225, 255),
                "white": (235, 250, 255), "leather": (60, 90, 170)}
SPECTRAL_ALPHA = {"blade": 165, "edge": 215, "gold": 235, "gem": 235, "white": 240, "leather": 210}

def baal_greatsword(m):
    """Tip at y=31 (SpectralBladeRenderer.MODEL_TIP_OFFSET depends on this)."""
    m.box(8, -3.7, 8, 2.4, 1.2, 1.9, "gold"); m.box(8, -3.7, 8, 1.0, 1.0, 2.3, "gem", bias=1)
    m.box(8, -0.8, 8, 1.35, 4.6, 1.35, "leather")
    for yy in (-2.4, -1.2, 0.0, 1.2): m.box(8, yy, 8, 1.5, 0.3, 1.5, "gold")
    m.box(8, 2.3, 8, 7.2, 1.0, 1.8, "gold")
    for side in (1, -1):
        m.box(8 + side * 4.0, 2.9, 8, 1.7, 1.1, 1.6, "gold", rot=("z", side * 22.5))
        m.box(8 + side * 4.9, 3.8, 8, 1.0, 1.5, 1.2, "gold", rot=("z", side * 45))
        m.box(8 + side * 5.2, 4.8, 8, 0.75, 0.75, 0.95, "gem", bias=1)
    m.box(8, 2.6, 8, 2.4, 2.4, 2.2, "gold", rot=("z", 45)); m.box(8, 2.6, 8, 1.4, 1.4, 2.6, "gem", rot=("z", 45), bias=1)
    n, y, h = 24, 3.4, 1.15
    for i in range(n):
        t = (i + 0.5) / n
        w = 3.6 if t < 0.62 else max(0.3, 3.6 * (1 - (t - 0.62) / 0.38) ** 0.85)
        m.box(8, y + h / 2, 8, w, h * 1.03, 0.9, "blade")
        if w > 0.9:
            for side in (1, -1):
                m.box(8 + side * (w / 2 - w * 0.06), y + h / 2, 8, w * 0.12, h * 1.03, 0.98, "edge", bias=1)
            m.box(8, y + h / 2, 8, max(0.25, w * 0.09), h * 1.03, 1.02, "white", bias=2)
        if i % 3 == 1 and w > 1.4:                      # lightning veins across both faces
            for zf in (8.53, 7.47):
                m.box(8 + (0.5 if i % 2 else -0.5), y + h / 2, zf, w * 0.55, 0.22, 0.08, "edge", rot=("z", 45 if i % 2 else -45), bias=2)
        y += h

# ---------------------------------------------------------------- UI / effect textures
def ui_textures():
    from PIL import ImageFilter
    os.makedirs(f"{A}/textures/gui/arts", exist_ok=True); os.makedirs(f"{A}/textures/effect", exist_ok=True)
    BG, GOLDC, BOLT, CORE = (16, 24, 52, 255), (214, 175, 58, 255), (120, 200, 255, 255), (235, 248, 255, 255)
    def icon(name, draw_fn):
        im = Image.new("RGBA", (24, 24), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
        d.rounded_rectangle((0, 0, 23, 23), 4, fill=BG, outline=GOLDC)
        draw_fn(d); im.save(f"{A}/textures/gui/arts/{name}.png")
    bolt = [(15, 3), (10, 11), (14, 11), (8, 20), (12, 12), (8, 12)]
    icon("lightning_spear", lambda d: (d.line([(4, 19), (19, 4)], fill=BOLT, width=2), d.polygon([(19, 4), (13, 5), (18, 10)], fill=CORE)))
    icon("chain_lightning", lambda d: (d.line([(4, 18), (9, 8), (13, 15), (19, 5)], fill=BOLT, width=2),
                                        [d.ellipse((x - 2, y - 2, x + 2, y + 2), fill=CORE) for x, y in ((4, 18), (13, 15), (19, 5))]))
    icon("thunder_cage", lambda d: (d.ellipse((4, 4, 19, 19), outline=BOLT, width=2), [d.line([(x, 6), (x, 17)], fill=CORE, width=1) for x in (8, 11, 14, 17)]))
    icon("bararaq_saiqa", lambda d: (d.line([(12, 21), (12, 9)], fill=CORE, width=2), d.line([(9, 18), (15, 18)], fill=GOLDC, width=2),
                                      d.line([(16, 2), (12, 6), (15, 6), (11, 10)], fill=BOLT, width=2)))
    icon("bararaq_inqerad_saiqa", lambda d: (d.ellipse((5, 2, 18, 7), outline=BOLT, width=1), d.polygon([(10, 6), (14, 6), (13, 19), (12, 22), (11, 19)], fill=BOLT, outline=CORE),
                                              d.line([(8, 8), (16, 8)], fill=GOLDC, width=2)))
    icon("lightning_dash", lambda d: (d.line([(3, 9), (9, 9)], fill=CORE, width=1), d.line([(3, 15), (8, 15)], fill=CORE, width=1),
                                       d.line([(10, 6), (16, 12), (12, 12), (20, 18)], fill=BOLT, width=2)))
    # magic circle (Persian geometric star + rune band), drawn bright with a soft glow layer
    S = 512; im = Image.new("RGBA", (S, S), (0, 0, 0, 0)); d = ImageDraw.Draw(im); c = S / 2
    LINE = (205, 238, 255, 255)
    def ring(r, w): d.ellipse((c - r, c - r, c + r, c + r), outline=LINE, width=w)
    for r, w in ((248, 6), (234, 3), (196, 4), (184, 2), (72, 4), (52, 2)): ring(r, w)
    import random as _r; rng = _r.Random(431)
    for k in range(24):                                              # rune band
        a0 = 2 * math.pi * k / 24
        cx, cy = c + math.cos(a0) * 215, c + math.sin(a0) * 215
        for _ in range(3):
            ax, ay = rng.uniform(-9, 9), rng.uniform(-9, 9); bx, by = rng.uniform(-9, 9), rng.uniform(-9, 9)
            d.line([(cx + ax, cy + ay), (cx + bx, cy + by)], fill=LINE, width=3)
    for rot in (0, math.pi / 4):                                      # 8-pointed star (two squares)
        pts = [(c + math.cos(rot + math.pi / 2 * i) * 180, c + math.sin(rot + math.pi / 2 * i) * 180) for i in range(4)]
        d.polygon(pts, outline=LINE, width=4)
    pts16 = [(c + math.cos(2 * math.pi * i / 16) * 180, c + math.sin(2 * math.pi * i / 16) * 180) for i in range(16)]
    for i in range(16): d.line([pts16[i], pts16[(i + 5) % 16]], fill=LINE, width=2)
    for i in range(8):                                                # rosette
        a0 = 2 * math.pi * i / 8; px, py = c + math.cos(a0) * 40, c + math.sin(a0) * 40
        d.ellipse((px - 16, py - 16, px + 16, py + 16), outline=LINE, width=3)
    glow = im.filter(ImageFilter.GaussianBlur(7))
    out = Image.alpha_composite(Image.new("RGBA", (S, S), (0, 0, 0, 0)), glow)
    out = Image.alpha_composite(out, glow); out = Image.alpha_composite(out, im)
    out.save(f"{A}/textures/effect/baal_magic_circle.png")

# ---------------------------------------------------------------- output helpers
def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f: json.dump(obj, f, separators=(",", ":"))

def save_palette(name, img):
    p = f"{A}/textures/item/{name}.png"; os.makedirs(os.path.dirname(p), exist_ok=True); img.save(p)

WEAPON_GUI = (12, -20, -45)
WEAPON_FIXED = (0, 180, 45)
WEAPON_GROUND = (0, 0, -45)
PROP_GUI = (14, -24, 0)

PARENTS = {
    "handheld_3d": {"gui_light": "front", "display": {
        "thirdperson_righthand": {"rotation": [0, -90, 10], "translation": [0, 4, 0.5], "scale": [0.85, 0.85, 0.85]},
        "thirdperson_lefthand": {"rotation": [0, 90, -10], "translation": [0, 4, 0.5], "scale": [0.85, 0.85, 0.85]},
        "firstperson_righthand": {"rotation": [0, -90, -20], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
        "firstperson_lefthand": {"rotation": [0, 90, 20], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
        "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]}}},
    "bow_3d": {"gui_light": "front", "display": {
        "thirdperson_righthand": {"rotation": [-80, 260, -85], "translation": [-1, -2, 2.5], "scale": [0.9, 0.9, 0.9]},
        "thirdperson_lefthand": {"rotation": [-80, -280, 85], "translation": [-1, -2, 2.5], "scale": [0.9, 0.9, 0.9]},
        "firstperson_righthand": {"rotation": [0, -90, -20], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
        "firstperson_lefthand": {"rotation": [0, 90, 20], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
        "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]}}},
    "prop_3d": {"gui_light": "front", "display": {
        "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.55, 0.55, 0.55]},
        "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.55, 0.55, 0.55]},
        "firstperson_righthand": {"rotation": [0, -90, 0], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
        "firstperson_lefthand": {"rotation": [0, 90, 0], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
        "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]}}},
}

# ---------------------------------------------------------------- preview renderer
def render(model, rot, size, bg=(30, 32, 44)):
    lo, hi = model.bbox_view(rot)
    ex, ey = hi[0] - lo[0], hi[1] - lo[1]
    s = 0.92 * size / max(ex, ey, 1e-6)
    cx, cy = (lo[0] + hi[0]) / 2, (lo[1] + hi[1]) / 2
    img = Image.new("RGB", (size, size), bg); d = ImageDraw.Draw(img)
    faces = []
    for e in model.els:
        (x0, y0, z0), (x1, y1, z1) = e["fr"], e["to"]
        quads = {"south": [(x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)],
                 "north": [(x1, y0, z0), (x0, y0, z0), (x0, y1, z0), (x1, y1, z0)],
                 "east": [(x1, y0, z1), (x1, y0, z0), (x1, y1, z0), (x1, y1, z1)],
                 "west": [(x0, y0, z0), (x0, y0, z1), (x0, y1, z1), (x0, y1, z0)],
                 "up": [(x0, y1, z1), (x1, y1, z1), (x1, y1, z0), (x0, y1, z0)],
                 "down": [(x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1)]}
        for name, q in quads.items():
            if e["ang"]: q = [rot_axis(p, e["axis"], e["ang"], e["origin"]) for p in q]
            q = [view_apply(p, rot) for p in q]
            n = NORMALS[name]
            if e["ang"]: n = rot_axis(n, e["axis"], e["ang"])
            n = rot_axis(rot_axis(rot_axis(n, "z", rot[2]), "y", rot[1]), "x", rot[0])
            if n[2] <= 0.02: continue
            lv, row = e["faces"][name]
            col = model.pal.getpixel((lv * 4 + 1, row * 4 + 1))[:3]
            faces.append((sum(p[2] for p in q) / 4, q, col))
    faces.sort(key=lambda f: f[0])
    for _, q, col in faces:
        poly = [(size / 2 + (p[0] - cx) * s, size / 2 - (p[1] - cy) * s) for p in q]
        d.polygon(poly, fill=col, outline=tuple(int(c * 0.55) for c in col))
    return img

def sheet(items, rot, cols, cell, path, labels=True):
    rows = (len(items) + cols - 1) // cols
    img = Image.new("RGB", (cols * cell, rows * cell), (30, 32, 44)); d = ImageDraw.Draw(img)
    for i, (label, m) in enumerate(items):
        img.paste(render(m, rot, cell), ((i % cols) * cell, (i // cols) * cell))
        if labels: d.text(((i % cols) * cell + 6, (i // cols) * cell + 4), label, fill=(255, 255, 255))
    os.makedirs(os.path.dirname(path), exist_ok=True); img.save(path)

# ---------------------------------------------------------------- armor worn textures (64x32 layers)
def armor_layers():
    T = (0, 0, 0, 0)
    IV, IV2, WH = (222, 204, 160, 255), (200, 182, 140, 255), (240, 232, 210, 255)
    GD, GD2, RD, RD2 = (214, 175, 58, 255), (170, 130, 40, 255), (150, 28, 40, 255), (110, 20, 30, 255)
    TQ, LE, LE2, BK = (28, 178, 170, 255), (104, 64, 38, 255), (78, 46, 26, 255), (20, 20, 28, 255)
    def canvas(): return Image.new("RGBA", (64, 32), T)
    def rect(im, x0, y0, x1, y1, c):
        for x in range(x0, x1):
            for y in range(y0, y1): im.putpixel((x, y), c)
    def weave(im, x0, y0, x1, y1, a, b):
        for x in range(x0, x1):
            for y in range(y0, y1): im.putpixel((x, y), a if (x + y) % 2 == 0 else b)
    # ---- layer 1: helmet (head), chestplate (body+arms), boots (legs, bottom rows)
    l1 = canvas()
    # head: top 8..16,0..8 ; bottom 16..24 ; sides right 0..8 / front 8..16 / left 16..24 / back 24..32 (y 8..16)
    weave(l1, 8, 0, 16, 8, IV, IV2)
    for i in range(0, 16):                                        # diagonal wrap on top
        for j in range(8):
            if (i + j) % 4 == 0 and 8 + (i % 8) < 16: l1.putpixel((8 + (i + j) % 8, j), WH)
    rect(l1, 11, 3, 13, 5, RD)
    for x0 in (0, 8, 16, 24):                                     # turban band on all four sides
        weave(l1, x0, 8, x0 + 8, 11, IV, IV2)
        rect(l1, x0, 11, x0 + 8, 12, GD)
        for x in range(x0, x0 + 8, 2): l1.putpixel((x, 8), WH)
        rect(l1, x0, 12, x0 + 8, 13, GD2)
    rect(l1, 11, 10, 13, 12, TQ); rect(l1, 10, 11, 14, 12, GD)    # front gem
    weave(l1, 8, 14, 16, 16, IV, IV2); rect(l1, 8, 14, 16, 15, IV2)   # lower face wrap (front)
    for x0 in (0, 16, 24): weave(l1, x0, 13, x0 + 8, 16, IV, IV2)      # sides/back drape
    for x0 in (0, 16, 24): rect(l1, x0, 15, x0 + 8, 16, GD)
    rect(l1, 24, 13, 32, 14, RD)
    # body: top 20..28,16..20 ; bottom 28..36 ; right 16..20 ; front 20..28 ; left 28..32 ; back 32..40 (y 20..32)
    for x0, x1 in ((20, 28), (32, 40), (16, 20), (28, 32)):
        weave(l1, x0, 20, x1, 32, IV, IV2)
    rect(l1, 20, 20, 28, 21, GD); rect(l1, 20, 21, 28, 22, RD2)
    for x in (20, 27): rect(l1, x, 20, x + 1, 32, GD)
    rect(l1, 22, 22, 26, 27, GD); rect(l1, 23, 23, 25, 26, GD2); rect(l1, 23, 24, 25, 25, TQ)
    rect(l1, 20, 28, 28, 30, RD); rect(l1, 20, 30, 28, 31, GD); rect(l1, 23, 28, 25, 30, GD); rect(l1, 23, 29, 25, 30, TQ)
    rect(l1, 20, 31, 28, 32, GD2)
    rect(l1, 32, 20, 40, 21, GD); rect(l1, 32, 28, 40, 30, RD); rect(l1, 32, 30, 40, 31, GD); rect(l1, 35, 23, 37, 27, GD)
    for x0, x1 in ((16, 20), (28, 32)): rect(l1, x0, 28, x1, 30, RD); rect(l1, x0, 30, x1, 31, GD)
    weave(l1, 20, 16, 28, 20, GD, GD2); weave(l1, 28, 16, 36, 20, IV, IV2)
    # arms (right arm block 40..56): top 44..48,16..20 ; sides
    for x0, x1 in ((40, 44), (44, 48), (48, 52), (52, 56)):
        weave(l1, x0, 20, x1, 32, IV, IV2); rect(l1, x0, 28, x1, 30, GD); rect(l1, x0, 30, x1, 32, RD2)
    weave(l1, 44, 16, 48, 20, GD, GD2); weave(l1, 48, 16, 52, 20, IV, IV2)
    for x0, x1 in ((40, 44), (44, 48), (48, 52), (52, 56)): rect(l1, x0, 20, x1, 22, GD)
    # boots on the legs region (bottom 6 rows: y 26..32)
    for x0, x1 in ((0, 4), (4, 8), (8, 12), (12, 16)):
        weave(l1, x0, 27, x1, 32, LE, LE2); rect(l1, x0, 26, x1, 27, GD); rect(l1, x0, 25, x1, 26, RD)
        rect(l1, x0, 31, x1, 32, BK)
    rect(l1, 5, 26, 7, 27, TQ); weave(l1, 4, 16, 8, 20, LE, LE2); weave(l1, 8, 16, 12, 20, BK, BK)
    # ---- layer 2: leggings (legs + waist)
    l2 = canvas()
    for x0, x1 in ((0, 4), (4, 8), (8, 12), (12, 16)):
        weave(l2, x0, 20, x1, 32, IV, IV2)
        rect(l2, x0, 29, x1, 30, GD); rect(l2, x0, 30, x1, 32, RD)
        rect(l2, x0, 20, x1, 21, GD)
    for x in (4, 7): rect(l2, x, 21, x + 1, 29, GD2)                      # thigh stripes (front)
    rect(l2, 5, 21, 7, 29, RD); rect(l2, 5, 21, 7, 22, GD); rect(l2, 5, 24, 7, 25, GD)
    weave(l2, 4, 16, 8, 20, GD, GD2)
    for x0, x1 in ((20, 28), (32, 40), (16, 20), (28, 32)):                # waist sash rows in body region
        rect(l2, x0, 20, x1, 24, RD); rect(l2, x0, 20, x1, 21, GD); rect(l2, x0, 23, x1, 24, GD)
    rect(l2, 23, 21, 25, 23, GD); rect(l2, 23, 21, 25, 22, TQ)
    return l1, l2

# ---------------------------------------------------------------- main
def main():
    os.makedirs(f"{A}/models/item", exist_ok=True)
    for name, obj in PARENTS.items(): write_json(f"{A}/models/item/{name}.json", obj)

    preview = {"weapons": [], "extras": []}
    for tier, over in TIER_PAL.items():
        pal = make_palette(over); save_palette(f"palette_{tier}", pal)
        tex = f"magi:item/palette_{tier}"
        for kind, fn in WEAPONS.items():
            m = Model(pal); fn(m)
            dsp = fit_display(m, WEAPON_GUI, WEAPON_FIXED, WEAPON_GROUND)
            write_json(f"{A}/models/item/{tier}_{kind}.json", m.json(tex, "magi:item/handheld_3d", dsp))
            preview["weapons"].append((f"{tier} {kind}", m))
        # bow: base + 3 pulling states
        base = Model(pal); bow(base, None)
        dsp = fit_display(base, WEAPON_GUI, WEAPON_FIXED, WEAPON_GROUND)
        bj = base.json(tex, "magi:item/bow_3d", dsp)
        bj["overrides"] = [
            {"predicate": {"pulling": 1}, "model": f"magi:item/{tier}_bow_pulling_0"},
            {"predicate": {"pulling": 1, "pull": 0.65}, "model": f"magi:item/{tier}_bow_pulling_1"},
            {"predicate": {"pulling": 1, "pull": 0.9}, "model": f"magi:item/{tier}_bow_pulling_2"}]
        write_json(f"{A}/models/item/{tier}_bow.json", bj)
        preview["weapons"].append((f"{tier} bow", base))
        for i in range(3):
            pm = Model(pal); bow(pm, i)
            write_json(f"{A}/models/item/{tier}_bow_pulling_{i}.json", pm.json(tex, "magi:item/bow_3d", dsp))
            if tier == "steel": preview["extras"].append((f"steel bow pull {i}", pm))

    apal = make_palette(ARMOR_PAL); save_palette("palette_armor", apal)
    for name, fn in (("helmet", helmet), ("chestplate", chestplate), ("leggings", leggings), ("boots", boots)):
        m = Model(apal); fn(m)
        dsp = fit_display(m, PROP_GUI, (0, 180, 0), (0, 0, 0), maxfit=15.0, maxscale=1.2)
        write_json(f"{A}/models/item/desert_nomad_{name}.json", m.json("magi:item/palette_armor", "magi:item/prop_3d", dsp))
        preview["extras"].append((f"nomad {name}", m))

    variants = []
    for dj, col in CORE_COLORS.items():
        cpal = make_palette({"gem": col}); save_palette(f"palette_core_{dj}", cpal)
        m = Model(cpal); core(m)
        dsp = fit_display(m, PROP_GUI, (0, 180, 0), (0, 0, 0), maxfit=15.0, maxscale=1.2)
        write_json(f"{A}/models/item/djinn_core_{dj}.json", m.json(f"magi:item/palette_core_{dj}", "magi:item/prop_3d", dsp))
        preview["extras"].append((f"core {dj}", m))
        if dj != "default": variants.append(dj)
    default_json = json.load(open(f"{A}/models/item/djinn_core_default.json"))
    default_json["overrides"] = [{"predicate": {"magi:djinn": i + 1}, "model": f"magi:item/djinn_core_{dj}"}
                                 for i, dj in enumerate(["baal", "leraje", "cerberus", "zagan", "amon", "talmir"])]
    write_json(f"{A}/models/item/djinn_core.json", default_json)

    spal = make_palette(SPECTRAL_PAL, SPECTRAL_ALPHA); save_palette("palette_spectral", spal)
    gm = Model(spal); baal_greatsword(gm)
    gj = gm.json("magi:item/palette_spectral", "minecraft:item/generated")
    del gj["parent"]
    gj["render_type"] = "minecraft:translucent"
    write_json(f"{A}/models/item/spectral_greatsword.json", gj)
    preview["extras"].append(("spectral greatsword", gm))
    ui_textures()

    l1, l2 = armor_layers()
    os.makedirs(f"{A}/textures/models/armor", exist_ok=True)
    l1.save(f"{A}/textures/models/armor/desert_nomad_layer_1.png"); l2.save(f"{A}/textures/models/armor/desert_nomad_layer_2.png")

    # remove obsolete flat sprites
    tdir = f"{A}/textures/item"
    for f in os.listdir(tdir):
        if f.startswith("palette_"): continue
        if f == "djinn_core.png" or f.startswith(("iron_", "steel_", "dungeon_", "desert_nomad_")):
            os.remove(f"{tdir}/{f}")

    sheet(preview["weapons"], WEAPON_GUI, 6, 260, f"{PREV}/weapons_gui.png")
    sheet([x for x in preview["weapons"] if x[0].startswith("steel")], (0, 0, 0), 6, 300, f"{PREV}/weapons_front_steel.png")
    sheet(preview["extras"], PROP_GUI, 5, 260, f"{PREV}/armor_cores_bows.png")
    n = sum(len(m.els) for _, m in preview["weapons"] + preview["extras"])
    print("models written; total cuboids in previewed set:", n)

if __name__ == "__main__":
    main()
