# Expense Tracker (M-Pesa)

Android app that turns M-Pesa SMS confirmations into a categorized expense log. It parses each
incoming M-Pesa message, prompts the user to categorize the transaction from a notification, and
stores the categorized log on the device for tracking spending per category over time.

Distributed as a sideloaded APK, not through Google Play, which is what makes the SMS permissions
viable.

## Stack
- Kotlin, Jetpack Compose (Empty Activity template), min SDK 26, Gradle Kotlin DSL
- Room (SQLite) for local storage; the on-device database is the source of truth
- WorkManager for the daily inbox sweep
- Off-device sync or export is undecided (see `ROADMAP.md`)

## Dev machine constraints
- MacBook with 8 GB RAM, which is tight with Android Studio open. Follow these rules:
  - The `Makefile` wraps the allowed Gradle tasks (`make help` lists them); keep it in sync.
  - Run the narrowest Gradle task that answers the question. For the parser, use
    `./gradlew :app:testDebugUnitTest --tests "*MpesaParser*"`, not `./gradlew build` or `check`.
  - Never run `assembleRelease`, `lint`, or `connectedAndroidTest` unless asked.
  - Build an APK (`./gradlew :app:assembleDebug`) only when I'm about to install it on a device.
  - Don't start or assume an emulator. Ask first if a task truly needs one.
  - Keep the parser and other logic free of Android imports so they're testable on the JVM.
  - Don't add dependencies without asking. Each one adds build time and memory.
  - Don't set `org.gradle.parallel=true` or raise the memory caps below.
  - After a long session of builds, suggest `./gradlew --stop` to free the Gradle daemon's memory.
- `gradle.properties` should keep:
  ```
  org.gradle.jvmargs=-Xmx1536m -XX:MaxMetaspaceSize=384m
  kotlin.daemon.jvmargs=-Xmx512m
  ```
- Emulator SMS testing: `adb emu sms send MPESA "<message>"`

## Architecture
```
AppContainer.kt                  // wires stores, ingest, and categorizer for receivers and UI
sms/MpesaSms.kt                  // sender filter + multipart join (pure Kotlin)
sms/MpesaSmsReceiver.kt          // manifest-registered SMS_RECEIVED receiver, sender == "MPESA"
sms/MpesaParser.kt               // SMS body -> Transaction (pure Kotlin, no Android deps)
sms/TransactionIngest.kt         // parse, dedupe by code, save, notify (shared by receiver and backfill)
sms/InboxBackfill.kt             // scans SMS inbox for missed M-Pesa messages (first import + sweep)
sms/DailySweepWorker.kt          // WorkManager job running the sweep once a day
data/                            // Room: transactions, categories, unparsed messages
categorize/                      // Category, CategoryStore, Categorizer, new-name validation
suggest/CategorySuggester.kt     // interface: transaction -> ranked category suggestions
notify/CategoryPrompt.kt         // notification with category action buttons
notify/CategoryActionReceiver.kt // handles button taps
sync/                            // later: sync or export, target undecided
ui/                              // uncategorized queue, category picker, settings, first-run setup
```

## Key design decisions
- **No always-running service.** The manifest-registered receiver is woken by Android on each SMS.
  Use `goAsync()` plus a coroutine for the DB write.
- **Multipart SMS:** concatenate all parts from `getMessagesFromIntent` before parsing.
- **Transaction code is the primary key.** Insert with `OnConflictStrategy.IGNORE` for dedupe.
- **Money is stored as Long cents**, never floating point.
- **Sender ID is exactly `MPESA`** (confirmed from 681 real messages). Keep it in one constant.
- **Message types:** PAID (till), PAYBILL, SENT, WITHDRAW, AIRTIME, RECEIVED, plus Fuliza and others.
  Rule order matters: check paybill ("for account") before plain send.
- **Parser has three outcomes**, keyed on the transaction code (every money movement has one):
  no code, or a balance check → Ignored. Code + amount → a transaction (UNKNOWN type if no keyword
  matches). Code but no readable amount → Unrecognized, which goes to `UnparsedMessage`.
