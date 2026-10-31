package com.taskgullak.app.ui.components

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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskgullak.app.ui.theme.PoppinsFontFamily

/**
 * Draws the exact CSS background grid lines (`background-grid`) with configurable spacing and line color.
 */
@Composable
fun CssGridOverlay(
    gridSizeDp: Dp = 30.dp,
    lineColor: Color = Color(0x0A664D91),
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val stepPx = gridSizeDp.toPx()
        if (stepPx <= 0f) return@Canvas
        var x = 0f
        while (x <= size.width) {
            drawLine(
                color = lineColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1.dp.toPx()
            )
            x += stepPx
        }
        var y = 0f
        while (y <= size.height) {
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
            y += stepPx
        }
    }
}

/**
 * Floating 3D sphere/orb matching `.purple-orb`, `.orange-orb`, and `.floating-ball` from CSS.
 */
@Composable
fun Floating3DOrb(
    size: Dp,
    colors: List<Color>,
    offsetX: Dp,
    offsetY: Dp,
    alignment: Alignment = Alignment.TopStart,
    durationMillis: Int = 4500,
    delayMillis: Int = 0,
    alpha: Float = 0.72f
) {
    val transition = rememberInfiniteTransition(label = "orbFloat")
    val dy by transition.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, delayMillis = delayMillis),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbDy"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = alignment
    ) {
        Canvas(
            modifier = Modifier
                .offset(x = offsetX, y = offsetY + dy.dp)
                .size(size)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = colors,
                    center = Offset(this.size.width * 0.30f, this.size.height * 0.25f),
                    radius = this.size.width * 0.75f
                ),
                alpha = alpha
            )
        }
    }
}

/**
 * Primary gradient button with CSS-matching diagonal shine sweep (`::after` skewX(-22deg) animation).
 */
@Composable
fun ShinyGradientButton(
    text: String,
    onClick: () -> Unit,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
    cornerRadius: Dp = 11.dp,
    fontSizeSp: Float = 11.5f,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    testTag: String = "shiny_gradient_button",
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "buttonShine")
    // Matches CSS @keyframes buttonShine (4s ease-in-out infinite: 0%, 55% { left: -70%; } 75%, 100% { left: 140%; })
    val cycleFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shineCycle"
    )
    val shineLeftFraction = when {
        cycleFraction <= 0.55f -> -0.70f
        cycleFraction <= 0.80f -> {
            val t = (cycleFraction - 0.55f) / 0.25f
            -0.70f + (2.10f * t)
        }
        else -> 1.40f
    }

    val shape = RoundedCornerShape(cornerRadius)
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .shadow(
                elevation = if (enabled) 12.dp else 0.dp,
                shape = shape,
                ambientColor = gradientColors.first().copy(alpha = 0.30f),
                spotColor = gradientColors.first().copy(alpha = 0.40f)
            )
            .clip(shape)
            .background(
                brush = Brush.linearGradient(colors = gradientColors),
                alpha = if (enabled) 1f else 0.65f
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Color.White),
                enabled = enabled && !isLoading,
                onClick = onClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        // Skewed -22deg shine ribbon matching `.next-button::after`
        Canvas(modifier = Modifier.fillMaxSize()) {
            val ribbonWidth = size.width * 0.42f
            val topX = size.width * shineLeftFraction
            val skewOffset = size.height * 0.40f
            val path = Path().apply {
                moveTo(topX + skewOffset, -size.height * 0.2f)
                lineTo(topX + ribbonWidth + skewOffset, -size.height * 0.2f)
                lineTo(topX + ribbonWidth - skewOffset, size.height * 1.2f)
                lineTo(topX - skewOffset, size.height * 1.2f)
                close()
            }
            drawPath(
                path = path,
                color = Color.White.copy(alpha = 0.25f)
            )
        }

        if (isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Text(
                    text = text,
                    color = Color.White,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSizeSp.sp
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.padding(horizontal = 14.dp)
            ) {
                leadingContent?.invoke()
                Text(
                    text = text,
                    color = Color.White,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSizeSp.sp
                )
                trailingContent?.invoke()
            }
        }
    }
}

