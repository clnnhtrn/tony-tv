#!/usr/bin/env python3
"""Add logos, categories and country info to channels.json.

Run after build_channels.py (it calls this automatically when Pillow is installed):
    channels/.venv/bin/python channels/enrich.py

Data source: iptv-org/database (public domain / Unlicense) — channel names, categories
and logo URLs. Logos are the broadcasters' own marks, used only to identify their
channels.

For every channel it:
  1. matches the Free-TV entry to an iptv-org channel (by id, then by name in the same country);
  2. picks the best logo (in use, main feed, SVG > large PNG > others), falls back to tvg-logo;
  3. downloads it and normalises it: trims padding, scales to max 480x240, saves logos/<id>.png;
  4. decides how the app should show it on a tile badge:
        badge = "light"  -> logo on the white badge (default design)
        badge = "dark"   -> logo is mostly white; show it on a dark badge instead
        badge = "fill"   -> logo has its own solid background; badge uses badgeColor
  5. picks a tileColor (dark tone of the logo's brand colour) for the fallback tile
     when no live still is available.
"""

import colorsys
import csv
import io
import json
import re
import sys
import time
import urllib.request
from collections import Counter
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

try:
    from PIL import Image
except ImportError:
    Image = None
try:
    import resvg_py
except ImportError:  # SVGs are skipped without it
    resvg_py = None

HERE = Path(__file__).resolve().parent
CHANNELS = HERE / "channels.json"
LOGOS = HERE / "logos"
CACHE = HERE / ".cache"
DB = "https://raw.githubusercontent.com/iptv-org/database/master/data/"
UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_0) AppleWebKit/605.1.15 (KHTML, like Gecko) Tony/1.0"
MAX_W, MAX_H = 320, 160   # badge max is 150x52 px; fallback tile shows it ~2x
NEUTRAL_TILE = "#24252D"   # design surface colour
DARK_BADGE = "#0B0C10"     # for white logos; app adds a 1px rgba(255,255,255,0.14) hairline

CATEGORY_NAMES = {
    "news": "News", "legislative": "News", "weather": "News", "business": "Business",
    "kids": "Kids", "animation": "Kids", "family": "Family",
    "movies": "Movies", "classic": "Movies", "series": "Series", "comedy": "Comedy",
    "music": "Music", "sports": "Sport", "outdoor": "Sport", "auto": "Sport",
    "religious": "Religion", "documentary": "Documentary", "science": "Documentary",
    "education": "Education", "culture": "Culture", "travel": "Travel",
    "cooking": "Food", "lifestyle": "Lifestyle", "shop": "Shopping",
    "entertainment": "Entertainment", "general": "General", "relax": "Lifestyle",
}
NON_COUNTRY_GROUPS = re.compile(r"^(news|business|weather|music|documentaries|vod)\b", re.I)


# ---------- helpers ----------

def get(url, tries=3):
    for i in range(tries):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": UA})
            with urllib.request.urlopen(req, timeout=20) as r:
                return r.read()
        except urllib.error.HTTPError as e:
            if e.code == 429 and i < tries - 1:
                time.sleep(2 ** (i + 2))
                continue
            raise
        except Exception:
            if i == tries - 1:
                raise
            time.sleep(1)


def save_channels(path, data):
    """One channel per line: small file, readable git diffs."""
    head = {k: v for k, v in data.items() if k != "channels"}
    lines = ",\n".join(json.dumps(c, ensure_ascii=False, separators=(",", ":")) for c in data["channels"])
    path.write_text(json.dumps(head, ensure_ascii=False)[:-1] + ',"channels":[\n' + lines + "\n]}\n")


def get_cached(url):
    import hashlib
    d = CACHE / "img"
    d.mkdir(parents=True, exist_ok=True)
    f = d / hashlib.sha1(url.encode()).hexdigest()
    if f.exists() and time.time() - f.stat().st_mtime < 7 * 86400:
        return f.read_bytes()
    data = get(url)
    f.write_bytes(data)
    return data


def db_csv(name):
    CACHE.mkdir(exist_ok=True)
    path = CACHE / f"{name}.csv"
    if not path.exists() or time.time() - path.stat().st_mtime > 86400:
        path.write_bytes(get(DB + f"{name}.csv"))
    with path.open(newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))


def norm(s):
    return re.sub(r"[^a-z0-9]+", "", s.lower())


