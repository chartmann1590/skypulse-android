package com.charles.skypulse.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Glassmorphic panel surface from DESIGN.md: semi-transparent navy fill + 1px white-10%
 * stroke. True backdrop blur is not portable below API 31, so depth is conveyed with the
 * translucent fill + hairline border (the design's "tonal layering" approach).
 */
fun Modifier.glassPanel(
    shape: Shape,
    fill: Color = SkyColors.GlassSurface,
    stroke: Color = SkyColors.GlassStroke,
): Modifier = this
    .background(fill, shape)
    .border(1.dp, stroke, shape)

/** Subtle cyan glow for active/selected HUD elements. */
fun Modifier.glowActive(
    shape: Shape,
    color: Color = SkyColors.PrimaryFixedDim,
): Modifier = this.shadow(
    elevation = 12.dp,
    shape = shape,
    ambientColor = color,
    spotColor = color,
)