/**
 * Exact replica of the 3D CSS Piggy Bank & Dropping ₹ Coin from splash.html / splash.css.
 */
@Composable
fun Css3DPiggyBankStage(
    coinDropFraction: Float,
    piggyScaleFraction: Float,
    modifier: Modifier = Modifier
) {
    // Interpolate exact CSS @keyframes piggy-enter:
    // 0% { opacity: 0; transform: translateY(15px) rotateX(15deg) scale(0.82); }
    // 70% { transform: translateY(-2px) rotateX(-2deg) scale(1.03); }
    // 100% { opacity: 1; transform: translateY(0) rotateX(0) scale(1); }
    val p = piggyScaleFraction.coerceIn(0f, 1f)
    val piggyAlpha = (p / 0.5f).coerceIn(0f, 1f)
    val piggyTranslateY = if (p <= 0.7f) {
        val t = p / 0.7f
        15f + (-17f * t)
    } else {
        val t = (p - 0.7f) / 0.3f
        -2f + (2f * t)
    }
    val piggyRotateX = if (p <= 0.7f) {
        val t = p / 0.7f
        15f + (-17f * t)
    } else {
        val t = (p - 0.7f) / 0.3f
        -2f + (2f * t)
    }
    val piggyScale = if (p <= 0.7f) {
        val t = p / 0.7f
        0.82f + (0.21f * t)
    } else {
        val t = (p - 0.7f) / 0.3f
        1.03f + (-0.03f * t)
    }

    // Interpolate exact CSS @keyframes coin-drop:
    // 0%   { opacity: 0; translateY(-75px) rotateY(0deg) scale(0.75); }
    // 45%  { opacity: 1; translateY(-30px) rotateY(180deg) scale(1); }
    // 78%  { translateY(5px) rotateY(330deg) scale(1.04); }
    // 90%  { translateY(-4px) rotateY(350deg) scale(0.98); }
    // 100% { opacity: 1; translateY(0) rotateY(360deg) scale(1); }
    val c = coinDropFraction.coerceIn(0f, 1f)
    val coinAlpha = (c / 0.45f).coerceIn(0f, 1f)
    val coinTranslateY = when {
        c <= 0.45f -> -75f + (45f * (c / 0.45f))
        c <= 0.78f -> -30f + (35f * ((c - 0.45f) / 0.33f))
        c <= 0.90f -> 5f + (-9f * ((c - 0.78f) / 0.12f))
        else -> -4f + (4f * ((c - 0.90f) / 0.10f))
    }
    val coinRotateY = when {
        c <= 0.45f -> 180f * (c / 0.45f)
        c <= 0.78f -> 180f + (150f * ((c - 0.45f) / 0.33f))
        c <= 0.90f -> 330f + (20f * ((c - 0.78f) / 0.12f))
        else -> 350f + (10f * ((c - 0.90f) / 0.10f))
    }
    val coinScale = when {
        c <= 0.45f -> 0.75f + (0.25f * (c / 0.45f))
        c <= 0.78f -> 1.00f + (0.04f * ((c - 0.45f) / 0.33f))
        c <= 0.90f -> 1.04f + (-0.06f * ((c - 0.78f) / 0.12f))
        else -> 0.98f + (0.02f * ((c - 0.90f) / 0.10f))
    }

    Box(
        modifier = modifier
            .width(170.dp)
            .height(152.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Soft, wide, unclipped 3D Ground Shadow beneath Piggy Bank (blends seamlessly to 0 alpha before canvas bounds)
        if (p > 0.02f) {
            Canvas(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 14.dp)
                    .width(164.dp)
                    .height(54.dp)
                    .graphicsLayer {
                        alpha = piggyAlpha
                        scaleX = piggyScale
                        scaleY = piggyScale
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
                                0.0f to Color(0xFF6D28D9).copy(alpha = 0.16f),
                                0.35f to Color(0xFF6D28D9).copy(alpha = 0.09f),
                                0.68f to Color(0xFF7C3AED).copy(alpha = 0.035f),
                                0.90f to Color(0xFF7C3AED).copy(alpha = 0.008f),
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
        }

        // Coin contact shadow (.coin-shadow) — seamless elliptical radial gradient
        if (c > 0.02f) {
            val shadowScale = when {
                c <= 0.75f -> 0.4f + (0.7f * (c / 0.75f))
                else -> 1.1f - (0.1f * ((c - 0.75f) / 0.25f))
            }
            Canvas(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
                    .width(64.dp)
                    .height(20.dp)
                    .graphicsLayer {
                        scaleX = shadowScale
                        scaleY = shadowScale
                        alpha = coinAlpha
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
                                0.0f to Color(0xFF0F172A).copy(alpha = 0.12f),
                                0.50f to Color(0xFF0F172A).copy(alpha = 0.05f),
                                1.0f to Color.Transparent
                            ),
                            center = cOffset,
                            radius = w * 0.46f
                        ),
                        radius = w * 0.46f,
                        center = cOffset
                    )
                }
            }
        }

        // Animated 3D ₹ Coin (.coin at top of stage above piggy bank)
        if (c > 0.01f) {
            val textMirror = if (coinRotateY in 90f..270f) -1f else 1f
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(64.dp)
                    .graphicsLayer {
                        alpha = coinAlpha
                        translationY = coinTranslateY.dp.toPx()
                        rotationY = coinRotateY
                        scaleX = coinScale
                        scaleY = coinScale
                        cameraDistance = 12f * density
                    },
                contentAlignment = Alignment.Center
            ) {
                // Soft unclipped radial gold glow under coin
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(y = 5.dp)
                ) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.0f to Color(0xFFF59E0B).copy(alpha = 0.24f),
                                0.45f to Color(0xFFF59E0B).copy(alpha = 0.10f),
                                0.80f to Color(0xFFF59E0B).copy(alpha = 0.02f),
                                1.0f to Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension * 0.48f
                        ),
                        radius = size.minDimension * 0.48f,
                        center = center
                    )
                }

                Box(
                    modifier = Modifier
                        .size(35.dp)
                        .shadow(
                            elevation = 5.dp,
                            shape = CircleShape,
                            ambientColor = Color(0xFFF59E0B).copy(alpha = 0.18f),
                            spotColor = Color(0xFFF59E0B).copy(alpha = 0.24f)
                        )
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFDE68A),
                                    Color(0xFFFBBF24),
                                    Color(0xFFD97706)
                                )
                            )
                        )
                        .border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "₹",
                        color = Color.White,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .offset(y = (-0.5).dp)
                            .graphicsLayer {
                                scaleX = textMirror
                            }
                    )
                }
            }
        }

        // Piggy Bank Structure (96x83dp at bottom of stage)
        if (p > 0.01f) {
            Box(
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .width(96.dp)
                    .height(83.dp)
                    .graphicsLayer {
                        alpha = piggyAlpha
                        translationY = piggyTranslateY.dp.toPx()
                        rotationX = piggyRotateX
                        scaleX = piggyScale
                        scaleY = piggyScale
                        cameraDistance = 12f * density
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                // Left Ear (.ear-left: top -10px, left 12px, rotate(-25deg))
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = 12.dp, y = 0.dp)
                        .size(27.dp)
                        .rotate(-25f)
                        .clip(RoundedCornerShape(topStart = 9.dp, topEnd = 15.dp, bottomStart = 9.dp, bottomEnd = 15.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFA855F7), Color(0xFF7C3AED))
                            )
                        )
                )
                // Right Ear (.ear-right: top -10px, right 12px, rotate(25deg))
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-12).dp, y = 0.dp)
                        .size(27.dp)
                        .rotate(25f)
                        .clip(RoundedCornerShape(topStart = 9.dp, topEnd = 15.dp, bottomStart = 9.dp, bottomEnd = 15.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFA855F7), Color(0xFF7C3AED))
                            )
                        )
                )

                // Legs (.leg-one, .leg-two)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = 20.dp, y = 6.dp)
                        .width(15.dp)
                        .height(13.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF9333EA), Color(0xFF6D28D9))
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-20).dp, y = 6.dp)
                        .width(15.dp)
                        .height(13.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF9333EA), Color(0xFF6D28D9))
                            )
                        )
                )

                // Main Piggy Body (96x73dp)
                Box(
                    modifier = Modifier
                        .width(96.dp)
                        .height(73.dp)
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(
                                topStart = 48.dp,
                                topEnd = 48.dp,
                                bottomStart = 35.dp,
                                bottomEnd = 35.dp
                            ),
                            ambientColor = Color(0xFF6D28D9).copy(alpha = 0.14f),
                            spotColor = Color(0xFF6D28D9).copy(alpha = 0.20f)
                        )
                        .clip(
                            RoundedCornerShape(
                                topStart = 48.dp,
                                topEnd = 48.dp,
                                bottomStart = 35.dp,
                                bottomEnd = 35.dp
                            )
                        )
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFC084FC),
                                    Color(0xFF9333EA),
                                    Color(0xFF6D28D9)
                                )
                            )
                        )
                ) {
                    // Highlight (.pig-highlight)
                    Box(
                        modifier = Modifier
                            .offset(x = 19.dp, y = 9.dp)
                            .width(38.dp)
                            .height(16.dp)
                            .rotate(-10f)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f))
                    )

                    // Pig Face (69x60dp at left 13px, top 13px)
                    Box(
                        modifier = Modifier
                            .offset(x = 13.5.dp, y = 13.dp)
                            .width(69.dp)
                            .height(60.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFFD8B4FE), Color(0xFFA855F7))
                                )
                            )
                    ) {
                        // Left Eye
                        Box(
                            modifier = Modifier
                                .offset(x = 20.dp, y = 20.dp)
                                .width(6.dp)
                                .height(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF172554))
                        )
                        // Right Eye
                        Box(
                            modifier = Modifier
                                .offset(x = 43.dp, y = 20.dp)
                                .width(6.dp)
                                .height(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF172554))
                        )
                        // Snout (31x20dp)
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp)
                                .width(31.dp)
                                .height(20.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFFF3E8FF), Color(0xFFD8B4FE))
                                    )
                                ),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF7E22CE))
                            )
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF7E22CE))
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class Task3DIconType {
    Video,
    Feedback,
    Checklist,
    Other
}

