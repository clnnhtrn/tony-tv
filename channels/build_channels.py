#!/usr/bin/env python3
"""Build channels.json for Tony from the Free-TV/IPTV playlist.

Usage:
    python3 build_channels.py                      # all countries
    python3 build_channels.py Lebanon Australia    # only these groups
    python3 build_channels.py --no-test Lebanon    # skip stream tests

Only standard-library Python is used. Streams are tested from wherever this runs
(the owner is in Australia), so geo-blocked channels from other countries will
show as "geoblocked" rather than "ok".
"""

import argparse
import concurrent.futures as cf
import gzip
import http.client
import json
import zlib
import re
import sys
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

PLAYLIST_URL = "https://raw.githubusercontent.com/Free-TV/IPTV/master/playlist.m3u8"
HERE = Path(__file__).resolve().parent
OUT = HERE / "channels.json"
UA = "Mozilla/5.0 (Linux; Android 12; Google TV) Tony/1.0"
TIMEOUT = 10

MARK_SD, MARK_GEO, MARK_YT, MARK_T = "Ⓢ", "Ⓖ", "Ⓨ", "Ⓣ"
ATTR_RE = re.compile(r'([\w-]+)="([^"]*)"')

# Rough category guess from the channel name (Free-TV has no category field).
CATEGORIES = [
    ("Kids", r"\b(kids?|junior|cartoon|me|pakapaka|baby|toon)\b"),
    ("News", r"\b(news|noticias|info|24|cnn|bbc world|al jazeera|al arabiya|mayadeen|euronews|parliament)\b"),
    ("Sport", r"\b(sports?|racing|rush|football|golf)\b"),
    ("Music", r"\b(music|mtv hits|hits|radio|fm)\b"),
    ("Movies", r"\b(movies?|cinema|film|flix)\b"),
    ("Food", r"\b(food|cook|kitchen)\b"),
    ("Shopping", r"\b(tvsn|shop|shopping)\b"),
    ("Religion", r"\b(nour|noursat|sat7|church|quran|iman|aliman|god|gospel|catholic|islam)\b"),
]


def fetch(url, limit=None):
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=TIMEOUT) as r:
        try:
            body = r.read(limit) if limit else r.read()
        except http.client.IncompleteRead as e:  # server cut off mid-body; partial data is fine
            body = e.partial
        if body[:2] == b"\x1f\x8b":  # some servers gzip playlists unasked
            try:
                body = gzip.decompress(body)
            except (OSError, EOFError):
                body = zlib.decompressobj(16 + zlib.MAX_WBITS).decompress(body)
        return r.geturl(), body


def parse_playlist(text):
    channels, meta = [], None
    for line in text.splitlines():
        line = line.strip()
        if line.startswith("#EXTINF"):
            head, _, name = line.partition(",")
            meta = dict(ATTR_RE.findall(head))
            meta["display"] = name.strip()
        elif line and not line.startswith("#") and meta is not None:
            meta["url"] = line
            channels.append(meta)
            meta = None
    return channels


def slug(s):
    return re.sub(r"[^a-z0-9]+", "-", s.lower()).strip("-")


def guess_category(name):
    n = name.lower()
    for cat, pattern in CATEGORIES:
        if re.search(pattern, n):
            return cat
    return "General"


def is_youtube(url):
    host = urllib.parse.urlparse(url).netloc.lower()
    return "youtube.com" in host or "youtu.be" in host


