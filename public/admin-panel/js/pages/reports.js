'use strict';
/* Monthly reports + report images */

/* ============================================================
   MONTHLY RECORDS / REPORTS
   ============================================================ */
const payName = w => { const u = S.users.find(x => x.id === canon(w.userId)); return w.userName || (u && (u.fullName || u.email)) || w.userEmail || w.userId || 'User'; };
function spendByTask(ap) {
  const per = new Map();
  ap.forEach(s => { const t = taskOfSub(s), name = t ? taskName(t) : (s.taskTitle || 'Unknown app'), r = num(s.rewardAmount), o = per.get(name) || { name, n: 0, amt: 0, rates: new Set() }; o.n++; o.amt += r; o.rates.add(r); per.set(name, o); });
  return [...per.values()].sort((a, b) => b.amt - a.amt);
}
function payoutRows(list) {
  const per = new Map();
  list.forEach(w => { const name = payName(w), k = canon(w.userId) || name, o = per.get(k) || { name, n: 0, amt: 0 }; o.n++; o.amt += num(w.paidAmount) || num(w.amount); per.set(k, o); });
  return [...per.values()].sort((a, b) => b.amt - a.amt);
}
const sIc = (n, size = 16, color = '#fff') => ic(n, size).replace('currentColor', color);   // explicit colour so the exported image keeps it
const stShell = (id, tone, status, m, big, meta, listTitle, list, total) => `<div class="stmt" id="${id}"><i class="gc g1"></i><i class="gc g2"></i><i class="gc g3"></i><div class="rc">
    <div class="rc-top ${tone}"><div class="rc-brand">${sIc('wallet', 16, '#fff')}<span>Task Gullak</span></div><div class="rc-per">${esc(monthLabel(m.key))}</div></div>
    <div class="rc-body">
      <div class="rc-status">${status}</div>
      <div class="st-big" data-v="${big[1]}">${inr(big[1])}</div><div class="rc-cap">${big[0]}</div>
      <div class="rc-meta" style="grid-template-columns:repeat(${meta.length},1fr)">${meta.map(([k, v]) => `<div><span>${k}</span><b>${v}</b></div>`).join('')}</div>
      <div class="rc-cut"><i class="n l"></i><i class="n r"></i></div>
      <div class="rc-sec">${listTitle}</div>
      <div class="rc-list">${list}</div>
      <div class="rc-box"><span>${total[0]}</span><b>${total[1]}</b></div>
      <div class="rc-foot">${sIc('shield', 13, '#9ca3af')}<span>Secured by Task Gullak · ${esc(fmtD(Date.now()))}</span></div>
    </div></div></div>`;
