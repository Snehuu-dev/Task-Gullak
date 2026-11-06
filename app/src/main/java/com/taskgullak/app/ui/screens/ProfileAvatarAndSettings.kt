package com.taskgullak.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.window.Dialog
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import com.taskgullak.app.data.remote.FirebaseEmailDispatchService
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.taskgullak.app.R
import com.taskgullak.app.data.model.SupportTicket
import com.taskgullak.app.ui.components.ShinyGradientButton
import com.taskgullak.app.ui.theme.InterFontFamily
import com.taskgullak.app.ui.theme.PoppinsFontFamily

data class Avatar3DPreset(
    val id: String,
    val title: String,
    val tag: String,
    @DrawableRes val drawableResId: Int,
    val bgColors: List<Color>,
    val skinOrBaseColor: Color,
    val accentColor: Color,
    val visorOrHairColor: Color
)

val CURATED_3D_AVATARS: List<Avatar3DPreset> = listOf(
    Avatar3DPreset(
        id = "avatar_3d_astronaut",
        title = "Sky Goggles",
        tag = "Explorer",
        drawableResId = R.drawable.avatar_3d_1,
        bgColors = listOf(Color(0xFF38BDF8), Color(0xFF0284C7), Color(0xFF0369A1)),
        skinOrBaseColor = Color(0xFFF8FAFC),
        accentColor = Color(0xFF38BDF8),
        visorOrHairColor = Color(0xFF1E1B4B)
    ),
    Avatar3DPreset(
        id = "avatar_3d_cyber_ninja",
        title = "Frost Beanie",
        tag = "Winter",
        drawableResId = R.drawable.avatar_3d_2,
        bgColors = listOf(Color(0xFF0EA5E9), Color(0xFF2563EB), Color(0xFF1D4ED8)),
        skinOrBaseColor = Color(0xFFE2E8F0),
        accentColor = Color(0xFF22D3EE),
        visorOrHairColor = Color(0xFF0F172A)
    ),
    Avatar3DPreset(
        id = "avatar_3d_pixel_gamer",
        title = "Neon Shades",
        tag = "Arcade",
        drawableResId = R.drawable.avatar_3d_3,
        bgColors = listOf(Color(0xFF9333EA), Color(0xFF7E22CE), Color(0xFF6B21A8)),
        skinOrBaseColor = Color(0xFFFDE68A),
        accentColor = Color(0xFFA855F7),
        visorOrHairColor = Color(0xFF3B0764)
    ),
    Avatar3DPreset(
        id = "avatar_3d_chill_panda",
        title = "Panda Hood",
        tag = "Legend",
        drawableResId = R.drawable.avatar_3d_4,
        bgColors = listOf(Color(0xFF6366F1), Color(0xFF4F46E5), Color(0xFF4338CA)),
        skinOrBaseColor = Color(0xFFFFFFFF),
        accentColor = Color(0xFF818CF8),
        visorOrHairColor = Color(0xFF1E293B)
    ),
    Avatar3DPreset(
        id = "avatar_3d_emerald_fox",
        title = "Coral Fin",
        tag = "Clever",
        drawableResId = R.drawable.avatar_3d_5,
        bgColors = listOf(Color(0xFFFB923C), Color(0xFFF97316), Color(0xFFEA580C)),
        skinOrBaseColor = Color(0xFFFB923C),
        accentColor = Color(0xFFFFF7ED),
        visorOrHairColor = Color(0xFF7C2D12)
    ),
    Avatar3DPreset(
        id = "avatar_3d_robo_buddy",
        title = "Cyber Visor",
        tag = "Tech Pro",
        drawableResId = R.drawable.avatar_3d_6,
        bgColors = listOf(Color(0xFFEC4899), Color(0xFFDB2777), Color(0xFFBE185D)),
        skinOrBaseColor = Color(0xFFF1F5F9),
        accentColor = Color(0xFFF472B6),
        visorOrHairColor = Color(0xFF0F172A)
    ),
    Avatar3DPreset(
        id = "avatar_3d_sakura_muse",
        title = "Street Style",
        tag = "Creator",
        drawableResId = R.drawable.avatar_3d_7,
        bgColors = listOf(Color(0xFFF472B6), Color(0xFFEC4899), Color(0xFFDB2777)),
        skinOrBaseColor = Color(0xFFFFE4E6),
        accentColor = Color(0xFFF472B6),
        visorOrHairColor = Color(0xFF831843)
    ),
    Avatar3DPreset(
        id = "avatar_3d_gold_mogul",
        title = "Gullak King",
        tag = "Saver Pro",
        drawableResId = R.drawable.avatar_3d_8,
        bgColors = listOf(Color(0xFFF59E0B), Color(0xFFD97706), Color(0xFFB45309)),
        skinOrBaseColor = Color(0xFFFFEDD5),
        accentColor = Color(0xFFFDE047),
        visorOrHairColor = Color(0xFF78350F)
    ),
    Avatar3DPreset(
        id = "avatar_3d_captain_blaze",
        title = "Royal Prince",
        tag = "Champion",
        drawableResId = R.drawable.avatar_3d_9,
        bgColors = listOf(Color(0xFF10B981), Color(0xFF059669), Color(0xFF047857)),
        skinOrBaseColor = Color(0xFFFED7AA),
        accentColor = Color(0xFFFBBF24),
        visorOrHairColor = Color(0xFF064E3B)
    ),
    Avatar3DPreset(
        id = "avatar_3d_starlight_sage",
        title = "Astro Nova",
        tag = "Cosmic",
        drawableResId = R.drawable.avatar_3d_10,
        bgColors = listOf(Color(0xFF38BDF8), Color(0xFF0284C7), Color(0xFF1D4ED8)),
        skinOrBaseColor = Color(0xFFFEF3C7),
        accentColor = Color(0xFFFACC15),
        visorOrHairColor = Color(0xFF1E1B4B)
    )
)

