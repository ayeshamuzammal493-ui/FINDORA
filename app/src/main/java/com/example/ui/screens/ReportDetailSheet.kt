package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.db.FoundReportEntity
import com.example.data.db.LostReportEntity
import com.example.data.db.SafeHandoverSpots
import com.example.data.db.UserEntity
import com.example.ui.components.ItemPhoto
import com.example.ui.components.ReportTypeBadge
import com.example.ui.components.SafetyBanner
import com.example.ui.components.SafetyReportDialog
import com.example.ui.components.formatFullDate
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueTint
import com.example.ui.theme.LostCrimson
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailSheet(
    currentUser: UserEntity,
    lostReport: LostReportEntity?,
    foundReport: FoundReportEntity?,
    onDismiss: () -> Unit,
    onStartChat: () -> Unit,
    onSubmitSafetyReport: (targetType: String, targetId: String, reason: String, details: String) -> Unit
) {
    val isLost = lostReport != null
    val title = lostReport?.title ?: foundReport?.title ?: return
    val category = lostReport?.category ?: foundReport?.category ?: ""
    val description = lostReport?.description ?: foundReport?.description ?: ""
    val approxLoc = lostReport?.approxLocationName ?: foundReport?.approxLocationName ?: ""
    val city = lostReport?.city ?: foundReport?.city ?: ""
    val dateMillis = lostReport?.dateLostMillis ?: foundReport?.dateFoundMillis ?: 0L
    val photoUri = lostReport?.photoUri ?: foundReport?.photoUri
    val status = lostReport?.status ?: foundReport?.status ?: ""
    val ownerId = lostReport?.userId ?: foundReport?.userId ?: ""

    var showReportDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = PureWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ItemPhoto(
                photoUri = photoUri,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportTypeBadge(isLost = isLost, status = status)
                Surface(color = ElectricBlueTint, shape = RoundedCornerShape(8.dp)) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricBlue,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(text = title, style = MaterialTheme.typography.headlineSmall, color = DeepNavy)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "$approxLoc, $city (~500m privacy zone)",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateText
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = SlateMuted, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${if (isLost) "Lost on" else "Found on"} ${formatFullDate(dateMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateMuted
                )
            }

            Text(text = description, style = MaterialTheme.typography.bodyMedium, color = DeepNavy)

            if (lostReport != null && lostReport.rewardOffered.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ElectricBlueTint,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Thank-you reward offered: ${lostReport.rewardOffered}",
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricBlue,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Crucial privacy protection: NEVER reveal hiddenIdentifyingDetails publicly!
            if (foundReport != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SlateSurface,
                    border = BorderStroke(1.dp, SlateBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Lock, contentDescription = null, tint = DeepNavy, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Ownership Verification Protected",
                                style = MaterialTheme.typography.titleMedium,
                                color = DeepNavy
                            )
                            Text(
                                text = if (ownerId == currentUser.id) {
                                    "Your hidden details: ${foundReport.hiddenIdentifyingDetails}"
                                } else {
                                    "Identifying details are hidden. Tap below to answer the finder's private verification question."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateText
                            )
                        }
                    }
                }
            }

            SafetyBanner(
                text = "Recommended handover: ${SafeHandoverSpots.recommendations.first()}"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (ownerId != currentUser.id) {
                    Button(
                        onClick = onStartChat,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("detail_contact_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (isLost) "I Found This" else "Claim & Verify")
                    }
                }
                OutlinedButton(
                    onClick = { showReportDialog = true },
                    modifier = Modifier
                        .height(50.dp)
                        .testTag("detail_report_safety_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, LostCrimson.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Outlined.Flag, contentDescription = "Report listing", tint = LostCrimson, modifier = Modifier.size(18.dp))
                    if (ownerId == currentUser.id) {
                        Spacer(Modifier.width(6.dp))
                        Text("Flag Issue", color = LostCrimson)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showReportDialog) {
        SafetyReportDialog(
            targetLabel = title,
            onDismiss = { showReportDialog = false },
            onSubmit = { reason, details ->
                val targetType = if (isLost) "LOST_REPORT" else "FOUND_REPORT"
                val targetId = lostReport?.id ?: foundReport?.id ?: ""
                onSubmitSafetyReport(targetType, targetId, reason, details)
                showReportDialog = false
                onDismiss()
            }
        )
    }
}
