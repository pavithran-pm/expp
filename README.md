# Paisa — Voice Expense Tracker

Android app for logging expenses by voice or a typed sentence. 100% offline,
no accounts, no cloud, no running cost. Kotlin + Jetpack Compose + Room.

Built to the module plan in `docs/plan.md` (M0–M10).

## What it does

- **Type or speak a sentence** — "two fifty lunch at Saravana Bhavan" — and it
  is parsed into amount, category and merchant, then saved.
- **Never rejects input.** Anything the parser is unsure about is still saved,
  flagged `needsReview`, and collected in the Review tab with a badge count.
- **The original sentence is always kept** (`rawText`), so a parser mistake is
  always recoverable.
- Edit sheet (amount, category, merchant, date), swipe-to-delete, undo on every
  destructive action.
- Monthly summary: total, per-category bars with percentages, daily average,
  month selector. Aggregated in SQL.
- Home-screen widget (Jetpack Glance): today's total, one tap into listening via
  a transparent activity — no main screen in between.
- CSV export **and import** through the Storage Access Framework, plus a monthly
  export reminder.

## Building the APK

```bash
./gradlew test            # 50 unit tests: parser + CSV round-trip
./gradlew assembleDebug   # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease # app/build/outputs/apk/release/app-release.apk
```

Requirements: JDK 17 and an Android SDK with platform 35 (Android Studio, or
`sdkmanager "platforms;android-35" "build-tools;35.0.0"`).

CI builds both APKs on every push — see the **Build APK** workflow, artifact
`paisa-apk`. Download the artifact, unzip, and install `paisa-debug.apk` on the
phone (allow "install from unknown sources" once).

### Release signing

`assembleRelease` falls back to the debug key when no keystore is configured, so
it always produces an installable APK. For a real release key:

```bash
keytool -genkey -v -keystore paisa-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 -alias paisa
```

Then add to `local.properties` (git-ignored, never commit it):

```properties
keystore.path=/absolute/path/to/paisa-release.jks
keystore.password=…
key.alias=paisa
key.password=…
```

Back up that keystore in two places. Losing it means you can never install an
update over the existing app without uninstalling — which deletes your data.

## Quality gates

Every push runs, in order:

| Gate | What it covers |
|---|---|
| Unit tests | 63 tests: the parser, CSV round-trips, and the voice recovery matrix |
| Instrumented tests | 10 tests on an emulator, including swipe-to-delete and a 10,000-expense load test |
| Smoke pass | 23 checks against the **release** APK, the build that ships |
| Performance | Cold start, memory, frame timing, and query times under load |

Results land in `docs/qa-report.md`; the voice scenarios are listed in
`docs/voice-test-plan.md`.

## Seeing it run

`docs/emulator-run.md` is written by CI on every push: the app is installed
from the debug APK onto a Pixel 6 emulator (API 34), driven through adb, and
screenshotted at each step, then the instrumented UI tests run against it.
The screenshots live in `docs/screenshots/`.

The emulator has no usable speech input, so the mic there only proves the
recogniser starts and the error paths behave. Voice transcripts feed the same
`ExpenseParser` as typing, which the unit and UI tests cover.

## Project layout

| Path | What lives there |
|---|---|
| `parse/ExpenseParser.kt` | Pure-Kotlin parser. Zero Android imports, so it unit-tests in a second. |
| `data/` | Room entity, DAO, database, repository, money/date formatting. |
| `ui/` | Compose screens: log, summary, review, edit sheet. |
| `voice/` | `SpeechRecognizer` wrapper (every callback implemented) and the widget's transparent capture activity. |
| `widget/` | Glance app widget. |
| `backup/` | CSV export/import and the monthly reminder worker. |

## Notes that cost time to rediscover

- Room data is deleted on uninstall. Export before switching phones.
- `fallbackToDestructiveMigration()` is deliberately **not** enabled — every
  schema change needs a real `Migration`. Schemas are exported to `app/schemas`.
- Speech is requested as `en-IN`; on-device recognition is tried first on API 31+
  and falls back to the standard recognizer.
- The recognizer is destroyed in `onDestroy`/`onCleared`. A leaked one holds the
  mic and the next tap does nothing.
- Some OEM battery optimisers (MIUI, ColorOS, OneUI) suppress widget updates.
  Whitelist the app if today's total goes stale.