fun findAvatarPresetById(avatarId: String?): Avatar3DPreset? {
    val clean = avatarId?.trim().orEmpty()
    if (clean.isBlank()) return CURATED_3D_AVATARS.first()
    return CURATED_3D_AVATARS.firstOrNull { it.id == clean } ?: CURATED_3D_AVATARS.first()
}

/**
 * Renders the selected 3D illustrated avatar preset (from the 10 curated 3D profile avatars),
 * used on both the Profile screen and the Home screen top bar greeting circle.
 */
@Composable
fun UserAvatar3DBadge(
    avatarId: String,
    userInitial: String,
    size: Dp,
    showEditBadge: Boolean = false,
    onClick: (() -> Unit)? = null,
    testTag: String? = null
) {
    val preset = remember(avatarId) { findAvatarPresetById(avatarId) ?: CURATED_3D_AVATARS.first() }
    val bgBrush = remember(preset) {
        Brush.linearGradient(colors = preset.bgColors)
    }

    val rootMod = Modifier
        .size(size)
        .let { m -> if (testTag != null) m.testTag(testTag) else m }
        .let { m ->
            if (onClick != null) {
                m.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
            } else {
                m
            }
        }

    Box(
        modifier = rootMod,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(bgBrush),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = preset.drawableResId),
                contentDescription = "${preset.title} profile avatar ($userInitial)",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .scale(1.25f)
            )
        }

        if (showEditBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 3.dp, y = 3.dp)
                    .size(26.dp)
                    .shadow(6.dp, RoundedCornerShape(8.dp))
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
                            ),
                            RoundedCornerShape(6.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Change avatar",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun Avatar3DCharacterCanvas(
    preset: Avatar3DPreset,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Ambient 3D top-left specular sphere highlight
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.35f),
                    Color.Transparent
                ),
                center = Offset(w * 0.30f, h * 0.22f),
                radius = w * 0.50f
            ),
            center = Offset(w * 0.30f, h * 0.22f),
            radius = w * 0.50f
        )

        // 2. 3D Shoulders / Torso bust at bottom
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    preset.accentColor,
                    preset.visorOrHairColor
                ),
                startY = h * 0.66f,
                endY = h
            ),
            topLeft = Offset(w * 0.20f, h * 0.67f),
            size = Size(w * 0.60f, h * 0.36f),
            cornerRadius = CornerRadius(w * 0.22f, w * 0.22f)
        )

        // 3. Accessory / Ears behind head for specific 3D characters
        when (preset.id) {
            "avatar_3d_emerald_fox" -> {
                // Left & Right 3D Fox Ears
                val leftEar = Path().apply {
                    moveTo(w * 0.26f, h * 0.38f)
                    lineTo(w * 0.20f, h * 0.14f)
                    lineTo(w * 0.42f, h * 0.28f)
                    close()
                }
                val rightEar = Path().apply {
                    moveTo(w * 0.74f, h * 0.38f)
                    lineTo(w * 0.80f, h * 0.14f)
                    lineTo(w * 0.58f, h * 0.28f)
                    close()
                }
                drawPath(leftEar, color = preset.skinOrBaseColor)
                drawPath(rightEar, color = preset.skinOrBaseColor)
            }
            "avatar_3d_chill_panda" -> {
                // Round dark 3D Panda Ears
                drawCircle(
                    color = preset.visorOrHairColor,
                    radius = w * 0.11f,
                    center = Offset(w * 0.26f, h * 0.25f)
                )
                drawCircle(
                    color = preset.visorOrHairColor,
                    radius = w * 0.11f,
                    center = Offset(w * 0.74f, h * 0.25f)
                )
            }
            "avatar_3d_pixel_gamer" -> {
                // 3D Headphone band + earcups
                drawArc(
                    color = preset.accentColor,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.19f, h * 0.18f),
                    size = Size(w * 0.62f, h * 0.52f),
                    style = Stroke(width = w * 0.065f, cap = StrokeCap.Round)
                )
                drawRoundRect(
                    color = preset.accentColor,
                    topLeft = Offset(w * 0.14f, h * 0.36f),
                    size = Size(w * 0.10f, h * 0.20f),
                    cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
                )
                drawRoundRect(
                    color = preset.accentColor,
                    topLeft = Offset(w * 0.76f, h * 0.36f),
                    size = Size(w * 0.10f, h * 0.20f),
                    cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
                )
            }
            "avatar_3d_robo_buddy" -> {
                // Glowing 3D Antenna on top
                drawLine(
                    color = preset.accentColor,
                    start = Offset(w * 0.5f, h * 0.12f),
                    end = Offset(w * 0.5f, h * 0.24f),
                    strokeWidth = w * 0.045f,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = Color.White,
                    radius = w * 0.055f,
                    center = Offset(w * 0.5f, h * 0.11f)
                )
            }
        }

        // 4. Main 3D Head Sphere with radial 3D shading
        val headCenter = Offset(w * 0.50f, h * 0.45f)
        val headRadius = w * 0.25f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.95f),
                    preset.skinOrBaseColor,
                    preset.skinOrBaseColor.copy(alpha = 0.88f)
                ),
                center = Offset(w * 0.43f, h * 0.37f),
                radius = headRadius * 1.35f
            ),
            radius = headRadius,
            center = headCenter
        )

        // 5. Character-specific 3D Visor / Crown / Eyes / Expressions
        when (preset.id) {
            "avatar_3d_astronaut", "avatar_3d_cyber_ninja", "avatar_3d_robo_buddy" -> {
                // Glossy 3D Visor Shield
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            preset.visorOrHairColor,
                            preset.accentColor.copy(alpha = 0.85f)
                        )
                    ),
                    topLeft = Offset(w * 0.31f, h * 0.35f),
                    size = Size(w * 0.38f, h * 0.18f),
                    cornerRadius = CornerRadius(w * 0.09f, w * 0.09f)
                )
                // Glowing LED Eyes inside visor
                drawCircle(
                    color = Color.White,
                    radius = w * 0.032f,
                    center = Offset(w * 0.42f, h * 0.44f)
                )
                drawCircle(
                    color = Color.White,
                    radius = w * 0.032f,
                    center = Offset(w * 0.58f, h * 0.44f)
                )
            }
            "avatar_3d_gold_mogul", "avatar_3d_starlight_sage" -> {
                // 3D Royal Crown / Hat above head
                val crownPath = Path().apply {
                    moveTo(w * 0.30f, h * 0.27f)
                    lineTo(w * 0.34f, h * 0.13f)
                    lineTo(w * 0.44f, h * 0.21f)
                    lineTo(w * 0.50f, h * 0.11f)
                    lineTo(w * 0.56f, h * 0.21f)
                    lineTo(w * 0.66f, h * 0.13f)
                    lineTo(w * 0.70f, h * 0.27f)
                    close()
                }
                drawPath(
                    path = crownPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFEF08A), preset.accentColor)
                    )
                )
                // Cool 3D Sunglasses / Eyes
                drawRoundRect(
                    color = preset.visorOrHairColor,
                    topLeft = Offset(w * 0.34f, h * 0.38f),
                    size = Size(w * 0.14f, h * 0.08f),
                    cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
                )
                drawRoundRect(
                    color = preset.visorOrHairColor,
                    topLeft = Offset(w * 0.52f, h * 0.38f),
                    size = Size(w * 0.14f, h * 0.08f),
                    cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
                )
                // Confident smile arc
                drawArc(
                    color = preset.visorOrHairColor,
                    startAngle = 20f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(w * 0.42f, h * 0.47f),
                    size = Size(w * 0.16f, h * 0.09f),
                    style = Stroke(width = w * 0.026f, cap = StrokeCap.Round)
                )
            }
            else -> {
                // Expressive 3D Eyes with white specular catchlights + rosy cheeks + smile
                drawCircle(
                    color = preset.visorOrHairColor,
                    radius = w * 0.036f,
                    center = Offset(w * 0.41f, h * 0.42f)
                )
                drawCircle(
                    color = preset.visorOrHairColor,
                    radius = w * 0.036f,
                    center = Offset(w * 0.59f, h * 0.42f)
                )
                drawCircle(
                    color = Color.White,
                    radius = w * 0.013f,
                    center = Offset(w * 0.40f, h * 0.405f)
                )
                drawCircle(
                    color = Color.White,
                    radius = w * 0.013f,
                    center = Offset(w * 0.58f, h * 0.405f)
                )
                // Rosy 3D cheeks
                drawCircle(
                    color = Color(0xFFF43F5E).copy(alpha = 0.32f),
                    radius = w * 0.038f,
                    center = Offset(w * 0.35f, h * 0.48f)
                )
                drawCircle(
                    color = Color(0xFFF43F5E).copy(alpha = 0.32f),
                    radius = w * 0.038f,
                    center = Offset(w * 0.65f, h * 0.48f)
                )
                // Happy smile
                drawArc(
                    color = preset.visorOrHairColor,
                    startAngle = 20f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(w * 0.43f, h * 0.47f),
                    size = Size(w * 0.14f, h * 0.08f),
                    style = Stroke(width = w * 0.025f, cap = StrokeCap.Round)
                )
            }
        }
    }
}

