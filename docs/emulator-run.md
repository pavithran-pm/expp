# Emulator run

Pixel 6 profile, API 34, x86_64. The app is installed from the debug
APK and driven through adb; taps resolve through the accessibility
tree rather than fixed coordinates.

- Scripted walkthrough: **failed**
- Instrumented UI tests: **fail**

## Expenses in the database afterwards

```
(database not pulled)
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
(/tmp/androidtest.log not produced)
```

## Walkthrough output

```
[shot] 01-empty: 82656 bytes — Fresh install: empty state, mic button, text entry
[shot] 02-first-expense: 117634 bytes — Typed a sentence: parsed to ₹250 · Food · Saravana Bhavan
[shot] 03-log-list: 211136 bytes — A day of expenses, with the unparsed entry flagged amber
[shot] 04-summary: 110078 bytes — Monthly total, category bars, daily average
[shot] 05-review: 77103 bytes — Review queue: entries the parser was unsure about
[shot] 06-edit: 109379 bytes — Edit sheet: original sentence kept, amount and category fixable
[shot] 07-final: 195588 bytes — Back on the log, review badge cleared where fixed
[shot] 08-voice: 212058 bytes — Mic tapped — recogniser state / error handling
walkthrough complete
```
