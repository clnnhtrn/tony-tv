# Design

- `mockups/v5-FINAL/` is **the spec to build**. It has one file per screen:
  - `V5Home`: the home screen.
  - `V5Details`: channel details, with the "Added to Favourites" toast.
  - `V5Player`: watching, with the preview strip open.
  - `V5PlayerOptions`: watching, with the options panel (subtitles and other settings).
  - `V5Search`: search.
  - `V5Favourites`: the favourites page.
  - `V5Settings`: settings.
- The other folders hold earlier versions, kept for history. `CLAUDE.md` section 8 says what each one was and why it was or wasn't chosen.
- `reference/style-reference.jpg` is the owner's photo of a streaming app, used as inspiration for how clean the design should feel. It's for cleanliness only; don't copy its layout or artwork.

## About `.dc.html` files
These are artboards from a Claude Design canvas. Each one is HTML with inline styles and a small template script, and it loads `./support.js`, a file that only exists inside the canvas. Opened straight from disk, they won't render fully. To see them, open the live canvas: https://claude.ai/artifact/AnE8LENCwm8ykb6yptppkh. The exact sizes and colours are in the inline `style` attributes and in the `renderVals()` data.

Each artboard is drawn at 1920×1080 px. On Android TV that's roughly 960×540 dp, so divide by 2.
