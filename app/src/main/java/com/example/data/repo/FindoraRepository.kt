package com.example.data.repo

import android.content.Context
import android.net.Uri
import com.example.data.db.ConversationEntity
import com.example.data.db.FindoraDatabase
import com.example.data.db.FoundReportEntity
import com.example.data.db.LostReportEntity
import com.example.data.db.MatchEntity
import com.example.data.db.MessageEntity
import com.example.data.db.MessageType
import com.example.data.db.NotificationEntity
import com.example.data.db.OrganizationEntity
import com.example.data.db.PlanTier
import com.example.data.db.ReportStatus
import com.example.data.db.ReturnConfirmationEntity
import com.example.data.db.SafetyReportEntity
import com.example.data.db.UserEntity
import com.example.data.db.UserRole
import com.example.data.db.VerificationStatus
import com.example.data.prefs.ApproxLocation
import com.example.data.prefs.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class FindoraRepository(
    private val context: Context,
    private val db: FindoraDatabase,
    private val sessionManager: SessionManager
) {
    private val userDao = db.userDao()
    private val orgDao = db.organizationDao()
    private val lostDao = db.lostReportDao()
    private val foundDao = db.foundReportDao()
    private val matchDao = db.matchDao()
    private val conversationDao = db.conversationDao()
    private val messageDao = db.messageDao()
    private val notificationDao = db.notificationDao()
    private val safetyReportDao = db.safetyReportDao()
    private val returnConfirmationDao = db.returnConfirmationDao()

    val currentUserIdFlow: Flow<String?> = sessionManager.currentUserId
    val approxLocationFlow: Flow<ApproxLocation> = sessionManager.approxLocation

    fun observeUser(userId: String): Flow<UserEntity?> = userDao.observeUserById(userId)
    fun observeAllUsers(): Flow<List<UserEntity>> = userDao.observeAllUsers()
    fun observeOrganizations(): Flow<List<OrganizationEntity>> = orgDao.observeOrganizations()
    fun observePublicLostReports(): Flow<List<LostReportEntity>> = lostDao.observePublicLostReports()
    fun observeAllLostReportsForAdmin(): Flow<List<LostReportEntity>> = lostDao.observeAllLostReports()
    fun observePublicFoundReports(): Flow<List<FoundReportEntity>> = foundDao.observePublicFoundReports()
    fun observeAllFoundReportsForAdmin(): Flow<List<FoundReportEntity>> = foundDao.observeAllFoundReports()
    fun observeUserLostReports(userId: String): Flow<List<LostReportEntity>> = lostDao.observeUserLostReports(userId)
    fun observeUserFoundReports(userId: String): Flow<List<FoundReportEntity>> = foundDao.observeUserFoundReports(userId)
    fun observeAllMatches(): Flow<List<MatchEntity>> = matchDao.observeAllMatches()
    fun observeUserConversations(userId: String): Flow<List<ConversationEntity>> = conversationDao.observeUserConversations(userId)
    fun observeConversation(id: String): Flow<ConversationEntity?> = conversationDao.observeConversation(id)
    fun observeMessages(conversationId: String): Flow<List<MessageEntity>> = messageDao.observeMessages(conversationId)
    fun observeFlaggedMessages(): Flow<List<MessageEntity>> = messageDao.observeFlaggedMessages()
    fun observeRecentMessagesForAdmin(): Flow<List<MessageEntity>> = messageDao.observeRecentMessagesForAdmin()
    fun observeUserNotifications(userId: String): Flow<List<NotificationEntity>> = notificationDao.observeUserNotifications(userId)
    fun observeSafetyReports(): Flow<List<SafetyReportEntity>> = safetyReportDao.observeAllReports()
    fun observeReturnConfirmations(): Flow<List<ReturnConfirmationEntity>> = returnConfirmationDao.observeAllReturnConfirmations()
    fun observeReturnConfirmation(conversationId: String): Flow<ReturnConfirmationEntity?> =
        returnConfirmationDao.observeForConversation(conversationId)

    suspend fun setApproxLocation(label: String, lat: Double, lng: Double) {
        sessionManager.setApproxLocation(label, lat, lng)
    }

    // --- Authentication ---
    suspend fun login(identifier: String, password: String, usePhone: Boolean): Result<UserEntity> {
        val clean = identifier.trim()
        if (clean.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter your credentials."))
        }
        val user = if (usePhone) userDao.getUserByPhone(clean) else userDao.getUserByEmail(clean)
        if (user == null) {
            return Result.failure(IllegalArgumentException("No account found for $clean."))
        }
        if (user.isSuspended) {
            return Result.failure(IllegalStateException("This account is currently suspended by Trust & Safety."))
        }
        val hash = FindoraDatabase.hashPassword(password)
        if (user.passwordHash != hash) {
            return Result.failure(IllegalArgumentException("Incorrect password. Please try again or reset."))
        }
        sessionManager.setCurrentUserId(user.id)
        return Result.success(user)
    }

    suspend fun signUp(
        fullName: String,
        email: String,
        phone: String,
        city: String,
        password: String
    ): Result<UserEntity> {
        val cleanName = fullName.trim()
        val cleanEmail = email.trim()
        val cleanPhone = phone.trim()
        if (cleanName.length < 2) return Result.failure(IllegalArgumentException("Enter your full name."))
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return Result.failure(IllegalArgumentException("Enter a valid email address."))
        }
        if (cleanPhone.length < 7) return Result.failure(IllegalArgumentException("Enter a valid phone number."))
        if (password.length < 6) return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))

        if (userDao.getUserByEmail(cleanEmail) != null) {
            return Result.failure(IllegalArgumentException("An account with this email already exists."))
        }
        if (userDao.getUserByPhone(cleanPhone) != null) {
            return Result.failure(IllegalArgumentException("An account with this phone number already exists."))
        }

        val newUser = UserEntity(
            id = "user_${UUID.randomUUID().toString().take(8)}",
            fullName = cleanName,
            email = cleanEmail,
            phone = cleanPhone,
            passwordHash = FindoraDatabase.hashPassword(password),
            role = UserRole.USER,
            planTier = PlanTier.FREE,
            city = city.trim().ifBlank { "New York" },
            bio = "FINDORA member",
            isVerified = true,
            isSuspended = false
        )
        userDao.insertUser(newUser)
        notificationDao.insertNotification(
            NotificationEntity(
                id = "notif_${UUID.randomUUID().toString().take(8)}",
                userId = newUser.id,
                title = "Welcome to FINDORA",
                body = "Basic reporting and private messaging are always free. Your personal contact details remain hidden.",
                type = "SAFETY"
            )
        )
        sessionManager.setCurrentUserId(newUser.id)
        return Result.success(newUser)
    }

    suspend fun resetPassword(identifier: String, usePhone: Boolean, newPassword: String): Result<Unit> {
        val clean = identifier.trim()
        if (clean.isBlank()) return Result.failure(IllegalArgumentException("Enter your email or phone number."))
        if (newPassword.length < 6) return Result.failure(IllegalArgumentException("New password must be at least 6 characters."))
        val user = (if (usePhone) userDao.getUserByPhone(clean) else userDao.getUserByEmail(clean))
            ?: return Result.failure(IllegalArgumentException("Account not found for $clean."))
        userDao.updatePasswordHash(user.id, FindoraDatabase.hashPassword(newPassword))
        return Result.success(Unit)
    }

    suspend fun quickDemoLogin(userId: String): Result<UserEntity> {
        val user = userDao.getUserById(userId)
            ?: return Result.failure(IllegalStateException("Demo profile unavailable."))
        if (user.isSuspended) {
            return Result.failure(IllegalStateException("Account suspended."))
        }
        sessionManager.setCurrentUserId(user.id)
        return Result.success(user)
    }

    suspend fun logout() {
        sessionManager.setCurrentUserId(null)
    }

    suspend fun updateProfile(userId: String, fullName: String, city: String, bio: String): Result<Unit> {
        val existing = userDao.getUserById(userId) ?: return Result.failure(IllegalStateException("User not found"))
        if (fullName.trim().length < 2) return Result.failure(IllegalArgumentException("Name is too short"))
        userDao.updateUser(
            existing.copy(
                fullName = fullName.trim(),
                city = city.trim().ifBlank { existing.city },
                bio = bio.trim()
            )
        )
        return Result.success(Unit)
    }

    suspend fun upgradePlan(userId: String, newTier: String): Result<Unit> {
        userDao.updatePlanTier(userId, newTier)
        notificationDao.insertNotification(
            NotificationEntity(
                id = "notif_${UUID.randomUUID().toString().take(8)}",
                userId = userId,
                title = "Plan Updated to $newTier",
                body = "Your account benefits are now active across search alerts and hub priority.",
                type = "SAFETY"
            )
        )
        return Result.success(Unit)
    }

    // --- Photo Persistence ---
    suspend fun copyPhotoToAppStorage(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.filesDir, "report_photos")
            if (!dir.exists()) dir.mkdirs()
            val outFile = File(dir, "photo_${UUID.randomUUID().toString().take(10)}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                outFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null
            outFile.absolutePath
        }.getOrNull()
    }

    // --- Duplicate Checking & Report Publishing ---
    suspend fun findPotentialDuplicateLost(userId: String, title: String, category: String): LostReportEntity? {
        return lostDao.getActiveReportsSync().firstOrNull {
            it.userId == userId &&
                it.category.equals(category, ignoreCase = true) &&
                MatchingEngine.isNearDuplicateTitle(it.title, title)
        }
    }

    suspend fun findPotentialDuplicateFound(userId: String, title: String, category: String): FoundReportEntity? {
        return foundDao.getActiveReportsSync().firstOrNull {
            it.userId == userId &&
                it.category.equals(category, ignoreCase = true) &&
                MatchingEngine.isNearDuplicateTitle(it.title, title)
        }
    }

    suspend fun publishLostReport(report: LostReportEntity): Result<LostReportEntity> {
        if (report.title.trim().length < 3) {
            return Result.failure(IllegalArgumentException("Item name must be at least 3 characters."))
        }
        if (report.description.trim().length < 10) {
            return Result.failure(IllegalArgumentException("Please provide a fuller description (10+ characters)."))
        }
        if (report.approxLocationName.trim().isBlank()) {
            return Result.failure(IllegalArgumentException("Approximate location is required."))
        }
        val clean = report.copy(
            title = report.title.trim(),
            description = report.description.trim(),
            approxLocationName = report.approxLocationName.trim(),
            city = report.city.trim().ifBlank { "New York" }
        )
        lostDao.insertLostReport(clean)
        recomputeMatchesForLost(clean)
        return Result.success(clean)
    }

    suspend fun publishFoundReport(report: FoundReportEntity): Result<FoundReportEntity> {
        if (report.title.trim().length < 3) {
            return Result.failure(IllegalArgumentException("Item title must be at least 3 characters."))
        }
        if (report.description.trim().length < 10) {
            return Result.failure(IllegalArgumentException("Public description must be at least 10 characters."))
        }
        if (report.hiddenIdentifyingDetails.trim().length < 5) {
            return Result.failure(IllegalArgumentException("Add private hidden details so only the real owner can verify."))
        }
        if (report.verificationQuestion.trim().length < 8) {
            return Result.failure(IllegalArgumentException("Add an ownership verification question."))
        }
        val clean = report.copy(
            title = report.title.trim(),
            description = report.description.trim(),
            approxLocationName = report.approxLocationName.trim(),
            city = report.city.trim().ifBlank { "New York" },
            hiddenIdentifyingDetails = report.hiddenIdentifyingDetails.trim(),
            verificationQuestion = report.verificationQuestion.trim()
        )
        foundDao.insertFoundReport(clean)
        recomputeMatchesForFound(clean)
        return Result.success(clean)
    }

    suspend fun deleteLostReport(reportId: String) {
        lostDao.deleteById(reportId)
    }

    suspend fun deleteFoundReport(reportId: String) {
        foundDao.deleteById(reportId)
    }

    private suspend fun recomputeMatchesForLost(lost: LostReportEntity) {
        val activeFound = foundDao.getActiveReportsSync()
        for (found in activeFound) {
            val existing = matchDao.findByPair(lost.id, found.id)
            val candidate = MatchingEngine.buildMatchEntity(lost, found, existing?.id)
            if (candidate != null) {
                matchDao.insertMatch(candidate.copy(status = existing?.status ?: "SUGGESTED"))
                if (existing == null) {
                    notificationDao.insertNotification(
                        NotificationEntity(
                            id = "notif_${UUID.randomUUID().toString().take(8)}",
                            userId = lost.userId,
                            title = "${candidate.score}% Possible Match",
                            body = "'${found.title}' matches your lost '${lost.title}' (${candidate.reasonSummary}).",
                            type = "MATCH",
                            relatedId = candidate.id
                        )
                    )
                    notificationDao.insertNotification(
                        NotificationEntity(
                            id = "notif_${UUID.randomUUID().toString().take(8)}",
                            userId = found.userId,
                            title = "${candidate.score}% Possible Match",
                            body = "Someone reported losing '${lost.title}' near ${lost.approxLocationName}.",
                            type = "MATCH",
                            relatedId = candidate.id
                        )
                    )
                }
            }
        }
    }

    private suspend fun recomputeMatchesForFound(found: FoundReportEntity) {
        val activeLost = lostDao.getActiveReportsSync()
        for (lost in activeLost) {
            val existing = matchDao.findByPair(lost.id, found.id)
            val candidate = MatchingEngine.buildMatchEntity(lost, found, existing?.id)
            if (candidate != null) {
                matchDao.insertMatch(candidate.copy(status = existing?.status ?: "SUGGESTED"))
                if (existing == null) {
                    notificationDao.insertNotification(
                        NotificationEntity(
                            id = "notif_${UUID.randomUUID().toString().take(8)}",
                            userId = lost.userId,
                            title = "${candidate.score}% Possible Match",
                            body = "'${found.title}' was just reported near ${found.approxLocationName}.",
                            type = "MATCH",
                            relatedId = candidate.id
                        )
                    )
                }
            }
        }
    }

    suspend fun dismissMatch(matchId: String) {
        matchDao.updateMatchStatus(matchId, "DISMISSED")
    }

    // --- Conversations, Messaging & Ownership Verification ---
    suspend fun startOrGetConversation(
        currentUserId: String,
        lostReport: LostReportEntity?,
        foundReport: FoundReportEntity?
    ): Result<ConversationEntity> {
        val ownerId: String
        val finderId: String
        val itemTitle: String
        val prompt: String

        when {
            foundReport != null && lostReport != null -> {
                ownerId = lostReport.userId
                finderId = foundReport.userId
                itemTitle = lostReport.title
                prompt = foundReport.verificationQuestion
            }
            foundReport != null -> {
                ownerId = currentUserId
                finderId = foundReport.userId
                itemTitle = foundReport.title
                prompt = foundReport.verificationQuestion
            }
            lostReport != null -> {
                ownerId = lostReport.userId
                finderId = currentUserId
                itemTitle = lostReport.title
                prompt = "Describe a unique detail or serial number that proves this item belongs to you."
            }
            else -> return Result.failure(IllegalArgumentException("Missing item context."))
        }

        if (ownerId == finderId) {
            return Result.failure(IllegalArgumentException("You cannot message yourself on your own listing."))
        }

        val existing = conversationDao.findExisting(ownerId, finderId, lostReport?.id, foundReport?.id)
        if (existing != null) {
            return Result.success(existing)
        }

        val now = System.currentTimeMillis()
        val convId = "conv_${UUID.randomUUID().toString().take(8)}"
        val starterText = if (currentUserId == ownerId) {
            "Hi! I believe this item may be mine. I'm ready to answer ownership verification."
        } else {
            "Hi! I may have found your item. Let's verify ownership privately."
        }
        val conv = ConversationEntity(
            id = convId,
            lostReportId = lostReport?.id,
            foundReportId = foundReport?.id,
            itemTitle = itemTitle,
            ownerUserId = ownerId,
            finderUserId = finderId,
            lastMessagePreview = starterText,
            lastMessageAt = now,
            verificationStatus = if (foundReport != null) VerificationStatus.REQUESTED else VerificationStatus.NONE,
            verificationPrompt = prompt
        )
        conversationDao.insertConversation(conv)
        messageDao.insertMessage(
            MessageEntity(
                id = "msg_${UUID.randomUUID().toString().take(8)}",
                conversationId = convId,
                senderId = currentUserId,
                body = starterText,
                messageType = MessageType.TEXT,
                createdAt = now
            )
        )
        val recipientId = if (currentUserId == ownerId) finderId else ownerId
        notificationDao.insertNotification(
            NotificationEntity(
                id = "notif_${UUID.randomUUID().toString().take(8)}",
                userId = recipientId,
                title = "New Private Message",
                body = "Regarding '$itemTitle': $starterText",
                type = "MESSAGE",
                relatedId = convId
            )
        )
        return Result.success(conv)
    }

    suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        body: String,
        type: String = MessageType.TEXT
    ): Result<Unit> {
        val conv = conversationDao.getById(conversationId)
            ?: return Result.failure(IllegalStateException("Conversation not found"))
        if (conv.isBlocked) {
            return Result.failure(IllegalStateException("This conversation is blocked."))
        }
        val clean = body.trim()
        if (clean.isBlank()) return Result.failure(IllegalArgumentException("Message cannot be empty."))

        val now = System.currentTimeMillis()
        val flagged = containsSuspiciousPattern(clean)
        messageDao.insertMessage(
            MessageEntity(
                id = "msg_${UUID.randomUUID().toString().take(8)}",
                conversationId = conversationId,
                senderId = senderId,
                body = clean,
                messageType = type,
                isFlagged = flagged,
                createdAt = now
            )
        )
        conversationDao.updateConversation(
            conv.copy(lastMessagePreview = clean, lastMessageAt = now)
        )
        val recipientId = if (senderId == conv.ownerUserId) conv.finderUserId else conv.ownerUserId
        notificationDao.insertNotification(
            NotificationEntity(
                id = "notif_${UUID.randomUUID().toString().take(8)}",
                userId = recipientId,
                title = "New Message • ${conv.itemTitle}",
                body = clean,
                type = "MESSAGE",
                relatedId = conversationId
            )
        )
        return Result.success(Unit)
    }

    private fun containsSuspiciousPattern(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("gift card") || lower.contains("wire money") || lower.contains("crypto")
    }

    suspend fun requestVerification(conversationId: String, finderId: String, question: String): Result<Unit> {
        val conv = conversationDao.getById(conversationId)
            ?: return Result.failure(IllegalStateException("Conversation not found"))
        val cleanQ = question.trim()
        if (cleanQ.length < 6) return Result.failure(IllegalArgumentException("Enter a clear verification question."))
        val now = System.currentTimeMillis()
        conversationDao.updateConversation(
            conv.copy(
                verificationStatus = VerificationStatus.REQUESTED,
                verificationPrompt = cleanQ,
                lastMessagePreview = "Ownership proof requested: $cleanQ",
                lastMessageAt = now
            )
        )
        messageDao.insertMessage(
            MessageEntity(
                id = "msg_${UUID.randomUUID().toString().take(8)}",
                conversationId = conversationId,
                senderId = finderId,
                body = cleanQ,
                messageType = MessageType.VERIFICATION_REQUEST,
                createdAt = now
            )
        )
        notificationDao.insertNotification(
            NotificationEntity(
                id = "notif_${UUID.randomUUID().toString().take(8)}",
                userId = conv.ownerUserId,
                title = "Verification Requested",
                body = "Answer the finder's private question for '${conv.itemTitle}'.",
                type = "VERIFICATION",
                relatedId = conversationId
            )
        )
        return Result.success(Unit)
    }

    suspend fun submitVerificationAnswer(conversationId: String, ownerId: String, answer: String): Result<Unit> {
        val conv = conversationDao.getById(conversationId)
            ?: return Result.failure(IllegalStateException("Conversation not found"))
        val cleanA = answer.trim()
        if (cleanA.length < 2) return Result.failure(IllegalArgumentException("Please enter your proof answer."))
        val now = System.currentTimeMillis()
        conversationDao.updateConversation(
            conv.copy(
                verificationStatus = VerificationStatus.SUBMITTED,
                verificationAnswer = cleanA,
                lastMessagePreview = "Ownership proof submitted",
                lastMessageAt = now
            )
        )
        messageDao.insertMessage(
            MessageEntity(
                id = "msg_${UUID.randomUUID().toString().take(8)}",
                conversationId = conversationId,
                senderId = ownerId,
                body = cleanA,
                messageType = MessageType.VERIFICATION_ANSWER,
                createdAt = now
            )
        )
        notificationDao.insertNotification(
            NotificationEntity(
                id = "notif_${UUID.randomUUID().toString().take(8)}",
                userId = conv.finderUserId,
                title = "Ownership Proof Submitted",
                body = "Review the claimant's answer for '${conv.itemTitle}'.",
                type = "VERIFICATION",
                relatedId = conversationId
            )
        )
        return Result.success(Unit)
    }

    suspend fun reviewVerification(conversationId: String, finderId: String, approved: Boolean): Result<Unit> {
        val conv = conversationDao.getById(conversationId)
            ?: return Result.failure(IllegalStateException("Conversation not found"))
        val newStatus = if (approved) VerificationStatus.APPROVED else VerificationStatus.REJECTED
        val msgBody = if (approved) {
            "Ownership verified! Choose a safe public handover location below."
        } else {
            "Verification answer did not match private details. Please double-check or ask another detail."
        }
        val now = System.currentTimeMillis()
        conversationDao.updateConversation(
            conv.copy(
                verificationStatus = newStatus,
                lastMessagePreview = msgBody,
                lastMessageAt = now
            )
        )
        messageDao.insertMessage(
            MessageEntity(
                id = "msg_${UUID.randomUUID().toString().take(8)}",
                conversationId = conversationId,
                senderId = finderId,
                body = msgBody,
                messageType = MessageType.SYSTEM,
                createdAt = now
            )
        )
        if (approved) {
            conv.lostReportId?.let { lostDao.updateStatus(it, ReportStatus.MATCHED) }
            conv.foundReportId?.let { foundDao.updateStatus(it, ReportStatus.MATCHED) }
        }
        return Result.success(Unit)
    }

    suspend fun suggestHandoverSpot(conversationId: String, senderId: String, spot: String): Result<Unit> {
        return sendMessage(
            conversationId = conversationId,
            senderId = senderId,
            body = "Suggested safe public handover point: $spot",
            type = MessageType.HANDOVER_SUGGESTION
        )
    }

    suspend fun confirmReturn(
        conversationId: String,
        actingUserId: String,
        handoverSpot: String
    ): Result<ReturnConfirmationEntity> {
        val conv = conversationDao.getById(conversationId)
            ?: return Result.failure(IllegalStateException("Conversation not found"))
        val existing = returnConfirmationDao.getForConversation(conversationId)
        val isOwner = actingUserId == conv.ownerUserId
        val updated = if (existing == null) {
            ReturnConfirmationEntity(
                id = "ret_${UUID.randomUUID().toString().take(8)}",
                conversationId = conversationId,
                lostReportId = conv.lostReportId,
                foundReportId = conv.foundReportId,
                ownerUserId = conv.ownerUserId,
                finderUserId = conv.finderUserId,
                handoverSpot = handoverSpot,
                ownerConfirmed = isOwner,
                finderConfirmed = !isOwner,
                completedAt = null
            )
        } else {
            val ownerOk = existing.ownerConfirmed || isOwner
            val finderOk = existing.finderConfirmed || !isOwner
            existing.copy(
                handoverSpot = handoverSpot.ifBlank { existing.handoverSpot },
                ownerConfirmed = ownerOk,
                finderConfirmed = finderOk,
                completedAt = if (ownerOk && finderOk) System.currentTimeMillis() else null
            )
        }
        returnConfirmationDao.upsertConfirmation(updated)
        val now = System.currentTimeMillis()
        val bothDone = updated.ownerConfirmed && updated.finderConfirmed
        val statusMsg = if (bothDone) {
            "Return completed and confirmed by both parties at ${updated.handoverSpot}!"
        } else {
            "${if (isOwner) "Owner" else "Finder"} confirmed safe handover at ${updated.handoverSpot}. Waiting for the other party."
        }
        messageDao.insertMessage(
            MessageEntity(
                id = "msg_${UUID.randomUUID().toString().take(8)}",
                conversationId = conversationId,
                senderId = actingUserId,
                body = statusMsg,
                messageType = MessageType.SYSTEM,
                createdAt = now
            )
        )
        conversationDao.updateConversation(
            conv.copy(lastMessagePreview = statusMsg, lastMessageAt = now)
        )
        if (bothDone) {
            conv.lostReportId?.let { lostDao.updateStatus(it, ReportStatus.RETURNED) }
            conv.foundReportId?.let { foundDao.updateStatus(it, ReportStatus.RETURNED) }
        }
        return Result.success(updated)
    }

    suspend fun toggleBlockConversation(conversationId: String, actingUserId: String) {
        val conv = conversationDao.getById(conversationId) ?: return
        val newBlocked = !conv.isBlocked
        conversationDao.updateConversation(
            conv.copy(
                isBlocked = newBlocked,
                blockedByUserId = if (newBlocked) actingUserId else null
            )
        )
    }

    // --- Notifications ---
    suspend fun markNotificationsRead(userId: String) {
        notificationDao.markAllRead(userId)
    }

    // --- Trust & Safety + Admin Moderation ---
    suspend fun submitSafetyReport(
        reporterUserId: String,
        targetType: String,
        targetId: String,
        reason: String,
        details: String
    ): Result<Unit> {
        if (reason.isBlank()) return Result.failure(IllegalArgumentException("Select a reason."))
        safetyReportDao.insertReport(
            SafetyReportEntity(
                id = "safety_${UUID.randomUUID().toString().take(8)}",
                reporterUserId = reporterUserId,
                targetType = targetType,
                targetId = targetId,
                reason = reason,
                details = details.trim().ifBlank { "Reported from in-app safety flow." },
                status = "OPEN"
            )
        )
        return Result.success(Unit)
    }

    suspend fun adminUpdateSafetyReportStatus(reportId: String, status: String) {
        safetyReportDao.updateReportStatus(reportId, status)
    }

    suspend fun adminRemoveListing(isLost: Boolean, reportId: String) {
        if (isLost) {
            lostDao.updateStatus(reportId, ReportStatus.REMOVED)
        } else {
            foundDao.updateStatus(reportId, ReportStatus.REMOVED)
        }
    }

    suspend fun adminRestoreListing(isLost: Boolean, reportId: String) {
        if (isLost) {
            lostDao.updateStatus(reportId, ReportStatus.ACTIVE)
        } else {
            foundDao.updateStatus(reportId, ReportStatus.ACTIVE)
        }
    }

    suspend fun adminSetUserSuspended(userId: String, suspended: Boolean) {
        userDao.setUserSuspended(userId, suspended)
    }

    suspend fun adminToggleOrgVerified(orgId: String, verified: Boolean) {
        orgDao.setVerified(orgId, verified)
    }

    suspend fun adminModerateMessage(messageId: String, delete: Boolean) {
        if (delete) {
            messageDao.deleteMessage(messageId)
        } else {
            messageDao.setFlagged(messageId, false)
        }
    }
}
