package com.charles.skypulse.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.charles.skypulse.app.domain.model.AltitudeUnit
import com.charles.skypulse.app.domain.model.DistanceUnit
import com.charles.skypulse.app.domain.model.SavedBugReport
import com.charles.skypulse.app.domain.model.SpeedUnit
import com.charles.skypulse.app.ui.components.GhostButton
import com.charles.skypulse.app.ui.theme.SkyColors
import com.charles.skypulse.app.ui.theme.SkyType
import java.util.Locale

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenRewards: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val cacheCleared by viewModel.cacheCleared.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    val submittedIssues by viewModel.submittedIssues.collectAsStateWithLifecycle()
    val comments by viewModel.comments.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var showReportDialog by remember { mutableStateOf(false) }
    var selectedIssueForDetail by remember { mutableStateOf<SavedBugReport?>(null) }

    LaunchedEffect(selectedIssueForDetail) {
        selectedIssueForDetail?.let {
            viewModel.loadComments(it.number)
        }
    }

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
            Text("Settings", style = SkyType.HeadlineLgMobile, color = SkyColors.TextHigh)
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Data sources card
            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.Wifi, null, tint = SkyColors.PrimaryFixedDim)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Connection Status", style = SkyType.DataLg, color = SkyColors.TextHigh)
                        Text("Open ADS-B sources", style = SkyType.LabelSm, color = SkyColors.PrimaryFixedDim)
                    }
                }
                StatusRow("ADSB.lol", "Primary")
                StatusRow("OpenSky", "Fallback")
                StatusRow("OpenFlights DB", "Active")
            }

            Text("Preferences", style = SkyType.TitleMd, color = SkyColors.OnSurface)
            SettingsCard {
                SegLabel("Refresh Interval")
                SegmentedRow(
                    options = listOf(5, 10, 30),
                    selectedIndex = listOf(5, 10, 30).indexOf(settings.refreshIntervalSeconds).coerceAtLeast(0),
                    labels = listOf("5s", "10s", "30s"),
                    onSelect = { viewModel.setRefreshInterval(listOf(5, 10, 30)[it]) },
                )
                SegLabel("Altitude Units")
                SegmentedRow(
                    options = AltitudeUnit.entries,
                    selectedIndex = AltitudeUnit.entries.indexOf(settings.altitudeUnit),
                    labels = listOf("Feet", "Meters"),
                    onSelect = { viewModel.setAltitudeUnit(AltitudeUnit.entries[it]) },
                )
                SegLabel("Speed Units")
                SegmentedRow(
                    options = SpeedUnit.entries,
                    selectedIndex = SpeedUnit.entries.indexOf(settings.speedUnit),
                    labels = listOf("Knots", "MPH"),
                    onSelect = { viewModel.setSpeedUnit(SpeedUnit.entries[it]) },
                )
                SegLabel("Distance Units")
                SegmentedRow(
                    options = DistanceUnit.entries,
                    selectedIndex = DistanceUnit.entries.indexOf(settings.distanceUnit),
                    labels = listOf("Miles", "Km", "NM"),
                    onSelect = { viewModel.setDistanceUnit(DistanceUnit.entries[it]) },
                )
            }

            // Ad-free rewards
            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.CardGiftcard, null, tint = SkyColors.PrimaryFixedDim)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ad-free rewards", style = SkyType.TitleMd, color = SkyColors.OnSurface)
                        Text(
                            "Watch ads to earn credits and unlock ad-free time.",
                            style = SkyType.LabelSm,
                            color = SkyColors.OnSurfaceVariant,
                        )
                    }
                }
                GhostButton("Open rewards", onClick = onOpenRewards, modifier = Modifier.fillMaxWidth())
            }

            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.Person, null, tint = SkyColors.PrimaryFixedDim)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Profile", style = SkyType.TitleMd, color = SkyColors.OnSurface)
                        Text(
                            "Sign in with Google or an email account.",
                            style = SkyType.LabelSm,
                            color = SkyColors.OnSurfaceVariant,
                        )
                    }
                }
                GhostButton("Open profile", onClick = onOpenProfile, modifier = Modifier.fillMaxWidth())
            }

            // Privacy
            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.PrivacyTip, null, tint = SkyColors.PrimaryFixedDim)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Privacy Focus", style = SkyType.TitleMd, color = SkyColors.OnSurface)
                        Text(
                            "Your alerts and saved data stay local to your device.",
                            style = SkyType.LabelSm,
                            color = SkyColors.OnSurfaceVariant,
                        )
                    }
                }
                GhostButton("Read privacy details", onClick = onOpenPrivacy, modifier = Modifier.fillMaxWidth())
            }

            // Support & Feedback
            Text("Support & Feedback", style = SkyType.TitleMd, color = SkyColors.OnSurface)
            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.BugReport, null, tint = SkyColors.PrimaryFixedDim)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Report a Problem", style = SkyType.TitleMd, color = SkyColors.OnSurface)
                        Text(
                            "Submit bugs directly to our GitHub repository.",
                            style = SkyType.LabelSm,
                            color = SkyColors.OnSurfaceVariant,
                        )
                    }
                }
                GhostButton("New Report", onClick = { showReportDialog = true }, modifier = Modifier.fillMaxWidth())

                if (submittedIssues.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Your Submitted Reports", style = SkyType.LabelSm, color = SkyColors.PrimaryFixedDim)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        submittedIssues.forEach { report ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SkyColors.SurfaceContainerHigh)
                                    .clickable { selectedIssueForDetail = report }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = report.title,
                                        style = SkyType.BodyMd,
                                        color = SkyColors.TextHigh,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Issue #${report.number} • ${report.createdAt}",
                                        style = SkyType.LabelSm,
                                        color = SkyColors.OnSurfaceVariant
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (report.status == "open") Color(0xFF00E676).copy(alpha = 0.15f) else SkyColors.Outline.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = report.status.uppercase(Locale.US),
                                        style = SkyType.LabelSm,
                                        color = if (report.status == "open") Color(0xFF00E676) else SkyColors.Outline
                                    )
                                }
                            }
                        }
                    }
                }
            }

            GhostButton(
                text = if (cacheCleared) "Cache Cleared ✓" else "Clear Cache",
                onClick = viewModel::clearCache,
                modifier = Modifier.fillMaxWidth(),
            )

            // About + links
            SettingsCard {
                Text("About", style = SkyType.TitleMd, color = SkyColors.OnSurface)
                Text(
                    "SkyPulse — live aircraft tracking powered by free, open ADS-B data. No account, no API key.",
                    style = SkyType.BodyMd,
                    color = SkyColors.OnSurfaceVariant,
                )
                LinkRow("Visit the SkyPulse website") { uriHandler.openUri(WEBSITE_URL) }
                LinkRow("☕ Buy me a coffee") { uriHandler.openUri(BMC_URL) }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("SkyPulse", style = SkyType.TitleMd, color = SkyColors.OnSurface)
                Text("Powered by open ADS-B data.", style = SkyType.LabelSm, color = SkyColors.OnSurfaceVariant)
                Text("Version 1.0.0", style = SkyType.LabelSm, color = SkyColors.Outline, textAlign = TextAlign.Center)
            }
        }
    }

    if (showReportDialog) {
        ReportFormDialog(
            isSubmitting = isSubmitting,
            errorMessage = errorMessage,
            onDismiss = {
                viewModel.clearErrorMessage()
                showReportDialog = false
            },
            onSubmit = { title, description, screenshotBytes, screenshotFileName, includeDiagnostics ->
                viewModel.createIssue(
                    title = title,
                    description = description,
                    screenshotBytes = screenshotBytes,
                    screenshotFileName = screenshotFileName,
                    includeDiagnostics = includeDiagnostics,
                    onSuccess = {
                        showReportDialog = false
                    }
                )
            }
        )
    }

    selectedIssueForDetail?.let { report ->
        IssueDetailDialog(
            report = report,
            comments = comments,
            isLoading = isLoading,
            errorMessage = errorMessage,
            onDismiss = {
                viewModel.clearErrorMessage()
                selectedIssueForDetail = null
            },
            onPostComment = { body, screenshotBytes, screenshotFileName ->
                viewModel.postComment(
                    issueNumber = report.number,
                    body = body,
                    screenshotBytes = screenshotBytes,
                    screenshotFileName = screenshotFileName,
                    onSuccess = {
                        // comment auto-refreshed in viewmodel
                    }
                )
            }
        )
    }
}

