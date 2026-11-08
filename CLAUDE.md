# CLAUDE.md — Project "Tony" (family live-TV app for Google TV)

> This file is the full hand-over from the first planning chat (8 Nov 2026, Sydney time).
> Read it completely before writing any code. It records what we are building, who it is for,
> every design decision and preference, what is explicitly out of scope, and what to do next.

---

## 1. One-paragraph summary

**Tony** is a private Android TV / Google TV app that shows **free-to-air, legally free** TV channels
(Lebanon, Australia and any other countries the viewer picks) in a **clean, dark, streaming-style
interface**. It is built for the owner's **older, Arabic-speaking (Lebanese) family**, who are not
technical and not strong in English. The channel list comes from the open-source
**Free-TV/IPTV** playlist on GitHub, filtered to chosen countries. It will be tested first on the
owner's **Chromecast with Google TV**, then installed on a **Google TV stick plugged into the
family's Samsung TV**. The family says: **"Put Tony on."**

---

## 2. Use case and users

- **Owner / developer:** the person who will run this repo in VS Code with Claude Code. Comfortable
  following steps; not necessarily an Android developer. Prefers the **best, easiest, quickest** route.
- **End users:** older family members (Lebanese background). They are **not visually impaired** —
  do NOT oversize the UI (a large-print version was tried and rejected). They need it **clean,
  simple, intuitive, modern**. Limited English, so wording must be short and plain.
- **What they do:** open Tony, see their favourite channels, pick one, watch. While watching, peek at
  another channel and switch. Occasionally search, manage favourites, turn on subtitles.
- **Family TV:** **Samsung** (runs Tizen, NOT Google TV) → needs a **Google TV stick** (e.g. Google TV
  Streamer or another Google TV / Android TV stick). The Samsung remote can usually control the stick
  via **HDMI-CEC (Anynet+)**. Their remote was described as "black, flat, rectangular, like a Google TV
  remote". Design for **D-pad + OK + Back + Home only** (number keys and colour buttons not required).
- **Test device:** owner's **Chromecast with Google TV** (discontinued by Google in 2024, replaced by
  the Google TV Streamer; still works fine for this).

---

## 3. Hard boundaries (must be respected in all future work)

These were discussed explicitly and declined in the first chat. Do not implement them, and do not
help work around them:

1. **No pirated content.** No Netflix / Prime / Disney+ / other paid-platform content. No scraping
   or embedding of piracy sites (e.g. FrameX TV, framextv.*), and no "site like FrameX" either.
2. **No DRM circumvention** (Widevine/PlayReady/FairPlay) and no bypassing of a site's
   anti-inspection, anti-scraping, token or bot protections.
3. **Free, public streams only.** *Changed by the owner on 8 Nov 2026:* the "officially free only" rule
   is relaxed. Tony combines **Free-TV/IPTV** (curated, official) **and iptv-org/iptv** (larger,
   user-submitted public links). Free-TV entries are listed first and win on numbering.
4. **Always excluded:** sexual/adult (NSFW) channels (iptv-org `is_nsfw` / XXX). Films and series with
   violence or horror are fine. **Paid/premium subscription channels** (beIN, OSN, Foxtel, Kayo,
   DAZN, HBO, Cinemax, Showtime, Starz, Sky Sports/Cinema, Eurosport, Canal+, TNT Sports, Fox Sports,
   Setanta, Arena Sport, Sport TV, MBC Max, AXN, AMC, TCM, Syfy, Paramount Network…) are excluded because free links to them are pirated
   re-streams. Matching is by **channel name only**, and free ad-supported services (Pluto, Samsung TV
   Plus, Plex, Tubi, Rakuten…) are never excluded. Racing.com and Shahid's free channels are fine.
   The **iptv-org blocklist** (`api/blocklist.json`: DMCA takedowns + NSFW) is applied to both sources.
   Note: no free channel shows films currently in cinemas; that's only on paid services (owner asked,
   9 Nov 2026). Tony offers a **Movies** row of free movie channels instead.
5. **Allowed:** using playback headers the playlist itself publishes (`#EXTVLCOPT:http-user-agent`,
   `http-referrer`), as every IPTV player does; multiple backup URLs per channel; retries.
   **Not allowed (owner asked; declined 8 Nov 2026):** DRM cracking, token generation, VPN/proxy
   geo-unblocking, or inventing headers to get past a server that refuses us.

