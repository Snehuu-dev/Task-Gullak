'use strict';
/* ============================================================
   SETTINGS
   ============================================================ */
RENDER.settings = () => {
  renderThemeCard();
  const kv = rows => `<dl class="kv">${rows.map(([k, v]) => `<dt>${k}</dt><dd>${v}</dd>`).join('')}</dl>`;
  setHTML($('#st-session'), kv([['Signed in as', esc(S.admin)], ['Access', 'Authorized admin (email whitelist)'], ['Browser time', esc(Intl.DateTimeFormat().resolvedOptions().timeZone)]]) + `<div class="row wrap mt16"><button class="btn bad sm" data-act="logout">${ic('logout', 14)}Log out</button></div><div class="fs12 muted mt12">Authorized emails:</div><div class="col mt4" style="gap:4px">${ALLOWED_ADMIN_EMAILS.map(e => `<span class="mono fs12">${esc(e)}</span>`).join('')}</div>`);
  setHTML($('#st-app'), kv([['Firebase project', esc(firebaseConfig.projectId)], ['Collections', 'users, tasks, offers, submissions, withdrawals, broadcasts, notifications, adminLogs, adminMeta'], ['Date handling', 'All dates use this device’s timezone'], ['Panel', 'TASK GULLAK Admin v2']]));
  setHTML($('#st-roles'), [['Super Admin', 'Everything incl. settings & wallet adjustments'], ['Moderator / Approval Admin', 'Tasks, comments, approvals'], ['Payout Admin', 'Withdrawals and payout records'], ['Read-only viewer', 'View dashboards and reports only']].map(([r, d]) => `<div class="row between wrap" style="border:1px solid var(--line);border-radius:12px;padding:9px 12px"><div><div class="b fs13">${r}</div><div class="fs12 muted">${d}</div></div><span class="pill mute">Needs backend</span></div>`).join(''));
  setHTML($('#st-sec'), [['Sensitive actions need confirmation (reject, delete, block, payout)', 1], ['Audit log for approvals, payouts, wallet, maintenance, notifications', 1], ['Bank account numbers masked by default (reveal is logged)', 1], ['Payout & wallet changes use Firestore transactions (no double processing)', 1], ['Admin master password is stored in this page’s source — move login to Firebase Auth + Firestore rules', 0], ['Enforce roles & rate limits with Firestore Security Rules / Cloud Functions', 0]].map(([t, ok]) => `<div class="note ${ok ? 'ok' : 'warn'}">${ic(ok ? 'check' : 'alert', 16)}<div>${t}</div></div>`).join(''));
};
