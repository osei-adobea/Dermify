"""
Generates Dermify_Architecture_Diagram.pdf — an actual component architecture
diagram (not a workflow): what the system is made of, and how the pieces
connect. One page, slide-sized (13.333 x 7.5in).

Layout: the on-device app is a bounded region with three internal
components — Capture & Gate feeding the Disease Model ("the brain"),
which feeds Local Storage & Consent. Three external systems the device
depends on (Firebase, Supabase, WhatsApp) sit outside that boundary, each
connected by one labelled line explaining *why* the connection exists.
"""

import math
from reportlab.pdfgen import canvas
from reportlab.lib.units import inch
from reportlab.lib.colors import HexColor

PAGE_W, PAGE_H = 13.333 * inch, 7.5 * inch

INK = HexColor("#152023")
INK_MUTED = HexColor("#5B6E72")
BG = HexColor("#F5FAFB")

TEAL_FILL = HexColor("#E4F4F6")
TEAL_DEPTH = HexColor("#7FCBD3")
TEAL_BORDER = HexColor("#0097A7")
TEAL_TEXT = HexColor("#00707D")

BRAIN_FILL = HexColor("#CDEEF2")
BRAIN_DEPTH = HexColor("#4FB6C4")
BRAIN_BORDER = HexColor("#00707D")

SAND_FILL = HexColor("#FBEEDD")
SAND_DEPTH = HexColor("#E4B876")
SAND_BORDER = HexColor("#C98A3E")
SAND_TEXT = HexColor("#8A5A1E")

ACCENT = HexColor("#0097A7")
DEVICE_BOUNDARY = HexColor("#5B6E72")

c = canvas.Canvas("Dermify_Architecture_Diagram.pdf", pagesize=(PAGE_W, PAGE_H))
c.setFillColor(BG)
c.rect(0, 0, PAGE_W, PAGE_H, fill=1, stroke=0)


# ---------------------------------------------------------------------------
# Primitives
# ---------------------------------------------------------------------------
def raised_rect(x, y, w, h, fill, depth, border, radius=10, depth_px=5, dashed=False):
    c.setFillColor(depth)
    c.roundRect(x + depth_px, y - depth_px, w, h, radius, fill=1, stroke=0)
    c.setFillColor(fill)
    c.setStrokeColor(border)
    c.setLineWidth(1.2)
    if dashed:
        c.setDash(4, 3)
    c.roundRect(x, y, w, h, radius, fill=1, stroke=1)
    c.setDash()


def node_text(cx, top_y, title, subs, title_size=13, sub_size=9.2, title_color=INK, mono=False):
    c.setFillColor(title_color)
    c.setFont("Courier-Bold" if mono else "Helvetica-Bold", title_size - (1.5 if mono else 0))
    ty = top_y
    c.drawCentredString(cx, ty, title)
    c.setFillColor(INK_MUTED)
    c.setFont("Helvetica", sub_size)
    ty -= title_size + 4
    for line in subs:
        c.drawCentredString(cx, ty, line)
        ty -= sub_size + 3


def connector(x1, y1, x2, y2, color=INK_MUTED, width=1.6, dashed=False, head=6, label=None,
              label_size=9, label_color=None, label_bold=False, label_dx=0, label_dy=6):
    c.setStrokeColor(color)
    c.setLineWidth(width)
    if dashed:
        c.setDash(2, 3)
    c.line(x1, y1, x2, y2)
    c.setDash()
    ang = math.atan2(y2 - y1, x2 - x1)
    a1, a2 = ang + math.radians(155), ang - math.radians(155)
    p1 = (x2 + head * math.cos(a1), y2 + head * math.sin(a1))
    p2 = (x2 + head * math.cos(a2), y2 + head * math.sin(a2))
    c.setFillColor(color)
    path = c.beginPath()
    path.moveTo(x2, y2)
    path.lineTo(*p1)
    path.lineTo(*p2)
    path.close()
    c.drawPath(path, fill=1, stroke=0)
    if label:
        mx, my = (x1 + x2) / 2 + label_dx, (y1 + y2) / 2 + label_dy
        c.setFillColor(label_color or INK_MUTED)
        c.setFont("Helvetica-Bold" if label_bold else "Helvetica", label_size)
        c.drawCentredString(mx, my, label)


