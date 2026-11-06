# ANY TASK — Admin Panel (multi-file)

Same app as before, now split into folders so each part can be edited on its own.

## Folder map — kahan kya badalna hai

| Kya badalna hai | File |
|---|---|
| Page ka HTML (screens, popups, buttons) | `index.html` |
| Firebase SDK version | `index.html` (import URLs) |
| Firebase keys, admin email whitelist | `js/core/config.js` |
| Chhote helper functions (format, date, icons) | `js/core/helpers.js` |
| App state (filters, selections) | `js/core/state.js` |
| Toast / popup dialogs | `js/core/ui.js` |
| Page switching (sidebar) | `js/core/navigation.js` |
| Button clicks / typing (event handling) | `js/core/events.js` |
| Login / logout | `js/core/auth.js` |
| Firestore realtime listeners | `js/core/realtime.js` |
| Dashboard | `js/views/overview.js` |
| Approvals, search, bulk approve/reject/block | `js/views/approvals.js` |
| Task Manager (add/edit task, day filter) | `js/views/tasks.js` |
| Play Store name / icon / rating fetch | `js/services/play-store.js` |
| Comments, feedback | `js/views/comments.js` |
| Users & wallet | `js/views/users.js` |
| Withdrawals | `js/views/withdrawals.js` |
| Monthly records / reports | `js/views/reports.js` |
| Push notifications | `js/views/notifications.js` |
| Maintenance mode | `js/views/maintenance.js` |
| Settings, Activity logs, Developer tools | `js/views/settings.js`, `logs.js`, `devtools.js` |
| Startup | `js/app.js` |
| Colors, layout, theme | `css/base.css` |
| Cards, buttons, lists, chips | `css/components.css` |
| Approvals / task card / popups / login | `css/approvals.css`, `tasks.css`, `overlays.css` |
| Mobile + dark mode | `css/responsive.css` |
| Newer styles (dashboard, skeleton, animation) | `css/enhancements.css` |
| Order in which JS files load | `js/manifest.js` |
| Play Store proxy (Cloudflare Worker) | `tools/play-proxy-worker.js` |

## Update karne ka tarika
1. Jo file badalni hai wahi edit karo.
2. `js/manifest.js` me `APP_VERSION` badha do (jaise `1.0.1`) taaki browser purani cached file na dikhaye.
3. Upload / deploy karo.

## Run karna
Ye ek website hai, isliye local server ya hosting se chalao (file ko seedha double-click karke nahi):

    cd admin-panel
    python3 -m http.server 8000      # phir http://localhost:8000

Hosting: Firebase Hosting / Netlify / GitHub Pages (folder ko jyon ka tyon upload karo).
Google login ke liye apna domain Firebase Console → Authentication → Settings → Authorized domains me add karna zaroori hai.

## Nayi file jodna
JS file banao (top par `'use strict';` likho) → `js/manifest.js` ki list me sahi jagah add karo. Files ek hi shared scope me chalti hain, isliye ek file ke functions dusri file me seedha use ho sakte hain.
