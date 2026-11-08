package com.taskgullak.app.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import com.airbnb.lottie.compose.*
import kotlinx.coroutines.delay
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.GroupWork
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import com.taskgullak.app.R
import com.taskgullak.app.data.model.AvailableTask
import com.taskgullak.app.data.model.SupportBotMessage
import com.taskgullak.app.data.model.SupportTicket
import com.taskgullak.app.data.model.TaskSubmission
import com.taskgullak.app.data.model.UserProfile
import com.taskgullak.app.data.model.WalletTransaction
import com.taskgullak.app.data.remote.AchievementBatchInfo
import com.taskgullak.app.data.remote.FirebaseEmailDispatchService
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.taskgullak.app.data.model.AiTaskVerificationResult
import com.taskgullak.app.data.model.BroadcastNotification
import com.taskgullak.app.ui.components.AppTimerPill
import com.taskgullak.app.ui.components.GoldenTicketPriceCard
import com.taskgullak.app.ui.components.ShinyGradientButton
import com.taskgullak.app.ui.theme.InterFontFamily
import com.taskgullak.app.ui.theme.PoppinsFontFamily
import com.taskgullak.app.ui.theme.TgBluePrimary
import com.taskgullak.app.ui.theme.TgSlateMuted
import com.taskgullak.app.ui.theme.TgSuccessGreen
import com.taskgullak.app.util.TaskGullakNotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

private val RemoteIconBitmapCache = ConcurrentHashMap<String, ImageBitmap>()

/**
 * Automatically fetches and renders the App Icon / Logo from the Admin Panel's link or iconUrl,
 * with a sleek stylized gradient emblem fallback.
 */
@Composable
private fun RemoteAppIconBox(
    iconUrl: String,
    appTitle: String,
    size: Dp = 54.dp,
    cornerRadius: Dp = 15.dp
) {
    val cleanUrl = iconUrl.trim()
    if (cleanUrl.isEmpty()) return

    var loadedBitmap by remember(cleanUrl) {
        mutableStateOf(if (cleanUrl.isNotEmpty()) RemoteIconBitmapCache[cleanUrl] else null)
    }

    LaunchedEffect(cleanUrl) {
        if (cleanUrl.isNotEmpty() && loadedBitmap == null) {
            val fetched = withContext(Dispatchers.IO) {
                runCatching {
                    if (cleanUrl.startsWith("data:image")) {
                        val raw = cleanUrl.substringAfter("base64,")
                        val bytes = Base64.decode(raw, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                    } else {
                        val connection = (URL(cleanUrl).openConnection() as HttpURLConnection).apply {
                            connectTimeout = 6000
                            readTimeout = 6000
                            instanceFollowRedirects = true
                            setRequestProperty("User-Agent", "Mozilla/5.0")
                        }
                        connection.inputStream.use { stream ->
                            BitmapFactory.decodeStream(stream)?.asImageBitmap()
                        }
                    }
                }.getOrNull()
            }
            if (fetched != null) {
                RemoteIconBitmapCache[cleanUrl] = fetched
                loadedBitmap = fetched
            }
        }
    }

    Box(
        modifier = Modifier
            .size(size)
            .shadow(4.dp, RoundedCornerShape(cornerRadius), spotColor = Color(0xFF1E293B).copy(alpha = 0.2f))
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                )
            )
            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.35f), RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        if (loadedBitmap != null) {
            Image(
                bitmap = loadedBitmap!!,
                contentDescription = appTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(RoundedCornerShape(cornerRadius))
            )
        } else {
            Text(
                text = appTitle.trim().take(2).uppercase().ifBlank { "TG" },
                color = Color(0xFF38BDF8),
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = (size.value * 0.34f).sp
            )
        }
    }
}

/**
 * Shared contextual page header for non-Home tabs (Offers, Refer, Wallet, Profile).
 * Does NOT show the "Task Gullak" top branding header so each screen remains minimal and contextual.
 */
@Composable
internal fun ContextualTabHeader(
    eyebrowIcon: ImageVector,
    eyebrowText: String,
    title: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp)
            .padding(top = 4.dp, bottom = 2.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFF7B3FE4).copy(alpha = 0.10f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = eyebrowIcon,
                contentDescription = null,
                tint = Color(0xFF7B3FE4),
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = eyebrowText,
                color = Color(0xFF7B3FE4),
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 9.5.sp,
                letterSpacing = 1.1.sp,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
            )
        }

        Text(
            text = title,
            color = Color(0xFF17142E),
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 26.sp,
            letterSpacing = (-0.6).sp,
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
        )
    }
}

/**
 * Full-width Account Suspended / Banned Screen rendered immediately when Admin Panel sets isBanned = true on /users/{userId}.
 */
@Composable
internal fun AccountBannedOverlayCard(
    userName: String,
    userEmail: String,
    banReason: String,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp)
            .padding(top = 24.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = Color(0xFFE11D48).copy(alpha = 0.25f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.98f))
            .border(1.5.dp, Color(0xFFFECDD3), RoundedCornerShape(24.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFE4E6)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Block,
                contentDescription = "Account Suspended",
                tint = Color(0xFFE11D48),
                modifier = Modifier.size(32.dp)
            )
        }

        Text(
            text = "Account Access Restricted",
            color = Color(0xFF9F1239),
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Hi $userName ($userEmail), your Task Gullak account access has been restricted by the compliance team.",
            color = Color(0xFF475569),
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        if (banReason.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFFF1F2))
                    .border(1.dp, Color(0xFFFECDD3), RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "RESTRICTION NOTICE",
                    color = Color(0xFFE11D48),
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.5.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = banReason,
                    color = Color(0xFF881337),
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        ShinyGradientButton(
            text = "Sign Out",
            onClick = onSignOut,
            gradientColors = listOf(Color(0xFFE11D48), Color(0xFFBE123C)),
            height = 44.dp,
            cornerRadius = 14.dp,
            fontSizeSp = 12.5f,
            testTag = "banned_sign_out_button"
        )
    }
}

/**
 * Live Notifications Panel showing real-time Broadcasts from the Admin Panel (/broadcasts)
 */
