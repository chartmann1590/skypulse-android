/* auth.js — Authentication modal */

import { esc } from './utils.js';

let _modalId = null;
let _firebaseMod = null;
let _onAuthChange = null;
let _activeTab = 'signin';

/**
 * Initialize the auth module.
 */
export function init(modalId, firebaseMod, onAuthChangeCb) {
  _modalId = modalId;
  _firebaseMod = firebaseMod;
  _onAuthChange = onAuthChangeCb;
  _renderModal();
  _bindOverlay();
}

export function showModal() {
  const modal = document.getElementById(_modalId);
  if (modal) modal.classList.remove('hidden');
}

export function hideModal() {
  const modal = document.getElementById(_modalId);
  if (modal) modal.classList.add('hidden');
}

/**
 * Update header avatar + verification banner based on auth state.
 */
export function renderUserState(user) {
  // Auth buttons
  const btns = [document.getElementById('auth-btn'), document.getElementById('auth-btn-mobile')];
  btns.forEach(btn => {
    if (!btn) return;
    if (user) {
      if (user.photoURL) {
        btn.innerHTML = `<img src="${esc(user.photoURL)}" class="w-full h-full object-cover" alt="Profile"/>`;
      } else {
        const initial = (user.displayName || user.email || '?')[0].toUpperCase();
        btn.innerHTML = `<span class="font-title-md text-on-primary text-sm font-bold" style="background:transparent;">${esc(initial)}</span>`;
        btn.style.background = '#00363a';
      }
    } else {
      btn.innerHTML = `<span class="material-symbols-outlined text-on-surface-variant text-sm">person</span>`;
      btn.style.background = '';
    }
  });

  // Email verification banner
  const banner = document.getElementById('verify-banner');
  if (banner) {
    if (user && !user.emailVerified && user.providerData?.[0]?.providerId === 'password') {
      banner.classList.remove('hidden');
    } else {
      banner.classList.add('hidden');
    }
  }
}

