package com.taskgullak.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskgullak.app.data.model.TaskCommentItem
import com.taskgullak.app.data.model.UserOfferTask
import com.taskgullak.app.ui.components.AppTimerPill
import kotlinx.coroutines.delay

@Composable
fun OfferDetailScreen(
    task: UserOfferTask,
    claimedComment: TaskCommentItem?,
    submissionStatus: String?, // null, "pending", "approved", "rejected"
    siblingTasksForSameApp: List<UserOfferTask> = emptyList(),
    onClaimAndCopyComment: () -> Unit,
    onTimerExpiredRelease: (TaskCommentItem) -> Unit,
    onPickAndUploadScreenshot: () -> Unit,
    onSwitchToSiblingTask: (UserOfferTask) -> Unit = {},
    AppIconContent: @Composable (Modifier) -> Unit
) {
    val context = LocalContext.current
    var remainingSeconds by remember(claimedComment?.id, claimedComment?.claimExpiresAtMillis) {
        val exp = claimedComment?.claimExpiresAtMillis ?: 0L
        val diff = if (exp > 0L) ((exp - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
        else (task.durationMinutes.coerceAtLeast(1) * 60L)
        mutableLongStateOf(diff)
    }

    val isTimerRunning = claimedComment != null &&
        claimedComment.claimExpiresAtMillis > 0L &&
        submissionStatus == null

    // Countdown tick for this specific claimed comment
    LaunchedEffect(claimedComment?.id, isTimerRunning) {
        if (isTimerRunning && claimedComment != null) {
            while (remainingSeconds > 0L) {
                delay(1000L)
                val updated = ((claimedComment.claimExpiresAtMillis - System.currentTimeMillis()) / 1000L)
                    .coerceAtLeast(0L)
                remainingSeconds = updated
            }
            if (remainingSeconds == 0L) {
                onTimerExpiredRelease(claimedComment)
            }
        }
    }

    // Subtle pulse animation on the timer badge
    val infiniteTransition = rememberInfiniteTransition(label = "offer_timer_pulse")
    val timerScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isTimerRunning) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "timer_scale"
    )

    val mm = (remainingSeconds / 60L).toString().padStart(2, '0')
    val ss = (remainingSeconds % 60L).toString().padStart(2, '0')

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Point 8: If the same app has multiple tasks, allow switching/skipping freely
        if (siblingTasksForSameApp.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                siblingTasksForSameApp.forEachIndexed { idx, sibling ->
                    val selected = sibling.id == task.id
                    FilterChip(
                        selected = selected,
                        onClick = { if (!selected) onSwitchToSiblingTask(sibling) },
                        label = {
                            Text(
                                text = "Task #${idx + 1} (₹${sibling.rewardInr})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    )
                }
            }
        }

        // Top Compact App Header Card with Animated Timer Badge
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            tonalElevation = 1.dp,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, Color(0xFFE8ECF4)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIconContent(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = task.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (task.rewardInr > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFECFDF5)
                            ) {
                                Text(
                                    text = "₹${task.rewardInr}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF059669),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "Install App and Rate",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Real Rating & Downloads (No hardcoded 4.5 or 10K+)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (task.appRating.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                repeat(5) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = task.appRating,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF475569)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Android,
                                    contentDescription = null,
                                    tint = Color(0xFF3B82F6),
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Android",
                                    fontSize = 9.sp,
                                    color = Color(0xFF475569)
                                )
                            }
                        }
                        if (task.appDownloads.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFFF1F5F9)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        tint = Color(0xFF3B82F6),
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "${task.appDownloads} Installs",
                                        fontSize = 9.sp,
                                        color = Color(0xFF475569)
                                    )
                                }
                            }
                        }
                    }
                }

                // Animated Countdown Timer Pill (Matching Image 2)
                AppTimerPill(
                    displayTime = "$mm:$ss",
                    isOn = isTimerRunning,
                    modifier = Modifier.scale(timerScale)
                )
            }
        }

        // Consistent Status Banner
        if (!submissionStatus.isNullOrBlank()) {
            val normalizedStatus = if (submissionStatus.equals("rework", ignoreCase = true)) {
                "rejected"
            } else {
                submissionStatus.lowercase()
            }
            val (bgColor, textColor, labelText) = when (normalizedStatus) {
                "approved" -> Triple(Color(0xFFECFDF5), Color(0xFF059669), "Approved")
                "rejected" -> Triple(Color(0xFFFEF2F2), Color(0xFFDC2626), "Rejected")
                else -> Triple(Color(0xFFFFFBEB), Color(0xFFD97706), "Pending Verification")
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = bgColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Status: $labelText",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        // Compact "How to Avail" / Details Section (No Raw Links)
        Text(
            text = "How to Avail",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val steps = task.cleanInstructions.ifEmpty {
                    listOf(
                        "Click on 'Install this app' button below and open the app.",
                        "Rate the app 5 stars on Play Store.",
                        "Copy the exact comment below and post it as your review.",
                        "Take a screenshot of your posted review and upload it below."
                    )
                }
                steps.forEach { step ->
                    Text(
                        text = "• $step",
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                        color = Color(0xFF475569)
                    )
                }
            }
        }

        // Compact "Paste this exact comment to get paid" Box
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Paste this exact comment to get paid",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onClaimAndCopyComment()
                            val textToCopy = claimedComment?.commentText.orEmpty()
                            if (textToCopy.isNotBlank()) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Task Comment", textToCopy))
                                Toast.makeText(context, "Comment copied! Timer started.", Toast.LENGTH_SHORT).show()
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = claimedComment?.commentText?.ifBlank {
                                "Tap to claim & copy your unique review comment"
                            } ?: "Tap to claim & copy your unique review comment",
                            fontSize = 11.5.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Comment",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Outlined "Install this app" Button (Raw App Link stays completely hidden)
        OutlinedButton(
            onClick = {
                if (task.storeUrl.isNotBlank()) {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(task.storeUrl)))
                    }
                }
            },
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFF93C5FD)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color(0xFFF8FAFF),
                contentColor = Color(0xFF2563EB)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            Text(
                text = "Install this app",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Compact "Upload Media" Section
        Text(
            text = "Upload Media",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFF8FAFF),
            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clickable { onPickAndUploadScreenshot() }
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileUpload,
                        contentDescription = "Upload Screenshot",
                        tint = Color(0xFF334155),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Tap to upload review screenshot",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF475569)
                    )
                }
            }
        }
    }
}
