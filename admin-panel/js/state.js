'use strict';
/* App state, toast/dialogs, audit log, data accessors, charts */

/* ============================================================
   STATE
   ============================================================ */
const S = {
  users: [], tasks: [], subs: [], wds: [], bcasts: [], bulkReq: [], bkTab: 'pending', logs: [], meta: {},
  live: {}, errs: {}, admin: '', view: 'overview',
  ov: { apprTab: '24h', wdTab: 'pending', month: monthKey(Date.now()), q: '' },
  appr: { bulk: true, range: 'all', status: 'pending', from: '', to: '', q: '', groups: 6, open: {}, items: {}, folder: null, fmeta: {}, fs: 'pending', fq: '', fitems: 24 },
  sel: new Set(),
  tasksUI: { q: '', status: 'all', page: 1, day: 'all', date: '' },
  cm: { tab: 'bank', q: '', task: 'all', status: 'all', page: 1, sel: new Set(), blTask: '', folder: null },
  us: { q: '', status: 'all', sort: 'recent', from: '', to: '', page: 1 },
  wd: { tab: 'pending', q: '', method: 'all', month: monthKey(Date.now()), from: '', to: '', page: 1, reveal: new Set() },
  rp: { month: monthKey(Date.now()) },
  lg: { q: '', action: 'all', page: 1 },
  tm: { showCompleted: true },
  drawer: { uid: null, tab: 'subs', ledger: null }
};
let busy = false;
const serverNow = () => Date.now() + (S.clockOff || 0);
async function syncClock() {
  try { const r = doc(db, 'adminMeta', 'clock'); await setDoc(r, { t: serverTimestamp(), by: S.admin }); const sn = await getDoc(r), ms = tsMs(sn.data().t); S.clockOff = ms ? ms - Date.now() : null; }
  catch (_) { S.clockOff = null; }
}
async function archiveTask(t) {
  try { await setDoc(doc(db, 'taskArchive', t.id), { id: t.id, title: taskName(t), appId: t.appId || t.packageName || '', rewardInr: taskReward(t), submittedUserIds: t.submittedUserIds || [], blockedUserIds: t.blockedUserIds || [], createdAtMs: tsMs(t.createdAt) || 0, archivedAt: serverTimestamp() }, { merge: true }); } catch (e) { console.warn('archive', e.code || e.message); }
}
let bulkRun = false;            // while a bulk job runs, skip intermediate re-renders (one render at the end)
const locks = new Set();        // per-submission lock so several can be approved back-to-back without waiting
async function pool(items, fn, n = 8) { let i = 0; const run = async () => { while (i < items.length) { const it = items[i++]; await fn(it); } }; await Promise.all(Array.from({ length: Math.min(n, items.length) }, run)); }

/* ============================================================
   TOAST + DIALOGS
   ============================================================ */
function toast(msg, kind = '') {
  const box = $('#toasts'); if (!box) return;
  const el = document.createElement('div');
  el.className = 'toast ' + kind; el.textContent = msg; box.appendChild(el);
  setTimeout(() => el.remove(), kind === 'bad' ? 6000 : 3800);
}

