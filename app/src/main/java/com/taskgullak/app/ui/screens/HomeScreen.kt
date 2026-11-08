package com.taskgullak.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskgullak.app.data.model.AiTaskVerificationResult
import com.taskgullak.app.data.model.AvailableTask
import com.taskgullak.app.data.model.BroadcastNotification
import com.taskgullak.app.data.model.DEFAULT_AVAILABLE_TASKS
import com.taskgullak.app.data.model.MaintenanceConfig
import com.taskgullak.app.data.model.TaskSubmission
import com.taskgullak.app.data.model.UserProfile
import com.taskgullak.app.data.model.WalletTransaction
import com.taskgullak.app.ui.components.Glossy3DCheckBadge
import com.taskgullak.app.ui.components.Glossy3DTaskIcon
import com.taskgullak.app.ui.components.ShinyGradientButton
import com.taskgullak.app.ui.components.Task3DIconType
import com.taskgullak.app.ui.theme.InterFontFamily
import com.taskgullak.app.ui.theme.PoppinsFontFamily
import com.taskgullak.app.ui.theme.TgAuthError
import com.taskgullak.app.ui.theme.TgAuthPurple
import com.taskgullak.app.ui.theme.TgBluePrimary
import com.taskgullak.app.ui.theme.TgPurpleSecondary
import com.taskgullak.app.ui.theme.TgRewardGold
import com.taskgullak.app.ui.theme.TgSlateMuted
import com.taskgullak.app.ui.theme.TgSplashDarkText
import com.taskgullak.app.ui.theme.TgSuccessGreen
import com.taskgullak.app.data.remote.FirebaseEmailDispatchService
import com.taskgullak.app.service.TaskGullakFcmClient
import com.taskgullak.app.util.TaskGullakNotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

private enum class HomeBottomTab(val index: Int, val label: String) {
    Offers(0, "Offers"),
    Refer(1, "Refer"),
    Home(2, "Home"),
    Wallet(3, "Wallet"),
    Profile(4, "Profile")
}

private enum class SummaryStatKey(val label: String, val color: Color) {
    Pending("Pending Review", Color(0xFFF5A524)),
    Approved("Approved", Color(0xFF10B981)),
    Rejected("Rejected", Color(0xFFF43F5E))
}

