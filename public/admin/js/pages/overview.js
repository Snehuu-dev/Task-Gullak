'use strict';
/* Dashboard (monthly overview) */

/* ============================================================
   OVERVIEW
   ============================================================ */
const sparkSvg = (v, c) => { const mx = Math.max(1, ...v), w = 84, h = 34, st = w / (v.length - 1), pts = v.map((x, i) => [i * st, h - 3 - (x / mx) * (h - 8)]), d = pts.map((q, i) => (i ? 'L' : 'M') + q[0].toFixed(1) + ' ' + q[1].toFixed(1)).join(' '); return `<svg width="${w}" height="${h}" viewBox="0 0 ${w} ${h}" aria-hidden="true"><path d="${d} L${w} ${h} L0 ${h}Z" fill="${c}" fill-opacity=".14"/><path d="${d}" fill="none" stroke="${c}" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/></svg>`; };
const KV = {};
function countUp() {
  const reduce = window.matchMedia && matchMedia('(prefers-reduced-motion: reduce)').matches;
  $$('#v-overview .kpi .val').forEach((el, i) => {
    const raw = el.textContent, n = parseFloat(raw.replace(/[^0-9.]/g, '')); if (!isFinite(n) || reduce) return;
    const from = KV[i] === undefined ? 0 : KV[i]; KV[i] = n; if (from === n) return;
    const pre = raw.match(/^[^0-9]*/)[0], t0 = performance.now(), dur = 800;
    const tick = t => { const k = Math.min(1, (t - t0) / dur), e = 1 - Math.pow(1 - k, 3), v = Math.round(from + (n - from) * e); el.textContent = pre + v.toLocaleString('en-IN'); if (k < 1) requestAnimationFrame(tick); else el.textContent = raw; };
    requestAnimationFrame(tick);
  });
}
RENDER.overview = () => {
  const now = Date.now();
  if (!S.live.users && !S.live.submissions && !S.users.length && !S.subs.length) {
    const bad = !navigator.onLine || S.errs.users || S.errs.submissions;
    const clear = () => { setHTML($('#kpi-a'), ''); setHTML($('#kpi-b'), ''); setHTML($('#chart'), ''); setHTML($('#ov-month'), ''); };
    if (bad) {
      setHTML($('#ov-hero'), `<div class="note bad" style="width:100%;background:var(--red-bg)">${ic('alert', 18)}<div class="grow"><b>${navigator.onLine ? 'Could not load data' : 'No internet connection'}</b><div class="fs12">${navigator.onLine ? esc(String(S.errs.users || S.errs.submissions)) : 'Check your mobile data / Wi-Fi — the dashboard will load by itself when you are back online.'}</div></div><button class="btn ghost xs" data-act="reloadPage">Retry</button></div>`);
      clear(); return;
    }
    if (Date.now() - (S.bootAt || 0) < 200) { setHTML($('#ov-hero'), ''); clear(); return; }   // fast connection: no flash of loader
    setHTML($('#ov-hero'), ''); setHTML($('#ov-month'), ''); setHTML($('#kpi-b'), ''); setHTML($('#chart'), ''); setHTML($('#kpi-a'), LOADER('Loading dashboard')); return;
  }
  { const h = new Date().getHours(), g = h < 12 ? 'Good morning' : h < 17 ? 'Good afternoon' : 'Good evening', nm = String(S.admin || 'Admin').split('@')[0], t0 = new Date().setHours(0, 0, 0, 0);
    const hs = (ico, l, v) => `<div class="hs"><div class="hi">${ic(ico, 18)}</div><div><b>${v}</b><span>${l}</span></div></div>`;
    setHTML($('#ov-hero'), `<div class="hero-l"><div class="hero-hi">${g}, ${esc(nm)}</div><div class="hero-d">${new Date().toLocaleDateString('en-IN', { weekday: 'long', day: 'numeric', month: 'long' })} • ${pendingSubs().length} pending approvals</div></div><div class="row gap6"><button class="hbtn solid" data-act="go" data-v="approvals" data-range="all" data-status="pending">${ic('approve', 14)}Approvals</button><button class="hbtn" data-act="openTask">${ic('plus', 14)}Add task</button></div>`); }
  // ---- month selector (everything in the cards + chart + activity follows the chosen month) ----
  const mk = S.ov.month, curMk = monthKey(now), [ma, mb] = monthRange(mk), inM = t => t >= ma && t < mb, isCur = mk === curMk;
  const mLabel = new Date(ma).toLocaleDateString('en-IN', { month: 'long', year: 'numeric' });
  setHTML($('#ov-month'), `<button class="iconbtn sm" data-act="ovPrev" aria-label="Previous month">${ic('chev-left', 16)}</button><div class="ml">${esc(mLabel)}${isCur ? ' <span class="pill info" style="margin-left:6px">This month</span>' : ''}</div><button class="iconbtn sm" data-act="ovNext" aria-label="Next month" ${isCur ? 'disabled style="opacity:.4;cursor:not-allowed"' : ''}>${ic('chev-right', 16)}</button><button class="btn soft xs" data-act="ovNow" ${isCur ? 'disabled' : ''}>Current</button>`);
  const ms = monthStats(mk), prevMk = monthKey(new Date(new Date(ma).getFullYear(), new Date(ma).getMonth() - 1, 1).getTime()), pms = monthStats(prevMk);
  const delta = (cur, prev) => prev > 0 ? `${cur >= prev ? '↑ +' : '↓ '}${(((cur - prev) / prev) * 100).toFixed(0)}% vs last month` : 'vs last month: —';
  const tasksM = S.tasks.filter(t => inM(tsMs(t.createdAt))).length;
  const kpi = (cls, icon, label, val, sub) => `<div class="kpi ${cls}"><div class="ico">${ic(icon, 26)}</div><div class="grow"><div class="lbl">${label}</div><div class="val">${val}</div><div class="dl">${sub}</div></div></div>`;
  setHTML($('#kpi-a'),
    kpi('blue', 'users', 'New Users', ms.newUsers.toLocaleString('en-IN'), `${delta(ms.newUsers, pms.newUsers)} • total ${S.users.length.toLocaleString('en-IN')}`) +
    kpi('green', 'check', 'Active Users', ms.active.toLocaleString('en-IN'), `active in ${new Date(ma).toLocaleDateString('en-IN', { month: 'short' })}`) +
    kpi('purple', 'tasks', 'Tasks Added', tasksM.toLocaleString('en-IN'), `${ms.subs.length.toLocaleString('en-IN')} submissions • ${S.tasks.length} tasks total`));
  const paidM = ms.paid, pendAmt = pendingWds().reduce((n, w) => n + num(w.amount), 0);
  const sm = (cls, icon, label, val, sub, act) => `<div class="kpi s ${cls}"><div class="ico">${ic(icon, 24)}</div><div class="grow"><div class="lbl">${label}</div><div class="val">${val}</div>${sub ? `<div class="dl">${sub}</div>` : ''}</div><button class="go" data-act="go" data-v="${act[0]}" ${act[1] ? `data-range="all" data-status="${act[1]}"` : ''} aria-label="Open">${ic('arrow-right', 16)}</button></div>`;
  setHTML($('#kpi-b'),
    sm('orange', 'check', 'Pending (month)', ms.pn.length.toLocaleString('en-IN'), `<span class="muted">All pending ${pendingSubs().length}</span>`, ['approvals', 'pending']) +
    sm('red', 'x', 'Rejected (month)', ms.rj.length.toLocaleString('en-IN'), '', ['approvals', 'rejected']) +
    sm('green', 'wallet', 'Paid Out (month)', inr(paidM), `<span class="muted">Pending ${inr(pendAmt)}</span>`, ['withdrawals']) +
    sm('purple', 'trend', 'Spent (month)', inr(ms.spent), `<span class="muted">${ms.ap.length} approved • ${delta(ms.spent, pms.spent)}</span>`, ['reports']));
  countUp();

  // chart: day-by-day inside the chosen month
  const days = Math.round((mb - ma) / DAY), bins = [], labels = [];
  for (let i = 0; i < days; i++) {
    const a0 = new Date(new Date(ma).getFullYear(), new Date(ma).getMonth(), i + 1).getTime(), b0 = new Date(new Date(ma).getFullYear(), new Date(ma).getMonth(), i + 2).getTime();
    bins.push(ms.ap.filter(x => approvalMs(x) >= a0 && approvalMs(x) < b0).reduce((n, x) => n + num(x.rewardAmount), 0)); labels.push(String(i + 1));
  }
  $('#chart-month').textContent = mLabel;
  $('#chart-total').textContent = inr(ms.spent);
  const dl = $('#chart-delta');
  if (pms.spent > 0) { const pc = ((ms.spent - pms.spent) / pms.spent) * 100; dl.textContent = (pc >= 0 ? '↑ +' : '↓ ') + pc.toFixed(1) + '% vs ' + new Date(monthRange(prevMk)[0]).toLocaleDateString('en-IN', { month: 'short' }); dl.style.color = pc >= 0 ? 'var(--green)' : 'var(--red)'; }
  else { dl.textContent = ms.spent ? 'No previous month data' : 'No approved rewards this month'; dl.style.color = ''; }
  setHTML($('#chart'), lineChart(bins, labels));

  // recent activity (selected month)
  const ev = [];
  S.users.forEach(u => { if (inM(userMs(u))) ev.push({ t: userMs(u), icon: 'user-plus', c: 'green', dot: '#EF4444', ti: 'New User Registered', su: 'ID: ' + (u.userId || u.id).slice(0, 12) }); });
  S.subs.forEach(s => { const st = subSt(s), t = st === 'pending' ? subMs(s) : approvalMs(s); if (!inM(t)) return; ev.push({ t, icon: st === 'approved' ? 'check' : st === 'rejected' ? 'x' : 'upload', c: st === 'approved' ? 'green' : st === 'rejected' ? 'red' : 'blue', dot: '#F59E0B', ti: st === 'approved' ? 'Task Approved' : st === 'rejected' ? 'Task Rejected' : 'New Submission', su: (s.taskTitle || 'Task') + ' • ' + (s.userName || 'User') }); });
  S.wds.forEach(w => { if (inM(wdMs(w))) ev.push({ t: wdMs(w), icon: 'download', c: 'blue', dot: '#22C55E', ti: 'Withdrawal Request', su: inr(w.amount) + ' via ' + (wdMethod(w) === 'upi' ? 'UPI' : wdMethod(w) === 'bank' ? 'Bank' : 'wallet') }); });
  S.bcasts.filter(b => b.id !== 'system_maintenance_banner').forEach(b => { const t = tsMs(b.createdAt) || num(b.timestamp); if (inM(t)) ev.push({ t, icon: 'bell', c: 'purple', dot: '#8B5CF6', ti: 'Notification Sent', su: b.title || '' }); });
  ev.sort((a, b) => b.t - a.t);
  setHTML($('#recent'), ev.slice(0, 6).map(e => `<div class="lrow"><div class="rico ${e.c}">${ic(e.icon, 18)}</div><div class="grow"><div class="b fs13">${esc(e.ti)}</div><div class="fs12 muted trunc">${esc(e.su)}</div></div><div class="fs12 muted shrink0">${ago(e.t)}</div><span class="dot6" style="background:${e.dot}"></span></div>`).join('') || emptyHTML('activity', 'No activity in ' + mLabel));

  // pending approvals widget
  const pend = pendingSubs();
  const inWin = k => pend.filter(s => subMs(s) > now - RANGE_MS[k]);
  setHTML($('#ov-appr-tabs'), [['24h', '24 Hours'], ['7d', '7 Days'], ['30d', '30 Days']].map(([k, l]) => `<button class="tab ${S.ov.apprTab === k ? 'on' : ''}" data-act="ovApprTab" data-k="${k}">${l} <span class="n">(${inWin(k).length})</span></button>`).join(''));
  const top = inWin(S.ov.apprTab).slice(0, 3);
  setHTML($('#ov-appr-list'), top.map(s => {
    const t = taskOfSub(s);
    return `<div class="lrow"><img class="appico" src="${esc(t ? taskIcon(t) : PLAY_ICON)}" alt="" loading="lazy"><div class="grow"><div class="b fs13 trunc">${esc(s.taskTitle || 'Task')}</div><div class="fs12 muted trunc">“${esc(s.proofText || 'No comment')}” • ${esc(s.userName || 'User')}</div></div><div class="fs12 muted shrink0 hide-m">${ago(subMs(s))}</div><button class="btn ok xs" data-act="approve" data-id="${esc(s.id)}">Approve</button><button class="btn bad xs" data-act="reject" data-id="${esc(s.id)}">Reject</button></div>`;
  }).join('') || emptyHTML('approve', 'No pending submissions in this window'));

  // withdrawals widget
  const cnt = k => S.wds.filter(w => k === 'pending' ? ['pending', 'processing'].includes(wdSt(w)) : wdSt(w) === k).length;
  setHTML($('#ov-wd-tabs'), [['pending', 'Pending'], ['approved', 'Paid'], ['rejected', 'Rejected']].map(([k, l]) => `<button class="tab ${S.ov.wdTab === k ? 'on' : ''}" data-act="ovWdTab" data-k="${k}">${l} <span class="n">(${cnt(k)})</span></button>`).join(''));
  const wl = S.wds.filter(w => S.ov.wdTab === 'pending' ? ['pending', 'processing'].includes(wdSt(w)) : wdSt(w) === S.ov.wdTab).slice(0, 3);
  setHTML($('#ov-wd-list'), wl.map(w => `<div class="lrow"><span class="mono fs12 b shrink0">#${esc(w.id.slice(0, 6))}</span><span class="tag">${wdMethod(w) === 'upi' ? 'UPI' : wdMethod(w) === 'bank' ? 'Bank' : '—'}</span><div class="grow b">${inr(w.amount)}</div><div class="fs12 muted shrink0 hide-m">${ago(wdMs(w))}</div>${wdPill(wdSt(w))}</div>`).join('') || emptyHTML('wallet', 'No requests here'));
  $('#ov-wd-total').innerHTML = `Total Pending: <span class="brand-t">${inr(pendAmt)}</span> <span class="muted fs12">• Wallet liability ${inr(S.users.reduce((n, u) => n + num(u.walletBalance), 0))}</span>`;

  // maintenance card
  const md = maintDoc();
};
ACT.reloadPage = () => location.reload();
window.addEventListener('online', () => schedule()); window.addEventListener('offline', () => schedule());
ACT.ovApprTab = el => { S.ov.apprTab = el.dataset.k; renderAll(); };
ACT.ovWdTab = el => { S.ov.wdTab = el.dataset.k; renderAll(); };
ACT.ovPrev = () => { const [y, m] = S.ov.month.split('-').map(Number); S.ov.month = monthKey(new Date(y, m - 2, 1).getTime()); renderAll(); };
ACT.ovNext = () => { const [y, m] = S.ov.month.split('-').map(Number), nk = monthKey(new Date(y, m, 1).getTime()); if (nk <= monthKey(Date.now())) { S.ov.month = nk; renderAll(); } };
ACT.ovNow = () => { S.ov.month = monthKey(Date.now()); renderAll(); };
document.addEventListener('keydown', e => { if (e.key === 'Enter' && e.target.id === 'ov-search' && e.target.value.trim()) go('approvals', { range: 'all', status: 'all', q: e.target.value.trim() }); });
ACT.ovZip = async () => {
  const now = Date.now();
  const list = pendingSubs().filter(s => subMs(s) > now - RANGE_MS[S.ov.apprTab]);
  await downloadZip(list, 'Pending_' + S.ov.apprTab);
};
