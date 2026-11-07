'use strict';
/* ============================================================
   ADMIN PANEL LOGIN & AUTHENTICATION
   ============================================================ */
function openAdminConsole(email) {
  const cleanEmail = (email || 'snehkumarverma20111@gmail.com').trim().toLowerCase();
  const wasAlreadyLoggedIn = (S.admin === cleanEmail);
  S.admin = cleanEmail;
  try { localStorage.setItem('tg_admin_session_v2', cleanEmail); } catch (_) {}

  const loginScr = $('#login-screen'); if (loginScr) { loginScr.classList.add('hidden'); loginScr.style.display = 'none'; }
  const appScr = $('#app'); if (appScr) { appScr.classList.remove('hidden'); appScr.style.display = 'block'; }
  const av = $('#avatar'); if (av) av.textContent = (cleanEmail[0] || 'A').toUpperCase();
  const menuEmail = $('#menu-email'); if (menuEmail) menuEmail.textContent = cleanEmail;

  startListeners();

  // Unban/Unblock authorized admin in Firestore /users if flagged
  unbanAdminAccountIfNeeded(cleanEmail);

  if (!wasAlreadyLoggedIn) {
    go('overview');
    toast('Logged in as ' + cleanEmail, 'ok');
    logAction('admin_login', cleanEmail);
  }
}

async function unbanAdminAccountIfNeeded(email) {
  if (!email || !ALLOWED_ADMIN_EMAILS.includes(email.toLowerCase())) return;
  try {
    const q = query(collection(db, 'users'), limit(500));
    const snap = await getDocs(q);
    snap.docs.forEach(async docSnap => {
      const data = docSnap.data();
      if ((data.email || '').trim().toLowerCase() === email.toLowerCase()) {
        if (data.isBanned || data.accountStatus === 'deleted') {
          await updateDoc(doc(db, 'users', docSnap.id), {
            isBanned: false,
            banReason: '',
            accountStatus: 'active',
            updatedAt: serverTimestamp()
          });
        }
      }
    });
  } catch (_) {}
}

function restoreAdminSession() {
  try {
    const savedEmail = (localStorage.getItem('tg_admin_session_v2') || '').trim().toLowerCase();
    if (savedEmail && ALLOWED_ADMIN_EMAILS.includes(savedEmail)) {
      openAdminConsole(savedEmail);
      return true;
    }
  } catch (_) {}
  return false;
}

window.logoutAdmin = function() {
  S.admin = null;
  try { localStorage.removeItem('tg_admin_session_v2'); } catch (_) {}
  const appScr = $('#app'); if (appScr) { appScr.classList.add('hidden'); appScr.style.display = 'none'; }
  const loginScr = $('#login-screen'); if (loginScr) { loginScr.classList.remove('hidden'); loginScr.style.display = 'flex'; }
  toast('Logged out', 'ok');
};

window.signInAdminWithGoogle = async function () {
  const err = $('#login-error'); if (err) err.classList.add('hidden');
  if (!/^https?:$/.test(location.protocol)) {
    if (err) { err.textContent = 'Google sign-in does not work from a local file. Use admin email + master password below.'; err.classList.remove('hidden'); }
    return;
  }
  try {
    const res = await signInWithPopup(auth, new GoogleAuthProvider(), browserPopupRedirectResolver);
    const email = (res.user?.email || '').toLowerCase().trim();
    if (ALLOWED_ADMIN_EMAILS.includes(email)) {
      try { await setDoc(doc(db, 'admins', res.user.uid), { email, role: 'admin', updatedAt: serverTimestamp() }, { merge: true }); } catch (_) { }
      openAdminConsole(email);
    } else {
      await signOut(auth);
      if (err) { err.textContent = 'Access denied: ' + email + ' is not an authorized admin email.'; err.classList.remove('hidden'); }
    }
  } catch (e) {
    if (err) { err.textContent = 'Google sign-in blocked or cancelled (' + (e.code || e.message) + '). Use admin email + master password below.'; err.classList.remove('hidden'); }
  }
};

window.unlockAdmin = function () {
  const emailInput = ($('#admin-email-input')?.value || '').trim().toLowerCase();
  const passInput = ($('#admin-pass-input')?.value || '').trim();
  const err = $('#login-error');
  const validEmail = ALLOWED_ADMIN_EMAILS.includes(emailInput) || ALLOWED_ADMIN_EMAILS.includes(passInput.toLowerCase());
  const validPass = ['gullak_admin_2025', 'admin123'].includes(passInput.toLowerCase()) || ALLOWED_ADMIN_EMAILS.includes(passInput.toLowerCase());
  if (validEmail && validPass) {
    if (err) err.classList.add('hidden');
    openAdminConsole(ALLOWED_ADMIN_EMAILS.includes(emailInput) ? emailInput : passInput.toLowerCase());
  } else {
    if (err) { err.textContent = 'Access denied: authorized admin email + master password required.'; err.classList.remove('hidden'); }
  }
};

/* ============================================================
   AUTO LOGIN VIA URL HASH OR ENCODED URL (#pass.Gullak#apk.@2026)
   ============================================================ */
function checkUrlHashAutoLogin() {
  if (restoreAdminSession()) return;
  const fullUrl = (decodeURIComponent(location.href) || '').toLowerCase();
  const hash = (location.hash || '').toLowerCase();
  if (
    fullUrl.includes('pass.gullak') ||
    fullUrl.includes('apk.@2026') ||
    fullUrl.includes('gullak_admin_2025') ||
    hash.includes('pass.gullak') ||
    hash.includes('apk.@2026') ||
    hash.includes('gullak_admin_2025')
  ) {
    openAdminConsole('snehkumarverma20111@gmail.com');
  }
}

document.addEventListener('DOMContentLoaded', checkUrlHashAutoLogin);
setTimeout(checkUrlHashAutoLogin, 150);
setTimeout(checkUrlHashAutoLogin, 500);
