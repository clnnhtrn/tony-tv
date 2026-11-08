# Family TV Channels App — Plan (copy of the claude.ai doc, 8 Nov 2026)

## Overview
We will build a free, private Android TV app that shows free-to-air channels (Lebanon, Australia and others) in a streaming-style interface, test it on the owner's Chromecast with Google TV, then install it for the family.

- **What it is:** a real app with its own icon on the home screen. Open it, see channel tiles in rows, pick one and it plays full screen. No links, playlists or settings for the viewer to touch.
- **Where the channels come from:** the Free-TV list on GitHub (only channels broadcasters give away free). The app downloads that list and keeps only chosen channels.
- **How it's built:** native Android TV app (Kotlin), Compose for TV, Media3/ExoPlayer.
- **Rollout:** Chromecast first, then the family TV. Family TV is a **Samsung** (Tizen), so the app goes on a Google TV stick plugged into it.

## Home screen and how it feels
- Appears in Google TV's "Your apps" row; can be moved to the front (long-press OK → Move).
- Opens to the Home screen.
- Remote: arrows move, OK plays, Back goes back; while watching, up/down changes channel, OK opens the channel strip.
- Unpublished apps can't add rows/previews to the Google TV home screen or appear in the "Live" tab.

## Interface options considered
| Option | What you see on opening | While watching | Best for |
| --- | --- | --- | --- |
| A. Streaming rows | Banner + rows by country/category | Full screen; Back returns | Browsing |
| B. TV guide grid | Channels × time grid | Up/down flips channels | Traditional TV users |
| C. Hybrid (**chosen**) | Streaming rows with Recent/Favourites first | Up/down flips; OK opens channel strip over the picture | Mixed family |

Features ticked: Favourites row, Recently watched row, rows by country & category, live preview on focus, search, auto-hide broken channels, dark theme with own colours/logo.
Not ticked: open straight into last channel, channel-number entry, "What's on now" text, Arabic interface, PIN lock.

## What's needed
| Item | What it's for | Who |
| --- | --- | --- |
| Chromecast with Google TV | Test device | Owner has it |
| Windows/Mac computer on same Wi-Fi | Build & install | Owner |
| Android Studio (free) | Build the APK and install with Run | Owner installs |
| Developer options on Chromecast | Allow install from computer | Owner, once |
| GitHub account (optional, free) | Host the filtered channel list | Owner |
| Channel list | Free-TV playlist filtered to chosen countries | Claude builds filter |
| App code, logo, banner | The app | Claude |
| Google TV stick for family Samsung TV | Family install | Owner buys at phase 6 |

## Costs
Channels, tools, sideloading, GitHub: free. Play developer account (~US$25) not needed. Google TV stick approx. A$60–160 (check current prices). HD live uses roughly 1.5–3 GB/hour.

## Effect on the TV
Standalone app; doesn't change other apps or the launcher; uninstall removes it. Developer options are harmless and can be turned off. Play Protect may warn (normal for self-built apps). App ~15–30 MB. No auto-updates outside the Play Store (channel list updates remotely; app updates by reinstall).

## Drawbacks
Channel availability (Lebanese count unverified), geo-blocking, changing stream URLs, YouTube-based channels unplayable in ExoPlayer, limited TV-guide coverage, no recording in v1, SD quality on some channels, owner becomes support desk.

## Setup plan
1. Decide the interface — done (Version 5).
2. Mock-up review — done.
3. Channel list — next.
4. Build version 1.
5. Install and test on Chromecast: Settings → System → About → click "Android TV OS build" 7× → Developer options → USB/network debugging → install from Android Studio → use for a week, fix issues.
6. Deploy to the family TV via Google TV stick; move Tony to front of "Your apps"; turn Developer options off; show them Home → Tony.

## Decisions
| Decision | Choice |
| --- | --- |
| Layout | C, hybrid — clean like Apple TV / Netflix (final visual = mock-up Version 5) |
| Look | Dark, clean, rounded tiles, subtle focus zoom, lots of space |
| Features | Favourites, Recent, rows by country & category, live preview, search, auto-hide broken channels |
| Channels | All countries available; first-run picker; changeable in Settings |
| Language | English |
| On opening | Home screen |
| Remote | Standard Google TV / Samsung (CEC) remote: arrows, OK, Back, Home |
| App name | **Tony** — "Put Tony on." |
| Build route | Owner installs Android Studio, opens project, presses Run (Wi-Fi debugging to Chromecast) |
| Family TV | Samsung (Tizen) → needs Google TV stick; Samsung remote via HDMI-CEC (Anynet+) |
