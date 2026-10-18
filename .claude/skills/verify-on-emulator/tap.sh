#!/bin/zsh
# Taps the first on-screen element whose text or accessibility label matches $1 (a regex).
# Usage: tap.sh "Add spending"   Prints where it tapped, or "not found" (exit 1).
ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
DUMP=$(mktemp)
$ADB shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1 && $ADB shell cat /sdcard/ui.xml > "$DUMP"
XY=$(python3 -c "
import re, sys
s = open(sys.argv[2]).read()
for m in re.finditer(r'<node [^>]*>', s):
    n = m.group(0)
    t = re.search(r' text=\"([^\"]+)\"', n) or re.search(r' content-desc=\"([^\"]+)\"', n)
    b = re.search(r'bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', n)
    if t and b and re.fullmatch(sys.argv[1], t.group(1)):
        x1, y1, x2, y2 = map(int, b.groups()); print((x1 + x2) // 2, (y1 + y2) // 2); break
" "$1" "$DUMP")
rm -f "$DUMP"
if [ -z "$XY" ]; then echo "not found: $1"; exit 1; fi
set -- ${=XY}; $ADB shell input tap $1 $2; echo "tapped at $XY"