Repos the owner asked about on 9 Nov 2026 (cinepro core, cinextma, boxarr, a TMDB pipeline, the Stremio
addon SDK) are assessed in `docs/RESEARCH-repos.md`. Short version: cinepro and cinextma pull video from
piracy sites (**no**); boxarr is a Radarr download helper (not relevant); Stremio's film content comes
mostly from torrent addons (**no**), though its open addon protocol could be used for legal sources.

Legal free alternatives already suggested to the owner for movies/shows (outside this app): SBS On
Demand, ABC iview, 7plus, 9Now, 10, Tubi, Pluto TV, Plex, Kanopy (library card). Some Pluto/Plex
channels already appear in the Free-TV playlist.

---

## 4. Channel source

- Playlist: `https://raw.githubusercontent.com/Free-TV/IPTV/master/playlist.m3u8`
- Repo: `https://github.com/Free-TV/IPTV` — the `.m3u8` is generated by `make_playlist.py` from the
  `.md` files in `lists/` (one file per country/group; `<h1>` = group title; only rows whose URL
  column starts with `[>]` are included). Non-HD, geo-blocked and YouTube-live channels are marked
  in those `.md` files.
- Standard M3U entry shape: `#EXTINF:-1 tvg-name="…" tvg-logo="…" tvg-id="…" group-title="<Country>",<Channel name>` then the stream URL on the next line. **Verify the real attributes by downloading the file — the first chat could not (network blocked).**
- **Verified 8 Nov 2026 (playlist downloaded):** entry shape is
  `#EXTINF:-1 tvg-name="…" tvg-logo="…" tvg-id="…" [tvg-chno="…"] [tvg-country="AU"] group-title="<Country>",<Name> [markers]`.
  Markers at the end of the display name: `Ⓢ` = not HD, `Ⓖ` = geo-blocked, `Ⓨ` = YouTube live,
  `Ⓣ` = undocumented (4 entries). 129 YouTube entries overall, as channel `/live` URLs
  (`youtube.com/@handle/live`, `/c/…/live`, `/user/…/live`), not video IDs.
- **Lebanon: 15 channels** (MTV Lebanon, Tele Liban, Al Manar, Al Mayadeen, Voice of Lebanon, NourSAT,
  Noursat English, Nour Al Koddas, Nour Al Sharq, Aliman TV, Al Ittihad, CharityTV, Falestinona,
  ALWifak News TV, Arabica TV). All have `tvg-chno`, **none have logos**, none are YouTube.
  LBCI and Al Jadeed are **not** in the list. **Australia: 26** (ABC, SBS, Seven, Nine, 10 families,
  plus others; mostly via `i.mjh.nz`). Earlier note that Free-TV excludes religious/party channels is
  **wrong**: the Lebanon list includes religious and party-affiliated channels. The owner can hide
  any of them in Settings → Channels.
- Mock-up channel names are examples only.
- **Second source (added 8 Nov 2026):** `https://iptv-org.github.io/iptv/index.m3u` (≈11,500 entries)
  plus the iptv-org **database** (`iptv-org/database` `data/*.csv`) for channel matching, categories,
  NSFW flags, country names and **logos**. iptv-org names carry `(1080p)` and `[Not 24/7]` /
  `[Geo-blocked]` tags. ≈900 entries publish header hints via `#EXTVLCOPT`.
- **Combined result (Lebanon + Australia, 8 Nov 2026):** Lebanon **27/32 working** (adds LBC
  International, Al Jadeed, OTV, Lana, Red TV, Future TV, mfm, One TV and others). Australia
  **44–45/57 working**. Results vary run to run.
- **Cross-country rows (9 Nov 2026):** `dist/rows/arabic.json` (channels whose main feed language is
  Arabic, ≈285 working) and `dist/rows/movies.json` (category Movies, ≈490 working), listed in
  `dist/index.json` under `rows`. Each channel carries `languages` (ISO 639-3) and `country`.
- **Do not hide any channels for now** (owner, 8 Nov 2026). Everything stays in the list. Status
  fields only drive ordering, badges and fail-over.
- **Script:** `channels/build_channels.py` (see `channels/README.md`) merges both sources into one
  entry per channel, with `url` + `backupUrls` + `headers`, tests every stream, keeps stable
  numbers, then `channels/enrich.py` adds logos (`channels/logos/<id>.png`) and badge colours.
  `channels/contact_sheet.py` renders all badges for visual QA. **Always look at the sheet after
  logo changes.**

