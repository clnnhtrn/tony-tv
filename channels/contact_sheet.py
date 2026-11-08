#!/usr/bin/env python3
"""Render logo badges on fallback tiles (v5 design sizes) for visual QA.

    channels/.venv/bin/python channels/contact_sheet.py [channels.json] [out.png] [Country ...]
"""
import json
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

src = Path(sys.argv[1] if len(sys.argv) > 1 else Path(__file__).parent / "channels.json")
out = Path(sys.argv[2] if len(sys.argv) > 2 else "/tmp/sheet.png")
countries = set(sys.argv[3:])
chs = [c for c in json.loads(src.read_text())["channels"] if not countries or c["country"] in countries]

TW, TH, GAP, COLS = 400, 225, 28, 5
AREA, MAX_LW, MAX_LH, PAD_X, PAD_Y = 120 * 36, 150, 52, 12, 10  # px @1080p
rows = (len(chs) + COLS - 1) // COLS
sheet = Image.new("RGB", (COLS * (TW + GAP) + GAP, rows * (TH + 70) + GAP), "#15161B")
draw = ImageDraw.Draw(sheet)
try:
    font = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial.ttf", 22)
    small = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial.ttf", 16)
except OSError:
    font = small = ImageFont.load_default()

for i, c in enumerate(chs):
    x = GAP + (i % COLS) * (TW + GAP)
    y = GAP + (i // COLS) * (TH + 70)
    draw.rounded_rectangle([x, y, x + TW, y + TH], 20, fill=c.get("tileColor") or "#2A2B33")
    if c.get("logo"):
        logo = Image.open(src.parent / c["logo"]).convert("RGBA")
        # Optical sizing: equal visual area for every logo, clamped (spec in CLAUDE.md 7.1).
        aspect = logo.width / logo.height
        w = (AREA * aspect) ** 0.5
        h = w / aspect
        k = min(1, MAX_LW / w, MAX_LH / h)
        scale = (w * k) / logo.width
        logo = logo.resize((max(1, int(logo.width * scale)), max(1, int(logo.height * scale))), Image.LANCZOS)
        bw, bh = logo.width + 2 * PAD_X, logo.height + 2 * PAD_Y
        bx, by = x + 16, y + 16
        draw.rounded_rectangle([bx, by, bx + bw, by + bh], 10, fill=c.get("logoBadgeColor") or "#FFFFFF",
                               outline=None if c.get("logoBadge") == "light" else "#3A3B42", width=1)
        sheet.paste(logo, (bx + PAD_X, by + (bh - logo.height) // 2), logo)
    else:
        draw.text((x + 24, y + TH - 60), c["name"], fill="#F5F5F7", font=font)
    draw.text((x, y + TH + 10), c["name"][:30], fill="#F5F5F7", font=font)
    meta = f'{c["country"][:14]} · {c.get("category","")} · {c.get("logoBadge") or "no logo"}{" · LOW-RES" if c.get("logoLowRes") else ""}'
    draw.text((x, y + TH + 38), meta, fill="#8E8E93", font=small)

out.parent.mkdir(parents=True, exist_ok=True)
sheet.save(out)
print(out, sheet.size)
