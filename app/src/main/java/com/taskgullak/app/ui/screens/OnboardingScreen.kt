package com.taskgullak.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskgullak.app.R
import com.taskgullak.app.ui.components.ShinyGradientButton
import com.taskgullak.app.ui.theme.PoppinsFontFamily
import com.taskgullak.app.ui.theme.TgBluePrimary
import com.taskgullak.app.ui.theme.TgOnboardingText
import com.taskgullak.app.ui.theme.TgPurpleSecondary
import com.taskgullak.app.ui.theme.TgRewardGold
import com.taskgullak.app.ui.theme.TgSlateMuted
import com.taskgullak.app.ui.theme.TgSuccessGreen
import kotlinx.coroutines.launch
import kotlin.math.abs

// Matches CSS `--ease: cubic-bezier(0.22, 1, 0.36, 1)`
private val OnboardingCubicEase = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)

/**
 * Exact 100% UI & 3D animation replica of onboarding.html + onboarding.css + onboarding.js
 */
@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        Color(0xFFF8FAFC),
                        Color(0xFFF1F5F9)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("onboarding_screen")
    ) {
        // Ambient Background Aesthetics (.ambient-blue, .ambient-purple, .ambient-gold) — unclipped radial canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to TgBluePrimary.copy(alpha = 0.10f),
                        0.50f to TgBluePrimary.copy(alpha = 0.04f),
                        1.0f to Color.Transparent
                    ),
                    center = Offset(w * 0.08f, h * 0.18f),
                    radius = w * 0.55f
                ),
                radius = w * 0.55f,
                center = Offset(w * 0.08f, h * 0.18f)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to TgPurpleSecondary.copy(alpha = 0.09f),
                        0.50f to TgPurpleSecondary.copy(alpha = 0.035f),
                        1.0f to Color.Transparent
                    ),
                    center = Offset(w * 0.92f, h * 0.44f),
                    radius = w * 0.58f
                ),
                radius = w * 0.58f,
                center = Offset(w * 0.92f, h * 0.44f)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to TgRewardGold.copy(alpha = 0.06f),
                        0.50f to TgRewardGold.copy(alpha = 0.02f),
                        1.0f to Color.Transparent
                    ),
                    center = Offset(w * 0.42f, h * 0.88f),
                    radius = w * 0.45f
                ),
                radius = w * 0.45f,
                center = Offset(w * 0.42f, h * 0.88f)
            )
        }

        // Onboarding Shell (.onboarding-shell)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp)
                .align(Alignment.Center)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // App Header (.onboarding-header)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Task ",
                        color = TgOnboardingText,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 21.sp,
                        letterSpacing = (-0.6).sp
                    )
                    Text(
                        text = "Gullak",
                        style = TextStyle(
                            brush = Brush.linearGradient(
                                colors = listOf(TgBluePrimary, TgPurpleSecondary)
                            ),
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 21.sp,
                            letterSpacing = (-0.6).sp
                        )
                    )
                }

                TextButton(
                    onClick = onFinishOnboarding,
                    modifier = Modifier.testTag("skip_onboarding_button")
                ) {
                    Text(
                        text = "Skip",
                        color = TgSlateMuted,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }

            // Slides Pager (.slides) with smooth cubic-bezier interpolation and zero jitter
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                val absOffset = abs(pageOffset).coerceIn(0f, 1f)
                val easedProgress = 1f - OnboardingCubicEase.transform(absOffset)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = 0.25f + (0.75f * easedProgress)
                            val scale = 0.96f + (0.04f * easedProgress)
                            scaleX = scale
                            scaleY = scale
                            translationX = -pageOffset * 24.dp.toPx()
                        }
                ) {
                    when (page) {
                        0 -> OnboardingSlideContent(
                            slideIndex = 0,
                            slideProgress = easedProgress,
                            imageRes = R.drawable.img_onboarding_1,
                            eyebrowIcon = Icons.Outlined.Explore,
                            eyebrowText = "DISCOVER",
                            eyebrowColor = TgBluePrimary,
                            title = "Find Tasks That Fit You",
                            description = "Discover simple tasks, testing opportunities, and campaigns that match your interests.",
                            badge1 = FloatingBadgeSpec(Icons.Outlined.PlayCircleOutline, TgBluePrimary, TgBluePrimary.copy(alpha = 0.12f)),
                            badge2 = FloatingBadgeSpec(Icons.Outlined.StarOutline, TgPurpleSecondary, TgPurpleSecondary.copy(alpha = 0.12f)),
                            badge3 = FloatingBadgeSpec(Icons.Outlined.Checklist, Color(0xFFD97706), TgRewardGold.copy(alpha = 0.16f)),
                            showSparks = true,
                            showVerificationRing = false,
                            showCoins = false
                        )
                        1 -> OnboardingSlideContent(
                            slideIndex = 1,
                            slideProgress = easedProgress,
                            imageRes = R.drawable.img_onboarding_2,
                            eyebrowIcon = Icons.Outlined.Shield,
                            eyebrowText = "VERIFY",
                            eyebrowColor = TgSuccessGreen,
                            title = "Complete. Submit. Get Verified.",
                            description = "Follow the instructions, submit your work, and track verification from your dashboard.",
                            badge1 = FloatingBadgeSpec(Icons.Outlined.CloudUpload, TgBluePrimary, TgBluePrimary.copy(alpha = 0.12f)),
                            badge2 = FloatingBadgeSpec(Icons.Outlined.Verified, TgSuccessGreen, TgSuccessGreen.copy(alpha = 0.13f)),
                            badge3 = FloatingBadgeSpec(Icons.Outlined.Shield, TgPurpleSecondary, TgPurpleSecondary.copy(alpha = 0.12f)),
                            showSparks = false,
                            showVerificationRing = true,
                            showCoins = false
                        )
                        else -> OnboardingSlideContent(
                            slideIndex = 2,
                            slideProgress = easedProgress,
                            imageRes = R.drawable.img_onboarding_3,
                            eyebrowIcon = Icons.Outlined.AutoAwesome,
                            eyebrowText = "EARN",
                            eyebrowColor = TgPurpleSecondary,
                            title = "Earn & Track Your Rewards",
                            description = "See your verified rewards, wallet activity, and campaign progress in one place.",
                            badge1 = FloatingBadgeSpec(Icons.Outlined.AccountBalanceWallet, Color(0xFFD97706), TgRewardGold.copy(alpha = 0.16f)),
                            badge2 = FloatingBadgeSpec(Icons.AutoMirrored.Outlined.TrendingUp, TgBluePrimary, TgBluePrimary.copy(alpha = 0.12f)),
                            badge3 = FloatingBadgeSpec(Icons.Outlined.MonetizationOn, TgPurpleSecondary, TgPurpleSecondary.copy(alpha = 0.12f)),
                            showSparks = false,
                            showVerificationRing = false,
                            showCoins = true
                        )
                    }
                }
            }

            // Bottom Navigation Controls (.onboarding-footer)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Progress Dots (.progress)
                Row(
                    modifier = Modifier.height(26.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { index ->
                        val isActive = pagerState.currentPage == index
                        val isCompleted = index < pagerState.currentPage
                        val dotWidth by animateDpAsState(
                            targetValue = if (isActive) 23.dp else 8.dp,
                            animationSpec = tween(320, easing = OnboardingCubicEase),
                            label = "dotWidth"
                        )
                        val dotColor by animateColorAsState(
                            targetValue = when {
                                isActive -> TgBluePrimary
                                isCompleted -> TgBluePrimary.copy(alpha = 0.35f)
                                else -> Color(0xFFCBD5E1)
                            },
                            label = "dotColor"
                        )
                        Box(
                            modifier = Modifier
                                .width(dotWidth)
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isActive) {
                                        Brush.horizontalGradient(
                                            listOf(TgBluePrimary, TgPurpleSecondary)
                                        )
                                    } else {
                                        Brush.horizontalGradient(listOf(dotColor, dotColor))
                                    }
                                )
                                .clickable {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(
                                            page = index,
                                            animationSpec = tween(440, easing = OnboardingCubicEase)
                                        )
                                    }
                                }
                                .testTag("onboarding_dot_$index")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Next & Back Action Buttons (.navigation-row)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (pagerState.currentPage > 0) {
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(
                                        page = pagerState.currentPage - 1,
                                        animationSpec = tween(440, easing = OnboardingCubicEase)
                                    )
                                }
                            },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("onboarding_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous slide",
                                tint = TgSlateMuted,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Back",
                                color = TgSlateMuted,
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(84.dp))
                    }

                    val isLastSlide = pagerState.currentPage == 2
                    ShinyGradientButton(
                        text = if (isLastSlide) "Get Started" else "Next",
                        onClick = {
                            if (isLastSlide) {
                                onFinishOnboarding()
                            } else {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(
                                        page = pagerState.currentPage + 1,
                                        animationSpec = tween(440, easing = OnboardingCubicEase)
                                    )
                                }
                            }
                        },
                        gradientColors = listOf(TgBluePrimary, TgPurpleSecondary),
                        height = 48.dp,
                        cornerRadius = 15.dp,
                        fontSizeSp = 13f,
                        modifier = Modifier.width(if (isLastSlide) 148.dp else 114.dp),
                        testTag = "onboarding_next_button",
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

