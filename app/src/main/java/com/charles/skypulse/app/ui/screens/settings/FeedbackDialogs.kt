package com.charles.skypulse.app.ui.screens.settings

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.charles.skypulse.app.data.remote.GitHubCommentDto
import com.charles.skypulse.app.domain.model.SavedBugReport
import com.charles.skypulse.app.ui.components.GhostButton
import com.charles.skypulse.app.ui.theme.SkyColors
import com.charles.skypulse.app.ui.theme.SkyType
import java.util.Locale

@Composable
fun ReportFormDialog(
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (
        title: String,
        description: String,
        screenshotBytes: ByteArray?,
        screenshotFileName: String?,
        includeDiagnostics: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var includeDiagnostics by remember { mutableStateOf(true) }

    var screenshotBytes by remember { mutableStateOf<ByteArray?>(null) }
    var screenshotFileName by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                screenshotFileName = uri.lastPathSegment ?: "screenshot_${System.currentTimeMillis()}.png"
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }.onSuccess { bytes ->
                    screenshotBytes = bytes
                }
            }
        }
    )

    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SkyColors.PitchBlack.copy(alpha = 0.8f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SkyColors.SurfaceContainer, RoundedCornerShape(24.dp))
                    .border(1.dp, SkyColors.GlassStroke, RoundedCornerShape(24.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Report a Problem",
                        style = SkyType.TitleMd,
                        color = SkyColors.TextHigh
                    )
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isSubmitting
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = SkyColors.OnSurface)
                    }
                }

                // Public Warning Notice
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SkyColors.ErrorContainer.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .border(1.dp, SkyColors.ErrorContainer, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = SkyColors.Error,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Warning: Everything submitted (including screenshot and comments) will be public on GitHub. Do not include passwords or sensitive personal keys.",
                        style = SkyType.LabelSm,
                        color = SkyColors.Error,
                        modifier = Modifier.weight(1f)
                    )
                }

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        FeedbackTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = "Title / Subject *",
                            singleLine = true,
                            enabled = !isSubmitting
                        )
                    }
                    item {
                        FeedbackTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = "Description / Steps to Reproduce *",
                            singleLine = false,
                            minLines = 3,
                            enabled = !isSubmitting
                        )
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                FeedbackTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = "Your Name (Optional)",
                                    singleLine = true,
                                    enabled = !isSubmitting
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                FeedbackTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = "Email (Optional)",
                                    singleLine = true,
                                    enabled = !isSubmitting
                                )
                            }
                        }
                    }

                    // Checkbox for diagnostics
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isSubmitting) { includeDiagnostics = !includeDiagnostics }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = includeDiagnostics,
                                onCheckedChange = { includeDiagnostics = it },
                                enabled = !isSubmitting,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = SkyColors.PrimaryFixedDim,
                                    uncheckedColor = SkyColors.Outline
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = "Include system diagnostics & active models",
                                    style = SkyType.BodyMd,
                                    color = SkyColors.OnSurface
                                )
                                Text(
                                    text = "Brand, SDK level, Locale, Storage, RAM, and Ollama tags. No IP/API keys.",
                                    style = SkyType.LabelSm,
                                    color = SkyColors.OnSurfaceVariant
                                )
                            }
                        }
                    }

                    // Attachment and Preview
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            GhostButton(
                                text = "Attach Screenshot",
                                onClick = {
                                    if (!isSubmitting) {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                }
                            )

                            screenshotBytes?.let { bytes ->
                                val bitmap = remember(bytes) {
                                    runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size).asImageBitmap() }.getOrNull()
                                }
                                if (bitmap != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Image(
                                            bitmap = bitmap,
                                            contentDescription = "Thumbnail",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(1.dp, SkyColors.GlassStroke, RoundedCornerShape(8.dp))
                                        )
                                        IconButton(
                                            onClick = {
                                                screenshotBytes = null
                                                screenshotFileName = null
                                            },
                                            enabled = !isSubmitting,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove Screenshot",
                                                tint = SkyColors.AlertRed
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    errorMessage?.let { error ->
                        item {
                            Text(
                                text = error,
                                color = SkyColors.Error,
                                style = SkyType.LabelSm,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GhostButton(
                        text = "Cancel",
                        onClick = { if (!isSubmitting) onDismiss() },
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = {
                            var desc = description
                            if (name.isNotBlank() || email.isNotBlank()) {
                                desc = "Submitted by: ${name.ifBlank { "Anonymous" }} (${email.ifBlank { "No email" }})\n\n$desc"
                            }
                            onSubmit(title, desc, screenshotBytes, screenshotFileName, includeDiagnostics)
                        },
                        enabled = !isSubmitting && title.isNotBlank() && description.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SkyColors.PrimaryFixedDim,
                            contentColor = SkyColors.OnPrimary,
                            disabledContainerColor = SkyColors.SurfaceContainerHighest,
                            disabledContentColor = SkyColors.OnSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = SkyColors.OnPrimary
                            )
                        } else {
                            Text("Submit", style = SkyType.TitleMd)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IssueDetailDialog(
    report: SavedBugReport,
    comments: List<GitHubCommentDto>,
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onPostComment: (body: String, screenshotBytes: ByteArray?, screenshotFileName: String?) -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var replyText by remember { mutableStateOf("") }
    var screenshotBytes by remember { mutableStateOf<ByteArray?>(null) }
    var screenshotFileName by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                screenshotFileName = uri.lastPathSegment ?: "reply_screenshot_${System.currentTimeMillis()}.png"
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }.onSuccess { bytes ->
                    screenshotBytes = bytes
                }
            }
        }
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SkyColors.PitchBlack.copy(alpha = 0.8f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .background(SkyColors.SurfaceContainer, RoundedCornerShape(24.dp))
                    .border(1.dp, SkyColors.GlassStroke, RoundedCornerShape(24.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Issue #${report.number}",
                            style = SkyType.LabelSm,
                            color = SkyColors.PrimaryFixedDim
                        )
                        Text(
                            text = report.title,
                            style = SkyType.TitleMd,
                            color = SkyColors.TextHigh,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = SkyColors.OnSurface)
                    }
                }

                // Real-time Loading / Error Indicator
                if (isLoading && comments.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SkyColors.PrimaryFixedDim)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Original Report card at the top
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SkyColors.SurfaceContainerLow, RoundedCornerShape(12.dp))
                                    .border(1.dp, SkyColors.GlassStroke, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Initial Report", style = SkyType.LabelSm, color = SkyColors.PrimaryFixedDim)
                                    Text(
                                        text = report.status.uppercase(Locale.US),
                                        style = SkyType.LabelSm,
                                        color = if (report.status == "open") Color(0xFF00E676) else SkyColors.Outline
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Created: ${report.createdAt}",
                                    style = SkyType.LabelSm,
                                    color = SkyColors.OnSurfaceVariant
                                )
                            }
                        }

                        // Thread Comments
                        items(comments) { comment ->
                            val isUserReply = comment.body.startsWith("**[User Reply from App]**")
                            val displayText = if (isUserReply) {
                                comment.body.removePrefix("**[User Reply from App]**").trim()
                            } else {
                                comment.body
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isUserReply) SkyColors.GlassSurface else SkyColors.SurfaceContainerHigh,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(1.dp, SkyColors.GlassStroke, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = comment.user.login,
                                            style = SkyType.TitleMd.copy(fontSize = 14.sp),
                                            color = SkyColors.TextHigh
                                        )
                                        if (isUserReply) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(SkyColors.PrimaryFixed.copy(alpha = 0.2f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    "App User",
                                                    style = SkyType.LabelSm.copy(fontSize = 10.sp),
                                                    color = SkyColors.PrimaryFixedDim
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = comment.created_at,
                                        style = SkyType.LabelSm.copy(fontSize = 10.sp),
                                        color = SkyColors.Outline
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = displayText,
                                    style = SkyType.BodyMd.copy(fontSize = 14.sp),
                                    color = SkyColors.OnSurface
                                )
                            }
                        }
                    }
                }

                // Add Comment inputs
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Screenshot attachment preview
                    screenshotBytes?.let { bytes ->
                        val bitmap = remember(bytes) {
                            runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size).asImageBitmap() }.getOrNull()
                        }
                        if (bitmap != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = "Attachment Thumbnail",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .border(1.dp, SkyColors.GlassStroke, RoundedCornerShape(6.dp))
                                )
                                Text(
                                    text = screenshotFileName ?: "screenshot.png",
                                    style = SkyType.LabelSm,
                                    color = SkyColors.OnSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        screenshotBytes = null
                                        screenshotFileName = null
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, "Remove attachment", tint = SkyColors.AlertRed)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = !isLoading
                        ) {
                            Icon(Icons.Default.AttachFile, "Attach Screenshot", tint = SkyColors.PrimaryFixedDim)
                        }

                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            label = { Text("Write a reply...") },
                            enabled = !isLoading,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = SkyColors.TextHigh,
                                unfocusedTextColor = SkyColors.TextHigh,
                                focusedContainerColor = SkyColors.SurfaceContainerLowest,
                                unfocusedContainerColor = SkyColors.SurfaceContainerLowest,
                                focusedBorderColor = SkyColors.PrimaryFixedDim,
                                unfocusedBorderColor = SkyColors.GlassStroke,
                                focusedLabelColor = SkyColors.PrimaryFixedDim,
                                unfocusedLabelColor = SkyColors.Outline
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = {
                                onPostComment(replyText, screenshotBytes, screenshotFileName)
                                replyText = ""
                                screenshotBytes = null
                                screenshotFileName = null
                            },
                            enabled = !isLoading && replyText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SkyColors.PrimaryFixedDim,
                                contentColor = SkyColors.OnPrimary,
                                disabledContainerColor = SkyColors.SurfaceContainerHighest,
                                disabledContentColor = SkyColors.OnSurfaceVariant
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = SkyColors.OnPrimary
                                )
                            } else {
                                Text("Post", style = SkyType.LabelSm)
                            }
                        }
                    }
                }

                // Error message
                errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = SkyColors.Error,
                        style = SkyType.LabelSm,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                // Deep-link and Close Action footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GhostButton(
                        text = "View on GitHub",
                        onClick = { uriHandler.openUri(report.htmlUrl) },
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SkyColors.SurfaceContainerHighest,
                            contentColor = SkyColors.OnSurface
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("Close", style = SkyType.TitleMd)
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedbackTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean,
    minLines: Int = 1,
    enabled: Boolean
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        enabled = enabled,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = SkyColors.TextHigh,
            unfocusedTextColor = SkyColors.TextHigh,
            focusedContainerColor = SkyColors.SurfaceContainerLowest,
            unfocusedContainerColor = SkyColors.SurfaceContainerLowest,
            focusedBorderColor = SkyColors.PrimaryFixedDim,
            unfocusedBorderColor = SkyColors.GlassStroke,
            focusedLabelColor = SkyColors.PrimaryFixedDim,
            unfocusedLabelColor = SkyColors.Outline
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    )
}
