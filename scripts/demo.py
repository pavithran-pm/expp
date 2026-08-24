#!/usr/bin/env python3
"""
Drives Paisa on a running emulator and captures screenshots of each step.

Taps are resolved from the accessibility tree rather than hardcoded pixel
coordinates, so the script survives layout and screen-size changes.
"""
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

PKG = "com.pavithran.paisa"
OUT = "docs/screenshots"
SHOTS = []


def sh(cmd, check=False):
    r = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    if check and r.returncode != 0:
        raise RuntimeError(f"{cmd}\n{r.stdout}\n{r.stderr}")
    return r


def dump_tree():
    for _ in range(8):
        sh(f"adb shell uiautomator dump /sdcard/ui.xml")
        xml = sh("adb shell cat /sdcard/ui.xml").stdout.strip()
        if xml.startswith("<?xml"):
            return xml
        time.sleep(1)
    return ""


def find(predicate):
    xml = dump_tree()
    if not xml:
        return None
    try:
        root = ET.fromstring(xml)
    except ET.ParseError:
        return None
    for node in root.iter("node"):
        if predicate(node):
            x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds", "")))
            return (x1 + x2) // 2, (y1 + y2) // 2
    return None


def matches(node, text, exact):
    for attr in ("text", "content-desc"):
        value = node.get(attr) or ""
        if (value == text) if exact else (text.lower() in value.lower()):
            return True
    return False


def tap(text, exact=True, timeout=25, label=None):
    deadline = time.time() + timeout
    while time.time() < deadline:
        point = find(lambda n: matches(n, text, exact))
        if point:
            sh(f"adb shell input tap {point[0]} {point[1]}")
            time.sleep(1.2)
            return True
        time.sleep(1)
    print(f"!! could not find '{label or text}'")
    return False


def type_text(sentence):
    escaped = sentence.replace(" ", "%s").replace("'", "")
    sh(f"adb shell input text '{escaped}'")
    time.sleep(0.6)


def shot(name, caption):
    path = f"{OUT}/{name}.png"
    sh(f"adb exec-out screencap -p > {path}")
    # Downscale so the repository stays light; the full-size copies go to the
    # workflow artifact.
    sh(f"cp {path} /tmp/{name}-full.png")
    sh(f"convert {path} -resize 420x -strip PNG8:{path}")
    size = os.path.getsize(path) if os.path.exists(path) else 0
    print(f"[shot] {name}: {size} bytes — {caption}")
    SHOTS.append((name, caption))


def log_expense(sentence):
    tap("What did you spend on?", exact=False, label="text field")
    type_text(sentence)
    tap("Log expense", label="log button")
    time.sleep(1.2)


def main():
    os.makedirs(OUT, exist_ok=True)
    sh(f"adb shell pm clear {PKG}")
    # Pre-grant so no system permission dialog covers the screenshots.
    for perm in ("android.permission.RECORD_AUDIO", "android.permission.POST_NOTIFICATIONS"):
        sh(f"adb shell pm grant {PKG} {perm}")
    sh(f"adb shell am start -n {PKG}/.MainActivity", check=True)
    time.sleep(6)

    shot("01-empty", "Fresh install: empty state, mic button, text entry")

    log_expense("250 lunch at saravana bhavan")
    shot("02-first-expense", "Typed a sentence: parsed to ₹250 · Food · Saravana Bhavan")

    for sentence in [
        "1.2k petrol",
        "chai 20",
        "swiggy 450",
        "recharge 299",
        "two fifty auto",
        "medicine 150 apollo",
        "qwerty nonsense",
    ]:
        log_expense(sentence)
    shot("03-log-list", "A day of expenses, with the unparsed entry flagged amber")

    tap("Summary", label="summary tab")
    time.sleep(1.5)
    shot("04-summary", "Monthly total, category bars, daily average")

    tap("Review", label="review tab")
    time.sleep(1.5)
    shot("05-review", "Review queue: entries the parser was unsure about")

    tap("Other", exact=False, label="review row")
    time.sleep(1.5)
    shot("06-edit", "Edit sheet: original sentence kept, amount and category fixable")

    sh("adb shell input keyevent 4")  # back
    time.sleep(1)
    tap("Log", label="log tab")
    time.sleep(1)
    shot("07-final", "Back on the log, review badge cleared where fixed")

    # The voice path: the emulator image has no speech service, so this
    # exercises the error handling rather than real recognition.
    tap("Log by voice", label="mic button")
    time.sleep(3)
    shot("08-voice", "Mic tapped — recogniser state / error handling")

    print("walkthrough complete")
    return 0


if __name__ == "__main__":
    sys.exit(main())
