/* === js/core/firebase.js === */
'use strict';
/* ============================================================
   FIREBASE BRIDGE — SDK is imported in index.html and exposed as window.FB
   (to change the SDK version, edit the import URLs in index.html)
   ============================================================ */
const {
  initializeApp,
  initializeAuth,
  browserLocalPersistence,
  inMemoryPersistence,
  browserPopupRedirectResolver,
  GoogleAuthProvider,
  signInWithPopup,
  signOut,
  getFirestore,
  collection,
  doc,
  onSnapshot,
  setDoc,
  updateDoc,
  deleteDoc,
  getDoc,
  getDocs,
  query,
  orderBy,
  limit,
  serverTimestamp,
  runTransaction,
  arrayUnion,
  arrayRemove
} = window.FB;


/* === js/core/config.js === */
'use strict';
/* ============================================================
   CONFIG  (same Firebase project / same admin whitelist as before)
   ============================================================ */
const firebaseConfig = {
  apiKey: "AIzaSyAJhZHDJnN4wX2R6S9hGb_XTh9b8rSvFhY",
  authDomain: "task-gullak.firebaseapp.com",
  projectId: "task-gullak",
  storageBucket: "task-gullak.firebasestorage.app",
  messagingSenderId: "402307564487",
  appId: "1:402307564487:android:344e64291c25e394274fe2"
};
const ALLOWED_ADMIN_EMAILS = ['snehkumarverma20111@gmail.com', 'snehkumarverma2011@gmail.com', 'savvy132007@gmail.com'];
const PLAY_ICON = 'data:image/svg+xml;utf8,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 128 128%22%3E%3Cdefs%3E%3ClinearGradient id=%22g%22 x1=%220%22 y1=%220%22 x2=%221%22 y2=%221%22%3E%3Cstop offset=%220%22 stop-color=%22%2338BDF8%22/%3E%3Cstop offset=%221%22 stop-color=%22%236366F1%22/%3E%3C/linearGradient%3E%3C/defs%3E%3Crect width=%22128%22 height=%22128%22 rx=%2230%22 fill=%22url%28%23g%29%22/%3E%3Crect x=%2214%22 y=%2214%22 width=%22100%22 height=%22100%22 rx=%2224%22 fill=%22%23fff%22 fill-opacity=%22.14%22/%3E%3Cpath d=%22M50 36.5v55a4 4 0 0 0 6 3.4l45-27.5a4 4 0 0 0 0-6.8L56 33.1a4 4 0 0 0-6 3.4z%22 fill=%22%23fff%22/%3E%3C/svg%3E';
const MAINT_TITLE = 'Maintenance Mode';
const MAINT_MSG = "We are currently undergoing scheduled maintenance to bring you a better experience. We'll be back online shortly!";
const DEFAULT_INSTRUCTIONS = [
  'Open the app using the provided Google Play Store link and install it, if you choose to test it.',
  'Explore the app and share your honest experience or feedback.',
  'Submit your own genuine feedback; do not copy a scripted review or promise a particular star rating.',
  'Upload the requested proof/screenshot before the task deadline, where applicable.'
];
const DAY = 864e5;
const RANGE_MS = { '24h': DAY, '7d': 7 * DAY, '30d': 30 * DAY };

const app = initializeApp(firebaseConfig);
/* No popup resolver at startup => Google's gapi script (apis.google.com) is NOT loaded unless Google sign-in is clicked on http(s). */
const auth = initializeAuth(app, { persistence: /^https?:$/.test(location.protocol) ? browserLocalPersistence : inMemoryPersistence });
const db = getFirestore(app);


/* === js/core/helpers.js === */
'use strict';
/* ============================================================
   HELPERS
   ============================================================ */
