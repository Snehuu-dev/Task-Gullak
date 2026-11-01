package com.taskgullak.app.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Sms
import com.taskgullak.app.R
import com.taskgullak.app.data.remote.FirebaseEmailDispatchService
import com.taskgullak.app.data.remote.PhoneCountryOption
import com.taskgullak.app.data.repository.TaskGullakRepository
import com.taskgullak.app.ui.components.CssGridOverlay
import com.taskgullak.app.ui.components.Floating3DOrb
import com.taskgullak.app.ui.components.ShinyGradientButton
import com.taskgullak.app.ui.theme.InterFontFamily
import com.taskgullak.app.ui.theme.PoppinsFontFamily
import com.taskgullak.app.ui.theme.TgAuthError
import com.taskgullak.app.ui.theme.TgAuthErrorDark
import com.taskgullak.app.ui.theme.TgAuthInputBg
import com.taskgullak.app.ui.theme.TgAuthPurple
import com.taskgullak.app.ui.theme.TgAuthTextMain
import com.taskgullak.app.ui.theme.TgAuthTextMuted
import com.taskgullak.app.ui.theme.TgAuthTextSoft
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.ceil

private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

data class PendingGooglePasswordSetup(
    val fullName: String,
    val email: String,
    val idToken: String
)

/**
 * Silent Auto-Sign-In on startup using GetGoogleIdOption in its own GetCredentialRequest.
 */
fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    FirebaseEmailDispatchService.cleanupTurn10SyncIfNeeded(context)
    if (FirebaseAuth.getInstance().currentUser != null || FirebaseEmailDispatchService.isUserLoggedIn(context)) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                val extractedEmail = googleCred.id
                val extractedName = googleCred.displayName ?: ""
                val existsInDb = FirebaseEmailDispatchService.checkGoogleEmailAccountExistsInDatabase(
                    context = context,
                    rawEmail = extractedEmail
                )
                if (!existsInDb) {
                    onUnauthenticated()
                    return@launch
                }
                FirebaseEmailDispatchService.saveGoogleAccountSession(
                    context = context,
                    fullName = extractedName,
                    email = extractedEmail
                )
                runCatching {
                    val authCredential = GoogleAuthProvider.getCredential(googleCred.idToken, null)
                    FirebaseAuth.getInstance().signInWithCredential(authCredential).await()
                }
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}

/**
 * Interactive Google Sign-In using GetSignInWithGoogleOption in its own GetCredentialRequest.
 * Checks if the Google email already exists in the database:
 * - If it exists: signs in and navigates immediately to HomeScreen.
 * - If it does not exist: asks the user to enter Password and Confirm Password first,
 *   and once confirmed, creates the account in the database and logs them in automatically.
 */
fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {},
    onRequirePasswordSetup: (PendingGooglePasswordSetup) -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val activityContext = context as? Activity ?: context
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                val extractedEmail = googleCred.id.trim().lowercase()
                val extractedName = googleCred.displayName.orEmpty().ifBlank {
                    extractedEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
                }

                val existsInDb = FirebaseEmailDispatchService.checkGoogleEmailAccountExistsInDatabase(context, extractedEmail)
                if (!existsInDb) {
                    onRequirePasswordSetup(
                        PendingGooglePasswordSetup(
                            fullName = extractedName,
                            email = extractedEmail,
                            idToken = googleCred.idToken
                        )
                    )
                    return@launch
                }

                // Save Google account session and ensure profile in database
                FirebaseEmailDispatchService.saveGoogleAccountSession(
                    context = context,
                    fullName = extractedName,
                    email = extractedEmail
                )

                runCatching {
                    val authCredential = GoogleAuthProvider.getCredential(googleCred.idToken, null)
                    FirebaseAuth.getInstance().signInWithCredential(authCredential).await()
                    val firebaseUser = FirebaseAuth.getInstance().currentUser
                    val resolvedName = firebaseUser?.displayName?.takeIf { it.isNotBlank() } ?: extractedName
                    val resolvedEmail = firebaseUser?.email?.takeIf { it.isNotBlank() } ?: extractedEmail
                    FirebaseEmailDispatchService.saveGoogleAccountSession(
                        context = context,
                        fullName = resolvedName,
                        email = resolvedEmail
                    )
                    TaskGullakRepository(context).ensureOrCreateProfile(
                        fullName = resolvedName,
                        email = resolvedEmail,
                        emailVerified = true,
                        explicitUserId = firebaseUser?.uid,
                        referralCode = FirebaseEmailDispatchService.getOrCreateReferralCode(context, resolvedEmail),
                        referredByCode = FirebaseEmailDispatchService.getReferredByCode(context, resolvedEmail),
                        creditReferrerOnLogin = true
                    )
                }.onFailure { err ->
                    Log.w("Auth", "Firebase credential link note (continuing with Google profile): ${err.message}")
                }

                FirebaseEmailDispatchService.setUserLoggedIn(context, true)
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.w("Auth", "Google Sign-In failed: ${e.message}", e)
            onAuthError(e.localizedMessage ?: "Unable to sign in with Google. Please try again.")
        }
    }
}

/**
 * Signs out of Firebase Auth and clears CredentialManager state.
 */
fun signOutUser(
    context: Context,
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    FirebaseAuth.getInstance().signOut()
    FirebaseEmailDispatchService.clearEmailSession(context)
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w("Auth", "Failed to clear credential state: ${e.message}")
        } finally {
            onSignOutComplete()
        }
    }
}