private const val WEBSITE_URL = "https://chartmann1590.github.io/skypulse-android/"
private const val BMC_URL = "https://buymeacoffee.com/charleshartmann"

@Composable
private fun LinkRow(text: String, onClick: () -> Unit) {
    Text(
        text,
        style = SkyType.TitleMd,
        color = SkyColors.PrimaryFixedDim,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SkyColors.SurfaceContainerLow.copy(alpha = 0.6f), shape)
            .border(1.dp, SkyColors.GlassStroke, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) { content() }
}

@Composable
private fun StatusRow(name: String, tag: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, style = SkyType.BodyMd, color = SkyColors.OnSurface)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(SkyColors.SurfaceContainerHigh, RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text(tag, style = SkyType.LabelSm, color = SkyColors.OnSurfaceVariant)
        }
    }
}

@Composable
private fun SegLabel(text: String) {
    Text(text, style = SkyType.TitleMd, color = SkyColors.OnSurface, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun <T> SegmentedRow(
    options: List<T>,
    selectedIndex: Int,
    labels: List<String>,
    onSelect: (Int) -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SkyColors.SurfaceContainerLowest, shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.indices.forEach { i ->
            val selected = i == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (selected) SkyColors.SurfaceContainerHigh else androidx.compose.ui.graphics.Color.Transparent,
                        RoundedCornerShape(8.dp),
                    )
                    .clickable { onSelect(i) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    labels.getOrElse(i) { options[i].toString() },
                    style = SkyType.BodyMd,
                    color = if (selected) SkyColors.PrimaryFixedDim else SkyColors.OnSurfaceVariant,
                )
            }
        }
    }
}
