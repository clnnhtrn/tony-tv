#!/usr/bin/env python3
"""Render Design-canvas artboards (.dc.html) locally with Playwright and screenshot them.

    tools/.venv/bin/python tools/render_dc.py <runtime.js> <blob-map.json> <out-dir> <file.dc.html> [...]

- <runtime.js>: the canvas runtime (artifact-type/dc-runtime.js, read from the canvas); served as
  ./support.js next to the artboards, exactly as the canvas does.
- <blob-map.json>: {"<blob id>": "<local file>"} so /_blob/<id> image URLs resolve offline.
- Prints every browser console error/warning and page error, then writes <out-dir>/<name>.png.
Exit code 1 if any page logged an error, so it can gate "done".
"""
import http.server
import json
import shutil
import socketserver
import sys
import tempfile
import threading
from pathlib import Path

from playwright.sync_api import sync_playwright

runtime, blob_map, out_dir, *files = sys.argv[1:]
blobs = {k: str((Path(__file__).resolve().parent.parent / v) if not Path(v).is_absolute() else v)
         for k, v in json.loads(Path(blob_map).read_text()).items()}
out = Path(out_dir)
out.mkdir(parents=True, exist_ok=True)

site = Path(tempfile.mkdtemp())
shutil.copy(runtime, site / "support.js")
for f in files:
    shutil.copy(f, site / Path(f).name)


class Quiet(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *a, **k):
        super().__init__(*a, directory=str(site), **k)

    def log_message(self, *a):
        pass


srv = socketserver.TCPServer(("127.0.0.1", 0), Quiet)
port = srv.server_address[1]
threading.Thread(target=srv.serve_forever, daemon=True).start()

failed = False
with sync_playwright() as p:
    browser = p.chromium.launch()
    for f in files:
        name = Path(f).name
        page = browser.new_page(viewport={"width": 1920, "height": 1080})
        logs = []
        page.on("console", lambda m: logs.append(f"console.{m.type}: {m.text}") if m.type in ("error", "warning") else None)
        page.on("pageerror", lambda e: logs.append(f"pageerror: {e}"))

        def blob(route):
            key = route.request.url.rsplit("/_blob/", 1)[1].split("?")[0]
            path = blobs.get(key)
            if path and Path(path).exists():
                route.fulfill(path=path)
            else:
                logs.append(f"missing blob {key}")
                route.fulfill(status=404, body="")

        page.route("**/_blob/**", blob)
        page.goto(f"http://127.0.0.1:{port}/{name}")
        page.wait_for_timeout(2500)  # fonts, images, runtime
        page.screenshot(path=str(out / (name.replace(".dc.html", "") + ".png")))
        print(f"== {name}")
        for line in logs:
            print("  " + line[:400])
            if "error" in line.lower() or "missing blob" in line:
                failed = True
        if not logs:
            print("  no console errors")
        page.close()
    browser.close()
srv.shutdown()
sys.exit(1 if failed else 0)