---

## 5. Technical approach (decided)

- **Platform:** native **Android TV app (Kotlin)** targeting Google TV.
- **UI:** **Jetpack Compose for TV** (`androidx.tv:tv-material`, `tv-foundation`) — focus handling,
  D-pad navigation, focus scale/ring.
- **Playback:** **Media3 / ExoPlayer** (HLS `.m3u8`, DASH where present). Allow cleartext HTTP
  (`usesCleartextTraffic` / network security config) because many free streams are `http://`.
- **Launcher:** declare `LEANBACK_LAUNCHER` intent + **TV banner (320×180)** so Tony appears in
  Google TV "Your apps". Unpublished apps can't add rows to the Google TV home screen or the "Live" tab.
- **Build/install route (chosen as quickest):** owner installs **Android Studio** (free), opens the
  project, enables Wi-Fi debugging on the Chromecast and presses **Run**. The first chat's sandbox had
  no Android SDK and no internet, so it could not build an APK.
- **Enable on Chromecast:** Settings → System → About → click "Android TV OS build" 7× → Developer
  options → enable USB/Network debugging. Google Play Protect may warn about an app not from the Play
  Store — expected.
- **Updates:** sideloaded apps don't auto-update. Channel fixes via hosted `channels.json`; app updates
  via reinstall (optional in-app "update available" prompt later).
- **Live stills on tiles:** tiles show a recent frame from each channel (refreshed every few minutes)
  with the channel logo badge on top. Implementation idea: short-lived low-res ExoPlayer grab or
  thumbnail capture in a background worker; must be cheap on the Chromecast's limited RAM/CPU and
  ~8 GB storage. Fall back to logo-on-colour if a still can't be grabbed.
- **Live preview:** focused tile / preview card plays muted after ~1 s; stops when focus moves.
  Use at most ONE preview player at a time besides the main player.
- **Design size note:** mock-ups are drawn at **1920×1080 px**. On Android TV at 1080p the logical
  size is typically **960×540 dp**, so **divide mock-up px by 2 for dp/sp**.

---

## 5a. TV safety and security (owner requirement, 8 Nov 2026)