const stAv = n => `<div class="st-av" style="background:${avc(n)}">${esc(String(n)[0].toUpperCase())}</div>`;
function expenseStmt(m) {
  const rows = spendByTask(m.ap);
  const list = rows.map((o, i) => { const one = o.rates.size === 1 ? [...o.rates][0] : null; return `<div class="st-row" style="--i:${i}">${stAv(o.name)}<div class="st-main"><div class="st-name">${esc(o.name)}</div><div class="st-det">${one !== null ? `${o.n} approved × ${inr(one)}` : `${o.n} approved`}</div></div><div class="st-amt">${inr(o.amt)}</div></div>`; }).join('') || '<div class="st-empty">No approved reviews this month</div>';
  return stShell('stmt-exp', 'tb', 'Expense summary', m, ['Spent on approved reviews', m.spent], [['Submitted', m.subs.length], ['Approved', m.ap.length], ['Rejected', m.rj.length], ['Pending', m.pn.length]], 'Spend by app', list, ['Total spent', inr(m.spent)]);
}
function payStmt(m) {
  const rows = payoutRows(m.payouts);
  const list = rows.map((o, i) => `<div class="st-row" style="--i:${i}">${stAv(o.name)}<div class="st-main"><div class="st-name">${esc(o.name)}</div><div class="st-det">${o.n} payout${o.n === 1 ? '' : 's'}</div></div><div class="st-amt">${inr(o.amt)}</div></div>`).join('') || '<div class="st-empty">No payments this month</div>';
  return stShell('stmt-pay', 'tp', 'Payment successful', m, ['Paid to users', m.paid], [['Receipt no.', 'TG-' + m.key.replace('-', '')], ['Payouts', m.payouts.length], ['Users', rows.length]], 'Paid to', list, ['Total paid', inr(m.paid)]);
}
function reportPage(m) {
  const spend = spendByTask(m.ap), pay = payoutRows(m.payouts), top = (l, n) => [l.slice(0, n), Math.max(0, l.length - n)];
  const row = (name, det, amt) => `<div class="pg-r"><div class="pg-n"><b>${esc(name)}</b><span>${det}</span></div><em>${amt}</em></div>`;
  const [sp, spMore] = top(spend, 8), [py, pyMore] = top(pay, 8);
  const more = (n, l) => n ? `<div class="pg-more">+ ${n} more ${l}</div>` : '';
  return `<div class="stmt pg" id="stmt-page"><i class="gc g1"></i><i class="gc g2"></i><i class="gc g3"></i><div class="pg-card">
    <div class="pg-head"><div><div class="pg-t">Monthly report</div><div class="pg-m">${esc(monthLabel(m.key))}</div></div><div class="pg-brand">${sIc('wallet', 15, '#0369a1')}Task Gullak</div></div>
    <div class="pg-hero">
      <div class="pg-h a"><span>Spent on reviews</span><b>${inr(m.spent)}</b><em>${m.ap.length} approved</em></div>
      <div class="pg-h b"><span>Paid to users</span><b>${inr(m.paid)}</b><em>${m.payouts.length} payout${m.payouts.length === 1 ? '' : 's'} · ${pay.length} user${pay.length === 1 ? '' : 's'}</em></div>
    </div>
    <div class="pg-stats"><div><b>${m.subs.length}</b><span>Submitted</span></div><div><b>${m.ap.length}</b><span>Approved</span></div><div><b>${m.rj.length}</b><span>Rejected</span></div><div><b>${m.pn.length}</b><span>Pending</span></div></div>
    <div class="pg-sec">Spend by app</div>
    ${sp.map(o => { const one = o.rates.size === 1 ? [...o.rates][0] : null; return row(o.name, one !== null ? `${o.n} × ${inr(one)}` : `${o.n} approved`, inr(o.amt)); }).join('') || '<div class="pg-more">No approved reviews</div>'}${more(spMore, 'apps')}
    <div class="pg-sec">Paid to users</div>
    ${py.map(o => row(o.name, `${o.n} payout${o.n === 1 ? '' : 's'}`, inr(o.amt))).join('') || '<div class="pg-more">No payments</div>'}${more(pyMore, 'users')}
    <div class="pg-foot">Generated ${esc(fmtD(Date.now()))} · Task Gullak</div>
  </div></div>`;
}
ACT.rpPage = async () => {
  const m = monthStats(S.rp.month); let w = document.getElementById('rp-off');
  if (!w) { w = document.createElement('div'); w.id = 'rp-off'; w.style.cssText = 'position:fixed;left:-10000px;top:0;width:560px;pointer-events:none'; document.body.appendChild(w); }
  w.innerHTML = reportPage(m);
  try { await ACT.stmtImg({ dataset: { t: 'stmt-page', n: 'report' } }); } finally { w.innerHTML = ''; }
};
RENDER.reports = () => {
  const key = S.rp.month, m = monthStats(key);
  const mi = $('#rp-month'); if (document.activeElement !== mi) mi.value = key;
  setHTML($('#rp-exp-card'), expenseStmt(m));
  setHTML($('#rp-pay-card'), payStmt(m));
  $$('.st-big[data-v]:not([data-done])').forEach(el => {       // amount counts up when a statement appears / month changes
    el.dataset.done = '1'; const v = num(el.dataset.v), t0 = performance.now();
    if (matchMedia('(prefers-reduced-motion:reduce)').matches || !v) return;
    const tick = now => { const k = Math.min(1, (now - t0) / 800), e = 1 - Math.pow(1 - k, 3); el.textContent = inr(Math.round(v * e)); if (k < 1) requestAnimationFrame(tick); else el.textContent = inr(v); };
    requestAnimationFrame(tick);
  });
  const rows = [5, 4, 3, 2, 1, 0].map(i => monthStats(shiftMonth(monthKey(Date.now()), -i))).reverse();
  setHTML($('#rp-months'), `<div class="table"><div class="thead cols-rp"><span>Month</span><span>Approved</span><span>Spent</span><span>Paid out</span></div>${rows.map(r => `<button class="trow cols-rp fs12" style="width:100%;border:0;text-align:left;cursor:pointer;font:inherit;color:inherit;${r.key === key ? 'background:var(--hover)' : ''}" data-act="rpPick" data-k="${r.key}"><span class="b">${esc(monthLabel(r.key))}</span><span data-l="Approved">${r.ap.length}</span><span class="b" data-l="Spent">${inr(r.spent)}</span><span class="b ok-t" data-l="Paid out">${inr(r.paid)}</span></button>`).join('')}</div>`);
};
INP.rpMonth = el => { if (el.value) { S.rp.month = el.value; renderAll(); } };
ACT.rpPrev = () => { S.rp.month = shiftMonth(S.rp.month, -1); renderAll(); };
ACT.rpNext = () => { S.rp.month = shiftMonth(S.rp.month, 1); renderAll(); };
ACT.rpPick = el => { S.rp.month = el.dataset.k; renderAll(); window.scrollTo({ top: 0 }); };
ACT.rpCsv = () => {
  const m = monthStats(S.rp.month);
  const rows = [[`Monthly record — ${monthLabel(m.key)}`], [], ['Submitted', m.subs.length], ['Approved', m.ap.length], ['Rejected', m.rj.length], ['Pending', m.pn.length], ['Total spent on approved reviews', m.spent], ['Total paid to users', m.paid], [],
  ['App', 'Approved', 'Amount spent']];
  spendByTask(m.ap).forEach(o => rows.push([o.name, o.n, o.amt]));
  rows.push([], ['User', 'Payouts', 'Amount paid']);
  payoutRows(m.payouts).forEach(o => rows.push([o.name, o.n, o.amt]));
  saveText(`monthly_${m.key}.csv`, toCsv(rows), 'text/csv'); logAction('export_report_csv', m.key);
};
/* ---- statement -> image (screenshot to share in your group) ---- */
function loadH2C() {
  if (window.html2canvas) return Promise.resolve(window.html2canvas);
  return new Promise((res, rej) => {
    const s = document.createElement('script'); s.src = 'https://cdnjs.cloudflare.com/ajax/libs/html2canvas/1.4.1/html2canvas.min.js';
    s.onload = () => window.html2canvas ? res(window.html2canvas) : rej(new Error('Image library missing')); s.onerror = () => rej(new Error('Image library failed to load (check internet)')); document.head.appendChild(s);
  });
}
ACT.stmtImg = async el => {
  const node = document.getElementById(el.dataset.t); if (!node) return;
  const name = `${el.dataset.n}_${S.rp.month}.png`;
  toast('Preparing image…');
  try {
    const h2c = await loadH2C();
    const canvas = await h2c(node, { scale: 3, backgroundColor: null, useCORS: true, logging: false,
      onclone: d => { d.querySelectorAll('.stmt, .stmt *').forEach(n => { n.style.animation = 'none'; n.style.opacity = '1'; n.style.transform = 'none'; });
        d.querySelectorAll('.stmt').forEach(n => { n.style.boxShadow = 'none'; n.style.margin = '0'; }); const off = d.getElementById('rp-off'); if (off) { off.style.position = 'static'; off.style.left = '0'; }
        d.querySelectorAll('.st-big[data-v]').forEach(n => { n.textContent = inr(num(n.dataset.v)); }); } });
    const blob = await new Promise(r => canvas.toBlob(r, 'image/png'));
    let o = document.getElementById('img-overlay');
    if (!o) { o = document.createElement('div'); o.id = 'img-overlay'; o.className = 'overlay'; document.body.appendChild(o); o.addEventListener('mousedown', e => { if (e.target === o) o.classList.remove('show'); }); }
    o.innerHTML = `<div class="sheet" style="max-width:480px"><div class="sheet-h"><h3 class="fs16 xb">Summary image</h3><button class="iconbtn" data-act="imgClose" aria-label="Close">${ic('x')}</button></div><div style="padding:14px"><img class="stimg" src="${canvas.toDataURL('image/png')}" style="width:100%;border-radius:26px;display:block" alt="Summary"><div class="hint" style="text-align:center">If download doesn’t start, long-press the image to save it.</div></div><div class="sheet-f"><button class="btn ghost" data-act="imgShare">${ic('send', 15)}Share</button><button class="btn primary" data-act="imgSave">${ic('download', 15)}Download</button></div></div>`;
    o._blob = blob; o._name = name; o.classList.add('show');
  } catch (e) { toast(e.message || 'Could not create image', 'bad'); }
};
ACT.imgClose = () => document.getElementById('img-overlay')?.classList.remove('show');
ACT.imgSave = () => { const o = document.getElementById('img-overlay'); if (!o?._blob) return; const a = document.createElement('a'); a.href = URL.createObjectURL(o._blob); a.download = o._name; document.body.appendChild(a); a.click(); a.remove(); setTimeout(() => URL.revokeObjectURL(a.href), 3000); };
ACT.imgShare = async () => {
  const o = document.getElementById('img-overlay'); if (!o?._blob) return;
  const f = new File([o._blob], o._name, { type: 'image/png' });
  if (navigator.canShare && navigator.canShare({ files: [f] })) { try { await navigator.share({ files: [f], title: o._name }); } catch (_) { } }
  else toast('Sharing is not supported here — use Download or long-press the image', 'bad');
};
ACT.cmExpand = el => el.classList.toggle('open');
ACT.proofExpand = el => el.classList.toggle('open');
ACT.rpPrint = () => window.print();ACT.rpPrint = () => window.print();