/**
 * Exact 100% 3D glossy squircle icon replica of `.task-icon` (`video-icon`, `feedback-icon`, `checklist-icon`)
 * from `splash.css` and `image_ab88d470.png`:
 * - Diffused colored 3D drop shadow (`0 12px 22px rgba(..., 0.25)`)
 * - `.task-icon::after` 3D bottom extruded ledge (`left: 7px; right: 7px; bottom: -4px; height: 7px; border-radius: 0 0 10px 10px; opacity: 0.36; filter: blur(1px)`)
 * - 145deg 3-stop rich gradient body (`50x50dp`, `border-radius: 16dp`)
 * - `inset 0 2px 2px rgba(255,255,255,0.42)` top inner specular rim + `inset 0 -5px 8px` bottom inner 3D bevel
 * - `.task-icon::before` off-center top-left glossy window (`top: 3px; left: 5px; width: 70%; height: 35%; border-radius: 12px`)
 * - Crisp Lucide vector glyph (`play-circle`, `star`, `list-check`) with `drop-shadow(0 2px 2px rgba(0,0,0,0.16))`
 */
@Composable
fun Glossy3DTaskIcon(
    type: Task3DIconType,
    modifier: Modifier = Modifier,
    boxSize: Dp = 50.dp,
    cornerRadius: Dp = 16.dp,
    fallbackIcon: ImageVector? = null
) {
    val gradientColors: List<Color>
    val afterLedgeColor: Color
    val dropShadowColor: Color
    val innerBottomShadowColor: Color

    when (type) {
        Task3DIconType.Video -> {
            gradientColors = listOf(
                Color(0xFF60A5FA),
                Color(0xFF2563EB),
                Color(0xFF1D4ED8)
            )
            afterLedgeColor = Color(0xFF1D4ED8)
            dropShadowColor = Color(0xFF2563EB)
            innerBottomShadowColor = Color(0xFF1E3A8A)
        }
        Task3DIconType.Feedback -> {
            gradientColors = listOf(
                Color(0xFFFCD34D),
                Color(0xFFF59E0B),
                Color(0xFFD97706)
            )
            afterLedgeColor = Color(0xFFD97706)
            dropShadowColor = Color(0xFFF59E0B)
            innerBottomShadowColor = Color(0xFF92400E)
        }
        Task3DIconType.Checklist -> {
            gradientColors = listOf(
                Color(0xFFA78BFA),
                Color(0xFF7C3AED),
                Color(0xFF6D28D9)
            )
            afterLedgeColor = Color(0xFF6D28D9)
            dropShadowColor = Color(0xFF7C3AED)
            innerBottomShadowColor = Color(0xFF4C1D95)
        }
        Task3DIconType.Other -> {
            gradientColors = listOf(
                Color(0xFFFF7E5F),
                Color(0xFFFF6B35),
                Color(0xFFE11D48)
            )
            afterLedgeColor = Color(0xFFBE123C)
            dropShadowColor = Color(0xFFF43F5E)
            innerBottomShadowColor = Color(0xFF881337)
        }
    }

    val scaleRatio = boxSize / 50.dp

    // Outer unclipped container so ::after 3D bottom ledge and diffused colored drop shadow render freely
    Box(
        modifier = modifier.size(boxSize),
        contentAlignment = Alignment.Center
    ) {
        // 1. Diffused colored 3D drop shadow — multi-stop radial gradient fading smoothly without clipping
        Canvas(
            modifier = Modifier
                .size(boxSize * 1.45f)
                .offset(y = 5.dp * scaleRatio)
        ) {
            val r = size.minDimension * 0.48f
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to dropShadowColor.copy(alpha = 0.24f),
                        0.45f to dropShadowColor.copy(alpha = 0.11f),
                        0.78f to dropShadowColor.copy(alpha = 0.03f),
                        1.0f to Color.Transparent
                    ),
                    center = center,
                    radius = r
                ),
                radius = r,
                center = center
            )
        }

        // 2. .task-icon::after — soft 3D under-lip protruding slightly below the icon bottom edge
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 7.5.dp * scaleRatio)
                .fillMaxWidth()
                .height(6.dp * scaleRatio)
                .offset(y = 3.2.dp * scaleRatio)
                .clip(
                    RoundedCornerShape(
                        topStart = 2.dp * scaleRatio,
                        topEnd = 2.dp * scaleRatio,
                        bottomStart = 10.dp * scaleRatio,
                        bottomEnd = 10.dp * scaleRatio
                    )
                )
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            afterLedgeColor.copy(alpha = 0.34f),
                            afterLedgeColor.copy(alpha = 0.08f)
                        )
                    )
                )
        )

        // 3. Main 50x50dp 3D Squircle Body
        val squircleShape = RoundedCornerShape(cornerRadius)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = 6.dp * scaleRatio,
                    shape = squircleShape,
                    ambientColor = dropShadowColor.copy(alpha = 0.22f),
                    spotColor = dropShadowColor.copy(alpha = 0.28f)
                )
                .clip(squircleShape)
                .background(
                    Brush.linearGradient(
                        colorStops = arrayOf(
                            0.0f to gradientColors[0],
                            0.48f to gradientColors[1],
                            1.0f to gradientColors[2]
                        ),
                        start = Offset.Zero,
                        end = Offset(140f, 160f)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // 3a. Inset 3D shading:
            // - inset 0 -5px 8px rgba(..., 0.28) (bottom 3D cushion shadow)
            // - inset 0 2px 2px rgba(255,255,255,0.42) (top 3D specular rim)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cr = cornerRadius.toPx()

                // Bottom inner 3D bevel shadow (inset 0 -5px 8px)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            innerBottomShadowColor.copy(alpha = 0.14f),
                            innerBottomShadowColor.copy(alpha = 0.36f)
                        ),
                        startY = h * 0.66f,
                        endY = h
                    ),
                    cornerRadius = CornerRadius(cr, cr)
                )

                // Top inner specular rim highlight (inset 0 2px 2px rgba(255,255,255,0.44))
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.48f),
                            Color.White.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = 4.dp.toPx()
                    ),
                    cornerRadius = CornerRadius(cr, cr)
                )

                // Subtle 1dp inner glass border for 3D pillow contour
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.42f),
                            Color.White.copy(alpha = 0.08f),
                            innerBottomShadowColor.copy(alpha = 0.30f)
                        )
                    ),
                    cornerRadius = CornerRadius(cr, cr),
                    style = Stroke(width = 1.1.dp.toPx())
                )
            }

            // 3b. .task-icon::before — Top-left glossy reflection window:
            // top: 3px; left: 5px; width: 70%; height: 35%; border-radius: 12px;
            // background: linear-gradient(180deg, rgba(255,255,255,0.40), rgba(255,255,255,0));
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 5.dp * scaleRatio, y = 3.dp * scaleRatio)
                    .width(boxSize * 0.70f)
                    .height(boxSize * 0.35f)
                    .clip(RoundedCornerShape(12.dp * scaleRatio))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.43f),
                                Color.White.copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // 3c. Crisp Lucide Vector Glyph (25x25dp, stroke-width 2.2, filter: drop-shadow(0 2px 2px rgba(0,0,0,0.16)))
            if (type == Task3DIconType.Other && fallbackIcon != null) {
                androidx.compose.material3.Icon(
                    imageVector = fallbackIcon,
                    contentDescription = null,
                    tint = Color.Black.copy(alpha = 0.18f),
                    modifier = Modifier
                        .size(25.dp * scaleRatio)
                        .offset(y = 1.8.dp * scaleRatio)
                )
                androidx.compose.material3.Icon(
                    imageVector = fallbackIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(25.dp * scaleRatio)
                )
            } else {
                Canvas(
                    modifier = Modifier.size(25.dp * scaleRatio)
                ) {
                    val strokePx = 2.2.dp.toPx() * scaleRatio.coerceAtLeast(0.85f)
                    val dropOffsetPx = 1.8.dp.toPx() * scaleRatio
                    val sx = size.width / 24f
                    val sy = size.height / 24f

                    fun drawGlyph(tint: Color, dy: Float) {
                        withTransform({
                            translate(left = 0f, top = dy)
                        }) {
                            val strokeStyle = Stroke(
                                width = strokePx,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                            when (type) {
                                Task3DIconType.Video -> {
                                    // Lucide play-circle: circle(cx=12, cy=12, r=10) + polygon(10 8, 16 12, 10 16)
                                    drawCircle(
                                        color = tint,
                                        radius = 9.6f * sx,
                                        center = Offset(12f * sx, 12f * sy),
                                        style = strokeStyle
                                    )
                                    val playPath = Path().apply {
                                        moveTo(10.2f * sx, 8.2f * sy)
                                        lineTo(15.8f * sx, 12.0f * sy)
                                        lineTo(10.2f * sx, 15.8f * sy)
                                        close()
                                    }
                                    drawPath(
                                        path = playPath,
                                        color = tint,
                                        style = strokeStyle
                                    )
                                }
                                Task3DIconType.Feedback -> {
                                    // Lucide star: polygon(12 2, 15.09 8.26, 22 9.27, 17 14.14, 18.18 21.02, 12 17.77, 5.82 21.02, 7 14.14, 2 9.27, 8.91 8.26)
                                    val starPath = Path().apply {
                                        moveTo(12.0f * sx, 2.3f * sy)
                                        lineTo(15.09f * sx, 8.26f * sy)
                                        lineTo(21.7f * sx, 9.27f * sy)
                                        lineTo(16.85f * sx, 14.14f * sy)
                                        lineTo(18.0f * sx, 20.7f * sy)
                                        lineTo(12.0f * sx, 17.6f * sy)
                                        lineTo(6.0f * sx, 20.7f * sy)
                                        lineTo(7.15f * sx, 14.14f * sy)
                                        lineTo(2.3f * sx, 9.27f * sy)
                                        lineTo(8.91f * sx, 8.26f * sy)
                                        close()
                                    }
                                    drawPath(
                                        path = starPath,
                                        color = tint,
                                        style = strokeStyle
                                    )
                                }
                                Task3DIconType.Checklist, Task3DIconType.Other -> {
                                    // Lucide list-check:
                                    // M16 6H3, M16 12H3, M11 18H3, m15 18 2 2 4-4
                                    drawLine(
                                        color = tint,
                                        start = Offset(3.5f * sx, 6.5f * sy),
                                        end = Offset(16.0f * sx, 6.5f * sy),
                                        strokeWidth = strokePx,
                                        cap = StrokeCap.Round
                                    )
                                    drawLine(
                                        color = tint,
                                        start = Offset(3.5f * sx, 12.0f * sy),
                                        end = Offset(16.0f * sx, 12.0f * sy),
                                        strokeWidth = strokePx,
                                        cap = StrokeCap.Round
                                    )
                                    drawLine(
                                        color = tint,
                                        start = Offset(3.5f * sx, 17.5f * sy),
                                        end = Offset(10.8f * sx, 17.5f * sy),
                                        strokeWidth = strokePx,
                                        cap = StrokeCap.Round
                                    )
                                    val checkPath = Path().apply {
                                        moveTo(14.5f * sx, 17.2f * sy)
                                        lineTo(16.7f * sx, 19.4f * sy)
                                        lineTo(20.8f * sx, 15.2f * sy)
                                    }
                                    drawPath(
                                        path = checkPath,
                                        color = tint,
                                        style = strokeStyle
                                    )
                                }
                            }
                        }
                    }

                    // 1. SVG drop-shadow(0 2px 2px rgba(0,0,0,0.16))
                    drawGlyph(tint = Color.Black.copy(alpha = 0.20f), dy = dropOffsetPx)
                    // 2. Crisp foreground white stroke
                    drawGlyph(tint = Color.White, dy = 0f)
                }
            }
        }
    }
}

