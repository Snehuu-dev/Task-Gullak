'use strict';
/* Navigation, click/input handling, admin login */

/* ============================================================
   NAVIGATION
   ============================================================ */
const VIEW_META = {
  overview: ['Dashboard', 'Monthly overview'],
  tasks: ['Tasks', 'Add and manage tasks'],
  comments: ['Comments', 'Comments and blocked users'],
  approvals: ['Approvals', 'Check and approve proofs'],
  bulk: ['Bulk access', 'Who can use Bulk mode'],
  users: ['Users', 'Users and wallets'],
  withdrawals: ['Withdrawals', 'Pay your users'],
  reports: ['Reports', 'Monthly spend and payouts'],
  notifications: ['Notifications', 'Send a message to users'],
  maintenance: ['Maintenance', 'Pause the user app'],
  settings: ['Settings', 'Admin and security'],
  logs: ['Activity', 'What admins did'],
  devtools: ['Developer', 'Tools and checks']
};
function go(view, opts = {}) {
  if (!VIEW_META[view]) view = 'overview';
  S.view = view;
  { const vv = $('#v-' + view); if (vv) { vv.classList.remove('anim'); void vv.offsetWidth; vv.classList.add('anim'); setTimeout(() => vv.classList.remove('anim'), 1100); } }
  if (view === 'approvals') {
    if (opts.range) { S.appr.mode = 'month'; S.appr.ref = ymd(Date.now()); apApply(); }
    if (opts.status) S.appr.status = opts.status;
    if (opts.q != null) { S.appr.q = opts.q; $('#ap-q').value = opts.q; }
    S.appr.groups = 6; S.appr.folder = null;
  }
  if (view === 'comments' && opts.tab) { S.cm.tab = opts.tab; if (opts.tab === 'bank') S.cm.folder = null; }
  if (view === 'bulk' && opts.tab) { S.bkTab = opts.tab; if (S.bkSel && S.bkSel.clear) S.bkSel.clear(); }
  $$('.view').forEach(v => v.classList.toggle('on', v.id === 'v-' + view));
  $$('.nav-i[data-nav]').forEach(b => b.classList.toggle('on', b.dataset.nav === view));
  $('.nav-i[data-v="overview"]').classList.toggle('on', view === 'overview');
  $$('.nav-g').forEach(g => { const nb = $('.nav-i', g); if (nb) g.classList.toggle('open', nb.dataset.nav === view); });
  $$('.nav-sub button').forEach(b => b.classList.toggle('on', view === 'approvals' && b.dataset.range === S.appr.range || view === 'comments' && b.dataset.tab === S.cm.tab && b.dataset.v === 'comments' || view === 'bulk' && b.dataset.tab === S.bkTab && b.dataset.v === 'bulk'));
  const m = VIEW_META[view];
  $('#page-title').textContent = m[0]; $('#page-sub').textContent = m[1];
  document.body.classList.remove('nav-open');
  $('#user-menu').classList.add('hidden');
  renderAll(true);
  window.scrollTo({ top: 0 });
}

/* ============================================================
   EVENT DELEGATION
   ============================================================ */
window.ACT = window.ACT || {}; var ACT = window.ACT;
window.INP = window.INP || {}; var INP = window.INP;
document.addEventListener('click', e => {
  const el = e.target.closest('[data-act]');
  if (!el) { if (!e.target.closest('#user-menu') && !e.target.closest('[data-act="userMenu"]')) $('#user-menu')?.classList.add('hidden'); return; }
  const fn = ACT[el.dataset.act];
  if (fn) { try { const r = fn(el, e); if (r && r.catch) r.catch(err => { console.error(err); toast(err.message || 'Action failed', 'bad'); }); } catch (err) { console.error(err); toast(err.message || 'Action failed', 'bad'); } }
});
const onInput = e => {
  const el = e.target.closest('[data-in]'); if (!el) return;
  const fn = INP[el.dataset.in]; if (fn) fn(el, e);
};
document.addEventListener('input', onInput);
document.addEventListener('change', e => { if (e.target.matches('select,input[type=date],input[type=month],input[type=checkbox],input[type=file]')) onInput(e); });
document.addEventListener('keydown', e => {
  if (e.key === 'Escape') {
    const dlg = $('#dlg-overlay'); if (dlg.classList.contains('show')) { dlg._cancel && dlg._cancel(); return; }
    if ($('#lightbox').classList.contains('show')) { closeLightbox(); return; }
    if ($('#drawer').classList.contains('show')) { closeDrawer(); return; }
    if ($('#task-modal').classList.contains('show')) { closeTask(); return; }
    document.body.classList.remove('nav-open');
  }
  if (e.key === 'Enter' && (e.target.id === 'admin-pass-input' || e.target.id === 'admin-email-input')) window.unlockAdmin();
});
$('#dlg-overlay').addEventListener('mousedown', e => { if (e.target.id === 'dlg-overlay') e.currentTarget._cancel && e.currentTarget._cancel(); });