const $ = (s, r = document) => r.querySelector(s);
const $$ = (s, r = document) => [...r.querySelectorAll(s)];
const num = v => { const n = Number(v); return Number.isFinite(n) ? n : 0; };
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const inr = n => '₹' + num(n).toLocaleString('en-IN');
const tsMs = v => {
  if (!v) return 0;
  if (typeof v === 'number') return v < 1e11 ? v * 1000 : v;
  if (typeof v.toMillis === 'function') return v.toMillis();
  if (v.seconds != null) return v.seconds * 1000;
  if (typeof v === 'string') { const t = Date.parse(v); return isNaN(t) ? 0 : t; }
  return 0;
};
const fmtDT = ms => ms ? new Date(ms).toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: 'numeric', minute: '2-digit' }) : '—';
const fmtD = ms => ms ? new Date(ms).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : '—';
const ago = ms => {
  if (!ms) return '—';
  const s = Math.max(0, (Date.now() - ms) / 1000);
  if (s < 60) return 'just now';
  if (s < 3600) return Math.floor(s / 60) + ' min ago';
  if (s < 86400) return Math.floor(s / 3600) + ' hr ago';
  return Math.floor(s / 86400) + ' d ago';
};
const mask = acc => { const a = String(acc || ''); return a.length > 4 ? 'XXXX' + a.slice(-4) : a ? 'XXXX' : '—'; };
const slug = s => String(s || '').replace(/\W+/g, '_').replace(/^_+|_+$/g, '') || 'x';
const monthKey = ms => { const d = new Date(ms); return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0'); };
const monthRange = key => { const [y, m] = key.split('-').map(Number); return [new Date(y, m - 1, 1).getTime(), new Date(y, m, 1).getTime()]; };
const setHTML = (el, html) => { if (el && el._h !== html) { el.innerHTML = html; el._h = html; } };
const csvCell = v => { const s = String(v ?? ''); return /[",\n]/.test(s) ? '"' + s.replace(/"/g, '""') + '"' : s; };
const toCsv = rows => rows.map(r => r.map(csvCell).join(',')).join('\n');
function saveText(name, text, mime = 'text/plain') {
  const a = document.createElement('a');
  a.href = URL.createObjectURL(new Blob([text], { type: mime + ';charset=utf-8' }));
  a.download = name; document.body.appendChild(a); a.click(); a.remove();
  setTimeout(() => URL.revokeObjectURL(a.href), 2000);
}
const compact = v => v >= 1e7 ? (v / 1e7).toFixed(1).replace(/\.0$/, '') + 'Cr' : v >= 1e5 ? (v / 1e5).toFixed(1).replace(/\.0$/, '') + 'L' : v >= 1e3 ? (v / 1e3).toFixed(1).replace(/\.0$/, '') + 'K' : String(Math.round(v));
const copyText = async t => { try { await navigator.clipboard.writeText(t); toast('Copied', 'ok'); } catch { toast('Copy not allowed in this browser', 'bad'); } };

/* ---------- icons (inline SVG, no CDN) ---------- */
const ICONS = {
  sun: '<circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/>',
  moon: '<path d="M20 14.5A8 8 0 0 1 9.5 4a8 8 0 1 0 10.5 10.5z"/>',
  home: '<path d="M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z"/>',
  tasks: '<rect x="5" y="4" width="14" height="17" rx="2"/><path d="M9 4h6v3H9z"/><path d="m9 14 2 2 4-4"/>',
  message: '<path d="M21 12a8 8 0 0 1-11.5 7.2L4 20l1.1-4.3A8 8 0 1 1 21 12z"/><path d="M8.5 12h.01M12 12h.01M15.5 12h.01"/>',
  approve: '<rect x="3.5" y="3.5" width="17" height="17" rx="4"/><path d="m8 12 3 3 5-6"/>',
  clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
  calendar: '<rect x="3" y="5" width="18" height="16" rx="3"/><path d="M8 3v4M16 3v4M3 10h18"/>',
  users: '<circle cx="9" cy="8" r="3.5"/><path d="M2.5 20a6.5 6.5 0 0 1 13 0"/><path d="M16 5a3.2 3.2 0 0 1 0 6M18 14a6 6 0 0 1 3.5 6"/>',
  wallet: '<path d="M4 7a2 2 0 0 1 2-2h11v3"/><rect x="3" y="7" width="18" height="13" rx="3"/><circle cx="16.5" cy="13.5" r="1.2"/>',
  bank: '<path d="M3 9 12 4l9 5"/><path d="M5 10v8M9.5 10v8M14.5 10v8M19 10v8M3 20h18"/>',
  chart: '<path d="M4 20V10M10 20V4M16 20v-7M21 20H3"/>',
  bell: '<path d="M6 16V11a6 6 0 1 1 12 0v5l2 2H4z"/><path d="M10 21a2 2 0 0 0 4 0"/>',
  wrench: '<path d="M14.5 6.5a4 4 0 0 0-5.2 5.2L3.5 17.5a2 2 0 0 0 3 3l5.8-5.8a4 4 0 0 0 5.2-5.2l-2.6 2.6-2.4-.6-.6-2.4z"/>',
  settings: '<circle cx="12" cy="12" r="3"/><path d="M12 2v3M12 19v3M4.9 4.9 7 7M17 17l2.1 2.1M2 12h3M19 12h3M4.9 19.1 7 17M17 7l2.1-2.1"/>',
  activity: '<path d="M3 12h4l3-8 4 16 3-8h4"/>',
  code: '<path d="m8 8-5 4 5 4M16 8l5 4-5 4M14 5l-4 14"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  search: '<circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/>',
  download: '<path d="M12 4v11m0 0-4-4m4 4 4-4M5 20h14"/>',
  x: '<path d="M6 6l12 12M18 6 6 18"/>',
  check: '<path d="m5 12 5 5 9-10"/>',
  'chev-down': '<path d="m6 9 6 6 6-6"/>', 'chev-right': '<path d="m9 6 6 6-6 6"/>', 'chev-left': '<path d="m15 6-6 6 6 6"/>',
  menu: '<path d="M4 7h16M4 12h16M4 17h16"/>',
  shield: '<path d="M12 3 4.5 6v6c0 4.5 3.2 7.8 7.5 9 4.3-1.2 7.5-4.5 7.5-9V6z"/>',
  copy: '<rect x="8" y="8" width="12" height="12" rx="2"/><path d="M16 8V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h2"/>',
  eye: '<path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z"/><circle cx="12" cy="12" r="3"/>',
  'eye-off': '<path d="M3 3l18 18M10.6 6.1A9.7 9.7 0 0 1 12 6c6.5 0 10 6 10 6a17 17 0 0 1-3 3.6M6.6 7.6A16 16 0 0 0 2 12s3.5 7 10 7a9.6 9.6 0 0 0 4-.9"/>',
  trash: '<path d="M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13M10 11v6M14 11v6"/>',
  edit: '<path d="M4 20h4L19 9l-4-4L4 16z"/><path d="m13.5 6.5 4 4"/>',
  pause: '<rect x="6" y="5" width="4" height="14" rx="1"/><rect x="14" y="5" width="4" height="14" rx="1"/>',
  play: '<path d="M7 4.5v15l12-7.5z"/>',
  image: '<rect x="3" y="4" width="18" height="16" rx="3"/><circle cx="9" cy="10" r="1.6"/><path d="m4 18 5-5 4 4 3-3 4 4"/>',
  rocket: '<path d="M5 15c-1 1-1.5 4-1.5 4s3-.5 4-1.5M14 4c3-1 6-1 6-1s0 3-1 6l-6 6-5-5z"/><circle cx="15" cy="9" r="1.2"/>',
  send: '<path d="M21 3 10 14M21 3l-7 18-4-7-7-4z"/>',
  lock: '<rect x="5" y="11" width="14" height="9" rx="2"/><path d="M8 11V8a4 4 0 0 1 8 0v3"/>',
  logout: '<path d="M10 4H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h4M16 8l4 4-4 4M20 12H9"/>',
  refresh: '<path d="M20 11a8 8 0 0 0-14.5-3M4 5v4h4M4 13a8 8 0 0 0 14.5 3M20 19v-4h-4"/>',
  'arrow-right': '<path d="M5 12h14m-5-5 5 5-5 5"/>',
  'arrow-up': '<path d="M12 19V5m-6 6 6-6 6 6"/>',
  folder: '<path d="M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>',
  zap: '<path d="M13 2 4 14h7l-1 8 9-12h-7z"/>',
  'user-plus': '<circle cx="9" cy="8" r="3.5"/><path d="M2.5 20a6.5 6.5 0 0 1 13 0M19 8v6M16 11h6"/>',
  alert: '<path d="M12 3 2 20h20z"/><path d="M12 10v4M12 17h.01"/>',
  ban: '<circle cx="12" cy="12" r="9"/><path d="m5.6 5.6 12.8 12.8"/>',
  list: '<path d="M8 6h13M8 12h13M8 18h13M3.5 6h.01M3.5 12h.01M3.5 18h.01"/>',
  smartphone: '<rect x="7" y="2.5" width="10" height="19" rx="2.5"/><path d="M11 18.5h2"/>',
  star: '<path d="m12 3 2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1L3.2 9.5l6.1-.9z"/>',
  info: '<circle cx="12" cy="12" r="9"/><path d="M12 11v6M12 7.5h.01"/>',
  file: '<path d="M6 3h8l5 5v13H6z"/><path d="M14 3v5h5"/>',
  upload: '<path d="M12 20V9m0 0 4 4m-4-4-4 4M5 4h14"/>',
  bag: '<path d="M6 8h12l1 12H5z"/><path d="M9 8V6a3 3 0 0 1 6 0v2"/>',
  trend: '<path d="M3 17 9 11l4 4 8-9"/><path d="M15 6h6v6"/>',
  cash: '<rect x="3" y="6" width="18" height="12" rx="2"/><circle cx="12" cy="12" r="2.5"/>'
};
const ic = (n, s = 18) => `<svg class="ic" width="${s}" height="${s}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${ICONS[n] || ''}</svg>`;
function hydrateIcons(root = document) {
  $$('[data-ic]', root).forEach(el => {
    const tmp = document.createElement('span');
    tmp.innerHTML = ic(el.dataset.ic, el.dataset.s || 18);
    const svg = tmp.firstChild;
    if (el.className) svg.setAttribute('class', 'ic ' + el.className);
    el.replaceWith(svg);
  });
}


/* === js/core/state.js === */
'use strict';
/* ============================================================
   STATE
   ============================================================ */
const S = {
  users: [], tasks: [], subs: [], wds: [], bcasts: [], logs: [], meta: {},
  live: {}, errs: {}, admin: '', view: 'overview',
  ov: { apprTab: '24h', wdTab: 'pending', chart: 'month', q: '' },
  appr: { range: '24h', status: 'pending', from: '', to: '', q: '', groups: 6, open: {}, items: {}, folder: null, fmeta: {}, fs: 'pending', fq: '', fitems: 24 },
  sel: new Set(),
  tasksUI: { q: '', status: 'all', page: 1, day: 'today', date: '' },
  cm: { tab: 'bank', q: '', task: 'all', status: 'all', page: 1, sel: new Set(), blTask: '', folder: null },
  us: { q: '', status: 'all', sort: 'recent', from: '', to: '', page: 1 },
  wd: { tab: 'pending', q: '', method: 'all', month: monthKey(Date.now()), from: '', to: '', page: 1, reveal: new Set() },
  rp: { month: monthKey(Date.now()) },
  lg: { q: '', action: 'all', page: 1 },
  tm: { showCompleted: true },
  drawer: { uid: null, tab: 'subs', ledger: null }
};
let busy = false;


/* === js/core/ui.js === */
'use strict';
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


/* === js/core/audit.js === */
'use strict';
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


/* === js/core/data.js === */
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


/* === js/core/navigation.js === */
'use strict';
/* ============================================================
   NAVIGATION
   ============================================================ */
const VIEW_META = {
  overview: ['Dashboard', 'Welcome back, Admin! 👋'],
  tasks: ['Task Management', 'Create, edit, pause and archive app tasks'],
  comments: ['Comment Management', 'Internal feedback prompts and user restrictions'],
  approvals: ['Approval Management', 'Review proofs task-wise and approve or reject in bulk'],
  users: ['Users & Wallet', 'Profiles, wallet balance, history and account actions'],
  withdrawals: ['Withdrawal Management', 'Process UPI and bank payouts with full records'],
  reports: ['Monthly Records', 'Month-wise tasks, payouts, revenue and profit'],
  notifications: ['Push Notifications', 'Templates, custom messages and delivery history'],
  maintenance: ['Maintenance Mode', 'Lock the user app with a live notice'],
  settings: ['Settings', 'Admin access, security and app info'],
  logs: ['Activity Logs', 'Audit trail of every important admin action'],
  devtools: ['Developer Tools', 'Connections, exports and diagnostics']
};
function go(view, opts = {}) {
  if (!VIEW_META[view]) view = 'overview';
  S.view = view;
  { const vv = $('#v-' + view); if (vv) { vv.classList.remove('anim'); void vv.offsetWidth; vv.classList.add('anim'); setTimeout(() => vv.classList.remove('anim'), 1100); } }
  if (view === 'approvals') {
    if (opts.range) S.appr.range = opts.range;
    if (opts.status) S.appr.status = opts.status;
    if (opts.q != null) { S.appr.q = opts.q; $('#ap-q').value = opts.q; }
    S.appr.groups = 6; S.appr.folder = null;
  }
  if (view === 'comments' && opts.tab) { S.cm.tab = opts.tab; if (opts.tab === 'bank') S.cm.folder = null; }
  $$('.view').forEach(v => v.classList.toggle('on', v.id === 'v-' + view));
  $$('.nav-i[data-nav]').forEach(b => b.classList.toggle('on', b.dataset.nav === view));
  $('.nav-i[data-v="overview"]').classList.toggle('on', view === 'overview');
  $$('.nav-g').forEach(g => { const nb = $('.nav-i', g); if (nb) g.classList.toggle('open', nb.dataset.nav === view); });
  $$('.nav-sub button').forEach(b => b.classList.toggle('on', view === 'approvals' && b.dataset.range === S.appr.range || view === 'comments' && b.dataset.tab === S.cm.tab && b.dataset.v === 'comments'));
  const m = VIEW_META[view];
  $('#page-title').textContent = m[0]; $('#page-sub').textContent = m[1];
  document.body.classList.remove('nav-open');
  $('#user-menu').classList.add('hidden');
  renderAll(true);
  window.scrollTo({ top: 0 });
}


/* === js/core/events.js === */
'use strict';
/* ============================================================
   EVENT DELEGATION
   ============================================================ */
const ACT = {};
const INP = {};
document.addEventListener('click', e => {
  const el = e.target.closest('[data-act]');
  if (!el) { if (!e.target.closest('#user-menu') && !e.target.closest('[data-act="userMenu"]')) $('#user-menu')?.classList.add('hidden'); return; }
  const fn = ACT[el.dataset.act];
  if (fn) { try { const r = fn(el, e); if (r && r.catch) r.catch(err => { console.error(err); toast(err.message || 'Action failed', 'bad'); }); } catch (err) { console.error(err); toast(err.message || 'Action failed', 'bad'); } }
});
const onInput = e => {
  const el = e.target.closest('[data-in]'); if (!el) return;
  const fn = INP[el.dataset.in]; if (fn) fn(el, e);
};
document.addEventListener('input', onInput);
document.addEventListener('change', e => { if (e.target.matches('select,input[type=date],input[type=month],input[type=checkbox],input[type=file]')) onInput(e); });
document.addEventListener('keydown', e => {
  if (e.key === 'Escape') {
    const dlg = $('#dlg-overlay'); if (dlg.classList.contains('show')) { dlg._cancel && dlg._cancel(); return; }
    if ($('#lightbox').classList.contains('show')) { closeLightbox(); return; }
    if ($('#drawer').classList.contains('show')) { closeDrawer(); return; }
    if ($('#task-modal').classList.contains('show')) { closeTask(); return; }
    document.body.classList.remove('nav-open');
  }
  if (e.key === 'Enter' && (e.target.id === 'admin-pass-input' || e.target.id === 'admin-email-input')) window.unlockAdmin();
});
$('#dlg-overlay').addEventListener('mousedown', e => { if (e.target.id === 'dlg-overlay') e.currentTarget._cancel && e.currentTarget._cancel(); });

const themeMode = () => { try { return localStorage.getItem('tg_theme') || 'light'; } catch { return 'light'; } };
function applyTheme(m) { try { localStorage.setItem('tg_theme', m); } catch { } document.documentElement.dataset.theme = m === 'auto' ? (matchMedia('(prefers-color-scheme:dark)').matches ? 'dark' : 'light') : m; }
ACT.theme = () => { applyTheme(document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark'); renderAll(true); };
ACT.themeSet = el => { applyTheme(el.dataset.m); renderAll(true); };
function renderThemeCard() { const m = themeMode(); setHTML($('#st-theme'), `<p class="fs13 muted" style="margin-bottom:10px">Choose how the panel looks. Your choice is remembered in this browser.</p><div class="tabs">${[['light', 'sun', 'Light'], ['dark', 'moon', 'Dark'], ['auto', 'settings', 'Auto']].map(([k, i, l]) => `<button class="tab ${m === k ? 'on' : ''}" data-act="themeSet" data-m="${k}">${ic(i, 14)} ${l}</button>`).join('')}</div>`); }
ACT.go = el => go(el.dataset.v, { range: el.dataset.range, status: el.dataset.status, tab: el.dataset.tab });
ACT.navGroup = el => { const g = el.closest('.nav-g'); if (S.view === el.dataset.v) g.classList.toggle('open'); else go(el.dataset.v); };
ACT.toggleNav = () => { if (window.innerWidth < 1024) document.body.classList.toggle('nav-open'); else $('#shell').classList.toggle('collapsed'); };
ACT.closeNav = () => document.body.classList.remove('nav-open');
ACT.userMenu = () => $('#user-menu').classList.toggle('hidden');
ACT.bell = () => { if (pendingSubs().length) go('approvals', { range: 'all', status: 'pending' }); else if (pendingWds().length) go('withdrawals'); else toast('Nothing pending 🎉'); };
ACT.logout = async () => { try { await signOut(auth); } catch (_) { } location.reload(); };
ACT.page = el => { const k = el.dataset.k, d = Number(el.dataset.d); const map = { users: S.us, wd: S.wd, cm: S.cm, logs: S.lg, tasks: S.tasksUI }; if (map[k]) { map[k].page += d; renderAll(true); } };


/* === js/core/auth.js === */
'use strict';
/* ============================================================
   ADMIN PANEL LOGIN & AUTHENTICATION
   ============================================================ */
function openAdminConsole(email) {
  const cleanEmail = (email || 'snehkumarverma20111@gmail.com').trim().toLowerCase();
  const wasAlreadyLoggedIn = (S.admin === cleanEmail);
  S.admin = cleanEmail;
  try { localStorage.setItem('tg_admin_session_v2', cleanEmail); } catch (_) {}

  const loginScr = $('#login-screen'); if (loginScr) { loginScr.classList.add('hidden'); loginScr.style.display = 'none'; }
  const appScr = $('#app'); if (appScr) { appScr.classList.remove('hidden'); appScr.style.display = 'block'; }
  const av = $('#avatar'); if (av) av.textContent = (cleanEmail[0] || 'A').toUpperCase();
  const menuEmail = $('#menu-email'); if (menuEmail) menuEmail.textContent = cleanEmail;

  startListeners();

  // Unban/Unblock authorized admin in Firestore /users if flagged
  unbanAdminAccountIfNeeded(cleanEmail);

  if (!wasAlreadyLoggedIn) {
    go('overview');
    toast('Logged in as ' + cleanEmail, 'ok');
    logAction('admin_login', cleanEmail);
  }
}

async function unbanAdminAccountIfNeeded(email) {
  if (!email || !ALLOWED_ADMIN_EMAILS.includes(email.toLowerCase())) return;
  try {
    const q = query(collection(db, 'users'), limit(500));
    const snap = await getDocs(q);
    snap.docs.forEach(async docSnap => {
      const data = docSnap.data();
      if ((data.email || '').trim().toLowerCase() === email.toLowerCase()) {
        if (data.isBanned || data.accountStatus === 'deleted') {
          await updateDoc(doc(db, 'users', docSnap.id), {
            isBanned: false,
            banReason: '',
            accountStatus: 'active',
            bannedAt: null,
            updatedAt: serverTimestamp()
          });
        }
      }
    });
  } catch (_) {}
}

function restoreAdminSession() {
  try {
    const savedEmail = (localStorage.getItem('tg_admin_session_v2') || '').trim().toLowerCase();
    if (savedEmail && ALLOWED_ADMIN_EMAILS.includes(savedEmail)) {
      openAdminConsole(savedEmail);
      return true;
    }
  } catch (_) {}
  return false;
}

window.logoutAdmin = function() {
  S.admin = null;
  try { localStorage.removeItem('tg_admin_session_v2'); } catch (_) {}
  const appScr = $('#app'); if (appScr) { appScr.classList.add('hidden'); appScr.style.display = 'none'; }
  const loginScr = $('#login-screen'); if (loginScr) { loginScr.classList.remove('hidden'); loginScr.style.display = 'flex'; }
  toast('Logged out', 'ok');
};

window.signInAdminWithGoogle = async function () {
  const err = $('#login-error'); if (err) err.classList.add('hidden');
  if (!/^https?:$/.test(location.protocol)) {
    if (err) { err.textContent = 'Google sign-in does not work from a local file. Use admin email + master password below.'; err.classList.remove('hidden'); }
    return;
  }
  try {
    const res = await signInWithPopup(auth, new GoogleAuthProvider(), browserPopupRedirectResolver);
    const email = (res.user?.email || '').toLowerCase().trim();
    if (ALLOWED_ADMIN_EMAILS.includes(email)) {
      try { await setDoc(doc(db, 'admins', res.user.uid), { email, role: 'admin', updatedAt: serverTimestamp() }, { merge: true }); } catch (_) { }
      openAdminConsole(email);
    } else {
      await signOut(auth);
      if (err) { err.textContent = 'Access denied: ' + email + ' is not an authorized admin email.'; err.classList.remove('hidden'); }
    }
  } catch (e) {
    if (err) { err.textContent = 'Google sign-in blocked or cancelled (' + (e.code || e.message) + '). Use admin email + master password below.'; err.classList.remove('hidden'); }
  }
};

window.unlockAdmin = function () {
  const emailInput = ($('#admin-email-input')?.value || '').trim().toLowerCase();
  const passInput = ($('#admin-pass-input')?.value || '').trim();
  const err = $('#login-error');
  const validEmail = ALLOWED_ADMIN_EMAILS.includes(emailInput) || ALLOWED_ADMIN_EMAILS.includes(passInput.toLowerCase());
  const validPass = ['gullak_admin_2025', 'admin123'].includes(passInput.toLowerCase()) || ALLOWED_ADMIN_EMAILS.includes(passInput.toLowerCase());
  if (validEmail && validPass) {
    if (err) err.classList.add('hidden');
    openAdminConsole(ALLOWED_ADMIN_EMAILS.includes(emailInput) ? emailInput : passInput.toLowerCase());
  } else {
    if (err) { err.textContent = 'Access denied: authorized admin email + master password required.'; err.classList.remove('hidden'); }
  }
};

/* ============================================================
   AUTO LOGIN VIA URL HASH OR ENCODED URL (#pass.Gullak#apk.@2026)
   ============================================================ */
function checkUrlHashAutoLogin() {
  if (restoreAdminSession()) return;
  const fullUrl = (decodeURIComponent(location.href) || '').toLowerCase();
  const hash = (location.hash || '').toLowerCase();
  if (
    fullUrl.includes('pass.gullak') ||
    fullUrl.includes('apk.@2026') ||
    fullUrl.includes('gullak_admin_2025') ||
    hash.includes('pass.gullak') ||
    hash.includes('apk.@2026') ||
    hash.includes('gullak_admin_2025')
  ) {
    openAdminConsole('snehkumarverma20111@gmail.com');
  }
}

document.addEventListener('DOMContentLoaded', checkUrlHashAutoLogin);
setTimeout(checkUrlHashAutoLogin, 150);
setTimeout(checkUrlHashAutoLogin, 500);


/* === js/core/realtime.js === */
'use strict';
/* ============================================================
   REALTIME LISTENERS
   ============================================================ */
let raf = 0;
const schedule = () => { cancelAnimationFrame(raf); raf = requestAnimationFrame(() => renderAll()); };

/* Submitted = hidden: once a user has submitted a task, add them to that task's blocked list so it never shows for them again.
   You decide per upload: edit the task and choose "Show it again to everyone who already submitted" (sets reshowAt = now). Only submissions made after that are counted. */
let syncT = 0, syncBusy = false;
const queueSync = () => { clearTimeout(syncT); syncT = setTimeout(syncSubmitters, 1500); };
async function syncSubmitters() {
  if (syncBusy || !S.admin || !S.live.tasks || !S.live.submissions) return;
  const jobs = {}, mig = [];
  for (const t of S.tasks) if ((t.autoBlockedUserIds || []).length) mig.push(t);   // tidy-up: earlier version had put submitters into the blocked list
  for (const sb of S.subs) {
    if (!sb.userId) continue; const t = taskOfSub(sb); if (!t || t.hideAfterComplete === false) continue;
    const re = num(t.reshowAt); if (re && subMs(sb) && subMs(sb) <= re) continue;
    if ((t.submittedUserIds || []).includes(sb.userId) || (t.autoBlockedUserIds || []).includes(sb.userId)) continue;
    (jobs[t.id] = jobs[t.id] || new Set()).add(sb.userId);
  }
  if (!mig.length && !Object.keys(jobs).length) return;
  syncBusy = true;
  try {
    for (const t of mig) { const u = t.autoBlockedUserIds; await updateDoc(doc(db, 'tasks', t.id), { blockedUserIds: arrayRemove(...u), submittedUserIds: arrayUnion(...u), autoBlockedUserIds: [] }); }
    for (const tid of Object.keys(jobs)) { const u = [...jobs[tid]]; await updateDoc(doc(db, 'tasks', tid), { submittedUserIds: arrayUnion(...u), updatedAt: serverTimestamp() }); }
  } catch (e) { console.warn('submitter sync', e.code || e.message); }
  syncBusy = false;
}
let started = false;
function listen(key, ref, apply) {
  onSnapshot(ref, snap => { S.live[key] = true; S.errs[key] = null; apply(snap); schedule(); if (key === 'tasks' || key === 'submissions') queueSync(); },
    err => { S.live[key] = false; S.errs[key] = err.code || err.message; if (!['adminLogs', 'adminMeta'].includes(key)) console.warn('Listener', key, err.code || err.message); schedule(); });
}
function startListeners() {
  if (started) return; started = true;
  listen('users', collection(db, 'users'), snap => { S.users = snap.docs.map(d => ({ id: d.id, ...d.data() })); });
  listen('tasks', collection(db, 'tasks'), snap => { S.tasks = snap.docs.map(d => ({ id: d.id, ...d.data() })).sort((a, b) => tsMs(b.createdAt) - tsMs(a.createdAt)); setTimeout(sweepExpired, 800); });
  listen('submissions', collection(db, 'submissions'), snap => { S.subs = snap.docs.map(d => ({ id: d.id, ...d.data() })).sort((a, b) => subMs(b) - subMs(a)); });
  listen('withdrawals', collection(db, 'withdrawals'), snap => { S.wds = snap.docs.map(d => ({ id: d.id, ...d.data() })).sort((a, b) => wdMs(b) - wdMs(a)); });
  listen('broadcasts', collection(db, 'broadcasts'), snap => { S.bcasts = snap.docs.map(d => ({ id: d.id, ...d.data() })).sort((a, b) => tsMs(b.createdAt) - tsMs(a.createdAt)); });
  listen('supportTickets', collection(db, 'support_tickets'), snap => { S.tickets = snap.docs.map(d => ({ id: d.id, ...d.data() })).sort((a, b) => tsMs(b.createdAt) - tsMs(a.createdAt)); });
  listen('adminLogs', query(collection(db, 'adminLogs'), orderBy('createdMs', 'desc'), limit(300)), snap => { S.logs = snap.docs.map(d => ({ id: d.id, ...d.data() })); });
  listen('adminMeta', collection(db, 'adminMeta'), snap => { S.meta = {}; snap.docs.forEach(d => { S.meta[d.id] = d.data(); }); });
}


/* === js/core/finance.js === */
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


/* === js/core/render.js === */
'use strict';
/* ============================================================
   RENDER DISPATCH
   ============================================================ */
const RENDER = {};
function renderAll(force) {
  if (!S.admin) return;
  renderChrome();
  const fn = RENDER[S.view]; if (fn) fn(force);
  renderBulk();
  if (S.drawer.uid) renderDrawer();
}
function renderChrome() {
  const ps = pendingSubs().length, pw = pendingWds().length;
  $('#bdg-appr').textContent = ps; $('#bdg-appr').classList.toggle('hidden', !ps);
  $('#bdg-wd').textContent = pw; $('#bdg-wd').classList.toggle('hidden', !pw);
  const bell = ps + pw; $('#bell-cnt').textContent = bell > 99 ? '99+' : bell; $('#bell-cnt').classList.toggle('hidden', !bell);
  const core = ['users', 'tasks', 'submissions', 'withdrawals', 'broadcasts'];
  const errs = core.filter(k => S.errs[k]), okAll = core.every(k => S.live[k]);
  const c = $('#conn');
  c.classList.toggle('off', !okAll);
  c.lastElementChild.textContent = errs.length ? 'Check access' : okAll ? 'Online' : 'Connecting…';
  const on = isMaintActive();
  ['ov-switch', 'mt-switch'].forEach(id => { const e = $('#' + id); if (e) { e.classList.toggle('on', on); e.setAttribute('aria-pressed', on); } });
}


/* === js/views/overview.js === */
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


/* === js/views/approvals.js === */
'use strict';
/* ============================================================
   APPROVALS
   ============================================================ */
const hashCache = new Map();
function imgHash(s) {
  const b = s.screenshotBase64; if (!b) return null;
  const k = s.id + ':' + b.length; let h = hashCache.get(k); if (h) return h;
  let x = 2166136261; const stride = Math.max(1, Math.floor(b.length / 4000));
  for (let i = 0; i < b.length; i += stride) { x ^= b.charCodeAt(i); x = Math.imul(x, 16777619); }
  h = (x >>> 0).toString(36) + '-' + b.length; hashCache.set(k, h); return h;
}
let dupCache = { for: null, img: new Map(), ut: new Map() };
function dupFlags(s) {
  if (dupCache.for !== S.subs) {
    dupCache = { for: S.subs, img: new Map(), ut: new Map() };
    S.subs.forEach(x => { const h = imgHash(x); if (h) dupCache.img.set(h, (dupCache.img.get(h) || 0) + 1); const k = (x.userId || '') + '|' + (x.taskId || x.taskTitle || ''); if (x.userId) dupCache.ut.set(k, (dupCache.ut.get(k) || 0) + 1); });
  }
  const out = [], h = imgHash(s);
  if (h && dupCache.img.get(h) > 1) out.push('Same image used ×' + dupCache.img.get(h));
  const k = (s.userId || '') + '|' + (s.taskId || s.taskTitle || '');
  if (s.userId && dupCache.ut.get(k) > 1) out.push('Repeat from same user ×' + dupCache.ut.get(k));
  return out;
}
function apprBase(noQ) {
  const f = S.appr, now = Date.now(); let from = 0, to = Infinity;
  if (RANGE_MS[f.range]) from = now - RANGE_MS[f.range];
  else if (f.range === 'custom') { if (f.from) from = new Date(f.from + 'T00:00:00').getTime(); if (f.to) to = new Date(f.to + 'T23:59:59.999').getTime(); }
  const q = noQ ? '' : f.q.trim().toLowerCase();
  return S.subs.filter(s => {
    const t = subMs(s) || now;
    if (f.range !== 'all' && (t < from || t > to)) return false;
    if (q && ![s.proofText, s.userId, s.id, s.userName, s.userEmail, s.taskTitle, s.taskId, s.adminNote].join(' ').toLowerCase().includes(q)) return false;
    return true;
  });
}
const apprList = () => { const b = apprBase(); return S.appr.status === 'all' ? b : b.filter(s => subSt(s) === S.appr.status); };
function groupSubs(list) {
  const g = {};
  list.forEach(s => { const k = subKey(s); (g[k] = g[k] || { key: k, taskId: (s.taskId || 'general').trim(), title: (s.taskTitle || 'Untitled Task').trim(), items: [] }).items.push(s); });
  return Object.values(g).sort((a, b) => subMs(b.items[0]) - subMs(a.items[0]));
}
function subCardHTML(s) {
  const st = subSt(s), sel = S.sel.has(s.id), fl = dupFlags(s);
  const shot = s.screenshotBase64 ? `<div class="shot-wrap"><img class="shot" src="${esc(s.screenshotBase64)}" alt="Proof screenshot" loading="lazy" decoding="async" data-act="zoom" data-id="${esc(s.id)}"><span class="zoom">Zoom</span></div>` : '<div class="noimg">No image</div>';
  return `<div class="sub ${sel ? 'sel' : ''}">
    <div class="row" style="align-items:flex-start;gap:10px">
      <input type="checkbox" class="chk" style="margin-top:3px" data-act="selOne" data-id="${esc(s.id)}" ${sel ? 'checked' : ''} aria-label="Select submission">
      ${shot}
      <div class="grow col" style="gap:4px;min-width:0">
        <div class="row between" style="gap:6px"><span class="b fs13 trunc">${esc(s.userName || 'User')}</span>${statusPill(st)}</div>
        <div class="fs12 muted trunc">${esc(s.userEmail || s.userId || '')}</div>
        <div class="fs12 muted">${fmtDT(subMs(s))}</div>
        <div class="fs13 xb brand-t">Reward ${inr(s.rewardAmount)}</div>
        <div class="proof">💬 ${esc(s.proofText || 'No feedback text')}</div>
        ${fl.map(f => `<div class="pill warn" style="align-self:flex-start">⚠ ${esc(f)}</div>`).join('')}
        ${st === 'rejected' && s.adminNote ? `<div class="fs11 bad-t">Reason: ${esc(s.adminNote)}</div>` : ''}
      </div>
    </div>
    <div class="row between wrap" style="gap:6px;border-top:1px solid var(--line);padding-top:10px">
      <span class="fs11 muted mono">ID ${esc(s.id.slice(0, 8))}</span>
      <div class="row" style="gap:6px">
        <button class="btn ghost xs" data-act="saveShot" data-id="${esc(s.id)}" title="Download proof">${ic('download', 13)}</button>
        ${st === 'pending' ? `<button class="btn ok xs" data-act="approve" data-id="${esc(s.id)}">${ic('check', 13)}Approve</button><button class="btn bad xs" data-act="reject" data-id="${esc(s.id)}">${ic('x', 13)}Reject</button>` : `<span class="pill ${st === 'approved' ? 'ok' : 'bad'}">${ic('lock', 12)}${st === 'approved' ? 'Approved' : 'Rejected'} · final</span>`}
      </div>
    </div>
  </div>`;
}
// Exact matching: the whole comment must equal the pasted line character-for-character (case, punctuation, emoji, inner spaces).
// Only line endings, outer spaces and Unicode composition form are ignored, so a pasted line still equals the stored text.
const normT = x => String(x == null ? '' : x).normalize('NFC').replace(/\r/g, '').trim();
const subMatches = (s, qs) => { const pt = normT(s.proofText); return !!pt && qs.includes(pt); };
function folderView() {
  const f = S.appr, all = apprBase(true).filter(s => subKey(s) === f.folder), qs = [...new Set(f.fq.split('\n').map(normT).filter(Boolean))];
  const searched = qs.length ? all.filter(s => subMatches(s, qs)) : all;
  return { all, qs, searched, shown: f.fs === 'all' ? searched : searched.filter(s => subSt(s) === f.fs) };
}
const cntSt = (l, k) => l.filter(s => subSt(s) === k).length;
RENDER.approvals = () => {
  const f = S.appr, inF = !!f.folder;
  setHTML($('#ap-range'), [['24h', '24 Hours'], ['7d', '7 Days'], ['30d', '30 Days'], ['all', 'All time'], ['custom', 'Date wise']].map(([k, l]) => `<button class="tab ${f.range === k ? 'on' : ''}" data-act="apRange" data-k="${k}">${l}</button>`).join(''));
  $('#ap-custom').classList.toggle('hidden', f.range !== 'custom');
  $('#ap-row2').classList.toggle('hidden', inF); $('#ap-folder').classList.toggle('hidden', !inF);
  $('#ap-groups').classList.toggle('hidden', inF); $('#ap-more').classList.toggle('hidden', inF);
  if (inF) return renderApFolder();
  const base = apprBase(), c = k => base.filter(s => subSt(s) === k).length;
  setHTML($('#ap-status'), [['pending', 'Pending', c('pending')], ['approved', 'Approved', c('approved')], ['rejected', 'Rejected', c('rejected')], ['all', 'All', base.length]].map(([k, l, n]) => `<button class="tab ${f.status === k ? 'on' : ''}" data-act="apStatus" data-k="${k}">${l} <span class="n">(${n})</span></button>`).join(''));
  if (!S.live.submissions && !S.subs.length) { setHTML($('#ap-groups'), skelHTML(3)); setHTML($('#ap-more'), ''); return; }
  const groups = groupSubs(base).filter(g => f.status === 'all' || g.items.some(s => subSt(s) === f.status));
  if (!groups.length) { setHTML($('#ap-groups'), `<div class="card">${emptyHTML('folder', 'No folders match these filters.')}</div>`); setHTML($('#ap-more'), ''); return; }
  setHTML($('#ap-groups'), `<div class="g2 auto">${groups.slice(0, f.groups).map(g => {
    const t = taskOfSub(g.items[0]), shots = g.items.filter(s => s.screenshotBase64).length, ts = g.items.map(subMs).filter(Boolean);
    const dr = ts.length ? fmtD(Math.min(...ts)) + (Math.min(...ts) !== Math.max(...ts) ? ' – ' + fmtD(Math.max(...ts)) : '') : '—';
    return `<button class="folder" data-act="apOpen" data-key="${esc(g.key)}"><div class="row"><img class="appico" style="width:50px;height:50px;border-radius:14px" src="${esc(t ? taskIcon(t) : PLAY_ICON)}" alt=""><div class="grow" style="min-width:0;text-align:left"><div class="b fs16 trunc">${esc(g.title)}</div><div class="fs12 muted">${ic('image', 12)} ${shots} screenshot${shots === 1 ? '' : 's'} · ${esc(dr)}</div></div>${ic('chev-right', 16)}</div><div class="row wrap gap6"><span class="pill warn">${cntSt(g.items, 'pending')} pending</span><span class="pill ok">${cntSt(g.items, 'approved')} approved</span><span class="pill bad">${cntSt(g.items, 'rejected')} rejected</span></div></button>`;
  }).join('')}</div>`);
  setHTML($('#ap-more'), groups.length > f.groups ? `<button class="btn ghost" data-act="apMoreGroups">Show more folders (${groups.length - f.groups} left)</button>` : '');
};
function renderApFolder() {
  const f = S.appr, { all, qs, searched, shown } = folderView(), meta = f.fmeta || {}, first = all[0];
  const t = first ? taskOfSub(first) : S.tasks.find(x => x.id === meta.taskId), title = meta.title || (t && taskName(t)) || 'Task';
  const pend = searched.filter(s => subSt(s) === 'pending'), ts = all.map(subMs).filter(Boolean);
  const dr = ts.length ? fmtD(Math.min(...ts)) + (Math.min(...ts) !== Math.max(...ts) ? ' – ' + fmtD(Math.max(...ts)) : '') : '—';
  const k = (cls, l, v) => `<div class="kpi s ${cls}" style="padding:10px 14px"><div class="grow"><div class="lbl">${l}</div><div class="val" style="font-size:20px">${v}</div></div></div>`;
  const dis = pend.length && !busy ? '' : 'disabled';
  setHTML($('#ap-fhead'), `<div class="card"><button class="btn ghost sm" data-act="apBack">${ic('chev-left', 15)}All folders</button>
    <div class="row wrap mt12" style="gap:14px"><img class="appico" style="width:60px;height:60px;border-radius:16px" src="${esc(t ? taskIcon(t) : PLAY_ICON)}" alt=""><div class="grow" style="min-width:180px"><div class="b fs20 trunc">${esc(title)}</div><div class="row wrap gap6 mt4"><span class="tag mono">Task ${esc(String(meta.taskId || '').slice(0, 10))}</span><span class="tag">${inr(t ? taskReward(t) : first ? first.rewardAmount : 0)} per task</span><span class="tag">${esc(dr)}</span></div></div>
    <div class="row wrap gap6"><button class="btn okfill sm" data-act="apFApprove" ${dis}>${ic('check', 14)}Approve all (${pend.length})</button><button class="btn bad sm" data-act="apFReject" ${dis}>${ic('x', 14)}Reject all (${pend.length})</button><button class="btn ghost sm" data-act="apFZip">${ic('download', 14)}ZIP</button></div></div>
    <div class="g4 mt12">${k('blue', 'Total screenshots', all.filter(s => s.screenshotBase64).length) + k('orange', 'Pending', cntSt(all, 'pending')) + k('green', 'Approved', cntSt(all, 'approved')) + k('red', 'Rejected', cntSt(all, 'rejected'))}</div></div>`);
  const lines = [...new Set(f.fq.split('\n').map(normT).filter(Boolean))], miss = lines.filter(l => !all.some(s => subMatches(s, [l])));
  setHTML($('#ap-fsearchinfo'), qs.length ? `<div class="mbar"><span class="spill ${searched.length ? 'ok' : 'mute'}">${ic('check', 12)}${searched.length} exact match${searched.length === 1 ? '' : 'es'}</span>${miss.length ? `<details class="smiss"><summary class="spill warn">${ic('alert', 12)}${miss.length} not found</summary><div class="smiss-l">${miss.slice(0, 8).map(m => `<div>${esc(m.slice(0, 80))}</div>`).join('')}${miss.length > 8 ? `<div class="muted">+${miss.length - 8} more</div>` : ''}</div></details>` : ''}</div><div class="sact"><button class="btn okfill xs" data-act="apFApprove" ${dis}>${ic('check', 12)}Approve ${pend.length}</button><button class="btn bad xs" data-act="apFReject" ${dis}>${ic('x', 12)}Reject ${pend.length}</button><button class="btn ghost xs" data-act="apFBlock" ${searched.length && !busy ? '' : 'disabled'}>${ic('ban', 12)}Block users</button><button class="btn ghost xs" data-act="apFClear">Clear search</button><span class="fs11 muted">Exact word-for-word match • actions apply only to matched items</span></div>` : '');
  setHTML($('#ap-ftabs'), [['pending', 'Pending'], ['approved', 'Approved'], ['rejected', 'Rejected'], ['all', 'All']].map(([x, l]) => `<button class="tab ${f.fs === x ? 'on' : ''}" data-act="apFTab" data-k="${x}">${l} <span class="n">(${x === 'all' ? searched.length : cntSt(searched, x)})</span></button>`).join(''));
  setHTML($('#ap-fitems'), shown.slice(0, f.fitems).map(subCardHTML).join('') || `<div class="card" style="grid-column:1/-1">${emptyHTML('image', qs.length ? 'No screenshots match this search.' : 'No screenshots here.')}</div>`);
  setHTML($('#ap-fmore'), shown.length > f.fitems ? `<button class="btn ghost sm" data-act="apFMore">Show more (${shown.length - f.fitems} left)</button>` : '');
}
ACT.apOpen = el => { const g = groupSubs(apprBase()).find(x => x.key === el.dataset.key); S.appr.folder = el.dataset.key; S.appr.fmeta = g ? { title: g.title, taskId: g.taskId } : {}; S.appr.fs = 'pending'; S.appr.fq = ''; $('#ap-fq').value = ''; S.appr.fitems = 24; S.sel.clear(); renderAll(); window.scrollTo({ top: 0 }); };
ACT.apBack = () => { S.appr.folder = null; S.sel.clear(); renderAll(); window.scrollTo({ top: 0 }); };
ACT.apFTab = el => { S.appr.fs = el.dataset.k; S.appr.fitems = 24; renderAll(); };
ACT.apFMore = () => { S.appr.fitems += 24; renderAll(); };
let apFqT = 0;
INP.apFq = el => { S.appr.fq = el.value; S.appr.fitems = 24; clearTimeout(apFqT); apFqT = setTimeout(renderAll, 250); };
ACT.apFZip = async () => { const v = folderView(); await downloadZip(v.shown, (S.appr.fmeta && S.appr.fmeta.title) || 'Proofs'); };
ACT.apFApprove = async () => {
  const items = folderView().searched.filter(s => subSt(s) === 'pending'); if (!items.length) return toast('No pending screenshots to approve', 'bad');
  const total = items.reduce((n, s) => n + num(s.rewardAmount), 0);
  if (!await confirmBox(`Approve ${items.length} screenshot${items.length > 1 ? 's' : ''}?`, `A total of <b>${inr(total)}</b> will be credited to the users’ wallets. Approved items are final and cannot be changed later.`, { ok: `Approve ${items.length}` })) return;
  busy = true; renderAll(); let ok = 0, fail = 0;
  for (const s of items) { try { await approveCore(s); ok++; } catch (e) { fail++; console.warn(e); } }
  busy = false; renderAll(); toast(`Approved ${ok}${fail ? `, failed ${fail}` : ''}`, fail ? 'bad' : 'ok');
};
ACT.apFReject = async () => {
  const items = folderView().searched.filter(s => subSt(s) === 'pending'); if (!items.length) return toast('No pending screenshots to reject', 'bad');
  const reason = await rejectDialog(`Reject ${items.length} screenshot${items.length > 1 ? 's' : ''}?`, 'Rejected items are final and cannot be changed later. The reason will be shown to users.');
  if (reason === null) return;
  busy = true; renderAll(); let ok = 0, fail = 0;
  for (const s of items) { try { await rejectCore(s, reason); ok++; } catch (e) { fail++; console.warn(e); } }
  busy = false; renderAll(); toast(`Rejected ${ok}${fail ? `, failed ${fail}` : ''}`, fail ? 'bad' : 'ok');
};
ACT.apFClear = () => { S.appr.fq = ''; $('#ap-fq').value = ''; S.appr.fitems = 24; renderAll(); };
ACT.apFBlock = () => blockUsersFromSubs(folderView().searched);
ACT.bulkBlock = async () => { const ok = await blockUsersFromSubs(S.subs.filter(s => S.sel.has(s.id))); if (ok) { S.sel.clear(); renderAll(); } };
async function blockUsersFromSubs(items) {
  const by = {}; items.forEach(x => { const t = taskOfSub(x); if (t && x.userId) (by[t.id] = by[t.id] || new Set()).add(x.userId); });
  const tids = Object.keys(by), n = new Set(items.map(x => x.userId).filter(Boolean)).size;
  if (!tids.length) { toast('No users to block', 'bad'); return false; }
  if (!await confirmBox(`Block ${n} user${n > 1 ? 's' : ''}?`, 'They will no longer see this task. Users are blocked only when you do this — you can unblock them any time from Comments → Blocked users.', { ok: `Block ${n}` })) return false;
  busy = true; renderAll(); let done = false;
  try { for (const tid of tids) { const u = [...by[tid]]; await updateDoc(doc(db, 'tasks', tid), { blockedUserIds: arrayUnion(...u), updatedAt: serverTimestamp(), updatedBy: S.admin }); logAction('user_block_task', tid, { users: u }); } toast(`Blocked ${n} user${n > 1 ? 's' : ''}`, 'ok'); done = true; }
  catch (e) { toast('Block failed: ' + e.message, 'bad'); }
  busy = false; renderAll(); return done;
}
ACT.apRange = el => { S.appr.range = el.dataset.k; S.appr.groups = 6; renderAll(); };
ACT.apStatus = el => { S.appr.status = el.dataset.k; S.appr.groups = 6; renderAll(); };
INP.apQ = el => { S.appr.q = el.value; S.appr.groups = 6; renderAll(); };
INP.apFrom = el => { S.appr.from = el.value; renderAll(); };
INP.apTo = el => { S.appr.to = el.value; renderAll(); };
ACT.apToggle = el => {
  const k = el.dataset.key, f = S.appr, idx = groupSubs(apprList()).findIndex(x => x.key === k);
  f.open[k] = !(f.open[k] ?? (idx < 2 || !!f.q)); renderAll();
};
ACT.apMoreItems = el => { S.appr.items[el.dataset.key] = (S.appr.items[el.dataset.key] || 12) + 24; renderAll(); };
ACT.apMoreGroups = () => { S.appr.groups += 6; renderAll(); };
ACT.selOne = el => {
  el.checked ? S.sel.add(el.dataset.id) : S.sel.delete(el.dataset.id);
  const card = el.closest('.sub'); if (card) card.classList.toggle('sel', el.checked);
  const grp = el.closest('.grp'); if (grp) { const all = $$('.sub .chk', grp); const gc = $('.grp-h .chk', grp); if (gc) gc.checked = all.length > 0 && all.every(c => c.checked); }
  grp && (grp.parentElement._h = null); renderBulk();
};
ACT.selGroup = el => {
  const g = groupSubs(apprList()).find(x => x.key === el.dataset.key); if (!g) return;
  g.items.forEach(s => el.checked ? S.sel.add(s.id) : S.sel.delete(s.id)); renderAll();
};
ACT.selClear = () => { S.sel.clear(); renderAll(); };

function renderBulk() {
  const bar = $('#bulkbar');
  S.sel.forEach(id => { if (!S.subs.find(s => s.id === id)) S.sel.delete(id); });
  if (S.view !== 'approvals' || !S.sel.size) { bar.classList.add('hidden'); return; }
  const items = S.subs.filter(s => S.sel.has(s.id));
  const money = items.filter(s => subSt(s) === 'pending').reduce((n, s) => n + num(s.rewardAmount), 0);
  bar.classList.remove('hidden');
  const dis = busy ? 'disabled' : '', pc = items.filter(s => subSt(s) === 'pending').length;
  setHTML(bar, `<div class="bb-info"><div class="bb-count">${items.length}</div><div class="bb-txt"><b>${items.length === 1 ? 'Submission' : 'Submissions'} selected</b><span>${pc} pending • <em>${inr(money)}</em> to credit</span></div></div>
    <div class="bb-sep"></div>
    <div class="bb-acts">
      <button class="bb ok" data-act="bulkApprove" ${dis}>${ic('check', 15)}Approve All</button>
      <button class="bb no" data-act="bulkReject" ${dis}>${ic('x', 15)}Reject All</button>
      <button class="bb blk" data-act="bulkBlock" ${dis}>${ic('ban', 15)}Block</button>
      <button class="bb clr" data-act="selClear" title="Clear selection">${ic('x', 14)}Clear</button>
    </div>`);
}

/* ---------- approve / reject core (same Firestore logic as before) ---------- */
async function approveCore(sub) {
  const reward = Math.max(0, num(sub.rewardAmount));
  const subRef = doc(db, 'submissions', sub.id);
  const userRef = sub.userId ? doc(db, 'users', sub.userId) : null;
  await runTransaction(db, async tx => {
    const sSnap = await tx.get(subRef);
    if (sSnap.exists() && !['pending', ''].includes((sSnap.data().status || '').toLowerCase())) throw new Error('Already ' + sSnap.data().status + ' — decisions are final');
    const uSnap = userRef ? await tx.get(userRef) : null;
    tx.update(subRef, { status: 'approved', adminNote: 'Approved & credited by Admin', reviewedBy: S.admin, updatedAt: serverTimestamp() });
    if (uSnap && uSnap.exists()) {
      tx.update(userRef, { walletBalance: num(uSnap.data().walletBalance) + reward, completedTasksCount: num(uSnap.data().completedTasksCount) + 1, updatedAt: serverTimestamp() });
    }
    if (userRef) tx.set(doc(collection(db, 'users', sub.userId, 'transactions')), { userId: sub.userId, amount: reward, type: 'task_reward', note: `Approved: ${sub.taskTitle || 'Task'}`, linkedSubmission: sub.id, createdAt: serverTimestamp() });
  });
  logAction('approve_submission', sub.id, { task: sub.taskTitle, user: sub.userId, amount: reward });
}
async function rejectCore(sub, reason) {
  const wasApproved = subSt(sub) === 'approved';
  const subRef = doc(db, 'submissions', sub.id);
  const userRef = sub.userId ? doc(db, 'users', sub.userId) : null;
  await runTransaction(db, async tx => {
    const sSnap = await tx.get(subRef);
    if (sSnap.exists() && !['pending', ''].includes((sSnap.data().status || '').toLowerCase())) throw new Error('Already ' + sSnap.data().status + ' — decisions are final');
    const uSnap = userRef ? await tx.get(userRef) : null;
    tx.update(subRef, { status: 'rejected', adminNote: reason, rejectionReason: reason, reviewedBy: S.admin, updatedAt: serverTimestamp() });
    if (wasApproved && uSnap && uSnap.exists()) tx.update(userRef, { walletBalance: Math.max(0, num(uSnap.data().walletBalance) - Math.max(0, num(sub.rewardAmount))), updatedAt: serverTimestamp() });
  });
  logAction('reject_submission', sub.id, { task: sub.taskTitle, user: sub.userId, reason, reversedCredit: wasApproved });
}
const REJECT_REASONS = ['Proof did not meet requirements', 'Screenshot unclear or incomplete', 'Duplicate / reused proof', 'Task steps not completed', 'Other (write below)'];
const rejectDialog = (title, body, danger = true) => ask({
  title, body, ok: 'Reject', danger, fields: [
    { id: 'preset', label: 'Reason', type: 'select', options: REJECT_REASONS, value: REJECT_REASONS[0] },
    { id: 'custom', label: 'Custom reason (optional)', type: 'textarea', placeholder: 'This is shown to the user' }]
}).then(v => v ? (v.custom || (v.preset.startsWith('Other') ? 'Rejected by Admin' : v.preset)) : null);

ACT.approve = async el => {
  const sub = S.subs.find(x => x.id === el.dataset.id); if (!sub || busy) return;
  if (subSt(sub) !== 'pending') return toast('This decision is final and cannot be changed', 'bad');
  busy = true;
  try { await approveCore(sub); toast(`Approved & credited ${inr(sub.rewardAmount)} to ${sub.userName || 'user'}`, 'ok'); } catch (e) { toast('Approve failed: ' + e.message, 'bad'); }
  busy = false; renderAll();
};
ACT.reject = async el => {
  const sub = S.subs.find(x => x.id === el.dataset.id); if (!sub || busy) return;
  if (subSt(sub) !== 'pending') return toast('This decision is final and cannot be changed', 'bad');
  const wasApproved = subSt(sub) === 'approved';
  const reason = await rejectDialog('Reject submission', wasApproved
    ? `<b>Warning:</b> already approved. Rejecting will reverse <b>${inr(sub.rewardAmount)}</b> from ${esc(sub.userName || 'the user')}'s wallet.`
    : `Reject ${esc(sub.userName || 'user')}'s proof for <b>${esc(sub.taskTitle || 'task')}</b>? The reason may be shown to the user.`);
  if (reason === null) return;
  busy = true;
  try { await rejectCore(sub, reason); toast('Submission rejected', 'ok'); } catch (e) { toast('Reject failed: ' + e.message, 'bad'); }
  busy = false; renderAll();
};
ACT.bulkApprove = async () => {
  const items = S.subs.filter(s => S.sel.has(s.id) && subSt(s) === 'pending'); if (!items.length) return toast('No pending items selected', 'bad');
  const total = items.reduce((n, s) => n + num(s.rewardAmount), 0);
  if (!await confirmBox(`Approve ${items.length} submission${items.length > 1 ? 's' : ''}?`, `A total of <b>${inr(total)}</b> will be credited to the users’ wallets. Only the selected records will be approved; the rest stay pending.`, { ok: `Approve ${items.length}` })) return;
  busy = true; renderBulk(); let ok = 0, fail = 0;
  for (const s of items) { try { await approveCore(s); ok++; } catch (e) { fail++; console.warn(e); } }
  busy = false; S.sel.clear(); renderAll(); toast(`Approved ${ok}${fail ? `, failed ${fail}` : ''}`, fail ? 'bad' : 'ok');
};
ACT.bulkReject = async () => {
  const all = S.subs.filter(s => S.sel.has(s.id)), items = all.filter(s => subSt(s) === 'pending'), skipped = all.length - items.length;
  if (!items.length) return toast('Only pending items can be rejected — approved and rejected items are final.', 'bad');
  const reason = await rejectDialog(`Reject ${items.length} submission${items.length > 1 ? 's' : ''}?`, `Only the selected records will be rejected${skipped ? ` (${skipped} non-pending will be skipped)` : ''}. The reason will be shown to users.`);
  if (reason === null) return;
  busy = true; renderBulk(); let ok = 0, fail = 0;
  for (const s of items) { try { await rejectCore(s, reason); ok++; } catch (e) { fail++; console.warn(e); } }
  busy = false; S.sel.clear(); renderAll(); toast(`Rejected ${ok}${fail ? `, failed ${fail}` : ''}`, fail ? 'bad' : 'ok');
};

/* ---------- lightbox & downloads ---------- */
function extOf(dataUrl) { const m = /^data:image\/(\w+)/.exec(dataUrl || ''); const e = m ? m[1].toLowerCase() : 'jpg'; return e === 'jpeg' ? 'jpg' : e; }
function fileLabel(s) { return `${slug(s.taskTitle)}_${slug(s.userName)}_${s.id.slice(0, 5)}.${extOf(s.screenshotBase64)}`; }
function downloadShot(id) {
  const s = S.subs.find(x => x.id === id); if (!s || !s.screenshotBase64) return toast('No screenshot found', 'bad');
  const a = document.createElement('a'); a.href = s.screenshotBase64; a.download = fileLabel(s); document.body.appendChild(a); a.click(); a.remove();
}
ACT.saveShot = el => downloadShot(el.dataset.id);
ACT.zoom = el => openLightbox(el.dataset.id);
function openLightbox(id) {
  const s = S.subs.find(x => x.id === id); if (!s || !s.screenshotBase64) return;
  const img = $('#lb-img'); img.src = s.screenshotBase64; img.classList.remove('z');
  $('#lb-cap').textContent = `${s.taskTitle || 'Task'} • ${s.userName || 'User'} • ${inr(s.rewardAmount)}`;
  $('#lb-meta').innerHTML = `${statusPill(subSt(s))} &nbsp; ${fmtDT(subMs(s))} &nbsp; <span class="mono">ID ${esc(s.id)}</span><br>💬 ${esc(s.proofText || 'No feedback text')}`;
  $('#lb-dl').onclick = () => downloadShot(id);
  $('#lightbox').classList.add('show');
}
function closeLightbox() { $('#lightbox').classList.remove('show'); }
ACT.closeLb = closeLightbox;
ACT.lbZoom = () => $('#lb-img').classList.toggle('z');
$('#lightbox').addEventListener('mousedown', e => { if (e.target.id === 'lightbox') closeLightbox(); });

function loadJSZip() {
  if (window.JSZip) return Promise.resolve(window.JSZip);
  return new Promise((res, rej) => {
    const s = document.createElement('script'); s.src = 'https://cdnjs.cloudflare.com/ajax/libs/jszip/3.10.1/jszip.min.js';
    s.onload = () => window.JSZip ? res(window.JSZip) : rej(new Error('JSZip missing')); s.onerror = () => rej(new Error('ZIP library failed to load (internet / CDN blocked)')); document.head.appendChild(s);
  });
}
async function downloadZip(list, name) {
  list = list.filter(s => s.screenshotBase64);
  if (!list.length) return toast('No screenshots in this selection', 'bad');
  toast(`Packaging ${list.length} proofs into ZIP…`);
  try {
    const JSZip = await loadJSZip(), zip = new JSZip();
    list.forEach((s, i) => {
      const b64 = s.screenshotBase64.includes(',') ? s.screenshotBase64.split(',')[1] : s.screenshotBase64;
      zip.folder(`${slug(s.taskTitle)}_${(s.taskId || 'id').slice(0, 6)}`).file(`${i + 1}_${slug(s.userName)}_${subSt(s)}_${s.id.slice(0, 5)}.${extOf(s.screenshotBase64)}`, b64, { base64: true });
    });
    const blob = await zip.generateAsync({ type: 'blob' });
    const a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = `${slug(name)}_${Date.now()}.zip`; document.body.appendChild(a); a.click(); a.remove();
    setTimeout(() => URL.revokeObjectURL(a.href), 3000);
    logAction('download_proofs_zip', name, { count: list.length });
    toast(`ZIP ready (${list.length} proofs)`, 'ok');
  } catch (e) { toast(e.message, 'bad'); }
}
ACT.apZip = async el => {
  const g = groupSubs(apprList()).find(x => x.key === el.dataset.key); if (!g) return;
  const picked = g.items.filter(s => S.sel.has(s.id));
  await downloadZip(picked.length ? picked : g.items, g.title);
};
ACT.apZipAll = async () => { const sel = S.subs.filter(s => S.sel.has(s.id)); await downloadZip(sel.length ? sel : apprList(), 'Proofs_' + S.appr.range); };


/* === js/views/tasks.js === */
'use strict';
/* ============================================================
   TASKS
   ============================================================ */
const ymd = d => { const x = new Date(d); return x.getFullYear() + '-' + String(x.getMonth() + 1).padStart(2, '0') + '-' + String(x.getDate()).padStart(2, '0'); };
function taskDayRange(f) {
  const d0 = new Date(); d0.setHours(0, 0, 0, 0); const t0 = d0.getTime();
  if (f.day === 'today') return { from: t0, to: t0 + DAY - 1, label: 'Today (' + fmtD(t0) + ')', ymd: [ymd(t0), ymd(t0)] };
  if (f.day === 'yesterday') return { from: t0 - DAY, to: t0 - 1, label: 'Yesterday (' + fmtD(t0 - DAY) + ')', ymd: [ymd(t0 - DAY), ymd(t0 - DAY)] };
  if (f.day === '7d') return { from: t0 - 6 * DAY, to: t0 + DAY - 1, label: 'Last 7 days', ymd: [ymd(t0 - 6 * DAY), ymd(t0)] };
  if (f.day === 'date' && f.date) { const a = new Date(f.date + 'T00:00:00').getTime(); return { from: a, to: a + DAY - 1, label: fmtD(a), ymd: [f.date, f.date] }; }
  return null;
}
function tasksSubsMap() {
  const m = new Map(S.tasks.map(t => [t.id, []]));
  S.subs.forEach(s => {
    let id = m.has(s.taskId) ? s.taskId : null;
    if (!id) { const t = S.tasks.find(t => taskName(t).trim().toLowerCase() === String(s.taskTitle || '').trim().toLowerCase()); id = t && t.id; }
    if (id) m.get(id).push(s);
  });
  return m;
}
const TASK_PILL = { active: '<span class="pill ok">LIVE</span>', paused: '<span class="pill mute">PAUSED</span>', draft: '<span class="pill info">DRAFT</span>', archived: '<span class="pill mute">ARCHIVED</span>' };
RENDER.tasks = () => {
  const f = S.tasksUI, q = f.q.trim().toLowerCase();
  const sm = tasksSubsMap(), rg = taskDayRange(f);
  setHTML($('#tk-days'), [['today', 'Today'], ['yesterday', 'Yesterday'], ['7d', '7 Days'], ['all', 'All time']].map(([k, l]) => `<button class="tab ${f.day === k ? 'on' : ''}" data-act="tkDay" data-k="${k}">${l}</button>`).join(''));
  $('#tk-date').value = f.day === 'date' ? f.date : ''; $('#tk-datewrap').classList.toggle('on', f.day === 'date');
  $('#tk-dayinfo').textContent = rg ? `Showing only ${rg.label} — submissions, screenshots & proofs` : 'Showing all submissions';
  let list = S.tasks.filter(t => (f.status === 'all' || taskStatus(t) === f.status) && (!q || [taskName(t), t.id, t.appId, t.packageName, t.category].join(' ').toLowerCase().includes(q)));
  if (!S.live.tasks && !S.tasks.length) { setHTML($('#tasks-grid'), skelCards(4)); return; }
  const p = paginate(list, 'tasks', 12, f);
  setHTML($('#tasks-grid'), p.items.map(t => {
    const subs = (sm.get(t.id) || []).filter(x => !rg || (subMs(x) >= rg.from && subMs(x) <= rg.to)), pn = subs.filter(s => subSt(s) === 'pending').length, ap = subs.filter(s => subSt(s) === 'approved').length, rj = subs.filter(s => subSt(s) === 'rejected').length;
    const st = taskStatus(t), liability = pn * taskReward(t);
    return `<div class="card tcard">
      <div class="row" style="align-items:flex-start;gap:12px"><img class="appico" src="${esc(taskIcon(t))}" alt="" loading="lazy">
        <div class="grow" style="min-width:0"><div class="row between" style="gap:6px"><h4 class="b fs16 trunc">${esc(taskName(t))}</h4>${TASK_PILL[st]}</div>
          <div class="row wrap gap6 fs12 mt4 tmeta"><span class="brand-t xb">${inr(taskReward(t))}</span><span class="mchip rate">${ic('star', 12)}${esc(String(t.appRating || t.rating || '—'))}</span><span class="mchip">${ic('download', 12)}${esc(String(t.appDownloads || t.downloads || '—'))}</span><span class="mchip">${ic('clock', 12)}${num(t.durationMinutes) || 15}m</span></div>
          <div class="row wrap gap6 mt8"><span class="tag mono">ID ${esc(t.id.slice(0, 8))}</span><span class="tag tgi">${ic('file', 11)}${(t.comments || []).length} comments</span><span class="tag tgi">${ic('ban', 11)}${(t.blockedUserIds || []).length} blocked</span><span class="tag tgi" title="Users who already submitted — this task is hidden from them">${ic('check', 11)}${(t.submittedUserIds || []).length} submitted</span><span class="tag">${fmtD(tsMs(t.createdAt))}</span></div></div></div>
      <div class="mini" style="grid-template-columns:repeat(4,1fr)"><div><b>${subs.length}</b>Submitted</div><div><b>${subs.filter(x => x.screenshotBase64).length}</b>Screenshots</div><div><b>${pn}</b>Pending</div><div><b>${ap}</b>Approved</div></div>
      <div class="row between wrap fs12"><span class="muted">${rj} rejected</span><span class="b">Pending reward liability: <span class="brand-t">${inr(liability)}</span></span></div>
      <div class="row wrap" style="border-top:1px solid var(--line);padding-top:12px;gap:6px">
        <button class="btn ghost sm" data-act="${st === 'active' ? 'taskPause' : 'taskActivate'}" data-id="${esc(t.id)}">${ic(st === 'active' ? 'pause' : 'play', 14)}${st === 'active' ? 'Pause' : 'Activate'}</button>
        <button class="btn ghost sm" data-act="taskApprovals" data-id="${esc(t.id)}">${ic('approve', 14)}Proofs</button>
        <div class="grow"></div>
        <button class="btn blue sm" data-act="editTask" data-id="${esc(t.id)}">${ic('edit', 14)}Edit</button>
        ${st !== 'archived' ? `<button class="btn ghost sm" data-act="taskArchive" data-id="${esc(t.id)}" title="Archive">${ic('folder', 14)}</button>` : ''}
        <button class="btn bad sm" data-act="taskDelete" data-id="${esc(t.id)}" title="Delete">${ic('trash', 14)}</button>
      </div></div>`;
  }).join('') || `<div class="card" style="grid-column:1/-1">${emptyHTML('tasks', S.tasks.length ? 'No tasks match these filters.' : 'No tasks yet. Click “Add New Task” to publish one.')}</div>`);
  setHTML($('#tasks-pager'), pagerHTML(p));
};
INP.taskQ = el => { S.tasksUI.q = el.value; S.tasksUI.page = 1; renderAll(); };
INP.taskStatus = el => { S.tasksUI.status = el.value; S.tasksUI.page = 1; renderAll(); };
ACT.taskApprovals = el => {
  const t = S.tasks.find(x => x.id === el.dataset.id), rg = taskDayRange(S.tasksUI);
  go('approvals', { range: rg ? 'custom' : 'all', status: 'all', q: t ? taskName(t) : '' });
  if (rg) { S.appr.from = rg.ymd[0]; S.appr.to = rg.ymd[1]; $('#ap-from').value = rg.ymd[0]; $('#ap-to').value = rg.ymd[1]; renderAll(); }
};
ACT.tkDay = el => { S.tasksUI.day = el.dataset.k; S.tasksUI.date = ''; S.tasksUI.page = 1; renderAll(); };
INP.tkDate = el => { S.tasksUI.date = el.value; S.tasksUI.day = el.value ? 'date' : 'today'; S.tasksUI.page = 1; renderAll(); };
async function setTaskStatus(id, status) {
  const active = status === 'active';
  const patch = { active, isActive: active, status, updatedAt: serverTimestamp(), updatedBy: S.admin };
  await updateDoc(doc(db, 'tasks', id), patch);
  try { await updateDoc(doc(db, 'offers', id), patch); } catch (_) { }
  logAction('task_status', id, { status });
}
ACT.taskPause = async el => { await setTaskStatus(el.dataset.id, 'paused'); toast('Task paused'); };
ACT.taskActivate = async el => { await setTaskStatus(el.dataset.id, 'active'); toast('Task is live', 'ok'); };
ACT.taskArchive = async el => {
  const t = S.tasks.find(x => x.id === el.dataset.id); if (!t) return;
  if (!await confirmBox('Archive this task?', `<b>${esc(taskName(t))}</b> will be removed from the user app, but its data stays safe.`, { ok: 'Archive' })) return;
  await setTaskStatus(t.id, 'archived'); toast('Task archived');
};
ACT.taskDelete = async el => {
  const t = S.tasks.find(x => x.id === el.dataset.id); if (!t) return;
  if (!await confirmBox('Delete task permanently?', `<b>${esc(taskName(t))}</b> and its whole comment/feedback pool will be deleted. This cannot be undone. Archiving may be a better option.`, { ok: 'Delete task', danger: true })) return;
  await deleteDoc(doc(db, 'tasks', t.id));
  try { await deleteDoc(doc(db, 'offers', t.id)); } catch (_) { }
  logAction('task_delete', t.id, { name: taskName(t) });
  toast('Task deleted');
};

/* ---------- task editor (separate full-screen form) ---------- */
const buildCommentItems = (taskId, title, appId, texts, existing = []) => {
  const byText = new Map((existing || []).map(i => [i.commentText, i])); const now = Date.now();
  return texts.map((txt, i) => byText.get(txt) || ({
    id: `cmt_${taskId.slice(0, 6)}_${now.toString(36)}_${i}`, appTitle: title, appId, taskId, commentText: txt, language: 'English', status: 'available',
    assignedUser: '', claimedAtMillis: 0, claimExpiresAtMillis: 0, submittedAtMillis: 0, updatedAtMillis: now
  }));
};
const lines = v => v.split('\n').map(x => x.trim()).filter(Boolean);
const pkgOf = url => { const t = String(url || '').trim(); let m = t.match(/[?&]id=([a-zA-Z0-9._]+)/); if (m) return m[1]; m = t.match(/^[a-zA-Z][a-zA-Z0-9_]*(?:\.[a-zA-Z0-9_]+)+$/); return m ? m[0] : ''; };
function openTaskModal(t) {
  $('#tm-id').value = t ? t.id : '';
  $('#tm-heading').textContent = t ? 'Edit Task' : 'Add New Task';
  $('#tm-url').value = t ? (t.storeUrl || t.actionUrl || t.appLink || '') : '';
  $('#tm-title').value = t ? taskName(t) : '';
  $('#tm-rating').value = t ? (t.appRating || t.rating || '') : '';
  $('#tm-downloads').value = t ? (t.appDownloads || t.downloads || '') : '';
  const icon = t ? (t.iconUrl || t.appIcon || t.taskPfpBase64 || '') : '';
  $('#tm-icon-url').value = icon; $('#tm-icon-preview').src = icon || PLAY_ICON;
  $('#tm-reward').value = t ? (taskReward(t) || 10) : 10;
  $('#tm-duration').value = t ? (num(t.durationMinutes) || 15) : 15;
  $('#tm-once').value = t && t.hideAfterComplete === false ? 'keep' : 'hide';
  $('#tm-category').value = t ? (t.category || 'App') : 'App';
  $('#tm-status').value = t ? taskStatus(t) : 'active';
  $('#tm-instructions').value = (t && (t.instructions || []).length ? t.instructions : DEFAULT_INSTRUCTIONS).join('\n');
  $('#tm-comments').value = t ? (t.comments || []).join('\n') : '';
  $('#tm-blocked').value = t ? (t.blockedUserIds || []).join(', ') : '';
  $('#tm-error').textContent = ''; $('#tm-fetch-status').textContent = 'Paste the link — app name, icon, rating and downloads are filled in automatically. If it fails, enter them manually or upload an icon.';
  $('#task-modal').classList.add('show');
  $('#tm-users-card').classList.toggle('hidden', !t);
  refreshCompleted(); updateTaskPreview();
}
function closeTask() { $('#task-modal').classList.remove('show'); }
ACT.openTask = () => openTaskModal(null);
ACT.editTask = el => openTaskModal(S.tasks.find(x => x.id === el.dataset.id));
ACT.closeTask = closeTask;
function updateTaskPreview() {
  const title = $('#tm-title').value.trim() || 'App name', reward = num($('#tm-reward').value), dur = num($('#tm-duration').value) || 15;
  const steps = lines($('#tm-instructions').value).slice(0, 3), st = $('#tm-status').value;
  setHTML($('#tm-preview'), `<div class="fs11 muted" style="text-align:center;margin-bottom:8px">Task card${st !== 'active' ? ` <span class="pill mute">${st.toUpperCase()}</span>` : ''}</div>
    <div class="card"><div class="row"><img class="appico" src="${esc($('#tm-icon-url').value || PLAY_ICON)}" alt=""><div class="grow"><div class="b fs13 trunc">${esc(title)}</div><div class="fs11 muted">${esc($('#tm-category').value || 'App')} • ${dur} min</div></div><span class="pill ok">${inr(reward)}</span></div>
    <ol style="margin:10px 0 0;padding-left:18px;font-size:11.5px;color:var(--ink2);line-height:1.5">${steps.map(s => `<li>${esc(s)}</li>`).join('')}</ol>
    <div class="btn primary" style="width:100%;margin-top:10px;pointer-events:none;font-size:12px">Start task</div></div>`);
}
let fetchTimer = 0;
INP.tmPrev = (el) => { updateTaskPreview(); if (el.id === 'tm-title') refreshCompleted(); };
INP.tmUrl = el => { clearTimeout(fetchTimer); if (pkgOf(el.value)) fetchTimer = setTimeout(fetchPlayMeta, 500); };
INP.tmIconFile = el => {
  const f = el.files && el.files[0]; if (!f) return; const img = new Image(), url = URL.createObjectURL(f);
  img.onload = () => { const c = document.createElement('canvas'); c.width = c.height = 128; c.getContext('2d').drawImage(img, 0, 0, 128, 128); const d = c.toDataURL('image/png'); $('#tm-icon-url').value = d; $('#tm-icon-preview').src = d; URL.revokeObjectURL(url); updateTaskPreview(); toast('Icon added', 'ok'); };
  img.onerror = () => toast('Could not read that image', 'bad'); img.src = url; el.value = '';
};
INP.tmBlocked = () => refreshCompleted();
ACT.tmFetch = () => fetchPlayMeta();
ACT.tmToggleUsers = () => { S.tm.showCompleted = !S.tm.showCompleted; refreshCompleted(); };
function refreshCompleted() {
  const box = $('#tm-completed'), title = $('#tm-title').value.trim().toLowerCase();
  if (!S.tm.showCompleted) { box.innerHTML = '<div class="fs12 muted">Hidden.</div>'; return; }
  const users = {};
  S.subs.filter(s => title ? String(s.taskTitle || '').trim().toLowerCase() === title : subSt(s) === 'approved').forEach(s => { if (s.userId) users[s.userId] = s.userName || s.userEmail || s.userId; });
  const entries = Object.entries(users);
  if (!entries.length) { box.innerHTML = `<div class="fs12 muted">${title ? 'No user has completed this app yet.' : 'No data.'}</div>`; return; }
  const blocked = $('#tm-blocked').value.split(',').map(x => x.trim()).filter(Boolean);
  box.innerHTML = entries.slice(0, 40).map(([uid, name]) => `<label class="row" style="padding:7px 10px;border:1px solid var(--line);border-radius:10px;background:var(--panel);cursor:pointer"><input type="checkbox" class="chk" data-in="tmBlockUid" data-uid="${esc(uid)}" ${blocked.includes(uid) ? 'checked' : ''}><span class="grow fs12"><b>${esc(name)}</b> <span class="mono muted">${esc(uid)}</span></span></label>`).join('');
}
INP.tmBlockUid = el => {
  const input = $('#tm-blocked'), set = new Set(input.value.split(',').map(x => x.trim()).filter(Boolean));
  el.checked ? set.add(el.dataset.uid) : set.delete(el.dataset.uid); input.value = [...set].join(', ');
};
ACT.tmSave = async () => {
  const err = $('#tm-error'); err.textContent = '';
  const existingId = $('#tm-id').value.trim(), existing = S.tasks.find(t => t.id === existingId);
  const url = $('#tm-url').value.trim();
  if (url && !/^https?:\/\/play\.google\.com\/store\/apps\/details\?(?:[^#]*&)?id=[a-zA-Z0-9._]+/i.test(url)) { err.textContent = 'Invalid link: enter a Google Play Store app link (play.google.com/store/apps/details?id=…).'; return; }
  let title = $('#tm-title').value.trim();
  if (!title && url) title = pkgOf(url).split('.').pop() || '';
  if (!title) { err.textContent = 'An app name or a valid Play Store link is required.'; return; }
  const reward = Math.max(1, parseInt($('#tm-reward').value || '0', 10) || 0), duration = Math.max(1, parseInt($('#tm-duration').value || '0', 10) || 15);
  if (!reward) { err.textContent = 'Reward must be ₹1 or more.'; return; }
  const onceMode = $('#tm-once').value, hideAfterComplete = onceMode !== 'keep', reshow = onceMode === 'reshow' && !!existingId, status = $('#tm-status').value, active = status === 'active';
  const appId = pkgOf(url) || title.toLowerCase().replace(/\W+/g, '_');
  if (!existingId && S.tasks.some(t => (t.appId || t.packageName) === appId && taskStatus(t) !== 'archived')) {
    if (!await confirmBox('Duplicate task?', 'A task for this app already exists. Create a new one anyway?', { ok: 'Create anyway' })) return;
  }
  const instructions = lines($('#tm-instructions').value), texts = lines($('#tm-comments').value);
  const blockedUserIds = $('#tm-blocked').value.split(',').map(x => x.trim()).filter(Boolean);
  const ratingStr = $('#tm-rating').value.trim(), downloadsStr = $('#tm-downloads').value.trim(), iconUrl = $('#tm-icon-url').value.trim(), category = $('#tm-category').value.trim() || 'App';
  const docRef = existingId ? doc(db, 'tasks', existingId) : doc(collection(db, 'tasks'));
  const ins = instructions.length ? instructions : DEFAULT_INSTRUCTIONS;
  const payload = {
    id: docRef.id, taskId: docRef.id, title, appName: title, subtitle: url || `Earn Reward • ${title}`, storeUrl: url, actionUrl: url, appLink: url, appId, packageName: appId,
    appRating: ratingStr, rating: parseFloat(ratingStr) || 4.5, appDownloads: downloadsStr, downloads: downloadsStr, installs: downloadsStr, blockedUserIds,
    iconUrl, appIcon: iconUrl, taskPfpBase64: iconUrl, referenceScreenshotUrl: existing?.referenceScreenshotUrl || '', exampleProofBase64: existing?.exampleProofBase64 || '',
    category, rewardInr: reward, rewardAmount: reward, reward, durationMinutes: duration, timerMinutes: duration, deadlineHours: existing?.deadlineHours || 24, maxSubmissions: 0, hideAfterComplete, oneTimePerUser: hideAfterComplete, ...(reshow ? { reshowAt: Date.now(), submittedUserIds: [] } : {}),
    instructions: ins, description: ins.join('\n'), proofRequirement: existing?.proofRequirement || 'Upload completion screenshot and your genuine feedback',
    active, isActive: active, pausedByMaintenance: false, status,
    comments: texts, commentPool: texts, commentItems: buildCommentItems(docRef.id, title, appId, texts, existing?.commentItems), updatedAt: serverTimestamp(), updatedBy: S.admin
  };
  if (!existingId) { payload.createdAt = serverTimestamp(); payload.createdBy = S.admin; }
  const btn = $('#tm-save'); btn.disabled = true;
  try {
    await setDoc(docRef, payload, { merge: true });
    try { await setDoc(doc(db, 'offers', docRef.id), payload, { merge: true }); } catch (_) { }
    logAction(existingId ? 'task_edit' : 'task_create', docRef.id, { title, reward, status });
    closeTask(); toast(`Task “${title}” saved (${inr(reward)}, ${status === 'active' ? 'LIVE' : status})`, 'ok');
  } catch (e) { err.textContent = 'Save failed: ' + (e.code || e.message); }
  btn.disabled = false;
};


/* === js/services/play-store.js === */
'use strict';
/* ============================================================
   PLAY STORE FETCH  (name, icon, rating, downloads from a Play Store link)
   Set PLAY_PROXY to your own Cloudflare Worker (tools/play-proxy-worker.js) for a stable fetch.
   ============================================================ */
/* Optional but the most reliable option: deploy play-proxy-worker.js on Cloudflare Workers (free) and paste its URL here, e.g. 'https://play-proxy.yourname.workers.dev' */
const PLAY_PROXY = '';
async function fetchHtml(target, pkg) {
  const enc = encodeURIComponent, titleOf = h => { const m = h.match(/<title[^>]*>([^<]*)/i) || h.match(/^Title:\s*(.+)$/im); return m ? m[1] : ''; };
  const valid = h => typeof h === 'string' && h.length > 1200 && !/just a moment|attention required|access denied|captcha|before you continue|error 4\d\d|403 forbidden|too many requests/i.test(titleOf(h)) && (h.includes(pkg) || /og:title/i.test(h)) && /google play|og:title/i.test(h);
  const get = async (url, kind, ms) => {
    const c = new AbortController(), t = setTimeout(() => c.abort(), ms);
    try {
      const r = await fetch(url, { signal: c.signal }); if (!r.ok) throw new Error('http ' + r.status);
      const h = kind === 'json' ? ((await r.json()).contents || '') : await r.text();
      if (!valid(h)) throw new Error('bad page'); return h;
    } finally { clearTimeout(t); }
  };
  if (PLAY_PROXY) { try { return await get(PLAY_PROXY + (PLAY_PROXY.includes('?') ? '&' : '?') + 'url=' + enc(target), 'text', 12000); } catch (_) { } }
  const list = [
    [u => 'https://corsproxy.io/?url=' + enc(u)],
    [u => 'https://api.allorigins.win/get?url=' + enc(u), 'json'],
    [u => 'https://api.codetabs.com/v1/proxy/?quest=' + enc(u)],
    [u => 'https://api.allorigins.win/raw?url=' + enc(u)],
    [u => 'https://thingproxy.freeboard.io/fetch/' + u],
    [u => 'https://r.jina.ai/' + u]   // reader service: CORS-enabled, returns page text (parsed by parsePlay too)
  ];
  for (const ms of [10000, 18000]) {   // two rounds: all proxies in parallel, first valid page wins
    try { return await Promise.any(list.map(([f, k]) => get(f(target), k, ms))); } catch (_) { }
  }
  throw new Error('proxy');
}
function parsePlay(html, pkg) {
  const meta = k => { const m = html.match(new RegExp('<meta[^>]+(?:property|name|itemprop)="' + k + '"[^>]+content="([^"]*)"', 'i')) || html.match(new RegExp('<meta[^>]+content="([^"]*)"[^>]+(?:property|name|itemprop)="' + k + '"', 'i')); return m ? m[1] : ''; };
  const dec = x => { const d = document.createElement('textarea'); d.innerHTML = x; return d.value; };
  const clean = x => dec(x).replace(/\s*[-–]\s*Apps on Google Play.*$/i, '').replace(/\s*-\s*Google Play.*$/i, '').trim();
  const compact = n => { n = +n; return n >= 1e9 ? (n / 1e9) + 'B+' : n >= 1e6 ? (n / 1e6) + 'M+' : n >= 1e3 ? (n / 1e3) + 'K+' : n + '+'; };
  let ld = {}; try { const m = html.match(/<script[^>]+application\/ld\+json[^>]*>([\s\S]*?)<\/script>/i); if (m) ld = JSON.parse(m[1]); } catch (_) { }
  let appName = clean(meta('og:title') || meta('name') || ld.name || ''); if (!appName) { const m = html.match(/<title[^>]*>([^<]+)<\/title>/i) || html.match(/^Title:\s*(.+)$/im); appName = m ? clean(m[1]) : ''; }
  let img = meta('og:image') || ld.image || ''; if (typeof img !== 'string') img = (img && img.url) || '';
  if (!img) { const m = html.match(/https:\/\/play-lh\.googleusercontent\.com\/[A-Za-z0-9_\-]+/); img = m ? m[0] : ''; }
  let rating = ld.aggregateRating && ld.aggregateRating.ratingValue ? String(ld.aggregateRating.ratingValue) : '';
  if (!rating) { const rm = html.match(/Rated ([0-9.]+) stars/i) || html.match(/"ratingValue"\s*:\s*"?([0-9.]+)/i) || html.match(/itemprop="ratingValue"[^>]*content="([0-9.]+)"/i) || html.match(/>\s*([0-5]\.[0-9])\s*(?:<[^>]*>\s*)*star/i) || html.match(/\b([0-5]\.[0-9])\s*star/i); rating = rm ? rm[1] : ''; }
  let downloads = ''; const dm = html.match(/>\s*([0-9][0-9.,]*\s?[KMBkmb]\+?)\s*<\/div>\s*<div[^>]*>\s*Downloads/i) || html.match(/([0-9][0-9.,]*\s?[KMB]\+)\s*(?:<[^>]*>\s*)*Downloads/i);
  if (dm) downloads = dm[1].replace(/\s/g, '').toUpperCase(); else { const n = html.match(/"([0-9]{1,3}(?:,[0-9]{3})+)\+"/) || html.match(/"(?:numDownloads|installs)"\s*:\s*"?([0-9,]+)/i); if (n) downloads = compact(n[1].replace(/,/g, '')); else { const k = html.match(/"([0-9][0-9.]*[KMB]\+)"/); if (k) downloads = k[1]; } }
  const cat = ld.applicationCategory ? String(ld.applicationCategory).replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase()) : '';
  const nice = pkg.split('.').pop().replace(/[_-]+/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
  return { appName: appName || nice, iconUrl: img ? img.split('=')[0] + '=s256-rw' : '', rating: rating ? parseFloat(rating).toFixed(1) : '', downloads, category: cat };
}
let metaTok = 0;
async function fetchPlayMeta() {
  const raw = $('#tm-url').value.trim(), st = $('#tm-fetch-status'); if (!raw) return;
  const pkg = pkgOf(raw), tok = ++metaTok;
  if (!pkg) { st.textContent = 'This does not look like a valid Play Store link — enter the details manually.'; return; }
  const link = `https://play.google.com/store/apps/details?id=${pkg}`;
  if (!/play\.google\.com\/store\/apps\/details/.test(raw)) $('#tm-url').value = link;
  st.textContent = '⚡ Fetching name, icon, rating & downloads…';
  try {
    const html = await fetchHtml(link + '&hl=en&gl=IN', pkg);
    if (tok !== metaTok) return;
    const { appName, iconUrl, rating, downloads, category } = parsePlay(html, pkg);
    if (category && !$('#tm-category').value.trim()) $('#tm-category').value = category;
    if (appName) $('#tm-title').value = appName;
    $('#tm-rating').value = rating || $('#tm-rating').value; $('#tm-downloads').value = downloads || $('#tm-downloads').value;
    if (iconUrl) { $('#tm-icon-url').value = iconUrl; $('#tm-icon-preview').src = iconUrl; }
    const miss = [!iconUrl && 'icon', !rating && 'rating', !downloads && 'downloads'].filter(Boolean);
    st.textContent = `✓ Fetched: ${appName} (${rating || '—'}★ • ${downloads || '—'})` + (miss.length ? ` — ${miss.join(', ')} not found, please fill manually.` : '');
    refreshCompleted(); updateTaskPreview();
  } catch (e) {
    if (tok !== metaTok) return;
    if (!$('#tm-title').value) $('#tm-title').value = pkg.split('.').pop().replace(/[_-]+/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
    st.textContent = 'Could not reach the Play Store through the public proxies. Tap Fetch to retry, or enter the details manually. (For a permanent fix, deploy play-proxy-worker.js and set PLAY_PROXY.)'; updateTaskPreview();
  }
}


/* === js/views/comments.js === */
'use strict';
/* ============================================================
   COMMENTS / FEEDBACK
   ============================================================ */
const expOf = (it, t) => num(it.claimExpiresAtMillis) || (num(it.claimedAtMillis) ? num(it.claimedAtMillis) + (num(t.durationMinutes) || 15) * 60000 : 0);
const isExpiredClaim = (it, t, now = Date.now()) => String(it.status || '').toLowerCase() === 'claimed' && expOf(it, t) > 0 && expOf(it, t) < now;
const userLabel = uid => { const u = S.users.find(x => x.id === uid || x.userId === uid); return u ? (u.fullName || u.email || uid) : (uid || 'a user'); };
function allComments() {
  const out = [], now = Date.now(), idx = new Map();
  S.subs.forEach(x => { const t = x.taskId || ''; if (x.commentId) idx.set(t + '|id:' + x.commentId, x); if (x.proofText) idx.set(t + '|' + String(x.proofText).trim(), x); });
  S.tasks.forEach(t => {
    const items = Array.isArray(t.commentItems) && t.commentItems.length ? t.commentItems : (t.comments || []).map((c, i) => ({ id: `${t.id.slice(0, 6)}_c${i}`, commentText: c, status: 'available', assignedUser: '' }));
    items.forEach((it, i) => {
      const text = it.commentText || it.text || '', id = it.id || `${t.id.slice(0, 6)}_c${i}`;
      let st = String(it.status || 'available').toLowerCase(), user = it.assignedUser || '', remark = '', when = num(it.submittedAtMillis);
      const sub = idx.get(t.id + '|id:' + id) || idx.get(t.id + '|' + String(text).trim());
      if (sub && st !== 'submitted') { st = 'submitted'; user = sub.userId || user; when = subMs(sub); }
      if (st === 'claimed' && isExpiredClaim(it, t, now)) { st = 'available'; user = ''; remark = 'Returned to the pool — the user did not finish in time'; }
      else if (st === 'claimed') { const e = expOf(it, t); remark = 'Assigned to ' + userLabel(user) + (e ? ' · due ' + fmtDT(e) : ''); }
      else if (st === 'submitted') remark = 'Completed · used by ' + userLabel(user) + (when ? ' · ' + fmtDT(when) : '');
      out.push({ key: `${t.id}||${it.id || i}`, id, text, status: st, user, remark, task: t, item: it });
    });
  });
  return out;
}
async function sweepExpired() {
  if (!S.admin) return; const now = Date.now();
  for (const t of S.tasks) {
    if (!Array.isArray(t.commentItems) || !t.commentItems.some(i => isExpiredClaim(i, t, now))) continue;
    if ((sweepExpired.g[t.id] || 0) > now - 300000) continue; sweepExpired.g[t.id] = now;
    try {
      let n = 0, fixedItems = null;
      await runTransaction(db, async tx => {
        const ref = doc(db, 'tasks', t.id), snap = await tx.get(ref); if (!snap.exists()) return;
        const d = snap.data(), items = Array.isArray(d.commentItems) ? d.commentItems : []; n = 0;
        fixedItems = items.map(i => isExpiredClaim(i, d, Date.now()) ? (n++, { ...i, status: 'available', assignedUser: '', claimedAtMillis: 0, claimExpiresAtMillis: 0, remark: 'Returned to the pool — time expired', updatedAtMillis: Date.now() }) : i);
        if (n) tx.update(ref, { commentItems: fixedItems, updatedAt: serverTimestamp() });
      });
      if (n) { try { await setDoc(doc(db, 'offers', t.id), { commentItems: fixedItems }, { merge: true }); } catch (_) { } logAction('comments_auto_release', t.id, { count: n }); }
    } catch (_) { }
  }
}
sweepExpired.g = {};
async function savePool(task, items) {
  const texts = items.map(i => i.commentText);
  const patch = { comments: texts, commentPool: texts, commentItems: items, updatedAt: serverTimestamp(), updatedBy: S.admin };
  await updateDoc(doc(db, 'tasks', task.id), patch);
  try { await setDoc(doc(db, 'offers', task.id), patch, { merge: true }); } catch (_) { }
}
const taskOptions = (withAll, sel) => (withAll ? '<option value="all">All tasks</option>' : '') + S.tasks.map(t => `<option value="${esc(t.id)}" ${sel === t.id ? 'selected' : ''}>${esc(taskName(t))}</option>`).join('');
function syncSelect(el, html, keep) { if (el._h !== html) { const cur = keep ?? el.value; el.innerHTML = html; el._h = html; if (cur) el.value = cur; if (el.value !== cur && el.options.length) el.selectedIndex = Math.max(0, [...el.options].findIndex(o => o.value === cur)); } }
RENDER.comments = () => {
  const bank = S.cm.tab === 'bank', fol = bank && S.cm.folder && S.tasks.find(t => t.id === S.cm.folder);
  if (bank && S.cm.folder && !fol) S.cm.folder = null;
  $('#cm-folders').classList.toggle('hidden', !(bank && !fol)); $('#cm-bank').classList.toggle('hidden', !fol); $('#cm-block').classList.toggle('hidden', bank);
  if (fol) { S.cm.task = fol.id; renderFolderHead(fol); }
  $$('#v-comments > .tabs .tab').forEach(b => b.classList.toggle('on', b.dataset.tab === S.cm.tab));
  syncSelect($('#cm-add-task'), taskOptions(false));
  syncSelect($('#cm-task-filter'), taskOptions(true), S.cm.task);
  syncSelect($('#bl-task'), taskOptions(true), S.cm.blTask || 'all');
  if (!S.cm.blTask) S.cm.blTask = 'all';
  { const bt = $('#bl-task'); if (bt.value !== S.cm.blTask) bt.value = S.cm.blTask; }
  if (bank && fol) {
    const f = S.cm, q = f.q.trim().toLowerCase();
    const list = allComments().filter(c => (f.task === 'all' || c.task.id === f.task) && (f.status === 'all' || c.status === f.status) && (!q || [c.id, c.text, taskName(c.task), c.status].join(' ').toLowerCase().includes(q)));
    const p = paginate(list, 'cm', 20, f);
    setHTML($('#cm-list'), p.items.map(c => `<div class="cmi ${c.status}"><input type="checkbox" class="chk" data-act="cmSel" data-key="${esc(c.key)}" ${f.sel.has(c.key) ? 'checked' : ''} aria-label="Select"><div class="grow" style="min-width:0"><div class="fs13" style="line-height:1.45">${esc(c.text)}</div><div class="row wrap gap6 mt4"><span class="mono fs11 muted">${esc(String(c.id).slice(0, 14))}</span>${c.remark ? `<span class="remark">${ic(c.status === 'submitted' ? 'check' : c.status === 'claimed' ? 'clock' : 'refresh', 12)}${esc(c.remark)}</span>` : ''}</div></div>${c.status === 'available' ? '<span class="pill ok">Available</span>' : c.status === 'claimed' ? '<span class="pill warn">Assigned</span>' : '<span class="pill info">Completed</span>'}<button class="btn bad xs" data-act="cmDel" data-key="${esc(c.key)}" aria-label="Delete">${ic('trash', 13)}</button></div>`).join('') || emptyHTML('message', 'No feedback entries match.'));
    setHTML($('#cm-pager'), pagerHTML(p));
    $('#cm-del-sel').classList.toggle('hidden', !f.sel.size);
    $('#cm-del-sel').lastChild.textContent = `Delete selected (${f.sel.size})`;
  } else if (bank) renderFolders(); else renderBlocked();
};
const cmStats = (t, all, sm) => { const cs = all.filter(c => c.task.id === t.id), n = k => cs.filter(c => c.status === k).length; return { total: cs.length, av: n('available'), as: n('claimed'), done: n('submitted'), shots: (sm.get(t.id) || []).filter(x => x.screenshotBase64).length, bl: (t.blockedUserIds || []).length }; };
function renderFolders() {
  $('#cm-add-task').parentElement.classList.remove('hidden'); $('#cm-task-filter').classList.remove('hidden');
  const all = allComments(), sm = tasksSubsMap();
  const cards = S.tasks.filter(t => taskStatus(t) !== 'archived' || all.some(c => c.task.id === t.id)).map(t => {
    const k = cmStats(t, all, sm);
    return `<button class="folder" data-act="cmFolder" data-id="${esc(t.id)}"><div class="row"><img class="appico" style="width:46px;height:46px;border-radius:13px" src="${esc(taskIcon(t))}" alt=""><div class="grow" style="min-width:0;text-align:left"><div class="b fs13 trunc">${esc(taskName(t))}</div><div class="fs12 muted">${ic('folder', 12)} ${k.total} comment${k.total === 1 ? '' : 's'} · ${k.done} used</div></div>${ic('chev-right', 16)}</div><div class="row wrap gap6"><span class="pill ok">${k.av} available</span><span class="pill warn">${k.as} assigned</span><span class="pill info">${k.done} completed</span><span class="pill purple">${ic('image', 11)}${k.shots} screenshots</span>${k.bl ? `<span class="pill bad">${k.bl} blocked</span>` : ''}</div></button>`;
  }).join('');
  setHTML($('#cm-folders'), cards ? `<div class="g2 auto">${cards}</div>` : `<div class="card">${emptyHTML('folder', 'Create a task in Task Management — each task gets its own comment folder here.')}</div>`);
}
function renderFolderHead(t) {
  const k = cmStats(t, allComments(), tasksSubsMap()), st = (l, v, c) => `<div class="kpi s ${c}" style="padding:10px 12px"><div class="grow"><div class="lbl">${l}</div><div class="val" style="font-size:20px">${v}</div></div></div>`;
  setHTML($('#cm-fhead'), `<div class="card"><div class="row wrap"><button class="btn ghost sm" data-act="cmBack">${ic('chev-left', 15)}All folders</button><img class="appico" style="width:44px;height:44px;border-radius:12px" src="${esc(taskIcon(t))}" alt=""><div class="grow" style="min-width:140px"><div class="b fs16 trunc">${esc(taskName(t))}</div><div class="fs12 muted">Manage this app’s comments and blocked users here</div></div><button class="btn bad sm" data-act="cmManageBlocks" data-id="${esc(t.id)}">${ic('ban', 14)}${k.bl} blocked</button></div><div class="g4 mt12">${st('Total comments', k.total, 'blue') + st('Available', k.av, 'green') + st('Assigned', k.as, 'orange') + st('Completed / used', k.done, 'purple')}</div><div class="fs12 muted mt8">${ic('image', 13)} ${k.shots} screenshot${k.shots === 1 ? '' : 's'} submitted for this app</div></div>`);
  $('#cm-add-task').parentElement.classList.add('hidden'); $('#cm-task-filter').classList.add('hidden');
}
ACT.cmFolder = el => { S.cm.folder = el.dataset.id; S.cm.task = el.dataset.id; S.cm.page = 1; S.cm.sel.clear(); renderAll(); window.scrollTo({ top: 0 }); };
ACT.cmBack = () => { S.cm.folder = null; S.cm.task = 'all'; S.cm.sel.clear(); renderAll(); };
ACT.cmManageBlocks = el => { S.cm.tab = 'block'; S.cm.blTask = el.dataset.id; renderAll(); };
INP.cmQ = el => { S.cm.q = el.value; S.cm.page = 1; renderAll(); };
INP.cmTask = el => { S.cm.task = el.value; S.cm.page = 1; renderAll(); };
INP.cmStatus = el => { S.cm.status = el.value; S.cm.page = 1; renderAll(); };
INP.blTask = el => { S.cm.blTask = el.value; S.cm.blSel = new Set(); renderAll(); };
ACT.cmTab = el => { S.cm.tab = el.dataset.tab; renderAll(); };
ACT.cmSel = el => { el.checked ? S.cm.sel.add(el.dataset.key) : S.cm.sel.delete(el.dataset.key); renderAll(); };
ACT.cmSelAll = el => { const f = S.cm, q = f.q.trim().toLowerCase(); allComments().filter(c => (f.task === 'all' || c.task.id === f.task) && (f.status === 'all' || c.status === f.status) && (!q || [c.id, c.text, taskName(c.task), c.status].join(' ').toLowerCase().includes(q))).forEach(c => el.checked ? f.sel.add(c.key) : f.sel.delete(c.key)); renderAll(); };
ACT.cmAdd = async () => {
  const tid = S.cm.folder || $('#cm-add-task').value, task = S.tasks.find(t => t.id === tid), texts = lines($('#cm-add-text').value);
  if (!task) return toast('Select a task first', 'bad'); if (!texts.length) return toast('Write at least one entry', 'bad');
  const cur = Array.isArray(task.commentItems) && task.commentItems.length ? task.commentItems : buildCommentItems(task.id, taskName(task), task.appId || '', task.comments || []);
  const have = new Set(cur.map(i => i.commentText)), fresh = [...new Set(texts)].filter(t => !have.has(t));
  if (!fresh.length) return toast('These entries already exist', 'bad');
  await savePool(task, [...cur, ...buildCommentItems(task.id, taskName(task), task.appId || '', fresh)]);
  logAction('comments_add', task.id, { count: fresh.length });
  $('#cm-add-text').value = ''; toast(`${fresh.length} entr${fresh.length > 1 ? 'ies' : 'y'} added`, 'ok');
};
async function deleteComments(keys) {
  const byTask = {};
  keys.forEach(k => { const [tid, cid] = k.split('||'); (byTask[tid] = byTask[tid] || new Set()).add(cid); });
  for (const [tid, ids] of Object.entries(byTask)) {
    const task = S.tasks.find(t => t.id === tid); if (!task) continue;
    const cur = allComments().filter(c => c.task.id === tid).map(c => c.item);
    const keep = Array.isArray(task.commentItems) && task.commentItems.length
      ? task.commentItems.filter((it, i) => !ids.has(String(it.id || i)))
      : buildCommentItems(tid, taskName(task), task.appId || '', (task.comments || []).filter((c, i) => !ids.has(`${tid.slice(0, 6)}_c${i}`)));
    void cur; await savePool(task, keep);
  }
  logAction('comments_delete', '', { count: keys.length });
}
ACT.cmDel = async el => {
  if (!await confirmBox('Delete this entry?', 'This feedback entry will be removed from the pool.', { ok: 'Delete', danger: true })) return;
  await deleteComments([el.dataset.key]); S.cm.sel.delete(el.dataset.key); toast('Entry deleted');
};
ACT.cmDelSel = async () => {
  const keys = [...S.cm.sel]; if (!keys.length) return;
  if (!await confirmBox(`Delete ${keys.length} entr${keys.length > 1 ? 'ies' : 'y'}?`, 'The selected entries will be permanently deleted.', { ok: `Delete ${keys.length}`, danger: true })) return;
  await deleteComments(keys); S.cm.sel.clear(); toast('Deleted ' + keys.length);
};

/* ---------- user restrictions ---------- */
const blockedTargets = () => S.cm.blTask === 'all' ? S.tasks.filter(t => taskStatus(t) !== 'archived') : S.tasks.filter(t => t.id === S.cm.blTask);
function renderBlocked() {
  S.cm.blSel = S.cm.blSel || new Set();
  const withB = S.tasks.filter(t => (t.blockedUserIds || []).length), total = withB.reduce((n, t) => n + t.blockedUserIds.length, 0);
  $('#bl-count').textContent = `${total} block${total === 1 ? '' : 's'} in ${withB.length} folder${withB.length === 1 ? '' : 's'}`;
  if (!withB.length) { setHTML($('#bl-list'), emptyHTML('ban', 'No users are blocked in any task yet.')); return; }
  setHTML($('#bl-list'), `<div class="row wrap" style="margin-bottom:10px"><button class="btn ok sm" data-act="blUnblockSel">${ic('check', 14)}Unblock selected (${S.cm.blSel.size})</button></div>` + withB.map(t => `<div class="grp" style="margin-bottom:10px;box-shadow:none"><div class="grp-h"><img class="appico" style="width:34px;height:34px" src="${esc(taskIcon(t))}" alt=""><div class="grow b fs13 trunc">${ic('folder', 15)} ${esc(taskName(t))}</div><span class="pill bad">${t.blockedUserIds.length} blocked</span></div><div style="padding:2px 16px 6px">${t.blockedUserIds.map(uid => { const u = S.users.find(x => x.id === uid || x.userId === uid), key = t.id + '||' + uid; return `<div class="lrow"><input type="checkbox" class="chk" data-act="blSel" data-key="${esc(key)}" ${S.cm.blSel.has(key) ? 'checked' : ''}><div class="grow"><div class="b fs13">${esc(u ? (u.fullName || u.email || 'User') : 'Unknown user')}</div><div class="mono fs12 muted">${esc(uid)}</div></div><button class="btn ok xs" data-act="blOne" data-key="${esc(key)}">Unblock</button></div>`; }).join('')}</div></div>`).join(''));
}
async function writeBlocked(task, ids) {
  const patch = { blockedUserIds: ids, updatedAt: serverTimestamp(), updatedBy: S.admin };
  await updateDoc(doc(db, 'tasks', task.id), patch); try { await updateDoc(doc(db, 'offers', task.id), patch); } catch (_) { }
}
ACT.blSel = el => { el.checked ? S.cm.blSel.add(el.dataset.key) : S.cm.blSel.delete(el.dataset.key); renderAll(); };
async function unblockKeys(keys) {
  const by = {}; keys.forEach(k => { const [tid, uid] = k.split('||'); (by[tid] = by[tid] || []).push(uid); });
  for (const [tid, ids] of Object.entries(by)) { const t = S.tasks.find(x => x.id === tid); if (t) { await writeBlocked(t, (t.blockedUserIds || []).filter(x => !ids.includes(x))); logAction('user_unblock_task', tid, { users: ids }); } }
}
ACT.blOne = async el => { await unblockKeys([el.dataset.key]); toast('User unblocked', 'ok'); };
ACT.blUnblockSel = async () => {
  const keys = [...S.cm.blSel]; if (!keys.length) return toast('Select users first', 'bad');
  if (!await confirmBox(`Unblock ${keys.length} user${keys.length > 1 ? 's' : ''}?`, 'The selected users will be able to claim their tasks again.', { ok: 'Unblock' })) return;
  await unblockKeys(keys); S.cm.blSel.clear(); toast(`${keys.length} unblocked`, 'ok');
};
ACT.blBulk = async el => {
  const ts = blockedTargets(); if (!ts.length) return toast('Select a task', 'bad');
  const ids = [...new Set($('#bl-ids').value.split(/[\s,]+/).map(x => x.trim()).filter(Boolean))]; if (!ids.length) return toast('Enter user IDs', 'bad');
  const block = el.dataset.mode === 'block', where = S.cm.blTask === 'all' ? `<b>${ts.length} tasks</b> (all)` : `<b>${esc(taskName(ts[0]))}</b>`;
  if (!await confirmBox(`${block ? 'Block' : 'Unblock'} ${ids.length} user${ids.length > 1 ? 's' : ''}?`, `Task: ${where}`, { ok: block ? 'Block users' : 'Unblock users', danger: block })) return;
  for (const t of ts) { const cur = t.blockedUserIds || []; await writeBlocked(t, block ? [...new Set([...cur, ...ids])] : cur.filter(x => !ids.includes(x))); }
  logAction(block ? 'user_block_task' : 'user_unblock_task', S.cm.blTask, { users: ids }); $('#bl-ids').value = ''; toast(`${ids.length} user${ids.length > 1 ? 's' : ''} ${block ? 'blocked' : 'unblocked'}`, 'ok');
};


/* === js/views/users.js === */
'use strict';
/* ============================================================
   USERS & WALLET
   ============================================================ */
const isOnline = u => userSeen(u) > Date.now() - 5 * 60 * 1000;
function usersFiltered() {
  const f = S.us, q = f.q.trim().toLowerCase();
  const from = f.from ? new Date(f.from + 'T00:00:00').getTime() : 0, to = f.to ? new Date(f.to + 'T23:59:59.999').getTime() : Infinity;
  let list = S.users.filter(u => {
    if (q && ![u.fullName, u.email, u.id, u.userId, u.upiId, u.phone].join(' ').toLowerCase().includes(q)) return false;
    if (f.status === 'active' && u.isBanned) return false; if (f.status === 'banned' && !u.isBanned) return false; if (f.status === 'balance' && !num(u.walletBalance)) return false;
    const t = userMs(u); if ((f.from || f.to) && (t < from || t > to)) return false;
    return true;
  });
  const sorters = { recent: (a, b) => userMs(b) - userMs(a), balance: (a, b) => num(b.walletBalance) - num(a.walletBalance), seen: (a, b) => userSeen(b) - userSeen(a), name: (a, b) => String(a.fullName || '').localeCompare(String(b.fullName || '')) };
  return list.sort(sorters[f.sort] || sorters.recent);
}
RENDER.users = () => {
  const online = S.users.filter(isOnline).length, banned = S.users.filter(u => u.isBanned).length;
  const k = (cls, icon, l, v, sub) => `<div class="kpi s ${cls}"><div class="ico">${ic(icon, 22)}</div><div class="grow"><div class="lbl">${l}</div><div class="val">${v}</div>${sub ? `<div class="fs11 muted">${sub}</div>` : ''}</div></div>`;
  setHTML($('#us-kpi'), k('blue', 'users', 'Total Users', S.users.length, '') + k('green', 'check', 'Online now', online, 'seen in last 5 min') + k('red', 'ban', 'Blocked', banned, '') + k('orange', 'wallet', 'Wallet Liability', inr(S.users.reduce((n, u) => n + num(u.walletBalance), 0)), ''));
  if (!S.live.users && !S.users.length) { setHTML($('#us-list'), skelHTML(5)); return; }
  const p = paginate(usersFiltered(), 'users', 15, S.us);
  setHTML($('#us-list'), p.items.map(u => {
    const seen = userSeen(u), on = isOnline(u);
    return `<div class="trow cols-users"><div class="row" style="min-width:0"><span class="avatar" style="width:38px;height:38px;background:var(--peach);color:var(--brand-deep)">${esc((u.fullName || u.email || 'U')[0].toUpperCase())}</span><div style="min-width:0"><div class="b fs13 trunc">${esc(u.fullName || 'User')}</div><div class="fs12 muted trunc">${esc(u.email || '')}</div><div class="mono fs11 brand-t trunc">${esc(u.userId || u.id)}</div></div></div>
      <span class="fs12" data-l="Registered">${fmtD(userMs(u))}</span><span data-l="Status">${u.isBanned ? '<span class="pill bad">Blocked</span>' : '<span class="pill ok">Active</span>'}</span>
      <span class="b ok-t" data-l="Wallet">${inr(u.walletBalance)}</span>
      <span class="fs12" data-l="Last active">${on ? '<span class="pill ok">● Online</span>' : seen ? `<span class="muted">${ago(seen)}</span>` : '<span class="faint">—</span>'}</span>
      <span><button class="btn soft xs" data-act="openUser" data-id="${esc(u.id)}">View</button></span></div>`;
  }).join('') || emptyHTML('users', 'No users match.'));
  setHTML($('#us-pager'), pagerHTML(p));
};
INP.usQ = el => { S.us.q = el.value; S.us.page = 1; renderAll(); };
INP.usStatus = el => { S.us.status = el.value; S.us.page = 1; renderAll(); };
INP.usSort = el => { S.us.sort = el.value; renderAll(); };
INP.usFrom = el => { S.us.from = el.value; S.us.page = 1; renderAll(); };
INP.usTo = el => { S.us.to = el.value; S.us.page = 1; renderAll(); };

/* ---------- user drawer ---------- */
const isCreditTx = t => { const s = String(t.type || ''); return !(/deduct|debit|penalty/i.test(s) || (/withdraw/i.test(s) && !/refund/i.test(s))); };
async function openDrawer(uid) {
  S.drawer = { uid, tab: 'subs', ledger: null };
  $('#drawer').classList.add('show'); $('#dscrim').classList.add('show'); renderDrawer();
  try {
    const snap = await getDocs(collection(db, 'users', uid, 'transactions'));
    if (S.drawer.uid === uid) { S.drawer.ledger = snap.docs.map(d => ({ id: d.id, ...d.data() })).sort((a, b) => tsMs(b.createdAt) - tsMs(a.createdAt)); renderDrawer(); }
  } catch (e) { if (S.drawer.uid === uid) { S.drawer.ledger = []; S.drawer.ledgerErr = e.code || e.message; renderDrawer(); } }
}
function closeDrawer() { S.drawer.uid = null; $('#drawer').classList.remove('show'); $('#dscrim').classList.remove('show'); }
ACT.closeDrawer = closeDrawer;
ACT.openUser = el => openDrawer(el.dataset.id);
ACT.drTab = el => { S.drawer.tab = el.dataset.k; renderDrawer(); };
function renderDrawer() {
  const u = S.users.find(x => x.id === S.drawer.uid); if (!u) return;
  const subs = S.subs.filter(s => s.userId === u.id || s.userId === u.userId), wds = S.wds.filter(w => w.userId === u.id || w.userId === u.userId);
  const ap = subs.filter(s => subSt(s) === 'approved').length, rj = subs.filter(s => subSt(s) === 'rejected').length, pn = subs.filter(s => subSt(s) === 'pending').length;
  const tab = S.drawer.tab, seen = userSeen(u);
  const tabs = [['subs', `Submissions (${subs.length})`], ['wds', `Withdrawals (${wds.length})`], ['ledger', 'Wallet ledger'], ['acts', 'Admin activity']].map(([k, l]) => `<button class="tab ${tab === k ? 'on' : ''}" data-act="drTab" data-k="${k}">${l}</button>`).join('');
  let body = '';
  if (tab === 'subs') body = subs.slice(0, 40).map(s => `<div class="lrow"><div class="grow"><div class="b fs13 trunc">${esc(s.taskTitle || 'Task')}</div><div class="fs12 muted">${fmtDT(subMs(s))} • ${inr(s.rewardAmount)}</div></div>${statusPill(subSt(s))}</div>`).join('') || emptyHTML('tasks', 'No submissions');
  if (tab === 'wds') body = wds.map(w => `<div class="lrow"><div class="grow"><div class="b fs13">${inr(w.amount)} <span class="tag">${wdMethod(w).toUpperCase()}</span></div><div class="fs12 muted">${fmtDT(wdMs(w))}${w.utrNumber ? ' • UTR ' + esc(w.utrNumber) : ''}</div></div>${wdPill(wdSt(w))}</div>`).join('') || emptyHTML('wallet', 'No withdrawals');
  if (tab === 'ledger') body = S.drawer.ledger === null ? skelHTML(3) : (S.drawer.ledgerErr ? `<div class="note bad">Could not load the ledger: ${esc(S.drawer.ledgerErr)}</div>` : '') + S.drawer.ledger.map(t => `<div class="lrow"><div class="rico ${isCreditTx(t) ? 'green' : 'red'}">${ic(isCreditTx(t) ? 'plus' : 'trend', 16)}</div><div class="grow" style="min-width:0"><div class="b fs13 trunc">${esc(t.note || t.type || 'Transaction')}</div><div class="fs11 muted mono">${esc(t.id.slice(0, 10))} • ${esc(t.type || '')} • ${fmtDT(tsMs(t.createdAt))}</div></div><div class="b ${isCreditTx(t) ? 'ok-t' : 'bad-t'}">${isCreditTx(t) ? '+' : '−'}${inr(t.amount)}</div></div>`).join('') || (S.drawer.ledger.length ? '' : emptyHTML('wallet', 'No ledger entries'));
  if (tab === 'acts') body = S.logs.filter(l => l.target === u.id || (l.details && (l.details.user === u.id || l.details.uid === u.id))).map(l => `<div class="lrow"><div class="grow"><div class="b fs13">${esc(l.action)}</div><div class="fs12 muted">${esc(l.adminEmail || '')} • ${fmtDT(tsMs(l.createdAt) || l.createdMs)}</div></div></div>`).join('') || emptyHTML('activity', 'No admin actions logged');
  setHTML($('#drawer'), `<div class="row between" style="padding:18px 20px;border-bottom:1px solid var(--line);position:sticky;top:0;background:var(--panel);z-index:2"><div class="row" style="min-width:0"><span class="avatar" style="width:46px;height:46px;background:var(--peach);color:var(--brand-deep);font-size:18px">${esc((u.fullName || u.email || 'U')[0].toUpperCase())}</span><div style="min-width:0"><div class="b fs16 trunc">${esc(u.fullName || 'User')}</div><div class="fs12 muted trunc">${esc(u.email || '')}</div></div></div><button class="iconbtn" data-act="closeDrawer" aria-label="Close">${ic('x')}</button></div>
    <div style="padding:18px 20px" class="col" style="gap:14px">
      <div class="row wrap">${u.isBanned ? '<span class="pill bad">Blocked</span>' : '<span class="pill ok">Active</span>'}${isOnline(u) ? '<span class="pill ok">● Online</span>' : seen ? `<span class="pill mute">Last seen ${ago(seen)}</span>` : '<span class="pill mute">Last seen unknown</span>'}<button class="btn ghost xs" data-act="copyTxt" data-t="${esc(u.userId || u.id)}">${ic('copy', 12)}${esc((u.userId || u.id).slice(0, 14))}</button></div>
      ${u.isBanned && u.banReason ? `<div class="note bad mt8">${ic('ban', 16)}<div>${esc(u.banReason)}</div></div>` : ''}
      <div class="kpi orange mt12" style="padding:14px 18px"><div class="ico">${ic('wallet', 24)}</div><div class="grow"><div class="lbl">Wallet balance</div><div class="val ok-t" style="font-size:28px">${inr(u.walletBalance)}</div></div></div>
      <div class="mini mt12"><div><b>${num(u.completedTasksCount)}</b>Completed</div><div><b>${pn}</b>Pending</div><div><b>${ap}/${rj}</b>Appr/Rej</div></div>
      <dl class="kv mt12"><dt>User ID</dt><dd class="mono">${esc(u.userId || u.id)}</dd><dt>Registered</dt><dd>${fmtDT(userMs(u))}</dd><dt>Last active</dt><dd>${seen ? fmtDT(seen) : '—'}</dd>${u.upiId ? `<dt>UPI ID</dt><dd class="mono">${esc(u.upiId)}</dd>` : ''}</dl>
      <div class="row wrap mt16"><button class="btn ok sm" data-act="walletAdj" data-id="${esc(u.id)}" data-credit="1">${ic('plus', 14)}Credit ₹</button><button class="btn soft sm" data-act="walletAdj" data-id="${esc(u.id)}" data-credit="0">Deduct ₹</button><button class="btn ${u.isBanned ? 'blue' : 'bad'} sm" data-act="banToggle" data-id="${esc(u.id)}">${ic('ban', 14)}${u.isBanned ? 'Unblock' : 'Block user'}</button><button class="btn ghost sm" data-act="notifyUser" data-id="${esc(u.id)}">${ic('bell', 14)}Notify</button></div>
      <div class="tabs mt16">${tabs}</div><div class="mt8">${body}</div></div>`);
}
ACT.copyTxt = el => copyText(el.dataset.t);
ACT.notifyUser = el => { closeDrawer(); go('notifications'); $('#nt-mode').value = 'uids'; $('#nt-uids-wrap').classList.remove('hidden'); $('#nt-uids').value = el.dataset.id; updateNtPreview(); };
ACT.walletAdj = async el => {
  const u = S.users.find(x => x.id === el.dataset.id); if (!u) return; const credit = el.dataset.credit === '1';
  const v = await ask({ title: `${credit ? 'Credit' : 'Deduct'} wallet — ${u.fullName || u.id}`, body: `Current balance: <b>${inr(u.walletBalance)}</b>. This action will be recorded in the audit log.`, ok: credit ? 'Credit wallet' : 'Deduct wallet', danger: !credit, fields: [{ id: 'amount', label: 'Amount (₹)', type: 'number', min: 1, required: true, value: '10' }, { id: 'reason', label: 'Reason (shown in ledger)', type: 'textarea', required: true, placeholder: credit ? 'e.g. Manual bonus for support ticket #123' : 'e.g. Reversal of invalid reward' }] });
  if (!v) return; const amt = Math.floor(num(v.amount)); if (amt <= 0) return toast('Enter a valid amount', 'bad');
  const ref = doc(db, 'users', u.id); let after = 0;
  await runTransaction(db, async tx => {
    const snap = await tx.get(ref); if (!snap.exists()) throw new Error('User record not found');
    const bal = num(snap.data().walletBalance), nb = credit ? bal + amt : bal - amt; if (nb < 0) throw new Error('Insufficient balance for this deduction'); after = nb;
    tx.update(ref, { walletBalance: nb, updatedAt: serverTimestamp() });
    tx.set(doc(collection(db, 'users', u.id, 'transactions')), { userId: u.id, amount: amt, type: credit ? 'admin_credit' : 'admin_deduct', note: v.reason, balanceAfter: nb, adminEmail: S.admin, createdAt: serverTimestamp() });
  });
  logAction(credit ? 'wallet_credit' : 'wallet_deduct', u.id, { amount: amt, reason: v.reason, before: num(u.walletBalance), after });
  toast(`${credit ? 'Credited' : 'Deducted'} ${inr(amt)} — new balance ${inr(after)}`, 'ok');
  S.drawer.ledger = null; openDrawerLedgerRefresh(u.id);
};
async function openDrawerLedgerRefresh(uid) { try { const snap = await getDocs(collection(db, 'users', uid, 'transactions')); if (S.drawer.uid === uid) { S.drawer.ledger = snap.docs.map(d => ({ id: d.id, ...d.data() })).sort((a, b) => tsMs(b.createdAt) - tsMs(a.createdAt)); renderDrawer(); } } catch (_) { } }
ACT.banToggle = async el => {
  const u = S.users.find(x => x.id === el.dataset.id); if (!u) return; const ban = !u.isBanned;
  const v = await ask({ title: ban ? `Block ${u.fullName || 'user'}?` : `Unblock ${u.fullName || 'user'}?`, body: ban ? 'The user will not be able to use the app. A reason is required.' : 'The user will be able to use the app again.', ok: ban ? 'Block user' : 'Unblock', danger: ban, fields: ban ? [{ id: 'reason', label: 'Reason', type: 'textarea', required: true, placeholder: 'e.g. Fake proofs submitted repeatedly' }] : [] });
  if (!v) return; const reason = ban ? v.reason : '';
  await updateDoc(doc(db, 'users', u.id), { isBanned: ban, banReason: ban ? reason : '', bannedAt: ban ? serverTimestamp() : null, accountStatus: ban ? 'banned' : 'active', updatedAt: serverTimestamp() });
  logAction(ban ? 'user_block' : 'user_unblock', u.id, { reason }); toast(ban ? 'User blocked' : 'User unblocked', 'ok');
};


ACT.cleanDuplicates = async () => {
  const emailMap = {};
  const duplicateUsers = [];

  S.users.forEach(u => {
    const emailKey = (u.email || '').trim().toLowerCase();
    if (!emailKey) return;
    if (!emailMap[emailKey]) {
      emailMap[emailKey] = [];
    }
    emailMap[emailKey].push(u);
  });

  Object.keys(emailMap).forEach(key => {
    const group = emailMap[key];
    if (group.length > 1) {
      group.sort((a, b) => (num(b.walletBalance) - num(a.walletBalance)) || (userMs(b) - userMs(a)));
      for (let i = 1; i < group.length; i++) {
        duplicateUsers.push(group[i]);
      }
    }
  });

  if (duplicateUsers.length === 0) {
    return toast('No duplicate user accounts found!', 'ok');
  }

  const v = await ask({
    title: `Remove ${duplicateUsers.length} duplicate accounts?`,
    body: `Found <b>${duplicateUsers.length}</b> secondary duplicate user accounts sharing emails. Secondary accounts will be blocked (1 account per user rule). Primary account with highest balance will be kept active.`,
    ok: 'Clean Duplicates',
    danger: true
  });

  if (!v) return;

  let cleaned = 0;
  for (const du of duplicateUsers) {
    try {
      await updateDoc(doc(db, 'users', du.id), {
        isBanned: true,
        banReason: 'Duplicate account removed (1 account per user rule)',
        accountStatus: 'deleted',
        updatedAt: serverTimestamp()
      });
      cleaned++;
    } catch (_) {}
  }

  toast(`Cleaned ${cleaned} duplicate user accounts!`, 'ok');
  logAction('clean_duplicate_users', 'bulk', { count: cleaned });
};


/* === js/views/withdrawals.js === */
'use strict';
/* ============================================================
   WITHDRAWALS
   ============================================================ */
function wdBase() {
  const f = S.wd, q = f.q.trim().toLowerCase(); let a = 0, b = Infinity;
  if (f.month) [a, b] = monthRange(f.month);
  if (f.from) a = Math.max(a, new Date(f.from + 'T00:00:00').getTime()); if (f.to) b = Math.min(b, new Date(f.to + 'T23:59:59.999').getTime());
  return S.wds.filter(w => {
    if (f.method !== 'all' && wdMethod(w) !== f.method) return false;
    if (q && ![w.id, w.userId, w.userName, w.userEmail, w.upiId, w.utrNumber, w.accountHolderName].join(' ').toLowerCase().includes(q)) return false;
    const t = wdSt(w) === 'approved' ? wdPaidMs(w) : wdMs(w); return t >= a && t <= b;
  });
}
function wdFiltered() { const t = S.wd.tab; return wdBase().filter(w => { const st = wdSt(w); return t === 'all' || (t === 'pending' ? ['pending', 'processing'].includes(st) : st === t); }); }
function wdExtras(B) {
  const f = S.wd, set = (sel, v) => { const e = $(sel); if (e && document.activeElement !== e) e.value = v; };
  set('[data-in="wdMonth"]', f.month); set('[data-in="wdFrom"]', f.from); set('[data-in="wdTo"]', f.to);
  $('#wd-mlabel').textContent = (f.from || f.to) ? `${f.from || '…'} → ${f.to || '…'}` : f.month ? monthLabel(f.month) : 'All time';
  const days = {};
  B.forEach(w => { const st = wdSt(w), t = st === 'approved' ? wdPaidMs(w) : wdMs(w), k = new Date(t).toLocaleDateString('en-CA'), d = days[k] = days[k] || { k, n: 0, paid: 0, pend: 0, rej: 0 }, amt = num(w.amount); d.n++; if (st === 'approved') d.paid += amt; else if (st === 'rejected') d.rej += amt; else d.pend += amt; });
  const rows = Object.values(days).sort((a, b) => b.k.localeCompare(a.k));
  setHTML($('#wd-days'), rows.length ? `<div class="table"><div class="thead cols-day"><span>Date</span><span>Requests</span><span>Paid</span><span>Pending</span><span>Rejected</span></div>${rows.map(d => `<button class="trow cols-day" style="width:100%;border:0;text-align:left;cursor:pointer;font:inherit;color:inherit" data-act="wdDay" data-d="${d.k}"><span class="b" data-l="Date">${fmtD(new Date(d.k + 'T00:00:00').getTime())}</span><span data-l="Requests">${d.n}</span><span class="ok-t b" data-l="Paid">${inr(d.paid)}</span><span class="brand-t b" data-l="Pending">${inr(d.pend)}</span><span class="bad-t" data-l="Rejected">${inr(d.rej)}</span></button>`).join('')}</div>` : emptyHTML('calendar', 'No payment records in this period.'));
}
const wdShift = d => { S.wd.month = shiftMonth(S.wd.month || monthKey(Date.now()), d); S.wd.from = S.wd.to = ''; S.wd.page = 1; renderAll(); };
ACT.wdPrev = () => wdShift(-1); ACT.wdNext = () => wdShift(1);
ACT.wdAll = () => { S.wd.month = S.wd.from = S.wd.to = ''; S.wd.page = 1; renderAll(); };
ACT.wdDay = el => { S.wd.from = S.wd.to = el.dataset.d; S.wd.month = ''; S.wd.tab = 'all'; S.wd.page = 1; renderAll(); };
RENDER.withdrawals = () => {
  const B = wdBase(), pend = B.filter(w => ['pending', 'processing'].includes(wdSt(w))), sum = l => l.reduce((n, w) => n + num(w.amount), 0);
  const k = (cls, icon, l, v, sub) => `<div class="kpi s ${cls}"><div class="ico">${ic(icon, 22)}</div><div class="grow"><div class="lbl">${l}</div><div class="val">${v}</div>${sub ? `<div class="fs11 muted">${sub}</div>` : ''}</div></div>`;
  const paid = B.filter(w => wdSt(w) === 'approved'), rej = B.filter(w => wdSt(w) === 'rejected');
  setHTML($('#wd-kpi'), k('orange', 'clock', 'Pending payout', inr(sum(pend)), `${pend.length} request${pend.length === 1 ? '' : 's'}`) + k('green', 'check', 'Paid', inr(sum(paid)), `${paid.length} payouts`) + k('red', 'x', 'Rejected', rej.length, inr(sum(rej))) + k('blue', 'wallet', 'Total requested', inr(sum(B)), `${B.length} requests`));
  const cnt = k2 => B.filter(w => k2 === 'pending' ? ['pending', 'processing'].includes(wdSt(w)) : k2 === 'all' ? true : wdSt(w) === k2).length;
  setHTML($('#wd-tabs'), [['pending', 'Pending'], ['approved', 'Paid / Completed'], ['rejected', 'Rejected'], ['all', 'All']].map(([x, l]) => `<button class="tab ${S.wd.tab === x ? 'on' : ''}" data-act="wdTab" data-k="${x}">${l} <span class="n">(${cnt(x)})</span></button>`).join(''));
  wdExtras(B);
  if (!S.live.withdrawals && !S.wds.length) { setHTML($('#wd-list'), skelHTML(4)); return; }
  const p = paginate(wdFiltered(), 'wd', 12, S.wd);
  setHTML($('#wd-list'), p.items.map(w => {
    const st = wdSt(w), m = wdMethod(w), pending = ['pending', 'processing'].includes(st), rev = S.wd.reveal.has(w.id);
    const pay = m === 'upi' ? `<div class="row gap6"><span class="mono fs12 trunc">${esc(w.upiId)}</span><button class="btn ghost xs" data-act="copyTxt" data-t="${esc(w.upiId)}" aria-label="Copy UPI">${ic('copy', 12)}</button></div>`
      : m === 'bank' ? `<div class="fs12" style="line-height:1.5">${esc(w.accountHolderName || '')}<br>${esc(w.bankName || 'Bank')}<br><span class="mono">${rev ? esc(w.accountNumber) : mask(w.accountNumber)}</span> <button class="btn ghost xs" data-act="wdReveal" data-id="${esc(w.id)}" aria-label="Toggle account number">${ic(rev ? 'eye-off' : 'eye', 12)}</button><br><span class="mono muted">IFSC ${esc(w.ifscCode || '—')}</span></div>` : '<span class="muted">—</span>';
    const act = pending ? `<div class="row gap6"><button class="btn okfill xs" data-act="wdPay" data-id="${esc(w.id)}">Mark Paid</button><button class="btn bad xs" data-act="wdReject" data-id="${esc(w.id)}">Reject</button></div>` : st === 'approved' ? `<span class="fs11 muted">Paid ${fmtDT(wdPaidMs(w))}<br>by ${esc(w.processedBy || '—')}</span>` : `<span class="fs11 bad-t">${esc(w.adminNote || '')}</span>`;
    return `<div class="trow cols-wd"><span class="mono fs12 b" data-l="ID">#${esc(w.id.slice(0, 8))}</span><div style="min-width:0" data-l="User"><div class="b fs13 trunc">${esc(w.userName || w.userEmail || 'User')}</div><div class="mono fs11 muted trunc">${esc(w.userId || '')}</div></div><span class="b brand-t fs16" data-l="Amount">${inr(w.amount)}</span><span class="fs12" data-l="Requested">${fmtDT(wdMs(w))}</span><div data-l="Details" style="min-width:0">${pay}</div><div data-l="Status">${wdPill(st)}${w.utrNumber ? `<div class="mono fs11 muted mt4">UTR ${esc(w.utrNumber)}</div>` : ''}</div><div>${act}</div></div>`;
  }).join('') || emptyHTML('wallet', 'No withdrawal requests in this folder.'));
  setHTML($('#wd-pager'), pagerHTML(p));
};
ACT.wdTab = el => { S.wd.tab = el.dataset.k; S.wd.page = 1; renderAll(); };
INP.wdQ = el => { S.wd.q = el.value; S.wd.page = 1; renderAll(); };
INP.wdMethod = el => { S.wd.method = el.value; S.wd.page = 1; renderAll(); };
INP.wdMonth = el => { S.wd.month = el.value; S.wd.page = 1; renderAll(); };
INP.wdFrom = el => { S.wd.from = el.value; S.wd.page = 1; renderAll(); };
INP.wdTo = el => { S.wd.to = el.value; S.wd.page = 1; renderAll(); };
ACT.wdReveal = async el => {
  const id = el.dataset.id;
  if (S.wd.reveal.has(id)) { S.wd.reveal.delete(id); return renderAll(); }
  if (!await confirmBox('Show full account number?', 'This is sensitive bank information. This access will be recorded in the audit log.', { ok: 'Show' })) return;
  S.wd.reveal.add(id); logAction('reveal_bank_details', id); renderAll();
};
ACT.wdPay = async el => {
  const w = S.wds.find(x => x.id === el.dataset.id); if (!w || busy) return;
  const v = await ask({
    title: 'Mark payout as paid', body: `Pay <b>${inr(w.amount)}</b> to <b>${esc(w.userName || w.userId || 'user')}</b> via ${wdMethod(w) === 'upi' ? 'UPI <span class="mono">' + esc(w.upiId) + '</span>' : 'bank transfer'}.<br>Confirm only after the payment has been sent.`, ok: 'Confirm paid',
    fields: [{ id: 'utr', label: 'UTR / transaction reference', required: true, placeholder: 'e.g. 4231 5678 9012' }, { id: 'paid', label: 'Paid amount (₹)', type: 'number', value: String(num(w.amount)), required: true, min: 1 }, { id: 'note', label: 'Note (optional)', type: 'textarea' }]
  });
  if (!v) return; busy = true;
  try {
    const ref = doc(db, 'withdrawals', w.id), note = v.note || `Paid manually by Admin (UTR: ${v.utr})`;
    const uref = w.userId ? doc(db, 'users', w.userId) : null;
    await runTransaction(db, async tx => {
      const snap = await tx.get(ref); const cur = String(snap.data()?.status || 'pending').toLowerCase();
      if (!['pending', 'processing'].includes(cur)) throw new Error('This request is already ' + cur + '.');
      tx.update(ref, { status: 'approved', utrNumber: v.utr, paidAmount: num(v.paid), paidAt: serverTimestamp(), processedAt: serverTimestamp(), processedBy: S.admin, adminNote: note, updatedAt: serverTimestamp() });

      if (uref) {
        const us = await tx.get(uref);
        if (us && us.exists()) {
          const uData = us.data();
          const referredByUid = uData.referredByUserId || '';
          const referredByCode = uData.referredByCode || '';
          const withdrawAmt = num(v.paid);

          // 1. Check First Withdrawal Bonus Eligibility (₹5 for Referred User B, ₹5 for Referrer A)
          const firstWdClaimed = uData.firstWithdrawalBonusClaimed === true;
          const isEligibleFirstWd = (referredByUid || referredByCode) && !firstWdClaimed;

          if (isEligibleFirstWd) {
            tx.update(uref, {
              firstWithdrawalBonusClaimed: true,
              walletBalance: num(uData.walletBalance) + 5,
              totalReferralBonusEarned: num(uData.totalReferralBonusEarned) + 5,
              updatedAt: serverTimestamp()
            });
            tx.set(doc(collection(db, 'users', w.userId, 'transactions')), {
              userId: w.userId,
              amount: 5,
              type: 'referral_bonus',
              note: '1st Withdrawal Referral Bonus (+₹5)',
              createdAt: serverTimestamp()
            });
          }

          // 2. Credit 1% Lifetime Referral Commission to Referrer A
          if (referredByUid) {
            const referrerRef = doc(db, 'users', referredByUid);
            const refSnap = await tx.get(referrerRef);
            if (refSnap.exists()) {
              const commAmount = Math.max(1, Math.round(withdrawAmt * 0.01));
              const refBal = num(refSnap.data().walletBalance);
              const refComm = num(refSnap.data().totalLifetimeCommissionEarned);
              const refBonus = isEligibleFirstWd ? 5 : 0;

              tx.update(referrerRef, {
                walletBalance: refBal + commAmount + refBonus,
                totalLifetimeCommissionEarned: refComm + commAmount,
                totalReferralBonusEarned: num(refSnap.data().totalReferralBonusEarned) + refBonus,
                updatedAt: serverTimestamp()
              });

              // Log 1% Lifetime Commission Transaction
              const commEventId = `comm_wd_${referredByUid}_${w.userId}_${w.id}`.replace(/[^a-zA-Z0-9_-]/g, "_").slice(0, 120);
              tx.set(doc(db, 'users', referredByUid, 'transactions', commEventId), {
                userId: referredByUid,
                amount: commAmount,
                type: 'referral_commission',
                note: `1% Lifetime Commission: ${w.userName || 'Friend'} withdrew ₹${withdrawAmt} (+₹${commAmount})`,
                withdrawalId: w.id,
                createdAt: serverTimestamp()
              });

              // Log ₹5 Bonus Transaction for Referrer
              if (isEligibleFirstWd) {
                const bonusEventId = `bonus_first_wd_${referredByUid}_${w.userId}`.replace(/[^a-zA-Z0-9_-]/g, "_").slice(0, 120);
                tx.set(doc(db, 'users', referredByUid, 'transactions', bonusEventId), {
                  userId: referredByUid,
                  amount: 5,
                  type: 'referral_bonus',
                  note: `Referral Bonus (+₹5): ${w.userName || 'Friend'} completed 1st withdrawal`,
                  createdAt: serverTimestamp()
                });
              }
            }
          }
        }
      }
    });
    if (w.userId) { try { await setDoc(doc(db, 'users', w.userId, 'withdrawals', w.id), { status: 'approved', utrNumber: v.utr, adminNote: note, updatedAt: serverTimestamp() }, { merge: true }); } catch (_) { } }
    logAction('payout_paid', w.id, { user: w.userId, amount: num(v.paid), utr: v.utr }); toast(`Marked ${inr(v.paid)} as paid`, 'ok');
  } catch (e) { toast(e.message, 'bad'); }
  busy = false; renderAll();
};
ACT.wdReject = async el => {
  const w = S.wds.find(x => x.id === el.dataset.id); if (!w || busy) return;
  const v = await ask({ title: 'Reject & refund?', body: `<b>${inr(w.amount)}</b> will be credited back to the user’s wallet. The reason will be shown to the user.`, ok: 'Reject & refund', danger: true, fields: [{ id: 'reason', label: 'Reason', type: 'textarea', required: true, value: 'Invalid payment details' }] });
  if (!v) return; busy = true;
  try {
    const ref = doc(db, 'withdrawals', w.id), uref = w.userId ? doc(db, 'users', w.userId) : null;
    await runTransaction(db, async tx => {
      const snap = await tx.get(ref); const cur = String(snap.data()?.status || 'pending').toLowerCase();
      if (!['pending', 'processing'].includes(cur)) throw new Error('This request is already ' + cur + '.');
      const us = uref ? await tx.get(uref) : null;
      tx.update(ref, { status: 'rejected', adminNote: v.reason, rejectionReason: v.reason, processedBy: S.admin, updatedAt: serverTimestamp() });
      if (us && us.exists()) {
        tx.update(uref, { walletBalance: num(us.data().walletBalance) + num(w.amount), updatedAt: serverTimestamp() });
        tx.set(doc(collection(db, 'users', w.userId, 'transactions')), { userId: w.userId, amount: num(w.amount), type: 'withdrawal_refund', note: `Withdrawal rejected: ${v.reason}`, createdAt: serverTimestamp() });
      }
    });
    if (w.userId) { try { await setDoc(doc(db, 'users', w.userId, 'withdrawals', w.id), { status: 'rejected', adminNote: v.reason, updatedAt: serverTimestamp() }, { merge: true }); } catch (_) { } }
    logAction('payout_reject', w.id, { user: w.userId, amount: num(w.amount), reason: v.reason }); toast(`Rejected & refunded ${inr(w.amount)}`, 'ok');
  } catch (e) { toast(e.message, 'bad'); }
  busy = false; renderAll();
};
ACT.wdCsv = () => {
  const rows = [['Withdrawal ID', 'User ID', 'Name', 'Amount', 'Requested', 'Status', 'Method', 'UPI / Bank', 'Account (masked)', 'IFSC', 'UTR', 'Paid at', 'Processed by', 'Note']];
  wdFiltered().forEach(w => rows.push([w.id, w.userId, w.userName || w.userEmail, num(w.amount), fmtDT(wdMs(w)), wdSt(w), wdMethod(w), w.upiId || w.bankName || '', mask(w.accountNumber), w.ifscCode || '', w.utrNumber || '', wdSt(w) === 'approved' ? fmtDT(wdPaidMs(w)) : '', w.processedBy || '', w.adminNote || '']));
  saveText(`payouts_${S.wd.tab}_${Date.now()}.csv`, toCsv(rows), 'text/csv'); logAction('export_payouts_csv', S.wd.tab, { rows: rows.length - 1 });
};


/* === js/views/reports.js === */
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


/* === js/views/notifications.js === */
'use strict';
/* ============================================================
   PUSH NOTIFICATIONS
   ============================================================ */
const NT_TEMPLATES = [
  ['New task available', 'New task available 🎯', 'A new task is live. Open the app to see it and earn.'],
  ['Task reminder', 'Task reminder ⏰', 'You have a task in progress. Submit your proof before the timer ends.'],
  ['Submission received', 'Submission received', 'We received your submission and will review it shortly.'],
  ['Approval completed', 'Submission approved ✅', 'Your submission was approved and the reward was added to your wallet.'],
  ['Submission rejected', 'Submission rejected', 'Your submission was rejected. Reason: [add reason here]'],
  ['Withdrawal received', 'Withdrawal request received', 'We received your withdrawal request and will process it soon.'],
  ['Withdrawal paid', 'Withdrawal processed ✅', 'Your withdrawal has been processed. Please check your account.'],
  ['Maintenance notice', 'Scheduled maintenance', MAINT_MSG]
];
function updateNtPreview() {
  const t = $('#nt-title').value.trim() || 'Notification title', m = $('#nt-msg').value.trim() || 'Your message will appear here.';
  setHTML($('#nt-prev'), `<div class="row" style="align-items:flex-start"><span class="avatar" style="background:linear-gradient(135deg,#38BDF8,#0EA5E9);width:34px;height:34px;border-radius:10px">A</span><div style="min-width:0"><div class="row between fs11 muted"><span>TASK GULLAK</span><span>now</span></div><div class="b fs13">${esc(t)}</div><div class="fs12 ink2" style="line-height:1.45">${esc(m)}</div></div></div>`);
}
INP.ntPrev = () => updateNtPreview();
INP.ntMode = el => { $('#nt-uids-wrap').classList.toggle('hidden', el.value !== 'uids'); updateNtPreview(); };
ACT.ntTpl = el => { const t = NT_TEMPLATES[Number(el.dataset.i)]; $('#nt-title').value = t[1]; $('#nt-msg').value = t[2]; updateNtPreview(); $('#nt-title').focus(); };
RENDER.notifications = () => {
  setHTML($('#nt-templates'), NT_TEMPLATES.map((t, i) => `<button class="tab" data-act="ntTpl" data-i="${i}">${esc(t[0])}</button>`).join(''));
  if (!$('#nt-prev')._h) updateNtPreview();
  const list = S.bcasts.filter(b => b.id !== 'system_maintenance_banner');
  $('#nt-count').textContent = `${list.length} sent`;
  setHTML($('#nt-history'), list.slice(0, 40).map(b => `<div class="lrow"><div class="rico purple">${ic('bell', 16)}</div><div class="grow" style="min-width:0"><div class="b fs13 trunc">${esc(b.title || '')}</div><div class="fs12 muted trunc">${esc(b.message || '')}</div><div class="fs11 muted mono">To: ${esc(b.targetUserId && b.targetUserId !== 'all' ? b.targetUserId.slice(0, 14) : 'All users')} • ${esc(b.type || 'broadcast')} • ${fmtDT(tsMs(b.createdAt) || num(b.timestamp))}</div></div><span class="pill ok">Sent</span><button class="btn bad xs" data-act="ntDel" data-id="${esc(b.id)}" aria-label="Delete">${ic('trash', 13)}</button></div>`).join('') || emptyHTML('bell', 'No notifications sent yet.'));
};
ACT.ntDel = async el => {
  if (!await confirmBox('Delete this notification?', 'It may also be removed from the user app.', { ok: 'Delete', danger: true })) return;
  await deleteDoc(doc(db, 'broadcasts', el.dataset.id)); try { await deleteDoc(doc(db, 'notifications', el.dataset.id)); } catch (_) { }
  logAction('notification_delete', el.dataset.id); toast('Notification deleted');
};
ACT.ntSend = async () => {
  const title = $('#nt-title').value.trim(), message = $('#nt-msg').value.trim(), type = $('#nt-type').value, mode = $('#nt-mode').value, link = $('#nt-link').value.trim();
  if (!title || !message) return toast('Enter both a title and a message', 'bad');
  let targets = ['all'], label = 'ALL users';
  if (mode === 'uids') { targets = [...new Set($('#nt-uids').value.split(/[\s,]+/).map(x => x.trim()).filter(Boolean))]; if (!targets.length) return toast('Enter user IDs', 'bad'); label = `${targets.length} selected user${targets.length > 1 ? 's' : ''}`; }
  if (mode === 'earners') { targets = S.users.filter(u => num(u.completedTasksCount) > 0 && !u.isBanned).map(u => u.id); if (!targets.length) return toast('No users in this segment', 'bad'); label = `${targets.length} users with completed tasks`; }
  if (targets.length > 400) return toast('You cannot send to more than 400 users at once — narrow the segment.', 'bad');
  if (!await confirmBox('Send notification?', `<div class="note info" style="margin-bottom:10px"><div><b>${esc(title)}</b><br>${esc(message)}</div></div>Send to: <b>${label}</b>.`, { ok: 'Send now' })) return;
  let sent = 0;
  for (const target of targets) {
    const ref = doc(collection(db, 'broadcasts'));
    const payload = { id: ref.id, title, message, body: message, description: message, type, targetUserId: target, userId: target, active: true, isActive: true, read: false, isRead: false, timestamp: Date.now(), createdAt: serverTimestamp(), updatedAt: serverTimestamp(), sentBy: S.admin };
    if (link) payload.deepLink = link;
    await setDoc(ref, payload); try { await setDoc(doc(db, 'notifications', ref.id), payload); } catch (_) { } sent++;
  }
  logAction('notification_send', mode, { title, recipients: sent });
  $('#nt-title').value = ''; $('#nt-msg').value = ''; $('#nt-link').value = ''; updateNtPreview(); toast(`Sent to ${label}`, 'ok');
};


/* === js/views/tickets.js === */
'use strict';
/* ============================================================
   SUPPORT TICKETS & INQUIRIES
   ============================================================ */
RENDER.tickets = () => {
  const tickets = S.tickets || [];
  const countEl = $('#ticket-count'); if (countEl) countEl.textContent = `${tickets.length} total ticket${tickets.length === 1 ? '' : 's'}`;
  const badgeEl = $('#bdg-tickets');
  const openCount = tickets.filter(t => (t.status || 'open').toLowerCase() === 'open').length;
  if (badgeEl) {
    badgeEl.textContent = openCount;
    badgeEl.classList.toggle('hidden', !openCount);
  }

  const listContainer = $('#tickets-list'); if (!listContainer) return;
  if (!tickets.length) {
    setHTML(listContainer, emptyHTML('support', 'No user support tickets logged yet.'));
    return;
  }

  setHTML(listContainer, tickets.map(t => {
    const st = (t.status || 'open').toLowerCase();
    const isResolved = st === 'resolved' || st === 'closed';
    return `<div class="card"><div class="row between wrap"><div class="row gap10"><span class="avatar" style="width:38px;height:38px;background:var(--peach);color:var(--brand-deep)">${esc((t.userName || t.userEmail || 'U')[0].toUpperCase())}</span><div><div class="b fs14">${esc(t.userName || 'Task Gullak User')}</div><div class="fs12 muted">${esc(t.userEmail || t.userId || '')} • ${fmtDT(tsMs(t.createdAt))}</div></div></div><span class="pill ${isResolved ? 'ok' : 'bad'}">${isResolved ? 'Resolved' : 'Pending Review'}</span></div><div class="note mt12" style="background:var(--field)"><i data-ic="message"></i><div class="fs13" style="white-space:pre-wrap">${esc(t.message || 'No details provided')}</div></div><div class="row wrap mt12"><button class="btn soft sm" data-act="replyTicket" data-uid="${esc(t.userId)}" data-name="${esc(t.userName || '')}">${ic('send', 14)}Reply via Push</button><button class="btn ok sm" data-act="resolveTicket" data-id="${esc(t.id)}" ${isResolved ? 'disabled' : ''}>${ic('check', 14)}${isResolved ? 'Resolved' : 'Mark Resolved'}</button><button class="btn ghost sm" data-act="openUser" data-id="${esc(t.userId)}">View User Profile</button></div></div>`;
  }).join(''));
};

ACT.resolveTicket = async el => {
  const id = el.dataset.id; if (!id) return;
  try {
    await updateDoc(doc(db, 'support_tickets', id), {
      status: 'resolved',
      resolvedAt: serverTimestamp(),
      resolvedBy: S.admin
    });
    toast('Support ticket marked as resolved!', 'ok');
    logAction('resolve_support_ticket', id);
  } catch (e) {
    toast('Error: ' + e.message, 'bad');
  }
};

ACT.replyTicket = async el => {
  const uid = el.dataset.uid, name = el.dataset.name || 'User'; if (!uid) return;
  const v = await ask({
    title: `Reply to ${name}`,
    body: `Send a direct push notification reply to this user's app.`,
    ok: 'Send Reply',
    fields: [
      { id: 'title', label: 'Notification Title', value: 'Support Reply — Task Gullak', required: true },
      { id: 'message', label: 'Message / Solution', type: 'textarea', placeholder: 'Type your reply message here...', required: true }
    ]
  });
  if (!v) return;
  try {
    const ref = doc(collection(db, 'broadcasts'));
    await setDoc(ref, {
      id: ref.id,
      title: v.title,
      message: v.message,
      body: v.message,
      type: 'alert',
      targetUserId: uid,
      active: true,
      createdBy: S.admin,
      createdAt: serverTimestamp()
    });
    toast(`Reply notification sent to ${name}!`, 'ok');
    logAction('reply_support_ticket', uid, { message: v.message });
  } catch (e) {
    toast('Error: ' + e.message, 'bad');
  }
};


/* === js/views/maintenance.js === */
'use strict';
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
  logAction('maintenance_toggle', 'system_maintenance_banner', { before: on, after: !on }); toast(!on ? '🚧 Maintenance ENABLED' : '✅ Maintenance DISABLED', 'ok');
};
ACT.maintEdit = async () => {
  const md = maintDoc();
  const v = await ask({ title: 'Update maintenance message', ok: 'Save message', fields: [{ id: 'title', label: 'Title', value: md.title || MAINT_TITLE, required: true }, { id: 'msg', label: 'Message shown in user app', type: 'textarea', value: md.message || MAINT_MSG, required: true }] });
  if (!v) return; await saveMaintenance(isMaintActive(), v.title, v.msg); logAction('maintenance_message', 'system_maintenance_banner', { title: v.title }); toast('Message updated', 'ok');
};


/* === js/views/settings.js === */
'use strict';
/* ============================================================
   SETTINGS
   ============================================================ */
RENDER.settings = () => {
  renderThemeCard();
  const kv = rows => `<dl class="kv">${rows.map(([k, v]) => `<dt>${k}</dt><dd>${v}</dd>`).join('')}</dl>`;
  setHTML($('#st-session'), kv([['Signed in as', esc(S.admin)], ['Access', 'Authorized admin (email whitelist)'], ['Browser time', esc(Intl.DateTimeFormat().resolvedOptions().timeZone)]]) + `<div class="row wrap mt16"><button class="btn bad sm" data-act="logout">${ic('logout', 14)}Log out</button></div><div class="fs12 muted mt12">Authorized emails:</div><div class="col mt4" style="gap:4px">${ALLOWED_ADMIN_EMAILS.map(e => `<span class="mono fs12">${esc(e)}</span>`).join('')}</div>`);
  setHTML($('#st-app'), kv([['Firebase project', esc(firebaseConfig.projectId)], ['Collections', 'users, tasks, offers, submissions, withdrawals, broadcasts, notifications, adminLogs, adminMeta'], ['Date handling', 'All dates use this device’s timezone'], ['Panel', 'TASK GULLAK Admin v2']]));
  setHTML($('#st-roles'), [['Super Admin', 'Everything incl. settings & wallet adjustments'], ['Moderator / Approval Admin', 'Tasks, comments, approvals'], ['Payout Admin', 'Withdrawals and payout records'], ['Read-only viewer', 'View dashboards and reports only']].map(([r, d]) => `<div class="row between wrap" style="border:1px solid var(--line);border-radius:12px;padding:9px 12px"><div><div class="b fs13">${r}</div><div class="fs12 muted">${d}</div></div><span class="pill mute">Needs backend</span></div>`).join(''));
  setHTML($('#st-sec'), [['Sensitive actions need confirmation (reject, delete, block, payout)', 1], ['Audit log for approvals, payouts, wallet, maintenance, notifications', 1], ['Bank account numbers masked by default (reveal is logged)', 1], ['Payout & wallet changes use Firestore transactions (no double processing)', 1], ['Admin master password is stored in this page’s source — move login to Firebase Auth + Firestore rules', 0], ['Enforce roles & rate limits with Firestore Security Rules / Cloud Functions', 0]].map(([t, ok]) => `<div class="note ${ok ? 'ok' : 'warn'}">${ic(ok ? 'check' : 'alert', 16)}<div>${t}</div></div>`).join(''));
};


/* === js/views/logs.js === */
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


/* === js/views/devtools.js === */
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


/* === js/app.js === */
'use strict';
/* ============================================================
   INIT
   ============================================================ */
hydrateIcons();
document.addEventListener('visibilitychange', () => { if (!document.hidden && S.admin) renderAll(); });
setInterval(() => { if (S.admin && !document.hidden) { renderAll(); sweepExpired(); } }, 60000);
window.addEventListener('online', () => S.admin && renderAll());
window.addEventListener('offline', () => { const c = $('#conn'); if (c) { c.classList.add('off'); c.lastElementChild.textContent = 'Offline'; } });
