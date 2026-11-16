# Task Gullak Admin Panel

How to run: open `index.html` from a web server or Firebase Hosting (`firebase deploy --only hosting`).
Double-clicking the file also works for the master-password login. Google sign-in needs https.

```
index.html            page markup only (login, sidebar, every page, dialogs)
css/
  base.css            colours, layout, cards, buttons, lists
  layout.css          responsive rules + motion/dashboard styles
  pages.css           task cards, approvals, reports, receipts, notifications
  loader.css          loading screen (ring) + thin top bar
js/
  boot.js             loads Firebase, then every file below in order
  guard.js            shows readable errors on the login screen
  config.js           Firebase config, admin emails, database connection
  helpers.js          small helpers + icons
  state.js            app state, toast/dialogs, audit log, data accessors
  shell.js            navigation, click handling, login
  listeners.js        live Firestore listeners, finance numbers, render dispatch
  pages/
    overview.js       dashboard (monthly)
    approvals.js      approvals, folders, bulk mode
    tasks.js          task list + task editor (Normal / Bulk / Both)
    comments.js       comment pool, blocked users
    users.js          users + wallets
    withdrawals.js    withdrawals
    reports.js        monthly reports + report images
    notifications.js  notifications to users
    bulk-access.js    Bulk Access (user requests -> Activate / Reject)
    system.js         maintenance, settings, activity log, developer tools
  init.js             start-up
user-app/             small files for your USER app (bulk access, task details)
functions/            optional Cloud Functions (push, daily cleanup, screenshots)
SETUP-STEPS.md        optional setup
```

Add a new page: create `js/pages/<name>.js`, add its file name in `js/boot.js` (FILES list) and in the `<link rel="preload">` list in `index.html`.
Files load in the order listed in `boot.js`; a file can use anything defined in the files above it.

Faster loading: Firestore keeps a copy of your data on the device (the next visit opens instantly),
the first screen loads tasks / submissions / withdrawals / users first, and logs, notifications and
bulk requests load afterwards when the browser is idle.
