package com.taskgullak.app.service

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.taskgullak.app.MainActivity
import com.taskgullak.app.R
import com.taskgullak.app.util.TaskGullakNotificationHelper
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * BroadcastReceiver and remote message handler that processes incoming Firebase Cloud Messaging (FCM)
 * payloads and displays local system notifications even when the application is in the background.
 */
class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val action = intent.action.orEmpty()
        if (action == ACTION_HANDLE_REMOTE_MESSAGE ||
            action == ACTION_C2DM_RECEIVE ||
            intent.extras != null
        ) {
            val remoteMessage = try {
                intent.extras?.let { RemoteMessage(it) }
            } catch (_: Exception) {
                null
            }

            if (remoteMessage != null &&
                (!remoteMessage.notification?.title.isNullOrBlank() ||
                    !remoteMessage.notification?.body.isNullOrBlank() ||
                    remoteMessage.data.isNotEmpty())
            ) {
                handleRemoteMessage(context, remoteMessage)
                return
            }

            val title = intent.getStringExtra(EXTRA_TITLE)
                ?: intent.getStringExtra("gcm.notification.title")
                ?: intent.getStringExtra("title")
            val body = intent.getStringExtra(EXTRA_BODY)
                ?: intent.getStringExtra("gcm.notification.body")
                ?: intent.getStringExtra("body")
                ?: intent.getStringExtra("message")
            val category = intent.getStringExtra(EXTRA_CATEGORY) ?: "fcm_push"
            val messageId = intent.getStringExtra(EXTRA_MESSAGE_ID)
                ?: "fcm_${System.currentTimeMillis()}"

            if (!title.isNullOrBlank() || !body.isNullOrBlank()) {
                handleRemoteMessagePayload(
                    context = context,
                    title = title,
                    body = body,
                    data = mapOf("category" to category),
                    messageId = messageId
                )
            }
        }
    }

    companion object {
        private const val TAG = "NotificationReceiver"

        const val ACTION_HANDLE_REMOTE_MESSAGE = "com.taskgullak.app.ACTION_HANDLE_REMOTE_MESSAGE"
        const val ACTION_C2DM_RECEIVE = "com.google.android.c2dm.intent.RECEIVE"
        const val EXTRA_TITLE = "extra_fcm_title"
        const val EXTRA_BODY = "extra_fcm_body"
        const val EXTRA_CATEGORY = "extra_fcm_category"
        const val EXTRA_MESSAGE_ID = "extra_fcm_message_id"

        /**
         * Processes a Firebase [RemoteMessage] (both notification and data payloads) and displays
         * a high-priority local notification via [NotificationCompat] even when the app is backgrounded.
         */
        fun handleRemoteMessage(context: Context, remoteMessage: RemoteMessage) {
            val notificationTitle = remoteMessage.notification?.title
            val notificationBody = remoteMessage.notification?.body
            val dataMap = remoteMessage.data
            val resolvedMessageId = remoteMessage.messageId
                ?: dataMap[EXTRA_MESSAGE_ID]
                ?: dataMap["messageId"]
                ?: dataMap["id"]
                ?: "fcm_${System.currentTimeMillis()}"

            handleRemoteMessagePayload(
                context = context,
                title = notificationTitle,
                body = notificationBody,
                data = dataMap,
                messageId = resolvedMessageId
            )
        }

        /**
         * Extracts title, message, and metadata from an incoming remote message payload,
         * records it in local notification history, and displays a local notification.
         */
        fun handleRemoteMessagePayload(
            context: Context,
            title: String?,
            body: String?,
            data: Map<String, String> = emptyMap(),
            messageId: String? = null
        ) {
            val resolvedTitle = title?.takeIf { it.isNotBlank() }
                ?: data[EXTRA_TITLE]?.takeIf { it.isNotBlank() }
                ?: data["title"]?.takeIf { it.isNotBlank() }
                ?: data["gcm.notification.title"]?.takeIf { it.isNotBlank() }
                ?: "Task Gullak Live Update"

            val resolvedBody = body?.takeIf { it.isNotBlank() }
                ?: data[EXTRA_BODY]?.takeIf { it.isNotBlank() }
                ?: data["body"]?.takeIf { it.isNotBlank() }
                ?: data["message"]?.takeIf { it.isNotBlank() }
                ?: data["gcm.notification.body"]?.takeIf { it.isNotBlank() }
                ?: "You have a new update in Task Gullak. Tap to view details."

            val resolvedCategory = data[EXTRA_CATEGORY]?.takeIf { it.isNotBlank() }
                ?: data["category"]?.takeIf { it.isNotBlank() }
                ?: data["type"]?.takeIf { it.isNotBlank() }
                ?: "fcm_push"

            val uniqueId = messageId?.takeIf { it.isNotBlank() }
                ?: "fcm_${resolvedTitle.hashCode()}_${resolvedBody.hashCode()}_${System.currentTimeMillis() / 1000}"

            TaskGullakNotificationHelper.recordReceivedPushNotification(
                context = context,
                id = uniqueId,
                title = resolvedTitle,
                message = resolvedBody,
                category = resolvedCategory
            )

            displayLocalNotification(
                context = context,
                uniqueId = uniqueId,
                title = resolvedTitle,
                message = resolvedBody
            )
        }

        /**
         * Builds and posts a high-priority local notification that wakes up the system tray
         * and opens [MainActivity] directly into the full-screen Notifications view when tapped.
         */
        fun displayLocalNotification(
            context: Context,
            uniqueId: String,
            title: String,
            message: String
        ) {
            TaskGullakNotificationHelper.ensureNotificationChannel(context)
            if (!TaskGullakNotificationHelper.hasNotificationPermission(context)) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted; notification recorded in-app.")
                return
            }

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(TaskGullakNotificationHelper.EXTRA_OPEN_NOTIFICATIONS, true)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                uniqueId.hashCode(),
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(
                context,
                TaskGullakNotificationHelper.CHANNEL_ID
            )
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
                NotificationManagerCompat.from(context).notify(uniqueId.hashCode(), notification)
            } catch (e: SecurityException) {
                Log.w(TAG, "SecurityException posting local notification: ${e.message}")
            }
        }

        /**
         * Dispatches a broadcast to [NotificationReceiver] to demonstrate handling an incoming
         * remote message immediately or after [delayMillis] (so the user can background the app
         * and observe the local notification arriving while the app is in the background).
         */
        fun dispatchSimulatedRemoteMessage(
            context: Context,
            title: String,
            body: String,
            category: String = "fcm_push",
            delayMillis: Long = 0L
        ) {
            val appContext = context.applicationContext
            val sendBroadcastTask = Runnable {
                val messageId = "fcm_sim_${System.currentTimeMillis()}"
                val intent = Intent(appContext, NotificationReceiver::class.java).apply {
                    action = ACTION_HANDLE_REMOTE_MESSAGE
                    putExtra(EXTRA_TITLE, title)
                    putExtra(EXTRA_BODY, body)
                    putExtra(EXTRA_CATEGORY, category)
                    putExtra(EXTRA_MESSAGE_ID, messageId)
                }
                appContext.sendBroadcast(intent)
            }

            if (delayMillis <= 0L) {
                sendBroadcastTask.run()
            } else {
                Handler(Looper.getMainLooper()).postDelayed(sendBroadcastTask, delayMillis)
            }
        }
    }
}

