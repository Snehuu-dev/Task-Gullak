'use strict';
/* ============================================================
   MONTHLY RECORDS / REPORTS
   ============================================================ */
const monthLabel = key => { const [y, m] = key.split('-').map(Number); return new Date(y, m - 1, 1).toLocaleDateString('en-IN', { month: 'long', year: 'numeric' }); };
const shiftMonth = (key, d) => { const [y, m] = key.split('-').map(Number); return monthKey(new Date(y, m - 1 + d, 1).getTime()); };
RENDER.reports = () => {
  const key = S.rp.month, m = monthStats(key);
  const mi = $('#rp-month'); if (document.activeElement !== mi) mi.value = key;
  const k = (cls, icon, l, v, sub) => `<div class="kpi s ${cls}"><div class="ico">${ic(icon, 22)}</div><div class="grow"><div class="lbl">${l}</div><div class="val">${v}</div>${sub ? `<div class="fs11 muted">${sub}</div>` : ''}</div></div>`;
  setHTML($('#rp-kpi'),
    k('blue', 'tasks', 'Task submissions', m.subs.length, `${m.pn.length} pending`) + k('green', 'check', 'Approved', m.ap.length, `${m.rj.length} rejected`) +
    k('purple', 'users', 'New users', m.newUsers, `${m.active} active`) + k('orange', 'cash', 'Task rewards (approved)', inr(m.rewards), '') +
    k('teal', 'download', 'Withdrawals requested', inr(m.requested), `${m.wds.length} requests`) + k('green', 'wallet', 'Paid payouts', inr(m.paid), '') +
    k('red', 'clock', 'Pending payout liability', inr(m.pending), '') + k(m.net < 0 ? 'red' : 'green', 'trend', 'Estimated net profit', m.hasFin ? inr(m.net) : '—', m.hasFin ? '' : 'Enter revenue below'));
  const ri = $('#rp-rev'), ei = $('#rp-exp'), tag = key + ':' + (S.meta['finance_' + key] ? 'db' : 'x');
  if (S.rp.loaded !== tag && document.activeElement !== ri && document.activeElement !== ei) { const f = getFinance(key); ri.value = f.revenue || ''; ei.value = f.expenses || ''; S.rp.loaded = tag; }
  setHTML($('#rp-net'), `<div class="mini" style="grid-template-columns:repeat(4,1fr)"><div><b>${inr(m.revenue)}</b>Revenue</div><div><b>${inr(m.rewards)}</b>Rewards</div><div><b>${inr(m.expenses)}</b>Expenses</div><div><b class="${m.net < 0 ? 'bad-t' : 'ok-t'}">${inr(m.net)}</b>Net</div></div>`);
  const rows = [5, 4, 3, 2, 1, 0].map(i => monthStats(shiftMonth(monthKey(Date.now()), -i))).reverse();
  setHTML($('#rp-months'), `<div class="table"><div class="thead cols-rep"><span>Month</span><span>Subs</span><span>Approved</span><span>Rewards</span><span>Paid</span><span>Revenue</span><span>Net</span></div>${rows.map(r => `<div class="trow cols-rep fs12"><span class="b">${monthLabel(r.key)}</span><span data-l="Subs">${r.subs.length}</span><span data-l="Approved">${r.ap.length}</span><span data-l="Rewards">${inr(r.rewards)}</span><span data-l="Paid">${inr(r.paid)}</span><span data-l="Revenue">${inr(r.revenue)}</span><span class="b ${r.net < 0 ? 'bad-t' : ''}" data-l="Net">${r.hasFin ? inr(r.net) : '—'}</span></div>`).join('')}</div>`);
  const per = {}; m.subs.forEach(s => { const t = taskOfSub(s); const n = t ? taskName(t) : (s.taskTitle || 'Unknown'); const o = per[n] = per[n] || { n, sub: 0, ap: 0, rj: 0, pn: 0, rw: 0 }; o.sub++; const st = subSt(s); if (st === 'approved') { o.ap++; o.rw += num(s.rewardAmount); } else if (st === 'rejected') o.rj++; else o.pn++; });
  const tl = Object.values(per).sort((a, b) => b.sub - a.sub);
  setHTML($('#rp-tasks'), tl.length ? `<div class="table"><div class="thead cols-task"><span>Task</span><span>Submitted</span><span>Approved</span><span>Rejected</span><span>Pending</span><span>Rewards</span></div>${tl.map(o => `<div class="trow cols-task fs12"><span class="b trunc">${esc(o.n)}</span><span data-l="Submitted">${o.sub}</span><span data-l="Approved">${o.ap}</span><span data-l="Rejected">${o.rj}</span><span data-l="Pending">${o.pn}</span><span data-l="Rewards">${inr(o.rw)}</span></div>`).join('')}</div>` : emptyHTML('chart', 'No submissions in this month.'));
};
INP.rpMonth = el => { if (el.value) { S.rp.month = el.value; renderAll(); } };
ACT.rpPrev = () => { S.rp.month = shiftMonth(S.rp.month, -1); renderAll(); };
ACT.rpNext = () => { S.rp.month = shiftMonth(S.rp.month, 1); renderAll(); };
ACT.rpSave = async () => {
  const key = S.rp.month, data = { revenue: num($('#rp-rev').value), expenses: num($('#rp-exp').value) };
  try { await setDoc(doc(db, 'adminMeta', 'finance_' + key), { ...data, updatedBy: S.admin, updatedAt: serverTimestamp() }, { merge: true }); toast('Saved for ' + monthLabel(key), 'ok'); }
  catch (e) { localStorage.setItem('anytask_finance_' + key, JSON.stringify(data)); toast('Firestore rules blocked the write — saved locally in this browser', 'bad'); }
  logAction('finance_update', key, data); S.rp.loaded = null; renderAll();
};
ACT.rpCsv = () => {
  const m = monthStats(S.rp.month);
  const rows = [[`Monthly report — ${monthLabel(m.key)} (device timezone)`], [],
  ['Metric', 'Value'], ['Task submissions', m.subs.length], ['Approved', m.ap.length], ['Rejected', m.rj.length], ['Pending', m.pn.length], ['New users', m.newUsers], ['Active users', m.active],
  ['Task rewards (approved)', m.rewards], ['Withdrawals requested', m.requested], ['Paid payouts', m.paid], ['Pending payout liability', m.pending], ['Platform revenue', m.revenue], ['Operational expenses', m.expenses], ['Net profit', m.net], [],
  ['Withdrawal ID', 'User ID', 'Amount', 'Status', 'Requested', 'UTR']];
  m.wds.forEach(w => rows.push([w.id, w.userId, num(w.amount), wdSt(w), fmtDT(wdMs(w)), w.utrNumber || '']));
  saveText(`report_${m.key}.csv`, toCsv(rows), 'text/csv'); logAction('export_report_csv', m.key);
};
ACT.rpPrint = () => window.print();
