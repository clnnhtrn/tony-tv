# v8 — Clean pass (9 Nov 2026)

Owner: v7 in the app "doesn't look clean like Apple TV". Reference: Apple TV and Netflix TV screenshots.
What changed from v7, and why:

1. **Hero = 5 things only**: huge channel name (no logo box, no chips), one quiet line (logo dot · Live · country · category),
   one short description, a white pill **Watch** + round ♡ and ⓘ icon buttons. Page dots centred.
2. **Channel "key art"**: live frame anchored to the top and taller than the card, so tickers along the bottom of the
   frame fall outside; strong dark fade; the channel name set big and bold on the art (title treatment); tiny round
   logo mark top-right. **No text under cards.**
3. **Variety of shapes**: wide favourites, tall numbered "Top in Lebanon" posters, Netflix-style category tiles.
4. **Focus = lift + shadow** (+ thin white ring only on the category tiles).
5. Navy-slate gradient background; plain centred text tabs; round settings icon; no row counts or subtitles.

Canvas: https://claude.ai/artifact/LSq5YwuxkFVzbMK6pg8wBp (boards "v8 · …"). Checked renders in `screens/`.

## Full screen set (9 Nov 2026)
`V8Details` channel page · `V8Watch` watching with channel strip + big preview · `V8Options` frosted options panel
(subtitle rows greyed when unavailable) · `V8Search` · `V8Welcome` first-run countries (selected = white card) ·
`V8Settings` · plus `V8Home`, `V8Rows`.

**"Top in …" ranking (decided approach):** what this TV watches most (minutes per channel, counted on the TV only),
falling back to a fixed list of each country's main broadcasters (Lebanon: MTV, LBCI, Al Jadeed, OTV, Tele Liban,
Al Mayadeen; Australia: ABC, Seven, Nine, 10, SBS), working channels only.
