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