"Make sure any implementation is secure and safe to be used on the TV and that it won't affect anything
else on that TV. No function from the app can change anything on the TV." The app MUST:
- Request **only** `INTERNET` (+ `ACCESS_NETWORK_STATE`). **No** other permissions: no storage,
  no `RECEIVE_BOOT_COMPLETED`, no `SYSTEM_ALERT_WINDOW`, no accessibility service, no device admin,
  no `REQUEST_INSTALL_PACKAGES`, no `WRITE_SETTINGS`, no foreground services that outlive the app,
  no microphone (voice search uses the system's own mic button, which needs no permission).
- **Not** be a launcher/home replacement, not change TV settings, inputs, CEC, volume defaults or
  other apps. Uninstall removes everything (all data in app-private storage).
- Stop all playback and background work when the app leaves the foreground. Live-still and channel
  checks run **only while Tony is open** (no WorkManager jobs when closed).
- Only intent it sends out: opening a YouTube channel in the YouTube app (explicit, user-triggered).
- Treat `channels.json`, playlists and logos as **untrusted input**: fetch the list over HTTPS only,
  validate the schema, cap sizes, ignore unknown fields, keep the last good copy if a download is bad.
  Stream URLs may be `http://` (cleartext allowed for media only; the list itself must be HTTPS).
- WebView (YouTube embed only): JavaScript on for the embed, file/content access **off**, no JS
  bridges, only `youtube.com`/`youtube-nocookie.com`/`ytimg.com`/`googlevideo.com` navigation allowed.
- No analytics, no ads SDKs, no accounts, no data leaves the TV except stream/list/logo requests.
- Release build: R8 minify, `debuggable=false`, `allowBackup=false`.

---

## 6. Owner's preferences (from the conversation)

- Likes: **Apple TV app** (clean), **Netflix** (good but "can be busy"). Doesn't mind **Disney+** but
  doesn't love it. **Dislikes Prime Video.**
- Wants: **clean, simple, intuitive, modern, dark**, "a little like Apple TV", easy to navigate.
- **Not oversized** ("they're not blind") — normal sizes, just clean.
- **English interface.** (Arabic UI option was not chosen.)
- Opens to the **Home screen** (not straight into last channel).
- **All channels available**, with a **first-run setup where the viewer picks countries**.
- Liked most: the **preview element while watching** (check another channel without leaving).
- Wants tiles to show **something about what's playing**, not just text → live stills.
- Wants **versions kept**, never overwritten; everything archived for the repo.
- Name: **Tony** (chosen from Frank, Jon, Sami, Tony, Shasha, Yalla, Hala, Aerial, Tuned…). Tone:
  friendly, a bit funny, easy for non-native English speakers to say.

---

## 7. Final design: Version 5 ("clean") — BUILD THIS

Source files: `design/mockups/v5-FINAL/*.dc.html` (+ live canvas link in section 12).
Earlier versions are kept for reference only (section 8).

### 7.1 Visual system
| Token | Value |
| --- | --- |
| Background | `#15161B` |
| Surface (cards, inputs, panels) | `#24252D`, panel `#1C1D24`, keys/secondary buttons `#24252D`/`#2A2B33` |
| Text primary | `#F5F5F7` / `#FFFFFF` |
| Text secondary | `#C7C7CC`, `#AEAEB2` |
| Text tertiary | `#8E8E93` |
| Accent (single) | `#FF9F0A` amber (alternatives offered: `#0A84FF`, `#30D158`, `#FF375F`) — used for LIVE dots, switches, favourite hearts |
| Font | **Inter** (400–900). Wordmark "tony" lowercase, weight 900, with a small accent dot after it |
| Tile radius | 18–20 px; pills fully rounded |
| Focus state | **white ring with a dark gap** (`0 0 0 4px bg, 0 0 0 8px #FFF`) + **scale ~1.08** + soft shadow; label under focused tile turns white/bold, others muted grey |
| Channel tile | live still (placeholder in mock-ups) + **white logo badge** top-left + optional **LIVE** pill / **heart** badge |
| Logo badge (real logos, decided 8 Nov 2026) | Badge colour from `channels.json`: `logoBadge` = `light` (white `#FFFFFF`), `dark` (`#0B0C10`, for white logos) or `fill` (`logoBadgeColor`, the logo's own background). Non-white badges get a 1 px `rgba(255,255,255,0.14)` hairline. Radius 10 px, padding 10×12 px. **Optical sizing:** every logo gets the same visual area (120×36 px² @1080p), so w = √(A·aspect), h = w/aspect, clamped to max 150×52 px. Don't size by fixed height, or square logos look tiny. |
| Fallback tile (no live still) | `tileColor` from `channels.json` (dark brand tone, or neutral `#24252D`). Logo badge centred and larger instead of top-left. No logo → channel name in Inter 700 |
| Spacing | left page margin 104 px, top 48 px, row gap ~44 px, tile gap 28 px |
| Sizes (px @1080p) | Wide tile 400×225; standard tile 250×140; section titles 24 px/700; nav pills 21 px; body 21–22 px |
| No | gradients washes, emoji, clutter, big help bars, oversized text |

### 7.2 Screens (all in v5-FINAL)
1. **Home (`V5Home`)** — header: `tony•` wordmark, nav pills **Home / Favourites / Search**, clock,
   settings icon. Rows in order:
   1. **Favourites** — wide live tiles (logo badge + LIVE pill).
   2. **Recent** — standard live tiles (hearts on ones that are favourites). (Renamed from "Keep watching" at owner's request.)
   3. **One row per selected country** (e.g. Lebanon, Australia), in Settings order.
2. **First-run country picker** — only drafted in v1 (`v1-first-draft/Welcome.dc.html`); needs a v5-style
   redesign: "Which countries do you want to watch?", grid of country cards with check marks + channel
   counts, Continue button; changeable later in Settings.
3. **Channel details (`V5Details`)** — big muted live preview top-right, logo, name (72 px), meta line
   (LIVE · Channel N · Country · Category · HD), one-line description, buttons **Watch** (focused) and
   **Favourite / In Favourites**, row "More from <country>".
4. **Favouriting** — pressing the favourite button shows a top-centre toast **"Added to Favourites"**
   (heart in accent) and the button becomes a filled heart "In Favourites". Also: hold OK on any tile.
5. **Watching + preview (`V5Player`)** — full-screen picture, small "Now watching" chip top-left. OK opens
   a bottom strip: pills (Favourites / Lebanon / Australia) + row of live tiles; current channel marked
   "WATCHING". Resting on a tile pops up a **larger preview card** above it ("PREVIEW", channel name,
   "OK to switch") while the current channel keeps playing. Up/Down while watching (strip closed) =
   next/previous channel. Back = close strip / back to Home.
6. **Watching options (`V5PlayerOptions`)** — right side panel "Options": **Subtitles** (only when the
   stream has captions, e.g. some Australian channels; otherwise "not available"), Subtitle size,
   Audio (when multiple tracks), Picture size (Fit/Fill), Quality (Auto), **Report a problem**.
7. **Search (`V5Search`)** — left: search field + **6-column letter grid** (a–z, 1–4) + Space/Delete/Clear
   + "hold the mic button and say a channel name" hint. Right: results as live tiles in a 3-column grid.
   (Changed from a single long row of letters — owner found it annoying; a grid needs far fewer presses.)
8. **Favourites page (`V5Favourites`)** — 4-column grid of live tiles with country under each; hint
   "Hold OK on a channel to remove or move it".
9. **Settings (`V5Settings`)** — left menu: **Countries** (on/off switches + row order), **Channels**
   (hide/reorder), **Subtitles** default, **When Tony opens** (Home), **Check channels** (last checked
   time), **About Tony** (version, up to date). Right: the selected section.

### 7.3 Features chosen (from the plan checklist)
- ✅ Favourites row · ✅ Recently watched row · ✅ Rows by country and by category · ✅ Live preview
  on focus · ✅ Search by channel name · ✅ Auto-hide channels that aren't working · ✅ Dark theme
  with own colours/logo
- ❌ Open straight into last channel (Home instead) · ❌ Channel-number keypad entry · ❌ "What's on
  now" guide text (only possible where EPG exists; can revisit) · ❌ Arabic interface · ❌ PIN lock
- Channel **numbers** still shown on details ("Channel 1") as a stable ordering aid.

### 7.4 Open design questions
- **Empty Favourites state — DECIDED (8 Nov 2026):** keep the row and show the short message
  "Hold OK on any channel to add it here". Same message on the Favourites page when it's empty.
- Redesign the **first-run country picker** in v5 style.
- Programme names on tiles (needs EPG, e.g. iptv-org EPG) — optional later.
- **YouTube-based channels — DECIDED (8 Nov 2026):** play them **inside Tony** where possible, using
  YouTube's **official embedded player** (IFrame Player API in a WebView, e.g. the open-source
  `android-youtube-player` library). Keep YouTube's branding and ads; no ad-blocking or stream
  extraction (section 3). **If a channel can't be embedded** (embedding disabled by the owner, not
  live right now, or the player errors), **open it in the YouTube app** with an intent to the
  channel's `/live` URL. Caveat: the embed needs a current live **video ID**, and the playlist only
  has `/live` channel URLs. Get the ID legitimately, either from the YouTube Data API (free key,
  `search?channelId=…&eventType=live`) or the channel-ID live embed. Don't scrape YouTube pages.
  Work out the details in phase 4. YouTube tiles get no live stills or muted preview (just the logo).
  Lebanon and Australia currently have no YouTube channels, so this only affects other countries.

---

## 8. Design history (all kept in `design/mockups/`)
| Version | Folder | What it was | Outcome |
| --- | --- | --- | --- |
| v1 | `v1-first-draft` | Hero + rows, country picker, side channel list player, search with keyboard grid | Starting point |
| v2 | `v2-apple-tv-inspired` | Slate bg, "Keep watching" wide tiles, numbered "Your channels" row, bottom channel strip player | "Don't love it" |
| v3 | `v3-cinematic` | Full-screen live background, huge channel name, single row of white logo cards | Not chosen |
| v4 | `v4-large-accessible` | Big text, 4 huge cards, permanent help bar | Rejected: too big ("they're not blind") |
| v5 | `v5-clean-home-only` → `v5-full-set-rev1` → **`v5-FINAL`** | Clean, normal sizes; then full screen set; then Recent + Favourites row + grid search | **Chosen** (base for all screens) |
| v7 | `v7-polished` | v6 + polish: hairline borders, lift + 3 px edge focus (replaces the thick double ring), refined uppercase chips, frosted logo badges, 16/14 px radius, row subtitles + "See all", hero divider, Movies row | **Proposed 9 Nov 2026**, awaiting owner review |
| v6 | `v6-hero` | v5 + live **hero** at top of Home (featured favourite plays muted, follows focus), real logos + real live stills, LIVE/HD pills, row count chips. Inspired by the hero-plus-rows layout the owner liked on a streaming site: layout ideas only, no branding copied | **Proposed 9 Nov 2026**, awaiting owner review |

`.dc.html` files are Claude "Design canvas" artboards (HTML + a small template runtime loaded from
`./support.js`, which only exists inside the canvas). Open them in the canvas link to view; in the repo
they are the **visual spec**. Read their inline styles for exact sizes/colours.
`design/reference/style-reference.jpg` is the owner's photo of the Apple TV app on a Samsung TV —
inspiration for cleanliness only; **do not copy Apple's layouts, artwork or branding**.

---

## 9. Drawbacks and warnings to keep in mind
- Free streams **break or change URLs**; some are **geo-blocked** outside their country; quality varies
  (some SD). The app must detect failures, hide/mark broken channels, and allow quick list updates.
- **YouTube-live** channels can't play in ExoPlayer.
- **No DVR/pause live** in v1.
- Data use roughly 1.5–3 GB/hour for HD live — check the family's internet plan.
- **LG/webOS** was considered earlier: custom apps via Developer Mode expire unless renewed, so
  Google TV stick was chosen. The family TV is Samsung (Tizen), so a stick is required anyway.
- The owner will be **support desk**; design for zero-maintenance (auto-hide broken channels,
  remote-updatable list).
- Chromecast has limited RAM/storage — keep previews/stills lightweight.
- App won't change other TV functions; doesn't replace the launcher; uninstall removes it fully.

## 10. Costs
App, tools, Free-TV list, sideloading and GitHub: **free**. Google Play dev account (~US$25 one-time)
**not needed**. Google TV stick for family TV: roughly A$60–160 depending on model (check current
prices).

---

## 11. Plan / phases and status
1. Decide interface — **DONE** (Version 5).
2. Mock-up review — **DONE** (v5-FINAL), minus v5 country picker and empty-favourites state.
3. **Channel list — DONE (9 Nov 2026)**: combined Free-TV + iptv-org, 11,168 channels / 187 groups,
   logos, per-country files. **Repo: https://github.com/clnnhtrn/tony-tv (public).** The app reads
   `https://raw.githubusercontent.com/clnnhtrn/tony-tv/main/channels/dist/index.json`, then
   `…/channels/dist/countries/<slug>.json`, with logos at `…/channels/<logo>` (verified 200 over HTTPS).
4. Build app v1 in `app/` (Kotlin, Compose for TV, Media3): Home rows, details, player + preview strip,
   options panel, search, favourites, settings, first-run picker, auto-hide broken channels, live stills.
5. Install & test on owner's Chromecast with Google TV for ~a week; fix issues.
6. Deploy to family: Google TV stick on the Samsung, move Tony to front of "Your apps", enable CEC,
   turn off Developer options, show them: Home → Tony.

## 12. Links from the first chat (owner's private claude.ai artifacts)
- Plan doc: https://claude.ai/artifact/JyaEbnFL1pioS3hdFDY6wc ("Family TV Channels App — Plan")
- Mock-up canvas: https://claude.ai/artifact/AnE8LENCwm8ykb6yptppkh ("Tony — TV app mock-ups";
  pages Version 1–5)
