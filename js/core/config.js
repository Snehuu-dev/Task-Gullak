'use strict';
/* ============================================================
   CONFIG  (same Firebase project / same admin whitelist as before)
   ============================================================ */
const firebaseConfig = {
  apiKey: "AIzaSyAJhZHDJnN4wX2R6S9hGb_XTh9b8rSvFhY",
  authDomain: "task-gullak.firebaseapp.com",
  projectId: "task-gullak",
  storageBucket: "task-gullak.firebasestorage.app",
  messagingSenderId: "402307564487",
  appId: "1:402307564487:android:344e64291c25e394274fe2"
};
const ALLOWED_ADMIN_EMAILS = ['snehkumarverma20111@gmail.com', 'snehkumarverma2011@gmail.com', 'savvy132007@gmail.com'];
const PLAY_ICON = 'data:image/svg+xml;utf8,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 128 128%22%3E%3Cdefs%3E%3ClinearGradient id=%22g%22 x1=%220%22 y1=%220%22 x2=%221%22 y2=%221%22%3E%3Cstop offset=%220%22 stop-color=%22%2338BDF8%22/%3E%3Cstop offset=%221%22 stop-color=%22%236366F1%22/%3E%3C/linearGradient%3E%3C/defs%3E%3Crect width=%22128%22 height=%22128%22 rx=%2230%22 fill=%22url%28%23g%29%22/%3E%3Crect x=%2214%22 y=%2214%22 width=%22100%22 height=%22100%22 rx=%2224%22 fill=%22%23fff%22 fill-opacity=%22.14%22/%3E%3Cpath d=%22M50 36.5v55a4 4 0 0 0 6 3.4l45-27.5a4 4 0 0 0 0-6.8L56 33.1a4 4 0 0 0-6 3.4z%22 fill=%22%23fff%22/%3E%3C/svg%3E';
const MAINT_TITLE = 'Maintenance Mode';
const MAINT_MSG = "We are currently undergoing scheduled maintenance to bring you a better experience. We'll be back online shortly!";
const DEFAULT_INSTRUCTIONS = [
  'Open the app using the provided Google Play Store link and install it, if you choose to test it.',
  'Explore the app and share your honest experience or feedback.',
  'Submit your own genuine feedback; do not copy a scripted review or promise a particular star rating.',
  'Upload the requested proof/screenshot before the task deadline, where applicable.'
];
const DAY = 864e5;
const RANGE_MS = { '24h': DAY, '7d': 7 * DAY, '30d': 30 * DAY };

const app = initializeApp(firebaseConfig);
/* No popup resolver at startup => Google's gapi script (apis.google.com) is NOT loaded unless Google sign-in is clicked on http(s). */
const auth = initializeAuth(app, { persistence: /^https?:$/.test(location.protocol) ? browserLocalPersistence : inMemoryPersistence });
const db = getFirestore(app);
