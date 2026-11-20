/* Secret Hash URL Protection & Login Error Guard */
(function () {
  var SECRET_HASH = '#pass.Gullak#apk.@2026';
  function isSecretValid() {
    var h = location.hash || '';
    return h === SECRET_HASH || h.indexOf(SECRET_HASH) === 0 || sessionStorage.getItem('tg_admin_unlocked') === 'true';
  }
  if (!isSecretValid()) {
    try {
      document.documentElement.innerHTML = '<!DOCTYPE html><html><head><title>404 Not Found</title></head><body style="background:#0d0e15;color:#f4f4fa;display:flex;align-items:center;justify-content:center;height:100vh;margin:0;font-family:sans-serif"><h1>404 Not Found</h1></body></html>';
      if (window.stop) window.stop();
    } catch (_) { }
    return;
  }
  try { sessionStorage.setItem('tg_admin_unlocked', 'true'); } catch (_) { }

  function showLoginError(msg) {
    var el = document.getElementById('login-error');
    if (!el) { alert(msg); return; }
    el.textContent = msg; el.classList.remove('hidden');
  }
  window.__showLoginError = showLoginError;
  var notReady = function () { showLoginError('Admin script failed to load. The Firebase SDK (gstatic.com) may be blocked — check your internet / AdBlock / VPN and hard-refresh.'); };
  window.unlockAdmin = notReady; window.signInAdminWithGoogle = notReady;
  var noise = function (m, f) { m = String(m || ''); return /getContext|Script error|jserror|ResizeObserver/i.test(m) || (f && !/^(file:|content:|about:|blob:)?$/.test(f) && f.indexOf(location.origin) !== 0 && location.origin !== 'null' ? true : /^(content|file):/.test(f || '')); };
  var loginOpen = function () { var l = document.getElementById('login-screen'); return l && !l.classList.contains('hidden'); };
  window.addEventListener('error', function (e) { if (noise(e.message, e.filename) || !loginOpen()) { return; } showLoginError('Script error: ' + (e.message || 'unknown') + (e.lineno ? ' (line ' + e.lineno + ')' : '')); });
  window.addEventListener('unhandledrejection', function (e) { var m = (e.reason && (e.reason.message || e.reason.code)) || e.reason; if (noise(m) || !loginOpen()) return; showLoginError('Error: ' + m); });
})();