- **Fields are found by landmarks**, not whole-sentence templates: each field is the first value of
  its shape, or the text between a keyword and the next landmark.
- **Wording is inconsistent**, so match loosely: `Confirmed`/`confirmed`, missing or doubled spaces,
  a trailing period on names, `Ksh 948.44` vs `Ksh948.44`, `on d/M/yy at h:mm AM` vs no "at".
  Anchor on the stable pieces and ignore the promotional tail. See `mpesa_samples/`.
- **Ratiba (standing order) payments** start "Confirmed M-PESA Ratiba." and mask the recipient's
  name and number (`HELEN A*****`). They are SENT transactions, not notices.
- **Phone numbers are optional and opaque.** Since 2026, received-money messages mask the sender's
  number (`0700***003`). Pochi la Biashara sends have no number at all.
- **Transaction cost is stored separately** from the amount, for tracking.
- **The full SMS is stored with each transaction** (`body`), shown on request as "original message"
  and available for re-parsing if the parser improves. Same for unparsed messages.
- **Fuliza:** the payment message is the spending. The drawdown's access fee is recorded as a cost.
  The repayment is not spending (counting it would double-count).
- **Unmatched messages** go to an `UnparsedMessage` table, never silently dropped.
- **Notification:** max 3 actions, so use 2 smart picks plus "Other…". Smart picks come from
  merchant memory (last category for this counterparty) first, then most-used categories.
  Merchant memory is keyed by counterparty name (ignoring case), not phone number, because numbers
  can be masked. It is derived from categorized transactions, not stored separately, so the user's
  choices have one source of truth and re-categorizing updates it.
  "Other…" opens a dialog-themed activity with the full grid and "add new category".
  Android blocks background activity launches, so never pop a screen directly.
- **Smart picks come from a `CategorySuggester` interface**, so sources can be added without
  touching the notification code. `ChainedSuggester` runs merchant memory, then most-used
  categories. A model-based suggester (on-device first, because this is financial data) may be added
  later for merchants with no history. It only suggests, never auto-assigns, and never blocks the
  notification on a network call.
- **Backfill** reads M-Pesa messages already in the SMS inbox (needs `READ_SMS`) and saves any
  the database doesn't have yet. Backfilled items go to the uncategorized queue without
  notifications. Two uses:
  - **First import:** during first-run setup the user picks how far back to import: none, 7, 14,
    or 30 days. Default 14.
  - **Sweeper:** a fixed, invisible 3-day look-back on every app open and daily (WorkManager,
    `DailySweepWorker`), catching anything the receiver missed. Overlap is harmless: codes dedupe.
- **Insights start from categories.** The app doesn't decide what counts as "spending"; the user's
  categories carry the meaning (e.g. a "Savings" category). Whether to also split money in vs out,
  and how user-editable rules fit in, is an open question in `ROADMAP.md`. Don't bake either in.
- **Look:** warm and vibrant, built on maroon and brown. Light theme "Maroon & Latte" (latte
  background, maroon highlights, espresso text) and dark theme "Cocoa Night" (chocolate background,
  maroon highlights, cream text). A light/dark switch in the app. Category colours are separate
  vibrant accents (gold, rose, terracotta, caramel, sage) that differ in lightness, not just hue.
- **Charts are drawn by hand in Compose** (donut, stacked bars) for full control over the look.
  A chart library (likely Vico) is for trend charts later; ask before adding it.
- **Sync / export is undecided** (Google Sheet, a hosted database, or none). Whatever is chosen:
  only categorized rows, retried in the background (WorkManager) with a network constraint, and
  re-categorization updates the row matching the transaction code instead of adding a duplicate.
- **Battery:** prompt with `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` on first run, and show
  instructions to add the app to Samsung's "Never sleeping apps".
- **Manual add** button for cash and card spending.

## Roadmap
Build order, status, later features and open questions live in `ROADMAP.md`. Update it when a
milestone or slice is finished, and start work from its "Now" section.

## Working agreements
- Write tests alongside the parser and keep the parser free of Android imports.
- Real message samples live in `app/src/test/resources/mpesa_samples/`. Treat them as private and
  anonymize names and phone numbers before committing.
- Ask before adding new dependencies.
