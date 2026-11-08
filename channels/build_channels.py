#!/usr/bin/env python3
"""Build channels.json for Tony from two public playlists, combined and tested.

Sources (owner decision, 8 Nov 2026):
  * Free-TV/IPTV  — curated, officially free channels (listed first, its numbering wins)
  * iptv-org/iptv — much larger, user-submitted public streams

The same channel from both lists becomes ONE entry with several stream URLs: the first
working one is `url`, the other working/untested ones are `backupUrls` so the app can
fail over without the viewer noticing.

Always excluded (family app): adult channels (iptv-org is_nsfw / XXX), paid/premium
channels (see PREMIUM), and nothing is ever sent with spoofed headers or tokens to get
past a server that refuses us.

Usage:
    channels/.venv/bin/python channels/build_channels.py                    # everything
    channels/.venv/bin/python channels/build_channels.py Lebanon Australia  # only these countries (merged into existing file)
    channels/.venv/bin/python channels/build_channels.py --no-test Lebanon  # no stream tests (keeps earlier results)
    ... --free-tv-only                                                      # skip iptv-org
"""

import argparse
import concurrent.futures as cf
import gzip
import http.client
import json
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import zlib
from collections import Counter, defaultdict
from datetime import datetime, timezone
from pathlib import Path

import enrich  # shared iptv-org database helpers (Pillow optional for this part)

FREE_TV_URL = "https://raw.githubusercontent.com/Free-TV/IPTV/master/playlist.m3u8"
IPTV_ORG_URL = "https://iptv-org.github.io/iptv/index.m3u"
HERE = Path(__file__).resolve().parent
OUT = HERE / "channels.json"
UA = "Mozilla/5.0 (Linux; Android 12; Google TV) Tony/1.0"
TIMEOUT = 10

MARK_SD, MARK_GEO, MARK_YT, MARK_T = "Ⓢ", "Ⓖ", "Ⓨ", "Ⓣ"
ATTR_RE = re.compile(r'([\w-]+)="([^"]*)"')
EXTINF_RE = re.compile(r'^#EXTINF:\s*-?\d+((?:\s+[\w-]+="[^"]*")*)\s*,(.*)$')

# Paid / premium subscription channels — never included. Matched on the channel NAME only
# (owner/network matching caught free channels such as Racing.com).
PREMIUM = re.compile(
    r"\b(bein|osn|sky sports?(?! news)|sky cinema|sky atlantic|foxtel|kayo|dazn|hbo|showtime|starz|cinemax|"
    r"canal\+|eurosport|bt sport|tnt sports|fox sports|fox deportes|supersport|ssc sport|setanta|"
    r"premier sports|arena sport|sport tv\s?\d|mbc max|movistar\+)\b", re.I)
# Free ad-supported services whose channels sometimes carry premium brand names ("DAZN x Pluto TV").
FREE_SERVICE = re.compile(r"pluto|samsung tv plus|plex|tubi|rakuten|roku|xumo|stirr|redbox|freevee", re.I)
WEB_HOSTS = ("twitch.tv", "dailymotion.com", "ok.ru", "facebook.com", "vimeo.com")
TRANSIENT = ("URLError", "TimeoutError", "RemoteDisconnected", "ConnectionResetError", "SSLError",
             "IncompleteRead", "HTTP 500", "HTTP 502", "HTTP 503", "HTTP 504")
STATUS_RANK = {"ok": 0, "untested": 1, "web": 2, "geoblocked": 3, "broken": 4}


# ---------------- networking ----------------

def fetch(url, limit=None, headers=None):
    req = urllib.request.Request(url, headers={"User-Agent": UA, **(headers or {})})
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
        return r.geturl(), body, (r.headers.get("Content-Type") or "").lower()


def download(url, tries=3):
    for i in range(tries):
        try:
            return fetch(url)[1].decode("utf-8", "replace")
        except Exception:
            if i == tries - 1:
                raise
            time.sleep(2 * (i + 1))


def sniff(body, ctype):
    """What kind of media is this? hls | dash | ts | audio | web | video | None"""
    head = body.lstrip()[:2000]
    text = head.decode("utf-8", "replace")
    if text.startswith("#EXTM3U"):
        return "hls"
    if "<MPD" in text:
        return "dash"
    if len(body) >= 189 and body[0] == 0x47 and body[188] == 0x47:
        return "ts"
    if ctype.startswith("audio/") or body[:3] == b"ID3" or (len(body) > 1 and body[0] == 0xFF and body[1] & 0xF0 == 0xF0):
        return "audio"
    if "html" in ctype or text.lower().startswith(("<!doctype", "<html")):
        return "web"
    if ctype.startswith("video/"):
        return "video"
    return None