function _renderModal() {
  const content = document.getElementById('auth-modal-content');
  if (!content) return;

  content.innerHTML = `
    <!-- Drag handle -->
    <div class="w-12 h-1.5 bg-outline-variant rounded-full mx-auto mb-6"></div>

    <!-- Tab switcher -->
    <div class="flex border-b border-glass-stroke mb-6">
      <button class="auth-tab-btn flex-1 pb-3 font-title-md text-title-md text-on-surface-variant transition-all active" data-tab="signin">Sign In</button>
      <button class="auth-tab-btn flex-1 pb-3 font-title-md text-title-md text-on-surface-variant transition-all" data-tab="create">Create Account</button>
    </div>

    <!-- Signed-in state (hidden by default) -->
    <div id="auth-signed-in" class="hidden text-center py-4">
      <div id="auth-user-avatar" class="w-16 h-16 rounded-full bg-surface-container-high border border-glass-stroke flex items-center justify-center mx-auto mb-3 overflow-hidden">
        <span class="material-symbols-outlined text-on-surface-variant text-3xl">person</span>
      </div>
      <p id="auth-user-name" class="font-title-md text-title-md text-text-high mb-1"></p>
      <p id="auth-user-email" class="font-label-sm text-label-sm text-on-surface-variant mb-6"></p>
      <button id="auth-signout-btn"
        class="w-full py-3 px-4 rounded-full border border-error/50 text-error font-title-md text-title-md hover:bg-error/10 transition-colors flex items-center justify-center gap-2">
        <span class="material-symbols-outlined text-[20px]">logout</span>
        Sign Out
      </button>
    </div>

    <!-- Sign in / Create forms -->
    <div id="auth-forms">
      <!-- Google button -->
      <button id="auth-google-btn"
        class="w-full py-3 px-4 rounded-full bg-white text-[#202124] font-title-md text-title-md flex items-center justify-center gap-3 hover:bg-gray-50 active:scale-95 transition-all shadow-md mb-4">
        <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
          <path d="M17.64 9.2c0-.637-.057-1.251-.164-1.84H9v3.481h4.844c-.209 1.125-.843 2.078-1.796 2.717v2.258h2.908c1.702-1.567 2.684-3.874 2.684-6.615z" fill="#4285F4"/>
          <path d="M9 18c2.43 0 4.467-.806 5.956-2.18l-2.908-2.259c-.806.54-1.837.86-3.048.86-2.344 0-4.328-1.584-5.036-3.711H.957v2.332A8.997 8.997 0 009 18z" fill="#34A853"/>
          <path d="M3.964 10.71A5.41 5.41 0 013.682 9c0-.593.102-1.17.282-1.71V4.958H.957A8.996 8.996 0 000 9c0 1.452.348 2.827.957 4.042l3.007-2.332z" fill="#FBBC05"/>
          <path d="M9 3.58c1.321 0 2.508.454 3.44 1.345l2.582-2.58C13.463.891 11.426 0 9 0A8.997 8.997 0 00.957 4.958L3.964 7.29C4.672 5.163 6.656 3.58 9 3.58z" fill="#EA4335"/>
        </svg>
        Continue with Google
      </button>

      <!-- Divider -->
      <div class="flex items-center gap-3 mb-4">
        <div class="flex-1 h-px bg-glass-stroke"></div>
        <span class="font-label-sm text-label-sm text-on-surface-variant">or</span>
        <div class="flex-1 h-px bg-glass-stroke"></div>
      </div>

      <!-- Email/Password form -->
      <form id="auth-email-form" class="flex flex-col gap-3">
        <div id="auth-error" class="hidden text-error font-label-sm text-label-sm bg-error-container/20 border border-error/30 rounded-lg px-3 py-2"></div>
        <input id="auth-email" type="email" autocomplete="email"
          class="glass-input w-full px-4 py-3 rounded-lg font-body-md text-body-md"
          placeholder="Email address"/>
        <input id="auth-password" type="password" autocomplete="current-password"
          class="glass-input w-full px-4 py-3 rounded-lg font-body-md text-body-md"
          placeholder="Password"/>
        <div id="auth-name-field" class="hidden">
          <input id="auth-displayname" type="text" autocomplete="name"
            class="glass-input w-full px-4 py-3 rounded-lg font-body-md text-body-md"
            placeholder="Display name (optional)"/>
        </div>
        <button type="submit" id="auth-submit-btn"
          class="w-full py-3 px-4 rounded-full bg-primary text-pitch-black font-title-md text-title-md hover:bg-primary-fixed active:scale-95 transition-all">
          Sign In
        </button>
      </form>

      <!-- Forgot password -->
      <div id="auth-forgot-row" class="mt-3 text-center">
        <button id="auth-forgot-btn" class="font-label-sm text-label-sm text-primary hover:text-primary-fixed transition-colors">
          Forgot password?
        </button>
      </div>
    </div>`;

  _bindModalEvents();
}

