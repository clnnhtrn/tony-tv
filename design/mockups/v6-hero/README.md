# v6 — Home with a live hero (9 Nov 2026)

Builds on v5-FINAL (v5 itself is untouched). The owner felt the v5 Home looked "a little plain" and
liked the hero-plus-rows look of streaming sites. v6 adds:

1. **Featured hero**: one channel (from Favourites) plays muted behind the header. A dark fade on the
   left and bottom keeps the text readable. This is the *only* gradient allowed in the app. It shows
   the logo badge, name (72 px), "Channel N · Country · Category · HD", a one-line description, the
   **Watch** and **In Favourites** buttons, and position dots.
2. **Hero follows focus**: resting on a Favourites tile swaps the hero to that channel.
3. **Real live stills and real logos** on every tile, with LIVE/HD pills and favourite hearts.
4. **Row headers with a count chip** ("Lebanon 32").

Artboards: `Main.dc.html` (focus on Watch), `HeroFocus.dc.html` (LBCI tile focused),
`Rows.dc.html` (scrolled into the rows).
Live canvas: https://claude.ai/artifact/7Be4ZdiYyh2Jb96tKEUR63 (images in the `.dc.html` files are
canvas uploads; local copies of the stills are in `stills/`).

The stills are real frames grabbed from each channel's stream on 9 Nov 2026 with ffmpeg. All 18
grabs succeeded, which shows the live-stills feature is practical.
