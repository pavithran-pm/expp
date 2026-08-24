# Voice Expense Tracker — Module-Wise Development Plan

**Target:** Android app, Kotlin + Jetpack Compose, 100% offline, ₹0 running cost.
**Rule of this plan:** every module ends with an installable APK on your phone that is strictly more useful than the previous one.

---

## How to read this plan

Each module has the same six parts:

| Part | What it means |
|---|---|
| **Goal** | One sentence. What this module adds. |
| **Tasks** | Numbered, concrete steps in build order. |
| **Technical notes** | The specific gotchas and API details you need. |
| **Acceptance test** | Do this on your physical phone. If it passes, module is done. |
| **APK checkpoint** | The build command, and what you can actually *do* with this build. |
| **Time** | Realistic hours for someone comfortable with Kotlin. Double it if you're learning. |

---

## Non-negotiable rules for every module

1. **Never end a session with a broken build.** If you're mid-refactor at 11pm, `git stash` it.
2. **Commit at the end of every module** with the tag `m0`, `m1`, `m2`… so you can always roll back to a working APK.
3. **Bump `versionCode` every module.** It lets you install over the previous build without uninstalling, so you keep your data.
4. **Install on your real phone, not just the emulator.** The emulator's microphone and speech recognition behave differently from a real device.
5. **From Module 4 onward you have real expense data.** Stop using destructive database migrations at that point (see M6 notes).

---

# Module 0 — Foundation & Your First APK

**Goal:** Prove the entire toolchain works end-to-end before writing a single line of app logic.

Most abandoned Android projects die here, not at the hard parts. Get this bulletproof first.

### Tasks

1. Install **Android Studio** (latest stable). Let it download the SDK and command-line tools.
2. Create a new project → **Empty Activity (Compose)** template.
   - Package name: `com.yourname.paisa` (pick something you won't want to change — it's painful later)
   - Minimum SDK: **API 26 (Android 8.0)** — covers effectively every phone in use, and gives you modern `java.time` APIs