/**
 * Exact 100% UI replica of login.html + login.css + login.js
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignup: () -> Unit,
    onNavigateToForgotPassword: (String) -> Unit,
    onNavigateToVerifyEmail: (String) -> Unit,
    onNavigateToTerms: () -> Unit,
    onNavigateToPrivacy: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var statusBannerTitle by remember { mutableStateOf<String?>(null) }
    var statusBannerMessage by remember { mutableStateOf<String?>(null) }
    var unregisteredGmailNoticeEmail by remember { mutableStateOf<String?>(null) }

    var isLoginLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var pendingGoogleSetup by remember { mutableStateOf<PendingGooglePasswordSetup?>(null) }

    // Rate-limit guard from login.js (MAX_FAILED_ATTEMPTS = 5, LOCKOUT_TIME_MS = 15 min)
    var failedAttempts by remember { mutableIntStateOf(0) }
    var lockoutUntil by remember { mutableLongStateOf(0L) }

    fun registerFailedAttempt() {
        failedAttempts += 1
        if (failedAttempts >= 5) {
            lockoutUntil = System.currentTimeMillis() + 15 * 60 * 1000L
            emailError = "5 failed attempts reached. Login locked for 15 minutes."
        }
    }

    fun triggerGoogleSignIn() {
        if (isGoogleLoading || isLoginLoading) return
        isGoogleLoading = true
        emailError = null
        passwordError = null
        statusBannerTitle = null
        onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isGoogleLoading = false
                failedAttempts = 0
                lockoutUntil = 0L
                onLoginSuccess()
            },
            onAuthError = { errMsg ->
                isGoogleLoading = false
                statusBannerTitle = "Google Sign-In failed"
                statusBannerMessage = errMsg
            },
            scope = coroutineScope,
            onAuthCancelled = {
                isGoogleLoading = false
            },
            onRequirePasswordSetup = { setup ->
                isGoogleLoading = false
                pendingGoogleSetup = setup
            }
        )
    }

    fun handleEmailFormSubmit() {
        if (isLoginLoading || isGoogleLoading) return
        val now = System.currentTimeMillis()
        if (lockoutUntil > now) {
            val remainingMin = ceil((lockoutUntil - now) / 60000.0).toInt()
            emailError = "Too many failed attempts. Try again after $remainingMin minute(s)."
            return
        } else if (lockoutUntil != 0L && now >= lockoutUntil) {
            failedAttempts = 0
            lockoutUntil = 0L
        }

        val cleanEmail = email.trim().lowercase()
        emailError = null
        passwordError = null
        statusBannerTitle = null
        unregisteredGmailNoticeEmail = null

        if (cleanEmail.isEmpty()) {
            emailError = "Please enter your email address."
            return
        }

        if (!EMAIL_REGEX.matches(cleanEmail)) {
            emailError = "Please enter a valid email address."
            return
        }
        if (password.isEmpty()) {
            passwordError = "Please enter your password."
            return
        }

        isLoginLoading = true
        coroutineScope.launch {
            val result = FirebaseEmailDispatchService.signInEmailAccount(
                context = context,
                rawEmail = cleanEmail,
                password = password
            )
            if (!result.success) {
                isLoginLoading = false
                registerFailedAttempt()
                statusBannerTitle = result.title
                statusBannerMessage = result.message
                if (result.title.contains("Create an account", ignoreCase = true)) {
                    emailError = "Please create an account first."
                }
                return@launch
            }

            failedAttempts = 0
            lockoutUntil = 0L

            if (!result.emailVerified) {
                isLoginLoading = false
                onNavigateToVerifyEmail(cleanEmail)
                return@launch
            }

            FirebaseEmailDispatchService.setUserLoggedIn(context, true)
            isLoginLoading = false
            onLoginSuccess()
        }
    }

    AuthPurpleBackground(testTag = "login_screen") {
val activeGoogleSetup = pendingGoogleSetup
if (activeGoogleSetup != null) {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    GooglePasswordSetupCard(
      setup = activeGoogleSetup,
      onConfirmSuccess = { pendingGoogleSetup = null; onLoginSuccess() },
      onCancel = { pendingGoogleSetup = null }
    )
  }
} else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // BRAND HEADER (.login-brand)
                AuthBrandHeader()

                Spacer(modifier = Modifier.height(13.dp))

                // LOGIN CARD (.login-card)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 385.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = Color(0xFF472B6C).copy(alpha = 0.12f),
                        spotColor = Color(0xFF472B6C).copy(alpha = 0.15f)
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.97f))
                    .border(1.dp, Color(0xFF483265).copy(alpha = 0.085f), RoundedCornerShape(20.dp))
                    .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 16.dp)
            ) {
                // Card Top-Right Glow (.card-glow)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 70.dp, y = (-115).dp)
                        .size(170.dp)
                        .blur(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF835BDB).copy(alpha = 0.08f))
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    // HEADER (.login-header)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(TgAuthPurple.copy(alpha = 0.065f))
                            .border(1.dp, TgAuthPurple.copy(alpha = 0.11f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(TgAuthPurple)
                        )
                        Text(
                            text = "Secure Login",
                            color = Color(0xFF704DC5),
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 9.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(9.dp))

                    Text(
                        text = "Welcome back",
                        style = TextStyle(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF2B2435),
                                    Color(0xFF543795),
                                    Color(0xFF7955CC),
                                    Color(0xFF916FDF)
                                )
                            ),
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 23.sp,
                            lineHeight = 27.sp,
                            letterSpacing = (-0.8).sp
                        )
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = "Login to continue earning with Task Gullak.",
                        color = TgAuthTextSoft,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(13.dp))

                    // GLOBAL STATUS (.login-status)
                    if (statusBannerTitle != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 11.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(TgAuthError.copy(alpha = 0.05f))
                                .border(1.dp, TgAuthError.copy(alpha = 0.34f), RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(23.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(TgAuthError.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ErrorOutline,
                                    contentDescription = null,
                                    tint = TgAuthError,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = statusBannerTitle ?: "",
                                    color = TgAuthErrorDark,
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = statusBannerMessage ?: "",
                                    color = Color(0xFF97616B),
                                    fontFamily = InterFontFamily,
                                    fontSize = 9.sp,
                                    lineHeight = 13.sp
                                )
                                if (!unregisteredGmailNoticeEmail.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Create Account with ${unregisteredGmailNoticeEmail} →",
                                        color = Color(0xFF704AC8),
                                        fontFamily = PoppinsFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.5.sp,
                                        modifier = Modifier
                                            .clickable { onNavigateToSignup() }
                                            .testTag("login_unregistered_gmail_create_btn")
                                    )
                                }
                            }
                        }
                    }

                    // EMAIL INPUT (.input-group)
                    AuthInputField(
                        label = "Email address",
                        value = email,
                        onValueChange = {
                            email = it
                            emailError = null
                        },
                        placeholder = "Enter your email",
                        leadingIcon = Icons.Outlined.MailOutline,
                        errorMessage = emailError,
                        hintText = "Use the email linked to your Task Gullak account.",
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        testTag = "login_email_input"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // PASSWORD INPUT (.input-group)
                    AuthInputField(
                        label = "Password",
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = null
                        },
                        placeholder = "Enter your password ",
                        leadingIcon = Icons.Outlined.Lock,
                        errorMessage = passwordError,
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        testTag = "login_password_input"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // FORM OPTIONS (.form-options)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = Color(0xFF918B98),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Secure sign in",
                                color = Color(0xFF918B98),
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 9.sp
                            )
                        }

                        Text(
                            text = "Forgot password?",
                            color = Color(0xFF704AC8),
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 9.5.sp,
                            modifier = Modifier
                                .clickable { onNavigateToForgotPassword(email.trim()) }
                                .padding(vertical = 2.dp)
                                .testTag("forgot_password_link")
                        )
                    }

                    // LOGIN BUTTON (.login-button)
                    ShinyGradientButton(
                        text = if (isLoginLoading) "Logging in..." else "Login to Task Gullak",
                        onClick = { handleEmailFormSubmit() },
                        gradientColors = listOf(
                            Color(0xFF7046CE),
                            Color(0xFF8057DB),
                            Color(0xFF6A3EC5)
                        ),
                        height = 44.dp,
                        cornerRadius = 11.dp,
                        fontSizeSp = 11f,
                        isLoading = isLoginLoading,
                        testTag = "login_submit_button"
                    )

                    // DIVIDER (.auth-divider)
                    AuthOrDivider()

                    // GOOGLE LOGIN BUTTON (.google-login-button)
                    GoogleAuthButton(
                        text = if (isGoogleLoading) "Signing in..." else "Continue with Google",
                        isLoading = isGoogleLoading,
                        onClick = { triggerGoogleSignIn() },
                        testTag = "google_login_button"
                    )

                    // CREATE ACCOUNT SWITCH (.signup-switch)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Don't have an account? ",
                            color = Color(0xFF918B98),
                            fontFamily = InterFontFamily,
                            fontSize = 9.5.sp
                        )
                        Text(
                            text = "Create Account",
                            color = Color(0xFF704AC8),
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp,
                            modifier = Modifier
                                .clickable { onNavigateToSignup() }
                                .padding(vertical = 2.dp)
                                .testTag("navigate_to_signup_link")
                        )
                    }

                    // LEGAL FOOTER (.login-legal)
                    AuthLegalFooter(
                        onNavigateToTerms = onNavigateToTerms,
                        onNavigateToPrivacy = onNavigateToPrivacy
                    )
                }
            }
}
        }
    }
}

/**
 * Exact 100% UI replica of signup.html + signup.css + signup.js
 */