def test_stream(url):
    """HTTP check plus a short play test: playlist -> variant -> first segment."""
    try:
        final, body = fetch(url, 256 * 1024)
        text = body.decode("utf-8", "replace")
        if text.lstrip().startswith("<?xml") or "<MPD" in text[:2000]:
            return "ok", None  # DASH manifest reachable
        if not text.lstrip().startswith("#EXTM3U"):
            return "broken", "not an HLS playlist"
        for _ in range(3):  # follow master -> media playlist -> segment
            lines = [l.strip() for l in text.splitlines() if l.strip() and not l.startswith("#")]
            if not lines:
                return "broken", "empty playlist"
            nxt = urllib.parse.urljoin(final, lines[0])
            final, body = fetch(nxt, 4096)
            if not body:
                return "broken", "empty response"
            text = body.decode("utf-8", "replace")
            if not text.lstrip().startswith("#EXTM3U"):
                return "ok", None  # got media bytes
        return "ok", None
    except urllib.error.HTTPError as e:
        return ("geoblocked" if e.code in (401, 403, 451) else "broken"), f"HTTP {e.code}"
    except Exception as e:  # timeouts, DNS, TLS, resets
        return "broken", type(e).__name__


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("countries", nargs="*", help="group-title values to keep (default: all)")
    ap.add_argument("--no-test", action="store_true", help="skip stream tests")
    ap.add_argument("--workers", type=int, default=24)
    args = ap.parse_args()

    print("Downloading playlist…", file=sys.stderr)
    raw = parse_playlist(fetch(PLAYLIST_URL)[1].decode("utf-8", "replace"))
    wanted = {c.lower() for c in args.countries}
    raw = [c for c in raw if not wanted or c.get("group-title", "").lower() in wanted]
    if wanted:
        missing = wanted - {c.get("group-title", "").lower() for c in raw}
        if missing:
            print(f"Warning: no channels for {', '.join(sorted(missing))}", file=sys.stderr)

    # Keep previous numbers so channel numbers stay stable between runs.
    previous = {}
    if OUT.exists():
        for c in json.loads(OUT.read_text()).get("channels", []):
            previous[c["id"]] = c["number"]

    channels, used, seen = [], {}, set()
    for c in raw:
        country = c.get("group-title", "Other")
        display = c["display"]
        marks = {m for m in (MARK_SD, MARK_GEO, MARK_YT, MARK_T) if m in display}
        name = re.sub(r"\s*[ⓈⒼⓎⓉ]\s*", " ", display).strip()
        cid = slug(c.get("tvg-id") or f"{country}-{name}")
        if cid in seen:
            cid = f"{cid}-{len(seen)}"
        seen.add(cid)
        chno = c.get("tvg-chno", "")
        channels.append({
            "id": cid,
            "number": previous.get(cid) or (int(chno) if chno.isdigit() else None),
            "name": name,
            "country": country,
            "countryCode": c.get("tvg-country") or None,
            "category": guess_category(name),
            "logo": c.get("tvg-logo") or None,
            "url": c["url"],
            "hd": MARK_SD not in marks,
            "geoblocked": MARK_GEO in marks,
            "youtube": MARK_YT in marks or is_youtube(c["url"]),
            "status": "untested",
            "statusDetail": None,
            "lastChecked": None,
        })

    # Fill in missing / clashing numbers per country.
    for ch in channels:
        nums = used.setdefault(ch["country"], set())
        if ch["number"] in nums:
            ch["number"] = None
        if ch["number"] is not None:
            nums.add(ch["number"])
    for ch in channels:
        if ch["number"] is None:
            nums = used[ch["country"]]
            ch["number"] = next(n for n in range(1, 10000) if n not in nums)
            nums.add(ch["number"])

    if not args.no_test:
        to_test = [ch for ch in channels if not ch["youtube"]]
        print(f"Testing {len(to_test)} streams…", file=sys.stderr)
        now = datetime.now(timezone.utc).isoformat(timespec="seconds")
        with cf.ThreadPoolExecutor(args.workers) as pool:
            for ch, (status, detail) in zip(to_test, pool.map(lambda c: test_stream(c["url"]), to_test)):
                if status == "broken" and ch["geoblocked"]:
                    status = "geoblocked"
                ch.update(status=status, statusDetail=detail, lastChecked=now)
        for ch in channels:
            if ch["youtube"]:
                ch["status"] = "youtube"  # played via official embed / YouTube app; not tested here

    channels.sort(key=lambda c: (c["country"], c["number"]))
    OUT.write_text(json.dumps({
        "source": PLAYLIST_URL,
        "generated": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "channels": channels,
    }, ensure_ascii=False, indent=2) + "\n")

    # Report
    by_country = {}
    for ch in channels:
        by_country.setdefault(ch["country"], {}).setdefault(ch["status"], 0)
        by_country[ch["country"]][ch["status"]] += 1
    focus = [c for c in ("Lebanon", "Australia") if c in by_country]
    others = sorted(set(by_country) - set(focus))
    print(f"\nWrote {OUT.name}: {len(channels)} channels in {len(by_country)} countries\n")
    for country in focus + (others if len(others) <= 15 else []):
        s = by_country[country]
        print(f"  {country:<22} " + "  ".join(f"{k}={v}" for k, v in sorted(s.items())))
    if len(others) > 15:
        totals = {}
        for country in others:
            for k, v in by_country[country].items():
                totals[k] = totals.get(k, 0) + v
        print(f"  {len(others)} other countries   " + "  ".join(f"{k}={v}" for k, v in sorted(totals.items())))
    for country in focus:
        print(f"\n{country}:")
        for ch in (c for c in channels if c["country"] == country):
            extra = f" ({ch['statusDetail']})" if ch["statusDetail"] else ""
            print(f"  {ch['number']:>3}  {ch['name']:<26} {ch['status']}{extra}")


if __name__ == "__main__":
    main()