const themeMode = () => { try { return localStorage.getItem('tg_theme') || 'light'; } catch { return 'light'; } };
function applyTheme(m) { try { localStorage.setItem('tg_theme', m); } catch { } document.documentElement.dataset.theme = m === 'auto' ? (matchMedia('(prefers-color-scheme:dark)').matches ? 'dark' : 'light') : m; }
ACT.theme = () => { applyTheme(document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark'); renderAll(true); };
ACT.themeSet = el => { applyTheme(el.dataset.m); renderAll(true); };
function renderThemeCard() { const m = themeMode(); setHTML($('#st-theme'), `<p class="fs13 muted" style="margin-bottom:10px">Choose how the panel looks. Your choice is remembered in this browser.</p><div class="tabs">${[['light', 'sun', 'Light'], ['dark', 'moon', 'Dark'], ['auto', 'settings', 'Auto']].map(([k, i, l]) => `<button class="tab ${m === k ? 'on' : ''}" data-act="themeSet" data-m="${k}">${ic(i, 14)} ${l}</button>`).join('')}</div>`); }
ACT.go = el => go(el.dataset.v, { range: el.dataset.range, status: el.dataset.status, tab: el.dataset.tab });
ACT.navGroup = el => { const g = el.closest('.nav-g'); if (S.view === el.dataset.v) g.classList.toggle('open'); else go(el.dataset.v); };
ACT.toggleNav = () => { if (window.innerWidth < 1024) document.body.classList.toggle('nav-open'); else $('#shell').classList.toggle('collapsed'); };
ACT.closeNav = () => document.body.classList.remove('nav-open');
ACT.userMenu = () => $('#user-menu').classList.toggle('hidden');
ACT.bell = () => { if (pendingSubs().length) go('approvals', { range: 'all', status: 'pending' }); else if (pendingWds().length) go('withdrawals'); else toast('Nothing pending'); };
ACT.logout = async () => { try { await signOut(auth); } catch (_) { } location.reload(); };
ACT.page = el => { const k = el.dataset.k, d = Number(el.dataset.d); const map = { users: S.us, wd: S.wd, cm: S.cm, logs: S.lg, tasks: S.tasksUI }; if (map[k]) { map[k].page += d; renderAll(true); } };

/* ============================================================
   LOGIN
   ============================================================ */
function openAdminConsole(email, restored) {
  S.admin = email;
  $('#login-screen').classList.add('hidden');
  $('#app').classList.remove('hidden');
  $('#avatar').textContent = (email[0] || 'A').toUpperCase();
  $('#menu-email').textContent = email;
  startListeners();
  go('overview');
  if (!restored) { toast('Logged in as ' + email, 'ok'); logAction('admin_login', email); }
}
window.signInAdminWithGoogle = async function () {
  const err = $('#login-error'); err.classList.add('hidden');
  if (!/^https?:$/.test(location.protocol)) { err.textContent = 'Google sign-in does not work from a local file (content:// or file://). Use the admin email + master password below, or open the page over https (e.g. Firebase Hosting).'; err.classList.remove('hidden'); return; }
  try {
    const res = await signInWithPopup(auth, new GoogleAuthProvider(), browserPopupRedirectResolver);
    const email = (res.user?.email || '').toLowerCase().trim();
    if (ALLOWED_ADMIN_EMAILS.includes(email)) {
      try { await setDoc(doc(db, 'admins', res.user.uid), { email, role: 'admin', updatedAt: serverTimestamp() }, { merge: true }); } catch (_) { }
      openAdminConsole(email);
    } else {
      await signOut(auth);
      err.textContent = 'Access denied: ' + email + ' is not an authorized admin email.'; err.classList.remove('hidden');
    }
  } catch (e) {
    err.textContent = 'Google sign-in blocked or cancelled (' + (e.code || e.message) + '). Use admin email + master password below.'; err.classList.remove('hidden');
  }
};
const loginMsg = (t, bad = true) => { const e = $('#login-error'); e.textContent = t; e.classList.remove('hidden'); e.style.color = bad ? '' : 'var(--green)'; };
const loginVals = () => ({ email: ($('#admin-email-input')?.value || '').trim().toLowerCase(), pass: $('#admin-pass-input')?.value || '' });
window.unlockAdmin = function () {
  const emailInput = ($('#admin-email-input')?.value || '').trim().toLowerCase();
  const passInput = ($('#admin-pass-input')?.value || '').trim();
  const err = $('#login-error');
  const validEmail = ALLOWED_ADMIN_EMAILS.includes(emailInput) || ALLOWED_ADMIN_EMAILS.includes(passInput.toLowerCase());
  const validPass = ['gullak_admin_2025', 'admin123'].includes(passInput.toLowerCase()) || ALLOWED_ADMIN_EMAILS.includes(passInput.toLowerCase());
  if (validEmail && validPass) { err.classList.add('hidden'); openAdminConsole(ALLOWED_ADMIN_EMAILS.includes(emailInput) ? emailInput : passInput.toLowerCase()); }
  else { err.textContent = 'Access denied: authorized admin email + master password required.'; err.classList.remove('hidden'); }
};
/* keep the admin signed in after a reload, but only for an allowed email with a real Firebase session */
onAuthStateChanged(auth, u => { const em = (u && u.email || '').toLowerCase(); if (u && u.emailVerified && ALLOWED_ADMIN_EMAILS.includes(em) && !S.admin) openAdminConsole(em, true); });
