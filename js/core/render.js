'use strict';
/* ============================================================
   RENDER DISPATCH
   ============================================================ */
const RENDER = {};
function renderAll(force) {
  if (!S.admin) return;
  renderChrome();
  const fn = RENDER[S.view]; if (fn) fn(force);
  renderBulk();
  if (S.drawer.uid) renderDrawer();
}
function renderChrome() {
  const ps = pendingSubs().length, pw = pendingWds().length;
  $('#bdg-appr').textContent = ps; $('#bdg-appr').classList.toggle('hidden', !ps);
  const bb = $('#bdg-bulk');
  if (bb) {
    const bp = (typeof bulkRows === 'function' ? (bulkRows() || []) : []).filter(r => r && r.status === 'pending').length;
    bb.textContent = bp;
    bb.classList.toggle('hidden', !bp);
  }
  $('#bdg-wd').textContent = pw; $('#bdg-wd').classList.toggle('hidden', !pw);
  const pt = (S.tickets || []).filter(t => (t.status || 'open').toLowerCase() === 'open').length;
  const bt = $('#bdg-tickets'); if (bt) { bt.textContent = pt; bt.classList.toggle('hidden', !pt); }
  const bell = ps + pw; $('#bell-cnt').textContent = bell > 99 ? '99+' : bell; $('#bell-cnt').classList.toggle('hidden', !bell);
  const core = ['users', 'tasks', 'submissions', 'withdrawals', 'broadcasts'];
  const errs = core.filter(k => S.errs[k]), okAll = core.every(k => S.live[k]);
  const c = $('#conn');
  c.classList.toggle('off', !okAll);
  c.lastElementChild.textContent = errs.length ? 'Check access' : okAll ? 'Online' : 'Connecting…';
  const on = isMaintActive();
  ['ov-switch', 'mt-switch'].forEach(id => { const e = $('#' + id); if (e) { e.classList.toggle('on', on); e.setAttribute('aria-pressed', on); } });
}
