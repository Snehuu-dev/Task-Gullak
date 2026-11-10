'use strict';
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