@Composable
internal fun LiveNotificationsBannerPanel(
    firstName: String,
    broadcasts: List<BroadcastNotification>,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = Color(0xFF4628BE).copy(alpha = 0.16f)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.97f))
            .border(1.dp, Color(0xFFE0E7FF), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3E8FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Campaign,
                        contentDescription = null,
                        tint = Color(0xFF7B3FE4),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "Live Notifications & Broadcasts",
                    color = Color(0xFF17142E),
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp
                )
            }

            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Close notifications",
                tint = Color(0xFF6F6E8A),
                modifier = Modifier
                    .size(18.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onDismiss() }
            )
        }

        if (broadcasts.isEmpty()) {
            Text(
                text = "Welcome to Task Gullak, $firstName! Any new projects, announcements, or task approvals will trigger a real notification on your phone and appear here.",
                color = Color(0xFF475569),
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.5.sp,
                lineHeight = 16.sp
            )
        } else {
            broadcasts.take(5).forEach { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8F7FF))
                        .border(1.dp, Color(0xFFEDE9FE), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = item.title,
                        color = Color(0xFF17142E),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = item.message,
                        color = Color(0xFF6F6E8A),
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

/**
 * 1. OFFERS SCREEN SHELL + LIVE ADMIN TASKS + SCREENSHOT PROOF SUBMISSION
 */
@Composable
internal fun OffersTabShellContent(
    availableTasks: List<AvailableTask>,
    submissions: List<TaskSubmission>,
    pendingCount: Int,
    approvedCount: Int,
    rejectedCount: Int,
    walletBalance: Long,
    isSubmitting: Boolean = false,
    feedbackMessage: String? = null,
    onClearFeedback: () -> Unit = {},
    onSubmitTaskProof: (AvailableTask, String, String, (AiTaskVerificationResult?) -> Unit) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current
    var selectedPeriodIndex by remember { mutableIntStateOf(0) }
    val periods = remember { listOf("Today", "7 Days", "30 Days") }

    val nowMs = System.currentTimeMillis()
    val todayStartMs = remember(nowMs) {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }
    val sevenDaysAgoMs = remember(nowMs) { nowMs - (7L * 24 * 60 * 60 * 1000) }
    val thirtyDaysAgoMs = remember(nowMs) { nowMs - (30L * 24 * 60 * 60 * 1000) }

    val dateFilteredSubmissions = remember(submissions, selectedPeriodIndex, todayStartMs, sevenDaysAgoMs, thirtyDaysAgoMs) {
        val threshold = when (selectedPeriodIndex) {
            0 -> todayStartMs
            1 -> sevenDaysAgoMs
            2 -> thirtyDaysAgoMs
            else -> 0L
        }
        submissions.filter { sub ->
            val subTimeMs = sub.createdAt?.toDate()?.time ?: sub.updatedAt?.toDate()?.time ?: 0L
            subTimeMs >= threshold
        }
    }

    var selectedStatusFilterIndex by remember { mutableIntStateOf(0) }
    val statusFilterLabels = remember { listOf("All", "Submitted", "Approved", "Rejected") }

    val periodSubmittedCount = remember(dateFilteredSubmissions) {
        dateFilteredSubmissions.count {
            val st = it.status.lowercase()
            st == "pending" || st == "submitted" || st == "in_review" || st == "under_review"
        }
    }
    val periodApprovedCount = remember(dateFilteredSubmissions) {
        dateFilteredSubmissions.count {
            val st = it.status.lowercase()
            st == "approved" || st == "verified" || st == "completed"
        }
    }
    val periodRejectedCount = remember(dateFilteredSubmissions) {
        dateFilteredSubmissions.count {
            val st = it.status.lowercase()
            st == "rejected" || st == "declined" || st == "rework"
        }
    }
    val periodApprovedEarnings = remember(dateFilteredSubmissions) {
        dateFilteredSubmissions.filter {
            val st = it.status.lowercase()
            st == "approved" || st == "verified" || st == "completed"
        }.sumOf { it.rewardAmount }
    }

    val finalFilteredSubmissions = remember(dateFilteredSubmissions, selectedStatusFilterIndex) {
        when (selectedStatusFilterIndex) {
            1 -> dateFilteredSubmissions.filter {
                val st = it.status.lowercase()
                st == "pending" || st == "submitted" || st == "in_review" || st == "under_review"
            }
            2 -> dateFilteredSubmissions.filter {
                val st = it.status.lowercase()
                st == "approved" || st == "verified" || st == "completed"
            }
            3 -> dateFilteredSubmissions.filter {
                val st = it.status.lowercase()
                st == "rejected" || st == "declined" || st == "rework"
            }
            else -> dateFilteredSubmissions
        }
    }

    var activeTaskForProof by remember { mutableStateOf<AvailableTask?>(null) }
    var appliedTasksRefreshTick by remember { mutableIntStateOf(0) }
    var celebratedSubmittedTask by remember {
        mutableStateOf<Pair<AvailableTask, AiTaskVerificationResult?>?>(null)
    }

    val filteredTasks = availableTasks

    if (celebratedSubmittedTask != null) {
        val (doneTask, aiResult) = celebratedSubmittedTask!!
        TaskSubmittedCelebrationDialog(
            task = doneTask,
            aiVerificationResult = aiResult,
            onDismiss = { celebratedSubmittedTask = null }
        )
    }

    var showSubmissionsSection by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Offers Header Row with "Submissions" Pill Button (No count badge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFF7B3FE4).copy(alpha = 0.10f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocalOffer,
                        contentDescription = null,
                        tint = Color(0xFF7B3FE4),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "EARN REWARDS",
                        color = Color(0xFF7B3FE4),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.5.sp,
                        letterSpacing = 1.1.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Offers",
                    color = Color(0xFF17142E),
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp,
                    letterSpacing = (-0.6).sp
                )
                Text(
                    text = "${filteredTasks.size} offer available",
                    color = Color(0xFF6F6E8A),
                    fontFamily = InterFontFamily,
                    fontSize = 11.5.sp
                )
            }

            // "Submissions" Pill Button on Top Right (Directly opens full-screen My Submissions modal)
            Box(
                modifier = Modifier
                    .shadow(3.dp, RoundedCornerShape(14.dp), spotColor = Color(0xFF5B21B6).copy(alpha = 0.20f))
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (showSubmissionsSection) Color(0xFFEDE9FE) else Color.White)
                    .border(1.dp, Color(0xFFDDD6FE), RoundedCornerShape(14.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = androidx.compose.material3.ripple()
                    ) { 
                        showSubmissionsSection = true 
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("submissions_header_pill_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AssignmentTurnedIn,
                        contentDescription = "Submissions",
                        tint = Color(0xFF5B21B6),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Submissions",
                        color = Color(0xFF5B21B6),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        if (!feedbackMessage.isNullOrBlank()) {
            val isRejectedBanner = feedbackMessage.contains("Rejected", ignoreCase = true) ||
                feedbackMessage.contains("Re-work", ignoreCase = true)
            val isPartialBanner = feedbackMessage.contains("Quality Review Complete", ignoreCase = true)
            val bannerBg = when {
                isRejectedBanner -> Color(0xFFFFE4E6)
                isPartialBanner -> Color(0xFFFEF3C7)
                else -> Color(0xFFDCFCE7)
            }
            val bannerBorder = when {
                isRejectedBanner -> Color(0xFFFDA4AF)
                isPartialBanner -> Color(0xFFFDE68A)
                else -> Color(0xFF86EFAC)
            }
            val bannerTint = when {
                isRejectedBanner -> Color(0xFFE11D48)
                isPartialBanner -> Color(0xFFD97706)
                else -> Color(0xFF15803D)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(bannerBg)
                    .border(1.dp, bannerBorder, RoundedCornerShape(14.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onClearFeedback() }
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (isRejectedBanner) Icons.Outlined.Cancel else Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = bannerTint,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = feedbackMessage,
                    color = Color(0xFF1E293B),
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.5.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Available Tasks Header + Single/Bulk Filter Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Available Tasks",
                    color = Color(0xFF17142E),
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.5.sp
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEDE9FE)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${filteredTasks.size}",
                        color = Color(0xFF6D28D9),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                }
            }
        }

        // Available Tasks List or Empty State Card
        if (filteredTasks.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(22.dp),
                        ambientColor = Color(0xFF32238C).copy(alpha = 0.08f),
                        spotColor = Color(0xFF32238C).copy(alpha = 0.14f)
                    )
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White.copy(alpha = 0.94f))
                    .border(1.dp, Color.White, RoundedCornerShape(22.dp))
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3E8FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.HourglassEmpty,
                        contentDescription = null,
                        tint = Color(0xFF7B3FE4),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Tasks not available",
                    color = Color(0xFF17142E),
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.5.sp
                )
                Text(
                    text = "New tasks will appear here soon",
                    color = Color(0xFF6F6E8A),
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.5.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            filteredTasks.forEachIndexed { index, task ->
                val isSelectedTask = activeTaskForProof?.id == task.id
                val targetTotal = task.targetCount.coerceAtLeast(1)
                val remainingTasks = (targetTotal - task.completedCount).coerceAtLeast(0)
                val appDisplayTitle = task.appName.ifBlank { task.title }

                // Check user's submission status for this specific project/task
                val taskSubmissions = remember(submissions, task.id, task.title) {
                    submissions.filter {
                        it.taskId == task.id || it.taskTitle.equals(task.title, ignoreCase = true)
                    }
                }
                val isCompletedForUser = remember(taskSubmissions) {
                    taskSubmissions.any {
                        val st = it.status.lowercase()
                        st == "approved" || st == "verified" || st == "completed"
                    }
                }
                val isPendingReview = remember(taskSubmissions, isCompletedForUser) {
                    !isCompletedForUser && taskSubmissions.any {
                        val st = it.status.lowercase()
                        st == "pending" || st == "submitted" || st == "in_review" || st == "under_review"
                    }
                }
                val isReworkRequired = remember(taskSubmissions, isCompletedForUser, isPendingReview) {
                    !isCompletedForUser && !isPendingReview && taskSubmissions.any {
                        val st = it.status.lowercase()
                        st == "rejected" || st == "declined" || st == "rework"
                    }
                }
                val isAppliedForUser = remember(task.id, appliedTasksRefreshTick, taskSubmissions) {
                    FirebaseEmailDispatchService.isTaskApplied(context, task.id) || taskSubmissions.isNotEmpty()
                }

                val (activeTaskComment, liveRemainingSeconds, onCopyStartTimer) = rememberTaskTimerAndComment(context, task)

                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(22.dp),
                                ambientColor = Color(0xFF32238C).copy(alpha = 0.08f),
                                spotColor = Color(0xFF32238C).copy(alpha = 0.16f)
                            )
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = if (isCompletedForUser) {
                                        listOf(Color(0xFFF8FAFC), Color(0xFFF0FDF4))
                                    } else {
                                        listOf(Color(0xFFFFFFFF), Color(0xFFF9F7FF))
                                    }
                                )
                            )
                            .border(
                                width = if (isSelectedTask || isCompletedForUser) 1.6.dp else 1.dp,
                                color = when {
                                    isCompletedForUser -> Color(0xFF86EFAC)
                                    isSelectedTask -> Color(0xFF7B3FE4)
                                    isReworkRequired -> Color(0xFFFDA4AF)
                                    else -> Color(0xFFE9D5FF)
                                },
                                shape = RoundedCornerShape(22.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Top Status Row (Only shown when rework required / rejected)
                        if (!isCompletedForUser && isReworkRequired) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(Color(0xFFFEF2F2))
                                        .border(1.dp, Color(0xFFFECDD3), RoundedCornerShape(999.dp))
                                        .padding(horizontal = 9.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "Rejected",
                                        color = Color(0xFFDC2626),
                                        fontFamily = PoppinsFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }

                    // Main Offer Row: App Info on Left + Timer & Price Ticket Cards on Right (Matching Image 1)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(11.dp)
                        ) {
                            val effectiveIconUrl = remember(task.iconUrl, task.instructions) {
                                task.iconUrl.ifBlank {
                                    task.instructions.firstOrNull { it.contains("[TASK_PFP]:") }
                                        ?.substringAfter("[TASK_PFP]:")?.trim().orEmpty()
                                }
                            }
                            RemoteAppIconBox(
                                iconUrl = effectiveIconUrl,
                                appTitle = appDisplayTitle,
                                size = 56.dp,
                                cornerRadius = 16.dp
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = appDisplayTitle,
                                    color = Color(0xFF17142E),
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val isRawUrl = task.subtitle.trim().startsWith("http://") || task.subtitle.trim().startsWith("https://")
                                if (!isRawUrl && task.subtitle.isNotBlank()) {
                                    Text(
                                        text = task.subtitle,
                                        color = Color(0xFF64748B),
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // 5-Star Rating Row (Matching Image 1)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    val ratingVal = task.rating.coerceIn(1.0, 5.0)
                                    repeat(5) { starIdx ->
                                        val starColor = if (starIdx < ratingVal.toInt()) Color(0xFFF59E0B) else Color(0xFFCBD5E1)
                                        Text(
                                            text = "★",
                                            color = starColor,
                                            fontSize = 10.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = String.format(java.util.Locale.US, "%.1f", ratingVal),
                                        color = Color(0xFF475569),
                                        fontFamily = PoppinsFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                // Badges Row: Android + Installs (Matching Image 1)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFEEF2FF))
                                            .border(1.dp, Color(0xFFE0E7FF), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 7.dp, vertical = 2.5.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Android,
                                                contentDescription = null,
                                                tint = Color(0xFF4F46E5),
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Text(
                                                text = "Android",
                                                color = Color(0xFF4F46E5),
                                                fontFamily = PoppinsFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFEEF2FF))
                                            .border(1.dp, Color(0xFFE0E7FF), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.FileDownload,
                                                contentDescription = null,
                                                tint = Color(0xFF4F46E5),
                                                modifier = Modifier.size(9.dp)
                                            )
                                            val installsTagText = remember(task.installsLabel) {
                                                val raw = task.installsLabel.ifBlank { "10K+ Installs" }
                                                    .replace("Downloads", "Installs", ignoreCase = true)
                                                if (raw.contains("Installs", ignoreCase = true)) raw else "$raw Installs"
                                            }
                                            Text(
                                                text = installsTagText,
                                                color = Color(0xFF4F46E5),
                                                fontFamily = PoppinsFontFamily,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 8.sp,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Right Column: Glowing Timer Pill Card + Golden Ticket Price Card (Matching Image 2)
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // 1. Glowing Timer Pill Card (Live Countdown with glowing animation)
                            val liveMins = liveRemainingSeconds / 60L
                            val liveSecs = liveRemainingSeconds % 60L
                            val timerDisplay = String.format(java.util.Locale.US, "%02d:%02d", liveMins, liveSecs)
                            val rawTotalDuration = task.durationMinutes.coerceAtLeast(1) * 60L
                            val totalDurationSecs = rawTotalDuration.toFloat()
                            val timerProgress = (liveRemainingSeconds.toFloat() / totalDurationSecs).coerceIn(0f, 1f)
                            val isTimerRunning = liveRemainingSeconds < rawTotalDuration && liveRemainingSeconds > 0L

                            AppTimerPill(
                                displayTime = timerDisplay,
                                isOn = isTimerRunning,
                                progress = timerProgress
                            )

                            // 2. Golden Ticket Price Card (PRICE ₹25)
                            GoldenTicketPriceCard(
                                rewardInr = task.rewardInr
                            )
                        }
                    }

                    // Task Progress Bar Card (Dynamic based on comment availability)
                    val totalComments = task.totalCommentsCount.coerceAtLeast(1)
                    val usedComments = (task.totalCommentsCount - task.availableCommentsCount).coerceAtLeast(0)
                    
                    val currentProgressPct = when {
                        isCompletedForUser || isPendingReview -> 100
                        task.totalCommentsCount > 0 -> {
                            val pct = (usedComments.toFloat() / totalComments.toFloat()) * 100f
                            pct.toInt().coerceIn(0, 100)
                        }
                        isAppliedForUser -> 50
                        else -> 0
                    }
                    val progressLabel = if (task.totalCommentsCount > 0) {
                        "$usedComments/$totalComments"
                    } else {
                        "$currentProgressPct/100"
                    }

                    val animatedProgress by animateFloatAsState(
                        targetValue = currentProgressPct / 100f,
                        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                        label = "task_progress_anim"
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AssignmentTurnedIn,
                                    contentDescription = null,
                                    tint = Color(0xFF6366F1),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Task Progress",
                                    color = Color(0xFF64748B),
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                text = progressLabel,
                                color = Color(0xFF1E1B4B),
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = Color(0xFF6366F1),
                            trackColor = Color(0xFFEEF2FF)
                        )
                    }

                    // Action Button: Locked when Completed, or "Apply for Task" -> "Applied • Upload Proof >" / "Re-work & Submit Proof >"
                    val buttonColors = when {
                        isCompletedForUser -> listOf(Color(0xFF15803D), Color(0xFF10B981))
                        isPendingReview -> listOf(Color(0xFFD97706), Color(0xFFF59E0B))
                        isReworkRequired -> listOf(Color(0xFFE11D48), Color(0xFFF43F5E))
                        isAppliedForUser -> listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
                        else -> listOf(Color(0xFF5B21B6), Color(0xFF7C3AED), Color(0xFF9333EA))
                    }
                    val buttonText = when {
                        isCompletedForUser -> "✓ Completed"
                        isPendingReview -> "In Review • Submitted  >"
                        isReworkRequired -> "Re-work & Submit Proof  >"
                        isAppliedForUser -> "Applied • Upload Proof  >"
                        else -> "Apply for Task  >"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .shadow(
                                elevation = if (isCompletedForUser) 2.dp else 6.dp,
                                shape = RoundedCornerShape(12.dp),
                                spotColor = Color(0xFF6D28D9).copy(alpha = 0.25f)
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.horizontalGradient(colors = buttonColors))
                            .clickable(enabled = !isCompletedForUser) {
                                if (!isCompletedForUser) {
                                    FirebaseEmailDispatchService.markTaskApplied(context, task.id)
                                    appliedTasksRefreshTick += 1
                                    activeTaskForProof = if (isSelectedTask) null else task
                                }
                            }
                            .testTag("offer_view_details_${task.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (!isSelectedTask && !isCompletedForUser) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            Text(
                                text = buttonText,
                                color = Color.White,
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                        }
                    }
                }

                // 3D Folded Blue-Purple Ribbon Asset Offset 0.3 cm Further to the Right
                Box(
                    modifier = Modifier
                        .offset(x = 22.dp, y = (-2).dp)
                        .width(115.dp)
                        .height(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_offer_ribbon_3d),
                        contentDescription = "Offer Ribbon",
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize()
                    )
                    Row(
                        modifier = Modifier.padding(start = 16.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isCompletedForUser) "✓ Completed" else "Offer ${index + 1}",
                            color = Color.White,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp
                        )
                    }
                }
            }

        if (activeTaskForProof != null) {
            val currentTask = activeTaskForProof!!
            val (activeTaskComment, liveRemainingSeconds, onCopyStartTimer) = rememberTaskTimerAndComment(context, currentTask)

            val taskSubmissions = remember(submissions, currentTask.id, currentTask.title) {
                submissions.filter {
                    it.taskId == currentTask.id || it.taskTitle.equals(currentTask.title, ignoreCase = true)
                }
            }
            val isPendingReview = remember(taskSubmissions) {
                taskSubmissions.any {
                    val st = it.status.lowercase()
                    st == "pending" || st == "submitted" || st == "in_review" || st == "under_review"
                }
            }
            val isReworkRequired = remember(taskSubmissions, isPendingReview) {
                !isPendingReview && taskSubmissions.any {
                    val st = it.status.lowercase()
                    st == "rejected" || st == "declined" || st == "rework"
                }
            }

            TaskDetailsFullScreenModal(
                task = currentTask,
                isSubmitting = isSubmitting,
                isPendingReview = isPendingReview,
                isRework = isReworkRequired,
                assignedReviewText = activeTaskComment,
                liveRemainingSeconds = liveRemainingSeconds,
                onCopyComment = onCopyStartTimer,
                onDismiss = { activeTaskForProof = null },
                onSubmit = { proofMsg, screenshotB64 ->
                    val submittedTask = currentTask
                    activeTaskForProof = null
                    celebratedSubmittedTask = Pair(submittedTask, null)

                    onSubmitTaskProof(submittedTask, proofMsg, screenshotB64) { aiResult ->
                        if (celebratedSubmittedTask?.first?.id == submittedTask.id) {
                            celebratedSubmittedTask = Pair(submittedTask, aiResult)
                        }
                    }
                }
            )
        }
    }
}
}

    if (showSubmissionsSection) {
        MySubmissionsFullScreenModal(
            submissions = submissions,
            onDismiss = { showSubmissionsSection = false }
        )
    }
}


@Composable
private fun SubmissionMetricTile(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    count: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color(0xFF32238C).copy(alpha = 0.06f),
                spotColor = Color(0xFF32238C).copy(alpha = 0.12f)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = "$count",
            color = Color(0xFF17142E),
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp
        )
        Text(
            text = label,
            color = Color(0xFF64748B),
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 10.5.sp
        )
    }
}