private val HomeRiseEasing = CubicBezierEasing(0.2f, 0.75f, 0.25f, 1.0f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel?,
    fallbackName: String,
    fallbackEmail: String,
    onNavigateToTerms: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onReplayOnboarding: () -> Unit,
    onSignOut: () -> Unit,
    initialOpenNotifications: Boolean = false
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val profileUiState = viewModel?.profileState?.collectAsStateWithLifecycle()?.value
    val submissionsUiState = viewModel?.submissionsState?.collectAsStateWithLifecycle()?.value
    val activeTasksUiState = viewModel?.activeTasksState?.collectAsStateWithLifecycle()?.value
    val broadcastsUiState = viewModel?.broadcastsState?.collectAsStateWithLifecycle()?.value
    val transactionsUiState = viewModel?.transactionsState?.collectAsStateWithLifecycle()?.value
    val vmFeedbackMsg = viewModel?.submissionFeedback?.collectAsStateWithLifecycle()?.value
    val vmIsSubmitting = viewModel?.isSubmitting?.collectAsStateWithLifecycle()?.value ?: false
    val vmIsWithdrawing = viewModel?.isWithdrawing?.collectAsStateWithLifecycle()?.value ?: false
    val vmWithdrawFeedback = viewModel?.withdrawFeedback?.collectAsStateWithLifecycle()?.value
    val vmSelectedAvatarId = viewModel?.selectedAvatarId?.collectAsStateWithLifecycle()?.value.orEmpty()
    val vmNameOverride = viewModel?.nameOverride?.collectAsStateWithLifecycle()?.value
    val vmIsSavingProfile = viewModel?.isSavingProfile?.collectAsStateWithLifecycle()?.value ?: false
    val vmIsRefreshing = viewModel?.isRefreshing?.collectAsStateWithLifecycle()?.value ?: false
    val referredUsers = viewModel?.referredUsersState?.collectAsStateWithLifecycle()?.value.orEmpty()
    val referralSummary = viewModel?.referralSummaryState?.collectAsStateWithLifecycle()?.value
    val supportTickets = viewModel?.supportTicketsState?.collectAsStateWithLifecycle()?.value.orEmpty()
    val supportBotMessages = viewModel?.supportBotMessagesState?.collectAsStateWithLifecycle()?.value.orEmpty()
    val maintenanceConfig = viewModel?.maintenanceState?.collectAsStateWithLifecycle()?.value
    val receivedPushList by TaskGullakNotificationHelper.receivedPushFlow.collectAsStateWithLifecycle()

    val localSubmissions = remember { mutableStateListOf<TaskSubmission>() }
    val localTransactions = remember { mutableStateListOf<WalletTransaction>() }
    var localBalanceDeduction by remember { mutableStateOf(0L) }
    var localFeedbackMsg by remember { mutableStateOf<String?>(null) }
    var localWithdrawFeedback by remember { mutableStateOf<String?>(null) }
    var localAvatarId by remember {
        mutableStateOf(FirebaseEmailDispatchService.getSavedUserAvatarId(context, fallbackEmail))
    }
    var localNameOverride by remember { mutableStateOf<String?>(null) }
    var localIsRefreshing by remember { mutableStateOf(false) }
    val isPullRefreshing = vmIsRefreshing || localIsRefreshing

    val triggerHomeRefresh: () -> Unit = {
        if (viewModel != null) {
            viewModel.refreshData {
                FirebaseEmailDispatchService.syncAllKnownLocalAccountsToFirestore(context)
                TaskGullakNotificationHelper.loadPersistedPushHistory(context)
            }
        } else {
            coroutineScope.launch {
                localIsRefreshing = true
                FirebaseEmailDispatchService.syncAllKnownLocalAccountsToFirestore(context)
                TaskGullakNotificationHelper.loadPersistedPushHistory(context)
                delay(450)
                localIsRefreshing = false
            }
        }
    }

    var activeTab by remember { mutableStateOf(HomeBottomTab.Home) }
    var selectedTask by remember { mutableStateOf<AvailableTask?>(null) }
    var proofInput by remember { mutableStateOf("") }
    var proofError by remember { mutableStateOf<String?>(null) }
    var showNotificationsScreen by remember { mutableStateOf(initialOpenNotifications) }
    var topCardThemeIndex by remember { mutableIntStateOf(0) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* handled gracefully */ }
    )

    // Initial skeleton shimmer state + Notification channel, FCM client & permission initialization
    var initialSkeletonActive by remember { mutableStateOf(true) }
    var enterAnimTriggered by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val savedAvatar = FirebaseEmailDispatchService.getSavedUserAvatarId(context, fallbackEmail)
        if (savedAvatar.isNotBlank()) {
            localAvatarId = savedAvatar
            viewModel?.setInitialAvatarIfBlank(savedAvatar)
        }
        TaskGullakNotificationHelper.ensureNotificationChannel(context)
        TaskGullakFcmClient.initialize(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !TaskGullakNotificationHelper.hasNotificationPermission(context)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        enterAnimTriggered = true
        delay(850)
        initialSkeletonActive = false
    }

    val isRemoteLoading = (profileUiState is UiState.Loading && submissionsUiState is UiState.Loading)
    val isSkeletonLoading = initialSkeletonActive || isRemoteLoading

    val profile: UserProfile? = (profileUiState as? UiState.Success)?.data
    val remoteSubmissions: List<TaskSubmission> =
        (submissionsUiState as? UiState.Success)?.data ?: emptyList()
    val submissions: List<TaskSubmission> = if (remoteSubmissions.isNotEmpty()) {
        remoteSubmissions
    } else {
        localSubmissions
    }
    val availableTasks: List<AvailableTask> =
        (activeTasksUiState as? UiState.Success)?.data ?: DEFAULT_AVAILABLE_TASKS
    val broadcasts: List<BroadcastNotification> =
        (broadcastsUiState as? UiState.Success)?.data ?: emptyList()
    val remoteTransactions: List<WalletTransaction> =
        (transactionsUiState as? UiState.Success)?.data ?: emptyList()
    val transactions: List<WalletTransaction> = if (remoteTransactions.isNotEmpty()) {
        remoteTransactions
    } else {
        localTransactions
    }

    val feedbackMsg = vmFeedbackMsg ?: localFeedbackMsg
    val withdrawFeedbackMsg = vmWithdrawFeedback ?: localWithdrawFeedback

    val walletBalance = remember(profile?.walletBalance, transactions, submissions, localBalanceDeduction) {
        val baseBalance = if (profile != null && profile.walletBalance > 0L) {
            profile.walletBalance
        } else {
            val subRewards = submissions.filter { it.status == "verified" || it.status == "approved" }.sumOf { it.rewardAmount }
            val txCredits = transactions.filter {
                val t = it.type.lowercase()
                t == "referral_bonus" || t == "referral_commission" || t == "admin_credit"
            }.sumOf { it.amount }
            val raw = subRewards + txCredits
            if (raw > 0L) raw else (profile?.walletBalance ?: 0L)
        }
        (baseBalance - localBalanceDeduction).coerceAtLeast(0L)
    }

    val displayName = remember(localNameOverride, vmNameOverride, profile?.fullName, fallbackName) {
        (
            localNameOverride?.takeIf { it.isNotBlank() }
                ?: vmNameOverride?.takeIf { it.isNotBlank() }
                ?: profile?.fullName?.takeIf { it.isNotBlank() }
                ?: fallbackName
        )
            .trim()
            .ifBlank { "Divyanshu" }
    }
    val firstName = remember(displayName) { displayName.substringBefore(" ").ifBlank { "Divyanshu" } }
    val userInitial = remember(firstName) { firstName.firstOrNull()?.uppercaseChar()?.toString() ?: "D" }
    val displayEmail = remember(profile?.email, fallbackEmail) {
        (profile?.email?.takeIf { it.isNotBlank() } ?: fallbackEmail)
            .trim()
            .ifBlank { "member@taskgullak.app" }
    }

    val currentAvatarId = remember(vmSelectedAvatarId, localAvatarId, profile?.avatarId) {
        vmSelectedAvatarId.takeIf { it.isNotBlank() }
            ?: localAvatarId.takeIf { it.isNotBlank() }
            ?: profile?.avatarId?.takeIf { it.isNotBlank() }
            ?: ""
    }

    LaunchedEffect(profile?.avatarId) {
        val remoteAvatar = profile?.avatarId?.trim().orEmpty()
        if (remoteAvatar.isNotBlank() && localAvatarId.isBlank()) {
            localAvatarId = remoteAvatar
            FirebaseEmailDispatchService.saveUserAvatarId(context, remoteAvatar, displayEmail)
        }
    }

    // Real-time system notification listeners for Admin Panel actions (shared with background service)
    LaunchedEffect(activeTasksUiState) {
        val tasksList = (activeTasksUiState as? UiState.Success)?.data ?: return@LaunchedEffect
        TaskGullakNotificationHelper.processTasksSnapshot(context, tasksList)
    }

    LaunchedEffect(broadcastsUiState) {
        val list = (broadcastsUiState as? UiState.Success)?.data ?: return@LaunchedEffect
        TaskGullakNotificationHelper.processBroadcastsSnapshot(context, list)
    }

    LaunchedEffect(submissionsUiState) {
        val list = (submissionsUiState as? UiState.Success)?.data ?: return@LaunchedEffect
        TaskGullakNotificationHelper.processSubmissionsSnapshot(context, list)
    }

    LaunchedEffect(transactionsUiState) {
        val list = (transactionsUiState as? UiState.Success)?.data ?: return@LaunchedEffect
        TaskGullakNotificationHelper.processTransactionsSnapshot(context, list)
    }

    LaunchedEffect(profile?.walletBalance, profile?.isBanned) {
        val currentProfile = profile ?: return@LaunchedEffect
        TaskGullakNotificationHelper.processUserProfileSnapshot(context, currentProfile)
    }

    LaunchedEffect(profile?.userId, profile?.isDeleted, profile?.accountStatus, profile?.isBanned) {
        val currentProfile = profile ?: return@LaunchedEffect
        if (currentProfile.userId.isBlank()) return@LaunchedEffect
        if (!currentProfile.isBanned) return@LaunchedEffect
        if (currentProfile.isDeleted || currentProfile.accountStatus.equals("deleted", ignoreCase = true)) {
            onSignOut()
        }
    }

    val walletIdCode = remember(displayEmail) {
        val seed = displayEmail.uppercase()
        val hash = seed.hashCode().toUInt().toString(36).uppercase().padStart(8, 'Z')
        "TG-${hash.take(8)}"
    }

    val pendingCount = remember(submissions) {
        submissions.count {
            val s = it.status.lowercase()
            s == "pending" || s == "submitted" || s == "under_review"
        }
    }
    val approvedCount = remember(submissions) {
        submissions.count {
            val s = it.status.lowercase()
            s == "verified" || s == "approved"
        }
    }
    val rejectedCount = remember(submissions) {
        submissions.count {
            val s = it.status.lowercase()
            s == "rejected" || s == "declined"
        }
    }
    val totalTasks = remember(pendingCount, approvedCount, rejectedCount) {
        pendingCount + approvedCount + rejectedCount
    }

    val totalWithdrawn = remember(transactions) {
        transactions
            .filter { it.type.equals("withdrawal", ignoreCase = true) }
            .sumOf { it.amount }
    }

    val pendingPct = if (totalTasks > 0) ((pendingCount * 100f) / totalTasks).roundToInt() else 0
    val approvedPct = if (totalTasks > 0) ((approvedCount * 100f) / totalTasks).roundToInt() else 0
    val rejectedPct = if (totalTasks > 0) ((rejectedCount * 100f) / totalTasks).roundToInt() else 0

    // Infinite shimmer & ambient 3D animations matching home.css
    val infiniteTransition = rememberInfiniteTransition(label = "home_infinite")
    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = -1.2f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sk_shimmer"
    )
    val ringSpinDegrees by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_spin"
    )
    val walletGlowScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wallet_glow_scale"
    )
    val walletGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wallet_glow_alpha"
    )
    val rupeeFloatOffsetY by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rupee_float"
    )
    val auroraShiftX by infiniteTransition.animateFloat(
        initialValue = -14f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aurora_shift"
    )
    val navSheenFraction by infiniteTransition.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "nav_sheen"
    )

    // Staggered rise animations matching body.tg-home-enter #home .rise
    val headRise by animateFloatAsState(
        targetValue = if (enterAnimTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = 0, easing = HomeRiseEasing),
        label = "head_rise"
    )
    val walletRise by animateFloatAsState(
        targetValue = if (enterAnimTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = 45, easing = HomeRiseEasing),
        label = "wallet_rise"
    )
    val summaryRise by animateFloatAsState(
        targetValue = if (enterAnimTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = 90, easing = HomeRiseEasing),
        label = "summary_rise"
    )

    val density = LocalDensity.current.density

    val isUnderMaintenance =
        maintenanceConfig?.isMaintenanceActive == true || profile?.maintenanceLocked == true
    if (isUnderMaintenance) {
        val customMsg = maintenanceConfig?.message?.takeIf { it.isNotBlank() }
            ?: "We are currently upgrading the app to bring you a better experience.\nWe'll be back shortly! Thank you for your patience."
        MaintenanceModeFullScreen(message = customMsg)
        return
    }

    if (showNotificationsScreen) {
        NotificationsScreen(
            firstName = firstName,
            broadcasts = broadcasts,
            submissions = submissions,
            transactions = transactions,
            onBack = { showNotificationsScreen = false },
            onRefresh = triggerHomeRefresh
        )
        return
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
            .testTag("home_screen")
    ) {
        // Seamless Background Aura Blobs (.aura i in home.html) drawn with unclipped radial gradients
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0xFF9171F4).copy(alpha = 0.14f),
                        0.50f to Color(0xFF9171F4).copy(alpha = 0.05f),
                        1.0f to Color.Transparent
                    ),
                    center = Offset(w * 0.90f, h * 0.06f),
                    radius = w * 0.58f
                ),
                radius = w * 0.58f,
                center = Offset(w * 0.90f, h * 0.06f)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0xFFFF4F8B).copy(alpha = 0.09f),
                        0.50f to Color(0xFFFF4F8B).copy(alpha = 0.03f),
                        1.0f to Color.Transparent
                    ),
                    center = Offset(w * 0.05f, h * 0.45f),
                    radius = w * 0.54f
                ),
                radius = w * 0.54f,
                center = Offset(w * 0.05f, h * 0.45f)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0xFF5CCBFB).copy(alpha = 0.12f),
                        0.50f to Color(0xFF5CCBFB).copy(alpha = 0.04f),
                        1.0f to Color.Transparent
                    ),
                    center = Offset(w * 0.88f, h * 0.84f),
                    radius = w * 0.55f
                ),
                radius = w * 0.55f,
                center = Offset(w * 0.88f, h * 0.84f)
            )
        }

        PullToRefreshBox(
            isRefreshing = isPullRefreshing,
            onRefresh = triggerHomeRefresh,
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_pull_to_refresh_box")
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 108.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
            // 1. TOP APP BAR (.top: Brand Logo + "Task Gullak" text rendered ONLY on the Home tab)
            if (activeTab == HomeBottomTab.Home) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 520.dp)
                            .graphicsLayer {
                                alpha = headRise
                                translationY = (1f - headRise) * 13f * density
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // .logo (36x36dp, border-radius 11dp, linear-gradient(135deg, indigo, violet 55%, pink))
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .shadow(
                                        elevation = 8.dp,
                                        shape = RoundedCornerShape(11.dp),
                                        spotColor = Color(0xFF5B34D6).copy(alpha = 0.35f)
                                    )
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF4F46E5),
                                                Color(0xFF7B3FE4),
                                                Color(0xFFFF4F8B)
                                            )
                                        )
                                    )
                                    .border(
                                        width = 0.8.dp,
                                        color = Color.White.copy(alpha = 0.38f),
                                        shape = RoundedCornerShape(11.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "₹",
                                    color = Color.White,
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                )
                            }

                            Text(
                                text = "Task Gullak",
                                color = Color(0xFF17142E),
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                letterSpacing = (-0.5).sp
                            )
                        }

                        // Notification Bell Button (.notification-btn) — opens dedicated full-screen NotificationsScreen
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(15.dp),
                                    spotColor = Color(0xFF4628BE).copy(alpha = 0.20f)
                                )
                                .clip(RoundedCornerShape(15.dp))
                                .background(Color.White.copy(alpha = 0.90f))
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFF745ED5).copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(15.dp)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { showNotificationsScreen = true }
                                .testTag("home_notification_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsNone,
                                contentDescription = "Open notifications",
                                tint = Color(0xFF51439A),
                                modifier = Modifier.size(21.dp)
                            )
                            if (broadcasts.isNotEmpty() || receivedPushList.any { !it.isRead }) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = (-9).dp, y = 9.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF4F8B))
                                )
                            }
                        }
                    }
                }
            }

            if (profile?.isBanned == true) {
                item {
                    AccountBannedOverlayCard(
                        userName = displayName,
                        userEmail = displayEmail,
                        banReason = profile.banReason,
                        onSignOut = onSignOut
                    )
                }
            } else {
                // MAIN TAB CONTENT
                when (activeTab) {
                    HomeBottomTab.Home -> {
                    // 2. WELCOME CARD (.welcome-card in home.html / home.css)
                    item {
                        val cardGradientColors = when (topCardThemeIndex % 3) {
                            0 -> listOf(
                                Color.White.copy(alpha = 0.78f),
                                Color(0xFFF8F4FF).copy(alpha = 0.68f),
                                Color(0xFFF0E8FF).copy(alpha = 0.62f)
                            )
                            1 -> listOf(
                                Color(0xFFFFF7ED).copy(alpha = 0.82f),
                                Color(0xFFFFF1F2).copy(alpha = 0.72f),
                                Color(0xFFF3E8FF).copy(alpha = 0.66f)
                            )
                            else -> listOf(
                                Color(0xFFF0F9FF).copy(alpha = 0.82f),
                                Color(0xFFEDE9FE).copy(alpha = 0.70f),
                                Color(0xFFFCE7F3).copy(alpha = 0.65f)
                            )
                        }
                        val eyebrowText = when (topCardThemeIndex % 3) {
                            0 -> "YOUR SAVINGS JOURNEY"
                            1 -> "DAILY REWARDS & EARNINGS"
                            else -> "SMART GULLAK GOALS"
                        }
                        val headlineHighlight = when (topCardThemeIndex % 3) {
                            0 -> "Let's grow your Gullak"
                            1 -> "Boost your Gullak"
                            else -> "Keep earning big"
                        }

                        val welcomeShape = RoundedCornerShape(
                            topStart = 22.dp,
                            topEnd = 22.dp,
                            bottomEnd = 22.dp,
                            bottomStart = 9.dp
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 520.dp)
                                .padding(top = 2.dp, bottom = 18.dp)
                                .graphicsLayer {
                                    alpha = headRise
                                    translationY = (1f - headRise) * 10f * density
                                }
                        ) {
                            // Matte Frosted Glass Welcome Card with radial mesh accents
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(
                                        elevation = 12.dp,
                                        shape = welcomeShape,
                                        ambientColor = Color(0xFF4628BE).copy(alpha = 0.12f),
                                        spotColor = Color(0xFF4628BE).copy(alpha = 0.20f)
                                    )
                                    .clip(welcomeShape)
                                    .background(Brush.linearGradient(colors = cardGradientColors))
                                    .drawBehind {
                                        // Radial mesh 1: top-right violet glow
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    Color(0xFF9171F4).copy(alpha = 0.30f),
                                                    Color.Transparent
                                                ),
                                                center = Offset(size.width * 0.95f, 0f),
                                                radius = size.width * 0.65f
                                            ),
                                            center = Offset(size.width * 0.95f, 0f),
                                            radius = size.width * 0.65f
                                        )
                                        // Radial mesh 2: bottom-left pink glow
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    Color(0xFFFF4F8B).copy(alpha = 0.16f),
                                                    Color.Transparent
                                                ),
                                                center = Offset(0f, size.height),
                                                radius = size.width * 0.55f
                                            ),
                                            center = Offset(0f, size.height),
                                            radius = size.width * 0.55f
                                        )
                                        // Radial mesh 3: bottom-center sky glow
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    Color(0xFF5CCBFB).copy(alpha = 0.20f),
                                                    Color.Transparent
                                                ),
                                                center = Offset(size.width * 0.55f, size.height * 1.1f),
                                                radius = size.width * 0.50f
                                            ),
                                            center = Offset(size.width * 0.55f, size.height * 1.1f),
                                            radius = size.width * 0.50f
                                        )
                                    }
                                    .border(
                                        width = 1.2.dp,
                                        color = Color.White.copy(alpha = 0.88f),
                                        shape = welcomeShape
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { topCardThemeIndex = (topCardThemeIndex + 1) % 3 }
                                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 20.dp)
                                    .testTag("home_top_greeting_card")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(end = 52.dp)
                                ) {
                                    // .welcome-eyebrow
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(14.dp)
                                                .height(2.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(Color(0xFF4F46E5), Color(0xFFD926A9))
                                                    )
                                                )
                                        )
                                        Text(
                                            text = eyebrowText,
                                            color = Color(0xFF8066CB),
                                            fontFamily = PoppinsFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 10.sp,
                                            letterSpacing = 1.15.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Heading: .grad + .user-name
                                    Text(
                                        text = buildAnnotatedString {
                                            withStyle(
                                                SpanStyle(
                                                    brush = Brush.horizontalGradient(
                                                        colors = listOf(
                                                            Color(0xFF4F46E5),
                                                            Color(0xFF7B3FE4),
                                                            Color(0xFFD926A9)
                                                        )
                                                    ),
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            ) {
                                                append(headlineHighlight)
                                            }
                                            if (!isSkeletonLoading) {
                                                withStyle(
                                                    SpanStyle(
                                                        color = Color(0xFF17142E),
                                                        fontWeight = FontWeight.ExtraBold
                                                    )
                                                ) {
                                                    append(", $firstName!")
                                                }
                                            }
                                        },
                                        fontFamily = PoppinsFontFamily,
                                        fontSize = 21.sp,
                                        lineHeight = 26.sp,
                                        letterSpacing = (-0.5).sp
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    if (isSkeletonLoading) {
                                        // Shimmer lines while loading
                                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                            ShimmerBlock(
                                                shimmerProgress = shimmerProgress,
                                                modifier = Modifier
                                                    .fillMaxWidth(0.86f)
                                                    .height(10.dp)
                                            )
                                            ShimmerBlock(
                                                shimmerProgress = shimmerProgress,
                                                modifier = Modifier
                                                    .fillMaxWidth(0.55f)
                                                    .height(10.dp)
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "$firstName, every task adds to your savings.",
                                            color = Color(0xFF6F6E8A),
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 12.sp,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }

                            // Overlapping Profile Circle (.header-profile at right: 18dp, bottom: -24dp)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = (-18).dp, y = 24.dp)
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.38f))
                                    .padding(3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .shadow(
                                            elevation = 10.dp,
                                            shape = CircleShape,
                                            spotColor = Color(0xFF4628BE).copy(alpha = 0.35f)
                                        )
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .padding(3.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { activeTab = HomeBottomTab.Profile },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSkeletonLoading) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.linearGradient(
                                                        colors = listOf(
                                                            Color(0xFF5B5CF0),
                                                            Color(0xFF9B4FE0),
                                                            Color(0xFFFF4F8B)
                                                        )
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.PersonOutline,
                                                contentDescription = "Profile",
                                                tint = Color(0xFF2E1065).copy(alpha = 0.75f),
                                                modifier = Modifier.size(25.dp)
                                            )
                                        }
                                    } else {
                                        UserAvatar3DBadge(
                                            avatarId = currentAvatarId,
                                            userInitial = userInitial,
                                            size = 48.dp,
                                            testTag = "home_greeting_avatar_badge"
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. TASK GULLAK WALLET CARD (.wallet in home.html / home.css)
                    item {
                        TaskGullakWalletCard(
                            walletBalance = walletBalance,
                            walletIdCode = walletIdCode,
                            isLoading = isSkeletonLoading,
                            shimmerProgress = shimmerProgress,
                            walletGlowScale = walletGlowScale,
                            walletGlowAlpha = walletGlowAlpha,
                            rupeeFloatOffsetY = rupeeFloatOffsetY,
                            auroraShiftX = auroraShiftX,
                            onViewWallet = { activeTab = HomeBottomTab.Wallet },
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 520.dp)
                                .padding(top = 6.dp)
                                .graphicsLayer {
                                    alpha = walletRise
                                    translationY = (1f - walletRise) * 13f * density
                                }
                        )
                    }

                    // 4. TASK SUMMARY SECTION (.summary in home.html / home.css — interactive donut & metric rows)
                    item {
                        TaskSummarySection(
                            pendingCount = pendingCount,
                            approvedCount = approvedCount,
                            rejectedCount = rejectedCount,
                            totalTasks = totalTasks,
                            pendingPct = pendingPct,
                            approvedPct = approvedPct,
                            rejectedPct = rejectedPct,
                            isLoading = isSkeletonLoading,
                            shimmerProgress = shimmerProgress,
                            ringSpinDegrees = ringSpinDegrees,
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 520.dp)
                                .padding(top = 12.dp)
                                .graphicsLayer {
                                    alpha = summaryRise
                                    translationY = (1f - summaryRise) * 13f * density
                                }
                        )
                    }
                }

                HomeBottomTab.Offers -> {
                    item {
                        OffersTabShellContent(
                            availableTasks = availableTasks,
                            submissions = submissions,
                            pendingCount = pendingCount,
                            approvedCount = approvedCount,
                            rejectedCount = rejectedCount,
                            walletBalance = walletBalance,
                            isSubmitting = vmIsSubmitting,
                            feedbackMessage = feedbackMsg,
                            onClearFeedback = {
                                viewModel?.clearFeedback()
                                localFeedbackMsg = null
                            },
                            onSubmitTaskProof = { task, message, screenshotBase64, onComplete ->
                                FirebaseEmailDispatchService.creditReferrerLifetimeBonusForTask(
                                    context = context,
                                    taskRewardCoins = (task.rewardInr * 100).coerceAtLeast(100),
                                    taskTitle = task.title
                                )
                                if (viewModel != null) {
                                    viewModel.submitProof(
                                        taskId = task.id,
                                        taskTitle = task.title,
                                        category = task.category,
                                        rewardAmount = task.rewardInr.toLong(),
                                        proofText = message,
                                        screenshotBase64 = screenshotBase64,
                                        userName = displayName,
                                        userEmail = displayEmail,
                                        assignedComment = task.assignedComment,
                                        appName = task.appName.ifBlank { task.title },
                                        referenceScreenshotUrl = task.referenceScreenshotUrl,
                                        onComplete = onComplete
                                    )
                                } else {
                                    val fallbackResult = AiTaskVerificationResult(
                                        completionPercentage = 100,
                                        matchedTaskName = task.appName.ifBlank { task.title },
                                        status = "approved",
                                        originalRewardInr = task.rewardInr.toLong(),
                                        creditedRewardInr = task.rewardInr.toLong(),
                                        aiVerdict = "TIER_96_100_FULL_CREDIT",
                                        adminNote = "Verification Complete (100% Match): Task requirements met.",
                                        aiSummary = "Verification Complete (100% Match): Task requirements met."
                                    )
                                    localSubmissions.add(
                                        0,
                                        TaskSubmission(
                                            id = "local_${System.currentTimeMillis()}",
                                            userId = "local_user",
                                            userName = displayName,
                                            userEmail = displayEmail,
                                            taskId = task.id,
                                            taskTitle = task.title,
                                            category = task.category,
                                            proofText = message,
                                            screenshotBase64 = screenshotBase64,
                                            referenceScreenshotUrl = task.referenceScreenshotUrl,
                                            rewardAmount = task.rewardInr.toLong(),
                                            originalRewardAmount = task.rewardInr.toLong(),
                                            status = "approved",
                                            adminNote = fallbackResult.adminNote,
                                            aiVerified = true,
                                            aiCompletionPercent = 100,
                                            aiVerdict = fallbackResult.aiVerdict,
                                            aiSummary = fallbackResult.aiSummary,
                                            matchedTaskName = fallbackResult.matchedTaskName
                                        )
                                    )
                                    localFeedbackMsg = "Verification Complete (100% Match): +₹${task.rewardInr} credited to your Gullak."
                                    onComplete(fallbackResult)
                                }
                            }
                        )
                    }
                }

                HomeBottomTab.Refer -> {
                    item {
                        ReferTabShellContent(
                            walletIdCode = walletIdCode,
                            referredUsers = referredUsers,
                            liveReferralSummary = referralSummary
                        )
                    }
                }

                HomeBottomTab.Wallet -> {
                    item {
                        val savedUpi = profile?.upiId?.takeIf { it.isNotBlank() }
                            ?: FirebaseEmailDispatchService.getSavedUpiId(context, displayEmail)
                        WalletTabShellContent(
                            walletCardSlot = {
                                TaskGullakWalletCard(
                                    walletBalance = walletBalance,
                                    walletIdCode = walletIdCode,
                                    isLoading = isSkeletonLoading,
                                    shimmerProgress = shimmerProgress,
                                    walletGlowScale = walletGlowScale,
                                    walletGlowAlpha = walletGlowAlpha,
                                    rupeeFloatOffsetY = rupeeFloatOffsetY,
                                    auroraShiftX = auroraShiftX,
                                    onViewWallet = {},
                                    actionButtonText = "View Wallet",
                                    isWithdrawAction = false,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            walletBalance = walletBalance,
                            pendingCount = pendingCount,
                            submissions = submissions,
                            transactions = transactions,
                            isWithdrawing = vmIsWithdrawing,
                            withdrawFeedback = withdrawFeedbackMsg,
                            savedUpiId = savedUpi,
                            precalculatedTotalWithdrawn = totalWithdrawn,
                            onSaveUpiId = { newUpi: String ->
                                FirebaseEmailDispatchService.saveUpiId(context, newUpi, displayEmail)
                                viewModel?.updateUpiId(newUpi)
                            },
                            onClearWithdrawFeedback = {
                                viewModel?.clearWithdrawFeedback()
                                localWithdrawFeedback = null
                            },
                            onRequestWithdrawal = { amount: Long, payoutMethod: String, payoutDetails: String ->
                                if (viewModel != null) {
                                    viewModel.requestWithdrawal(
                                        amount = amount,
                                        payoutMethod = payoutMethod,
                                        payoutDetails = payoutDetails
                                    )
                                } else {
                                    localBalanceDeduction += amount
                                    localTransactions.add(
                                        0,
                                        WalletTransaction(
                                            id = "tx_${System.currentTimeMillis()}",
                                            userId = "local_user",
                                            amount = amount,
                                            type = "withdrawal",
                                            note = "Withdrawal via $payoutMethod ($payoutDetails) (-₹$amount)"
                                        )
                                    )
                                    localWithdrawFeedback =
                                        "Withdrawal of ₹$amount via $payoutMethod ($payoutDetails) submitted!"
                                }
                            }
                        )
                    }
                }

                HomeBottomTab.Profile -> {
                    item {
                        ProfileTabShellContent(
                            userInitial = userInitial,
                            displayName = displayName,
                            displayEmail = displayEmail,
                            currentAvatarId = currentAvatarId,
                            walletIdCode = walletIdCode,
                            totalTasks = totalTasks,
                            approvedCount = approvedCount,
                            walletBalance = walletBalance,
                            isSavingName = vmIsSavingProfile,
                            remoteBio = profile?.bio.orEmpty(),
                            remoteUsername = profile?.username.orEmpty(),
                            remoteBannerId = profile?.bannerId.orEmpty(),
                            referredByCode = profile?.referredByCode.orEmpty(),
                            bulkStatus = profile?.bulkStatus.orEmpty().ifBlank { "none" },
                            bulkDeviceCount = profile?.bulkDeviceCount ?: 0,
                            supportBotMessages = supportBotMessages,
                            onSubmitBulkModRequest = { reason: String, count: Int ->
                                viewModel?.submitBulkModRequest(reason, count)
                            },
                            onSendSupportBotMessage = { text: String -> viewModel?.sendSupportBotMessage(text) },
                            onConfirmSupportBotAction = { actionType: String, actionData: String ->
                                viewModel?.executeSupportBotAction(actionType, actionData, context)
                            },
                            onSelectAvatar = { chosenId: String ->
                                localAvatarId = chosenId
                                FirebaseEmailDispatchService.saveUserAvatarId(
                                    context = context,
                                    avatarId = chosenId,
                                    email = displayEmail
                                )
                                viewModel?.updateAvatar(
                                    avatarId = chosenId,
                                    onPersistLocally = { id ->
                                        FirebaseEmailDispatchService.saveUserAvatarId(
                                            context = context,
                                            avatarId = id,
                                            email = displayEmail
                                        )
                                    }
                                )
                            },
                            onSaveBioAndCustomization = { bannerIdVal: String, usernameVal: String, bioVal: String ->
                                viewModel?.updateBioAndCustomization(
                                    bannerId = bannerIdVal,
                                    username = usernameVal,
                                    bio = bioVal
                                )
                            },
                            onSaveDisplayName = { newName: String, onResult: (Boolean, String) -> Unit ->
                                val cleaned = newName.trim()
                                localNameOverride = cleaned
                                FirebaseEmailDispatchService.saveUpdatedUserName(
                                    context = context,
                                    fullName = cleaned,
                                    email = displayEmail
                                )
                                if (viewModel != null) {
                                    viewModel.updateDisplayName(
                                        newName = cleaned,
                                        onPersistLocally = { saved ->
                                            FirebaseEmailDispatchService.saveUpdatedUserName(
                                                context = context,
                                                fullName = saved,
                                                email = displayEmail
                                            )
                                        },
                                        onComplete = onResult
                                    )
                                } else {
                                    onResult(true, "Your display name has been updated to $cleaned.")
                                }
                            },
                            supportTickets = supportTickets,
                            onSubmitSupportTicket = { msg: String, onComplete: (Boolean) -> Unit ->
                                if (viewModel != null) {
                                    viewModel.submitSupportTicket(msg, onComplete)
                                } else {
                                    onComplete(false)
                                }
                            },
                            onNavigateToWallet = { activeTab = HomeBottomTab.Wallet },
                            onNavigateToOffers = { activeTab = HomeBottomTab.Offers },
                            onNavigateToRefer = { activeTab = HomeBottomTab.Refer },
                            onNavigateToTerms = onNavigateToTerms,
                            onSignOut = onSignOut
                        )
                    }
                }
            }
            }
            }
        }

        // 5. ANIMATED SKY-BLUE BOTTOM NAVIGATION BAR (Exact match to home.html + navigation.js with sliding curved notch & orb)
        TaskGullakBottomNavBar(
            activeTab = activeTab,
            sheenFraction = navSheenFraction,
            onSelectTab = { activeTab = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

/**
 * Exact 100% replica of #home .wallet (.wglow + .wl--pink + .wl--magenta + .wcard + SVG artwork + .wtop/.wmid/.wbottom)
 */
@Composable
private fun TaskGullakWalletCard(
    walletBalance: Long,
    walletIdCode: String,
    isLoading: Boolean,
    shimmerProgress: Float,
    walletGlowScale: Float,
    walletGlowAlpha: Float,
    rupeeFloatOffsetY: Float,
    auroraShiftX: Float,
    onViewWallet: () -> Unit,
    actionButtonText: String = "View Wallet",
    isWithdrawAction: Boolean = false,
    modifier: Modifier = Modifier
) {
    val formattedBalance = remember(walletBalance) {
        String.format(Locale.US, "%d", walletBalance)
    }

    Box(
        modifier = modifier.height(174.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        // 1. .wglow (breathing radial purple glow behind wallet — unclipped multi-stop radial gradient)
        Canvas(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.96f)
                .height(160.dp)
                .offset(y = 10.dp)
                .graphicsLayer {
                    scaleX = walletGlowScale
                    scaleY = walletGlowScale
                    alpha = walletGlowAlpha
                }
        ) {
            val r = size.width * 0.48f
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0xFF7B3FE4).copy(alpha = 0.28f),
                        0.50f to Color(0xFF7B3FE4).copy(alpha = 0.11f),
                        0.82f to Color(0xFF7B3FE4).copy(alpha = 0.02f),
                        1.0f to Color.Transparent
                    ),
                    center = center,
                    radius = r
                ),
                radius = r,
                center = center
            )
        }

        // 2. .wl--pink (tilted +2.2deg tucked cleanly under the bottom-right of .wcard)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .height(158.dp)
                .offset(x = 5.dp, y = 11.dp)
                .rotate(2.2f)
                .clip(
                    RoundedCornerShape(
                        topStart = 38.dp,
                        topEnd = 34.dp,
                        bottomEnd = 36.dp,
                        bottomStart = 30.dp
                    )
                )
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFF4F8B).copy(alpha = 0.72f),
                            Color(0xFFFF7A59).copy(alpha = 0.72f)
                        )
                    )
                )
        )

        // 3. .wl--magenta (tilted -2.2deg tucked cleanly under the bottom-left of .wcard)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .height(158.dp)
                .offset(x = (-5).dp, y = 11.dp)
                .rotate(-2.2f)
                .clip(
                    RoundedCornerShape(
                        topStart = 34.dp,
                        topEnd = 38.dp,
                        bottomEnd = 30.dp,
                        bottomStart = 36.dp
                    )
                )
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFD926A9).copy(alpha = 0.80f),
                            Color(0xFFFF4F8B).copy(alpha = 0.80f)
                        )
                    )
                )
        )

        // 4. .wcard (Main Wallet Card — 164.dp tall so bottom layers peek out cleanly underneath)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(164.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(23.dp),
                    spotColor = Color(0xFF502DC8).copy(alpha = 0.58f)
                )
                .clip(RoundedCornerShape(23.dp))
                .background(
                    Brush.linearGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xFF3446E0),
                            0.40f to Color(0xFF5B3FE6),
                            0.70f to Color(0xFF7B3FE4),
                            1.0f to Color(0xFFA53FD6)
                        ),
                        start = Offset.Zero,
                        end = Offset(1000f, 550f)
                    )
                )
                .drawBehind {
                    val w = size.width
                    val h = size.height

                    // .aur.a1: Sky radial glow at top-right (cx=292/340, cy=0)
                    val a1Center = Offset(w * 0.86f + auroraShiftX, 0f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF5CCBFB).copy(alpha = 0.55f),
                                Color.Transparent
                            ),
                            center = a1Center,
                            radius = w * 0.36f
                        ),
                        center = a1Center,
                        radius = w * 0.36f
                    )

                    // .aur.a2: Pink radial glow at bottom-left (cx=26/340, cy=196/190)
                    val a2Center = Offset(w * 0.08f - auroraShiftX, h * 1.02f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF4F8B).copy(alpha = 0.55f),
                                Color.Transparent
                            ),
                            center = a2Center,
                            radius = w * 0.38f
                        ),
                        center = a2Center,
                        radius = w * 0.38f
                    )

                    // .aur.a3: Violet radial glow at center (cx=180/340, cy=100/190)
                    val a3Center = Offset(w * 0.53f + auroraShiftX * 0.5f, h * 0.52f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFB58CFF).copy(alpha = 0.48f),
                                Color.Transparent
                            ),
                            center = a3Center,
                            radius = w * 0.30f
                        ),
                        center = a3Center,
                        radius = w * 0.30f
                    )

                    // Diagonal glossy polygon (M-30 0 L190 0 L80 190 L-140 190Z)
                    val glossPath = Path().apply {
                        moveTo(-w * 0.09f, 0f)
                        lineTo(w * 0.56f, 0f)
                        lineTo(w * 0.24f, h)
                        lineTo(-w * 0.41f, h)
                        close()
                    }
                    drawPath(
                        path = glossPath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            startX = 0f,
                            endX = w * 0.56f
                        )
                    )

                    // Top-right soft circle (cx=302/340, cy=38/190, r=46)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.06f),
                        center = Offset(w * 0.89f, h * 0.20f),
                        radius = w * 0.135f
                    )
                }
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.58f),
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.26f)
                        )
                    ),
                    shape = RoundedCornerShape(23.dp)
                )
                .padding(horizontal = 15.dp, vertical = 12.dp)
        ) {
            // Floating ₹ Watermark (.rupee at x=268/340, y=140/190 — non-intrusive background glyph)
            Text(
                text = "₹",
                style = TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.20f),
                            Color.White.copy(alpha = 0.02f)
                        )
                    ),
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 104.sp,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = (-16).dp, y = (2 + rupeeFloatOffsetY).dp)
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // .wtop: TASK GULLAK WALLET + .wicon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TASK GULLAK WALLET",
                        color = Color.White.copy(alpha = 0.86f),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 9.5.sp,
                        letterSpacing = 1.1.sp
                    )

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.16f))
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onViewWallet() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountBalanceWallet,
                            contentDescription = "Wallet",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                // .wmid: Available Balance + Amount + Trend Pill
                Column {
                    Text(
                        text = "Available Balance",
                        color = Color.White.copy(alpha = 0.86f),
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    color = Color.White.copy(alpha = 0.96f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 23.sp
                                )
                            ) {
                                append("₹")
                            }
                            withStyle(
                                SpanStyle(
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 28.sp,
                                    letterSpacing = (-0.8).sp
                                )
                            ) {
                                append(if (isLoading) "—" else formattedBalance)
                            }
                            withStyle(
                                SpanStyle(
                                    color = Color.White.copy(alpha = 0.80f),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.5.sp
                                )
                            ) {
                                append(if (isLoading) ".––" else ".00")
                            }
                        },
                        fontFamily = PoppinsFontFamily,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            lineHeight = 30.sp
                        ),
                        modifier = Modifier.testTag("wallet_balance_text")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // .trend pill with custom SVG vector arrow (M7 17 17 7 / M8 7h9v9)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(Color.White.copy(alpha = 0.14f))
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.20f),
                                shape = RoundedCornerShape(99.dp)
                            )
                            .padding(start = 7.dp, end = 9.dp, top = 3.dp, bottom = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Canvas(modifier = Modifier.size(11.dp)) {
                            val w = size.width
                            val h = size.height
                            val arrowPath = Path().apply {
                                moveTo(w * 0.29f, h * 0.71f)
                                lineTo(w * 0.71f, h * 0.29f)
                                moveTo(w * 0.33f, h * 0.29f)
                                lineTo(w * 0.71f, h * 0.29f)
                                lineTo(w * 0.71f, h * 0.67f)
                            }
                            drawPath(
                                path = arrowPath,
                                color = Color(0xFF86EFAC),
                                style = Stroke(
                                    width = 1.8.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                        Text(
                            text = "Your approved rewards",
                            color = Color.White,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 9.5.sp,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                        )
                    }
                }

                // .wbottom: .wbtn (View Wallet) + .wid (WALLET ID)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Row(
                        modifier = Modifier
                            .height(29.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(99.dp),
                                spotColor = Color(0xFF140A50).copy(alpha = 0.45f)
                            )
                            .clip(RoundedCornerShape(99.dp))
                            .background(if (isWithdrawAction) Color(0xFF16A34A) else Color.White)
                            .clickable { onViewWallet() }
                            .padding(start = 11.dp, end = 4.dp)
                            .testTag(if (isWithdrawAction) "wallet_card_withdraw_button" else "view_wallet_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = actionButtonText,
                            color = if (isWithdrawAction) Color.White else Color(0xFF4535D6),
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                        )
                        Box(
                            modifier = Modifier
                                .size(21.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isWithdrawAction) Color.White.copy(alpha = 0.22f) else Color(0xFFEFE9FD)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = if (isWithdrawAction) Color.White else Color(0xFF4535D6),
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }

                    val context = LocalContext.current
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F0B36).copy(alpha = 0.65f))
                            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .clickable {
                                if (!isLoading && walletIdCode.isNotBlank()) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    clipboard?.setPrimaryClip(ClipData.newPlainText("Wallet ID", walletIdCode))
                                    Toast.makeText(context, "Wallet ID copied: $walletIdCode", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("wallet_id_chip")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (isLoading) "TG-········" else walletIdCode,
                                    color = Color.White,
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.5.sp,
                                    letterSpacing = 0.8.sp,
                                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                )
                            }
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Copy Wallet ID",
                                tint = Color(0xFFC7D2FE),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Exact replica of #home .summary (Header + .sum-card with spinning skeleton loader AND clean, tap-highlight-free interactive Donut + Legend rows)
 */
