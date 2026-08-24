# Emulator run

Pixel 6 profile, API 34, driven through adb + the accessibility tree.

## What the database held afterwards

`amount | category | merchant | needsReview | rawText`

```
/system/bin/sh: syntax error: unexpected '('
```

## Screens

### Fresh install: empty state, mic button, text entry

![01-empty](screenshots/01-empty.png)

### Typed a sentence: parsed to ₹250 · Food · Saravana Bhavan

![02-first-expense](screenshots/02-first-expense.png)

### A day of expenses, with the unparsed entry flagged amber

![03-log-list](screenshots/03-log-list.png)

### Monthly total, category bars, daily average

![04-summary](screenshots/04-summary.png)

### Review queue: entries the parser was unsure about

![05-review](screenshots/05-review.png)

### Edit sheet: original sentence kept, amount and category fixable

![06-edit](screenshots/06-edit.png)

### Back on the log, review badge cleared where fixed

![07-final](screenshots/07-final.png)

### Mic tapped — recogniser state / error handling

![08-voice](screenshots/08-voice.png)

