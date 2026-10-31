package com.taskgullak.app.ui.components

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskgullak.app.ui.theme.InterFontFamily
import com.taskgullak.app.ui.theme.PoppinsFontFamily

/**
 * Scalloped Ticket Cutout Shape matching timer-price (1).html with top and bottom semi-circle notches.
 */
val TicketCutoutShape = GenericShape { size, _ ->
    val scaleFactor = size.height / 38f
    val cornerRadius = 9f * scaleFactor
    val cz = 28f * scaleFactor
    val notchR = 4.5f * scaleFactor

    // Start top-left
    moveTo(cornerRadius, 0f)
    // Line to top notch
    lineTo(cz - notchR, 0f)
    // Arc top notch inward
    arcTo(
        rect = Rect(cz - notchR, -notchR, cz + notchR, notchR),
        startAngleDegrees = 180f,
        sweepAngleDegrees = -180f,
        forceMoveTo = false
    )
    // Line to top-right
    lineTo(size.width - cornerRadius, 0f)
    // Top-right corner
    arcTo(
        rect = Rect(size.width - 2 * cornerRadius, 0f, size.width, 2 * cornerRadius),
        startAngleDegrees = 270f,
        sweepAngleDegrees = 90f,
        forceMoveTo = false
    )
    // Line to bottom-right
    lineTo(size.width, size.height - cornerRadius)
    // Bottom-right corner
    arcTo(
        rect = Rect(size.width - 2 * cornerRadius, size.height - 2 * cornerRadius, size.width, size.height),
        startAngleDegrees = 0f,
        sweepAngleDegrees = 90f,
        forceMoveTo = false
    )
    // Line to bottom notch
    lineTo(cz + notchR, size.height)
    // Arc bottom notch inward
    arcTo(
        rect = Rect(cz - notchR, size.height - notchR, cz + notchR, size.height + notchR),
        startAngleDegrees = 0f,
        sweepAngleDegrees = -180f,
        forceMoveTo = false
    )
    // Line to bottom-left
    lineTo(cornerRadius, size.height)
    // Bottom-left corner
    arcTo(
        rect = Rect(0f, size.height - 2 * cornerRadius, 2 * cornerRadius, size.height),
        startAngleDegrees = 90f,
        sweepAngleDegrees = 90f,
        forceMoveTo = false
    )
    // Line to top-left
    lineTo(0f, cornerRadius)
    // Top-left corner
    arcTo(
        rect = Rect(0f, 0f, 2 * cornerRadius, 2 * cornerRadius),
        startAngleDegrees = 180f,
        sweepAngleDegrees = 90f,
        forceMoveTo = false
    )
    close()
}

/**
 * Watch Ring Canvas matching timer-price (1).html:
 * - Rotating gear wheel (counter-clockwise)
 * - Orbiting bead
 * - Sweeping minute hand
 * - Breathing watch face
 * - Gradient progress arc
 */
@Composable
fun WatchRingCanvas(
    isOn: Boolean,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "watch_anim")

    val gearAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gear_angle"
    )

    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_angle"
    )

    val minuteAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "minute_angle"
    )

    val faceScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "face_scale"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val scaleFactor = w / 48f

        val ringGrad = Brush.linearGradient(
            colors = listOf(Color(0xFFF35FB8), Color(0xFFB65EF0), Color(0xFF3F48EA)),
            start = Offset(7f * scaleFactor, 41f * scaleFactor),
            end = Offset(41f * scaleFactor, 7f * scaleFactor)
        )

        val clockGrad = Brush.linearGradient(
            colors = listOf(Color(0xFFB369F3), Color(0xFF6547E6)),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        )

        // 1. Background circle
        drawCircle(
            color = Color(0xFFF8EFFC),
            radius = 18.5f * scaleFactor,
            center = Offset(cx, cy)
        )

        // 2. Outer track circle
        drawCircle(
            color = Color(0xFFEADCF5),
            radius = 21f * scaleFactor,
            center = Offset(cx, cy),
            style = Stroke(width = 5f * scaleFactor)
        )

        // 3. Progress arc
        val sweep = 360f * progress.coerceIn(0f, 1f)
        drawArc(
            brush = ringGrad,
            startAngle = -90f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(cx - 21f * scaleFactor, cy - 21f * scaleFactor),
            size = Size(42f * scaleFactor, 42f * scaleFactor),
            style = Stroke(width = 5f * scaleFactor, cap = StrokeCap.Round)
        )

        // 4. Rotating Gear
        val currentGearAngle = if (isOn) gearAngle else 0f
        withTransform({
            rotate(currentGearAngle, pivot = Offset(cx, cy))
        }) {
            drawCircle(
                color = Color(0xFFB65EF0).copy(alpha = 0.55f),
                radius = 14.5f * scaleFactor,
                center = Offset(cx, cy),
                style = Stroke(
                    width = 1.3f * scaleFactor,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(1.4f * scaleFactor, 3.1f * scaleFactor)
                    )
                )
            )
        }

        // 5. Orbiting Pink Bead
        val currentOrbitAngle = if (isOn) orbitAngle else 0f
        withTransform({
            rotate(currentOrbitAngle, pivot = Offset(cx, cy))
        }) {
            drawCircle(
                color = Color(0xFFF35FB8),
                radius = 1.5f * scaleFactor,
                center = Offset(cx, cy - 14.5f * scaleFactor)
            )
        }

        // 6. Breathing Watch Face
        val currentFaceScale = if (isOn) faceScale else 1f
        withTransform({
            scale(currentFaceScale, currentFaceScale, pivot = Offset(cx, cy))
        }) {
            drawCircle(
                brush = clockGrad,
                radius = 10.5f * scaleFactor,
                center = Offset(cx, cy)
            )

            // Hour hand (rotating smoothly when timer is on)
            val currentHourAngle = if (isOn) minuteAngle * 0.12f else 0f
            withTransform({
                rotate(currentHourAngle, pivot = Offset(cx, cy))
            }) {
                drawLine(
                    color = Color.White,
                    start = Offset(cx, cy),
                    end = Offset(cx + 2.8f * scaleFactor, cy + 1.6f * scaleFactor),
                    strokeWidth = 2f * scaleFactor,
                    cap = StrokeCap.Round
                )
            }

            // Sweeping Minute hand
            val currentMinAngle = if (isOn) minuteAngle else 0f
            withTransform({
                rotate(currentMinAngle, pivot = Offset(cx, cy))
            }) {
                drawLine(
                    color = Color.White,
                    start = Offset(cx, cy + 2f * scaleFactor),
                    end = Offset(cx, cy - 7f * scaleFactor),
                    strokeWidth = 1.5f * scaleFactor,
                    cap = StrokeCap.Round
                )
            }

            // Center white dot
            drawCircle(
                color = Color.White,
                radius = 1.4f * scaleFactor,
                center = Offset(cx, cy)
            )
        }
    }
}

