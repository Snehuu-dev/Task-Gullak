package com.taskgullak.app.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val walletBalance: Long = 0L,
    val completedTasksCount: Long = 0L,
    val pendingRewards: Long = 0L,
    val emailVerified: Boolean = false,
    @get:PropertyName("isBanned") @set:PropertyName("isBanned")
    var isBanned: Boolean = false,
    val banReason: String = "",
    val avatarId: String = "",
    val bannerId: String = "",
    val username: String = "",
    val bio: String = "",
    val upiId: String = "",
    @get:PropertyName("isDeleted") @set:PropertyName("isDeleted")
    var isDeleted: Boolean = false,
    val accountStatus: String = "active",
    val firstWithdrawalBonusClaimed: Boolean = false,
    val maintenanceLocked: Boolean = false,
    val referralCode: String = "",
    val referredByCode: String = "",
    val referredByUserId: String = "",
    val referralCommissionRatePercent: Long = 5L,
    val referralCommissionEarned: Long = 0L,
    val lastCreditedCompletedTasksCount: Long = 0L,
    val lastCompletedTaskReward: Long = 50L,
    val dailyStreakDays: Long = 1L,
    val lastStreakDate: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MaintenanceConfig(
    val id: String = "",
    val enabled: Boolean = false,
    val maintenanceMode: Boolean = false,
    val active: Boolean = false,
    val message: String = "Our team is currently working on system upgrades and improvements to bring you a better experience. Everything will be back to normal shortly."
) {
    val isMaintenanceActive: Boolean
        get() = enabled || maintenanceMode || active
}

