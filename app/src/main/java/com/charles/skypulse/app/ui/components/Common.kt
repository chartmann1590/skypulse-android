package com.charles.skypulse.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.charles.skypulse.app.ui.theme.SkyColors
import com.charles.skypulse.app.ui.theme.SkyType
import com.charles.skypulse.app.ui.theme.glassPanel

/** A glassmorphic card container with consistent 20dp internal padding. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 24,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .glassPanel(shape, fill = SkyColors.SurfaceContainerLow.copy(alpha = 0.55f))
            .padding(contentPadding),
    ) { content() }
}

/** Pill-shaped selectable chip used for sort options and segmented choices. */
@Composable
fun PillChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    val bg = if (selected) SkyColors.PrimaryFixedDim.copy(alpha = 0.18f) else Color.Transparent
    val borderColor = if (selected) SkyColors.PrimaryFixedDim else SkyColors.OutlineVariant
    val textColor = if (selected) SkyColors.PrimaryFixedDim else SkyColors.OnSurfaceVariant
    Box(
        modifier = modifier
            .clip(shape)
            .background(bg, shape)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = SkyType.TitleMd, color = textColor)
    }
}

/** Solid primary action button (cyan fill, dark text, pill). */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .background(SkyColors.PrimaryFixedDim, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, null, tint = SkyColors.PitchBlack, modifier = Modifier.size(20.dp))
        }
        Text(
            text,
            style = SkyType.TitleMd,
            color = SkyColors.PitchBlack,
            modifier = Modifier.padding(start = if (leadingIcon != null) 8.dp else 0.dp),
        )
    }
}

/** Ghost button: glass stroke + light text. */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .background(SkyColors.SurfaceContainerLow, shape)
            .border(1.dp, SkyColors.GlassStroke, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, null, tint = SkyColors.OnSurface, modifier = Modifier.size(20.dp))
        }
        Text(
            text,
            style = SkyType.TitleMd,
            color = SkyColors.OnSurface,
            modifier = Modifier.padding(start = if (leadingIcon != null) 8.dp else 0.dp),
        )
    }
}

/** A small "LIVE" / source badge with a coloured dot. */
@Composable
fun StatusBadge(
    text: String,
    dotColor: Color = SkyColors.PrimaryFixedDim,
    textColor: Color = SkyColors.PrimaryFixedDim,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .background(SkyColors.SurfaceContainerHigh, shape)
            .border(1.dp, SkyColors.GlassStroke, shape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(50))
                .background(dotColor, RoundedCornerShape(50)),
        )
        Text(text, style = SkyType.LabelSm, color = textColor)
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier, message: String = "Scanning the skies…") {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator(color = SkyColors.PrimaryFixedDim)
        Text(message, style = SkyType.BodyMd, color = SkyColors.OnSurfaceVariant)
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = SkyColors.OutlineVariant, modifier = Modifier.size(48.dp))
        Text(title, style = SkyType.TitleMd, color = SkyColors.OnSurface)
        Text(
            subtitle,
            style = SkyType.BodyMd,
            color = SkyColors.OnSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Something went wrong", style = SkyType.TitleMd, color = SkyColors.Error)
        Text(
            message,
            style = SkyType.BodyMd,
            color = SkyColors.OnSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) GhostButton(text = "Retry", onClick = onRetry)
    }
}
