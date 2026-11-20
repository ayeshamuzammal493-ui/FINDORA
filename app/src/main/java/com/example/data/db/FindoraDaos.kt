package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun observeAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun observeUserById(id: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun getUserByPhone(phone: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET isSuspended = :suspended WHERE id = :userId")
    suspend fun setUserSuspended(userId: String, suspended: Boolean)

    @Query("UPDATE users SET planTier = :planTier WHERE id = :userId")
    suspend fun updatePlanTier(userId: String, planTier: String)

    @Query("UPDATE users SET passwordHash = :newHash WHERE id = :userId")
    suspend fun updatePasswordHash(userId: String, newHash: String)
}

@Dao
interface OrganizationDao {
    @Query("SELECT * FROM organizations ORDER BY name ASC")
    fun observeOrganizations(): Flow<List<OrganizationEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrganizations(orgs: List<OrganizationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrganization(org: OrganizationEntity)

    @Query("UPDATE organizations SET verifiedBadge = :verified WHERE id = :orgId")
    suspend fun setVerified(orgId: String, verified: Boolean)
}

@Dao
interface LostReportDao {
    @Query("SELECT * FROM lost_reports ORDER BY createdAt DESC")
    fun observeAllLostReports(): Flow<List<LostReportEntity>>

    @Query("SELECT * FROM lost_reports WHERE status != 'REMOVED' ORDER BY createdAt DESC")
    fun observePublicLostReports(): Flow<List<LostReportEntity>>

    @Query("SELECT * FROM lost_reports WHERE userId = :userId AND status != 'REMOVED' ORDER BY createdAt DESC")
    fun observeUserLostReports(userId: String): Flow<List<LostReportEntity>>

    @Query("SELECT * FROM lost_reports WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): LostReportEntity?

    @Query("SELECT * FROM lost_reports WHERE status = 'ACTIVE'")
    suspend fun getActiveReportsSync(): List<LostReportEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLostReport(report: LostReportEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLostReports(reports: List<LostReportEntity>)

    @Update
    suspend fun updateLostReport(report: LostReportEntity)

    @Query("UPDATE lost_reports SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM lost_reports WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface FoundReportDao {
    @Query("SELECT * FROM found_reports ORDER BY createdAt DESC")
    fun observeAllFoundReports(): Flow<List<FoundReportEntity>>

    @Query("SELECT * FROM found_reports WHERE status != 'REMOVED' ORDER BY createdAt DESC")
    fun observePublicFoundReports(): Flow<List<FoundReportEntity>>

    @Query("SELECT * FROM found_reports WHERE userId = :userId AND status != 'REMOVED' ORDER BY createdAt DESC")
    fun observeUserFoundReports(userId: String): Flow<List<FoundReportEntity>>

    @Query("SELECT * FROM found_reports WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): FoundReportEntity?

    @Query("SELECT * FROM found_reports WHERE status = 'ACTIVE'")
    suspend fun getActiveReportsSync(): List<FoundReportEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoundReport(report: FoundReportEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFoundReports(reports: List<FoundReportEntity>)

    @Update
    suspend fun updateFoundReport(report: FoundReportEntity)

    @Query("UPDATE found_reports SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM found_reports WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface MatchDao {
    @Query("SELECT * FROM matches ORDER BY score DESC, createdAt DESC")
    fun observeAllMatches(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE lostReportId = :lostId AND foundReportId = :foundId LIMIT 1")
    suspend fun findByPair(lostId: String, foundId: String): MatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMatches(matches: List<MatchEntity>)

    @Query("UPDATE matches SET status = :status WHERE id = :matchId")
    suspend fun updateMatchStatus(matchId: String, status: String)
}

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations WHERE ownerUserId = :userId OR finderUserId = :userId ORDER BY lastMessageAt DESC")
    fun observeUserConversations(userId: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    fun observeConversation(id: String): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE (ownerUserId = :ownerId AND finderUserId = :finderId) AND (lostReportId = :lostId OR foundReportId = :foundId) LIMIT 1")
    suspend fun findExisting(ownerId: String, finderId: String, lostId: String?, foundId: String?): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertConversations(conversations: List<ConversationEntity>)

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun observeMessages(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE isFlagged = 1 ORDER BY createdAt DESC")
    fun observeFlaggedMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages ORDER BY createdAt DESC LIMIT 50")
    fun observeRecentMessagesForAdmin(): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("UPDATE messages SET isFlagged = :flagged WHERE id = :messageId")
    suspend fun setFlagged(messageId: String, flagged: Boolean)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeUserNotifications(userId: String): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllRead(userId: String)
}

@Dao
interface SafetyReportDao {
    @Query("SELECT * FROM reports ORDER BY createdAt DESC")
    fun observeAllReports(): Flow<List<SafetyReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: SafetyReportEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReports(reports: List<SafetyReportEntity>)

    @Query("UPDATE reports SET status = :status WHERE id = :reportId")
    suspend fun updateReportStatus(reportId: String, status: String)
}

@Dao
interface ReturnConfirmationDao {
    @Query("SELECT * FROM return_confirmations ORDER BY completedAt DESC")
    fun observeAllReturnConfirmations(): Flow<List<ReturnConfirmationEntity>>

    @Query("SELECT * FROM return_confirmations WHERE conversationId = :conversationId LIMIT 1")
    fun observeForConversation(conversationId: String): Flow<ReturnConfirmationEntity?>

    @Query("SELECT * FROM return_confirmations WHERE conversationId = :conversationId LIMIT 1")
    suspend fun getForConversation(conversationId: String): ReturnConfirmationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConfirmation(confirmation: ReturnConfirmationEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertConfirmations(confirmations: List<ReturnConfirmationEntity>)
}
