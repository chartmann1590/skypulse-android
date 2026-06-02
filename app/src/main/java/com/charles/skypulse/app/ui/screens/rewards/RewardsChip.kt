package com.charles.skypulse.app.ui.screens.rewards

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.charles.skypulse.app.ui.theme.SkyColors
import com.charles.skypulse.app.ui.theme.SkyType
import com.charles.skypulse.app.ui.theme.glassPanel

/**
 * Compact glass pill surfacing the rewards entry point on the map. Shows ad-free time remaining
 * when active, otherwise the current credit count. Tapping opens the Rewards screen.
 */
@Composable
fun RewardsChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RewardsViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .glassPanel(shape, fill = SkyColors.PitchBlack.copy(alpha = 0.6f))
            .border(1.dp, SkyColors.GlassStroke, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (ui.adFree) {
            Icon(Icons.Filled.Shield, "Ad-free time", tint = SkyColors.PrimaryFixedDim, modifier = Modifier.size(20.dp))
            Text(formatRemaining(ui.adFreeRemainingMs), style = SkyType.TitleMd, color = SkyColors.PrimaryFixedDim)
        } else {
            Icon(Icons.Filled.CardGiftcard, "Rewards", tint = SkyColors.PrimaryFixedDim, modifier = Modifier.size(20.dp))
            Text("${ui.credits}", style = SkyType.TitleMd, color = SkyColors.PrimaryFixedDim)
        }
    }
}
