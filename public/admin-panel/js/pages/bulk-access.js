'use strict';
/* Bulk Access: users ask, admin activates or rejects */

/* ---------- Bulk access: a user asks, the admin activates or rejects ---------- */
function bulkRows() {
  const m = new Map();
  const row = uid => { const c = canon(uid); let r = m.get(c); if (!r) m.set(c, r = { uid: c, req: null }); return r; };
  (S.bulkReq || []).forEach(q => { const uid = q.userId || q.uid || q.id; if (!uid) return; const r = row(uid); if (!r.req) r.req = q; });
  (S.users || []).forEach(u => { if (u && u.id) row(u.id); });
  return [...m.values()].map(r => {
    const u = userInfo(r.uid), q = r.req; r.user = u;
    r.name = (u && (u.fullName || u.userName || u.name || u.email)) || (q && (q.userName || q.userEmail)) || r.uid;
    r.email = (u && u.email) || (q && q.userEmail) || '';
    const uSt = String((u && u.bulkStatus) || '').toLowerCase();
    const qSt = String((q && q.status) || '').toLowerCase();
    const qType = String((q && (q.type || q.requestType)) || '').toLowerCase();

    const isExit = qType === 'exit' || qSt === 'requested_exit' || uSt === 'requested_exit';
    const isActive = !isExit && ((u && u.bulkMode === true) || uSt === 'active' || uSt === 'approved' || qSt === 'approved');
    const isPending = !isActive && (isExit || qSt === 'pending' || uSt === 'pending' || uSt === 'requested');
    const isRejected = !isActive && !isPending && (['rejected', 'revoked'].includes(qSt) || ['rejected', 'revoked'].includes(uSt));
    r.status = isActive ? 'active' : isPending ? 'pending' : isRejected ? 'rejected' : 'available';
    r.isExit = isExit;
    return r;
  }).sort((a, b) => {
    if (a.status === 'pending' && b.status !== 'pending') return -1;
    if (b.status === 'pending' && a.status !== 'pending') return 1;
    return tsMs(b.req && b.req.createdAt) - tsMs(a.req && a.req.createdAt) || String(a.name).localeCompare(String(b.name));
  });
}
const bkIds = uid => { const ids = idsOfUser(uid).filter(id => S.usersRaw.some(u => u.id === id)); return ids.length ? ids : [uid]; };
async function bkSet(uid, userPatch, reqPatch, statuses) {
  await Promise.all(bkIds(uid).map(id => setDoc(doc(db, 'users', id), userPatch, { merge: true })));
  const reqs = (S.bulkReq || []).filter(q => canon(q.userId) === canon(uid) && statuses.includes(q.status));
  await Promise.all(reqs.map(q => updateDoc(doc(db, 'bulkRequests', q.id), reqPatch)));
}
ACT.bkTab = el => { S.bkTab = el.dataset.k; renderAll(); };
ACT.bkApprove = async el => {
  const uid = el.dataset.uid, r = bulkRows().find(x => x.uid === uid);
  try { await bkSet(uid, { bulkMode: true, bulkStatus: 'active', bulkApprovedAt: serverTimestamp(), bulkApprovedBy: S.admin, bulkRejectReason: '' }, { status: 'approved', reviewedBy: S.admin, updatedAt: serverTimestamp() }, ['pending', 'approved', 'requested_exit']); logAction('bulk_access_on', uid, { name: r && r.name }); toast(`Bulk mode is active for ${r ? r.name : 'user'}`, 'ok'); }
  catch (e) { toast('Failed: ' + (e.code || e.message), 'bad'); }
};
ACT.bkAcceptExit = async el => {
  const uid = el.dataset.uid, r = bulkRows().find(x => x.uid === uid);
  if (!await confirmBox('Accept Exit Request?', `${esc(r ? r.name : 'This user')} will exit Bulk Mode and return to Normal Mode.`, { ok: 'Accept Exit' })) return;
  try { await bkSet(uid, { bulkMode: false, bulkStatus: 'none', bulkApprovedLimit: 0, bulkRejectReason: '' }, { status: 'approved', reviewedBy: S.admin, updatedAt: serverTimestamp() }, ['pending', 'requested_exit', 'approved']); logAction('bulk_access_exit_accept', uid, { name: r && r.name }); toast(`Exit request accepted. ${r ? r.name : 'User'} is now a Normal User.`, 'ok'); }
  catch (e) { toast('Failed: ' + (e.code || e.message), 'bad'); }
};
ACT.bkReject = async el => {
  const uid = el.dataset.uid, r = bulkRows().find(x => x.uid === uid);
  const reason = await rejectDialog(`Reject request for ${r ? r.name : 'this user'}?`, '<p class="fs12 muted">Request will be rejected.</p>'); if (!reason) return;
  try { await bkSet(uid, { bulkMode: false, bulkStatus: 'rejected', bulkRejectReason: reason }, { status: 'rejected', adminNote: reason, reviewedBy: S.admin, updatedAt: serverTimestamp() }, ['pending', 'requested_exit']); logAction('bulk_access_reject', uid, { name: r && r.name, reason }); toast('Rejected.', 'ok'); }
  catch (e) { toast('Failed: ' + (e.code || e.message), 'bad'); }
};
ACT.bkRevoke = async el => {
  const uid = el.dataset.uid, r = bulkRows().find(x => x.uid === uid);
  if (!await confirmBox('Turn off Bulk mode?', `${esc(r ? r.name : 'This user')} will no longer see Bulk mode.`, { ok: 'Turn off', danger: true })) return;
  try { await bkSet(uid, { bulkMode: false, bulkStatus: 'revoked' }, { status: 'revoked', reviewedBy: S.admin, updatedAt: serverTimestamp() }, ['pending', 'approved', 'requested_exit']); logAction('bulk_access_off', uid, { name: r && r.name }); toast('Bulk mode turned off', 'ok'); }
  catch (e) { toast('Failed: ' + (e.code || e.message), 'bad'); }
};
RENDER.bulk = () => {
  const rows = bulkRows(), f = S.bkTab || 'pending', by = k => rows.filter(r => r.status === k);
  setHTML($('#bk-tabs'), [
    ['pending', 'Pending'],
    ['active', 'Active'],
    ['rejected', 'Rejected'],
    ['available', 'All Users']
  ].map(([k, l]) => `<button class="tab ${f === k ? 'on' : ''}" data-act="bkTab" data-k="${k}">${l} <span class="fs11">${by(k).length}</span></button>`).join(''));
  if (loadGate('bulkRequests', rows.length, LOADER(), $('#bk-list'))) return;
  const list = by(f), pill = {
    pending: '<span class="pill warn">Pending</span>',
    active: '<span class="pill ok">Active</span>',
    rejected: '<span class="pill bad">Not allowed</span>',
    available: '<span class="pill mute">Normal</span>'
  };
  setHTML($('#bk-list'), list.map(r => {
    const q = r.req, uid = esc(r.uid);
    const act = r.isExit
      ? `<button class="btn okfill sm" data-act="bkAcceptExit" data-uid="${uid}">Accept Exit Request</button><button class="btn bad sm" data-act="bkReject" data-uid="${uid}">Reject</button>`
      : f === 'active'
      ? `<button class="btn bad sm" data-act="bkRevoke" data-uid="${uid}">Turn off</button>`
      : `<button class="btn okfill sm" data-act="bkApprove" data-uid="${uid}">Activate Bulk Mode</button>`;
    const pillBadge = r.isExit ? '<span class="pill warn">Exit Requested</span>' : pill[r.status];
    return `<div class="card"><div class="row" style="gap:10px;align-items:flex-start"><span class="avatar" style="background:${avc(r.name)}">${esc(String(r.name)[0].toUpperCase())}</span><div class="grow" style="min-width:0"><div class="b fs14 trunc">${esc(r.name)}</div><div class="fs12 muted trunc">${esc(r.email)}</div>${q ? `<div class="fs12 muted">${r.isExit ? 'Exit Requested' : 'Requested'} ${fmtDT(tsMs(q.createdAt))}</div>` : ''}${q && q.note ? `<div class="fs12" style="margin-top:4px">${esc(q.note)}</div>` : ''}${f === 'rejected' && q && q.adminNote ? `<div class="fs12 muted" style="margin-top:4px">Reason: ${esc(q.adminNote)}</div>` : ''}</div>${pillBadge}</div><div class="row gap6 mt8">${act}</div></div>`;
  }).join('') || `<div class="card">${emptyHTML('bulk', f === 'pending' ? 'No pending requests waiting.' : f === 'active' ? 'No user has Bulk mode active yet.' : f === 'rejected' ? 'No rejected users.' : 'No registered users found.')}</div>`);
};

function startBulkListener() {
  listen('bulkRequests', collection(db, 'bulkRequests'), snap => { S.bulkReq = snap.docs.map(d => ({ id: d.id, ...d.data() })).sort((x, y) => tsMs(y.createdAt) - tsMs(x.createdAt)); });
}
