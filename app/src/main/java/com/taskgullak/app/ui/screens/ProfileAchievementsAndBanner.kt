package com.taskgullak.app.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.taskgullak.app.data.remote.AchievementBatchInfo
import com.taskgullak.app.ui.theme.InterFontFamily
import com.taskgullak.app.ui.theme.PoppinsFontFamily
import kotlinx.coroutines.delay

data class ProfileBannerPreset(
    val id: String,
    val label: String,
    val colors: List<Color>,
    val accentPetalColor: Color
)

val PROFILE_BANNER_PRESETS: List<ProfileBannerPreset> = listOf(
    ProfileBannerPreset(
        id = "banner_sky_blossom",
        label = "Sky Blossom",
        colors = listOf(Color(0xFFDbeafe), Color(0xFFEDE9FE), Color(0xFFFCE7F3)),
        accentPetalColor = Color(0xFFF472B6)
    ),
    ProfileBannerPreset(
        id = "banner_mint_meadow",
        label = "Mint Meadow",
        colors = listOf(Color(0xFFD1FAE5), Color(0xFFE0F2FE), Color(0xFFFEF3C7)),
        accentPetalColor = Color(0xFF34D399)
    ),
    ProfileBannerPreset(
        id = "banner_sunset_peach",
        label = "Sunset Peach",
        colors = listOf(Color(0xFFFFEDD5), Color(0xFFFCE7F3), Color(0xFFEDE9FE)),
        accentPetalColor = Color(0xFFFB923C)
    ),
    ProfileBannerPreset(
        id = "banner_royal_lavender",
        label = "Royal Lavender",
        colors = listOf(Color(0xFFDDD6FE), Color(0xFFC7D2FE), Color(0xFFE0E7FF)),
        accentPetalColor = Color(0xFF818CF8)
    ),
    ProfileBannerPreset(
        id = "banner_golden_dawn",
        label = "Golden Dawn",
        colors = listOf(Color(0xFFFEF3C7), Color(0xFFFFEDD5), Color(0xFFFEE2E2)),
        accentPetalColor = Color(0xFFFBBF24)
    ),
    ProfileBannerPreset(
        id = "banner_ocean_breeze",
        label = "Ocean Breeze",
        colors = listOf(Color(0xFFCFFAFE), Color(0xFFDBEAFE), Color(0xFFE0E7FF)),
        accentPetalColor = Color(0xFF38BDF8)
    )
)

fun findProfileBannerPreset(bannerId: String): ProfileBannerPreset {
    return PROFILE_BANNER_PRESETS.firstOrNull { it.id == bannerId } ?: PROFILE_BANNER_PRESETS.first()
}

/**
 * Simple, clean pastel Profile Banner with soft clouds, sparkles, and "Edit Banner" button
 * matching Image 3.
 */
@Composable
fun EditableProfilePastelBanner(
    bannerId: String,
    onEditBannerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val preset = remember(bannerId) { findProfileBannerPreset(bannerId) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(126.dp)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
            .background(Brush.linearGradient(colors = preset.colors))
            .testTag("profile_pastel_banner")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Soft dreamy pastel clouds & floral circles
            drawCircle(
                color = Color.White.copy(alpha = 0.45f),
                radius = h * 0.55f,
                center = Offset(w * 0.16f, h * 0.85f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.38f),
                radius = h * 0.48f,
                center = Offset(w * 0.84f, h * 0.25f)
            )
            drawCircle(
                color = preset.accentPetalColor.copy(alpha = 0.18f),
                radius = h * 0.42f,
                center = Offset(w * 0.52f, h * 0.18f)
            )

            // Subtle 4-point sparkles
            val sparkleCenters = listOf(
                Offset(w * 0.22f, h * 0.30f),
                Offset(w * 0.72f, h * 0.62f),
                Offset(w * 0.36f, h * 0.68f)
            )
            sparkleCenters.forEach { c ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = 4.5f,
                    center = c
                )
            }
        }

        // Top-right "Edit Banner" rounded-corner box button
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.94f))
                .border(1.dp, Color(0xFFC7D2FE), RoundedCornerShape(10.dp))
                .clickable { onEditBannerClick() }
                .padding(horizontal = 11.dp, vertical = 6.dp)
                .testTag("profile_edit_banner_button"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit Banner",
                tint = Color(0xFF4F46E5),
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = "Edit Banner",
                color = Color(0xFF1E1B4B),
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }
    }
}

