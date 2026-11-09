#!/bin/bash
# Drive Tony on a connected Google TV device (owner's Chromecast over Wi-Fi debugging, or an emulator).
#   tools/tv.sh shot <file.png>        screenshot
#   tools/tv.sh keys up up ok back     press remote buttons (up down left right ok back home menu)
#   tools/tv.sh install <apk>          install/update Tony
#   tools/tv.sh start | stop           open / close Tony
#   tools/tv.sh log                    Tony's recent log lines (errors, player)
# Set TV_SERIAL to pick a device; default = the first device adb lists.
ADB="$HOME/Library/Android/sdk/platform-tools/adb"
SERIAL="${TV_SERIAL:-$($ADB devices | awk 'NR>1 && $2=="device"{print $1; exit}')}"
a() { "$ADB" -s "$SERIAL" "$@"; }
case "$1" in
  shot) a exec-out screencap -p > "$2" ;;
  keys) shift; for k in "$@"; do
          case "$k" in up) c=19;; down) c=20;; left) c=21;; right) c=22;; ok) c=23;; back) c=4;; home) c=3;; menu) c=82;;
            longok) a shell input keyevent --longpress 23; sleep 0.6; continue;; wait) sleep 1; continue;; *) c="$k";; esac
          a shell input keyevent "$c"; sleep 0.45; done ;;
  install) a install -r "$2" ;;
  start) a shell am start -n app.tony.tv/app.tony.MainActivity >/dev/null ;;
  stop) a shell am force-stop app.tony.tv ;;
  clear) a shell pm clear app.tony.tv ;;
  log) a logcat -d -t 300 | grep -E "app.tony|AndroidRuntime|ExoPlayer|MediaCodec.*Error|FATAL" | tail -60 ;;
  *) echo "usage: $0 shot|keys|install|start|stop|clear|log"; exit 1 ;;
esac