# ---------------------------------------------------------------------------
# Icons — simple, generic shapes (not brand logos), built from primitives so
# a reader recognises the component's role at a glance, not just its label.
# ---------------------------------------------------------------------------
def icon_brain(cx, cy, w, h, color):
    """One cohesive lobed silhouette (a closed polygon perturbed by a sine
    wave, tapered narrower at the bottom) plus a centre line and two short
    inward fold arcs — reads as a single brain, not two disconnected blobs."""
    rx, ry = w / 2, h / 2
    n = 48
    pts = []
    for i in range(n):
        theta = 2 * math.pi * i / n
        r = 1 + 0.16 * math.sin(3 * theta + 0.4) + 0.06 * math.sin(7 * theta)
        taper = 1.0 if math.sin(theta) >= 0 else 0.72
        pts.append((cx + rx * r * math.cos(theta), cy + ry * r * math.sin(theta) * taper))

    c.setFillColor(color)
    p = c.beginPath()
    p.moveTo(*pts[0])
    for pt in pts[1:]:
        p.lineTo(*pt)
    p.close()
    c.drawPath(p, fill=1, stroke=0)

    c.setStrokeColor(HexColor("#FFFFFF"))
    c.setLineWidth(1.8)
    c.line(cx, cy + ry * 0.62, cx, cy - ry * 0.40)
    c.setLineWidth(1.1)
    for side in (-1, 1):
        fold = c.beginPath()
        fold.moveTo(cx + side * 0.10 * w, cy + ry * 0.32)
        fold.curveTo(cx + side * 0.26 * w, cy + ry * 0.20,
                     cx + side * 0.24 * w, cy - ry * 0.02,
                     cx + side * 0.12 * w, cy - ry * 0.06)
        c.drawPath(fold, fill=0, stroke=1)


def icon_camera(cx, cy, s, color):
    c.setFillColor(color)
    c.roundRect(cx - 0.34 * s, cy - 0.20 * s, 0.68 * s, 0.42 * s, 0.06 * s, fill=1, stroke=0)
    c.roundRect(cx - 0.14 * s, cy + 0.20 * s, 0.20 * s, 0.10 * s, 0.02 * s, fill=1, stroke=0)
    c.setFillColor(BG)
    c.circle(cx, cy, 0.14 * s, fill=1, stroke=0)
    c.setFillColor(color)
    c.circle(cx, cy, 0.08 * s, fill=1, stroke=0)


def icon_drawer(cx, cy, s, color):
    c.setFillColor(color)
    for i, dy in enumerate((0.16, -0.02, -0.20)):
        c.roundRect(cx - 0.32 * s, cy + dy * s, 0.64 * s, 0.15 * s, 0.02 * s, fill=1, stroke=0)
        c.setFillColor(BG)
        c.roundRect(cx - 0.07 * s, cy + dy * s + 0.055 * s, 0.14 * s, 0.03 * s, 0.01 * s, fill=1, stroke=0)
        c.setFillColor(color)


def icon_cloud(cx, cy, s, color):
    c.setFillColor(color)
    for dx, dy, r in [(-0.22, -0.02, 0.16), (0.0, 0.08, 0.20), (0.24, -0.02, 0.17), (0.02, -0.10, 0.20)]:
        c.circle(cx + dx * s, cy + dy * s, r * s, fill=1, stroke=0)
    c.roundRect(cx - 0.36 * s, cy - 0.20 * s, 0.72 * s, 0.14 * s, 0.05 * s, fill=1, stroke=0)


def icon_key(cx, cy, s, color):
    c.setStrokeColor(color)
    c.setLineWidth(0.10 * s)
    c.setFillColor(color)
    c.circle(cx - 0.20 * s, cy, 0.14 * s, fill=0, stroke=1)
    c.line(cx - 0.07 * s, cy, cx + 0.30 * s, cy)
    c.line(cx + 0.20 * s, cy, cx + 0.20 * s, cy - 0.12 * s)
    c.line(cx + 0.30 * s, cy, cx + 0.30 * s, cy - 0.12 * s)


