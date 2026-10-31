package com.taskgullak.app.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.taskgullak.app.MainActivity
import com.taskgullak.app.R
import com.taskgullak.app.data.model.AvailableTask
import com.taskgullak.app.data.model.BroadcastNotification
import com.taskgullak.app.data.model.TaskSubmission
import com.taskgullak.app.data.model.UserProfile
import com.taskgullak.app.data.model.WalletTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

data class ReceivedPushNotification(
    val id: String,
    val title: String,
    val message: String,
    val category: String = "fcm_push",
    val timestampMillis: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

object TaskGullakNotificationHelper {

    const val CHANNEL_ID = "tg_admin_live_updates"
    const val EXTRA_OPEN_NOTIFICATIONS = "extra_open_notifications_screen"
    private const val CHANNEL_NAME = "Task Gullak Live Updates & Rewards"
    private const val CHANNEL_DESC =
        "Real-time alerts for new tasks, announcements, task approvals, and wallet rewards."
    private const val PREFS_NOTIFIED = "tg_notified_admin_events"
    private const val KEY_INITIALIZED_BASELINE = "baseline_initialized_v1"
    private const val KEY_LAST_WALLET_BALANCE = "last_wallet_balance_v1"
    private const val KEY_HAS_LAST_WALLET_BALANCE = "has_last_wallet_balance_v1"
    private const val KEY_LAST_BANNED_STATE = "last_banned_state_v1"
    private const val KEY_HAS_LAST_BANNED_STATE = "has_last_banned_state_v1"
    private const val KEY_FCM_TOKEN = "fcm_registration_token_v1"
    private const val KEY_FCM_TOPICS = "fcm_subscribed_topics_v1"
    private const val KEY_PUSH_HISTORY_JSON = "fcm_received_push_history_v1"
    private const val KEY_DISMISSED_NOTIFICATION_IDS = "dismissed_notification_ids_v1"

    private val _receivedPushFlow = MutableStateFlow<List<ReceivedPushNotification>>(emptyList())
    val receivedPushFlow: StateFlow<List<ReceivedPushNotification>> = _receivedPushFlow.asStateFlow()

    private val _dismissedIdsFlow = MutableStateFlow<Set<String>>(emptySet())
    val dismissedIdsFlow: StateFlow<Set<String>> = _dismissedIdsFlow.asStateFlow()

    @Synchronized
    fun loadPersistedPushHistory(context: Context): List<ReceivedPushNotification> {
        val prefs = context.getSharedPreferences(PREFS_NOTIFIED, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_PUSH_HISTORY_JSON, null)
        if (raw.isNullOrBlank()) {
            _receivedPushFlow.value = emptyList()
            return emptyList()
        }
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<ReceivedPushNotification>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                list.add(
                    ReceivedPushNotification(
                        id = obj.optString("id", "push_$i"),
                        title = obj.optString("title", "Task Gullak Alert"),
                        message = obj.optString("message", ""),
                        category = obj.optString("category", "fcm_push"),
                        timestampMillis = obj.optLong("timestampMillis", System.currentTimeMillis()),
                        isRead = obj.optBoolean("isRead", false)
                    )
                )
            }
            _receivedPushFlow.value = list
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun recordReceivedPushNotification(
        context: Context,
        id: String,
        title: String,
        message: String,
        category: String = "fcm_push",
        timestampMillis: Long = System.currentTimeMillis()
    ) {
        val current = loadPersistedPushHistory(context).toMutableList()
        if (current.none { it.id == id }) {
            current.add(
                0,
                ReceivedPushNotification(
                    id = id,
                    title = title,
                    message = message,
                    category = category,
                    timestampMillis = timestampMillis,
                    isRead = false
                )
            )
            val trimmed = current.take(40)
            savePushHistoryList(context, trimmed)
            _receivedPushFlow.value = trimmed
        }
    }

    @Synchronized
    fun markAllPushNotificationsRead(context: Context) {
        val updated = loadPersistedPushHistory(context).map { it.copy(isRead = true) }
        savePushHistoryList(context, updated)
        _receivedPushFlow.value = updated
    }

    @Synchronized
    fun clearPushNotificationHistory(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NOTIFIED, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_PUSH_HISTORY_JSON).apply()
        _receivedPushFlow.value = emptyList()
    }

    @Synchronized
    fun loadDismissedNotificationIds(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NOTIFIED, Context.MODE_PRIVATE)
        val saved = prefs.getStringSet(KEY_DISMISSED_NOTIFICATION_IDS, emptySet()).orEmpty()
        _dismissedIdsFlow.value = saved
        return saved
    }

    @Synchronized
    fun deleteNotificationById(context: Context, notificationId: String) {
        if (notificationId.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NOTIFIED, Context.MODE_PRIVATE)
        val dismissed = HashSet(prefs.getStringSet(KEY_DISMISSED_NOTIFICATION_IDS, emptySet()).orEmpty())
        dismissed.add(notificationId)
        prefs.edit().putStringSet(KEY_DISMISSED_NOTIFICATION_IDS, dismissed).apply()
        _dismissedIdsFlow.value = dismissed

        if (notificationId.startsWith("push_")) {
            val rawPushId = notificationId.removePrefix("push_")
            val remaining = loadPersistedPushHistory(context).filterNot { it.id == rawPushId }
            savePushHistoryList(context, remaining)
            _receivedPushFlow.value = remaining
        }
    }

    @Synchronized
    fun deleteAllNotifications(context: Context, allNotificationIds: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NOTIFIED, Context.MODE_PRIVATE)
        val dismissed = HashSet(prefs.getStringSet(KEY_DISMISSED_NOTIFICATION_IDS, emptySet()).orEmpty())
        dismissed.addAll(allNotificationIds)
        prefs.edit()
            .putStringSet(KEY_DISMISSED_NOTIFICATION_IDS, dismissed)
            .remove(KEY_PUSH_HISTORY_JSON)
            .apply()
        _dismissedIdsFlow.value = dismissed
        _receivedPushFlow.value = emptyList()
    }

    private fun savePushHistoryList(context: Context, list: List<ReceivedPushNotification>) {
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("message", item.message)
                put("category", item.category)
                put("timestampMillis", item.timestampMillis)
                put("isRead", item.isRead)
            }
            arr.put(obj)
        }
        val prefs = context.getSharedPreferences(PREFS_NOTIFIED, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PUSH_HISTORY_JSON, arr.toString()).apply()
    }

    fun saveFcmToken(context: Context, token: String) {
        if (token.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NOTIFIED, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
    }

    fun getSavedFcmToken(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NOTIFIED, Context.MODE_PRIVATE)
        return prefs.getString(KEY_FCM_TOKEN, "").orEmpty()
    }

    fun saveSubscribedTopics(context: Context, topics: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NOTIFIED, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_FCM_TOPICS, topics).apply()
    }

    fun getSubscribedTopics(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NOTIFIED, Context.MODE_PRIVATE)
        return prefs.getStringSet(
            KEY_FCM_TOPICS,
            setOf("all_users", "task_gullak_broadcasts", "new_tasks")
        ) ?: setOf("all_users", "task_gullak_broadcasts", "new_tasks")
    }

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESC
                    enableVibration(true)
                    enableLights(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @Synchronized
    fun showSystemNotification(
        context: Context,
        uniqueKey: String,
        title: String,
        message: String
    ) {
        val prefs = context.getSharedPreferences(PREFS_NOTIFIED, Context.MODE_PRIVATE)
        if (prefs.getBoolean(uniqueKey, false)) {
            return
        }
        prefs.edit().putBoolean(uniqueKey, true).apply()

        ensureNotificationChannel(context)
        if (!hasNotificationPermission(context)) {
            return
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_NOTIFICATIONS, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            uniqueKey.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(uniqueKey.hashCode(), notification)
        } catch (_: SecurityException) {
            // Permission may have been revoked at runtime
        }
    }

    @Synchronized
    fun markEventSeenSilently(context: Context, uniqueKey: String) {
        val prefs = context.getSharedPreferences(PREFS_NOTIFIED, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(uniqueKey, false)) {
            prefs.edit().putBoolean(uniqueKey, true).apply()
        }
    }

    @Synchronized
    fun isEventAlreadySeen(context: Context, uniqueKey: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NOTIFIED, Context.MODE_PRIVATE)
        return prefs.getBoolean(uniqueKey, false)
    }

    @Synchronized
    fun isBaselineInitialized(context: Context, scopeKey: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NOTIFIED, Context.MODE_PRIVATE)
        return prefs.getBoolean("${KEY_INITIALIZED_BASELINE}_$scopeKey", false)
    }

    @Synchronized
    fun setBaselineInitialized(context: Context, scopeKey: String) {
        val prefs = context.getSharedPreferences(PREFS_NOTIFIED, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("${KEY_INITIALIZED_BASELINE}_$scopeKey", true).apply()
    }

    @Synchronized
    fun processTasksSnapshot(context: Context, tasksList: List<AvailableTask>) {
        if (!isBaselineInitialized(context, "tasks")) {
            tasksList.forEach { task ->
                markEventSeenSilently(context, "task_${task.id}")
            }
            setBaselineInitialized(context, "tasks")
        } else {
            tasksList.forEach { task ->
                val uniqueKey = "task_${task.id}"
                if (!isEventAlreadySeen(context, uniqueKey)) {
                    showSystemNotification(
                        context = context,
                        uniqueKey = uniqueKey,
                        title = "New Project Added: ${task.title}",
                        message = "Earn ₹${task.rewardInr} by completing '${task.title}'. Open Offers to start!"
                    )
                }
            }
        }
    }

    @Synchronized
    fun processBroadcastsSnapshot(context: Context, list: List<BroadcastNotification>) {
        val currentUserUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val currentUserEmail = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: ""
        val emailSlug = currentUserEmail.substringBefore("@").replace(".", "_")

        val activeBroadcasts = list.filter {
            it.active &&
            (it.targetUserId.isEmpty() || it.targetUserId.equals("all", ignoreCase = true) || it.targetUserId == currentUserUid || (emailSlug.isNotBlank() && it.targetUserId == "user_$emailSlug")) &&
            it.id != "system_maintenance_banner"
        }

        if (!isBaselineInitialized(context, "broadcasts")) {
            activeBroadcasts.forEach { b ->
                markEventSeenSilently(context, "broadcast_${b.id}")
            }
            setBaselineInitialized(context, "broadcasts")
        } else {
            activeBroadcasts.forEach { b ->
                val uniqueKey = "broadcast_${b.id}"
                if (!isEventAlreadySeen(context, uniqueKey)) {
                    showSystemNotification(
                        context = context,
                        uniqueKey = uniqueKey,
                        title = b.title.ifBlank { "Task Gullak Announcement" },
                        message = b.message
                    )
                }
            }
        }
    }

    @Synchronized
    fun processSubmissionsSnapshot(context: Context, list: List<TaskSubmission>) {
        if (!isBaselineInitialized(context, "submissions_status")) {
            list.forEach { sub ->
                markEventSeenSilently(
                    context,
                    "sub_status_${sub.id}_${sub.status.lowercase()}"
                )
            }
            setBaselineInitialized(context, "submissions_status")
        } else {
            list.forEach { sub ->
                val statusLower = sub.status.lowercase()
                val uniqueKey = "sub_status_${sub.id}_$statusLower"
                if (!isEventAlreadySeen(context, uniqueKey)) {
                    when (statusLower) {
                        "approved", "verified" -> {
                            com.taskgullak.app.data.remote.FirebaseEmailDispatchService.creditReferrerLifetimeBonusForTask(
                                context = context,
                                taskRewardCoins = (sub.rewardAmount.toInt() * 100).coerceAtLeast(100),
                                taskTitle = sub.taskTitle
                            )
                            showSystemNotification(
                                context = context,
                                uniqueKey = uniqueKey,
                                title = "Task Approved! +₹${sub.rewardAmount} Credited",
                                message = "Your submission for '${sub.taskTitle}' has been verified and approved."
                            )
                        }
                        "rejected", "declined" -> {
                            val sanitizedNote = sub.adminNote
                                .replace(Regex("admin panel", RegexOption.IGNORE_CASE), "Review Desk")
                                .replace(Regex("\\badmin\\b", RegexOption.IGNORE_CASE), "Verification Team")
                                .replace(Regex("\\bfirebase\\b", RegexOption.IGNORE_CASE), "Cloud")
                                .replace(Regex("\\bbackend\\b", RegexOption.IGNORE_CASE), "System")
                            val reasonSuffix = sanitizedNote.takeIf { it.isNotBlank() }?.let { " Note: $it" } ?: ""
                            showSystemNotification(
                                context = context,
                                uniqueKey = uniqueKey,
                                title = "Task Submission Rejected",
                                message = "Your submission for '${sub.taskTitle}' was not approved.$reasonSuffix"
                            )
                        }
                        else -> {
                            markEventSeenSilently(context, uniqueKey)
                        }
                    }
                }
            }
        }
    }

    @Synchronized
    fun processTransactionsSnapshot(context: Context, list: List<WalletTransaction>) {
        if (!isBaselineInitialized(context, "wallet_tx")) {
            list.forEach { tx ->
                markEventSeenSilently(context, "tx_${tx.id}")
            }
            setBaselineInitialized(context, "wallet_tx")
        } else {
            list.forEach { tx ->
                val uniqueKey = "tx_${tx.id}"
                if (!isEventAlreadySeen(context, uniqueKey)) {
                    val typeLower = tx.type.lowercase()
                    val isCredit = !typeLower.contains("deduct") && !typeLower.contains("debit")
                    val cleanNote = tx.note
                        .replace(Regex("bonus credited by admin", RegexOption.IGNORE_CASE), "Bonus received")
                        .replace(Regex("credited by admin", RegexOption.IGNORE_CASE), "Received")
                        .replace(Regex("adjusted by admin", RegexOption.IGNORE_CASE), "Adjustment deducted")
                        .replace(Regex("deducted by admin", RegexOption.IGNORE_CASE), "Deducted")
                        .replace(Regex("admin panel", RegexOption.IGNORE_CASE), "Support Desk")
                        .replace(Regex("\\badmin\\b", RegexOption.IGNORE_CASE), "Support")
                        .replace(Regex("\\bfirebase\\b", RegexOption.IGNORE_CASE), "Cloud")
                        .replace(Regex("\\bbackend\\b", RegexOption.IGNORE_CASE), "Server")
                    val bodyText = if (cleanNote.isNotBlank()) {
                        cleanNote
                    } else if (isCredit) {
                        "₹${tx.amount} added to your Task Gullak wallet."
                    } else {
                        "₹${tx.amount} deducted from your Task Gullak wallet."
                    }
                    showSystemNotification(
                        context = context,
                        uniqueKey = uniqueKey,
                        title = if (isCredit) "Received: +₹${tx.amount}" else "Deducted: -₹${tx.amount}",
                        message = bodyText
                    )
                }
            }
        }
    }

    @Synchronized
    fun processUserProfileSnapshot(context: Context, profile: UserProfile) {
        if (profile.userId.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NOTIFIED, Context.MODE_PRIVATE)
        val hasBalKey = "${KEY_HAS_LAST_WALLET_BALANCE}_${profile.userId}"
        val balKey = "${KEY_LAST_WALLET_BALANCE}_${profile.userId}"
        val hasBanKey = "${KEY_HAS_LAST_BANNED_STATE}_${profile.userId}"
        val banKey = "${KEY_LAST_BANNED_STATE}_${profile.userId}"

        if (prefs.getBoolean(hasBalKey, false)) {
            val oldBal = prefs.getLong(balKey, profile.walletBalance)
            if (oldBal != profile.walletBalance) {
                val diff = profile.walletBalance - oldBal
                val uniqueKey = "bal_${profile.userId}_${oldBal}_to_${profile.walletBalance}"
                if (diff > 0L) {
                    showSystemNotification(
                        context = context,
                        uniqueKey = uniqueKey,
                        title = "₹$diff Added to Gullak Wallet!",
                        message = "Your updated Task Gullak balance is ₹${profile.walletBalance}."
                    )
                } else if (diff < 0L) {
                    showSystemNotification(
                        context = context,
                        uniqueKey = uniqueKey,
                        title = "Wallet Balance Updated (-₹${-diff})",
                        message = "Your updated Task Gullak balance is ₹${profile.walletBalance}."
                    )
                }
            }
        }
        prefs.edit()
            .putBoolean(hasBalKey, true)
            .putLong(balKey, profile.walletBalance)
            .apply()

        if (prefs.getBoolean(hasBanKey, false)) {
            val oldBan = prefs.getBoolean(banKey, profile.isBanned)
            if (oldBan != profile.isBanned) {
                val uniqueKey = "ban_${profile.userId}_${oldBan}_to_${profile.isBanned}"
                if (profile.isBanned) {
                    showSystemNotification(
                        context = context,
                        uniqueKey = uniqueKey,
                        title = "Account Access Restricted",
                        message = profile.banReason.ifBlank { "Your Task Gullak account has been temporarily restricted." }
                    )
                } else {
                    showSystemNotification(
                        context = context,
                        uniqueKey = uniqueKey,
                        title = "Account Access Restored!",
                        message = "Your Task Gullak account access has been restored."
                    )
                }
            }
        }
        prefs.edit()
            .putBoolean(hasBanKey, true)
            .putBoolean(banKey, profile.isBanned)
            .apply()
    }

    /**
     * High-quality proof screenshot compressor using maxDimensionPx = 1280, jpegQuality = 92,
     * and ARGB_8888 encoding for crisp screenshot verification in Admin Panel.
     */
    suspend fun encodeProofScreenshotHighQuality(
        context: Context,
        uri: Uri,
        maxDimensionPx: Int = 1280,
        jpegQuality: Int = 92
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext null
            val opts = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inScaled = false
            }
            val original = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, opts)
                ?: return@withContext null
            val scale = minOf(1f, maxDimensionPx.toFloat() / maxOf(original.width, original.height).toFloat())
            val resized = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    original,
                    (original.width * scale).toInt().coerceAtLeast(1),
                    (original.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else original
            val out = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, jpegQuality, out)
            "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        }.getOrNull()
    }

    suspend fun compressUriToBase64DataUrl(
        context: Context,
        uri: Uri,
        maxDimensionPx: Int = 1280,
        jpegQuality: Int = 92
    ): String? {
        return encodeProofScreenshotHighQuality(context, uri, maxDimensionPx, jpegQuality)
    }
}
