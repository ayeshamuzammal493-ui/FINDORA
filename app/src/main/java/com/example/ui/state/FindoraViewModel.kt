package com.example.ui.state

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.db.ConversationEntity
import com.example.data.db.FoundReportEntity
import com.example.data.db.LostReportEntity
import com.example.data.db.MatchEntity
import com.example.data.db.MessageEntity
import com.example.data.db.NotificationEntity
import com.example.data.db.OrganizationEntity
import com.example.data.db.ReturnConfirmationEntity
import com.example.data.db.SafetyReportEntity
import com.example.data.db.UserEntity
import com.example.data.prefs.ApproxLocation
import com.example.data.repo.FindoraRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class ReportTypeFilter { ALL, LOST, FOUND }
enum class DateWindowFilter(val label: String, val days: Int?) {
    ALL("Any Date", null),
    TODAY("Last 24h", 1),
    WEEK("Last 7d", 7),
    MONTH("Last 30d", 30)
}

data class SearchFilters(
    val query: String = "",
    val reportType: ReportTypeFilter = ReportTypeFilter.ALL,
    val category: String = "All",
    val maxDistanceKm: Double? = 50.0,
    val dateWindow: DateWindowFilter = DateWindowFilter.ALL
)

sealed interface AuthUiState {
    data object Loading : AuthUiState
    data object Unauthenticated : AuthUiState
    data class Authenticated(val user: UserEntity) : AuthUiState
}

data class FindoraDashboardData(
    val allUsers: List<UserEntity> = emptyList(),
    val organizations: List<OrganizationEntity> = emptyList(),
    val publicLost: List<LostReportEntity> = emptyList(),
    val publicFound: List<FoundReportEntity> = emptyList(),
    val adminAllLost: List<LostReportEntity> = emptyList(),
    val adminAllFound: List<FoundReportEntity> = emptyList(),
    val matches: List<MatchEntity> = emptyList(),
    val conversations: List<ConversationEntity> = emptyList(),
    val notifications: List<NotificationEntity> = emptyList(),
    val safetyReports: List<SafetyReportEntity> = emptyList(),
    val returnConfirmations: List<ReturnConfirmationEntity> = emptyList(),
    val flaggedMessages: List<MessageEntity> = emptyList(),
    val recentMessages: List<MessageEntity> = emptyList(),
    val approxLocation: ApproxLocation = ApproxLocation()
)

