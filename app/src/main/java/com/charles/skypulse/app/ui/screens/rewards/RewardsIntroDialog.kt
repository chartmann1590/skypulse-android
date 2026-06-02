package com.charles.skypulse.app.ui.screens.rewards

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.charles.skypulse.app.ui.theme.SkyColors

/** One-time explainer shown on first open so users immediately understand the ad-free rewards. */
@Composable
fun RewardsIntroDialog(
    onLearnMore: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.CardGiftcard, null, tint = SkyColors.PrimaryFixedDim) },
        title = { Text("Earn ad-free time", color = SkyColors.OnSurface) },
        text = {
            Text(
                "SkyPulse is free and supported by a few ads. Watch a short ad to earn a credit, " +
                    "then spend credits for blocks of ad-free time — up to a few hours a day. " +
                    "Open it anytime from the credits chip on the map or from Settings.",
                color = SkyColors.OnSurfaceVariant,
            )
        },
        confirmButton = {
            TextButton(onClick = onLearnMore) {
                Text("Show me", color = SkyColors.PrimaryFixedDim)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it", color = SkyColors.OnSurfaceVariant)
            }
        },
        containerColor = SkyColors.SurfaceContainerLow,
    )
}