@Composable
fun SignupScreen(
    onSignupGoogleSuccess: () -> Unit,
    onSignupEmailSubmitted: (fullName: String, email: String) -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToTerms: () -> Unit,
    onNavigateToPrivacy: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var referralCodeInput by remember {
        mutableStateOf(FirebaseEmailDispatchService.getPendingDeepLinkReferralCode(context))
    }
    var termsAccepted by remember { mutableStateOf(false) }

    var selectedCountry by remember {
        mutableStateOf(FirebaseEmailDispatchService.SUPPORTED_PHONE_COUNTRIES.first())
    }
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var otpSentPhoneE164 by remember { mutableStateOf("") }
    var otpPreviewCode by remember { mutableStateOf("") }
    var isPhoneOtpVerified by remember { mutableStateOf(false) }
    var phoneOtpStatusMsg by remember { mutableStateOf<String?>(null) }
    var phoneOtpError by remember { mutableStateOf<String?>(null) }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var termsError by remember { mutableStateOf<String?>(null) }

    var isSignupLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var pendingGoogleSetup by remember { mutableStateOf<PendingGooglePasswordSetup?>(null) }

    // Exact password complexity check from signup.js
    fun checkPasswordComplexity(pwd: String): String? {
        if (pwd.length < 12) return "Password must be at least 12 characters long."
        if (!pwd.any { it in 'A'..'Z' }) return "Password must contain at least one uppercase letter (A-Z)."
        if (!pwd.any { it in 'a'..'z' }) return "Password must contain at least one lowercase letter (a-z)."
        if (!pwd.any { it in '0'..'9' }) return "Password must contain at least one number (0-9)."
        if (!pwd.any { it in "@\$!%*?&" }) return "Password must contain at least one special character (@, \$, !, %, *, ?, &)."
        return null
    }

    fun handleSignupSubmit() {
        if (isSignupLoading || isGoogleLoading) return
        val cleanName = fullName.trim()
        val cleanEmail = email.trim().lowercase()

        nameError = null
        emailError = null
        passwordError = null
        confirmPasswordError = null
        termsError = null
        phoneOtpError = null

        // 1. Name Check (Minimum 5 Characters)
        if (cleanName.length < 5) {
            nameError = "Full name must be at least 5 characters long."
            if (!termsAccepted) {
                termsError = "You must agree to the Terms of Service and Privacy Policy to create an account."
            }
            return
        }
        // 2. Email Validation
        if (cleanEmail.isEmpty() || !EMAIL_REGEX.matches(cleanEmail)) {
            emailError = "Please enter a valid email address."
            if (!termsAccepted) {
                termsError = "You must agree to the Terms of Service and Privacy Policy to create an account."
            }
            return
        }
        // 5. Mandatory Checkbox Check
        if (!termsAccepted) {
            termsError = "You must agree to the Terms of Service and Privacy Policy to create an account."
            return
        }

        isSignupLoading = true
        coroutineScope.launch {
            // 2b. Single Account Per Phone Check (1 device = 1 account)
            val singleDeviceErr = FirebaseEmailDispatchService.checkSingleAccountPerPhoneRule(
                context = context,
                candidateEmail = cleanEmail,
                candidatePhoneE164 = ""
            )
            if (singleDeviceErr != null) {
                isSignupLoading = false
                emailError = singleDeviceErr
                return@launch
            }

            try {
                val result = FirebaseEmailDispatchService.registerEmailAccountAndSendVerification(
                    context = context,
                    fullName = cleanName,
                    rawEmail = cleanEmail,
                    password = password
                )
                if (!result.success) {
                    emailError = result.message
                    return@launch
                }

                FirebaseEmailDispatchService.bindDeviceToAccount(
                    context = context,
                    email = cleanEmail,
                    phoneE164 = ""
                )
                if (referralCodeInput.isNotBlank()) {
                    FirebaseEmailDispatchService.applyReferralCodeOnSignup(
                        context = context,
                        enteredCodeOrUrl = referralCodeInput,
                        newUserEmail = cleanEmail,
                        newUserName = cleanName
                    )
                }

                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    runCatching {
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(cleanName)
                            .build()
                        currentUser.updateProfile(profileUpdates).await()
                        TaskGullakRepository(context).ensureOrCreateProfile(
                            fullName = cleanName,
                            email = cleanEmail,
                            emailVerified = result.emailVerified,
                            referralCode = FirebaseEmailDispatchService.getOrCreateReferralCode(context, cleanEmail),
                            referredByCode = FirebaseEmailDispatchService.getReferredByCode(context, cleanEmail),
                            creditReferrerOnLogin = true
                        )
                    }
                }
                if (result.emailVerified) {
                    onSignupGoogleSuccess()
                } else {
                    onSignupEmailSubmitted(cleanName, cleanEmail)
                }
            } finally {
                isSignupLoading = false
            }
        }
    }

    fun handleGoogleSignup() {
        if (isGoogleLoading || isSignupLoading) return
        isGoogleLoading = true
        termsError = null
        emailError = null
        onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isGoogleLoading = false
                onSignupGoogleSuccess()
            },
            onAuthError = { errMsg ->
                isGoogleLoading = false
                emailError = errMsg
            },
            scope = coroutineScope,
            onAuthCancelled = {
                isGoogleLoading = false
            },
            onRequirePasswordSetup = { setup ->
                isGoogleLoading = false
                pendingGoogleSetup = setup
            }
        )
    }

    AuthPurpleBackground(testTag = "signup_screen") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // BRAND HEADER (.signup-brand)
            AuthBrandHeader()

            Spacer(modifier = Modifier.height(11.dp))

            val activeGoogleSetup = pendingGoogleSetup
            if (activeGoogleSetup != null) {
                GooglePasswordSetupCard(
                    setup = activeGoogleSetup,
                    onConfirmSuccess = {
                        pendingGoogleSetup = null
                        onSignupGoogleSuccess()
                    },
                    onCancel = {
                        pendingGoogleSetup = null
                    }
                )
            } else {
            // SIGNUP CARD (.signup-card)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 385.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = Color(0xFF472B6C).copy(alpha = 0.12f),
                        spotColor = Color(0xFF472B6C).copy(alpha = 0.15f)
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.97f))
                    .border(1.dp, Color(0xFF483265).copy(alpha = 0.085f), RoundedCornerShape(20.dp))
                    .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // HEADER (.signup-header)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(TgAuthPurple.copy(alpha = 0.065f))
                            .border(1.dp, TgAuthPurple.copy(alpha = 0.11f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(TgAuthPurple)
                        )
                        Text(
                            text = "Create Account",
                            color = Color(0xFF704DC5),
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 9.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Join Task Gullak",
                        style = TextStyle(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF2B2435),
                                    Color(0xFF7955CC),
                                    Color(0xFF916FDF)
                                )
                            ),
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Create an account to start earning with Task Gullak.",
                        color = TgAuthTextSoft,
                        fontFamily = PoppinsFontFamily,
                        fontSize = 10.5.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // FULL NAME
                    AuthInputField(
                        label = "Full Name",
                        value = fullName,
                        onValueChange = {
                            fullName = it
                            nameError = null
                        },
                        placeholder = "Enter your full name",
                        leadingIcon = Icons.Outlined.PersonOutline,
                        errorMessage = nameError,
                        heightDp = 41,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        testTag = "signup_name_input"
                    )

                    Spacer(modifier = Modifier.height(9.dp))

                    // EMAIL ADDRESS
                    AuthInputField(
                        label = "Email address",
                        value = email,
                        onValueChange = {
                            email = it
                            emailError = null
                        },
                        placeholder = "Enter your email",
                        leadingIcon = Icons.Outlined.MailOutline,
                        errorMessage = emailError,
                        heightDp = 41,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        testTag = "signup_email_input"
                    )

                    Spacer(modifier = Modifier.height(9.dp))

                    // PASSWORD
                    AuthInputField(
                        label = "Password",
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = null
                        },
                        placeholder = "Create password (min 12 chars)",
                        leadingIcon = Icons.Outlined.Lock,
                        errorMessage = passwordError,
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
                        heightDp = 41,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        testTag = "signup_password_input"
                    )

                    Spacer(modifier = Modifier.height(9.dp))

                    // CONFIRM PASSWORD
                    AuthInputField(
                        label = "Confirm Password",
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            confirmPasswordError = null
                        },
                        placeholder = "Re-enter your password",
                        leadingIcon = Icons.Outlined.Lock,
                        errorMessage = confirmPasswordError,
                        isPassword = true,
                        passwordVisible = confirmPasswordVisible,
                        onTogglePasswordVisibility = { confirmPasswordVisible = !confirmPasswordVisible },
                        heightDp = 41,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        testTag = "signup_confirm_password_input"
                    )

                    Spacer(modifier = Modifier.height(9.dp))

                    // REFERRAL CODE (OPTIONAL - taskgullak.space)
                    AuthInputField(
                        label = "Referral Code or Link (Optional)",
                        value = referralCodeInput,
                        onValueChange = { referralCodeInput = it },
                        placeholder = "e.g. RW5M4XPG or taskgullak.space/?ref=...",
                        leadingIcon = Icons.Outlined.CardGiftcard,
                        errorMessage = null,
                        heightDp = 41,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        testTag = "signup_referral_code_input"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // TERMS AND PRIVACY CHECKBOX (.terms-group)
                    val checkboxInteractionSource = remember { MutableInteractionSource() }
                    val termsLinkInteractionSource = remember { MutableInteractionSource() }
                    val privacyLinkInteractionSource = remember { MutableInteractionSource() }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (termsAccepted) TgAuthPurple else TgAuthInputBg)
                                    .border(
                                        width = 1.dp,
                                        color = when {
                                            termsAccepted -> TgAuthPurple
                                            !termsError.isNullOrEmpty() -> TgAuthError
                                            else -> Color(0xFF4C3667).copy(alpha = 0.28f)
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .clickable(
                                        interactionSource = checkboxInteractionSource,
                                        indication = null
                                    ) {
                                        termsAccepted = !termsAccepted
                                        termsError = null
                                    }
                                    .testTag("signup_terms_checkbox"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (termsAccepted) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "I agree to Task Gullak's ",
                                    color = Color(0xFF6A6373),
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 8.5.sp
                                )
                                Text(
                                    text = "Terms of Service",
                                    color = Color(0xFF704AC8),
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 8.5.sp,
                                    modifier = Modifier.clickable(
                                        interactionSource = termsLinkInteractionSource,
                                        indication = null
                                    ) { onNavigateToTerms() }
                                )
                                Text(
                                    text = " and ",
                                    color = Color(0xFF6A6373),
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 8.5.sp
                                )
                                Text(
                                    text = "Privacy Policy",
                                    color = Color(0xFF704AC8),
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 8.5.sp,
                                    modifier = Modifier.clickable(
                                        interactionSource = privacyLinkInteractionSource,
                                        indication = null
                                    ) { onNavigateToPrivacy() }
                                )
                            }
                        }

                        if (!termsError.isNullOrEmpty()) {
                            Text(
                                text = termsError ?: "",
                                color = TgAuthError,
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 8.5.sp,
                                modifier = Modifier.padding(top = 3.dp)
                            )
                        }
                    }

                    // SIGNUP BUTTON (.login-button)
                    ShinyGradientButton(
                        text = if (isSignupLoading) "Creating Account..." else "Create Account with Task Gullak",
                        onClick = { handleSignupSubmit() },
                        gradientColors = listOf(
                            Color(0xFF7046CE),
                            Color(0xFF8057DB),
                            Color(0xFF6A3EC5)
                        ),
                        height = 42.dp,
                        cornerRadius = 11.dp,
                        fontSizeSp = 10.5f,
                        isLoading = isSignupLoading,
                        testTag = "signup_submit_button"
                    )

                    // DIVIDER (.auth-divider)
                    AuthOrDivider(verticalMarginDp = 10)

                    // GOOGLE SIGNUP (.google-signup-button)
                    GoogleAuthButton(
                        text = if (isGoogleLoading) "Signing up..." else "Sign up with Google",
                        isLoading = isGoogleLoading,
                        onClick = { handleGoogleSignup() },
                        heightDp = 40,
                        testTag = "google_signup_button"
                    )

                    // SWITCH TO LOGIN (.signup-switch)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Already have an account? ",
                            color = Color(0xFF918B98),
                            fontFamily = InterFontFamily,
                            fontSize = 9.sp
                        )
                        Text(
                            text = "Log in",
                            color = Color(0xFF704AC8),
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            modifier = Modifier
                                .clickable { onNavigateToLogin() }
                                .padding(vertical = 2.dp)
                                .testTag("navigate_to_login_link")
                        )
                    }

                    // LEGAL FOOTER (.signup-legal)
                    AuthLegalFooter(
                        onNavigateToTerms = onNavigateToTerms,
                        onNavigateToPrivacy = onNavigateToPrivacy
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun AuthPurpleBackground(
    testTag: String,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFCFBFF),
                        Color(0xFFF6F2FB),
                        Color(0xFFFBFAFF)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag(testTag)
    ) {
        CssGridOverlay(
            gridSizeDp = 30.dp,
            lineColor = Color(0x0A664D91)
        )

        // Floating Purple Orbs (.orb-one, .orb-two, .orb-three)
        Floating3DOrb(
            size = 105.dp,
            colors = listOf(
                Color.White,
                Color(0xFFBAA0EF).copy(alpha = 0.55f),
                Color(0xFF7D54D2).copy(alpha = 0.15f)
            ),
            offsetX = (-48).dp,
            offsetY = 65.dp,
            alignment = Alignment.TopStart,
            durationMillis = 4200
        )
        Floating3DOrb(
            size = 70.dp,
            colors = listOf(
                Color.White,
                Color(0xFFBAA0EF).copy(alpha = 0.55f),
                Color(0xFF7D54D2).copy(alpha = 0.15f)
            ),
            offsetX = 25.dp,
            offsetY = 0.dp,
            alignment = Alignment.CenterEnd,
            durationMillis = 3800,
            delayMillis = 800
        )
        Floating3DOrb(
            size = 46.dp,
            colors = listOf(
                Color.White,
                Color(0xFFBAA0EF).copy(alpha = 0.55f),
                Color(0xFF7D54D2).copy(alpha = 0.15f)
            ),
            offsetX = 24.dp,
            offsetY = (-52).dp,
            alignment = Alignment.BottomStart,
            durationMillis = 4600,
            delayMillis = 1400
        )

        content()
    }
}

