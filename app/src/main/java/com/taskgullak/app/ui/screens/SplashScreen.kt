package com.taskgullak.app.ui.screens

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskgullak.app.ui.components.Css3DPiggyBankStage
import com.taskgullak.app.ui.components.Glossy3DCheckBadge
import com.taskgullak.app.ui.components.Glossy3DTaskIcon
import com.taskgullak.app.ui.components.Task3DIconType
import com.taskgullak.app.ui.theme.PoppinsFontFamily
import com.taskgullak.app.ui.theme.TgBluePrimary
import com.taskgullak.app.ui.theme.TgPurpleSecondary
import com.taskgullak.app.ui.theme.TgSlateMuted
import com.taskgullak.app.ui.theme.TgSplashDarkText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val PremiumCardEasing = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)

/**
 * Exact 100% UI & 3D animation replica of splash.html + splash.css + splash.js
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val card1Progress = remember { Animatable(0f) }
    val card2Progress = remember { Animatable(0f) }
    val card3Progress = remember { Animatable(0f) }

    val check1Progress = remember { Animatable(0f) }
    val check2Progress = remember { Animatable(0f) }
    val check3Progress = remember { Animatable(0f) }

    val piggyProgress = remember { Animatable(0f) }
    val coinProgress = remember { Animatable(0f) }
    val brandProgress = remember { Animatable(0f) }
    val exitAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // Card entrances matching splash.css animation-delays (0.65s, 1.05s, 1.45s, duration 0.72s)
        launch {
            delay(450)
            card1Progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 720, easing = PremiumCardEasing)
            )
        }
        launch {
            delay(850)
            card2Progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 720, easing = PremiumCardEasing)
            )
        }
        launch {
            delay(1250)
            card3Progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 720, easing = PremiumCardEasing)
            )
        }
        // Checkmark pop animations (1.55s, 1.90s, 2.25s, duration 0.50s)
        launch {
            delay(1350)
            check1Progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = PremiumCardEasing)
            )
        }
        launch {
            delay(1700)
            check2Progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = PremiumCardEasing)
            )
        }
        launch {
            delay(2050)
            check3Progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = PremiumCardEasing)
            )
        }
        // Piggy Bank entrance (2.25s, duration 0.7s)
        launch {
            delay(2050)
            piggyProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 700, easing = PremiumCardEasing)
            )
        }
        // Coin drop animation (2.55s, duration 0.9s) + coin chime sound (3.15s)
        launch {
            delay(2350)
            coinProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 900, easing = PremiumCardEasing)
            )
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 45)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
            } catch (_: Exception) {
                // Ignore audio error in headless environments
            }
        }
        // Brand entrance (2.9s, duration 0.65s)
        launch {
            delay(2700)
            brandProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 650, easing = PremiumCardEasing)
            )
        }
        // Exit transition directly without fading to black
        delay(3900)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFF8FAFC),
                        Color(0xFFEEF5FF),
                        Color(0xFFF6F0FF)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("splash_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        // Seamless Ambient Background Glows (.glow-blue, .glow-purple) drawn with unclipped radial gradients
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Top-left soft blue ambient glow
            val blueCenter = Offset(w * 0.05f, h * 0.04f)
            val blueRadius = w * 0.72f
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0xFF2563EB).copy(alpha = 0.10f),
                        0.45f to Color(0xFF2563EB).copy(alpha = 0.045f),
                        0.80f to Color(0xFF2563EB).copy(alpha = 0.01f),
                        1.0f to Color.Transparent
                    ),
                    center = blueCenter,
                    radius = blueRadius
                ),
                radius = blueRadius,
                center = blueCenter
            )

            // Bottom-right soft purple ambient glow
            val purpleCenter = Offset(w * 0.92f, h * 0.90f)
            val purpleRadius = w * 0.78f
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0xFF7C3AED).copy(alpha = 0.11f),
                        0.45f to Color(0xFF7C3AED).copy(alpha = 0.05f),
                        0.80f to Color(0xFF7C3AED).copy(alpha = 0.012f),
                        1.0f to Color.Transparent
                    ),
                    center = purpleCenter,
                    radius = purpleRadius
                ),
                radius = purpleRadius,
                center = purpleCenter
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(76.dp))

            // Task Preview Stage (.task-stage)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 390.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                SplashTaskCard(
                    cardProgress = card1Progress.value,
                    checkProgress = check1Progress.value,
                    title = "Watch Video",
                    subtitle = "Earn Reward",
                    iconType = Task3DIconType.Video
                )

                SplashTaskCard(
                    cardProgress = card2Progress.value,
                    checkProgress = check2Progress.value,
                    title = "Give Feedback",
                    subtitle = "Earn Reward",
                    iconType = Task3DIconType.Feedback
                )

                SplashTaskCard(
                    cardProgress = card3Progress.value,
                    checkProgress = check3Progress.value,
                    title = "Complete Task",
                    subtitle = "Earn Reward",
                    iconType = Task3DIconType.Checklist
                )
            }

            Spacer(modifier = Modifier.height(34.dp))

            // 3D Piggy Bank & Coin Stage (.piggy-stage)
            Css3DPiggyBankStage(
                coinDropFraction = coinProgress.value,
                piggyScaleFraction = piggyProgress.value
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Brand Details (.brand-section: @keyframes brand-enter)
            val bProg = brandProgress.value.coerceIn(0f, 1f)
            val brandOffsetY = (1f - bProg) * 24f
            Column(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = bProg
                        translationY = brandOffsetY.dp.toPx()
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text(
                        text = "Task",
                        color = TgSplashDarkText,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 36.sp,
                        letterSpacing = (-1.4).sp
                    )
                    Text(
                        text = "Gullak",
                        style = TextStyle(
                            brush = Brush.horizontalGradient(
                                colors = listOf(TgBluePrimary, TgPurpleSecondary)
                            ),
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 36.sp,
                            letterSpacing = (-1.4).sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(11.dp))

                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = TgSlateMuted, fontWeight = FontWeight.SemiBold)) {
                            append("Complete Tasks ")
                        }
                        withStyle(SpanStyle(color = TgPurpleSecondary, fontWeight = FontWeight.ExtraBold)) {
                            append("•")
                        }
                        withStyle(SpanStyle(color = TgSlateMuted, fontWeight = FontWeight.SemiBold)) {
                            append(" Earn Rewards ")
                        }
                        withStyle(SpanStyle(color = TgPurpleSecondary, fontWeight = FontWeight.ExtraBold)) {
                            append("•")
                        }
                        withStyle(SpanStyle(color = TgSlateMuted, fontWeight = FontWeight.SemiBold)) {
                            append(" Save More")
                        }
                    },
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.5.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Exact replica of .task-card + @keyframes premium-card-in + .task-icon + .checkmark + @keyframes check-pop
 */