@Composable
private fun TaskSummarySection(
    pendingCount: Int,
    approvedCount: Int,
    rejectedCount: Int,
    totalTasks: Int,
    pendingPct: Int,
    approvedPct: Int,
    rejectedPct: Int,
    isLoading: Boolean,
    shimmerProgress: Float,
    ringSpinDegrees: Float,
    modifier: Modifier = Modifier
) {
    var selectedStat by remember { mutableStateOf<SummaryStatKey?>(null) }

    val activeCount = when (selectedStat) {
        SummaryStatKey.Pending -> pendingCount
        SummaryStatKey.Approved -> approvedCount
        SummaryStatKey.Rejected -> rejectedCount
        null -> totalTasks
    }
    val activeLabel = selectedStat?.label ?: "Total Tasks"

    fun milestoneSweep(count: Int): Float {
        if (count <= 0) return 0f
        val cycleCount = (count % 500).let { if (it == 0) 500 else it }
        return ((cycleCount / 500f) * 360f).coerceAtLeast(10f)
    }

    val targetHighlightSweep = when (selectedStat) {
        null -> 0f
        SummaryStatKey.Pending -> if (pendingCount > 0) milestoneSweep(pendingCount) else 3f
        SummaryStatKey.Approved -> if (approvedCount > 0) milestoneSweep(approvedCount) else 3f
        SummaryStatKey.Rejected -> if (rejectedCount > 0) milestoneSweep(rejectedCount) else 3f
    }
    val animatedHighlightSweep by animateFloatAsState(
        targetValue = targetHighlightSweep,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "donut_highlight_sweep"
    )

    val rankLevel = (approvedCount / 10) + 1
    val rejectionPenaltyPct = (rejectedCount / 20) * 5

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // .sec-head
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Task Summary",
                    color = Color(0xFF17142E),
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    letterSpacing = (-0.3).sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(Color(0xFFEDE9FE))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (rejectionPenaltyPct > 0) {
                            "Lv.$rankLevel • -${rejectionPenaltyPct}% Rank"
                        } else {
                            "Lv.$rankLevel • ${totalTasks % 500}/500"
                        },
                        color = Color(0xFF6D28D9),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color(0xFF7B3FE4).copy(alpha = 0.08f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { selectedStat = null }
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (selectedStat == null) "Lifetime overview" else "Reset view",
                    color = Color(0xFF7B3FE4),
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 9.5.sp
                )
            }
        }

        // .sum-card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 14.dp,
                    shape = RoundedCornerShape(21.dp),
                    ambientColor = Color(0xFF32238C).copy(alpha = 0.14f),
                    spotColor = Color(0xFF32238C).copy(alpha = 0.24f)
                )
                .clip(RoundedCornerShape(21.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.96f),
                            Color(0xFFFCFBFF).copy(alpha = 0.92f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.90f),
                    shape = RoundedCornerShape(21.dp)
                )
                .padding(horizontal = 14.dp, vertical = 13.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Left: Interactive .donut (100.dp) — cycles through Total -> Pending -> Approved -> Rejected with zero tap flash
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = !isLoading
                        ) {
                            selectedStat = when (selectedStat) {
                                null -> SummaryStatKey.Pending
                                SummaryStatKey.Pending -> SummaryStatKey.Approved
                                SummaryStatKey.Approved -> SummaryStatKey.Rejected
                                SummaryStatKey.Rejected -> null
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                rotationZ = if (isLoading) ringSpinDegrees else 0f
                            }
                    ) {
                        val strokeWidthPx = 9.5.dp.toPx()
                        val radius = (size.minDimension - strokeWidthPx) / 2f
                        val topLeft = Offset(
                            (size.width - radius * 2f) / 2f,
                            (size.height - radius * 2f) / 2f
                        )
                        val arcSize = Size(radius * 2f, radius * 2f)

                        if (isLoading) {
                            drawArc(
                                color = Color(0xFFEDE9FE),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = Color(0xFFDDD5F7),
                                startAngle = -90f,
                                sweepAngle = 82f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                            )
                        } else {
                            // Base Track Ring (.track: #eef0f8)
                            drawArc(
                                color = Color(0xFFEEF0F8),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                            )

                            val activeFilter = selectedStat
                            if (activeFilter != null) {
                                // Smoothly animated active status arc (or tiny dot when count is 0)
                                drawArc(
                                    color = activeFilter.color.copy(alpha = 0.14f),
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = activeFilter.color,
                                    startAngle = -90f,
                                    sweepAngle = animatedHighlightSweep,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                                )
                            } else if (totalTasks <= 0) {
                                // 0 tasks: show only tiny status dots at top of ring
                                drawArc(
                                    color = SummaryStatKey.Pending.color,
                                    startAngle = -96f,
                                    sweepAngle = 2.5f,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = SummaryStatKey.Approved.color,
                                    startAngle = -90f,
                                    sweepAngle = 2.5f,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = SummaryStatKey.Rejected.color,
                                    startAngle = -84f,
                                    sweepAngle = 2.5f,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                                )
                            } else {
                                // Gradual 500-task milestone fill so 1 task only fills a small slice
                                val rawPending = milestoneSweep(pendingCount)
                                val rawApproved = milestoneSweep(approvedCount)
                                val rawRejected = milestoneSweep(rejectedCount)
                                val rawSum = rawPending + rawApproved + rawRejected
                                val scale = if (rawSum > 360f) 360f / rawSum else 1f

                                var currentStart = -90f
                                val pendingSweep = rawPending * scale
                                val approvedSweep = rawApproved * scale
                                val rejectedSweep = rawRejected * scale

                                if (pendingSweep > 0f) {
                                    drawArc(
                                        color = SummaryStatKey.Pending.color,
                                        startAngle = currentStart,
                                        sweepAngle = pendingSweep,
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = arcSize,
                                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                                    )
                                    currentStart += pendingSweep
                                }
                                if (approvedSweep > 0f) {
                                    drawArc(
                                        color = SummaryStatKey.Approved.color,
                                        startAngle = currentStart,
                                        sweepAngle = approvedSweep,
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = arcSize,
                                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                                    )
                                    currentStart += approvedSweep
                                }
                                if (rejectedSweep > 0f) {
                                    drawArc(
                                        color = SummaryStatKey.Rejected.color,
                                        startAngle = currentStart,
                                        sweepAngle = rejectedSweep,
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = arcSize,
                                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                                    )
                                }
                            }
                        }
                    }

                    // .center (#cNum + #cLabel) with smooth crossfade transition
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    ) {
                        AnimatedContent(
                            targetState = if (isLoading) "—" else "$activeCount",
                            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                            label = "donut_num"
                        ) { targetText ->
                            Text(
                                text = targetText,
                                color = when {
                                    isLoading -> Color(0xFF17142E).copy(alpha = 0.35f)
                                    selectedStat != null -> selectedStat!!.color
                                    else -> Color(0xFF17142E)
                                },
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 21.sp,
                                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        AnimatedContent(
                            targetState = activeLabel,
                            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                            label = "donut_label"
                        ) { labelText ->
                            Text(
                                text = labelText,
                                color = Color(0xFF6F6E8A),
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 8.8.sp,
                                lineHeight = 10.5.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Right: .legend (Either 3 Shimmer Skeleton Rows OR 3 Clean Interactive Metric Rows)
                if (isLoading) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        repeat(3) {
                            SummarySkeletonRow(shimmerProgress = shimmerProgress)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TaskSummaryStatusRow(
                            statKey = SummaryStatKey.Pending,
                            subtitle = "$pendingPct% of total",
                            count = pendingCount,
                            isSelected = selectedStat == SummaryStatKey.Pending,
                            onClick = {
                                selectedStat = if (selectedStat == SummaryStatKey.Pending) null else SummaryStatKey.Pending
                            }
                        )
                        TaskSummaryStatusRow(
                            statKey = SummaryStatKey.Approved,
                            subtitle = "$approvedPct% of total",
                            count = approvedCount,
                            isSelected = selectedStat == SummaryStatKey.Approved,
                            onClick = {
                                selectedStat = if (selectedStat == SummaryStatKey.Approved) null else SummaryStatKey.Approved
                            }
                        )
                        TaskSummaryStatusRow(
                            statKey = SummaryStatKey.Rejected,
                            subtitle = "$rejectedPct% of total",
                            count = rejectedCount,
                            isSelected = selectedStat == SummaryStatKey.Rejected,
                            onClick = {
                                selectedStat = if (selectedStat == SummaryStatKey.Rejected) null else SummaryStatKey.Rejected
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Animated Shimmer Block (.sk in home.css: linear-gradient(90deg, #eceaf6 0%, #f8f6fe 45%, #eceaf6 90%))
 */
@Composable
private fun ShimmerBlock(
    shimmerProgress: Float,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .drawBehind {
                val width = size.width
                val startX = shimmerProgress * width
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFECEAF6),
                            Color(0xFFF8F6FE),
                            Color(0xFFECEAF6)
                        ),
                        startX = startX,
                        endX = startX + width * 0.9f
                    )
                )
            }
    )
}

/**
 * Exact replica of .sk-row in home.html / home.css
 */
@Composable
private fun SummarySkeletonRow(shimmerProgress: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 5.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        // .sk-dot (8x8dp circle)
        ShimmerBlock(
            shimmerProgress = shimmerProgress,
            cornerRadius = 99.dp,
            modifier = Modifier.size(9.dp)
        )
        // .sk-txt (.sk-line 70% + .sk-short 42%)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            ShimmerBlock(
                shimmerProgress = shimmerProgress,
                modifier = Modifier
                    .fillMaxWidth(0.70f)
                    .height(8.dp)
            )
            ShimmerBlock(
                shimmerProgress = shimmerProgress,
                modifier = Modifier
                    .fillMaxWidth(0.42f)
                    .height(6.dp)
            )
        }
        // .sk-num (22x12dp)
        ShimmerBlock(
            shimmerProgress = shimmerProgress,
            modifier = Modifier
                .width(22.dp)
                .height(12.dp)
        )
    }
}

/**
 * Interactive Legend Row (.row in home.css) with zero harsh tap highlight (indication = null)
 * and a subtle, smooth active state transition.
 */
@Composable
private fun TaskSummaryStatusRow(
    statKey: SummaryStatKey,
    subtitle: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val dotColor = statKey.color
    val activeProgress by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "row_active_${statKey.name}"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(dotColor.copy(alpha = 0.08f * activeProgress))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // .dot with soft halo ring
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(dotColor.copy(alpha = 0.18f + 0.14f * activeProgress)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
            Column {
                Text(
                    text = statKey.label,
                    color = Color(0xFF17142E),
                    fontFamily = PoppinsFontFamily,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 11.5.sp,
                    lineHeight = 14.sp
                )
                Text(
                    text = subtitle,
                    color = Color(0xFF6F6E8A),
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 9.5.sp,
                    lineHeight = 12.sp
                )
            }
        }

        Text(
            text = "$count",
            color = if (isSelected) dotColor else Color(0xFF17142E),
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp
        )
    }
}

/**
 * Exact replica of <nav class="nav"> + ../15-SHARED/navigation.js:
 * - Sky-blue gradient bar (#5ccbfb -> #2aaef0 -> #0b8ad4) with animated sheen & white rim
 * - Smooth concave cradle (bent-hill / inverted curved notch) carved into the bar's upper edge beneath the selected tab
 * - Floating circular 3D orb centered above the concave cradle notch, sliding horizontally with the notch
 * - Active tab icon lifts directly into the exact center of the floating orb while its label stays aligned below
 */
@Composable
private fun TaskGullakBottomNavBar(
    activeTab: HomeBottomTab,
    sheenFraction: Float,
    onSelectTab: (HomeBottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = remember { HomeBottomTab.entries }

    val animatedTabIndex by animateFloatAsState(
        targetValue = activeTab.index.toFloat(),
        animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
        label = "nav_tab_slide"
    )

    BoxWithConstraints(
        modifier = modifier.height(88.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        val totalWidth = maxWidth
        val tabWidth = totalWidth / tabs.size
        val orbSize = 48.dp

        // Animated horizontal center X of both the concave cradle notch and the floating circular orb
        val orbCenterXDp = tabWidth * (animatedTabIndex + 0.5f)

        // 1. Full-height Canvas drawing the unclipped ambient shadow, outer orb glow, and pill bar with concave cradle notch
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val w = size.width
            val h = size.height
            val barTop = 30.dp.toPx()
            val cornerR = 21.dp.toPx()
            val cx = (w / tabs.size) * (animatedTabIndex + 0.5f)
            val orbCenterY = 24.dp.toPx()

            // Soft unclipped ambient elevation glow under the pill bar
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to Color.Transparent,
                        0.45f to Color(0xFF0B8AD4).copy(alpha = 0.14f),
                        0.85f to Color(0xFF0B8AD4).copy(alpha = 0.22f),
                        1.0f to Color.Transparent
                    ),
                    startY = barTop + 6.dp.toPx(),
                    endY = h + 6.dp.toPx()
                ),
                topLeft = Offset(4.dp.toPx(), barTop + 4.dp.toPx()),
                size = Size(w - 8.dp.toPx(), h - barTop),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerR, cornerR)
            )

            // Soft outer cyan halo (.orb-glow) behind the floating circular orb
            val glowRadius = 35.dp.toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF38BDF8).copy(alpha = 0.46f),
                        Color(0xFF38BDF8).copy(alpha = 0.16f),
                        Color.Transparent
                    ),
                    center = Offset(cx, orbCenterY),
                    radius = glowRadius
                ),
                center = Offset(cx, orbCenterY),
                radius = glowRadius
            )

            // Concave cradle (inverted curved notch) geometry:
            // Dips smoothly downward from barTop (30.dp) to valleyY (52.dp) cradling the 48.dp orb centered at y=24.dp
            val notchHalfW = 41.dp.toPx()
            val notchDepth = 22.dp.toPx()
            val valleyY = barTop + notchDepth

            val waveStart = (cx - notchHalfW).coerceAtLeast(cornerR)
            val waveEnd = (cx + notchHalfW).coerceAtMost(w - cornerR)
            val leftSpan = (cx - waveStart).coerceAtLeast(1f)
            val rightSpan = (waveEnd - cx).coerceAtLeast(1f)

            val barPath = Path().apply {
                moveTo(cornerR, barTop)
                if (waveStart > cornerR) {
                    lineTo(waveStart, barTop)
                }
                // Left half of concave cradle: flat top edge -> smooth valley bottom
                cubicTo(
                    waveStart + leftSpan * 0.43f, barTop,
                    cx - leftSpan * 0.48f, valleyY,
                    cx, valleyY
                )
                // Right half of concave cradle: smooth valley bottom -> flat top edge
                cubicTo(
                    cx + rightSpan * 0.48f, valleyY,
                    waveEnd - rightSpan * 0.43f, barTop,
                    waveEnd, barTop
                )
                if (waveEnd < w - cornerR) {
                    lineTo(w - cornerR, barTop)
                }

                // Top-right rounded corner
                quadraticTo(w, barTop, w, barTop + cornerR)
                lineTo(w, h - cornerR)
                // Bottom-right rounded corner
                quadraticTo(w, h, w - cornerR, h)
                lineTo(cornerR, h)
                // Bottom-left rounded corner
                quadraticTo(0f, h, 0f, h - cornerR)
                lineTo(0f, barTop + cornerR)
                // Top-left rounded corner
                quadraticTo(0f, barTop, cornerR, barTop)
                close()
            }

            // Fill with #barGrad (#5ccbfb -> #2aaef0 -> #0b8ad4)
            drawPath(
                path = barPath,
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0xFF5CCBFB),
                        0.55f to Color(0xFF2AAEF0),
                        1.0f to Color(0xFF0B8AD4)
                    ),
                    startY = barTop,
                    endY = h
                )
            )

            // Animated horizontal sheen (#sheen)
            val sheenCenter = sheenFraction * w
            drawPath(
                path = barPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.22f),
                        Color.Transparent
                    ),
                    startX = sheenCenter - 120.dp.toPx(),
                    endX = sheenCenter + 120.dp.toPx()
                )
            )

            // Crisp top/outer rim highlight (#barRim) following the flat top edge and concave cradle
            drawPath(
                path = barPath,
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to Color.White.copy(alpha = 0.92f),
                        0.45f to Color.White.copy(alpha = 0.22f),
                        1.0f to Color.White.copy(alpha = 0.28f)
                    ),
                    startY = barTop,
                    endY = h
                ),
                style = Stroke(width = 1.4.dp.toPx())
            )
        }

        // 2. Sliding Floating Circular Orb (.orb: 48.dp diameter, centered at x = orbCenterXDp, y = 24.dp)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = orbCenterXDp - (orbSize / 2), y = 0.dp)
                .size(orbSize),
            contentAlignment = Alignment.Center
        ) {
            // .orb-face (crisp white outer ring + soft drop shadow + cyan 3D sphere gradient)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        ambientColor = Color(0xFF0284C7).copy(alpha = 0.35f),
                        spotColor = Color(0xFF0284C7).copy(alpha = 0.50f)
                    )
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(2.5.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF6FD6FF),
                                    Color(0xFF2AAEF0),
                                    Color(0xFF0B8AD4)
                                )
                            )
                        )
                        .drawBehind {
                            // Subtle top inner specular gloss inside the orb
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.32f),
                                        Color.Transparent
                                    ),
                                    center = Offset(size.width * 0.5f, size.height * 0.22f),
                                    radius = size.width * 0.48f
                                ),
                                center = Offset(size.width * 0.5f, size.height * 0.22f),
                                radius = size.width * 0.48f
                            )
                        }
                )
            }
        }

        // 3. Interactive 5 Tab Slots (.tab)
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Bottom
        ) {
            tabs.forEach { tab ->
                val isSelected = activeTab == tab
                val iconLiftProgress by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0f,
                    animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
                    label = "icon_lift_${tab.name}"
                )

                val testTag = when (tab) {
                    HomeBottomTab.Offers -> "nav_offers"
                    HomeBottomTab.Refer -> "nav_refer"
                    HomeBottomTab.Home -> "nav_home"
                    HomeBottomTab.Wallet -> "nav_wallet"
                    HomeBottomTab.Profile -> "nav_profile"
                }

                val interactionSource = remember { MutableInteractionSource() }

                // When iconLiftProgress == 1f (active), top offset is 13.dp so the 22.dp icon's center is at 24.dp (exact center of the 48.dp orb)
                // When iconLiftProgress == 0f (inactive), top offset is 39.dp, sitting cleanly inside the bar above the label
                val iconTopOffsetDp = 39f - (26f * iconLiftProgress)
                val iconAlpha = 0.86f + (0.14f * iconLiftProgress)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { onSelectTab(tab) }
                        .testTag(testTag)
                ) {
                    NavBarSvgIcon(
                        tab = tab,
                        tint = Color.White.copy(alpha = iconAlpha),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = iconTopOffsetDp.dp)
                            .size(22.dp)
                    )

                    Text(
                        text = tab.label,
                        color = Color.White.copy(alpha = if (isSelected) 1f else 0.88f),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 10.5.sp,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Draws the exact <svg viewBox="0 0 24 24"> stroke icons from home.html for Offers, Refer, Home, Wallet, and Profile
 */
@Composable
private fun NavBarSvgIcon(
    tab: HomeBottomTab,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val strokeStyle = Stroke(
            width = 2.0f * sx,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        when (tab) {
            HomeBottomTab.Offers -> {
                // Tag outline: M12.586 2.586A2 2 0 0 0 11.172 2H4a2 2 0 0 0-2 2v7.172a2 2 0 0 0 .586 1.414l8.704 8.704a2.426 2.426 0 0 0 3.42 0l6.58-6.58a2.426 2.426 0 0 0 0-3.42z
                val path = Path().apply {
                    moveTo(4f * sx, 2f * sy)
                    lineTo(11.2f * sx, 2f * sy)
                    lineTo(21.2f * sx, 12f * sy)
                    lineTo(13.5f * sx, 19.7f * sy)
                    lineTo(11.3f * sx, 21.2f * sy)
                    lineTo(2f * sx, 11.2f * sy)
                    lineTo(2f * sx, 4f * sy)
                    close()
                }
                drawPath(path = path, color = tint, style = strokeStyle)
                drawCircle(
                    color = tint,
                    radius = 1.1f * sx,
                    center = Offset(7.2f * sx, 7.2f * sy)
                )
            }

            HomeBottomTab.Refer -> {
                // Person head (cx=9, cy=7, r=4) + shoulders (M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2) + plus (M19 8v6 M22 11h-6)
                drawCircle(
                    color = tint,
                    radius = 3.8f * sx,
                    center = Offset(9f * sx, 7f * sy),
                    style = strokeStyle
                )
                val bodyPath = Path().apply {
                    moveTo(2.2f * sx, 20.5f * sy)
                    lineTo(2.2f * sx, 18.8f * sy)
                    quadraticTo(2.2f * sx, 15f * sy, 6f * sx, 15f * sy)
                    lineTo(12f * sx, 15f * sy)
                    quadraticTo(15.8f * sx, 15f * sy, 15.8f * sx, 18.8f * sy)
                    lineTo(15.8f * sx, 20.5f * sy)
                }
                drawPath(path = bodyPath, color = tint, style = strokeStyle)
                val plusPath = Path().apply {
                    moveTo(19f * sx, 8f * sy)
                    lineTo(19f * sx, 14f * sy)
                    moveTo(16f * sx, 11f * sy)
                    lineTo(22f * sx, 11f * sy)
                }
                drawPath(path = plusPath, color = tint, style = strokeStyle)
            }

            HomeBottomTab.Home -> {
                // House outline + door (M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8 & M3 10l9-7 9 7v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z)
                val housePath = Path().apply {
                    moveTo(3.2f * sx, 10.2f * sy)
                    lineTo(12f * sx, 3f * sy)
                    lineTo(20.8f * sx, 10.2f * sy)
                    lineTo(20.8f * sx, 19.2f * sy)
                    quadraticTo(20.8f * sx, 20.8f * sy, 19.2f * sx, 20.8f * sy)
                    lineTo(4.8f * sx, 20.8f * sy)
                    quadraticTo(3.2f * sx, 20.8f * sy, 3.2f * sx, 19.2f * sy)
                    close()
                }
                drawPath(path = housePath, color = tint, style = strokeStyle)
                val doorPath = Path().apply {
                    moveTo(9.2f * sx, 20.8f * sy)
                    lineTo(9.2f * sx, 12.5f * sy)
                    lineTo(14.8f * sx, 12.5f * sy)
                    lineTo(14.8f * sx, 20.8f * sy)
                }
                drawPath(path = doorPath, color = tint, style = strokeStyle)
            }

            HomeBottomTab.Wallet -> {
                // Wallet body + clasp
                val walletPath = Path().apply {
                    moveTo(19f * sx, 7f * sy)
                    lineTo(19f * sx, 4.5f * sy)
                    lineTo(5f * sx, 4.5f * sy)
                    quadraticTo(3.2f * sx, 4.5f * sy, 3.2f * sx, 6.5f * sy)
                    lineTo(3.2f * sx, 18.5f * sy)
                    quadraticTo(3.2f * sx, 20.5f * sy, 5.2f * sx, 20.5f * sy)
                    lineTo(19.8f * sx, 20.5f * sy)
                    lineTo(19.8f * sx, 7.5f * sy)
                    lineTo(4.5f * sx, 7.5f * sy)
                }
                drawPath(path = walletPath, color = tint, style = strokeStyle)
                val claspPath = Path().apply {
                    moveTo(19.8f * sx, 11.5f * sy)
                    lineTo(16.2f * sx, 11.5f * sy)
                    quadraticTo(14.5f * sx, 11.5f * sy, 14.5f * sx, 13.5f * sy)
                    quadraticTo(14.5f * sx, 15.5f * sy, 16.2f * sx, 15.5f * sy)
                    lineTo(19.8f * sx, 15.5f * sy)
                }
                drawPath(path = claspPath, color = tint, style = strokeStyle)
            }

            HomeBottomTab.Profile -> {
                // Person head (cx=12, cy=8, r=4.5) + shoulders (M20 21a8 8 0 0 0-16 0)
                drawCircle(
                    color = tint,
                    radius = 4.4f * sx,
                    center = Offset(12f * sx, 8f * sy),
                    style = strokeStyle
                )
                val arcPath = Path().apply {
                    moveTo(4.2f * sx, 20.8f * sy)
                    cubicTo(
                        4.8f * sx, 15.2f * sy,
                        19.2f * sx, 15.2f * sy,
                        19.8f * sx, 20.8f * sy
                    )
                }
                drawPath(path = arcPath, color = tint, style = strokeStyle)
            }
        }
    }
}

/**
 * Full-Screen Maintenance Mode Lock Screen matching WhatsApp Image 2026-10-25 at 6.13.47 AM.jpeg.
 * Hides all navigation bars, estimated downtime timers, and never says "account suspended".
 */
@Composable
internal fun MaintenanceModeFullScreen(
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFCFC),
                        Color(0xFFFFF5F6),
                        Color(0xFFFFE8EC)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("maintenance_mode_screen")
    ) {
        // Subtle warm coral wave at the bottom
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .align(Alignment.BottomCenter)
        ) {
            val w = size.width
            val h = size.height
            val wavePath = Path().apply {
                moveTo(0f, h * 0.45f)
                cubicTo(w * 0.30f, h * 0.90f, w * 0.65f, h * 0.05f, w, h * 0.35f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = wavePath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFFDA4AF).copy(alpha = 0.35f),
                        Color(0xFFFB7185).copy(alpha = 0.55f)
                    )
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top-Left App Brand Header (Rounded-Square Purple Icon + Title + "Earn • Review • Grow")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .shadow(8.dp, RoundedCornerShape(14.dp), spotColor = Color(0xFF6D28D9))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF6D28D9), Color(0xFF4F46E5))
                            ),
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Glossy3DCheckBadge(badgeSize = 26.dp)
                }

                Column {
                    Text(
                        text = "Task Gullak",
                        color = Color(0xFF1E2238),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )
                    Text(
                        text = "Earn • Review • Grow",
                        color = Color(0xFF64748B),
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.8f))

            // 3D Robot Maintenance Illustration with soft blush backdrop & floating coral spheres
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width * 0.52f
                    val cy = size.height * 0.48f
                    drawCircle(
                        color = Color(0xFFFFE4E6).copy(alpha = 0.72f),
                        radius = size.minDimension * 0.44f,
                        center = Offset(cx, cy)
                    )
                    // Floating 3D coral dots matching mockup
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFDA4AF), Color(0xFFF43F5E)),
                            center = Offset(size.width * 0.31f, size.height * 0.14f),
                            radius = 18f
                        ),
                        radius = 15f,
                        center = Offset(size.width * 0.32f, size.height * 0.15f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFDA4AF), Color(0xFFE11D48)),
                            center = Offset(size.width * 0.78f, size.height * 0.11f),
                            radius = 22f
                        ),
                        radius = 18f,
                        center = Offset(size.width * 0.79f, size.height * 0.12f)
                    )
                }

                val context = LocalContext.current
                val robotResId = remember(context) {
                    val id = context.resources.getIdentifier("img_maintenance_robot", "drawable", context.packageName)
                    if (id != 0) id else context.resources.getIdentifier("ic_launcher", "mipmap", context.packageName)
                }
                if (robotResId != 0) {
                    Image(
                        painter = painterResource(id = robotResId),
                        contentDescription = "Under Maintenance Illustration",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(245.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Two-Tone Heading: "We're Under" (Navy) + "Maintenance!" (Coral-Red)
            Text(
                text = "We’re Under",
                color = Color(0xFF1E2238),
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 29.sp,
                lineHeight = 34.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Maintenance!",
                color = Color(0xFFE11D48),
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 31.sp,
                lineHeight = 36.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = message.ifBlank {
                    "Our app is currently under maintenance\nto bring you a better experience.\nWe’ll be back soon!"
                },
                color = Color(0xFF475569),
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.5.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp)
            )

            Spacer(modifier = Modifier.weight(1.2f))
        }
    }
}


