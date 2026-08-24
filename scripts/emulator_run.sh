#!/usr/bin/env bash
# Runs inside the emulator action. Never aborts: every step records its own
# exit code so the report can show what happened.
set -x
: > /tmp/status

adb install -r app/build/outputs/apk/debug/app-debug.apk
echo "install_exit=$?" >> /tmp/status

python3 scripts/demo.py > /tmp/demo.log 2>&1
echo "demo_exit=$?" >> /tmp/status

./gradlew connectedDebugAndroidTest > /tmp/androidtest.log 2>&1
echo "tests_exit=$?" >> /tmp/status

# Stage through /sdcard: piping run-as output through adb exec-out corrupts
# the binary stream, so the copy happens on the device instead.
for f in paisa.db paisa.db-wal paisa.db-shm; do
  adb shell "run-as com.pavithran.paisa cat /data/data/com.pavithran.paisa/databases/$f > /sdcard/$f" 2>/dev/null
  adb pull "/sdcard/$f" "/tmp/$f" 2>/dev/null
done
echo "db_bytes=$(stat -c %s /tmp/paisa.db 2>/dev/null || echo 0)" >> /tmp/status

cat /tmp/status
exit 0