@IgnoreExtraProperties
data class ReferralCommissionEvent(
    val id: String = "",
    val referrerUserId: String = "",
    val referrerCode: String = "",
    val referredUserId: String = "",
    val referredUserName: String = "",
    val referredUserEmail: String = "",
    val completedTasksCountSnapshot: Long = 1L,
    val taskRewardAmount: Long = 50L,
    val commissionPercent: Long = 5L,
    val commissionAmount: Long = 1L,
    val taskTitle: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class AiTaskVerificationResult(
    val completionPercentage: Int,
    val matchedTaskName: String,
    val status: String, // "approved" or "rejected"
    val originalRewardInr: Long,
    val creditedRewardInr: Long,
    val aiVerdict: String, // "ABOVE_95_FULL_100", "90_PERCENT_40_CREDIT", "85_PERCENT_15_CREDIT", "80_PERCENT_10_CREDIT", "BELOW_80_REJECTED"
    val adminNote: String,
    val aiSummary: String
)

@IgnoreExtraProperties
data class TaskSubmission(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val taskId: String = "",
    val taskTitle: String = "",
    val category: String = "",
    val rewardAmount: Long = 0L,
    val originalRewardAmount: Long = 0L,
    val proofText: String = "",
    val screenshotBase64: String = "",
    val referenceScreenshotUrl: String = "",
    val status: String = "pending",
    val adminNote: String = "",
    val aiVerified: Boolean = false,
    val aiCompletionPercent: Long = 0L,
    val aiVerdict: String = "",
    val aiSummary: String = "",
    val matchedTaskName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AdminTask(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "Install App and Rate",
    val category: String = "Task",
    val rewardInr: Long = 5L,
    val price: Long = 0L,
    val reward: Long = 0L,
    val durationMinutes: Long = 15L,
    val timeLimit: Long = 0L,
    val instructions: List<String> = emptyList(),
    val proofRequirement: String = "Upload a clear screenshot showing the completed task as proof.",
    val active: Boolean = true,
    val appName: String = "",
    val iconUrl: String = "",
    val logoUrl: String = "",
    val appIcon: String = "",
    val imageUrl: String = "",
    val storeUrl: String = "",
    val appLink: String = "",
    val link: String = "",
    val referenceScreenshotUrl: String = "",
    val sampleScreenshotUrl: String = "",
    val exampleScreenshotUrl: String = "",
    val sampleProofUrl: String = "",
    val referenceImage: String = "",
    val referenceScreenshotBase64: String = "",
    val exampleScreenshotBase64: String = "",
    val rating: String = "4.5",
    val ratingCountLabel: String = "12.8K",
    val installsLabel: String = "10K+ Installs",
    val targetCount: Long = 100L,
    val completedCount: Long = 0L,
    val assignedComment: String = "",
    val comment: String = "",
    val downloads: String = "",
    val appId: String = "",
    val blockedUserIds: List<String> = emptyList(),
    val commentItems: List<Map<String, Any?>> = emptyList(),
    val commentsList: List<String> = emptyList(),
    @get:PropertyName("comments")
    val comments: List<String> = emptyList(),
    @get:PropertyName("isBulk") @set:PropertyName("isBulk")
    var isBulk: Boolean = false,
    val timerEnabled: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toAvailableTask(): AvailableTask {
        val effectiveReward = when {
            rewardInr > 0L -> rewardInr.toInt()
            price > 0L -> price.toInt()
            reward > 0L -> reward.toInt()
            else -> 5
        }
        val effectiveDuration = when {
            durationMinutes > 0L -> durationMinutes.toInt()
            timeLimit > 0L -> timeLimit.toInt()
            else -> 15
        }
        val resolvedAppName = appName.ifBlank { title.ifBlank { "App Name" } }
        val resolvedStoreUrl = storeUrl.ifBlank { appLink.ifBlank { link } }

        var extractedPfp = ""
        var extractedExampleProof = ""
        val cleanInstructions = mutableListOf<String>()

        val rawInstructions = if (instructions.isNotEmpty()) instructions else listOf(
            "Install the app",
            "Open and use for a few minutes",
            "Give 5 Star Rating & post the assigned comment",
            "Take a Screenshot of the completed task"
        )

        rawInstructions.forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("[TASK_PFP]:") -> {
                    extractedPfp = trimmed.removePrefix("[TASK_PFP]:").trim()
                }
                trimmed.contains("[TASK_PFP]:") -> {
                    val pfpPart = trimmed.substringAfter("[TASK_PFP]:").trim()
                    extractedPfp = pfpPart
                    val textBefore = trimmed.substringBefore("[TASK_PFP]:").trim()
                    if (textBefore.isNotBlank()) cleanInstructions.add(textBefore)
                }
                trimmed.startsWith("[EXAMPLE_PROOF]:") -> {
                    extractedExampleProof = trimmed.removePrefix("[EXAMPLE_PROOF]:").trim()
                }
                trimmed.startsWith("[SAMPLE_PROOF]:") -> {
                    extractedExampleProof = trimmed.removePrefix("[SAMPLE_PROOF]:").trim()
                }
                trimmed.startsWith("[EXAMPLE]:") -> {
                    extractedExampleProof = trimmed.removePrefix("[EXAMPLE]:").trim()
                }
                else -> {
                    cleanInstructions.add(line)
                }
            }
        }

        val resolvedIconUrl = sanitizeIconUrl(
            iconUrl
                .ifBlank { extractedPfp }
                .ifBlank { logoUrl }
                .ifBlank { appIcon }
                .ifBlank { imageUrl }
                .ifBlank { deriveIconUrlFromLink(resolvedStoreUrl) }
        )

        val resolvedReferenceScreenshot = referenceScreenshotUrl
            .ifBlank { extractedExampleProof }
            .ifBlank { sampleScreenshotUrl }
            .ifBlank { exampleScreenshotUrl }
            .ifBlank { sampleProofUrl }
            .ifBlank { referenceImage }
            .ifBlank { referenceScreenshotBase64 }
            .ifBlank { exampleScreenshotBase64 }

        val resolvedComments = comments
            .ifEmpty { commentsList }
        val resolvedComment = assignedComment
            .ifBlank { comment }
            .ifBlank { resolvedComments.firstOrNull().orEmpty() }
            .ifBlank {
                "Great app! Very easy to use and helpful features. Highly recommended! 👍"
            }

        val resolvedDownloads = downloads.ifBlank { installsLabel }.ifBlank { "10K+ Installs" }
        val formattedInstalls = resolvedDownloads
            .replace("Downloads", "Installs", ignoreCase = true)
            .let { if (it.contains("Installs", ignoreCase = true)) it else "$it Installs" }

        return AvailableTask(
            id = id.ifBlank { "task_${resolvedAppName.lowercase().replace(Regex("[^a-z0-9]+"), "_")}" },
            title = title.ifBlank { resolvedAppName },
            subtitle = subtitle.ifBlank { "Install App and Rate" },
            category = category.ifBlank { "Task" },
            rewardInr = effectiveReward.coerceAtLeast(1),
            durationMinutes = effectiveDuration.coerceAtLeast(1),
            instructions = cleanInstructions,
            proofRequirement = proofRequirement
                .replace("admin panel", "verification team", ignoreCase = true)
                .replace("within the timer", "after completing the steps", ignoreCase = true)
                .ifBlank {
                    "Upload a clear screenshot showing the completed task as proof."
                },
            appName = resolvedAppName,
            iconUrl = resolvedIconUrl,
            storeUrl = resolvedStoreUrl,
            referenceScreenshotUrl = resolvedReferenceScreenshot,
            rating = (rating.toDoubleOrNull() ?: 4.5).let { if (it > 0.0) it else 4.5 },
            ratingCountLabel = ratingCountLabel.ifBlank { "12.8K" },
            installsLabel = formattedInstalls,
            targetCount = targetCount.toInt().coerceAtLeast(1),
            completedCount = completedCount.toInt().coerceAtLeast(0),
            assignedComment = resolvedComment,
            commentsList = resolvedComments,
            isBulk = isBulk || category.contains("bulk", ignoreCase = true) || targetCount > 1L,
            timerEnabled = false,
            totalCommentsCount = commentItems.size.coerceAtLeast(resolvedComments.size),
            availableCommentsCount = resolvedComments.size
        )
    }

    companion object {
        fun sanitizeIconUrl(url: String): String {
            val trimmed = url.trim()
            if (trimmed.contains("s2/favicons?domain=play.google", ignoreCase = true) ||
                trimmed.contains("s2/favicons?domain=google.com", ignoreCase = true)
            ) {
                return ""
            }
            return trimmed
        }

        fun deriveIconUrlFromLink(rawUrl: String): String {
            val trimmed = rawUrl.trim()
            if (trimmed.isBlank()) return ""
            // If it's already a direct image link, return it directly
            if (trimmed.endsWith(".png", ignoreCase = true) ||
                trimmed.endsWith(".jpg", ignoreCase = true) ||
                trimmed.endsWith(".jpeg", ignoreCase = true) ||
                trimmed.endsWith(".webp", ignoreCase = true) ||
                trimmed.contains("googleusercontent.com", ignoreCase = true) ||
                trimmed.contains("imgur.com", ignoreCase = true)
            ) {
                return trimmed
            }
            // Do NOT return play.google.com favicon as it is the generic Play Store triangular logo!
            return try {
                val uri = java.net.URI(if (trimmed.startsWith("http")) trimmed else "https://$trimmed")
                val host = uri.host?.lowercase().orEmpty()
                if (host.isBlank() || host.contains("play.google") || host.contains("google.com")) {
                    ""
                } else {
                    "https://www.google.com/s2/favicons?domain=$host&sz=128"
                }
            } catch (_: Exception) {
                ""
            }
        }
    }
}

