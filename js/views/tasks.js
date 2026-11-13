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
const TASK_PILL = { active: '<span class="pill ok">LIVE</span>', paused: '<span class="pill mute">PAUSED</span>', draft: '<span class="pill info">DRAFT</span>', archived: '<span class="pill mute">ARCHIVED</span>', exhausted: '<span class="pill bad">EXHAUSTED</span>' };
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
    const mType = (t.modeType || t.mode_type || (t.isBulk ? 'BULK' : 'NORMAL')).toUpperCase();
    const modeBadge = mType === 'BOTH' ? '<span class="pill info">BOTH (NORMAL + BULK)</span>' : mType === 'BULK' ? '<span class="pill brand">BULK ONLY</span>' : '<span class="pill mute">NORMAL ONLY</span>';
    const totalQty = num(t.totalQuantity || t.total_quantity || 50);
    const usedQty = num(t.usedQuantity || t.used_quantity || 0);
    const remQty = Math.max(0, num(t.remainingQuantity || t.remaining_quantity || (totalQty - usedQty)));
    const stockBadge = remQty === 0 ? '<span class="pill bad">STOCK EXHAUSTED (0 Left)</span>' : `<span class="pill ok">Stock: ${remQty}/${totalQty}</span>`;
    const bulkMin = num(t.bulkMinQuantity || t.bulk_min_quantity || 1);
    const bulkMax = num(t.bulkMaxQuantity || t.bulk_max_quantity || 5);
    const bulkLimitsTag = (mType === 'BULK' || mType === 'BOTH') ? `<span class="tag brand">Min ${bulkMin} • Max ${bulkMax}</span><span class="tag warn">Timer: Disabled</span>` : '';

    return `<div class="card tcard">
      <div class="row" style="align-items:flex-start;gap:12px"><img class="appico" src="${esc(taskIcon(t))}" alt="" loading="lazy">
        <div class="grow" style="min-width:0"><div class="row between wrap" style="gap:6px"><h4 class="b fs16 trunc">${esc(taskName(t))}</h4><div class="row gap6">${modeBadge}${TASK_PILL[st] || TASK_PILL.active}</div></div>
          <div class="row wrap gap6 fs12 mt4 tmeta"><span class="brand-t xb">${inr(taskReward(t))}</span><span class="mchip rate">${ic('star', 12)}${esc(String(t.appRating || t.rating || '—'))}</span><span class="mchip">${ic('download', 12)}${esc(String(t.appDownloads || t.downloads || '—'))}</span><span class="mchip">${ic('clock', 12)}${mType === 'BULK' ? 'No Timer' : (num(t.durationMinutes) || 15) + 'm'}</span>${stockBadge}</div>
          <div class="row wrap gap6 mt8"><span class="tag mono">ID ${esc(t.id.slice(0, 8))}</span>${bulkLimitsTag}<span class="tag tgi">${ic('file', 11)}${(t.comments || []).length} comments</span><span class="tag tgi">${ic('ban', 11)}${(t.blockedUserIds || []).length} blocked</span><span class="tag tgi" title="Users who already submitted — this task is hidden from them">${ic('check', 11)}${(t.submittedUserIds || []).length} submitted</span><span class="tag">${fmtD(tsMs(t.createdAt))}</span></div></div></div>
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

  const mType = t ? (t.modeType || t.mode_type || (t.isBulk ? 'BULK' : 'NORMAL')).toUpperCase() : 'NORMAL';
  const bulkMin = t ? (num(t.bulkMinQuantity || t.bulk_min_quantity) || 1) : 1;
  const bulkMax = t ? (num(t.bulkMaxQuantity || t.bulk_max_quantity) || 5) : 5;
  const totalQty = t ? (num(t.totalQuantity || t.total_quantity) || 50) : 50;

  if ($('#tm-mode-type')) $('#tm-mode-type').value = mType;
  if ($('#tm-bulk-min')) $('#tm-bulk-min').value = bulkMin;
  if ($('#tm-bulk-max')) $('#tm-bulk-max').value = bulkMax;
  if ($('#tm-total-quantity')) $('#tm-total-quantity').value = totalQty;
  if ($('#tm-bulk-wrap')) $('#tm-bulk-wrap').style.display = (mType === 'BULK' || mType === 'BOTH') ? 'flex' : 'none';
  if ($('#tm-duration')) $('#tm-duration').disabled = (mType === 'BULK');

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
INP.tmModeType = el => {
  const m = (el.value || 'NORMAL').toUpperCase();
  if ($('#tm-bulk-wrap')) $('#tm-bulk-wrap').style.display = (m === 'BULK' || m === 'BOTH') ? 'flex' : 'none';
  if ($('#tm-duration')) $('#tm-duration').disabled = (m === 'BULK');
};
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

  const modeType = ($('#tm-mode-type')?.value || 'NORMAL').toUpperCase();
  const bulkMin = Math.max(1, parseInt($('#tm-bulk-min')?.value || '1', 10) || 1);
  const bulkMax = Math.max(bulkMin, parseInt($('#tm-bulk-max')?.value || '5', 10) || 5);
  const totalQty = Math.max(1, parseInt($('#tm-total-quantity')?.value || '50', 10) || 50);
  const usedQty = existing ? Math.max(0, num(existing.usedQuantity || existing.used_quantity || 0)) : 0;
  const remainingQty = Math.max(0, totalQty - usedQty);

  const onceMode = $('#tm-once').value, hideAfterComplete = onceMode !== 'keep', reshow = onceMode === 'reshow' && !!existingId;
  let status = $('#tm-status').value;
  if (remainingQty <= 0 && status === 'active') {
    status = 'exhausted';
  }
  const active = status === 'active';
  const isBulk = modeType === 'BULK' || modeType === 'BOTH';
  const timerEnabled = modeType === 'BULK' ? false : (existing?.timerEnabled ?? true);

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
    isBulk, modeType, mode_type: modeType,
    bulkMinQuantity: bulkMin, bulk_min_quantity: bulkMin,
    bulkMaxQuantity: bulkMax, bulk_max_quantity: bulkMax,
    totalQuantity: totalQty, total_quantity: totalQty,
    usedQuantity: usedQty, used_quantity: usedQty,
    remainingQuantity: remainingQty, remaining_quantity: remainingQty,
    timerEnabled, timer_enabled: timerEnabled,
    comments: texts, commentPool: texts, commentItems: buildCommentItems(docRef.id, title, appId, texts, existing?.commentItems), updatedAt: serverTimestamp(), updatedBy: S.admin
  };
  if (!existingId) { payload.createdAt = serverTimestamp(); payload.createdBy = S.admin; }
  const btn = $('#tm-save'); btn.disabled = true;
  try {
    await setDoc(docRef, payload, { merge: true });
    try { await setDoc(doc(db, 'offers', docRef.id), payload, { merge: true }); } catch (_) { }
    logAction(existingId ? 'task_edit' : 'task_create', docRef.id, { title, reward, status, modeType, remainingQty });
    closeTask(); toast(`Task “${title}” saved (${inr(reward)}, ${status === 'active' ? 'LIVE' : status})`, 'ok');
  } catch (e) { err.textContent = 'Save failed: ' + (e.code || e.message); }
  btn.disabled = false;
};
