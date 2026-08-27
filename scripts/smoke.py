#!/usr/bin/env python3
"""
Smoke, performance and stability checks against the installed build.

Runs the flows a person actually uses, measures start-up, memory and jank,
and writes docs/qa-report.md. Every check records PASS or FAIL; the exit
code is non-zero if any critical check failed.
"""
import os
import re
import statistics
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from uiauto import (  # noqa: E402
    PKG, back, find, force_stop, launch, screenshot, sh, swipe, tap, type_text, wait_for
)

RESULTS = []
PERF = {}


def check(name, ok, detail="", critical=True):
    RESULTS.append((name, bool(ok), detail, critical))
    print(f"[{'PASS' if ok else 'FAIL'}] {name} {detail}")
    return ok


def log_expense(sentence):
    if not tap("What did you spend on?", exact=False, timeout=15):
        return False
    type_text(sentence)
    return tap("Log expense", timeout=10)


# --- start-up ------------------------------------------------------------

def measure_cold_start():
    times = []
    for _ in range(3):
        force_stop()
        out = sh(f"adb shell am start -W -n {PKG}/.MainActivity").stdout
        match = re.search(r"TotalTime:\s*(\d+)", out)
        if match:
            times.append(int(match.group(1)))
        time.sleep(2)
    if times:
        PERF["Cold start (median of 3)"] = f"{int(statistics.median(times))} ms"
        PERF["Cold start (worst of 3)"] = f"{max(times)} ms"
    return times


def measure_memory():
    out = sh(f"adb shell dumpsys meminfo {PKG}").stdout
    match = re.search(r"TOTAL(?:\s+PSS)?:?\s+(\d+)", out)
    if match:
        PERF["Memory (total PSS)"] = f"{int(match.group(1)) / 1024:.1f} MB"


def measure_jank():
    sh(f"adb shell dumpsys gfxinfo {PKG} reset")
    # Scroll the list a few times to generate frames.
    for _ in range(6):
        swipe(540, 1700, 540, 700, 250)
        swipe(540, 700, 540, 1700, 250)
    out = sh(f"adb shell dumpsys gfxinfo {PKG}").stdout
    total = re.search(r"Total frames rendered:\s*(\d+)", out)
    janky = re.search(r"Janky frames:\s*(\d+)\s*\(([\d.]+)%\)", out)
    p95 = re.search(r"95th percentile:\s*(\d+)ms", out)
    if total and janky:
        PERF["Frames rendered while scrolling"] = total.group(1)
        PERF["Janky frames"] = f"{janky.group(1)} ({janky.group(2)}%)"
        if p95:
            PERF["95th percentile frame time"] = f"{p95.group(1)} ms"
        return float(janky.group(2))
    return None


def crashes_since_start():
    out = sh("adb logcat -d -b crash").stdout
    return [line for line in out.splitlines() if PKG in line]


# --- the smoke pass ------------------------------------------------------

