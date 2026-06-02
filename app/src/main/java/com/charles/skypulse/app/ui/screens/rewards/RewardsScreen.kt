package com.charles.skypulse.app.ui.screens.rewards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.charles.skypulse.app.domain.ads.AdRewardCalculator
import com.charles.skypulse.app.ui.ads.findActivity
import com.charles.skypulse.app.ui.components.GhostButton
import com.charles.skypulse.app.ui.components.GlassCard
import com.charles.skypulse.app.ui.components.PrimaryButton
import com.charles.skypulse.app.ui.theme.SkyColors
import com.charles.skypulse.app.ui.theme.SkyType

@Composable
fun RewardsScreen(
    onBack: () -> Unit,
    viewModel: RewardsViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.onScreenOpened() }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 40.dp, start = 4.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = SkyColors.OnSurface)
            }
            Text("Ad-Free Rewards", style = SkyType.HeadlineLgMobile, color = SkyColors.TextHigh)
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Status: ad-free time + credits.
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        if (ui.adFree) Icons.Filled.Shield else Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = SkyColors.PrimaryFixedDim,
                        modifier = Modifier.size(40.dp),
                    )
                    if (ui.adFree) {
                        Text("Ad-free active", style = SkyType.TitleMd, color = SkyColors.OnSurface)
                        Text(
                            formatRemaining(ui.adFreeRemainingMs),
                            style = SkyType.DataLg,
                            color = SkyColors.PrimaryFixedDim,
                        )
                        Text("remaining", style = SkyType.LabelSm, color = SkyColors.OnSurfaceVariant)
                    } else {
                        Text("Ads are on", style = SkyType.TitleMd, color = SkyColors.OnSurface)
                        Text(
                            "Spend a credit to go ad-free for ${AdRewardCalculator.MINUTES_PER_CREDIT} minutes.",
                            style = SkyType.BodyMd,
                            color = SkyColors.OnSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            // Credits balance.
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.CardGiftcard, null, tint = SkyColors.PrimaryFixedDim, modifier = Modifier.size(28.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Your credits", style = SkyType.TitleMd, color = SkyColors.OnSurface)
                        Text(
                            "Earned today: ${ui.creditsEarnedToday} / ${AdRewardCalculator.MAX_CREDITS_PER_DAY}",
                            style = SkyType.LabelSm,
                            color = SkyColors.OnSurfaceVariant,
                        )
                    }
                    Text("${ui.credits}", style = SkyType.DataLg, color = SkyColors.PrimaryFixedDim)
                }
            }

            // Actions.
            PrimaryButton(
                text = if (ui.canEarn) "Watch an ad → earn 1 credit" else "Daily limit reached",
                leadingIcon = Icons.Filled.PlayCircleFilled,
                onClick = {
                    if (ui.canEarn) {
                        context.findActivity()?.let { activity ->
                            viewModel.watchAd(activity) { /* state updates reactively */ }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            GhostButton(
                text = "Spend 1 credit → ${AdRewardCalculator.MINUTES_PER_CREDIT} min ad-free",
                onClick = { if (ui.canSpend) viewModel.spendCredit() },
                modifier = Modifier.fillMaxWidth(),
            )

            // How it works.
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("How it works", style = SkyType.TitleMd, color = SkyColors.OnSurface)
                    HowItWorksLine("Watch a short rewarded ad to earn 1 credit.")
                    HowItWorksLine("You can earn up to ${AdRewardCalculator.MAX_CREDITS_PER_DAY} credits each day.")
                    HowItWorksLine("Spend a credit for ${AdRewardCalculator.MINUTES_PER_CREDIT} minutes with no ads.")
                    HowItWorksLine("Up to ${AdRewardCalculator.MAX_AD_FREE_PER_DAY_SECONDS / 3600} hours of ad-free time per day.")
                    HowItWorksLine("Credits and limits reset at midnight.")
                }
            }
        }
    }
}

@Composable
private fun HowItWorksLine(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("•", style = SkyType.BodyMd, color = SkyColors.PrimaryFixedDim)
        Text(text, style = SkyType.BodyMd, color = SkyColors.OnSurfaceVariant)
    }
}

/** Formats milliseconds as H:MM:SS (or M:SS under an hour). */
internal fun formatRemaining(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
