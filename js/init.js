'use strict';
/* Start-up */

/* ============================================================
   INIT
   ============================================================ */
hydrateIcons();
document.addEventListener('visibilitychange', () => { if (!document.hidden && S.admin) renderAll(); });
setInterval(() => { if (S.admin && !document.hidden) { renderAll(); sweepExpired(); sweepOldTasks(); sweepOldBcasts(); } }, 60000);
window.addEventListener('online', () => S.admin && renderAll());
window.addEventListener('offline', () => { const c = $('#conn'); if (c) { c.classList.add('off'); c.lastElementChild.textContent = 'Offline'; } });
