#!/usr/bin/env python3
"""Small adb + accessibility-tree driver shared by the walkthrough and smoke runs."""
import re
import subprocess
import time
import xml.etree.ElementTree as ET

PKG = "com.pavithran.paisa"


def sh(cmd):
    return subprocess.run(cmd, shell=True, capture_output=True, text=True)


def dump_tree():
    for _ in range(8):
        sh("adb shell uiautomator dump /sdcard/ui.xml")
        xml = sh("adb shell cat /sdcard/ui.xml").stdout.strip()
        if xml.startswith("<?xml"):
            return xml
        time.sleep(1)
    return ""


def _nodes():
    xml = dump_tree()
    if not xml:
        return []
    try:
        return list(ET.fromstring(xml).iter("node"))
    except ET.ParseError:
        return []


def _matches(node, text, exact):
    for attr in ("text", "content-desc"):
        value = node.get(attr) or ""
        if (value == text) if exact else (text.lower() in value.lower()):
            return True
    return False


def find(text, exact=True):
    for node in _nodes():
        if _matches(node, text, exact):
            x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds", "")))
            return (x1 + x2) // 2, (y1 + y2) // 2
    return None


def wait_for(text, exact=True, timeout=20):
    deadline = time.time() + timeout
    while time.time() < deadline:
        point = find(text, exact)
        if point:
            return point
        time.sleep(1)
    return None


def tap(text, exact=True, timeout=20):
    point = wait_for(text, exact, timeout)
    if not point:
        return False
    sh(f"adb shell input tap {point[0]} {point[1]}")
    time.sleep(1.2)
    return True


def scroll_to(text, exact=False, attempts=6):
    """Find text, scrolling the list up until it appears."""
    point = find(text, exact)
    if point:
        return point
    for _ in range(attempts):
        swipe(540, 1700, 540, 900, 250)
        point = find(text, exact)
        if point:
            return point
    return None


def screen_size():
    out = sh("adb shell wm size").stdout
    match = re.search(r"(\d+)x(\d+)", out)
    return (int(match.group(1)), int(match.group(2))) if match else (1080, 2400)


def ensure_app():
    """Bring the app back if something (a system screen, a stray back) left it."""
    if in_app():
        return True
    launch()
    return in_app()


def in_app():
    out = sh("adb shell dumpsys window | grep -E 'mCurrentFocus|mFocusedApp'").stdout
    return PKG in out


def tap_at(x, y):
    sh(f"adb shell input tap {x} {y}")
    time.sleep(1.0)


def type_text(sentence):
    sh(f"adb shell input text '{sentence.replace(' ', '%s').replace(chr(39), '')}'")
    time.sleep(0.6)


def swipe(x1, y1, x2, y2, ms=300):
    sh(f"adb shell input swipe {x1} {y1} {x2} {y2} {ms}")
    time.sleep(1.0)


def back():
    sh("adb shell input keyevent 4")
    time.sleep(1.0)


def launch():
    sh(f"adb shell am start -n {PKG}/.MainActivity")
    time.sleep(4)


def force_stop():
    sh(f"adb shell am force-stop {PKG}")
    time.sleep(1)


def screenshot(path, width=420):
    sh(f"adb exec-out screencap -p > {path}")
    sh(f"convert {path} -resize {width}x -strip PNG8:{path}")
