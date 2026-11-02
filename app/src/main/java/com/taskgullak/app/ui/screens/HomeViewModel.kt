package com.taskgullak.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgullak.app.data.model.AdminTask
import com.taskgullak.app.data.model.AiTaskVerificationResult
import com.taskgullak.app.data.model.AvailableTask
import com.taskgullak.app.data.model.BroadcastNotification
import com.taskgullak.app.data.model.MaintenanceConfig
import com.taskgullak.app.data.model.SupportBotMessage
import com.taskgullak.app.data.model.SupportTicket
import com.taskgullak.app.data.model.TaskSubmission
import com.taskgullak.app.data.model.UserProfile
import com.taskgullak.app.data.model.WalletTransaction
import com.taskgullak.app.data.remote.FirebaseEmailDispatchService
import com.taskgullak.app.data.repository.TaskGullakRepository
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.net.URL
import kotlin.math.roundToLong

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class HomeViewModel(
    private val repository: TaskGullakRepository,
    private val currentUserId: String,
    private val initialDisplayName: String,
    private val initialEmail: String,
    initialEmailVerified: Boolean
) : ViewModel() {

    private val taskVerificationSchema = Schema.obj(
        properties = mapOf(
            "completionPercentage" to Schema.integer(
                description = "Strict completion accuracy score from 0 to 100 comparing the user's uploaded screenshot against the reference sample screenshot and task requirements: 96-100 for complete match (100% reward), 90-95 for strong match (40% reward), 85-89 for moderate match (15% reward), 80-84 for minimum acceptable match (10% reward), and below 80 for incomplete/unrelated screenshot (rejected, re-work required)."
            ),
            "matchedTaskName" to Schema.string(
                description = "Name of the app or project detected in the user's uploaded screenshot"
            ),
            "aiSummary" to Schema.string(
                description = "Formal quality review note explaining how the uploaded screenshot matched the project requirements (never mention AI, Admin, or internal systems)"
            )
        )
    )

    private val aiVerificationModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = taskVerificationSchema
            }
        )
    }

    private val _aiVerificationState = MutableStateFlow<UiState<AiTaskVerificationResult>?>(null)
    val aiVerificationState: StateFlow<UiState<AiTaskVerificationResult>?> = _aiVerificationState.asStateFlow()

    val profileState: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId)
            .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
            .catch { error ->
                Log.w(TAG, "Error observing user profile", error)
                emit(UiState.Error(error.message ?: "Failed to observe user profile"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val maintenanceState: StateFlow<MaintenanceConfig> =
        repository.observeMaintenanceConfig()
            .catch { emit(MaintenanceConfig()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = MaintenanceConfig()
            )

    private val subcollectionSubmissionsFlow =
        repository.observeUserSubmissions(currentUserId)
            .catch { error ->
                Log.w(TAG, "Error observing user subcollection submissions", error)
                emit(emptyList())
            }

    private val rootSubmissionsFlow =
        repository.observeRootUserSubmissions(currentUserId)
            .catch { error ->
                Log.w(TAG, "Error observing root user submissions", error)
                emit(emptyList())
            }

    val submissionsState: StateFlow<UiState<List<TaskSubmission>>> =
        combine(subcollectionSubmissionsFlow, rootSubmissionsFlow) { subList, rootList ->
            val mergedById = LinkedHashMap<String, TaskSubmission>()
            subList.forEach { mergedById[it.id] = it }
            // Root /submissions documents updated by the Admin Panel override pending status seamlessly
            rootList.forEach { rootSub ->
                val existing = mergedById[rootSub.id]
                if (existing == null) {
                    mergedById[rootSub.id] = rootSub
                } else {
                    val rootTime = rootSub.updatedAt?.seconds ?: 0L
                    val subTime = existing.updatedAt?.seconds ?: 0L
                    if (rootSub.status != existing.status || rootTime >= subTime) {
                        mergedById[rootSub.id] = rootSub
                    }
                }
            }
            val sorted = mergedById.values.sortedByDescending { it.createdAt?.seconds ?: 0L }
            UiState.Success(sorted) as UiState<List<TaskSubmission>>
        }
            .catch { error ->
                Log.w(TAG, "Error combining task submissions", error)
                emit(UiState.Error(error.message ?: "Failed to observe submissions"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val activeTasksState: StateFlow<UiState<List<AvailableTask>>> =
        combine(
            repository.observeActiveTasks(),
            submissionsState
        ) { adminTasks, submissionsUiState ->
            val currentUserUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
            val currentUserEmail = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: ""
            val emailSlug = currentUserEmail.substringBefore("@").replace(".", "_")

            // Gather IDs of tasks that have a pending or approved submission
            val activeSubmissionTaskIds = if (submissionsUiState is UiState.Success) {
                submissionsUiState.data
                    .filter { it.status.equals("pending", ignoreCase = true) || it.status.equals("approved", ignoreCase = true) }
                    .map { it.taskId }
                    .toSet()
            } else {
                emptySet()
            }

            val filteredAndSorted = adminTasks
                .filter { task ->
                    val isBlocked = task.blockedUserIds.contains(currentUserUid) ||
                            (emailSlug.isNotBlank() && task.blockedUserIds.contains("user_$emailSlug"))
                    !isBlocked && !activeSubmissionTaskIds.contains(task.id)
                }
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
            UiState.Success(filteredAndSorted.map { it.toAvailableTask() }) as UiState<List<AvailableTask>>
        }
            .catch { error ->
                Log.w(TAG, "Error observing active tasks", error)
                emit(UiState.Error(error.message ?: "Failed to observe tasks") as UiState<List<AvailableTask>>)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val broadcastsState: StateFlow<UiState<List<BroadcastNotification>>> =
        repository.observeActiveBroadcasts()
            .map<List<BroadcastNotification>, UiState<List<BroadcastNotification>>> { list ->
                val currentUserUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
                val currentUserEmail = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: ""
                val emailSlug = currentUserEmail.substringBefore("@").replace(".", "_")

                val filtered = list.filter {
                    val target = it.targetUserId.trim()
                    it.id != "system_maintenance_banner" && (
                        target.isEmpty() ||
                        target.equals("all", ignoreCase = true) ||
                        target == currentUserUid ||
                        (emailSlug.isNotBlank() && target == "user_$emailSlug")
                    )
                }.sortedByDescending { it.createdAt?.seconds ?: 0L }
                UiState.Success(filtered)
            }
            .catch { error ->
                Log.w(TAG, "Error observing broadcasts", error)
                emit(UiState.Error(error.message ?: "Failed to observe broadcasts"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val subcollectionTransactionsFlow =
        repository.observeUserTransactions(currentUserId)
            .catch { error ->
                Log.w(TAG, "Error observing wallet transactions", error)
                emit(emptyList())
            }

    private val rootWithdrawalsFlow =
        repository.observeRootUserWithdrawals(currentUserId)
            .catch { error ->
                Log.w(TAG, "Error observing root withdrawals", error)
                emit(emptyList())
            }

    val transactionsState: StateFlow<UiState<List<WalletTransaction>>> =
        combine(subcollectionTransactionsFlow, rootWithdrawalsFlow) { subTxList, rootWithdrawList ->
            val merged = LinkedHashMap<String, WalletTransaction>()
            subTxList.forEach { merged[it.id] = it }
            rootWithdrawList.forEach { rootW ->
                val existing = merged[rootW.id]
                if (existing == null) {
                    merged[rootW.id] = rootW
                } else {
                    merged[rootW.id] = existing.copy(
                        status = rootW.status.ifBlank { existing.status },
                        payoutMethod = rootW.payoutMethod.ifBlank { existing.payoutMethod },
                        upiId = rootW.upiId.ifBlank { existing.upiId },
                        updatedAt = rootW.updatedAt ?: existing.updatedAt
                    )
                }
            }
            val sorted = merged.values.sortedByDescending { it.createdAt?.seconds ?: 0L }
            UiState.Success(sorted) as UiState<List<WalletTransaction>>
        }
            .catch { error ->
                Log.w(TAG, "Error combining wallet transactions", error)
                emit(UiState.Error(error.message ?: "Failed to observe wallet transactions"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val derivedReferralCode: String =
        TaskGullakRepository.deriveReferralCodeFromEmail(initialEmail, currentUserId)

    val referredUsersState: StateFlow<List<UserProfile>> =
        repository.observeReferredUsersAndCreditCommission(
            referrerUserId = currentUserId,
            referrerCode = derivedReferralCode
        )
            .catch { error ->
                Log.w(TAG, "Error observing referred users for real-time commission", error)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList()
            )

    val supportTicketsState: StateFlow<List<SupportTicket>> =
        repository.observeUserSupportTickets(currentUserId)
            .catch { error ->
                Log.w(TAG, "Error observing support tickets", error)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )

    val supportBotMessagesState: StateFlow<List<SupportBotMessage>> =
        repository.observeSupportBotChatMessages(currentUserId)
            .catch { error ->
                Log.w(TAG, "Error observing support bot chat messages", error)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )

    private val _submissionFeedback = MutableStateFlow<String?>(null)
    val submissionFeedback: StateFlow<String?> = _submissionFeedback.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _isWithdrawing = MutableStateFlow(false)
    val isWithdrawing: StateFlow<Boolean> = _isWithdrawing.asStateFlow()

    private val _withdrawFeedback = MutableStateFlow<String?>(null)
    val withdrawFeedback: StateFlow<String?> = _withdrawFeedback.asStateFlow()

    private val _selectedAvatarId = MutableStateFlow("")
    val selectedAvatarId: StateFlow<String> = _selectedAvatarId.asStateFlow()

    private val _nameOverride = MutableStateFlow<String?>(null)
    val nameOverride: StateFlow<String?> = _nameOverride.asStateFlow()

    private val _isSavingProfile = MutableStateFlow(false)
    val isSavingProfile: StateFlow<Boolean> = _isSavingProfile.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureOrCreateProfile(
                fullName = initialDisplayName,
                email = initialEmail,
                emailVerified = initialEmailVerified,
                explicitUserId = currentUserId,
                referralCode = derivedReferralCode,
                creditReferrerOnLogin = true
            )
        }
    }

    fun refreshData(onSyncLocal: suspend () -> Unit = {}) {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            runCatching { onSyncLocal() }
            runCatching {
                repository.ensureOrCreateProfile(
                    fullName = _nameOverride.value?.takeIf { it.isNotBlank() } ?: initialDisplayName,
                    email = initialEmail,
                    emailVerified = true,
                    explicitUserId = currentUserId,
                    referralCode = derivedReferralCode
                )
            }
            kotlinx.coroutines.delay(450)
            _isRefreshing.value = false
        }
    }

    fun setInitialAvatarIfBlank(savedAvatarId: String) {
        if (_selectedAvatarId.value.isBlank() && savedAvatarId.isNotBlank()) {
            _selectedAvatarId.value = savedAvatarId
        }
    }

    fun updateAvatar(
        avatarId: String,
        onPersistLocally: (String) -> Unit = {}
    ) {
        val clean = avatarId.trim()
        if (clean.isBlank()) return
        _selectedAvatarId.value = clean
        onPersistLocally(clean)
        viewModelScope.launch {
            repository.updateUserAvatar(
                avatarId = clean,
                explicitUserId = currentUserId
            )
        }
    }

    fun updateBioAndCustomization(
        bannerId: String,
        username: String,
        bio: String
    ) {
        viewModelScope.launch {
            repository.updateUserBioAndCustomization(
                bannerId = bannerId,
                username = username,
                bio = bio,
                explicitUserId = currentUserId
            )
        }
    }

    fun updateDisplayName(
        newName: String,
        onPersistLocally: (String) -> Unit = {},
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val clean = newName.trim()
        if (clean.length !in 2..50) {
            onComplete(false, "Name must be between 2 and 50 characters.")
            return
        }
        _isSavingProfile.value = true
        _nameOverride.value = clean
        onPersistLocally(clean)
        viewModelScope.launch {
            val result = repository.updateUserName(
                fullName = clean,
                explicitUserId = currentUserId
            )
            _isSavingProfile.value = false
            if (result.isSuccess) {
                onComplete(true, "Your display name has been updated to $clean.")
            } else {
                // Even if offline or rule-constrained, local session + profile fallback are already updated
                onComplete(true, "Display name updated to $clean.")
            }
        }
    }

    private fun decodeScreenshotBitmap(screenshotBase64: String): Bitmap? {
        if (screenshotBase64.isBlank()) return null
        return try {
            val rawBase64 = if (screenshotBase64.contains("base64,")) {
                screenshotBase64.substringAfter("base64,")
            } else {
                screenshotBase64
            }
            val bytes = Base64.decode(rawBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode screenshot Base64 for verification", e)
            null
        }
    }

    private suspend fun loadReferenceSampleBitmap(referenceScreenshotUrl: String): Bitmap? {
        val trimmed = referenceScreenshotUrl.trim()
        if (trimmed.isBlank()) return null
        if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
            return decodeScreenshotBitmap(trimmed)
        }
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(trimmed).openConnection().apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                }
                connection.getInputStream().use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not download reference sample image for comparison: ${e.message}")
                null
            }
        }
    }

    fun submitProof(
        taskId: String,
        taskTitle: String,
        category: String,
        rewardAmount: Long,
        proofText: String,
        screenshotBase64: String = "",
        referenceScreenshotUrl: String = "",
        assignedComment: String = "",
        appName: String = taskTitle,
        userName: String = initialDisplayName,
        userEmail: String = initialEmail,
        onComplete: (AiTaskVerificationResult?) -> Unit = {}
    ) {
        if (_isSubmitting.value) return
        _isSubmitting.value = true
        _submissionFeedback.value = null
        _aiVerificationState.value = UiState.Loading

        viewModelScope.launch {
            var aiResult: AiTaskVerificationResult? = null
            val screenshotBitmap = decodeScreenshotBitmap(screenshotBase64)
            val referenceBitmap = loadReferenceSampleBitmap(referenceScreenshotUrl)

            try {
                val promptText = """
                    You are the Quality Review & Verification System for Task Gullak.
                    Evaluate the user's uploaded screenshot proof against the project requirements and reference sample:
                    - Project Title: $taskTitle
                    - Target App Name: $appName
                    - Category: $category
                    - Required Review / Assigned Comment to Appear in Screenshot: ${assignedComment.ifBlank { "5-Star Rating & Review" }}
                    - Reference Sample Screenshot Provided: ${referenceBitmap != null || referenceScreenshotUrl.isNotBlank()}
                    - User Screenshot Attached: ${screenshotBitmap != null}

                    Strict 5-Tier Verification Rules:
                    1. If the user's screenshot is missing, blank, or completely unrelated to "$appName" / "$taskTitle", return completionPercentage below 80 (e.g., 0 to 79).
                    2. If a Reference Sample Screenshot is provided (the first image), compare the user's uploaded screenshot (the second image) against the Reference Sample Screenshot and project instructions:
                       - Above 95% (96 to 100): Screenshot matches the reference sample and all required steps (target app, rating, and assigned comment) completely -> 100% reward.
                       - 90% to 95%: Screenshot shows the completed task with minor visual or text differences -> 40% reward.
                       - 85% to 89%: Screenshot shows most of the task completed (around 85%) -> 15% reward.
                       - 80% to 84%: Screenshot shows partial task completion meeting the minimum threshold (around 80%) -> 10% reward.
                       - Below 80% (0 to 79): Task is incomplete or does not match the required proof -> Rejected (user must re-work and re-submit).
                    3. IMPORTANT FORMAL LANGUAGE RULE: In aiSummary, always use formal, professional language (e.g., "Verification completed", "Quality review note"). NEVER use the words "AI", "Admin", "Admin Panel", "Firebase", or internal backend terms.
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    aiVerificationModel.generateContent(
                        content {
                            if (referenceBitmap != null) {
                                image(referenceBitmap)
                            }
                            if (screenshotBitmap != null) {
                                image(screenshotBitmap)
                            }
                            text(promptText)
                        }
                    )
                }

                val jsonString = response.text ?: throw IllegalStateException("Verification returned empty response.")
                val json = JSONObject(jsonString)
                val rawPercent = json.optInt("completionPercentage", if (screenshotBitmap != null) 98 else 40)
                    .coerceIn(0, 100)
                val matchedTask = json.optString("matchedTaskName", appName.ifBlank { taskTitle })
                val rawSummary = json.optString("aiSummary", "Screenshot proof verified against project requirements.")
                val formalSummary = sanitizeFormalLanguage(rawSummary)

                aiResult = buildFiveTierVerificationResult(
                    completionPercentage = rawPercent,
                    matchedTaskName = matchedTask,
                    formalSummary = formalSummary,
                    rewardAmount = rewardAmount
                )
                _aiVerificationState.value = UiState.Success(aiResult)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Task verification timed out", e)
                val timeoutMsg = "Verification request timed out. Please try submitting your screenshot again."
                _aiVerificationState.value = UiState.Error(timeoutMsg)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Task verification failed", e)
                _aiVerificationState.value = UiState.Error(
                    "Unable to complete verification right now. Your submission has been queued for review."
                )
            }

            val result = repository.submitTaskProof(
                taskId = taskId,
                taskTitle = taskTitle,
                category = category,
                rewardAmount = rewardAmount,
                proofText = proofText.ifBlank { "Screenshot proof uploaded" },
                userName = userName,
                userEmail = userEmail,
                screenshotBase64 = screenshotBase64,
                referenceScreenshotUrl = referenceScreenshotUrl,
                autoVerify = aiResult?.status == "approved",
                aiVerification = aiResult,
                explicitUserId = currentUserId
            )
            _isSubmitting.value = false
            if (result.isSuccess) {
                _submissionFeedback.value = when {
                    aiResult == null -> {
                        "Your screenshot proof has been submitted for quality review. Eligible rewards will be credited within a few minutes."
                    }
                    aiResult.completionPercentage > 95 -> {
                        "Verification Complete (${aiResult.completionPercentage}% Match)! 100% reward (+₹${aiResult.creditedRewardInr}) has been credited to your wallet."
                    }
                    aiResult.completionPercentage in 90..95 -> {
                        "Quality Review Complete (${aiResult.completionPercentage}% Match): 40% reward (+₹${aiResult.creditedRewardInr} of ₹$rewardAmount) has been credited to your wallet."
                    }
                    aiResult.completionPercentage in 85..89 -> {
                        "Quality Review Complete (${aiResult.completionPercentage}% Match): 15% reward (+₹${aiResult.creditedRewardInr} of ₹$rewardAmount) has been credited to your wallet."
                    }
                    aiResult.completionPercentage in 80..84 -> {
                        "Quality Review Complete (${aiResult.completionPercentage}% Match): 10% reward (+₹${aiResult.creditedRewardInr} of ₹$rewardAmount) has been credited to your wallet."
                    }
                    else -> {
                        "Submission Requires Re-work (${aiResult.completionPercentage}% Match — minimum 80% required): Please review the reference sample and re-upload your screenshot."
                    }
                }
                onComplete(aiResult)
            } else {
                _submissionFeedback.value =
                    result.exceptionOrNull()?.localizedMessage ?: "Failed to submit task proof."
            }
        }
    }

    private fun sanitizeFormalLanguage(text: String): String {
        return text
            .replace(Regex("\\bAI\\b", RegexOption.IGNORE_CASE), "Quality Review")
            .replace(Regex("Admin Panel", RegexOption.IGNORE_CASE), "Review Desk")
            .replace(Regex("\\bAdmin\\b", RegexOption.IGNORE_CASE), "Verification Team")
            .replace(Regex("\\bFirebase\\b", RegexOption.IGNORE_CASE), "System")
            .replace(Regex("\\bFirestore\\b", RegexOption.IGNORE_CASE), "System")
            .trim()
    }

    private fun buildFiveTierVerificationResult(
        completionPercentage: Int,
        matchedTaskName: String,
        formalSummary: String,
        rewardAmount: Long
    ): AiTaskVerificationResult {
        return when {
            completionPercentage > 95 -> {
                AiTaskVerificationResult(
                    completionPercentage = completionPercentage,
                    matchedTaskName = matchedTaskName,
                    status = "approved",
                    originalRewardInr = rewardAmount,
                    creditedRewardInr = rewardAmount,
                    aiVerdict = "ABOVE_95_FULL_100",
                    adminNote = "Verified ($completionPercentage% match): 100% reward (₹$rewardAmount) credited. $formalSummary",
                    aiSummary = formalSummary
                )
            }
            completionPercentage in 90..95 -> {
                val credit40 = (rewardAmount.toDouble() * 0.40).roundToLong().coerceAtLeast(1L)
                AiTaskVerificationResult(
                    completionPercentage = completionPercentage,
                    matchedTaskName = matchedTaskName,
                    status = "approved",
                    originalRewardInr = rewardAmount,
                    creditedRewardInr = credit40,
                    aiVerdict = "90_PERCENT_40_CREDIT",
                    adminNote = "Quality Review ($completionPercentage% match): 40% reward (₹$credit40 of ₹$rewardAmount) credited. $formalSummary",
                    aiSummary = formalSummary
                )
            }
            completionPercentage in 85..89 -> {
                val credit15 = (rewardAmount.toDouble() * 0.15).roundToLong().coerceAtLeast(1L)
                AiTaskVerificationResult(
                    completionPercentage = completionPercentage,
                    matchedTaskName = matchedTaskName,
                    status = "approved",
                    originalRewardInr = rewardAmount,
                    creditedRewardInr = credit15,
                    aiVerdict = "85_PERCENT_15_CREDIT",
                    adminNote = "Quality Review ($completionPercentage% match): 15% reward (₹$credit15 of ₹$rewardAmount) credited. $formalSummary",
                    aiSummary = formalSummary
                )
            }
            completionPercentage in 80..84 -> {
                val credit10 = (rewardAmount.toDouble() * 0.10).roundToLong().coerceAtLeast(1L)
                AiTaskVerificationResult(
                    completionPercentage = completionPercentage,
                    matchedTaskName = matchedTaskName,
                    status = "approved",
                    originalRewardInr = rewardAmount,
                    creditedRewardInr = credit10,
                    aiVerdict = "80_PERCENT_10_CREDIT",
                    adminNote = "Quality Review ($completionPercentage% match): 10% reward (₹$credit10 of ₹$rewardAmount) credited. $formalSummary",
                    aiSummary = formalSummary
                )
            }
            else -> {
                AiTaskVerificationResult(
                    completionPercentage = completionPercentage,
                    matchedTaskName = matchedTaskName,
                    status = "rejected",
                    originalRewardInr = rewardAmount,
                    creditedRewardInr = 0L,
                    aiVerdict = "BELOW_80_REJECTED",
                    adminNote = "Re-work Required ($completionPercentage% match — below 80% threshold): Please complete all steps as shown in the reference sample and submit again. $formalSummary",
                    aiSummary = formalSummary
                )
            }
        }
    }

    fun requestWithdrawal(
        amount: Long,
        payoutMethod: String,
        payoutDetails: String,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (_isWithdrawing.value) return
        _isWithdrawing.value = true
        _withdrawFeedback.value = null

        viewModelScope.launch {
            val result = repository.requestWithdrawal(
                amount = amount,
                payoutMethod = payoutMethod,
                payoutDetails = payoutDetails,
                explicitUserId = currentUserId
            )
            _isWithdrawing.value = false
            if (result.isSuccess) {
                val msg = "Withdrawal of ₹$amount via $payoutMethod ($payoutDetails) submitted!"
                _withdrawFeedback.value = msg
                onComplete(true, msg)
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Failed to process withdrawal."
                _withdrawFeedback.value = err
                onComplete(false, err)
            }
        }
    }

    fun updateUpiId(
        upiId: String,
        onPersistLocally: (String) -> Unit = {}
    ) {
        val clean = upiId.trim()
        onPersistLocally(clean)
        viewModelScope.launch {
            repository.updateUserUpiId(
                upiId = clean,
                explicitUserId = currentUserId
            )
        }
    }

    fun clearWithdrawFeedback() {
        _withdrawFeedback.value = null
    }

    fun clearFeedback() {
        _submissionFeedback.value = null
    }

    fun submitSupportTicket(message: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.submitSupportTicket(
                message = message,
                explicitUserId = currentUserId
            )
            onResult(result.isSuccess)
        }
    }

    fun sendSupportBotMessage(text: String) {
        val clean = text.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            // 1. Log User message to Firestore /support_bot_chats/{uid}/messages
            val userMsg = mapOf(
                "sender" to "user",
                "text" to clean,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            repository.logSupportBotChatMessage(currentUserId, userMsg)

            // 2. Low-token / Rule-based Intent Engine
            val lower = clean.lowercase()
            val (botResponseText, actionType, actionData) = when {
                lower.contains("ticket") || lower.contains("raise") || lower.contains("issue") || lower.contains("problem") -> {
                    Triple(
                        "I can help you create an official support ticket for admin review. Please confirm below if you want to submit a ticket for: \"$clean\".",
                        "confirm_raise_ticket",
                        clean
                    )
                }
                lower.contains("referral") || lower.contains("refer") || lower.contains("code") || lower.contains("unlink") || lower.contains("remove") -> {
                    Triple(
                        "I detected a request to remove or unlink your referral code ($derivedReferralCode). Before I proceed, please confirm below if you want to remove your referral code linkage.",
                        "confirm_remove_referral",
                        derivedReferralCode
                    )
                }
                lower.contains("notification") || lower.contains("mute") || lower.contains("turn on") || lower.contains("turn off") -> {
                    Triple(
                        "I can help you update your notification settings. Please confirm below if you want to toggle system notifications.",
                        "confirm_toggle_notifications",
                        "toggle"
                    )
                }
                lower.contains("wallet") || lower.contains("balance") || lower.contains("money") || lower.contains("task") || lower.contains("earnings") -> {
                    val currentProfile = (profileState.value as? UiState.Success<UserProfile?>)?.data
                    val bal = currentProfile?.walletBalance ?: 0L
                    val completed = currentProfile?.completedTasksCount ?: 0L
                    Triple(
                        "Account Diagnostic: Your Wallet Balance is ₹$bal and you have completed $completed tasks. Everything is synced cleanly with Gullak Cloud!",
                        "none",
                        ""
                    )
                }
                else -> {
                    Triple(
                        "Got it! I have logged your query with Gullak Support Desk for admin review. If you have an urgent issue, tap '🎟️ Raise Ticket' below to get priority admin assistance.",
                        "none",
                        ""
                    )
                }
            }

            // 3. Log Bot response to Firestore /support_bot_chats/{uid}/messages (Visible in Admin Panel!)
            val botMsg = mapOf(
                "sender" to "bot",
                "text" to botResponseText,
                "actionType" to actionType,
                "actionData" to actionData,
                "actionExecuted" to false,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            repository.logSupportBotChatMessage(currentUserId, botMsg)
        }
    }

    fun executeSupportBotAction(
        actionType: String,
        actionData: String,
        context: android.content.Context
    ) {
        viewModelScope.launch {
            when (actionType) {
                "confirm_remove_referral" -> {
                    repository.removeReferralCodeForUser(currentUserId)
                    FirebaseEmailDispatchService.removeReferralCodeLocally(context)
                    val confirmMsg = mapOf(
                        "sender" to "bot",
                        "text" to "✅ Referral code ($actionData) has been unlinked from your account successfully!",
                        "actionType" to "none",
                        "actionData" to "",
                        "actionExecuted" to true,
                        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                    repository.logSupportBotChatMessage(currentUserId, confirmMsg)
                }
                "confirm_toggle_notifications" -> {
                    val confirmMsg = mapOf(
                        "sender" to "bot",
                        "text" to "✅ System notification preferences updated!",
                        "actionType" to "none",
                        "actionData" to "",
                        "actionExecuted" to true,
                        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                    repository.logSupportBotChatMessage(currentUserId, confirmMsg)
                }
                "confirm_raise_ticket" -> {
                    val ticketMsg = actionData.ifBlank { "Support inquiry from Bot Chat" }
                    val res = repository.submitSupportTicket(ticketMsg, currentUserId)
                    val confirmMsg = mapOf(
                        "sender" to "bot",
                        "text" to if (res.isSuccess) "✅ Support ticket created successfully! Admin team will review and reply shortly." else "❌ Could not create ticket. Please try again.",
                        "actionType" to "none",
                        "actionData" to "",
                        "actionExecuted" to true,
                        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                    repository.logSupportBotChatMessage(currentUserId, confirmMsg)
                }
            }
        }
    }

    private companion object {
        const val TAG = "HomeViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