@Composable
private fun MySubmissionsFullScreenModal(
    submissions: List<TaskSubmission>,
    onDismiss: () -> Unit
) {
    var selectedPeriodIndex by remember { mutableIntStateOf(0) }
    val periods = remember { listOf("Today", "7 Days", "30 Days") }

    val nowMs = System.currentTimeMillis()
    val todayStartMs = remember(nowMs) {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }
    val sevenDaysAgoMs = remember(nowMs) { nowMs - (7L * 24 * 60 * 60 * 1000) }
    val thirtyDaysAgoMs = remember(nowMs) { nowMs - (30L * 24 * 60 * 60 * 1000) }

    val dateFilteredSubmissions = remember(submissions, selectedPeriodIndex, todayStartMs, sevenDaysAgoMs, thirtyDaysAgoMs) {
        val threshold = when (selectedPeriodIndex) {
            0 -> todayStartMs
            1 -> sevenDaysAgoMs
            2 -> thirtyDaysAgoMs
            else -> 0L
        }
        submissions.filter { sub ->
            val subTimeMs = sub.createdAt?.toDate()?.time ?: sub.updatedAt?.toDate()?.time ?: 0L
            subTimeMs >= threshold
        }
    }

    val periodSubmittedCount = remember(dateFilteredSubmissions) {
        dateFilteredSubmissions.count {
            val st = it.status.lowercase()
            st == "pending" || st == "submitted" || st == "in_review" || st == "under_review"
        }
    }
    val periodApprovedCount = remember(dateFilteredSubmissions) {
        dateFilteredSubmissions.count {
            val st = it.status.lowercase()
            st == "approved" || st == "verified" || st == "completed"
        }
    }
    val periodRejectedCount = remember(dateFilteredSubmissions) {
        dateFilteredSubmissions.count {
            val st = it.status.lowercase()
            st == "rejected" || st == "declined" || st == "rework"
        }
    }
    val periodApprovedEarnings = remember(dateFilteredSubmissions) {
        dateFilteredSubmissions.filter {
            val st = it.status.lowercase()
            st == "approved" || st == "verified" || st == "completed"
        }.sumOf { it.rewardAmount }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF8FAFC)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Header Row: Back Arrow + Title "My submissions"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF1E293B),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = "My submissions",
                        color = Color(0xFF17142E),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp
                    )
                }

                // Period Tabs: Today | 7 Days | 30 Days
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFFEDEAF6))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    periods.forEachIndexed { idx, label ->
                        val isSelected = selectedPeriodIndex == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    if (isSelected) {
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF635BFF), Color(0xFF8B5CF6))
                                        )
                                    } else {
                                        Brush.horizontalGradient(
                                            listOf(Color.Transparent, Color.Transparent)
                                        )
                                    }
                                )
                                .clickable { selectedPeriodIndex = idx }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else Color(0xFF6F6E8A),
                                fontFamily = PoppinsFontFamily,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }

                // 3 Metric Cards Row: Submitted | Approved | Rejected
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SubmissionMetricTile(
                        icon = Icons.Outlined.AccessTime,
                        iconTint = Color(0xFF7B3FE4),
                        iconBg = Color(0xFFF3E8FF),
                        count = periodSubmittedCount,
                        label = "Submitted",
                        modifier = Modifier.weight(1f)
                    )
                    SubmissionMetricTile(
                        icon = Icons.Outlined.CheckCircle,
                        iconTint = Color(0xFF10B981),
                        iconBg = Color(0xFFDCFCE7),
                        count = periodApprovedCount,
                        label = "Approved",
                        modifier = Modifier.weight(1f)
                    )
                    SubmissionMetricTile(
                        icon = Icons.Outlined.Cancel,
                        iconTint = Color(0xFFF43F5E),
                        iconBg = Color(0xFFFFE4E6),
                        count = periodRejectedCount,
                        label = "Rejected",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Approved Earnings Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(18.dp),
                            ambientColor = Color(0xFF32238C).copy(alpha = 0.06f),
                            spotColor = Color(0xFF32238C).copy(alpha = 0.12f)
                        )
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDCFCE7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "₹",
                                color = Color(0xFF10B981),
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = "Approved earnings",
                            color = Color(0xFF17142E),
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }

                    Text(
                        text = "₹$periodApprovedEarnings",
                        color = Color(0xFF10B981),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    )
                }

                // Submissions List or Empty State
                if (dateFilteredSubmissions.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(22.dp))
                            .padding(horizontal = 24.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF3E8FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = null,
                                tint = Color(0xFF7B3FE4),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "No submissions yet",
                            color = Color(0xFF17142E),
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Submissions for the selected period will appear here.",
                            color = Color(0xFF6F6E8A),
                            fontFamily = InterFontFamily,
                            fontSize = 11.5.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    dateFilteredSubmissions.forEach { sub ->
                        val statusLower = sub.status.lowercase()
                        val isApproved = statusLower == "verified" || statusLower == "approved"
                        val isRejected = statusLower == "rejected" || statusLower == "declined"
                        val badgeColor = when {
                            isApproved -> Color(0xFF059669)
                            isRejected -> Color(0xFFDC2626)
                            else -> Color(0xFFD97706)
                        }
                        val badgeLabel = when {
                            isApproved -> "Approved"
                            isRejected -> "Rejected"
                            else -> "Pending"
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                .padding(13.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(11.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(badgeColor.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when {
                                            isApproved -> Icons.Outlined.VerifiedUser
                                            isRejected -> Icons.Outlined.Cancel
                                            else -> Icons.Outlined.AccessTime
                                        },
                                        contentDescription = null,
                                        tint = badgeColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = sub.taskTitle.ifBlank { "Task" },
                                            color = Color(0xFF17142E),
                                            fontFamily = PoppinsFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(badgeColor.copy(alpha = 0.12f))
                                                .border(1.dp, badgeColor.copy(alpha = 0.25f), RoundedCornerShape(999.dp))
                                                .padding(horizontal = 9.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = badgeLabel,
                                                color = badgeColor,
                                                fontFamily = PoppinsFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.5.sp
                                            )
                                        }
                                    }
                                    val dateStr = sub.createdAt?.toDate()?.let { d ->
                                        java.text.SimpleDateFormat("MMM dd, hh:mm a", java.util.Locale.ENGLISH).format(d)
                                    } ?: "Recently"
                                    Text(
                                        text = dateStr,
                                        color = Color(0xFF64748B),
                                        fontFamily = InterFontFamily,
                                        fontSize = 10.5.sp
                                    )
                                }
                                Text(
                                    text = "₹${sub.rewardAmount}",
                                    color = Color(0xFF17142E),
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                            }

                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFF3E8FF))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "Screenshot proof",
                                            color = Color(0xFF6D28D9),
                                            fontFamily = PoppinsFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 9.5.sp
                                        )
                                    }
                                }

                                val footnoteText = when {
                                    isApproved -> "Approved. Payment arrives in 7–8 days."
                                    isRejected -> "Rejected. You can try this task again."
                                    else -> "Waiting for verification."
                                }
                                Text(
                                    text = footnoteText,
                                    color = Color(0xFF64748B),
                                    fontFamily = InterFontFamily,
                                    fontSize = 10.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dedicated Full-Screen Task Details Page (Matching Reference Screenshot 2):
 * - Top Left: "< Back" Button
 * - Top Card: Hero App Icon, Title, Subtitle, Stars, Badges, Live Timer, Price Ticket
 * - Standalone Section Cards: Details Box, Assigned Review Comment, Install Button, Upload Proof, Action Buttons
 */
@Composable
private fun TaskDetailsFullScreenModal(
    task: AvailableTask,
    isSubmitting: Boolean,
    isPendingReview: Boolean = false,
    isRework: Boolean = false,
    assignedReviewText: String = "",
    liveRemainingSeconds: Long = 900L,
    onCopyComment: () -> Unit = {},
    onDismiss: () -> Unit,
    onSubmit: (proofMessage: String, screenshotBase64: String) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF8FAFC)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Header Row: "< Back" Button (Matching Screenshot 2)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFFEFF6FF))
                            .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(999.dp))
                            .clickable { onDismiss() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "‹",
                                color = Color(0xFF2563EB),
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Back",
                                color = Color(0xFF2563EB),
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // App Hero Header Card (Matching Screenshot 2)
                val appDisplayTitle = task.appName.ifBlank { task.title }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(11.dp)
                        ) {
                            val effectiveIconUrl = remember(task.iconUrl, task.instructions) {
                                task.iconUrl.ifBlank {
                                    task.instructions.firstOrNull { it.contains("[TASK_PFP]:") }
                                        ?.substringAfter("[TASK_PFP]:")?.trim().orEmpty()
                                }
                            }
                            RemoteAppIconBox(
                                iconUrl = effectiveIconUrl,
                                appTitle = appDisplayTitle,
                                size = 50.dp,
                                cornerRadius = 14.dp
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = appDisplayTitle,
                                    color = Color(0xFF17142E),
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = task.subtitle.ifBlank { "Install App and Rate" },
                                    color = Color(0xFF64748B),
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    val ratingVal = task.rating.coerceIn(1.0, 5.0)
                                    repeat(5) { starIdx ->
                                        val starColor = if (starIdx < ratingVal.toInt()) Color(0xFFF59E0B) else Color(0xFFCBD5E1)
                                        Text(text = "★", color = starColor, fontSize = 10.sp)
                                    }
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = String.format(java.util.Locale.US, "%.1f", ratingVal),
                                        color = Color(0xFF475569),
                                        fontFamily = PoppinsFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFEEF2FF))
                                            .border(1.dp, Color(0xFFE0E7FF), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 7.dp, vertical = 2.5.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Android,
                                                contentDescription = null,
                                                tint = Color(0xFF4F46E5),
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Text(
                                                text = "Android",
                                                color = Color(0xFF4F46E5),
                                                fontFamily = PoppinsFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFEEF2FF))
                                            .border(1.dp, Color(0xFFE0E7FF), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.FileDownload,
                                                contentDescription = null,
                                                tint = Color(0xFF4F46E5),
                                                modifier = Modifier.size(9.dp)
                                            )
                                            val installsTagText = remember(task.installsLabel) {
                                                val raw = task.installsLabel.ifBlank { "10K+ Installs" }
                                                    .replace("Downloads", "Installs", ignoreCase = true)
                                                if (raw.contains("Installs", ignoreCase = true)) raw else "$raw Installs"
                                            }
                                            Text(
                                                text = installsTagText,
                                                color = Color(0xFF4F46E5),
                                                fontFamily = PoppinsFontFamily,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 8.sp,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Right Column: Glowing Timer Pill Card + Golden Ticket Price Card (Matching Image 2)
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val liveMins = liveRemainingSeconds / 60L
                            val liveSecs = liveRemainingSeconds % 60L
                            val timerDisplay = String.format(java.util.Locale.US, "%02d:%02d", liveMins, liveSecs)
                            val rawTotalDuration = task.durationMinutes.coerceAtLeast(1) * 60L
                            val totalDurationSecs = rawTotalDuration.toFloat()
                            val timerProgress = (liveRemainingSeconds.toFloat() / totalDurationSecs).coerceIn(0f, 1f)
                            val isTimerRunning = liveRemainingSeconds < rawTotalDuration && liveRemainingSeconds > 0L

                            AppTimerPill(
                                displayTime = timerDisplay,
                                isOn = isTimerRunning,
                                progress = timerProgress
                            )

                            GoldenTicketPriceCard(
                                rewardInr = task.rewardInr
                            )
                        }
                    }
                }

                // Render the Standalone Section Cards (Details, Assigned Review Comment, Install, Upload Screenshot Proof, Action Buttons)
                TaskSubmissionFormPanel(
                    task = task,
                    isSubmitting = isSubmitting,
                    isPendingReview = isPendingReview,
                    isRework = isRework,
                    assignedReviewText = assignedReviewText,
                    onCopyComment = onCopyComment,
                    onCancel = onDismiss,
                    onSubmit = onSubmit
                )
            }
        }
    }
}

/**
 * Inline Task Details & Screenshot-Only Submission Panel:
 * - No time limits or countdown timers
 * - Shows "Application Status: Applied ✓" and "Reward: ₹..."
 * - Displays Reference Sample Screenshot if provided on the project
 * - Copy Assigned Review Comment button (opens Play Store / link if provided)
 * - Screenshot-only proof upload (no user text message box)
 */
@Composable
private fun TaskSubmissionFormPanel(
    task: AvailableTask,
    isSubmitting: Boolean,
    isPendingReview: Boolean = false,
    isRework: Boolean = false,
    assignedReviewText: String = "",
    onCopyComment: () -> Unit = {},
    onCancel: () -> Unit,
    onSubmit: (proofMessage: String, screenshotBase64: String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val effectiveReviewText = remember(task.id, assignedReviewText, task.assignedComment) {
        assignedReviewText.ifBlank {
            task.assignedComment.ifBlank {
                "Best app experience! Very fast, clean interface and super helpful features."
            }
        }
    }

    var screenshotBase64 by remember(task.id) { mutableStateOf("") }
    var isCompressingImage by remember(task.id) { mutableStateOf(false) }
    var validationError by remember(task.id) { mutableStateOf<String?>(null) }
    var commentCopied by remember(task.id) { mutableStateOf(false) }
    var showFullSampleImage by remember(task.id) { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isCompressingImage = true
            validationError = null
            scope.launch {
                val encoded = TaskGullakNotificationHelper.compressUriToBase64DataUrl(context, uri)
                isCompressingImage = false
                if (encoded != null) {
                    screenshotBase64 = encoded
                } else {
                    validationError = "Could not process selected screenshot. Please try another image."
                }
            }
        }
    }

    val previewBitmap = remember(screenshotBase64) {
        if (screenshotBase64.startsWith("data:image")) {
            try {
                val raw = screenshotBase64.substringAfter("base64,")
                val bytes = Base64.decode(raw, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    val appDisplayTitle = task.appName.ifBlank { task.title }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Details Section (Title + Light Green Card with Numbered Steps)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Details",
                color = Color(0xFF17142E),
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.5.sp
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFECFDF5))
                    .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val stepsList = remember(task.instructions) {
                    val raw = if (task.instructions.isNotEmpty()) {
                        task.instructions
                    } else {
                        listOf(
                            "Install the app.",
                            "Give a 5-star review on the Play Store.",
                            "Copy the comment and paste it there.",
                            "Send a screenshot after posting."
                        )
                    }
                    raw.filterNot { 
                        it.contains("[TASK_PFP]:") || 
                        it.contains("[EXAMPLE_PROOF]:") || 
                        it.contains("[SAMPLE_PROOF]:") || 
                        it.contains("[EXAMPLE]:") ||
                        it.trim().startsWith("data:image/")
                    }.map { line ->
                        line.trim().replace(Regex("^[0-9]+[.\\s]+"), "").trim()
                    }.filter { it.isNotBlank() }
                }

                stepsList.forEachIndexed { idx, cleanStep ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD1FAE5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${idx + 1}",
                                color = Color(0xFF047857),
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = cleanStep,
                            color = Color(0xFF065F46),
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.5.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFA7F3D0).copy(alpha = 0.6f))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = Color(0xFFB45309),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "The screenshot must not be cropped or edited.",
                        color = Color(0xFFB45309),
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.5.sp
                    )
                }
            }
        }

        // 4. "Assigned Review Comment" Box with integrated Copy Icon (English only, no separate button)
        if (effectiveReviewText.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFDDD6FE), RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Assigned Review Comment",
                        color = Color(0xFF17142E),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (commentCopied) Color(0xFF10B981) else Color(0xFFCBD5E1)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            clipboardManager.setText(AnnotatedString(effectiveReviewText))
                            onCopyComment()
                            FirebaseEmailDispatchService.recordTaskApplication(
                                context = context,
                                taskId = task.id,
                                taskTitle = appDisplayTitle,
                                commentText = effectiveReviewText
                            )
                            commentCopied = true
                            scope.launch {
                                kotlinx.coroutines.delay(500L)
                                commentCopied = false
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "\"$effectiveReviewText\"",
                            color = Color(0xFF1E293B),
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (commentCopied) Color(0xFFDCFCE7) else Color(0xFFEDE9FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (commentCopied) Icons.Default.Check else Icons.Outlined.ContentCopy,
                                contentDescription = "Copy Comment",
                                tint = if (commentCopied) Color(0xFF16A34A) else Color(0xFF6D28D9),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "Copying comment starts the timer.",
                    color = Color(0xFF64748B),
                    fontFamily = InterFontFamily,
                    fontSize = 10.5.sp,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }

        // 5. Install Button (Redirects to Play Store & triggers 50% Task Progress)
        val playStoreTargetUrl = remember(task.storeUrl, task.subtitle) {
            val candidate = task.storeUrl.ifBlank { task.subtitle }
            if (candidate.startsWith("www.")) "https://$candidate" else candidate
        }

        if (playStoreTargetUrl.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF4F46E5), Color(0xFF6366F1), Color(0xFF7C3AED))
                        )
                    )
                    .clickable {
                        FirebaseEmailDispatchService.markTaskApplied(context, task.id)
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(playStoreTargetUrl))
                            )
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Install",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Install",
                    color = Color.White,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp
                )
            }
        }

        // 4. Reference Sample Screenshot Card (Below Steps to Complete & Above Upload Proof)
        val effectiveReferenceScreenshot = remember(task.referenceScreenshotUrl, task.instructions) {
            if (task.referenceScreenshotUrl.isNotBlank()) {
                task.referenceScreenshotUrl
            } else {
                task.instructions.firstOrNull { 
                    it.contains("[EXAMPLE_PROOF]:") || it.contains("[SAMPLE_PROOF]:") || it.contains("[EXAMPLE]:")
                }?.let { line ->
                    when {
                        line.contains("[EXAMPLE_PROOF]:") -> line.substringAfter("[EXAMPLE_PROOF]:").trim()
                        line.contains("[SAMPLE_PROOF]:") -> line.substringAfter("[SAMPLE_PROOF]:").trim()
                        line.contains("[EXAMPLE]:") -> line.substringAfter("[EXAMPLE]:").trim()
                        else -> ""
                    }
                }.orEmpty()
            }
        }

        if (effectiveReferenceScreenshot.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFC4B5FD), RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "REFERENCE SAMPLE SCREENSHOT (TAP TO ENLARGE)",
                    color = Color(0xFF5B21B6),
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.5.sp,
                    letterSpacing = 0.7.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.clickable { showFullSampleImage = true }
                    ) {
                        RemoteAppIconBox(
                            iconUrl = effectiveReferenceScreenshot,
                            appTitle = "Sample",
                            size = 72.dp,
                            cornerRadius = 12.dp
                        )
                    }
                    Text(
                        text = "Complete the task exactly as shown in this reference sample. Tap image to view full screen.",
                        color = Color(0xFF475569),
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showFullSampleImage = true }
                    )
                }
            }
        }

        // Full Screen Sample Screenshot Modal (Preserves exact 9:16 or original aspect ratio, no cropping)
        if (showFullSampleImage && effectiveReferenceScreenshot.isNotBlank()) {
            FullSampleImageDialog(
                imageUrl = effectiveReferenceScreenshot,
                onDismiss = { showFullSampleImage = false }
            )
        }

        // 5. Upload Screenshot Proof Card (Disabled when Pending Review)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .border(
                    width = 1.2.dp,
                    color = if (screenshotBase64.isNotBlank()) Color(0xFF10B981) else Color(0xFFC4B5FD),
                    shape = RoundedCornerShape(14.dp)
                )
                .clickable(
                    enabled = !isPendingReview,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (!isPendingReview) {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                }
                .padding(12.dp)
                .testTag("upload_task_screenshot_button"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (previewBitmap != null) {
                Image(
                    bitmap = previewBitmap,
                    contentDescription = "Selected screenshot preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isPendingReview) Color(0xFFFEF3C7) else Color(0xFFF3E8FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPendingReview) Icons.Outlined.HourglassEmpty else Icons.Outlined.AddPhotoAlternate,
                        contentDescription = "Upload screenshot",
                        tint = if (isPendingReview) Color(0xFFD97706) else Color(0xFF7B3FE4),
                        modifier = Modifier.size(21.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        isPendingReview -> "Submission Under Review"
                        isCompressingImage -> "Processing screenshot..."
                        screenshotBase64.isNotBlank() -> "Screenshot Proof Attached ✓"
                        else -> "Upload Screenshot Proof"
                    },
                    color = when {
                        isPendingReview -> Color(0xFFD97706)
                        screenshotBase64.isNotBlank() -> Color(0xFF15803D)
                        else -> Color(0xFF17142E)
                    },
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = when {
                        isPendingReview -> "Your proof is currently being reviewed by the team."
                        screenshotBase64.isNotBlank() -> "Tap to change image • Ready for submission"
                        else -> "Tap here to select your completed task screenshot"
                    },
                    color = Color(0xFF6F6E8A),
                    fontFamily = InterFontFamily,
                    fontSize = 10.sp
                )
            }
        }

        if (validationError != null) {
            Text(
                text = validationError!!,
                color = Color(0xFFE11D48),
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(0.32f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE2E8F0))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onCancel() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Cancel",
                    color = Color(0xFF475569),
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Box(modifier = Modifier.weight(0.68f)) {
                ShinyGradientButton(
                    text = when {
                        isPendingReview -> "Submitted • Awaiting Review"
                        isSubmitting -> "Verifying Proof..."
                        else -> "Submit Screenshot Proof"
                    },
                    onClick = {
                        if (!isPendingReview) {
                            if (screenshotBase64.isBlank()) {
                                validationError = "Please upload your screenshot proof before submitting."
                                return@ShinyGradientButton
                            }
                            validationError = null
                            onSubmit(assignedReviewText, screenshotBase64)
                        }
                    },
                    enabled = !isPendingReview && !isSubmitting && !isCompressingImage,
                    gradientColors = if (isPendingReview) listOf(Color(0xFFD97706), Color(0xFFF59E0B)) else listOf(Color(0xFF6D28D9), Color(0xFF9333EA)),
                    height = 44.dp,
                    cornerRadius = 12.dp,
                    fontSizeSp = 12.5f
                )
            }
        }
    }
}

