#!/usr/bin/env bash
# Runs inside the emulator action. Never aborts: every step records its own
# exit code so the report can show what happened.
set -x
: > /tmp/status

# --- debug build: walkthrough, UI tests, load and performance tests -------
adb install -r app/build/outputs/apk/debug/app-debug.apk
echo "install_exit=$?" >> /tmp/status

python3 scripts/demo.py > /tmp/demo.log 2>&1
echo "demo_exit=$?" >> /tmp/status

adb logcat -c
./gradlew connectedDebugAndroidTest > /tmp/androidtest.log 2>&1
echo "tests_exit=$?" >> /tmp/status

adb logcat -d -s PaisaPerf:I > /tmp/perf.txt 2>/dev/null

# --- release build: smoke, performance and stability ---------------------
# This is the build that ships, so the smoke pass runs against the minified
# APK rather than the debug one.
adb uninstall com.pavithran.paisa
adb install -r app/build/outputs/apk/release/app-release.apk
echo "release_install_exit=$?" >> /tmp/status
adb shell pm list packages com.pavithran.paisa

python3 scripts/smoke.py > /tmp/smoke.log 2>&1
echo "smoke_exit=$?" >> /tmp/status
tail -40 /tmp/smoke.log

cat /tmp/status
exit 0
