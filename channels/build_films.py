#!/usr/bin/env python3
"""Build Tony's on-demand "Classic films" catalogue from the Internet Archive (official APIs).

    python3 channels/build_films.py            # default: best 3000 candidates by popularity
    python3 channels/build_films.py --max 6000

Legal filter (CLAUDE.md section 3): keep an item only if the Archive marks it public domain /
Creative Commons, or it was released in 1963 or earlier (US public-domain cut-off for films whose
copyright wasn't renewed; the Archive's film collections are built on that basis).
Family filter: drop adult/sexual titles and subjects.
Playable filter: the item must have an H.264 MP4 file (ExoPlayer plays it directly).

Output: channels/dist/films/index.json (rows) + channels/dist/films/<row>.json
"""
import argparse
import concurrent.futures as cf
import json
import re
import sys
import time
import urllib.parse
import urllib.request
from collections import Counter, defaultdict
from datetime import datetime, timezone
from pathlib import Path

HERE = Path(__file__).resolve().parent
CACHE = HERE / ".cache" / "ia"
OUT = HERE / "dist" / "films"
UA = "Tony/1.0 (family TV app; github.com/clnnhtrn/tony-tv)"
SEARCH = "https://archive.org/advancedsearch.php"
META = "https://archive.org/metadata/"
COLLECTIONS = ["feature_films", "Film_Noir", "SciFi_Horror", "silent_films", "classic_cartoons"]

ADULT = re.compile(r"\b(sex\w*|erotic\w*|nud(e|ity)|porn\w*|xxx|adult film|stag film|burlesque|strip\w*|"
                   r"nudist|naked|exploitation|sexploitation|playboy|lingerie|bikini|hooker|prostitut\w*)\b", re.I)
# Not adult, but not for a relaxed family "Popular" row: graphic war/atrocity footage, propaganda, newsreels.
NOT_POPULAR = re.compile(r"\b(concentration camp|holocaust|nazi|propaganda|newsreel|atrocit\w*|execution|documentary)\b", re.I)
ROWS = [  # (key, title, test(subjects+title lowercase, collections))
    ("comedy",   "Comedy",            lambda t, c: re.search(r"\bcomed|\bslapstick|\bchaplin|\bkeaton|laurel|three stooges|marx brothers", t)),
    ("drama",    "Drama",             lambda t, c: re.search(r"\bdrama|\bromance|\bmelodrama", t)),
    ("noir",     "Film noir & crime", lambda t, c: "film_noir" in c or re.search(r"\bnoir|\bcrime|\bgangster|\bdetective|\bmystery|\bthriller", t)),
    ("horror",   "Horror & sci-fi",   lambda t, c: "scifi_horror" in c or re.search(r"\bhorror|\bsci-?fi|science fiction|\bmonster|\bzombie|\bvampire", t)),
    ("western",  "Westerns",          lambda t, c: re.search(r"\bwestern|\bcowboy", t)),
    ("war",      "War & adventure",   lambda t, c: re.search(r"\bwar\b|\bworld war|\badventure|\baction", t)),
    ("silent",   "Silent films",      lambda t, c: "silent_films" in c or re.search(r"\bsilent", t)),
    ("cartoons", "Classic cartoons",  lambda t, c: "classic_cartoons" in c or re.search(r"\bcartoon|\banimation|\banimated", t)),
]


def get_json(url, cache_name=None, tries=4):
    if cache_name:
        f = CACHE / cache_name
        if f.exists() and time.time() - f.stat().st_mtime < 14 * 86400:
            return json.loads(f.read_text())
    for i in range(tries):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": UA})
            with urllib.request.urlopen(req, timeout=30) as r:
                data = json.loads(r.read())
            if cache_name:
                CACHE.mkdir(parents=True, exist_ok=True)
                (CACHE / cache_name).write_text(json.dumps(data))
            return data
        except Exception:
            if i == tries - 1:
                raise
            time.sleep(2 * (i + 1))


def search(max_items):
    q = "(" + " OR ".join(f"collection:{c}" for c in COLLECTIONS) + ") AND mediatype:movies"
    fields = ["identifier", "title", "year", "downloads", "licenseurl", "subject", "collection", "description", "language"]
    docs, page = [], 1
    while len(docs) < max_items:
        params = [("q", q), ("rows", "500"), ("page", str(page)), ("output", "json"), ("sort[]", "downloads desc")]
        params += [("fl[]", f) for f in fields]
        data = get_json(SEARCH + "?" + urllib.parse.urlencode(params), f"search-{max_items}-{page}.json")
        batch = data["response"]["docs"]
        if not batch:
            break
        docs += batch
        page += 1
    return docs[:max_items]


def as_list(v):
    return v if isinstance(v, list) else ([v] if v else [])


def first(v):
    """Archive fields are sometimes lists (e.g. two titles); use the first value as text."""
    v = as_list(v)
    return str(v[0]) if v else ""


def legal_and_family(d):
    lic = (d.get("licenseurl") or "").lower()
    year = int(str(d.get("year") or "0")[:4] or 0) if str(d.get("year") or "")[:4].isdigit() else 0
    ok_rights = ("publicdomain" in lic or "creativecommons" in lic) or (0 < year <= 1963)
    text = " ".join([first(d.get("title"))] + [str(x) for x in as_list(d.get("subject"))])
    return ok_rights and not ADULT.search(text), year


