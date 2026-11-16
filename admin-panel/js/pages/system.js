'use strict';
/* Maintenance, settings, activity log, developer tools */

/* ============================================================
   MAINTENANCE
   ============================================================ */
RENDER.maintenance = () => {
  const md = maintDoc(), on = isMaintActive();
  setHTML($('#mt-pill'), on ? '<span class="pill bad">LOCKDOWN ACTIVE</span>' : '<span class="pill ok">LIVE (NORMAL)</span>');
  setHTML($('#mt-body'), `<div class="card flat" style="background:var(--soft)"><div class="b fs13">${esc(md.title || MAINT_TITLE)}</div><p class="fs13 ink2 mt4" style="line-height:1.55">${esc(md.message || MAINT_MSG)}</p><div class="fs11 muted mt8">Last updated ${fmtDT(tsMs(md.updatedAt))}</div></div>`);
};
async function saveMaintenance(active, title, message) {
  await setDoc(doc(db, 'broadcasts', 'system_maintenance_banner'), { active, title, message, type: 'alert', targetUserId: 'all', updatedAt: serverTimestamp(), createdAt: serverTimestamp() }, { merge: true });
}
ACT.maintToggle = async () => {
  const on = isMaintActive(), md = maintDoc();
  if (!await confirmBox(on ? 'Turn maintenance OFF?' : 'Turn maintenance ON?', on ? 'The user app returns to normal and actions are unlocked.' : 'The user app will show a maintenance banner and actions will be locked.', { ok: on ? 'Turn OFF' : 'Turn ON', danger: !on })) return;
  await saveMaintenance(!on, md.title || MAINT_TITLE, md.message || MAINT_MSG);
  logAction('maintenance_toggle', 'system_maintenance_banner', { before: on, after: !on }); toast(!on ? 'Maintenance ENABLED' : 'Maintenance DISABLED', 'ok');
};
ACT.maintEdit = async () => {
  const md = maintDoc();
  const v = await ask({ title: 'Update maintenance message', ok: 'Save message', fields: [{ id: 'title', label: 'Title', value: md.title || MAINT_TITLE, required: true }, { id: 'msg', label: 'Message shown in user app', type: 'textarea', value: md.message || MAINT_MSG, required: true }] });
  if (!v) return; await saveMaintenance(isMaintActive(), v.title, v.msg); logAction('maintenance_message', 'system_maintenance_banner', { title: v.title }); toast('Message updated', 'ok');
};

/* ============================================================
   SETTINGS
   ============================================================ */
RENDER.settings = () => {
  renderThemeCard();
  const kv = rows => `<dl class="kv">${rows.map(([k, v]) => `<dt>${k}</dt><dd>${v}</dd>`).join('')}</dl>`;
  setHTML($('#st-session'), kv([['Signed in as', esc(S.admin)], ['Access', 'Authorized admin (email whitelist)'], ['Browser time', esc(Intl.DateTimeFormat().resolvedOptions().timeZone)]]) + `<div class="row wrap mt16"><button class="btn bad sm" data-act="logout">${ic('logout', 14)}Log out</button></div><div class="fs12 muted mt12">Authorized emails:</div><div class="col mt4" style="gap:4px">${ALLOWED_ADMIN_EMAILS.map(e => `<span class="mono fs12">${esc(e)}</span>`).join('')}</div>`);
  setHTML($('#st-app'), kv([['Firebase project', esc(firebaseConfig.projectId)], ['Collections', 'users, tasks, offers, submissions, withdrawals, broadcasts, notifications, adminLogs, adminMeta'], ['Date handling', 'All dates use this device’s timezone'], ['Panel', 'TASK GULLAK Admin v2']]));
  setHTML($('#st-sec'), [['Admin login uses a master password stored in this page’s source (anyone who views the source can read it)', 0], ['Sensitive actions need confirmation (reject, delete, block, payout)', 1], ['Audit log for approvals, payouts, wallet, maintenance, notifications', 1], ['Approve credits the task’s own reward, verified inside the transaction', 1], ['Bank account numbers masked by default (reveal is logged)', 1], ['Firestore rules cannot restrict admin data while the master-password login is used (no Firebase session)', 0], ['Deploy the Cloud Functions for daily cleanup, push and screenshot storage', 0]].map(([t, ok]) => `<div class="note ${ok ? 'ok' : 'warn'}">${ic(ok ? 'check' : 'alert', 16)}<div>${t}</div></div>`).join(''));
};

/* ============================================================
   ACTIVITY LOGS
   ============================================================ */