def icon_chat(cx, cy, s, color):
    c.setFillColor(color)
    c.roundRect(cx - 0.34 * s, cy - 0.18 * s, 0.68 * s, 0.42 * s, 0.10 * s, fill=1, stroke=0)
    p = c.beginPath()
    p.moveTo(cx - 0.16 * s, cy - 0.18 * s)
    p.lineTo(cx - 0.24 * s, cy - 0.34 * s)
    p.lineTo(cx - 0.02 * s, cy - 0.18 * s)
    p.close()
    c.drawPath(p, fill=1, stroke=0)
    c.setFillColor(BG)
    for dx in (-0.16, 0.0, 0.16):
        c.circle(cx + dx * s, cy + 0.02 * s, 0.035 * s, fill=1, stroke=0)


def icon_lock(cx, cy, s, color):
    c.setStrokeColor(color)
    c.setLineWidth(0.07 * s)
    c.setFillColor(color)
    c.roundRect(cx - 0.16 * s, cy - 0.14 * s, 0.32 * s, 0.24 * s, 0.03 * s, fill=1, stroke=0)
    p = c.beginPath()
    p.moveTo(cx - 0.10 * s, cy + 0.10 * s)
    p.curveTo(cx - 0.10 * s, cy + 0.26 * s, cx + 0.10 * s, cy + 0.26 * s, cx + 0.10 * s, cy + 0.10 * s)
    c.drawPath(p, fill=0, stroke=1)


def icon_cylinder(cx, cy, s, color):
    c.setFillColor(color)
    c.setStrokeColor(color)
    c.ellipse(cx - 0.18 * s, cy + 0.10 * s, cx + 0.18 * s, cy + 0.18 * s, fill=1, stroke=0)
    c.rect(cx - 0.18 * s, cy - 0.12 * s, 0.36 * s, 0.22 * s, fill=1, stroke=0)
    c.setFillColor(BG)
    p = c.beginPath()
    p.moveTo(cx - 0.18 * s, cy - 0.12 * s)
    p.curveTo(cx - 0.18 * s, cy - 0.22 * s, cx + 0.18 * s, cy - 0.22 * s, cx + 0.18 * s, cy - 0.12 * s)
    c.setFillColor(color)
    c.drawPath(p, fill=1, stroke=0)


# ---------------------------------------------------------------------------
# Title
# ---------------------------------------------------------------------------
c.setFillColor(INK)
c.setFont("Helvetica-Bold", 21)
c.drawCentredString(PAGE_W / 2, PAGE_H - 0.42 * inch, "Dermify — System Architecture")
c.setFillColor(INK_MUTED)
c.setFont("Helvetica", 11.5)
c.drawCentredString(PAGE_W / 2, PAGE_H - 0.66 * inch,
                     "System components and how they connect — the disease model is the on-device core")

MARGIN = 0.45 * inch
TOP = PAGE_H - 0.95 * inch    # top of usable area, below the subtitle
BOTTOM = 0.45 * inch          # bottom margin

# ---------------------------------------------------------------------------
# On-device boundary
# ---------------------------------------------------------------------------
dev_x, dev_y, dev_w = MARGIN, BOTTOM, 6.35 * inch
dev_h = TOP - BOTTOM
c.setStrokeColor(DEVICE_BOUNDARY)
c.setLineWidth(1.4)
c.setDash(5, 4)
c.roundRect(dev_x, dev_y, dev_w, dev_h, 20, fill=0, stroke=1)
c.setDash()
c.setFillColor(DEVICE_BOUNDARY)
c.setFont("Helvetica-Bold", 12)
c.drawString(dev_x + 0.25 * inch, dev_y + dev_h - 0.32 * inch, "ON-DEVICE — DERMIFY ANDROID APP")

cx = dev_x + dev_w / 2