- A copy of the plan is in `docs/PLAN.md`.

## 13. How to explain Tony to the family (owner asked for this)
> "TV stations in Lebanon and other countries put their channels on the internet for free. There's a
> free public list that collects them. I pick the channels you'd want and put them in an app with a
> nice menu, like Netflix but for live TV. It's on a little stick on your TV — press the button and
> pick a channel. If a channel stops working, I update the list."

## 14. Working rules for Claude in this repo
- **Quality bar (owner, 8 Nov 2026):** this will be shown to the family. It must look professional, not
  "half done or AI slop". Use the design skills and solid UI/UX principles. **Test every route and
  deliberately try to break things** (no network, dead stream, bad list, slow stream, empty favourites,
  geo-blocked, radio-only, YouTube, remote button mashing), then fix what breaks.
- **Check your own visual work before showing it:** render mock-ups with `tools/render_dc.py`
  (Playwright) and look at the screenshots. Later, check the app on the Android TV emulator with
  `adb exec-out screencap`. No HTML comments in `.dc.html` files (see `tools/README.md`).
- Keep **every design/version**; never overwrite a version — add a new one.
- Ask before big or irreversible changes; small, clear tasks can go ahead.
- Keep the UI **clean and normal-sized**; resist adding clutter.
- Respect section 3 at all times.