@IgnoreExtraProperties
data class BroadcastNotification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "broadcast",
    val targetUserId: String = "all",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class WalletTransaction(
    val id: String = "",
    val userId: String = "",
    val amount: Long = 0L,
    val type: String = "admin_credit", // admin_credit, admin_deduct, task_reward, withdrawal, referral_commission
    val note: String = "",
    val status: String = "completed", // pending, approved, processing, paid, completed, rejected
    val payoutMethod: String = "",
    val upiId: String = "",
    val withdrawalId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class AvailableTask(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val rewardInr: Int,
    val durationMinutes: Int,
    val instructions: List<String>,
    val proofRequirement: String,
    val appName: String = "",
    val iconUrl: String = "",
    val storeUrl: String = "",
    val referenceScreenshotUrl: String = "",
    val rating: Double = 4.5,
    val ratingCountLabel: String = "12.8K",
    val installsLabel: String = "10K+ Installs",
    val targetCount: Int = 100,
    val completedCount: Int = 0,
    val assignedComment: String = "Great app! Very easy to use and helpful features. Highly recommended! 👍",
    val commentsList: List<String> = emptyList(),
    val isBulk: Boolean = false,
    val timerEnabled: Boolean = false,
    val totalCommentsCount: Int = 0,
    val availableCommentsCount: Int = 0
)

val DEFAULT_AVAILABLE_TASKS = emptyList<AvailableTask>()

@IgnoreExtraProperties
data class SupportTicket(
    val id: String = "",
    val ticketId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val walletIdCode: String = "",
    val message: String = "",
    val status: String = "open", // open, replied, closed
    val teamReply: String = "",
    val adminReply: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val displayReply: String
        get() = teamReply.ifBlank { adminReply }
}

@IgnoreExtraProperties
data class SupportBotMessage(
    val id: String = "",
    val userId: String = "",
    val sender: String = "bot", // "user", "bot", "admin"
    val text: String = "",
    val actionType: String = "", // "none", "confirm_remove_referral", "confirm_toggle_notifications", "confirm_raise_ticket"
    val actionData: String = "",
    val actionExecuted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TaskCommentItem(
    val id: String = "",
    val appTitle: String = "",
    val appId: String = "",
    val taskId: String = "",
    val commentText: String = "",
    val language: String = "English",
    val status: String = "available", // "available", "reserved", "submitted"
    val assignedUser: String = "",
    val claimedAtMillis: Long = 0L,
    val claimExpiresAtMillis: Long = 0L,
    val submittedAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
) {
    fun isClaimExpired(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val isReserved = status.equals("reserved", ignoreCase = true) ||
            status.equals("claimed", ignoreCase = true) ||
            status.equals("assigned", ignoreCase = true)
        if (!isReserved || submittedAtMillis > 0L) return false
        return claimExpiresAtMillis > 0L && nowMillis > claimExpiresAtMillis
    }

    fun isAvailableForPool(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (status.equals("submitted", ignoreCase = true) || submittedAtMillis > 0L) return false
        if (status.equals("available", ignoreCase = true)) return true
        return isClaimExpired(nowMillis)
    }

    fun isClaimedByUser(userId: String, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (userId.isBlank() || assignedUser != userId) return false
        if (status.equals("submitted", ignoreCase = true) || submittedAtMillis > 0L) return false
        return status.equals("reserved", ignoreCase = true) && !isClaimExpired(nowMillis)
    }

    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "id" to id,
        "appTitle" to appTitle,
        "appId" to appId,
        "taskId" to taskId,
        "commentText" to commentText,
        "language" to language,
        "status" to status,
        "assignedUser" to assignedUser,
        "claimedAtMillis" to claimedAtMillis,
        "claimExpiresAtMillis" to claimExpiresAtMillis,
        "submittedAtMillis" to submittedAtMillis,
        "updatedAtMillis" to updatedAtMillis
    )
}

@IgnoreExtraProperties
data class UserOfferTask(
    val id: String = "",
    val appId: String = "",
    val title: String = "",
    val storeUrl: String = "",
    val iconUrl: String = "",
    val referenceScreenshotUrl: String = "",
    val rewardInr: Int = 0,
    val durationMinutes: Int = 15,
    val appRating: String = "",     // Only shown when non-blank (real fetched value)
    val appDownloads: String = "",  // Only shown when non-blank (real fetched value)
    val instructions: List<String> = emptyList(),
    val active: Boolean = true,
    val blockedUserIds: List<String> = emptyList(),
    val commentItems: List<TaskCommentItem> = emptyList()
) {
    /**
     * Point 5 & Point 8:
     * - Checks if the user is blocked specifically from this app/task (`blockedUserIds`).
     * - Checks completion strictly by `taskId` (`completedTaskIds`), so multiple tasks
     *   for the same app remain independently available unless the user completed THIS specific taskId.
     */
    fun isVisibleToUser(currentUserId: String, completedTaskIds: Set<String>): Boolean {
        if (!active) return false
        if (currentUserId.isNotBlank() && blockedUserIds.contains(currentUserId)) return false
        if (completedTaskIds.contains(id)) return false
        return true
    }

    /**
     * Strips any raw http/https URL lines from instructions so raw links are never exposed.
     */
    val cleanInstructions: List<String>
        get() = instructions
            .filterNot { it.startsWith("[TASK_PFP]:") || it.startsWith("[EXAMPLE_PROOF]:") }
            .map { line ->
                // Remove raw URLs if present in legacy task instructions
                line.replace(Regex("https?://\\S+"), "the button below").trim()
            }
            .filter { it.isNotEmpty() }
}

