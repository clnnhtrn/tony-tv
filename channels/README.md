# Channels

Turns two public playlists into one tested `channels.json` that Tony loads, plus a folder of
cleaned-up logos.

| Source | What it is |
| --- | --- |
| [Free-TV/IPTV](https://github.com/Free-TV/IPTV) | Curated, officially free channels (~2,200). Listed first; its numbering wins. |
| [iptv-org/iptv](https://github.com/iptv-org/iptv) | Much larger set of user-submitted public streams (~11,500). |
| [iptv-org/database](https://github.com/iptv-org/database) | Channel IDs, categories, adult flags, country names, logo URLs. |

## Run it

One-time setup (creates `channels/.venv` with Pillow for logo processing):

```sh
python3 -m venv channels/.venv && channels/.venv/bin/pip install pillow resvg-py
```

Then:

```sh
channels/.venv/bin/python channels/build_channels.py                     # every country (~13k streams, 20–40 min)
channels/.venv/bin/python channels/build_channels.py Lebanon Australia   # just these; merged into the existing file
channels/.venv/bin/python channels/build_channels.py --no-test Lebanon   # no stream tests; keeps earlier results
channels/.venv/bin/python channels/contact_sheet.py channels/channels.json /tmp/sheet.png Lebanon   # logo QA image
```

## What it does

1. Downloads both playlists and the iptv-org database.
2. Matches each entry to an iptv-org channel ID (by ID, then by name within the country). The same
   channel from both lists becomes **one** entry with several stream URLs.
3. **Excludes** adult (NSFW) channels and paid/premium subscription channels (beIN, HBO, OSN, Foxtel,
   DAZN and so on), matched by name. Free services like Pluto TV are never excluded.
4. **Tests every stream:** playlist, then media playlist, then the first bytes of a segment. It then
   runs a slower second pass for failures that look temporary. It sends the playback headers a
   playlist publishes for a stream, and never invents its own.
5. Picks the best working URL as `url`, keeps the other working URLs as `backupUrls` for fail-over.
6. Keeps **channel numbers stable** between runs (previous number, then Free-TV `tvg-chno`, then the
   next free number).
7. `enrich.py` downloads each logo (from the iptv-org database first, otherwise the playlist), trims
   it, scales it to at most 320×160, saves it as `logos/<id>.webp`, and works out how the badge should
   look (see CLAUDE.md 7.1).

## Fields in `channels.json`

`id, number, name, country, countryCode, groupKind (country|theme|vod), category, sources[],
url, headers{}, backupUrls[], streams[], status, statusDetail, media (video|audio|web|youtube),
hd, geoblocked, youtube, notAlwaysOn, lastChecked, iptvOrgId, logo, logoBadge (light|dark|fill),
logoBadgeColor, tileColor, logoWidth, logoHeight, logoLowRes`

- `status`: `ok`, `broken`, `geoblocked` (HTTP 401/403/451 from Australia, or marked geo-blocked),
  `web` (a web page, not a stream), `youtube` (played via the official embed or the YouTube app), or
  `untested` (RTMP/RTSP, which the app tries itself).
- `media: audio` means a radio station (no picture). The app shows the logo with "Radio".
- **Nothing is hidden** (owner decision). `status` only drives ordering, badges and fail-over. A
  passing test doesn't guarantee a picture, so the app re-checks when it plays.

## Safety

Everything downloaded is treated as untrusted. Logo files are capped at 5 MB and 16 MP, SVGs are
rendered offline (no external fetches), and the script only writes inside `channels/`.
