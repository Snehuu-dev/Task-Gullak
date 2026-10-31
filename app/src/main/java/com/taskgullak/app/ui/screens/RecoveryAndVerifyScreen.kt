package com.taskgullak.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskgullak.app.R
import com.taskgullak.app.ui.components.CssGridOverlay
import com.taskgullak.app.ui.components.Floating3DOrb
import com.taskgullak.app.ui.components.ShinyGradientButton
import com.taskgullak.app.ui.theme.InterFontFamily
import com.taskgullak.app.ui.theme.PoppinsFontFamily
import com.taskgullak.app.ui.theme.TgActionOrange
import com.taskgullak.app.ui.theme.TgActionPink
import com.taskgullak.app.ui.theme.TgAuthError
import com.taskgullak.app.ui.theme.TgOrangeEnd
import com.taskgullak.app.ui.theme.TgOrangePrimary
import com.taskgullak.app.ui.theme.TgOrangeStart
import com.taskgullak.app.ui.theme.TgVerifySuccess
import com.taskgullak.app.ui.theme.TgVerifySuccessDark
import com.taskgullak.app.ui.theme.TgWarmTextMain
import com.taskgullak.app.ui.theme.TgWarmTextMuted
import com.taskgullak.app.ui.theme.TgWarmTextSoft
import com.taskgullak.app.data.remote.FirebaseEmailDispatchService
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val EMAIL_CHECK_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

/**
 * Exact 100% UI replica of forgot-password.html + forgot-password.css + forgot-password.js
 */
