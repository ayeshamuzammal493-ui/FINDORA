package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

object UserRole {
    const val USER = "USER"
    const val ORG_STAFF = "ORG_STAFF"
    const val ADMIN = "ADMIN"
}

object PlanTier {
    const val FREE = "FREE"
    const val PREMIUM = "PREMIUM"
    const val ORGANIZATION = "ORGANIZATION"
}

object ReportStatus {
    const val ACTIVE = "ACTIVE"
    const val MATCHED = "MATCHED"
    const val RETURNED = "RETURNED"
    const val REMOVED = "REMOVED"
}

object VerificationStatus {
    const val NONE = "NONE"
    const val REQUESTED = "REQUESTED"
    const val SUBMITTED = "SUBMITTED"
    const val APPROVED = "APPROVED"
    const val REJECTED = "REJECTED"
}

object MessageType {
    const val TEXT = "TEXT"
    const val VERIFICATION_REQUEST = "VERIFICATION_REQUEST"
    const val VERIFICATION_ANSWER = "VERIFICATION_ANSWER"
    const val HANDOVER_SUGGESTION = "HANDOVER_SUGGESTION"
    const val SYSTEM = "SYSTEM"
}

object ItemCategories {
    val all = listOf(
        "Electronics",
        "Bags & Luggage",
        "Watches & Jewelry",
        "Keys & Access",
        "Wallets & IDs",
        "Travel Gear",
        "Documents",
        "Other"
    )
}

data class GeoHubPreset(
    val id: String,
    val name: String,
    val city: String,
    val country: String,
    val orgType: String,
    val latitude: Double,
    val longitude: Double
)

object GeoHubPresets {
    val hubs = listOf(
        GeoHubPreset("hub_jfk", "JFK Terminal 4 Desk", "New York", "USA", "AIRPORT", 40.6413, -73.7781),
        GeoHubPreset("hub_midtown", "Midtown Transit Concourse", "New York", "USA", "MALL", 40.7527, -73.9772),
        GeoHubPreset("hub_columbia", "Morningside Campus Hall", "New York", "USA", "UNIVERSITY", 40.8075, -73.9626),
        GeoHubPreset("hub_soho", "SoHo Grand Hotel Concierge", "New York", "USA", "HOTEL", 40.7223, -74.0049),
        GeoHubPreset("hub_lhr", "Heathrow Terminal 5 Desk", "London", "UK", "AIRPORT", 51.4700, -0.4543),
        GeoHubPreset("hub_sin", "Changi Jewel Concierge", "Singapore", "SG", "AIRPORT", 1.3602, 103.9898)
    )
}

object SafeHandoverSpots {
    val recommendations = listOf(
        "Airport Terminal Lost & Found Desk (Staffed 24/7)",
        "University Campus Security & Welcome Desk",
        "Shopping Mall Concierge / Information Booth",
        "Hotel Front Desk Lobby (CCTV Monitored)",
        "Local Police Safe Exchange Zone"
    )
}

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val passwordHash: String,
    val role: String = UserRole.USER,
    val planTier: String = PlanTier.FREE,
    val organizationId: String? = null,
    val city: String = "New York",
    val bio: String = "",
    val isVerified: Boolean = false,
    val isSuspended: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "organizations")
data class OrganizationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val orgType: String, // AIRPORT, UNIVERSITY, HOTEL, MALL
    val city: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val planTier: String = PlanTier.ORGANIZATION,
    val verifiedBadge: Boolean = true,
    val contactDesk: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "lost_reports")
data class LostReportEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val category: String,
    val description: String,
    val colorBrand: String = "",
    val dateLostMillis: Long,
    val approxLocationName: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val photoUri: String? = null,
    val contactPreference: String = "IN_APP_ONLY", // IN_APP_ONLY, EMAIL_RELAY, ORG_DESK
    val rewardOffered: String = "",
    val status: String = ReportStatus.ACTIVE,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "found_reports")
data class FoundReportEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val organizationId: String? = null,
    val title: String,
    val category: String,
    val description: String,
    val dateFoundMillis: Long,
    val approxLocationName: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val photoUri: String? = null,
    val hiddenIdentifyingDetails: String, // NEVER shown publicly
    val verificationQuestion: String,
    val status: String = ReportStatus.ACTIVE,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey val id: String,
    val lostReportId: String,
    val foundReportId: String,
    val score: Int,
    val distanceKm: Double,
    val reasonSummary: String,
    val status: String = "SUGGESTED", // SUGGESTED, CONTACTED, DISMISSED, CONFIRMED
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val lostReportId: String? = null,
    val foundReportId: String? = null,
    val itemTitle: String,
    val ownerUserId: String,
    val finderUserId: String,
    val lastMessagePreview: String,
    val lastMessageAt: Long,
    val isBlocked: Boolean = false,
    val blockedByUserId: String? = null,
    val verificationStatus: String = VerificationStatus.NONE,
    val verificationPrompt: String = "",
    val verificationAnswer: String = ""
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val body: String,
    val messageType: String = MessageType.TEXT,
    val isFlagged: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val body: String,
    val type: String, // MATCH, MESSAGE, VERIFICATION, SAFETY, RETURN
    val relatedId: String? = null,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reports")
data class SafetyReportEntity(
    @PrimaryKey val id: String,
    val reporterUserId: String,
    val targetType: String, // USER, LOST_REPORT, FOUND_REPORT, MESSAGE
    val targetId: String,
    val reason: String,
    val details: String,
    val status: String = "OPEN", // OPEN, REVIEWED, DISMISSED, ACTION_TAKEN
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "return_confirmations")
data class ReturnConfirmationEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val lostReportId: String? = null,
    val foundReportId: String? = null,
    val ownerUserId: String,
    val finderUserId: String,
    val handoverSpot: String,
    val ownerConfirmed: Boolean = false,
    val finderConfirmed: Boolean = false,
    val completedAt: Long? = null
)
