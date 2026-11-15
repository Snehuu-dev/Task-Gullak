'use strict';
/* ============================================================
   BULK ACCESS MANAGEMENT & ADMIN APPROVAL DASHBOARD
   ============================================================ */

const safeCanon = id => (typeof canon === 'function' ? canon(id) : id);

const findUser = uid => {
  const c = safeCanon(uid);
  return (S.users || []).find(u => u && (u.id === c || u.id === uid || (u._aliases && u._aliases.includes(uid))));
};

const getUserIds = uid => {
  const u = findUser(uid);
  if (u && Array.isArray(u._aliases) && u._aliases.length) {
    return [u.id, ...u._aliases];
  }
  return [uid];
};

function bulkRows() {
  const m = new Map();
  const row = uid => {
    const c = safeCanon(uid);
    let r = m.get(c);
    if (!r) m.set(c, (r = { uid: c, req: null }));
    return r;
  };

  const reqList = Array.isArray(S.bulkReq) ? S.bulkReq : [];
  const modReqList = Array.isArray(S.bulkModReq) ? S.bulkModReq : [];
  const ticketList = Array.isArray(S.tickets) ? S.tickets : [];
  const userList = Array.isArray(S.users) ? S.users : [];

  const resolveReqUid = q => {
    if (!q) return '';
    let uid = q.userId || q.uid || q.user_id;
    if (!uid && (q.userEmail || q.email)) {
      const em = String(q.userEmail || q.email).trim().toLowerCase();
      const match = userList.find(u => u && String(u.email || '').trim().toLowerCase() === em);
      if (match) uid = match.id;
    }
    if (!uid && q.id && !String(q.id).startsWith('mod_') && !String(q.id).startsWith('user_') && !String(q.id).startsWith('bulk_')) {
      const match = userList.find(u => u && u.id === q.id);
      if (match) uid = match.id;
    }
    return uid || q.id || '';
  };

  reqList.forEach(q => {
    if (!q) return;
    const uid = resolveReqUid(q);
    if (!uid) return;
    const r = row(uid);
    if (!r.req) r.req = q;
  });

  modReqList.forEach(q => {
    if (!q) return;
    const uid = resolveReqUid(q);
    if (!uid) return;
    const r = row(uid);
    if (!r.req) {
      r.req = {
        id: q.id || 'mod_' + uid,
        userId: uid,
        userName: q.userName || q.name || q.userEmail || '',
        userEmail: q.userEmail || q.email || '',
        note: q.reason || q.note || '',
        status: q.status || 'pending',
        createdAt: q.createdAt || q.updatedAt,
        requestData: { deviceCount: q.deviceCount || q.approvedLimit || 2, tier: 'Standard' }
      };
    }
  });

  ticketList.forEach(t => {
    if (!t || (!String(t.id || '').startsWith('bulk_') && String(t.category || '').toLowerCase() !== 'bulk')) return;
    const uid = t.userId || (String(t.id).startsWith('bulk_') ? String(t.id).replace('bulk_', '') : '');
    if (!uid) return;
    const r = row(uid);
    if (!r.req) {
      r.req = {
        id: t.id,
        userId: uid,
        userName: t.userName || t.userEmail || '',
        userEmail: t.userEmail || '',
        note: t.message || '',
        status: t.status === 'open' ? 'pending' : (t.status === 'closed' ? 'approved' : t.status),
        createdAt: t.createdAt || t.updatedAt,
        requestData: { deviceCount: 2, tier: 'Standard' }
      };
    }
  });

  userList.forEach(u => {
    if (!u) return;
    const st = String(u.bulkStatus || '').toLowerCase();
    if (u.bulkMode === true || st === 'pending' || st === 'active' || st === 'requested' || st === 'rejected') {
      const r = row(u.id);
      if (!r.req) {
        r.req = {
          id: 'user_' + u.id,
          userId: u.id,
          userName: u.fullName || u.username || u.name || '',
          userEmail: u.email || '',
          note: u.bulkRejectReason || 'Requested via Mobile App',
          status: u.bulkMode === true ? 'approved' : (st === 'active' ? 'approved' : (st === 'pending' || st === 'requested' ? 'pending' : st)),
          createdAt: u.updatedAt || u.createdAt,
          requestData: { deviceCount: u.bulkDeviceCount || 2, tier: 'Standard' }
        };
      }
    }
  });

  return [...m.values()]
    .map(r => {
      const u = findUser(r.uid), q = r.req;
      r.user = u;
      const rawName = (u && (u.fullName || u.username || u.name)) || (q && (q.userName || q.name)) || (u && u.email) || (q && q.userEmail) || r.uid || 'User';
      r.name = String(rawName).trim() || 'User';
      r.email = String((u && u.email) || (q && q.userEmail) || '').trim();

      const uStatus = String((u && u.bulkStatus) || '').toLowerCase();
      const qStatus = String((q && q.status) || '').toLowerCase();

      const isApproved = (u && u.bulkMode === true) || uStatus === 'active' || uStatus === 'approved' || qStatus === 'approved' || qStatus === 'active';
      const isRejected = !isApproved && (['rejected', 'revoked'].includes(qStatus) || ['rejected', 'revoked'].includes(uStatus));
      const isPending = !isApproved && !isRejected;

      r.status = isApproved ? 'active' : isPending ? 'pending' : isRejected ? 'rejected' : 'pending';
      return r;
    })
    .sort((a, b) => {
      const ta = tsMs(a.req && (a.req.createdAt || a.req.requestedAt || a.req.updatedAt)) || (a.user && tsMs(a.user.updatedAt)) || 0;
      const tb = tsMs(b.req && (b.req.createdAt || b.req.requestedAt || b.req.updatedAt)) || (b.user && tsMs(b.user.updatedAt)) || 0;
      return tb - ta;
    });
}