# Capture & Gate — the three actual layers, named, not summarised
cap_w, cap_h = 3.5 * inch, 1.18 * inch
cap_y = dev_y + dev_h - 0.55 * inch - cap_h
raised_rect(cx - cap_w / 2, cap_y, cap_w, cap_h, TEAL_FILL, TEAL_DEPTH, TEAL_BORDER)
icon_camera(cx - cap_w / 2 + 0.40 * inch, cap_y + cap_h - 0.42 * inch, 0.55 * inch, TEAL_TEXT)
node_text(cx + 0.20 * inch, cap_y + cap_h - 0.28 * inch, "Capture & Gate",
          ["1. Colour heuristic (YCbCr)",
           "2. ImageNet object gate",
           "3. Domain-trained skin gate"], sub_size=9.0)

# Disease Model — the brain, the visual centre of the whole diagram
brain_w, brain_h = 3.15 * inch, 2.05 * inch
brain_y = cap_y - 0.5 * inch - brain_h
raised_rect(cx - brain_w / 2, brain_y, brain_w, brain_h, BRAIN_FILL, BRAIN_DEPTH, BRAIN_BORDER, radius=16)
icon_brain(cx, brain_y + brain_h - 0.68 * inch, 1.25 * inch, 0.80 * inch, BRAIN_BORDER)
node_text(cx, brain_y + 0.72 * inch, "Disease Model", ["EfficientNetB0 · dual-output TFLite"],
          title_size=15, sub_size=9.5)
c.setFillColor(TEAL_TEXT)
c.setFont("Helvetica-Oblique", 9.0)
c.drawCentredString(cx, brain_y + 0.30 * inch, "the on-device “brain” — predicts the")
c.drawCentredString(cx, brain_y + 0.16 * inch, "condition and produces the CAM heatmap")

connector(cx, cap_y, cx, brain_y + brain_h + 0.08 * inch, color=INK_MUTED, dashed=True,
          label="verified photo", label_dx=1.0 * inch, label_dy=0)

# Local Storage & Consent — the full pipeline, named, not folded into one
# vague line: what gets stripped, what the hash is, how the queue behaves.
store_w, store_h = 3.5 * inch, 1.35 * inch
store_y = dev_y + 0.30 * inch
raised_rect(cx - store_w / 2, store_y, store_w, store_h, TEAL_FILL, TEAL_DEPTH, TEAL_BORDER)
icon_drawer(cx - store_w / 2 + 0.40 * inch, store_y + store_h - 0.55 * inch, 0.55 * inch, TEAL_TEXT)
node_text(cx + 0.20 * inch, store_y + store_h - 0.28 * inch, "Local Storage & Consent",
          ["Room scan history",
           "de-identify: strip EXIF/GPS,",
           "downscale, reject faces",
           "perceptual hash (DCT · 64-bit)"], sub_size=8.7)

connector(cx, brain_y, cx, store_y + store_h + 0.08 * inch, color=INK_MUTED, dashed=True)
c.setFillColor(INK_MUTED)
c.setFont("Helvetica", 9)
label_cx = cx + 1.85 * inch
label_cy = (brain_y + store_y + store_h + 0.08 * inch) / 2
c.drawCentredString(label_cx, label_cy + 6, "result +")
c.drawCentredString(label_cx, label_cy - 6, "opt-in contribution")

# ---------------------------------------------------------------------------
# External systems
# ---------------------------------------------------------------------------
ext_x = dev_x + dev_w + 1.65 * inch
ext_w = PAGE_W - MARGIN - ext_x
ecx = ext_x + ext_w / 2

fb_h = 0.85 * inch
fb_y = TOP - fb_h
raised_rect(ext_x, fb_y, ext_w, fb_h, SAND_FILL, SAND_DEPTH, SAND_BORDER)
icon_key(ext_x + 0.5 * inch, fb_y + fb_h / 2, 0.65 * inch, SAND_TEXT)
node_text(ecx + 0.22 * inch, fb_y + fb_h - 0.30 * inch, "Firebase",
          ["Auth · 3-tier roles (Clinician / Specialist / Admin)"], sub_size=8.8)

wa_h = 0.85 * inch
wa_y = BOTTOM
raised_rect(ext_x, wa_y, ext_w, wa_h, SAND_FILL, SAND_DEPTH, SAND_BORDER)
icon_chat(ext_x + 0.5 * inch, wa_y + wa_h / 2, 0.65 * inch, SAND_TEXT)
node_text(ecx + 0.22 * inch, wa_y + wa_h - 0.30 * inch, "WhatsApp",
          ["de-identified composite · specialist referral"], sub_size=8.8)