function _bindModalEvents() {
  // Tab switching
  document.querySelectorAll('.auth-tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      _activeTab = btn.dataset.tab;
      document.querySelectorAll('.auth-tab-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      _updateFormForTab();
    });
  });

  // Google sign in
  document.getElementById('auth-google-btn')?.addEventListener('click', async () => {
    try {
      _setError('');
      await _firebaseMod.signInWithGoogle();
      hideModal();
    } catch (err) {
      _setError(err.message);
    }
  });

  // Email form submit
  document.getElementById('auth-email-form')?.addEventListener('submit', async e => {
    e.preventDefault();
    const email = document.getElementById('auth-email')?.value.trim();
    const password = document.getElementById('auth-password')?.value;
    if (!email || !password) return;
    _setError('');
    const submitBtn = document.getElementById('auth-submit-btn');
    if (submitBtn) submitBtn.disabled = true;
    try {
      if (_activeTab === 'create') {
        await _firebaseMod.createEmailAccount(email, password);
      } else {
        await _firebaseMod.signInWithEmail(email, password);
      }
      hideModal();
    } catch (err) {
      _setError(_friendlyError(err));
    } finally {
      if (submitBtn) submitBtn.disabled = false;
    }
  });

  // Forgot password
  document.getElementById('auth-forgot-btn')?.addEventListener('click', async () => {
    const email = document.getElementById('auth-email')?.value.trim();
    if (!email) { _setError('Enter your email address first.'); return; }
    try {
      await _firebaseMod.sendPasswordReset(email);
      _setError('');
      alert('Password reset email sent. Check your inbox.');
    } catch (err) {
      _setError(err.message);
    }
  });

  // Sign out
  document.getElementById('auth-signout-btn')?.addEventListener('click', async () => {
    await _firebaseMod.signOut();
    hideModal();
  });

  // Resend verification
  document.getElementById('resend-verify')?.addEventListener('click', async () => {
    await _firebaseMod.resendVerification?.();
    alert('Verification email sent.');
  });

  // Dismiss banner
  document.getElementById('dismiss-banner')?.addEventListener('click', () => {
    document.getElementById('verify-banner')?.classList.add('hidden');
  });
}

function _updateFormForTab() {
  const submitBtn = document.getElementById('auth-submit-btn');
  const nameField = document.getElementById('auth-name-field');
  const forgotRow = document.getElementById('auth-forgot-row');
  if (submitBtn) submitBtn.textContent = _activeTab === 'create' ? 'Create Account' : 'Sign In';
  if (nameField) nameField.classList.toggle('hidden', _activeTab !== 'create');
  if (forgotRow) forgotRow.classList.toggle('hidden', _activeTab === 'create');
  _setError('');
}

function _setError(msg) {
  const el = document.getElementById('auth-error');
  if (!el) return;
  if (msg) {
    el.textContent = msg;
    el.classList.remove('hidden');
  } else {
    el.classList.add('hidden');
  }
}

function _friendlyError(err) {
  const code = err.code || '';
  if (code.includes('user-not-found') || code.includes('wrong-password') || code.includes('invalid-credential'))
    return 'Invalid email or password.';
  if (code.includes('email-already-in-use'))
    return 'An account with this email already exists. Try signing in.';
  if (code.includes('weak-password'))
    return 'Password must be at least 6 characters.';
  if (code.includes('too-many-requests'))
    return 'Too many attempts. Please try again later.';
  return err.message || 'An error occurred.';
}

function _bindOverlay() {
  document.getElementById('auth-modal-overlay')?.addEventListener('click', hideModal);
}

/**
 * Update the modal to reflect the signed-in user.
 */
export function updateModalForUser(user) {
  const signedInDiv = document.getElementById('auth-signed-in');
  const formsDiv = document.getElementById('auth-forms');
  if (!signedInDiv || !formsDiv) return;

  if (user) {
    signedInDiv.classList.remove('hidden');
    formsDiv.classList.add('hidden');
    const nameEl = document.getElementById('auth-user-name');
    const emailEl = document.getElementById('auth-user-email');
    const avatarEl = document.getElementById('auth-user-avatar');
    if (nameEl) nameEl.textContent = user.displayName || 'Signed in';
    if (emailEl) emailEl.textContent = user.email || '';
    if (avatarEl) {
      if (user.photoURL) {
        avatarEl.innerHTML = `<img src="${esc(user.photoURL)}" class="w-full h-full object-cover" alt="Profile"/>`;
      } else {
        const initial = (user.displayName || user.email || '?')[0].toUpperCase();
        avatarEl.innerHTML = `<span class="font-headline-lg-mobile text-on-surface text-2xl">${esc(initial)}</span>`;
      }
    }
    // Re-bind sign out
    document.getElementById('auth-signout-btn')?.addEventListener('click', async () => {
      await _firebaseMod.signOut();
      hideModal();
    });
  } else {
    signedInDiv.classList.add('hidden');
    formsDiv.classList.remove('hidden');
  }
}
