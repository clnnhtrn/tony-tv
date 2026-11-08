# Recap of the first chat (8 Nov 2026, ~20:45–22:55 Sydney)

A chronological account of what was asked, answered, decided and produced.

## 1. IPTV background
- **Q:** How do IPTV subscriptions (Lebanese channels + Netflix content) work?
- **A:** Explained capture → re-encode → restream → reseller panels, M3U playlists and player apps. Most such services are **unlicensed**; in Australia they're copyright infringement (sites blocked under s115A of the Copyright Act); risks include outages, scams, malware, no refunds. Suggested legal options: Shahid, OSN+, broadcasters' own apps (LBCI, MTV Lebanon, Al Jadeed), licensed Arabic packages.

## 2. Free-TV playlist
- Owner has the **Free-TV/IPTV** repo. Explained it lists only officially free channels; use `https://raw.githubusercontent.com/Free-TV/IPTV/master/playlist.m3u8` in any IPTV player (VLC, TiviMate, IPTV Smarters); lists live in `lists/*.md`, playlist built by `make_playlist.py`; geo-blocked channels are marked.

## 3. Declined requests (boundaries)
- Adding Netflix content / replicating a paid IPTV service → declined (piracy).
- **FrameX TV** (framextv.xyz/.cc/.org) explained as an unlicensed streaming aggregator; pulling its content into an app → declined; "a site like FrameX" → declined.
- General legit scraping explained (find JSON API in DevTools Network tab, find .m3u8/.mpd, backend + TV front end; DRM can't be bypassed).
- How sites hide DevTools explained at a high level; help getting around a site's right-click/inspect block → declined. Offered to check for an official API/embed instead.
- Legal free alternatives suggested: SBS On Demand, ABC iview, 7plus, 9Now, 10, Tubi, Pluto TV, Plex, Kanopy, Internet Archive, TMDB (metadata), official studio YouTube uploads.

## 4. Jellyfin
- Explained Jellyfin (free open-source media server for your own files; Live TV accepts M3U, so Free-TV can be added). Not chosen for this project.

## 5. Putting IPTV on a TV
- Steps for Android/Google TV/Firestick (TiviMate, IPTV Smarters, VLC), Samsung/LG (SS IPTV, Smart IPTV — device ID + website; some charge activation), laptop + HDMI.
- LG runs **webOS** (not Google/Android). Custom webOS apps need Developer Mode, which expires unless renewed → Google TV stick preferred.

## 6. Custom app idea
- Owner wants a real app with a streaming-style interface (no loading links). Confirmed possible. Wrote plain-language explanations for the family.
- Owner said "stop" once mid-build (nothing had been built; network fetch of the playlist was blocked).

## 7. Plan doc
- Wrote the detailed plan (see `PLAN.md`): native Android TV app, test on Chromecast, costs, effects on TV, drawbacks, phases, decisions.
- Owner ticked features and chose: layout C, Apple-TV-like dark look, all channels with first-run country picker, English, Home on open.
- Family TV confirmed **Samsung** → Google TV stick needed; Samsung remote via HDMI-CEC.
- Name options: Aerial, Tuned, Atlas, Relay, Frank, Jon, Sami, **Tony**, Shasha, Yalla, Hala → owner chose **Tony**.
- Build route: Android Studio + Run (Claude's sandbox had no Android SDK/network to build an APK).

## 8. Mock-ups (canvas "Tony — TV app mock-ups")
- **v1**: Home, first-run country picker, player with side channel list, search.
- Owner shared a photo of the Apple TV app on their Samsung as style reference → **v2** (Apple-TV-inspired, numbered channel row, bottom strip player). Owner: "don't love it".
- **v3**: cinematic full-screen background, white logo cards. Owner explained the audience (older people; likes Apple TV, Netflix a bit busy, dislikes Prime, lukewarm on Disney+).
- **v4**: large/accessible. Owner: too big — "they're not blind", just clean.
- **v5**: clean, normal size → **chosen**. Expanded to full set: Home (live stills on tiles), Channel details + favouriting toast, Watching with preview strip (owner's favourite element), Watching options (subtitles etc.), Search, Favourites, Settings.
- Revisions: "Keep watching" → **Recent**; **Favourites row added to Home** (top); Search keyboard changed from one long line to a **6-column grid**.
- Open question asked: empty Favourites row → show hint message (recommended) or hide?

## 9. This repo
- Owner asked for everything as a zip to continue in VS Code with Claude Code. Every version was archived (never overwritten). `CLAUDE.md` is the master brief.
