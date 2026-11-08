# Repos the owner asked about (9 Nov 2026)

Checked by reading each repo's README and, where it mattered, its source. Verdicts follow CLAUDE.md section 3.

| Repo | What it is | Use in Tony? |
| --- | --- | --- |
| [cinepro-org/core](https://github.com/cinepro-org/core) | A Node.js server that **scrapes third-party streaming websites** and returns direct video links for movies and TV shows ("50+ sources per title"), bypassing those sites' ads and redirects. It can also act as a Stremio addon. Its own README says users are responsible for legality. | **No.** It extracts streams from piracy sites (section 3.1) and bypasses site protections (section 3.2). |
| [wisnuwirayuda15/cinextma](https://github.com/wisnuwirayuda15/cinextma) | A Next.js website for browsing movies and shows. Information and posters come from TMDB; the video comes from **embedded players of piracy aggregators** (vidsrc, 2embed, embed.su, autoembed, vidlink, multiembed…, seen in `src/utils/players.ts`). | **No** for its video sources. Its UI ideas are generic (browse, search, library). |
| [iongpt/boxarr](https://github.com/iongpt/boxarr) | A small tool that reads the **weekly box-office top 10** (Box Office Mojo) and adds those films to **Radarr**, the movie-download manager in the "*arr" home-server family. Boxarr itself only handles lists and metadata. Radarr usually downloads through torrents or Usenet. | **No.** Tony is live TV, not a download library. Showing "in cinemas now" titles has no use without a legal way to watch them. |
| [rat-nick/film-data-ingestion-pipeline](https://github.com/rat-nick/film-data-ingestion-pipeline) | An example data pipeline (Apache Airflow + PostgreSQL in Docker) that **downloads movie metadata from TMDB weekly** into a database. Information only: titles, genres, dates. No video. | **Not needed.** Legal, but heavy (Airflow, Postgres) for no current Tony feature. If we ever show film info, we'd call TMDB directly. |
| [Stremio addon SDK](https://www.stremio.com/addon-sdk) | Stremio is a media-centre app that has **no content of its own**. Everything comes from "addons": small web services speaking an open HTTP/JSON protocol (`/manifest.json`, `/catalog/…`, `/meta/…`, `/stream/…`). The SDK helps build them. Official addons give metadata (Cinemeta) and some legal sources. The popular film addons (Torrentio and others) stream **torrents of pirated films**. | **The protocol is usable; most of the content isn't.** See below. |

## "Is there a Stremio API to get the films into Tony?"

Stremio doesn't have films. It forwards requests to whichever addons you install, so "Stremio's films" are
really those addons' films. The addon protocol is plain HTTP + JSON, and Tony *could* speak it. But:

- New and popular films reach Stremio through torrent addons. That's piracy, and those torrents are
  shared peer-to-peer, so watching them also uploads the film to others from the family's internet
  connection (real legal exposure in Australia). **Not going into Tony.**
- Legal addons exist, but the films they offer are public domain or already on free services, which
  we can reach more directly:
  - **Internet Archive** public-domain feature films (old classics, legal and free).
  - **Pluto TV / Samsung TV Plus / Plex / Tubi** movie channels: many are **already in Tony's Movies
    row** (≈490 working free movie channels).
  - **YouTube** official free movies (studios' own channels), via the official YouTube player.

**Possible later feature (legal):** an on-demand "Classic films" row from the Internet Archive's
public-domain collection (official API, free, no keys). That's on-demand, not live, so it's a
separate decision.
