# QA report

Run against the **release** APK (R8-minified) on a Pixel 6 emulator, API 34.

**Smoke: 23/23 checks passed.**

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
| Still in the app after editing | pass |  |
| Log tab still lists the expenses | pass |  |
| Overflow menu offers export and import | pass |  |
| Overflow menu closes again | pass |  |
| Still in the app after the menu | pass |  |
| Mic tap does not crash the app | pass |  |
| App is still responsive after the mic attempt | pass |  |
| Survives rotation | pass |  |
| Data survives a force-stop | pass |  |
| Frame timing recorded while scrolling | pass | 92.31% janky — software rendering on the emulator |
| No crashes in the crash buffer | pass | 0 lines |

## Performance

| Metric | Value |
|---|---|
| Cold start (median of 3) | 1458 ms |
| Cold start (worst of 3) | 1627 ms |
| Frames rendered while scrolling | 39 |
| Janky frames | 36 (92.31%) |
| 95th percentile frame time | 150 ms |
| Memory (total PSS) | 28.9 MB |

## Load (10,000 expenses)

| Measurement | Value |
|---|---|
| `bulk_insert_ms_for_10000` | 4578 |
| `single_insert_ms_for_50` | 107 |
| `observe_all_first_emission_ms` | 157 |
| `category_aggregation_ms` | 4 |
| `month_total_ms` | 1 |
| `review_count_ms` | 4 |
| `parse_10000_ms` | 3117 |
| `parse_micros_each` | 311.7 |
| `burst_100_inserts_ms` | 188 |

## Notes

- The emulator has no microphone input, so the mic checks cover the
  failure and fallback paths only; real transcription needs a phone.
- Jank is measured on an emulator without hardware GPU acceleration,
  so it is an upper bound rather than a phone-representative number.
