package com.taskgullak.app.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.taskgullak.app.data.model.TaskCommentItem
import kotlinx.coroutines.tasks.await

class TaskCommentClaimRepository(
    // Always use standard (default) Firestore database
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        const val DEFAULT_CLAIM_DURATION_MS = 15 * 60 * 1000L // 15 minutes
    }

    /**
     * Point 4 (Case A & Case C):
     * Atomically claims one available comment for `userId` on `taskId` with its own independent
     * 15-minute countdown timer (`claimExpiresAtMillis`).
     * - Automatically releases any expired unsubmitted claims back to `"available"` during transaction.
     * - Checks that the task still exists and is active (Point 3) and that `userId` is not in `blockedUserIds` (Point 5).
     */
    suspend fun claimAvailableComment(
        taskId: String,
        userId: String,
        claimDurationMs: Long = DEFAULT_CLAIM_DURATION_MS
    ): Result<TaskCommentItem> = runCatching {
        val taskRef = firestore.collection("tasks").document(taskId)
        val nowMs = System.currentTimeMillis()
        val expiresMs = nowMs + claimDurationMs

        firestore.runTransaction { tx ->
            val userRef = firestore.collection("users").document(userId)
            val userSnap = tx.get(userRef)
            if (userSnap.exists()) {
                val rawDailyCount = userSnap.getLong("dailyTasksCompletedCount") ?: 0L
                val lastCompletedTs = userSnap.getTimestamp("lastTaskCompletedTimestamp")
                val lastTsMs = lastCompletedTs?.toDate()?.time ?: 0L
                val twentyFourHoursMs = 24L * 60L * 60L * 1000L
                val currentEffectiveDaily = if (lastTsMs > 0L && (nowMs - lastTsMs) >= twentyFourHoursMs) 0L else rawDailyCount
                if (currentEffectiveDaily >= 5L) {
                    throw IllegalStateException("Daily Limit Reached (Max 5 tasks per day). Please try again tomorrow!")
                }
            }

            val snap = tx.get(taskRef)
            if (!snap.exists() || snap.getBoolean("active") == false) {
                throw IllegalStateException("This task is no longer available.")
            }

            val blockedUsers = (snap.get("blockedUserIds") as? List<*>)
                ?.mapNotNull { it?.toString()?.trim() }
                .orEmpty()
            if (blockedUsers.contains(userId)) {
                throw IllegalStateException("You are not eligible for this app's task.")
            }

            val appTitle = snap.getString("title").orEmpty()
            val appId = snap.getString("appId").orEmpty()
            val rawItems = (snap.get("commentItems") as? List<*>)?.mapNotNull { item ->
                val map = item as? Map<*, *> ?: return@mapNotNull null
                TaskCommentItem(
                    id = map["id"]?.toString().orEmpty(),
                    appTitle = map["appTitle"]?.toString().orEmpty().ifBlank { appTitle },
                    appId = map["appId"]?.toString().orEmpty().ifBlank { appId },
                    taskId = taskId,
                    commentText = map["commentText"]?.toString().orEmpty(),
                    language = map["language"]?.toString().orEmpty().ifBlank { "English" },
                    status = map["status"]?.toString().orEmpty().ifBlank { "available" },
                    assignedUser = map["assignedUser"]?.toString().orEmpty(),
                    claimedAtMillis = (map["claimedAtMillis"] as? Number)?.toLong() ?: 0L,
                    claimExpiresAtMillis = (map["claimExpiresAtMillis"] as? Number)?.toLong() ?: 0L,
                    submittedAtMillis = (map["submittedAtMillis"] as? Number)?.toLong() ?: 0L,
                    updatedAtMillis = (map["updatedAtMillis"] as? Number)?.toLong() ?: nowMs
                )
            }.orEmpty()

            // Step 1: Release any expired unsubmitted claims (Case A & Case C)
            val normalizedItems = rawItems.map { item ->
                if (item.isClaimExpired(nowMs)) {
                    item.copy(
                        status = "available",
                        assignedUser = "",
                        claimedAtMillis = 0L,
                        claimExpiresAtMillis = 0L,
                        updatedAtMillis = nowMs
                    )
                } else {
                    item
                }
            }.toMutableList()

            // Step 2: If this user already has an active non-expired claim on this task, return it
            val existingUserClaim = normalizedItems.firstOrNull { it.isClaimedByUser(userId, nowMs) }
            if (existingUserClaim != null) {
                tx.update(taskRef, "commentItems", normalizedItems.map { it.toFirestoreMap() })
                return@runTransaction existingUserClaim
            }

            // Step 3: Pick the first available comment from the pool
            val targetIndex = normalizedItems.indexOfFirst { it.isAvailableForPool(nowMs) }
            if (targetIndex == -1) {
                throw IllegalStateException("All comments for this task are currently claimed or completed.")
            }

            val claimedItem = normalizedItems[targetIndex].copy(
                status = "reserved",
                assignedUser = userId,
                claimedAtMillis = nowMs,
                claimExpiresAtMillis = expiresMs,
                updatedAtMillis = nowMs
            )
            normalizedItems[targetIndex] = claimedItem

            val availableStrings = normalizedItems
                .filter { it.isAvailableForPool(nowMs) }
                .map { it.commentText }

            val totalQty = ((snap.get("totalQuantity") as? Number) ?: (snap.get("total_quantity") as? Number))?.toInt() ?: (rawItems.size.takeIf { it > 0 } ?: 50)
            val usedQty = ((snap.get("usedQuantity") as? Number) ?: (snap.get("used_quantity") as? Number))?.toInt()?.coerceAtLeast(0) ?: 0
            val currentRem = ((snap.get("remainingQuantity") as? Number) ?: (snap.get("remaining_quantity") as? Number))?.toInt() ?: (totalQty - usedQty)

            if (currentRem <= 0) {
                throw IllegalStateException("Stock exhausted (0 remaining)")
            }

            val newUsed = usedQty + 1
            val newRem = (totalQty - newUsed).coerceAtLeast(0)
            val isExhausted = newRem == 0
            val newStatus = if (isExhausted) "exhausted" else (snap.getString("status") ?: "active")
            val newActive = if (isExhausted) false else (snap.getBoolean("active") ?: true)

            tx.update(
                taskRef,
                mapOf(
                    "commentItems" to normalizedItems.map { it.toFirestoreMap() },
                    "comments" to availableStrings,
                    "usedQuantity" to newUsed,
                    "used_quantity" to newUsed,
                    "remainingQuantity" to newRem,
                    "remaining_quantity" to newRem,
                    "status" to newStatus,
                    "active" to newActive,
                    "isActive" to newActive,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            try {
                val offerRef = firestore.collection("offers").document(taskId)
                tx.update(offerRef, mapOf(
                    "usedQuantity" to newUsed,
                    "used_quantity" to newUsed,
                    "remainingQuantity" to newRem,
                    "remaining_quantity" to newRem,
                    "status" to newStatus,
                    "active" to newActive,
                    "isActive" to newActive,
                    "updatedAt" to FieldValue.serverTimestamp()
                ))
            } catch (ignored: Exception) {}

            // Sync subcollection document as well
            if (claimedItem.id.isNotBlank()) {
                val subDoc = taskRef.collection("comments").document(claimedItem.id)
                tx.set(subDoc, claimedItem.toFirestoreMap())
            }

            claimedItem
        }.await()
    }

    /**
     * Point 4 (Case A & Case C):
     * Releases a specific comment back to `"available"` ONLY if its proof was NOT submitted.
     * Other claimed comments by the same or other users remain untouched.
     */
    suspend fun releaseCommentIfUnsubmitted(
        taskId: String,
        commentId: String,
        userId: String
    ): Result<Unit> = runCatching {
        val taskRef = firestore.collection("tasks").document(taskId)
        val nowMs = System.currentTimeMillis()

        firestore.runTransaction { tx ->
            val snap = tx.get(taskRef)
            if (!snap.exists()) return@runTransaction

            val rawItems = (snap.get("commentItems") as? List<*>)?.mapNotNull { item ->
                val map = item as? Map<*, *> ?: return@mapNotNull null
                TaskCommentItem(
                    id = map["id"]?.toString().orEmpty(),
                    appTitle = map["appTitle"]?.toString().orEmpty(),
                    appId = map["appId"]?.toString().orEmpty(),
                    taskId = taskId,
                    commentText = map["commentText"]?.toString().orEmpty(),
                    language = map["language"]?.toString().orEmpty().ifBlank { "English" },
                    status = map["status"]?.toString().orEmpty().ifBlank { "available" },
                    assignedUser = map["assignedUser"]?.toString().orEmpty(),
                    claimedAtMillis = (map["claimedAtMillis"] as? Number)?.toLong() ?: 0L,
                    claimExpiresAtMillis = (map["claimExpiresAtMillis"] as? Number)?.toLong() ?: 0L,
                    submittedAtMillis = (map["submittedAtMillis"] as? Number)?.toLong() ?: 0L,
                    updatedAtMillis = (map["updatedAtMillis"] as? Number)?.toLong() ?: nowMs
                )
            }.orEmpty()

            val updatedItems = rawItems.map { item ->
                val matchesTarget = item.id == commentId && (item.assignedUser == userId || item.isClaimExpired(nowMs))
                // Only release if NOT submitted
                if ((matchesTarget || item.isClaimExpired(nowMs)) &&
                    !item.status.equals("submitted", ignoreCase = true) &&
                    item.submittedAtMillis == 0L
                ) {
                    item.copy(
                        status = "available",
                        assignedUser = "",
                        claimedAtMillis = 0L,
                        claimExpiresAtMillis = 0L,
                        updatedAtMillis = nowMs
                    )
                } else {
                    item
                }
            }

            val availableStrings = updatedItems
                .filter { it.isAvailableForPool(nowMs) }
                .map { it.commentText }

            tx.update(
                taskRef,
                mapOf(
                    "commentItems" to updatedItems.map { it.toFirestoreMap() },
                    "comments" to availableStrings,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }.await()
    }

    /**
     * Point 4 (Case B) & Point 1 (Per-Task Folder Mapping):
     * Submits the user's screenshot proof with the exact `taskId` and permanently marks
     * the claimed comment as `"submitted"` so it never returns to the available pool.
     */
    suspend fun submitTaskProofAndLockComment(
        taskId: String,
        taskTitle: String,
        rewardInr: Int,
        userId: String,
        userName: String,
        userEmail: String,
        claimedComment: TaskCommentItem,
        screenshotBase64: String
    ): Result<String> = runCatching {
        val taskRef = firestore.collection("tasks").document(taskId)
        val submissionRef = firestore.collection("submissions").document()
        val nowMs = System.currentTimeMillis()

        firestore.runTransaction { tx ->
            val snap = tx.get(taskRef)
            if (!snap.exists()) {
                throw IllegalStateException("Task no longer exists.")
            }

            val rawItems = (snap.get("commentItems") as? List<*>)?.mapNotNull { item ->
                val map = item as? Map<*, *> ?: return@mapNotNull null
                TaskCommentItem(
                    id = map["id"]?.toString().orEmpty(),
                    appTitle = map["appTitle"]?.toString().orEmpty(),
                    appId = map["appId"]?.toString().orEmpty(),
                    taskId = taskId,
                    commentText = map["commentText"]?.toString().orEmpty(),
                    language = map["language"]?.toString().orEmpty().ifBlank { "English" },
                    status = map["status"]?.toString().orEmpty().ifBlank { "available" },
                    assignedUser = map["assignedUser"]?.toString().orEmpty(),
                    claimedAtMillis = (map["claimedAtMillis"] as? Number)?.toLong() ?: 0L,
                    claimExpiresAtMillis = (map["claimExpiresAtMillis"] as? Number)?.toLong() ?: 0L,
                    submittedAtMillis = (map["submittedAtMillis"] as? Number)?.toLong() ?: 0L,
                    updatedAtMillis = (map["updatedAtMillis"] as? Number)?.toLong() ?: nowMs
                )
            }.orEmpty()

            // Permanently mark this comment as "submitted" (Case B)
            val updatedItems = rawItems.map { item ->
                if (item.id == claimedComment.id ||
                    item.commentText.equals(claimedComment.commentText, ignoreCase = true)
                ) {
                    item.copy(
                        status = "submitted",
                        assignedUser = userId,
                        submittedAtMillis = nowMs,
                        updatedAtMillis = nowMs
                    )
                } else if (item.isClaimExpired(nowMs)) {
                    item.copy(
                        status = "available",
                        assignedUser = "",
                        claimedAtMillis = 0L,
                        claimExpiresAtMillis = 0L,
                        updatedAtMillis = nowMs
                    )
                } else {
                    item
                }
            }

            val availableStrings = updatedItems
                .filter { it.isAvailableForPool(nowMs) }
                .map { it.commentText }

            tx.update(
                taskRef,
                mapOf(
                    "commentItems" to updatedItems.map { it.toFirestoreMap() },
                    "comments" to availableStrings,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            // Save submission strictly mapped to `taskId` so Admin Panel saves it in this task's own folder
            val submissionData = mapOf(
                "userId" to userId,
                "userName" to userName,
                "userEmail" to userEmail,
                "taskId" to taskId,
                "taskTitle" to taskTitle,
                "rewardInr" to rewardInr,
                "proofText" to claimedComment.commentText,
                "commentId" to claimedComment.id,
                "screenshotBase64" to screenshotBase64,
                "status" to "pending", // Admin Panel sets "approved" or "rejected" (never "rework")
                "submittedAt" to Timestamp.now()
            )
            tx.set(submissionRef, submissionData)
        }.await()

        submissionRef.id
    }
}
