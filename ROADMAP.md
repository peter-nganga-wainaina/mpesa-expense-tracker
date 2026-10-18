# Roadmap

Where the app is, what comes next, and what is planned for later. Design decisions behind each
piece live in `CLAUDE.md`; this file tracks order and status.

```
✅ Capture ──▶ ✅ Categorize ──▶ ✅ Catch-up ──▶ ⏳ Insights ──▶ Release prep ──▶ 🚀 First install
 parser,        queue, picker,    first-run       theme, history,  signing key,
 receiver,      quick picks,      setup, inbox    monthly charts,  real-device
 ingest         merchant memory   backfill,       manual add       check
                                  daily sweep
```

Status: ✅ done · ⏳ in progress · ⬜ not started

## Done

| # | Milestone | What it delivers |
|---|---|---|
| 1 | ✅ M-Pesa parser | SMS body → transaction, unit-tested against real (anonymized) samples |
| 2 | ✅ Receiver and notification | Each M-Pesa SMS is parsed and prompts for a category |
| 3 | ✅ Storage and categorizing | Room database, uncategorized queue, category picker, notification quick picks |
| 4 | ✅ Smart picks | Merchant memory first, then most-used categories, behind `CategorySuggester` |
| 6 | ✅ Setup and backfill | First-run setup (permissions, first import of 0/7/14/30 days, battery), inbox sweep on app open and daily |
| — | ✅ Original message | Each transaction keeps its full SMS; "Show original message" in the picker shows it (selectable) |

## Now: milestone 5, insights

Built in slices; each one is usable on its own.

| Slice | Status | What it delivers |
|---|---|---|
| **5a. Theme, navigation, history** | ✅ | Maroon & Latte (light) and Cocoa Night (dark) themes with a light/dark switch; bottom tabs (To categorize · History · Insights); a history list where any transaction can be re-categorized |
| **5b. Monthly insights** | ✅ | A month's categorized totals, largest first, with a hand-drawn donut, a month switcher and a count of uncategorized transactions. Category-only for now: money in/out and fees are open questions below. |
| **5b+. Category drill-down** | ✅ | Tapping a category in Insights (e.g. October › Savings) opens a full screen listing that month's transactions in that category, grouped by day like History. Tapping one opens the usual picker to re-file it or see its original message. Read-only over existing data: nothing new is stored. Donut slices aren't tappable for now (too small to hit reliably). |
| **5c. Manual add** | ✅ | Spending that never sends an SMS. A floating "+ Add spending" button on every tab opens a form: amount, optional "what for", paid with Cash / Card / Other, when (defaults to now), and category, picked right there (merchant memory pre-selects it). Saved straight to History and Insights, with no queue or notification; a brief "Saved Ksh450.00 · Food [View]" confirmation shows it worked, since the queue list itself doesn't change. Manual entries can be edited and deleted; M-Pesa entries can only be re-categorized. |
| **5d. Unparsed messages** | ⬜ Next | A screen listing receipts the parser couldn't read, reached from a banner on To categorize that only shows when there are some. Each one offers "Add as spending" (opens the manual form) and "Dismiss". Dismiss marks it dismissed rather than deleting it, or the sweep would bring it back. No phone notification for unreadable messages, and no Share button for now. |

## Before the first real install

| Item | Status | Why |
|---|---|---|
| Release signing key, backed up | ⬜ | Android only installs an update over an app signed with the same key. Losing the key means uninstalling, which wipes the data. |
| Release build and APK size check | ⬜ | Confirms the shrunk build works and measures the real size |
| Real-device check (Samsung) | ⬜ | Real Safaricom SMS delivery, notification behaviour, and Samsung's "Never sleeping apps" steps can't be tested on the emulator |
| **Auto-update** | ⬜ | Must ship in the first installed version, or that version can't update itself. Sideloaded apps can't update silently: Android always asks the user to tap "Install". Likely shape: publish each signed APK as a GitHub Release; the app checks the latest release on open (or daily), and offers "Update available" → download → Android's install prompt. Needs the `INTERNET` permission (the app has none today, a privacy-visible change for a finance app) and `REQUEST_INSTALL_PACKAGES`. Every update must be signed with the same release key. A no-code alternative: Obtainium, a third-party app that watches a GitHub repo's releases and installs updates. |
| **CI: tests on every push** | ✅ `.github/workflows/tests.yml` runs the unit tests on every push and pull request | A GitHub Actions workflow running `./gradlew :app:testDebugUnitTest` on push and pull request, on GitHub's machines (no load on the dev Mac). Later it can also build the signed release APK and attach it to a GitHub Release, which is what auto-update would download. |

## Later

| Feature | Notes |
|---|---|
| **Trend charts** | Spending over months, with tapping and scrolling. A chart library (Vico, ~1 MB, no extra dependencies beyond Compose) is the likely choice here; ask before adding it. |
| **Sync / export** | Target undecided: Google Sheet, a hosted database, or none. Rules for whichever is chosen are in `CLAUDE.md`. |
| **Auto-categorization rules** | User-made rules such as "counterparty is X (optionally: and type is SENT/RECEIVED/…) → category Y". In `TransactionIngest`, after parsing and saving: if a rule matches, assign its category with no notification; otherwise notify as usual. Rules are the user's own decisions, so this doesn't conflict with suggesters never auto-assigning. Pure Kotlin matching on parsed fields, not raw text; list, edit and delete in settings; a natural entry point is "Always file this here" when categorizing. |
| ↳ First use case: Ziidi | Ziidi (M-Pesa money market fund) can auto-save a percentage of each payment as its own coded message ("sent to ZIIDI"), which parses as SENT. Until rules exist, file it under "Savings" and merchant memory remembers it. With rules: ZIIDI + SENT → savings deposit, ZIIDI + RECEIVED → savings withdrawal. Match the counterparty, not the word "Ziidi" (most messages carry a Ziidi promo tail), and confirm the real wording first: "for account" would make it PAYBILL. |
| **Import older messages** | A settings option to pull in history beyond the first import. Reuses the backfill; duplicates are skipped by transaction code. |
| **Uncategorized reminder** | A gentle "3 transactions waiting" nudge if items sit uncategorized for a day or so |
| **Model-based suggestions** | For merchants with no history; on-device first, since this is financial data. Suggests only, never assigns. |

## Open questions

| Question | Affects |
|---|---|
| How should categories, money in/out, and rules work together? Leaning towards no meaning baked into the app: parsing stays in code, while interpretation (direction, category, whether to prompt) becomes user-editable rules seeded with defaults. Fuliza shows why it matters: a payment, a borrow and a repayment are three messages, and adding them up naively double-counts. | 5b, rules |
| Where should data sync to, if anywhere? | Sync / export |
| Should transaction fees show in insights, as their own line or inside each category? They are parsed and stored already. | Insights |
