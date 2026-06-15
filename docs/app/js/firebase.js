/* firebase.js — Firebase v11 integration via CDN ES modules */

import { initializeApp } from 'https://www.gstatic.com/firebasejs/11.0.0/firebase-app.js';
import {
  getAuth,
  GoogleAuthProvider,
  signInWithPopup,
  signInWithRedirect,
  getRedirectResult,
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  sendEmailVerification,
  sendPasswordResetEmail,
  signOut as fbSignOut,
  onAuthStateChanged,
  signInAnonymously,
} from 'https://www.gstatic.com/firebasejs/11.0.0/firebase-auth.js';
import {
  getFirestore,
  collection,
  doc,
  getDocs,
  setDoc,
  deleteDoc,
  addDoc,
  serverTimestamp,
} from 'https://www.gstatic.com/firebasejs/11.0.0/firebase-firestore.js';

const firebaseConfig = {
  apiKey: "AIzaSyDAE1ILKJgqQ-xBlhG1axCxhRhuqEuwYcc",
  authDomain: "skypulse-tracker-2026.firebaseapp.com",
  projectId: "skypulse-tracker-2026",
  storageBucket: "skypulse-tracker-2026.firebasestorage.app",
  messagingSenderId: "871581888905",
  appId: "FIREBASE_WEB_APP_ID_PLACEHOLDER"
};

let _auth = null;
let _db = null;
let _app = null;

/**
 * Initialize Firebase. Safe to call multiple times.
 */
export function initFirebase() {
  if (_app) return { auth: _auth, db: _db };
  _app = initializeApp(firebaseConfig);
  _auth = getAuth(_app);
  _db = getFirestore(_app);
  return { auth: _auth, db: _db };
}

function isMobileDevice() {
  return /iPhone|iPad|iPod|Android/i.test(navigator.userAgent);
}

/**
 * Sign in with Google. Uses popup on desktop, redirect on mobile.
 */
export async function signInWithGoogle(forceMobile = false) {
  const provider = new GoogleAuthProvider();
  provider.setCustomParameters({ client_id: '871581888905-n2gfa3pup119urvv7l9ttj9bbtllm1av.apps.googleusercontent.com' });
  if (forceMobile || isMobileDevice()) {
    return signInWithRedirect(_auth, provider);
  }
  return signInWithPopup(_auth, provider);
}

/**
 * Handle Google redirect result — call on page load.
 */
export async function handleGoogleRedirect() {
  try {
    const result = await getRedirectResult(_auth);
    return result?.user || null;
  } catch (err) {
    console.warn('[Firebase] Redirect result error:', err.message);
    return null;
  }
}

/**
 * Sign in with email/password.
 */
export async function signInWithEmail(email, password) {
  const cred = await signInWithEmailAndPassword(_auth, email, password);
  return cred.user;
}

/**
 * Create email account + send verification email.
 */
export async function createEmailAccount(email, password) {
  const cred = await createUserWithEmailAndPassword(_auth, email, password);
  await sendEmailVerification(cred.user);
  return cred.user;
}

/**
 * Send password reset email.
 */
export async function sendPasswordReset(email) {
  return sendPasswordResetEmail(_auth, email);
}

/**
 * Resend email verification to current user.
 */
export async function resendVerification() {
  if (_auth.currentUser) {
    return sendEmailVerification(_auth.currentUser);
  }
}

/**
 * Sign out.
 */
export async function signOut() {
  return fbSignOut(_auth);
}

/**
 * Subscribe to auth state changes.
 */
export function onAuthChange(callback) {
  return onAuthStateChanged(_auth, callback);
}

/* ── Saved Aircraft ── */
export async function listSavedAircraft(uid) {
  const snap = await getDocs(collection(_db, 'users', uid, 'saved_aircraft'));
  return snap.docs.map(d => ({ id: d.id, ...d.data() }));
}

export async function saveSavedAircraft(uid, data) {
  const ref = doc(_db, 'users', uid, 'saved_aircraft', data.hex);
  await setDoc(ref, { ...data, savedAtEpochMs: Date.now() }, { merge: true });
}

export async function deleteSavedAircraft(uid, hex) {
  return deleteDoc(doc(_db, 'users', uid, 'saved_aircraft', hex));
}

/* ── Saved Airports ── */
export async function listSavedAirports(uid) {
  const snap = await getDocs(collection(_db, 'users', uid, 'saved_airports'));
  return snap.docs.map(d => ({ id: d.id, ...d.data() }));
}

export async function saveSavedAirport(uid, data) {
  const ref = doc(_db, 'users', uid, 'saved_airports', data.airportId || data.icao || String(data.id));
  await setDoc(ref, { ...data, savedAtEpochMs: Date.now() }, { merge: true });
}

export async function deleteSavedAirport(uid, id) {
  return deleteDoc(doc(_db, 'users', uid, 'saved_airports', String(id)));
}

/* ── Saved Areas ── */
export async function listSavedAreas(uid) {
  const snap = await getDocs(collection(_db, 'users', uid, 'saved_areas'));
  return snap.docs.map(d => ({ id: d.id, ...d.data() }));
}

export async function saveSavedArea(uid, data) {
  const areaId = data.id || String(Date.now());
  const ref = doc(_db, 'users', uid, 'saved_areas', areaId);
  await setDoc(ref, { ...data, id: areaId, savedAtEpochMs: Date.now() }, { merge: true });
  return areaId;
}

export async function deleteSavedArea(uid, id) {
  return deleteDoc(doc(_db, 'users', uid, 'saved_areas', String(id)));
}

/* ── Shared Flights ── */
export async function createSharedFlight(data) {
  // Sign in anonymously if not authenticated
  if (!_auth.currentUser) {
    await signInAnonymously(_auth);
  }
  const ref = await addDoc(collection(_db, 'shared_flights'), {
    ...data,
    createdAt: serverTimestamp(),
  });
  return ref.id;
}

/* ── Getters for already-initialized instances ── */
export function getFirebaseAuth() { return _auth; }
export function getFirebaseDb() { return _db; }