/**
 * Exact 3D replica of `.check-icon` from `splash.css` and `image_ab88d470.png`:
 * - `width: 31px; height: 31px; border-radius: 50%;`
 * - `background: linear-gradient(145deg, #4ADE80, #22C55E);`
 * - `box-shadow: 0 8px 16px rgba(34,197,94,0.24), inset 0 1px 2px rgba(255,255,255,0.52);`
 */
@Composable
fun Glossy3DCheckBadge(
    modifier: Modifier = Modifier,
    badgeSize: Dp = 31.dp
) {
    val scaleRatio = badgeSize / 31.dp
    Box(
        modifier = modifier.size(badgeSize),
        contentAlignment = Alignment.Center
    ) {
        // Unclipped soft green 3D drop shadow (0 8px 16px rgba(34,197,94,0.28))
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF22C55E).copy(alpha = 0.34f),
                        Color(0xFF22C55E).copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.72f),
                    radius = size.width * 0.72f
                ),
                center = Offset(size.width * 0.5f, size.height * 0.72f),
                radius = size.width * 0.72f
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = 6.dp * scaleRatio,
                    shape = CircleShape,
                    ambientColor = Color(0xFF22C55E).copy(alpha = 0.30f),
                    spotColor = Color(0xFF22C55E).copy(alpha = 0.40f)
                )
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF4ADE80), Color(0xFF22C55E)),
                        start = Offset.Zero,
                        end = Offset(90f, 90f)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Top inner highlight (inset 0 1px 2px rgba(255,255,255,0.52)) + crisp checkmark with subtle shadow
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.52f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = size.height * 0.38f
                    )
                )
                drawCircle(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.48f),
                            Color.Transparent,
                            Color(0xFF15803D).copy(alpha = 0.25f)
                        )
                    ),
                    style = Stroke(width = 1.dp.toPx())
                )

                val strokePx = 2.6.dp.toPx() * scaleRatio.coerceAtLeast(0.85f)
                val shadowDy = 1.2.dp.toPx()
                val w = size.width
                val h = size.height
                val checkPath = Path().apply {
                    moveTo(w * 0.30f, h * 0.52f)
                    lineTo(w * 0.45f, h * 0.67f)
                    lineTo(w * 0.72f, h * 0.36f)
                }
                // Subtle shadow under check stroke
                withTransform({ translate(top = shadowDy) }) {
                    drawPath(
                        path = checkPath,
                        color = Color.Black.copy(alpha = 0.16f),
                        style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
                drawPath(
                    path = checkPath,
                    color = Color.White,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}

