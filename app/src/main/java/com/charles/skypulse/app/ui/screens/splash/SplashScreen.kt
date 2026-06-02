package com.charles.skypulse.app.ui.screens.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.charles.skypulse.app.ui.components.StatusBadge
import com.charles.skypulse.app.ui.theme.SkyColors
import com.charles.skypulse.app.ui.theme.SkyType
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1600)
        onFinished()
    }

    val transition = rememberInfiniteTransition(label = "radar")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "sweep",
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                    listOf(0.4f, 0.7f, 1.0f).forEach { r ->
                        drawCircle(
                            color = SkyColors.PrimaryFixedDim.copy(alpha = 0.25f),
                            radius = size.minDimension / 2 * r,
                            center = center,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f),
                        )
                    }
                }
                Canvas(modifier = Modifier.fillMaxSize().rotate(sweep)) {
                    drawArc(
                        color = SkyColors.PrimaryFixedDim.copy(alpha = 0.35f),
                        startAngle = 0f,
                        sweepAngle = 60f,
                        useCenter = true,
                    )
                }
                Icon(
                    Icons.Filled.Navigation,
                    contentDescription = null,
                    tint = SkyColors.PrimaryContainer,
                    modifier = Modifier.size(56.dp),
                )
            }
            Text("SkyPulse", style = SkyType.DisplayLg, color = SkyColors.PrimaryFixedDim)
            Text(
                "Live aircraft around you",
                style = SkyType.BodyMd,
                color = SkyColors.OnSurfaceVariant,
            )
        }
        StatusBadge(
            text = "OPEN ADS-B DATA",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
        )
    }
}