class FindoraViewModel(
    private val repository: FindoraRepository
) : ViewModel() {

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isAuthBusy = MutableStateFlow(false)
    val isAuthBusy: StateFlow<Boolean> = _isAuthBusy.asStateFlow()

    private val _searchFilters = MutableStateFlow(SearchFilters())
    val searchFilters: StateFlow<SearchFilters> = _searchFilters.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarEvents = _snackbarEvents.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val authState: StateFlow<AuthUiState> = repository.currentUserIdFlow
        .flatMapLatest { uid ->
            if (uid.isNullOrBlank()) {
                flowOf(AuthUiState.Unauthenticated)
            } else {
                repository.observeUser(uid).flatMapLatest { user ->
                    if (user == null || user.isSuspended) {
                        flowOf(AuthUiState.Unauthenticated)
                    } else {
                        flowOf(AuthUiState.Authenticated(user))
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AuthUiState.Loading)

    @OptIn(ExperimentalCoroutinesApi::class)
    val appData: StateFlow<FindoraDashboardData> = repository.currentUserIdFlow
        .flatMapLatest { uid ->
            if (uid.isNullOrBlank()) {
                flowOf(FindoraDashboardData())
            } else {
                val baseFlows = combine(
                    repository.observeAllUsers(),
                    repository.observeOrganizations(),
                    repository.observePublicLostReports(),
                    repository.observePublicFoundReports(),
                    repository.observeAllMatches()
                ) { users, orgs, pubLost, pubFound, matches ->
                    QuadBase(users, orgs, pubLost, pubFound, matches)
                }

                val userFlows = combine(
                    repository.observeUserConversations(uid),
                    repository.observeUserNotifications(uid),
                    repository.observeSafetyReports(),
                    repository.observeReturnConfirmations(),
                    repository.approxLocationFlow
                ) { convs, notifs, safety, returns, approx ->
                    UserFlows(convs, notifs, safety, returns, approx)
                }

                val adminFlows = combine(
                    repository.observeAllLostReportsForAdmin(),
                    repository.observeAllFoundReportsForAdmin(),
                    repository.observeFlaggedMessages(),
                    repository.observeRecentMessagesForAdmin()
                ) { adminLost, adminFound, flagged, recent ->
                    AdminFlows(adminLost, adminFound, flagged, recent)
                }

                combine(baseFlows, userFlows, adminFlows) { base, usr, adm ->
                    FindoraDashboardData(
                        allUsers = base.users,
                        organizations = base.orgs,
                        publicLost = base.pubLost,
                        publicFound = base.pubFound,
                        adminAllLost = adm.adminLost,
                        adminAllFound = adm.adminFound,
                        matches = base.matches,
                        conversations = usr.convs,
                        notifications = usr.notifs,
                        safetyReports = usr.safety,
                        returnConfirmations = usr.returns,
                        flaggedMessages = adm.flagged,
                        recentMessages = adm.recent,
                        approxLocation = usr.approx
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FindoraDashboardData())

    fun clearAuthError() {
        _authError.value = null
    }

    fun login(identifier: String, password: String, usePhone: Boolean) {
        viewModelScope.launch {
            _isAuthBusy.value = true
            _authError.value = null
            val res = repository.login(identifier, password, usePhone)
            res.onFailure { _authError.value = it.message ?: "Sign in failed." }
            _isAuthBusy.value = false
        }
    }

    fun signUp(fullName: String, email: String, phone: String, city: String, password: String) {
        viewModelScope.launch {
            _isAuthBusy.value = true
            _authError.value = null
            val res = repository.signUp(fullName, email, phone, city, password)
            res.onFailure { _authError.value = it.message ?: "Sign up failed." }
            _isAuthBusy.value = false
        }
    }

    fun resetPassword(identifier: String, usePhone: Boolean, newPassword: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _isAuthBusy.value = true
            _authError.value = null
            val res = repository.resetPassword(identifier, usePhone, newPassword)
            res.onSuccess {
                _snackbarEvents.tryEmit("Password updated. You can now log in.")
                onDone()
            }.onFailure {
                _authError.value = it.message ?: "Password reset failed."
            }
            _isAuthBusy.value = false
        }
    }

    fun quickDemoLogin(userId: String) {
        viewModelScope.launch {
            _isAuthBusy.value = true
            _authError.value = null
            repository.quickDemoLogin(userId).onFailure {
                _authError.value = it.message ?: "Demo sign-in failed."
            }
            _isAuthBusy.value = false
        }
    }

    fun logout() {
        viewModelScope.launch { repository.logout() }
    }

    fun updateFilters(transform: (SearchFilters) -> SearchFilters) {
        _searchFilters.value = transform(_searchFilters.value)
    }

    fun setApproxLocation(label: String, lat: Double, lng: Double) {
        viewModelScope.launch {
            repository.setApproxLocation(label, lat, lng)
            _snackbarEvents.tryEmit("Search area updated to $label")
        }
    }

    fun updateProfile(userId: String, fullName: String, city: String, bio: String) {
        viewModelScope.launch {
            repository.updateProfile(userId, fullName, city, bio)
                .onSuccess { _snackbarEvents.tryEmit("Profile saved") }
                .onFailure { _snackbarEvents.tryEmit(it.message ?: "Unable to save profile") }
        }
    }

    fun upgradePlan(userId: String, tier: String) {
        viewModelScope.launch {
            repository.upgradePlan(userId, tier)
                .onSuccess { _snackbarEvents.tryEmit("Account plan updated to $tier") }
        }
    }

    suspend fun copyPickedPhoto(uri: Uri): String? = repository.copyPhotoToAppStorage(uri)

    suspend fun checkDuplicateLost(userId: String, title: String, category: String): LostReportEntity? =
        repository.findPotentialDuplicateLost(userId, title, category)

    suspend fun checkDuplicateFound(userId: String, title: String, category: String): FoundReportEntity? =
        repository.findPotentialDuplicateFound(userId, title, category)

    fun saveLostReport(
        existingId: String?,
        userId: String,
        title: String,
        category: String,
        description: String,
        colorBrand: String,
        dateLostMillis: Long,
        approxLocationName: String,
        city: String,
        latitude: Double,
        longitude: Double,
        photoUri: String?,
        contactPreference: String,
        rewardOffered: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val entity = LostReportEntity(
                id = existingId ?: "lost_${UUID.randomUUID().toString().take(8)}",
                userId = userId,
                title = title,
                category = category,
                description = description,
                colorBrand = colorBrand,
                dateLostMillis = dateLostMillis,
                approxLocationName = approxLocationName,
                city = city,
                latitude = latitude,
                longitude = longitude,
                photoUri = photoUri,
                contactPreference = contactPreference,
                rewardOffered = rewardOffered
            )
            repository.publishLostReport(entity)
                .onSuccess {
                    _snackbarEvents.tryEmit(if (existingId == null) "Lost report published & matched!" else "Lost report updated")
                    onSuccess()
                }
                .onFailure { onError(it.message ?: "Validation failed") }
        }
    }

    fun saveFoundReport(
        existingId: String?,
        userId: String,
        organizationId: String?,
        title: String,
        category: String,
        description: String,
        dateFoundMillis: Long,
        approxLocationName: String,
        city: String,
        latitude: Double,
        longitude: Double,
        photoUri: String?,
        hiddenDetails: String,
        verificationQuestion: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val entity = FoundReportEntity(
                id = existingId ?: "found_${UUID.randomUUID().toString().take(8)}",
                userId = userId,
                organizationId = organizationId,
                title = title,
                category = category,
                description = description,
                dateFoundMillis = dateFoundMillis,
                approxLocationName = approxLocationName,
                city = city,
                latitude = latitude,
                longitude = longitude,
                photoUri = photoUri,
                hiddenIdentifyingDetails = hiddenDetails,
                verificationQuestion = verificationQuestion
            )
            repository.publishFoundReport(entity)
                .onSuccess {
                    _snackbarEvents.tryEmit(if (existingId == null) "Found report published safely!" else "Found report updated")
                    onSuccess()
                }
                .onFailure { onError(it.message ?: "Validation failed") }
        }
    }

    fun deleteLostReport(reportId: String) {
        viewModelScope.launch {
            repository.deleteLostReport(reportId)
            _snackbarEvents.tryEmit("Lost report deleted")
        }
    }

    fun deleteFoundReport(reportId: String) {
        viewModelScope.launch {
            repository.deleteFoundReport(reportId)
            _snackbarEvents.tryEmit("Found report deleted")
        }
    }

    fun dismissMatch(matchId: String) {
        viewModelScope.launch {
            repository.dismissMatch(matchId)
        }
    }

    fun startConversation(
        currentUserId: String,
        lostReport: LostReportEntity?,
        foundReport: FoundReportEntity?,
        onOpened: (String) -> Unit
    ) {
        viewModelScope.launch {
            repository.startOrGetConversation(currentUserId, lostReport, foundReport)
                .onSuccess { onOpened(it.id) }
                .onFailure { _snackbarEvents.tryEmit(it.message ?: "Unable to start conversation") }
        }
    }

    fun observeConversation(id: String): Flow<ConversationEntity?> = repository.observeConversation(id)
    fun observeMessages(id: String): Flow<List<MessageEntity>> = repository.observeMessages(id)
    fun observeReturnConfirmation(convId: String): Flow<ReturnConfirmationEntity?> =
        repository.observeReturnConfirmation(convId)

    fun sendMessage(conversationId: String, senderId: String, body: String) {
        viewModelScope.launch {
            repository.sendMessage(conversationId, senderId, body)
                .onFailure { _snackbarEvents.tryEmit(it.message ?: "Failed to send") }
        }
    }

    fun requestVerification(conversationId: String, finderId: String, question: String) {
        viewModelScope.launch {
            repository.requestVerification(conversationId, finderId, question)
                .onSuccess { _snackbarEvents.tryEmit("Verification prompt sent") }
                .onFailure { _snackbarEvents.tryEmit(it.message ?: "Invalid question") }
        }
    }

    fun submitVerificationAnswer(conversationId: String, ownerId: String, answer: String) {
        viewModelScope.launch {
            repository.submitVerificationAnswer(conversationId, ownerId, answer)
                .onSuccess { _snackbarEvents.tryEmit("Proof submitted privately") }
                .onFailure { _snackbarEvents.tryEmit(it.message ?: "Invalid answer") }
        }
    }

    fun reviewVerification(conversationId: String, finderId: String, approved: Boolean) {
        viewModelScope.launch {
            repository.reviewVerification(conversationId, finderId, approved)
        }
    }

    fun suggestHandoverSpot(conversationId: String, senderId: String, spot: String) {
        viewModelScope.launch {
            repository.suggestHandoverSpot(conversationId, senderId, spot)
        }
    }

    fun confirmReturn(conversationId: String, userId: String, spot: String) {
        viewModelScope.launch {
            repository.confirmReturn(conversationId, userId, spot)
                .onSuccess { _snackbarEvents.tryEmit("Handover confirmation recorded") }
        }
    }

    fun toggleBlockConversation(conversationId: String, actingUserId: String) {
        viewModelScope.launch {
            repository.toggleBlockConversation(conversationId, actingUserId)
            _snackbarEvents.tryEmit("Conversation block status updated")
        }
    }

    fun markNotificationsRead(userId: String) {
        viewModelScope.launch { repository.markNotificationsRead(userId) }
    }

    fun submitSafetyReport(
        reporterUserId: String,
        targetType: String,
        targetId: String,
        reason: String,
        details: String
    ) {
        viewModelScope.launch {
            repository.submitSafetyReport(reporterUserId, targetType, targetId, reason, details)
                .onSuccess { _snackbarEvents.tryEmit("Report submitted to Trust & Safety") }
                .onFailure { _snackbarEvents.tryEmit(it.message ?: "Unable to submit report") }
        }
    }

    // --- Admin Actions ---
    fun adminUpdateSafetyStatus(reportId: String, status: String) {
        viewModelScope.launch {
            repository.adminUpdateSafetyReportStatus(reportId, status)
            _snackbarEvents.tryEmit("Safety report marked $status")
        }
    }

    fun adminToggleListingRemoval(isLost: Boolean, reportId: String, remove: Boolean) {
        viewModelScope.launch {
            if (remove) repository.adminRemoveListing(isLost, reportId)
            else repository.adminRestoreListing(isLost, reportId)
            _snackbarEvents.tryEmit(if (remove) "Listing removed" else "Listing restored")
        }
    }

    fun adminSetUserSuspended(userId: String, suspended: Boolean) {
        viewModelScope.launch {
            repository.adminSetUserSuspended(userId, suspended)
            _snackbarEvents.tryEmit(if (suspended) "User suspended" else "User reinstated")
        }
    }

    fun adminToggleOrgVerified(orgId: String, verified: Boolean) {
        viewModelScope.launch {
            repository.adminToggleOrgVerified(orgId, verified)
            _snackbarEvents.tryEmit("Organization verification updated")
        }
    }

    fun adminModerateMessage(messageId: String, delete: Boolean) {
        viewModelScope.launch {
            repository.adminModerateMessage(messageId, delete)
            _snackbarEvents.tryEmit(if (delete) "Message removed" else "Message cleared")
        }
    }

    private data class QuadBase(
        val users: List<UserEntity>,
        val orgs: List<OrganizationEntity>,
        val pubLost: List<LostReportEntity>,
        val pubFound: List<FoundReportEntity>,
        val matches: List<MatchEntity>
    )

    private data class UserFlows(
        val convs: List<ConversationEntity>,
        val notifs: List<NotificationEntity>,
        val safety: List<SafetyReportEntity>,
        val returns: List<ReturnConfirmationEntity>,
        val approx: ApproxLocation
    )

    private data class AdminFlows(
        val adminLost: List<LostReportEntity>,
        val adminFound: List<FoundReportEntity>,
        val flagged: List<MessageEntity>,
        val recent: List<MessageEntity>
    )

    companion object {
        fun factory(repository: FindoraRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return FindoraViewModel(repository) as T
                }
            }
    }
}
