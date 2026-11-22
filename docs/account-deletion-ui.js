import { initFirebase, signInWithEmail, sendPasswordReset } from './app/js/firebase.js';
import { deleteSkyPulseAccount } from './account-deletion-core.js';
import {
  onAuthStateChanged, deleteUser, GoogleAuthProvider, EmailAuthProvider,
  reauthenticateWithCredential, reauthenticateWithPopup, signInWithPopup, signOut,
} from 'https://www.gstatic.com/firebasejs/11.0.0/firebase-auth.js';
import {
  collection, doc, getDocsFromServer, limit, query, where, runTransaction,
  serverTimestamp, writeBatch,
} from 'https://www.gstatic.com/firebasejs/11.0.0/firebase-firestore.js';

const { auth, db } = initFirebase();
const el = id => document.getElementById(id);
const state = el('status');
const panel = el('confirm-panel');
const signin = el('signin-panel');
const signinForm = el('signin-form');
const signinEmail = el('signin-email');
const signinPassword = el('signin-password');
const form = el('delete-form');
const confirm = el('confirmation');
const submit = el('submit-deletion');
const passwordInput = el('password');
const options = [...form.querySelectorAll('input[type=radio]')];
let current = null;
let busy = false;
let done = false;

const pick = () => options.find(radio => radio.checked && !radio.disabled)?.value;
function refresh() {
  for (const button of signin.querySelectorAll('button')) button.disabled = busy;
  el('switch-account').disabled = busy;
  const usePassword = pick() === 'password';
  el('password-row').hidden = !usePassword;
  passwordInput.disabled = busy || !usePassword;
  passwordInput.required = usePassword;
  submit.disabled = busy || done || !current || !pick() || confirm.value !== 'DELETE';
}
function showUser(user) {
  if (done || busy) return;
  const next = user && !user.isAnonymous ? user : null;
  if (next !== current) {
    confirm.value = '';
    passwordInput.value = '';
  }
  current = next;
  signin.hidden = Boolean(current);
  panel.hidden = !current;
  if (!current) {
    state.textContent = 'Sign in to the SkyPulse account you want to delete.';
    refresh();
    return;
  }
  el('account-name').textContent = user.email || user.displayName || 'your current SkyPulse account';
  const supported = new Set(user.providerData.map(entry => entry.providerId));
  if (!user.email) supported.delete('password');
  for (const radio of options) {
    radio.disabled = !supported.has(radio.value);
    radio.closest('label').hidden = radio.disabled;
    if (radio.disabled) radio.checked = false;
  }
  if (!pick()) {
    const first = options.find(radio => !radio.disabled);
    if (first) first.checked = true;
  }
  state.textContent = pick() ? 'Verify your sign-in, then confirm the deletion.' : 'Use your original SkyPulse sign-in method to verify this account.';
  refresh();
}

function firestoreApi(method, password) {
  return {
    isCurrentAccount(account) { return auth.currentUser === account && !account.isAnonymous; },
    async reauthenticate(account) {
      if (method === 'password' && account.email && password) {
        await reauthenticateWithCredential(account, EmailAuthProvider.credential(account.email, password));
      } else if (method === 'google.com') {
        await reauthenticateWithPopup(account, googleProvider());
      } else {
        throw new Error('deletion/no-method');
      }
      await account.getIdToken(true);
    },
    beginDeletion(uid) {
      const marker = doc(db, 'account_deletions', uid);
      return runTransaction(db, async tx => {
        if (!(await tx.get(marker)).exists()) tx.set(marker, { createdAt: serverTimestamp() });
      });
    },
    async readServerPage(uid, group) {
      const filter = group === 'legacy_shared_flights'
        ? query(collection(db, 'shared_flights'), where('sharedBy', '==', uid), limit(8))
        : query(collection(db, 'users', uid, group), limit(group === 'shared_flight_refs' ? 4 : 48));
      return (await getDocsFromServer(filter)).docs;
    },
    async removePage(uid, group, page) {
      if (group === 'shared_flight_refs') {
        // Delete the private references and their existing public snapshots together.
        // A transaction also makes safely retrying interrupted requests possible.
        await runTransaction(db, async tx => {
          const rows = await Promise.all(page.map(row => tx.get(row.ref)));
          const present = rows.filter(row => row.exists());
          const flights = await Promise.all(present.map(row => tx.get(doc(db, 'shared_flights', row.id))));
          for (const row of flights) if (row.exists()) tx.delete(row.ref);
          for (const row of present) tx.delete(row.ref);
        });
        return;
      }
      if (group === 'legacy_shared_flights') {
        await runTransaction(db, async tx => {
          const rows = await Promise.all(page.map(row => tx.get(row.ref)));
          for (const row of rows) if (row.exists() && row.get('sharedBy') === uid) tx.delete(row.ref);
        });
        return;
      }
      const batch = writeBatch(db);
      for (const row of page) batch.delete(row.ref);
      await batch.commit();
    },
    removeAccount(account) { return deleteUser(account); },
  };
}