/**
 * Full-Screen Modal to display sample screenshot in its exact, uncropped aspect ratio (e.g. 9:16).
 */
@Composable
private fun FullSampleImageDialog(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    val cleanUrl = imageUrl.trim()
    var loadedBitmap by remember(cleanUrl) {
        mutableStateOf(if (cleanUrl.isNotEmpty()) RemoteIconBitmapCache[cleanUrl] else null)
    }

    LaunchedEffect(cleanUrl) {
        if (cleanUrl.isNotEmpty() && loadedBitmap == null) {
            val fetched = withContext(Dispatchers.IO) {
                runCatching {
                    if (cleanUrl.startsWith("data:image")) {
                        val raw = cleanUrl.substringAfter("base64,")
                        val bytes = Base64.decode(raw, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                    } else {
                        val connection = (URL(cleanUrl).openConnection() as HttpURLConnection).apply {
                            connectTimeout = 6000
                            readTimeout = 6000
                            instanceFollowRedirects = true
                            setRequestProperty("User-Agent", "Mozilla/5.0")
                        }
                        connection.inputStream.use { stream ->
                            BitmapFactory.decodeStream(stream)?.asImageBitmap()
                        }
                    }
                }.getOrNull()
            }
            if (fetched != null) {
                RemoteIconBitmapCache[cleanUrl] = fetched
                loadedBitmap = fetched
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0F172A))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sample Screenshot Example",
                        color = Color.White,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { onDismiss() }
                    )
                }

                if (loadedBitmap != null) {
                    Image(
                        bitmap = loadedBitmap!!,
                        contentDescription = "Full Sample Screenshot",
                        contentScale = ContentScale.Fit, // Preserves exact 9:16 or original aspect ratio without cropping!
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 500.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFC4B5FD),
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Celebratory Dark Task Submitted Modal with formal 5-tier verification results (no timer or internal terms).
 */
@Composable
private fun TaskSubmittedCelebrationDialog(
    task: AvailableTask,
    aiVerificationResult: AiTaskVerificationResult? = null,
    onDismiss: () -> Unit
) {
    val creditedInr = aiVerificationResult?.creditedRewardInr ?: task.rewardInr.toLong()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(20.dp, RoundedCornerShape(32.dp), ambientColor = Color(0xFFB9A8E8))
                .clip(RoundedCornerShape(32.dp)),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFFFF1C9), Color(0xFFF8F4FF), Color.White)
                        )
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hero Section with wave background, 3D bouncing coins, and 3D heart eyes smiley
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFFFFD95D), Color(0xFFFFEBA2), Color(0xFFFFF8E6), Color.White)
                            )
                        )
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val wave = Path().apply {
                            moveTo(0f, size.height * .70f)
                            cubicTo(
                                size.width * .27f,
                                size.height * .87f,
                                size.width * .68f,
                                size.height * .61f,
                                size.width,
                                size.height * .73f
                            )
                            lineTo(size.width, size.height)
                            lineTo(0f, size.height)
                            close()
                        }
                        drawPath(wave, Color.White.copy(alpha = .88f))
                    }

                    // Bouncing 3D Rupee Coins
                    CelebrationBouncingCoin(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 20.dp)
                            .offset(y = (-10).dp),
                        size = 46.dp,
                        duration = 1550
                    )
                    CelebrationBouncingCoin(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 32.dp, end = 28.dp),
                        size = 42.dp,
                        duration = 1850
                    )
                    CelebrationBouncingCoin(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 64.dp)
                            .offset(y = 30.dp),
                        size = 34.dp,
                        duration = 1350
                    )

                    // Pulse-animated 3D Heart Eyes Smiley
                    val pulse = rememberInfiniteTransition(label = "smileyPulse")
                    val scale by pulse.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.05f,
                        animationSpec = infiniteRepeatable(
                            tween(1500, easing = FastOutSlowInEasing),
                            RepeatMode.Reverse
                        ),
                        label = "smileyScale"
                    )
                    Image(
                        painter = painterResource(id = R.drawable.heart_smile_3d),
                        contentDescription = "3D heart eyes smiley",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = 12.dp)
                            .size(140.dp)
                            .scale(scale)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Submission successful!",
                    color = Color(0xFF11113F),
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Your proof has been submitted.",
                    color = Color(0xFF85849F),
                    fontSize = 13.5.sp,
                    fontFamily = InterFontFamily,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "It will be verified soon.",
                    color = Color(0xFF85849F),
                    fontSize = 13.5.sp,
                    fontFamily = InterFontFamily,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Reward Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFFF5F1FF), Color(0xFFF0EDFF))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    // Reward Row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .shadow(6.dp, RoundedCornerShape(18.dp), ambientColor = Color(0xFF17B967).copy(alpha = .25f))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF17B967).copy(alpha = .18f), Color(0xFF17B967).copy(alpha = .08f))
                                    ),
                                    RoundedCornerShape(18.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF17B967), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "✓",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Reward amount",
                                color = Color(0xFF85849F),
                                fontFamily = InterFontFamily,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "₹$creditedInr",
                                color = Color(0xFF11113F),
                                fontFamily = PoppinsFontFamily,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFFDCD7ED))
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Payout Row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .shadow(6.dp, RoundedCornerShape(18.dp), ambientColor = Color(0xFF6744DC).copy(alpha = .25f))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF6744DC).copy(alpha = .18f), Color(0xFF6744DC).copy(alpha = .08f))
                                    ),
                                    RoundedCornerShape(18.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF6744DC), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "▦",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "You will get paid in",
                                color = Color(0xFF85849F),
                                fontFamily = InterFontFamily,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "1–2 days",
                                color = Color(0xFF11113F),
                                fontFamily = PoppinsFontFamily,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Trust Indicators Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Top
                ) {
                    CelebrationTrustItem("₹", "On-time\npayments", Color(0xFFE3A21C), Color(0xFFFFF0C9))
                    CelebrationTrustItem("✓", "Trusted", Color(0xFF7048DF), Color(0xFFECE4FF))
                    CelebrationTrustItem("+", "100%\nreliable", Color(0xFF18B968), Color(0xFFE0FFEC))
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Back to Offers Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .height(52.dp)
                        .shadow(8.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF7050F2), Color(0xFF3547E8))
                                ),
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Back to offers",
                            color = Color.White,
                            fontFamily = PoppinsFontFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CelebrationBouncingCoin(modifier: Modifier = Modifier, size: Dp, duration: Int) {
    val transition = rememberInfiniteTransition(label = "coinBounce")
    val y by transition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            tween(duration, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "coinY"
    )
    Image(
        painter = painterResource(id = R.drawable.rupee_coin_3d),
        contentDescription = "Gold rupee coin",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .offset(y = y.dp)
            .size(size)
    )
}