/**
 * Featured Task Star / Current Achievement Batch Card with Progress Bar & Flower Unlock trigger
 * matching Image 3.
 */
@Composable
fun FeaturedAchievementBatchCard(
    currentBatch: AchievementBatchInfo,
    completedTasksCount: Int,
    unlockedBatchesCount: Int,
    onTriggerFlowerUnlock: (AchievementBatchInfo) -> Unit,
    onOpenAllBatches: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(22.dp))
            .testTag("featured_achievement_batch_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFFBEB),
                            Color(0xFFFFFFFF)
                        )
                    )
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Task Achievement Batch",
                        color = Color(0xFF92400E),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFFFEF3C7))
                        .clickable { onOpenAllBatches() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("view_all_50_batches_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "$unlockedBatchesCount/50 Batches",
                        color = Color(0xFFB45309),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFFB45309),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Golden Shield Star Badge + Blooming Flower Preview
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clickable {
                        if (currentBatch.isUnlocked) {
                            onTriggerFlowerUnlock(currentBatch)
                        } else {
                            onOpenAllBatches()
                        }
                    }
                    .testTag("featured_batch_shield_icon"),
                contentAlignment = Alignment.Center
            ) {
                GoldenStarShieldCanvas(
                    batchNumber = currentBatch.batchNumber,
                    isUnlocked = currentBatch.isUnlocked,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Batch ${currentBatch.batchNumber}: ${currentBatch.title}",
                color = Color(0xFF1F2937),
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp
            )

            Text(
                text = currentBatch.subtitle,
                color = Color(0xFF6B7280),
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.5.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            val displayCompleted = completedTasksCount.coerceIn(0, currentBatch.requiredTasks)
            val progress = (displayCompleted.toFloat() / currentBatch.requiredTasks.toFloat()).coerceIn(0f, 1f)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    color = Color(0xFF16A34A),
                    trackColor = Color(0xFFE5E7EB),
                    modifier = Modifier
                        .weight(1f)
                        .height(9.dp)
                        .clip(RoundedCornerShape(999.dp))
                )
                Text(
                    text = "$completedTasksCount/${currentBatch.requiredTasks}",
                    color = Color(0xFF15803D),
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.5.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFEC4899),
                                Color(0xFFF59E0B)
                            )
                        )
                    )
                    .clickable { onTriggerFlowerUnlock(currentBatch) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
                    .testTag("play_flower_unlock_animation_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Play Flower Unlock Animation 🌸",
                    color = Color.White,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp
                )
            }
        }
    }
}

/**
 * 3-Stage Flower Unlock Animation Dialog matching the bottom of Image 3:
 * Stage 1: "1. Unlocking..." (Closed glowing lotus bud)
 * Stage 2: "2. Flower Blooms: Achievement Unlocked!" (Blooming petals + sparkles & confetti)
 * Stage 3: "3. Final View: Batch N - Task Star" (Full lotus + Golden Shield Badge)
 */
