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

## Now: milestone 5, insights

Built in slices; each one is usable on its own.

| Slice | Status | What it delivers |
|---|---|---|
| **5a. Theme, navigation, history** | ⬜ Next | Maroon & Latte (light) and Cocoa Night (dark) themes with a light/dark switch; bottom tabs (To categorize · History · Insights); a history list where any transaction can be re-categorized |
| **5b. Monthly insights** | ⬜ | A month's totals broken down by category; hand-drawn donut chart; month switcher. Depends on the first open question below. |
| **5c. Manual add** | ⬜ | A **+** button for cash and card spending that never sends an SMS |
| **5d. Unparsed messages** | ⬜ | A screen listing receipts the parser couldn't read, so they can be checked |

## Before the first real install

| Item | Status | Why |
|---|---|---|
| Release signing key, backed up | ⬜ | Android only installs an update over an app signed with the same key. Losing the key means uninstalling, which wipes the data. |
| Release build and APK size check | ⬜ | Confirms the shrunk build works and measures the real size |
| Real-device check (Samsung) | ⬜ | Real Safaricom SMS delivery, notification behaviour, and Samsung's "Never sleeping apps" steps can't be tested on the emulator |

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
| **Keep the raw SMS text** | Lets transactions be re-parsed if the parser improves. Needs a database migration once real data exists. |

## Open questions

| Question | Affects |
|---|---|
| How should categories, money in/out, and rules work together? Leaning towards no meaning baked into the app: parsing stays in code, while interpretation (direction, category, whether to prompt) becomes user-editable rules seeded with defaults. Fuliza shows why it matters: a payment, a borrow and a repayment are three messages, and adding them up naively double-counts. | 5b, rules |
| Where should data sync to, if anywhere? | Sync / export |
