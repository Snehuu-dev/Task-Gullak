'use strict';
/* ============================================================
   LOGIN
   ============================================================ */
function openAdminConsole(email) {
  S.admin = email;
  $('#login-screen').classList.add('hidden');
  $('#app').classList.remove('hidden');
  $('#avatar').textContent = (email[0] || 'A').toUpperCase();
  $('#menu-email').textContent = email;
  startListeners();
  go('overview');
  toast('Logged in as ' + email, 'ok');
  logAction('admin_login', email);
}
window.signInAdminWithGoogle = async function () {
  const err = $('#login-error'); err.classList.add('hidden');
  if (!/^https?:$/.test(location.protocol)) { err.textContent = 'Google sign-in does not work from a local file (content:// or file://). Use the admin email + master password below, or open the page over https (e.g. Firebase Hosting).'; err.classList.remove('hidden'); return; }
  try {
    const res = await signInWithPopup(auth, new GoogleAuthProvider(), browserPopupRedirectResolver);
    const email = (res.user?.email || '').toLowerCase().trim();
    if (ALLOWED_ADMIN_EMAILS.includes(email)) {
      try { await setDoc(doc(db, 'admins', res.user.uid), { email, role: 'admin', updatedAt: serverTimestamp() }, { merge: true }); } catch (_) { }
      openAdminConsole(email);
    } else {
      await signOut(auth);
      err.textContent = 'Access denied: ' + email + ' is not an authorized admin email.'; err.classList.remove('hidden');
    }
  } catch (e) {
    err.textContent = 'Google sign-in blocked or cancelled (' + (e.code || e.message) + '). Use admin email + master password below.'; err.classList.remove('hidden');
  }
};
window.unlockAdmin = function () {
  const emailInput = ($('#admin-email-input')?.value || '').trim().toLowerCase();
  const passInput = ($('#admin-pass-input')?.value || '').trim();
  const err = $('#login-error');
  const validEmail = ALLOWED_ADMIN_EMAILS.includes(emailInput) || ALLOWED_ADMIN_EMAILS.includes(passInput.toLowerCase());
  const validPass = ['gullak_admin_2025', 'admin123'].includes(passInput.toLowerCase()) || ALLOWED_ADMIN_EMAILS.includes(passInput.toLowerCase());
  if (validEmail && validPass) { err.classList.add('hidden'); openAdminConsole(ALLOWED_ADMIN_EMAILS.includes(emailInput) ? emailInput : passInput.toLowerCase()); }
  else { err.textContent = 'Access denied: authorized admin email + master password required.'; err.classList.remove('hidden'); }
};

/* ============================================================
   AUTO LOGIN VIA URL HASH (#pass.Gullak#apk.@2026)
   ============================================================ */
function checkUrlHashAutoLogin() {
  const hash = (location.hash || '').toLowerCase();
  if (
    hash.includes('pass.gullak') ||
    hash.includes('apk.@2026') ||
    hash.includes('gullak_admin_2025') ||
    hash.includes('admin')
  ) {
    openAdminConsole('snehkumarverma20111@gmail.com');
  }
}
document.addEventListener('DOMContentLoaded', checkUrlHashAutoLogin);
setTimeout(checkUrlHashAutoLogin, 300);
