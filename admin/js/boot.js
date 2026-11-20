/* Loads Firebase, then every app file in order. Files download in parallel but run one after another. */
(function () {
  var V = '10.14.1', base = 'https://www.gstatic.com/firebasejs/' + V + '/';
  var FB = {'app': ['initializeApp'], 'auth': ['initializeAuth', 'browserLocalPersistence', 'inMemoryPersistence', 'browserPopupRedirectResolver', 'GoogleAuthProvider', 'signInWithPopup', 'signOut', 'signInWithEmailAndPassword', 'onAuthStateChanged', 'createUserWithEmailAndPassword', 'sendEmailVerification', 'sendPasswordResetEmail'], 'firestore': ['getFirestore', 'initializeFirestore', 'persistentLocalCache', 'persistentMultipleTabManager', 'collection', 'doc', 'onSnapshot', 'setDoc', 'updateDoc', 'deleteDoc', 'getDoc', 'getDocs', 'query', 'where', 'orderBy', 'limit', 'serverTimestamp', 'runTransaction', 'arrayUnion', 'arrayRemove']};
  var FILES = ['js/config.js', 'js/helpers.js', 'js/state.js', 'js/shell.js', 'js/listeners.js', 'js/pages/overview.js', 'js/pages/approvals.js', 'js/pages/tasks.js', 'js/pages/comments.js', 'js/pages/users.js', 'js/pages/withdrawals.js', 'js/pages/reports.js', 'js/pages/notifications.js', 'js/pages/bulk-access.js', 'js/pages/system.js', 'js/init.js'];
  function fail(m) { if (window.__showLoginError) window.__showLoginError(m); else alert(m); }
  Promise.all([import(base + 'firebase-app.js'), import(base + 'firebase-auth.js'), import(base + 'firebase-firestore.js')]).then(function (m) {
    [m[0], m[1], m[2]].forEach(function (ns, i) { FB[['app', 'auth', 'firestore'][i]].forEach(function (n) { globalThis[n] = ns[n]; }); });
    FILES.forEach(function (f, i) {
      var s = document.createElement('script'); s.src = f + '?v=1.1.2'; s.async = false;
      s.onerror = function () { fail('Could not load ' + f + '. Open the site from a web server or Firebase Hosting.'); };
      document.body.appendChild(s);
    });
  }).catch(function (e) { fail('Firebase SDK failed to load (network / AdBlock / CDN blocked). Check your internet and reload the page.'); console.error(e); });
})();
