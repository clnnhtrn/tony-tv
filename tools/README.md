# Tools

## `render_dc.py`: check design mock-ups yourself

Renders Design-canvas artboards (`.dc.html`) locally with the canvas's own runtime in headless
Chromium (Playwright), prints browser errors, and saves 1920×1080 screenshots. **Run this and look
at the screenshots before showing any mock-up to the owner.**

```sh
python3 -m venv tools/.venv && tools/.venv/bin/pip install playwright && tools/.venv/bin/playwright install chromium
tools/.venv/bin/python tools/render_dc.py tools/canvas-runtime/dc-runtime.js \
    design/mockups/v6-hero/blobs.json /tmp/shots design/mockups/v6-hero/*.dc.html
```

- `canvas-runtime/dc-runtime.js` is the canvas's runtime. It is **not committed** (it's the canvas
  provider's code, not ours). Ask Claude to read `artifact-type/dc-runtime.js` from the canvas
  and save it there.
- `blobs.json` maps the canvas's uploaded `/_blob/<id>` images to local files.
- Exit code 1 means a script error or a missing image.

**Gotcha found 9 Nov 2026:** the live canvas editor stops running an artboard's script when the
markup contains HTML comments (`<!-- -->`). The page then shows raw `{{holes}}`, which a local render
does NOT reproduce. Never put HTML comments in `.dc.html` files.
