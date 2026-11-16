# Optional setup (the panel itself works with just index.html)

Login: same as before (master password + admin email). Nothing to set up for it.

Only if you want these extra features:

1. Functions (needs the Firebase Blaze plan): run `firebase init functions`, copy functions/index.js and
   package.json, then `firebase deploy --only functions`. This gives:
   - real push notifications (user app subscribes to topic "all_users" and saves users/{uid}.fcmToken)
   - daily cleanup of old tasks and expired notifications at 00:05 IST
   - moving screenshots from Firestore to Storage
2. User app changes:
   - show notification.howToSteps and hide anything past expiresAtMillis (see user-notification-steps.js)
   - show screenshots from screenshotUrl, upload new ones to Storage
   - hide tasks past expiresAtMillis
3. After the user app uses screenshotUrl, set DELETE_BASE64 = true in functions/index.js and redeploy.

Note: with the master-password login there is no Firebase sign-in, so Firestore rules cannot be limited to admins
(the panel would stop working). Keep your current rules unless you switch to Firebase Auth login.
