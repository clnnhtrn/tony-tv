# v7 — Polished (9 Nov 2026)

v6's hero-plus-rows layout with a polish pass the owner asked for ("I don't like the thick borders…
hairline borders… more polished, especially the cards and UI"). v6 is untouched.

- **Hairlines everywhere:** 1 px `rgba(255,255,255,0.10)` inside every tile (an overlay, so it sits over
  the picture), 0.14–0.18 on chips, buttons, the nav capsule and the settings button.
- **Focus = lift + crisp edge:** scale 1.05–1.06, a 3 px white inner edge (3 rather than 2 so older eyes
  can spot it from the sofa) and a soft shadow. Unfocused stills are dimmed to 80% brightness. The old
  double ring with a dark gap is gone.
- **Chips:** small uppercase, 1.4 px letter spacing, 7 px radius, translucent dark with a hairline
  (LIVE with the accent dot, FEATURED, HD). The favourite heart is a matching square chip, bottom-right.
- **Logo badge:** white badges get a soft shadow; dark badges are frosted (blur) with a hairline; filled
  badges get a hairline plus shadow.
- **Radius** 16 px (wide) and 14 px (standard), down from 20.
- **Type:** title 76/800 at −2.2 px tracking; row titles 26/700 at −0.3 px; tabular digits in the clock.
- **Row header:** title, outlined count chip, muted subtitle, and "See all ›" on the right.
- **Hairline divider** between the hero and the rows. Buttons are 12 px-radius rectangles: a solid white
  primary when focused, translucent with a hairline otherwise.
- New **Movies** row (free movie channels from `dist/rows/movies.json`) with real stills.

Stills: letterboxed frames are auto-cropped (black bars removed, centre-cropped to 16:9). **The app must
do the same when it makes live stills.**

Canvas: https://claude.ai/artifact/LSq5YwuxkFVzbMK6pg8wBp · checked renders in `screens/`.