def test_stream(url, headers=None):
    """HTTP check plus a short play test: playlist -> variant -> first segment bytes.
    Returns (status, detail, media) — media is video | audio | web."""
    parsed = urllib.parse.urlparse(url)
    scheme, host = parsed.scheme.lower(), parsed.netloc.lower()
    if scheme not in ("http", "https"):
        return "untested", scheme.upper(), "video"  # RTMP/RTSP/etc.: the app tries it
    if any(host == h or host.endswith("." + h) for h in WEB_HOSTS):
        return "web", host, "web"
    try:
        final, body, ctype = fetch(url, 256 * 1024, headers)
        kind = sniff(body, ctype)
        if kind in ("dash", "ts", "video"):
            return "ok", kind, "video"
        if kind == "audio":
            return "ok", "radio", "audio"
        if kind == "web":
            return "web", "web page, not a stream", "web"
        if kind != "hls":
            return "broken", "empty response" if not body else "unknown format", "video"
        audio_only = (b"RESOLUTION=" not in body and b"avc1" not in body and b"hvc1" not in body
                      and b'CODECS="mp4a' in body)
        media = "audio" if audio_only else "video"
        for _ in range(3):  # master -> media playlist -> segment
            text = body.decode("utf-8", "replace")
            lines = [l.strip() for l in text.splitlines() if l.strip() and not l.startswith("#")]
            if not lines:
                return "broken", "empty playlist", media
            final, body, ctype = fetch(urllib.parse.urljoin(final, lines[0]), 4096, headers)
            if not body:
                return "broken", "empty segment", media
            if sniff(body, ctype) != "hls":
                return "ok", None, media
        return "ok", None, media
    except urllib.error.HTTPError as e:
        return ("geoblocked" if e.code in (401, 403, 451) else "broken"), f"HTTP {e.code}", "video"
    except Exception as e:
        return "broken", type(e).__name__, "video"


# ---------------- parsing ----------------

def parse_m3u(text):
    entries, meta, opts = [], None, {}
    for line in text.splitlines():
        line = line.strip()
        if line.startswith("#EXTINF"):
            m = EXTINF_RE.match(line)  # attributes are quoted and may contain commas
            head, name = (m.group(1), m.group(2)) if m else line.partition(",")[::2]
            meta = dict(ATTR_RE.findall(head))
            meta["display"] = name.strip() or meta.get("tvg-name", "Unknown")
            opts = {}
        elif line.startswith("#EXTVLCOPT:") and meta is not None:
            k, _, v = line[11:].partition("=")
            opts[k] = v
        elif line and not line.startswith("#") and meta is not None:
            meta["url"] = line
            hdrs = {}
            ua = opts.get("http-user-agent") or meta.get("http-user-agent")
            ref = opts.get("http-referrer") or meta.get("http-referrer")
            if ua:
                hdrs["User-Agent"] = ua
            if ref:
                hdrs["Referer"] = ref
            meta["headers"] = hdrs
            entries.append(meta)
            meta = None
    return entries


def slug(s):
    return re.sub(r"[^a-z0-9]+", "-", s.lower()).strip("-")


def is_youtube(url):
    host = urllib.parse.urlparse(url).netloc.lower()
    return "youtube.com" in host or "youtu.be" in host


def free_tv_entries(text):
    out = []
    for e in parse_m3u(text):
        d = e["display"]
        name = re.sub(r"\s*[ⓈⒼⓎⓉ]\s*", " ", d).strip()
        chno = e.get("tvg-chno", "")
        out.append({
            "source": "free-tv", "name": name, "group": e.get("group-title", "Other"),
            "tvgId": e.get("tvg-id") or None, "logo": e.get("tvg-logo") or None, "url": e["url"],
            "hd": MARK_SD not in d, "geoblocked": MARK_GEO in d,
            "youtube": MARK_YT in d or is_youtube(e["url"]), "notAlwaysOn": False,
            "chno": int(chno) if chno.isdigit() else None, "headers": {},
        })
    return out