const bkIds = uid => {
  return getUserIds(uid);
};

/** Single-Item Updates + Instant User Notification Push */
async function bkSet(uid, userPatch, reqPatch, statuses, notifTitle, notifMessage, notifType = 'info') {
  const reqList = Array.isArray(S.bulkReq) ? S.bulkReq : [];
  const modReqList = Array.isArray(S.bulkModReq) ? S.bulkModReq : [];
  const allTargetUids = bkIds(uid);

  await Promise.all(allTargetUids.map(id => setDoc(doc(db, 'users', id), userPatch, { merge: true })));

  const reqs = reqList.filter(q => q && safeCanon(q.userId || q.uid) === safeCanon(uid) && statuses.includes(q.status));
  await Promise.all(reqs.map(q => updateDoc(doc(db, 'bulkRequests', q.id), reqPatch).catch(() => {})));

  const modReqs = modReqList.filter(q => q && safeCanon(q.userId || q.uid) === safeCanon(uid) && statuses.includes(q.status));
  await Promise.all(modReqs.map(q => updateDoc(doc(db, 'bulk_mod_requests', q.id), reqPatch).catch(() => {})));

  try {
    const tRef = doc(db, 'support_tickets', `bulk_${uid}`);
    await updateDoc(tRef, { status: userPatch.bulkMode ? 'closed' : 'rejected', updatedAt: serverTimestamp() });
  } catch (_) {}

  // Push real-time notification to User App (/users/{uid}/notifications, /broadcasts, and /notifications)
  if (notifTitle && notifMessage) {
    const nowMs = Date.now();
    for (const targetUid of allTargetUids) {
      const bRef = doc(collection(db, 'broadcasts'));
      const payload = {
        id: bRef.id,
        title: notifTitle,
        message: notifMessage,
        body: notifMessage,
        description: notifMessage,
        type: notifType,
        targetUserId: targetUid,
        userId: targetUid,
        active: true,
        isActive: true,
        read: false,
        isRead: false,
        timestamp: nowMs,
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
        sentBy: S.admin || 'admin'
      };
      try { await setDoc(bRef, payload); } catch (_) {}
      try { await setDoc(doc(db, 'notifications', bRef.id), payload); } catch (_) {}
      try { await setDoc(doc(db, 'users', targetUid, 'notifications', bRef.id), payload); } catch (_) {}
    }
  }
}

ACT.bkTab = el => {
  S.bkTab = el.dataset.k;
  if (S.bkSel && S.bkSel.clear) S.bkSel.clear();
  renderAll();
};

ACT.bkApprove = async el => {
  const uid = el.dataset.uid, r = bulkRows().find(x => x.uid === uid);
  try {
    await bkSet(
      uid,
      { bulkMode: true, bulkStatus: 'approved', bulkApprovedAt: serverTimestamp(), bulkApprovedBy: S.admin, bulkRejectReason: '' },
      { status: 'approved', approvedAt: serverTimestamp(), reviewedBy: S.admin, updatedAt: serverTimestamp() },
      ['pending', 'approved', 'rejected', 'revoked'],
      'Bulk Mode Activated! 🚀',
      'Congratulations! Your Bulk Mode access request has been approved by Admin. You can now select multiple devices/comments at once in the Offers tab.',
      'reward'
    );
    logAction('bulk_access_on', uid, { name: r && r.name });
    toast(`Bulk mode activated & notification sent to ${r ? r.name : 'user'}`, 'ok');
  } catch (e) {
    toast('Failed: ' + (e.code || e.message), 'bad');
  }
};

ACT.bkReject = async el => {
  const uid = el.dataset.uid, r = bulkRows().find(x => x.uid === uid);
  const reason = await rejectDialog(
    `Reject Bulk access for ${r ? r.name : 'this user'}?`,
    '<p class="fs12 muted">Bulk mode stays hidden for this user. You can activate it later.</p>'
  );
  if (!reason) return;
  try {
    await bkSet(
      uid,
      { bulkMode: false, bulkStatus: 'rejected', bulkRejectReason: reason },
      { status: 'rejected', adminNote: reason, reviewedBy: S.admin, updatedAt: serverTimestamp() },
      ['pending', 'approved', 'active'],
      'Bulk Mode Request Rejected',
      `Your Bulk Mode access request was not approved. Reason: ${reason}`,
      'alert'
    );
    logAction('bulk_access_reject', uid, { name: r && r.name, reason });
    toast('Rejected & notification sent to user', 'ok');
  } catch (e) {
    toast('Failed: ' + (e.code || e.message), 'bad');
  }
};

ACT.bkRevoke = async el => {
  const uid = el.dataset.uid, r = bulkRows().find(x => x.uid === uid);
  if (
    !await confirmBox(
      'Turn off Bulk mode?',
      `${esc(r ? r.name : 'This user')} will no longer see Bulk mode.`,
      { ok: 'Turn off', danger: true }
    )
  )
    return;
  try {
    await bkSet(
      uid,
      { bulkMode: false, bulkStatus: 'revoked' },
      { status: 'revoked', reviewedBy: S.admin, updatedAt: serverTimestamp() },
      ['pending', 'approved', 'active'],
      'Bulk Mode Turned Off',
      'Your Bulk Mode access has been turned off by Admin. Your account is now in Normal Mode.',
      'warning'
    );
    logAction('bulk_access_off', uid, { name: r && r.name });
    toast('Bulk mode turned off & user notified', 'ok');
  } catch (e) {
    toast('Failed: ' + (e.code || e.message), 'bad');
  }
};

