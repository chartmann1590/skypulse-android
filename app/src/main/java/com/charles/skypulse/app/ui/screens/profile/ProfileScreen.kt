package com.charles.skypulse.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.charles.skypulse.app.data.auth.AuthUser
import com.charles.skypulse.app.ui.theme.SkyColors
import com.charles.skypulse.app.ui.theme.SkyType

@Composable
fun ProfileScreen(
    showBack: Boolean = true,
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 40.dp, start = 4.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = SkyColors.OnSurface)
                }
            } else {
                Spacer(modifier = Modifier.padding(start = 16.dp))
            }
            Text("Profile", style = SkyType.HeadlineLgMobile, color = SkyColors.TextHigh)
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            state.user?.let { user ->
                SignedInProfile(
                    user = user,
                    loading = state.loading,
                    message = state.message,
                    error = state.error,
                    onSendVerification = viewModel::sendVerificationEmail,
                    onSignOut = viewModel::signOut,
                )
            } ?: SignedOutProfile(
                state = state,
                onEmailChanged = viewModel::onEmailChanged,
                onPasswordChanged = viewModel::onPasswordChanged,
                onConfirmPasswordChanged = viewModel::onConfirmPasswordChanged,
                onGoogle = { viewModel.signInWithGoogle(context) },
                onSignIn = viewModel::signInWithEmail,
                onSignUp = viewModel::createWithEmail,
                onReset = viewModel::sendPasswordReset,
            )
        }
    }
}

@Composable
private fun SignedInProfile(
    user: AuthUser,
    loading: Boolean,
    message: String?,
    error: String?,
    onSendVerification: () -> Unit,
    onSignOut: () -> Unit,
) {
    ProfileCard {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(SkyColors.PrimaryFixedDim.copy(alpha = 0.16f))
                    .border(1.dp, SkyColors.PrimaryFixedDim.copy(alpha = 0.6f), CircleShape)
                    .padding(18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Person, null, tint = SkyColors.PrimaryFixedDim)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    user.displayName ?: "SkyPulse account",
                    style = SkyType.TitleMd,
                    color = SkyColors.TextHigh,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    user.email ?: user.uid,
                    style = SkyType.BodyMd,
                    color = SkyColors.OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        StatusLine(
            label = "Email",
            value = if (user.isEmailVerified) "Verified" else "Not verified",
            good = user.isEmailVerified,
        )
        StatusLine(
            label = "Sign-in providers",
            value = user.providerIds.joinToString(", ").ifBlank { "Firebase" },
            good = true,
        )

        if (!user.isEmailVerified && user.email != null) {
            AuthButton(
                text = "Send verification email",
                enabled = !loading,
                leadingIcon = Icons.Filled.VerifiedUser,
                onClick = onSendVerification,
            )
        }
        AuthButton(
            text = "Sign out",
            enabled = !loading,
            leadingIcon = Icons.AutoMirrored.Filled.Logout,
            filled = false,
            onClick = onSignOut,
        )
        FeedbackText(message = message, error = error)
    }
}

@Composable
private fun SignedOutProfile(
    state: ProfileUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onConfirmPasswordChanged: (String) -> Unit,
    onGoogle: () -> Unit,
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
    onReset: () -> Unit,
) {
    ProfileCard {
        Text("Sign in to SkyPulse", style = SkyType.TitleMd, color = SkyColors.TextHigh)
        Text(
            "Use Google or create an email account to keep your profile ready for synced features.",
            style = SkyType.BodyMd,
            color = SkyColors.OnSurfaceVariant,
        )
        AuthButton(
            text = "Continue with Google",
            enabled = !state.loading,
            leadingIcon = Icons.AutoMirrored.Filled.Login,
            onClick = onGoogle,
        )

        DividerLabel("Email")
        AuthTextField(
            value = state.email,
            onValueChange = onEmailChanged,
            label = "Email address",
            keyboardType = KeyboardType.Email,
            leadingIcon = Icons.Filled.Email,
        )
        AuthTextField(
            value = state.password,
            onValueChange = onPasswordChanged,
            label = "Password",
            keyboardType = KeyboardType.Password,
            leadingIcon = Icons.Filled.Key,
            isPassword = true,
        )
        AuthTextField(
            value = state.confirmPassword,
            onValueChange = onConfirmPasswordChanged,
            label = "Confirm password",
            keyboardType = KeyboardType.Password,
            leadingIcon = Icons.Filled.Key,
            isPassword = true,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            AuthButton(
                text = "Sign in",
                enabled = state.canSubmit,
                modifier = Modifier.weight(1f),
                onClick = onSignIn,
            )
            AuthButton(
                text = "Sign up",
                enabled = state.canSignUp,
                modifier = Modifier.weight(1f),
                filled = false,
                onClick = onSignUp,
            )
        }
        TextButton(
            enabled = state.email.isNotBlank() && !state.loading,
            onClick = onReset,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text("Reset password", color = SkyColors.PrimaryFixedDim)
        }
        if (state.loading) {
            CircularProgressIndicator(color = SkyColors.PrimaryFixedDim, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        FeedbackText(message = state.message, error = state.error)
    }
}

@Composable
private fun ProfileCard(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SkyColors.SurfaceContainerLow.copy(alpha = 0.6f), shape)
            .border(1.dp, SkyColors.GlassStroke, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        leadingIcon = { Icon(leadingIcon, null) },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun AuthButton(
    text: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    filled: Boolean = true,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    val background = when {
        !enabled -> SkyColors.SurfaceContainerHighest
        filled -> SkyColors.PrimaryFixedDim
        else -> SkyColors.SurfaceContainerLow
    }
    val contentColor = when {
        !enabled -> SkyColors.Outline
        filled -> SkyColors.PitchBlack
        else -> SkyColors.OnSurface
    }
    Row(
        modifier = modifier
            .clip(shape)
            .background(background, shape)
            .border(1.dp, if (filled) Color.Transparent else SkyColors.GlassStroke, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, null, tint = contentColor)
            Spacer(Modifier.padding(start = 8.dp))
        }
        Text(text, style = SkyType.TitleMd, color = contentColor, maxLines = 1)
    }
}

@Composable
private fun StatusLine(label: String, value: String, good: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = SkyType.BodyMd, color = SkyColors.OnSurfaceVariant)
        Text(value, style = SkyType.BodyMd, color = if (good) SkyColors.PrimaryFixedDim else SkyColors.Error)
    }
}

@Composable
private fun DividerLabel(text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(SkyColors.GlassStroke),
        )
        Text(text, style = SkyType.LabelSm, color = SkyColors.OnSurfaceVariant)
    }
}

@Composable
private fun FeedbackText(message: String?, error: String?) {
    val text = error ?: message ?: return
    Text(
        text = text,
        style = SkyType.BodyMd,
        color = if (error != null) SkyColors.Error else SkyColors.PrimaryFixedDim,
    )
}