@Composable
private fun CelebrationTrustItem(symbol: String, label: String, tint: Color, bg: Color) {
    Column(
        modifier = Modifier.width(82.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .shadow(6.dp, RoundedCornerShape(18.dp), ambientColor = tint.copy(alpha = .22f))
                .background(bg, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(tint, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = symbol,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = Color(0xFF25243B),
            fontFamily = InterFontFamily,
            fontSize = 11.5.sp,
            lineHeight = 15.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 2. REFER SCREEN SHELL (Clean, structured UI shell matching the Task Gullak theme)
 */
@Composable
internal fun ReferTabShellContent(
    walletIdCode: String,
    referredUsers: List<UserProfile> = emptyList()
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ContextualTabHeader(
            eyebrowIcon = Icons.Outlined.CardGiftcard,
            eyebrowText = "INVITE & EARN",
            title = "Refer & Earn"
        )
        ExactReferAndEarnScreen(
            liveReferredUsers = referredUsers
        )
    }
}

@Composable
private fun ReferStepItem(
    step: String,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.24f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step,
                color = Color.White,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 9.5.sp,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
            )
        }
        Text(
            text = label,
            color = Color.White,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp
        )
    }
}

// Pre-compiled once at class load time, NOT inside composable rendering loops!
private val REGEX_BONUS_CREDITED = Regex("bonus credited by admin", RegexOption.IGNORE_CASE)
private val REGEX_CREDITED_BY_ADMIN = Regex("credited by admin", RegexOption.IGNORE_CASE)
private val REGEX_ADJUSTED_BY_ADMIN = Regex("adjusted by admin", RegexOption.IGNORE_CASE)
private val REGEX_DEDUCTED_BY_ADMIN = Regex("deducted by admin", RegexOption.IGNORE_CASE)
private val REGEX_ADMIN_PANEL = Regex("admin panel", RegexOption.IGNORE_CASE)
private val REGEX_ADMIN_WORD = Regex("\\badmin\\b", RegexOption.IGNORE_CASE)
private val REGEX_FIREBASE_WORD = Regex("\\bfirebase\\b", RegexOption.IGNORE_CASE)
private val REGEX_BACKEND_WORD = Regex("\\bbackend\\b", RegexOption.IGNORE_CASE)

private fun sanitizeTransactionNote(rawNote: String, isDeduction: Boolean): String {
    if (rawNote.isBlank()) return if (isDeduction) "Amount deducted" else "Amount received"
    return rawNote
        .replace(REGEX_BONUS_CREDITED, "Bonus received")
        .replace(REGEX_CREDITED_BY_ADMIN, "Received")
        .replace(REGEX_ADJUSTED_BY_ADMIN, "Adjustment deducted")
        .replace(REGEX_DEDUCTED_BY_ADMIN, "Deducted")
        .replace(REGEX_ADMIN_PANEL, "Support Desk")
        .replace(REGEX_ADMIN_WORD, "Support")
        .replace(REGEX_FIREBASE_WORD, "Cloud")
        .replace(REGEX_BACKEND_WORD, "Server")
}

/**
 * 3. WALLET SCREEN SHELL (Clean, structured UI shell + real-time Admin credit/deduction ledger)
 */
@Composable
internal fun WalletTabShellContent(
    walletCardSlot: @Composable () -> Unit,
    walletBalance: Long,
    pendingCount: Int,
    submissions: List<TaskSubmission>,
    transactions: List<WalletTransaction> = emptyList(),
    isWithdrawing: Boolean = false,
    withdrawFeedback: String? = null,
    savedUpiId: String = "",
    precalculatedTotalWithdrawn: Long = -1L,
    onSaveUpiId: (String) -> Unit = {},
    onClearWithdrawFeedback: () -> Unit = {},
    onRequestWithdrawal: (amount: Long, payoutMethod: String, payoutDetails: String) -> Unit = { _, _, _ -> }
) {
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    val filters = remember { listOf("All", "Credits", "Payouts") }
    var selectedPayoutMethod by remember { mutableIntStateOf(0) }
    var payoutDetailsInput by remember(savedUpiId) { mutableStateOf(savedUpiId) }
    var upiSavedNotice by remember { mutableStateOf(false) }
    var withdrawAmountInput by remember { mutableStateOf("") }
    var localWithdrawError by remember { mutableStateOf<String?>(null) }
    var showWithdrawForm by remember { mutableStateOf(true) }
    var selectedWithdrawalForReceipt by remember { mutableStateOf<WalletTransaction?>(null) }
    var submittedWithdrawalSuccess by remember { mutableStateOf<Triple<Long, String, String>?>(null) }

    if (selectedWithdrawalForReceipt != null) {
        WithdrawalPaymentStatusModal(
            transaction = selectedWithdrawalForReceipt!!,
            fallbackUpiId = savedUpiId,
            onDismiss = { selectedWithdrawalForReceipt = null }
        )
    }

    if (submittedWithdrawalSuccess != null) {
        val (amt, method, details) = submittedWithdrawalSuccess!!
        WithdrawalSubmittedSuccessModal(
            amount = amt,
            payoutMethod = method,
            payoutDetails = details,
            onDismiss = { submittedWithdrawalSuccess = null }
        )
    }

    val approvedSubmissions = remember(submissions) {
        submissions.filter {
            val s = it.status.lowercase()
            s == "verified" || s == "approved"
        }
    }

    val totalWithdrawn = if (precalculatedTotalWithdrawn >= 0L) precalculatedTotalWithdrawn else remember(transactions) {
        transactions
            .filter { it.type.equals("withdrawal", ignoreCase = true) }
            .sumOf { it.amount }
    }

    val triggerWithdrawalAction = remember(
        withdrawAmountInput,
        selectedPayoutMethod,
        payoutDetailsInput,
        walletBalance
    ) {
        {
            onClearWithdrawFeedback()
            localWithdrawError = null
            val parsedAmount = withdrawAmountInput.trim().toLongOrNull()
                ?: walletBalance.takeIf { it >= 100L }
                ?: 0L
            val methodLabel = if (selectedPayoutMethod == 0) "UPI Transfer" else "Bank (IMPS)"
            val cleanDetails = payoutDetailsInput.trim()
            when {
                walletBalance < 100L -> {
                    localWithdrawError = "Minimum withdrawal amount is ₹100 (Available balance: ₹$walletBalance)."
                }
                parsedAmount < 100L -> {
                    localWithdrawError = "Minimum withdrawal amount is ₹100."
                }
                parsedAmount > walletBalance -> {
                    localWithdrawError = "Maximum withdrawal amount is ₹$walletBalance (Your available earned balance)."
                }
                cleanDetails.length < 3 -> {
                    localWithdrawError = if (selectedPayoutMethod == 0) {
                        "Please enter your UPI ID (e.g. yourname@upi)."
                    } else {
                        "Please enter your Bank Account & IFSC details."
                    }
                }
                else -> {
                    onRequestWithdrawal(parsedAmount, methodLabel, cleanDetails)
                    submittedWithdrawalSuccess = Triple(parsedAmount, methodLabel, cleanDetails)
                    withdrawAmountInput = ""
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ContextualTabHeader(
            eyebrowIcon = Icons.Outlined.AccountBalanceWallet,
            eyebrowText = "GULLAK TREASURY",
            title = "Wallet"
        )

        walletCardSlot()

        // 3 Balance Breakdown Tiles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WalletBreakdownTile(
                label = "Total Earned",
                amountText = "₹${walletBalance + totalWithdrawn}",
                accentColor = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
            WalletBreakdownTile(
                label = "In Review",
                amountText = "$pendingCount Tasks",
                accentColor = Color(0xFFF5A524),
                modifier = Modifier.weight(1f)
            )
            WalletBreakdownTile(
                label = "Withdrawn",
                amountText = "₹$totalWithdrawn",
                accentColor = Color(0xFF7B3FE4),
                modifier = Modifier.weight(1f)
            )
        }

        // Payout & Withdrawal Card with Withdraw Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(22.dp),
                    ambientColor = Color(0xFF32238C).copy(alpha = 0.08f),
                    spotColor = Color(0xFF32238C).copy(alpha = 0.14f)
                )
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.95f))
                .border(1.dp, Color.White, RoundedCornerShape(22.dp))
                .padding(18.dp)
                .testTag("wallet_withdrawal_card"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Quick Withdrawal",
                        color = Color(0xFF17142E),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (walletBalance >= 100L) {
                            "Min: ₹100 • Max: ₹$walletBalance (Available Earned)"
                        } else {
                            "Available: ₹$walletBalance • Min Withdrawal: ₹100"
                        },
                        color = Color(0xFF6F6E8A),
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PayoutMethodChip(
                    icon = Icons.Outlined.QrCode2,
                    title = "UPI Transfer",
                    subtitle = "Instant • 0% Fee",
                    isSelected = selectedPayoutMethod == 0,
                    onClick = { selectedPayoutMethod = 0 },
                    modifier = Modifier.weight(1f)
                )
                PayoutMethodChip(
                    icon = Icons.Outlined.AccountBalance,
                    title = "Bank (IMPS)",
                    subtitle = "1–2 hrs • Verified",
                    isSelected = selectedPayoutMethod == 1,
                    onClick = { selectedPayoutMethod = 1 },
                    modifier = Modifier.weight(1f)
                )
            }

            if (showWithdrawForm) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = payoutDetailsInput,
                        onValueChange = {
                            payoutDetailsInput = it
                            localWithdrawError = null
                            upiSavedNotice = false
                        },
                        singleLine = true,
                        label = {
                            Text(
                                text = if (selectedPayoutMethod == 0) "Enter UPI ID (e.g. name@upi)" else "Account Number & IFSC Code",
                                fontFamily = InterFontFamily,
                                fontSize = 12.sp
                            )
                        },
                        trailingIcon = {
                            if (selectedPayoutMethod == 0 && payoutDetailsInput.trim().length >= 3) {
                                Box(
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(Color(0xFFEDE9FE))
                                        .clickable {
                                            onSaveUpiId(payoutDetailsInput.trim())
                                            upiSavedNotice = true
                                        }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                        .testTag("save_upi_id_button")
                                ) {
                                    Text(
                                        text = if (upiSavedNotice) "✓ Saved" else "Save UPI",
                                        color = Color(0xFF6D28D9),
                                        fontFamily = PoppinsFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("withdraw_payout_details_input")
                    )

                    OutlinedTextField(
                        value = withdrawAmountInput,
                        onValueChange = { input ->
                            withdrawAmountInput = input.filter { it.isDigit() }.take(7)
                            localWithdrawError = null
                        },
                        singleLine = true,
                        label = {
                            Text(
                                text = if (walletBalance >= 100L) {
                                    "Amount to Withdraw (Min ₹100 • Max ₹$walletBalance)"
                                } else {
                                    "Amount to Withdraw (Min ₹100)"
                                },
                                fontFamily = InterFontFamily,
                                fontSize = 12.sp
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("withdraw_amount_input")
                    )

                    // Quick Amount Chips (Min ₹100 up to User's Total Earned Balance)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(100L, 250L, 500L).forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color(0xFFF3E8FF))
                                    .clickable {
                                        withdrawAmountInput = preset.toString()
                                        localWithdrawError = null
                                    }
                                    .padding(horizontal = 11.dp, vertical = 5.dp)
                                    .testTag("withdraw_preset_$preset")
                            ) {
                                Text(
                                    text = "₹$preset",
                                    color = Color(0xFF7B3FE4),
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                )
                            }
                        }
                        if (walletBalance >= 100L) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color(0xFFDCFCE7))
                                    .clickable {
                                        withdrawAmountInput = walletBalance.toString()
                                        localWithdrawError = null
                                    }
                                    .padding(horizontal = 11.dp, vertical = 5.dp)
                                    .testTag("withdraw_preset_max")
                            ) {
                                Text(
                                    text = "Max (₹$walletBalance)",
                                    color = Color(0xFF15803D),
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                )
                            }
                        }
                    }

                    val activeMessage = localWithdrawError ?: withdrawFeedback
                    if (!activeMessage.isNullOrBlank()) {
                        val isErrorMsg = localWithdrawError != null ||
                            activeMessage.contains("Minimum", ignoreCase = true) ||
                            activeMessage.contains("Maximum", ignoreCase = true) ||
                            activeMessage.contains("Insufficient", ignoreCase = true) ||
                            activeMessage.contains("Failed", ignoreCase = true) ||
                            activeMessage.contains("valid", ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isErrorMsg) Color(0xFFFEF2F2) else Color(0xFFECFDF5)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isErrorMsg) Color(0xFFFECACA) else Color(0xFFA7F3D0),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("withdraw_feedback_banner"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isErrorMsg) Icons.Outlined.Cancel else Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = if (isErrorMsg) Color(0xFFDC2626) else Color(0xFF16A34A),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = activeMessage,
                                color = if (isErrorMsg) Color(0xFF991B1B) else Color(0xFF065F46),
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF16A34A), Color(0xFF10B981))
                                )
                            )
                            .clickable(enabled = !isWithdrawing) { triggerWithdrawalAction() }
                            .testTag("confirm_withdraw_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isWithdrawing) "Submitting Withdrawal..." else "Withdraw Now",
                            color = Color.White,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Confirm Withdraw",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            val progressFraction = (walletBalance.toFloat() / 100f).coerceIn(0f, 1f)
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Payout threshold progress",
                        color = Color(0xFF6F6E8A),
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "₹$walletBalance / ₹100",
                        color = Color(0xFF17142E),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Color(0xFFEDEAF6))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction.coerceAtLeast(0.04f))
                            .height(7.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(
                               Brush.horizontalGradient(
                                    listOf(Color(0xFF4F46E5), Color(0xFF7B3FE4))
                                )
                            )
                    )
                }
            }
        }

        // Recent Transactions Header + Filter Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Activity",
                color = Color(0xFF17142E),
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.5.sp
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFEDEAF6))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                filters.forEachIndexed { idx, label ->
                    val isSelected = selectedFilterIndex == idx
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                if (isSelected) {
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF635BFF), Color(0xFF8B5CF6))
                                    )
                                } else {
                                    Brush.horizontalGradient(
                                        listOf(Color.Transparent, Color.Transparent)
                                    )
                                }
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { selectedFilterIndex = idx }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else Color(0xFF6F6E8A),
                            fontFamily = PoppinsFontFamily,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        val filteredTransactions = remember(transactions, selectedFilterIndex) {
            when (selectedFilterIndex) {
                1 -> transactions.filter {
                    val t = it.type.lowercase()
                    t != "withdrawal" && t != "admin_deduct"
                }
                2 -> transactions.filter {
                    val t = it.type.lowercase()
                    t == "withdrawal" || t == "admin_deduct"
                }
                else -> transactions
            }
        }
        val filteredSubmissions = remember(approvedSubmissions, selectedFilterIndex) {
            if (selectedFilterIndex == 2) emptyList() else approvedSubmissions
        }

        val displayedTransactions = remember(filteredTransactions) {
            filteredTransactions.take(25)
        }

        val hasActivity = displayedTransactions.isNotEmpty() || filteredSubmissions.isNotEmpty()
        if (!hasActivity) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White.copy(alpha = 0.78f))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(22.dp))
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3E8FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ReceiptLong,
                        contentDescription = null,
                        tint = Color(0xFF7B3FE4),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "No transactions yet",
                    color = Color(0xFF17142E),
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Your verified task credits, referral bonuses (+₹5 on referred login), and reward withdrawals will appear here.",
                    color = Color(0xFF6F6E8A),
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.5.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            displayedTransactions.forEach { tx ->
                val lowerType = remember(tx.type) { tx.type.lowercase() }
                val isWithdrawal = lowerType == "withdrawal"
                val isDeduction = lowerType == "admin_deduct" || isWithdrawal
                val txColor = if (isDeduction) Color(0xFFF43F5E) else TgSuccessGreen
                val txStatusLower = remember(tx.status) { tx.status.lowercase() }
                val statusLabel = when (txStatusLower) {
                    "paid", "completed", "success" -> "Paid"
                    "processing" -> "Processing"
                    "approved" -> "Approved"
                    "rejected", "failed" -> "Rejected"
                    else -> "Pending"
                }
                val statusBadgeColor = when (txStatusLower) {
                    "paid", "completed", "success" -> Color(0xFF16A34A)
                    "processing", "approved" -> Color(0xFF2563EB)
                    "rejected", "failed" -> Color(0xFFE11D48)
                    else -> Color(0xFFD97706)
                }
                val txTitle = when (lowerType) {
                    "withdrawal" -> "Wallet Withdrawal"
                    "admin_deduct" -> "Deducted"
                    "referral_commission" -> "Referral Bonus"
                    "admin_credit" -> "Received"
                    else -> if (isDeduction) "Deducted" else "Received"
                }
                val cleanNote = remember(tx.note, isDeduction) {
                    sanitizeTransactionNote(tx.note, isDeduction)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                        .clickable(enabled = isWithdrawal) {
                            selectedWithdrawalForReceipt = tx
                        }
                        .padding(13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(txColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isDeduction) Icons.Outlined.ReceiptLong else Icons.Outlined.AccountBalanceWallet,
                            contentDescription = null,
                            tint = txColor,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = txTitle,
                                color = Color(0xFF17142E),
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            if (isWithdrawal) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(statusBadgeColor.copy(alpha = 0.14f))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = statusLabel,
                                        color = statusBadgeColor,
                                        fontFamily = PoppinsFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (isWithdrawal) "$cleanNote • Tap for receipt" else cleanNote,
                            color = TgSlateMuted,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.5.sp,
                            maxLines = 2
                        )
                    }
                    Text(
                        text = if (isDeduction) "-₹${tx.amount}" else "+₹${tx.amount}",
                        color = txColor,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                }
            }

            filteredSubmissions.take(15).forEach { sub ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                        .padding(13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(TgSuccessGreen.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VerifiedUser,
                            contentDescription = null,
                            tint = TgSuccessGreen,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = sub.taskTitle,
                            color = Color(0xFF17142E),
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = sub.proofText,
                            color = TgSlateMuted,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.5.sp,
                            maxLines = 2
                        )
                    }
                    Text(
                        text = "+₹${sub.rewardAmount}",
                        color = TgSuccessGreen,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun WalletBreakdownTile(
    label: String,
    amountText: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color(0xFF32238C).copy(alpha = 0.06f),
                spotColor = Color(0xFF32238C).copy(alpha = 0.12f)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.95f))
            .border(1.dp, Color.White, RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            color = Color(0xFF6F6E8A),
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 10.5.sp
        )
        Text(
            text = amountText,
           color = accentColor,
           fontFamily = PoppinsFontFamily,
           fontWeight = FontWeight.ExtraBold,
           fontSize = 15.5.sp
       )
   }
}

@Composable
private fun PayoutMethodChip(
   icon: ImageVector,
   title: String,
   subtitle: String,
   isSelected: Boolean,
   onClick: () -> Unit,
   modifier: Modifier = Modifier
) {
   val borderColor by animateColorAsState(
       targetValue = if (isSelected) Color(0xFF7B3FE4) else Color(0xFFE2E8F0),
       animationSpec = tween(220),
       label = "payout_border"
   )
   val bgColor by animateColorAsState(
       targetValue = if (isSelected) Color(0xFFF5F3FF) else Color(0xFFF8FAFC),
       animationSpec = tween(220),
       label = "payout_bg"
   )

   Row(
       modifier = modifier
           .clip(RoundedCornerShape(14.dp))
           .background(bgColor)
           .border(1.2.dp, borderColor, RoundedCornerShape(14.dp))
           .clickable(
               interactionSource = remember { MutableInteractionSource() },
               indication = null,
               onClick = onClick
           )
           .padding(horizontal = 11.dp, vertical = 10.dp),
       verticalAlignment = Alignment.CenterVertically,
       horizontalArrangement = Arrangement.spacedBy(8.dp)
   ) {
       Icon(
           imageVector = icon,
           contentDescription = null,
           tint = if (isSelected) Color(0xFF7B3FE4) else Color(0xFF6F6E8A),
           modifier = Modifier.size(18.dp)
       )
       Column {
           Text(
               text = title,
               color = Color(0xFF17142E),
               fontFamily = PoppinsFontFamily,
               fontWeight = FontWeight.Bold,
               fontSize = 11.5.sp
           )
           Text(
               text = subtitle,
               color = Color(0xFF6F6E8A),
               fontFamily = InterFontFamily,
               fontWeight = FontWeight.Normal,
               fontSize = 9.5.sp
           )
       }
   }
}