@Composable
fun ForgotPasswordScreen(
    initialEmail: String = "",
    onBackToLogin: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var email by remember { mutableStateOf(initialEmail) }
    var fieldMessage by remember { mutableStateOf<String?>(null) }
    var isFieldSuccess by remember { mutableStateOf(false) }

    var statusTitle by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var statusType by remember { mutableStateOf("info") } // "success", "error", "info"

    var isLoading by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "forgotFloat")
    val logoOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoOffsetY"
    )

    fun handleSendResetLink() {
        if (isLoading) return
        val cleanEmail = email.trim().lowercase()
        fieldMessage = null
        isFieldSuccess = false
        statusTitle = null

        if (cleanEmail.isEmpty()) {
            fieldMessage = "Please enter your email address."
            isFieldSuccess = false
            return
        }
        if (!EMAIL_CHECK_REGEX.matches(cleanEmail)) {
            fieldMessage = "Please enter a valid email address."
            isFieldSuccess = false
            statusTitle = "Invalid email address"
            statusMessage = "Please check the email address and enter a valid email."
            statusType = "error"
            return
        }

        isLoading = true
        coroutineScope.launch {
            try {
                val result = FirebaseEmailDispatchService.sendPasswordResetLinkEmail(
                    context = context,
                    rawEmail = cleanEmail
                )
                isFieldSuccess = result.success
                fieldMessage = if (result.success) "Email address looks good." else result.title
                statusTitle = result.title
                statusMessage = result.message
                statusType = result.statusType
                if (result.success) {
                    email = ""
                }
            } finally {
                isLoading = false
            }
        }
    }

    WarmOrangeBackground(testTag = "forgot_password_screen") {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            // FORGOT PASSWORD CARD (.forgot-card)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 390.dp)
                    .shadow(
                        elevation = 18.dp,
                        shape = RoundedCornerShape(32.dp),
                        ambientColor = TgOrangePrimary.copy(alpha = 0.12f),
                        spotColor = TgOrangePrimary.copy(alpha = 0.16f)
                    )
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White.copy(alpha = 0.86f))
                    .border(1.5.dp, TgOrangePrimary.copy(alpha = 0.20f), RoundedCornerShape(32.dp))
            ) {
                // Bold Gradient Top Line (.card-top-line)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.5.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(TgOrangeStart, TgOrangePrimary, TgOrangeEnd)
                            )
                        )
                )

                // Soft decorative side circles (.circle-left, .circle-right)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = (-35).dp, y = (-20).dp)
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(TgOrangePrimary.copy(alpha = 0.08f))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 35.dp, y = (-60).dp)
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(TgOrangePrimary.copy(alpha = 0.08f))
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 22.dp, end = 22.dp, top = 28.dp, bottom = 34.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 3D ILLUSTRATION (.auth-illustration-container)
                    Image(
                        painter = painterResource(id = R.drawable.img_forgot_password),
                        contentDescription = "Forgot Password Illustration",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(145.dp)
                            .offset(y = logoOffsetY.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // HEADER (.forgot-header)
                    Text(
                        text = "Forgot Your Password?",
                        style = TextStyle(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFF5E62),
                                    Color(0xFFFF9966),
                                    Color(0xFFE84393)
                                )
                            ),
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 23.sp,
                            letterSpacing = (-0.4).sp,
                            textAlign = TextAlign.Center
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Enter your Gmail address and we'll send you a link to reset your password.",
                        color = TgWarmTextSoft,
                        fontFamily = PoppinsFontFamily,
                        fontSize = 11.5.sp,
                        lineHeight = 17.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    // EMAIL INPUT (.input-box)
                    val hasError = fieldMessage != null && !isFieldSuccess
                    val borderColor = when {
                        hasError -> TgAuthError.copy(alpha = 0.6f)
                        isFieldSuccess -> TgVerifySuccess.copy(alpha = 0.55f)
                        else -> TgOrangePrimary.copy(alpha = 0.22f)
                    }
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.90f))
                                .border(1.dp, borderColor, RoundedCornerShape(14.dp)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(44.dp)
                                    .height(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // Gmail Icon Circle (.gmail-icon-circle)
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(TgOrangePrimary.copy(alpha = 0.08f))
                                        .border(1.dp, TgOrangePrimary.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.MailOutline,
                                        contentDescription = null,
                                        tint = TgOrangePrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (email.isEmpty()) {
                                    Text(
                                        text = "Enter your Gmail address",
                                        color = TgWarmTextMuted,
                                        fontFamily = PoppinsFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.5.sp
                                    )
                                }
                                BasicTextField(
                                    value = email,
                                    onValueChange = {
                                        email = it
                                        fieldMessage = null
                                        isFieldSuccess = false
                                        statusTitle = null
                                    },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = TgWarmTextMain,
                                        fontFamily = PoppinsFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.5.sp
                                    ),
                                    cursorBrush = SolidColor(TgOrangePrimary),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Email,
                                        imeAction = ImeAction.Send
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("forgot_email_input")
                                )
                            }
                        }

                        if (!fieldMessage.isNullOrEmpty()) {
                            Text(
                                text = fieldMessage ?: "",
                                color = if (isFieldSuccess) TgVerifySuccess else TgAuthError,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(top = 5.dp, start = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // STATUS MESSAGE (.forgot-status)
                    if (statusTitle != null) {
                        StatusAlertBox(
                            title = statusTitle ?: "",
                            message = statusMessage ?: "",
                            type = statusType,
                            modifier = Modifier.padding(bottom = 14.dp)
                        )
                    }

                    // SUBMIT BUTTON (.forgot-submit-btn)
                    ShinyGradientButton(
                        text = if (isLoading) "Sending reset link..." else "Send Reset Link →",
                        onClick = { handleSendResetLink() },
                        gradientColors = listOf(TgOrangeStart, TgOrangeEnd),
                        height = 48.dp,
                        cornerRadius = 14.dp,
                        fontSizeSp = 12f,
                        isLoading = isLoading,
                        testTag = "forgot_submit_button"
                    )

                    // BACK TO LOGIN (.signup-switch)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Remember your password? ",
                            color = TgWarmTextSoft,
                            fontFamily = PoppinsFontFamily,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "Back to Login",
                            color = TgOrangePrimary,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .clickable { onBackToLogin() }
                                .padding(vertical = 2.dp)
                                .testTag("forgot_back_to_login")
                        )
                    }

                    // FOOTER NOTE (.forgot-footer-note)
                    HorizontalDivider(
                        color = TgOrangePrimary.copy(alpha = 0.08f),
                        modifier = Modifier.padding(top = 14.dp, bottom = 12.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Send,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "We'll send the reset link to your Gmail shortly.",
                            color = Color(0xFF64748B),
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 9.5.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Exact 100% UI replica of verify-email.html + verify-email.css + verify-email.js
 */
@Composable
fun VerifyEmailScreen(
    userEmail: String,
    onVerifiedContinue: () -> Unit,
    onOpenEmailActionScreen: () -> Unit,
    onBackToLogin: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var remainingSeconds by remember { mutableIntStateOf(0) }
    var isResending by remember { mutableStateOf(false) }
    var isChecking by remember { mutableStateOf(false) }

    var statusTitle by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var statusType by remember { mutableStateOf("info") }

    val activeEmail = userEmail.ifBlank { FirebaseAuth.getInstance().currentUser?.email ?: "" }

    val infiniteTransition = rememberInfiniteTransition(label = "verifyFloat")
    val illustrationOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "verifyOffsetY"
    )

    // Automatically dispatch verification email when arriving on VerifyEmailScreen if not just sent on signup
    LaunchedEffect(activeEmail) {
        if (activeEmail.isNotBlank() && statusTitle == null) {
            if (FirebaseEmailDispatchService.wasVerificationJustSent(context)) {
                remainingSeconds = 60
            } else {
                val result = FirebaseEmailDispatchService.sendVerificationLinkEmail(
                    context = context,
                    fallbackEmail = activeEmail
                )
                if (result.success) {
                    remainingSeconds = 60
                }
            }
        }
    }

    // Automatically poll Firebase verification status every 2.5 seconds so as soon as the user
    // clicks the verification link in their email, they are automatically taken to the Home page.
    LaunchedEffect(activeEmail) {
        while (true) {
            delay(2500L)
            val isVerified = runCatching {
                FirebaseEmailDispatchService.checkAndSyncVerificationStatus(
                    context = context,
                    fallbackEmail = activeEmail
                )
            }.getOrDefault(false)
            if (isVerified) {
                FirebaseEmailDispatchService.setUserLoggedIn(context, true)
                statusTitle = "Email verified successfully"
                statusMessage = "Your Task Gullak account is verified! Taking you to Home..."
                statusType = "success"
                delay(350L)
                onVerifiedContinue()
                break
            }
        }
    }

    // Cooldown countdown timer matching RESEND_COOLDOWN = 60 in verify-email.js
    LaunchedEffect(remainingSeconds) {
        if (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds -= 1
        }
    }

    // Matches resendVerificationEmail() in verify-email.js
    fun handleResendVerificationEmail() {
        if (isResending) return
        if (remainingSeconds > 0) {
            statusTitle = "Please wait"
            statusMessage =
                "You can request another verification email in $remainingSeconds seconds."
            statusType = "info"
            return
        }

        isResending = true
        coroutineScope.launch {
            try {
                val result = FirebaseEmailDispatchService.sendVerificationLinkEmail(
                    context = context,
                    fallbackEmail = activeEmail
                )
                statusTitle = result.title
                statusMessage = result.message
                statusType = result.statusType
                if (result.success) {
                    remainingSeconds = 60
                }
            } finally {
                isResending = false
            }
        }
    }

    // Matches checkVerificationStatus() in verify-email.js
    fun handleCheckVerificationStatus() {
        if (isChecking) return
        isChecking = true
        statusTitle = "Checking verification"
        statusMessage = "We're checking your email verification status..."
        statusType = "info"

        coroutineScope.launch {
            try {
                val isVerified = FirebaseEmailDispatchService.checkAndSyncVerificationStatus(
                    context = context,
                    fallbackEmail = activeEmail
                )
                delay(300)
                if (isVerified) {
                    FirebaseEmailDispatchService.setUserLoggedIn(context, true)
                    statusTitle = "Email verified successfully"
                    statusMessage = "Your Task Gullak account is now verified. Welcome aboard!"
                    statusType = "success"
                    delay(350)
                    onVerifiedContinue()
                } else {
                    statusTitle = "Not verified yet"
                    statusMessage =
                        "Your email is still not verified. Please click the link in your email first, then try again."
                    statusType = "info"
                }
            } finally {
                isChecking = false
            }
        }
    }

    WarmOrangeBackground(testTag = "verify_email_screen") {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            // VERIFY CARD (.verify-card)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 390.dp)
                    .shadow(
                        elevation = 18.dp,
                        shape = RoundedCornerShape(32.dp),
                        ambientColor = TgOrangePrimary.copy(alpha = 0.10f),
                        spotColor = TgOrangePrimary.copy(alpha = 0.14f)
                    )
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White.copy(alpha = 0.88f))
                    .border(1.5.dp, TgOrangePrimary.copy(alpha = 0.19f), RoundedCornerShape(32.dp))
            ) {
                // Top gradient line (.card-top-line)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.5.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(TgOrangeStart, TgOrangePrimary, TgOrangeEnd)
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 22.dp, end = 22.dp, top = 24.dp, bottom = 25.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 3D EMAIL ILLUSTRATION (.verify-illustration-container)
                    Image(
                        painter = painterResource(id = R.drawable.img_verify_email),
                        contentDescription = "Email verification illustration",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(172.dp)
                            .offset(y = illustrationOffsetY.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // HEADER (.verify-header)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(TgVerifySuccess.copy(alpha = 0.07f))
                            .border(1.dp, TgVerifySuccess.copy(alpha = 0.14f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(15.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF22C55E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✓",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Text(
                            text = "Email Verification",
                            color = TgVerifySuccessDark,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 9.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(7.dp))

                    Text(
                        text = "Verify Your Email",
                        style = TextStyle(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFF5E62),
                                    Color(0xFFFF9966),
                                    Color(0xFFE84393)
                                )
                            ),
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 23.sp,
                            letterSpacing = (-0.5).sp
                        )
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = "We've sent a verification link to your email address.",
                        color = TgWarmTextSoft,
                        fontFamily = InterFontFamily,
                        fontSize = 10.5.sp,
                        lineHeight = 16.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(15.dp))

                    // EMAIL DISPLAY (.email-display)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(43.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(Color.White.copy(alpha = 0.88f))
                            .border(1.dp, TgOrangePrimary.copy(alpha = 0.13f), RoundedCornerShape(13.dp))
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(27.dp)
                                .clip(CircleShape)
                                .background(TgOrangePrimary.copy(alpha = 0.08f))
                                .border(1.dp, TgOrangePrimary.copy(alpha = 0.13f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MailOutline,
                                contentDescription = null,
                                tint = TgOrangePrimary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = activeEmail.ifBlank { "Your registered email" },
                            color = TgWarmTextMain,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // INFORMATION (.verify-message)
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = TgWarmTextSoft)) {
                                append("Open your email and click the ")
                            }
                            withStyle(SpanStyle(color = TgWarmTextMain, fontWeight = FontWeight.Bold)) {
                                append("verification link")
                            }
                            withStyle(SpanStyle(color = TgWarmTextSoft)) {
                                append(" to activate your Task Gullak account.")
                            }
                        },
                        fontFamily = InterFontFamily,
                        fontSize = 10.sp,
                        lineHeight = 15.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(13.dp))

                    // STATUS MESSAGE (.verify-status)
                    if (statusTitle != null) {
                        StatusAlertBox(
                            title = statusTitle ?: "",
                            message = statusMessage ?: "",
                            type = statusType,
                            modifier = Modifier.padding(bottom = 13.dp)
                        )
                    }

                    // ACTION BUTTONS (.verify-actions)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        // OPEN GMAIL (.primary-action)
                        ShinyGradientButton(
                            text = "Open Gmail",
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://mail.google.com/"))
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    statusTitle = "Gmail link ready"
                                    statusMessage = "Please check your Gmail app for the Task Gullak verification link."
                                    statusType = "info"
                                }
                            },
                            gradientColors = listOf(TgOrangeStart, TgOrangeEnd),
                            height = 46.dp,
                            cornerRadius = 14.dp,
                            fontSizeSp = 11.5f,
                            testTag = "open_gmail_button",
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Outlined.MailOutline,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            trailingContent = {
                                Text(
                                    text = "↗",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        )

                        // RESEND VERIFICATION EMAIL (.secondary-action)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.78f))
                                .border(1.dp, TgOrangePrimary.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
                                .clickable(enabled = remainingSeconds == 0 && !isResending) {
                                    handleResendVerificationEmail()
                                }
                                .testTag("resend_verification_button"),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isResending) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = TgOrangePrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(7.dp))
                                Text(
                                    text = "Sending verification email...",
                                    color = TgOrangePrimary,
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                )
                            } else {
                                if (remainingSeconds == 0) {
                                    Text(
                                        text = "↻ ",
                                        color = TgOrangePrimary,
                                        fontFamily = PoppinsFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Text(
                                    text = if (remainingSeconds > 0) {
                                        "Resend available in ${remainingSeconds}s"
                                    } else {
                                        "Resend Verification Email"
                                    },
                                    color = TgOrangePrimary.copy(alpha = if (remainingSeconds > 0) 0.6f else 1f),
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                )
                            }
                        }

                        // CHECK VERIFICATION (.check-action)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .clickable(enabled = !isChecking) {
                                    handleCheckVerificationStatus()
                                }
                                .testTag("check_verification_button"),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(19.dp)
                                    .clip(CircleShape)
                                    .background(TgVerifySuccess.copy(alpha = 0.08f))
                                    .border(1.dp, TgVerifySuccess.copy(alpha = 0.16f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "✓",
                                    color = TgVerifySuccess,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "I've Verified My Email",
                                color = TgWarmTextSoft,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // HELP (.verify-help)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(Color(0xFFF8FAFC).copy(alpha = 0.85f))
                            .border(1.dp, Color(0xFF94A3B8).copy(alpha = 0.14f), RoundedCornerShape(11.dp))
                            .padding(horizontal = 11.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(21.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF64748B).copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Didn't receive the email?",
                                color = TgWarmTextMain,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp
                            )
                            Text(
                                text = "Check your spam or promotions folder.",
                                color = TgWarmTextSoft,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 9.sp
                            )
                        }
                    }

                    // FOOTER (.verify-footer)
                    HorizontalDivider(
                        color = TgOrangePrimary.copy(alpha = 0.08f),
                        modifier = Modifier.padding(top = 15.dp, bottom = 12.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "← Back to Login",
                            color = TgOrangePrimary,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp,
                            modifier = Modifier
                                .clickable { onBackToLogin() }
                                .testTag("verify_back_to_login")
                        )
                        Text(
                            text = "Sign out",
                            color = TgWarmTextMuted,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 9.sp,
                            modifier = Modifier
                                .clickable { onLogout() }
                                .testTag("verify_logout_button")
                        )
                    }
                }
            }
        }
    }
}

