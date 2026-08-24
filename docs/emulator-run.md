# Emulator run

Pixel 6 profile, API 34, x86_64. The app is installed from the debug
APK and driven through adb; taps resolve through the accessibility
tree rather than fixed coordinates.

- Scripted walkthrough: **ok**
- Instrumented UI tests: **fail**

## Expenses in the database afterwards

```
(could not read database: no such table: expenses)
```

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

### Mic tapped — recogniser state and error handling

![08-voice](screenshots/08-voice.png)

## Instrumented test output

```
> Task :app:mergeDebugNativeLibs UP-TO-DATE
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
> Task :app:checkDebugAndroidTestDuplicateClasses
> Task :app:mergeDebugAndroidTestJniLibFolders
> Task :app:mergeExtDexDebugAndroidTest FROM-CACHE
> Task :app:mergeLibDexDebugAndroidTest FROM-CACHE
> Task :app:mergeDebugAndroidTestNativeLibs NO-SOURCE
> Task :app:stripDebugAndroidTestDebugSymbols NO-SOURCE
> Task :app:validateSigningDebugAndroidTest
> Task :app:writeDebugAndroidTestSigningConfigVersions
> Task :app:kspDebugAndroidTestKotlin

> Task :app:compileDebugAndroidTestKotlin FAILED
e: file:///home/runner/work/expp/expp/app/src/androidTest/java/com/pavithran/paisa/LoggingFlowTest.kt:59:36 Unresolved reference 'getOrNull'.

> Task :app:copyRoomSchemas NO-SOURCE
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/__reactivecircus_android-emulator-runner-1787595492578.json

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':app:compileDebugAndroidTestKotlin'.
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.GradleCompilerRunnerWithWorkers$GradleKotlinCompilerWorkAction
   > Compilation error. See log for more details

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights.
> Get more help at https://help.gradle.org.

BUILD FAILED in 10s
58 actionable tasks: 13 executed, 10 from cache, 35 up-to-date
```

## Walkthrough output

```
[shot] 01-empty: 83094 bytes — Fresh install: empty state, mic button, text entry
[shot] 02-first-expense: 117585 bytes — Typed a sentence: parsed to ₹250 · Food · Saravana Bhavan
[shot] 03-log-list: 211684 bytes — A day of expenses, with the unparsed entry flagged amber
[shot] 04-summary: 110536 bytes — Monthly total, category bars, daily average
[shot] 05-review: 76589 bytes — Review queue: entries the parser was unsure about
[shot] 06-edit: 108894 bytes — Edit sheet: original sentence kept, amount and category fixable
[shot] 07-final: 195736 bytes — Back on the log, review badge cleared where fixed
[shot] 08-voice: 197564 bytes — Mic tapped — recogniser state / error handling
walkthrough complete
```
