'use strict';
/* ============================================================
   FIREBASE BRIDGE — SDK is imported in index.html and exposed as window.FB
   (to change the SDK version, edit the import URLs in index.html)
   ============================================================ */
const {
  initializeApp,
  initializeAuth,
  browserLocalPersistence,
  inMemoryPersistence,
  browserPopupRedirectResolver,
  GoogleAuthProvider,
  signInWithPopup,
  signOut,
  getFirestore,
  collection,
  doc,
  onSnapshot,
  setDoc,
  updateDoc,
  deleteDoc,
  getDoc,
  getDocs,
  query,
  orderBy,
  limit,
  serverTimestamp,
  runTransaction,
  arrayUnion,
  arrayRemove
} = window.FB;
