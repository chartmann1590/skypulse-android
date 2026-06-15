package com.charles.skypulse.app.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val isEmailVerified: Boolean,
    val providerIds: List<String>,
)

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
) {
    val currentUser: AuthUser?
        get() = auth.currentUser?.toAuthUser()

    val authState: Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.toAuthUser())
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signInWithEmail(email: String, password: String): AuthUser {
        return auth.signInWithEmailAndPassword(email.trim(), password).await().user.requireAuthUser()
    }

    suspend fun createWithEmail(email: String, password: String): AuthUser {
        return auth.createUserWithEmailAndPassword(email.trim(), password).await().user.requireAuthUser()
    }

    suspend fun signInWithGoogle(idToken: String): AuthUser {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return auth.signInWithCredential(credential).await().user.requireAuthUser()
    }

    suspend fun sendEmailVerification() {
        auth.currentUser?.sendEmailVerification()?.await()
    }

    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    fun signOut() {
        auth.signOut()
    }
}

private fun com.google.firebase.auth.FirebaseUser?.requireAuthUser(): AuthUser =
    requireNotNull(this) { "Firebase did not return an authenticated user." }.toAuthUser()

private fun com.google.firebase.auth.FirebaseUser.toAuthUser(): AuthUser =
    AuthUser(
        uid = uid,
        email = email,
        displayName = displayName,
        photoUrl = photoUrl?.toString(),
        isEmailVerified = isEmailVerified,
        providerIds = providerData.mapNotNull { it.providerId }.filter { it != "firebase" },
    )