def norm_loose(s):
    s = re.sub(r"\b(tv|hd|channel|television|live|the)\b", " ", s.lower())
    return norm(s)


def luminance(r, g, b):
    def ch(c):
        c /= 255
        return c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4
    return 0.2126 * ch(r) + 0.7152 * ch(g) + 0.0722 * ch(b)


def contrast(l1, l2):
    hi, lo = max(l1, l2), min(l1, l2)
    return (hi + 0.05) / (lo + 0.05)


def hexc(rgb):
    return "#%02X%02X%02X" % tuple(int(c) for c in rgb[:3])


# ---------- matching ----------

def build_index(db_channels):
    by_id = {c["id"].lower(): c for c in db_channels}
    by_name = {}
    for c in db_channels:
        cc = c["country"].upper()
        names = [c["name"]] + [n for n in c["alt_names"].split(";") if n]
        for n in names:
            for key in (norm(n), norm_loose(n)):
                if key:
                    by_name.setdefault((cc, key), c)
    return by_id, by_name


def group_country_codes(channels):
    """Free-TV groups are names ("Lebanon"); work out the ISO code from tvg-id suffixes."""
    votes = {}
    for ch in channels:
        cc = (ch.get("countryCode") or "").upper()
        if not cc:
            m = re.search(r"\.([a-z]{2})(@|$)", (ch.get("tvgId") or "").lower())
            cc = m.group(1).upper() if m else ""
        if cc:
            votes.setdefault(ch["country"], Counter())[cc] += 1
    return {g: v.most_common(1)[0][0] for g, v in votes.items()}


def match(ch, cc, by_id, by_name):
    raw_id = (ch.get("tvgId") or "").split("@")[0].lower()
    if raw_id and raw_id in by_id:
        return by_id[raw_id]
    for key in (norm(ch["name"]), norm_loose(ch["name"])):
        hit = by_name.get((cc, key))
        if hit:
            return hit
    # name + country suffix, e.g. "MTV" -> "MTV Lebanon"
    if cc and ch["country"]:
        hit = by_name.get((cc, norm(ch["name"] + ch["country"])))
        if hit:
            return hit
    return None


def logo_candidates(db_id, logos_by_channel, fallback):
    cands = []
    for l in logos_by_channel.get(db_id, []):
        fmt = l["format"].upper()
        w = int(l["width"] or 0)
        h = int(l["height"] or 0)
        score = (
            l["in_use"] == "TRUE",
            l["feed"] == "",
            fmt == "SVG" and resvg_py is not None,
            fmt in ("PNG", "WEBP", "SVG"),          # transparency-capable
            min(w * h, 400 * 200),
        )
        cands.append((score, l["url"], fmt))
    cands.sort(reverse=True)
    urls = [(u, f) for _, u, f in cands]
    if fallback:
        urls.append((fallback, "SVG" if fallback.lower().endswith(".svg") else ""))
    return urls


# ---------- image processing ----------

MAX_DOWNLOAD = 5 * 1024 * 1024  # logos are untrusted input: cap size and pixel count


def load_image(data, fmt):
    if len(data) > MAX_DOWNLOAD:
        raise ValueError("logo file too large")
    Image.MAX_IMAGE_PIXELS = 4096 * 4096  # refuse decompression bombs
    if fmt == "SVG" or data[:5] in (b"<?xml", b"<svg ") or b"<svg" in data[:300]:
        if resvg_py is None:
            raise ValueError("svg without renderer")
        data = bytes(resvg_py.svg_to_bytes(svg_string=data.decode("utf-8", "replace"), width=MAX_W * 2))
    img = Image.open(io.BytesIO(data))
    img.seek(0)
    return img.convert("RGBA")


