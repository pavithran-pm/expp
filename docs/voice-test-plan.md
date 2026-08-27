# Voice input — test plan

Scope: everything that happens between tapping the mic and an expense being
saved. The parser itself is covered separately (46 unit tests); this plan is
about the recogniser, its failure modes, permissions, and lifecycle.

## Why this plan exists

A report that "pressing record shows an error" has many possible causes, and
`SpeechRecognizer` reports 15 distinct error codes. Guessing which one fires
is wasted work, so the plan covers each code and each engine explicitly.

## Where each layer runs

| Layer | What it covers | How it runs |
|---|---|---|
| **U** Unit (JVM) | The recovery decision for every error code and engine | `./gradlew test` |
| **I** Instrumented | Real taps on a device: crash-freedom, recovery, permissions | `./gradlew connectedDebugAndroidTest` |
| **E** Emulator script | The visible flow, screenshots, recogniser log | CI emulator job |
| **M** Manual | Anything needing real speech, real network, or a real widget | Physical phone |

## The engines, in order

1. **On-device** — offline, needs a downloaded language pack for `en-IN`.
2. **Bound service** — the usual Google recogniser; needs network.
3. **System dialog** — the OS's own "Speak now" screen; works even where no
   service can be bound.

The chain matters: a phone can report on-device recognition as *available*
while the pack is missing, so attempt 1 fails and the user sees an error
unless the app falls through to the next engine.

---

## Positive scenarios

| ID | Scenario | Expected | Layer |
|---|---|---|---|
| P1 | Mic tapped with permission granted, speech service present | Listening state within 1s; partial text appears while speaking | E, M |
| P2 | Speech recognised | Transcript goes through the *same* parser as typing; row appears; Undo snackbar | I, M |
| P3 | Offline pack missing (`ERROR_LANGUAGE_UNAVAILABLE` on on-device) | Falls back to the bound service without user action | U |
| P4 | Bound service can't do `en-IN` | Falls back to the system dialog | U |
| P5 | No bindable recogniser at all | System dialog is launched instead of an error | U, I |
| P6 | Network lost mid-attempt on the bound service | Retries on-device, with "No network — trying offline recognition" | U |
| P7 | Recogniser busy from a previous attempt | Retries the same engine after teardown | U |
| P8 | Transcript from the system dialog | Parsed and saved like any other input | U, M |
| P9 | Widget mic → speak → save | Expense saved, widget total updated, activity dismisses | M |
| P10 | Mic used repeatedly | Every attempt works; no degradation from a leaked recogniser | I, M |

## Negative scenarios

| ID | Scenario | Expected | Layer |
|---|---|---|---|
| N1 | Permission never granted | System prompt appears; on grant, listening starts | E |
| N2 | Permission denied once | Message explaining why the mic is needed; app stays usable | E |
| N3 | Permission denied permanently ("don't ask again") | Message pointing at Settings › Permissions, not a repeated prompt | I, M |
| N4 | Permission revoked while the app is open | Next tap re-prompts rather than failing silently | E |
| N5 | Nothing said (`ERROR_SPEECH_TIMEOUT`) | "No speech detected"; no retry loop | U |
| N6 | Speech unintelligible (`ERROR_NO_MATCH`) | "Didn't catch that — try again"; no retry loop | U |
| N7 | Mic held by another app (`ERROR_AUDIO`) | "Microphone problem — try again" | U |
| N8 | Rate limited (`ERROR_TOO_MANY_REQUESTS`) | Explains to try again shortly; stops | U |
| N9 | Every engine fails in turn | Stops after 3 attempts with an actionable message — never loops | U |
| N10 | Recogniser never calls back at all | Watchdog fires at 12s and moves down the chain | U (policy), M |
| N11 | Mic tapped twice quickly | No crash, no stuck listening state | I |
| N12 | Screen closed while listening | Recogniser destroyed; mic released | I, M |
| N13 | Unknown/new error code | Reports the code rather than failing silently | U |
| N14 | Empty transcript returned | Treated as "didn't catch that", not saved as an empty expense | U |
| N15 | Airplane mode, no offline pack | Explains it needs either network or the offline pack | M |

## Exit criteria

- Every U row passes in `./gradlew test`.
- Every I row passes in `connectedDebugAndroidTest` on the CI emulator.
- The E rows produce screenshots showing the expected state.
- The M rows are checked once on a physical phone before trusting voice daily.

## What the emulator cannot tell us

The CI emulator image has no usable microphone input, so it can prove the
error paths, the permission flow and the fallback chain, but never that a real
sentence is transcribed correctly. P1, P9 and N15 need a phone.
