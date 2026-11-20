package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.db.FoundReportEntity
import com.example.data.db.LostReportEntity
import com.example.data.db.OrganizationEntity
import com.example.data.db.ReportStatus
import com.example.data.db.UserEntity
import com.example.data.repo.MatchingEngine
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FoundReportCard
import com.example.ui.components.LostReportCard
import com.example.ui.components.formatRelativeDate
import com.example.ui.state.FindoraDashboardData
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.AmberAlertSoft
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueSoft
import com.example.ui.theme.ElectricBlueTint
import com.example.ui.theme.FoundEmerald
import com.example.ui.theme.LostCrimson
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    currentUser: UserEntity,
    data: FindoraDashboardData,
    onOpenSearch: () -> Unit,
    onReportLost: () -> Unit,
    onReportFound: () -> Unit,
    onSelectLost: (LostReportEntity) -> Unit,
    onSelectFound: (FoundReportEntity) -> Unit,
    onOpenMatches: () -> Unit,
    onMarkNotificationsRead: () -> Unit
) {
    var showNotifications by remember { mutableStateOf(false) }
    val unreadCount = remember(data.notifications) { data.notifications.count { !it.isRead } }

    val userLostIds = remember(data.publicLost, currentUser.id) {
        data.publicLost.filter { it.userId == currentUser.id }.map { it.id }.toSet()
    }
    val userFoundIds = remember(data.publicFound, currentUser.id) {
        data.publicFound.filter { it.userId == currentUser.id }.map { it.id }.toSet()
    }
    val myMatchesCount = remember(data.matches, userLostIds, userFoundIds) {
        data.matches.count {
            it.status != "DISMISSED" && (it.lostReportId in userLostIds || it.foundReportId in userFoundIds)
        }
    }

    val combinedNearby = remember(data.publicLost, data.publicFound, data.approxLocation) {
        val lostItems = data.publicLost
            .filter { it.status == ReportStatus.ACTIVE }
            .map {
                val dist = MatchingEngine.haversineKm(
                    data.approxLocation.latitude, data.approxLocation.longitude,
                    it.latitude, it.longitude
                )
                NearbyItem.Lost(it, dist)
            }
        val foundItems = data.publicFound
            .filter { it.status == ReportStatus.ACTIVE }
            .map {
                val dist = MatchingEngine.haversineKm(
                    data.approxLocation.latitude, data.approxLocation.longitude,
                    it.latitude, it.longitude
                )
                NearbyItem.Found(it, dist)
            }
        (lostItems + foundItems).sortedBy { it.distanceKm }.take(5)
    }

    val recentItems = remember(data.publicLost, data.publicFound, data.approxLocation) {
        val lostItems = data.publicLost.map {
            val dist = MatchingEngine.haversineKm(
                data.approxLocation.latitude, data.approxLocation.longitude,
                it.latitude, it.longitude
            )
            NearbyItem.Lost(it, dist)
        }
        val foundItems = data.publicFound.map {
            val dist = MatchingEngine.haversineKm(
                data.approxLocation.latitude, data.approxLocation.longitude,
                it.latitude, it.longitude
            )
            NearbyItem.Found(it, dist)
        }
        (lostItems + foundItems).sortedByDescending { it.createdAt }.take(8)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Brand Header + Notifications
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FINDORA",
                        style = MaterialTheme.typography.headlineMedium,
                        color = DeepNavy
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${data.approxLocation.label} • Hi, ${currentUser.fullName.substringBefore(" ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(
                    onClick = { showNotifications = true },
                    modifier = Modifier.testTag("home_notifications_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadCount > 0) {
                                Badge(containerColor = ElectricBlue) {
                                    Text(unreadCount.toString(), color = PureWhite)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = DeepNavy
                        )
                    }
                }
            }
        }

        // Search Bar Trigger
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_search_bar")
                    .clickable(onClick = onOpenSearch),
                shape = RoundedCornerShape(16.dp),
                color = SlateSurface,
                border = BorderStroke(1.dp, SlateBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = ElectricBlue
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Search keys, watches, laptops, hubs...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateMuted
                    )
                }
            }
        }

        // Report Lost / Report Found Hero Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionHeroCard(
                    title = "Report Lost",
                    subtitle = "Log missing item & auto-match",
                    containerColor = DeepNavy,
                    accentColor = LostCrimson,
                    onClick = onReportLost,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_report_lost_button")
                )
                ActionHeroCard(
                    title = "Report Found",
                    subtitle = "Verify owner privately",
                    containerColor = ElectricBlue,
                    accentColor = FoundEmerald,
                    onClick = onReportFound,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_report_found_button")
                )
            }
        }

        // Possible Matches Callout
        if (myMatchesCount > 0) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenMatches),
                    shape = RoundedCornerShape(16.dp),
                    color = AmberAlertSoft,
                    border = BorderStroke(1.dp, AmberAlert.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(PureWhite),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.TravelExplore, contentDescription = null, tint = AmberAlert)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$myMatchesCount Possible Match${if (myMatchesCount > 1) "es" else ""} Ready",
                                style = MaterialTheme.typography.titleMedium,
                                color = DeepNavy
                            )
                            Text(
                                text = "Matched by category, keywords & distance.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateText
                            )
                        }
                        Text(
                            text = "View",
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricBlue
                        )
                    }
                }
            }
        }

        // Nearby Reports
        item {
            SectionHeader(
                title = "Nearby Reports",
                actionLabel = "Map & Filters",
                onAction = onOpenSearch
            )
        }

        if (combinedNearby.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No active reports nearby",
                    subtitle = "Switch search areas or publish a report above."
                )
            }
        } else {
            items(combinedNearby, key = { "nearby_${it.key}" }) { item ->
                when (item) {
                    is NearbyItem.Lost -> LostReportCard(
                        report = item.report,
                        distanceKm = item.distanceKm,
                        onClick = { onSelectLost(item.report) }
                    )
                    is NearbyItem.Found -> FoundReportCard(
                        report = item.report,
                        distanceKm = item.distanceKm,
                        onClick = { onSelectFound(item.report) }
                    )
                }
            }
        }

        // Verified Partner Hubs (Airports, Universities, Hotels, Malls)
        if (data.organizations.isNotEmpty()) {
            item {
                SectionHeader(title = "Verified Lost & Found Hubs")
            }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(data.organizations, key = { it.id }) { org ->
                        OrganizationHubCard(org = org)
                    }
                }
            }
        }

        // Recently Posted
        item {
            SectionHeader(title = "Recently Posted")
        }

        items(recentItems, key = { "recent_${it.key}" }) { item ->
            when (item) {
                is NearbyItem.Lost -> LostReportCard(
                    report = item.report,
                    distanceKm = item.distanceKm,
                    onClick = { onSelectLost(item.report) }
                )
                is NearbyItem.Found -> FoundReportCard(
                    report = item.report,
                    distanceKm = item.distanceKm,
                    onClick = { onSelectFound(item.report) }
                )
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }

    if (showNotifications) {
        ModalBottomSheet(onDismissRequest = { showNotifications = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Notifications", style = MaterialTheme.typography.headlineSmall, color = DeepNavy)
                    if (unreadCount > 0) {
                        TextButton(onClick = onMarkNotificationsRead) {
                            Text("Mark all read")
                        }
                    }
                }
                if (data.notifications.isEmpty()) {
                    EmptyStateCard(
                        title = "All caught up",
                        subtitle = "Match alerts, messages, and verification updates appear here."
                    )
                } else {
                    data.notifications.take(12).forEach { notif ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (notif.isRead) SlateSurface else ElectricBlueTint,
                            border = BorderStroke(1.dp, if (notif.isRead) SlateBorder else ElectricBlueSoft),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = notif.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = DeepNavy
                                    )
                                    Text(
                                        text = formatRelativeDate(notif.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SlateMuted
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = notif.body,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateText
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private sealed interface NearbyItem {
    val key: String
    val distanceKm: Double
    val createdAt: Long

    data class Lost(val report: LostReportEntity, override val distanceKm: Double) : NearbyItem {
        override val key = "lost_${report.id}"
        override val createdAt = report.createdAt
    }

    data class Found(val report: FoundReportEntity, override val distanceKm: Double) : NearbyItem {
        override val key = "found_${report.id}"
        override val createdAt = report.createdAt
    }
}

@Composable
private fun ActionHeroCard(
    title: String,
    subtitle: String,
    containerColor: androidx.compose.ui.graphics.Color,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(PureWhite.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.AddCircleOutline,
                    contentDescription = null,
                    tint = PureWhite
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = PureWhite
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = PureWhite.copy(alpha = 0.82f)
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, color = DeepNavy)
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel, color = ElectricBlue)
            }
        }
    }
}

@Composable
private fun OrganizationHubCard(org: OrganizationEntity) {
    Surface(
        modifier = Modifier.width(220.dp),
        shape = RoundedCornerShape(16.dp),
        color = SlateSurface,
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Apartment,
                    contentDescription = null,
                    tint = DeepNavy,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = org.orgType,
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricBlue
                )
                Spacer(Modifier.weight(1f))
                if (org.verifiedBadge) {
                    Icon(
                        imageVector = Icons.Outlined.Verified,
                        contentDescription = "Verified partner desk",
                        tint = ElectricBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = org.name,
                style = MaterialTheme.typography.titleMedium,
                color = DeepNavy,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = org.contactDesk,
                style = MaterialTheme.typography.bodySmall,
                color = SlateText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
