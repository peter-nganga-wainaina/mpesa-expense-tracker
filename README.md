# M-Pesa Expense Tracker

An Android app that turns M-Pesa confirmation messages into a categorized expense log. Each
incoming M-Pesa SMS is read on the phone, and a notification asks which category it belongs to.
The app then shows spending per category, month by month.

## Features

- **Automatic capture:** reads M-Pesa SMS as they arrive (payments, pay bill, sends, withdrawals,
  airtime, money received, Fuliza), plus a daily sweep for anything missed.
- **One-tap categorizing:** notification buttons suggest categories from past choices for the
  same merchant, then the most-used ones.
- **Insights:** money out and money in per category with a donut chart, the month's net, and
  M-Pesa fees as their own line.
- **Manual entries** for cash and card spending.
- **Self-updating:** new versions are offered in the app and installed with Android's prompt.
- Light and dark themes.

## Privacy

Everything stays on the phone, in a local database. The app's only network use is checking this
repository's GitHub Releases for updates; it sends no data.

## Install

The app is distributed as an APK, not through the Play Store, because Play doesn't allow SMS
reading for apps like this. See **[docs/INSTALL.md](docs/INSTALL.md)** for step-by-step
instructions, or download the latest
[`expense-tracker.apk`](https://github.com/peter-nganga-wainaina/mpesa-expense-tracker/releases/latest/download/expense-tracker.apk).

## How it works

```
M-Pesa SMS ──▶ Receiver ──▶ Parser ──▶ Room database ──▶ Notification (pick a category)
                                            │
                    Daily sweep (inbox) ────┘──▶ Insights
```

The parser is plain Kotlin with no Android dependencies, tested against real (anonymized) message
samples in `app/src/test/resources/mpesa_samples/`.

## Development

Kotlin, Jetpack Compose, Room and WorkManager; min SDK 26. Common tasks are wrapped in the
`Makefile`:

```
make help     # list commands
make test     # run all JVM unit tests
make apk      # build the debug APK
```

- [`ROADMAP.md`](ROADMAP.md): what's done and what's next
- [`docs/DATABASE.md`](docs/DATABASE.md): the database schema

## Releasing

1. Raise `versionCode` and set `versionName` in `app/build.gradle.kts`, then commit and push.
2. Tag the commit and push the tag:
   ```
   git tag -a v1.2 -m "Release 1.2"
   git push origin v1.2
   ```

The `Release` workflow then runs the tests, builds and signs the APK, and publishes a GitHub
Release. Installed copies of the app offer the update on their next check.
