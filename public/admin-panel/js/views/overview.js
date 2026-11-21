'use strict';
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
  if (!S.live.users && !S.live.submissions && !S.users.length && !S.subs.length) { setHTML($('#ov-hero'), '<div class="skel" style="height:150px;border-radius:26px;margin:0"></div>'); setHTML($('#kpi-a'), skelKpis(3)); setHTML($('#kpi-b'), skelKpis(4)); setHTML($('#chart'), skelHTML(3)); return; }
  { const h = new Date().getHours(), g = h < 12 ? 'Good morning' : h < 17 ? 'Good afternoon' : 'Good evening', nm = String(S.admin || 'Admin').split('@')[0], t0 = new Date().setHours(0, 0, 0, 0);
    const hs = (ico, l, v) => `<div class="hs"><div class="hi">${ic(ico, 18)}</div><div><b>${v}</b><span>${l}</span></div></div>`;
    setHTML($('#ov-hero'), `<div class="hero-l"><div class="hero-hi">${g}, ${esc(nm)} 👋</div><div class="hero-d">${new Date().toLocaleDateString('en-IN', { weekday: 'long', day: 'numeric', month: 'long' })} • ${pendingSubs().length} pending approvals</div></div><div class="row gap6"><button class="hbtn solid" data-act="go" data-v="approvals" data-range="all" data-status="pending">${ic('approve', 14)}Approvals</button><button class="hbtn" data-act="openTask">${ic('plus', 14)}Add task</button></div>`); }
  const act30 = new Set(S.subs.filter(s => subMs(s) > now - 30 * DAY).map(s => s.userId).filter(Boolean));
  S.users.forEach(u => { if (userSeen(u) > now - 30 * DAY) act30.add(u.id); });
  const wk = S.users.filter(u => userMs(u) > now - 7 * DAY).length, wkT = S.tasks.filter(t => tsMs(t.createdAt) > now - 7 * DAY).length;
  const kpi = (cls, icon, label, val, sub, series, color, id) => `<div class="kpi ${cls}"><div class="ico">${ic(icon, 26)}</div><div class="grow"><div class="lbl">${label}</div><div class="val">${val}</div><div class="dl">${sub}</div></div></div>`;
  setHTML($('#kpi-a'),
    kpi('blue', 'users', 'Total Users', S.users.length.toLocaleString('en-IN'), `↑ +${wk} this week`, dayBins(S.users, userMs), '#3B82F6', 'u') +
    kpi('green', 'check', 'Active Users', act30.size.toLocaleString('en-IN'), 'last 30 days', dayBins(S.subs, subMs), '#22C55E', 'a') +
    kpi('purple', 'tasks', 'Total Tasks', S.tasks.length.toLocaleString('en-IN'), `Today: ${S.subs.filter(s => subMs(s) >= new Date().setHours(0, 0, 0, 0)).length} submissions`, dayBins(S.tasks, t => tsMs(t.createdAt)), '#8B5CF6', 't'));
  const paid = S.wds.filter(w => wdSt(w) === 'approved').reduce((n, w) => n + num(w.amount), 0);
  const pendAmt = pendingWds().reduce((n, w) => n + num(w.amount), 0);
  const ms = monthStats(monthKey(now));
  const sm = (cls, icon, label, val, sub, act) => `<div class="kpi s ${cls}"><div class="ico">${ic(icon, 24)}</div><div class="grow"><div class="lbl">${label}</div><div class="val">${val}</div>${sub ? `<div class="dl">${sub}</div>` : ''}</div><button class="go" data-act="go" data-v="${act[0]}" ${act[1] ? `data-range="all" data-status="${act[1]}"` : ''} aria-label="Open">${ic('arrow-right', 16)}</button></div>`;
  setHTML($('#kpi-b'),
    sm('orange', 'check', 'Pending Approvals', pendingSubs().length.toLocaleString('en-IN'), '', ['approvals', 'pending']) +
    sm('red', 'x', 'Rejected Tasks', S.subs.filter(s => subSt(s) === 'rejected').length.toLocaleString('en-IN'), '', ['approvals', 'rejected']) +
    sm('green', 'wallet', 'Total Withdrawals', inr(paid), `<span class="muted">Pending ${inr(pendAmt)}</span>`, ['withdrawals']) +
    sm('purple', 'trend', 'Net Profit (month)', ms.hasFin ? inr(ms.net) : '—', ms.hasFin ? '' : '<span class="muted">Add revenue in Monthly Records</span>', ['reports']));
  countUp();

  // chart
  const mode = S.ov.chart, d = new Date();
  const bins = [], labels = []; let prevTotal = 0;
  const val = (a, b) => S.subs.filter(s => subSt(s) === 'approved' && approvalMs(s) >= a && approvalMs(s) < b).reduce((n, s) => n + num(s.rewardAmount), 0);
  if (mode === 'month') {
    const [a, b] = monthRange(monthKey(now)); const n = Math.ceil((b - a) / (7 * DAY));
    for (let i = 0; i < n; i++) { bins.push(val(a + i * 7 * DAY, Math.min(b, a + (i + 1) * 7 * DAY))); labels.push('Week ' + (i + 1)); }
    const pk = new Date(d.getFullYear(), d.getMonth() - 1, 1).getTime(); prevTotal = val(pk, a);
  } else {
    const n = mode === '7d' ? 7 : 30, end = new Date(); end.setHours(24, 0, 0, 0);
    for (let i = n - 1; i >= 0; i--) { const e = end.getTime() - i * DAY, s0 = e - DAY; bins.push(val(s0, e)); labels.push(new Date(s0).toLocaleDateString('en-IN', mode === '7d' ? { weekday: 'short' } : { day: 'numeric', month: 'short' })); }
    prevTotal = val(end.getTime() - 2 * n * DAY, end.getTime() - n * DAY);
  }
  const total = bins.reduce((a, b) => a + b, 0);
  $('#chart-total').textContent = inr(total);
  const dl = $('#chart-delta');
  if (prevTotal > 0) { const p = ((total - prevTotal) / prevTotal) * 100; dl.textContent = (p >= 0 ? '↑ +' : '↓ ') + p.toFixed(1) + '% vs previous'; dl.style.color = p >= 0 ? 'var(--green)' : 'var(--red)'; }
  else dl.textContent = total ? 'No previous period data' : 'No approved rewards yet';
  setHTML($('#chart'), lineChart(bins, labels));

  // recent activity
  const ev = [];
  S.users.forEach(u => ev.push({ t: userMs(u), icon: 'user-plus', c: 'green', dot: '#EF4444', ti: 'New User Registered', su: 'ID: ' + (u.userId || u.id).slice(0, 12) }));
  S.subs.forEach(s => { const st = subSt(s); ev.push({ t: st === 'pending' ? subMs(s) : approvalMs(s), icon: st === 'approved' ? 'check' : st === 'rejected' ? 'x' : 'upload', c: st === 'approved' ? 'green' : st === 'rejected' ? 'red' : 'blue', dot: '#F59E0B', ti: st === 'approved' ? 'Task Approved' : st === 'rejected' ? 'Task Rejected' : 'New Submission', su: (s.taskTitle || 'Task') + ' • ' + (s.userName || 'User') }); });
  S.wds.forEach(w => ev.push({ t: wdMs(w), icon: 'download', c: 'blue', dot: '#22C55E', ti: 'Withdrawal Request', su: inr(w.amount) + ' via ' + (wdMethod(w) === 'upi' ? 'UPI' : wdMethod(w) === 'bank' ? 'Bank' : 'wallet') }));
  S.bcasts.filter(b => b.id !== 'system_maintenance_banner').forEach(b => ev.push({ t: tsMs(b.createdAt) || num(b.timestamp), icon: 'bell', c: 'purple', dot: '#8B5CF6', ti: 'Notification Sent', su: b.title || '' }));
  ev.sort((a, b) => b.t - a.t);
  setHTML($('#recent'), ev.slice(0, 6).map(e => `<div class="lrow"><div class="rico ${e.c}">${ic(e.icon, 18)}</div><div class="grow"><div class="b fs13">${esc(e.ti)}</div><div class="fs12 muted trunc">${esc(e.su)}</div></div><div class="fs12 muted shrink0">${ago(e.t)}</div><span class="dot6" style="background:${e.dot}"></span></div>`).join('') || emptyHTML('activity', 'No activity yet'));

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

  // monthly mini table
  const months = [0, 1, 2].map(i => { const dd = new Date(d.getFullYear(), d.getMonth() - i, 1); return monthStats(monthKey(dd.getTime())); });
  setHTML($('#ov-month'), `<div class="table" style="border:0"><div class="thead" style="grid-template-columns:1.1fr 1fr 1fr 1fr;padding:8px 0;background:none;border-radius:0"><span>Month</span><span>Rewards</span><span>Paid out</span><span>Profit</span></div>${months.map(m => `<div class="trow" style="grid-template-columns:1.1fr 1fr 1fr 1fr;padding:9px 0;font-size:12.5px"><span class="b">${new Date(...m.key.split('-').map((x, i) => i ? x - 1 : +x), 1).toLocaleDateString('en-IN', { month: 'short', year: 'numeric' })}</span><span>${inr(m.rewards)}</span><span>${inr(m.paid)}</span><span class="b ${m.net < 0 ? 'bad-t' : ''}">${m.hasFin ? inr(m.net) : '—'}</span></div>`).join('')}</div>`);

  // maintenance card
  const md = maintDoc();
  setHTML($('#ov-maint'), `<p class="fs13 muted" style="line-height:1.5">${esc(md.message || MAINT_MSG)}</p><div class="row between wrap mt12"><span>${isMaintActive() ? '<span class="pill bad">LOCKDOWN ACTIVE</span>' : '<span class="pill ok">LIVE (NORMAL)</span>'}</span><button class="btn soft sm" data-act="maintEdit">${ic('edit', 14)}Update Message</button></div>`);
};
ACT.ovApprTab = el => { S.ov.apprTab = el.dataset.k; renderAll(); };
ACT.ovWdTab = el => { S.ov.wdTab = el.dataset.k; renderAll(); };
INP.ovChart = el => { S.ov.chart = el.value; renderAll(); };
document.addEventListener('keydown', e => { if (e.key === 'Enter' && e.target.id === 'ov-search' && e.target.value.trim()) go('approvals', { range: 'all', status: 'all', q: e.target.value.trim() }); });
ACT.ovZip = async () => {
  const now = Date.now();
  const list = pendingSubs().filter(s => subMs(s) > now - RANGE_MS[S.ov.apprTab]);
  await downloadZip(list, 'Pending_' + S.ov.apprTab);
};
