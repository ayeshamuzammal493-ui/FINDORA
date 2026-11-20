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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.db.FoundReportEntity
import com.example.data.db.LostReportEntity
import com.example.data.db.PlanTier
import com.example.data.db.UserEntity
import com.example.data.db.UserRole
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FoundReportCard
import com.example.ui.components.LostReportCard
import com.example.ui.components.formatFullDate
import com.example.ui.state.FindoraDashboardData
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueSoft
import com.example.ui.theme.ElectricBlueTint
import com.example.ui.theme.FoundEmerald
import com.example.ui.theme.FoundEmeraldSoft
import com.example.ui.theme.LostCrimson
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateText

@Composable
fun DashboardScreen(
    currentUser: UserEntity,
    data: FindoraDashboardData,
    onEditLost: (LostReportEntity) -> Unit,
    onDeleteLost: (String) -> Unit,
    onEditFound: (FoundReportEntity) -> Unit,
    onDeleteFound: (String) -> Unit,
    onOpenMatchChat: (LostReportEntity?, FoundReportEntity?) -> Unit,
    onDismissMatch: (String) -> Unit,
    onUpdateProfile: (fullName: String, city: String, bio: String) -> Unit,
    onUpgradePlan: (String) -> Unit,
    onOpenAdminPanel: () -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showEditProfile by remember { mutableStateOf(false) }
    var showPlanSheet by remember { mutableStateOf(false) }

    val myLost = remember(data.publicLost, currentUser.id) {
        data.publicLost.filter { it.userId == currentUser.id }
    }
    val myFound = remember(data.publicFound, currentUser.id) {
        data.publicFound.filter { it.userId == currentUser.id }
    }
    val myLostById = remember(data.publicLost) { data.publicLost.associateBy { it.id } }
    val myFoundById = remember(data.publicFound) { data.publicFound.associateBy { it.id } }

    val myMatches = remember(data.matches, myLost, myFound) {
        val lostIds = myLost.map { it.id }.toSet()
        val foundIds = myFound.map { it.id }.toSet()
        data.matches.filter {
            it.status != "DISMISSED" && (it.lostReportId in lostIds || it.foundReportId in foundIds)
        }
    }
    val myReturns = remember(data.returnConfirmations, currentUser.id) {
        data.returnConfirmations.filter {
            it.ownerConfirmed && it.finderConfirmed &&
                (it.ownerUserId == currentUser.id || it.finderUserId == currentUser.id)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Profile Header Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = DeepNavy)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(ElectricBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentUser.fullName.take(1).uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                color = PureWhite
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(currentUser.fullName, style = MaterialTheme.typography.titleLarge, color = PureWhite)
                            Text(
                                text = "${currentUser.email} • ${currentUser.city}",
                                style = MaterialTheme.typography.bodySmall,
                                color = PureWhite.copy(alpha = 0.78f)
                            )
                        }
                        IconButton(onClick = { showEditProfile = true }, modifier = Modifier.testTag("dashboard_edit_profile_button")) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit profile", tint = PureWhite)
                        }
                        IconButton(onClick = onLogout, modifier = Modifier.testTag("dashboard_logout_button")) {
                            Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "Log out", tint = PureWhite)
                        }
                    }

                    if (currentUser.bio.isNotBlank()) {
                        Text(
                            text = currentUser.bio,
                            style = MaterialTheme.typography.bodySmall,
                            color = PureWhite.copy(alpha = 0.85f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = PureWhite.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showPlanSheet = true }
                                .testTag("dashboard_plan_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.WorkspacePremium, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Plan: ${currentUser.planTier}", style = MaterialTheme.typography.labelMedium, color = PureWhite)
                            }
                        }

                        if (currentUser.role == UserRole.ADMIN) {
                            Button(
                                onClick = onOpenAdminPanel,
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("dashboard_admin_button")
                            ) {
                                Icon(Icons.Outlined.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Admin Panel")
                            }
                        }
                    }
                }
            }
        }

        // Tabs: My Lost | My Found | Matches | Returns
        item {
            val tabs = listOf(
                "Lost (${myLost.size})",
                "Found (${myFound.size})",
                "Matches (${myMatches.size})",
                "Returns (${myReturns.size})"
            )
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = PureWhite,
                contentColor = ElectricBlue
            ) {
                tabs.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(title, style = MaterialTheme.typography.labelMedium, maxLines = 1) }
                    )
                }
            }
        }

        when (selectedTab) {
            0 -> {
                if (myLost.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "No lost reports yet",
                            subtitle = "Your published lost items will appear here for easy editing and tracking."
                        )
                    }
                } else {
                    items(myLost, key = { it.id }) { report ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LostReportCard(report = report, distanceKm = null, onClick = { onEditLost(report) })
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { onEditLost(report) }) {
                                    Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Edit")
                                }
                                TextButton(onClick = { onDeleteLost(report.id) }) {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = LostCrimson, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Delete", color = LostCrimson)
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                if (myFound.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "No found reports yet",
                            subtitle = "Items you report as found will appear here."
                        )
                    }
                } else {
                    items(myFound, key = { it.id }) { report ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            FoundReportCard(report = report, distanceKm = null, onClick = { onEditFound(report) })
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { onEditFound(report) }) {
                                    Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Edit")
                                }
                                TextButton(onClick = { onDeleteFound(report.id) }) {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = LostCrimson, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Delete", color = LostCrimson)
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                if (myMatches.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "No possible matches right now",
                            subtitle = "FINDORA checks category, keywords, dates, and distance automatically when new items arrive."
                        )
                    }
                } else {
                    items(myMatches, key = { it.id }) { match ->
                        val lost = myLostById[match.lostReportId]
                        val found = myFoundById[match.foundReportId]
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SlateSurface,
                            border = BorderStroke(1.dp, SlateBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(color = ElectricBlueSoft, shape = RoundedCornerShape(8.dp)) {
                                        Text(
                                            text = "${match.score}% MATCH",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ElectricBlue,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Text(
                                        text = String.format("%.1f km", match.distanceKm),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SlateMuted
                                    )
                                }
                                Text(
                                    text = "${lost?.title ?: "Lost item"} ↔ ${found?.title ?: "Found item"}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = DeepNavy
                                )
                                Text(
                                    text = match.reasonSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateText
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { onOpenMatchChat(lost, found) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                                    ) {
                                        Text("Message Privately")
                                    }
                                    OutlinedButton(onClick = { onDismissMatch(match.id) }) {
                                        Text("Dismiss")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            3 -> {
                if (myReturns.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "No completed returns yet",
                            subtitle = "Confirmed dual-party handovers appear here as your verified return history."
                        )
                    }
                } else {
                    items(myReturns, key = { it.id }) { ret ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = FoundEmeraldSoft,
                            border = BorderStroke(1.dp, FoundEmerald.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = FoundEmerald)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Verified Return Completed",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = DeepNavy
                                    )
                                    Text(
                                        text = "Handover spot: ${ret.handoverSpot}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SlateText
                                    )
                                    ret.completedAt?.let {
                                        Text(
                                            text = "Completed ${formatFullDate(it)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SlateMuted
                                        )
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

    if (showEditProfile) {
        var nameInput by remember { mutableStateOf(currentUser.fullName) }
        var cityInput by remember { mutableStateOf(currentUser.city) }
        var bioInput by remember { mutableStateOf(currentUser.bio) }
        AlertDialog(
            onDismissRequest = { showEditProfile = false },
            title = { Text("Edit Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text("Full Name") })
                    OutlinedTextField(value = cityInput, onValueChange = { cityInput = it }, label = { Text("City") })
                    OutlinedTextField(value = bioInput, onValueChange = { bioInput = it }, label = { Text("Short Bio") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateProfile(nameInput, cityInput, bioInput)
                    showEditProfile = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfile = false }) { Text("Cancel") }
            }
        )
    }

    if (showPlanSheet) {
        AlertDialog(
            onDismissRequest = { showPlanSheet = false },
            title = { Text("FINDORA Account Plans") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PlanOptionCard(
                        title = "Free Account ($0)",
                        desc = "Unlimited lost/found reporting, private messaging, and matching.",
                        active = currentUser.planTier == PlanTier.FREE,
                        onClick = { onUpgradePlan(PlanTier.FREE); showPlanSheet = false }
                    )
                    PlanOptionCard(
                        title = "Premium Individual ($4.99/mo)",
                        desc = "Extended 200 km instant radius alerts & priority verification badge.",
                        active = currentUser.planTier == PlanTier.PREMIUM,
                        onClick = { onUpgradePlan(PlanTier.PREMIUM); showPlanSheet = false }
                    )
                    PlanOptionCard(
                        title = "Partner Organization ($49/mo)",
                        desc = "For Airports, Universities, Hotels & Malls managing high-volume desks.",
                        active = currentUser.planTier == PlanTier.ORGANIZATION,
                        onClick = { onUpgradePlan(PlanTier.ORGANIZATION); showPlanSheet = false }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlanSheet = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun PlanOptionCard(title: String, desc: String, active: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (active) ElectricBlueTint else SlateSurface,
        border = BorderStroke(1.dp, if (active) ElectricBlue else SlateBorder)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = DeepNavy)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = SlateText)
        }
    }
}
