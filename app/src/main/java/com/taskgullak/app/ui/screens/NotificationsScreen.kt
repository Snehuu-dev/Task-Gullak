package com.taskgullak.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskgullak.app.data.model.BroadcastNotification
import com.taskgullak.app.data.model.TaskSubmission
import com.taskgullak.app.data.model.WalletTransaction
import com.taskgullak.app.data.remote.FirebaseEmailDispatchService
import com.taskgullak.app.ui.theme.InterFontFamily
import com.taskgullak.app.ui.theme.PoppinsFontFamily
import com.taskgullak.app.util.ReceivedPushNotification
import com.taskgullak.app.util.TaskGullakNotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class NotificationFilterTab(val label: String) {
    All("All"),
    Broadcasts("Broadcasts"),
    Tasks("Tasks"),
    Wallet("Wallet"),
    Announcement("Announcement")
}

private data class UnifiedNotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val badgeLabel: String,
    val badgeColor: Color,
    val badgeBgColor: Color,
    val icon: ImageVector,
    val timestampText: String,
    val sortTimestamp: Long,
    val filterTab: NotificationFilterTab
)

/**
 * Dedicated full-screen Notification Screen opened by tapping the Bell icon on the Home header
 * (or tapping a system tray notification). Supports pull-to-refresh, category filtering
 * ("Announcement", "Broadcasts", "Tasks", "Wallet"), individual notification deletion, and Clear All.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    firstName: String,
    broadcasts: List<BroadcastNotification>,
    submissions: List<TaskSubmission>,
    transactions: List<WalletTransaction>,
    onBack: () -> Unit,
    onRefresh: () -> Unit = {}
) {
    BackHandler {
        onBack()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isPullRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        TaskGullakNotificationHelper.loadPersistedPushHistory(context)
        TaskGullakNotificationHelper.loadDismissedNotificationIds(context)
        TaskGullakNotificationHelper.markAllPushNotificationsRead(context)
    }

    val receivedPushList by TaskGullakNotificationHelper.receivedPushFlow.collectAsStateWithLifecycle()
    val dismissedIds by TaskGullakNotificationHelper.dismissedIdsFlow.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf(NotificationFilterTab.All) }
    var statusBannerText by remember { mutableStateOf<String?>(null) }

    val allNotifications = remember(broadcasts, submissions, transactions, receivedPushList, dismissedIds) {
        buildUnifiedNotificationFeed(
            broadcasts = broadcasts,
            submissions = submissions,
            transactions = transactions,
            receivedPushList = receivedPushList
        ).filterNot { it.id in dismissedIds }
    }

    val filteredNotifications = remember(allNotifications, selectedFilter) {
        if (selectedFilter == NotificationFilterTab.All) {
            allNotifications
        } else {
            allNotifications.filter { it.filterTab == selectedFilter }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF7F8FF),
                        Color(0xFFF3F5FF),
                        Color(0xFFF9F7FF)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("notifications_full_screen")
    ) {
        // Subtle ambient aura backdrop matching Task Gullak theme
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0xFF9171F4).copy(alpha = 0.13f),
                        0.55f to Color(0xFF9171F4).copy(alpha = 0.04f),
                        1.0f to Color.Transparent
                    ),
                    center = Offset(w * 0.88f, h * 0.08f),
                    radius = w * 0.56f
                ),
                radius = w * 0.56f,
                center = Offset(w * 0.88f, h * 0.08f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0xFF5CCBFB).copy(alpha = 0.11f),
                        0.55f to Color(0xFF5CCBFB).copy(alpha = 0.03f),
                        1.0f to Color.Transparent
                    ),
                    center = Offset(w * 0.12f, h * 0.78f),
                    radius = w * 0.54f
                ),
                radius = w * 0.54f,
                center = Offset(w * 0.12f, h * 0.78f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Dedicated Full-Screen Top Bar with Delete All Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .padding(top = 10.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(14.dp),
                                spotColor = Color(0xFF4628BE).copy(alpha = 0.18f)
                            )
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .border(
                                width = 1.dp,
                                color = Color(0xFF745ED5).copy(alpha = 0.16f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onBack() }
                            .testTag("notifications_back_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = Color(0xFF17142E),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Notifications",
                            color = Color(0xFF17142E),
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 21.sp,
                            letterSpacing = (-0.5).sp,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                        )
                        Text(
                            text = "${allNotifications.size} active alerts for $firstName",
                            color = Color(0xFF6F6E8A),
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.5.sp
                        )
                    }
                }

                if (allNotifications.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFE4E6))
                            .border(1.dp, Color(0xFFFECDD3), RoundedCornerShape(12.dp))
                            .clickable {
                                val idsToDelete = filteredNotifications.map { it.id }
                                if (selectedFilter == NotificationFilterTab.All) {
                                    TaskGullakNotificationHelper.deleteAllNotifications(
                                        context = context,
                                        allNotificationIds = allNotifications.map { it.id }
                                    )
                                    statusBannerText = "All notifications deleted."
                                } else {
                                    idsToDelete.forEach { id ->
                                        TaskGullakNotificationHelper.deleteNotificationById(context, id)
                                    }
                                    statusBannerText = "Deleted ${idsToDelete.size} ${selectedFilter.label} notifications."
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                            .testTag("notifications_clear_all_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteSweep,
                                contentDescription = "Delete notifications",
                                tint = Color(0xFFE11D48),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Delete All",
                                color = Color(0xFFE11D48),
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                            )
                        }
                    }
                }
            }

            // 2. Category Filter Pills (All | Broadcasts | Tasks | Wallet | Announcement)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(NotificationFilterTab.entries) { tab ->
                    val isSelected = selectedFilter == tab
                    val countForTab = if (tab == NotificationFilterTab.All) {
                        allNotifications.size
                    } else {
                        allNotifications.count { it.filterTab == tab }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                if (isSelected) {
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF4F46E5), Color(0xFF7B3FE4))
                                    )
                                } else {
                                    Brush.horizontalGradient(
                                        listOf(Color.White, Color.White)
                                    )
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.Transparent else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(999.dp)
                            )
                            .clickable { selectedFilter = tab }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("notification_filter_${tab.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${tab.label} ($countForTab)",
                            color = if (isSelected) Color.White else Color(0xFF475569),
                            fontFamily = PoppinsFontFamily,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.5.sp,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                        )
                    }
                }
            }

            // Status toast banner
            AnimatedVisibility(visible = !statusBannerText.isNullOrBlank()) {
                statusBannerText?.let { msg ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 520.dp)
                            .padding(bottom = 10.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFDCFCE7))
                            .border(1.dp, Color(0xFF86EFAC), RoundedCornerShape(14.dp))
                            .clickable { statusBannerText = null }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF15803D),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = msg,
                            color = Color(0xFF14532D),
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. Full-Screen Notification List with Pull-Down Refresh
            PullToRefreshBox(
                isRefreshing = isPullRefreshing,
                onRefresh = {
                    if (!isPullRefreshing) {
                        coroutineScope.launch {
                            isPullRefreshing = true
                            TaskGullakNotificationHelper.loadPersistedPushHistory(context)
                            TaskGullakNotificationHelper.loadDismissedNotificationIds(context)
                            TaskGullakNotificationHelper.markAllPushNotificationsRead(context)
                            FirebaseEmailDispatchService.syncAllKnownLocalAccountsToFirestore(context)
                            onRefresh()
                            delay(450)
                            isPullRefreshing = false
                            statusBannerText = "Notifications refreshed!"
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 520.dp)
                    .testTag("notifications_pull_to_refresh_box")
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("notifications_list"),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (filteredNotifications.isEmpty()) {
                        item {
                            EmptyNotificationsStateCard(
                                firstName = firstName,
                                filterLabel = selectedFilter.label
                            )
                        }
                    } else {
                        items(
                            items = filteredNotifications,
                            key = { it.id }
                        ) { item ->
                            NotificationFeedCard(
                                item = item,
                                onDelete = {
                                    TaskGullakNotificationHelper.deleteNotificationById(context, item.id)
                                    statusBannerText = "Notification deleted."
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationFeedCard(
    item: UnifiedNotificationItem,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = Color(0xFF32238C).copy(alpha = 0.10f)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.96f))
            .border(1.dp, Color(0xFFEDE9FE), RoundedCornerShape(18.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(item.badgeBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = item.badgeColor,
                modifier = Modifier.size(21.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(item.badgeBgColor)
                        .padding(horizontal = 8.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        text = item.badgeLabel,
                        color = item.badgeColor,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.5.sp,
                        letterSpacing = 0.4.sp,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.timestampText,
                        color = Color(0xFF94A3B8),
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.5.sp
                    )

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFF1F2))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onDelete
                            )
                            .testTag("delete_notification_${item.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete notification",
                            tint = Color(0xFFE11D48),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Text(
                text = item.title,
                color = Color(0xFF17142E),
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = item.message,
                color = Color(0xFF64748B),
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun EmptyNotificationsStateCard(
    firstName: String,
    filterLabel: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = Color(0xFF4628BE).copy(alpha = 0.12f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.96f))
            .border(1.dp, Color(0xFFEDE9FE), RoundedCornerShape(24.dp))
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color(0xFFF3E8FF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.NotificationsNone,
                contentDescription = null,
                tint = Color(0xFF7B3FE4),
                modifier = Modifier.size(30.dp)
            )
        }

        Text(
            text = if (filterLabel == "All") "No Notifications Yet" else "No $filterLabel Notifications",
            color = Color(0xFF17142E),
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 17.sp,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Hi $firstName! Any announcements, official updates, new project alerts, task approvals, or wallet credits will appear right here. Pull down anytime to refresh.",
            color = Color(0xFF64748B),
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center
        )
    }
}

private fun buildUnifiedNotificationFeed(
    broadcasts: List<BroadcastNotification>,
    submissions: List<TaskSubmission>,
    transactions: List<WalletTransaction>,
    receivedPushList: List<ReceivedPushNotification>
): List<UnifiedNotificationItem> {
    val formatter = SimpleDateFormat("dd MMM, hh:mm a", Locale.US)
    val items = mutableListOf<UnifiedNotificationItem>()

    // 1. Received Announcements (formerly FCM Push) from NotificationReceiver
    receivedPushList.forEach { push ->
        items.add(
            UnifiedNotificationItem(
                id = "push_${push.id}",
                title = push.title,
                message = push.message,
                badgeLabel = "ANNOUNCEMENT",
                badgeColor = Color(0xFF4F46E5),
                badgeBgColor = Color(0xFFEEF2FF),
                icon = Icons.Outlined.NotificationsActive,
                timestampText = formatter.format(Date(push.timestampMillis)),
                sortTimestamp = push.timestampMillis,
                filterTab = NotificationFilterTab.Announcement
            )
        )
    }

    // 2. Live Broadcasts from Admin Panel (/broadcasts)
    broadcasts.forEach { b ->
        val ts = b.createdAt?.toDate()?.time ?: System.currentTimeMillis()
        items.add(
            UnifiedNotificationItem(
                id = "broadcast_${b.id}",
                title = b.title.ifBlank { "Task Gullak Announcement" },
                message = b.message,
                badgeLabel = "BROADCAST",
                badgeColor = Color(0xFF7B3FE4),
                badgeBgColor = Color(0xFFF3E8FF),
                icon = Icons.Outlined.Campaign,
                timestampText = if (b.createdAt != null) formatter.format(b.createdAt.toDate()) else "Live",
                sortTimestamp = ts,
                filterTab = NotificationFilterTab.Broadcasts
            )
        )
    }

    // 3. Task Submissions Status Alerts (/submissions)
    submissions.forEach { sub ->
        val statusLower = sub.status.lowercase()
        val subTime = sub.updatedAt ?: sub.createdAt
        val ts = subTime?.toDate()?.time ?: System.currentTimeMillis()
        when (statusLower) {
            "approved", "verified" -> {
                items.add(
                    UnifiedNotificationItem(
                        id = "sub_app_${sub.id}",
                        title = "Task Approved! +₹${sub.rewardAmount} Credited",
                        message = "Your submission for '${sub.taskTitle}' has been verified and approved.",
                        badgeLabel = "TASK APPROVED",
                        badgeColor = Color(0xFF059669),
                        badgeBgColor = Color(0xFFDCFCE7),
                        icon = Icons.Outlined.TaskAlt,
                        timestampText = if (subTime != null) formatter.format(subTime.toDate()) else "Recent",
                        sortTimestamp = ts,
                        filterTab = NotificationFilterTab.Tasks
                    )
                )
            }
            "rejected", "declined" -> {
                val sanitizedNote = sub.adminNote
                    .replace(Regex("admin panel", RegexOption.IGNORE_CASE), "Review Desk")
                    .replace(Regex("\\badmin\\b", RegexOption.IGNORE_CASE), "Verification Team")
                    .replace(Regex("\\bfirebase\\b", RegexOption.IGNORE_CASE), "Cloud")
                    .replace(Regex("\\bbackend\\b", RegexOption.IGNORE_CASE), "System")
                val reasonSuffix = sanitizedNote.takeIf { it.isNotBlank() }?.let { " Reason: $it" } ?: ""
                items.add(
                    UnifiedNotificationItem(
                        id = "sub_rej_${sub.id}",
                        title = "Task Submission Rejected",
                        message = "Your submission for '${sub.taskTitle}' was not approved.$reasonSuffix",
                        badgeLabel = "TASK UPDATE",
                        badgeColor = Color(0xFFE11D48),
                        badgeBgColor = Color(0xFFFFE4E6),
                        icon = Icons.Outlined.WarningAmber,
                        timestampText = if (subTime != null) formatter.format(subTime.toDate()) else "Recent",
                        sortTimestamp = ts,
                        filterTab = NotificationFilterTab.Tasks
                    )
                )
            }
        }
    }

    // 4. Wallet Transactions (/transactions)
    transactions.forEach { tx ->
        val ts = tx.createdAt?.toDate()?.time ?: System.currentTimeMillis()
        val typeLower = tx.type.lowercase()
        val isCredit = !typeLower.contains("deduct") && !typeLower.contains("debit")
        val cleanNote = tx.note
            .replace(Regex("bonus credited by admin", RegexOption.IGNORE_CASE), "Bonus received")
            .replace(Regex("credited by admin", RegexOption.IGNORE_CASE), "Received")
            .replace(Regex("adjusted by admin", RegexOption.IGNORE_CASE), "Adjustment deducted")
            .replace(Regex("deducted by admin", RegexOption.IGNORE_CASE), "Deducted")
            .replace(Regex("admin panel", RegexOption.IGNORE_CASE), "Support Desk")
            .replace(Regex("\\badmin\\b", RegexOption.IGNORE_CASE), "Support")
            .replace(Regex("\\bfirebase\\b", RegexOption.IGNORE_CASE), "Cloud")
            .replace(Regex("\\bbackend\\b", RegexOption.IGNORE_CASE), "Server")
        val bodyText = cleanNote.ifBlank {
            if (isCredit) {
                "₹${tx.amount} added to your Task Gullak wallet."
            } else {
                "₹${tx.amount} deducted from your Task Gullak wallet."
            }
        }
        items.add(
            UnifiedNotificationItem(
                id = "tx_${tx.id}",
                title = if (isCredit) "Received: +₹${tx.amount}" else "Deducted: -₹${tx.amount}",
                message = bodyText,
                badgeLabel = if (isCredit) "RECEIVED" else "DEDUCTED",
                badgeColor = if (isCredit) Color(0xFF0D9488) else Color(0xFFE11D48),
                badgeBgColor = if (isCredit) Color(0xFFCCFBF1) else Color(0xFFFFE4E6),
                icon = Icons.Outlined.AccountBalanceWallet,
                timestampText = if (tx.createdAt != null) formatter.format(tx.createdAt.toDate()) else "Recent",
                sortTimestamp = ts,
                filterTab = NotificationFilterTab.Wallet
            )
        )
    }

    return items.sortedByDescending { it.sortTimestamp }
}