ACT.bkSetLimit = async el => {
  const uid = el.dataset.uid, r = bulkRows().find(x => x.uid === uid);
  const currentLimit = r && r.user ? (r.user.bulkDeviceCount || 2) : 2;
  const v = await ask({
    title: `Set Device Limit for ${r ? r.name : 'User'}`,
    body: `Current Device Limit: <b>${currentLimit} Devices</b>.<br>Enter new device limit (higher to increase, lower to decrease):`,
    ok: 'Update Device Limit',
    fields: [
      { id: 'limit', label: 'New Device Limit', type: 'number', required: true, value: String(currentLimit) }
    ]
  });
  if (!v) return;
  const newLimit = Math.max(1, parseInt(v.limit, 10) || 1);
  try {
    await bkSet(
      uid,
      { bulkDeviceCount: newLimit, bulkApprovedLimit: newLimit },
      {},
      [],
      'Device Limit Updated! 📱',
      `Your Bulk Mode device limit has been updated to ${newLimit} devices by Admin.`,
      'info'
    );
    toast(`Device limit updated to ${newLimit} for ${r ? r.name : 'user'}`, 'ok');
  } catch (e) {
    toast('Failed: ' + (e.code || e.message), 'bad');
  }
};

const getAppeal = id => {
  const rawAppeals = Array.isArray(S.bulkAppeals) ? S.bulkAppeals : [];
  let found = rawAppeals.find(x => x && x.id === id);
  if (found) return found;

  const ticketAppeals = (S.tickets || []).map(t => ({
    id: t.id,
    userId: t.userId,
    userName: t.userName || t.userEmail || t.userId,
    userEmail: t.userEmail || '',
    requestType: t.requestType || 'increase_limit',
    requestedDeviceCount: t.deviceCount || t.requestedDeviceCount || 2,
    reason: t.reason || t.message || '',
    status: t.status === 'open' ? 'pending' : (t.status === 'resolved' || t.status === 'closed' ? 'approved' : t.status)
  }));
  found = ticketAppeals.find(x => x && x.id === id);
  if (found) return found;

  const modReqList = Array.isArray(S.bulkModReq) ? S.bulkModReq : [];
  found = modReqList.find(q => q && q.id === id);
  if (found) {
    return {
      id: found.id,
      userId: found.userId,
      userName: found.userName || found.userEmail || found.userId,
      userEmail: found.userEmail || '',
      requestType: found.requestType || 'increase_limit',
      requestedDeviceCount: found.deviceCount || found.approvedLimit || 2,
      reason: found.reason || found.note || '',
      status: found.status || 'pending'
    };
  }

  const reqList = Array.isArray(S.bulkReq) ? S.bulkReq : [];
  found = reqList.find(q => q && q.id === id);
  if (found) {
    return {
      id: found.id,
      userId: found.userId || found.uid,
      userName: found.userName || found.userEmail || '',
      userEmail: found.userEmail || '',
      requestType: 'increase_limit',
      requestedDeviceCount: found.deviceCount || 2,
      reason: found.note || found.reason || '',
      status: found.status || 'pending'
    };
  }

  return null;
};

ACT.bkApproveLimitAppeal = async el => {
  const id = el.dataset.id, ap = getAppeal(id);
  if (!ap) { toast('Appeal record not found', 'bad'); return; }
  const newLimit = ap.requestedDeviceCount || 1;
  try {
    await bkSet(
      ap.userId,
      { bulkDeviceCount: newLimit, bulkApprovedLimit: newLimit, bulkMode: true, bulkStatus: 'approved' },
      {},
      [],
      'Device Limit Approved! 📱',
      `Your request to set device limit to ${newLimit} devices has been approved by Admin.`,
      'reward'
    );
    try { await updateDoc(doc(db, 'bulk_task_appeals', id), { status: 'approved', reviewedBy: S.admin, updatedAt: serverTimestamp() }); } catch (_) {}
    try { await updateDoc(doc(db, 'support_tickets', id), { status: 'closed', resolvedAt: serverTimestamp(), resolvedBy: S.admin }); } catch (_) {}
    try { await updateDoc(doc(db, 'bulk_mod_requests', id), { status: 'approved', approvedLimit: newLimit, reviewedBy: S.admin, updatedAt: serverTimestamp() }); } catch (_) {}
    toast(`Approved device limit of ${newLimit} devices for user!`, 'ok');
    renderAll(true);
  } catch (e) {
    toast('Action failed: ' + (e.code || e.message), 'bad');
  }
};