def process(img):
    """Return (image, badge, badgeColor, tileColor, lowres)."""
    w, h = img.size
    px = img.load()
    alpha = img.getchannel("A")
    transparent = sum(1 for a in alpha.getdata() if a < 20) / (w * h)
    badge, badge_color, solid_bg = "light", None, False

    if transparent > 0.03:
        bbox = alpha.point(lambda a: 255 if a >= 20 else 0).getbbox()
        if bbox:
            img = img.crop(bbox)
    else:
        # Opaque image: is the border one solid colour? Then that's the logo's background.
        edge = [px[x, 0] for x in range(w)] + [px[x, h - 1] for x in range(w)] + \
               [px[0, y] for y in range(h)] + [px[w - 1, y] for y in range(h)]
        bg = Counter((p[0] // 8, p[1] // 8, p[2] // 8) for p in edge).most_common(1)[0][0]
        bg_rgb = tuple(c * 8 + 4 for c in bg)
        close = lambda p: all(abs(p[i] - bg_rgb[i]) < 24 for i in range(3))
        if sum(close(p) for p in edge) / len(edge) > 0.85:
            mask = Image.new("L", img.size)
            mask.putdata([0 if close(p) else 255 for p in img.getdata()])
            bbox = mask.getbbox()
            if bbox:
                img = img.crop(bbox)
            if luminance(*bg_rgb) > 0.85:
                badge = "light"  # white background blends into the white badge
                solid_bg = True
            else:
                badge, badge_color = "fill", hexc(bg_rgb)
        else:
            badge, badge_color = "fill", hexc(bg_rgb)  # photo-like square logo, show edge to edge

    lowres = max(img.size) < 120
    img.thumbnail((MAX_W, MAX_H), Image.LANCZOS)

    # Colour stats over visible pixels
    small = img.copy()
    small.thumbnail((96, 96))
    vis = [p for p in small.getdata() if p[3] > 128]
    if not vis:
        raise ValueError("empty logo")

    if badge == "light" and solid_bg:
        badge_color = "#FFFFFF"
    elif badge == "light":
        white = luminance(255, 255, 255)
        weak = sum(1 for p in vis if contrast(luminance(*p[:3]), white) < 1.6) / len(vis)
        if weak > 0.55:
            badge, badge_color = "dark", DARK_BADGE
        else:
            badge_color = "#FFFFFF"

    # Brand colour: most common saturated hue among visible pixels
    hues = Counter()
    for r, g, b, _ in vis:
        hh, ll, ss = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
        if ss > 0.35 and 0.15 < ll < 0.85:
            hues[int(hh * 24)] += 1
    tile = NEUTRAL_TILE
    if hues and sum(hues.values()) > len(vis) * 0.08:
        hue = (hues.most_common(1)[0][0] + 0.5) / 24
        if not 40 <= hue * 360 <= 100:  # dark yellows/olives read as mud; keep those neutral
            r, g, b = colorsys.hls_to_rgb(hue, 0.22, 0.50)
            tile = hexc((r * 255, g * 255, b * 255))
    # A filled badge must stand out from its tile.
    if badge_color and badge != "light":
        bl = luminance(*[int(badge_color[i:i + 2], 16) for i in (1, 3, 5)])
        tl = luminance(*[int(tile[i:i + 2], 16) for i in (1, 3, 5)])
        if contrast(bl, tl) < 1.5:
            tile = NEUTRAL_TILE if contrast(bl, luminance(0x24, 0x25, 0x2D)) >= 1.5 else "#3A3B45"
    return img, badge, badge_color, tile, lowres


PROC_VERSION = "1"  # bump when process() changes, to invalidate the processed-logo cache


def process_cached(url, fmt):
    """Processed logo for a source URL, cached on disk: (webp_path, meta). Reruns skip re-encoding."""
    import hashlib
    d = CACHE / "proc"
    d.mkdir(parents=True, exist_ok=True)
    key = hashlib.sha1(f"{PROC_VERSION}|{url}".encode()).hexdigest()
    webp, meta_f = d / f"{key}.webp", d / f"{key}.json"
    if webp.exists() and meta_f.exists():
        return webp, json.loads(meta_f.read_text())
    img, badge, badge_color, tile, lowres = process(load_image(get_cached(url), fmt))
    img.save(webp, "WEBP", quality=90, method=4)  # alpha kept; ~10x smaller than PNG
    meta = {"badge": badge, "badgeColor": badge_color, "tile": tile, "lowres": lowres,
            "w": img.width, "h": img.height}
    meta_f.write_text(json.dumps(meta))
    return webp, meta


def fetch_logo(ch, cands):
    """Try candidates best-first; prefer the first that isn't low-res, else the first that worked.
    Returns (webp_path, meta, url, errors)."""
    errors, fallback = [], None
    for url, fmt in cands:
        try:
            webp, meta = process_cached(url, fmt)
        except Exception as e:
            errors.append(f"{type(e).__name__} {url}")
            continue
        if not meta["lowres"]:
            return webp, meta, url, errors
        fallback = fallback or (webp, meta, url)
    return (*(fallback or (None, None, None)), errors)


# ---------- main ----------

def main(path=None):
    global CHANNELS, LOGOS
    if path:
        CHANNELS = Path(path).resolve()
        LOGOS = CHANNELS.parent / "logos"
    data = json.loads(CHANNELS.read_text())
    channels = data["channels"]
    print("Loading iptv-org database…", file=sys.stderr)
    db_channels = db_csv("channels")
    by_id, by_name = build_index(db_channels)
    logos_by_channel = {}
    for l in db_csv("logos"):
        logos_by_channel.setdefault(l["channel"], []).append(l)

    if any("tvgId" not in c for c in channels):
        text = get("https://raw.githubusercontent.com/Free-TV/IPTV/master/playlist.m3u8").decode("utf-8", "replace")
        ids, last = {}, None
        for line in text.splitlines():
            if line.startswith("#EXTINF"):
                m = re.search(r'tvg-id="([^"]*)"', line)
                last = m.group(1) if m else ""
            elif line.strip() and not line.startswith("#"):
                ids[line.strip()] = last
        for c in channels:
            c.setdefault("tvgId", ids.get(c["url"], "") or None)

    codes = group_country_codes(channels)
    LOGOS.mkdir(exist_ok=True)

    jobs = []
    for ch in channels:
        group = ch["country"]
        cc = ch.get("countryCode") or codes.get(group, "")
        if "groupKind" not in ch:
            clean = re.sub(r"^.*?/\s*", "", group) if "/" in group else group  # "日本 / Japan" -> "Japan"
            ch["countryName"] = clean
            ch["countryCode"] = cc or None
            ch["groupKind"] = ("vod" if group.lower().startswith("vod") else
                               "theme" if NON_COUNTRY_GROUPS.match(group) else "country")
        m = by_id.get((ch.get("iptvOrgId") or "").lower()) or match(ch, cc, by_id, by_name)
        ch["iptvOrgId"] = m["id"] if m else None
        if m and m["categories"]:
            ch["category"] = CATEGORY_NAMES.get(m["categories"].split(";")[0], m["categories"].split(";")[0].title())
        if not ch.get("logoSource"):
            ch["logoSource"] = ch.get("logo")  # original tvg-logo URL
        jobs.append((ch, logo_candidates(m["id"] if m else None, logos_by_channel, ch["logoSource"])))

    print(f"Fetching logos for {len(jobs)} channels…", file=sys.stderr)
    stats = Counter()

    def work(job):
        ch, cands = job
        return ch, fetch_logo(ch, cands) if cands else (None, None, None, [])

    import shutil
    with ThreadPoolExecutor(12) as pool:
        for ch, (webp, meta, url, errors) in pool.map(work, jobs):
            if webp is None:
                ch.update(logo=None, logoFile=None, logoBadge=None, logoBadgeColor=None,
                          tileColor=NEUTRAL_TILE, logoLowRes=None, logoFrom=None)
                stats["no logo"] += 1
                continue
            path = LOGOS / f"{ch['id']}.webp"
            shutil.copyfile(webp, path)
            badge = meta["badge"]
            ch.update(logo=f"logos/{path.name}", logoFile=path.name, logoBadge=badge,
                      logoBadgeColor=meta["badgeColor"], tileColor=meta["tile"], logoLowRes=meta["lowres"],
                      logoFrom=url, logoWidth=meta["w"], logoHeight=meta["h"])
            stats[f"badge {badge}"] += 1
            stats["low-res"] += bool(meta["lowres"])

    save_channels(CHANNELS, data)
    referenced = {c.get("logoFile") for c in channels}
    for f in [*LOGOS.glob("*.png"), *LOGOS.glob("*.webp")]:  # remove logos of channels that no longer exist
        if f.name not in referenced:
            f.unlink()
    found = len(channels) - stats["no logo"]
    print(f"\nLogos: {found}/{len(channels)} found  " +
          "  ".join(f"{k}={v}" for k, v in sorted(stats.items())))
    for country in ("Lebanon", "Australia"):
        rows = [c for c in channels if c["country"] == country]
        if rows:
            print(f"\n{country}:")
            for c in rows:
                print(f"  {c['name']:<24} {c['category']:<13} {c['logoBadge'] or 'NO LOGO':<6} "
                      f"{(c['logoBadgeColor'] or ''):<8} {'low-res' if c.get('logoLowRes') else ''}")


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else None)
