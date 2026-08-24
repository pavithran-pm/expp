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

DB=/data/data/com.pavithran.paisa/databases/paisa.db
adb exec-out run-as com.pavithran.paisa cat "$DB" > /tmp/paisa.db 2>/dev/null
adb exec-out run-as com.pavithran.paisa cat "$DB-wal" > /tmp/paisa.db-wal 2>/dev/null
adb exec-out run-as com.pavithran.paisa cat "$DB-shm" > /tmp/paisa.db-shm 2>/dev/null
echo "db_bytes=$(stat -c %s /tmp/paisa.db 2>/dev/null || echo 0)" >> /tmp/status

cat /tmp/status
exit 0
