# QA report

Run against the **release** APK (R8-minified) on a Pixel 6 emulator, API 34.

**Smoke: 13/20 checks passed.**

## Smoke tests

| Check | Result | Detail |
|---|---|---|
| The build under test is installed | pass |  |
| App launches to the log screen | pass |  |
| Typed expense is parsed and saved | pass |  |
| Amount is formatted in Indian rupees | pass |  |
| Quick chip fills the field | pass |  |
| Unparseable input is still saved | pass |  |
| Shorthand amount expands | pass |  |
| Summary tab opens | pass |  |
| Category breakdown is shown | pass |  |
| Review tab opens | pass |  |
| Edit sheet opens on a flagged entry | pass |  |
| Edit sheet saves | pass |  |
| Swipe deletes with an Undo | **FAIL** | row not found |
| Overflow menu offers export and import | **FAIL** |  |
| Mic tap does not crash the app | **FAIL** |  |
| App is still responsive after the mic attempt | **FAIL** |  |
| Survives rotation | **FAIL** |  |
| Data survives a force-stop | **FAIL** |  |
| Scrolling stays under 25% janky frames | warn | 97.62% janky |
| No crashes in the crash buffer | pass | 0 lines |

## Performance

| Metric | Value |
|---|---|
| Cold start (median of 3) | 1442 ms |
| Cold start (worst of 3) | 1477 ms |
| Frames rendered while scrolling | 42 |
| Janky frames | 41 (97.62%) |
| 95th percentile frame time | 150 ms |
| Memory (total PSS) | 29.4 MB |

## Load (10,000 expenses)

| Measurement | Value |
|---|---|
| `bulk_insert_ms_for_10000` | 22155 |
| `single_insert_ms_for_50` | 191 |
| `observe_all_first_emission_ms` | 165 |
| `category_aggregation_ms` | 4 |
| `month_total_ms` | 1 |
| `review_count_ms` | 3 |
| `parse_10000_ms` | 17207 |
| `parse_micros_each` | 1720.7 |
| `burst_100_inserts_ms` | 369 |

## Notes

- The emulator has no microphone input, so the mic checks cover the
  failure and fallback paths only; real transcription needs a phone.
- Jank is measured on an emulator without hardware GPU acceleration,
  so it is an upper bound rather than a phone-representative number.