ACT.bkApproveExitAppeal = async el => {
  const id = el.dataset.id, ap = getAppeal(id);
  if (!ap) { toast('Appeal record not found', 'bad'); return; }
  try {
    await bkSet(
      ap.userId,
      { bulkMode: false, bulkStatus: 'none', bulkRejectReason: 'User requested exit' },
      { status: 'revoked', reviewedBy: S.admin, updatedAt: serverTimestamp() },
      ['pending', 'approved', 'active'],
      'Bulk Mode Turned Off',
      'Your request to exit Bulk Mode has been approved. Your account is now in Normal Mode.',
      'info'
    );
    try { await updateDoc(doc(db, 'bulk_task_appeals', id), { status: 'approved', reviewedBy: S.admin, updatedAt: serverTimestamp() }); } catch (_) {}
    try { await updateDoc(doc(db, 'support_tickets', id), { status: 'closed', resolvedAt: serverTimestamp(), resolvedBy: S.admin }); } catch (_) {}
    try { await updateDoc(doc(db, 'bulk_mod_requests', id), { status: 'approved', reviewedBy: S.admin, updatedAt: serverTimestamp() }); } catch (_) {}
    toast('Exit Bulk Mode request approved. User returned to Normal Mode!', 'ok');
    renderAll(true);
  } catch (e) {
    toast('Action failed: ' + (e.code || e.message), 'bad');
  }
};

/** Task 2: Multi-Select & Selection Handlers */
ACT.bkSelOne = el => {
  if (!S.bkSel) S.bkSel = new Set();
  const uid = el.dataset.uid;
  if (el.checked) {
    S.bkSel.add(uid);
  } else {
    S.bkSel.delete(uid);
  }
  renderAll();
};

ACT.bkSelAll = el => {
  if (!S.bkSel) S.bkSel = new Set();
  const rows = bulkRows().filter(r => r.status === S.bkTab);
  if (el.checked) {
    rows.forEach(r => S.bkSel.add(r.uid));
  } else {
    S.bkSel.clear();
  }
  renderAll();
};

ACT.bkSelClear = () => {
  if (S.bkSel && S.bkSel.clear) S.bkSel.clear();
  renderAll();
};

/**
 * Task 2: Batch Processing - Atomic batch update via writeBatch(db)
 */
ACT.bkBulkApprove = async () => {
  if (!S.bkSel) S.bkSel = new Set();
  const selectedUids = [...S.bkSel];
  if (!selectedUids.length) return toast('No requests selected', 'bad');

  const rows = bulkRows().filter(r => selectedUids.includes(r.uid));
  if (!rows.length) return toast('No valid requests selected', 'bad');

  if (
    !await ask({
      title: `Approve ${rows.length} Bulk Access Request${rows.length > 1 ? 's' : ''}?`,
      body: `<p class="fs13">This will activate Bulk Mode for <b>${rows.length}</b> selected user(s) atomically.</p>`,
      ok: `Approve ${rows.length} Selected`
    })
  )
    return;

  busy = true;
  renderAll();

  try {
    let approvedCount = 0;
    for (const r of rows) {
      await bkSet(
        r.uid,
        {
          bulkMode: true,
          bulkStatus: 'approved',
          bulkApprovedAt: serverTimestamp(),
          bulkApprovedBy: S.admin,
          bulkRejectReason: ''
        },
        {
          status: 'approved',
          approvedAt: serverTimestamp(),
          reviewedBy: S.admin,
          updatedAt: serverTimestamp()
        },
        ['pending', 'approved', 'rejected', 'revoked'],
        'Bulk Mode Activated! 🚀',
        'Congratulations! Your Bulk Mode access request has been approved by Admin. You can now select multiple devices/comments at once in the Offers tab.',
        'reward'
      );
      approvedCount++;
    }

    logAction('bulk_access_batch_approve', 'batch', { count: approvedCount });
    toast(`Approved & activated Bulk Mode for ${approvedCount} user(s)!`, 'ok');
    S.bkSel.clear();
  } catch (e) {
    console.error('Batch approve error:', e);
    toast('Batch approval failed: ' + (e.code || e.message), 'bad');
  } finally {
    busy = false;
    renderAll();
  }
};

ACT.bkBulkReject = async () => {
  if (!S.bkSel) S.bkSel = new Set();
  const selectedUids = [...S.bkSel];
  if (!selectedUids.length) return toast('No requests selected', 'bad');

  const rows = bulkRows().filter(r => selectedUids.includes(r.uid));
  if (!rows.length) return toast('No valid requests selected', 'bad');

  const reason = await rejectDialog(
    `Reject ${rows.length} Bulk Access Request${rows.length > 1 ? 's' : ''}?`,
    '<p class="fs12 muted">Bulk mode will remain hidden for selected users. Reason will be saved.</p>'
  );
  if (!reason) return;

  busy = true;
  renderAll();

  try {
    let rejectedCount = 0;
    for (const r of rows) {
      await bkSet(
        r.uid,
        {
          bulkMode: false,
          bulkStatus: 'rejected',
          bulkRejectReason: reason
        },
        {
          status: 'rejected',
          adminNote: reason,
          reviewedBy: S.admin,
          updatedAt: serverTimestamp()
        },
        ['pending', 'approved', 'active'],
        'Bulk Mode Request Rejected',
        `Your Bulk Mode access request was not approved. Reason: ${reason}`,
        'alert'
      );
      rejectedCount++;
    }

    logAction('bulk_access_batch_reject', 'batch', { count: rejectedCount, reason });
    toast(`Rejected ${rejectedCount} bulk access request(s).`, 'ok');
    S.bkSel.clear();
  } catch (e) {
    console.error('Batch reject error:', e);
    toast('Batch rejection failed: ' + (e.code || e.message), 'bad');
  } finally {
    busy = false;
    renderAll();
  }
};