/**
 * Exact 100% UI replica of email-action.html + email-action.css + email-action.js
 */
@Composable
fun EmailActionScreen(
    userEmail: String,
    onContinueToApp: () -> Unit,
    onBackToLogin: () -> Unit,
    onNavigateToTerms: () -> Unit,
    onNavigateToPrivacy: () -> Unit
) {
    val context = LocalContext.current
    var isVerifying by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        FirebaseEmailDispatchService.checkAndSyncVerificationStatus(
            context = context,
            fallbackEmail = userEmail
        )
        delay(600)
        isVerifying = false
    }

    WarmOrangeBackground(testTag = "email_action_screen") {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            // EMAIL ACTION CARD (.email-action-card)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 430.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(28.dp),
                        ambientColor = Color(0xFF231914).copy(alpha = 0.10f),
                        spotColor = Color(0xFF231914).copy(alpha = 0.14f)
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White.copy(alpha = 0.94f))
                    .border(1.dp, Color.White, RoundedCornerShape(28.dp))
            ) {
                // Card Top Line (.card-top-line)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(0.72f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(TgActionOrange, Color(0xFFFF8B38), TgActionPink)
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 22.dp, end = 22.dp, top = 30.dp, bottom = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Status Icon (.action-icon 86x86dp)
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .clip(RoundedCornerShape(27.dp))
                            .background(
                                if (isVerifying) {
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFFFFF7F0), Color(0xFFFFEFE7))
                                    )
                                } else {
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFFF0FDF4), Color(0xFFDCFCE7))
                                    )
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = if (isVerifying) {
                                    TgActionOrange.copy(alpha = 0.10f)
                                } else {
                                    TgVerifySuccess.copy(alpha = 0.12f)
                                },
                                shape = RoundedCornerShape(27.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(30.dp),
                                color = TgActionOrange,
                                strokeWidth = 3.dp
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .border(3.dp, TgVerifySuccess, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Email Verified",
                                    tint = TgVerifySuccess,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Badge (.action-badge)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFFFFF7ED))
                            .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.13f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 11.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isVerifying) TgActionOrange else TgVerifySuccess)
                        )
                        Text(
                            text = if (isVerifying) "Verifying Email" else "Email Verified",
                            color = Color(0xFFB45309),
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Title (.action-title)
                    Text(
                        text = if (isVerifying) "Verifying Your Email" else "Email Verified!",
                        style = TextStyle(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF171717),
                                    Color(0xFFFF6D16),
                                    Color(0xFFEF4778)
                                )
                            ),
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 27.sp,
                            letterSpacing = (-0.8).sp
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isVerifying) {
                            "Please wait while we securely verify your email address."
                        } else {
                            "Your email address has been successfully verified with Task Gullak."
                        },
                        color = Color(0xFF686868),
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    // Account Info (.action-info)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFFFFAF6))
                            .border(1.dp, TgActionOrange.copy(alpha = 0.09f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 13.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(11.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(35.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(Color(0xFFFFF0E5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MailOutline,
                                contentDescription = null,
                                tint = TgActionOrange,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "TASK GULLAK ACCOUNT",
                                color = Color(0xFF929292),
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                letterSpacing = 0.6.sp
                            )
                            Text(
                                text = userEmail.ifBlank { "Task Gullak account" },
                                color = Color(0xFF171717),
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(15.dp))

                    if (!isVerifying) {
                        // Status (.action-status.success)
                        StatusAlertBox(
                            title = "Verification successful",
                            message = "Your Task Gullak account is now email verified.",
                            type = "success",
                            modifier = Modifier.padding(bottom = 15.dp)
                        )

                        // Continue Button (.primary-action)
                        ShinyGradientButton(
                            text = "Continue to Task Gullak",
                            onClick = onContinueToApp,
                            gradientColors = listOf(
                                Color(0xFFFF7116),
                                Color(0xFFFF8534),
                                Color(0xFFFF4F81)
                            ),
                            height = 48.dp,
                            cornerRadius = 14.dp,
                            fontSizeSp = 13f,
                            testTag = "email_action_continue_button",
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(9.dp))

                        // Secondary Back to Login Button (.secondary-action)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFF171717).copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                .clickable { onBackToLogin() }
                                .testTag("email_action_login_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Back to Login",
                                color = Color(0xFF4B4B4B),
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Security Note (.security-note)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(Color(0xFFFAFAFA))
                            .border(1.dp, Color(0xFF171717).copy(alpha = 0.055f), RoundedCornerShape(13.dp))
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(Color(0xFFF1F1F1)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = Color(0xFF737373),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Secure verification",
                                color = Color(0xFF454545),
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "Your verification is securely processed with end-to-end encryption.",
                                color = Color(0xFF8A8A8A),
                                fontFamily = InterFontFamily,
                                fontSize = 9.5.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    // Footer (.action-footer)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 17.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "© Task Gullak",
                            color = Color(0xFF9A9A9A),
                            fontFamily = InterFontFamily,
                            fontSize = 9.5.sp
                        )
                        Text(
                            text = " • ",
                            color = Color(0xFFC5C5C5),
                            fontSize = 9.5.sp
                        )
                        Text(
                            text = "Terms",
                            color = Color(0xFF777777),
                            fontFamily = InterFontFamily,
                            fontSize = 9.5.sp,
                            modifier = Modifier.clickable { onNavigateToTerms() }
                        )
                        Text(
                            text = " • ",
                            color = Color(0xFFC5C5C5),
                            fontSize = 9.5.sp
                        )
                        Text(
                            text = "Privacy",
                            color = Color(0xFF777777),
                            fontFamily = InterFontFamily,
                            fontSize = 9.5.sp,
                            modifier = Modifier.clickable { onNavigateToPrivacy() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WarmOrangeBackground(
    testTag: String,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFFCFB),
                        Color(0xFFFFF4F0),
                        Color(0xFFFFFAF9)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag(testTag)
    ) {
        CssGridOverlay(
            gridSizeDp = 32.dp,
            lineColor = Color(0x09FF6B35)
        )

        // Floating Orange Orbs (.orb-one, .orb-two)
        Floating3DOrb(
            size = 58.dp,
            colors = listOf(
                Color.White,
                Color(0xFFFF8C5A).copy(alpha = 0.35f),
                Color(0xFFFF5014).copy(alpha = 0.08f)
            ),
            offsetX = 28.dp,
            offsetY = 95.dp,
            alignment = Alignment.TopStart,
            durationMillis = 4000
        )
        Floating3DOrb(
            size = 44.dp,
            colors = listOf(
                Color.White,
                Color(0xFFFF8C5A).copy(alpha = 0.35f),
                Color(0xFFFF5014).copy(alpha = 0.08f)
            ),
            offsetX = (-32).dp,
            offsetY = (-95).dp,
            alignment = Alignment.BottomEnd,
            durationMillis = 4400,
            delayMillis = 1200
        )

        content()
    }
}

@Composable
private fun StatusAlertBox(
    title: String,
    message: String,
    type: String,
    modifier: Modifier = Modifier
) {
    val bgColor = when (type) {
        "success" -> Color(0xFFF0FDF4)
        "error" -> Color(0xFFFEF2F2)
        else -> Color(0xFFF8FAFC)
    }
    val borderColor = when (type) {
        "success" -> Color(0xFFBBF7D0)
        "error" -> Color(0xFFFECACA)
        else -> Color(0xFFE2E8F0)
    }
    val textColor = when (type) {
        "success" -> Color(0xFF166534)
        "error" -> Color(0xFFB91C1C)
        else -> Color(0xFF475569)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(11.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            color = textColor,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
        Text(
            text = message,
            color = textColor,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            lineHeight = 14.sp
        )
    }
}