/**
 * Animated Timer Pill matching timer-price (1).html exactly.
 */
@Composable
fun AppTimerPill(
    displayTime: String,
    modifier: Modifier = Modifier,
    isOn: Boolean = true,
    progress: Float = 1.0f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "timer_pill_blink_transition")

    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.00f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "timer_blink_alpha"
    )

    val currentBlink = if (isOn) blinkAlpha else 1.0f

    Box(
        modifier = modifier
            .drawBehind {
                if (isOn) {
                    val cornerRadiusPx = 999.dp.toPx()
                    val outerGlowColor = Color(0xFFD946EF).copy(alpha = currentBlink * 0.40f)
                    drawRoundRect(
                        color = outerGlowColor,
                        topLeft = Offset(-5.dp.toPx(), -5.dp.toPx()),
                        size = Size(size.width + 10.dp.toPx(), size.height + 10.dp.toPx()),
                        cornerRadius = CornerRadius(cornerRadiusPx)
                    )
                    val innerGlowColor = Color(0xFFA855F7).copy(alpha = currentBlink * 0.55f)
                    drawRoundRect(
                        color = innerGlowColor,
                        topLeft = Offset(-2.5.dp.toPx(), -2.5.dp.toPx()),
                        size = Size(size.width + 5.dp.toPx(), size.height + 5.dp.toPx()),
                        cornerRadius = CornerRadius(cornerRadiusPx)
                    )
                }
            }
            .width(106.dp)
            .height(36.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFF7EAFB), Color(0xFFEFE1F8))
                )
            )
            .border(
                width = if (isOn) 1.8.dp else 1.5.dp,
                brush = if (isOn) {
                    Brush.linearGradient(
                        listOf(
                            Color(0xFFE879F9).copy(alpha = currentBlink),
                            Color(0xFFC084FC).copy(alpha = currentBlink),
                            Color(0xFF8B5CF6).copy(alpha = currentBlink)
                        )
                    )
                } else {
                    SolidColor(Color(0xFFE7D4F4))
                },
                shape = RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            WatchRingCanvas(
                isOn = isOn,
                progress = progress,
                modifier = Modifier.size(28.dp)
            )

            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = displayTime,
                    color = Color(0xFF14134A).copy(alpha = if (isOn) (0.45f + currentBlink * 0.55f) else 1.0f),
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
                Text(
                    text = "Minutes",
                    color = Color(0xFF6E73A3).copy(alpha = if (isOn) (0.55f + currentBlink * 0.45f) else 1.0f),
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 9.sp,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                )
            }
        }
    }
}

/**
 * Gold-Orange Reward Ticket Price Badge matching timer-price (1).html exactly.
 */
@Composable
fun GoldenTicketPriceCard(
    rewardInr: Any,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(106.dp)
            .height(38.dp)
            .shadow(
                elevation = 3.dp,
                shape = TicketCutoutShape,
                spotColor = Color(0xFF8A4D00).copy(alpha = 0.35f),
                ambientColor = Color(0xFFFF9A0A).copy(alpha = 0.20f)
            )
            .clip(TicketCutoutShape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFFFE680), Color(0xFFFFBF24), Color(0xFFFF9A0A))
                )
            )
    ) {
        // Top gloss shine overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.48f)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.04f))
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Coin Container (28.dp width)
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                // Golden Coin SVG drawing
                Canvas(modifier = Modifier.size(21.dp)) {
                    val r = size.width / 2f
                    drawCircle(color = Color(0xFFE29A00), radius = r)
                    drawCircle(color = Color(0xFFFFE08A), radius = r * 0.95f)
                    drawCircle(color = Color(0xFFF2B100), radius = r * 0.70f)
                    drawCircle(
                        color = Color(0xFFFFD04D),
                        radius = r * 0.70f,
                        style = Stroke(width = 1.2f)
                    )
                }
                Text(
                    text = "₹",
                    color = Color(0xFF3B2000),
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                )
            }

            // Ticket Dashed Divider Line
            Canvas(
                modifier = Modifier
                    .height(22.dp)
                    .width(1.5.dp)
            ) {
                drawLine(
                    color = Color(0xFF8A4D00).copy(alpha = 0.45f),
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 2.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.5f, 3.5f), 0f)
                )
            }

            // Right Text Column (PRICE & Amount)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 2.dp, end = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "PRICE",
                    color = Color(0xFF8A4D00),
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 8.sp,
                    letterSpacing = 0.12.sp,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                )
                Text(
                    text = "₹$rewardInr",
                    color = Color(0xFF4D2500),
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }
    }
}
