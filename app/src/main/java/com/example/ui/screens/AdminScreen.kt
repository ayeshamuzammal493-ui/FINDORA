package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.db.ReportStatus
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.formatRelativeDate
import com.example.ui.state.FindoraDashboardData
import com.example.ui.state.FindoraViewModel
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.AmberAlertSoft
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueTint
import com.example.ui.theme.FoundEmerald
import com.example.ui.theme.FoundEmeraldSoft
import com.example.ui.theme.LostCrimson
import com.example.ui.theme.LostCrimsonSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    data: FindoraDashboardData,
    viewModel: FindoraViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var selectedTab by remember { mutableIntStateOf(0) }
    val usersById = remember(data.allUsers) { data.allUsers.associateBy { it.id } }

    Scaffold(
        containerColor = PureWhite,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Admin Moderation Panel", style = MaterialTheme.typography.titleLarge, color = DeepNavy)
                        Text("Trust, Safety & Platform Statistics", style = MaterialTheme.typography.bodySmall, color = SlateMuted)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = DeepNavy)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PureWhite)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(PureWhite),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Platform Statistics Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard("Users", data.allUsers.size.toString(), ElectricBlue, Modifier.weight(1f))
                        StatCard("Lost", data.adminAllLost.size.toString(), LostCrimson, Modifier.weight(1f))
                        StatCard("Found", data.adminAllFound.size.toString(), FoundEmerald, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard("Matches", data.matches.size.toString(), DeepNavy, Modifier.weight(1f))
                        StatCard(
                            "Returns",
                            data.returnConfirmations.count { it.ownerConfirmed && it.finderConfirmed }.toString(),
                            FoundEmerald,
                            Modifier.weight(1f)
                        )
                        StatCard(
                            "Open Flags",
                            data.safetyReports.count { it.status == "OPEN" }.toString(),
                            AmberAlert,
                            Modifier.weight(1f)
                        )
                    }
                }
            }

            // Moderation Tabs
            item {
                val tabs = listOf("Safety", "Listings", "Users/Orgs", "Messages")
                TabRow(selectedTabIndex = selectedTab, containerColor = PureWhite, contentColor = ElectricBlue) {
                    tabs.forEachIndexed { idx, label ->
                        Tab(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            text = { Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1) }
                        )
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    if (data.safetyReports.isEmpty()) {
                        item { EmptyStateCard("No safety reports", "Queue is clear.") }
                    } else {
                        items(data.safetyReports, key = { it.id }) { sr ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = SlateSurface,
                                border = BorderStroke(1.dp, SlateBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${sr.targetType} • ${sr.reason}", style = MaterialTheme.typography.titleMedium, color = DeepNavy)
                                        Surface(
                                            color = if (sr.status == "OPEN") AmberAlertSoft else FoundEmeraldSoft,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                sr.status,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (sr.status == "OPEN") AmberAlert else FoundEmerald,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                    Text(sr.details, style = MaterialTheme.typography.bodySmall, color = SlateText)
                                    Text(
                                        "Target ID: ${sr.targetId} • ${formatRelativeDate(sr.createdAt)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SlateMuted
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                viewModel.adminUpdateSafetyStatus(sr.id, "ACTION_TAKEN")
                                                if (sr.targetType == "USER") {
                                                    viewModel.adminSetUserSuspended(sr.targetId, true)
                                                } else if (sr.targetType == "LOST_REPORT") {
                                                    viewModel.adminToggleListingRemoval(true, sr.targetId, true)
                                                } else if (sr.targetType == "FOUND_REPORT") {
                                                    viewModel.adminToggleListingRemoval(false, sr.targetId, true)
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = LostCrimson),
                                            modifier = Modifier.weight(1f)
                                        ) { Text("Take Action") }
                                        OutlinedButton(
                                            onClick = { viewModel.adminUpdateSafetyStatus(sr.id, "DISMISSED") },
                                            modifier = Modifier.weight(1f)
                                        ) { Text("Dismiss") }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    items(data.adminAllLost, key = { "adm_l_${it.id}" }) { report ->
                        val removed = report.status == ReportStatus.REMOVED
                        AdminListingRow(
                            badge = "LOST • ${report.status}",
                            title = report.title,
                            subtitle = "${report.approxLocationName}, ${report.city}",
                            isRemoved = removed,
                            onToggleRemove = { viewModel.adminToggleListingRemoval(true, report.id, !removed) }
                        )
                    }
                    items(data.adminAllFound, key = { "adm_f_${it.id}" }) { report ->
                        val removed = report.status == ReportStatus.REMOVED
                        AdminListingRow(
                            badge = "FOUND • ${report.status}",
                            title = report.title,
                            subtitle = "${report.approxLocationName}, ${report.city}",
                            isRemoved = removed,
                            onToggleRemove = { viewModel.adminToggleListingRemoval(false, report.id, !removed) }
                        )
                    }
                }
                2 -> {
                    items(data.allUsers, key = { "adm_u_${it.id}" }) { user ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SlateSurface,
                            border = BorderStroke(1.dp, SlateBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${user.fullName} (${user.role})", style = MaterialTheme.typography.titleMedium, color = DeepNavy)
                                    Text("${user.email} • ${user.planTier}", style = MaterialTheme.typography.bodySmall, color = SlateText)
                                }
                                if (user.role != "ADMIN") {
                                    OutlinedButton(
                                        onClick = { viewModel.adminSetUserSuspended(user.id, !user.isSuspended) },
                                        modifier = Modifier.testTag("admin_toggle_user_${user.id}")
                                    ) {
                                        Text(
                                            text = if (user.isSuspended) "Reinstate" else "Suspend",
                                            color = if (user.isSuspended) FoundEmerald else LostCrimson
                                        )
                                    }
                                }
                            }
                        }
                    }
                    items(data.organizations, key = { "adm_o_${it.id}" }) { org ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ElectricBlueTint,
                            border = BorderStroke(1.dp, SlateBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${org.name} (${org.orgType})", style = MaterialTheme.typography.titleMedium, color = DeepNavy)
                                    Text("${org.city} • ${org.contactDesk}", style = MaterialTheme.typography.bodySmall, color = SlateText)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.adminToggleOrgVerified(org.id, !org.verifiedBadge) }
                                ) {
                                    Text(if (org.verifiedBadge) "Verified" else "Verify")
                                }
                            }
                        }
                    }
                }
                3 -> {
                    val msgs = if (data.flaggedMessages.isNotEmpty()) data.flaggedMessages else data.recentMessages
                    if (msgs.isEmpty()) {
                        item { EmptyStateCard("No messages to moderate", "Flagged messages appear here automatically.") }
                    } else {
                        items(msgs, key = { "adm_m_${it.id}" }) { msg ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (msg.isFlagged) LostCrimsonSoft else SlateSurface,
                                border = BorderStroke(1.dp, SlateBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "${usersById[msg.senderId]?.fullName ?: msg.senderId} • ${if (msg.isFlagged) "FLAGGED" else "RECENT"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (msg.isFlagged) LostCrimson else DeepNavy
                                    )
                                    Text(msg.body, style = MaterialTheme.typography.bodyMedium, color = DeepNavy)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { viewModel.adminModerateMessage(msg.id, delete = true) },
                                            colors = ButtonDefaults.buttonColors(containerColor = LostCrimson)
                                        ) { Text("Remove Message") }
                                        if (msg.isFlagged) {
                                            OutlinedButton(
                                                onClick = { viewModel.adminModerateMessage(msg.id, delete = false) }
                                            ) { Text("Clear Flag") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, accent: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = SlateSurface,
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineSmall, color = accent)
            Text(label, style = MaterialTheme.typography.labelSmall, color = SlateMuted)
        }
    }
}

@Composable
private fun AdminListingRow(
    badge: String,
    title: String,
    subtitle: String,
    isRemoved: Boolean,
    onToggleRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isRemoved) LostCrimsonSoft.copy(alpha = 0.4f) else SlateSurface,
        border = BorderStroke(1.dp, SlateBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(badge, style = MaterialTheme.typography.labelSmall, color = ElectricBlue)
                Text(title, style = MaterialTheme.typography.titleMedium, color = DeepNavy)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SlateText)
            }
            OutlinedButton(onClick = onToggleRemove) {
                Text(
                    text = if (isRemoved) "Restore" else "Remove",
                    color = if (isRemoved) FoundEmerald else LostCrimson
                )
            }
        }
    }
}
