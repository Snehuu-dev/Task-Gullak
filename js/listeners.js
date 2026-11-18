'use strict';
/* Live Firestore listeners, finance stats, render dispatch */

/* ============================================================
   REALTIME LISTENERS
   ============================================================ */
let raf = 0;
const schedule = () => { if (bulkRun) return; cancelAnimationFrame(raf); raf = requestAnimationFrame(() => renderAll()); };

/* Submitted = hidden: once a user has submitted a task, add them to that task's blocked list so it never shows for them again.
   You decide per upload: edit the task and choose "Show it again to everyone who already submitted" (sets reshowAt = now). Only submissions made after that are counted. */
let syncT = 0, syncBusy = false;
const queueSync = () => { clearTimeout(syncT); syncT = setTimeout(syncSubmitters, 1500); };
async function syncSubmitters() {
  if (syncBusy || !S.admin || !S.live.tasks || !S.live.submissions) return;
  const jobs = {}, mig = [];
  for (const t of S.tasks) if ((t.autoBlockedUserIds || []).length) mig.push(t);   // tidy-up: earlier version had put submitters into the blocked list
  for (const sb of S.subs) {
    if (!sb.userId) continue; const t = taskOfSub(sb); if (!t || t.hideAfterComplete === false) continue;
    const re = num(t.reshowAt); if (re && subMs(sb) && subMs(sb) <= re) continue;
    if ((t.submittedUserIds || []).includes(sb.userId) || (t.autoBlockedUserIds || []).includes(sb.userId)) continue;
    (jobs[t.id] = jobs[t.id] || new Set()).add(sb.userId);
  }
  if (!mig.length && !Object.keys(jobs).length) return;
  syncBusy = true;
  try {
    for (const t of mig) { const u = t.autoBlockedUserIds; await updateDoc(doc(db, 'tasks', t.id), { blockedUserIds: arrayRemove(...u), submittedUserIds: arrayUnion(...u), autoBlockedUserIds: [] }); }
    for (const tid of Object.keys(jobs)) { const u = [...jobs[tid]]; await updateDoc(doc(db, 'tasks', tid), { submittedUserIds: arrayUnion(...u), updatedAt: serverTimestamp() }); }
  } catch (e) { console.warn('submitter sync', e.code || e.message); }
  syncBusy = false;
}

/* One person = one user. The same email can exist as two Firestore docs (Google login -> Firebase UID doc, email/password -> "user_<email>" doc).
   We merge them by email so the admin sees a single user. The UID doc wins; its wallet is shown (never added twice). */
const isLegacyId = id => /^user_/i.test(String(id)) || /@/.test(String(id));
const canon = id => (S.uidAlias && S.uidAlias.get(id)) || id;
function dedupeUsers(list) {
  const g = new Map();
  list.forEach(u => { const em = String(u.email || '').trim().toLowerCase(); const k = em || ('id:' + u.id); if (!g.has(k)) g.set(k, []); g.get(k).push(u); });
  const out = [];
  g.forEach(arr => {
    if (arr.length === 1) { out.push(arr[0]); return; }
    arr.sort((a, b) => (isLegacyId(a.id) - isLegacyId(b.id)) || (num(b.walletBalance) - num(a.walletBalance)) || (userSeen(b) - userSeen(a)));
    const p = { ...arr[0] }, rest = arr.slice(1);
    rest.forEach(o => Object.keys(o).forEach(k => { if (k !== 'id' && (p[k] === undefined || p[k] === null || p[k] === '')) p[k] = o[k]; }));
    p.isBanned = arr.some(x => x.isBanned);
    const seen = Math.max(...arr.map(userSeen)); if (seen) p.lastActiveAt = seen;
    const joins = arr.map(userMs).filter(Boolean); if (joins.length) p.createdAt = Math.min(...joins);
    p._aliases = rest.map(o => o.id);
    out.push(p);
  });
  return out;
}
let started = false;
function listen(key, ref, apply) {
  onSnapshot(ref, snap => { S.live[key] = true; S.errs[key] = null; apply(snap); schedule(); if (key === 'tasks' || key === 'submissions') queueSync(); },
    err => { S.live[key] = false; S.errs[key] = err.code || err.message; if (!['adminLogs', 'adminMeta'].includes(key)) console.warn('Listener', key, err.code || err.message); schedule(); });
}
function startListeners() {
  if (started) return; started = true; S.bootAt = Date.now(); setTimeout(schedule, 250); setTimeout(schedule, 8200); syncClock();
  listen('tasks', collection(db, 'tasks'), snap => { S.tasks = snap.docs.map(d => ({ id: d.id, ...d.data() })).sort((a, b) => tsMs(b.createdAt) - tsMs(a.createdAt)); setTimeout(sweepExpired, 800); setTimeout(sweepOldTasks, 1200); setTimeout(sweepOldBcasts, 1500); });
  listen('submissions', collection(db, 'submissions'), snap => { S.subs = snap.docs.map(d => ({ id: d.id, ...d.data() })).sort((a, b) => subMs(b) - subMs(a)); });
  listen('withdrawals', collection(db, 'withdrawals'), snap => { S.wds = snap.docs.map(d => ({ id: d.id, ...d.data() })).sort((a, b) => wdMs(b) - wdMs(a)); });
  listen('users', collection(db, 'users'), snap => { S.usersRaw = snap.docs.map(d => ({ id: d.id, ...d.data() })); S.users = dedupeUsers(S.usersRaw); S.uidAlias = new Map(); S.users.forEach(u => (u._aliases || []).forEach(a => S.uidAlias.set(a, u.id))); });
  /* less important data waits until the page is idle, so the first screen loads sooner */
  const later = f => (window.requestIdleCallback ? requestIdleCallback(f, { timeout: 1200 }) : setTimeout(f, 800));
  later(() => {
    listen('broadcasts', collection(db, 'broadcasts'), snap => { S.bcasts = snap.docs.map(d => ({ id: d.id, ...d.data() })).sort((a, b) => tsMs(b.createdAt) - tsMs(a.createdAt)); });
    listen('adminLogs', query(collection(db, 'adminLogs'), orderBy('createdMs', 'desc'), limit(300)), snap => { S.logs = snap.docs.map(d => ({ id: d.id, ...d.data() })); });
    listen('adminMeta', collection(db, 'adminMeta'), snap => { S.meta = {}; snap.docs.forEach(d => { S.meta[d.id] = d.data(); }); });
    startBulkListener();
  });
}

