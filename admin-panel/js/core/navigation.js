'use strict';
/* ============================================================
   NAVIGATION
   ============================================================ */
const VIEW_META = {
  overview: ['Dashboard', 'Welcome back, Admin! 👋'],
  tasks: ['Task Management', 'Create, edit, pause and archive app tasks'],
  comments: ['Comment Management', 'Internal feedback prompts and user restrictions'],
  approvals: ['Approval Management', 'Review proofs task-wise and approve or reject in bulk'],
  bulk: ['Bulk Access Management', 'Approve or reject bulk mode access requests from users'],
  users: ['Users & Wallet', 'Profiles, wallet balance, history and account actions'],
  withdrawals: ['Withdrawal Management', 'Process UPI and bank payouts with full records'],
  referrals: ['Referral Management', 'Track user referral links, bonuses, and 1% lifetime commissions'],
  reports: ['Monthly Records', 'Month-wise tasks, payouts, revenue and profit'],
  notifications: ['Push Notifications', 'Templates, custom messages and delivery history'],
  tickets: ['Support Tickets & Inquiries', 'User support messages and inquiry tickets'],
  maintenance: ['Maintenance Mode', 'Lock the user app with a live notice'],
  settings: ['Settings', 'Admin access, security and app info'],
  logs: ['Activity Logs', 'Audit trail of every important admin action'],
  devtools: ['Developer Tools', 'Connections, exports and diagnostics']
};
function go(view, opts = {}) {
  if (!VIEW_META[view]) view = 'overview';
  S.view = view;
  { const vv = $('#v-' + view); if (vv) { vv.classList.remove('anim'); void vv.offsetWidth; vv.classList.add('anim'); setTimeout(() => vv.classList.remove('anim'), 1100); } }
  if (view === 'approvals') {
    if (opts.range) S.appr.range = opts.range;
    if (opts.status) S.appr.status = opts.status;
    if (opts.q != null) { S.appr.q = opts.q; $('#ap-q').value = opts.q; }
    S.appr.groups = 6; S.appr.folder = null;
  }
  if (view === 'comments' && opts.tab) { S.cm.tab = opts.tab; if (opts.tab === 'bank') S.cm.folder = null; }
  $$('.view').forEach(v => v.classList.toggle('on', v.id === 'v-' + view));
  $$('.nav-i[data-nav]').forEach(b => b.classList.toggle('on', b.dataset.nav === view));
  $('.nav-i[data-v="overview"]').classList.toggle('on', view === 'overview');
  $$('.nav-g').forEach(g => { const nb = $('.nav-i', g); if (nb) g.classList.toggle('open', nb.dataset.nav === view); });
  $$('.nav-sub button').forEach(b => b.classList.toggle('on', view === 'approvals' && b.dataset.range === S.appr.range || view === 'comments' && b.dataset.tab === S.cm.tab && b.dataset.v === 'comments'));
  const m = VIEW_META[view];
  $('#page-title').textContent = m[0]; $('#page-sub').textContent = m[1];
  document.body.classList.remove('nav-open');
  $('#user-menu').classList.add('hidden');
  renderAll(true);
  window.scrollTo({ top: 0 });
}

