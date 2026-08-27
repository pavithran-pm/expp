# Emulator run

Pixel 6 profile, API 34, x86_64. The app is installed from the debug
APK and driven through adb; taps resolve through the accessibility
tree rather than fixed coordinates.

- Scripted walkthrough: **ok**
- Instrumented UI, load and performance tests: **pass**
- Release APK installs: **no**
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
08-27 13:39:08.293  2612  2612 W PaisaVoice: error 12 on ON_DEVICE (attempt 1)
08-27 13:39:08.293  2612  2612 D PaisaVoice: ready for speech on ON_DEVICE
08-27 13:39:08.694  2612  2612 D PaisaVoice: ready for speech on SYSTEM_SERVICE
08-27 13:39:13.728  2612  2612 W PaisaVoice: error 7 on SYSTEM_SERVICE (attempt 2)
```

## Load and performance measurements

```
--------- beginning of main
08-27 13:40:28.429  6902  6923 I PaisaPerf: bulk_insert_ms_for_10000=24949
08-27 13:40:28.622  6902  6923 I PaisaPerf: single_insert_ms_for_50=193
08-27 13:40:28.791  6902  6923 I PaisaPerf: observe_all_first_emission_ms=169
08-27 13:40:28.796  6902  6923 I PaisaPerf: category_aggregation_ms=5
08-27 13:40:28.798  6902  6923 I PaisaPerf: month_total_ms=2
08-27 13:40:28.802  6902  6923 I PaisaPerf: review_count_ms=4
08-27 13:40:46.778  6902  6923 I PaisaPerf: parse_10000_ms=17969
08-27 13:40:46.779  6902  6923 I PaisaPerf: parse_micros_each=1796.9
08-27 13:40:50.587  6902  6923 I PaisaPerf: burst_100_inserts_ms=375
```

## Smoke output

```
[FAIL] App launches to the log screen 
[FAIL] Typed expense is parsed and saved 
[FAIL] Amount is formatted in Indian rupees 
[FAIL] Quick chip fills the field 
[FAIL] Unparseable input is still saved 
[FAIL] Shorthand amount expands 
[FAIL] Summary tab opens 
[FAIL] Category breakdown is shown 
[FAIL] Review tab opens 
[FAIL] Edit sheet opens on a flagged entry 
[FAIL] Edit sheet saves 
[FAIL] Swipe deletes with an Undo row not found
[FAIL] Overflow menu offers export and import 
[FAIL] Mic tap does not crash the app 
[FAIL] App is still responsive after the mic attempt 
[FAIL] Survives rotation 
[FAIL] Data survives a force-stop 
[FAIL] Scrolling stays under 25% janky frames no data
[PASS] No crashes in the crash buffer 0 lines
```

## Instrumented test output

```
> Task :app:kspDebugKotlin
> Task :app:compileDebugKotlin UP-TO-DATE
> Task :app:compileDebugJavaWithJavac NO-SOURCE
> Task :app:dexBuilderDebug UP-TO-DATE
> Task :app:mergeDebugGlobalSynthetics UP-TO-DATE
> Task :app:processDebugJavaRes UP-TO-DATE
> Task :app:mergeDebugJavaResource UP-TO-DATE
> Task :app:mergeProjectDexDebug UP-TO-DATE
> Task :app:packageDebug UP-TO-DATE
> Task :app:createDebugApkListingFileRedirect UP-TO-DATE
> Task :app:mergeLibDexDebugAndroidTest FROM-CACHE
> Task :app:bundleDebugClassesToCompileJar
> Task :app:mergeDebugAndroidTestNativeLibs NO-SOURCE
> Task :app:stripDebugAndroidTestDebugSymbols NO-SOURCE
> Task :app:validateSigningDebugAndroidTest
> Task :app:writeDebugAndroidTestSigningConfigVersions
> Task :app:mergeExtDexDebugAndroidTest
> Task :app:kspDebugAndroidTestKotlin
> Task :app:compileDebugAndroidTestKotlin
> Task :app:compileDebugAndroidTestJavaWithJavac NO-SOURCE
> Task :app:copyRoomSchemas NO-SOURCE
> Task :app:dexBuilderDebugAndroidTest
> Task :app:mergeDebugAndroidTestGlobalSynthetics FROM-CACHE
> Task :app:processDebugAndroidTestJavaRes
> Task :app:mergeProjectDexDebugAndroidTest
> Task :app:mergeDebugAndroidTestJavaResource
> Task :app:packageDebugAndroidTest
> Task :app:createDebugAndroidTestApkListingFileRedirect
[EmulatorConsole]: Failed to start Emulator console for 5554

> Task :app:connectedDebugAndroidTest
Starting 10 tests on emulator-5554 - 14

emulator-5554 - 14 Tests 0/10 completed. (0 skipped) (0 failed)
emulator-5554 - 14 Tests 1/10 completed. (0 skipped) (0 failed)
emulator-5554 - 14 Tests 3/10 completed. (0 skipped) (0 failed)
emulator-5554 - 14 Tests 5/10 completed. (0 skipped) (0 failed)
emulator-5554 - 14 Tests 7/10 completed. (0 skipped) (0 failed)
emulator-5554 - 14 Tests 8/10 completed. (0 skipped) (0 failed)
emulator-5554 - 14 Tests 9/10 completed. (0 skipped) (0 failed)
Finished 10 tests on emulator-5554 - 14
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/__reactivecircus_android-emulator-runner-1787837978183.json

BUILD SUCCESSFUL in 2m 3s
66 actionable tasks: 27 executed, 4 from cache, 35 up-to-date
```

## Walkthrough output

```
[shot] 01-empty: 281115 bytes — Fresh install: empty state, mic button, text entry
[shot] 02-first-expense: 310446 bytes — Typed a sentence: parsed to ₹250 · Food · Saravana Bhavan
[shot] 03-log-list: 373987 bytes — A day of expenses, with the unparsed entry flagged amber
[shot] 04-summary: 356463 bytes — Monthly total, category bars, daily average
[shot] 05-review: 108305 bytes — Review queue: entries the parser was unsure about
[shot] 06-edit: 130746 bytes — Edit sheet: original sentence kept, amount and category fixable
[shot] 07-final: 352189 bytes — Back on the log, review badge cleared where fixed
[shot] 08-voice: 348153 bytes — Mic tapped — recogniser state and fallback
=== PaisaVoice log ===
--------- beginning of main
08-27 13:39:08.293  2612  2612 W PaisaVoice: error 12 on ON_DEVICE (attempt 1)
08-27 13:39:08.293  2612  2612 D PaisaVoice: ready for speech on ON_DEVICE
08-27 13:39:08.694  2612  2612 D PaisaVoice: ready for speech on SYSTEM_SERVICE
08-27 13:39:13.728  2612  2612 W PaisaVoice: error 7 on SYSTEM_SERVICE (attempt 2)
[shot] 09-voice-permission: 253988 bytes — Mic without permission — the system prompt appears
[shot] 10-voice-denied: 363877 bytes — Permission denied — the app says why the mic is needed
walkthrough complete
```