const logMs = l => tsMs(l.createdAt) || num(l.createdMs);
const allLogs = () => { let loc = []; try { loc = JSON.parse(localStorage.getItem('tg_logs') || '[]'); } catch { } return [...S.logs, ...loc].sort((a, b) => logMs(b) - logMs(a)); };
function logsFiltered() {
  const f = S.lg, q = f.q.trim().toLowerCase();
  return allLogs().filter(l => (f.action === 'all' || l.action === f.action) && (!q || [l.adminEmail, l.action, l.target, JSON.stringify(l.details || {})].join(' ').toLowerCase().includes(q)));
}
RENDER.logs = () => {
  const acts = [...new Set(allLogs().map(l => l.action))].sort();
  syncSelect($('#lg-action'), '<option value="all">All actions</option>' + acts.map(a => `<option value="${esc(a)}">${esc(a)}</option>`).join(''), S.lg.action);
  setHTML($('#lg-note'), S.errs.adminLogs ? `<div class="note warn">${ic('alert', 16)}<div>Firestore rules are blocking adminLogs (${esc(S.errs.adminLogs)}) — logs are currently saved only in this browser. Allow the <span class="mono">adminLogs</span> collection in your Firestore rules.</div></div>` : '');
  const p = paginate(logsFiltered(), 'logs', 20, S.lg);
  setHTML($('#lg-list'), p.items.map(l => `<div class="trow cols-log fs12"><span data-l="Time">${fmtDT(logMs(l))}</span><span class="trunc" data-l="Admin">${esc(l.adminEmail || '')}</span><span data-l="Action"><span class="pill info">${esc(l.action)}</span></span><span class="mono trunc" data-l="Target">${esc(l.target || '—')}</span><span class="muted trunc mono" data-l="Details" title="${esc(JSON.stringify(l.details || {}))}">${esc(JSON.stringify(l.details || {}))}</span></div>`).join('') || emptyHTML('activity', 'No log entries yet.'));
  setHTML($('#lg-pager'), pagerHTML(p));
};
INP.lgQ = el => { S.lg.q = el.value; S.lg.page = 1; renderAll(); };
INP.lgAction = el => { S.lg.action = el.value; S.lg.page = 1; renderAll(); };
ACT.lgCsv = () => saveText(`audit_log_${Date.now()}.csv`, toCsv([['Time', 'Admin', 'Action', 'Target', 'Details'], ...logsFiltered().map(l => [fmtDT(logMs(l)), l.adminEmail, l.action, l.target, JSON.stringify(l.details || {})])]), 'text/csv');

/* ============================================================
   DEVELOPER TOOLS
   ============================================================ */
function diagText() {
  return [`Panel: TASK GULLAK Admin v2`, `Project: ${firebaseConfig.projectId}`, `Admin: ${S.admin}`, `Time: ${new Date().toISOString()}`,
  ...Object.keys({ users: 1, tasks: 1, submissions: 1, withdrawals: 1, broadcasts: 1, adminLogs: 1, adminMeta: 1 }).map(k => `${k}: ${S.live[k] ? 'live' : 'down'}${S.errs[k] ? ' (' + S.errs[k] + ')' : ''}`),
  `Users ${S.users.length} • Tasks ${S.tasks.length} • Submissions ${S.subs.length} • Withdrawals ${S.wds.length}`, `UA: ${navigator.userAgent}`].join('\n');
}
RENDER.devtools = () => {
  const names = { users: 'users', tasks: 'tasks', submissions: 'submissions', withdrawals: 'withdrawals', broadcasts: 'broadcasts', adminLogs: 'adminLogs', adminMeta: 'adminMeta' };
  const counts = { users: S.users.length, tasks: S.tasks.length, submissions: S.subs.length, withdrawals: S.wds.length, broadcasts: S.bcasts.length, adminLogs: S.logs.length, adminMeta: Object.keys(S.meta).length };
  setHTML($('#dv-conn'), Object.keys(names).map(k => `<div class="lrow"><span class="dot6" style="background:${S.live[k] ? '#22C55E' : S.errs[k] ? '#EF4444' : '#F59E0B'};width:10px;height:10px"></span><div class="grow"><div class="b fs13 mono">${k}</div>${S.errs[k] ? `<div class="fs11 bad-t">${esc(S.errs[k])}</div>` : ''}</div><span class="tag">${counts[k]} docs</span></div>`).join(''));
  $('#dv-diag').textContent = diagText();
};
ACT.dvCopy = () => copyText(diagText());
ACT.dvExport = el => {
  const k = el.dataset.k; let data = S[k];
  if (k === 'subs') data = S.subs.map(({ screenshotBase64, ...r }) => ({ ...r, hasScreenshot: !!screenshotBase64 }));
  saveText(`${k}_${Date.now()}.json`, JSON.stringify(data, (kk, v) => (v && typeof v === 'object' && typeof v.toMillis === 'function') ? new Date(v.toMillis()).toISOString() : v, 2), 'application/json');
  logAction('export_json', k);
};