function googleProvider() {
  const provider = new GoogleAuthProvider();
  provider.setCustomParameters({ prompt: 'select_account' });
  return provider;
}

async function authAction(work, success = '', error = 'Could not sign in. Check your account details or try again in a full browser.') {
  if (busy || done) return;
  busy = true;
  refresh();
  let text = '';
  try {
    await work();
    text = success;
  } catch (e) {
    text = (e?.code === 'auth/popup-blocked' || e?.code === 'auth/popup-closed-by-user')
      ? 'The Google window did not finish. Allow pop-ups or try a full browser.' : error;
  } finally {
    busy = false;
    showUser(auth.currentUser);
    if (text) state.textContent = text;
  }
}
signinForm.addEventListener('submit', event => {
  event.preventDefault();
  if (busy || current || done) return;
  const email = signinEmail.value.trim();
  const password = signinPassword.value;
  signinPassword.value = '';
  void authAction(() => signInWithEmail(email, password));
});
el('signin-google').addEventListener('click', () => {
  if (!current) void authAction(() => signInWithPopup(auth, googleProvider()));
});
el('reset-password').addEventListener('click', () => {
  if (current || !signinEmail.reportValidity()) return;
  void authAction(() => sendPasswordReset(signinEmail.value.trim()),
    'If your SkyPulse email has a password, check your inbox for a reset link.',
    'The reset could not be requested. Check the email address or try again later.');
});
el('switch-account').addEventListener('click', () => {
  void authAction(() => signOut(auth), '', 'Could not sign out. Try again before deleting anything.');
});

form.addEventListener('input', refresh);
form.addEventListener('submit', async event => {
  event.preventDefault();
  const account = current;
  const value = confirm.value;
  const method = pick();
  const password = passwordInput.value;
  if (busy || done || !account || !method || value !== 'DELETE') return;
  busy = true;
  passwordInput.value = '';
  confirm.value = '';
  el('methods').disabled = true;
  state.textContent = 'Verifying your account and deleting your SkyPulse data. Keep this page open.';
  refresh();
  let message = '';
  try {
    await deleteSkyPulseAccount(firestoreApi(method, password), account, value);
    done = true;
    message = 'Your SkyPulse account and the synced data listed above were deleted.';
  } catch (error) {
    const code = error?.code || error?.message;
    if (code === 'deletion/account-changed' || code === 'auth/user-mismatch') {
      message = 'The signed-in account changed or did not match. Sign in to the intended account and try again.';
    } else if (code === 'auth/popup-blocked' || code === 'auth/popup-closed-by-user' || code === 'auth/cancelled-popup-request') {
      message = 'Google verification did not finish. Allow the Google window to open, or try a full browser, then try again.';
    } else if (code === 'auth/invalid-credential' || code === 'auth/wrong-password') {
      message = 'That password did not verify your account. Check it or reset it in SkyPulse Web.';
    } else {
      message = 'Deletion was not confirmed. Some data may already have been removed. If the account still has access, sign in again and repeat these steps to finish.';
    }
  } finally {
    busy = false;
    el('methods').disabled = false;
    if (done) {
      panel.hidden = true;
      signin.hidden = true;
    } else {
      showUser(auth.currentUser);
    }
    state.textContent = message;
    refresh();
  }
});

onAuthStateChanged(auth, showUser, () => {
  if (!busy && !done) state.textContent = 'Could not check sign-in. Reload this page or sign in through SkyPulse Web.';
});