@Composable
private fun AuthBrandHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 385.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = Color(0xFF4C2F74).copy(alpha = 0.18f))
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.96f))
                .border(1.dp, Color(0xFF4D346F).copy(alpha = 0.10f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.task_gullak_logo),
                contentDescription = "Task Gullak logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(verticalArrangement = Arrangement.Center) {
            Text(
                text = "Task Gullak",
                style = TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF4C2B96),
                            Color(0xFF7250CE),
                            Color(0xFF9270DF),
                            Color(0xFF633DBD)
                        )
                    ),
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    letterSpacing = (-0.6).sp
                )
            )
            Text(
                text = "EARN • COMPLETE • GROW",
                color = Color(0xFF918B9C),
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 8.sp,
                letterSpacing = 0.8.sp
            )
        }
    }
}

@Composable
private fun AuthInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    errorMessage: String?,
    hintText: String? = null,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePasswordVisibility: (() -> Unit)? = null,
    heightDp: Int = 43,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    testTag: String
) {
    val hasError = !errorMessage.isNullOrEmpty()
    val borderColor = if (hasError) {
        TgAuthError.copy(alpha = 0.58f)
    } else {
        Color(0xFF4C3667).copy(alpha = 0.12f)
    }
    val boxBg = if (hasError) {
        TgAuthError.copy(alpha = 0.03f)
    } else {
        TgAuthInputBg
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = Color(0xFF575060),
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(heightDp.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(boxBg)
                .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(38.dp)
                    .height(heightDp.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (hasError) TgAuthError else Color(0xFF89818F),
                    modifier = Modifier.size(16.dp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = if (isPassword) 4.dp else 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = Color(0xFFAAA4B1),
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = TgAuthTextMain,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    ),
                    cursorBrush = SolidColor(TgAuthPurple),
                    visualTransformation = if (isPassword && !passwordVisible) {
                        PasswordVisualTransformation()
                    } else {
                        VisualTransformation.None
                    },
                    keyboardOptions = keyboardOptions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(testTag)
                )
            }

            if (isPassword && onTogglePasswordVisibility != null) {
                IconButton(
                    onClick = onTogglePasswordVisibility,
                    modifier = Modifier
                        .size(34.dp)
                        .padding(end = 4.dp)
                ) {
                    Icon(
                        imageVector = if (passwordVisible) {
                            Icons.Outlined.Visibility
                        } else {
                            Icons.Outlined.VisibilityOff
                        },
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = Color(0xFF89818F),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (hintText != null) {
            Text(
                text = hintText,
                color = Color(0xFFA19BA8),
                fontFamily = InterFontFamily,
                fontSize = 8.5.sp,
                lineHeight = 12.sp,
                modifier = Modifier.padding(top = 3.dp, start = 1.dp)
            )
        }

        if (hasError) {
            Text(
                text = errorMessage ?: "",
                color = TgAuthError,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 9.sp,
                lineHeight = 13.sp,
                modifier = Modifier.padding(top = 3.dp, start = 1.dp)
            )
        }
    }
}

@Composable
private fun AuthOrDivider(verticalMarginDp: Int = 12) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = verticalMarginDp.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Color(0xFF483265).copy(alpha = 0.08f)
        )
        Text(
            text = "OR",
            color = Color(0xFFAAA4B1),
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 9.sp,
            letterSpacing = 0.8.sp
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Color(0xFF483265).copy(alpha = 0.08f)
        )
    }
}

