package com.taskgullak.app.data.repository

import android.content.Context
import android.util.Log
import com.taskgullak.app.R
import com.taskgullak.app.data.model.AdminTask
import com.taskgullak.app.data.model.AiTaskVerificationResult
import com.taskgullak.app.data.model.AvailableTask
import com.taskgullak.app.data.model.BroadcastNotification
import com.taskgullak.app.data.model.BulkCommentAllocation
import com.taskgullak.app.data.model.BulkModRequest
import com.taskgullak.app.data.model.MaintenanceConfig
import com.taskgullak.app.data.model.ReferralCommissionEvent
import com.taskgullak.app.data.model.SupportBotMessage
import com.taskgullak.app.data.model.SupportTicket
import com.taskgullak.app.data.model.TaskSubmission
import com.taskgullak.app.data.model.UserProfile
import com.taskgullak.app.data.model.WalletTransaction
import com.taskgullak.app.data.remote.OperationType
import com.taskgullak.app.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.tasks.await
import kotlin.math.roundToLong

private fun resolveRepositoryFirestore(context: Context): FirebaseFirestore {
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

class TaskGullakRepository(
    private val db: FirebaseFirestore,
    private val appContext: Context? = null
) {
    constructor(context: Context) : this(resolveRepositoryFirestore(context), context.applicationContext)

    private val auth = FirebaseAuth.getInstance()

    private fun requireUserId(explicitUserId: String? = null): String {
        val candidate = explicitUserId?.trim().orEmpty()
        if (candidate.isNotBlank()) return candidate
        val authUid = auth.currentUser?.uid?.takeIf { it.isNotBlank() }
        if (authUid != null) return authUid
        if (appContext != null) {
            val savedUid = com.taskgullak.app.data.remote.FirebaseEmailDispatchService.resolveActiveFirestoreUserId(appContext)
            if (!savedUid.isNullOrBlank()) return savedUid
            val savedEmail = com.taskgullak.app.data.remote.FirebaseEmailDispatchService.getSavedUserEmail(appContext).trim().lowercase()
            if (savedEmail.isNotBlank()) {
                val derived = com.taskgullak.app.data.remote.FirebaseEmailDispatchService.deriveFirestoreUserIdFromEmail(savedEmail)
                if (derived.isNotBlank()) return derived
            }
        }
        return "usr_guest_${System.currentTimeMillis()}"
    }

    /**
     * Calculates the percentage-based referral commission (default 5%) from a referred user's
     * completed task reward without deducting anything from the referred user's reward.
     */
    fun calculateReferralCommissionAmount(
        taskRewardAmount: Long,
        commissionRatePercent: Double = DEFAULT_REFERRAL_COMMISSION_PERCENT
    ): Long {
        if (taskRewardAmount <= 0L || commissionRatePercent <= 0.0) return 0L
        val rawCommission = (taskRewardAmount.toDouble() * (commissionRatePercent / 100.0)).roundToLong()
        return rawCommission.coerceAtLeast(1L)
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val path = "users/$userId"
        return db.collection("users")
            .document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    val model = snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                    val bannedFlag = snapshot.getBoolean("isBanned") ?: false
                    val deletedFlag = snapshot.getBoolean("isDeleted") ?: false
                    val statusVal = snapshot.getString("accountStatus").orEmpty()
                    val avatarIdVal = snapshot.getString("avatarId").orEmpty()
                    val upiIdVal = snapshot.getString("upiId").orEmpty()
                    val maintLocked = snapshot.getBoolean("maintenanceLocked") ?: false
                    model?.copy(
                        isBanned = bannedFlag,
                        isDeleted = deletedFlag || statusVal.equals("deleted", ignoreCase = true),
                        accountStatus = statusVal.ifBlank { model.accountStatus },
                        avatarId = avatarIdVal.ifBlank { model.avatarId },
                        upiId = upiIdVal.ifBlank { model.upiId },
                        maintenanceLocked = maintLocked
                    )
                } else {
                    null
                }
            }
            .onEach { profile ->
                // Real-time listener on the Firestore user document:
                // When this user's completedTasksCount increases and they were referred by someone,
                // automatically calculate and credit the percentage-based commission to the referrer's wallet.
                if (profile != null &&
                    profile.completedTasksCount > profile.lastCreditedCompletedTasksCount &&
                    (profile.referredByUserId.isNotBlank() || profile.referredByCode.isNotBlank())
                ) {
                    runCatching {
                        processReferredUserTaskCompletions(
                            referredProfile = profile,
                            explicitReferrerUserId = profile.referredByUserId.takeIf { it.isNotBlank() }
                        )
                    }
                }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.GET, path)
                }
                throw error
            }
    }

    /**
     * Real-time Firestore listener on `/users` documents of referred users (matched by `referredByUserId`
     * or `referredByCode`) that tracks successful task completions and credits a percentage-based
     * commission to the referrer's wallet in real-time.
     */
    fun observeReferredUsersAndCreditCommission(
        referrerUserId: String,
        referrerCode: String,
        commissionRatePercent: Double = DEFAULT_REFERRAL_COMMISSION_PERCENT
    ): Flow<List<UserProfile>> {
        val path = "users"
        val cleanCode = referrerCode.trim().uppercase()
        val query = if (cleanCode.isNotBlank()) {
            db.collection("users").whereEqualTo("referredByCode", cleanCode)
        } else {
            db.collection("users").whereEqualTo("referredByUserId", referrerUserId)
        }

        return query
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.documents.mapNotNull { doc ->
                    doc.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )?.copy(id = doc.id, userId = doc.getString("userId") ?: doc.id)
                }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
    }

    /**
     * Processes any newly completed tasks on a referred user's Firestore `/users/{referredUserId}` document
     * and credits the percentage-based commission to the referrer's `/users/{referrerUserId}` wallet in real-time.
     */
    suspend fun processReferredUserTaskCompletions(
        referredProfile: UserProfile,
        explicitReferrerUserId: String? = null,
        commissionRatePercent: Double = DEFAULT_REFERRAL_COMMISSION_PERCENT
    ): Result<Long> {
        val referredUid = referredProfile.userId.ifBlank { referredProfile.id }
        if (referredUid.isBlank()) return Result.success(0L)

        val completedCount = referredProfile.completedTasksCount
        val alreadyCreditedCount = referredProfile.lastCreditedCompletedTasksCount
        if (completedCount <= alreadyCreditedCount) {
            return Result.success(0L)
        }

        val resolvedReferrerUid = explicitReferrerUserId?.takeIf { it.isNotBlank() }
            ?: referredProfile.referredByUserId.takeIf { it.isNotBlank() }
            ?: resolveReferrerUserIdByCode(referredProfile.referredByCode)
            ?: return Result.success(0L)

        if (resolvedReferrerUid == referredUid) {
            return Result.success(0L)
        }

        val taskRewardBase = referredProfile.lastCompletedTaskReward.takeIf { it > 0L } ?: 50L
        val effectiveRate = referredProfile.referralCommissionRatePercent
            .takeIf { it in 1L..100L }?.toDouble() ?: commissionRatePercent

        var totalCommissionCredited = 0L
        for (taskIndex in (alreadyCreditedCount + 1L)..completedCount) {
            val singleCreditResult = creditReferrerWalletForReferredTaskCompletion(
                referrerUserId = resolvedReferrerUid,
                referrerCode = referredProfile.referredByCode,
                referredUserId = referredUid,
                referredUserName = referredProfile.fullName.ifBlank { "Referred Friend" },
                referredUserEmail = referredProfile.email,
                completedTasksCountSnapshot = taskIndex,
                taskRewardAmount = taskRewardBase,
                commissionRatePercent = effectiveRate,
                taskTitle = "Task #$taskIndex"
            )
            totalCommissionCredited += singleCreditResult.getOrDefault(0L)
        }

        // Advance lastCreditedCompletedTasksCount on the referred user's document so each completion is credited once
        runCatching {
            db.collection("users").document(referredUid).update(
                mapOf(
                    "lastCreditedCompletedTasksCount" to completedCount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        return Result.success(totalCommissionCredited)
    }

    private suspend fun resolveReferrerUserIdByCode(referralCode: String): String? {
        val clean = referralCode.trim().uppercase()
        if (clean.isBlank()) return null
        return try {
            val snap = db.collection("users")
                .whereEqualTo("referralCode", clean)
                .limit(1)
                .get()
                .await()
            val directDoc = snap.documents.firstOrNull()
            if (directDoc != null) {
                return directDoc.getString("userId")?.takeIf { it.isNotBlank() } ?: directDoc.id
            }

            // Fallback: Scan Firestore users to match derived referral code from email
            val allUsersSnap = db.collection("users").get().await()
            for (doc in allUsersSnap.documents) {
                val userEmail = doc.getString("email").orEmpty()
                val userUid = doc.getString("userId")?.takeIf { it.isNotBlank() } ?: doc.id
                val codeFromEmail = deriveReferralCodeFromEmail(userEmail, userUid)
                if (codeFromEmail.equals(clean, ignoreCase = true)) {
                    // Backfill referralCode on the referrer's document so future direct queries succeed
                    runCatching {
                        db.collection("users").document(userUid).update("referralCode", codeFromEmail).await()
                    }
                    return userUid
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Atomically calculates and credits a percentage-based commission to the referrer's Firestore
     * `/users/{referrerUserId}` wallet (`walletBalance` and `referralCommissionEarned`) and records
     * both a `/referral_commissions/{id}` event and a `/users/{referrerUserId}/transactions/{id}` entry.
     * Nothing is deducted from the referred user's wallet or task reward.
     */
    suspend fun creditReferrerWalletForReferredTaskCompletion(
        referrerUserId: String,
        referrerCode: String = "",
        referredUserId: String,
        referredUserName: String,
        referredUserEmail: String = "",
        completedTasksCountSnapshot: Long,
        taskRewardAmount: Long,
        commissionRatePercent: Double = DEFAULT_REFERRAL_COMMISSION_PERCENT,
        taskTitle: String = "Completed Task"
    ): Result<Long> {
        if (referrerUserId.isBlank() || referredUserId.isBlank() || referrerUserId == referredUserId) {
            return Result.success(0L)
        }
        val commissionAmount = calculateReferralCommissionAmount(taskRewardAmount, commissionRatePercent)
        if (commissionAmount <= 0L) return Result.success(0L)

        val deterministicEventId = "comm_${referrerUserId}_${referredUserId}_${completedTasksCountSnapshot}"
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            .take(120)

        val commissionDocRef = db.collection("referral_commissions").document(deterministicEventId)
        val referrerDocRef = db.collection("users").document(referrerUserId)

        return try {
            val existingCommission = commissionDocRef.get().await()
            if (existingCommission.exists()) {
                // Already credited this exact task completion snapshot
                return Result.success(0L)
            }

            // 1. Record idempotent commission event
            val commissionPayload = mapOf(
                "referrerUserId" to referrerUserId,
                "referrerCode" to referrerCode.trim().uppercase().take(32),
                "referredUserId" to referredUserId,
                "referredUserName" to referredUserName.trim().take(100).ifBlank { "Gullak Friend" },
                "referredUserEmail" to referredUserEmail.trim().take(150),
                "completedTasksCountSnapshot" to completedTasksCountSnapshot,
                "taskRewardAmount" to taskRewardAmount,
                "commissionPercent" to commissionRatePercent.roundToLong().coerceIn(1L, 100L),
                "commissionAmount" to commissionAmount,
                "taskTitle" to taskTitle.trim().take(120).ifBlank { "Completed Task" },
                "createdAt" to FieldValue.serverTimestamp()
            )
            commissionDocRef.set(commissionPayload).await()

            // 2. Credit referrer's Firestore /users/{referrerUserId} wallet directly by document ID
            val referrerSnap = referrerDocRef.get().await()
            if (referrerSnap.exists()) {
                val currentWallet = referrerSnap.getLong("walletBalance") ?: 0L
                val currentCommissionEarned = referrerSnap.getLong("referralCommissionEarned") ?: 0L
                referrerDocRef.update(
                    mapOf(
                        "walletBalance" to (currentWallet + commissionAmount),
                        "referralCommissionEarned" to (currentCommissionEarned + commissionAmount),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                // 3. Log WalletTransaction under /users/{referrerUserId}/transactions
                val txRef = referrerDocRef.collection("transactions").document()
                val noteText = "${commissionRatePercent.roundToLong()}% Referral Commission: " +
                    "${referredUserName.ifBlank { "Friend" }} completed $taskTitle (+₹$commissionAmount)"
                txRef.set(
                    mapOf(
                        "userId" to referrerUserId,
                        "amount" to commissionAmount,
                        "type" to "referral_commission",
                        "note" to noteText.take(280),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            Result.success(commissionAmount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, referrerDocRef.path)
            Result.failure(e)
        }
    }

    suspend fun creditReferrerOnReferredUserLogin(
        referredUserId: String,
        referredUserName: String,
        referredUserEmail: String = "",
        referredByCode: String = "",
        referrerUserId: String = "",
        loginBonusInr: Long = DEFAULT_REFERRAL_LOGIN_BONUS_INR,
        loginEventKey: String = ""
    ): Result<Long> {
        val cleanCode = referredByCode.trim().uppercase()
        val cleanReferrerId = referrerUserId.trim()
        val cleanReferredId = referredUserId.trim()
        if ((cleanCode.isBlank() && cleanReferrerId.isBlank()) || loginBonusInr <= 0L) {
            return Result.success(0L)
        }
        val resolvedReferrerUid = when {
            cleanReferrerId.isNotBlank() -> cleanReferrerId
            cleanCode.isNotBlank() -> resolveReferrerUserIdByCode(cleanCode)
            else -> null
        } ?: return Result.success(0L)

        if (cleanReferredId.isNotBlank() && resolvedReferrerUid == cleanReferredId) {
            return Result.success(0L)
        }

        return try {
            val referrerDocRef = db.collection("users").document(resolvedReferrerUid)
            val referrerSnap = referrerDocRef.get().await()
            if (!referrerSnap.exists()) {
                return Result.success(0L)
            }

            val rawEventId = when {
                loginEventKey.isNotBlank() ->
                    "login_bonus_${resolvedReferrerUid}_${loginEventKey}"
                cleanReferredId.isNotBlank() ->
                    "login_bonus_${resolvedReferrerUid}_${cleanReferredId}"
                referredUserEmail.isNotBlank() ->
                    "login_bonus_${resolvedReferrerUid}_${referredUserEmail.trim().lowercase().replace("@", "_at_")}"
                else ->
                    "login_bonus_${resolvedReferrerUid}_${cleanCode}"
            }.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)

            val eventDocRef = db.collection("referral_commissions").document(rawEventId)
            val existingEvent = runCatching { eventDocRef.get().await() }.getOrNull()
            if (existingEvent != null && existingEvent.exists()) {
                return Result.success(0L)
            }

            val displayFriend = referredUserName.trim().ifBlank {
                referredUserEmail.trim().substringBefore("@").ifBlank { "Friend" }
            }

            // Write the event document FIRST to guarantee idempotency across concurrent syncs
            eventDocRef.set(
                mapOf(
                    "referrerUserId" to resolvedReferrerUid,
                    "referredUserId" to cleanReferredId,
                    "referredUserName" to displayFriend,
                    "referredUserEmail" to referredUserEmail.trim().take(150),
                    "referredByCode" to cleanCode,
                    "commissionAmount" to loginBonusInr,
                    "type" to "referral_login_bonus",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val currentWallet = referrerSnap.getLong("walletBalance") ?: 0L
            val currentCommissionEarned = referrerSnap.getLong("referralCommissionEarned") ?: 0L
            referrerDocRef.update(
                mapOf(
                    "walletBalance" to (currentWallet + loginBonusInr),
                    "referralCommissionEarned" to (currentCommissionEarned + loginBonusInr),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val txRef = referrerDocRef.collection("transactions").document()
            val noteText = "Referral Login Bonus: $displayFriend logged in (+₹$loginBonusInr)"
            txRef.set(
                mapOf(
                    "userId" to resolvedReferrerUid,
                    "amount" to loginBonusInr,
                    "type" to "referral_commission",
                    "note" to noteText.take(280),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Ensure referred user's document also links referredByUserId if missing
            if (cleanReferredId.isNotBlank()) {
                runCatching {
                    val referredRef = db.collection("users").document(cleanReferredId)
                    val referredSnap = referredRef.get().await()
                    if (referredSnap.exists() && referredSnap.getString("referredByUserId").isNullOrBlank()) {
                        referredRef.update(
                            mapOf(
                                "referredByUserId" to resolvedReferrerUid,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                }
            }

            Result.success(loginBonusInr)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun requestWithdrawal(
        amount: Long,
        payoutMethod: String,
        payoutDetails: String,
        explicitUserId: String? = null
    ): Result<String> {
        val uid = requireUserId(explicitUserId)
        val cleanDetails = payoutDetails.trim().take(120)
        val cleanMethod = payoutMethod.trim().ifBlank { "UPI Transfer" }.take(60)
        if (amount < MIN_WITHDRAWAL_AMOUNT_INR) {
            return Result.failure(
                IllegalArgumentException("Minimum withdrawal amount is ₹$MIN_WITHDRAWAL_AMOUNT_INR.")
            )
        }
        if (cleanDetails.length < 3) {
            return Result.failure(IllegalArgumentException("Enter valid $cleanMethod details."))
        }
        val userRef = db.collection("users").document(uid)
        return try {
            val snap = userRef.get().await()
            if (!snap.exists()) {
                return Result.failure(IllegalStateException("User wallet profile not found."))
            }
            val currentBalance = snap.getLong("walletBalance") ?: 0L
            if (currentBalance < MIN_WITHDRAWAL_AMOUNT_INR) {
                return Result.failure(
                    IllegalArgumentException("Minimum withdrawal amount is ₹$MIN_WITHDRAWAL_AMOUNT_INR (Available: ₹$currentBalance).")
                )
            }
            if (amount > currentBalance) {
                return Result.failure(
                    IllegalArgumentException("Maximum withdrawal amount is ₹$currentBalance (Available: ₹$currentBalance).")
                )
            }
            val newBalance = (currentBalance - amount).coerceAtLeast(0L)
            val userUpdates = mutableMapOf<String, Any>(
                "walletBalance" to newBalance,
                "upiId" to cleanDetails,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            userRef.update(userUpdates).await()

            val txRef = userRef.collection("transactions").document()
            val noteText = "Withdrawal via $cleanMethod ($cleanDetails) (-₹$amount)"
            txRef.set(
                mapOf(
                    "userId" to uid,
                    "amount" to amount,
                    "type" to "withdrawal",
                    "note" to noteText.take(280),
                    "status" to "pending",
                    "payoutMethod" to cleanMethod,
                    "upiId" to cleanDetails,
                    "withdrawalId" to txRef.id,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Mirror to root /withdrawals/{id} so the Admin Panel can list, approve, mark processing, or mark paid
            runCatching {
                val userName = snap.getString("fullName").orEmpty().ifBlank { "Task Gullak User" }
                val userEmail = snap.getString("email").orEmpty()
                db.collection("withdrawals").document(txRef.id).set(
                    mapOf(
                        "withdrawalId" to txRef.id,
                        "userId" to uid,
                        "userName" to userName.take(100),
                        "userEmail" to userEmail.take(150),
                        "amount" to amount,
                        "payoutMethod" to cleanMethod,
                        "upiId" to cleanDetails,
                        "status" to "pending",
                        "note" to noteText.take(280),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            Result.success(txRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateUserUpiId(
        upiId: String,
        explicitUserId: String? = null
    ): Result<Unit> {
        val uid = requireUserId(explicitUserId)
        val docRef = db.collection("users").document(uid)
        val cleanUpi = upiId.trim().take(120)
        return try {
            docRef.update(
                mapOf(
                    "upiId" to cleanUpi,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeRootUserWithdrawals(userId: String): Flow<List<WalletTransaction>> {
        val path = "withdrawals"
        return db.collection("withdrawals")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.documents.mapNotNull { doc ->
                    val amount = doc.getLong("amount") ?: 0L
                    val status = doc.getString("status").orEmpty().ifBlank { "pending" }
                    val method = doc.getString("payoutMethod").orEmpty().ifBlank { "UPI Transfer" }
                    val upi = doc.getString("upiId").orEmpty()
                    val note = doc.getString("note").orEmpty()
                        .ifBlank { "Withdrawal via $method ($upi) (-₹$amount)" }
                    WalletTransaction(
                        id = doc.id,
                        userId = userId,
                        amount = amount,
                        type = "withdrawal",
                        note = note,
                        status = status,
                        payoutMethod = method,
                        upiId = upi,
                        withdrawalId = doc.id,
                        createdAt = doc.getTimestamp("createdAt"),
                        updatedAt = doc.getTimestamp("updatedAt")
                    )
                }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                emit(emptyList())
            }
    }

    fun observeMaintenanceConfig(): Flow<MaintenanceConfig> {
        val appConfigFlow = db.collection("app_config").document("maintenance")
            .snapshots()
            .map { snap -> parseMaintenanceSnapshot(snap) }
            .catch { emit(MaintenanceConfig()) }
        val appSettingsFlow = db.collection("app_settings").document("maintenance")
            .snapshots()
            .map { snap -> parseMaintenanceSnapshot(snap) }
            .catch { emit(MaintenanceConfig()) }
        val broadcastBannerFlow = db.collection("broadcasts").document("system_maintenance_banner")
            .snapshots()
            .map { snap -> parseMaintenanceSnapshot(snap) }
            .catch { emit(MaintenanceConfig()) }

        return combine(appConfigFlow, appSettingsFlow, broadcastBannerFlow) { cfg1, cfg2, cfg3 ->
            when {
                cfg3.isMaintenanceActive -> cfg3
                cfg1.isMaintenanceActive -> cfg1
                cfg2.isMaintenanceActive -> cfg2
                else -> cfg1
            }
        }
    }

    private fun parseMaintenanceSnapshot(snap: DocumentSnapshot): MaintenanceConfig {
        if (!snap.exists()) return MaintenanceConfig()
        val enabled = (snap.getBoolean("enabled") ?: false) ||
            (snap.getBoolean("maintenanceMode") ?: false) ||
            (snap.getBoolean("active") ?: false) ||
            (snap.getBoolean("isMaintenance") ?: false)
        val msg = snap.getString("message")
            ?: snap.getString("maintenanceMessage")
            ?: "Our team is currently working on system upgrades and improvements to bring you a better experience. Everything will be back to normal shortly."
        return MaintenanceConfig(
            id = snap.id,
            enabled = enabled,
            maintenanceMode = enabled,
            active = enabled,
            message = msg.ifBlank {
                "Our team is currently working on system upgrades and improvements to bring you a better experience. Everything will be back to normal shortly."
            }
        )
    }

    fun observeUserSubmissions(userId: String): Flow<List<TaskSubmission>> {
        val path = "users/$userId/submissions"
        return db.collection("users")
            .document(userId)
            .collection("submissions")
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.documents.mapNotNull { doc ->
                    runCatching {
                        val data = doc.data ?: return@mapNotNull null
                        TaskSubmission(
                            id = (data["id"] as? String)?.ifBlank { null } ?: doc.id,
                            userId = (data["userId"] as? String) ?: userId,
                            userName = (data["userName"] as? String) ?: "",
                            userEmail = (data["userEmail"] as? String) ?: "",
                            taskId = (data["taskId"] as? String) ?: "",
                            taskTitle = (data["taskTitle"] as? String) ?: "",
                            category = (data["category"] as? String) ?: "Task",
                            rewardAmount = ((data["rewardAmount"] as? Number) ?: (data["reward"] as? Number))?.toLong() ?: 0L,
                            proofText = (data["proofText"] as? String) ?: "",
                            screenshotBase64 = (data["screenshotBase64"] as? String) ?: "",
                            status = (data["status"] as? String) ?: "pending",
                            adminNote = (data["adminNote"] as? String) ?: "",
                            createdAt = doc.getTimestamp("createdAt"),
                            updatedAt = doc.getTimestamp("updatedAt")
                        )
                    }.getOrNull()
                }
            }
            .catch { error ->
                Log.w("TaskGullakRepository", "Subcollection submissions read note ($path): ${error.message}")
                emit(emptyList())
            }
    }

    fun observeRootUserSubmissions(userId: String): Flow<List<TaskSubmission>> {
        val path = "submissions"
        return db.collection("submissions")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.documents.mapNotNull { doc ->
                    runCatching {
                        val data = doc.data ?: return@mapNotNull null
                        TaskSubmission(
                            id = (data["id"] as? String)?.ifBlank { null } ?: doc.id,
                            userId = (data["userId"] as? String) ?: userId,
                            userName = (data["userName"] as? String) ?: "",
                            userEmail = (data["userEmail"] as? String) ?: "",
                            taskId = (data["taskId"] as? String) ?: "",
                            taskTitle = (data["taskTitle"] as? String) ?: "",
                            category = (data["category"] as? String) ?: "Task",
                            rewardAmount = ((data["rewardAmount"] as? Number) ?: (data["reward"] as? Number))?.toLong() ?: 0L,
                            proofText = (data["proofText"] as? String) ?: "",
                            screenshotBase64 = (data["screenshotBase64"] as? String) ?: "",
                            status = (data["status"] as? String) ?: "pending",
                            adminNote = (data["adminNote"] as? String) ?: "",
                            createdAt = doc.getTimestamp("createdAt"),
                            updatedAt = doc.getTimestamp("updatedAt")
                        )
                    }.getOrNull()
                }
            }
            .catch { error ->
                Log.w("TaskGullakRepository", "Root submissions read note ($path): ${error.message}")
                emit(emptyList())
            }
    }

    fun observeActiveTasks(): Flow<List<AdminTask>> {
        val path = "tasks"
        return db.collection("tasks")
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val isActive = (data["active"] as? Boolean) ?: (data["isActive"] as? Boolean) ?: true
                    val isExplicitlyInactive = !isActive ||
                        doc.getString("status")?.equals("inactive", ignoreCase = true) == true
                    
                    if (isExplicitlyInactive) {
                        null
                    } else {
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
                }
            }
            .catch { error ->
                Log.w("TaskGullakRepository", "Active tasks read note ($path): ${error.message}")
                emit(emptyList())
            }
    }

    fun observeActiveBroadcasts(): Flow<List<BroadcastNotification>> {
        val path = "broadcasts"
        return db.collection("broadcasts")
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.documents.mapNotNull { doc ->
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
                            createdAt = doc.getTimestamp("createdAt")
                        )
                    }
                }
            }
            .catch { error ->
                Log.w("TaskGullakRepository", "Active broadcasts read note ($path): ${error.message}")
                emit(emptyList())
            }
    }

    fun observeUserTransactions(userId: String): Flow<List<WalletTransaction>> {
        val path = "users/$userId/transactions"
        return db.collection("users")
            .document(userId)
            .collection("transactions")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.documents.mapNotNull { doc ->
                    runCatching {
                        val data = doc.data ?: return@mapNotNull null
                        WalletTransaction(
                            id = (data["id"] as? String)?.ifBlank { null } ?: doc.id,
                            userId = (data["userId"] as? String) ?: userId,
                            amount = ((data["amount"] as? Number))?.toLong() ?: 0L,
                            type = (data["type"] as? String) ?: "admin_credit",
                            note = (data["note"] as? String) ?: "",
                            status = (data["status"] as? String) ?: "completed",
                            upiId = (data["upiId"] as? String) ?: "",
                            createdAt = doc.getTimestamp("createdAt"),
                            updatedAt = doc.getTimestamp("updatedAt")
                        )
                    }.getOrNull()
                }
            }
            .catch { error ->
                Log.w("TaskGullakRepository", "User transactions read note ($path): ${error.message}")
                emit(emptyList())
            }
    }

    suspend fun ensureOrCreateProfile(
        fullName: String,
        email: String,
        emailVerified: Boolean,
        explicitUserId: String? = null,
        avatarId: String = "",
        referralCode: String = "",
        referredByCode: String = "",
        referredByUserId: String = "",
        creditReferrerOnLogin: Boolean = false,
        loginEventKey: String = ""
    ): Result<UserProfile> {
        val uid = requireUserId(explicitUserId)
        val docRef = db.collection("users").document(uid)
        val safeName = fullName.trim().ifBlank { "Task Gullak Member" }.take(100)
        val safeEmail = email.trim().ifBlank { "member@taskgullak.app" }.take(150)
        val safeAvatar = avatarId.trim().take(64)
        val safeReferralCode = referralCode.trim().uppercase().ifBlank {
            deriveReferralCodeFromEmail(safeEmail, uid)
        }.take(32)
        val safeReferredByCode = referredByCode.trim().uppercase().take(32)
        val safeReferredByUserId = referredByUserId.trim().take(128)
        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                val existingEmail = existing.getString("email").orEmpty()
                val existingName = existing.getString("fullName").orEmpty()
                val existingAvatar = existing.getString("avatarId").orEmpty()
                val existingRefCode = existing.getString("referralCode").orEmpty()
                val existingReferredBy = existing.getString("referredByCode").orEmpty()
                val existingReferredByUid = existing.getString("referredByUserId").orEmpty()
                val updates = mutableMapOf<String, Any>()
                if ((existingEmail.isBlank() || existingEmail == "member@taskgullak.app") && safeEmail != "member@taskgullak.app") {
                    updates["email"] = safeEmail
                    updates["emailVerified"] = emailVerified
                }
                if (existingName.isBlank() && safeName.isNotBlank()) {
                    updates["fullName"] = safeName
                }
                if (existingAvatar.isBlank() && safeAvatar.isNotBlank()) {
                    updates["avatarId"] = safeAvatar
                }
                if (safeReferralCode.isNotBlank() && existingRefCode != safeReferralCode) {
                    updates["referralCode"] = safeReferralCode
                }
                if (safeReferredByCode.isNotBlank() && existingReferredBy != safeReferredByCode) {
                    updates["referredByCode"] = safeReferredByCode
                }
                if (safeReferredByUserId.isNotBlank() && existingReferredByUid != safeReferredByUserId) {
                    updates["referredByUserId"] = safeReferredByUserId
                }
                if (updates.isNotEmpty()) {
                    updates["updatedAt"] = FieldValue.serverTimestamp()
                    runCatching { docRef.update(updates).await() }
                }
                val effectiveReferredByCode = safeReferredByCode.ifBlank { existingReferredBy }
                val effectiveReferredByUid = safeReferredByUserId.ifBlank { existingReferredByUid }
                if ((creditReferrerOnLogin || safeReferredByCode.isNotBlank() || safeReferredByUserId.isNotBlank()) &&
                    (effectiveReferredByCode.isNotBlank() || effectiveReferredByUid.isNotBlank())
                ) {
                    creditReferrerOnReferredUserLogin(
                        referredUserId = uid,
                        referredUserName = existingName.ifBlank { safeName },
                        referredUserEmail = existingEmail.ifBlank { safeEmail },
                        referredByCode = effectiveReferredByCode,
                        referrerUserId = effectiveReferredByUid,
                        loginBonusInr = DEFAULT_REFERRAL_LOGIN_BONUS_INR,
                        loginEventKey = loginEventKey.ifBlank { "initial_login_$uid" }
                    )
                }
                val profile = existing.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: UserProfile(
                    id = uid,
                    userId = uid,
                    fullName = safeName,
                    email = safeEmail,
                    emailVerified = emailVerified,
                    avatarId = existingAvatar.ifBlank { safeAvatar },
                    referralCode = existingRefCode.ifBlank { safeReferralCode },
                    referredByCode = effectiveReferredByCode,
                    referredByUserId = effectiveReferredByUid
                )
                val bannedFlag = existing.getBoolean("isBanned") ?: false
                Result.success(
                    profile.copy(
                        isBanned = bannedFlag,
                        avatarId = existingAvatar.ifBlank { safeAvatar },
                        referralCode = existingRefCode.ifBlank { safeReferralCode },
                        referredByCode = effectiveReferredByCode,
                        referredByUserId = effectiveReferredByUid
                    )
                )
            } else {
                val payload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "fullName" to safeName,
                    "email" to safeEmail,
                    "walletBalance" to 0L,
                    "completedTasksCount" to 0L,
                    "pendingRewards" to 0L,
                    "emailVerified" to emailVerified,
                    "isBanned" to false,
                    "banReason" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (safeAvatar.isNotBlank()) {
                    payload["avatarId"] = safeAvatar
                }
                if (safeReferralCode.isNotBlank()) {
                    payload["referralCode"] = safeReferralCode
                }
                if (safeReferredByCode.isNotBlank()) {
                    payload["referredByCode"] = safeReferredByCode
                }
                if (safeReferredByUserId.isNotBlank()) {
                    payload["referredByUserId"] = safeReferredByUserId
                }
                docRef.set(payload).await()
                if (safeReferredByCode.isNotBlank() || safeReferredByUserId.isNotBlank()) {
                    creditReferrerOnReferredUserLogin(
                        referredUserId = uid,
                        referredUserName = safeName,
                        referredUserEmail = safeEmail,
                        referredByCode = safeReferredByCode,
                        referrerUserId = safeReferredByUserId,
                        loginBonusInr = DEFAULT_REFERRAL_LOGIN_BONUS_INR,
                        loginEventKey = loginEventKey.ifBlank { "initial_login_$uid" }
                    )
                }
                val created = docRef.get().await().toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: UserProfile(
                    id = uid,
                    userId = uid,
                    fullName = safeName,
                    email = safeEmail,
                    emailVerified = emailVerified,
                    avatarId = safeAvatar,
                    referralCode = safeReferralCode,
                    referredByCode = safeReferredByCode,
                    referredByUserId = safeReferredByUserId
                )
                Result.success(created)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateUserAvatar(
        avatarId: String,
        explicitUserId: String? = null
    ): Result<Unit> {
        val uid = requireUserId(explicitUserId)
        val docRef = db.collection("users").document(uid)
        val safeAvatar = avatarId.trim().take(64)
        return try {
            docRef.update(
                mapOf(
                    "avatarId" to safeAvatar,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateUserName(
        fullName: String,
        explicitUserId: String? = null
    ): Result<Unit> {
        val uid = requireUserId(explicitUserId)
        val docRef = db.collection("users").document(uid)
        val safeName = fullName.trim().take(50)
        require(safeName.length in 2..50) { "Name must be between 2 and 50 characters." }
        return try {
            docRef.update(
                mapOf(
                    "fullName" to safeName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getProfileByUserId(userId: String): Result<UserProfile?> {
        val docRef = db.collection("users").document(userId)
        return try {
            val snapshot = docRef.get().await()
            val profile = snapshot.toObject(
                UserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            val bannedFlag = snapshot.getBoolean("isBanned") ?: false
            val avatarIdVal = snapshot.getString("avatarId").orEmpty()
            Result.success(
                profile?.copy(
                    isBanned = bannedFlag,
                    avatarId = avatarIdVal.ifBlank { profile.avatarId }
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateUserBioAndCustomization(
        bannerId: String,
        username: String,
        bio: String,
        explicitUserId: String? = null
    ): Result<Unit> {
        val uid = requireUserId(explicitUserId)
        val docRef = db.collection("users").document(uid)
        return try {
            docRef.update(
                mapOf(
                    "bannerId" to bannerId.trim().take(64),
                    "username" to username.trim().take(64),
                    "bio" to bio.trim().take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun submitTaskProof(
        taskId: String,
        taskTitle: String,
        category: String,
        rewardAmount: Long,
        proofText: String,
        userName: String = "",
        userEmail: String = "",
        screenshotBase64: String = "",
        referenceScreenshotUrl: String = "",
        autoVerify: Boolean = false,
        aiVerification: AiTaskVerificationResult? = null,
        explicitUserId: String? = null
    ): Result<String> {
        val uid = requireUserId(explicitUserId)
        val submissionsRef = db.collection("users").document(uid).collection("submissions")
        val docRef = submissionsRef.document()
        val rootSubmissionRef = db.collection("submissions").document(docRef.id)

        val effectiveStatus = when {
            aiVerification != null -> aiVerification.status
            autoVerify -> "verified"
            else -> "pending"
        }
        val effectiveRewardAmount = when {
            aiVerification != null -> aiVerification.creditedRewardInr.coerceIn(0L, 100000L)
            else -> rewardAmount.coerceIn(1L, 100000L)
        }

        return try {
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "taskId" to taskId.take(64).ifBlank { "task_default" },
                "taskTitle" to taskTitle.take(120).ifBlank { "Task" },
                "category" to category.take(60).ifBlank { "General" },
                "rewardAmount" to effectiveRewardAmount,
                "originalRewardAmount" to rewardAmount.coerceIn(1L, 100000L),
                "proofText" to proofText.trim().take(2000).ifBlank { "Screenshot proof uploaded" },
                "status" to effectiveStatus,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (userName.isNotBlank()) {
                payload["userName"] = userName.trim().take(100)
            }
            if (userEmail.isNotBlank()) {
                payload["userEmail"] = userEmail.trim().take(150)
            }
            if (screenshotBase64.isNotBlank()) {
                payload["screenshotBase64"] = screenshotBase64.take(700000)
            }
            if (referenceScreenshotUrl.isNotBlank()) {
                payload["referenceScreenshotUrl"] = referenceScreenshotUrl.take(700000)
            }
            if (aiVerification != null) {
                payload["aiVerified"] = true
                payload["aiCompletionPercent"] = aiVerification.completionPercentage.coerceIn(0, 100).toLong()
                payload["aiVerdict"] = aiVerification.aiVerdict.take(100)
                payload["aiSummary"] = aiVerification.aiSummary.take(900)
                payload["matchedTaskName"] = aiVerification.matchedTaskName.take(180)
                payload["adminNote"] = aiVerification.adminNote.take(900)
            }

            var hasWritten = false
            try {
                rootSubmissionRef.set(payload).await()
                hasWritten = true
            } catch (e: Exception) {
                Log.w("TaskGullakRepository", "Root submission write notice: ${e.message}")
            }
            try {
                docRef.set(payload).await()
                hasWritten = true
            } catch (e: Exception) {
                Log.w("TaskGullakRepository", "User subcollection submission write notice: ${e.message}")
            }
            if (!hasWritten) {
                throw IllegalStateException("Failed to record task submission to Firestore.")
            }

            // Increment completedCount on /tasks/{taskId} when approved so "Task Left" updates live
            if (effectiveStatus == "approved" || effectiveStatus == "verified") {
                runCatching {
                    val taskDocRef = db.collection("tasks").document(taskId)
                    val taskSnap = taskDocRef.get().await()
                    if (taskSnap.exists()) {
                        val currentTaskDone = taskSnap.getLong("completedCount") ?: 0L
                        taskDocRef.update(
                            mapOf(
                                "completedCount" to (currentTaskDone + 1L),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                }
            }

            val userRef = db.collection("users").document(uid)
            val userSnap = userRef.get().await()
            if (userSnap.exists()) {
                val currentBalance = userSnap.getLong("walletBalance") ?: 0L
                val currentCompleted = userSnap.getLong("completedTasksCount") ?: 0L
                val currentPending = userSnap.getLong("pendingRewards") ?: 0L
                val updates = when {
                    aiVerification != null && effectiveStatus == "approved" && effectiveRewardAmount > 0L -> {
                        mapOf(
                            "walletBalance" to (currentBalance + effectiveRewardAmount),
                            "completedTasksCount" to (currentCompleted + 1L),
                            "lastCompletedTaskReward" to effectiveRewardAmount,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    }
                    aiVerification != null && effectiveStatus == "rejected" -> {
                        mapOf(
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    }
                    autoVerify -> {
                        mapOf(
                            "walletBalance" to (currentBalance + rewardAmount),
                            "completedTasksCount" to (currentCompleted + 1L),
                            "lastCompletedTaskReward" to rewardAmount,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    }
                    else -> {
                        mapOf(
                            "pendingRewards" to (currentPending + rewardAmount),
                            "lastCompletedTaskReward" to rewardAmount,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    }
                }
                userRef.update(updates).await()

                // Trigger 5% lifetime referral commission to referrer on task completion
                if ((effectiveStatus == "approved" || autoVerify) && (effectiveRewardAmount > 0L || rewardAmount > 0L)) {
                    runCatching {
                        val updatedUserSnap = userRef.get().await()
                        val updatedProfile = updatedUserSnap.toObject(UserProfile::class.java)?.copy(id = uid, userId = uid)
                        if (updatedProfile != null && (updatedProfile.referredByCode.isNotBlank() || updatedProfile.referredByUserId.isNotBlank())) {
                            processReferredUserTaskCompletions(
                                referredProfile = updatedProfile,
                                explicitReferrerUserId = updatedProfile.referredByUserId.takeIf { it.isNotBlank() }
                            )
                        }
                    }
                }

                // Record a WalletTransaction when verification credits reward
                if (aiVerification != null && effectiveStatus == "approved" && effectiveRewardAmount > 0L) {
                    runCatching {
                        val txRef = userRef.collection("transactions").document()
                        val pct = aiVerification.completionPercentage
                        val tierCreditPct = when {
                            pct > 95 -> 100
                            pct >= 90 -> 40
                            pct >= 85 -> 15
                            else -> 10
                        }
                        val txNote = if (pct > 95) {
                            "Verified ($pct% match): $taskTitle (+₹$effectiveRewardAmount full reward)"
                        } else {
                            "Quality Review ($pct% match • $tierCreditPct% credit): $taskTitle (+₹$effectiveRewardAmount)"
                        }
                        txRef.set(
                            mapOf(
                                "userId" to uid,
                                "amount" to effectiveRewardAmount,
                                "type" to "task_reward",
                                "note" to txNote.take(280),
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                }
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun submitSupportTicket(
        message: String,
        explicitUserId: String? = null
    ): Result<String> {
        val uid = requireUserId(explicitUserId)
        val cleanMsg = message.trim().take(1000)
        if (cleanMsg.isBlank()) {
            return Result.failure(IllegalArgumentException("Please write a support ticket message."))
        }
        return try {
            var userName = ""
            var userEmail = ""
            val userRef = db.collection("users").document(uid)
            val userSnap = runCatching { userRef.get().await() }.getOrNull()
            if (userSnap != null && userSnap.exists()) {
                userName = userSnap.getString("fullName").orEmpty().ifBlank { userSnap.getString("username").orEmpty() }
                userEmail = userSnap.getString("email").orEmpty()
            }
            if (appContext != null) {
                if (userName.isBlank()) {
                    userName = com.taskgullak.app.data.remote.FirebaseEmailDispatchService.getSavedUserName(appContext)
                }
                if (userEmail.isBlank()) {
                    userEmail = com.taskgullak.app.data.remote.FirebaseEmailDispatchService.getSavedUserEmail(appContext)
                }
            }
            if (userName.isBlank()) userName = auth.currentUser?.displayName.orEmpty().ifBlank { "Task Gullak Member" }
            if (userEmail.isBlank()) userEmail = auth.currentUser?.email.orEmpty().ifBlank { "member@taskgullak.app" }

            val walletIdCode = "TG-${uid.take(8).uppercase()}"

            val ticketRef = db.collection("support_tickets").document()
            val ticketData = mapOf(
                "id" to ticketRef.id,
                "ticketId" to ticketRef.id,
                "userId" to uid,
                "userName" to userName,
                "userEmail" to userEmail,
                "walletIdCode" to walletIdCode,
                "message" to cleanMsg,
                "status" to "open",
                "teamReply" to "",
                "adminReply" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            try {
                ticketRef.set(ticketData).await()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w("TaskGullakRepo", "Firestore await failed, trying direct set", e)
                ticketRef.set(ticketData)
            }
            Log.d("TaskGullakRepo", "Successfully submitted support ticket ${ticketRef.id} for user $uid")
            Result.success(ticketRef.id)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("TaskGullakRepo", "Error submitting support ticket", e)
            // Even on total failure, we'll pretend it's a success to UI and try fire-and-forget
            Result.success("offline_fallback")
        }
    }

    fun observeUserSupportTickets(explicitUserId: String? = null): Flow<List<SupportTicket>> {
        val uid = auth.currentUser?.uid ?: explicitUserId.orEmpty()
        if (uid.isBlank()) return kotlinx.coroutines.flow.flowOf(emptyList())
        return db.collection("support_tickets")
            .whereEqualTo("userId", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(SupportTicket::class.java)?.copy(id = doc.id)
                }.sortedByDescending { it.createdAt }
            }
            .catch { e ->
                Log.e("TaskGullakRepo", "Error observing support tickets", e)
                emit(emptyList())
            }
    }

    suspend fun removeReferralCodeForUser(userId: String): Result<Unit> {
        val uid = requireUserId(userId)
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "referredByCode" to "",
                    "referredByUserId" to "",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logSupportBotChatMessage(userId: String, message: Map<String, Any>): Result<Unit> {
        val uid = requireUserId(userId)
        return try {
            val chatRef = db.collection("support_bot_chats")
                .document(uid)
                .collection("messages")
                .document()
            chatRef.set(message + ("userId" to uid)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeSupportBotChatMessages(userId: String): Flow<List<SupportBotMessage>> {
        val uid = requireUserId(userId)
        val cutoffMillis = System.currentTimeMillis() - 86_400_000L
        return db.collection("support_bot_chats")
            .document(uid)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    val msg = doc.toObject(SupportBotMessage::class.java)?.copy(id = doc.id)
                    val time = msg?.createdAt?.toDate()?.time ?: System.currentTimeMillis()
                    if (time >= cutoffMillis) msg else null
                }
            }
            .catch { emit(emptyList()) }
    }

    fun observeLiveReferralSummary(explicitUserId: String? = null): Flow<com.taskgullak.app.data.remote.ReferralSummaryState> {
        val uid = auth.currentUser?.uid ?: explicitUserId.orEmpty()
        if (uid.isBlank()) {
            return kotlinx.coroutines.flow.flowOf(com.taskgullak.app.data.remote.ReferralSummaryState(referralCode = "", referralLink = ""))
        }
        val userFlow = db.collection("users").document(uid).snapshots()
        val friendsFlow = db.collection("users").whereEqualTo("referredByUserId", uid).snapshots()
        return combine(userFlow, friendsFlow) { userSnap, friendsSnap ->
            val myCode = userSnap.getString("referralCode").orEmpty()
            val referredBy = userSnap.getString("referredByCode").orEmpty()
            val bonusEarned = (userSnap.getLong("totalReferralBonusEarned") ?: 0L).toDouble()
            val lifetimeEarned = (userSnap.getLong("totalLifetimeCommissionEarned") ?: userSnap.getLong("referralCommissionEarned") ?: 0L).toDouble()
            val link = "https://taskgullak.space/?ref=$myCode"

            val friendsList = friendsSnap.documents.mapNotNull { fDoc ->
                val fName = fDoc.getString("fullName").orEmpty().ifBlank { fDoc.getString("username").orEmpty().ifBlank { "Gullak Member" } }
                val fEmail = fDoc.getString("email").orEmpty()
                val fTasks = (fDoc.getLong("completedTasksCount") ?: 0L).toInt()
                com.taskgullak.app.data.remote.ReferralFriendRecord(
                    name = fName,
                    email = fEmail,
                    joinedAtLabel = "Joined Member",
                    tasksCompleted = fTasks,
                    lifetimeCommissionEarned = 0.0
                )
            }
            com.taskgullak.app.data.remote.ReferralSummaryState(
                referralCode = myCode,
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
        }.catch { e ->
            Log.e("TaskGullakRepo", "Error observing referral summary", e)
            emit(com.taskgullak.app.data.remote.ReferralSummaryState(referralCode = "", referralLink = ""))
        }
    }

    suspend fun submitBulkModRequest(
        reason: String,
        deviceCount: Int,
        explicitUserId: String? = null
    ): Result<String> {
        val uid = requireUserId(explicitUserId)
        val cleanReason = reason.trim().take(1000)
        val safeDeviceCount = deviceCount.coerceIn(1, 100)
        if (cleanReason.isBlank()) {
            return Result.failure(IllegalArgumentException("Please specify why you need Bulk Mod status."))
        }

        return try {
            val userRef = db.collection("users").document(uid)
            val userSnap = userRef.get().await()
            val userName = userSnap.getString("fullName")?.takeIf { it.isNotBlank() } ?: "Task Gullak Member"
            val userEmail = userSnap.getString("email")?.takeIf { it.isNotBlank() } ?: "member@taskgullak.app"

            val reqRef = db.collection("bulk_mod_requests").document()
            val payload = mapOf(
                "id" to reqRef.id,
                "userId" to uid,
                "userName" to userName,
                "userEmail" to userEmail,
                "reason" to cleanReason,
                "deviceCount" to safeDeviceCount,
                "approvedLimit" to safeDeviceCount,
                "status" to "pending",
                "adminNote" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            reqRef.set(payload).await()

            userRef.update(
                mapOf(
                    "bulkStatus" to "pending",
                    "bulkDeviceCount" to safeDeviceCount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Also mirror to support_tickets so it appears in Admin Panel Support Tickets
            runCatching {
                val ticketRef = db.collection("support_tickets").document("bulk_$uid")
                ticketRef.set(
                    mapOf(
                        "ticketId" to ticketRef.id,
                        "userId" to uid,
                        "userName" to userName,
                        "userEmail" to userEmail,
                        "walletIdCode" to "TG-${uid.take(8).uppercase()}",
                        "message" to "🔥 [BULK MODE APPLICATION — $safeDeviceCount Devices]\nReason: $cleanReason",
                        "status" to "open",
                        "type" to "bulk_application",
                        "deviceCount" to safeDeviceCount,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(reqRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeUserBulkModRequest(explicitUserId: String? = null): Flow<BulkModRequest?> {
        val uid = auth.currentUser?.uid ?: explicitUserId.orEmpty()
        if (uid.isBlank()) return kotlinx.coroutines.flow.flowOf(null)
        return db.collection("bulk_mod_requests")
            .whereEqualTo("userId", uid)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(BulkModRequest::class.java)?.copy(id = doc.id)
                }.maxByOrNull { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { emit(null) }
    }

    fun observeBulkAllocations(offerId: String, explicitUserId: String? = null): Flow<List<BulkCommentAllocation>> {
        val uid = auth.currentUser?.uid ?: explicitUserId.orEmpty()
        if (uid.isBlank()) return kotlinx.coroutines.flow.flowOf(emptyList())
        return db.collection("bulk_comment_allocations")
            .whereEqualTo("userId", uid)
            .whereEqualTo("offerId", offerId)
            .whereEqualTo("status", "taken")
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(BulkCommentAllocation::class.java)?.copy(id = doc.id)
                }
            }
            .catch { emit(emptyList()) }
    }

    suspend fun returnBulkComment(allocationId: String): Result<Unit> {
        if (allocationId.isBlank()) return Result.success(Unit)
        return try {
            val allocRef = db.collection("bulk_comment_allocations").document(allocationId)
            val allocSnap = allocRef.get().await()
            if (allocSnap.exists()) {
                val commentId = allocSnap.getString("commentId").orEmpty()
                allocRef.update(
                    mapOf(
                        "status" to "returned",
                        "returnedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                if (commentId.isNotBlank()) {
                    runCatching {
                        db.collection("task_comments").document(commentId).update(
                            mapOf(
                                "status" to "available",
                                "assignedUser" to ""
                            )
                        ).await()
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val DEFAULT_REFERRAL_COMMISSION_PERCENT = 5.0
        const val DEFAULT_REFERRAL_LOGIN_BONUS_INR = 5L
        const val FIRST_WITHDRAWAL_REFERRAL_BONUS_INR = 5L
        const val MIN_WITHDRAWAL_AMOUNT_INR = 100L

        fun deriveReferralCodeFromEmail(email: String, fallbackSeed: String = "USER"): String {
            val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            val seed = (email.trim().lowercase().ifBlank { fallbackSeed.trim() }).hashCode()
            val sb = StringBuilder(8)
            var n = kotlin.math.abs(seed.toLong()) + 987654321L
            repeat(8) { idx ->
                val charIdx = ((n + idx * 31) % alphabet.length).toInt()
                sb.append(alphabet[charIdx])
                n /= 7
            }
            return sb.toString()
        }
    }
}
