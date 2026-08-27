# Emulator run

Pixel 6 profile, API 34, x86_64. The app is installed from the debug
APK and driven through adb; taps resolve through the accessibility
tree rather than fixed coordinates.

- Scripted walkthrough: **ok**
- Instrumented UI, load and performance tests: **pass**
- Release APK installs: **yes**
- Smoke pass on the release build: **fail** (see docs/qa-report.md)

## Screens

### Fresh install — empty state, text entry, mic button

![01-empty](screenshots/01-empty.png)

### Typed a sentence, parsed and saved with an Undo snackbar

![02-first-expense](screenshots/02-first-expense.png)

### A day of expenses; the unparsed one flagged amber

![03-log-list](screenshots/03-log-list.png)

### Monthly total, category bars, daily average

![04-summary](screenshots/04-summary.png)

### Review queue — entries the parser wasn't sure about

![05-review](screenshots/05-review.png)

### Edit sheet — original sentence kept, everything correctable

![06-edit](screenshots/06-edit.png)

### Back on the log tab

![07-final](screenshots/07-final.png)

### Mic tapped — recogniser state and fallback

![08-voice](screenshots/08-voice.png)

### Mic without permission — the system prompt

![09-voice-permission](screenshots/09-voice-permission.png)

### Permission denied — the app explains why the mic is needed

![10-voice-denied](screenshots/10-voice-denied.png)

## What the recogniser reported

```
--------- beginning of main
08-27 14:21:49.113  2715  2715 W PaisaVoice: error 12 on ON_DEVICE (attempt 1)
08-27 14:21:49.114  2715  2715 D PaisaVoice: ready for speech on ON_DEVICE
08-27 14:21:49.313  2715  2715 D PaisaVoice: ready for speech on SYSTEM_SERVICE
08-27 14:21:54.374  2715  2715 W PaisaVoice: error 7 on SYSTEM_SERVICE (attempt 2)
```

## Load and performance measurements

```
--------- beginning of main
08-27 14:22:55.103  7029  7050 I PaisaPerf: bulk_insert_ms_for_10000=21608
08-27 14:22:55.288  7029  7050 I PaisaPerf: single_insert_ms_for_50=185
08-27 14:22:55.449  7029  7050 I PaisaPerf: observe_all_first_emission_ms=160
08-27 14:22:55.452  7029  7050 I PaisaPerf: category_aggregation_ms=3
08-27 14:22:55.454  7029  7050 I PaisaPerf: month_total_ms=2
08-27 14:22:55.457  7029  7050 I PaisaPerf: review_count_ms=3
08-27 14:23:12.068  7029  7050 I PaisaPerf: parse_10000_ms=16605
08-27 14:23:12.068  7029  7050 I PaisaPerf: parse_micros_each=1660.5
08-27 14:23:15.702  7029  7050 I PaisaPerf: burst_100_inserts_ms=350
```

## Smoke output

```
[PASS] The build under test is installed 
[PASS] App launches to the log screen 
[PASS] Typed expense is parsed and saved 
[PASS] Amount is formatted in Indian rupees 
[PASS] Quick chip fills the field 
[PASS] Unparseable input is still saved 
[PASS] Shorthand amount expands 
[PASS] Summary tab opens 
[PASS] Category breakdown is shown 
[PASS] Review tab opens 
[PASS] Edit sheet opens on a flagged entry 
[PASS] Edit sheet saves 
[PASS] Still in the app after editing 
[FAIL] Swipe deletes with an Undo 
[FAIL] Undo restores the row 
[PASS] Overflow menu offers export and import 
[FAIL] Mic tap does not crash the app 
[FAIL] App is still responsive after the mic attempt 
[FAIL] Survives rotation 
[PASS] Data survives a force-stop 
[PASS] Frame timing recorded while scrolling 100.0% janky — software rendering on the emulator
[PASS] No crashes in the crash buffer 0 lines
```

## Instrumented test output