def iptv_org_entries(text):
    out = []
    for e in parse_m3u(text):
        d = e["display"]
        tags = " ".join(re.findall(r"\[([^\]]*)\]", d))
        res = re.search(r"\((\d{3,4})[pi]\)", d)
        name = re.sub(r"\s*\(\d{3,4}[pi]\)|\s*\[[^\]]*\]", "", d).strip()
        out.append({
            "source": "iptv-org", "name": name, "group": None, "category": e.get("group-title"),
            "tvgId": e.get("tvg-id") or None, "logo": e.get("tvg-logo") or None, "url": e["url"],
            "hd": bool(res and int(res.group(1)) >= 720) or (" HD" in name and not res),
            "geoblocked": "Geo-blocked" in tags, "youtube": is_youtube(e["url"]),
            "notAlwaysOn": "Not 24/7" in tags, "chno": None, "headers": e["headers"],
        })
    return out


# ---------------- main ----------------

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("countries", nargs="*", help="countries to rebuild (default: all)")
    ap.add_argument("--no-test", action="store_true", help="skip stream tests")
    ap.add_argument("--free-tv-only", action="store_true")
    ap.add_argument("--workers", type=int, default=32)
    ap.add_argument("--out", help="write here instead of channels/channels.json (testing)")
    args = ap.parse_args()
    global OUT
    if args.out:
        OUT = Path(args.out).resolve()
    wanted = {c.lower() for c in args.countries}
    now = datetime.now(timezone.utc).isoformat(timespec="seconds")

    print("Downloading playlists and iptv-org database…", file=sys.stderr)
    entries = free_tv_entries(download(FREE_TV_URL))
    n_free = len(entries)
    if not args.free_tv_only:
        entries += iptv_org_entries(download(IPTV_ORG_URL))
    db = enrich.db_csv("channels")
    by_id, by_name = enrich.build_index(db)
    country_names = {c["code"]: c["name"] for c in enrich.db_csv("countries")}
    country_names.update({"GB": "UK", "US": "USA", "AE": "UAE", "KR": "South Korea", "KP": "North Korea"})

    # Free-TV group name -> ISO code (by vote of tvg-id suffixes)
    votes = defaultdict(Counter)
    for e in entries:
        m = re.search(r"\.([a-z]{2})(@|$)", (e["tvgId"] or "").lower())
        if e["group"] and m:
            votes[e["group"]][m.group(1).upper()] += 1
    group_code = {g: v.most_common(1)[0][0] for g, v in votes.items()}

    # ---- resolve each entry to a channel key + country ----
    excluded = Counter()
    channels = {}
    for e in entries:
        tvg = (e["tvgId"] or "").split("@")[0]
        cc = ""
        m = re.search(r"\.([a-z]{2})$", tvg.lower())
        if m:
            cc = m.group(1).upper()
        elif e["group"]:
            cc = group_code.get(e["group"], "")
        db_ch = by_id.get(tvg.lower()) or enrich.match(
            {"tvgId": tvg, "name": e["name"], "country": country_names.get(cc, "")}, cc, by_id, by_name)
        if db_ch:
            cc = db_ch["country"].upper() or cc
        if (db_ch and db_ch["is_nsfw"] == "TRUE") or (e.get("category") or "").upper() == "XXX":
            excluded["adult"] += 1
            continue
        free_service = FREE_SERVICE.search(f'{e["name"]} {e["url"]} {db_ch["network"] if db_ch else ""}')
        if PREMIUM.search(e["name"]) and not free_service:
            excluded["premium"] += 1
            continue

        # Which group does it appear under in Tony?
        g = e["group"] or ""
        if g.lower().startswith("vod"):
            kind, country = "vod", g
        elif cc and cc in country_names:
            kind, country = "country", country_names[cc]
        elif g and not enrich.NON_COUNTRY_GROUPS.match(g):
            kind, country = "country", re.sub(r"^.*?/\s*", "", g)  # "日本 / Japan" -> "Japan"
        elif g:
            kind, country = "theme", g
        else:
            kind, country = "theme", "International"
        if wanted and country.lower() not in wanted:
            continue

        key = (db_ch["id"].lower() if db_ch else f"{cc}:{enrich.norm_loose(e['name']) or slug(e['name'])}", kind)
        ch = channels.get(key)
        if ch is None:
            ch = channels[key] = {
                "id": slug(db_ch["id"] if db_ch else f"{country}-{e['name']}"),
                "name": e["name"], "country": country, "countryCode": cc or None, "groupKind": kind,
                "category": None, "tvgId": e["tvgId"], "iptvOrgId": db_ch["id"] if db_ch else None,
                "logo": e["logo"], "sources": [], "streams": [], "chno": None,
                "hd": False, "geoblocked": False, "youtube": False, "notAlwaysOn": False,
            }
        if e["source"] == "free-tv" and "free-tv" not in ch["sources"]:
            ch["name"] = e["name"]  # Free-TV names are cleaner
            ch["logo"] = e["logo"] or ch["logo"]
        if e["source"] not in ch["sources"]:
            ch["sources"].append(e["source"])
        ch["chno"] = ch["chno"] or e["chno"]
        ch["logo"] = ch["logo"] or e["logo"]
        if not any(s["url"] == e["url"] for s in ch["streams"]):
            ch["streams"].append({k: e[k] for k in ("url", "source", "hd", "geoblocked", "youtube", "notAlwaysOn", "headers")})
        if db_ch and db_ch["categories"]:
            c0 = db_ch["categories"].split(";")[0]
            ch["category"] = enrich.CATEGORY_NAMES.get(c0, c0.title())
        elif not ch["category"] and e.get("category") and e["category"] != "Undefined":
            c0 = e["category"].split(";")[0].lower()
            ch["category"] = enrich.CATEGORY_NAMES.get(c0, c0.title())
    channels = list(channels.values())
    for ch in channels:
        ch["category"] = ch["category"] or "General"

    # ---- test every stream ----
    old = json.loads(OUT.read_text()).get("channels", []) if OUT.exists() else []
    previous = {c["id"]: c for c in old}
    old_results = {s["url"]: s for c in old for s in c.get("streams", []) if s.get("status")}
    streams = [s for ch in channels for s in ch["streams"]]
    if args.no_test:
        for s in streams:
            r = old_results.get(s["url"], {})
            s.update(status=r.get("status", "untested"), detail=r.get("detail"),
                     media=r.get("media", "video"), lastChecked=r.get("lastChecked"))
    else:
        to_test = [s for s in streams if not s["youtube"]]
        print(f"Testing {len(to_test)} streams…", file=sys.stderr)

        def run(batch, workers):
            done = 0
            with cf.ThreadPoolExecutor(workers) as pool:
                for s, (status, detail, media) in zip(batch, pool.map(lambda x: test_stream(x["url"], x["headers"]), batch)):
                    s.update(status=status, detail=detail, media=media, lastChecked=now)
                    done += 1
                    if done % 500 == 0:
                        print(f"  {done}/{len(batch)}", file=sys.stderr)

        run(to_test, args.workers)
        retry = [s for s in to_test if s["status"] != "ok" and s["detail"] in TRANSIENT]
        if retry:
            print(f"Retrying {len(retry)} temporary failures…", file=sys.stderr)
            run(retry, 8)
            print(f"  {sum(s['status'] == 'ok' for s in retry)} recovered", file=sys.stderr)
    for s in streams:
        if s["youtube"]:
            s.update(status="youtube", detail=None, media="youtube", lastChecked=s.get("lastChecked"))

    # ---- pick primary stream + backups per channel ----
    for ch in channels:
        rank = lambda s: (STATUS_RANK.get(s["status"], 5) if s["status"] != "youtube" else 1.5,
                          bool(s["headers"]), s["source"] != "free-tv", not s["hd"])
        ch["streams"].sort(key=rank)
        best = ch["streams"][0]
        ch["url"] = best["url"]
        ch["headers"] = best["headers"]  # the app sends these when playing `url`
        ch["backupUrls"] = [s["url"] for s in ch["streams"][1:] if s["status"] in ("ok", "untested")]
        ch["status"] = best["status"]
        ch["statusDetail"] = best.get("detail")
        ch["media"] = best.get("media", "video")
        ch["lastChecked"] = best.get("lastChecked")
        ch["hd"] = best["hd"]
        ch["geoblocked"] = any(s["geoblocked"] for s in ch["streams"]) or ch["status"] == "geoblocked"
        ch["youtube"] = best["youtube"]
        ch["notAlwaysOn"] = best["notAlwaysOn"]
        if ch["status"] == "broken" and ch["geoblocked"]:
            ch["status"] = "geoblocked"

    # ---- stable numbers per country: previous > Free-TV tvg-chno > next free ----
    kept = [c for c in old if wanted and c["country"].lower() not in wanted]
    used = defaultdict(set)
    for c in kept:
        used[c["country"]].add(c["number"])
    for ch in channels:
        ch["number"] = None
    channels.sort(key=lambda c: ("free-tv" not in c["sources"], c["chno"] or 10**6, c["status"] != "ok", c["name"].lower()))
    for source in ("prev", "chno"):
        for ch in channels:
            n = previous.get(ch["id"], {}).get("number") if source == "prev" else ch["chno"]
            if ch["number"] is None and n is not None and n not in used[ch["country"]]:
                ch["number"] = n
                used[ch["country"]].add(n)
    for ch in channels:
        if ch["number"] is None:
            ch["number"] = next(n for n in range(1, 10**6) if n not in used[ch["country"]])
            used[ch["country"]].add(ch["number"])
        del ch["chno"]

    # unique ids
    seen = Counter()
    for ch in channels:
        seen[ch["id"]] += 1
        if seen[ch["id"]] > 1:
            ch["id"] = f'{ch["id"]}-{seen[ch["id"]]}'

    channels = kept + channels
    channels.sort(key=lambda c: (c["groupKind"] != "country", c["country"], c["number"]))
    enrich.save_channels(OUT, {
        "sources": [FREE_TV_URL] + ([] if args.free_tv_only else [IPTV_ORG_URL]),
        "generated": now,
        "channels": channels,
    })
    print(f"Free-TV entries: {n_free}, iptv-org entries: {len(entries) - n_free}, "
          f"excluded: {dict(excluded)}", file=sys.stderr)

    # ---- logos, badge colours (needs Pillow) ----
    if enrich.Image is not None:
        enrich.main(OUT)
        channels = json.loads(OUT.read_text())["channels"]
    else:
        print("Skipped logos: run with channels/.venv/bin/python", file=sys.stderr)

    write_dist(channels, now)
    report(channels)


