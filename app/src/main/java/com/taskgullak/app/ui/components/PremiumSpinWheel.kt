package com.taskgullak.app.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

data class SpinWheelSlice(
    val label: String,
    val rewardValue: Int,
    val backgroundColor: Color,
    val textColor: Color = Color.White
)

val DefaultBalancedWheelSlices = listOf(
    SpinWheelSlice("₹5", 5, Color(0xFF4F46E5)),    // Royal Indigo
    SpinWheelSlice("₹10", 10, Color(0xFF0D9488)),  // Balanced Emerald Teal
    SpinWheelSlice("₹2", 2, Color(0xFF7C3AED)),    // Vivid Purple
    SpinWheelSlice("₹25", 25, Color(0xFFD97706)),  // Gullak Amber Gold
    SpinWheelSlice("₹1", 1, Color(0xFF2563EB)),    // Sapphire Blue
    SpinWheelSlice("₹15", 15, Color(0xFFE11D48)),  // Warm Rose
    SpinWheelSlice("₹8", 8, Color(0xFF059669)),    // Deep Emerald
    SpinWheelSlice("₹50", 50, Color(0xFF9333EA))   // Amethyst Violet
)

@Composable
fun PremiumSpinWheel(
    slices: List<SpinWheelSlice> = DefaultBalancedWheelSlices,
    wheelSize: Dp = 290.dp,
    enabled: Boolean = true,
    onSpinCompleted: (SpinWheelSlice) -> Unit
) {
    val scope = rememberCoroutineScope()
    val rotation = remember { Animatable(0f) }
    var isSpinning by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier.size(wheelSize + 24.dp)
        ) {
            // Wheel Canvas
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(wheelSize)
                    .align(Alignment.Center)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val diameter = min(size.width, size.height)
                    val radius = diameter / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val sweepAngle = 360f / slices.size

                    // Outer Metallic Gold Rim
                    drawCircle(
                        brush = Brush.sweepGradient(
                            listOf(
                                Color(0xFFFDE68A),
                                Color(0xFFD97706),
                                Color(0xFFF59E0B),
                                Color(0xFFFDE68A)
                            )
                        ),
                        radius = radius,
                        center = center
                    )

                    val innerRadius = radius * 0.92f
                    val arcTopLeft = Offset(center.x - innerRadius, center.y - innerRadius)
                    val arcSize = Size(innerRadius * 2f, innerRadius * 2f)

                    val textPaint = Paint().apply {
                        isAntiAlias = true
                        textAlign = Paint.Align.CENTER
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textSize = innerRadius * 0.135f
                    }

                    slices.forEachIndexed { index, slice ->
                        val startAngle = rotation.value + index * sweepAngle - 90f

                        // Slice Sector
                        drawArc(
                            color = slice.backgroundColor,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true,
                            topLeft = arcTopLeft,
                            size = arcSize
                        )

                        // Crisp White/Gold Separator Line between slices
                        drawArc(
                            color = Color.White.copy(alpha = 0.85f),
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true,
                            topLeft = arcTopLeft,
                            size = arcSize,
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // Non-overlapping Label placed at 65% radius along the slice center angle
                        val midAngleDeg = startAngle + sweepAngle / 2f
                        val midAngleRad = Math.toRadians(midAngleDeg.toDouble())
                        val labelRadius = innerRadius * 0.65f
                        val labelX = center.x + (labelRadius * cos(midAngleRad)).toFloat()
                        val labelY = center.y + (labelRadius * sin(midAngleRad)).toFloat()

                        drawContext.canvas.nativeCanvas.apply {
                            save()
                            translate(labelX, labelY)
                            rotate(midAngleDeg + 90f)
                            textPaint.color = slice.textColor.toArgb()
                            drawText(slice.label, 0f, textPaint.textSize * 0.35f, textPaint)
                            restore()
                        }
                    }
                }

                // Center Spin Hub Button
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1E1B4B),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .border(3.dp, Color(0xFFFBBF24), CircleShape)
                        .clickable(enabled = enabled && !isSpinning) {
                            scope.launch {
                                isSpinning = true
                                val targetIndex = slices.indices.random()
                                val sliceAngle = 360f / slices.size
                                val extraSpins = 360f * 6
                                val targetAngle = extraSpins + (360f - (targetIndex * sliceAngle + sliceAngle / 2f))
                                rotation.animateTo(
                                    targetValue = rotation.value + targetAngle,
                                    animationSpec = tween(
                                        durationMillis = 3600,
                                        easing = FastOutSlowInEasing
                                    )
                                )
                                isSpinning = false
                                onSpinCompleted(slices[targetIndex])
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isSpinning) "..." else "SPIN",
                            color = Color(0xFFFDE68A),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // Top Gold Pointer Triangle
            Canvas(
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.TopCenter)
            ) {
                val path = Path().apply {
                    moveTo(size.width / 2f, size.height)
                    lineTo(0f, 0f)
                    lineTo(size.width, 0f)
                    close()
                }
                drawPath(path = path, color = Color(0xFFF59E0B))
            }
        }
    }
}