/**
* 4. PROFILE SCREEN SHELL & SETTINGS FLOW
* - Contextual Header: "My Profile" (Zero "Task Gullak" brand references)
* - Interactive 3D Avatar Picker (8–10 curated 3D presets synced to SharedPreferences, Firestore /users/{userId}.avatarId, and Home top bar)
* - Profile Menu Items: Removed "Privacy" & "Tour Onboarding"; Added "Settings" (with Name Edit Flow) & "Help & Support"
*/
@Composable
internal fun ProfileTabShellContent(
   userInitial: String,
   displayName: String,
   displayEmail: String,
   currentAvatarId: String,
   walletIdCode: String,
   totalTasks: Int,
   approvedCount: Int,
   walletBalance: Long,
   isSavingName: Boolean = false,
   remoteBio: String = "",
   remoteUsername: String = "",
   remoteBannerId: String = "",
   referredByCode: String = "",
   bulkStatus: String = "none",
   bulkDeviceCount: Int = 0,
   supportTickets: List<SupportTicket> = emptyList(),
   supportBotMessages: List<SupportBotMessage> = emptyList(),
   onSubmitSupportTicket: (String) -> Unit = {},
   onSubmitBulkModRequest: (String, Int) -> Unit = { _, _ -> },
   onSendSupportBotMessage: (String) -> Unit = {},
   onConfirmSupportBotAction: (String, String) -> Unit = { _, _ -> },
   onSelectAvatar: (String) -> Unit = {},
   onSaveBioAndCustomization: (bannerId: String, username: String, bio: String) -> Unit = { _, _, _ -> },
   onSaveDisplayName: (String, (Boolean, String) -> Unit) -> Unit = { _, cb -> cb(true, "Saved") },
   onNavigateToWallet: () -> Unit = {},
   onNavigateToOffers: () -> Unit = {},
   onNavigateToRefer: () -> Unit = {},
   onNavigateToTerms: () -> Unit,
   onSignOut: () -> Unit
) {
   val context = LocalContext.current
   var notificationsEnabled by remember { mutableStateOf(true) }
   var showAvatarPickerSheet by remember { mutableStateOf(false) }
   var showHelpSupportSheet by remember { mutableStateOf(false) }
   var showSupportBotSheet by remember { mutableStateOf(false) }
   var showBulkModSheet by remember { mutableStateOf(false) }
   var showBannerAndBioSheet by remember { mutableStateOf(false) }
   var showAllBatchesSheet by remember { mutableStateOf(false) }
   var flowerAnimationBatch by remember { mutableStateOf<AchievementBatchInfo?>(null) }
   var isInSettingsSubScreen by remember { mutableStateOf(false) }

   var bannerId by remember {
       mutableStateOf(
           remoteBannerId.ifBlank { FirebaseEmailDispatchService.getSavedBannerId(context) }
       )
   }
   var usernameHandle by remember {
       mutableStateOf(
           remoteUsername.ifBlank { FirebaseEmailDispatchService.getSavedUsername(context) }
       )
   }
   var userBio by remember {
       mutableStateOf(
           remoteBio.ifBlank { FirebaseEmailDispatchService.getSavedBio(context) }
       )
   }

   LaunchedEffect(remoteBio, remoteUsername, remoteBannerId) {
       if (remoteBio.isNotBlank()) {
           userBio = remoteBio
           FirebaseEmailDispatchService.saveProfileBio(context, remoteBio)
       }
       if (remoteUsername.isNotBlank()) {
           usernameHandle = remoteUsername
           FirebaseEmailDispatchService.saveProfileUsername(context, remoteUsername)
       }
       if (remoteBannerId.isNotBlank()) {
           bannerId = remoteBannerId
           FirebaseEmailDispatchService.saveProfileBannerId(context, remoteBannerId)
       }
   }

   val effectiveCompletedCount = remember(approvedCount) {
       approvedCount.coerceAtLeast(0)
   }
   val allBatches = remember(effectiveCompletedCount) {
       FirebaseEmailDispatchService.getAchievementBatches(effectiveCompletedCount)
   }
   val unlockedBatchesCount = remember(allBatches) {
       allBatches.count { it.isUnlocked }
   }
   val currentBatch = remember(effectiveCompletedCount) {
       FirebaseEmailDispatchService.getCurrentUnlockedOrNextBatch(effectiveCompletedCount)
   }
   if (showAvatarPickerSheet) {
       AvatarPickerModalBottomSheet(
           currentAvatarId = currentAvatarId,
           userInitial = userInitial,
           onSelectAvatar = { chosenId ->
               onSelectAvatar(chosenId)
           },
           onDismiss = { showAvatarPickerSheet = false }
       )
   }

   if (showHelpSupportSheet) {
       HelpAndSupportBottomSheet(
           userName = displayName,
           userEmail = displayEmail,
           walletIdCode = walletIdCode,
           supportTickets = supportTickets,
           onSubmitTicket = onSubmitSupportTicket,
           onOpenSupportBot = { },
           onDismiss = { showHelpSupportSheet = false }
       )
   }

   if (showBulkModSheet) {
       BulkModApplyModalBottomSheet(
           currentBulkStatus = bulkStatus,
           currentDeviceCount = bulkDeviceCount,
           onSubmitRequest = onSubmitBulkModRequest,
           onDismiss = { showBulkModSheet = false }
       )
   }

   if (showBannerAndBioSheet) {
       ProfileBannerAndBioSheet(
           currentBannerId = bannerId,
           currentUsername = usernameHandle,
           currentBio = userBio,
           onSaveBannerAndBio = { newBanner, newUsername, newBio ->
               FirebaseEmailDispatchService.saveProfileBannerId(context, newBanner)
               FirebaseEmailDispatchService.saveProfileUsername(context, newUsername)
               FirebaseEmailDispatchService.saveProfileBio(context, newBio)
               bannerId = FirebaseEmailDispatchService.getSavedBannerId(context)
               usernameHandle = FirebaseEmailDispatchService.getSavedUsername(context)
               userBio = FirebaseEmailDispatchService.getSavedBio(context)
               onSaveBioAndCustomization(bannerId, usernameHandle, userBio)
               showBannerAndBioSheet = false
           },
           onDismiss = { showBannerAndBioSheet = false }
       )
   }

   if (showAllBatchesSheet) {
       AchievementsBatchesModalSheet(
           batches = allBatches,
           completedTasksCount = effectiveCompletedCount,
           onSelectBatchForFlowerAnimation = { chosenBatch ->
               if (chosenBatch.isUnlocked) {
                   showAllBatchesSheet = false
                   flowerAnimationBatch = chosenBatch
               }
           },
           onDismiss = { showAllBatchesSheet = false }
       )
   }

   val activeFlowerBatch = flowerAnimationBatch
   if (activeFlowerBatch != null) {
       FlowerUnlockAnimationDialog(
           batch = activeFlowerBatch,
           onDismiss = { flowerAnimationBatch = null }
       )
   }

   if (isInSettingsSubScreen) {
       ProfileSettingsScreenContent(
           currentDisplayName = displayName,
           currentEmail = displayEmail,
           currentAvatarId = currentAvatarId,
           userInitial = userInitial,
           notificationsEnabled = notificationsEnabled,
           isSavingName = isSavingName,
           onToggleNotifications = { notificationsEnabled = !notificationsEnabled },
           onOpenAvatarPicker = { showAvatarPickerSheet = true },
           onSaveDisplayName = onSaveDisplayName,
           onBackToProfile = { isInSettingsSubScreen = false },
           onSignOut = onSignOut
       )
       return
   }

   val activePreset = remember(currentAvatarId) { findAvatarPresetById(currentAvatarId) }

   Column(
       modifier = Modifier
           .fillMaxWidth()
           .widthIn(max = 520.dp),
       verticalArrangement = Arrangement.spacedBy(14.dp)
   ) {
       ContextualTabHeader(
           eyebrowIcon = Icons.Outlined.PersonOutline,
           eyebrowText = "ACCOUNT",
           title = "My Profile"
       )

       // 1. TOP PROFILE HERO CARD (Editable Pastel Banner + Overlapping 3D Avatar + Name + @username + Bio + Batch Pill)
       Column(
           modifier = Modifier
               .fillMaxWidth()
               .shadow(
                   elevation = 12.dp,
                   shape = RoundedCornerShape(24.dp),
                   ambientColor = Color(0xFF32238C).copy(alpha = 0.08f),
                   spotColor = Color(0xFF32238C).copy(alpha = 0.16f)
               )
               .clip(RoundedCornerShape(24.dp))
               .background(Color.White.copy(alpha = 0.98f))
               .border(1.dp, Color.White, RoundedCornerShape(24.dp))
       ) {
           Box(
               modifier = Modifier.fillMaxWidth(),
               contentAlignment = Alignment.BottomCenter
           ) {
               EditableProfilePastelBanner(
                   bannerId = bannerId,
                   onEditBannerClick = { showBannerAndBioSheet = true },
                   modifier = Modifier.padding(bottom = 36.dp)
               )

               // Overlapping Centered 3D Avatar (NO .clip(CircleShape) on outer Box so bottom-right Edit Badge is never clipped)
               Box(
                   modifier = Modifier
                       .size(84.dp)
                       .background(Color.White, CircleShape)
                       .padding(4.dp),
                   contentAlignment = Alignment.Center
               ) {
                   UserAvatar3DBadge(
                       avatarId = currentAvatarId,
                       userInitial = userInitial,
                       size = 76.dp,
                       showEditBadge = true,
                       onClick = { showAvatarPickerSheet = true },
                       testTag = "profile_avatar_picker_button"
                   )
               }
           }

           Column(
               modifier = Modifier
                   .fillMaxWidth()
                   .padding(horizontal = 18.dp, vertical = 12.dp),
               horizontalAlignment = Alignment.CenterHorizontally,
               verticalArrangement = Arrangement.spacedBy(8.dp)
           ) {
               // Row 1: Display Name + Rounded-Corner Verified Shield Badge
               Row(
                   verticalAlignment = Alignment.CenterVertically,
                   horizontalArrangement = Arrangement.spacedBy(8.dp)
               ) {
                   Text(
                       text = displayName,
                       color = Color(0xFF0F172A),
                       fontFamily = PoppinsFontFamily,
                       fontWeight = FontWeight.ExtraBold,
                       fontSize = 20.sp,
                       maxLines = 1,
                       overflow = TextOverflow.Ellipsis,
                       modifier = Modifier.testTag("profile_display_name_text")
                   )
                   Row(
                       modifier = Modifier
                           .clip(RoundedCornerShape(8.dp))
                           .background(Color(0xFFECFDF5))
                           .border(1.dp, Color(0xFF6EE7B7), RoundedCornerShape(8.dp))
                           .padding(horizontal = 8.dp, vertical = 3.dp),
                       verticalAlignment = Alignment.CenterVertically,
                       horizontalArrangement = Arrangement.spacedBy(4.dp)
                   ) {
                       Icon(
                           imageVector = Icons.Outlined.VerifiedUser,
                           contentDescription = "Verified",
                           tint = Color(0xFF059669),
                           modifier = Modifier.size(12.dp)
                       )
                       Text(
                           text = "Verified",
                           color = Color(0xFF047857),
                           fontFamily = PoppinsFontFamily,
                           fontWeight = FontWeight.Bold,
                           fontSize = 9.5.sp
                       )
                   }
               }

               // Row 2: Organized @Username Pill + Email Pill
               Row(
                   verticalAlignment = Alignment.CenterVertically,
                   horizontalArrangement = Arrangement.spacedBy(6.dp)
               ) {
                   Box(
                       modifier = Modifier
                           .clip(RoundedCornerShape(8.dp))
                           .background(Color(0xFFEEF2FF))
                           .border(1.dp, Color(0xFFC7D2FE), RoundedCornerShape(8.dp))
                           .clickable { showBannerAndBioSheet = true }
                           .padding(horizontal = 8.dp, vertical = 3.dp)
                   ) {
                       Text(
                           text = usernameHandle,
                           color = Color(0xFF4338CA),
                           fontFamily = PoppinsFontFamily,
                           fontWeight = FontWeight.Bold,
                           fontSize = 10.5.sp,
                           maxLines = 1
                       )
                   }

                   Box(
                       modifier = Modifier
                           .clip(RoundedCornerShape(8.dp))
                           .background(Color(0xFFF1F5F9))
                           .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                           .padding(horizontal = 9.dp, vertical = 3.dp)
                   ) {
                       Text(
                           text = displayEmail,
                           color = Color(0xFF334155),
                           fontFamily = InterFontFamily,
                           fontWeight = FontWeight.SemiBold,
                           fontSize = 10.5.sp,
                           maxLines = 1,
                           overflow = TextOverflow.Ellipsis
                       )
                   }
               }

               // Row 3: Organized Editable Bio Box (Tap directly to edit bio!)
               Row(
                   modifier = Modifier
                       .fillMaxWidth(0.92f)
                       .clip(RoundedCornerShape(10.dp))
                       .background(Color(0xFFF8FAFC))
                       .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                       .clickable { showBannerAndBioSheet = true }
                       .padding(horizontal = 12.dp, vertical = 6.dp)
                       .testTag("profile_bio_edit_box"),
                   verticalAlignment = Alignment.CenterVertically,
                   horizontalArrangement = Arrangement.Center
               ) {
                   Text(
                       text = userBio,
                       color = Color(0xFF1E293B),
                       fontFamily = InterFontFamily,
                       fontWeight = FontWeight.SemiBold,
                       fontSize = 12.sp,
                       textAlign = TextAlign.Center,
                       modifier = Modifier.weight(1f, fill = false)
                   )
                   Spacer(modifier = Modifier.width(6.dp))
                   Icon(
                       imageVector = Icons.Outlined.Edit,
                       contentDescription = "Edit Bio",
                       tint = Color(0xFF6366F1),
                       modifier = Modifier.size(13.dp)
                   )
               }

               // Row 4: Rounded-Corner Batch Box & Curated 3D Avatar Box
               Row(
                   horizontalArrangement = Arrangement.spacedBy(8.dp),
                   verticalAlignment = Alignment.CenterVertically
               ) {
                   Row(
                       modifier = Modifier
                           .clip(RoundedCornerShape(10.dp))
                           .background(
                               Brush.horizontalGradient(
                                   colors = listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A))
                               )
                           )
                           .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                           .clickable {
                               if (currentBatch.isUnlocked) {
                                   flowerAnimationBatch = currentBatch
                               } else {
                                   showAllBatchesSheet = true
                               }
                           }
                           .padding(horizontal = 12.dp, vertical = 6.dp)
                           .testTag("profile_creator_batch_pill"),
                       verticalAlignment = Alignment.CenterVertically,
                       horizontalArrangement = Arrangement.spacedBy(5.dp)
                   ) {
                       Text(
                           text = if (currentBatch.isUnlocked) {
                               "👑 Batch ${currentBatch.batchNumber}: ${currentBatch.title} >"
                           } else {
                               "🔒 Batch ${currentBatch.batchNumber}: ${currentBatch.title} ($effectiveCompletedCount/${currentBatch.requiredTasks}) >"
                           },
                           color = Color(0xFF92400E),
                           fontFamily = PoppinsFontFamily,
                           fontWeight = FontWeight.Bold,
                           fontSize = 10.5.sp
                       )
                   }

                   Box(
                       modifier = Modifier
                           .clip(RoundedCornerShape(10.dp))
                           .background(Color(0xFFEDE9FE))
                           .border(1.dp, Color(0xFFC4B5FD), RoundedCornerShape(10.dp))
                           .clickable { showAvatarPickerSheet = true }
                           .padding(horizontal = 11.dp, vertical = 6.dp)
                   ) {
                       Text(
                           text = activePreset?.title ?: "Pick 3D Avatar",
                           color = Color(0xFF5B21B6),
                           fontFamily = PoppinsFontFamily,
                           fontWeight = FontWeight.Bold,
                           fontSize = 10.5.sp
                       )
                   }
               }

               Spacer(modifier = Modifier.height(2.dp))
           }
       }

       // FEATURED TASK STAR / MILESTONE ACHIEVEMENT CARD WITH FLOWER UNLOCK ANIMATION
       FeaturedAchievementBatchCard(
           currentBatch = currentBatch,
           completedTasksCount = effectiveCompletedCount,
           unlockedBatchesCount = unlockedBatchesCount,
           onTriggerFlowerUnlock = { batch -> flowerAnimationBatch = batch },
           onOpenAllBatches = { showAllBatchesSheet = true }
       )

       // 5. PROFILE OPTIONS LIST (Edit Profile, Achievements, My Tasks, Rewards & Offers, Notification Switch, Help & Support, Settings, Terms)
       Column(
           modifier = Modifier
               .fillMaxWidth()
               .shadow(
                   elevation = 10.dp,
                   shape = RoundedCornerShape(22.dp),
                   ambientColor = Color(0xFF32238C).copy(alpha = 0.07f),
                   spotColor = Color(0xFF32238C).copy(alpha = 0.13f)
               )
               .clip(RoundedCornerShape(22.dp))
               .background(Color.White.copy(alpha = 0.96f))
               .border(1.dp, Color.White, RoundedCornerShape(22.dp))
               .padding(vertical = 8.dp)
       ) {
           ProfileSettingRow(
               icon = Icons.Outlined.Edit,
               iconTint = Color(0xFF2563EB),
               iconBg = Color(0xFFDBEAFE),
               title = "Edit Profile",
               subtitle = "Change name, @username, bio, avatar & banner",
               onClick = { showBannerAndBioSheet = true },
               testTag = "profile_edit_profile_row"
           )
           HorizontalDivider(
               color = Color(0xFFF1F5F9),
               modifier = Modifier.padding(horizontal = 16.dp)
           )
           ProfileSettingRow(
               icon = Icons.Outlined.EmojiEvents,
               iconTint = Color(0xFFD97706),
               iconBg = Color(0xFFFEF3C7),
               title = "Achievements",
               subtitle = "50 Milestone Batches (10 to 500 Tasks) & Flower Unlock",
               trailingText = "$unlockedBatchesCount/50 >",
               onClick = { showAllBatchesSheet = true },
               testTag = "profile_achievements_row"
           )
           HorizontalDivider(
               color = Color(0xFFF1F5F9),
               modifier = Modifier.padding(horizontal = 16.dp)
           )
           ProfileSettingRow(
               icon = Icons.Outlined.CheckCircle,
               iconTint = Color(0xFF7C3AED),
               iconBg = Color(0xFFEDE9FE),
               title = "My Tasks",
               subtitle = "View active tasks, submissions & approval status",
               onClick = onNavigateToOffers,
               testTag = "profile_my_tasks_row"
           )
           HorizontalDivider(
               color = Color(0xFFF1F5F9),
               modifier = Modifier.padding(horizontal = 16.dp)
           )
           ProfileSettingRow(
               icon = Icons.Outlined.CardGiftcard,
               iconTint = Color(0xFFEA580C),
               iconBg = Color(0xFFFFEDD5),
               title = "Rewards & Offers",
               subtitle = "Refer friends on taskgullak.space & earn 5% lifetime",
               onClick = onNavigateToRefer,
               testTag = "profile_rewards_offers_row"
           )
           HorizontalDivider(
               color = Color(0xFFF1F5F9),
               modifier = Modifier.padding(horizontal = 16.dp)
           )
           // Notification Row with ON/OFF Toggle Switch
           Row(
               modifier = Modifier
                   .fillMaxWidth()
                   .clickable { notificationsEnabled = !notificationsEnabled }
                   .padding(horizontal = 16.dp, vertical = 10.dp)
                   .testTag("profile_notification_toggle_row"),
               verticalAlignment = Alignment.CenterVertically,
               horizontalArrangement = Arrangement.spacedBy(12.dp)
           ) {
               Box(
                   modifier = Modifier
                       .size(38.dp)
                       .clip(RoundedCornerShape(12.dp))
                       .background(Color(0xFFDCFCE7)),
                   contentAlignment = Alignment.Center
               ) {
                   Icon(
                       imageVector = Icons.Outlined.NotificationsActive,
                       contentDescription = null,
                       tint = Color(0xFF16A34A),
                       modifier = Modifier.size(19.dp)
                   )
               }
               Column(modifier = Modifier.weight(1f)) {
                   Text(
                       text = "Notification",
                       color = Color(0xFF17142E),
                       fontFamily = PoppinsFontFamily,
                       fontWeight = FontWeight.Bold,
                       fontSize = 13.sp
                   )
                   Text(
                       text = if (notificationsEnabled) "ON • Task approvals & announcements" else "OFF • Muted",
                       color = Color(0xFF6F6E8A),
                       fontFamily = InterFontFamily,
                       fontSize = 11.sp
                   )
               }
               Switch(
                   checked = notificationsEnabled,
                   onCheckedChange = { notificationsEnabled = it },
                   colors = SwitchDefaults.colors(
                       checkedThumbColor = Color.White,
                       checkedTrackColor = Color(0xFF16A34A)
                   )
               )
           }
           HorizontalDivider(
               color = Color(0xFFF1F5F9),
               modifier = Modifier.padding(horizontal = 16.dp)
           )
           ProfileSettingRow(
               icon = Icons.Outlined.SupportAgent,
               iconTint = Color(0xFF0284C7),
               iconBg = Color(0xFFE0F2FE),
               title = "Help & Support",
               subtitle = "Contact support or send an account inquiry",
               onClick = { showHelpSupportSheet = true },
               testTag = "profile_help_support_row"
           )
           HorizontalDivider(
               color = Color(0xFFF1F5F9),
               modifier = Modifier.padding(horizontal = 16.dp)
           )
           val bulkSubtitle = when (bulkStatus) {
               "pending" -> "Request Pending Admin Review"
               "approved" -> "Bulk Mod Active (Device Limit: $bulkDeviceCount)"
               "banned" -> "Bulk Mod Banned"
               else -> "Apply to handle comments in bulk across multiple devices"
           }
           ProfileSettingRow(
               icon = Icons.Outlined.GroupWork,
               iconTint = Color(0xFF7B3FE4),
               iconBg = Color(0xFFF3E8FF),
               title = if (bulkStatus == "approved") "Bulk Mod Active" else "Apply for Bulk Mod",
               subtitle = bulkSubtitle,
               onClick = {
                   if (bulkStatus == "none" || bulkStatus == "rejected" || bulkStatus == "pending") {
                       showBulkModSheet = true
                   }
               },
               testTag = "profile_apply_bulk_mod_row"
           )
           HorizontalDivider(
               color = Color(0xFFF1F5F9),
               modifier = Modifier.padding(horizontal = 16.dp)
           )
           ProfileSettingRow(
               icon = Icons.Outlined.Settings,
               iconTint = Color(0xFF4F46E5),
               iconBg = Color(0xFFE0E7FF),
               title = "Settings",
               subtitle = "Edit profile name, avatar & account preferences",
               onClick = { isInSettingsSubScreen = true },
               testTag = "profile_settings_row"
           )
           HorizontalDivider(
               color = Color(0xFFF1F5F9),
               modifier = Modifier.padding(horizontal = 16.dp)
           )
           ProfileSettingRow(
               icon = Icons.Outlined.Gavel,
               iconTint = Color(0xFF0EA5E9),
               iconBg = Color(0xFFE0F2FE),
               title = "Terms & Conditions",
               subtitle = "Platform usage and task verification rules",
               onClick = onNavigateToTerms,
               testTag = "home_terms_link"
           )
       }

       ShinyGradientButton(
           text = "Logout",
           onClick = onSignOut,
           gradientColors = listOf(Color(0xFFEF4444), Color(0xFFDC2626)),
           height = 46.dp,
           cornerRadius = 14.dp,
           fontSizeSp = 12.5f,
           testTag = "profile_sign_out_button"
       )
   }
}

