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

## Usage (built 8 Nov 2026)

```sh
python3 channels/build_channels.py Lebanon Australia   # chosen countries
python3 channels/build_channels.py                     # every country (slow: ~2,000 streams)
python3 channels/build_channels.py --no-test Lebanon   # parse only, no stream tests
```

Plain Python 3, no installs needed. It writes `channels/channels.json` and prints a report.

- `status` is one of `ok`, `broken`, `geoblocked` (marked Ⓖ or HTTP 401/403/451 from Australia), `youtube` (not tested; played through YouTube's official embed or the YouTube app), or `untested`.
- **Channel numbers stay stable**: the playlist's `tvg-chno` is used where present, numbers from the previous `channels.json` are kept, and new channels get the next free number in their country.
- `category` is a rough guess from the channel name, because Free-TV has no category field.
- The test fetches the playlist, follows it to a media playlist, then downloads the first bytes of a segment. A pass doesn't guarantee a picture; the app still has to hide channels that fail to play.

First run (Lebanon + Australia): **Lebanon 13 of 15 OK, Australia 24 of 26 OK.** Results change between runs (Al Manar passed once, then returned 404), so the app should re-check channels itself and not rely on this file alone.
