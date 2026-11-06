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