@Composable
fun GoogleAuthButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    heightDp: Int = 42,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFF483265).copy(alpha = 0.11f), RoundedCornerShape(11.dp))
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(horizontal = 12.dp)
            .testTag(testTag),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(15.dp),
                color = TgAuthPurple,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else {
            GoogleColorGIcon(modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            color = Color(0xFF4A4352),
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.5.sp
        )
    }
}

@Composable
private fun GoogleColorGIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = -145f,
            sweepAngle = 105f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.22f)
        )
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 145f,
            sweepAngle = 70f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.22f)
        )
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 45f,
            sweepAngle = 100f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.22f)
        )
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -15f,
            sweepAngle = 65f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.22f)
        )
        val barPath = Path().apply {
            moveTo(w * 0.50f, h * 0.42f)
            lineTo(w * 0.94f, h * 0.42f)
            lineTo(w * 0.94f, h * 0.60f)
            lineTo(w * 0.50f, h * 0.60f)
            close()
        }
        drawPath(path = barPath, color = Color(0xFF4285F4))
    }
}

@Composable
private fun AuthLegalFooter(
    onNavigateToTerms: () -> Unit,
    onNavigateToPrivacy: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalDivider(
            color = Color(0xFF483265).copy(alpha = 0.065f),
            modifier = Modifier.padding(bottom = 9.dp)
        )
        Text(
            text = "By continuing, you agree to Task Gullak's",
            color = TgAuthTextMuted,
            fontFamily = InterFontFamily,
            fontSize = 8.5.sp,
            textAlign = TextAlign.Center
        )
        Row(
            modifier = Modifier.padding(top = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Terms of Service",
                color = Color(0xFF827B8B),
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 8.5.sp,
                modifier = Modifier
                    .clickable { onNavigateToTerms() }
                    .testTag("footer_terms_link")
            )
            Text(
                text = "•",
                color = Color(0xFFB8B2BD),
                fontSize = 8.5.sp
            )
            Text(
                text = "Privacy Policy",
                color = Color(0xFF827B8B),
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 8.5.sp,
                modifier = Modifier
                    .clickable { onNavigateToPrivacy() }
                    .testTag("footer_privacy_link")
            )
        }
    }
}

