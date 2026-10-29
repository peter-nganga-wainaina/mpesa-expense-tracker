# Installing Expense Tracker

Expense Tracker isn't on the Play Store, so it's installed from its download link once. After
that, the app updates itself: it tells you when a new version is out, and you tap **Update**.

These steps are written for Samsung Galaxy phones. Other Android phones are similar but skip the
Samsung-only parts. Menu names can differ slightly between phone software versions.

**You'll need:** Android 8.0 or newer, about 10 minutes, and Wi-Fi or mobile data (the download is
about 8 MB).

```
1. Download ──▶ 2. Unblock installs ──▶ 3. Install ──▶ 4. Turn protection back on ──▶ 5. Set up the app
```

## 1. Download the app

On the phone, open this link in Chrome:

**https://github.com/peter-nganga-wainaina/mpesa-expense-tracker/releases/latest/download/expense-tracker.apk**

Chrome may warn that the file might be harmful, because it's an app from outside the Play Store.
Tap **Download anyway**.

## 2. Unblock installs (Samsung)

Samsung's **Auto Blocker** stops apps from being installed from outside the Play Store. If it's on,
turn it off for now:

1. Open **Settings** → **Security and privacy** → **Auto Blocker**.
2. Turn it **off**.

You'll turn it back on in step 4.

## 3. Install

1. Open the downloaded file: tap the download notification, or open **My Files** → **Downloads** →
   **expense-tracker.apk**.
2. The first time, Android says the phone *"isn't allowed to install unknown apps from this
   source"*. Tap **Settings**, turn on **Allow from this source**, then tap **Back**.
3. Tap **Install**.

If **Google Play Protect** shows a warning:

| What it says | What to do |
|---|---|
| "Unsafe app blocked" with **More details** | Tap **More details** → **Install anyway** |
| "Send app for scanning?" | Either choice works. Then tap **Install** |
| Blocked, with no option to install | Open the **Play Store** → your profile picture → **Play Protect** → ⚙️ → turn off **Scan apps with Play Protect**. Go back and install, then turn scanning back **on** straight away |

Play Protect warns about this app because it reads SMS messages, which is also what scam apps
do. Expense Tracker only reads M-Pesa messages and keeps them on the phone.

## 4. Turn protection back on

1. **Settings** → **Security and privacy** → **Auto Blocker** → turn it back **on**.
2. If you turned off Play Protect scanning, make sure it's back **on**.

If Play Protect later says Expense Tracker is harmful and offers to remove it, choose **Keep app**
(or **Don't uninstall**). Removing it deletes everything saved in it.

## 5. Set up the app

Open **Expense Tracker** from its icon. Setup has three steps:

1. **Welcome:** tap **Allow SMS and notifications**, then **Allow** on each question.
2. **Import recent transactions:** choose how far back to bring in M-Pesa transactions you've
   already made (none, 7, 14 or 30 days), then **Continue**.
3. **Keep it running:** tap **Allow running in the background** and confirm. On Samsung phones,
   also open **Settings** → **Battery** → **Background usage limits** → **Never sleeping apps** →
   **+** → add **Expense Tracker**. Without this, Samsung may put the app to sleep and miss
   messages.

Then tap **Finish**. From now on, each M-Pesa message brings a notification asking which category
it belongs to.

## Updates

When a new version is out, a bar above the tabs says **Update available**. You may also get a
notification; tapping it opens a small update window that works the same way.

1. Tap **Update**. The download takes a few seconds.
2. The first time only, Android asks to allow installs from Expense Tracker: tap **Settings**, turn
   on **Allow from this source**, then **Back**. If you end up back in the app, tap **Update** again.
3. Android asks *"Do you want to update this app?"* Tap **Update**.
4. When it says **App installed**, tap **Done** and open the app from its icon.

Everything saved in the app is kept through updates.

## If something goes wrong

| Problem | Try this |
|---|---|
| "App not installed" | An older test copy with a different signature may be installed. Uninstall it first. (This deletes its data.) |
| No notification after an M-Pesa payment | Check notifications are allowed for Expense Tracker, and that it's in **Never sleeping apps** |
| A transaction is missing | Open the app: it looks back over the last 3 days of messages and adds anything missed |
| The update won't download | Check the connection, then tap **Try again** |
| The app closes as soon as it opens | Press and hold the app's icon, tap **Check for updates**, then **Update**. This works even when the app itself won't open |

## Privacy

- The app reads only messages from **MPESA**, and stores what it finds on the phone. Nothing is
  uploaded.
- Its one internet connection is to this project's GitHub Releases, to check for and download
  updates. It sends no personal data.