3. Accept whatever library versions the template ships with. Do not hand-pick versions; the template's Compose BOM is already internally consistent.
4. Enable **KSP** in your Gradle files (you'll need it for Room in M1). Not `kapt` — it's deprecated and roughly twice as slow.
5. Change the default "Hello Android" text to "Paisa" so you can visually confirm you're running *your* build.
6. Enable **Developer Options** and **USB Debugging** on your phone, connect it, and run.
7. Build a standalone debug APK and install it manually — don't rely only on Android Studio's Run button.
8. `git init`, add the standard Android `.gitignore`, commit as `m0`.

### Technical notes

- The debug APK lands at `app/build/outputs/apk/debug/app-debug.apk`
- Transfer it to your phone however you like (USB, Drive, email to yourself) and tap to install. You'll need to allow "install from unknown sources" once.
- Debug APKs are signed with a throwaway debug key. That's fine for personal use for the entire project — you only need a real keystore in M10 if you ever want to publish.

### Acceptance test

Unplug the USB cable. The app icon is on your home screen. Tap it. It opens and shows "Paisa". Close it and reopen it.

### APK checkpoint

```bash
./gradlew assembleDebug
```

**You now have:** a real app on your phone with your name on it. It does nothing. That's fine — the pipeline works, and that's the thing that usually breaks.

### Time

**2–4 hours** (mostly downloads and SDK setup on the first run)

### Common failure

Gradle sync fails behind a corporate or spotty network. Let it retry; don't start manually editing version numbers to "fix" it. That path leads to dependency hell.

---

# Module 1 — The Data Layer

**Goal:** Expenses can be stored and read back from a local database. Still no real UI.

### Tasks

1. Add **Room** dependencies (`room-runtime`, `room-ktx`, and `room-compiler` via KSP).
2. Create the entity:

```kotlin
@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,          // System.currentTimeMillis()
    val rawText: String,          // exactly what you said or typed
    val amount: Double,
    val category: String,
    val merchant: String? = null,
    val needsReview: Boolean = false
)
```

3. Create the DAO:

```kotlin
@Dao
interface ExpenseDao {
    @Insert suspend fun insert(expense: Expense): Long
    @Update suspend fun update(expense: Expense)
    @Delete suspend fun delete(expense: Expense)

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE needsReview = 1 ORDER BY timestamp DESC")
    fun observeNeedsReview(): Flow<List<Expense>>
}
```

4. Create the `@Database` class as a singleton.
5. Add a temporary debug button on screen: "Add fake expense" → inserts a hardcoded row.
6. Display the list below it using `collectAsStateWithLifecycle()`.

### Technical notes

- **Why `rawText` matters:** it is your safety net. If the parser gets something wrong six months from now, the original sentence is still there and you can fix it. Never make this column optional.
- **Why `needsReview` matters:** it lets the app save an entry it doesn't fully understand rather than rejecting it. An expense tracker that refuses input is an expense tracker you abandon.
- Store timestamps as `Long` epoch millis, not formatted strings. Formatting is a display concern.
- Return `Flow` from queries, not `List`. The UI then updates automatically on insert with no manual refresh code.
- During M1–M5, `.fallbackToDestructiveMigration()` is acceptable because you have no real data yet.

### Acceptance test

Tap "Add fake expense" three times → three rows appear. **Force-close the app and reopen it** → the three rows are still there. That last step is the actual test.

### APK checkpoint

```bash
./gradlew assembleDebug
```

**You now have:** working persistent storage. Ugly, but real.

### Time

**5–7 hours**

---

# Module 2 — The Parser (the heart of the app)

**Goal:** A pure Kotlin function that turns "spent 250 on lunch at Saravana Bhavan" into structured data. **No UI work in this module at all.**

This is the module that determines whether you'll still be using the app in three months. Give it the attention it deserves.

### Tasks

1. Create `ExpenseParser.kt` as a plain object with **zero Android dependencies**. This makes it unit-testable without a device.

```kotlin
data class ParseResult(
    val amount: Double?,
    val category: String,
    val merchant: String?,
    val confident: Boolean
)

object ExpenseParser {
    fun parse(input: String): ParseResult { ... }
}
```

2. **Write the tests first.** Create ~25 test cases from sentences you would actually say. Run them; watch them all fail. Then build the parser until they pass.

3. Implement amount extraction, in this priority order:
   - Plain digits: `250`, `1200`
   - Decimal: `99.50`
   - Shorthand: `1.2k` → 1200, `2k` → 2000
   - Word numbers: `hundred` → 100, `fifty` → 50, `two fifty` → 250, `five hundred` → 500
   - Strip currency noise: `rupees`, `rs`, `₹`, `bucks`

4. Implement category matching via a keyword map:

```kotlin
val categoryKeywords = mapOf(
    "Food"      to listOf("lunch","dinner","breakfast","tea","chai","coffee",
                          "snack","hotel","mess","restaurant","swiggy","zomato",
                          "biryani","meals","tiffin"),
    "Transport" to listOf("petrol","diesel","fuel","auto","uber","ola","rapido",
                          "bus","train","cab","parking","toll","metro"),
    "Groceries" to listOf("groceries","vegetables","milk","provision","supermarket",
                          "bigbasket","dmart"),
    "Shopping"  to listOf("amazon","flipkart","clothes","shirt","shoes","myntra"),
    "Health"    to listOf("medicine","pharmacy","doctor","hospital","apollo","tablets"),
    "Bills"     to listOf("recharge","bill","electricity","water","rent","emi",
                          "internet","wifi","gas","dth"),
    "Personal"  to listOf("haircut","salon","gym","movie","ticket")
)
```

5. Merchant extraction: everything left after removing the number, the matched category keyword, and filler words (`spent`, `on`, `at`, `for`, `paid`, `rupees`, `today`).

6. Confidence rule — **this is the important logic**:
   - No amount found → `confident = false`, amount stays `null`
   - Amount found but no category matched → category `"Other"`, `confident = false`
   - Both found → `confident = true`

7. Normalise input before parsing: lowercase, trim, collapse multiple spaces.

### Test cases to start with

| Input | Expected amount | Expected category |
|---|---|---|
| `250 lunch` | 250 | Food |
| `spent 250 on lunch at saravana bhavan` | 250 | Food |
| `two fifty lunch` | 250 | Food |
| `1.2k petrol` | 1200 | Transport |
| `auto 80` | 80 | Transport |
| `chai 20` | 20 | Food |
| `swiggy 450` | 450 | Food |
| `recharge 299` | 299 | Bills |
| `medicine 150 apollo` | 150 | Health |
| `paid 500 rupees for groceries` | 500 | Groceries |
| `hundred rupees tea` | 100 | Food |
| `2k rent` | 2000 | Bills |
| `blah blah` | null | Other (not confident) |
| `500` | 500 | Other (not confident) |

### Technical notes

- **Regional trap:** in Indian usage "hotel" usually means a restaurant, not accommodation. Keep it under Food. You'll find several of these — that's why Module 5 exists.
- **Order matters:** extract the amount *first*, then remove it from the string before doing category and merchant matching. Otherwise "2k rent" tries to match "2k" as a merchant.
- Match category keywords against **word boundaries**, not substrings. Otherwise "auto" matches inside "automobile" and worse cases.
- Keep this file free of `Context`, `Log`, or any Android import. If you ever need to debug it, you want to run it as a plain JVM test in under a second.

### Acceptance test

`./gradlew test` — all ~25 unit tests pass. Nothing to see on the phone this module.

### APK checkpoint

APK is unchanged from M1 (the parser isn't wired up yet). **This is the only module without a visible improvement, and it's the most valuable one.** Don't skip it or fold it into M3.

### Time

**6–8 hours**

---

# Module 3 — Text Entry (first genuinely usable build)

**Goal:** Type a sentence, get a correctly parsed expense saved. The app becomes real here.

### Tasks

1. Build the main screen in Compose:
   - A text field at the top
   - A "Log" button (and handle the keyboard's Done action too)
   - A scrolling list of recent expenses below
2. Wire: text field → `ExpenseParser.parse()` → build `Expense` → `dao.insert()`
3. **The critical save rule:** always save. If `confident == false`, save with `needsReview = true` and `amount = 0.0` if no amount was found. Never show an error and discard the input.
4. Show a confirmation **snackbar** after each save: `₹250 · Food · Saravana Bhavan` with an **Undo** action.
5. Give rows with `needsReview = true` a visible marker — an amber dot or left border.
6. Add a `ViewModel` holding the UI state. Don't put database calls in composables.
7. Format amounts in Indian style: `₹1,25,000` not `₹125,000`. Use `NumberFormat.getCurrencyInstance(Locale("en","IN"))`.

### Technical notes

- Use `LazyColumn` for the list, with `key = { it.id }` so recomposition stays cheap as the list grows.
- The snackbar with Undo is doing real work: it turns "did that save correctly?" anxiety into a visible answer. Don't replace it with a Confirm dialog — confirmation dialogs are the friction that kills daily use.
- Show a date header ("Today", "Yesterday", then dates) between groups. Cheap to build, makes the list far more readable.

### Acceptance test

Type `250 lunch at saravana bhavan` → tap Log → row appears reading ₹250, Food, Saravana Bhavan. Type `blah` → a row still appears, flagged for review, nothing crashes.

### APK checkpoint

```bash
./gradlew assembleDebug
```

**You now have:** a working expense tracker. Genuinely usable. If you stopped here you'd have something better than a notes app.

### Time

**8–10 hours**

---

# Module 4 — Voice Input

**Goal:** Tap the mic, speak, expense is logged. This is the feature you actually wanted.

### Tasks

1. Add to `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<queries>
    <intent><action android:name="android.speech.RecognitionService" /></intent>
</queries>
```

2. Request `RECORD_AUDIO` **at runtime** using `rememberLauncherForActivityResult`. Handle the denial case with a message explaining why it's needed.
3. Add a large mic FAB — bottom right, thumb-reachable, at least 64dp.
4. Implement `SpeechRecognizer`:
   - `SpeechRecognizer.createSpeechRecognizer(context)`
   - Intent with `ACTION_RECOGNIZE_SPEECH`, `LANGUAGE_MODEL_FREE_FORM`, and `EXTRA_LANGUAGE` set to `en-IN`
   - Set `EXTRA_PARTIAL_RESULTS = true` so you can show live text while speaking
5. Feed the final transcript into the **exact same** `ExpenseParser.parse()` from M2. No separate voice parsing path, ever.
6. Show visual state clearly: idle → listening (pulsing) → processing → saved.
7. **Handle every error callback explicitly:**

| Error | What to show |
|---|---|
| `ERROR_NO_MATCH` | "Didn't catch that — try again" |
| `ERROR_SPEECH_TIMEOUT` | "No speech detected" |
| `ERROR_INSUFFICIENT_PERMISSIONS` | Re-prompt for permission |
| `ERROR_NETWORK` | Fall back gracefully (see notes) |
| `ERROR_RECOGNIZER_BUSY` | Destroy and recreate the recognizer |

8. Always call `recognizer.destroy()` in `onDispose`. Leaking it holds the microphone and breaks the next attempt.

### Technical notes

- **The silent-failure trap:** `SpeechRecognizer` fails quietly if you don't implement `onError`. You'll tap the mic, nothing happens, and you'll have no idea why. Implement every callback with at least a log line before you test anything.
- **Offline recognition:** on API 31+ you can use `SpeechRecognizer.createOnDeviceSpeechRecognizer()` for fully offline speech. It requires the language pack to be downloaded on the device. Best approach: try on-device first, fall back to the standard recognizer. Add this as a refinement *after* the basic path works — not on the first attempt.
- Set the language to `en-IN` explicitly. The default locale handles Indian English words and accents noticeably worse.
- **Auto-save after 2 seconds** with an Undo snackbar. Do not require a Confirm tap. Speaking plus confirming is slower than typing, which defeats the entire purpose.
- Test in a noisy environment — a real shop, not your quiet room. That's where you'll be using it.

### Acceptance test

In a moderately noisy room, tap mic → say "two fifty lunch at Saravana Bhavan" → within ~3 seconds a correct row appears. Do this ten times with different sentences. Note every failure — you'll need that list for Module 5.

### APK checkpoint

```bash
./gradlew assembleDebug
```

**You now have:** the app you set out to build. **Stop developing and go to Module 5.**

### Time

**8–10 hours**

---

# Module 5 — Use It For One Week (write no code)

**Goal:** Find out what's actually wrong before building more.

This is a real module. Put it on your calendar.

### Tasks

1. Log every expense for **seven consecutive days**. Every one, including ₹10 chai.
2. Keep a running note of every parser miss: what you said, what it recorded.
3. At the end of the week, sit down for one hour and:
   - Add the missed keywords to your category map
   - Add unit tests for each failure so it can never regress
   - Fix any amount-parsing patterns that tripped it up
4. Honestly answer: **did you keep using it?** If you stopped by day 3, the problem is friction or habit, not features. Diagnose that before writing more code.

### Technical notes

Typical week-one findings, based on how these apps usually fail:

- Local merchant names transcribe badly. Fix by matching on partial/fuzzy names, or accept it and edit occasionally.
- You speak in Tamil-English mix more than you expected. Add those words directly to the keyword map — the parser doesn't care what language a keyword is.
- Numbers spoken quickly ("twoifty") get mangled. Add the specific mis-transcriptions you actually see as aliases.
- The mic button is one tap too far away. That's what Module 8 fixes.

### Acceptance test

Your parser accuracy on real sentences goes from roughly 70% to 90%+. Your test suite has grown to 40+ cases.

### APK checkpoint

Same APK, meaningfully smarter parser.

### Time

**7 days elapsed, ~2 hours of actual work**

---

# Module 6 — Edit, Delete & Review Queue

**Goal:** Fix mistakes without touching the database by hand.

### Tasks

1. Tap a row → edit sheet with amount, category (dropdown), merchant, and date. Show the original `rawText` at the top, read-only.
2. Saving an edit sets `needsReview = false`.
3. Swipe to delete, with an Undo snackbar.
4. Add a **Review** screen listing only `needsReview = true` rows, with a badge count on the main screen.
5. Add a manual date picker for logging past expenses ("I forgot Tuesday's auto fare").

### Technical notes

- **Switch to real Room migrations now.** You have a week of real data. If you add a column with `fallbackToDestructiveMigration()` still enabled, Room silently wipes your table. Write a proper `Migration(1, 2)` and set `exportSchema = true`.
- The review queue is what makes the "always save, flag if unsure" rule pay off. Without it, flagged rows just accumulate invisibly.

### Acceptance test

Log a deliberately broken entry, find it in Review, fix it, confirm the badge count drops to zero.

### APK checkpoint

```bash
./gradlew assembleDebug
```

**You now have:** a trustworthy app. Mistakes are recoverable.

### Time

**6–8 hours**

---

# Module 7 — Summary & Insights

**Goal:** See where the money went. This is the payoff that makes logging feel worthwhile.

### Tasks

1. Add a Summary tab (bottom navigation, two tabs: Log / Summary).
2. Show for the current month:
   - Total spent
   - Breakdown by category, sorted descending, with amount and percentage
   - Daily average
3. Add a month selector to view previous months.
4. Add a simple horizontal bar per category. **Skip the pie chart** — a bar list is easier to read and needs no charting library.
5. Add today's total to the main screen header.

### Technical notes

- Do the aggregation in SQL with `GROUP BY category`, not in Kotlin. It's faster and it's less code.
- Resist adding a charting library for this. Compose can draw a proportional-width `Box` in three lines, and you avoid a dependency you'd have to maintain.
- Resist adding budgets and goals here. Every feature you add is another thing that can break and another screen between you and logging.

### Acceptance test

After two weeks of data, the summary total matches a manual sum of your rows. Check this once — aggregation bugs are easy to write and hard to notice.

### APK checkpoint

```bash
./gradlew assembleDebug
```

**You now have:** actual insight, not just a list.

### Time

**5–7 hours**

---

# Module 8 — Home Screen Widget (the habit-maker)

**Goal:** One tap from home screen to speaking. This is what makes the app stick long-term.

### Tasks

1. Add **Jetpack Glance** (`androidx.glance:glance-appwidget`) — it lets you write widgets in Compose-style code instead of the old `RemoteViews` API.
2. Build a small 2×1 widget: a mic icon plus today's total.
3. Tapping it launches a transparent activity that starts listening immediately — no main screen in between.
4. Auto-dismiss after saving, with a toast confirmation.
5. Update the widget's displayed total after each save.

### Technical notes

- This module has the highest ratio of habit-value to code. The difference between "unlock, find app, open, tap mic" and "unlock, tap widget, speak" is what determines whether you're still using this in month three.
- **Battery optimisation** can prevent widget updates on some Android skins — MIUI, ColorOS and OneUI are the usual offenders. Test after leaving the phone idle overnight. You may need to whitelist your app manually; document that in your own notes.
- The transparent launcher activity needs `android:theme="@style/Theme.Translucent.NoTitleBar"` and `android:excludeFromRecents="true"`.

### Acceptance test

From a locked phone: unlock → tap widget → speak → expense saved. Under 5 seconds total.

### APK checkpoint

```bash
./gradlew assembleDebug
```

**You now have:** near-zero-friction logging.

### Time

**6–8 hours**

---

# Module 9 — Export & Backup

**Goal:** Your data can survive a lost phone. Do not skip this one.

### Tasks

1. Add **CSV export**: all columns including `rawText`, written via the Storage Access Framework (`ACTION_CREATE_DOCUMENT`) so you can save to Drive or Downloads.
2. Add **CSV import** so a restore actually works. Export without import is only half a backup.
3. Add a monthly reminder notification: "Export your data" on the 1st.
4. Enable Android's built-in cloud backup in the manifest:

```xml
android:allowBackup="true"
android:fullBackupContent="@xml/backup_rules"
```

### Technical notes

- **Room data is deleted when the app is uninstalled.** One accidental uninstall, or one new phone, and a year of data is gone. This module is insurance you will eventually be glad you bought.
- Use the Storage Access Framework, not direct file paths — scoped storage on Android 10+ blocks the old approach.
- Quote CSV fields properly. Merchant names contain commas more often than you'd think.
- Test the import by exporting, clearing app data, then importing. An untested restore is not a backup.

### Acceptance test

Export → uninstall the app → reinstall → import → all data is back, correctly.

### APK checkpoint

```bash
./gradlew assembleDebug
```

**You now have:** a durable app. Feature-complete for personal use.

### Time

**5–6 hours**

---

# Module 10 — Release Build & Polish

**Goal:** A proper signed APK you can reinstall for years without losing data.

### Tasks

1. Generate a **release keystore**:

```bash
keytool -genkey -v -keystore paisa-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 -alias paisa
```

2. **Back up that keystore file and its password in two separate places.** If you lose it, you can never install an updating build over your existing app — you'd have to uninstall, losing data.
3. Store credentials in `local.properties` (git-ignored), never in `build.gradle`.
4. Configure the release signing config and enable minification:

```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        signingConfig = signingConfigs.getByName("release")
    }
}
```

5. Add a proper app icon (Android Studio's Image Asset tool).
6. Polish pass: app name, dark mode check, empty states, rotation handling.
7. Build and install the release APK.

### Technical notes

- **Test the release build thoroughly.** R8 minification can strip things that reflection depends on. Room is generally safe, but verify every screen works after minifying — this is the classic "worked in debug, broken in release" trap.
- The release APK is at `app/build/outputs/apk/release/app-release.apk`.
- You're installing this yourself, so you never need Play Store or Play Console. No fees, no review, no policy compliance.
- Installing the release build over your debug build **will fail** (different signatures). Export your data first, uninstall, install release, import. Do this once and you're on the release track permanently.

### Acceptance test

Release APK installs on a phone that has never had Android Studio connected to it. Every feature works. Data imports correctly.

### APK checkpoint

```bash
./gradlew assembleRelease
```

**You now have:** your finished app.

### Time

**4–6 hours**

---

# Summary Timeline

| Module | What you get | Hours |
|---|---|---|
| M0 Foundation | App on phone (does nothing) | 2–4 |
| M1 Data layer | Persistent storage | 5–7 |
| M2 Parser | Tested parsing logic | 6–8 |
| M3 Text entry | **Genuinely usable app** | 8–10 |
| M4 Voice | **The app you wanted** | 8–10 |
| M5 Real usage | 90%+ accuracy | 2 (+7 days) |
| M6 Edit & review | Trustworthy | 6–8 |
| M7 Summary | Insight | 5–7 |
| M8 Widget | **Habit-forming** | 6–8 |
| M9 Export | Durable | 5–6 |
| M10 Release | Finished | 4–6 |
| | **Total** | **~57–80 hrs** |

**Realistic calendar:** 8–10 weekends, or about 6–8 weeks at 8 hours a week.

**The three modules that matter most:** M2 (parser quality decides everything), M5 (tells you the truth about your app), M8 (decides whether you keep using it).

---

# Explicitly Out of Scope for v1

Every one of these is a week you don't spend using the app. Add them later, or never:

- User accounts and login
- Cloud sync across devices
- Budgets, goals and alerts
- SMS/UPI auto-reading (needs sensitive permissions and per-bank parsing)
- Receipt photo scanning
- Multi-currency
- Income tracking
- Charts beyond simple bars
- Recurring transactions
- Play Store publication

---

# If You Get Stuck

| Symptom | Likely cause |
|---|---|
| Gradle sync fails | Network. Retry; don't edit versions manually. |
| Room compile errors | Missing KSP plugin, or DAO returns a non-suspend type incorrectly. |
| Mic does nothing | `onError` not implemented, or `RECORD_AUDIO` not requested at runtime. |
| Recognizer works once then stops | Not calling `destroy()` — the mic is leaked. |
| Data vanished after update | `fallbackToDestructiveMigration()` still enabled. |
| Release build crashes, debug fine | R8 minification. Check ProGuard rules. |
| Widget won't update | OEM battery optimisation. Whitelist the app. |
