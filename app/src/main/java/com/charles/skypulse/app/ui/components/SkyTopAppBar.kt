package com.charles.skypulse.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.charles.skypulse.app.ui.theme.SkyColors
import com.charles.skypulse.app.ui.theme.SkyType

/** Centered SkyPulse wordmark with an optional settings action (matches the design). */
@Composable
fun SkyTopAppBar(
    modifier: Modifier = Modifier,
    title: String = "SkyPulse",
    onSettings: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = SkyType.HeadlineLgMobile,
            color = SkyColors.PrimaryFixedDim,
        )
        if (onSettings != null) {
            IconButton(onClick = onSettings, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = SkyColors.OnSurfaceVariant,
                )
            }
        }
    }
}
