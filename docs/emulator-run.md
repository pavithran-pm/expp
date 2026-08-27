# Emulator run

Pixel 6 profile, API 34, x86_64. The app is installed from the debug
APK and driven through adb; taps resolve through the accessibility
tree rather than fixed coordinates.

- Scripted walkthrough: **ok**
- Instrumented UI, load and performance tests: **pass**
- Release APK installs: **yes**
- Smoke pass on the release build: **pass** (see docs/qa-report.md)

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
08-27 15:47:58.808  2947  2947 W PaisaVoice: error 12 on ON_DEVICE (attempt 1)
08-27 15:47:59.308  2947  2947 D PaisaVoice: ready for speech on SYSTEM_SERVICE
08-27 15:48:04.369  2947  2947 W PaisaVoice: error 7 on SYSTEM_SERVICE (attempt 2)
```

## Load and performance measurements

```
--------- beginning of main
08-27 15:49:06.970  7062  7082 I PaisaPerf: bulk_insert_ms_for_10000=23845
08-27 15:49:07.173  7062  7082 I PaisaPerf: single_insert_ms_for_50=203
08-27 15:49:07.359  7062  7082 I PaisaPerf: observe_all_first_emission_ms=185
08-27 15:49:07.363  7062  7082 I PaisaPerf: category_aggregation_ms=4
08-27 15:49:07.367  7062  7082 I PaisaPerf: month_total_ms=1
08-27 15:49:07.371  7062  7082 I PaisaPerf: review_count_ms=3
08-27 15:49:26.252  7062  7082 I PaisaPerf: parse_10000_ms=18874
08-27 15:49:26.252  7062  7082 I PaisaPerf: parse_micros_each=1887.4
08-27 15:49:30.383  7062  7082 I PaisaPerf: burst_100_inserts_ms=414
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
[PASS] Log tab still lists the expenses 
[PASS] Overflow menu offers export and import 
[PASS] Overflow menu closes again 
[PASS] Still in the app after the menu 
[PASS] Mic tap does not crash the app 
[PASS] App is still responsive after the mic attempt 
[PASS] Survives rotation 
[PASS] Data survives a force-stop 
[PASS] Frame timing recorded while scrolling 90.48% janky — software rendering on the emulator
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
emulator-5554 - 14 Tests 3/10 completed. (0 skipped) (0 failed)
emulator-5554 - 14 Tests 5/10 completed. (0 skipped) (0 failed)
emulator-5554 - 14 Tests 7/10 completed. (0 skipped) (0 failed)
emulator-5554 - 14 Tests 8/10 completed. (0 skipped) (0 failed)
emulator-5554 - 14 Tests 10/10 completed. (0 skipped) (0 failed)
Finished 10 tests on emulator-5554 - 14
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/__reactivecircus_android-emulator-runner-1787845708845.json

BUILD SUCCESSFUL in 1m 47s
67 actionable tasks: 17 executed, 15 from cache, 35 up-to-date
```

