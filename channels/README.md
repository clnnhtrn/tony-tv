# Channels (phase 3: build next)

Goal: turn the Free-TV playlist into a clean `channels.json` that Tony can load.

Source: `https://raw.githubusercontent.com/Free-TV/IPTV/master/playlist.m3u8`
(repo: https://github.com/Free-TV/IPTV; human-readable lists are in `lists/*.md`)

The script should:
1. Download the playlist and parse the `#EXTINF` attributes (`tvg-name`, `tvg-logo`, `tvg-id`, `group-title`) and the stream URLs. Check the real attribute names first.
2. Group the channels by country, and give each one a category where it can be worked out.
3. Test every stream from Australia (an HTTP check plus a short play test). Mark channels that are broken, geo-blocked or YouTube-based.
4. Assign stable channel numbers.
5. Write `channels.json`: `id, number, name, country, category, logo, url, hd, geoblocked, youtube, lastChecked`.
6. Report how many working Lebanese and Australian channels there are.

Optionally, host the output on the owner's GitHub so the app can fetch updates without being reinstalled.

Only use officially free sources (see `CLAUDE.md` section 3).