# Supabase — its own bounded region, mirroring the on-device side, showing
# the three real functions rather than one "Storage + Database" summary.
sb_gap = 0.22 * inch
sb_y = wa_y + wa_h + sb_gap
sb_h = fb_y - sb_gap - sb_y
raised_rect(ext_x, sb_y, ext_w, sb_h, SAND_FILL, HexColor("#EFDCC0"), SAND_BORDER, radius=16, depth_px=4)
c.setFillColor(SAND_TEXT)
c.setFont("Helvetica-Bold", 11.5)
c.drawCentredString(ecx, sb_y + sb_h - 0.30 * inch, "SUPABASE — BACKEND")
icon_cloud(ext_x + 0.5 * inch, sb_y + sb_h - 0.30 * inch, 0.5 * inch, SAND_BORDER)

sub_w = ext_w - 0.5 * inch
sub_h = 0.82 * inch
sub_gap = 0.12 * inch
n1_y = sb_y + sb_h - 0.55 * inch - sub_h
n2_y = n1_y - sub_gap - sub_h
n3_y = n2_y - sub_gap - sub_h

raised_rect(ecx - sub_w / 2, n1_y, sub_w, sub_h, HexColor("#FFF8EE"), SAND_DEPTH, SAND_BORDER, radius=9, depth_px=4)
node_text(ecx, n1_y + sub_h - 0.24 * inch, "register_contribution()",
          ["global pHash dedup · Hamming ≤ 5", "rejects photos already collected"],
          title_size=11.5, sub_size=8.6, mono=True)

raised_rect(ecx - sub_w / 2, n2_y, sub_w, sub_h, HexColor("#FFF8EE"), SAND_DEPTH, SAND_BORDER, radius=9, depth_px=4)
node_text(ecx, n2_y + sub_h - 0.24 * inch, "Storage Upload",
          ["plain write · reserved path · no upsert", "saves the file to its assigned slot"],
          title_size=11.5, sub_size=8.6)

raised_rect(ecx - sub_w / 2, n3_y, sub_w, sub_h, HexColor("#FFF8EE"), SAND_DEPTH, SAND_BORDER, radius=9, depth_px=4)
node_text(ecx, n3_y + sub_h - 0.24 * inch, "mark_contribution_stored()",
          ["verifies the object exists", "before trusting the client's word"],
          title_size=11.5, sub_size=8.6, mono=True)

connector(ecx, n1_y, ecx, n2_y + sub_h + 0.06 * inch, color=SAND_BORDER, width=1.3, dashed=True, head=5)
connector(ecx, n2_y, ecx, n3_y + sub_h + 0.06 * inch, color=SAND_BORDER, width=1.3, dashed=True, head=5)

c.setFillColor(SAND_TEXT)
c.setFont("Helvetica-Bold", 8.6)
c.drawCentredString(ecx, sb_y + 0.28 * inch, "RLS: anon role = INSERT + EXECUTE only — no SELECT")

# Connections crossing the on-device boundary — each labelled with *why*,
# not just "connects to".
connector(dev_x + dev_w, fb_y + fb_h / 2, ext_x, fb_y + fb_h / 2,
          color=SAND_BORDER, width=1.8, label="Auth · Tiers", label_dy=13, label_bold=True,
          label_color=SAND_TEXT, label_size=8.5)

connector(dev_x + dev_w, n1_y + sub_h / 2, ext_x, n1_y + sub_h / 2,
          color=ACCENT, width=2.2, label="de-identified upload only", label_dy=13, label_bold=True,
          label_color=TEAL_TEXT, label_size=8.5)

connector(dev_x + dev_w, wa_y + wa_h / 2, ext_x, wa_y + wa_h / 2,
          color=SAND_BORDER, width=1.8, label="referral if confidence is low", label_dy=13,
          label_bold=True, label_color=SAND_TEXT, label_size=8.5)

c.showPage()
c.save()
print("Saved Dermify_Architecture_Diagram.pdf")
