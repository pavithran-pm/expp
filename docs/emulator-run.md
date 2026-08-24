# Emulator run

Pixel 6 profile, API 34, x86_64. The app is installed from the debug
APK and driven through adb; taps resolve through the accessibility
tree rather than fixed coordinates.

- Scripted walkthrough: **ok**
- Instrumented UI tests: **fail**

## Expenses in the database afterwards

```
(could not read database: file is not a database)
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
> Task :app:createDebugApkListingFileRedirect UP-TO-DATE
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
Starting 4 tests on emulator-5554 - 14

emulator-5554 - 14 Tests 1/4 completed. (0 skipped) (0 failed)

> Task :app:connectedDebugAndroidTest

com.pavithran.paisa.LoggingFlowTest > inputFieldClearsAfterLogging[emulator-5554 - 14] [31mFAILED [0m
	java.lang.AssertionError: Failed to assert the following: (EditableText = '')
	Semantics of the node:
Tests on emulator-5554 - 14 failed: There was 1 failure(s).

Finished 4 tests on emulator-5554 - 14

> Task :app:connectedDebugAndroidTest FAILED
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/__reactivecircus_android-emulator-runner-1787594570642.json

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':app:connectedDebugAndroidTest'.
> There were failing tests. See the report at: file:///home/runner/work/expp/expp/app/build/reports/androidTests/connected/debug/index.html

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights.
> Get more help at https://help.gradle.org.

BUILD FAILED in 1m 2s
66 actionable tasks: 27 executed, 4 from cache, 35 up-to-date
```

## Walkthrough output

```
[shot] 01-empty: 82243 bytes — Fresh install: empty state, mic button, text entry
[shot] 02-first-expense: 117871 bytes — Typed a sentence: parsed to ₹250 · Food · Saravana Bhavan
[shot] 03-log-list: 212213 bytes — A day of expenses, with the unparsed entry flagged amber
[shot] 04-summary: 110142 bytes — Monthly total, category bars, daily average
[shot] 05-review: 77727 bytes — Review queue: entries the parser was unsure about
[shot] 06-edit: 109941 bytes — Edit sheet: original sentence kept, amount and category fixable
[shot] 07-final: 195932 bytes — Back on the log, review badge cleared where fixed
[shot] 08-voice: 198183 bytes — Mic tapped — recogniser state / error handling
walkthrough complete
```
