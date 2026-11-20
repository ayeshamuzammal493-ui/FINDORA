package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.db.FoundReportEntity
import com.example.data.db.LostReportEntity
import com.example.data.db.ReportStatus
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.AmberAlertSoft
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueSoft
import com.example.ui.theme.ElectricBlueTint
import com.example.ui.theme.FoundEmerald
import com.example.ui.theme.FoundEmeraldSoft
import com.example.ui.theme.LostCrimson
import com.example.ui.theme.LostCrimsonSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.SlateText
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatRelativeDate(millis: Long): String {
    val diff = System.currentTimeMillis() - millis
    val hours = diff / 3_600_000L
    return when {
        hours < 1L -> "Just now"
        hours < 24L -> "${hours}h ago"
        hours < 48L -> "Yesterday"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(millis))
    }
}

fun formatFullDate(millis: Long): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(millis))

@Composable
fun ItemPhoto(
    photoUri: String?,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val resId = when (photoUri) {
        "drawable://img_item_backpack" -> R.drawable.img_item_backpack
        "drawable://img_item_headphones" -> R.drawable.img_item_headphones
        "drawable://img_item_watch" -> R.drawable.img_item_watch
        "drawable://img_hero_lost_found" -> R.drawable.img_hero_lost_found
        else -> null
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SlateSurfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            resId != null -> {
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            !photoUri.isNullOrBlank() -> {
                val model: Any = if (photoUri.startsWith("/")) File(photoUri) else photoUri
                AsyncImage(
                    model = model,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Inventory2,
                        contentDescription = null,
                        tint = SlateMuted,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "No Photo",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateMuted
                    )
                }
            }
        }
    }
}

@Composable
fun ReportTypeBadge(isLost: Boolean, status: String) {
    val (bg, fg, label) = when {
        status == ReportStatus.RETURNED -> Triple(ElectricBlueSoft, ElectricBlue, "RETURNED")
        status == ReportStatus.MATCHED -> Triple(AmberAlertSoft, AmberAlert, "MATCHED")
        status == ReportStatus.REMOVED -> Triple(SlateSurfaceVariant, SlateMuted, "REMOVED")
        isLost -> Triple(LostCrimsonSoft, LostCrimson, "LOST")
        else -> Triple(FoundEmeraldSoft, FoundEmerald, "FOUND")
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun LostReportCard(
    report: LostReportEntity,
    distanceKm: Double?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ReportItemCard(
        title = report.title,
        category = report.category,
        approxLocation = report.approxLocationName,
        city = report.city,
        timestampMillis = report.dateLostMillis,
        photoUri = report.photoUri,
        isLost = true,
        status = report.status,
        distanceKm = distanceKm,
        hasPrivacyShield = false,
        rewardBadge = report.rewardOffered.takeIf { it.isNotBlank() },
        onClick = onClick,
        modifier = modifier.testTag("lost_card_${report.id}")
    )
}

@Composable
fun FoundReportCard(
    report: FoundReportEntity,
    distanceKm: Double?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ReportItemCard(
        title = report.title,
        category = report.category,
        approxLocation = report.approxLocationName,
        city = report.city,
        timestampMillis = report.dateFoundMillis,
        photoUri = report.photoUri,
        isLost = false,
        status = report.status,
        distanceKm = distanceKm,
        hasPrivacyShield = true,
        rewardBadge = if (report.organizationId != null) "Partner Desk" else null,
        onClick = onClick,
        modifier = modifier.testTag("found_card_${report.id}")
    )
}

@Composable
private fun ReportItemCard(
    title: String,
    category: String,
    approxLocation: String,
    city: String,
    timestampMillis: Long,
    photoUri: String?,
    isLost: Boolean,
    status: String,
    distanceKm: Double?,
    hasPrivacyShield: Boolean,
    rewardBadge: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, SlateBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ItemPhoto(
                photoUri = photoUri,
                contentDescription = title,
                modifier = Modifier.size(86.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ReportTypeBadge(isLost = isLost, status = status)
                    if (rewardBadge != null) {
                        Surface(
                            color = ElectricBlueTint,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = rewardBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricBlue,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                maxLines = 1
                            )
                        }
                    }
                    if (hasPrivacyShield) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Hidden details protected",
                            tint = DeepNavy,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = DeepNavy,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = category,
                    style = MaterialTheme.typography.bodySmall,
                    color = ElectricBlue
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = SlateMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "$approxLocation • $city",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = SlateMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = formatRelativeDate(timestampMillis),
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateMuted
                        )
                    }
                    if (distanceKm != null) {
                        Text(
                            text = if (distanceKm < 1.0) "~${(distanceKm * 1000).toInt()}m zone" else String.format("~%.1f km", distanceKm),
                            style = MaterialTheme.typography.labelSmall,
                            color = DeepNavy
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SafetyBanner(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = ElectricBlueTint,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, ElectricBlueSoft)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(ElectricBlueSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.VerifiedUser,
                    contentDescription = null,
                    tint = ElectricBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = DeepNavy
            )
        }
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SlateSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Explore,
                contentDescription = null,
                tint = ElectricBlue,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = DeepNavy
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = SlateMuted
            )
        }
    }
}

@Composable
fun SafetyReportDialog(
    targetLabel: String,
    onDismiss: () -> Unit,
    onSubmit: (reason: String, details: String) -> Unit
) {
    val reasons = listOf(
        "Suspected Fraud / Scam",
        "Advance Payment / Gift Card Request",
        "Duplicate or Spam Listing",
        "Harassment or Unsafe Behavior",
        "Exposes Personal Address / Phone"
    )
    var selectedReason by remember { mutableStateOf(reasons.first()) }
    var details by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Report $targetLabel",
                style = MaterialTheme.typography.titleLarge,
                color = DeepNavy
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Help Trust & Safety keep FINDORA safe. Moderators review every report.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateText
                )
                reasons.forEach { reason ->
                    val selected = selectedReason == reason
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = reason },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) ElectricBlueTint else SlateSurface,
                        border = BorderStroke(1.dp, if (selected) ElectricBlue else SlateBorder)
                    ) {
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = DeepNavy,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Additional details (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedReason, details) },
                colors = ButtonDefaults.buttonColors(containerColor = LostCrimson)
            ) {
                Text("Submit Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