```
> Task :app:stripDebugDebugSymbols UP-TO-DATE
> Task :app:validateSigningDebug UP-TO-DATE
> Task :app:writeDebugAppMetadata UP-TO-DATE
> Task :app:writeDebugSigningConfigVersions UP-TO-DATE
> Task :app:packageDebug UP-TO-DATE
> Task :app:createDebugApkListingFileRedirect UP-TO-DATE
> Task :app:mergeDebugAndroidTestShaders
> Task :app:compileDebugAndroidTestShaders NO-SOURCE
> Task :app:copyRoomSchemasToAndroidTestAssetsDebugAndroidTest
> Task :app:generateDebugAndroidTestAssets UP-TO-DATE
> Task :app:mergeDebugAndroidTestAssets
> Task :app:compressDebugAndroidTestAssets FROM-CACHE
> Task :app:desugarDebugAndroidTestFileDependencies FROM-CACHE
> Task :app:dexBuilderDebugAndroidTest FROM-CACHE
> Task :app:mergeDebugAndroidTestGlobalSynthetics FROM-CACHE
> Task :app:processDebugAndroidTestJavaRes
> Task :app:checkDebugAndroidTestDuplicateClasses
> Task :app:mergeDebugAndroidTestJniLibFolders
> Task :app:mergeExtDexDebugAndroidTest FROM-CACHE
> Task :app:mergeLibDexDebugAndroidTest FROM-CACHE
> Task :app:mergeProjectDexDebugAndroidTest FROM-CACHE
> Task :app:mergeDebugAndroidTestNativeLibs NO-SOURCE
> Task :app:stripDebugAndroidTestDebugSymbols NO-SOURCE
> Task :app:validateSigningDebugAndroidTest
> Task :app:writeDebugAndroidTestSigningConfigVersions
> Task :app:mergeDebugAndroidTestJavaResource
> Task :app:packageDebugAndroidTest
> Task :app:createDebugAndroidTestApkListingFileRedirect
[EmulatorConsole]: Failed to start Emulator console for 5554

> Task :app:connectedDebugAndroidTest
Starting 10 tests on emulator-5554 - 14

emulator-5554 - 14 Tests 0/10 completed. (0 skipped) (0 failed)
emulator-5554 - 14 Tests 1/10 completed. (0 skipped) (0 failed)
emulator-5554 - 14 Tests 2/10 completed. (0 skipped) (0 failed)
emulator-5554 - 14 Tests 4/10 completed. (0 skipped) (0 failed)
emulator-5554 - 14 Tests 7/10 completed. (0 skipped) (0 failed)
emulator-5554 - 14 Tests 8/10 completed. (0 skipped) (0 failed)
emulator-5554 - 14 Tests 9/10 completed. (0 skipped) (0 failed)
Finished 10 tests on emulator-5554 - 14
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/__reactivecircus_android-emulator-runner-1787840539732.json

BUILD SUCCESSFUL in 1m 39s
67 actionable tasks: 17 executed, 15 from cache, 35 up-to-date
```

## Walkthrough output

```
[shot] 01-empty: 281043 bytes — Fresh install: empty state, mic button, text entry
[shot] 02-first-expense: 310780 bytes — Typed a sentence: parsed to ₹250 · Food · Saravana Bhavan
[shot] 03-log-list: 372089 bytes — A day of expenses, with the unparsed entry flagged amber
[shot] 04-summary: 355549 bytes — Monthly total, category bars, daily average
[shot] 05-review: 107691 bytes — Review queue: entries the parser was unsure about
[shot] 06-edit: 129963 bytes — Edit sheet: original sentence kept, amount and category fixable
[shot] 07-final: 350307 bytes — Back on the log, review badge cleared where fixed
[shot] 08-voice: 346357 bytes — Mic tapped — recogniser state and fallback
=== PaisaVoice log ===
--------- beginning of main
08-27 14:21:49.113  2715  2715 W PaisaVoice: error 12 on ON_DEVICE (attempt 1)
08-27 14:21:49.114  2715  2715 D PaisaVoice: ready for speech on ON_DEVICE
08-27 14:21:49.313  2715  2715 D PaisaVoice: ready for speech on SYSTEM_SERVICE
08-27 14:21:54.374  2715  2715 W PaisaVoice: error 7 on SYSTEM_SERVICE (attempt 2)
[shot] 09-voice-permission: 253086 bytes — Mic without permission — the system prompt appears
[shot] 10-voice-denied: 362012 bytes — Permission denied — the app says why the mic is needed
walkthrough complete
```
