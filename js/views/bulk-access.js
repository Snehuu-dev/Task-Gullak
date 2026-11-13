'use strict';
/* ============================================================
   BULK ACCESS MANAGEMENT & ADMIN APPROVAL DASHBOARD
   ============================================================ */

const AVC = ['#0EA5E9', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6', '#EC4899', '#6366F1'];
const avc = s => AVC[[...String(s || 'User')].reduce((a, c) => a + c.charCodeAt(0), 0) % AVC.length];
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

/** Single-Item Updates */
async function bkSet(uid, userPatch, reqPatch, statuses) {
  const reqList = Array.isArray(S.bulkReq) ? S.bulkReq : [];
  const modReqList = Array.isArray(S.bulkModReq) ? S.bulkModReq : [];

  await Promise.all(bkIds(uid).map(id => setDoc(doc(db, 'users', id), userPatch, { merge: true })));

  const reqs = reqList.filter(q => q && safeCanon(q.userId || q.uid) === safeCanon(uid) && statuses.includes(q.status));
  await Promise.all(reqs.map(q => updateDoc(doc(db, 'bulkRequests', q.id), reqPatch)));

  const modReqs = modReqList.filter(q => q && safeCanon(q.userId || q.uid) === safeCanon(uid) && statuses.includes(q.status));
  await Promise.all(modReqs.map(q => updateDoc(doc(db, 'bulk_mod_requests', q.id), reqPatch)));

  try {
    const tRef = doc(db, 'support_tickets', `bulk_${uid}`);
    await updateDoc(tRef, { status: userPatch.bulkMode ? 'closed' : 'rejected', updatedAt: serverTimestamp() });
  } catch (_) {}
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
      { bulkMode: true, bulkStatus: 'active', bulkApprovedAt: serverTimestamp(), bulkApprovedBy: S.admin, bulkRejectReason: '' },
      { status: 'approved', approvedAt: serverTimestamp(), reviewedBy: S.admin, updatedAt: serverTimestamp() },
      ['pending', 'approved']
    );
    logAction('bulk_access_on', uid, { name: r && r.name });
    toast(`Bulk mode is active for ${r ? r.name : 'user'}`, 'ok');
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
      ['pending']
    );
    logAction('bulk_access_reject', uid, { name: r && r.name, reason });
    toast('Rejected. Bulk mode is hidden for this user', 'ok');
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
      ['pending', 'approved']
    );
    logAction('bulk_access_off', uid, { name: r && r.name });
    toast('Bulk mode turned off', 'ok');
  } catch (e) {
    toast('Failed: ' + (e.code || e.message), 'bad');
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
    const chunkSize = 200;
    let approvedCount = 0;
    const reqList = Array.isArray(S.bulkReq) ? S.bulkReq : [];
    const modReqList = Array.isArray(S.bulkModReq) ? S.bulkModReq : [];

    for (let i = 0; i < rows.length; i += chunkSize) {
      const chunk = rows.slice(i, i + chunkSize);
      const batch = writeBatch(db);

      chunk.forEach(r => {
        const uids = bkIds(r.uid);
        uids.forEach(id => {
          batch.set(
            doc(db, 'users', id),
            {
              bulkMode: true,
              bulkStatus: 'active',
              bulkApprovedAt: serverTimestamp(),
              bulkApprovedBy: S.admin,
              bulkRejectReason: ''
            },
            { merge: true }
          );
        });

        const reqs = reqList.filter(
          q => q && safeCanon(q.userId || q.uid) === safeCanon(r.uid) && ['pending', 'approved'].includes(q.status)
        );
        reqs.forEach(q => {
          batch.update(doc(db, 'bulkRequests', q.id), {
            status: 'approved',
            approvedAt: serverTimestamp(),
            reviewedBy: S.admin,
            updatedAt: serverTimestamp()
          });
        });

        const modReqs = modReqList.filter(
          q => q && safeCanon(q.userId || q.uid) === safeCanon(r.uid) && ['pending', 'approved'].includes(q.status)
        );
        modReqs.forEach(q => {
          batch.update(doc(db, 'bulk_mod_requests', q.id), {
            status: 'approved',
            approvedAt: serverTimestamp(),
            reviewedBy: S.admin,
            updatedAt: serverTimestamp()
          });
        });

        approvedCount++;
      });

      await batch.commit();
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
    const chunkSize = 200;
    let rejectedCount = 0;
    const reqList = Array.isArray(S.bulkReq) ? S.bulkReq : [];
    const modReqList = Array.isArray(S.bulkModReq) ? S.bulkModReq : [];

    for (let i = 0; i < rows.length; i += chunkSize) {
      const chunk = rows.slice(i, i + chunkSize);
      const batch = writeBatch(db);

      chunk.forEach(r => {
        const uids = bkIds(r.uid);
        uids.forEach(id => {
          batch.set(
            doc(db, 'users', id),
            {
              bulkMode: false,
              bulkStatus: 'rejected',
              bulkRejectReason: reason
            },
            { merge: true }
          );
        });

        const reqs = reqList.filter(
          q => q && safeCanon(q.userId || q.uid) === safeCanon(r.uid) && q.status === 'pending'
        );
        reqs.forEach(q => {
          batch.update(doc(db, 'bulkRequests', q.id), {
            status: 'rejected',
            adminNote: reason,
            reviewedBy: S.admin,
            updatedAt: serverTimestamp()
          });
        });

        const modReqs = modReqList.filter(
          q => q && safeCanon(q.userId || q.uid) === safeCanon(r.uid) && q.status === 'pending'
        );
        modReqs.forEach(q => {
          batch.update(doc(db, 'bulk_mod_requests', q.id), {
            status: 'rejected',
            adminNote: reason,
            reviewedBy: S.admin,
            updatedAt: serverTimestamp()
          });
        });

        rejectedCount++;
      });

      await batch.commit();
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

/** Task 2: Render Dashboard & Multi-Select Action Bar */
RENDER.bulk = () => {
  try {
    S.bulkReq = Array.isArray(S.bulkReq) ? S.bulkReq : [];
    S.bulkModReq = Array.isArray(S.bulkModReq) ? S.bulkModReq : [];
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
      setHTML(
        tabBox,
        [['pending', 'Pending'], ['active', 'Active'], ['rejected', 'Rejected']]
          .map(
            ([k, l]) =>
              `<button class="tab ${f === k ? 'on' : ''}" data-act="bkTab" data-k="${k}">${l} <span class="fs11">${by(k).length}</span></button>`
          )
          .join('')
      );
    }

    const listBox = $('#bk-list');
    if (!listBox) return;

    const list = by(f);
    const pill = {
      pending: '<span class="pill warn">Pending</span>',
      active: '<span class="pill ok">Active</span>',
      rejected: '<span class="pill bad">Not allowed</span>'
    };

    const allSelected = list.length > 0 && list.every(r => S.bkSel.has(r.uid));
    const selCount = S.bkSel.size;

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
          const reqCount = reqData.requestedCount || reqData.deviceCount || 2;
          const act =
            f === 'pending'
              ? `<button class="btn okfill sm" data-act="bkApprove" data-uid="${uid}">Activate</button><button class="btn bad sm" data-act="bkReject" data-uid="${uid}">Reject</button>`
              : f === 'active'
              ? `<button class="btn bad sm" data-act="bkRevoke" data-uid="${uid}">Turn off</button>`
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
              ${q ? `<div class="fs12 muted" style="margin-top:2px">Requested ${fmtDT(tsMs(q.createdAt || q.requestedAt))} • Tier: <b>${esc(reqData.tier || 'Standard')} (${reqCount} devices)</b></div>` : ''}
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
        f === 'pending' ? 'No pending requests waiting.' : f === 'active' ? 'No user has Bulk mode active yet.' : 'No rejected requests.'
      )}</div>`;

    setHTML(listBox, headerHTML + bulkActionBarHTML + listHTML);
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
}