@Composable
fun FlowerUnlockAnimationDialog(
    batch: AchievementBatchInfo,
    onDismiss: () -> Unit
) {
    var stage by remember { mutableIntStateOf(1) }

    LaunchedEffect(batch.batchNumber) {
        stage = 1
        delay(950)
        stage = 2
        delay(1250)
        stage = 3
    }

    val bloomProgress by animateFloatAsState(
        targetValue = when (stage) {
            1 -> 0.22f
            2 -> 0.88f
            else -> 1.0f
        },
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "flower_bloom_progress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "flower_aura")
    val auraPulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_pulse"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("flower_unlock_animation_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFDF2F8),
                                Color(0xFFFFFBEB),
                                Color.White
                            )
                        )
                    )
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Stage Stepper Pills (1. Unlocking -> 2. Flower Blooms -> 3. Final View)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StageIndicatorChip(step = 1, label = "1. Unlocking", activeStage = stage)
                    StageIndicatorChip(step = 2, label = "2. Blooms", activeStage = stage)
                    StageIndicatorChip(step = 3, label = "3. Unlocked", activeStage = stage)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Blooming Flower + Golden Shield Canvas
                Box(
                    modifier = Modifier.size(170.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val center = Offset(w / 2f, h / 2f)

                        // Golden-Pink Radial Glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFF472B6).copy(alpha = 0.32f * auraPulse),
                                    Color(0xFFFBBF24).copy(alpha = 0.22f * auraPulse),
                                    Color.Transparent
                                )
                            ),
                            radius = (w * 0.48f) * auraPulse,
                            center = center
                        )

                        // Confetti Sparkles in Stages 2 & 3
                        if (stage >= 2) {
                            val confettiColors = listOf(
                                Color(0xFFEC4899),
                                Color(0xFFF59E0B),
                                Color(0xFF10B981),
                                Color(0xFF8B5CF6),
                                Color(0xFF3B82F6)
                            )
                            for (i in 0 until 12) {
                                val angleRad = Math.toRadians((i * 30).toDouble())
                                val dist = w * 0.42f * bloomProgress
                                val cx = center.x + (kotlin.math.cos(angleRad) * dist).toFloat()
                                val cy = center.y + (kotlin.math.sin(angleRad) * dist).toFloat()
                                drawCircle(
                                    color = confettiColors[i % confettiColors.size],
                                    radius = 5.5f,
                                    center = Offset(cx, cy)
                                )
                            }
                        }

                        // 8 Blooming Lotus Petals
                        val petalCount = 8
                        for (i in 0 until petalCount) {
                            val deg = (i * (360f / petalCount))
                            rotate(degrees = deg, pivot = center) {
                                val petalWidth = w * (0.16f + 0.10f * bloomProgress)
                                val petalLength = h * (0.20f + 0.22f * bloomProgress)
                                drawOval(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFFF9A8D4),
                                            Color(0xFFEC4899),
                                            Color(0xFFDB2777)
                                        )
                                    ),
                                    topLeft = Offset(
                                        center.x - petalWidth / 2f,
                                        center.y - petalLength
                                    ),
                                    size = Size(petalWidth, petalLength)
                                )
                            }
                        }

                        // Inner Golden-Pink Petals
                        for (i in 0 until petalCount) {
                            val deg = (i * (360f / petalCount)) + 22.5f
                            rotate(degrees = deg, pivot = center) {
                                val petalWidth = w * (0.12f + 0.07f * bloomProgress)
                                val petalLength = h * (0.14f + 0.15f * bloomProgress)
                                drawOval(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFFFDE68A),
                                            Color(0xFFF472B6)
                                        )
                                    ),
                                    topLeft = Offset(
                                        center.x - petalWidth / 2f,
                                        center.y - petalLength
                                    ),
                                    size = Size(petalWidth, petalLength)
                                )
                            }
                        }
                    }

                    // Center Golden Star Shield (scales up in Stage 2 & 3)
                    GoldenStarShieldCanvas(
                        batchNumber = batch.batchNumber,
                        isUnlocked = true,
                        modifier = Modifier.size(if (stage == 1) 52.dp else 78.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = when (stage) {
                        1 -> "Unlocking Milestone..."
                        2 -> "Achievement Unlocked! 🌸"
                        else -> "Batch ${batch.batchNumber}: ${batch.title}"
                    },
                    color = Color(0xFF9D174D),
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${batch.subtitle} • Task Gullak Milestone Badge",
                    color = Color(0xFF6B7280),
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDB2777)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("close_flower_unlock_dialog_button")
                ) {
                    Text(
                        text = "Awesome! Collect Badge ✨",
                        color = Color.White,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StageIndicatorChip(
    step: Int,
    label: String,
    activeStage: Int
) {
    val isActive = activeStage >= step
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (isActive) Color(0xFFFCE7F3) else Color(0xFFF3F4F6))
            .border(
                width = 1.dp,
                color = if (isActive) Color(0xFFF472B6) else Color(0xFFE5E7EB),
                shape = RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = if (isActive) Color(0xFF9D174D) else Color(0xFF9CA3AF),
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp
        )
    }
}

/**
 * Custom Canvas Golden Shield with Star for Achievement Batches (1 to 50).
 */