@Composable
private fun ProfileBlueCardStatItem(
   label: String,
   value: String
) {
   Column(horizontalAlignment = Alignment.CenterHorizontally) {
       Text(
           text = label,
           color = Color.White.copy(alpha = 0.78f),
           fontFamily = InterFontFamily,
           fontWeight = FontWeight.Medium,
           fontSize = 10.5.sp
       )
       Spacer(modifier = Modifier.height(2.dp))
       Text(
           text = value,
           color = Color.White,
           fontFamily = PoppinsFontFamily,
           fontWeight = FontWeight.ExtraBold,
           fontSize = 16.sp
       )
   }
}

@Composable
private fun ProfileStatColumn(
   value: String,
   label: String
) {
   Column(horizontalAlignment = Alignment.CenterHorizontally) {
       Text(
           text = value,
           color = Color(0xFF17142E),
           fontFamily = PoppinsFontFamily,
           fontWeight = FontWeight.ExtraBold,
           fontSize = 16.sp
       )
       Text(
           text = label,
           color = Color(0xFF6F6E8A),
           fontFamily = InterFontFamily,
           fontWeight = FontWeight.Medium,
           fontSize = 10.5.sp
       )
   }
}

@Composable
private fun ProfileSettingRow(
   icon: ImageVector,
   iconTint: Color,
   iconBg: Color,
   title: String,
   subtitle: String,
   trailingText: String? = null,
   testTag: String? = null,
   onClick: () -> Unit
) {
   val baseModifier = Modifier
       .fillMaxWidth()
       .clickable(
           interactionSource = remember { MutableInteractionSource() },
           indication = null,
           onClick = onClick
       )
       .padding(horizontal = 16.dp, vertical = 12.dp)

   Row(
       modifier = if (testTag != null) baseModifier.testTag(testTag) else baseModifier,
       verticalAlignment = Alignment.CenterVertically,
       horizontalArrangement = Arrangement.spacedBy(12.dp)
   ) {
       Box(
           modifier = Modifier
               .size(38.dp)
               .clip(RoundedCornerShape(12.dp))
               .background(iconBg),
           contentAlignment = Alignment.Center
       ) {
           Icon(
               imageVector = icon,
               contentDescription = null,
               tint = iconTint,
               modifier = Modifier.size(19.dp)
           )
       }

       Column(modifier = Modifier.weight(1f)) {
           Text(
               text = title,
               color = Color(0xFF17142E),
               fontFamily = PoppinsFontFamily,
               fontWeight = FontWeight.Bold,
               fontSize = 13.sp
           )
           Text(
               text = subtitle,
               color = Color(0xFF6F6E8A),
               fontFamily = InterFontFamily,
               fontWeight = FontWeight.Normal,
               fontSize = 11.sp
           )
       }

       if (trailingText != null) {
           Box(
               modifier = Modifier
                   .clip(RoundedCornerShape(999.dp))
                   .background(Color(0xFFF3E8FF))
                   .padding(horizontal = 10.dp, vertical = 4.dp)
           ) {
               Text(
                   text = trailingText,
                   color = Color(0xFF7B3FE4),
                   fontFamily = PoppinsFontFamily,
                   fontWeight = FontWeight.Bold,
                   fontSize = 10.sp
               )
           }
       } else {
           Icon(
               imageVector = Icons.AutoMirrored.Filled.ArrowForward,
               contentDescription = null,
               tint = Color(0xFF94A3B8),
               modifier = Modifier.size(16.dp)
           )
       }
   }
}

// Low-Level Audio Synthesis via AudioTrack to render clean sound waves natively
private fun playSynthesizedSound(type: String) {
   runCatching {
       val sampleRate = 44100
       val numSamples: Int
       val generatedSnd: ShortArray

       if (type == "processing") {
           numSamples = (sampleRate * 2.0).toInt() // 2 Seconds swoosh
           generatedSnd = ShortArray(numSamples)
           for (i in 0 until numSamples) {
               val t = i.toDouble() / sampleRate
               val freq = 150.0 + (150.0 * (i.toDouble() / numSamples))
               val envelope = 1.0 - (i.toDouble() / numSamples)
               generatedSnd[i] = (kotlin.math.sin(2.0 * Math.PI * freq * t) * 32767.0 * 0.15 * envelope).toInt().toShort()
           }
       } else {
           numSamples = (sampleRate * 0.8).toInt()
           generatedSnd = ShortArray(numSamples)
           val note1Freq = 523.25 // C5
           val note2Freq = 783.99 // G5
           val note2Delay = (sampleRate * 0.12).toInt()

           for (i in 0 until numSamples) {
               val t = i.toDouble() / sampleRate
               var sample = 0.0

               val env1 = kotlin.math.max(0.0, 1.0 - (i.toDouble() / (sampleRate * 0.4)))
               sample += kotlin.math.sin(2.0 * Math.PI * note1Freq * t) * env1

               if (i >= note2Delay) {
                   val t2 = (i - note2Delay).toDouble() / sampleRate
                   val env2 = kotlin.math.max(0.0, 1.0 - ((i - note2Delay).toDouble() / (sampleRate * 0.6)))
                   sample += kotlin.math.sin(2.0 * Math.PI * note2Freq * t2) * env2
               }
               generatedSnd[i] = (sample * 32767.0 * 0.25).toInt().toShort()
           }
       }

       val audioTrack = android.media.AudioTrack(
           android.media.AudioManager.STREAM_MUSIC,
           sampleRate,
           android.media.AudioFormat.CHANNEL_OUT_MONO,
           android.media.AudioFormat.ENCODING_PCM_16BIT,
           generatedSnd.size * 2,
           android.media.AudioTrack.MODE_STATIC
       )
       audioTrack.write(generatedSnd, 0, generatedSnd.size)
       audioTrack.play()
   }
}

/**
* Exact 100% replica of Payment Success Popup matching Image 2:
* - Upper half screen: Glowing dashed neon-emerald vertical figure-8 (lemniscate) path + green paper plane flying + center checkmark badge
* - Bottom half screen: White rounded bottom card with "Payment Successful", "₹amount" (38.sp bold), "UPI ID: upiId", "Today, time"
*/
@Composable
fun PaymentSuccessPopup(
   visible: Boolean,
   amount: Long,
   upiId: String,
   onDismiss: () -> Unit
) {
   if (!visible) return

   val coroutineScope = rememberCoroutineScope()
   var paymentState by remember { mutableStateOf("processing") } // "processing" | "success"
   val formattedTime = remember {
       val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.US)
       "Today, " + sdf.format(java.util.Date())
   }

   LaunchedEffect(Unit) {
       coroutineScope.launch { playSynthesizedSound("processing") }
       delay(2000)
       paymentState = "success"
       playSynthesizedSound("success")
   }

   // In-place flying paper plane motion during "processing"
   val flightTransition = rememberInfiniteTransition(label = "paper_plane_in_place_flight")
   val bobbingOffset by flightTransition.animateFloat(
       initialValue = -6f,
       targetValue = 6f,
       animationSpec = infiniteRepeatable(
           animation = tween(1200, easing = FastOutSlowInEasing),
           repeatMode = RepeatMode.Reverse
       ),
       label = "plane_bobbing"
   )
   val tiltAngle by flightTransition.animateFloat(
       initialValue = -8f,
       targetValue = 8f,
       animationSpec = infiniteRepeatable(
           animation = tween(1400, easing = FastOutSlowInEasing),
           repeatMode = RepeatMode.Reverse
       ),
       label = "plane_tilt"
   )
   val wingPulse by flightTransition.animateFloat(
       initialValue = 0.95f,
       targetValue = 1.05f,
       animationSpec = infiniteRepeatable(
           animation = tween(1000, easing = FastOutSlowInEasing),
           repeatMode = RepeatMode.Reverse
       ),
       label = "plane_pulse"
   )

   val checkmarkScale by animateFloatAsState(
       targetValue = if (paymentState == "success") 1f else 0.2f,
       animationSpec = spring(
           dampingRatio = Spring.DampingRatioMediumBouncy,
           stiffness = Spring.StiffnessLow
       ),
       label = "checkmark_scale"
   )

   Dialog(
       onDismissRequest = onDismiss,
       properties = DialogProperties(usePlatformDefaultWidth = false)
   ) {
       Box(
           modifier = Modifier
               .fillMaxSize()
               .background(Color(0xD90F172A)) // Dark slate translucent backdrop
               .clickable(onClick = onDismiss),
           contentAlignment = Alignment.BottomCenter
       ) {
           // White Bottom Sheet Popup Card
           Surface(
               modifier = Modifier
                   .fillMaxWidth()
                   .widthIn(max = 440.dp)
                   .padding(horizontal = 16.dp, vertical = 20.dp)
                   .shadow(28.dp, RoundedCornerShape(32.dp), spotColor = Color(0xFF0F172A).copy(alpha = 0.4f))
                   .clip(RoundedCornerShape(32.dp))
                   .clickable(enabled = false, onClick = {}),
               color = Color.White
           ) {
               Column(
                   modifier = Modifier
                       .fillMaxWidth()
                       .background(
                           Brush.verticalGradient(
                               colors = listOf(Color(0xFFFAFCFF), Color(0xFFF1F5F9))
                           )
                       )
                       .padding(horizontal = 24.dp, vertical = 28.dp),
                   horizontalAlignment = Alignment.CenterHorizontally,
                   verticalArrangement = Arrangement.spacedBy(12.dp)
               ) {
                   // Top Center Stage for Flying Paper Plane or Green Checkmark Tick
                   Box(
                       contentAlignment = Alignment.Center,
                       modifier = Modifier.size(130.dp)
                   ) {
                       // Outer glowing emerald halo ring
                       Box(
                           modifier = Modifier
                               .size(110.dp)
                               .clip(CircleShape)
                               .background(Color(0xFFBBF7D0).copy(alpha = 0.65f))
                               .border(3.dp, Color(0xFF4ADE80), CircleShape)
                       )

                       if (paymentState == "processing") {
                           // In-place Flying Green Paper Plane with motion (tilt, bobbing, wing pulse) — No green background circle!
                           Box(
                               contentAlignment = Alignment.Center,
                               modifier = Modifier
                                   .offset(y = bobbingOffset.dp)
                                   .rotate(tiltAngle)
                                   .scale(wingPulse)
                                   .size(72.dp)
                           ) {
                               Icon(
                                   imageVector = Icons.AutoMirrored.Outlined.Send,
                                   contentDescription = "Flying Green Paper Plane",
                                   tint = Color(0xFF10B981),
                                   modifier = Modifier
                                       .size(52.dp)
                                       .shadow(8.dp, CircleShape, spotColor = Color(0xFF10B981))
                               )
                           }
                       } else {
                           // Converted Green Checkmark (Tick) Success Badge
                           Box(
                               contentAlignment = Alignment.Center,
                               modifier = Modifier
                                   .scale(checkmarkScale)
                                   .size(86.dp)
                                   .shadow(12.dp, CircleShape, spotColor = Color(0xFF10B981))
                                   .clip(CircleShape)
                                   .background(
                                       Brush.radialGradient(
                                           colors = listOf(Color(0xFF34D399), Color(0xFF059669))
                                       )
                                   )
                                   .border(2.dp, Color(0xFF86EFAC), CircleShape)
                           ) {
                               Icon(
                                   imageVector = Icons.Default.Check,
                                   contentDescription = "Success Tick",
                                   tint = Color.White,
                                   modifier = Modifier.size(50.dp)
                               )
                           }
                       }
                   }

                   // Title & Subtitle
                   Text(
                       text = if (paymentState == "processing") "Processing Request..." else "Payment Successful",
                       color = Color(0xFF334155),
                       fontFamily = PoppinsFontFamily,
                       fontWeight = FontWeight.SemiBold,
                       fontSize = 16.sp,
                       textAlign = TextAlign.Center
                   )

                   Text(
                       text = "₹${String.format(java.util.Locale.US, "%,d", amount)}",
                       color = Color(0xFF0F172A),
                       fontFamily = PoppinsFontFamily,
                       fontWeight = FontWeight.ExtraBold,
                       fontSize = 38.sp,
                       letterSpacing = (-1).sp,
                       textAlign = TextAlign.Center
                   )

                   Spacer(modifier = Modifier.height(2.dp))

                   Text(
                       text = "UPI ID: ${upiId.ifBlank { "taskgullak@upi" }}",
                       color = Color(0xFF334155),
                       fontFamily = InterFontFamily,
                       fontWeight = FontWeight.Bold,
                       fontSize = 13.5.sp,
                       textAlign = TextAlign.Center
                   )

                   Text(
                       text = formattedTime,
                       color = Color(0xFF64748B),
                       fontFamily = InterFontFamily,
                       fontWeight = FontWeight.Medium,
                       fontSize = 12.sp,
                       textAlign = TextAlign.Center
                   )

                   if (paymentState == "success") {
                       Spacer(modifier = Modifier.height(10.dp))
                       Button(
                           onClick = onDismiss,
                           colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                           shape = RoundedCornerShape(14.dp),
                           modifier = Modifier
                               .fillMaxWidth()
                               .height(50.dp)
                               .shadow(6.dp, RoundedCornerShape(14.dp))
                       ) {
                           Text(
                               text = "Done • View History",
                               color = Color.White,
                               fontFamily = PoppinsFontFamily,
                               fontWeight = FontWeight.Bold,
                               fontSize = 14.sp
                           )
                       }
                   }
               }
           }
       }
   }
}

