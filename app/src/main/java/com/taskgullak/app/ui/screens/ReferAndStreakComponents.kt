package com.taskgullak.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.taskgullak.app.R
import com.taskgullak.app.data.model.UserProfile
import com.taskgullak.app.data.remote.FirebaseEmailDispatchService
import com.taskgullak.app.data.remote.ReferralFriendRecord
import com.taskgullak.app.data.remote.ReferralSummaryState
import com.taskgullak.app.ui.theme.InterFontFamily
import com.taskgullak.app.ui.theme.PoppinsFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Exact Refer & Earn Tab Screen matching the user's provided design image
 * with `https://taskgullak.space/dl?ref=<CODE>` and Lifetime Referral Commission.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExactReferAndEarnScreen(
    liveReferredUsers: List<UserProfile> = emptyList(),
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    var refreshTick by remember { mutableIntStateOf(0) }
    var showTrackReferralsSheet by remember { mutableStateOf(false) }
    var copiedNotice by remember { mutableStateOf<String?>(null) }

    val baseSummary: ReferralSummaryState = remember(refreshTick) {
        FirebaseEmailDispatchService.getReferralSummaryState(context)
    }

    val combinedFriends = remember(baseSummary.referredFriends, liveReferredUsers, baseSummary.referralCode) {
        val byEmail = LinkedHashMap<String, ReferralFriendRecord>()
        baseSummary.referredFriends.forEach { friend ->
            val key = friend.email.lowercase().ifBlank { friend.name.lowercase() }
            byEmail[key] = friend
        }
        liveReferredUsers.forEach { user ->
            val key = user.email.lowercase().ifBlank { user.userId.lowercase() }
            val completed = user.completedTasksCount.toInt()
            val estCommission = (completed * 2.5)
            byEmail[key] = ReferralFriendRecord(
                name = user.fullName.ifBlank { "Referred Friend" },
                email = user.email,
                joinedAtLabel = "Joined via ${baseSummary.referralCode} • Active",
                tasksCompleted = completed,
                lifetimeCommissionEarned = estCommission
            )
        }
        byEmail.values.toList()
    }

    val totalFriendsCount = combinedFriends.size
    val directBonusTotal = remember(baseSummary.totalReferralBonusEarned, totalFriendsCount) {
        if (totalFriendsCount == 0) 0.0 else maxOf(baseSummary.totalReferralBonusEarned, totalFriendsCount * 5.0)
    }
    val lifetimeBonusTotal = remember(baseSummary.totalLifetimeCommissionEarned, combinedFriends) {
        if (combinedFriends.isEmpty()) {
            baseSummary.totalLifetimeCommissionEarned
        } else {
            maxOf(
                baseSummary.totalLifetimeCommissionEarned,
                combinedFriends.sumOf { it.lifetimeCommissionEarned }
            )
        }
    }
    val summary = remember(baseSummary, combinedFriends, totalFriendsCount, directBonusTotal, lifetimeBonusTotal) {
        baseSummary.copy(
            totalReferredFriends = totalFriendsCount,
            totalReferralBonusEarned = directBonusTotal,
            totalLifetimeCommissionEarned = lifetimeBonusTotal,
            referredFriends = combinedFriends
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(contentPadding)
            .testTag("refer_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
            // 1. DARK EMERALD HERO BANNER + OVERLAPPING WHITE STATS & HOW IT WORKS CARD
            Box(modifier = Modifier.fillMaxWidth()) {
                // Dark Emerald Hero Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(205.dp)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF0E3F33),
                                    Color(0xFF165B4A),
                                    Color(0xFF1D725D)
                                )
                            )
                        )
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                        .testTag("refer_hero_banner")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color(0xFF287A65).copy(alpha = 0.85f))
                                    .border(1.dp, Color(0xFF4ADE80).copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                                    .padding(horizontal = 11.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "REFER & EARN",
                                    color = Color(0xFFA7F3D0),
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.5.sp,
                                    letterSpacing = 0.9.sp
                                )
                            }

                            Text(
                                text = buildAnnotatedString {
                                    withStyle(
                                        SpanStyle(
                                            color = Color.White,
                                            fontFamily = PoppinsFontFamily,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 23.sp
                                        )
                                    ) {
                                        append("Invite Friends.\n")
                                    }
                                    withStyle(
                                        SpanStyle(
                                            color = Color(0xFF4ADE80),
                                            fontFamily = PoppinsFontFamily,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 23.sp
                                        )
                                    ) {
                                        append("Earn ")
                                    }
                                    withStyle(
                                        SpanStyle(
                                            color = Color.White,
                                            fontFamily = PoppinsFontFamily,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 23.sp
                                        )
                                    ) {
                                        append("Together.")
                                    }
                                },
                                lineHeight = 27.sp
                            )

                            Text(
                                text = "Earn 5% lifetime commission on friend's completed tasks — without any deduction from friend's earnings!",
                                color = Color(0xFFD1FAE5).copy(alpha = 0.88f),
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }

                        // Animated Referral Characters Illustration (Megaphone + Smartphone)
                        ReferralCharactersIllustration(
                            modifier = Modifier
                                .size(width = 150.dp, height = 168.dp)
                                .offset(y = 12.dp)
                        )
                    }
                }

                // White Elevated Summary & How It Works Card
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color.White,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 162.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 3 Summary Boxes: Direct Referral Bonus | FRIEND GETS | Lifetime Task Bonus (%)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReferStatMiniBox(
                                modifier = Modifier.weight(1f),
                                topLabel = "DIRECT BONUS",
                                mainValue = "₹${summary.perReferralBonus}",
                                subLabel = "Direct Referral"
                            )
                            ReferStatMiniBox(
                                modifier = Modifier.weight(1f),
                                topLabel = "FRIEND GETS",
                                mainValue = "₹${summary.friendBonusAfterFirstWithdraw}",
                                subLabel = "After 1st Withdraw"
                            )
                            ReferStatMiniBox(
                                modifier = Modifier.weight(1f),
                                topLabel = "LIFETIME %",
                                mainValue = "${summary.lifetimeCommissionPercent}%",
                                subLabel = "Task Commission"
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ✨ HOW IT WORKS ✨ Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color(0xFFE5E7EB)
                            )
                            Text(
                                text = "✨  HOW IT WORKS  ✨",
                                color = Color(0xFF6B7280),
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color(0xFFE5E7EB)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 01 -> 02 -> 03 -> 04 Stepper
                        Box(modifier = Modifier.fillMaxWidth()) {
                            HorizontalDivider(
                                color = Color(0xFFBBF7D0),
                                thickness = 2.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 36.dp)
                                    .align(Alignment.TopCenter)
                                    .padding(top = 19.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                HowItWorksStepItem(number = "01", label = "Share Code")
                                HowItWorksStepItem(number = "02", label = "Friend Joins")
                                HowItWorksStepItem(number = "03", label = "Completes Task")
                                HowItWorksStepItem(number = "04", label = "You Earn")
                            }
                        }
                    }
                }
            }

            // 2. MINT REFERRAL CODE & LINK CARD (https://taskgullak.space/dl?ref=...)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFEEFBF3))
                    .border(1.2.dp, Color(0xFFBBE8CC), RoundedCornerShape(20.dp))
                    .padding(16.dp)
                    .testTag("refer_code_and_link_card")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "YOUR REFERRAL CODE",
                        color = Color(0xFF15803D),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.8.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = summary.referralCode,
                                color = Color(0xFF0F291E),
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                letterSpacing = 2.sp,
                                modifier = Modifier.testTag("refer_code_text")
                            )

                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(10.dp))
                                    .clickable {
                                        copyToClipboard(context, "Referral Code", summary.referralCode)
                                        copiedNotice = "Referral code ${summary.referralCode} copied!"
                                    }
                                    .testTag("copy_referral_code_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "Copy Referral Code",
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Track Referrals > button
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFF86EFAC), RoundedCornerShape(12.dp))
                                .clickable { showTrackReferralsSheet = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("track_referrals_button"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Track Referrals",
                                color = Color(0xFF15803D),
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color(0xFF15803D),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dashed Divider
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                    ) {
                        drawLine(
                            color = Color(0xFF86EFAC),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "REFERRAL LINK",
                        color = Color(0xFF4B6B58),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.8.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFFD1FAE5), RoundedCornerShape(11.dp))
                                .clickable {
                                    copyToClipboard(context, "Referral Link", summary.referralLink)
                                    copiedNotice = "Link copied: ${summary.referralLink}"
                                }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = summary.referralLink,
                                color = Color(0xFF374151),
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("refer_link_text")
                            )
                        }

                        Row(
                            modifier = Modifier
                                .height(42.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(Color(0xFF1EB955))
                                .clickable {
                                    shareReferralLink(context, summary.referralCode, summary.referralLink)
                                }
                                .padding(horizontal = 14.dp)
                                .testTag("share_referral_link_button"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Link",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Share Link",
                                color = Color.White,
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            )
                        }
                    }

                    if (!copiedNotice.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = copiedNotice ?: "",
                            color = Color(0xFF15803D),
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.5.sp
                        )
                    }
                }
            }

            // 3. LIFETIME COMMISSION GUARANTEE BANNER (MATCHING BOTTOM MINT CARD IN IMAGE 1)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFEAFBF0))
                    .border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .testTag("lifetime_commission_info_banner"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1EB955)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Lifetime ${summary.lifetimeCommissionPercent}% Task Commission — Zero Deduction",
                        color = Color(0xFF0F291E),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp
                    )
                    Text(
                        text = "Whenever your referred friend completes any task, you earn ${summary.lifetimeCommissionPercent}% lifetime bonus directly from Task Gullak. Nothing is deducted from your friend's wallet or real task reward!",
                        color = Color(0xFF166534),
                        fontFamily = InterFontFamily,
                        fontSize = 10.5.sp,
                        lineHeight = 14.sp
                    )
                }
            }

            // 4. LIVE LIFETIME REFERRAL EARNINGS SUMMARY CARD
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            Icon(
                                imageVector = Icons.Outlined.Groups,
                                contentDescription = null,
                                tint = Color(0xFF15803D),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Your Lifetime Referral Earnings",
                                color = Color(0xFF111827),
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = "${summary.totalReferredFriends} Friends Joined",
                            color = Color(0xFF15803D),
                            fontFamily = PoppinsFontFamily,
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
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF0FDF4))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Direct Referral Bonus",
                                    color = Color(0xFF4B5563),
                                    fontFamily = InterFontFamily,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = String.format(Locale.US, "₹%.2f", summary.totalReferralBonusEarned),
                                    color = Color(0xFF15803D),
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF0FDF4))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Lifetime Task Bonus (5%)",
                                    color = Color(0xFF4B5563),
                                    fontFamily = InterFontFamily,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = String.format(Locale.US, "₹%.2f", summary.totalLifetimeCommissionEarned),
                                    color = Color(0xFF15803D),
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    modifier = Modifier.testTag("lifetime_commission_amount_text")
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
    }

    if (showTrackReferralsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTrackReferralsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Track Referrals & Lifetime Bonus",
                    color = Color(0xFF0F291E),
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp
                )
                Text(
                    text = "You earn ${summary.lifetimeCommissionPercent}% lifetime commission every time your referred friends complete tasks — with ₹0 deducted from their task rewards.",
                    color = Color(0xFF4B5563),
                    fontFamily = InterFontFamily,
                    fontSize = 11.5.sp
                )

                if (summary.referredFriends.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFF0FDF4))
                            .border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(16.dp))
                            .padding(horizontal = 18.dp, vertical = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Groups,
                            contentDescription = null,
                            tint = Color(0xFF15803D),
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "0 Referrals Yet",
                            color = Color(0xFF0F291E),
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Share your code (${summary.referralCode}) with friends. When they join, their progress and your 5% lifetime bonus will appear here!",
                            color = Color(0xFF4B5563),
                            fontFamily = InterFontFamily,
                            fontSize = 11.5.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    summary.referredFriends.forEach { friend ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFF0FDF4))
                                .border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = friend.name,
                                    color = Color(0xFF111827),
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "${friend.joinedAtLabel} • ${friend.tasksCompleted} Tasks Completed",
                                    color = Color(0xFF4B5563),
                                    fontFamily = InterFontFamily,
                                    fontSize = 10.5.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format(Locale.US, "+₹%.2f", friend.lifetimeCommissionEarned),
                                    color = Color(0xFF15803D),
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.5.sp
                                )
                                Text(
                                    text = "Lifetime 5%",
                                    color = Color(0xFF16A34A),
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 9.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReferStatMiniBox(
    modifier: Modifier = Modifier,
    topLabel: String,
    mainValue: String,
    subLabel: String
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF9FAFB))
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = topLabel,
            color = Color(0xFF6B7280),
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 8.5.sp,
            letterSpacing = 0.5.sp
        )
        Text(
            text = mainValue,
            color = Color(0xFF15803D),
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp
        )
        Text(
            text = subLabel,
            color = Color(0xFF6B7280),
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HowItWorksStepItem(
    number: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0xFFDCFCE7))
                .border(1.5.dp, Color(0xFF86EFAC), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                color = Color(0xFF15803D),
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp
            )
        }
        Text(
            text = label,
            color = Color(0xFF374151),
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 9.5.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ReferralCharactersIllustration(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "characters_float")
    val floatOffset by transition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "characters_bob"
    )

    Box(
        modifier = modifier.offset(y = floatOffset.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_referral_characters),
            contentDescription = "Invite Friends Illustration",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun copyToClipboard(context: Context, label: String, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, value))
    Toast.makeText(context, "$label copied!", Toast.LENGTH_SHORT).show()
}

private fun shareReferralLink(context: Context, code: String, link: String) {
    val cleanCode = code.ifBlank { "DYLDV847" }
    val cleanLink = if (link.isNotBlank()) link else "https://taskgullak.space/?ref=$cleanCode"
    val shareText = """
Join Task Gullak & Earn Rewards!

Looking for a platform to complete tasks and earn rewards? Give Task Gullak a try! 😊

🎁 ₹5 Bonus: Use my referral code $cleanCode and get a ₹5 bonus after your first withdrawal, as per the referral offer.

5% Lifetime Commission: Earn a 5% commission on eligible referrals, subject to the platform's terms and conditions.

🔗 Join here: $cleanLink

🖊️ Referral Code: $cleanCode

🔘 Sign up using my link and check the offer details to get started!
""".trimIndent()

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Join Task Gullak & Earn Rewards!")
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    val chooser = Intent.createChooser(sendIntent, "Share Referral Link").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(chooser) }
}