/** Generic dialog. fields: [{id,label,type,value,options,required,placeholder,hint}]. Resolves with values object / true / null. */
function ask({ title, body = '', ok = 'Confirm', cancel = 'Cancel', danger = false, fields = [] }) {
  return new Promise(resolve => {
    const ov = $('#dlg-overlay'), box = $('#dlg');
    const f = fields.map(x => {
      let input;
      if (x.type === 'textarea') input = `<textarea class="in" id="dlg-${x.id}" rows="3" placeholder="${esc(x.placeholder || '')}">${esc(x.value || '')}</textarea>`;
      else if (x.type === 'select') input = `<select class="in" id="dlg-${x.id}">${x.options.map(o => `<option value="${esc(o.v ?? o)}"${(o.v ?? o) === x.value ? ' selected' : ''}>${esc(o.t ?? o)}</option>`).join('')}</select>`;
      else input = `<input class="in" id="dlg-${x.id}" type="${x.type || 'text'}" value="${esc(x.value ?? '')}" placeholder="${esc(x.placeholder || '')}" ${x.min != null ? `min="${x.min}"` : ''}>`;
      return `<div class="field"><label class="lbl" for="dlg-${x.id}">${esc(x.label)}${x.required ? ' *' : ''}</label>${input}${x.hint ? `<div class="hint">${esc(x.hint)}</div>` : ''}</div>`;
    }).join('');
    box.innerHTML = `<h3>${esc(title)}</h3><div class="body">${body}</div>${f}<div class="err" id="dlg-err"></div>
      <div class="row" style="justify-content:flex-end;margin-top:14px"><button class="btn ghost" id="dlg-cancel">${esc(cancel)}</button><button class="btn ${danger ? 'bad' : 'primary'}" id="dlg-ok">${esc(ok)}</button></div>`;
    ov.classList.add('show');
    const done = v => { ov.classList.remove('show'); ov._cancel = null; resolve(v); };
    ov._cancel = () => done(null);
    $('#dlg-cancel').onclick = () => done(null);
    $('#dlg-ok').onclick = () => {
      const vals = {};
      for (const x of fields) {
        const v = $('#dlg-' + x.id).value.trim();
        if (x.required && !v) { $('#dlg-err').textContent = `${x.label} is required.`; $('#dlg-' + x.id).focus(); return; }
        vals[x.id] = v;
      }
      done(fields.length ? vals : true);
    };
    const first = fields.length ? $('#dlg-' + fields[0].id) : $('#dlg-ok');
    setTimeout(() => first && first.focus(), 30);
    box.onkeydown = e => { if (e.key === 'Enter' && e.target.tagName !== 'TEXTAREA') { e.preventDefault(); $('#dlg-ok').click(); } };
  });
}
const confirmBox = (title, body, opts = {}) => ask({ title, body: `<p>${body}</p>`, ...opts });

/* ============================================================
   AUDIT LOG
   ============================================================ */
async function logAction(action, target, details = {}) {
  try {
    await setDoc(doc(collection(db, 'adminLogs')), {
      action, target: String(target || ''), details, adminEmail: S.admin, createdAt: serverTimestamp(), createdMs: Date.now()
    });
  } catch (e) { try { const a = JSON.parse(localStorage.getItem('tg_logs') || '[]'); a.unshift({ id: 'local_' + Date.now(), action, target: String(target || ''), details, adminEmail: S.admin, createdMs: Date.now() }); localStorage.setItem('tg_logs', JSON.stringify(a.slice(0, 200))); } catch (_) { } }
}

/* ============================================================
   DATA ACCESSORS
   ============================================================ */
const shotOf = s => (s && (s.screenshotUrl || s.screenshotBase64)) || '';
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
const subDay = s => ymd(subMs(s) || Date.now());           // local calendar day the screenshot was uploaded
const subKey = s => `${(s.taskId || 'general').trim()}::${(s.taskTitle || 'Untitled Task').trim()}::${subDay(s)}`;   // one folder = one app + one day
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
const LOADER = (label = 'Loading') => `<div class="loader"><div class="ld-ring"><svg viewBox="0 0 48 48" aria-hidden="true"><circle class="ld-track" cx="24" cy="24" r="20"/><circle class="ld-arc" cx="24" cy="24" r="20"/></svg><i class="ld-core"></i></div><div class="ld-t">${label}</div></div>`;
const skelCards = () => LOADER(), skelFolders = () => LOADER(), skelHTML = () => LOADER();
/* Loading gate: skeleton ONLY while the first snapshot is genuinely pending (never forever).
   Error / offline / 8s timeout -> clear message + Retry instead of an endless skeleton. */
function loadGate(key, hasData, skel, box) {
  if (S.live[key] || hasData) return false;
  const waited = Date.now() - (S.bootAt || Date.now());
  if (S.errs[key] || !navigator.onLine || waited > 8000) {
    setHTML(box, `<div class="card"><div class="note bad">${ic('alert', 18)}<div class="grow"><b>${navigator.onLine ? 'Could not load data' : 'No internet connection'}</b><div class="fs12">${esc(String(S.errs[key] || (navigator.onLine ? 'Taking too long — check your connection or Firestore rules.' : 'It will load by itself when you are back online.')))}</div></div><button class="btn ghost xs" data-act="reloadPage">Retry</button></div></div>`);
    return true;
  }
  setHTML(box, skel); return true;
}

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
