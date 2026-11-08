package com.taskgullak.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.taskgullak.app.BuildConfig
import com.taskgullak.app.data.remote.FirebaseEmailDispatchService
import com.taskgullak.app.data.repository.TaskGullakRepository
import com.taskgullak.app.service.TaskGullakBackgroundSyncService
import com.taskgullak.app.service.TaskGullakFcmClient
import com.taskgullak.app.ui.screens.EmailActionScreen
import com.taskgullak.app.util.TaskGullakNotificationHelper
import com.taskgullak.app.ui.screens.ForgotPasswordScreen
import com.taskgullak.app.ui.screens.HomeScreen
import com.taskgullak.app.ui.screens.HomeViewModel
import com.taskgullak.app.ui.screens.LoginScreen
import com.taskgullak.app.ui.screens.OnboardingScreen
import com.taskgullak.app.ui.screens.PrivacyScreen
import com.taskgullak.app.ui.screens.SignupScreen
import com.taskgullak.app.ui.screens.SplashScreen
import com.taskgullak.app.ui.screens.TermsScreen
import com.taskgullak.app.ui.screens.VerifyEmailScreen
import com.taskgullak.app.ui.screens.signOutUser
import com.taskgullak.app.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

private const val TAG = "AppCheckInit"
private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

private const val PREFS_NAME = "task_gullak_prefs"
private const val KEY_ONBOARDING_COMPLETED = "tg_onboarding_completed"
private const val KEY_HAS_SEEN_ONBOARDING = "hasSeenOnboarding"

fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        if (BuildConfig.DEBUG) {
            val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)
            if (!debugToken.isNullOrEmpty()) {
                val persistenceKey = FirebaseApp.getInstance().persistenceKey
                val prefs = context.getSharedPreferences(
                    "$DEBUG_STORE_PREFIX$persistenceKey",
                    Context.MODE_PRIVATE
                )
                prefs.edit()
                    .putString(DEBUG_SECRET_KEY, debugToken)
                    .commit()
                Log.d(TAG, "App Check debug secret configured successfully.")
            } else {
                Log.w(TAG, "No debug token found in Intent extra.")
            }

            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            Log.d(TAG, "DebugAppCheckProviderFactory installed.")
        } else {
            val resId = context.resources.getIdentifier(RECAPTCHA_SITE_KEY_RES, "string", context.packageName)
            if (resId != 0) {
                val siteKey = context.getString(resId)
                if (siteKey.isNotBlank()) {
                    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                        RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                    )
                    Log.d(TAG, "RecaptchaAppCheckProviderFactory installed.")
                } else {
                    Log.e(TAG, "recaptcha_site_key resource string is blank.")
                }
            } else {
                Log.e(TAG, "recaptcha_site_key string resource not found in package.")
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
    }
}

sealed interface AppScreen {
    data object Splash : AppScreen
    data object Onboarding : AppScreen
    data object Login : AppScreen
    data object Signup : AppScreen
    data class ForgotPassword(val initialEmail: String = "") : AppScreen
    data class VerifyEmail(val email: String) : AppScreen
    data class EmailAction(val email: String) : AppScreen
    data class Terms(val fromSource: String, val returnScreen: AppScreen) : AppScreen
    data class Privacy(val fromSource: String, val returnScreen: AppScreen) : AppScreen
    data object Home : AppScreen
}

internal fun resolveInitialLoggedInState(
    context: Context,
    hasFirebaseUser: Boolean
): Boolean {
    FirebaseEmailDispatchService.cleanupTurn10SyncIfNeeded(context)
    val user = FirebaseAuth.getInstance().currentUser
    if (user != null) {
        val userEmail = user.email.orEmpty().trim().lowercase()
        val isGoogle = FirebaseEmailDispatchService.isGoogleAccountLocally(context, userEmail)
        if (isGoogle || user.isEmailVerified) {
            return FirebaseEmailDispatchService.isUserLoggedIn(context)
        }
        return false
    }
    return FirebaseEmailDispatchService.isUserLoggedIn(context)
}

internal fun resolveInitialScreen(isLoggedIn: Boolean): AppScreen {
    return if (isLoggedIn) AppScreen.Home else AppScreen.Splash
}

