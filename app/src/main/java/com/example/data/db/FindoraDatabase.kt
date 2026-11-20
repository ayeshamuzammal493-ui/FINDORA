package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.MessageDigest

@Database(
    entities = [
        UserEntity::class,
        OrganizationEntity::class,
        LostReportEntity::class,
        FoundReportEntity::class,
        MatchEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        NotificationEntity::class,
        SafetyReportEntity::class,
        ReturnConfirmationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FindoraDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun organizationDao(): OrganizationDao
    abstract fun lostReportDao(): LostReportDao
    abstract fun foundReportDao(): FoundReportDao
    abstract fun matchDao(): MatchDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun notificationDao(): NotificationDao
    abstract fun safetyReportDao(): SafetyReportDao
    abstract fun returnConfirmationDao(): ReturnConfirmationDao

    companion object {
        @Volatile
        private var INSTANCE: FindoraDatabase? = null

        fun getInstance(context: Context): FindoraDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun hashPassword(raw: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }

        private fun buildDatabase(context: Context): FindoraDatabase {
            lateinit var dbRef: FindoraDatabase
            dbRef = Room.databaseBuilder(
                context,
                FindoraDatabase::class.java,
                "findora_database.db"
            )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            seedInitialData(dbRef)
                        }
                    }
                })
                .build()
            return dbRef
        }

        suspend fun seedInitialData(db: FindoraDatabase) {
            val now = System.currentTimeMillis()
            val hour = 3_600_000L
            val day = 24 * hour

            val demoHash = hashPassword("Findora123!")

            val users = listOf(
                UserEntity(
                    id = "user_maya",
                    fullName = "Maya Lin",
                    email = "maya.lin@example.com",
                    phone = "+15550142",
                    passwordHash = demoHash,
                    role = UserRole.USER,
                    planTier = PlanTier.FREE,
                    city = "New York",
                    bio = "Daily commuter and traveler. Always grateful for honest finders.",
                    isVerified = true,
                    createdAt = now - 30 * day
                ),
                UserEntity(
                    id = "user_liam",
                    fullName = "Liam Patel",
                    email = "liam.patel@example.com",
                    phone = "+15550198",
                    passwordHash = demoHash,
                    role = UserRole.USER,
                    planTier = PlanTier.PREMIUM,
                    city = "New York",
                    bio = "Frequent flyer and coffee shop regular.",
                    isVerified = true,
                    createdAt = now - 25 * day
                ),
                UserEntity(
                    id = "user_jfk_desk",
                    fullName = "JFK T4 Lost & Found Desk",
                    email = "desk.jfk@findora-partner.org",
                    phone = "+15550210",
                    passwordHash = demoHash,
                    role = UserRole.ORG_STAFF,
                    planTier = PlanTier.ORGANIZATION,
                    organizationId = "org_jfk",
                    city = "New York",
                    bio = "Official Terminal 4 Lost Property Management Team.",
                    isVerified = true,
                    createdAt = now - 60 * day
                ),
                UserEntity(
                    id = "user_admin",
                    fullName = "Elena Vance (Trust & Safety)",
                    email = "admin@findora.app",
                    phone = "+15550001",
                    passwordHash = demoHash,
                    role = UserRole.ADMIN,
                    planTier = PlanTier.ORGANIZATION,
                    city = "New York",
                    bio = "Platform Moderator & Trust/Safety Lead.",
                    isVerified = true,
                    createdAt = now - 90 * day
                ),
                UserEntity(
                    id = "user_suspicious",
                    fullName = "Alex Mercer",
                    email = "alex.m@example.net",
                    phone = "+15550999",
                    passwordHash = demoHash,
                    role = UserRole.USER,
                    planTier = PlanTier.FREE,
                    city = "New York",
                    bio = "New member",
                    isVerified = false,
                    createdAt = now - 5 * day
                )
            )
            db.userDao().insertUsers(users)

            val orgs = listOf(
                OrganizationEntity(
                    id = "org_jfk",
                    name = "JFK International Terminal 4",
                    orgType = "AIRPORT",
                    city = "New York",
                    country = "USA",
                    latitude = 40.6413,
                    longitude = -73.7781,
                    planTier = PlanTier.ORGANIZATION,
                    verifiedBadge = true,
                    contactDesk = "Arrivals Level, Concourse B Desk",
                    createdAt = now - 60 * day
                ),
                OrganizationEntity(
                    id = "org_columbia",
                    name = "Columbia Morningside Campus",
                    orgType = "UNIVERSITY",
                    city = "New York",
                    country = "USA",
                    latitude = 40.8075,
                    longitude = -73.9626,
                    planTier = PlanTier.ORGANIZATION,
                    verifiedBadge = true,
                    contactDesk = "Low Library Safety & Lost Desk",
                    createdAt = now - 45 * day
                ),
                OrganizationEntity(
                    id = "org_soho",
                    name = "SoHo Grand Hotel",
                    orgType = "HOTEL",
                    city = "New York",
                    country = "USA",
                    latitude = 40.7223,
                    longitude = -74.0049,
                    planTier = PlanTier.ORGANIZATION,
                    verifiedBadge = true,
                    contactDesk = "Main Lobby Concierge",
                    createdAt = now - 40 * day
                ),
                OrganizationEntity(
                    id = "org_midtown_mall",
                    name = "Hudson Yards Atrium Mall",
                    orgType = "MALL",
                    city = "New York",
                    country = "USA",
                    latitude = 40.7538,
                    longitude = -74.0022,
                    planTier = PlanTier.ORGANIZATION,
                    verifiedBadge = true,
                    contactDesk = "Level 1 Guest Services Desk",
                    createdAt = now - 35 * day
                )
            )
            db.organizationDao().insertOrganizations(orgs)

            val lostReports = listOf(
                LostReportEntity(
                    id = "lost_backpack_maya",
                    userId = "user_maya",
                    title = "Navy Canvas Commuter Backpack",
                    category = "Bags & Luggage",
                    description = "Navy blue minimalist canvas backpack with leather buckle straps and front zipper pocket. Left near the Midtown concourse seating area.",
                    colorBrand = "Navy / Hershel-style",
                    dateLostMillis = now - 14 * hour,
                    approxLocationName = "Midtown Transit Concourse Area",
                    city = "New York",
                    latitude = 40.7527,
                    longitude = -73.9772,
                    photoUri = "drawable://img_item_backpack",
                    contactPreference = "IN_APP_ONLY",
                    rewardOffered = "$40 thank-you",
                    status = ReportStatus.ACTIVE,
                    createdAt = now - 12 * hour
                ),
                LostReportEntity(
                    id = "lost_watch_liam",
                    userId = "user_liam",
                    title = "Silver Chronograph Watch Leather Strap",
                    category = "Watches & Jewelry",
                    description = "Classic silver dial chronograph wristwatch with a dark espresso leather band. Misplaced near airport security screening trays.",
                    colorBrand = "Silver / Dark Brown Leather",
                    dateLostMillis = now - 2 * day,
                    approxLocationName = "JFK Terminal 4 Screening Zone",
                    city = "New York",
                    latitude = 40.6420,
                    longitude = -73.7795,
                    photoUri = "drawable://img_item_watch",
                    contactPreference = "ORG_DESK",
                    rewardOffered = "",
                    status = ReportStatus.ACTIVE,
                    createdAt = now - 2 * day
                ),
                LostReportEntity(
                    id = "lost_headphones_maya",
                    userId = "user_maya",
                    title = "Matte Silver Wireless Headphones",
                    category = "Electronics",
                    description = "Over-ear wireless noise-cancelling headphones in matte silver. Possibly left at a SoHo cafe table during afternoon work session.",
                    colorBrand = "Silver / Gray Cushions",
                    dateLostMillis = now - 3 * day,
                    approxLocationName = "SoHo Cafe District",
                    city = "New York",
                    latitude = 40.7235,
                    longitude = -74.0031,
                    photoUri = "drawable://img_item_headphones",
                    contactPreference = "IN_APP_ONLY",
                    rewardOffered = "",
                    status = ReportStatus.RETURNED,
                    createdAt = now - 3 * day
                ),
                LostReportEntity(
                    id = "lost_passport_traveler",
                    userId = "user_liam",
                    title = "Blue Travel Document Folio & Keychain",
                    category = "Documents",
                    description = "Slim navy travel wallet containing boarding passes and a brass apartment key tag.",
                    colorBrand = "Dark Blue",
                    dateLostMillis = now - 5 * hour,
                    approxLocationName = "Hudson Yards Plaza",
                    city = "New York",
                    latitude = 40.7538,
                    longitude = -74.0022,
                    photoUri = null,
                    contactPreference = "IN_APP_ONLY",
                    rewardOffered = "",
                    status = ReportStatus.ACTIVE,
                    createdAt = now - 4 * hour
                )
            )
            db.lostReportDao().insertLostReports(lostReports)

            val foundReports = listOf(
                FoundReportEntity(
                    id = "found_watch_jfk",
                    userId = "user_jfk_desk",
                    organizationId = "org_jfk",
                    title = "Silver Chronograph Wristwatch (Leather Band)",
                    category = "Watches & Jewelry",
                    description = "Turned in at JFK Terminal 4 Arrivals counter. Silver dial chronograph with dark leather strap.",
                    dateFoundMillis = now - 1 * day,
                    approxLocationName = "JFK Terminal 4 Desk",
                    city = "New York",
                    latitude = 40.6413,
                    longitude = -73.7781,
                    photoUri = "drawable://img_item_watch",
                    hiddenIdentifyingDetails = "Caseback engraved with initials 'L.P. 2023' and serial ending 4829.",
                    verificationQuestion = "What initials and year are engraved on the watch caseback?",
                    status = ReportStatus.ACTIVE,
                    createdAt = now - 1 * day
                ),
                FoundReportEntity(
                    id = "found_backpack_liam",
                    userId = "user_liam",
                    organizationId = null,
                    title = "Dark Navy Canvas Backpack with Buckles",
                    category = "Bags & Luggage",
                    description = "Found on a limestone bench near Midtown station entrance. Secured safely.",
                    dateFoundMillis = now - 9 * hour,
                    approxLocationName = "Midtown East Concourse Bench",
                    city = "New York",
                    latitude = 40.7519,
                    longitude = -73.9760,
                    photoUri = "drawable://img_item_backpack",
                    hiddenIdentifyingDetails = "Contains a green enamel transit pin inside the front zip pocket and a spiral notebook.",
                    verificationQuestion = "What small accessory or pin is inside the front zippered pocket?",
                    status = ReportStatus.ACTIVE,
                    createdAt = now - 8 * hour
                ),
                FoundReportEntity(
                    id = "found_headphones_soho",
                    userId = "user_liam",
                    organizationId = "org_soho",
                    title = "Silver Over-Ear Wireless Headphones",
                    category = "Electronics",
                    description = "Left on an oak cafe table near SoHo Grand. Held safely for verified owner.",
                    dateFoundMillis = now - 3 * day,
                    approxLocationName = "SoHo Grand Lobby Area",
                    city = "New York",
                    latitude = 40.7223,
                    longitude = -74.0049,
                    photoUri = "drawable://img_item_headphones",
                    hiddenIdentifyingDetails = "Bluetooth device name is 'Maya-QC-Audio' and tiny sticker on inner headband.",
                    verificationQuestion = "What custom Bluetooth pairing name appears when powered on?",
                    status = ReportStatus.RETURNED,
                    createdAt = now - 3 * day
                ),
                FoundReportEntity(
                    id = "found_keys_campus",
                    userId = "user_jfk_desk",
                    organizationId = "org_columbia",
                    title = "Brass Key Ring with Blue Fob",
                    category = "Keys & Access",
                    description = "Set of 3 keys with a blue university access fob turned into campus desk.",
                    dateFoundMillis = now - 6 * hour,
                    approxLocationName = "Morningside Campus Hall",
                    city = "New York",
                    latitude = 40.8075,
                    longitude = -73.9626,
                    photoUri = null,
                    hiddenIdentifyingDetails = "Key fob number ends in 714 and brass tag reads 'Apt 4B'.",
                    verificationQuestion = "What apartment label or number is stamped on the brass key tag?",
                    status = ReportStatus.ACTIVE,
                    createdAt = now - 5 * hour
                )
            )
            db.foundReportDao().insertFoundReports(foundReports)

            val matches = listOf(
                MatchEntity(
                    id = "match_backpack_1",
                    lostReportId = "lost_backpack_maya",
                    foundReportId = "found_backpack_liam",
                    score = 94,
                    distanceKm = 0.14,
                    reasonSummary = "Same category (Bags & Luggage) • 3 keyword matches • 0.1 km apart",
                    status = "SUGGESTED",
                    createdAt = now - 7 * hour
                ),
                MatchEntity(
                    id = "match_watch_1",
                    lostReportId = "lost_watch_liam",
                    foundReportId = "found_watch_jfk",
                    score = 96,
                    distanceKm = 0.14,
                    reasonSummary = "Same category (Watches & Jewelry) • 4 keyword matches • 0.1 km apart",
                    status = "CONTACTED",
                    createdAt = now - 20 * hour
                )
            )
            db.matchDao().insertMatches(matches)

            val conversations = listOf(
                ConversationEntity(
                    id = "conv_backpack",
                    lostReportId = "lost_backpack_maya",
                    foundReportId = "found_backpack_liam",
                    itemTitle = "Navy Canvas Commuter Backpack",
                    ownerUserId = "user_maya",
                    finderUserId = "user_liam",
                    lastMessagePreview = "Please confirm what pin is inside the front pocket so I can hand it over safely.",
                    lastMessageAt = now - 2 * hour,
                    isBlocked = false,
                    verificationStatus = VerificationStatus.REQUESTED,
                    verificationPrompt = "What small accessory or pin is inside the front zippered pocket?",
                    verificationAnswer = ""
                ),
                ConversationEntity(
                    id = "conv_headphones",
                    lostReportId = "lost_headphones_maya",
                    foundReportId = "found_headphones_soho",
                    itemTitle = "Matte Silver Wireless Headphones",
                    ownerUserId = "user_maya",
                    finderUserId = "user_liam",
                    lastMessagePreview = "Return confirmed! Thank you for using FINDORA safely.",
                    lastMessageAt = now - 2 * day,
                    isBlocked = false,
                    verificationStatus = VerificationStatus.APPROVED,
                    verificationPrompt = "What custom Bluetooth pairing name appears when powered on?",
                    verificationAnswer = "Maya-QC-Audio"
                )
            )
            db.conversationDao().insertConversations(conversations)

            val messages = listOf(
                MessageEntity(
                    id = "msg_1",
                    conversationId = "conv_backpack",
                    senderId = "user_maya",
                    body = "Hi Liam! I saw your found backpack listing near Midtown concourse. That looks just like mine!",
                    messageType = MessageType.TEXT,
                    createdAt = now - 3 * hour
                ),
                MessageEntity(
                    id = "msg_2",
                    conversationId = "conv_backpack",
                    senderId = "user_liam",
                    body = "Please confirm what pin is inside the front pocket so I can hand it over safely.",
                    messageType = MessageType.VERIFICATION_REQUEST,
                    createdAt = now - 2 * hour
                ),
                MessageEntity(
                    id = "msg_3",
                    conversationId = "conv_headphones",
                    senderId = "user_maya",
                    body = "The Bluetooth name is Maya-QC-Audio.",
                    messageType = MessageType.VERIFICATION_ANSWER,
                    createdAt = now - 2 * day - hour
                ),
                MessageEntity(
                    id = "msg_4",
                    conversationId = "conv_headphones",
                    senderId = "user_liam",
                    body = "Verified! Let's meet at SoHo Grand Lobby Concierge (CCTV Monitored).",
                    messageType = MessageType.HANDOVER_SUGGESTION,
                    createdAt = now - 2 * day - 1800_000L
                ),
                MessageEntity(
                    id = "msg_5",
                    conversationId = "conv_headphones",
                    senderId = "user_maya",
                    body = "Return confirmed! Thank you for using FINDORA safely.",
                    messageType = MessageType.SYSTEM,
                    createdAt = now - 2 * day
                ),
                MessageEntity(
                    id = "msg_flagged_demo",
                    conversationId = "conv_backpack",
                    senderId = "user_suspicious",
                    body = "Send $50 gift card code before pickup.",
                    messageType = MessageType.TEXT,
                    isFlagged = true,
                    createdAt = now - 4 * hour
                )
            )
            db.messageDao().insertMessages(messages)

            val notifications = listOf(
                NotificationEntity(
                    id = "notif_1",
                    userId = "user_maya",
                    title = "94% Possible Match Found",
                    body = "A 'Dark Navy Canvas Backpack' was found 0.1 km from your lost location.",
                    type = "MATCH",
                    relatedId = "match_backpack_1",
                    isRead = false,
                    createdAt = now - 7 * hour
                ),
                NotificationEntity(
                    id = "notif_2",
                    userId = "user_maya",
                    title = "Ownership Proof Requested",
                    body = "Liam requested verification for Navy Canvas Commuter Backpack.",
                    type = "VERIFICATION",
                    relatedId = "conv_backpack",
                    isRead = false,
                    createdAt = now - 2 * hour
                ),
                NotificationEntity(
                    id = "notif_3",
                    userId = "user_liam",
                    title = "96% Match at JFK Terminal 4",
                    body = "JFK T4 Lost & Found Desk logged a matching Silver Chronograph Wristwatch.",
                    type = "MATCH",
                    relatedId = "match_watch_1",
                    isRead = false,
                    createdAt = now - 20 * hour
                )
            )
            db.notificationDao().insertNotifications(notifications)

            val safetyReports = listOf(
                SafetyReportEntity(
                    id = "safety_1",
                    reporterUserId = "user_maya",
                    targetType = "USER",
                    targetId = "user_suspicious",
                    reason = "Advance Fee / Gift Card Request",
                    details = "Account asked for an upfront gift card before verifying item ownership.",
                    status = "OPEN",
                    createdAt = now - 4 * hour
                )
            )
            db.safetyReportDao().insertReports(safetyReports)

            val returnConfirmations = listOf(
                ReturnConfirmationEntity(
                    id = "ret_headphones",
                    conversationId = "conv_headphones",
                    lostReportId = "lost_headphones_maya",
                    foundReportId = "found_headphones_soho",
                    ownerUserId = "user_maya",
                    finderUserId = "user_liam",
                    handoverSpot = "Hotel Front Desk Lobby (CCTV Monitored)",
                    ownerConfirmed = true,
                    finderConfirmed = true,
                    completedAt = now - 2 * day
                )
            )
            db.returnConfirmationDao().insertConfirmations(returnConfirmations)
        }
    }
}
