# Expense Tracker (M-Pesa)

Android app that turns M-Pesa SMS confirmations into a categorized expense log. It parses each
incoming M-Pesa message, prompts the user to categorize the transaction from a notification, and
syncs categorized transactions to a Google Sheet.

Distributed as a sideloaded APK, not through Google Play, which is what makes the SMS permissions
viable.

## Stack
- Kotlin, Jetpack Compose (Empty Activity template), min SDK 26, Gradle Kotlin DSL
- Room for local storage, WorkManager for background sync
- Google Sheet sync through an Apps Script web app endpoint (POST JSON)

## Dev machine constraints
- MacBook with 8 GB RAM, which is tight with Android Studio open. Follow these rules:
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
sms/MpesaSmsReceiver.kt          // manifest-registered SMS_RECEIVED receiver, sender == "MPESA"
sms/MpesaParser.kt               // SMS body -> Transaction (pure Kotlin, no Android deps)
sms/TransactionIngest.kt         // parse, dedupe by code, save, notify (shared by receiver and backfill)
sms/InboxBackfill.kt             // scans SMS inbox for missed M-Pesa messages
data/                            // Room: TransactionEntity, Category, MerchantMemory, UnparsedMessage
notify/CategoryPrompt.kt         // notification with category action buttons
notify/CategoryActionReceiver.kt // handles button taps
sync/SheetSyncWorker.kt          // pushes categorized rows to the Google Sheet
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
- **Parser has three outcomes:** a transaction, an ignorable notice (failures, balance checks,
  Ratiba setup, "transaction underway"), or unparsed. Only the last goes to `UnparsedMessage`.
- **Wording is inconsistent**, so match loosely: `Confirmed`/`confirmed`, missing or doubled spaces,
  a trailing period on names, `Ksh 948.44` vs `Ksh948.44`, `on d/M/yy at h:mm AM` vs no "at".
  Anchor on the stable pieces and ignore the promotional tail. See `mpesa_samples/`.
- **Phone numbers are optional and opaque.** Since 2026, received-money messages mask the sender's
  number (`0700***003`). Pochi la Biashara sends have no number at all.
- **Transaction cost is stored separately** from the amount, for tracking.
- **Fuliza:** the payment message is the spending. The drawdown's access fee is recorded as a cost.
  The repayment is not spending (counting it would double-count).
- **Unmatched messages** go to an `UnparsedMessage` table, never silently dropped.
- **Notification:** max 3 actions, so use 2 smart picks plus "Other…". Smart picks come from
  MerchantMemory (last category for this counterparty) first, then most-used categories.
  MerchantMemory is keyed by counterparty name, not phone number, because numbers can be masked.
  "Other…" opens a dialog-themed activity with the full grid and "add new category".
  Android blocks background activity launches, so never pop a screen directly.
- **Backfill** runs on app open and daily. On first install, look back about 30 days. Backfilled
  items go to the uncategorized queue without individual notifications.
- **Sheet sync:** only categorized rows, retried with a network constraint. Handle re-categorization
  by having Apps Script update the row matching the transaction code instead of appending a duplicate.
- **Battery:** prompt with `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` on first run, and show
  instructions to add the app to Samsung's "Never sleeping apps".
- **Manual add** button for cash and card spending.

## Build order
1. `MpesaParser` plus unit tests from real M-Pesa message samples  <- START HERE
2. Receiver, ingest, and a basic notification, then sideload to a test device
3. Room, the uncategorized queue screen, and the category picker
4. Merchant memory and smart picks
5. Google Sheet sync (Apps Script plus WorkManager)
6. Inbox backfill and the first-run setup flow

## Working agreements
- Write tests alongside the parser and keep the parser free of Android imports.
- Real message samples live in `app/src/test/resources/mpesa_samples/`. Treat them as private and
  anonymize names and phone numbers before committing.
- Ask before adding new dependencies.
