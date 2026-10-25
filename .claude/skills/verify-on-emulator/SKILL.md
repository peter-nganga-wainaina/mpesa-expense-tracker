---
name: verify-on-emulator
description: Checklist for verifying app behavior on the Android emulator that unit tests can't reach (screens, notifications, receivers, themes, setup). Use after building a UI or Android-framework change, before calling it done, or when asked to "check it on the emulator".
---

# Verify on the emulator

Unit tests cover the logic. This checklist covers what only a running app shows. It exists because
of real misses: a snackbar that never went away (a library default), a total that overflowed the
donut, dark-on-dark status bar icons, and an unlabeled button.

## Before starting

- **Ask before starting the emulator** (project rule; the dev Mac has 8 GB RAM). Close it and run
  `make stop` when done.
- **Update vs fresh install:** `make install` keeps the app's data. Uninstall first
  (`adb uninstall io.github.peter_nganga_wainaina.expensetracker`) only when the database schema changed in place.
- Use `tap.sh` (next to this file) to tap by visible text or accessibility label, never by guessed
  coordinates: `.claude/skills/verify-on-emulator/tap.sh "Add spending"`. "not found" is itself a
  finding (see Accessibility).
- Test data is fake SMS sent with `make sms MSG="..."`. Change the transaction code each time.

## For every change

1. **Library defaults.** List the library calls the change relies on whose defaults decide behavior,
   and check each default in the library source (sources jars live under
   `~/.gradle/caches/modules-2/files-2.1/`). Typical ones here: snackbar duration, notification
   channel importance, `PendingIntent` flags, activity launch mode, WorkManager existing-work policy,
   Room conflict strategy, sheet `skipPartiallyExpanded`. Make the important ones explicit in code.
2. **Expected result first.** Write down what should happen (counts, text, which screen) before
   acting, then compare.
3. **Do nothing and wait.** After each action, leave the screen alone for ~15 seconds. Anything that
   should go away (snackbars, banners) must; nothing should go away that shouldn't.
4. **Allow for slowness.** The emulator is slow; wait for screens to settle before screenshots, and
   take a second screenshot before concluding something didn't happen.

## Screens

- **Both themes:** light (Maroon & Latte) and dark (Cocoa Night), including status bar icons.
- **Empty, normal, and large:** no data, typical data, and big values (amounts like Ksh1,234,567.00,
  long names) that could overflow.
- **Back:** the system Back button and any back arrow return to the right place.
- **Rotation / state:** a half-filled form or open sheet survives rotating the screen.
- **Arriving another way:** from a notification tap while on another tab, after a restart, and on
  first run vs later runs.

## Data paths

- **Live SMS, backfill, and sweep** each reach the database (check with the app's screens or by
  copying `databases/expensetracker.db*` via `adb exec-out run-as io.github.peter_nganga_wainaina.expensetracker cat`).
- **No duplicates** after re-delivery or a sweep; counts match the expected result.
- **Notifications:** posted when they should be and not when they shouldn't (backfill, manual
  entries); count via `adb logcat -b events | grep notification_enqueue`.

## Accessibility

- Dump the screen (`adb shell uiautomator dump`) and look for clickable nodes with no text and no
  `content-desc` (`NAF="true"`): they reach screen readers unnamed.
- Touch targets at least 48 dp; text readable in both themes.

## Report

Say what was checked by hand, what wasn't, and why, so untested paths stay visible.