/**
 * Interactive Modal Bottom Sheet displaying the curated grid of 10 modern 3D illustrated avatars.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarPickerModalBottomSheet(
    currentAvatarId: String,
    userInitial: String,
    onSelectAvatar: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFFCFBFF),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .testTag("avatar_picker_bottom_sheet"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF3E8FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Face,
                            contentDescription = null,
                            tint = Color(0xFF7B3FE4),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Choose Your 3D Avatar",
                            color = Color(0xFF17142E),
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Select a 3D preset to sync across your profile & home",
                            color = Color(0xFF6F6E8A),
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.5.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close avatar picker",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // 10 Curated 3D Avatars in a clean 2-row x 5-column (or 5 rows of 2 cards) grid
            val rows = remember { CURATED_3D_AVATARS.chunked(2) }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { preset ->
                            val isSelected = currentAvatarId == preset.id
                            val borderColor by animateColorAsState(
                                targetValue = if (isSelected) Color(0xFF7B3FE4) else Color(0xFFE2E8F0),
                                animationSpec = tween(200),
                                label = "avatar_border_${preset.id}"
                            )
                            val cardBg by animateColorAsState(
                                targetValue = if (isSelected) Color(0xFFF5F3FF) else Color.White,
                                animationSpec = tween(200),
                                label = "avatar_bg_${preset.id}"
                            )

                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(cardBg)
                                    .border(
                                        width = if (isSelected) 1.8.dp else 1.dp,
                                        color = borderColor,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        onSelectAvatar(preset.id)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 10.dp)
                                    .testTag("avatar_option_${preset.id}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                UserAvatar3DBadge(
                                    avatarId = preset.id,
                                    userInitial = userInitial,
                                    size = 44.dp
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = preset.title,
                                        color = Color(0xFF17142E),
                                        fontFamily = PoppinsFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = preset.tag,
                                        color = if (isSelected) Color(0xFF7B3FE4) else Color(0xFF6F6E8A),
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 10.sp
                                    )
                                }

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF7B3FE4)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dedicated Help & Support Bottom Sheet with direct support intent & inquiry submission.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpAndSupportBottomSheet(
    userName: String,
    userEmail: String,
    walletIdCode: String,
    supportTickets: List<SupportTicket> = emptyList(),
    onSubmitTicket: (String) -> Unit = {},
    onOpenSupportBot: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var inquiryText by remember { mutableStateOf("") }
    var inquirySent by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFFCFBFF),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .testTag("help_support_bottom_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE0F2FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SupportAgent,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Help & Support",
                            color = Color(0xFF17142E),
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Wallet ID: $walletIdCode • Priority Support",
                            color = Color(0xFF6F6E8A),
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.5.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close Help & Support",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Inquiry Input Box
            Text(
                text = "CREATE SUPPORT TICKET",
                color = Color(0xFF6F6E8A),
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 1.sp
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                if (inquiryText.isEmpty()) {
                    Text(
                        text = "Describe your issue with task completion, payout, or account settings...",
                        color = Color(0xFF94A3B8),
                        fontFamily = InterFontFamily,
                        fontSize = 12.sp
                    )
                }
                BasicTextField(
                    value = inquiryText,
                    onValueChange = {
                        inquiryText = it
                        inquirySent = false
                    },
                    textStyle = TextStyle(
                        color = Color(0xFF17142E),
                        fontFamily = InterFontFamily,
                        fontSize = 12.5.sp
                    ),
                    cursorBrush = SolidColor(Color(0xFF7B3FE4)),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("support_inquiry_input")
                )
            }

            AnimatedVisibility(visible = inquirySent) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFDCFCE7))
                        .border(1.dp, Color(0xFF86EFAC), RoundedCornerShape(12.dp))
                        .padding(11.dp),
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
                        text = "Support ticket logged! Our team will reply shortly.",
                        color = Color(0xFF14532D),
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp
                    )
                }
            }

            ShinyGradientButton(
                text = "Submit Support Ticket",
                onClick = {
                    val clean = inquiryText.trim()
                    if (clean.isNotBlank()) {
                        onSubmitTicket(clean)
                        inquirySent = true
                        inquiryText = ""
                    }
                },
                gradientColors = listOf(Color(0xFF4F46E5), Color(0xFF7B3FE4)),
                height = 44.dp,
                cornerRadius = 13.dp,
                fontSizeSp = 12f,
                testTag = "support_send_inquiry_button",
                leadingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            )

            if (supportTickets.isNotEmpty()) {
                Text(
                    text = "YOUR TICKETS & TEAM REPLIES",
                    color = Color(0xFF6F6E8A),
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                supportTickets.take(3).forEach { ticket ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = ticket.message,
                            color = Color(0xFF17142E),
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                        val reply = ticket.displayReply
                        if (reply.isNotBlank()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF0FDF4))
                                    .border(1.dp, Color(0xFF86EFAC), RoundedCornerShape(10.dp))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "Team Reply",
                                    color = Color(0xFF15803D),
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                )
                                Text(
                                    text = reply,
                                    color = Color(0xFF166534),
                                    fontFamily = InterFontFamily,
                                    fontSize = 11.5.sp
                                )
                            }
                        } else {
                            Text(
                                text = "Status: Open (Awaiting team review)",
                                color = Color(0xFF6D28D9),
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Bold,
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
 * Dedicated Settings Screen navigated to from My Profile -> Settings.
 * Includes the "Edit Profile / Change Name" input card with Save & Cancel actions and validation (2 to 50 chars).
 */