def main():
    os.makedirs("docs/qa", exist_ok=True)
    sh("adb logcat -c -b crash")
    sh(f"adb shell pm clear {PKG}")
    sh(f"adb shell pm grant {PKG} android.permission.RECORD_AUDIO")
    sh(f"adb shell pm grant {PKG} android.permission.POST_NOTIFICATIONS")

    measure_cold_start()
    launch()

    check("App launches to the log screen", wait_for("SPENT TODAY", exact=False, timeout=20) is not None)

    check("Typed expense is parsed and saved",
          log_expense("250 lunch at saravana bhavan") and
          wait_for("Saravana Bhavan", exact=False, timeout=15) is not None)

    check("Amount is formatted in Indian rupees",
          wait_for("₹250", exact=False, timeout=10) is not None)

    check("Quick chip fills the field",
          tap("chai 20", timeout=10) and tap("Log expense", timeout=10) and
          wait_for("₹20", exact=False, timeout=15) is not None)

    check("Unparseable input is still saved",
          log_expense("qwerty nonsense") and
          wait_for("Check", exact=False, timeout=15) is not None)

    for sentence in ["1.2k petrol", "swiggy 450", "recharge 299", "medicine 150 apollo"]:
        log_expense(sentence)

    check("Shorthand amount expands", wait_for("₹1,200", exact=False, timeout=10) is not None)

    screenshot("docs/screenshots/smoke-01-log.png")

    check("Summary tab opens", tap("Summary", timeout=10) and
          wait_for("TOTAL SPENT", exact=False, timeout=15) is not None)
    check("Category breakdown is shown", wait_for("BY CATEGORY", exact=False, timeout=10) is not None)
    screenshot("docs/screenshots/smoke-02-summary.png")

    check("Review tab opens", tap("Review", timeout=10) and
          wait_for("quick check", exact=False, timeout=15) is not None)
    screenshot("docs/screenshots/smoke-03-review.png")

    check("Edit sheet opens on a flagged entry",
          tap("qwerty", exact=False, timeout=10) and
          wait_for("Original", exact=False, timeout=10) is not None)
    check("Edit sheet saves", tap("Save", timeout=10))
    back()

    tap("Log", timeout=10)
    row = find("Swiggy", exact=False)
    if row:
        swipe(row[0] + 300, row[1], 80, row[1], 300)
        check("Swipe deletes with an Undo", wait_for("Deleted", exact=False, timeout=10) is not None)
        check("Undo restores the row", tap("Undo", timeout=8) and
              wait_for("Swiggy", exact=False, timeout=10) is not None)
    else:
        check("Swipe deletes with an Undo", False, "row not found")

    check("Overflow menu offers export and import",
          tap("More", timeout=10) and wait_for("Export to CSV", exact=False, timeout=8) is not None)
    back()

    # Voice: the emulator has no speech input, so this checks the failure path.
    sh("adb logcat -c")
    check("Mic tap does not crash the app", tap("Log by voice", timeout=10))
    time.sleep(8)
    voice_log = sh("adb logcat -d -s PaisaVoice:V").stdout.strip()
    with open("docs/qa/voice-log.txt", "w") as f:
        f.write(voice_log or "(no PaisaVoice lines)")
    check("App is still responsive after the mic attempt",
          wait_for("SPENT TODAY", exact=False, timeout=20) is not None)
    screenshot("docs/screenshots/smoke-04-voice.png")

    # Rotation and process death.
    sh("adb shell settings put system accelerometer_rotation 0")
    sh("adb shell settings put system user_rotation 1")
    time.sleep(3)
    check("Survives rotation", wait_for("SPENT TODAY", exact=False, timeout=20) is not None)
    sh("adb shell settings put system user_rotation 0")
    time.sleep(2)

    force_stop()
    launch()
    check("Data survives a force-stop",
          wait_for("Saravana Bhavan", exact=False, timeout=20) is not None)

    jank = measure_jank()
    check("Scrolling stays under 25% janky frames",
          jank is not None and jank < 25, f"{jank}% janky" if jank is not None else "no data",
          critical=False)

    measure_memory()

    crashes = crashes_since_start()
    check("No crashes in the crash buffer", not crashes, f"{len(crashes)} lines")

    write_report()

    failed = [r for r in RESULTS if not r[1] and r[3]]
    return 1 if failed else 0


def write_report():
    perf_file = "/tmp/perf.txt"
    load_lines = []
    if os.path.exists(perf_file):
        with open(perf_file, errors="replace") as f:
            for line in f:
                match = re.search(r"PaisaPerf.*?:\s*(\S+)=(\S+)", line)
                if match:
                    load_lines.append((match.group(1), match.group(2)))

    passed = sum(1 for r in RESULTS if r[1])
    with open("docs/qa-report.md", "w") as f:
        f.write("# QA report\n\n")
        f.write("Run against the **release** APK (R8-minified) on a Pixel 6 emulator, API 34.\n\n")
        f.write(f"**Smoke: {passed}/{len(RESULTS)} checks passed.**\n\n")

        f.write("## Smoke tests\n\n| Check | Result | Detail |\n|---|---|---|\n")
        for name, ok, detail, critical in RESULTS:
            mark = "pass" if ok else ("**FAIL**" if critical else "warn")
            f.write(f"| {name} | {mark} | {detail} |\n")

        f.write("\n## Performance\n\n| Metric | Value |\n|---|---|\n")
        for key, value in PERF.items():
            f.write(f"| {key} | {value} |\n")

        if load_lines:
            f.write("\n## Load (10,000 expenses)\n\n| Measurement | Value |\n|---|---|\n")
            for key, value in load_lines:
                f.write(f"| `{key}` | {value} |\n")

        f.write("\n## Notes\n\n")
        f.write("- The emulator has no microphone input, so the mic checks cover the\n")
        f.write("  failure and fallback paths only; real transcription needs a phone.\n")
        f.write("- Jank is measured on an emulator without hardware GPU acceleration,\n")
        f.write("  so it is an upper bound rather than a phone-representative number.\n")


if __name__ == "__main__":
    sys.exit(main())
