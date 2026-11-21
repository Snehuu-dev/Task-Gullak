'use strict';
/* ============================================================
   FINANCE / MONTH STATS (shared by dashboard + reports)
   ============================================================ */
function getFinance(key) {
  const fromDb = S.meta['finance_' + key];
  if (fromDb) return fromDb;
  try { return JSON.parse(localStorage.getItem('anytask_finance_' + key) || '{}'); } catch { return {}; }
}
function monthStats(key) {
  const [a, b] = monthRange(key);
  const inR = t => t >= a && t < b;
  const subs = S.subs.filter(s => inR(subMs(s) || Date.now()));
  const ap = subs.filter(s => subSt(s) === 'approved'), rj = subs.filter(s => subSt(s) === 'rejected'), pn = subs.filter(s => subSt(s) === 'pending');
  const rewards = ap.reduce((n, s) => n + num(s.rewardAmount), 0);
  const act = new Set(subs.map(s => s.userId).filter(Boolean));
  S.users.forEach(u => { if (inR(userSeen(u))) act.add(u.id); });
  const wds = S.wds.filter(w => inR(wdMs(w)));
  const sum = l => l.reduce((n, w) => n + num(w.amount), 0);
  const fin = getFinance(key);
  const revenue = num(fin.revenue), expenses = num(fin.expenses);
  return {
    key, subs, ap, rj, pn, rewards, newUsers: S.users.filter(u => inR(userMs(u))).length, active: act.size,
    wds, requested: sum(wds), paid: sum(wds.filter(w => wdSt(w) === 'approved')),
    pending: sum(wds.filter(w => ['pending', 'processing'].includes(wdSt(w)))), revenue, expenses, net: revenue - rewards - expenses, hasFin: !!(fin.revenue || fin.expenses)
  };
}
const dayBins = (items, msFn, n = 7) => {
  const out = Array(n).fill(0), end = new Date(); end.setHours(24, 0, 0, 0);
  items.forEach(it => { const t = msFn(it); const i = n - 1 - Math.floor((end.getTime() - 1 - t) / DAY); if (t && i >= 0 && i < n) out[i]++; });
  return out;
};
const approvalMs = s => tsMs(s.updatedAt) || subMs(s);