@Composable
fun ProfileSettingsScreenContent(
    currentDisplayName: String,
    currentEmail: String,
    currentAvatarId: String,
    userInitial: String,
    notificationsEnabled: Boolean,
    isSavingName: Boolean,
    onToggleNotifications: () -> Unit,
    onOpenAvatarPicker: () -> Unit,
    onSaveDisplayName: (String, (Boolean, String) -> Unit) -> Unit,
    onBackToProfile: () -> Unit,
    onSignOut: () -> Unit = {}
) {
    BackHandler {
        onBackToProfile()
    }
    val appCtx = LocalContext.current

    var nameInput by remember(currentDisplayName) { mutableStateOf(currentDisplayName) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var saveSuccessBanner by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp)
            .testTag("profile_settings_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Contextual Top Header for Settings (Zero "Task Gullak" brand logo/text)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(13.dp),
                        spotColor = Color(0xFF32238C).copy(alpha = 0.14f)
                    )
                    .clip(RoundedCornerShape(13.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(13.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBackToProfile
                    )
                    .testTag("settings_back_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to My Profile",
                    tint = Color(0xFF17142E),
                    modifier = Modifier.size(19.dp)
                )
            }

            Column {
                Text(
                    text = "PREFERENCES & PROFILE",
                    color = Color(0xFF7B3FE4),
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.5.sp,
                    letterSpacing = 1.1.sp
                )
                Text(
                    text = "Settings",
                    color = Color(0xFF17142E),
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    letterSpacing = (-0.5).sp
                )
            }
        }

        // Edit Profile / Change Name Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = Color(0xFF32238C).copy(alpha = 0.08f),
                    spotColor = Color(0xFF32238C).copy(alpha = 0.15f)
                )
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.97f))
                .border(1.dp, Color.White, RoundedCornerShape(24.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                UserAvatar3DBadge(
                    avatarId = currentAvatarId,
                    userInitial = userInitial,
                    size = 52.dp,
                    showEditBadge = true,
                    onClick = onOpenAvatarPicker
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Edit Profile / Change Name",
                        color = Color(0xFF17142E),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.5.sp
                    )
                    Text(
                        text = "Updates your name across your account & Home greeting",
                        color = Color(0xFF6F6E8A),
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DISPLAY NAME",
                        color = Color(0xFF475569),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.9.sp
                    )
                    val trimmedLen = nameInput.trim().length
                    Text(
                        text = "$trimmedLen / 50",
                        color = if (trimmedLen in 2..50) Color(0xFF6F6E8A) else Color(0xFFE11D48),
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.5.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(
                            width = 1.2.dp,
                            color = when {
                                validationError != null -> Color(0xFFE11D48)
                                else -> Color(0xFFCBD5E1)
                            },
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (nameInput.isEmpty()) {
                        Text(
                            text = "Enter your display name (2–50 characters)",
                            color = Color(0xFF94A3B8),
                            fontFamily = InterFontFamily,
                            fontSize = 13.sp
                        )
                    }
                    BasicTextField(
                        value = nameInput,
                        onValueChange = { updated ->
                            if (updated.length <= 55) {
                                nameInput = updated
                                validationError = null
                                saveSuccessBanner = null
                            }
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color(0xFF17142E),
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp
                        ),
                        cursorBrush = SolidColor(Color(0xFF7B3FE4)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_name_input")
                    )
                }

                Text(
                    text = "Linked email: $currentEmail",
                    color = Color(0xFF94A3B8),
                    fontFamily = InterFontFamily,
                    fontSize = 10.5.sp
                )
            }

            if (validationError != null) {
                Text(
                    text = validationError!!,
                    color = Color(0xFFE11D48),
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.5.sp,
                    modifier = Modifier.testTag("settings_name_error")
                )
            }

            if (saveSuccessBanner != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFDCFCE7))
                        .border(1.dp, Color(0xFF86EFAC), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("settings_name_success"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF15803D),
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = saveSuccessBanner!!,
                        color = Color(0xFF14532D),
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp
                    )
                }
            }

            // Save and Cancel Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.42f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Color(0xFFF1F5F9))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(13.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            nameInput = currentDisplayName
                            validationError = null
                            saveSuccessBanner = null
                            onBackToProfile()
                        }
                        .testTag("settings_cancel_name_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancel",
                        color = Color(0xFF475569),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                }

                Box(modifier = Modifier.weight(0.58f)) {
                    ShinyGradientButton(
                        text = if (isSavingName) "Saving..." else "Save Changes",
                        onClick = {
                            val cleaned = nameInput.trim().replace(Regex("\\s+"), " ")
                            val hasValidChars = cleaned.any { it.isLetterOrDigit() }
                            when {
                                cleaned.isBlank() -> {
                                    validationError = "Name cannot be blank."
                                }
                                cleaned.length < 2 || cleaned.length > 50 -> {
                                    validationError = "Name must be between 2 and 50 characters."
                                }
                                !hasValidChars -> {
                                    validationError = "Please enter a valid name."
                                }
                                else -> {
                                    validationError = null
                                    onSaveDisplayName(cleaned) { ok, msg ->
                                        if (ok) {
                                            saveSuccessBanner = msg
                                        } else {
                                            validationError = msg
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isSavingName,
                        gradientColors = listOf(Color(0xFF4F46E5), Color(0xFF7B3FE4)),
                        height = 44.dp,
                        cornerRadius = 13.dp,
                        fontSizeSp = 12.5f,
                        testTag = "settings_save_name_button"
                    )
                }
            }
        }

        // Additional Settings Preferences Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(22.dp),
                    ambientColor = Color(0xFF32238C).copy(alpha = 0.06f),
                    spotColor = Color(0xFF32238C).copy(alpha = 0.12f)
                )
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.96f))
                .border(1.dp, Color.White, RoundedCornerShape(22.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleNotifications
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF3E8FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsActive,
                            contentDescription = null,
                            tint = Color(0xFF7B3FE4),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Push & Broadcast Alerts",
                            color = Color(0xFF17142E),
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (notificationsEnabled) "Instant alerts for task approvals & new projects" else "Notifications muted",
                            color = Color(0xFF6F6E8A),
                            fontFamily = InterFontFamily,
                            fontSize = 11.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (notificationsEnabled) Color(0xFFDCFCE7) else Color(0xFFF1F5F9))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (notificationsEnabled) "ACTIVE" else "OFF",
                        color = if (notificationsEnabled) Color(0xFF15803D) else Color(0xFF64748B),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Set / Update Password Card (Security & Dual Login)
        var showSetPasswordDialog by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(22.dp),
                    ambientColor = Color(0xFF32238C).copy(alpha = 0.06f),
                    spotColor = Color(0xFF32238C).copy(alpha = 0.12f)
                )
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.96f))
                .border(1.dp, Color.White, RoundedCornerShape(22.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSetPasswordDialog = true },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Set Account Password",
                            color = Color(0xFF17142E),
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Enable Email & Password sign-in for your account",
                            color = Color(0xFF6F6E8A),
                            fontFamily = InterFontFamily,
                            fontSize = 11.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFFEFF6FF))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "SET →",
                        color = Color(0xFF2563EB),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
        }

        if (showSetPasswordDialog) {
            SetPasswordDialog(
                onDismiss = { showSetPasswordDialog = false },
                onSuccess = {
                    showSetPasswordDialog = false
                    android.widget.Toast.makeText(appCtx, "Password updated successfully! You can now log in using either Google or Email & Password.", android.widget.Toast.LENGTH_LONG).show()
                }
            )
        }

        // Delete My Account Card (Red Warning Card)
        var showDeleteConfirmDialog by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(22.dp),
                    ambientColor = Color(0xFFE11D48).copy(alpha = 0.08f),
                    spotColor = Color(0xFFE11D48).copy(alpha = 0.15f)
                )
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFFFEF2F2))
                .border(1.dp, Color(0xFFFECDD3), RoundedCornerShape(22.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFE4E6)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteForever,
                        contentDescription = "Delete Account",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Delete My Account",
                        color = Color(0xFF991B1B),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Permanently remove your profile, submissions & Gullak account",
                        color = Color(0xFF9F1239),
                        fontFamily = InterFontFamily,
                        fontSize = 11.sp
                    )
                }
            }

            OutlinedButton(
                onClick = { showDeleteConfirmDialog = true },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFF87171)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFFDC2626)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("delete_account_button")
            ) {
                Text(
                    text = "Delete My Account",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp
                )
            }
        }

        // Delete Account Confirmation Dialog (Requires typing DELETE exactly)
        if (showDeleteConfirmDialog) {
            var deleteTypedInput by remember { mutableStateOf("") }
            var isDeletingAccount by remember { mutableStateOf(false) }
            val coroutineScope = rememberCoroutineScope()

            Dialog(onDismissRequest = { if (!isDeletingAccount) showDeleteConfirmDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 16.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFE4E6)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "Delete My Account?",
                                color = Color(0xFF991B1B),
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                        }

                        Text(
                            text = "This action is permanent and CANNOT be undone. All your profile data, task submissions, and Gullak balance will be permanently erased.",
                            color = Color(0xFF475569),
                            fontFamily = InterFontFamily,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "To confirm, please type DELETE below:",
                                color = Color(0xFF1E293B),
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF8FAFC))
                                    .border(
                                        width = 1.2.dp,
                                        color = if (deleteTypedInput == "DELETE") Color(0xFFDC2626) else Color(0xFFCBD5E1),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (deleteTypedInput.isEmpty()) {
                                    Text(
                                        text = "Type DELETE to confirm",
                                        color = Color(0xFF94A3B8),
                                        fontFamily = InterFontFamily,
                                        fontSize = 13.sp
                                    )
                                }
                                BasicTextField(
                                    value = deleteTypedInput,
                                    onValueChange = { deleteTypedInput = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = Color(0xFF991B1B),
                                        fontFamily = PoppinsFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp
                                    ),
                                    cursorBrush = SolidColor(Color(0xFFDC2626)),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showDeleteConfirmDialog = false },
                                enabled = !isDeletingAccount,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                            ) {
                                Text("Cancel", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    if (deleteTypedInput.trim() == "DELETE" && !isDeletingAccount) {
                                        isDeletingAccount = true
                                        coroutineScope.launch {
                                            FirebaseEmailDispatchService.deleteAccountPermanently(
                                                appCtx,
                                                currentEmail
                                            )
                                            isDeletingAccount = false
                                            showDeleteConfirmDialog = false
                                            onSignOut()
                                        }
                                    }
                                },
                                enabled = deleteTypedInput.trim() == "DELETE" && !isDeletingAccount,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFDC2626),
                                    disabledContainerColor = Color(0xFFFECDD3)
                                ),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(42.dp)
                            ) {
                                Text(
                                    text = if (isDeletingAccount) "Deleting..." else "Confirm Delete",
                                    color = Color.White,
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
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
private fun SetPasswordDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    fun submitPassword() {
        if (isSubmitting) return
        errorMsg = null
        if (password.length < 12) {
            errorMsg = "Password must be at least 12 characters long."
            return
        }
        if (!password.any { it in 'A'..'Z' }) {
            errorMsg = "Password must contain at least 1 uppercase letter."
            return
        }
        if (!password.any { it in 'a'..'z' }) {
            errorMsg = "Password must contain at least 1 lowercase letter."
            return
        }
        if (!password.any { it in '0'..'9' }) {
            errorMsg = "Password must contain at least 1 number."
            return
        }
        if (password != confirmPassword) {
            errorMsg = "Passwords do not match."
            return
        }

        isSubmitting = true
        scope.launch {
            try {
                val firebaseUser = FirebaseAuth.getInstance().currentUser
                if (firebaseUser != null) {
                    firebaseUser.updatePassword(password).await()
                    val email = firebaseUser.email.orEmpty().ifBlank {
                        FirebaseEmailDispatchService.getSavedUserEmail(context)
                    }
                    FirebaseEmailDispatchService.completeGoogleAccountPasswordSetup(
                        context = context,
                        fullName = firebaseUser.displayName.orEmpty(),
                        rawEmail = email,
                        password = password
                    )
                    onSuccess()
                } else {
                    errorMsg = "User session expired. Please sign in again."
                }
            } catch (e: Exception) {
                errorMsg = e.localizedMessage ?: "Failed to set password. Please try again."
            } finally {
                isSubmitting = false
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Set Account Password",
                    color = Color(0xFF17142E),
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Create a password to enable Email & Password sign-in alongside Google Login.",
                    color = Color(0xFF6F6E8A),
                    fontFamily = InterFontFamily,
                    fontSize = 11.5.sp
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMsg = null },
                    label = { Text("New Password (min 12 chars)") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; errorMsg = null },
                    label = { Text("Confirm New Password") },
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        color = Color(0xFFDC2626),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { submitPassword() },
                        enabled = !isSubmitting
                    ) {
                        Text(if (isSubmitting) "Saving..." else "Save Password")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkModApplyModalBottomSheet(
    currentBulkStatus: String,
    currentDeviceCount: Int,
    onSubmitRequest: (reason: String, deviceCount: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var reasonText by remember { mutableStateOf("") }
    var deviceCountText by remember { mutableStateOf(if (currentDeviceCount > 0) currentDeviceCount.toString() else "2") }
    var guidelineAccepted by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFFCFBFF),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Apply for Bulk Mod",
                    color = Color(0xFF17142E),
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = "Why do you need Bulk Mod status?",
                color = Color(0xFF475569),
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            OutlinedTextField(
                value = reasonText,
                onValueChange = { reasonText = it; errorMsg = null },
                placeholder = { Text("Explain why you need bulk comment access...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
            )

            Text(
                text = "How many devices do you operate? (min 1)",
                color = Color(0xFF475569),
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            OutlinedTextField(
                value = deviceCountText,
                onValueChange = { deviceCountText = it.filter { c -> c.isDigit() }; errorMsg = null },
                modifier = Modifier.fillMaxWidth()
            )

            // Strict Guideline Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFEF2F2))
                    .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "STRICT BULK MOD GUIDELINES",
                        color = Color(0xFF991B1B),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "• Work only on specified devices.\n• Cannot hold more comments than approved device limit.\n• Extra comments must be returned immediately.\n• Misuse/duplicates lead to immediate ban.",
                        color = Color(0xFF7F1D1D),
                        fontFamily = InterFontFamily,
                        fontSize = 10.5.sp,
                        lineHeight = 14.5.sp
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = guidelineAccepted,
                    onCheckedChange = { guidelineAccepted = it }
                )
                Text(
                    text = "I have read and agree to all strict guidelines.",
                    color = Color(0xFF334155),
                    fontFamily = InterFontFamily,
                    fontSize = 11.5.sp
                )
            }

            if (errorMsg != null) {
                Text(
                    text = errorMsg!!,
                    color = Color(0xFFDC2626),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Button(
                onClick = {
                    val cleanReason = reasonText.trim()
                    val count = deviceCountText.toIntOrNull() ?: 0
                    if (cleanReason.isBlank()) {
                        errorMsg = "Please explain why you need Bulk Mod status."
                        return@Button
                    }
                    if (count < 1) {
                        errorMsg = "Please enter a valid device count (at least 1)."
                        return@Button
                    }
                    if (!guidelineAccepted) {
                        errorMsg = "You must agree to the strict guidelines."
                        return@Button
                    }
                    isSubmitting = true
                    onSubmitRequest(cleanReason, count)
                    onDismiss()
                },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B3FE4))
            ) {
                Text(
                    text = if (isSubmitting) "Submitting..." else "Submit Bulk Mod Application",
                    color = Color.White,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}