ACT.bkApproveAppeal = async el => {
  const id = el.dataset.id, ap = getAppeal(id);
  if (!ap) { toast('Appeal record not found', 'bad'); return; }
  const v = await ask({
    title: `Approve Price Appeal for ${ap.userName || ap.userId}?`,
    body: `Requested Device Count: <b>${ap.requestedDeviceCount || 1}</b> • Requested Reward: <b>₹${ap.requestedRewardInr || 0}</b>.<br>Reason: ${esc(ap.reason || 'No reason specified')}`,
    ok: 'Approve & Credit Wallet',
    fields: [
      { id: 'rewardInr', label: 'Custom Payout Amount (₹)', type: 'number', required: true, value: String(ap.requestedRewardInr || 50) }
    ]
  });
  if (!v) return;
  const creditAmt = Math.max(1, parseInt(v.rewardInr, 10) || 0);

  try {
    const userRef = doc(db, 'users', ap.userId);
    await runTransaction(db, async tx => {
      const snap = await tx.get(userRef);
      if (!snap.exists()) throw new Error('User record not found');
      const bal = num(snap.data().walletBalance);
      tx.update(userRef, { walletBalance: bal + creditAmt, updatedAt: serverTimestamp() });
      tx.set(doc(collection(db, 'users', ap.userId, 'transactions')), {
        userId: ap.userId,
        amount: creditAmt,
        type: 'admin_credit',
        note: `Approved Bulk Price Appeal: ${ap.taskTitle || 'Task'}`,
        createdAt: serverTimestamp()
      });
    });

    try { await updateDoc(doc(db, 'bulk_task_appeals', id), { status: 'approved', approvedRewardInr: creditAmt, reviewedBy: S.admin, updatedAt: serverTimestamp() }); } catch (_) {}
    try { await updateDoc(doc(db, 'support_tickets', id), { status: 'closed', resolvedAt: serverTimestamp(), resolvedBy: S.admin }); } catch (_) {}
    toast(`Appeal approved & ₹${creditAmt} credited to wallet!`, 'ok');
    renderAll(true);
  } catch (e) {
    toast('Action failed: ' + (e.code || e.message), 'bad');
  }
};

ACT.bkRejectAppeal = async el => {
  const id = el.dataset.id, ap = getAppeal(id);
  const reason = await rejectDialog('Reject Bulk Request / Appeal?', '<p class="fs12 muted">The request will be marked rejected.</p>');
  if (!reason) return;
  try {
    try { await updateDoc(doc(db, 'bulk_task_appeals', id), { status: 'rejected', adminNote: reason, reviewedBy: S.admin, updatedAt: serverTimestamp() }); } catch (_) {}
    try { await updateDoc(doc(db, 'support_tickets', id), { status: 'closed', adminReply: reason, resolvedBy: S.admin, updatedAt: serverTimestamp() }); } catch (_) {}
    try { await updateDoc(doc(db, 'bulk_mod_requests', id), { status: 'rejected', adminNote: reason, reviewedBy: S.admin, updatedAt: serverTimestamp() }); } catch (_) {}
    toast('Request rejected', 'ok');
    renderAll(true);
  } catch (e) {
    toast('Action failed: ' + (e.code || e.message), 'bad');
  }
};

ACT.bkApproveSub = async el => {
  const id = el.dataset.id;
  const sb = (S.subs || []).find(s => s && s.id === id);
  if (!sb) { toast('Submission record not found', 'bad'); return; }
  const defaultAmt = sb.rewardAmount || 10;
  const v = await ask({
    title: `Approve Bulk Submission for ${sb.userName || sb.userEmail || 'User'}?`,
    body: `Task: <b>${esc(sb.taskTitle || sb.matchedTaskName || 'Bulk Task')}</b> • Default Reward: <b>₹${defaultAmt}</b>`,
    ok: 'Approve & Credit Wallet',
    fields: [
      { id: 'rewardInr', label: 'Custom Payout Amount (₹)', type: 'number', required: true, value: String(defaultAmt) }
    ]
  });
  if (!v) return;
  const creditAmt = Math.max(1, parseInt(v.rewardInr, 10) || defaultAmt);

  try {
    const userRef = doc(db, 'users', sb.userId);
    await runTransaction(db, async tx => {
      const snap = await tx.get(userRef);
      if (!snap.exists()) throw new Error('User record not found');
      const bal = num(snap.data().walletBalance);
      tx.update(userRef, { walletBalance: bal + creditAmt, updatedAt: serverTimestamp() });
      tx.set(doc(collection(db, 'users', sb.userId, 'transactions')), {
        userId: sb.userId,
        amount: creditAmt,
        type: 'admin_credit',
        note: `Approved Bulk Task Submission: ${sb.taskTitle || 'Task'} (+₹${creditAmt})`,
        createdAt: serverTimestamp()
      });
    });

    try { await updateDoc(doc(db, 'submissions', id), { status: 'approved', rewardAmount: creditAmt, reviewedBy: S.admin, updatedAt: serverTimestamp() }); } catch (_) {}
    try { await updateDoc(doc(db, 'users', sb.userId, 'submissions', id), { status: 'approved', rewardAmount: creditAmt, reviewedBy: S.admin, updatedAt: serverTimestamp() }); } catch (_) {}
    toast(`Approved & credited ₹${creditAmt} to user's wallet!`, 'ok');
    renderAll(true);
  } catch (e) {
    toast('Action failed: ' + (e.code || e.message), 'bad');
  }
};

