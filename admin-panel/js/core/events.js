'use strict';
/* ============================================================
   EVENT DELEGATION
   ============================================================ */
const ACT = {};
const INP = {};
document.addEventListener('click', e => {
  const el = e.target.closest('[data-act]');
  if (!el) { if (!e.target.closest('#user-menu') && !e.target.closest('[data-act="userMenu"]')) $('#user-menu')?.classList.add('hidden'); return; }
  const fn = ACT[el.dataset.act];
  if (fn) { try { const r = fn(el, e); if (r && r.catch) r.catch(err => { console.error(err); toast(err.message || 'Action failed', 'bad'); }); } catch (err) { console.error(err); toast(err.message || 'Action failed', 'bad'); } }
});
const onInput = e => {
  const el = e.target.closest('[data-in]'); if (!el) return;
  const fn = INP[el.dataset.in]; if (fn) fn(el, e);
};
document.addEventListener('input', onInput);
document.addEventListener('change', e => { if (e.target.matches('select,input[type=date],input[type=month],input[type=checkbox],input[type=file]')) onInput(e); });
document.addEventListener('keydown', e => {
  if (e.key === 'Escape') {
    const dlg = $('#dlg-overlay'); if (dlg.classList.contains('show')) { dlg._cancel && dlg._cancel(); return; }
    if ($('#lightbox').classList.contains('show')) { closeLightbox(); return; }
    if ($('#drawer').classList.contains('show')) { closeDrawer(); return; }
    if ($('#task-modal').classList.contains('show')) { closeTask(); return; }
    document.body.classList.remove('nav-open');
  }
  if (e.key === 'Enter' && (e.target.id === 'admin-pass-input' || e.target.id === 'admin-email-input')) window.unlockAdmin();
});
$('#dlg-overlay').addEventListener('mousedown', e => { if (e.target.id === 'dlg-overlay') e.currentTarget._cancel && e.currentTarget._cancel(); });

const themeMode = () => { try { return localStorage.getItem('tg_theme') || 'light'; } catch { return 'light'; } };
function applyTheme(m) { try { localStorage.setItem('tg_theme', m); } catch { } document.documentElement.dataset.theme = m === 'auto' ? (matchMedia('(prefers-color-scheme:dark)').matches ? 'dark' : 'light') : m; }
ACT.theme = () => { applyTheme(document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark'); renderAll(true); };
ACT.themeSet = el => { applyTheme(el.dataset.m); renderAll(true); };
function renderThemeCard() { const m = themeMode(); setHTML($('#st-theme'), `<p class="fs13 muted" style="margin-bottom:10px">Choose how the panel looks. Your choice is remembered in this browser.</p><div class="tabs">${[['light', 'sun', 'Light'], ['dark', 'moon', 'Dark'], ['auto', 'settings', 'Auto']].map(([k, i, l]) => `<button class="tab ${m === k ? 'on' : ''}" data-act="themeSet" data-m="${k}">${ic(i, 14)} ${l}</button>`).join('')}</div>`); }
ACT.go = el => go(el.dataset.v, { range: el.dataset.range, status: el.dataset.status, tab: el.dataset.tab });
ACT.navGroup = el => { const g = el.closest('.nav-g'); if (S.view === el.dataset.v) g.classList.toggle('open'); else go(el.dataset.v); };
ACT.toggleNav = () => { if (window.innerWidth < 1024) document.body.classList.toggle('nav-open'); else $('#shell').classList.toggle('collapsed'); };
ACT.closeNav = () => document.body.classList.remove('nav-open');
ACT.userMenu = () => $('#user-menu').classList.toggle('hidden');
ACT.bell = () => { if (pendingSubs().length) go('approvals', { range: 'all', status: 'pending' }); else if (pendingWds().length) go('withdrawals'); else toast('Nothing pending 🎉'); };
ACT.logout = async () => { try { await signOut(auth); } catch (_) { } location.reload(); };
ACT.page = el => { const k = el.dataset.k, d = Number(el.dataset.d); const map = { users: S.us, wd: S.wd, cm: S.cm, logs: S.lg, tasks: S.tasksUI }; if (map[k]) { map[k].page += d; renderAll(true); } };