internal fun resolvePostSplashScreen(
    isLoggedIn: Boolean,
    hasSeenOnboarding: Boolean
): AppScreen {
    return when {
        isLoggedIn -> AppScreen.Home
        hasSeenOnboarding -> AppScreen.Login
        else -> AppScreen.Onboarding
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        captureDeepLinkReferralIfPresent(intent)
        enableEdgeToEdge()
        TaskGullakFcmClient.initialize(this)
        val openNotificationsFromIntent =
            intent?.getBooleanExtra(TaskGullakNotificationHelper.EXTRA_OPEN_NOTIFICATIONS, false) ?: false
        setContent {
            MyApplicationTheme {
                TaskGullakApp(initialOpenNotifications = openNotificationsFromIntent)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureDeepLinkReferralIfPresent(intent)
    }

    private fun captureDeepLinkReferralIfPresent(incomingIntent: Intent?) {
        val uri = incomingIntent?.data ?: return
        val refParam = uri.getQueryParameter("ref")
            ?: uri.getQueryParameter("code")
            ?: uri.getQueryParameter("referral")
            ?: uri.toString()
        if (refParam.isNotBlank()) {
            FirebaseEmailDispatchService.savePendingDeepLinkReferralCode(this, refParam)
        }
    }
}

@Composable
fun TaskGullakApp(initialOpenNotifications: Boolean = false) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val credentialManager = remember { CredentialManager.create(context) }

    LaunchedEffect(Unit) { FirebaseEmailDispatchService.cleanupTurn10SyncIfNeeded(context) }

    var currentUser by remember { mutableStateOf<FirebaseUser?>(FirebaseAuth.getInstance().currentUser) }
    var isSessionLoggedIn by remember {
        mutableStateOf(
            resolveInitialLoggedInState(
                context = context,
                hasFirebaseUser = FirebaseAuth.getInstance().currentUser != null
            )
        )
    }
    // Always show the Splash screen first on app launch; onSplashFinished routes to Home if logged in
    var currentScreen by remember {
        mutableStateOf<AppScreen>(AppScreen.Splash)
    }

    fun setOnboardingCompleted() {
        prefs.edit()
            .putBoolean(KEY_ONBOARDING_COMPLETED, true)
            .putBoolean(KEY_HAS_SEEN_ONBOARDING, true)
            .apply()
    }

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_HAS_SEEN_ONBOARDING, false) ||
            prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    // Listen to Firebase Auth state changes
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            currentUser = user
            if (user != null) {
                val userEmail = user.email.orEmpty().trim().lowercase()
                val userName = user.displayName.orEmpty()
                val isGoogle = FirebaseEmailDispatchService.isGoogleAccountLocally(context, userEmail)
                val isEmailVerified = user.isEmailVerified || isGoogle
                if (userEmail.isNotBlank() && isEmailVerified && FirebaseEmailDispatchService.isPasswordConfiguredLocally(context, userEmail)) {
                    isSessionLoggedIn = true
                    FirebaseEmailDispatchService.markEmailRegistered(
                        context = context,
                        email = userEmail,
                        fullName = userName
                    )
                    FirebaseEmailDispatchService.setUserLoggedIn(context, true)
                    setOnboardingCompleted()
                    if (currentScreen is AppScreen.Onboarding || currentScreen is AppScreen.Login) {
                        currentScreen = AppScreen.Home
                    }
                }
            }
        }
        FirebaseAuth.getInstance().addAuthStateListener(listener)
        onDispose {
            FirebaseAuth.getInstance().removeAuthStateListener(listener)
        }
    }

    // Initialize Task Gullak secondary Firebase config & FCM client on startup & sync known local accounts to Firestore
    LaunchedEffect(Unit) {
        TaskGullakFcmClient.initialize(context)
        FirebaseEmailDispatchService.ensureSecondaryFirebaseApp(context)
        FirebaseEmailDispatchService.syncAllKnownLocalAccountsToFirestore(context)
    }

    // Keep background Firestore notification listener & JobScheduler active whenever user is logged in
    LaunchedEffect(currentUser, isSessionLoggedIn) {
        if (currentUser != null || isSessionLoggedIn) {
            FirebaseEmailDispatchService.syncAllKnownLocalAccountsToFirestore(context)
            TaskGullakBackgroundSyncService.startAndEnsureActive(context)
        } else {
            TaskGullakBackgroundSyncService.stopSyncWhenSignedOut(context)
        }
    }

    fun completeLoginAndEnterHome() {
        setOnboardingCompleted()
        FirebaseEmailDispatchService.setUserLoggedIn(context, true)
        currentUser = FirebaseAuth.getInstance().currentUser
        isSessionLoggedIn = true
        TaskGullakBackgroundSyncService.startAndEnsureActive(context)
        currentScreen = AppScreen.Home
    }

    when (val screen = currentScreen) {
        is AppScreen.Splash -> {
            SplashScreen(
                onSplashFinished = {
                    val alreadyLoggedIn = resolveInitialLoggedInState(
                        context = context,
                        hasFirebaseUser = FirebaseAuth.getInstance().currentUser != null
                    )
                    isSessionLoggedIn = alreadyLoggedIn
                    currentScreen = if (alreadyLoggedIn) {
                        AppScreen.Home
                    } else {
                        AppScreen.Onboarding
                    }
                }
            )
        }

        is AppScreen.Onboarding -> {
            BackHandler(enabled = isSessionLoggedIn) {
                if (isSessionLoggedIn) {
                    currentScreen = AppScreen.Home
                }
            }
            OnboardingScreen(
                onFinishOnboarding = {
                    setOnboardingCompleted()
                    currentScreen = if (isSessionLoggedIn) AppScreen.Home else AppScreen.Login
                }
            )
        }

        is AppScreen.Login -> {
            BackHandler(enabled = !isOnboardingCompleted()) {
                currentScreen = AppScreen.Onboarding
            }
            LoginScreen(
                onLoginSuccess = {
                    completeLoginAndEnterHome()
                },
                onNavigateToSignup = {
                    currentScreen = AppScreen.Signup
                },
                onNavigateToForgotPassword = { email ->
                    currentScreen = AppScreen.ForgotPassword(email)
                },
                onNavigateToVerifyEmail = { email ->
                    currentScreen = AppScreen.VerifyEmail(email)
                },
                onNavigateToTerms = {
                    currentScreen = AppScreen.Terms("login", AppScreen.Login)
                },
                onNavigateToPrivacy = {
                    currentScreen = AppScreen.Privacy("login", AppScreen.Login)
                }
            )
        }

        is AppScreen.Signup -> {
            BackHandler {
                currentScreen = AppScreen.Login
            }
            SignupScreen(
                onSignupGoogleSuccess = {
                    completeLoginAndEnterHome()
                },
                onSignupEmailSubmitted = { _, email ->
                    setOnboardingCompleted()
                    currentScreen = AppScreen.VerifyEmail(email)
                },
                onNavigateToLogin = {
                    currentScreen = AppScreen.Login
                },
                onNavigateToTerms = {
                    currentScreen = AppScreen.Terms("signup", AppScreen.Signup)
                },
                onNavigateToPrivacy = {
                    currentScreen = AppScreen.Privacy("signup", AppScreen.Signup)
                }
            )
        }

        is AppScreen.ForgotPassword -> {
            BackHandler {
                currentScreen = AppScreen.Login
            }
            ForgotPasswordScreen(
                initialEmail = screen.initialEmail,
                onBackToLogin = {
                    currentScreen = AppScreen.Login
                }
            )
        }

        is AppScreen.VerifyEmail -> {
            BackHandler {
                currentScreen = AppScreen.Login
            }
            VerifyEmailScreen(
                userEmail = screen.email,
                onVerifiedContinue = {
                    completeLoginAndEnterHome()
                },
                onOpenEmailActionScreen = {
                    currentScreen = AppScreen.EmailAction(screen.email)
                },
                onBackToLogin = {
                    currentScreen = AppScreen.Login
                },
                onLogout = {
                    signOutUser(
                        context = context,
                        credentialManager = credentialManager,
                        onSignOutComplete = {
                            currentUser = null
                            isSessionLoggedIn = false
                            currentScreen = AppScreen.Login
                        },
                        scope = coroutineScope
                    )
                }
            )
        }

        is AppScreen.EmailAction -> {
            BackHandler {
                currentScreen = AppScreen.VerifyEmail(screen.email)
            }
            EmailActionScreen(
                userEmail = screen.email,
                onContinueToApp = {
                    completeLoginAndEnterHome()
                },
                onBackToLogin = {
                    currentScreen = AppScreen.Login
                },
                onNavigateToTerms = {
                    currentScreen = AppScreen.Terms("login", screen)
                },
                onNavigateToPrivacy = {
                    currentScreen = AppScreen.Privacy("login", screen)
                }
            )
        }

        is AppScreen.Terms -> {
            BackHandler {
                currentScreen = screen.returnScreen
            }
            TermsScreen(
                fromSource = screen.fromSource,
                onBack = {
                    currentScreen = screen.returnScreen
                },
                onSwitchToPrivacy = {
                    currentScreen = AppScreen.Privacy(screen.fromSource, screen.returnScreen)
                }
            )
        }

        is AppScreen.Privacy -> {
            BackHandler {
                currentScreen = screen.returnScreen
            }
            PrivacyScreen(
                fromSource = screen.fromSource,
                onBack = {
                    currentScreen = screen.returnScreen
                },
                onSwitchToTerms = {
                    currentScreen = AppScreen.Terms(screen.fromSource, screen.returnScreen)
                }
            )
        }

        is AppScreen.Home -> {
            val authenticatedUser = currentUser
            val hasActiveSession = authenticatedUser != null || isSessionLoggedIn ||
                FirebaseEmailDispatchService.isUserLoggedIn(context)

            if (!hasActiveSession) {
                LoginScreen(
                    onLoginSuccess = {
                        completeLoginAndEnterHome()
                    },
                    onNavigateToSignup = {
                        currentScreen = AppScreen.Signup
                    },
                    onNavigateToForgotPassword = { email ->
                        currentScreen = AppScreen.ForgotPassword(email)
                    },
                    onNavigateToVerifyEmail = { email ->
                        currentScreen = AppScreen.VerifyEmail(email)
                    },
                    onNavigateToTerms = {
                        currentScreen = AppScreen.Terms("login", AppScreen.Login)
                    },
                    onNavigateToPrivacy = {
                        currentScreen = AppScreen.Privacy("login", AppScreen.Login)
                    }
                )
            } else {
                // Instantiate HomeViewModel for both Google Sign-In (Firebase.auth.currentUser) and Email/Password sessions
                val activeUserId = authenticatedUser?.uid?.takeIf { it.isNotBlank() }
                    ?: FirebaseEmailDispatchService.resolveActiveFirestoreUserId(context)
                val homeViewModel: HomeViewModel? = if (!activeUserId.isNullOrBlank()) {
                    viewModel(
                        key = activeUserId,
                        factory = viewModelFactory {
                            initializer {
                                val app = checkNotNull(this[APPLICATION_KEY]) {
                                    "APPLICATION_KEY missing from CreationExtras"
                                }
                                val databaseId = runCatching {
                                    val resId = app.resources.getIdentifier("firestore_database_id", "string", app.packageName)
                                    if (resId != 0) app.getString(resId) else "(default)"
                                }.getOrDefault("(default)").ifBlank { "(default)" }
                                val db = FirebaseFirestore.getInstance(databaseId)
                                val resolvedName = FirebaseEmailDispatchService.getSavedUserName(context)
                                    .ifBlank { authenticatedUser?.displayName ?: "Savvy" }
                                val resolvedEmail = FirebaseEmailDispatchService.getSavedUserEmail(context)
                                    .ifBlank { authenticatedUser?.email ?: "member@taskgullak.app" }
                                HomeViewModel(
                                    repository = TaskGullakRepository(db, app),
                                    currentUserId = activeUserId,
                                    initialDisplayName = resolvedName,
                                    initialEmail = resolvedEmail,
                                    initialEmailVerified = true
                                )
                            }
                        }
                    )
                } else {
                    null
                }

                val savedName = FirebaseEmailDispatchService.getSavedUserName(context)
                    .ifBlank { authenticatedUser?.displayName ?: "Savvy" }
                val savedEmail = FirebaseEmailDispatchService.getSavedUserEmail(context)
                    .ifBlank { authenticatedUser?.email ?: "member@taskgullak.app" }

                HomeScreen(
                    viewModel = homeViewModel,
                    fallbackName = savedName,
                    fallbackEmail = savedEmail,
                    onNavigateToTerms = {
                        currentScreen = AppScreen.Terms("login", AppScreen.Home)
                    },
                    onNavigateToPrivacy = {
                        currentScreen = AppScreen.Privacy("login", AppScreen.Home)
                    },
                    onReplayOnboarding = {
                        currentScreen = AppScreen.Onboarding
                    },
                    onSignOut = {
                        signOutUser(
                            context = context,
                            credentialManager = credentialManager,
                            onSignOutComplete = {
                                currentUser = null
                                isSessionLoggedIn = false
                                currentScreen = AppScreen.Login
                            },
                            scope = coroutineScope
                        )
                    },
                    initialOpenNotifications = initialOpenNotifications
                )
            }
        }
    }
}
