'use strict';
/* ============================================================
   DATA ACCESSORS
   ============================================================ */
const subMs = s => tsMs(s.createdAt) || num(s.submittedAtMillis) || num(s.timestamp) || num(s.createdAtMillis) || 0;
const subSt = s => (s.status || 'pending').toLowerCase();
const wdSt = w => (w.status || 'pending').toLowerCase();
const wdMs = w => tsMs(w.createdAt) || num(w.timestamp) || num(w.requestedAt) || 0;
const wdPaidMs = w => tsMs(w.processedAt) || tsMs(w.paidAt) || wdMs(w);
const wdMethod = w => w.upiId ? 'upi' : (w.accountNumber || w.bankName) ? 'bank' : (String(w.method || w.paymentMethod || '').toLowerCase().includes('upi') ? 'upi' : 'other');
const userMs = u => tsMs(u.createdAt) || tsMs(u.joinedAt) || 0;
const userSeen = u => Math.max(tsMs(u.lastActiveAt), tsMs(u.lastSeen), tsMs(u.lastLoginAt), tsMs(u.lastActive), tsMs(u.presenceAt));
document.addEventListener('error', e => { const i = e.target; if (i && i.tagName === 'IMG' && i.classList.contains('appico') && i.src !== PLAY_ICON) { i.onerror = null; i.src = PLAY_ICON; } }, true);
const taskIcon = t => t.iconUrl || t.appIcon || t.taskPfpBase64 || PLAY_ICON;
const taskName = t => t.title || t.appName || 'Untitled App';
const taskReward = t => num(t.rewardInr ?? t.rewardAmount ?? t.reward);
const taskStatus = t => { const s = String(t.status || '').toLowerCase(); if (s === 'archived') return 'archived'; if (s === 'draft') return 'draft'; return (t.active === false || t.isActive === false || s === 'paused') ? 'paused' : 'active'; };
const taskOfSub = s => S.tasks.find(t => t.id === s.taskId) || S.tasks.find(t => taskName(t).trim().toLowerCase() === String(s.taskTitle || '').trim().toLowerCase());
const subKey = s => `${(s.taskId || 'general').trim()}::${(s.taskTitle || 'Untitled Task').trim()}`;
const statusPill = st => st === 'approved' ? '<span class="pill ok">Approved</span>' : st === 'rejected' ? '<span class="pill bad">Rejected</span>' : '<span class="pill warn">Pending</span>';
const wdPill = st => st === 'approved' ? '<span class="pill ok">Paid</span>' : st === 'rejected' ? '<span class="pill bad">Rejected</span>' : st === 'processing' ? '<span class="pill info">Processing</span>' : '<span class="pill warn">Pending</span>';
const isMaintActive = () => !!(S.bcasts.find(b => b.id === 'system_maintenance_banner') || {}).active;
const maintDoc = () => S.bcasts.find(b => b.id === 'system_maintenance_banner') || {};
const pendingSubs = () => S.subs.filter(s => subSt(s) === 'pending');
const pendingWds = () => S.wds.filter(w => ['pending', 'processing'].includes(wdSt(w)));

function paginate(list, key, size, state) {
  const pages = Math.max(1, Math.ceil(list.length / size));
  if (state.page > pages) state.page = pages;
  const start = (state.page - 1) * size;
  return { items: list.slice(start, start + size), pages, total: list.length, key, page: state.page, start, size };
}
function pagerHTML(p) {
  if (p.total <= p.size) return p.total ? `<div class="pager"><span>${p.total} record${p.total > 1 ? 's' : ''}</span></div>` : '';
  return `<div class="pager"><span>Showing ${p.start + 1}–${Math.min(p.start + p.size, p.total)} of ${p.total}</span>
    <div class="row"><button class="btn ghost sm" data-act="page" data-k="${p.key}" data-d="-1" ${p.page <= 1 ? 'disabled' : ''}>${ic('chev-left', 14)}Prev</button><span class="b">${p.page} / ${p.pages}</span><button class="btn ghost sm" data-act="page" data-k="${p.key}" data-d="1" ${p.page >= p.pages ? 'disabled' : ''}>Next${ic('chev-right', 14)}</button></div></div>`;
}
const emptyHTML = (icon, msg) => `<div class="empty">${ic(icon, 30)}${msg}</div>`;
const skelCards = n => Array.from({ length: n }, () => '<div class="skel" style="height:230px;border-radius:20px;margin:0"></div>').join('');
const skelKpis = n => Array.from({ length: n }, () => '<div class="skel" style="height:94px;border-radius:22px;margin:0"></div>').join('');
const skelHTML = n => Array.from({ length: n }, () => '<div class="skel" style="margin:10px 0"></div>').join('');

/* ---------- charts ---------- */
function spark(vals, color, id) {
  const W = 120, H = 46, max = Math.max(1, ...vals);
  const pts = vals.map((v, i) => [vals.length > 1 ? i * (W / (vals.length - 1)) : 0, H - 4 - (H - 10) * (v / max)]);
  const line = pts.map((p, i) => (i ? 'L' : 'M') + p[0].toFixed(1) + ' ' + p[1].toFixed(1)).join(' ');
  return `<svg class="sp" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" aria-hidden="true"><defs><linearGradient id="sg-${id}" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${color}" stop-opacity=".28"/><stop offset="1" stop-color="${color}" stop-opacity="0"/></linearGradient></defs><path d="${line} L${W} ${H} L0 ${H}z" fill="url(#sg-${id})"/><path d="${line}" fill="none" stroke="${color}" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/></svg>`;
}
function lineChart(vals, labels) {
  const W = 640, H = 236, pl = 46, pr = 28, pt = 14, pb = 32;
  let max = Math.max(...vals, 1);
  const step = Math.pow(10, Math.floor(Math.log10(max)));
  max = Math.ceil(max / step) * step;
  const x = i => pl + (vals.length > 1 ? (W - pl - pr) * i / (vals.length - 1) : (W - pl - pr) / 2);
  const y = v => pt + (H - pt - pb) * (1 - v / max);
  let grid = '';
  for (let i = 0; i <= 4; i++) { const v = max * i / 4, yy = y(v); grid += `<line x1="${pl}" x2="${W - pr}" y1="${yy}" y2="${yy}" stroke="rgba(128,150,180,.22)"/><text x="${pl - 8}" y="${yy + 4}" text-anchor="end">${v ? '₹' + compact(v) : '0'}</text>`; }
  const line = vals.map((v, i) => (i ? 'L' : 'M') + x(i).toFixed(1) + ' ' + y(v).toFixed(1)).join(' ');
  const every = Math.ceil(labels.length / 8);
  const xl = labels.map((l, i) => i % every === 0 || i === labels.length - 1 ? `<text x="${x(i)}" y="${H - 8}" text-anchor="middle">${esc(l)}</text>` : '').join('');
  const dots = vals.length <= 12 ? vals.map((v, i) => `<circle cx="${x(i)}" cy="${y(v)}" r="4" fill="#fff" stroke="#0EA5E9" stroke-width="2.4"/>`).join('') : '';
  return `<svg class="chart" viewBox="0 0 ${W} ${H}" width="100%" role="img" aria-label="Rewards chart"><defs><linearGradient id="cg" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#0EA5E9" stop-opacity=".30"/><stop offset="1" stop-color="#0EA5E9" stop-opacity=".02"/></linearGradient></defs>${grid}<path d="${line} L${x(vals.length - 1)} ${y(0)} L${x(0)} ${y(0)}z" fill="url(#cg)"/><path d="${line}" fill="none" stroke="#0EA5E9" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>${dots}${xl}</svg>`;
}
