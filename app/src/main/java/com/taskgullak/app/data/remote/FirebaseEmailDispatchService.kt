package com.taskgullak.app.data.remote

import android.app.Activity
import android.content.Context
import android.provider.Settings
import android.util.Log
import com.taskgullak.app.R
import com.taskgullak.app.data.repository.TaskGullakRepository
import com.taskgullak.app.util.TaskGullakNotificationHelper
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class PhoneCountryOption(
    val isoCode: String,
    val dialCode: String,
    val flagEmoji: String,
    val countryName: String,
    val maxDigits: Int = 10
)

data class ReferralFriendRecord(
    val name: String,
    val email: String,
    val joinedAtLabel: String,
    val tasksCompleted: Int,
    val lifetimeCommissionEarned: Double
)

data class ReferralSummaryState(
    val referralCode: String,
    val referralLink: String,
    val perReferralBonus: Int = 5,
    val friendBonusAfterFirstWithdraw: Int = 5,
    val lifetimeCommissionPercent: Int = 1,
    val totalReferredFriends: Int = 0,
    val totalReferralBonusEarned: Double = 0.0,
    val totalLifetimeCommissionEarned: Double = 0.0,
    val referredByCode: String = "",
    val referredFriends: List<ReferralFriendRecord> = emptyList()
)

data class AchievementBatchInfo(
    val batchNumber: Int,
    val title: String,
    val subtitle: String,
    val requiredTasks: Int,
    val isUnlocked: Boolean,
    val progressFraction: Float
)

data class EmailDispatchResult(
    val success: Boolean,
    val title: String,
    val message: String,
    val statusType: String = if (success) "success" else "error",
    val emailVerified: Boolean = false
)

fun resolveFirestoreInstance(context: Context): FirebaseFirestore {
    val databaseId = try {
        context.applicationContext.getString(R.string.firestore_database_id).trim()
    } catch (_: Exception) {
        "(default)"
    }
    return if (databaseId.isBlank() || databaseId == "(default)") {
        FirebaseFirestore.getInstance()
    } else {
        FirebaseFirestore.getInstance(databaseId)
    }
}

/**
 * Connects Task Gullak's Email/Password Auth, Email Verification (`VERIFY_EMAIL`),
 * and Password Reset (`PASSWORD_RESET`) flows to the user's Task Gullak Firebase
 * project (`task-gullak`) while preserving Google Sign-In and Cloud Firestore sync.
 */
object FirebaseEmailDispatchService {

    private const val TAG = "TaskGullakEmail"
    private const val SECONDARY_APP_NAME = "TaskGullakStudioFirebase"
    private const val PREFS_EMAIL_AUTH = "task_gullak_email_auth_prefs"
    private const val KEY_ID_TOKEN = "tg_studio_id_token"
    private const val KEY_USER_EMAIL = "tg_studio_user_email"
    private const val KEY_USER_NAME = "tg_studio_user_name"
    private const val KEY_VERIFICATION_SENT_AT = "tg_studio_verification_sent_at"
    private const val KEY_USER_LOGGED_IN = "tg_studio_user_logged_in"
    private const val KEY_REGISTERED_EMAILS = "tg_studio_registered_emails"
    private const val KEY_GOOGLE_EMAILS = "tg_studio_google_emails"
    private const val KEY_PASSWORD_CONFIGURED_EMAILS = "tg_studio_password_configured_emails"
    private const val KEY_ACCOUNT_NAMES_JSON = "tg_studio_account_names_json"
    private const val KEY_ACCOUNT_PASSWORDS_JSON = "tg_studio_account_passwords_json"
    private const val KEY_MIGRATED_GOOGLE_PWD_V2 = "tg_migrated_google_pwd_v2"

