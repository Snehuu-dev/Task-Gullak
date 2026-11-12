package com.taskgullak.app.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import com.taskgullak.app.R
import com.taskgullak.app.data.model.AdminTask
import com.taskgullak.app.data.model.BroadcastNotification
import com.taskgullak.app.data.model.TaskSubmission
import com.taskgullak.app.data.model.UserProfile
import com.taskgullak.app.data.model.WalletTransaction
import com.taskgullak.app.data.remote.FirebaseEmailDispatchService
import com.taskgullak.app.util.TaskGullakNotificationHelper
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class TaskGullakBackgroundSyncService : Service() {

    private val activeListeners = mutableListOf<ListenerRegistration>()
    private var attachedUserId: String? = null

    override fun onCreate() {
        super.onCreate()
        TaskGullakNotificationHelper.ensureNotificationChannel(applicationContext)
        attachFirestoreListenersIfNeeded()
        scheduleBackgroundSyncJobs(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        attachFirestoreListenersIfNeeded()
        scheduleBackgroundSyncJobs(applicationContext)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        // When user swipes Task Gullak away from Recent Apps ("Close All"),
        // schedule JobScheduler sync so notifications continue arriving in background.
        scheduleBackgroundSyncJobs(applicationContext, immediateWakeup = true)
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        clearListeners()
        super.onDestroy()
    }

    @Synchronized
    private fun attachFirestoreListenersIfNeeded() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: FirebaseEmailDispatchService.resolveActiveFirestoreUserId(applicationContext)
        if (uid.isNullOrBlank()) {
            clearListeners()
            return
        }
        if (attachedUserId == uid && activeListeners.isNotEmpty()) {
            return
        }

        clearListeners()
        attachedUserId = uid

        val ctx = applicationContext
        val databaseId = try {
            ctx.getString(R.string.firestore_database_id)
        } catch (_: Exception) {
            "(default)"
        }
        val firestore = try {
            FirebaseFirestore.getInstance(databaseId)
        } catch (_: Exception) {
            return
        }

        // 1. Listen for Admin Panel Broadcast Notifications (/broadcasts)
        val broadcastsReg = firestore.collection("broadcasts")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val broadcasts = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val isActive = (data["active"] as? Boolean) ?: (data["isActive"] as? Boolean) ?: true
                    if (!isActive) null
                    else {
                        BroadcastNotification(
                            id = (data["id"] as? String)?.ifBlank { null } ?: doc.id,
                            title = (data["title"] as? String) ?: "",
                            message = (data["message"] as? String) ?: (data["body"] as? String) ?: "",
                            type = (data["type"] as? String) ?: "broadcast",
                            targetUserId = (data["targetUserId"] as? String) ?: (data["targetUser"] as? String) ?: "all",
                            active = true,
                            createdAt = data["createdAt"] as? Timestamp
                        )
                    }
                }
                TaskGullakNotificationHelper.processBroadcastsSnapshot(ctx, broadcasts)
            }
        activeListeners.add(broadcastsReg)

        // 2. Listen for Admin Panel Newly Added Tasks (/tasks)
        val tasksReg = firestore.collection("tasks")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val tasks = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val isActive = (data["active"] as? Boolean) ?: (data["isActive"] as? Boolean) ?: true
                    val isExplicitlyInactive = !isActive || doc.getString("status")?.equals("inactive", ignoreCase = true) == true
                    if (isExplicitlyInactive) null
                    else {
                        AdminTask(
                            id = doc.id,
                            title = (data["title"] as? String)?.ifBlank { null } ?: (data["appName"] as? String) ?: "",
                            storeUrl = (data["storeUrl"] as? String)?.ifBlank { null } ?: (data["actionUrl"] as? String)?.ifBlank { null } ?: (data["subtitle"] as? String) ?: "",
                            appId = (data["appId"] as? String)?.ifBlank { null } ?: (data["packageName"] as? String) ?: "",
                            iconUrl = (data["iconUrl"] as? String)?.ifBlank { null } ?: (data["appIcon"] as? String)?.ifBlank { null } ?: (data["taskPfpBase64"] as? String) ?: "",
                            rating = (data["appRating"] as? String)?.ifBlank { null } ?: data["rating"]?.toString() ?: "",
                            downloads = (data["appDownloads"] as? String)?.ifBlank { null } ?: (data["downloads"] as? String)?.ifBlank { null } ?: (data["installs"] as? String) ?: "",
                            rewardInr = ((data["rewardInr"] as? Number) ?: (data["rewardAmount"] as? Number) ?: (data["reward"] as? Number))?.toLong() ?: 0L,
                            durationMinutes = ((data["durationMinutes"] as? Number) ?: (data["timerMinutes"] as? Number))?.toLong() ?: 15L,
                            instructions = (data["instructions"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                            comments = (data["comments"] as? List<*>)?.mapNotNull { it?.toString()?.trim()?.takeIf { s -> s.isNotEmpty() } } ?: emptyList(),
                            commentItems = (data["commentItems"] as? List<*>)?.mapNotNull { it as? Map<String, Any?> } ?: emptyList(),
                            blockedUserIds = (data["blockedUserIds"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                            active = true
                        )
                    }
                }.map { it.toAvailableTask() }
                TaskGullakNotificationHelper.processTasksSnapshot(ctx, tasks)
            }
        activeListeners.add(tasksReg)

        // 3. Listen for Task Approval / Rejection updates (/submissions where userId == uid)
        val submissionsReg = firestore.collection("submissions")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val submissions = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(TaskSubmission::class.java)?.copy(id = doc.id)
                    } catch (_: Exception) {
                        null
                    }
                }
                TaskGullakNotificationHelper.processSubmissionsSnapshot(ctx, submissions)
            }
        activeListeners.add(submissionsReg)

        // 4. Listen for User Profile Wallet Credit/Deduct & Ban/Unban (/users/{uid})
        val profileReg = firestore.collection("users")
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                val profile = try {
                    snapshot.toObject(UserProfile::class.java)?.copy(userId = uid)
                } catch (_: Exception) {
                    null
                } ?: return@addSnapshotListener
                TaskGullakNotificationHelper.processUserProfileSnapshot(ctx, profile)
            }
        activeListeners.add(profileReg)

        // 5. Listen for Wallet Transactions (/users/{uid}/transactions)
        val txReg = firestore.collection("users")
            .document(uid)
            .collection("transactions")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val transactions = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(WalletTransaction::class.java)?.copy(id = doc.id)
                    } catch (_: Exception) {
                        null
                    }
                }
                TaskGullakNotificationHelper.processTransactionsSnapshot(ctx, transactions)
            }
        activeListeners.add(txReg)
    }

    @Synchronized
    private fun clearListeners() {
        activeListeners.forEach { reg ->
            try {
                reg.remove()
            } catch (_: Exception) {
            }
        }
        activeListeners.clear()
        attachedUserId = null
    }

    companion object {
        private const val JOB_ID_PERIODIC_SYNC = 4501
        private const val JOB_ID_IMMEDIATE_WAKEUP = 4502
        private const val REQUEST_CODE_RESTART = 4503

        fun startAndEnsureActive(context: Context) {
            val appCtx = context.applicationContext
            TaskGullakNotificationHelper.ensureNotificationChannel(appCtx)
            scheduleBackgroundSyncJobs(appCtx)
            try {
                val intent = Intent(appCtx, TaskGullakBackgroundSyncService::class.java)
                appCtx.startService(intent)
            } catch (_: IllegalStateException) {
                // If app is in restricted background state, trigger immediate JobScheduler sync instead
                scheduleBackgroundSyncJobs(appCtx, immediateWakeup = true)
            } catch (_: Exception) {
            }
        }

        fun stopSyncWhenSignedOut(context: Context) {
            val appCtx = context.applicationContext
            try {
                appCtx.stopService(Intent(appCtx, TaskGullakBackgroundSyncService::class.java))
                val scheduler = appCtx.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler
                scheduler?.cancel(JOB_ID_PERIODIC_SYNC)
                scheduler?.cancel(JOB_ID_IMMEDIATE_WAKEUP)
            } catch (_: Exception) {
            }
        }

        internal fun scheduleBackgroundSyncJobs(context: Context, immediateWakeup: Boolean = false) {
            try {
                val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler
                    ?: return
                val componentName = ComponentName(context, TaskGullakSyncJobService::class.java)

                val periodicExists = scheduler.allPendingJobs.any { it.id == JOB_ID_PERIODIC_SYNC }
                if (!periodicExists) {
                    val periodicJob = JobInfo.Builder(JOB_ID_PERIODIC_SYNC, componentName)
                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                        .setPeriodic(15 * 60 * 1000L)
                        .build()
                    scheduler.schedule(periodicJob)
                }

                if (immediateWakeup) {
                    val oneOffJob = JobInfo.Builder(JOB_ID_IMMEDIATE_WAKEUP, componentName)
                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                        .setMinimumLatency(1000L)
                        .setOverrideDeadline(5000L)
                        .build()
                    scheduler.schedule(oneOffJob)
                }
            } catch (_: Exception) {
            }
        }
    }
}
