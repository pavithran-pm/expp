# QA report

Run against the **release** APK (R8-minified) on a Pixel 6 emulator, API 34.

**Smoke: 1/19 checks passed.**

## Smoke tests

| Check | Result | Detail |
|---|---|---|
| App launches to the log screen | **FAIL** |  |
| Typed expense is parsed and saved | **FAIL** |  |
| Amount is formatted in Indian rupees | **FAIL** |  |
| Quick chip fills the field | **FAIL** |  |
| Unparseable input is still saved | **FAIL** |  |
| Shorthand amount expands | **FAIL** |  |
| Summary tab opens | **FAIL** |  |
| Category breakdown is shown | **FAIL** |  |
| Review tab opens | **FAIL** |  |
| Edit sheet opens on a flagged entry | **FAIL** |  |
| Edit sheet saves | **FAIL** |  |
| Swipe deletes with an Undo | **FAIL** | row not found |
| Overflow menu offers export and import | **FAIL** |  |
| Mic tap does not crash the app | **FAIL** |  |
| App is still responsive after the mic attempt | **FAIL** |  |
| Survives rotation | **FAIL** |  |
| Data survives a force-stop | **FAIL** |  |
| Scrolling stays under 25% janky frames | warn | no data |
| No crashes in the crash buffer | pass | 0 lines |

## Performance

| Metric | Value |
|---|---|

## Load (10,000 expenses)

| Measurement | Value |
|---|---|
| `bulk_insert_ms_for_10000` | 24949 |
| `single_insert_ms_for_50` | 193 |
| `observe_all_first_emission_ms` | 169 |
| `category_aggregation_ms` | 5 |
| `month_total_ms` | 2 |
| `review_count_ms` | 4 |
| `parse_10000_ms` | 17969 |
| `parse_micros_each` | 1796.9 |
| `burst_100_inserts_ms` | 375 |

## Notes

- The emulator has no microphone input, so the mic checks cover the
  failure and fallback paths only; real transcription needs a phone.
- Jank is measured on an emulator without hardware GPU acceleration,
  so it is an upper bound rather than a phone-representative number.