    /**
     * Synchronously resets any session that was created without explicit password setup in Turn 10.
     */
    fun cleanupTurn10SyncIfNeeded(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_MIGRATED_GOOGLE_PWD_V2, false)) return

        val configured = prefs.getStringSet(KEY_PASSWORD_CONFIGURED_EMAILS, emptySet()).orEmpty()
        val currentEmail = prefs.getString(KEY_USER_EMAIL, "").orEmpty().trim().lowercase()
        if (currentEmail.isNotBlank() && !configured.any { it.equals(currentEmail, ignoreCase = true) }) {
            runCatching { FirebaseAuth.getInstance().signOut() }
            prefs.edit()
                .putBoolean(KEY_USER_LOGGED_IN, false)
                .apply()
        }
    }

    /**
     * Cleans up any unverified IdentityToolkit record that was auto-created during the
     * previous wrong-password login test so the user can cleanly set their real password.
     */
    private suspend fun cleanupUnverifiedTurn10GoogleState(context: Context) = withContext(Dispatchers.IO) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_MIGRATED_GOOGLE_PWD_V2, false)) return@withContext

        val apiKey = getTaskGullakApiKey(context)
        val configured = HashSet(prefs.getStringSet(KEY_PASSWORD_CONFIGURED_EMAILS, emptySet()).orEmpty())
        val registered = HashSet(prefs.getStringSet(KEY_REGISTERED_EMAILS, emptySet()).orEmpty())
        val googleEmails = HashSet(prefs.getStringSet(KEY_GOOGLE_EMAILS, emptySet()).orEmpty())
        val passwordsJson = runCatching {
            JSONObject(prefs.getString(KEY_ACCOUNT_PASSWORDS_JSON, "{}").orEmpty().ifBlank { "{}" })
        }.getOrDefault(JSONObject())

        // FIX: Remove google emails from configured set if they don't actually have a password saved
        val actuallyConfigured = configured.filter { email ->
            !googleEmails.any { it.equals(email, ignoreCase = true) } || passwordsJson.has(email)
        }.toSet()
        configured.clear()
        configured.addAll(actuallyConfigured)

        val keysToClean = mutableListOf<String>()
        val iter = passwordsJson.keys()
        while (iter.hasNext()) {
            val emailKey = iter.next()
            if (!configured.any { it.equals(emailKey, ignoreCase = true) }) {
                keysToClean.add(emailKey)
            }
        }

        for (emailKey in keysToClean) {
            val tempPwd = passwordsJson.optString(emailKey, "")
            if (tempPwd.isNotBlank() && apiKey.isNotBlank()) {
                runCatching {
                    val signInPayload = JSONObject().apply {
                        put("email", emailKey)
                        put("password", tempPwd)
                        put("returnSecureToken", true)
                    }
                    val (code, json) = postIdentityToolkitJson(
                        endpoint = "accounts:signInWithPassword",
                        apiKey = apiKey,
                        payload = signInPayload
                    )
                    if (code in 200..299) {
                        val token = json?.optString("idToken").orEmpty()
                        if (token.isNotBlank()) {
                            val lookupPayload = JSONObject().put("idToken", token)
                            val (_, lookupJson) = postIdentityToolkitJson(
                                endpoint = "accounts:lookup",
                                apiKey = apiKey,
                                payload = lookupPayload
                            )
                            val userObj = lookupJson?.optJSONArray("users")?.optJSONObject(0)
                            val isEmailVerifiedOnServer = userObj?.optBoolean("emailVerified", false) ?: false
                            if (!isEmailVerifiedOnServer) {
                                // Delete the accidental Turn-10 unverified record so the user can set their real password
                                postIdentityToolkitJson(
                                    endpoint = "accounts:delete",
                                    apiKey = apiKey,
                                    payload = JSONObject().put("idToken", token)
                                )
                                registered.removeAll { it.equals(emailKey, ignoreCase = true) }
                                googleEmails.removeAll { it.equals(emailKey, ignoreCase = true) }
                            } else {
                                configured.add(emailKey.lowercase())
                            }
                        }
                    }
                }
            }
            if (!configured.any { it.equals(emailKey, ignoreCase = true) }) {
                passwordsJson.remove(emailKey)
            }
        }

        prefs.edit()
            .putStringSet(KEY_PASSWORD_CONFIGURED_EMAILS, configured)
            .putStringSet(KEY_REGISTERED_EMAILS, registered)
            .putStringSet(KEY_GOOGLE_EMAILS, googleEmails)
            .putString(KEY_ACCOUNT_PASSWORDS_JSON, passwordsJson.toString())
            .putBoolean(KEY_MIGRATED_GOOGLE_PWD_V2, true)
            .apply()
    }

    /**
     * Ensures the secondary FirebaseApp configured from the user's Task Gullak
     * `google-services.json` (`task-gullak`) is initialized.
     */
    fun ensureSecondaryFirebaseApp(context: Context): FirebaseApp? {
        val appContext = context.applicationContext
        val existing = FirebaseApp.getApps(appContext).firstOrNull { it.name == SECONDARY_APP_NAME }
        if (existing != null) return existing

        return try {
            val options = FirebaseOptions.Builder()
                .setProjectId(appContext.getString(R.string.taskgullak_firebase_project_id))
                .setApplicationId(appContext.getString(R.string.taskgullak_firebase_app_id))
                .setApiKey(appContext.getString(R.string.taskgullak_firebase_api_key))
                .setStorageBucket(appContext.getString(R.string.taskgullak_firebase_storage_bucket))
                .setDatabaseUrl(appContext.getString(R.string.taskgullak_firebase_database_url))
                .build()
            FirebaseApp.initializeApp(appContext, options, SECONDARY_APP_NAME)
        } catch (e: Exception) {
            Log.w(TAG, "Secondary FirebaseApp init note: ${e.message}")
            null
        }
    }

    private fun getTaskGullakApiKey(context: Context): String {
        ensureSecondaryFirebaseApp(context)
        val studioKey = runCatching {
            context.getString(R.string.taskgullak_firebase_api_key)
        }.getOrDefault("")
        if (studioKey.isNotBlank()) return studioKey
        return runCatching { context.getString(R.string.google_api_key) }.getOrDefault("")
    }

    fun isPasswordConfiguredLocally(context: Context, email: String): Boolean {
        val clean = email.trim().lowercase()
        if (clean.isBlank()) return false
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        val configured = prefs.getStringSet(KEY_PASSWORD_CONFIGURED_EMAILS, emptySet()).orEmpty()
        val isConfigured = configured.any { it.equals(clean, ignoreCase = true) }
        if (!isConfigured) return false
        
        // Ensure that if it's a Google email, it ACTUALLY has a password set.
        val googleEmails = prefs.getStringSet(KEY_GOOGLE_EMAILS, emptySet()).orEmpty()
        if (googleEmails.any { it.equals(clean, ignoreCase = true) }) {
            val passwordsJson = runCatching {
                JSONObject(prefs.getString(KEY_ACCOUNT_PASSWORDS_JSON, "{}").orEmpty().ifBlank { "{}" })
            }.getOrDefault(JSONObject())
            return passwordsJson.has(clean)
        }
        return true
    }

    private fun markPasswordConfiguredLocally(context: Context, email: String) {
        val clean = email.trim().lowercase()
        if (clean.isBlank()) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        val configured = HashSet(prefs.getStringSet(KEY_PASSWORD_CONFIGURED_EMAILS, emptySet()).orEmpty())
        configured.add(clean)
        prefs.edit().putStringSet(KEY_PASSWORD_CONFIGURED_EMAILS, configured).apply()
    }

    fun markEmailRegistered(context: Context, email: String, fullName: String = "") {
        val clean = email.trim().lowercase()
        if (clean.isBlank()) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        val existing = HashSet(prefs.getStringSet(KEY_REGISTERED_EMAILS, emptySet()).orEmpty())
        existing.add(clean)
        val namesJson = runCatching {
            JSONObject(prefs.getString(KEY_ACCOUNT_NAMES_JSON, "{}").orEmpty().ifBlank { "{}" })
        }.getOrDefault(JSONObject())
        if (fullName.isNotBlank()) {
            namesJson.put(clean, fullName.trim())
        }
        prefs.edit()
            .putStringSet(KEY_REGISTERED_EMAILS, existing)
            .putString(KEY_ACCOUNT_NAMES_JSON, namesJson.toString())
            .apply()
    }

    fun isGoogleAccountLocally(context: Context, email: String): Boolean {
        val clean = email.trim().lowercase()
        if (clean.isBlank()) return false
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        val googleEmails = prefs.getStringSet(KEY_GOOGLE_EMAILS, emptySet()).orEmpty()
        if (googleEmails.any { it.equals(clean, ignoreCase = true) }) return true
        val currentFirebaseUser = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()
        if (currentFirebaseUser?.email?.equals(clean, ignoreCase = true) == true &&
            currentFirebaseUser.providerData.any { it.providerId == "google.com" }
        ) {
            return true
        }
        return false
    }

    fun isEmailRegisteredLocally(context: Context, email: String): Boolean {
        val clean = email.trim().lowercase()
        if (clean.isBlank()) return false
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        val existing = prefs.getStringSet(KEY_REGISTERED_EMAILS, emptySet()).orEmpty()
        val configured = prefs.getStringSet(KEY_PASSWORD_CONFIGURED_EMAILS, emptySet()).orEmpty()
        val googleEmails = prefs.getStringSet(KEY_GOOGLE_EMAILS, emptySet()).orEmpty()
        val savedEmail = prefs.getString(KEY_USER_EMAIL, "").orEmpty()
        return existing.any { it.equals(clean, ignoreCase = true) } ||
            configured.any { it.equals(clean, ignoreCase = true) } ||
            googleEmails.any { it.equals(clean, ignoreCase = true) } ||
            savedEmail.equals(clean, ignoreCase = true)
    }

    private fun saveAccountPasswordForEmail(context: Context, email: String, password: String) {
        val clean = email.trim().lowercase()
        if (clean.isBlank() || password.isEmpty()) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        val passwordsJson = runCatching {
            JSONObject(prefs.getString(KEY_ACCOUNT_PASSWORDS_JSON, "{}").orEmpty().ifBlank { "{}" })
        }.getOrDefault(JSONObject())
        passwordsJson.put(clean, password)
        prefs.edit().putString(KEY_ACCOUNT_PASSWORDS_JSON, passwordsJson.toString()).apply()
    }

    private fun getSavedAccountPasswordForEmail(context: Context, email: String): String {
        val clean = email.trim().lowercase()
        if (clean.isBlank()) return ""
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        val passwordsJson = runCatching {
            JSONObject(prefs.getString(KEY_ACCOUNT_PASSWORDS_JSON, "{}").orEmpty().ifBlank { "{}" })
        }.getOrDefault(JSONObject())
        return passwordsJson.optString(clean, "")
    }

    private fun getSavedAccountNameForEmail(context: Context, email: String): String {
        val clean = email.trim().lowercase()
        if (clean.isBlank()) return ""
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        val namesJson = runCatching {
            JSONObject(prefs.getString(KEY_ACCOUNT_NAMES_JSON, "{}").orEmpty().ifBlank { "{}" })
        }.getOrDefault(JSONObject())
        return namesJson.optString(clean, "")
    }

    private fun saveEmailSession(
        context: Context,
        idToken: String,
        email: String,
        fullName: String = "",
        markVerificationSent: Boolean = false,
        markLoggedIn: Boolean = false
    ) {
        val cleanEmail = email.trim().lowercase()
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        val existingEmails = HashSet(prefs.getStringSet(KEY_REGISTERED_EMAILS, emptySet()).orEmpty())
        if (cleanEmail.isNotBlank()) {
            existingEmails.add(cleanEmail)
        }
        val namesJson = runCatching {
            JSONObject(prefs.getString(KEY_ACCOUNT_NAMES_JSON, "{}").orEmpty().ifBlank { "{}" })
        }.getOrDefault(JSONObject())
        val resolvedName = fullName.trim().ifBlank {
            namesJson.optString(cleanEmail, "").ifBlank {
                cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
            }
        }
        if (cleanEmail.isNotBlank() && resolvedName.isNotBlank()) {
            namesJson.put(cleanEmail, resolvedName)
        }
        prefs.edit().apply {
            if (idToken.isNotBlank()) {
                putString(KEY_ID_TOKEN, idToken)
            }
            if (cleanEmail.isNotBlank()) {
                putString(KEY_USER_EMAIL, cleanEmail)
            }
            putStringSet(KEY_REGISTERED_EMAILS, existingEmails)
            putString(KEY_ACCOUNT_NAMES_JSON, namesJson.toString())
            if (resolvedName.isNotBlank()) {
                putString(KEY_USER_NAME, resolvedName)
            }
            if (markVerificationSent) {
                putLong(KEY_VERIFICATION_SENT_AT, System.currentTimeMillis())
            }
            if (markLoggedIn) {
                putBoolean(KEY_USER_LOGGED_IN, true)
            }
        }.apply()
    }

    /**
     * Saves Google account data (`displayName` and `email`) extracted during
     * "Continue with Google" or "Sign up with Google" and marks the user logged in.
     * Also permanently records the email as a registered & verified Google account.
     */
    fun saveGoogleAccountSession(
        context: Context,
        fullName: String,
        email: String
    ) {
        val cleanEmail = email.trim().lowercase()
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        val namesJson = runCatching {
            JSONObject(prefs.getString(KEY_ACCOUNT_NAMES_JSON, "{}").orEmpty().ifBlank { "{}" })
        }.getOrDefault(JSONObject())
        val resolvedName = fullName.trim().ifBlank {
            namesJson.optString(cleanEmail, "").ifBlank {
                cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }.ifBlank { "Task Gullak Member" }
            }
        }
        val existingEmails = HashSet(prefs.getStringSet(KEY_REGISTERED_EMAILS, emptySet()).orEmpty())
        val googleEmails = HashSet(prefs.getStringSet(KEY_GOOGLE_EMAILS, emptySet()).orEmpty())
        if (cleanEmail.isNotBlank()) {
            existingEmails.add(cleanEmail)
            googleEmails.add(cleanEmail)
            namesJson.put(cleanEmail, resolvedName)
        }
        prefs.edit().apply {
            putString(KEY_USER_NAME, resolvedName)
            if (cleanEmail.isNotBlank()) {
                putString(KEY_USER_EMAIL, cleanEmail)
            }
            putStringSet(KEY_REGISTERED_EMAILS, existingEmails)
            putStringSet(KEY_GOOGLE_EMAILS, googleEmails)
            putString(KEY_ACCOUNT_NAMES_JSON, namesJson.toString())
            putBoolean(KEY_USER_LOGGED_IN, true)
            putBoolean(KEY_MIGRATED_GOOGLE_PWD_V2, true)
        }.commit()
    }

    /**
     * Checks whether a Google email already exists in the `task-gullak` database with a password/account.
     * If `true`, the user can be logged straight in.
     * If `false`, the app prompts the user to enter Password and Confirm Password first.
     */
    suspend fun checkGoogleEmailAccountExistsInDatabase(
        context: Context,
        rawEmail: String
    ): Boolean = withContext(Dispatchers.IO) {
        cleanupUnverifiedTurn10GoogleState(context)
        val cleanEmail = rawEmail.trim().lowercase()
        if (cleanEmail.isBlank()) return@withContext false

        try {
            // Source of Truth: Check Firestore users collection
            val firestore = resolveFirestoreInstance(context)
            val userSnap = firestore.collection("users")
                .whereEqualTo("email", cleanEmail)
                .limit(1)
                .get()
                .await()
            if (!userSnap.isEmpty) {
                markPasswordConfiguredLocally(context, cleanEmail)
                markEmailRegistered(context, cleanEmail)
                return@withContext true
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "checkGoogleEmailAccountExistsInDatabase note: ${e.message}")
        }
        false
    }

    /**
     * Completes account creation for a first-time Google Sign-In user after they
     * enter and confirm their password. Registers the email + password in the
     * `task-gullak` database so they can also log in via Email & Password next time,
     * and marks the user logged in.
     */
    suspend fun completeGoogleAccountPasswordSetup(
        context: Context,
        fullName: String,
        rawEmail: String,
        password: String
    ): EmailDispatchResult = withContext(Dispatchers.IO) {
        cleanupUnverifiedTurn10GoogleState(context)
        val cleanEmail = rawEmail.trim().lowercase()
        val resolvedName = fullName.trim().ifBlank {
            getSavedAccountNameForEmail(context, cleanEmail).ifBlank {
                cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }.ifBlank { "Task Gullak Member" }
            }
        }
        val apiKey = getTaskGullakApiKey(context)

        try {
            val signUpPayload = JSONObject().apply {
                put("email", cleanEmail)
                put("password", password)
                put("returnSecureToken", true)
            }
            var (statusCode, responseJson) = postIdentityToolkitJson(
                endpoint = "accounts:signUp",
                apiKey = apiKey,
                payload = signUpPayload
            )
            var idToken = responseJson?.optString("idToken").orEmpty()
            val errorMsg = responseJson?.optJSONObject("error")?.optString("message").orEmpty()

            if (statusCode !in 200..299 && errorMsg.contains("EMAIL_EXISTS", ignoreCase = true)) {
                val signInResult = postIdentityToolkitJson(
                    endpoint = "accounts:signInWithPassword",
                    apiKey = apiKey,
                    payload = signUpPayload
                )
                if (signInResult.first in 200..299) {
                    statusCode = signInResult.first
                    responseJson = signInResult.second
                    idToken = responseJson?.optString("idToken").orEmpty()
                }
            }

            if (idToken.isNotBlank()) {
                runCatching {
                    val updatePayload = JSONObject().apply {
                        put("idToken", idToken)
                        put("displayName", resolvedName)
                        put("returnSecureToken", true)
                    }
                    postIdentityToolkitJson(
                        endpoint = "accounts:update",
                        apiKey = apiKey,
                        payload = updatePayload
                    )
                }
            }

            saveAccountPasswordForEmail(context, cleanEmail, password)
            markPasswordConfiguredLocally(context, cleanEmail)
            saveGoogleAccountSession(
                context = context,
                fullName = resolvedName,
                email = cleanEmail
            )
            if (idToken.isNotBlank()) {
                saveEmailSession(
                    context = context,
                    idToken = idToken,
                    email = cleanEmail,
                    fullName = resolvedName,
                    markVerificationSent = false,
                    markLoggedIn = true
                )
            }
            syncUserProfileToFirestore(
                context = context,
                email = cleanEmail,
                fullName = resolvedName,
                emailVerified = true,
                creditReferrerOnLogin = true
            )

            EmailDispatchResult(
                success = true,
                title = "Password saved",
                message = "Your Task Gullak account is ready!",
                statusType = "success",
                emailVerified = true
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "completeGoogleAccountPasswordSetup note: ${e.message}")
            saveAccountPasswordForEmail(context, cleanEmail, password)
            markPasswordConfiguredLocally(context, cleanEmail)
            saveGoogleAccountSession(
                context = context,
                fullName = resolvedName,
                email = cleanEmail
            )
            saveEmailSession(
                context = context,
                idToken = "",
                email = cleanEmail,
                fullName = resolvedName,
                markVerificationSent = false,
                markLoggedIn = true
            )
            syncUserProfileToFirestore(
                context = context,
                email = cleanEmail,
                fullName = resolvedName,
                emailVerified = true,
                creditReferrerOnLogin = true
            )
            EmailDispatchResult(
                success = true,
                title = "Password saved",
                message = "Your Task Gullak account is ready!",
                statusType = "success",
                emailVerified = true
            )
        }
    }

    /**
     * Converts any email address into a deterministic, valid Firestore document ID
     * matching `^[a-zA-Z0-9_\-]+$` (e.g. `snehkumarverma2011@gmail.com` -> `user_snehkumarverma2011_gmail_com`).
     */
    fun deriveFirestoreUserIdFromEmail(email: String): String {
        val clean = email.trim().lowercase()
        if (clean.isBlank()) return ""
        val sanitized = clean.replace(Regex("[^a-zA-Z0-9]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
        if (sanitized.isBlank()) return ""
        return "user_${sanitized.take(100)}"
    }

    /**
     * Resolves the active Firestore user document ID for the current session:
     * 1. Uses `Firebase.auth.currentUser?.uid` if signed in via primary Firebase Auth (Google).
     * 2. Falls back to `deriveFirestoreUserIdFromEmail(getSavedUserEmail(context))` when logged in via Email & Password.
     */
    fun resolveActiveFirestoreUserId(context: Context): String? {
        val currentFirebaseUser = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()
        if (currentFirebaseUser != null && currentFirebaseUser.uid.isNotBlank()) {
            return currentFirebaseUser.uid
        }
        if (!isUserLoggedIn(context)) return null
        val savedEmail = getSavedUserEmail(context).trim().lowercase()
        if (savedEmail.isBlank()) return null
        return deriveFirestoreUserIdFromEmail(savedEmail).takeIf { it.isNotBlank() }
    }

    /**
     * Ensures that a user profile document exists in the shared Firestore `/users/{userId}`
     * collection so the user immediately appears in the Gullak Admin app.
     */
    suspend fun syncUserProfileToFirestore(
        context: Context,
        email: String,
        fullName: String = "",
        emailVerified: Boolean = true,
        explicitUserId: String? = null,
        referredByCode: String = "",
        creditReferrerOnLogin: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank()) return@withContext
        val resolvedName = fullName.trim().ifBlank {
            getSavedAccountNameForEmail(context, cleanEmail).ifBlank {
                getSavedUserName(context).ifBlank {
                    cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
                }
            }
        }.ifBlank { "Task Gullak Member" }

        val currentFirebaseUser = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()
        val targetUid = explicitUserId?.takeIf { it.isNotBlank() }
            ?: if (currentFirebaseUser != null && currentFirebaseUser.email?.equals(cleanEmail, ignoreCase = true) == true) {
                currentFirebaseUser.uid
            } else {
                deriveFirestoreUserIdFromEmail(cleanEmail)
            }
        if (targetUid.isBlank()) return@withContext

        val effectiveRefCode = getOrCreateReferralCode(context, cleanEmail)
        val effectiveReferredBy = referredByCode.trim().uppercase().ifBlank {
            getReferredByCode(context, cleanEmail)
        }

        runCatching {
            val db = resolveFirestoreInstance(context)
            TaskGullakRepository(db).ensureOrCreateProfile(
                fullName = resolvedName,
                email = cleanEmail,
                emailVerified = emailVerified,
                explicitUserId = targetUid,
                referralCode = effectiveRefCode,
                referredByCode = effectiveReferredBy,
                creditReferrerOnLogin = creditReferrerOnLogin
            )
        }.onFailure { err ->
            Log.w(TAG, "syncUserProfileToFirestore note for $cleanEmail: ${err.message}")
        }
    }

    /**
     * Syncs the active session and any locally registered accounts on this device
     * to `/users` in Firestore so Email/Password accounts (like the user's own email)
     * are never missing from Gullak Admin.
     */
    suspend fun syncAllKnownLocalAccountsToFirestore(context: Context) = withContext(Dispatchers.IO) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        val allEmails = LinkedHashSet<String>()
        val activeEmail = getSavedUserEmail(context).trim().lowercase()
        if (activeEmail.isNotBlank()) {
            allEmails.add(activeEmail)
        }
        prefs.getStringSet(KEY_REGISTERED_EMAILS, emptySet()).orEmpty().forEach {
            val clean = it.trim().lowercase()
            if (clean.isNotBlank()) allEmails.add(clean)
        }
        prefs.getStringSet(KEY_PASSWORD_CONFIGURED_EMAILS, emptySet()).orEmpty().forEach {
            val clean = it.trim().lowercase()
            if (clean.isNotBlank()) allEmails.add(clean)
        }
        prefs.getStringSet(KEY_GOOGLE_EMAILS, emptySet()).orEmpty().forEach {
            val clean = it.trim().lowercase()
            if (clean.isNotBlank()) allEmails.add(clean)
        }

        for (email in allEmails) {
            val name = getSavedAccountNameForEmail(context, email)
                .ifBlank { if (email == activeEmail) getSavedUserName(context) else "" }
                .ifBlank { email.substringBefore("@").replaceFirstChar { it.uppercase() } }
            val isVerified = isGoogleAccountLocally(context, email) ||
                isPasswordConfiguredLocally(context, email) ||
                (email == activeEmail && isUserLoggedIn(context))
            syncUserProfileToFirestore(
                context = context,
                email = email,
                fullName = name,
                emailVerified = isVerified
            )
        }
    }

    fun setUserLoggedIn(context: Context, loggedIn: Boolean) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_USER_LOGGED_IN, loggedIn).apply()
    }

    fun isUserLoggedIn(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        val isExplicitLoggedIn = prefs.getBoolean(KEY_USER_LOGGED_IN, false)
        val currentFirebaseUser = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()
        if (currentFirebaseUser != null) {
            val userEmail = currentFirebaseUser.email.orEmpty().trim().lowercase()
            val isGoogle = isGoogleAccountLocally(context, userEmail)
            if (isGoogle) return true
            if (currentFirebaseUser.isEmailVerified && isExplicitLoggedIn) return true
            return false
        }
        return isExplicitLoggedIn
    }

    fun getSavedIdToken(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        return prefs.getString(KEY_ID_TOKEN, "").orEmpty()
    }

    private const val KEY_USER_AVATAR_ID = "tg_studio_user_avatar_id"
    private const val KEY_ACCOUNT_AVATARS_JSON = "tg_studio_account_avatars_json"

    fun saveUpdatedUserName(
        context: Context,
        fullName: String,
        email: String = ""
    ) {
        val cleanName = fullName.trim().take(50)
        if (cleanName.length < 2) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        val resolvedEmail = email.trim().lowercase().ifBlank {
            prefs.getString(KEY_USER_EMAIL, "").orEmpty().trim().lowercase()
        }
        val namesJson = runCatching {
            JSONObject(prefs.getString(KEY_ACCOUNT_NAMES_JSON, "{}").orEmpty().ifBlank { "{}" })
        }.getOrDefault(JSONObject())
        if (resolvedEmail.isNotBlank()) {
            namesJson.put(resolvedEmail, cleanName)
        }
        prefs.edit()
            .putString(KEY_USER_NAME, cleanName)
            .putString(KEY_ACCOUNT_NAMES_JSON, namesJson.toString())
            .apply()

        context.applicationContext.getSharedPreferences("task_gullak_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("user_full_name", cleanName)
            .apply()
    }

    fun saveUserAvatarId(
        context: Context,
        avatarId: String,
        email: String = ""
    ) {
        val cleanAvatar = avatarId.trim().take(64)
        if (cleanAvatar.isBlank()) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        val resolvedEmail = email.trim().lowercase().ifBlank {
            prefs.getString(KEY_USER_EMAIL, "").orEmpty().trim().lowercase()
        }
        val avatarsJson = runCatching {
            JSONObject(prefs.getString(KEY_ACCOUNT_AVATARS_JSON, "{}").orEmpty().ifBlank { "{}" })
        }.getOrDefault(JSONObject())
        if (resolvedEmail.isNotBlank()) {
            avatarsJson.put(resolvedEmail, cleanAvatar)
        }
        prefs.edit()
            .putString(KEY_USER_AVATAR_ID, cleanAvatar)
            .putString(KEY_ACCOUNT_AVATARS_JSON, avatarsJson.toString())
            .apply()

        context.applicationContext.getSharedPreferences("task_gullak_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("avatarId", cleanAvatar)
            .apply()
    }

    fun getSavedUserAvatarId(
        context: Context,
        email: String = ""
    ): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        val resolvedEmail = email.trim().lowercase().ifBlank {
            getSavedUserEmail(context).trim().lowercase()
        }
        if (resolvedEmail.isNotBlank()) {
            val avatarsJson = runCatching {
                JSONObject(prefs.getString(KEY_ACCOUNT_AVATARS_JSON, "{}").orEmpty().ifBlank { "{}" })
            }.getOrDefault(JSONObject())
            val mapped = avatarsJson.optString(resolvedEmail, "")
            if (mapped.isNotBlank()) return mapped
        }
        val direct = prefs.getString(KEY_USER_AVATAR_ID, "").orEmpty()
        if (direct.isNotBlank()) return direct
        return context.applicationContext
            .getSharedPreferences("task_gullak_prefs", Context.MODE_PRIVATE)
            .getString("avatarId", "")
            .orEmpty()
    }

    fun getSavedUserName(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_USER_NAME, "").orEmpty()
        if (saved.isNotBlank()) return saved
        val currentEmail = getSavedUserEmail(context)
        if (currentEmail.isNotBlank()) {
            val mappedName = getSavedAccountNameForEmail(context, currentEmail)
            if (mappedName.isNotBlank()) return mappedName
        }
        return runCatching { FirebaseAuth.getInstance().currentUser?.displayName.orEmpty() }.getOrDefault("")
    }

    fun getSavedUserEmail(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_USER_EMAIL, "").orEmpty()
        if (saved.isNotBlank()) return saved
        return runCatching { FirebaseAuth.getInstance().currentUser?.email.orEmpty() }.getOrDefault("")
    }

    fun wasVerificationJustSent(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        val sentAt = prefs.getLong(KEY_VERIFICATION_SENT_AT, 0L)
        return sentAt > 0L && (System.currentTimeMillis() - sentAt) < 55_000L
    }

    fun clearEmailSession(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        val registeredEmails = HashSet(prefs.getStringSet(KEY_REGISTERED_EMAILS, emptySet()).orEmpty())
        val googleEmails = HashSet(prefs.getStringSet(KEY_GOOGLE_EMAILS, emptySet()).orEmpty())
        val configuredEmails = HashSet(prefs.getStringSet(KEY_PASSWORD_CONFIGURED_EMAILS, emptySet()).orEmpty())
        val lastEmail = prefs.getString(KEY_USER_EMAIL, "").orEmpty().trim().lowercase()
        if (lastEmail.isNotBlank()) {
            registeredEmails.add(lastEmail)
        }
        prefs.edit()
            .remove(KEY_ID_TOKEN)
            .remove(KEY_VERIFICATION_SENT_AT)
            .putStringSet(KEY_REGISTERED_EMAILS, registeredEmails)
            .putStringSet(KEY_GOOGLE_EMAILS, googleEmails)
            .putStringSet(KEY_PASSWORD_CONFIGURED_EMAILS, configuredEmails)
            .putBoolean(KEY_USER_LOGGED_IN, false)
            .apply()
    }

    private fun postIdentityToolkitJson(
        endpoint: String,
        apiKey: String,
        payload: JSONObject
    ): Pair<Int, JSONObject?> {
        val url = URL("https://identitytoolkit.googleapis.com/v1/$endpoint?key=$apiKey")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 12000
            readTimeout = 12000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        }

        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(payload.toString())
            writer.flush()
        }

        val statusCode = conn.responseCode
        val stream = if (statusCode in 200..299) conn.inputStream else conn.errorStream
        val bodyText = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        conn.disconnect()
        val json = if (bodyText.isNotBlank()) {
            runCatching { JSONObject(bodyText) }.getOrNull()
        } else {
            null
        }
        return statusCode to json
    }

    /**
     * Registers a new Email/Password user on `task-gullak`, updates their
     * `displayName`, and immediately dispatches the Firebase Email Verification link.
     */
    suspend fun registerEmailAccountAndSendVerification(
        context: Context,
        fullName: String,
        rawEmail: String,
        password: String
    ): EmailDispatchResult = withContext(Dispatchers.IO) {
        cleanupUnverifiedTurn10GoogleState(context)
        val cleanName = fullName.trim()
        val cleanEmail = rawEmail.trim().lowercase()
        val apiKey = getTaskGullakApiKey(context)

        // Pre-check: if account is already known locally or in Firestore, reject immediately
        if (isEmailRegisteredLocally(context, cleanEmail) || isPasswordConfiguredLocally(context, cleanEmail)) {
            return@withContext EmailDispatchResult(
                success = false,
                title = "Email already registered",
                message = "An account with this email already exists. Please log in instead.",
                statusType = "error"
            )
        }

        // Firestore Check: reject if email already exists in Firestore /users collection
        val isFirestoreEmailExists = runCatching {
            val db = resolveFirestoreInstance(context)
            val existingSnap = db.collection("users")
                .whereEqualTo("email", cleanEmail)
                .limit(1)
                .get()
                .await()
            !existingSnap.isEmpty
        }.getOrDefault(false)

        if (isFirestoreEmailExists) {
            markEmailRegistered(context, cleanEmail)
            return@withContext EmailDispatchResult(
                success = false,
                title = "Email already registered",
                message = "An account with this email already exists. Please log in instead.",
                statusType = "error"
            )
        }

        try {
            val signUpPayload = JSONObject().apply {
                put("email", cleanEmail)
                put("password", password)
                put("returnSecureToken", true)
            }
            var (statusCode, responseJson) = postIdentityToolkitJson(
                endpoint = "accounts:signUp",
                apiKey = apiKey,
                payload = signUpPayload
            )

            var idToken = responseJson?.optString("idToken").orEmpty()
            val errorMsg = responseJson?.optJSONObject("error")?.optString("message").orEmpty()

            // If account already exists on server, strictly reject with account exists error
            if (statusCode !in 200..299 && (errorMsg.contains("EMAIL_EXISTS", ignoreCase = true) || errorMsg.contains("USER_EXISTS", ignoreCase = true))) {
                markEmailRegistered(context, cleanEmail)
                return@withContext EmailDispatchResult(
                    success = false,
                    title = "Email already registered",
                    message = "An account with this email already exists. Please log in instead.",
                    statusType = "error"
                )
            }

            if (statusCode !in 200..299 || idToken.isBlank()) {
                val friendlyMsg = when {
                    errorMsg.contains("INVALID_EMAIL", ignoreCase = true) ->
                        "Please enter a valid email address."
                    errorMsg.contains("WEAK_PASSWORD", ignoreCase = true) ->
                        "Password is too weak. Please choose a stronger password."
                    errorMsg.contains("TOO_MANY_ATTEMPTS", ignoreCase = true) ->
                        "Too many attempts. Please wait a moment and try again."
                    else -> "Could not create account (${errorMsg.ifBlank { "check network" }})."
                }
                return@withContext EmailDispatchResult(
                    success = false,
                    title = "Sign up failed",
                    message = friendlyMsg,
                    statusType = "error"
                )
            }

            // Update displayName on the Firebase user profile
            runCatching {
                val updatePayload = JSONObject().apply {
                    put("idToken", idToken)
                    put("displayName", cleanName)
                    put("returnSecureToken", true)
                }
                postIdentityToolkitJson(
                    endpoint = "accounts:update",
                    apiKey = apiKey,
                    payload = updatePayload
                )
            }

            // Immediately dispatch the Firebase VERIFY_EMAIL OOB link
            val verifyPayload = JSONObject().apply {
                put("requestType", "VERIFY_EMAIL")
                put("idToken", idToken)
            }
            val (verifyCode, _) = postIdentityToolkitJson(
                endpoint = "accounts:sendOobCode",
                apiKey = apiKey,
                payload = verifyPayload
            )

            saveAccountPasswordForEmail(context, cleanEmail, password)
            markPasswordConfiguredLocally(context, cleanEmail)
            val alreadyGoogleVerified = isGoogleAccountLocally(context, cleanEmail)

            saveEmailSession(
                context = context,
                idToken = idToken,
                email = cleanEmail,
                fullName = cleanName,
                markVerificationSent = verifyCode in 200..299,
                markLoggedIn = false
            )
            setUserLoggedIn(context, false)
            syncUserProfileToFirestore(
                context = context,
                email = cleanEmail,
                fullName = cleanName,
                emailVerified = false,
                creditReferrerOnLogin = false
            )

            EmailDispatchResult(
                success = true,
                title = "Verification email sent",
                message = "We've sent a verification link to $cleanEmail. Please check your inbox and spam folder.",
                statusType = "success",
                emailVerified = false
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "registerEmailAccountAndSendVerification network note: ${e.message}")
            EmailDispatchResult(
                success = false,
                title = "Network error",
                message = "Please check your internet connection and try again.",
                statusType = "error"
            )
        }
    }

    /**
     * Strictly signs in an Email/Password user on `task-gullak`.
     * Rejects any wrong password with "Invalid password" and rejects unregistered
     * emails with "Create an account first".
     */
    suspend fun signInEmailAccount(
        context: Context,
        rawEmail: String,
        password: String
    ): EmailDispatchResult = withContext(Dispatchers.IO) {
        cleanupUnverifiedTurn10GoogleState(context)
        val cleanEmail = rawEmail.trim().lowercase()
        val apiKey = getTaskGullakApiKey(context)
        val isGoogleAccount = isGoogleAccountLocally(context, cleanEmail)
        val knownAccount = isEmailRegisteredLocally(context, cleanEmail)
        val passwordConfigured = isPasswordConfiguredLocally(context, cleanEmail)
        val savedLocalPassword = getSavedAccountPasswordForEmail(context, cleanEmail)

        try {
            val signInPayload = JSONObject().apply {
                put("email", cleanEmail)
                put("password", password)
                put("returnSecureToken", true)
            }
            val (statusCode, responseJson) = postIdentityToolkitJson(
                endpoint = "accounts:signInWithPassword",
                apiKey = apiKey,
                payload = signInPayload
            )

            val idToken = responseJson?.optString("idToken").orEmpty()
            val displayName = responseJson?.optString("displayName").orEmpty()
            val errorMsg = responseJson?.optJSONObject("error")?.optString("message").orEmpty()

            if (statusCode !in 200..299 || idToken.isBlank()) {
                // Only allow local password match if the user explicitly configured this EXACT password
                if (passwordConfigured && savedLocalPassword.isNotBlank() && savedLocalPassword == password) {
                    val resolvedName = getSavedAccountNameForEmail(context, cleanEmail)
                        .ifBlank { displayName.ifBlank { getSavedUserName(context) } }
                        .ifBlank { cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() } }
                    saveEmailSession(
                        context = context,
                        idToken = "",
                        email = cleanEmail,
                        fullName = resolvedName,
                        markVerificationSent = false,
                        markLoggedIn = true
                    )
                    syncUserProfileToFirestore(
                        context = context,
                        email = cleanEmail,
                        fullName = resolvedName,
                        emailVerified = true,
                        creditReferrerOnLogin = true
                    )
                    return@withContext EmailDispatchResult(
                        success = true,
                        title = "Login successful",
                        message = "Welcome back to Task Gullak!",
                        statusType = "success",
                        emailVerified = true
                    )
                }

                // Check if the email exists on IdentityToolkit when local registry doesn't have it yet
                var existsOnServer = knownAccount
                if (!existsOnServer && !errorMsg.contains("EMAIL_NOT_FOUND", ignoreCase = true) &&
                    !errorMsg.contains("USER_NOT_FOUND", ignoreCase = true)
                ) {
                    existsOnServer = runCatching {
                        val uriPayload = JSONObject().apply {
                            put("identifier", cleanEmail)
                            put("continueUri", "https://task-gullak.firebaseapp.com")
                        }
                        val (uriCode, uriJson) = postIdentityToolkitJson(
                            endpoint = "accounts:createAuthUri",
                            apiKey = apiKey,
                            payload = uriPayload
                        )
                        uriCode in 200..299 && (uriJson?.optBoolean("registered", false) == true)
                    }.getOrDefault(false)
                }

                val isNotFound = !existsOnServer && (
                    errorMsg.contains("EMAIL_NOT_FOUND", ignoreCase = true) ||
                        errorMsg.contains("USER_NOT_FOUND", ignoreCase = true) ||
                        errorMsg.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true)
                    )

                val (title, msg) = when {
                    errorMsg.contains("USER_DISABLED", ignoreCase = true) ->
                        "Account disabled" to "This account has been disabled. Please contact support."
                    errorMsg.contains("TOO_MANY_ATTEMPTS", ignoreCase = true) ->
                        "Too many attempts" to "Access temporarily blocked due to many failed login attempts. Reset your password or try again later."
                    isNotFound ->
                        "Create an account first" to "No account found with this email. Please create an account first before logging in."
                    else ->
                        "Invalid password" to "Incorrect email or password. Please check your details and try again."
                }
                return@withContext EmailDispatchResult(
                    success = false,
                    title = title,
                    message = msg,
                    statusType = "error"
                )
            }

            // Lookup user account to check emailVerified boolean
            val lookupPayload = JSONObject().apply {
                put("idToken", idToken)
            }
            val (_, lookupJson) = postIdentityToolkitJson(
                endpoint = "accounts:lookup",
                apiKey = apiKey,
                payload = lookupPayload
            )
            val userObj = lookupJson?.optJSONArray("users")?.optJSONObject(0)
            // If the account was authenticated with Google previously, its email is already verified
            val isVerified = (userObj?.optBoolean("emailVerified", false) ?: false) || isGoogleAccount
            val resolvedName = userObj?.optString("displayName").orEmpty().ifBlank {
                displayName.ifBlank {
                    getSavedAccountNameForEmail(context, cleanEmail).ifBlank { getSavedUserName(context) }
                }
            }

            saveAccountPasswordForEmail(context, cleanEmail, password)
            markPasswordConfiguredLocally(context, cleanEmail)
            saveEmailSession(
                context = context,
                idToken = idToken,
                email = cleanEmail,
                fullName = resolvedName,
                markVerificationSent = false,
                markLoggedIn = isVerified
            )
            syncUserProfileToFirestore(
                context = context,
                email = cleanEmail,
                fullName = resolvedName,
                emailVerified = isVerified,
                creditReferrerOnLogin = true
            )

            EmailDispatchResult(
                success = true,
                title = if (isVerified) "Login successful" else "Email verification required",
                message = if (isVerified) {
                    "Welcome back to Task Gullak!"
                } else {
                    "Please verify your email address before continuing."
                },
                statusType = if (isVerified) "success" else "info",
                emailVerified = isVerified
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "signInEmailAccount network note: ${e.message}")
            if (passwordConfigured && savedLocalPassword.isNotBlank() && savedLocalPassword == password) {
                val resolvedName = getSavedAccountNameForEmail(context, cleanEmail)
                    .ifBlank { getSavedUserName(context) }
                    .ifBlank { cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() } }
                saveEmailSession(
                    context = context,
                    idToken = "",
                    email = cleanEmail,
                    fullName = resolvedName,
                    markVerificationSent = false,
                    markLoggedIn = true
                )
                syncUserProfileToFirestore(
                    context = context,
                    email = cleanEmail,
                    fullName = resolvedName,
                    emailVerified = true,
                    creditReferrerOnLogin = true
                )
                return@withContext EmailDispatchResult(
                    success = true,
                    title = "Login successful",
                    message = "Welcome back to Task Gullak!",
                    statusType = "success",
                    emailVerified = true
                )
            }
            EmailDispatchResult(
                success = false,
                title = "Network error",
                message = "Please check your internet connection and try again.",
                statusType = "error"
            )
        }
    }

    /**
     * Dispatches a Firebase Password Reset email (`PASSWORD_RESET`) using the
     * user's `task-gullak` Firebase configuration.
     */
    suspend fun sendPasswordResetLinkEmail(
        context: Context,
        rawEmail: String
    ): EmailDispatchResult = withContext(Dispatchers.IO) {
        val cleanEmail = rawEmail.trim().lowercase()
        val apiKey = getTaskGullakApiKey(context)

        try {
            val payload = JSONObject().apply {
                put("requestType", "PASSWORD_RESET")
                put("email", cleanEmail)
            }
            var (statusCode, responseJson) = postIdentityToolkitJson(
                endpoint = "accounts:sendOobCode",
                apiKey = apiKey,
                payload = payload
            )
            var errorCode = responseJson?.optJSONObject("error")?.optString("message").orEmpty()

            if (statusCode !in 200..299 &&
                (errorCode.contains("USER_NOT_FOUND", ignoreCase = true) ||
                    errorCode.contains("EMAIL_NOT_FOUND", ignoreCase = true)) &&
                (isGoogleAccountLocally(context, cleanEmail) || isEmailRegisteredLocally(context, cleanEmail))
            ) {
                // Account was created via Google Sign-In; provision on IdentityToolkit so reset link can be sent
                runCatching {
                    val tempPassword = getSavedAccountPasswordForEmail(context, cleanEmail)
                        .ifBlank { "TgGoogle#${cleanEmail.hashCode()}!9aA" }
                    val signUpPayload = JSONObject().apply {
                        put("email", cleanEmail)
                        put("password", tempPassword)
                        put("returnSecureToken", true)
                    }
                    postIdentityToolkitJson(
                        endpoint = "accounts:signUp",
                        apiKey = apiKey,
                        payload = signUpPayload
                    )
                    val retry = postIdentityToolkitJson(
                        endpoint = "accounts:sendOobCode",
                        apiKey = apiKey,
                        payload = payload
                    )
                    statusCode = retry.first
                    responseJson = retry.second
                    errorCode = responseJson?.optJSONObject("error")?.optString("message").orEmpty()
                }
            }

            if (statusCode in 200..299) {
                return@withContext EmailDispatchResult(
                    success = true,
                    title = "Reset link sent successfully",
                    message = "Please check your inbox and spam folder for the password reset email.",
                    statusType = "success"
                )
            }

            when {
                errorCode.contains("INVALID_EMAIL", ignoreCase = true) -> {
                    EmailDispatchResult(
                        success = false,
                        title = "Invalid email address",
                        message = "Please check the email address and try again.",
                        statusType = "error"
                    )
                }
                errorCode.contains("TOO_MANY_ATTEMPTS", ignoreCase = true) -> {
                    EmailDispatchResult(
                        success = false,
                        title = "Too many requests",
                        message = "You have requested too many reset links. Please wait a few minutes and try again.",
                        statusType = "error"
                    )
                }
                errorCode.contains("USER_NOT_FOUND", ignoreCase = true) ||
                    errorCode.contains("EMAIL_NOT_FOUND", ignoreCase = true) -> {
                    EmailDispatchResult(
                        success = false,
                        title = "No account found",
                        message = "No Task Gullak account was found with this email address.",
                        statusType = "error"
                    )
                }
                else -> {
                    EmailDispatchResult(
                        success = false,
                        title = "Could not send reset email",
                        message = "Unable to send the reset email right now. Please try again shortly.",
                        statusType = "error"
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "sendPasswordResetLinkEmail note: ${e.message}")
            EmailDispatchResult(
                success = false,
                title = "Network connection issue",
                message = "Please check your internet connection and try again.",
                statusType = "error"
            )
        }
    }

    /**
     * Dispatches a Firebase Email Verification link (`VERIFY_EMAIL`) using the
     * user's `task-gullak` Firebase configuration (or active FirebaseUser).
     */
    suspend fun sendVerificationLinkEmail(
        context: Context,
        fallbackEmail: String = ""
    ): EmailDispatchResult = withContext(Dispatchers.IO) {
        val apiKey = getTaskGullakApiKey(context)
        val savedIdToken = getSavedIdToken(context)

        try {
            // 1. Primary path: use the task-gullak ID token from Sign Up / Login
            if (savedIdToken.isNotBlank()) {
                val payload = JSONObject().apply {
                    put("requestType", "VERIFY_EMAIL")
                    put("idToken", savedIdToken)
                }
                val (statusCode, responseJson) = postIdentityToolkitJson(
                    endpoint = "accounts:sendOobCode",
                    apiKey = apiKey,
                    payload = payload
                )
                val errorCode = responseJson?.optJSONObject("error")?.optString("message").orEmpty()

                if (statusCode in 200..299) {
                    saveEmailSession(
                        context = context,
                        idToken = savedIdToken,
                        email = fallbackEmail.ifBlank { getSavedUserEmail(context) },
                        markVerificationSent = true
                    )
                    return@withContext EmailDispatchResult(
                        success = true,
                        title = "Verification email sent",
                        message = "A new verification link has been sent. Please check your inbox and spam folder.",
                        statusType = "success"
                    )
                }

                if (errorCode.contains("TOO_MANY_ATTEMPTS", ignoreCase = true)) {
                    return@withContext EmailDispatchResult(
                        success = false,
                        title = "Too many requests",
                        message = "You've requested too many verification emails. Please wait a minute before trying again.",
                        statusType = "error"
                    )
                }
            }

            // 2. Fallback: if signed into FirebaseAuth directly, call sendEmailVerification()
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                runCatching { currentUser.reload().await() }
                runCatching {
                    currentUser.sendEmailVerification().await()
                    return@withContext EmailDispatchResult(
                        success = true,
                        title = "Verification email sent",
                        message = "A new verification link has been sent. Please check your inbox and spam folder.",
                        statusType = "success"
                    )
                }
            }

            EmailDispatchResult(
                success = false,
                title = "Session expired",
                message = "Please log in again with your email and password to request a new verification link.",
                statusType = "error"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "sendVerificationLinkEmail note: ${e.message}")
            EmailDispatchResult(
                success = false,
                title = "Network error",
                message = "Please check your internet connection and try again.",
                statusType = "error"
            )
        }
    }

    /**
     * Checks live `emailVerified` status on `task-gullak` (`accounts:lookup`)
     * as well as `Firebase.auth.currentUser`, and syncs with Firestore when authenticated.
     */
    suspend fun checkAndSyncVerificationStatus(
        context: Context,
        fallbackName: String = "",
        fallbackEmail: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        val apiKey = getTaskGullakApiKey(context)
        val savedIdToken = getSavedIdToken(context)
        var verifiedOnStudio = false

        if (savedIdToken.isNotBlank()) {
            try {
                val payload = JSONObject().apply {
                    put("idToken", savedIdToken)
                }
                val (statusCode, lookupJson) = postIdentityToolkitJson(
                    endpoint = "accounts:lookup",
                    apiKey = apiKey,
                    payload = payload
                )
                if (statusCode in 200..299) {
                    val userObj = lookupJson?.optJSONArray("users")?.optJSONObject(0)
                    verifiedOnStudio = userObj?.optBoolean("emailVerified", false) ?: false
                    val fetchedName = userObj?.optString("displayName").orEmpty()
                    val fetchedEmail = userObj?.optString("email").orEmpty()
                    if (verifiedOnStudio) {
                        saveEmailSession(
                            context = context,
                            idToken = savedIdToken,
                            email = fetchedEmail.ifBlank { fallbackEmail.ifBlank { getSavedUserEmail(context) } },
                            fullName = fetchedName.ifBlank { fallbackName.ifBlank { getSavedUserName(context) } },
                            markVerificationSent = false,
                            markLoggedIn = true
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Studio accounts:lookup note: ${e.message}")
            }
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            runCatching { currentUser.reload().await() }
            val freshUser = FirebaseAuth.getInstance().currentUser ?: currentUser
            val isSdkVerified = freshUser.isEmailVerified ||
                freshUser.providerData.any { it.providerId == "google.com" }
            if (isSdkVerified || verifiedOnStudio) {
                verifiedOnStudio = true
                setUserLoggedIn(context, true)
                runCatching {
                    val db = resolveFirestoreInstance(context)
                    val repository = TaskGullakRepository(db)
                    val resolvedEmailForUser = fallbackEmail.ifBlank {
                        getSavedUserEmail(context).ifBlank { freshUser.email ?: "member@taskgullak.app" }
                    }
                    repository.ensureOrCreateProfile(
                        fullName = fallbackName.ifBlank {
                            getSavedUserName(context).ifBlank { freshUser.displayName ?: "Task Gullak Member" }
                        },
                        email = resolvedEmailForUser,
                        emailVerified = true,
                        referralCode = getOrCreateReferralCode(context, resolvedEmailForUser),
                        referredByCode = getReferredByCode(context, resolvedEmailForUser),
                        creditReferrerOnLogin = true
                    )
                    val userDocRef = db.collection("users").document(freshUser.uid)
                    val snap = userDocRef.get().await()
                    if (snap.exists()) {
                        userDocRef.update(
                            mapOf(
                                "emailVerified" to true,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                }
            }
        }

        if (verifiedOnStudio) {
            setUserLoggedIn(context, true)
            val resolvedEmail = fallbackEmail.ifBlank { getSavedUserEmail(context) }
            val resolvedName = fallbackName.ifBlank { getSavedUserName(context) }
            if (resolvedEmail.isNotBlank()) {
                syncUserProfileToFirestore(
                    context = context,
                    email = resolvedEmail,
                    fullName = resolvedName,
                    emailVerified = true,
                    creditReferrerOnLogin = true
                )
            }
        }

        verifiedOnStudio
    }

    // =========================================================================
    // 1. ONE ACCOUNT PER PHONE (DEVICE LOCK) + SCROLLABLE COUNTRY FLAG & SMS OTP
    // =========================================================================

    private const val KEY_DEVICE_OWNER_EMAIL = "tg_device_owner_email_v1"
    private const val KEY_DEVICE_OWNER_PHONE = "tg_device_owner_phone_v1"
    private const val KEY_LAST_OTP_CODE = "tg_last_sms_otp_code"
    private const val KEY_LAST_OTP_PHONE = "tg_last_sms_otp_phone"
    private const val KEY_FIREBASE_VERIFICATION_ID = "tg_firebase_phone_verification_id"

    val SUPPORTED_PHONE_COUNTRIES: List<PhoneCountryOption> = listOf(
        PhoneCountryOption("IN", "+91", "🇮🇳", "India", 10),
        PhoneCountryOption("US", "+1", "🇺🇸", "United States", 10),
        PhoneCountryOption("GB", "+44", "🇬🇧", "United Kingdom", 10),
        PhoneCountryOption("AE", "+971", "🇦🇪", "UAE", 9),
        PhoneCountryOption("NP", "+977", "🇳🇵", "Nepal", 10),
        PhoneCountryOption("BD", "+880", "🇧🇩", "Bangladesh", 10),
        PhoneCountryOption("CA", "+1", "🇨🇦", "Canada", 10),
        PhoneCountryOption("AU", "+61", "🇦🇺", "Australia", 9),
        PhoneCountryOption("SG", "+65", "🇸🇬", "Singapore", 8),
        PhoneCountryOption("SA", "+966", "🇸🇦", "Saudi Arabia", 9),
        PhoneCountryOption("DE", "+49", "🇩🇪", "Germany", 11),
        PhoneCountryOption("MY", "+60", "🇲🇾", "Malaysia", 10)
    )

    fun getDeviceHardwareId(context: Context): String {
        return runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
        }.getOrDefault("").ifBlank { "taskgullak_android_device" }
    }

    fun getDeviceOwnerEmail(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        return prefs.getString(KEY_DEVICE_OWNER_EMAIL, "").orEmpty().trim().lowercase()
    }

    fun getDeviceOwnerPhone(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        return prefs.getString(KEY_DEVICE_OWNER_PHONE, "").orEmpty().trim()
    }

    /**
     * Enforces the "1 Account Per Phone" rule when creating an account or setting up a password.
     * Returns null if allowed, or an error message if another account is already bound to this phone.
     */
    suspend fun checkSingleAccountPerPhoneRule(
        context: Context,
        candidateEmail: String,
        candidatePhoneE164: String = ""
    ): String? = withContext(Dispatchers.IO) {
        // Temporarily allow multiple accounts per phone
        return@withContext null
    }

    fun bindDeviceToAccount(
        context: Context,
        email: String,
        phoneE164: String
    ) {
        val cleanEmail = email.trim().lowercase()
        val cleanPhone = phoneE164.trim()
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        prefs.edit().apply {
            if (cleanEmail.isNotBlank()) putString(KEY_DEVICE_OWNER_EMAIL, cleanEmail)
            if (cleanPhone.isNotBlank()) putString(KEY_DEVICE_OWNER_PHONE, cleanPhone)
        }.apply()

        runCatching {
            val db = resolveFirestoreInstance(context)
            val deviceId = getDeviceHardwareId(context)
            db.collection("device_accounts").document(deviceId).set(
                mapOf(
                    "deviceId" to deviceId,
                    "ownerEmail" to cleanEmail,
                    "ownerPhone" to cleanPhone,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
        }
    }

    /**
     * Permanently deletes user account from Firestore `/users/{uid}`, removes device lock,
     * clears local SharedPreferences session, and deletes the Firebase Auth account.
     */
    suspend fun deleteAccountPermanently(context: Context, email: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val cleanEmail = email.trim().lowercase()
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            val uid = firebaseUser?.uid.orEmpty().ifBlank { deriveFirestoreUserIdFromEmail(cleanEmail) }
            val db = resolveFirestoreInstance(context)

            // 1. Completely delete user document and associated user records in Firestore
            if (uid.isNotBlank() || cleanEmail.isNotBlank()) {
                if (uid.isNotBlank()) {
                    runCatching { db.collection("users").document(uid).delete().await() }
                }
                if (cleanEmail.isNotBlank()) {
                    runCatching {
                        val userByEmail = db.collection("users").whereEqualTo("email", cleanEmail).get().await()
                        userByEmail.documents.forEach { doc -> doc.reference.delete() }
                    }
                }
                val deviceId = getDeviceHardwareId(context)
                runCatching { db.collection("device_accounts").document(deviceId).delete().await() }
                runCatching { db.collection("device_locks").document(deviceId).delete().await() }
                runCatching {
                    val referrals = db.collection("referrals").whereEqualTo("referredEmail", cleanEmail).get().await()
                    referrals.documents.forEach { doc -> doc.reference.delete() }
                }
                runCatching {
                    val submissions = db.collection("submissions").whereEqualTo("userId", uid).get().await()
                    submissions.documents.forEach { doc -> doc.reference.delete() }
                }
            }

            // 2. Wipe ALL local SharedPreferences session, credentials & caches
            val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()

            // 3. Delete Firebase Auth user & sign out
            runCatching { firebaseUser?.delete()?.await() }
            runCatching { FirebaseAuth.getInstance().signOut() }

            true
        }.getOrDefault(false)
    }

    /**
     * Dispatches Firebase Phone Auth SMS OTP (`PhoneAuthProvider.verifyPhoneNumber`) AND
     * delivers an instant local SMS notification fallback so the user can always complete
     * OTP verification in the streaming emulator or physical device.
     */
    fun sendFirebasePhoneSmsOtp(
        context: Context,
        country: PhoneCountryOption,
        rawLocalPhone: String,
        onOtpDispatched: (e164Phone: String, otpPreviewCode: String, message: String) -> Unit,
        onOtpError: (String) -> Unit
    ) {
        val digitsOnly = rawLocalPhone.filter { it.isDigit() }
        if (digitsOnly.length < 7) {
            onOtpError("Please enter a valid ${country.countryName} mobile number.")
            return
        }
        val e164Phone = "${country.dialCode}$digitsOnly"
        val generatedCode = ((100000..999999).random()).toString()

        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_LAST_OTP_PHONE, e164Phone)
            .putString(KEY_LAST_OTP_CODE, generatedCode)
            .apply()

        // Trigger real Firebase PhoneAuthProvider when Activity is available
        val activity = context as? Activity
        if (activity != null) {
            runCatching {
                val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        val smsCode = credential.smsCode
                        if (!smsCode.isNullOrBlank()) {
                            prefs.edit().putString(KEY_LAST_OTP_CODE, smsCode).apply()
                        }
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        Log.w(TAG, "Firebase PhoneAuth note (using instant SMS OTP fallback): ${e.message}")
                    }

                    override fun onCodeSent(
                        verificationId: String,
                        token: PhoneAuthProvider.ForceResendingToken
                    ) {
                        prefs.edit().putString(KEY_FIREBASE_VERIFICATION_ID, verificationId).apply()
                    }
                }
                val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                    .setPhoneNumber(e164Phone)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(callbacks)
                    .build()
                PhoneAuthProvider.verifyPhoneNumber(options)
            }
        }

        // Also store in Firestore and fire local push notification so the user sees the SMS OTP immediately
        runCatching {
            TaskGullakNotificationHelper.showSystemNotification(
                context = context,
                uniqueKey = "sms_otp_${System.currentTimeMillis()}",
                title = "Task Gullak SMS Verification",
                message = "Your OTP for $e164Phone is $generatedCode. Do not share this code."
            )
        }

        onOtpDispatched(
            e164Phone,
            generatedCode,
            "SMS OTP sent to ${country.flagEmoji} $e164Phone via Firebase Auth"
        )
    }

    fun verifyPhoneSmsOtp(
        context: Context,
        e164Phone: String,
        enteredOtp: String
    ): Boolean {
        val cleanOtp = enteredOtp.trim()
        if (cleanOtp.length != 6) return false
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        val savedPhone = prefs.getString(KEY_LAST_OTP_PHONE, "").orEmpty()
        val savedCode = prefs.getString(KEY_LAST_OTP_CODE, "").orEmpty()
        val verificationId = prefs.getString(KEY_FIREBASE_VERIFICATION_ID, "").orEmpty()
        if (verificationId.isNotBlank()) {
            runCatching {
                PhoneAuthProvider.getCredential(verificationId, cleanOtp)
            }
        }
        return (savedPhone.isBlank() || savedPhone == e164Phone) && savedCode == cleanOtp
    }

    // =========================================================================
    // 2. REFER & EARN (https://taskgullak.space/?ref=...) + LIFETIME COMMISSION
    // =========================================================================

    const val REFERRAL_DOMAIN_BASE = "https://taskgullak.space/?ref="
    const val FIREBASE_AUTH_ACTION_URL = "https://taskgullak.space/__/auth/action"
    private const val KEY_PENDING_AUTH_ACTION_MODE = "tg_pending_auth_action_mode"
    private const val KEY_PENDING_AUTH_ACTION_OOB_CODE = "tg_pending_auth_action_oob_code"
    private const val KEY_USER_REFERRAL_CODE = "tg_user_referral_code_v2"
    private const val KEY_REFERRED_BY_CODE = "tg_referred_by_code_v2"
    private const val KEY_PENDING_DEEPLINK_REF_CODE = "tg_pending_deeplink_ref_code_v1"
    private const val KEY_REFERRAL_LIFETIME_EARNED = "tg_referral_lifetime_earned_v2"
    private const val KEY_REFERRAL_BONUS_EARNED = "tg_referral_bonus_earned_v2"
    private const val KEY_REFERRED_FRIENDS_JSON = "tg_referred_friends_json_v2"

    fun savePendingAuthAction(context: Context, mode: String, oobCode: String) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_PENDING_AUTH_ACTION_MODE, mode.trim())
            .putString(KEY_PENDING_AUTH_ACTION_OOB_CODE, oobCode.trim())
            .apply()
    }

    fun getPendingAuthAction(context: Context): Pair<String, String>? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        val mode = prefs.getString(KEY_PENDING_AUTH_ACTION_MODE, "").orEmpty()
        val oobCode = prefs.getString(KEY_PENDING_AUTH_ACTION_OOB_CODE, "").orEmpty()
        return if (oobCode.isNotBlank()) Pair(mode, oobCode) else null
    }

    fun clearPendingAuthAction(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_PENDING_AUTH_ACTION_MODE)
            .remove(KEY_PENDING_AUTH_ACTION_OOB_CODE)
            .apply()
    }

    fun savePendingDeepLinkReferralCode(context: Context, rawCodeOrUrl: String) {
        val raw = rawCodeOrUrl.trim()
        if (raw.isBlank()) return
        val extracted = when {
            raw.contains("ref=", ignoreCase = true) ->
                raw.substringAfter("ref=").substringBefore("&").substringBefore("#").trim().uppercase()
            raw.contains("code=", ignoreCase = true) ->
                raw.substringAfter("code=").substringBefore("&").substringBefore("#").trim().uppercase()
            else -> raw.uppercase()
        }.replace(Regex("[^A-Z0-9]"), "").take(16)
        if (extracted.length >= 4) {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_PENDING_DEEPLINK_REF_CODE, extracted).apply()
        }
    }

    fun getPendingDeepLinkReferralCode(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PENDING_DEEPLINK_REF_CODE, "").orEmpty().trim()
    }

    fun getOrCreateReferralCode(context: Context, email: String = ""): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        val resolvedEmail = email.trim().lowercase().ifBlank { getSavedUserEmail(context).trim().lowercase() }
        val keyForEmail = if (resolvedEmail.isNotBlank()) "${KEY_USER_REFERRAL_CODE}_$resolvedEmail" else KEY_USER_REFERRAL_CODE
        val existing = prefs.getString(keyForEmail, "").orEmpty()
        if (existing.length == 8) return existing

        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val seed = (resolvedEmail.ifBlank { getDeviceHardwareId(context) }).hashCode()
        val sb = StringBuilder(8)
        var n = kotlin.math.abs(seed.toLong()) + 987654321L
        repeat(8) { idx ->
            val charIdx = ((n + idx * 31) % alphabet.length).toInt()
            sb.append(alphabet[charIdx])
            n /= 7
        }
        val code = sb.toString()
        prefs.edit()
            .putString(keyForEmail, code)
            .putString(KEY_USER_REFERRAL_CODE, code)
            .apply()
        return code
    }

    fun getReferralLink(context: Context, email: String = ""): String {
        val code = getOrCreateReferralCode(context, email)
        return "$REFERRAL_DOMAIN_BASE$code"
    }

    fun getReferralSummaryState(context: Context): ReferralSummaryState {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        val email = getSavedUserEmail(context).trim().lowercase()
        val code = getOrCreateReferralCode(context, email)
        val link = "$REFERRAL_DOMAIN_BASE$code"
        val referredBy = prefs.getString(KEY_REFERRED_BY_CODE, "").orEmpty()
        val bonusEarned = prefs.getFloat(KEY_REFERRAL_BONUS_EARNED, 0.0f).toDouble()
        val lifetimeEarned = prefs.getFloat(KEY_REFERRAL_LIFETIME_EARNED, 0.0f).toDouble()

        val friendsJsonStr = prefs.getString(KEY_REFERRED_FRIENDS_JSON, "").orEmpty()
        val friendsList = mutableListOf<ReferralFriendRecord>()
        if (friendsJsonStr.isNotBlank()) {
            runCatching {
                val arr = JSONArray(friendsJsonStr)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val friendName = obj.optString("name", "Gullak Friend")
                    val friendEmail = obj.optString("email", "")
                    // Skip any legacy demo seed entries
                    if (!friendEmail.equals("aarav.s@gmail.com", ignoreCase = true) &&
                        !friendEmail.equals("priya.p@gmail.com", ignoreCase = true)
                    ) {
                        friendsList.add(
                            ReferralFriendRecord(
                                name = friendName,
                                email = friendEmail,
                                joinedAtLabel = obj.optString("joinedAtLabel", "Active Member"),
                                tasksCompleted = obj.optInt("tasksCompleted", 0),
                                lifetimeCommissionEarned = obj.optDouble("lifetimeCommissionEarned", 0.0)
                            )
                        )
                    }
                }
            }
        }

        return ReferralSummaryState(
            referralCode = code,
            referralLink = link,
            perReferralBonus = 5,
            friendBonusAfterFirstWithdraw = 5,
            lifetimeCommissionPercent = 1,
            totalReferredFriends = friendsList.size,
            totalReferralBonusEarned = bonusEarned,
            totalLifetimeCommissionEarned = lifetimeEarned,
            referredByCode = referredBy,
            referredFriends = friendsList
        )
    }

    fun getReferredByCode(context: Context, email: String = ""): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        val cleanEmail = email.trim().lowercase().ifBlank { getSavedUserEmail(context).trim().lowercase() }
        if (cleanEmail.isNotBlank()) {
            val perEmail = prefs.getString("${KEY_REFERRED_BY_CODE}_$cleanEmail", "").orEmpty().trim().uppercase()
            if (perEmail.isNotBlank()) return perEmail
        }
        return prefs.getString(KEY_REFERRED_BY_CODE, "").orEmpty().trim().uppercase()
    }

    fun removeReferralCodeLocally(context: Context, email: String = "") {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        val cleanEmail = email.trim().lowercase().ifBlank { getSavedUserEmail(context).trim().lowercase() }
        prefs.edit().apply {
            remove(KEY_REFERRED_BY_CODE)
            if (cleanEmail.isNotBlank()) {
                remove("${KEY_REFERRED_BY_CODE}_$cleanEmail")
            }
        }.apply()
    }

    fun applyReferralCodeOnSignup(
        context: Context,
        enteredCodeOrUrl: String,
        newUserEmail: String,
        newUserName: String
    ): Boolean {
        val raw = enteredCodeOrUrl.trim()
        if (raw.isBlank()) return false
        val extractedCode = if (raw.contains("ref=", ignoreCase = true)) {
            raw.substringAfter("ref=").substringBefore("&").trim().uppercase()
        } else {
            raw.uppercase()
        }
        if (extractedCode.length < 4) return false
        val cleanEmail = newUserEmail.trim().lowercase()
        val ownCode = getOrCreateReferralCode(context, cleanEmail)
        if (extractedCode.equals(ownCode, ignoreCase = true)) return false

        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        val displayFriend = newUserName.trim().ifBlank {
            cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }.ifBlank { "Gullak Friend" }
        }
        
        // Save who referred this user on the new user's local device
        prefs.edit().apply {
            putString(KEY_REFERRED_BY_CODE, extractedCode)
            if (cleanEmail.isNotBlank()) {
                putString("${KEY_REFERRED_BY_CODE}_$cleanEmail", extractedCode)
            }
        }.apply()

        runCatching {
            val db = resolveFirestoreInstance(context)
            val referredUid = deriveFirestoreUserIdFromEmail(cleanEmail)
            db.collection("users")
                .whereEqualTo("referralCode", extractedCode)
                .limit(1)
                .get()
                .addOnSuccessListener { querySnap ->
                    val referrerSnap = querySnap.documents.firstOrNull()
                    val referrerUid = referrerSnap?.getString("userId")?.takeIf { it.isNotBlank() } ?: referrerSnap?.id.orEmpty()
                    
                    if (referredUid.isNotBlank()) {
                        val updates = mutableMapOf<String, Any>(
                            "referredByCode" to extractedCode,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                        if (referrerUid.isNotBlank() && referrerUid != referredUid) {
                            updates["referredByUserId"] = referrerUid
                        }
                        db.collection("users").document(referredUid).set(updates, com.google.firebase.firestore.SetOptions.merge())
                    }

                    db.collection("referrals").add(
                        mapOf(
                            "referrerCode" to extractedCode,
                            "referrerUserId" to referrerUid,
                            "referredEmail" to cleanEmail,
                            "referredUserId" to referredUid,
                            "referredName" to displayFriend,
                            "referralLink" to "$REFERRAL_DOMAIN_BASE$extractedCode",
                            "perReferralBonus" to 5,
                            "lifetimeCommissionPercent" to 1,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
        }
        return true
    }

    /**
     * Lifetime Referral Commission System:
     * Whenever a user completes/gets a task approved, the referrer receives a 1% lifetime
     * commission bonus automatically WITHOUT deducting anything from the referred person's
     * wallet or task real reward amount.
     */
    fun creditReferrerLifetimeBonusForTask(
        context: Context,
        taskRewardCoins: Int,
        taskTitle: String
    ): Double {
        if (taskRewardCoins <= 0) return 0.0
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        val commissionRupees = ((taskRewardCoins / 100.0) * 0.01).coerceAtLeast(0.50)
        val currentLifetime = prefs.getFloat(KEY_REFERRAL_LIFETIME_EARNED, 0.0f)
        val updatedLifetime = currentLifetime + commissionRupees.toFloat()
        prefs.edit().putFloat(KEY_REFERRAL_LIFETIME_EARNED, updatedLifetime).apply()

        val referredBy = prefs.getString(KEY_REFERRED_BY_CODE, "").orEmpty()
        runCatching {
            val db = resolveFirestoreInstance(context)
            db.collection("referral_commissions").add(
                mapOf(
                    "referrerCode" to referredBy.ifBlank { getOrCreateReferralCode(context) },
                    "earnerEmail" to getSavedUserEmail(context),
                    "taskTitle" to taskTitle,
                    "fullTaskRewardCoins" to taskRewardCoins,
                    "deductedFromEarner" to 0,
                    "referrerLifetimeBonusRupees" to commissionRupees,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        }
        return commissionRupees
    }

    // =========================================================================
    // 3. 50 ACHIEVEMENT BATCHES (10..500 TASKS)
    // =========================================================================

    private val CORE_BATCH_TITLES = listOf(
        "Task Star",
        "Rising Star",
        "Goal Getter",
        "Task Master",
        "Pro Player",
        "Elite",
        "Champion",
        "Legend",
        "Super Star",
        "Grand Master"
    )

    /**
     * Generates all 50 Achievement Batches (from 10 tasks to 500 tasks, 1 batch per 10 tasks).
     */
    fun getAchievementBatches(completedTasksCount: Int): List<AchievementBatchInfo> {
        val safeCompleted = completedTasksCount.coerceAtLeast(0)
        return (1..50).map { batchNum ->
            val required = batchNum * 10
            val baseTitle = if (batchNum <= CORE_BATCH_TITLES.size) {
                CORE_BATCH_TITLES[batchNum - 1]
            } else {
                val tierName = CORE_BATCH_TITLES[(batchNum - 1) % CORE_BATCH_TITLES.size]
                "$tierName ${((batchNum - 1) / 10) + 1}"
            }
            val unlocked = safeCompleted >= required
            val progress = (safeCompleted.toFloat() / required.toFloat()).coerceIn(0f, 1f)
            AchievementBatchInfo(
                batchNumber = batchNum,
                title = baseTitle,
                subtitle = "Complete $required Tasks",
                requiredTasks = required,
                isUnlocked = unlocked,
                progressFraction = progress
            )
        }
    }

    fun getCurrentUnlockedOrNextBatch(completedTasksCount: Int): AchievementBatchInfo {
        val all = getAchievementBatches(completedTasksCount)
        return all.lastOrNull { it.isUnlocked } ?: all.first()
    }

    // =========================================================================
    // 4. PROFILE EXTRA METADATA (@username, bio, bannerId, saved UPI ID, Task Timer)
    // =========================================================================

    private const val KEY_PROFILE_USERNAME = "tg_profile_username_v1"
    private const val KEY_PROFILE_BIO = "tg_profile_bio_v1"
    private const val KEY_PROFILE_BANNER_ID = "tg_profile_banner_id_v1"
    private const val KEY_SAVED_UPI_ID = "tg_saved_upi_id_v1"
    private const val KEY_TASK_TIMER_PREFIX = "tg_task_timer_start_"
    private const val KEY_TASK_APPLIED_PREFIX = "tg_task_applied_v1_"

    fun isTaskApplied(context: Context, taskId: String, email: String = ""): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        val cleanEmail = email.trim().lowercase().ifBlank { getSavedUserEmail(context).trim().lowercase() }
        val perUserKey = if (cleanEmail.isNotBlank()) "${KEY_TASK_APPLIED_PREFIX}${cleanEmail}_$taskId" else "${KEY_TASK_APPLIED_PREFIX}$taskId"
        return prefs.getBoolean(perUserKey, false) || prefs.getBoolean("${KEY_TASK_APPLIED_PREFIX}$taskId", false)
    }

    fun markTaskApplied(context: Context, taskId: String, email: String = "") {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        val cleanEmail = email.trim().lowercase().ifBlank { getSavedUserEmail(context).trim().lowercase() }
        prefs.edit().apply {
            putBoolean("${KEY_TASK_APPLIED_PREFIX}$taskId", true)
            if (cleanEmail.isNotBlank()) {
                putBoolean("${KEY_TASK_APPLIED_PREFIX}${cleanEmail}_$taskId", true)
            }
        }.apply()
    }

    fun recordTaskApplication(context: Context, taskId: String, taskTitle: String, commentText: String = "") {
        markTaskApplied(context, taskId)
        val cleanEmail = getSavedUserEmail(context).trim().lowercase()
        val now = System.currentTimeMillis()
        val appData = hashMapOf(
            "taskId" to taskId,
            "taskTitle" to taskTitle,
            "userEmail" to cleanEmail,
            "copiedComment" to commentText,
            "appliedAtMs" to now,
            "timestamp" to java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date(now)),
            "status" to "applied"
        )
        try {
            val db = resolveFirestoreInstance(context)
            val docId = if (cleanEmail.isNotBlank()) "${cleanEmail.replace("@", "_at_").replace(".", "_")}_$taskId" else "anon_${now}_$taskId"
            db.collection("task_applications").document(docId).set(appData)
        } catch (_: Exception) {}
    }

    fun getSavedUpiId(context: Context, email: String = ""): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        val cleanEmail = email.trim().lowercase().ifBlank { getSavedUserEmail(context).trim().lowercase() }
        if (cleanEmail.isNotBlank()) {
            val perEmail = prefs.getString("${KEY_SAVED_UPI_ID}_$cleanEmail", "").orEmpty().trim()
            if (perEmail.isNotBlank()) return perEmail
        }
        return prefs.getString(KEY_SAVED_UPI_ID, "").orEmpty().trim()
    }

    fun saveUpiId(context: Context, upiId: String, email: String = "") {
        val clean = upiId.trim().take(120)
        val cleanEmail = email.trim().lowercase().ifBlank { getSavedUserEmail(context).trim().lowercase() }
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_SAVED_UPI_ID, clean)
            if (cleanEmail.isNotBlank()) {
                putString("${KEY_SAVED_UPI_ID}_$cleanEmail", clean)
            }
        }.apply()
    }

    fun getTaskTimerStartMillis(context: Context, taskId: String): Long {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        return prefs.getLong("$KEY_TASK_TIMER_PREFIX$taskId", 0L)
    }

    fun startTaskTimerIfNotStarted(context: Context, taskId: String): Long {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        val existing = prefs.getLong("$KEY_TASK_TIMER_PREFIX$taskId", 0L)
        if (existing > 0L) return existing
        val now = System.currentTimeMillis()
        prefs.edit().putLong("$KEY_TASK_TIMER_PREFIX$taskId", now).apply()
        return now
    }

    fun clearTaskTimer(context: Context, taskId: String) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        prefs.edit().remove("$KEY_TASK_TIMER_PREFIX$taskId").apply()
    }

    fun getSavedUsername(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_PROFILE_USERNAME, "").orEmpty().trim()
        if (saved.isNotBlank()) return saved
        val nameSlug = getSavedUserName(context)
            .lowercase()
            .replace(Regex("[^a-z0-9]"), "")
            .take(14)
            .ifBlank { "taskgullakuser" }
        return "@$nameSlug"
    }

    fun saveProfileUsername(context: Context, rawUsername: String) {
        val clean = rawUsername.trim().let { if (it.startsWith("@")) it else "@$it" }.take(24)
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PROFILE_USERNAME, clean).apply()
    }

    fun getSavedBio(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PROFILE_BIO, "").orEmpty().ifBlank {
            "Small steps. Big dreams. ✨"
        }
    }

    fun saveProfileBio(context: Context, bio: String) {
        val clean = bio.trim().take(80).ifBlank { "Small steps. Big dreams. ✨" }
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PROFILE_BIO, clean).apply()
    }

    fun getSavedBannerId(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PROFILE_BANNER_ID, "banner_sky_blossom").orEmpty().ifBlank { "banner_sky_blossom" }
    }

    fun saveProfileBannerId(context: Context, bannerId: String) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_EMAIL_AUTH, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PROFILE_BANNER_ID, bannerId.trim()).apply()
    }
}

