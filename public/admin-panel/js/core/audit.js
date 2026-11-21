'use strict';
/* ============================================================
   AUDIT LOG
   ============================================================ */
async function logAction(action, target, details = {}) {
  try {
    await setDoc(doc(collection(db, 'adminLogs')), {
      action, target: String(target || ''), details, adminEmail: S.admin, createdAt: serverTimestamp(), createdMs: Date.now()
    });
  } catch (e) { try { const a = JSON.parse(localStorage.getItem('tg_logs') || '[]'); a.unshift({ id: 'local_' + Date.now(), action, target: String(target || ''), details, adminEmail: S.admin, createdMs: Date.now() }); localStorage.setItem('tg_logs', JSON.stringify(a.slice(0, 200))); } catch (_) { } }
}
