(function () {
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
