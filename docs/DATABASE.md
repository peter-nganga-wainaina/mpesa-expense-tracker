# Database schema

The app keeps everything in one on-device SQLite database, `expensetracker.db`, through Room.
This page explains what each table and column means and the rules the data follows.

- **Source of truth:** the entities in `app/src/main/java/com/example/expensetracker/data/Entities.kt`.
- **Machine-readable copy:** Room exports the exact schema to `app/schemas/` on every build.
- **Kept in sync by a test:** `DatabaseDocTest` fails if a table or column is missing from this page.

```
categories                    transactions                          unparsed_messages
 id ◀──────────────────────── categoryId (null = uncategorized)      code   (M-Pesa code)
 name                          code   (primary key) ◀───same code───▶  body
                               type · amountCents · costCents         receivedAt
                               counterparty · phone · reference       dismissed
                               occurredAt · body · source
```

## `transactions`

Every money movement: parsed from an M-Pesa SMS, or entered by hand.

| Column | Type | Meaning |
|---|---|---|
| `code` | text, primary key | The M-Pesa transaction code for SMS transactions and for entries typed in from an unreadable message (that message's code). A random UUID for other hand entries, which can never look like an M-Pesa code (always 10 letters and digits). |
| `type` | text | An `MpesaTransactionType` name: `PAID`, `PAYBILL`, `SENT`, `RECEIVED`, `WITHDRAW`, `AIRTIME`, `FULIZA_DRAWDOWN`, `FULIZA_REPAYMENT`, `UNKNOWN` (parsed, no known keyword), or `MANUAL` (entered by hand). |
| `amountCents` | integer | The amount in cents (Ksh × 100). Never floating point. Always positive. |
| `costCents` | integer | M-Pesa's fee, or Fuliza's access fee, in cents. 0 when none, and always 0 for hand entries. |
| `counterparty` | text, nullable | Who the money went to or came from. For hand entries, the "what for" note. Merchant memory matches on this, ignoring case. |
| `phone` | text, nullable | The counterparty's number as written in the SMS, possibly masked (`0700***003`). |
| `reference` | text, nullable | The paybill account number, or the withdrawal agent number. |
| `occurredAt` | text | When it happened, as an ISO local date-time (`2026-10-18T13:41`), in Kenyan local time with no zone. The text sorts chronologically. Falls back to the SMS arrival time when the message has none. |
| `body` | text, nullable | The full SMS as received, shown as "original message". Null for hand entries. |
| `source` | text | A `PaymentSource` name: how it was paid. `MPESA`, `CASH`, `CARD`, or `OTHER`. |
| `categoryId` | integer, nullable | The category it's filed under. Null while it waits in the "To categorize" queue. |

**Rules**
- **One row per code.** Inserting a code that already exists is skipped (`OnConflictStrategy.IGNORE`), so a re-delivered SMS or a sweep never duplicates a transaction. Hand entries are saved with an upsert, so editing replaces the row.
- **Entered by hand means `type = MANUAL`.** Only those rows can be edited or deleted; the delete query checks the type. Parsed transactions can only be re-categorized.
- **Paid with vs entered by:** `source` says how it was paid and `type` says who entered it. An entry typed in from an unreadable M-Pesa message is `type = MANUAL`, `source = MPESA`.
- **Uncategorized rows** (`categoryId` null) are counted on Insights, not totaled.

## `categories`

The user's categories. Seeded once on first launch with Food, Transport, Bills, Shopping, Savings,
Other; after that they're the user's data, and later code changes don't re-seed them.

| Column | Type | Meaning |
|---|---|---|
| `id` | integer, primary key, auto-generated | Stable ID. Transactions point here, so renaming a category moves every transaction with it. It also picks the category's colour. |
| `name` | text, unique ignoring case | The name shown everywhere. "food" can't be added beside "Food". |

## `unparsed_messages`

M-Pesa receipts the parser couldn't read: a transaction code but no readable amount. Kept so
nothing is silently dropped.

| Column | Type | Meaning |
|---|---|---|
| `code` | text, primary key | The M-Pesa transaction code found in the message. |
| `body` | text | The full SMS text. |
| `receivedAt` | text | When the SMS arrived, as an ISO local date-time. |
| `dismissed` | integer (boolean) | The user has dealt with it. Dismissed messages leave the list. |

**Rules**
- **One row per code, and the first one stays.** The sweep re-reads recent SMS, and inserting an existing code is skipped, so a dismissed message stays dismissed. That's why dismissing is a flag, not a delete.
- **Linked to its hand entry by code.** Typing a message in as spending creates a `transactions` row with the same `code`, so the message shows "Added as …" and saving again edits that entry instead of adding a second one. If a future parser reads the real transaction, its insert is skipped rather than double counting.

## Changing the schema

- **Before the first real install** the schema is changed in place (version 1); the emulator needs an uninstall and fresh install.
- **After it,** every change bumps the database version and adds a tested Room migration against the exported schema in `app/schemas/`, so existing data survives updates.
- Update this page in the same change. `DatabaseDocTest` will fail if a column is left out.