private data class FloatingBadgeSpec(
    val icon: ImageVector,
    val iconTint: Color,
    val iconBg: Color
)

@Composable
private fun OnboardingSlideContent(
    slideIndex: Int,
    slideProgress: Float,
    imageRes: Int,
    eyebrowIcon: ImageVector,
    eyebrowText: String,
    eyebrowColor: Color,
    title: String,
    description: String,
    badge1: FloatingBadgeSpec,
    badge2: FloatingBadgeSpec,
    badge3: FloatingBadgeSpec,
    showSparks: Boolean,
    showVerificationRing: Boolean,
    showCoins: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "slide3D_$slideIndex")

    // 1. @keyframes artworkFloat (5s ease-in-out infinite -> 2500ms half-cycle):
    //    0%, 100% { transform: translate3d(0, 0, 0) rotateZ(0deg); }
    //    50% { transform: translate3d(0, -7px, 0) rotateZ(0.6deg); }
    val artworkProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "artworkProgress"
    )
    val artOffsetY = -7f * artworkProgress
    val artRotateZ = 0.6f * artworkProgress

    // 2. @keyframes glowPulse (4.5s ease-in-out infinite -> 2250ms half-cycle):
    val glowProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2250, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowProgress"
    )

    // 3. @keyframes floatingCard (4s ease-in-out infinite -> 2000ms half-cycle) with exact CSS delays:
    val card1Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(100)
        ),
        label = "card1Progress"
    )
    val card2Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(650)
        ),
        label = "card2Progress"
    )
    val card3Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(1150)
        ),
        label = "card3Progress"
    )

    // 4. @keyframes sparkFloat (3.2s ease-in-out infinite -> 1600ms half-cycle) with delays 0s, 0.7s, 1.1s
    val spark1Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "spark1Progress"
    )
    val spark2Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(700)
        ),
        label = "spark2Progress"
    )
    val spark3Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(1100)
        ),
        label = "spark3Progress"
    )

    // 5. @keyframes verifyPop (3.5s ease-in-out infinite -> 1750ms half-cycle):
    val verifyPopScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "verifyPopScale"
    )

    // 6. @keyframes coinFloat (3s ease-in-out infinite -> 1500ms half-cycle) with delays 0s, 0.8s, 1.35s:
    val coin1Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coin1Progress"
    )
    val coin2Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(800)
        ),
        label = "coin2Progress"
    )
    val coin3Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(1350)
        ),
        label = "coin3Progress"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 3D Artwork Stage (.artwork-stage)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp)
                .height(310.dp),
            contentAlignment = Alignment.Center
        ) {
            // .artwork-glow with @keyframes glowPulse — soft unclipped radial halo
            val glowScale = 0.96f + (0.08f * glowProgress)
            val glowAlpha = 0.70f + (0.30f * glowProgress)
            val glowTranslateY = 10f - (6f * glowProgress)
            Canvas(
                modifier = Modifier
                    .size(290.dp)
                    .graphicsLayer {
                        scaleX = glowScale
                        scaleY = glowScale
                        alpha = glowAlpha
                        translationY = glowTranslateY.dp.toPx()
                    }
            ) {
                val r = size.minDimension * 0.48f
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to TgBluePrimary.copy(alpha = 0.11f),
                            0.45f to TgPurpleSecondary.copy(alpha = 0.055f),
                            0.80f to TgPurpleSecondary.copy(alpha = 0.015f),
                            1.0f to Color.Transparent
                        ),
                        center = center,
                        radius = r
                    ),
                    radius = r,
                    center = center
                )
            }

            // Ultra-soft, unclipped elliptical Ground Contact Shadow underneath the 3D illustration
            // Uses withTransform({ scale(1f, h / w) }) so the radial gradient fades to 0.0 alpha along the ellipse perimeter
            Canvas(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-6).dp)
                    .width(260.dp)
                    .height(64.dp)
                    .graphicsLayer {
                        val shadowScale = 1f - (0.05f * artworkProgress)
                        scaleX = shadowScale
                        scaleY = shadowScale
                        alpha = 0.88f - (0.12f * artworkProgress)
                    }
            ) {
                val w = size.width
                val h = size.height
                val cOffset = Offset(w / 2f, h / 2f)
                withTransform({
                    scale(scaleX = 1f, scaleY = h / w, pivot = cOffset)
                }) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.0f to Color(0xFF0F172A).copy(alpha = 0.11f),
                                0.35f to Color(0xFF2563EB).copy(alpha = 0.055f),
                                0.68f to Color(0xFF2563EB).copy(alpha = 0.018f),
                                0.90f to Color(0xFF2563EB).copy(alpha = 0.004f),
                                1.0f to Color.Transparent
                            ),
                            center = cOffset,
                            radius = w * 0.48f
                        ),
                        radius = w * 0.48f,
                        center = cOffset
                    )
                }
            }

            // Main 4K 3D Illustration (.artwork-image-wrap) with unclipped radial ambient depth behind it
            val imageBitmap = ImageBitmap.imageResource(id = imageRes)
            val bitmapPainter = remember(imageRes) {
                BitmapPainter(imageBitmap, filterQuality = FilterQuality.High)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize(0.88f)
                    .graphicsLayer {
                        translationY = artOffsetY.dp.toPx()
                        rotationZ = artRotateZ
                    },
                contentAlignment = Alignment.Center
            ) {
                // Soft unclipped ambient depth glow behind the 3D character & platform (never clips to box edges)
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(y = 12.dp)
                ) {
                    val r = size.minDimension * 0.46f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.0f to Color(0xFF0F172A).copy(alpha = 0.09f),
                                0.45f to Color(0xFF2563EB).copy(alpha = 0.045f),
                                0.78f to Color(0xFF2563EB).copy(alpha = 0.012f),
                                1.0f to Color.Transparent
                            ),
                            center = center,
                            radius = r
                        ),
                        radius = r,
                        center = center
                    )
                }

                // Crisp Foreground 3D Illustration
                Image(
                    painter = bitmapPainter,
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Floating UI Badge 1 (.float-card-one: top 17%, left 3%, delay 0.1s)
            FloatingGlassCard(
                spec = badge1,
                floatProgress = card1Progress,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-8).dp, y = 26.dp)
            )

            // Floating UI Badge 2 (.float-card-two: top 12%, right 4%, delay 0.65s)
            FloatingGlassCard(
                spec = badge2,
                floatProgress = card2Progress,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = 12.dp)
            )

            // Floating UI Badge 3 (.float-card-three: bottom 13%, left 8%, delay 1.15s)
            FloatingGlassCard(
                spec = badge3,
                floatProgress = card3Progress,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = 4.dp, y = (-12).dp)
            )

            // Decorative Animated Sparks on Slide 1 (.spark-one, .spark-two, .spark-three)
            if (showSparks) {
                AnimatedSparkDot(
                    size = 7.dp,
                    color = TgBluePrimary,
                    progress = spark1Progress,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-68).dp, y = 64.dp)
                )
                AnimatedSparkDot(
                    size = 5.dp,
                    color = TgPurpleSecondary,
                    progress = spark2Progress,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-50).dp, y = (-62).dp)
                )
                AnimatedSparkDot(
                    size = 4.5.dp,
                    color = TgRewardGold,
                    progress = spark3Progress,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = 50.dp, y = (-16).dp)
                )
            }

            // Verification Ring on Slide 2 (.verification-ring with @keyframes verifyPop & soft radial halo)
            if (showVerificationRing) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-18).dp, y = (-26).dp)
                        .graphicsLayer {
                            scaleX = verifyPopScale
                            scaleY = verifyPopScale
                        }
                        .size(84.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val r = size.minDimension * 0.48f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colorStops = arrayOf(
                                    0.0f to TgSuccessGreen.copy(alpha = 0.22f),
                                    0.50f to TgSuccessGreen.copy(alpha = 0.09f),
                                    0.82f to TgSuccessGreen.copy(alpha = 0.02f),
                                    1.0f to Color.Transparent
                                ),
                                center = center,
                                radius = r
                            ),
                            radius = r,
                            center = center
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .shadow(
                                elevation = 6.dp,
                                shape = CircleShape,
                                ambientColor = TgSuccessGreen.copy(alpha = 0.18f),
                                spotColor = TgSuccessGreen.copy(alpha = 0.24f)
                            )
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF22C55E), Color(0xFF16A34A))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Verified",
                            tint = Color.White,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                }
            }

            // Floating 3D Spinning ₹ Coins on Slide 3 (.coin-one, .coin-two, .coin-three)
            if (showCoins) {
                FloatingRupeeCoin(
                    size = 34.dp,
                    fontSizeSp = 15f,
                    progress = coin1Progress,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = 68.dp, y = 24.dp)
                )
                FloatingRupeeCoin(
                    size = 27.dp,
                    fontSizeSp = 12f,
                    progress = coin2Progress,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-48).dp, y = 50.dp)
                )
                FloatingRupeeCoin(
                    size = 25.dp,
                    fontSizeSp = 11f,
                    progress = coin3Progress,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-64).dp, y = (-36).dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Text Content (.text-content) with smooth cubic-bezier slide interpolation
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 390.dp)
                .padding(horizontal = 8.dp)
                .graphicsLayer {
                    alpha = slideProgress
                    translationY = (1f - slideProgress) * 12.dp.toPx()
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = eyebrowIcon,
                    contentDescription = null,
                    tint = eyebrowColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = eyebrowText,
                    color = eyebrowColor,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    letterSpacing = 1.4.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                color = TgOnboardingText,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 23.sp,
                lineHeight = 29.sp,
                letterSpacing = (-0.5).sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                color = TgSlateMuted,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.5.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 360.dp)
            )
        }
    }
}