@Composable
private fun GooglePasswordSetupCard(
    setup: PendingGooglePasswordSetup,
    onConfirmSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var referralCodeInput by remember {
        mutableStateOf(FirebaseEmailDispatchService.getPendingDeepLinkReferralCode(context))
    }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var selectedCountry by remember {
        mutableStateOf(FirebaseEmailDispatchService.SUPPORTED_PHONE_COUNTRIES.first())
    }
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var otpSentPhoneE164 by remember { mutableStateOf("") }
    var otpPreviewCode by remember { mutableStateOf("") }
    var isPhoneOtpVerified by remember { mutableStateOf(false) }
    var phoneOtpStatusMsg by remember { mutableStateOf<String?>(null) }
    var phoneOtpError by remember { mutableStateOf<String?>(null) }

    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    fun handleConfirmPassword() {
        if (isSaving) return
        passwordError = null
        confirmPasswordError = null
        phoneOtpError = null

        val singleDeviceErr = FirebaseEmailDispatchService.checkSingleAccountPerPhoneRule(
            context = context,
            candidateEmail = setup.email,
            candidatePhoneE164 = ""
        )
        if (singleDeviceErr != null) {
            passwordError = singleDeviceErr
            return
        }

        if (password.isEmpty()) {
            passwordError = "Please enter a password."
            return
        }
        // 12-character password rule enforced on Set Up Password page too
        if (password.length < 12) {
            passwordError = "Password must be at least 12 characters long."
            return
        }
        if (confirmPassword.isEmpty()) {
            confirmPasswordError = "Please confirm your password."
            return
        }
        if (password != confirmPassword) {
            confirmPasswordError = "Passwords do not match."
            return
        }

        isSaving = true
        coroutineScope.launch {
            try {
                FirebaseEmailDispatchService.completeGoogleAccountPasswordSetup(
                    context = context,
                    fullName = setup.fullName,
                    rawEmail = setup.email,
                    password = password
                )
                FirebaseEmailDispatchService.bindDeviceToAccount(
                    context = context,
                    email = setup.email,
                    phoneE164 = ""
                )
                if (referralCodeInput.isNotBlank()) {
                    FirebaseEmailDispatchService.applyReferralCodeOnSignup(
                        context = context,
                        enteredCodeOrUrl = referralCodeInput,
                        newUserEmail = setup.email,
                        newUserName = setup.fullName
                    )
                }
                runCatching {
                    val authCredential = GoogleAuthProvider.getCredential(setup.idToken, null)
                    FirebaseAuth.getInstance().signInWithCredential(authCredential).await()
                    val firebaseUser = FirebaseAuth.getInstance().currentUser
                    if (firebaseUser != null) {
                        val resolvedName = firebaseUser.displayName?.takeIf { it.isNotBlank() } ?: setup.fullName
                        val resolvedEmail = firebaseUser.email?.takeIf { it.isNotBlank() } ?: setup.email
                        FirebaseEmailDispatchService.saveGoogleAccountSession(
                            context = context,
                            fullName = resolvedName,
                            email = resolvedEmail
                        )
                        TaskGullakRepository(context).ensureOrCreateProfile(
                            fullName = resolvedName,
                            email = resolvedEmail,
                            emailVerified = true,
                            referralCode = FirebaseEmailDispatchService.getOrCreateReferralCode(context, resolvedEmail),
                            referredByCode = FirebaseEmailDispatchService.getReferredByCode(context, resolvedEmail),
                            creditReferrerOnLogin = true
                        )
                    }
                }.onFailure { err ->
                    Log.w("Auth", "Google password setup Firebase link note: ${err.message}")
                }
                FirebaseEmailDispatchService.setUserLoggedIn(context, true)
                onConfirmSuccess()
            } finally {
                isSaving = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 385.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color(0xFF472B6C).copy(alpha = 0.12f),
                spotColor = Color(0xFF472B6C).copy(alpha = 0.15f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.97f))
            .border(1.dp, Color(0xFF483265).copy(alpha = 0.085f), RoundedCornerShape(20.dp))
            .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 16.dp)
            .testTag("google_password_setup_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(TgAuthPurple.copy(alpha = 0.065f))
                    .border(1.dp, TgAuthPurple.copy(alpha = 0.11f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 9.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(TgAuthPurple)
                )
                Text(
                    text = "Complete Account Setup",
                    color = Color(0xFF704DC5),
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 9.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Set Your Password",
                style = TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF2B2435),
                            Color(0xFF543795),
                            Color(0xFF7955CC),
                            Color(0xFF916FDF)
                        )
                    ),
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    lineHeight = 26.sp,
                    letterSpacing = (-0.6).sp
                )
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "Create and confirm a 12+ character password & verify your phone for ${setup.email}.",
                color = TgAuthTextSoft,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 10.5.sp,
                lineHeight = 15.5.sp
            )

            Spacer(modifier = Modifier.height(13.dp))

            AuthInputField(
                label = "Password (Min 12 Characters)",
                value = password,
                onValueChange = {
                    password = it
                    passwordError = null
                },
                placeholder = "Enter password (min 12 chars)",
                leadingIcon = Icons.Outlined.Lock,
                errorMessage = passwordError,
                isPassword = true,
                passwordVisible = passwordVisible,
                onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                testTag = "google_setup_password_input"
            )

            Spacer(modifier = Modifier.height(10.dp))

            AuthInputField(
                label = "Confirm Password",
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    confirmPasswordError = null
                },
                placeholder = "Re-enter your 12+ char password",
                leadingIcon = Icons.Outlined.Lock,
                errorMessage = confirmPasswordError,
                isPassword = true,
                passwordVisible = confirmPasswordVisible,
                onTogglePasswordVisibility = { confirmPasswordVisible = !confirmPasswordVisible },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                testTag = "google_setup_confirm_password_input"
            )

            Spacer(modifier = Modifier.height(10.dp))

            AuthInputField(
                label = "Referral Code or Link (Optional)",
                value = referralCodeInput,
                onValueChange = { referralCodeInput = it },
                placeholder = "e.g. RW5M4XPG or taskgullak.space/?ref=...",
                leadingIcon = Icons.Outlined.CardGiftcard,
                errorMessage = null,
                heightDp = 41,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                testTag = "google_setup_referral_code_input"
            )

            Spacer(modifier = Modifier.height(14.dp))

            ShinyGradientButton(
                text = if (isSaving) "Saving password..." else "Confirm Password & Continue",
                onClick = { handleConfirmPassword() },
                gradientColors = listOf(
                    Color(0xFF7046CE),
                    Color(0xFF8057DB),
                    Color(0xFF6A3EC5)
                ),
                height = 44.dp,
                cornerRadius = 11.dp,
                fontSizeSp = 11f,
                isLoading = isSaving,
                testTag = "google_setup_submit_button"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 11.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Use a different account? ",
                    color = Color(0xFF918B98),
                    fontFamily = InterFontFamily,
                    fontSize = 9.5.sp
                )
                Text(
                    text = "Back",
                    color = Color(0xFF704AC8),
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.5.sp,
                    modifier = Modifier
                        .clickable(enabled = !isSaving) { onCancel() }
                        .padding(vertical = 2.dp)
                        .testTag("google_setup_cancel_button")
                )
            }
        }
    }
}