/**
 * Firebase Cloud Messaging (FCM) client service that receives remote messages and token refreshes
 * from Firebase and forwards payloads to [NotificationReceiver] for background/foreground display.
 */
class TaskGullakFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        TaskGullakNotificationHelper.saveFcmToken(applicationContext, token)
        TaskGullakFcmClient.syncFcmTokenToFirestore(applicationContext, token)
        TaskGullakFcmClient.subscribeDefaultTopics(applicationContext)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        NotificationReceiver.handleRemoteMessage(applicationContext, remoteMessage)
    }
}

/**
 * Helper object for initializing the FCM client, retrieving the registration token,
 * and subscribing the device to Task Gullak push notification topics.
 */
object TaskGullakFcmClient {
    private val DEFAULT_TOPICS = setOf(
        "all_users",
        "task_gullak_broadcasts",
        "new_tasks"
    )

    fun initialize(context: Context) {
        val appContext = context.applicationContext
        TaskGullakNotificationHelper.ensureNotificationChannel(appContext)
        TaskGullakNotificationHelper.loadPersistedPushHistory(appContext)

        try {
            val messaging = FirebaseMessaging.getInstance()
            messaging.isAutoInitEnabled = true
            messaging.token.addOnSuccessListener { token ->
                if (!token.isNullOrBlank()) {
                    TaskGullakNotificationHelper.saveFcmToken(appContext, token)
                    syncFcmTokenToFirestore(appContext, token)
                }
            }
            subscribeDefaultTopics(appContext)
        } catch (_: Exception) {
            // Gracefully handle environments where Google Play Services is unavailable (e.g. unit tests)
        }
    }

    fun syncFcmTokenToFirestore(context: Context, token: String) {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            ?: com.taskgullak.app.data.remote.FirebaseEmailDispatchService.resolveActiveFirestoreUserId(context)
        if (uid.isNullOrBlank() || token.isBlank()) return
        try {
            val databaseId = try { context.getString(R.string.firestore_database_id) } catch (_: Exception) { "(default)" }
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance(databaseId)
            db.collection("users").document(uid).set(
                mapOf(
                    "fcmToken" to token,
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
        } catch (_: Exception) {
        }
    }

    fun subscribeDefaultTopics(context: Context) {
        try {
            val messaging = FirebaseMessaging.getInstance()
            DEFAULT_TOPICS.forEach { topic ->
                messaging.subscribeToTopic(topic)
            }
            TaskGullakNotificationHelper.saveSubscribedTopics(context, DEFAULT_TOPICS)
        } catch (_: Exception) {
            // Ignore in test environments
        }
    }
}
