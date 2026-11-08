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
