#!/usr/bin/env python3
"""Collects everything the emulator run produced into docs/emulator-run.md."""
import os
import sqlite3

REPORT = "docs/emulator-run.md"
SHOTS = "docs/screenshots"

CAPTIONS = {
    "01-empty": "Fresh install — empty state, text entry, mic button",
    "02-first-expense": "Typed a sentence, parsed and saved with an Undo snackbar",
    "03-log-list": "A day of expenses; the unparsed one flagged amber",
    "04-summary": "Monthly total, category bars, daily average",
    "05-review": "Review queue — entries the parser wasn't sure about",
    "06-edit": "Edit sheet — original sentence kept, everything correctable",
    "07-final": "Back on the log tab",
    "08-voice": "Mic tapped — recogniser state and error handling",
}


def read(path, tail=None):
    if not os.path.exists(path):
        return f"({path} not produced)"
    with open(path, errors="replace") as f:
        lines = f.read().splitlines()
    return "\n".join(lines[-tail:] if tail else lines)


def status(key):
    raw = read("/tmp/status")
    for line in raw.splitlines():
        if line.startswith(key):
            return line.split("=", 1)[1].strip()
    return "?"


def database_rows():
    if not os.path.exists("/tmp/paisa.db"):
        return "(database not pulled)"
    try:
        con = sqlite3.connect("/tmp/paisa.db")
        rows = con.execute(
            "SELECT amount, category, IFNULL(merchant, '-'), needsReview, rawText "
            "FROM expenses ORDER BY id"
        ).fetchall()
        con.close()
    except Exception as exc:  # noqa: BLE001 - report whatever went wrong
        return f"(could not read database: {exc})"
    header = f"{'amount':>8} | {'category':<10} | {'merchant':<16} | review | rawText"
    out = [header, "-" * len(header)]
    for amount, category, merchant, review, raw in rows:
        flag = "yes" if review else "no"
        out.append(f"{amount:>8.0f} | {category:<10} | {merchant:<16} | {flag:<6} | {raw}")
    return "\n".join(out)


def main():
    tests_ok = status("tests_exit") == "0"
    demo_ok = status("demo_exit") == "0"
    print("status file:\n" + read("/tmp/status"))

    with open(REPORT, "w") as f:
        f.write("# Emulator run\n\n")
        f.write("Pixel 6 profile, API 34, x86_64. The app is installed from the debug\n")
        f.write("APK and driven through adb; taps resolve through the accessibility\n")
        f.write("tree rather than fixed coordinates.\n\n")
        f.write(f"- Scripted walkthrough: **{'ok' if demo_ok else 'failed'}**\n")
        f.write(f"- Instrumented UI tests: **{'pass' if tests_ok else 'fail'}**\n\n")

        f.write("## Expenses in the database afterwards\n\n```\n")
        f.write(database_rows())
        f.write("\n```\n\n")

        f.write("## Screens\n\n")
        for name, caption in CAPTIONS.items():
            if os.path.exists(f"{SHOTS}/{name}.png"):
                f.write(f"### {caption}\n\n![{name}](screenshots/{name}.png)\n\n")

        f.write("## Instrumented test output\n\n```\n")
        f.write(read("/tmp/androidtest.log", tail=45))
        f.write("\n```\n\n")

        if not (demo_ok and tests_ok):
            f.write("## Walkthrough output\n\n```\n")
            f.write(read("/tmp/demo.log", tail=40))
            f.write("\n```\n")

    print(read(REPORT, tail=60))


if __name__ == "__main__":
    main()
