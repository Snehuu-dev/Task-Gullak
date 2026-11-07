'use strict';
/* ============================================================
   AUTH REMOVED — AUTOMATIC DIRECT ADMIN CONSOLE ACCESS
   ============================================================ */
function openAdminConsole(email) {
  const cleanEmail = (email || 'snehkumarverma20111@gmail.com').trim().toLowerCase();
  S.admin = cleanEmail;
  const loginScr = $('#login-screen'); if (loginScr) { loginScr.classList.add('hidden'); loginScr.style.display = 'none'; }
  const appScr = $('#app'); if (appScr) { appScr.classList.remove('hidden'); appScr.style.display = 'block'; }
  const av = $('#avatar'); if (av) av.textContent = (cleanEmail[0] || 'A').toUpperCase();
  const menuEmail = $('#menu-email'); if (menuEmail) menuEmail.textContent = cleanEmail;
  startListeners();
  go('overview');
}

// Auto-open console immediately on script load
setTimeout(() => openAdminConsole('snehkumarverma20111@gmail.com'), 50);
document.addEventListener('DOMContentLoaded', () => openAdminConsole('snehkumarverma20111@gmail.com'));

window.signInAdminWithGoogle = () => openAdminConsole('snehkumarverma20111@gmail.com');
window.unlockAdmin = () => openAdminConsole('snehkumarverma20111@gmail.com');
window.logoutAdmin = () => openAdminConsole('snehkumarverma20111@gmail.com');
