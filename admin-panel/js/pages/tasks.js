'use strict';
/* Task Management + task editor */

/* ============================================================
   TASKS
   ============================================================ */
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
  const sm = tasksSubsMap(), t0 = new Date().setHours(0, 0, 0, 0), rg = { from: t0, to: t0 + DAY - 1 };   // task info = today only
  let list = S.tasks.filter(t => (f.status === 'all' || taskStatus(t) === f.status) && (!q || [taskName(t), t.id, t.appId, t.packageName, t.category].join(' ').toLowerCase().includes(q)));
  if (!S.live.tasks && !S.tasks.length) { setHTML($('#tasks-grid'), skelCards(4)); return; }
  const p = paginate(list, 'tasks', 12, f);
  setHTML($('#tasks-grid'), p.items.map(t => {
    const subs = (sm.get(t.id) || []).filter(x => subMs(x) >= rg.from && subMs(x) <= rg.to), pn = subs.filter(s => subSt(s) === 'pending').length, ap = subs.filter(s => subSt(s) === 'approved').length, rj = subs.filter(s => subSt(s) === 'rejected').length;
    const st = taskStatus(t), liability = pn * taskReward(t), blocked = new Set((t.blockedUserIds || []).map(canon)).size;
    return `<div class="card tcard">
      <div class="row" style="align-items:flex-start;gap:10px"><img class="appico" src="${esc(taskIcon(t))}" alt="" loading="lazy">
        <div class="grow" style="min-width:0"><div class="row between" style="gap:6px"><h4 class="b fs16 trunc">${esc(taskName(t))}</h4>${TASK_PILL[st]}</div>
          <div class="row wrap gap6 fs12 mt4 tmeta"><span class="brand-t xb">${inr(taskReward(t))}</span><span class="mchip rate">${ic('star', 12)}${esc(String(t.appRating || t.rating || '—'))}</span><span class="mchip">${ic('download', 12)}${esc(String(t.appDownloads || t.downloads || '—'))}</span><span class="mchip">${ic('clock', 12)}${num(t.durationMinutes) || 15}m</span></div>
          <div class="row wrap gap6 mt6"><span class="tag mono">ID ${esc(t.id.slice(0, 8))}</span><span class="tag">${({ normal: 'Normal', bulk: 'Bulk', both: 'Normal + Bulk' })[t.taskMode || 'normal']}</span><span class="tag tgi">${ic('file', 11)}${(t.comments || []).length} comments</span><span class="tag tgi">${ic('ban', 11)}${blocked} blocked</span></div></div></div>
      <div class="mini" style="grid-template-columns:repeat(4,1fr)"><div><b>${subs.length}</b>Submitted</div><div><b>${subs.filter(x => shotOf(x)).length}</b>Screenshots</div><div><b>${pn}</b>Pending</div><div><b>${ap}</b>Approved</div></div>
      <div class="row between wrap fs12"><span class="muted">${rj} rejected</span><span class="b">Pending reward liability: <span class="brand-t">${inr(liability)}</span></span></div>
      <div class="row wrap tcact" style="border-top:1px solid var(--line);padding-top:10px;gap:6px">
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
  const t = S.tasks.find(x => x.id === el.dataset.id);
  go('approvals', { range: 'all', status: 'all', q: t ? taskName(t) : '' });
};
async function setTaskStatus(id, newStatus) {
  const active = newStatus === 'active';
  const patch = { active, isActive: active, status: newStatus, updatedAt: serverTimestamp(), updatedBy: S.admin };
  await updateDoc(doc(db, 'tasks', id), patch);
  try { await updateDoc(doc(db, 'offers', id), patch); } catch (_) { }
  logAction('task_status', id, { status: newStatus });
}
/* A task lives only on the day it was added: from the next day it is deleted automatically (submissions / approvals are kept). */
async function sweepOldTasks() {
  if (!S.admin || !S.live.tasks || S.clockOff === undefined) return;
  if (S.clockOff === null) return;                                              // server time unknown: never delete
  if (Math.abs(S.clockOff) > 300000) { if (!S.clockWarned) { S.clockWarned = true; toast(`This device’s clock is ${Math.round(Math.abs(S.clockOff) / 60000)} min off — auto-delete paused`, 'bad'); } return; }
  const t0 = new Date(serverNow()).setHours(0, 0, 0, 0), old = S.tasks.filter(t => { const c = tsMs(t.createdAt); return c > 0 && c < t0 && !sweepOldTasks.done.has(t.id); });
  if (!old.length) return;
  for (const t of old) {
    sweepOldTasks.done.add(t.id);
    try { await archiveTask(t); await deleteDoc(doc(db, 'tasks', t.id)); try { await deleteDoc(doc(db, 'offers', t.id)); } catch (_) { } logAction('task_auto_delete', t.id, { title: taskName(t) }); }
    catch (e) { sweepOldTasks.done.delete(t.id); console.warn('auto-delete', e.code || e.message); }
  }
  toast(`${old.length} old task${old.length > 1 ? 's' : ''} removed automatically`);
}
sweepOldTasks.done = new Set();
async function sweepOldBcasts() {
  if (!S.admin || !S.clockOff && S.clockOff !== 0 || Math.abs(S.clockOff) > 300000) return; const now = serverNow();
  const old = S.bcasts.filter(b => b.id !== 'system_maintenance_banner' && num(b.expiresAtMillis) > 0 && num(b.expiresAtMillis) < now && !sweepOldBcasts.done.has(b.id)).slice(0, 40);
  for (const b of old) { sweepOldBcasts.done.add(b.id); try { await deleteDoc(doc(db, 'broadcasts', b.id)); try { await deleteDoc(doc(db, 'notifications', b.id)); } catch (_) { } } catch (e) { sweepOldBcasts.done.delete(b.id); } }
}
sweepOldBcasts.done = new Set();
ACT.taskPause = async el => { await setTaskStatus(el.dataset.id, 'paused'); toast('Task paused'); };
ACT.taskActivate = async el => {
  const t = S.tasks.find(x => x.id === el.dataset.id); if (!t) return;
  const bl = new Set(t.blockedUserIds || []), open = prevUsersForApp(taskName(t), t.appId || t.packageName).filter(u => !idsOfUser(u.id).some(i => bl.has(i)));
  if (open.length && !await confirmBox('Go live?', `<b>${open.length}</b> user${open.length > 1 ? 's' : ''} already did this app and ${open.length > 1 ? 'are' : 'is'} not blocked. Tap Edit → “Users who already did this app” to block them first, or go live anyway.`, { ok: 'Go live anyway' })) return;
  await setTaskStatus(t.id, 'active'); toast('Task is live', 'ok');
};
ACT.taskArchive = async el => {
  const t = S.tasks.find(x => x.id === el.dataset.id); if (!t) return;
  if (!await confirmBox('Archive this task?', `<b>${esc(taskName(t))}</b> will be removed from the user app, but its data stays safe.`, { ok: 'Archive' })) return;
  await setTaskStatus(t.id, 'archived'); toast('Task archived');
};
ACT.taskDelete = async el => {
  const t = S.tasks.find(x => x.id === el.dataset.id); if (!t) return;
  if (!await confirmBox('Delete task permanently?', `<b>${esc(taskName(t))}</b> and its whole comment/feedback pool will be deleted. This cannot be undone. Archiving may be a better option.`, { ok: 'Delete task', danger: true })) return;
  await archiveTask(t);
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
/* one person = one entry; blocking one login method blocks every id of that person (Google uid + email/password doc) */
function prevUsersForApp(title, appId) {
  const tl = String(title || '').trim().toLowerCase();
  const same = S.tasks.filter(t => (appId && (t.appId === appId || t.packageName === appId)) || (tl && taskName(t).trim().toLowerCase() === tl));
  const ids = new Set(same.map(t => t.id)), m = new Map();
  const add = (uid, name, st) => { if (!uid) return; const c = canon(uid), u = userInfo(uid); let e = m.get(c); if (!e) m.set(c, e = { id: c, name: (u && (u.fullName || u.email)) || name || c, email: (u && u.email) || '', n: 0, st: {} }); if (st) { e.n++; e.st[st] = (e.st[st] || 0) + 1; } };
  S.subs.forEach(x => { if (ids.has(x.taskId) || (tl && String(x.taskTitle || '').trim().toLowerCase() === tl)) add(x.userId, x.userName || x.userEmail, subSt(x)); });
  same.forEach(t => (t.submittedUserIds || []).forEach(u => add(u)));
  ((S.arch || {})[appId] || []).forEach(a => (a.submittedUserIds || []).forEach(u => add(u)));
  return [...m.values()].sort((a, b) => b.n - a.n);
}
const pkgOf = url => { const t = String(url || '').trim(); let m = t.match(/[?&]id=([a-zA-Z0-9._]+)/); if (m) return m[1]; m = t.match(/^[a-zA-Z][a-zA-Z0-9_]*(?:\.[a-zA-Z0-9_]+)+$/); return m ? m[0] : ''; };
function openTaskModal(t) {
  $('#tm-id').value = t ? t.id : '';
  S.tm.mode = (t && t.taskMode) || 'normal'; S.tm.more = false; $('#tm-more').checked = false; $('#tm-morewrap').classList.toggle('hidden', !!t); $('#tm-bulkmax').value = (t && t.bulkMaxComments) || 5; syncTmMode();
  $('#tm-heading').textContent = t ? 'Edit Task' : 'Add New Task';
  $('#tm-url').value = t ? (t.storeUrl || t.actionUrl || t.appLink || '') : '';
  $('#tm-title').value = t ? taskName(t) : '';
  $('#tm-rating').value = t ? (t.appRating || t.rating || '') : '';
  $('#tm-downloads').value = t ? (t.appDownloads || t.downloads || '') : '';
  const icon = t ? (t.iconUrl || t.appIcon || t.taskPfpBase64 || '') : '';
  $('#tm-icon-url').value = icon; $('#tm-icon-preview').src = icon || PLAY_ICON;
  $('#tm-reward').value = t ? (taskReward(t) || 10) : 10;
  $('#tm-duration').value = t ? (num(t.durationMinutes) || 15) : 15;
  $('#tm-category').value = t ? (t.category || 'App') : 'App';
  { const cur = t ? taskStatus(t) : 'draft', sel = $('#tm-status');
    sel.innerHTML = (cur === 'active' ? '<option value="active">Live</option>' : '') + [['draft', 'Draft'], ['paused', 'Paused'], ['archived', 'Archived']].map(([v, l]) => `<option value="${v}">${l}</option>`).join('');
    sel.value = cur; }
  $('#tm-instructions').value = (t && (t.instructions || []).length ? t.instructions : DEFAULT_INSTRUCTIONS).join('\n');
  $('#tm-comments').value = t ? (t.comments || []).join('\n') : '';
  $('#tm-blocked').value = t ? (t.blockedUserIds || []).join(', ') : '';
  $('#tm-error').textContent = ''; $('#tm-fetch-status').textContent = 'Paste the link — app name, icon, rating and downloads are filled in automatically. If it fails, enter them manually or upload an icon.';
  $('#task-modal').classList.add('show');
    refreshCompleted(); updateTaskPreview();
}
function syncTmMode() {
  const m = S.tm.mode || 'normal';
  ['normal', 'bulk', 'both'].forEach(k => $('#tm-m-' + k).classList.toggle('on', m === k));
  $('#tm-bulkcfg').classList.toggle('hidden', m === 'normal');
}
ACT.tmMode = el => { S.tm.mode = el.dataset.k; syncTmMode(); };
INP.tmMore = el => { S.tm.more = el.checked; };
function tmResetMore() {
  ['#tm-url', '#tm-title', '#tm-icon-url', '#tm-rating', '#tm-downloads', '#tm-category', '#tm-comments', '#tm-blocked'].forEach(i => { const e = $(i); if (e) e.value = ''; });
  $('#tm-icon-preview').src = PLAY_ICON; $('#tm-fetch-status').textContent = 'Saved. Add the next one.';
  try { refreshCompleted(); updateTaskPreview(); } catch (_) { } $('#tm-url').focus();
}
function closeTask() { $('#task-modal').classList.remove('show'); }
ACT.openTask = () => openTaskModal(null);
ACT.editTask = el => openTaskModal(S.tasks.find(x => x.id === el.dataset.id));
ACT.closeTask = closeTask;
function updateTaskPreview() {
  const title = $('#tm-title').value.trim() || 'App name', reward = num($('#tm-reward').value), dur = num($('#tm-duration').value) || 15;
  const steps = lines($('#tm-instructions').value).slice(0, 5), st = $('#tm-status').value;
  setHTML($('#tm-preview'), `<div class="fs11 muted" style="text-align:center;margin-bottom:8px">Task card${st !== 'active' ? ` <span class="pill mute">${st.toUpperCase()}</span>` : ''}</div>
    <div class="card"><div class="row"><img class="appico" src="${esc($('#tm-icon-url').value || PLAY_ICON)}" alt=""><div class="grow"><div class="b fs13 trunc">${esc(title)}</div><div class="fs11 muted">${esc($('#tm-category').value || 'App')} • ${dur} min</div></div><span class="pill ok">${inr(reward)}</span></div>
    <ol style="margin:10px 0 0;padding-left:18px;font-size:11.5px;color:var(--ink2);line-height:1.5">${steps.map(s => `<li>${esc(s).replaceAll('{app}', esc(title))}</li>`).join('')}</ol>
    <div class="btn primary" style="width:100%;margin-top:10px;pointer-events:none;font-size:12px">Start task</div></div>`);
}
let fetchTimer = 0;
INP.tmPrev = (el) => { updateTaskPreview(); if (el.id === 'tm-title') refreshCompleted(); };
INP.tmUrl = el => { refreshCompleted(); clearTimeout(fetchTimer); if (pkgOf(el.value)) fetchTimer = setTimeout(fetchPlayMeta, 500); };
INP.tmIconFile = el => {
  const f = el.files && el.files[0]; if (!f) return; const img = new Image(), url = URL.createObjectURL(f);
  img.onload = () => { const c = document.createElement('canvas'); c.width = c.height = 128; c.getContext('2d').drawImage(img, 0, 0, 128, 128); const d = c.toDataURL('image/png'); $('#tm-icon-url').value = d; $('#tm-icon-preview').src = d; URL.revokeObjectURL(url); updateTaskPreview(); toast('Icon added', 'ok'); };
  img.onerror = () => toast('Could not read that image', 'bad'); img.src = url; el.value = '';
};
INP.tmBlocked = () => refreshCompleted();
ACT.tmFetch = () => fetchPlayMeta();
ACT.tmToggleUsers = () => { S.tm.showCompleted = !S.tm.showCompleted; refreshCompleted(); };
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
  st.textContent = 'Fetching name, icon, rating & downloads…';
  try {
    const html = await fetchHtml(link + '&hl=en&gl=IN', pkg);
    if (tok !== metaTok) return;
    const { appName, iconUrl, rating, downloads, category } = parsePlay(html, pkg);
    if (category && !$('#tm-category').value.trim()) $('#tm-category').value = category;
    if (appName) $('#tm-title').value = appName;
    $('#tm-rating').value = rating || $('#tm-rating').value; $('#tm-downloads').value = downloads || $('#tm-downloads').value;
    if (iconUrl) { $('#tm-icon-url').value = iconUrl; $('#tm-icon-preview').src = iconUrl; }
    const miss = [!iconUrl && 'icon', !rating && 'rating', !downloads && 'downloads'].filter(Boolean);
    st.textContent = `Fetched: ${appName} (${rating || '—'}• ${downloads || '—'})` + (miss.length ? ` — ${miss.join(', ')} not found, please fill manually.` : '');
    refreshCompleted(); updateTaskPreview();
  } catch (e) {
    if (tok !== metaTok) return;
    if (!$('#tm-title').value) $('#tm-title').value = pkg.split('.').pop().replace(/[_-]+/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
    st.textContent = 'Could not reach the Play Store through the public proxies. Tap Fetch to retry, or enter the details manually. (For a permanent fix, deploy play-proxy-worker.js and set PLAY_PROXY.)'; updateTaskPreview();
  }
}
function modalPrevUsers() { return prevUsersForApp($('#tm-title').value.trim(), pkgOf($('#tm-url').value)); }
const blockedSet = () => new Set($('#tm-blocked').value.split(',').map(x => x.trim()).filter(Boolean));
function updUcount(users) {
  users = users || modalPrevUsers(); const b = blockedSet(), n = users.filter(u => idsOfUser(u.id).some(i => b.has(i))).length;
  $('#tm-ucount').textContent = `${users.length} user${users.length === 1 ? '' : 's'} • ${n} to block`;
}
async function loadArchive(appId) {
  S.arch = S.arch || {}; if (!appId || S.arch[appId] !== undefined) return; S.arch[appId] = [];
  try { const sn = await getDocs(query(collection(db, 'taskArchive'), where('appId', '==', appId))); S.arch[appId] = sn.docs.map(d => d.data()); if ($('#task-modal').classList.contains('show')) refreshCompleted(); } catch (_) { }
}
function refreshCompleted() {
  const box = $('#tm-completed'); loadArchive(pkgOf($('#tm-url').value));
  if (!S.tm.showCompleted) { setHTML(box, '<div class="fs12 muted">Hidden.</div>'); return; }
  if (!$('#tm-title').value.trim() && !pkgOf($('#tm-url').value)) { setHTML(box, '<div class="fs12 muted">Paste the app link or enter the app name to see who has already done it.</div>'); $('#tm-ucount').textContent = ''; return; }
  const users = modalPrevUsers(), b = blockedSet();
  updUcount(users);
  if (!users.length) { setHTML(box, '<div class="fs12 muted">No user has done this app yet.</div>'); return; }
  setHTML(box, users.slice(0, 300).map(u => `<label class="row" style="padding:7px 10px;border:1px solid var(--line);border-radius:10px;background:var(--panel);cursor:pointer"><input type="checkbox" class="chk" data-in="tmBlockUid" data-uid="${esc(u.id)}" ${idsOfUser(u.id).some(i => b.has(i)) ? 'checked' : ''}><span class="grow fs13 b trunc" style="min-width:0">${esc(u.name)}</span></label>`).join(''));
}
INP.tmBlockUid = el => {
  const input = $('#tm-blocked'), set = blockedSet();
  idsOfUser(el.dataset.uid).forEach(i => el.checked ? set.add(i) : set.delete(i)); input.value = [...set].join(', '); updUcount();
};
ACT.tmSelAll = () => { const set = blockedSet(); modalPrevUsers().forEach(u => idsOfUser(u.id).forEach(i => set.add(i))); $('#tm-blocked').value = [...set].join(', '); refreshCompleted(); };
ACT.tmSelNone = () => { const set = blockedSet(); modalPrevUsers().forEach(u => idsOfUser(u.id).forEach(i => set.delete(i))); $('#tm-blocked').value = [...set].join(', '); refreshCompleted(); };
async function tmSaveOne(opts = {}) {
  const err = $('#tm-error'); err.textContent = '';
  const existingId = $('#tm-id').value.trim(), existing = S.tasks.find(t => t.id === existingId);
  const url = $('#tm-url').value.trim();
  if (url && !/^https?:\/\/play\.google\.com\/store\/apps\/details\?(?:[^#]*&)?id=[a-zA-Z0-9._]+/i.test(url)) { err.textContent = 'Invalid link: enter a Google Play Store app link (play.google.com/store/apps/details?id=…).'; return; }
  let title = $('#tm-title').value.trim();
  if (!title && url) title = pkgOf(url).split('.').pop() || '';
  if (!title) { err.textContent = 'An app name or a valid Play Store link is required.'; return; }
  const reward = Math.max(1, parseInt($('#tm-reward').value || '0', 10) || 0), duration = Math.max(1, parseInt($('#tm-duration').value || '0', 10) || 15);
  if (!reward) { err.textContent = 'Reward must be ₹1 or more.'; return; }
  const hideAfterComplete = !(existing && existing.hideAfterComplete === false), reshow = false, status = $('#tm-status').value, active = status === 'active';
  const appId = pkgOf(url) || title.toLowerCase().replace(/\W+/g, '_');
  if (!existingId && !opts.skipDup && S.tasks.some(t => (t.appId || t.packageName) === appId && taskStatus(t) !== 'archived')) {
    if (!await confirmBox('Duplicate task?', 'A task for this app already exists. Create a new one anyway?', { ok: 'Create anyway' })) return;
  }
  const instructions = lines($('#tm-instructions').value), rawTexts = lines($('#tm-comments').value), seenC = new Set(), texts = rawTexts.filter(x => { const k = x.normalize('NFC').toLowerCase().replace(/[^\p{L}\p{N}\s]/gu, '').replace(/\s+/g, ' ').trim(); if (!k || seenC.has(k)) return false; seenC.add(k); return true; });
  if (rawTexts.length !== texts.length) { $('#tm-comments').value = texts.join('\n'); toast(`${rawTexts.length - texts.length} duplicate or near-same comment${rawTexts.length - texts.length > 1 ? 's' : ''} skipped`); }
  const blockedUserIds = $('#tm-blocked').value.split(',').map(x => x.trim()).filter(Boolean);
  const ratingStr = $('#tm-rating').value.trim(), downloadsStr = $('#tm-downloads').value.trim(), iconUrl = $('#tm-icon-url').value.trim(), category = $('#tm-category').value.trim() || 'App';
  const docRef = existingId ? doc(db, 'tasks', existingId) : doc(collection(db, 'tasks'));
  const ins = instructions.length ? instructions : DEFAULT_INSTRUCTIONS;
  const tMode = S.tm.mode || 'normal', bulkMax = Math.max(2, Math.min(50, Math.floor(Number($('#tm-bulkmax').value)) || 5));
  const payload = {
    id: docRef.id, taskId: docRef.id, title, appName: title, subtitle: url || `Earn Reward • ${title}`, storeUrl: url, actionUrl: url, appLink: url, appId, packageName: appId,
    appRating: ratingStr, rating: parseFloat(ratingStr) || 4.5, appDownloads: downloadsStr, downloads: downloadsStr, installs: downloadsStr, blockedUserIds,
    iconUrl, appIcon: iconUrl, taskPfpBase64: iconUrl, referenceScreenshotUrl: existing?.referenceScreenshotUrl || '', exampleProofBase64: existing?.exampleProofBase64 || '',
    category, rewardInr: reward, rewardAmount: reward, reward, durationMinutes: duration, timerMinutes: duration, deadlineHours: existing?.deadlineHours || 24, maxSubmissions: 0, hideAfterComplete, oneTimePerUser: hideAfterComplete, ...(reshow ? { reshowAt: Date.now(), submittedUserIds: [] } : {}),
    instructions: ins, description: ins.join('\n'), proofRequirement: existing?.proofRequirement || 'Upload completion screenshot and your genuine feedback',
    active, isActive: active, pausedByMaintenance: false, status,
    taskMode: tMode, normalEnabled: tMode !== 'bulk', bulkEnabled: tMode !== 'normal', bulkMaxComments: tMode === 'normal' ? 0 : bulkMax,
    comments: texts, commentPool: texts, commentItems: buildCommentItems(docRef.id, title, appId, texts, existing?.commentItems), updatedAt: serverTimestamp(), updatedBy: S.admin
  };
  if (!existingId) { payload.createdAt = serverTimestamp(); payload.createdBy = S.admin; payload.expiresAtMillis = new Date(serverNow()).setHours(23, 59, 59, 999); }   // user app can hide tasks after this time
  const btn = $('#tm-save'); btn.disabled = true; let okSave = false;
  try {
    await setDoc(docRef, payload, { merge: true });
    try { await setDoc(doc(db, 'offers', docRef.id), payload, { merge: true }); } catch (_) { }
    logAction(existingId ? 'task_edit' : 'task_create', docRef.id, { title, reward, status });
    archiveTask({ ...payload, id: docRef.id, createdAt: existing ? existing.createdAt : Date.now(), submittedUserIds: existing?.submittedUserIds || [] });
    okSave = true; if (!opts.keepOpen) closeTask(); if (!opts.quiet && !opts.keepOpen) toast(`Task “${title}” saved (${inr(reward)}, ${status === 'active' ? 'LIVE' : status})`, 'ok');
  } catch (e) { err.textContent = 'Save failed: ' + (e.code || e.message); }
  btn.disabled = false;
  return okSave;
}
ACT.tmSave = async () => {
  const ok = await tmSaveOne({ keepOpen: S.tm.more });
  if (ok && S.tm.more) { toast('Task saved', 'ok'); tmResetMore(); }
};