@Composable
private fun FloatingGlassCard(
    spec: FloatingBadgeSpec,
    floatProgress: Float,
    modifier: Modifier = Modifier
) {
    // Generous 84.dp unclipped outer container so the soft radial drop shadow fades to 0 alpha well before the bounds
    Box(
        modifier = modifier
            .size(84.dp)
            .graphicsLayer {
                translationY = (-9f * floatProgress).dp.toPx()
                rotationZ = 2f * floatProgress
            },
        contentAlignment = Alignment.Center
    ) {
        // Ultra-soft, unclipped radial ambient shadow (blends seamlessly into background without rectangular clipping)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = 7.dp)
        ) {
            val r = size.minDimension * 0.48f
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0xFF0F172A).copy(alpha = 0.085f),
                        0.42f to Color(0xFF0F172A).copy(alpha = 0.042f),
                        0.75f to Color(0xFF0F172A).copy(alpha = 0.012f),
                        1.0f to Color.Transparent
                    ),
                    center = center,
                    radius = r
                ),
                radius = r,
                center = center
            )
        }

        Box(
            modifier = Modifier
                .size(52.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = Color(0xFF0F172A).copy(alpha = 0.06f),
                    spotColor = Color(0xFF0F172A).copy(alpha = 0.09f)
                )
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.92f))
                .border(1.dp, Color.White.copy(alpha = 0.96f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(spec.iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = spec.icon,
                    contentDescription = null,
                    tint = spec.iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun AnimatedSparkDot(
    size: Dp,
    color: Color,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val scale = 0.80f + (0.35f * progress)
    val alpha = 0.30f + (0.55f * progress)
    val dy = -7f * progress

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
                translationY = dy.dp.toPx()
            }
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun FloatingRupeeCoin(
    size: Dp,
    fontSizeSp: Float,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val dy = -8f * progress
    val rotY = 180f * progress
    val textScaleX = if (rotY > 90f) -1f else 1f

    Box(
        modifier = modifier
            .size(size * 1.75f)
            .graphicsLayer {
                translationY = dy.dp.toPx()
                rotationY = rotY
                cameraDistance = 12f * density
            },
        contentAlignment = Alignment.Center
    ) {
        // Soft unclipped radial gold drop shadow
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = 5.dp)
        ) {
            val r = this.size.minDimension * 0.48f
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0xFFD97706).copy(alpha = 0.20f),
                        0.45f to Color(0xFFD97706).copy(alpha = 0.08f),
                        0.80f to Color(0xFFD97706).copy(alpha = 0.018f),
                        1.0f to Color.Transparent
                    ),
                    center = center,
                    radius = r
                ),
                radius = r,
                center = center
            )
        }

        Box(
            modifier = Modifier
                .size(size)
                .shadow(
                    elevation = 5.dp,
                    shape = CircleShape,
                    ambientColor = Color(0xFFD97706).copy(alpha = 0.14f),
                    spotColor = Color(0xFFD97706).copy(alpha = 0.20f)
                )
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFE8A3),
                            Color(0xFFFBBF24),
                            Color(0xFFD97706)
                        ),
                        center = Offset(20f, 18f)
                    )
                )
                .border(2.dp, Color.White.copy(alpha = 0.65f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "₹",
                color = Color(0xFF8A5200),
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = fontSizeSp.sp,
                modifier = Modifier.graphicsLayer {
                    scaleX = textScaleX
                }
            )
        }
    }
}