@Composable
private fun SplashTaskCard(
    cardProgress: Float,
    checkProgress: Float,
    title: String,
    subtitle: String,
    iconType: Task3DIconType
) {
    val density = LocalDensity.current.density
    val p = cardProgress.coerceIn(0f, 1f)

    // @keyframes premium-card-in:
    // 0%: opacity 0, translateY(-50px) rotateX(10deg) scale(0.94)
    // 65%: opacity 1, translateY(5px) rotateX(-1deg) scale(1.01)
    // 100%: opacity 1, translateY(0) rotateX(0deg) scale(1)
    val cardAlpha: Float
    val cardTranslateYDp: Float
    val cardRotateX: Float
    val cardScale: Float

    if (p <= 0.65f) {
        val t = p / 0.65f
        cardAlpha = t
        cardTranslateYDp = -50f + (50f + 5f) * t
        cardRotateX = 10f + (-1f - 10f) * t
        cardScale = 0.94f + (1.01f - 0.94f) * t
    } else {
        val t = (p - 0.65f) / 0.35f
        cardAlpha = 1f
        cardTranslateYDp = 5f * (1f - t)
        cardRotateX = -1f * (1f - t)
        cardScale = 1.01f + (1.0f - 1.01f) * t
    }

    // @keyframes check-pop:
    // 0%: opacity 0, scale(0.45)
    // 70%: opacity 1, scale(1.13)
    // 100%: opacity 1, scale(1)
    val cp = checkProgress.coerceIn(0f, 1f)
    val checkAlpha: Float
    val checkScale: Float
    if (cp <= 0.70f) {
        val t = cp / 0.70f
        checkAlpha = t
        checkScale = 0.45f + (1.13f - 0.45f) * t
    } else {
        val t = (cp - 0.70f) / 0.30f
        checkAlpha = 1f
        checkScale = 1.13f + (1.0f - 1.13f) * t
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .graphicsLayer {
                alpha = cardAlpha
                translationY = cardTranslateYDp * density
                rotationX = cardRotateX
                scaleX = cardScale
                scaleY = cardScale
                cameraDistance = 16f * density
            },
        contentAlignment = Alignment.Center
    ) {
        // .task-card::after — ultra-soft unclipped elliptical radial shadow that blends seamlessly into the background
        Canvas(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.94f)
                .height(44.dp)
                .offset(y = 14.dp)
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
                            0.0f to Color(0xFF2563EB).copy(alpha = 0.085f),
                            0.42f to Color(0xFF2563EB).copy(alpha = 0.042f),
                            0.76f to Color(0xFF2563EB).copy(alpha = 0.012f),
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

        Row(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = Color(0xFF2563EB).copy(alpha = 0.06f),
                    spotColor = Color(0xFF2563EB).copy(alpha = 0.10f)
                )
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.98f),
                            Color(0xFFF8FAFC).copy(alpha = 0.92f)
                        )
                    )
                )
                .border(1.dp, Color.White, RoundedCornerShape(20.dp))
                .padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Task Icon (50x50dp) with .task-icon::before glossy top highlight, ::after 3D bottom lip & colored 3D drop shadow
            Glossy3DTaskIcon(
                type = iconType,
                boxSize = 50.dp,
                cornerRadius = 16.dp
            )

            Spacer(modifier = Modifier.width(13.dp))

            // Task Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TgSplashDarkText,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = TgSlateMuted,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.5.sp
                )
            }

            // Checkmark Badge (31x31dp) with @keyframes check-pop overshoot + 3D green shadow
            Glossy3DCheckBadge(
                badgeSize = 31.dp,
                modifier = Modifier.graphicsLayer {
                    alpha = checkAlpha
                    scaleX = checkScale
                    scaleY = checkScale
                }
            )
        }
    }
}