def best_mp4(ident):
    """Pick a playable H.264 MP4 from the item's files; returns (url, seconds) or None."""
    meta = get_json(META + ident, f"meta-{ident}.json")
    files = meta.get("files", [])
    mp4s = [f for f in files if f.get("name", "").lower().endswith(".mp4")]
    if not mp4s:
        return None
    def score(f):
        fmt = (f.get("format") or "").lower()
        return ("h.264" in fmt and "ia" not in fmt, "h.264" in fmt, int(f.get("size") or 0) if int(f.get("size") or 0) < 2_500_000_000 else 0)
    f = max(mp4s, key=score)
    secs = 0
    try:
        L = str(f.get("length") or "0")
        secs = int(float(L)) if ":" not in L else sum(int(float(x)) * 60 ** i for i, x in enumerate(reversed(L.split(":"))))
    except ValueError:
        pass
    server_name = urllib.parse.quote(f["name"])
    return f"https://archive.org/download/{urllib.parse.quote(ident)}/{server_name}", secs


def title_of(d):
    return first(d.get("title"))


def clean_desc(s):
    s = " ".join(as_list(s)) if isinstance(s, list) else (s or "")
    s = re.sub(r"<[^>]+>", " ", s)
    s = re.sub(r"\s+", " ", s).strip()
    if len(s) > 220:
        s = s[:217].rsplit(" ", 1)[0] + "…"
    return s


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--max", type=int, default=3000, help="candidates to consider (most popular first)")
    args = ap.parse_args()
    now = datetime.now(timezone.utc).isoformat(timespec="seconds")

    print("Searching the Internet Archive…", file=sys.stderr)
    docs = search(args.max)
    seen_titles, cands, dropped = set(), [], Counter()
    for d in docs:
        ok, year = legal_and_family(d)
        if not ok:
            dropped["rights or adult"] += 1
            continue
        title = re.sub(r"\s+", " ", first(d.get("title")).strip())
        d["title"] = title
        key = (re.sub(r"[^a-z0-9]", "", title.lower()), year)
        if not title or key in seen_titles:  # same film uploaded several times: keep the most popular
            dropped["duplicate"] += 1
            continue
        seen_titles.add(key)
        d["_year"] = year
        cands.append(d)
    print(f"{len(cands)} candidates after rights/adult/duplicate filters; checking files…", file=sys.stderr)

    films = []
    def work(d):
        try:
            return d, best_mp4(d["identifier"])
        except Exception:
            return d, None
    with cf.ThreadPoolExecutor(6) as pool:  # gentle on the Archive
        for i, (d, res) in enumerate(pool.map(work, cands), 1):
            if i % 250 == 0:
                print(f"  {i}/{len(cands)}", file=sys.stderr)
            if not res:
                dropped["no playable MP4"] += 1
                continue
            url, secs = res
            colls = [c.lower() for c in as_list(d.get("collection"))]
            short = "classic_cartoons" in colls
            if not short and secs and secs < 40 * 60:
                dropped["shorter than 40 min"] += 1
                continue
            films.append({
                "id": "ia-" + re.sub(r"[^a-z0-9]+", "-", d["identifier"].lower()).strip("-"),
                "title": d["title"].strip(),
                "year": d["_year"] or None,
                "minutes": round(secs / 60) if secs else None,
                "description": clean_desc(d.get("description")),
                "poster": f"https://archive.org/services/img/{urllib.parse.quote(d['identifier'])}",
                "url": url,
                "source": "Internet Archive",
                "page": f"https://archive.org/details/{d['identifier']}",
                "popularity": int(d.get("downloads") or 0),
                "_text": (title_of(d) + " " + " ".join(str(x) for x in as_list(d.get("subject")))).lower(),
                "_colls": colls,
            })

    OUT.mkdir(parents=True, exist_ok=True)
    rows = defaultdict(list)
    for f in films:
        for key, title, test in ROWS:
            if test(f["_text"], f["_colls"]):
                rows[key].append(f)
    index_rows = [{"key": "popular", "title": "Popular classics", "file": "popular.json", "films": min(len(films), 60)}]
    write = lambda name, title, items: (OUT / name).write_text(json.dumps(
        {"row": title, "generated": now, "films": [{k: v for k, v in x.items() if not k.startswith("_")} for x in items]},
        ensure_ascii=False, separators=(",", ":")))
    films.sort(key=lambda f: -f["popularity"])
    popular = [f for f in films if (f["minutes"] or 0) >= 40 and "classic_cartoons" not in f["_colls"] and not NOT_POPULAR.search(f["_text"])]
    write("popular.json", "Popular classics", popular[:60])
    write("all.json", "All classic films", films)
    for key, title, _ in ROWS:
        items = sorted(rows[key], key=lambda f: -f["popularity"])
        if len(items) >= 8:
            write(f"{key}.json", title, items)
            index_rows.append({"key": key, "title": title, "file": f"{key}.json", "films": len(items)})
    (OUT / "index.json").write_text(json.dumps({"schema": 1, "generated": now, "source": "Internet Archive (public domain)",
                                                "total": len(films), "all": "all.json", "rows": index_rows}, indent=1))
    print(f"\n{len(films)} playable classic films. Dropped: {dict(dropped)}")
    for r in index_rows:
        print(f"  {r['title']:<20} {r['films']}")


if __name__ == "__main__":
    main()