@Composable
private fun PhoneOtpVerificationSection(
    selectedCountry: PhoneCountryOption,
    onSelectCountry: (PhoneCountryOption) -> Unit,
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    otpCode: String,
    onOtpCodeChange: (String) -> Unit,
    otpSentPhoneE164: String,
    otpPreviewCode: String,
    isPhoneOtpVerified: Boolean,
    statusMessage: String?,
    errorMessage: String?,
    onSendOtp: () -> Unit,
    onVerifyOtp: () -> Unit,
    tagPrefix: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF7F4FC))
            .border(1.dp, Color(0xFF7046CE).copy(alpha = 0.16f), RoundedCornerShape(12.dp))
            .padding(10.dp)
            .testTag("${tagPrefix}_phone_otp_section")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mobile SMS Verification",
                color = Color(0xFF4A3B69),
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (isPhoneOtpVerified) Color(0xFFDCFCE7) else Color(0xFFEDE9FE)
                    )
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isPhoneOtpVerified) "Verified ✓" else "1 Account / Phone",
                    color = if (isPhoneOtpVerified) Color(0xFF15803D) else Color(0xFF6D28D9),
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Scroll & tap your country flag (no need to type country code):",
            color = Color(0xFF7C738A),
            fontFamily = InterFontFamily,
            fontSize = 8.5.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        // Scrollable Country Flag Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .testTag("${tagPrefix}_country_flag_scroll"),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FirebaseEmailDispatchService.SUPPORTED_PHONE_COUNTRIES.forEach { country ->
                val isSelected = country.isoCode == selectedCountry.isoCode
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (isSelected) Color(0xFF7046CE) else Color.White
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) Color(0xFF7046CE) else Color(0xFFD8D0E5),
                            shape = RoundedCornerShape(999.dp)
                        )
                        .clickable { onSelectCountry(country) }
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                        .testTag("${tagPrefix}_flag_${country.isoCode}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = country.flagEmoji,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "${country.isoCode} ${country.dialCode}",
                        color = if (isSelected) Color.White else Color(0xFF3E354B),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 8.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Phone Number Row with selected Flag + Dial Code chip and Send OTP button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .height(39.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFF4C3667).copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = selectedCountry.flagEmoji, fontSize = 13.sp)
                Text(
                    text = selectedCountry.dialCode,
                    color = Color(0xFF3B2E58),
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(39.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFF4C3667).copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (phoneNumber.isEmpty()) {
                    Text(
                        text = "Mobile number (${selectedCountry.countryName})",
                        color = Color(0xFFAAA4B1),
                        fontFamily = InterFontFamily,
                        fontSize = 10.sp
                    )
                }
                BasicTextField(
                    value = phoneNumber,
                    onValueChange = onPhoneNumberChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = TgAuthTextMain,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done
                    ),
                    cursorBrush = SolidColor(TgAuthPurple),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("${tagPrefix}_phone_input")
                )
            }

            Box(
                modifier = Modifier
                    .height(39.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF7046CE))
                    .clickable { onSendOtp() }
                    .padding(horizontal = 10.dp)
                    .testTag("${tagPrefix}_send_otp_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (otpSentPhoneE164.isNotBlank()) "Resend" else "Send OTP",
                    color = Color.White,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.5.sp
                )
            }
        }

        if (otpSentPhoneE164.isNotBlank() && !isPhoneOtpVerified) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFF7046CE).copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (otpCode.isEmpty()) {
                        Text(
                            text = "Enter 6-digit SMS OTP",
                            color = Color(0xFFAAA4B1),
                            fontFamily = InterFontFamily,
                            fontSize = 10.sp
                        )
                    }
                    BasicTextField(
                        value = otpCode,
                        onValueChange = onOtpCodeChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            color = TgAuthTextMain,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 2.sp
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        cursorBrush = SolidColor(TgAuthPurple),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("${tagPrefix}_otp_input")
                    )
                }

                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF16A34A))
                        .clickable { onVerifyOtp() }
                        .padding(horizontal = 12.dp)
                        .testTag("${tagPrefix}_verify_otp_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Verify OTP",
                        color = Color.White,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.5.sp
                    )
                }
            }

            if (otpPreviewCode.isNotBlank()) {
                Spacer(modifier = Modifier.height(5.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEDE9FE))
                        .clickable {
                            onOtpCodeChange(otpPreviewCode)
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("${tagPrefix}_autofill_otp_chip"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SMS Verification Code: $otpPreviewCode",
                        color = Color(0xFF5B21B6),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                    Text(
                        text = "Tap to Auto-Fill",
                        color = Color(0xFF7C3AED),
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 8.5.sp
                    )
                }
            }
        }

        if (!statusMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = statusMessage,
                color = if (isPhoneOtpVerified) Color(0xFF15803D) else Color(0xFF5B21B6),
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 8.5.sp
            )
        }

        if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                color = TgAuthError,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 8.5.sp
            )
        }
    }
}


