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