ACT.bkDeleteAppeal = async el => {
  const id = el.dataset.id;
  if (!id) return;
  const ok = await confirmBox('Delete Appeal Record?', 'Are you sure you want to delete this appeal record permanently?', { ok: 'Delete', cancel: 'Cancel' });
  if (!ok) return;
  try {
    try { await deleteDoc(doc(db, 'bulk_task_appeals', id)); } catch (_) {}
    try { await deleteDoc(doc(db, 'support_tickets', id)); } catch (_) {}
    try { await deleteDoc(doc(db, 'bulk_mod_requests', id)); } catch (_) {}
    try { await deleteDoc(doc(db, 'bulkRequests', id)); } catch (_) {}
    toast('Appeal record deleted successfully!', 'ok');
    renderAll(true);
  } catch (e) {
    toast('Error deleting appeal: ' + (e.code || e.message), 'bad');
  }
};

/** Task 2: Render Dashboard & Multi-Select Action Bar */
RENDER.bulk = () => {
  try {
    S.bulkReq = Array.isArray(S.bulkReq) ? S.bulkReq : [];
    S.bulkModReq = Array.isArray(S.bulkModReq) ? S.bulkModReq : [];
    S.bulkAppeals = Array.isArray(S.bulkAppeals) ? S.bulkAppeals : [];
    S.bkSel = S.bkSel || new Set();

    const rows = bulkRows(),
      f = S.bkTab || 'pending',
      by = k => rows.filter(r => r && r.status === k);

    if (S.bkSel && S.bkSel.forEach) {
      S.bkSel.forEach(uid => {
        if (!rows.some(r => r && r.uid === uid && r.status === f)) {
          S.bkSel.delete(uid);
        }
      });
    }

    const tabBox = $('#bk-tabs');
    if (tabBox) {
      const bulkSubCount = (S.subs || []).filter(s => s && (s.isBulk || String(s.category || '').toLowerCase().includes('bulk'))).length;
      setHTML(
        tabBox,
        [
          ['pending', 'Pending Requests'],
          ['active', `Active Bulk Users (${by('active').length})`],
          ['rejected', 'Rejected'],
          ['appeals', `Appeals (${S.bulkAppeals.filter(a => a.status === 'pending').length})`],
          ['submissions', `Bulk Submissions (${bulkSubCount})`]
        ]
          .map(
            ([k, l]) =>
              `<button class="tab ${f === k ? 'on' : ''}" data-act="bkTab" data-k="${k}">${l}</button>`
          )
          .join('')
      );
    }

    const listBox = $('#bk-list');
    if (!listBox) return;

    if (f === 'appeals') {
      const rawAppeals = Array.isArray(S.bulkAppeals) ? S.bulkAppeals : [];
      const ticketAppeals = (S.tickets || []).filter(t => t && (t.type === 'bulk_application' || (t.id && String(t.id).startsWith('appeal_')) || (t.message && String(t.message).includes('BULK MODE APPEAL')))).map(t => ({
        id: t.id,
        userId: t.userId,
        userName: t.userName,
        userEmail: t.userEmail,
        requestType: t.requestType || 'increase_limit',
        requestedDeviceCount: t.deviceCount || t.requestedDeviceCount || 2,
        reason: t.reason || t.message || '',
        status: t.status === 'open' ? 'pending' : (t.status === 'resolved' || t.status === 'closed' ? 'approved' : t.status),
        createdAt: t.createdAt || t.updatedAt
      }));
      const appealsMap = new Map();
      [...rawAppeals, ...ticketAppeals].forEach(ap => { if (ap && ap.id) appealsMap.set(ap.id, ap); });
      const appealsList = [...appealsMap.values()].sort((a, b) => tsMs(b.createdAt) - tsMs(a.createdAt));
      const appealsHTML = appealsList.map(ap => {
        const id = esc(ap.id);
        const st = String(ap.status || 'pending').toLowerCase();
        const reqType = ap.requestType || 'price_increase';
        const pill = st === 'approved' ? '<span class="pill ok">Approved</span>' : st === 'rejected' ? '<span class="pill bad">Rejected</span>' : '<span class="pill warn">Pending Appeal</span>';

        let titleText = 'Price Increase Appeal';
        let detailText = `Requested Devices: <b>${ap.requestedDeviceCount || 1}</b> • Requested Reward: <b>₹${ap.requestedRewardInr || 0}</b>`;
        let act = '';

        if (reqType === 'increase_limit' || reqType === 'decrease_limit') {
          titleText = `Device Limit ${reqType === 'increase_limit' ? 'Increase' : 'Decrease'} Request`;
          detailText = `Requested Device Limit: <b>${ap.requestedDeviceCount || 1} Devices</b>`;
          if (st === 'pending') {
            act = `<button class="btn okfill sm" data-act="bkApproveLimitAppeal" data-id="${id}">Approve Device Limit (${ap.requestedDeviceCount})</button><button class="btn bad sm" data-act="bkRejectAppeal" data-id="${id}">Reject</button>`;
          }
        } else if (reqType === 'exit_bulk_mode') {
          titleText = `Exit Bulk Mode Request`;
          detailText = `User requested to turn off Bulk Mode and revert account to Normal Mode.`;
          if (st === 'pending') {
            act = `<button class="btn bad sm" data-act="bkApproveExitAppeal" data-id="${id}">Approve & Turn Off Bulk Mode</button><button class="btn ghost sm" data-act="bkRejectAppeal" data-id="${id}">Reject</button>`;
          }
        } else {
          if (st === 'pending') {
            act = `<button class="btn okfill sm" data-act="bkApproveAppeal" data-id="${id}">Approve Custom Reward</button><button class="btn bad sm" data-act="bkRejectAppeal" data-id="${id}">Reject</button>`;
          }
        }

        return `<div class="card">
          <div class="row between wrap" style="gap:6px">
            <div>
              <div class="b fs14">${esc(ap.userName || ap.userEmail || ap.userId)}</div>
              <div class="fs12 muted">${esc(ap.userEmail)} • <b>${titleText}</b></div>
            </div>
            ${pill}
          </div>
          <div class="fs12 mt8" style="background:rgba(14,165,233,0.06);padding:8px 12px;border-radius:8px">
            ${detailText}<br>
            Reason: ${esc(ap.reason || 'No reason provided')}
          </div>
          <div class="row gap6 mt8" style="justify-content:flex-end">${act}<button class="btn bad sm" data-act="bkDeleteAppeal" data-id="${id}" title="Delete Appeal Record"><i data-ic="trash" data-s="14"></i>Delete</button></div>
        </div>`;
      }).join('') || `<div class="card">${emptyHTML('bulk', 'No appeals or limit change requests submitted yet.')}</div>`;

      setHTML(listBox, appealsHTML);
      return;
    }

    if (f === 'submissions') {
      const bulkSubs = (S.subs || []).filter(s => s && (s.isBulk || String(s.category || '').toLowerCase().includes('bulk') || (S.tasks || []).some(t => t.id === s.taskId && (t.isBulk || (t.modeType || '').toUpperCase() === 'BULK'))));
      const subsHTML = bulkSubs.map(sb => {
        const id = esc(sb.id);
        const st = String(sb.status || 'pending').toLowerCase();
        const pill = st === 'approved' ? '<span class="pill ok">Approved</span>' : st === 'rejected' ? '<span class="pill bad">Rejected</span>' : '<span class="pill warn">Pending Review</span>';
        const img = sb.screenshotBase64 || sb.referenceScreenshotUrl || '';
        const proofImgHTML = img ? `<div class="mt8"><img src="${esc(img)}" style="max-height:120px;border-radius:8px;border:1px solid var(--line);cursor:pointer" onclick="window.open('${esc(img)}','_blank')"></div>` : '';
        const act = st === 'pending' ? `<button class="btn okfill sm" data-act="bkApproveSub" data-id="${id}">Approve & Credit Custom Amount (Default ₹${num(sb.rewardAmount)})</button><button class="btn bad sm" data-act="apReject" data-id="${id}">Reject</button>` : '';

        return `<div class="card">
          <div class="row between wrap" style="gap:6px">
            <div>
              <div class="b fs14">${esc(sb.userName || sb.userEmail || sb.userId)}</div>
              <div class="fs12 muted">Task: <b>${esc(sb.taskTitle || sb.matchedTaskName || 'Bulk Task')}</b> • Reward: <b>₹${num(sb.rewardAmount)}</b></div>
            </div>
            ${pill}
          </div>
          ${sb.proofText ? `<div class="fs12 mt8" style="background:var(--soft);padding:8px 12px;border-radius:8px">Proof Note: ${esc(sb.proofText)}</div>` : ''}
          ${proofImgHTML}
          <div class="row gap6 mt8" style="justify-content:flex-end">${act}</div>
        </div>`;
      }).join('') || `<div class="card">${emptyHTML('submissions', 'No bulk task submissions found.')}</div>`;

      setHTML(listBox, subsHTML);
      return;
    }

    const list = by(f);
    const pill = {
      pending: '<span class="pill warn">Pending</span>',
      active: '<span class="pill ok">Bulk Active</span>',
      rejected: '<span class="pill bad">Rejected</span>'
    };

    const allSelected = list.length > 0 && list.every(r => S.bkSel.has(r.uid));
    const selCount = S.bkSel.size;

    const summaryCardsHTML = `<div class="g3" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(140px,1fr));gap:10px">
      <div class="card" style="cursor:pointer;border:${f === 'pending' ? '2px solid #F59E0B' : '1px solid var(--line)'}" data-act="bkTab" data-k="pending">
        <div class="fs12 muted b">Pending Requests</div>
        <div class="fs20 xb" style="color:#F59E0B;margin-top:4px">${by('pending').length}</div>
      </div>
      <div class="card" style="cursor:pointer;border:${f === 'active' ? '2px solid #16A34A' : '1px solid var(--line)'}" data-act="bkTab" data-k="active">
        <div class="fs12 muted b">Active Bulk Users</div>
        <div class="fs20 xb" style="color:#16A34A;margin-top:4px">${by('active').length}</div>
      </div>
      <div class="card" style="cursor:pointer;border:${f === 'rejected' ? '2px solid #E11D48' : '1px solid var(--line)'}" data-act="bkTab" data-k="rejected">
        <div class="fs12 muted b">Rejected</div>
        <div class="fs20 xb" style="color:#E11D48;margin-top:4px">${by('rejected').length}</div>
      </div>
    </div>`;

    const headerHTML =
      list.length > 0
        ? `<div class="card row between wrap" style="gap:10px;padding:10px 16px;background:var(--card-bg,#ffffff)">
            <label class="row fs13 b" style="gap:8px;cursor:pointer">
              <input type="checkbox" class="chk" data-act="bkSelAll" ${allSelected ? 'checked' : ''}>
              <span>Select All (${list.length})</span>
            </label>
            ${
              selCount > 0
                ? `<span class="fs12 brand-t b">${selCount} request${selCount > 1 ? 's' : ''} selected</span>`
                : `<span class="fs12 muted">Select requests for bulk approval or rejection</span>`
            }
          </div>`
        : '';

    const bulkActionBarHTML =
      selCount > 0
        ? `<div class="card sticky-bulk-bar" style="position:sticky;top:60px;z-index:99;background:var(--card-bg,#1e293b);color:#ffffff;padding:12px 18px;border-radius:12px;margin-bottom:12px;box-shadow:0 10px 25px -5px rgba(0,0,0,0.25)">
            <div class="row between wrap" style="gap:12px">
              <div class="row" style="gap:8px">
                <span class="badge" style="background:#0EA5E9;color:#fff;padding:3px 10px;border-radius:20px;font-weight:bold">${selCount}</span>
                <span class="b fs14">${selCount === 1 ? 'Request' : 'Requests'} selected</span>
              </div>
              <div class="row" style="gap:8px">
                <button class="btn okfill sm" data-act="bkBulkApprove" ${busy ? 'disabled' : ''}>Approve Selected (${selCount})</button>
                <button class="btn bad sm" data-act="bkBulkReject" ${busy ? 'disabled' : ''}>Reject Selected (${selCount})</button>
                <button class="btn ghost sm" style="color:#e2e8f0;border-color:rgba(255,255,255,0.2)" data-act="bkSelClear">Clear Selection</button>
              </div>
            </div>
          </div>`
        : '';

    const safeAvc = name => (typeof avc === 'function' ? avc(name) : '#0EA5E9');

    const listHTML =
      list
        .map(r => {
          const q = r.req,
            uid = esc(r.uid),
            isSel = S.bkSel.has(r.uid);
          const reqData = (q && q.requestData) || {};
          const userDeviceCount = r.user ? (r.user.bulkDeviceCount || reqData.deviceCount || 2) : (reqData.deviceCount || 2);
          const act =
            f === 'pending'
              ? `<button class="btn okfill sm" data-act="bkApprove" data-uid="${uid}">Activate</button><button class="btn bad sm" data-act="bkReject" data-uid="${uid}">Reject</button>`
              : f === 'active'
              ? `<button class="btn okfill sm" style="background:#0284C7" data-act="bkSetLimit" data-uid="${uid}">Set Limit (${userDeviceCount})</button><button class="btn bad sm" data-act="bkRevoke" data-uid="${uid}">Turn off</button>`
              : `<button class="btn okfill sm" data-act="bkApprove" data-uid="${uid}">Activate</button>`;

          const safeInitial = esc(String((r.name || r.email || r.uid || 'U').trim())[0].toUpperCase());

          return `<div class="card ${isSel ? 'sel' : ''}">
          <div class="row" style="gap:12px;align-items:flex-start">
            <input type="checkbox" class="chk" style="margin-top:4px" data-act="bkSelOne" data-uid="${uid}" ${isSel ? 'checked' : ''} aria-label="Select request">
            <span class="avatar" style="background:${safeAvc(r.name)}">${safeInitial}</span>
            <div class="grow" style="min-width:0">
              <div class="row between wrap" style="gap:6px">
                <div class="b fs14 trunc">${esc(r.name)}</div>
                ${pill[r.status]}
              </div>
              <div class="fs12 muted trunc">${esc(r.email)}</div>
              <div class="fs12 brand-t b" style="margin-top:4px">Approved Device Limit: ${userDeviceCount} Devices</div>
              ${q ? `<div class="fs12 muted" style="margin-top:2px">Requested ${fmtDT(tsMs(q.createdAt || q.requestedAt))} • Tier: <b>${esc(reqData.tier || 'Standard')}</b></div>` : ''}
              ${q && (q.note || q.reason) ? `<div class="fs12" style="margin-top:6px;background:rgba(14,165,233,0.06);padding:6px 10px;border-radius:6px">Note: ${esc(q.note || q.reason)}</div>` : ''}
              ${f === 'rejected' && q && q.adminNote ? `<div class="fs12 bad-t" style="margin-top:4px">Reason: ${esc(q.adminNote)}</div>` : ''}
            </div>
          </div>
          <div class="row gap6 mt8" style="justify-content:flex-end">${act}</div>
        </div>`;
        })
        .join('') ||
      `<div class="card">${emptyHTML(
        'bulk',
        f === 'pending'
          ? 'No pending requests waiting.'
          : f === 'active'
          ? 'No active bulk access users yet.'
          : 'No rejected requests.'
      )}</div>`;

    setHTML(listBox, summaryCardsHTML + headerHTML + bulkActionBarHTML + listHTML);
  } catch (e) {
    console.error('RENDER.bulk error:', e);
  }
};

function startBulkListener() {
  listen('bulkRequests', collection(db, 'bulkRequests'), snap => {
    S.bulkReq = snap.docs
      .map(d => ({ id: d.id, ...d.data() }))
      .sort((x, y) => tsMs(y.createdAt || y.requestedAt) - tsMs(x.createdAt || x.requestedAt));
    renderAll();
  });
  listen('bulk_mod_requests', collection(db, 'bulk_mod_requests'), snap => {
    S.bulkModReq = snap.docs
      .map(d => ({ id: d.id, ...d.data() }))
      .sort((x, y) => tsMs(y.createdAt || y.requestedAt) - tsMs(x.createdAt || x.requestedAt));
    renderAll();
  });
  listen('bulk_task_appeals', collection(db, 'bulk_task_appeals'), snap => {
    S.bulkAppeals = snap.docs
      .map(d => ({ id: d.id, ...d.data() }))
      .sort((x, y) => tsMs(y.createdAt) - tsMs(x.createdAt));
    renderAll();
  });
}
