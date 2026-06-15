package com.charles.skypulse.app.ui.screens.profile

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charles.skypulse.app.R
import com.charles.skypulse.app.data.auth.AuthRepository
import com.charles.skypulse.app.data.auth.AuthUser
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: AuthUser? = null,
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val loading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
) {
    val canSubmit: Boolean
        get() = email.isNotBlank() && password.length >= 6 && !loading

    val canSignUp: Boolean
        get() = canSubmit && password == confirmPassword
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val formState = MutableStateFlow(ProfileUiState(user = authRepository.currentUser))

    val uiState: StateFlow<ProfileUiState> = combine(authRepository.authState, formState) { user, form ->
        form.copy(user = user)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = formState.value,
    )

    fun onEmailChanged(value: String) = updateForm { copy(email = value, message = null, error = null) }

    fun onPasswordChanged(value: String) = updateForm { copy(password = value, message = null, error = null) }

    fun onConfirmPasswordChanged(value: String) = updateForm { copy(confirmPassword = value, message = null, error = null) }

    fun signInWithEmail() = runAuthAction {
        authRepository.signInWithEmail(uiState.value.email, uiState.value.password)
        "Signed in."
    }

    fun createWithEmail() {
        val state = uiState.value
        if (state.password != state.confirmPassword) {
            updateForm { copy(error = "Passwords do not match.", message = null) }
            return
        }
        runAuthAction {
            authRepository.createWithEmail(state.email, state.password)
            authRepository.sendEmailVerification()
            "Account created. Check your email for a verification link."
        }
    }

    fun signInWithGoogle(context: Context) = runAuthAction {
        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(context.getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        val result = credentialManager.getCredential(context, request)
        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            authRepository.signInWithGoogle(googleCredential.idToken)
            "Signed in with Google."
        } else {
            error("Google did not return an ID token.")
        }
    }

    fun sendVerificationEmail() = runAuthAction {
        authRepository.sendEmailVerification()
        "Verification email sent."
    }

    fun sendPasswordReset() = runAuthAction {
        authRepository.sendPasswordReset(uiState.value.email)
        "Password reset email sent."
    }

    fun signOut() {
        authRepository.signOut()
        updateForm {
            copy(
                password = "",
                confirmPassword = "",
                message = "Signed out.",
                error = null,
            )
        }
    }

    fun clearMessage() = updateForm { copy(message = null, error = null) }

    private fun runAuthAction(block: suspend () -> String) {
        viewModelScope.launch {
            updateForm { copy(loading = true, message = null, error = null) }
            runCatching { block() }
                .onSuccess { message ->
                    updateForm {
                        copy(
                            loading = false,
                            password = "",
                            confirmPassword = "",
                            message = message,
                            error = null,
                        )
                    }
                }
                .onFailure { throwable ->
                    updateForm {
                        copy(
                            loading = false,
                            message = null,
                            error = throwable.toFriendlyMessage(),
                        )
                    }
                }
        }
    }

    private fun updateForm(reducer: ProfileUiState.() -> ProfileUiState) {
        formState.update(reducer)
    }
}

private fun Throwable.toFriendlyMessage(): String =
    when (this) {
        is GetCredentialException -> "Google sign-in was canceled or no Google account is available."
        is GoogleIdTokenParsingException -> "Google sign-in returned an unreadable token."
        else -> message?.takeIf { it.isNotBlank() } ?: "Authentication failed."
    }
