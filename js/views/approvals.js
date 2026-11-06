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
