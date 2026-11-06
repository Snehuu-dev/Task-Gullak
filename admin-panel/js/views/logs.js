'use strict';
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