/* ============================================================
   FINANCE / MONTH STATS (shared by dashboard + reports)
   ============================================================ */
function getFinance(key) {
  const fromDb = S.meta['finance_' + key];
  if (fromDb) return fromDb;
  try { return JSON.parse(localStorage.getItem('anytask_finance_' + key) || '{}'); } catch { return {}; }
}
function monthStats(key) {
  const [a, b] = monthRange(key), inR = t => t >= a && t < b;
  const subs = S.subs.filter(s => inR(subMs(s) || Date.now()));                                   // submitted this month
  const ap = S.subs.filter(s => subSt(s) === 'approved' && inR(approvalMs(s)));                    // approved this month
  const rj = S.subs.filter(s => subSt(s) === 'rejected' && inR(approvalMs(s)));
  const pn = subs.filter(s => subSt(s) === 'pending');
  const spent = ap.reduce((n, s) => n + num(s.rewardAmount), 0);                                   // what you spent on rewards
  const act = new Set(subs.map(s => canon(s.userId)).filter(Boolean));
  S.users.forEach(u => { if (inR(userSeen(u))) act.add(u.id); });
  const wds = S.wds.filter(w => inR(wdMs(w)));
  const sum = l => l.reduce((n, w) => n + num(w.amount), 0);
  const payouts = S.wds.filter(w => wdSt(w) === 'approved' && inR(wdPaidMs(w)));                   // paid out this month
  return {
    key, subs, ap, rj, pn, rewards: spent, spent, newUsers: S.users.filter(u => inR(userMs(u))).length, active: act.size,
    wds, requested: sum(wds), payouts, paid: payouts.reduce((n, w) => n + (num(w.paidAmount) || num(w.amount)), 0),
    pending: sum(wds.filter(w => ['pending', 'processing'].includes(wdSt(w))))
  };
}
const dayBins = (items, msFn, n = 7) => {
  const out = Array(n).fill(0), end = new Date(); end.setHours(24, 0, 0, 0);
  items.forEach(it => { const t = msFn(it); const i = n - 1 - Math.floor((end.getTime() - 1 - t) / DAY); if (t && i >= 0 && i < n) out[i]++; });
  return out;
};
const approvalMs = s => tsMs(s.updatedAt) || subMs(s);

/* ============================================================
   RENDER DISPATCH
   ============================================================ */
const RENDER = {};

function renderAll(force) {
  { const tl = $('#top-load'); if (tl) tl.classList.toggle('on', !!S.admin && ['tasks', 'submissions', 'users', 'withdrawals'].some(k => !S.live[k] && !S.errs[k])); }
  if (!S.admin) return;
  renderChrome();
  const fn = RENDER[S.view]; if (fn) fn(force);
  renderBulk();
  if (S.drawer.uid) renderDrawer();
}
function renderChrome() {
  const ps = pendingSubs().length, pw = pendingWds().length;
  $('#bdg-appr').textContent = ps; $('#bdg-appr').classList.toggle('hidden', !ps);
  { const bp = bulkRows().filter(r => r.status === 'pending').length, bb = $('#bdg-bulk'); if (bb) { bb.textContent = bp; bb.classList.toggle('hidden', !bp); } }
  $('#bdg-wd').textContent = pw; $('#bdg-wd').classList.toggle('hidden', !pw);
  const bell = ps + pw; $('#bell-cnt').textContent = bell > 99 ? '99+' : bell; $('#bell-cnt').classList.toggle('hidden', !bell);
  const core = ['users', 'tasks', 'submissions', 'withdrawals', 'broadcasts'];
  const errs = core.filter(k => S.errs[k]), okAll = core.every(k => S.live[k]);
  const c = $('#conn');
  c.classList.toggle('off', !okAll);
  c.lastElementChild.textContent = errs.length ? 'Check access' : okAll ? 'Online' : 'Connecting…';
  const on = isMaintActive();
  ['ov-switch', 'mt-switch'].forEach(id => { const e = $('#' + id); if (e) { e.classList.toggle('on', on); e.setAttribute('aria-pressed', on); } });
}