@Composable
fun GoldenStarShieldCanvas(
    batchNumber: Int,
    isUnlocked: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val shieldPath = Path().apply {
            moveTo(w * 0.50f, h * 0.08f)
            lineTo(w * 0.86f, h * 0.22f)
            lineTo(w * 0.82f, h * 0.60f)
            cubicTo(
                w * 0.78f, h * 0.78f,
                w * 0.62f, h * 0.88f,
                w * 0.50f, h * 0.94f
            )
            cubicTo(
                w * 0.38f, h * 0.88f,
                w * 0.22f, h * 0.78f,
                w * 0.18f, h * 0.60f
            )
            lineTo(w * 0.14f, h * 0.22f)
            close()
        }

        val fillBrush = if (isUnlocked) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFDE047),
                    Color(0xFFF59E0B),
                    Color(0xFFD97706)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFE5E7EB),
                    Color(0xFF9CA3AF)
                )
            )
        }

        drawPath(path = shieldPath, brush = fillBrush)
        drawPath(
            path = shieldPath,
            color = if (isUnlocked) Color(0xFFB45309) else Color(0xFF6B7280),
            style = Stroke(width = 4f)
        )

        // Center Star Circle
        drawCircle(
            color = Color.White.copy(alpha = if (isUnlocked) 0.92f else 0.65f),
            radius = w * 0.20f,
            center = Offset(w * 0.50f, h * 0.48f)
        )
    }
}

/**
 * Modal Bottom Sheet displaying all 50 Achievement Badges (10 to 500 tasks, every 10 tasks = 1 batch).
 * Tapping any badge plays the Flower Unlock Animation!
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsBatchesModalSheet(
    batches: List<AchievementBatchInfo>,
    completedTasksCount: Int,
    onSelectBatchForFlowerAnimation: (AchievementBatchInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(580.dp)
                .padding(horizontal = 18.dp)
                .testTag("achievements_50_batches_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Achievement Badges (Batch 1 – 50)",
                        color = Color(0xFF111827),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Every 10 completed tasks unlocks a new Batch (10 to 500 Tasks) • Tap any batch to bloom 🌸",
                        color = Color(0xFF6B7280),
                        fontFamily = InterFontFamily,
                        fontSize = 11.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF6B7280)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(batches, key = { it.batchNumber }) { batch ->
                    val unlocked = batch.isUnlocked
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (unlocked) Color(0xFFFFFBEB) else Color(0xFFF9FAFB),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (unlocked) Color(0xFFFCD34D) else Color(0xFFE5E7EB),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable(enabled = unlocked) { onSelectBatchForFlowerAnimation(batch) }
                            .testTag("achievement_batch_item_${batch.batchNumber}")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                GoldenStarShieldCanvas(
                                    batchNumber = batch.batchNumber,
                                    isUnlocked = unlocked,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (!unlocked) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = Color(0xFF4B5563),
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Unlocked",
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Batch ${batch.batchNumber}: ${batch.title}",
                                color = Color(0xFF1F2937),
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = batch.subtitle,
                                color = if (unlocked) Color(0xFFB45309) else Color(0xFF6B7280),
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Modal Bottom Sheet for picking a clean pastel Profile Banner or editing @username & Bio.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileBannerAndBioSheet(
    currentBannerId: String,
    currentUsername: String,
    currentBio: String,
    onSaveBannerAndBio: (bannerId: String, username: String, bio: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedBannerId by remember(currentBannerId) { mutableStateOf(currentBannerId) }
    var usernameInput by remember(currentUsername) { mutableStateOf(currentUsername) }
    var bioInput by remember(currentBio) { mutableStateOf(currentBio) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Customize Profile Banner & Bio",
                color = Color(0xFF111827),
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )

            Text(
                text = "Choose a simple, clean pastel banner:",
                color = Color(0xFF4B5563),
                fontFamily = InterFontFamily,
                fontSize = 12.sp
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(136.dp)
            ) {
                items(PROFILE_BANNER_PRESETS, key = { it.id }) { preset ->
                    val isSelected = preset.id == selectedBannerId
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color(0xFF4F46E5) else Color(0xFFE5E7EB),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedBannerId = preset.id }
                            .padding(6.dp)
                            .testTag("banner_preset_${preset.id}"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.linearGradient(preset.colors)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color(0xFF1E1B4B),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = preset.label,
                            color = Color(0xFF374151),
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 9.5.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            OutlinedTextField(
                value = usernameInput,
                onValueChange = { usernameInput = it.take(24) },
                label = { Text("Username (@handle)") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_username_input")
            )

            OutlinedTextField(
                value = bioInput,
                onValueChange = { bioInput = it.take(80) },
                label = { Text("Short Bio") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_bio_input")
            )

            Button(
                onClick = {
                    onSaveBannerAndBio(selectedBannerId, usernameInput, bioInput)
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("save_banner_and_bio_button")
            ) {
                Text(
                    text = "Save Profile Customization",
                    color = Color.White,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