# Fields the TV app needs (everything else stays in channels.json for the build).
APP_FIELDS = ("id", "number", "name", "category", "url", "headers", "backupUrls", "status", "media",
              "hd", "geoblocked", "notAlwaysOn", "logo", "logoBadge", "logoBadgeColor", "tileColor",
              "logoWidth", "logoHeight")


def write_dist(channels, now):
    """dist/index.json (country list for the picker) + dist/countries/<slug>.json (one per group).
    The TV downloads the index and only the countries the family picked."""
    if OUT.parent != HERE:
        dist = OUT.parent / "dist"
    else:
        dist = HERE / "dist"
    (dist / "countries").mkdir(parents=True, exist_ok=True)
    flags = {c["code"]: c["flag"] for c in enrich.db_csv("countries")}
    groups = defaultdict(list)
    for ch in channels:  # one group per file name, so two spellings can never overwrite each other
        groups[slug(ch["country"])].append(ch)
    index, written = [], set()
    for key, chs in sorted(groups.items(), key=lambda kv: (Counter(c["groupKind"] for c in kv[1]).most_common(1)[0][0] != "country", kv[0])):
        kind = Counter(c["groupKind"] for c in chs).most_common(1)[0][0]
        name = Counter(c["country"] for c in chs).most_common(1)[0][0]
        code = Counter(c.get("countryCode") for c in chs if c.get("countryCode")).most_common(1)
        code = code[0][0] if code else None
        fname = f"{key}.json"
        written.add(fname)
        chs.sort(key=lambda c: c["number"])
        rows = [{k: c.get(k) for k in APP_FIELDS} for c in chs]
        (dist / "countries" / fname).write_text(json.dumps(
            {"country": name, "code": code, "generated": now, "channels": rows},
            ensure_ascii=False, separators=(",", ":")))
        index.append({"name": name, "code": code, "kind": kind, "flag": flags.get(code or "", ""),
                      "file": f"countries/{fname}", "channels": len(chs),
                      "working": sum(c["status"] == "ok" for c in chs)})
    for f in (dist / "countries").glob("*.json"):
        if f.name not in written:
            f.unlink()
    (dist / "index.json").write_text(json.dumps(
        {"schema": 1, "generated": now, "countries": index}, ensure_ascii=False, indent=1))


def report(channels):
    by = defaultdict(Counter)
    for ch in channels:
        by[ch["country"]][ch["status"]] += 1
    total = Counter(ch["status"] for ch in channels)
    print(f"\nWrote {OUT.name}: {len(channels)} channels in {len(by)} groups — " +
          "  ".join(f"{k}={v}" for k, v in sorted(total.items())))
    for country in ("Lebanon", "Australia"):
        rows = [c for c in channels if c["country"] == country]
        if not rows:
            continue
        ok = sum(c["status"] == "ok" for c in rows)
        print(f"\n{country}: {ok}/{len(rows)} working")
        for c in rows:
            extra = f" ({c['statusDetail']})" if c["statusDetail"] else ""
            src = "+".join("F" if s == "free-tv" else "I" for s in c["sources"])
            print(f"  {c['number']:>3}  {c['name']:<28} {src:<3} {c['status']}{extra}"
                  f"{'  +' + str(len(c['backupUrls'])) + ' backup' if c['backupUrls'] else ''}")


if __name__ == "__main__":
    main()
