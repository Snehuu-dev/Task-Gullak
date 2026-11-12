package com.taskgullak.app.service

import android.app.job.JobParameters
import android.app.job.JobService
import com.taskgullak.app.R
import com.taskgullak.app.data.model.AdminTask
import com.taskgullak.app.data.model.BroadcastNotification
import com.taskgullak.app.data.model.TaskSubmission
import com.taskgullak.app.data.model.UserProfile
import com.taskgullak.app.data.model.WalletTransaction
import com.taskgullak.app.data.remote.FirebaseEmailDispatchService
import com.taskgullak.app.util.TaskGullakNotificationHelper
import com.google.firebase.Timestamp
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TaskGullakSyncJobService : JobService() {

    override fun onStartJob(params: JobParameters?): Boolean {
        val ctx = applicationContext

        val uid = FirebaseAuth.getInstance().currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: FirebaseEmailDispatchService.resolveActiveFirestoreUserId(ctx)
        if (uid.isNullOrBlank()) {
            jobFinished(params, false)
            return false
        }
        val databaseId = try {
            ctx.getString(R.string.firestore_database_id)
        } catch (_: Exception) {
            "(default)"
        }

        val firestore = try {
            FirebaseFirestore.getInstance(databaseId)
        } catch (_: Exception) {
            jobFinished(params, false)
            return false
        }

        val broadcastsTask = firestore.collection("broadcasts").get()
        val tasksTask = firestore.collection("tasks").get()
        val submissionsTask = firestore.collection("submissions").whereEqualTo("userId", uid).get()
        val profileTask = firestore.collection("users").document(uid).get()
        val txTask = firestore.collection("users").document(uid).collection("transactions").get()

        Tasks.whenAllComplete(broadcastsTask, tasksTask, submissionsTask, profileTask, txTask)
            .addOnCompleteListener {
                try {
                    if (broadcastsTask.isSuccessful) {
                    val broadcasts = broadcastsTask.result?.documents?.mapNotNull { doc ->
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
                    }.orEmpty()
                    TaskGullakNotificationHelper.processBroadcastsSnapshot(ctx, broadcasts)
                }

                    if (tasksTask.isSuccessful) {
                        val activeTasks = tasksTask.result?.documents?.mapNotNull { doc ->
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
                        }?.map { it.toAvailableTask() }.orEmpty()
                        TaskGullakNotificationHelper.processTasksSnapshot(ctx, activeTasks)
                    }

                    if (submissionsTask.isSuccessful) {
                        val submissions = submissionsTask.result?.documents?.mapNotNull { doc ->
                            try {
                                doc.toObject(TaskSubmission::class.java)?.copy(id = doc.id)
                            } catch (_: Exception) {
                                null
                            }
                        }.orEmpty()
                        TaskGullakNotificationHelper.processSubmissionsSnapshot(ctx, submissions)
                    }

                    if (profileTask.isSuccessful) {
                        val doc = profileTask.result
                        if (doc != null && doc.exists()) {
                            val profile = try {
                                doc.toObject(UserProfile::class.java)?.copy(userId = uid)
                            } catch (_: Exception) {
                                null
                            }
                            if (profile != null) {
                                TaskGullakNotificationHelper.processUserProfileSnapshot(ctx, profile)
                            }
                        }
                    }

                    if (txTask.isSuccessful) {
                        val transactions = txTask.result?.documents?.mapNotNull { doc ->
                            try {
                                doc.toObject(WalletTransaction::class.java)?.copy(id = doc.id)
                            } catch (_: Exception) {
                                null
                            }
                        }.orEmpty()
                        TaskGullakNotificationHelper.processTransactionsSnapshot(ctx, transactions)
                    }
                } catch (_: Exception) {
                } finally {
                    jobFinished(params, false)
                }
            }

        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return true
    }
}