/**
* Animated Flying Paper-Plane & Morphing Success Checkmark Modal
*/
@Composable
private fun WithdrawalSubmittedSuccessModal(
   amount: Long,
   payoutMethod: String,
   payoutDetails: String,
   onDismiss: () -> Unit
) {
   PaymentSuccessPopup(
       visible = true,
       amount = amount,
       upiId = payoutDetails,
       onDismiss = onDismiss
   )
}

/**
* Payment Status / Withdrawal Invoice Receipt Modal matching WhatsApp_Image_2026-10-25_at_9.23.49_AM_64d9fcf4.jpeg
*/
@Composable
private fun WithdrawalPaymentStatusModal(
   transaction: WalletTransaction,
   fallbackUpiId: String,
   onDismiss: () -> Unit
) {
   val context = LocalContext.current
   val statusLower = transaction.status.lowercase()
   val currentStepIndex = when (statusLower) {
       "paid", "completed", "success" -> 3
       "processing" -> 2
       "approved" -> 1
       else -> 0
   }
   val statusTitle = when (statusLower) {
       "paid", "completed", "success" -> "Payment Successful"
       "processing" -> "Payment Processing"
       "approved" -> "Withdrawal Approved"
       "rejected", "failed" -> "Withdrawal Rejected"
       else -> "Pending Verification Review"
   }
   val txDisplayId = transaction.withdrawalId.ifBlank { transaction.id }.uppercase().takeLast(12)
   val methodText = transaction.payoutMethod.ifBlank { "UPI Transfer" }
   val upiDisplay = transaction.upiId.ifBlank { fallbackUpiId.ifBlank { "Verified Account" } }
   val formattedDate = remember(transaction.createdAt) {
       val millis = transaction.createdAt?.toDate()?.time ?: System.currentTimeMillis()
       SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(millis))
   }

   Dialog(onDismissRequest = onDismiss) {
       Column(
           modifier = Modifier
               .fillMaxWidth()
               .clip(RoundedCornerShape(26.dp))
               .background(Color(0xFF1F2430))
               .padding(top = 18.dp, start = 14.dp, end = 14.dp, bottom = 14.dp),
           horizontalAlignment = Alignment.CenterHorizontally
       ) {
           // Top Receipt Printer Slot Bar
           Box(
               modifier = Modifier
                   .fillMaxWidth(0.68f)
                   .height(8.dp)
                   .clip(RoundedCornerShape(999.dp))
                   .background(Color(0xFF0F131A))
           )

           Spacer(modifier = Modifier.height(10.dp))

           // White Paper Receipt Card
           Column(
               modifier = Modifier
                   .fillMaxWidth()
                   .clip(RoundedCornerShape(20.dp))
                   .background(Color.White)
                   .padding(horizontal = 18.dp, vertical = 20.dp),
               horizontalAlignment = Alignment.CenterHorizontally,
               verticalArrangement = Arrangement.spacedBy(12.dp)
           ) {
               Box(
                   modifier = Modifier
                       .size(54.dp)
                       .clip(CircleShape)
                       .background(
                           if (statusLower == "rejected") Color(0xFFFFE4E6) else Color(0xFFDCFCE7)
                       ),
                   contentAlignment = Alignment.Center
               ) {
                   Box(
                       modifier = Modifier
                           .size(38.dp)
                           .clip(CircleShape)
                           .background(
                               if (statusLower == "rejected") Color(0xFFE11D48) else Color(0xFF10B981)
                           ),
                       contentAlignment = Alignment.Center
                   ) {
                       Icon(
                           imageVector = if (statusLower == "rejected") Icons.Outlined.Close else Icons.Filled.Check,
                           contentDescription = null,
                           tint = Color.White,
                           modifier = Modifier.size(22.dp)
                       )
                   }
               }

               Column(
                   horizontalAlignment = Alignment.CenterHorizontally,
                   verticalArrangement = Arrangement.spacedBy(2.dp)
               ) {
                   Text(
                       text = "Payment Status",
                       color = Color(0xFF17142E),
                       fontFamily = PoppinsFontFamily,
                       fontWeight = FontWeight.ExtraBold,
                       fontSize = 16.sp
                   )
                   Text(
                       text = statusTitle,
                       color = if (statusLower == "rejected") Color(0xFFE11D48) else Color(0xFF10B981),
                       fontFamily = InterFontFamily,
                       fontWeight = FontWeight.SemiBold,
                       fontSize = 11.5.sp
                   )
               }

               Text(
                   text = "₹${transaction.amount}.00",
                   color = Color(0xFF17142E),
                   fontFamily = PoppinsFontFamily,
                   fontWeight = FontWeight.ExtraBold,
                   fontSize = 26.sp
               )

               // 4-Step Status Progress Bar (Pending -> Approved -> Processing -> Paid)
               val steps = listOf("Pending", "Approved", "Processing", "Paid")
               Row(
                   modifier = Modifier
                       .fillMaxWidth()
                       .padding(vertical = 4.dp),
                   horizontalArrangement = Arrangement.SpaceBetween,
                   verticalAlignment = Alignment.CenterVertically
               ) {
                   steps.forEachIndexed { idx, stepLabel ->
                       val reached = idx <= currentStepIndex && statusLower != "rejected"
                       Column(
                           horizontalAlignment = Alignment.CenterHorizontally,
                           verticalArrangement = Arrangement.spacedBy(4.dp)
                       ) {
                           Box(
                               modifier = Modifier
                                   .size(22.dp)
                                   .clip(CircleShape)
                                   .background(
                                       if (reached) Color(0xFF10B981) else Color(0xFFE2E8F0)
                                   ),
                               contentAlignment = Alignment.Center
                           ) {
                               Icon(
                                   imageVector = Icons.Filled.Check,
                                   contentDescription = null,
                                   tint = if (reached) Color.White else Color(0xFF94A3B8),
                                   modifier = Modifier.size(12.dp)
                               )
                           }
                           Text(
                               text = stepLabel,
                               color = if (reached) Color(0xFF065F46) else Color(0xFF94A3B8),
                               fontFamily = PoppinsFontFamily,
                               fontWeight = if (reached) FontWeight.Bold else FontWeight.Medium,
                               fontSize = 9.sp
                           )
                       }
                   }
               }

               HorizontalDivider(color = Color(0xFFE2E8F0))

               // Receipt Breakdown Rows
               ReceiptDetailRow(label = "Transaction ID", value = "#$txDisplayId")
               ReceiptDetailRow(label = "Payment Method", value = methodText)
               ReceiptDetailRow(label = "Payout To", value = upiDisplay)
               ReceiptDetailRow(label = "Date & Time", value = formattedDate)

               Spacer(modifier = Modifier.height(4.dp))

               Row(
                   modifier = Modifier.fillMaxWidth(),
                   horizontalArrangement = Arrangement.spacedBy(10.dp)
               ) {
                   Box(
                       modifier = Modifier
                           .weight(0.38f)
                           .height(42.dp)
                           .clip(RoundedCornerShape(999.dp))
                           .background(Color(0xFFF1F5F9))
                           .clickable { onDismiss() },
                       contentAlignment = Alignment.Center
                   ) {
                       Text(
                           text = "Close",
                           color = Color(0xFF475569),
                           fontFamily = PoppinsFontFamily,
                           fontWeight = FontWeight.Bold,
                           fontSize = 12.sp
                       )
                   }

                   Row(
                       modifier = Modifier
                           .weight(0.62f)
                           .height(42.dp)
                           .clip(RoundedCornerShape(999.dp))
                           .background(Color(0xFF10B981))
                           .clickable {
                               val shareText =
                                   "Task Gullak Withdrawal Receipt\nAmount: ₹${transaction.amount}.00\nStatus: $statusTitle\nTransaction ID: #$txDisplayId\nMethod: $methodText ($upiDisplay)\nDate: $formattedDate"
                               runCatching {
                                   val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                       type = "text/plain"
                                       putExtra(Intent.EXTRA_TEXT, shareText)
                                   }
                                   context.startActivity(
                                       Intent.createChooser(sendIntent, "Share Payment Receipt")
                                   )
                               }
                           },
                       verticalAlignment = Alignment.CenterVertically,
                       horizontalArrangement = Arrangement.Center
                   ) {
                       Icon(
                           imageVector = Icons.Outlined.Share,
                           contentDescription = null,
                           tint = Color.White,
                           modifier = Modifier.size(15.dp)
                       )
                       Spacer(modifier = Modifier.width(6.dp))
                       Text(
                           text = "Download Invoice",
                           color = Color.White,
                           fontFamily = PoppinsFontFamily,
                           fontWeight = FontWeight.Bold,
                           fontSize = 11.5.sp
                       )
                   }
               }
           }
       }
   }
}

@Composable
private fun ReceiptDetailRow(
   label: String,
   value: String
) {
   Row(
       modifier = Modifier.fillMaxWidth(),
       horizontalArrangement = Arrangement.SpaceBetween,
       verticalAlignment = Alignment.CenterVertically
   ) {
       Text(
           text = label,
           color = Color(0xFF64748B),
           fontFamily = InterFontFamily,
           fontWeight = FontWeight.Medium,
           fontSize = 11.5.sp
       )
       Text(
           text = value,
           color = Color(0xFF1E293B),
           fontFamily = PoppinsFontFamily,
           fontWeight = FontWeight.Bold,
           fontSize = 11.5.sp,
           maxLines = 1,
           overflow = TextOverflow.Ellipsis
       )
   }
}

/**
* Live Countdown Timer & Deterministic Comment Selection per User:
* - Selects comment deterministically using user's UID seed % comments.size
* - Starts countdown timer live in reverse (15:00 -> 14:59) when user copies the comment
* - If timer reaches 00:00 before proof submission: rotates to the next comment & resets timer!
*/
@Composable
private fun rememberTaskTimerAndComment(
   context: Context,
   task: AvailableTask
): Triple<String, Long, () -> Unit> {
   val prefs = remember(context) {
       context.getSharedPreferences("task_timer_prefs", Context.MODE_PRIVATE)
   }

   val userId = remember {
       com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
   }
   val userSeed = remember(userId) { kotlin.math.abs(userId.hashCode()) }

   var rotationOffset by remember(task.id) {
       mutableIntStateOf(prefs.getInt("offset_${task.id}", 0))
   }
   var timerStartedAtMs by remember(task.id) {
       mutableLongStateOf(prefs.getLong("start_ms_${task.id}", 0L))
   }

   val allComments = remember(task.commentsList, task.assignedComment) {
       val list = task.commentsList.filter { it.isNotBlank() }
       if (list.isNotEmpty()) {
           list
       } else if (task.assignedComment.isNotBlank()) {
           listOf(task.assignedComment)
       } else {
           listOf(
               "Great app! Very easy to use and helpful features. Highly recommended! 👍",
               "Awesome user experience! Works smoothly without any lag. 5 stars!",
               "Super useful app with clean design and fast performance. Loved it!",
               "Exceeded my expectations. Very reliable and user friendly!"
           )
       }
   }

   val currentCommentIndex = remember(allComments, userSeed, rotationOffset) {
       if (allComments.isNotEmpty()) (userSeed + rotationOffset) % allComments.size else 0
   }
   val activeComment = remember(allComments, currentCommentIndex) {
       allComments.getOrNull(currentCommentIndex) ?: "Great app! Very easy to use."
   }

   val durationTotalSeconds = remember(task.durationMinutes) {
       task.durationMinutes.coerceAtLeast(1) * 60L
   }

   var remainingSeconds by remember(task.id, timerStartedAtMs, durationTotalSeconds) {
       mutableLongStateOf(
           if (timerStartedAtMs > 0L) {
               val elapsed = (System.currentTimeMillis() - timerStartedAtMs) / 1000L
               (durationTotalSeconds - elapsed).coerceAtLeast(0L)
           } else {
               durationTotalSeconds
           }
       )
   }

   LaunchedEffect(timerStartedAtMs, durationTotalSeconds, task.id) {
       if (timerStartedAtMs > 0L) {
           while (true) {
               val elapsed = (System.currentTimeMillis() - timerStartedAtMs) / 1000L
               val rem = (durationTotalSeconds - elapsed).coerceAtLeast(0L)
               remainingSeconds = rem

               if (rem <= 0L) {
                   val nextOffset = rotationOffset + 1
                   rotationOffset = nextOffset
                   timerStartedAtMs = 0L
                   prefs.edit()
                       .putInt("offset_${task.id}", nextOffset)
                       .putLong("start_ms_${task.id}", 0L)
                       .apply()
                   Toast.makeText(context, "Timer expired! Assigned review comment updated.", Toast.LENGTH_LONG).show()
                   break
               }
               kotlinx.coroutines.delay(1000L)
           }
       } else {
           remainingSeconds = durationTotalSeconds
       }
   }

   val startTimerOnCopy = remember(task.id, context, prefs) {
       {
           if (timerStartedAtMs == 0L) {
               val now = System.currentTimeMillis()
               timerStartedAtMs = now
               prefs.edit().putLong("start_ms_${task.id}", now).apply()
           }
       }
   }

   return Triple(activeComment, remainingSeconds, startTimerOnCopy)
}

