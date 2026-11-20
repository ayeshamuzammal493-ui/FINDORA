package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.ConversationEntity
import com.example.data.db.MessageEntity
import com.example.data.db.MessageType
import com.example.data.db.SafeHandoverSpots
import com.example.data.db.UserEntity
import com.example.data.db.VerificationStatus
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SafetyReportDialog
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
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.SlateText

@Composable
fun MessagesInboxScreen(
    currentUser: UserEntity,
    data: FindoraDashboardData,
    onOpenConversation: (String) -> Unit
) {
    val usersById = remember(data.allUsers) { data.allUsers.associateBy { it.id } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Private Messages", style = MaterialTheme.typography.headlineMedium, color = DeepNavy)
            Text(
                text = "End-to-end private in-app relay. Phone numbers stay hidden.",
                style = MaterialTheme.typography.bodySmall,
                color = SlateMuted
            )
            Spacer(Modifier.height(6.dp))
        }

        if (data.conversations.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No active conversations",
                    subtitle = "Open any Lost or Found report and tap Contact Privately to start verifying ownership."
                )
            }
        } else {
            items(data.conversations, key = { it.id }) { conv ->
                val otherId = if (conv.ownerUserId == currentUser.id) conv.finderUserId else conv.ownerUserId
                val otherName = usersById[otherId]?.fullName ?: "FINDORA Member"
                val roleLabel = if (conv.ownerUserId == currentUser.id) "Finder" else "Owner"
                ConversationRowCard(
                    conv = conv,
                    otherName = "$otherName ($roleLabel)",
                    onClick = { onOpenConversation(conv.id) }
                )
            }
        }
    }
}

@Composable
private fun ConversationRowCard(
    conv: ConversationEntity,
    otherName: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("conversation_row_${conv.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(DeepNavy),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = otherName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = PureWhite
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conv.itemTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepNavy,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = formatRelativeDate(conv.lastMessageAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateMuted
                    )
                }
                Text(text = otherName, style = MaterialTheme.typography.bodySmall, color = ElectricBlue)
                Spacer(Modifier.height(3.dp))
                Text(
                    text = conv.lastMessagePreview,
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                VerificationPill(status = conv.verificationStatus, isBlocked = conv.isBlocked)
            }
        }
    }
}

@Composable
private fun VerificationPill(status: String, isBlocked: Boolean) {
    val (bg, fg, label) = when {
        isBlocked -> Triple(LostCrimsonSoft, LostCrimson, "BLOCKED")
        status == VerificationStatus.APPROVED -> Triple(FoundEmeraldSoft, FoundEmerald, "OWNERSHIP VERIFIED")
        status == VerificationStatus.SUBMITTED -> Triple(AmberAlertSoft, AmberAlert, "PROOF SUBMITTED")
        status == VerificationStatus.REQUESTED -> Triple(ElectricBlueTint, ElectricBlue, "PROOF REQUESTED")
        status == VerificationStatus.REJECTED -> Triple(LostCrimsonSoft, LostCrimson, "PROOF REJECTED")
        else -> Triple(SlateSurfaceVariant, SlateMuted, "OPEN CHAT")
    }
    Surface(color = bg, shape = RoundedCornerShape(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationDetailScreen(
    conversationId: String,
    currentUser: UserEntity,
    data: FindoraDashboardData,
    viewModel: FindoraViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val conv by viewModel.observeConversation(conversationId).collectAsStateWithLifecycle(initialValue = null)
    val messages by viewModel.observeMessages(conversationId).collectAsStateWithLifecycle(initialValue = emptyList())
    val returnConf by viewModel.observeReturnConfirmation(conversationId).collectAsStateWithLifecycle(initialValue = null)

    val usersById = remember(data.allUsers) { data.allUsers.associateBy { it.id } }
    val foundReport = remember(conv?.foundReportId, data.publicFound) {
        data.publicFound.firstOrNull { it.id == conv?.foundReportId }
    }

    var messageInput by remember { mutableStateOf("") }
    var verificationAnswerInput by remember { mutableStateOf("") }
    var customQuestionInput by remember { mutableStateOf("") }
    var showRequestQuestionDialog by remember { mutableStateOf(false) }
    var showReturnDialog by remember { mutableStateOf(false) }
    var showReportUserDialog by remember { mutableStateOf(false) }

    val currentConv = conv
    if (currentConv == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading conversation...")
        }
        return
    }

    val isOwner = currentUser.id == currentConv.ownerUserId
    val otherUserId = if (isOwner) currentConv.finderUserId else currentConv.ownerUserId
    val otherUser = usersById[otherUserId]

    Scaffold(
        containerColor = PureWhite,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentConv.itemTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = DeepNavy,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${otherUser?.fullName ?: "Member"} • ${if (isOwner) "Finder" else "Claimant"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = DeepNavy)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleBlockConversation(currentConv.id, currentUser.id) },
                        modifier = Modifier.testTag("chat_block_button")
                    ) {
                        Icon(
                            Icons.Outlined.Block,
                            contentDescription = if (currentConv.isBlocked) "Unblock user" else "Block user",
                            tint = if (currentConv.isBlocked) LostCrimson else SlateText
                        )
                    }
                    IconButton(
                        onClick = { showReportUserDialog = true },
                        modifier = Modifier.testTag("chat_report_user_button")
                    ) {
                        Icon(Icons.Outlined.Flag, contentDescription = "Report user", tint = LostCrimson)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PureWhite)
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PureWhite)
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                if (currentConv.isBlocked) {
                    Surface(
                        color = LostCrimsonSoft,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "This conversation is blocked. Unblock via top-right icon to resume.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LostCrimson,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = messageInput,
                            onValueChange = { messageInput = it },
                            placeholder = { Text("Write a safe private message...") },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_message_input")
                        )
                        IconButton(
                            onClick = {
                                if (messageInput.isNotBlank()) {
                                    viewModel.sendMessage(currentConv.id, currentUser.id, messageInput)
                                    messageInput = ""
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(ElectricBlue)
                                .testTag("chat_send_button")
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Send", tint = PureWhite)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SlateSurface),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Ownership Verification Card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PureWhite,
                    border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Lock, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Ownership Verification", style = MaterialTheme.typography.titleMedium, color = DeepNavy)
                            }
                            VerificationPill(currentConv.verificationStatus, currentConv.isBlocked)
                        }

                        if (!isOwner && foundReport != null) {
                            // Finder-only private hint (hidden from claimant)
                            Surface(color = ElectricBlueTint, shape = RoundedCornerShape(10.dp)) {
                                Text(
                                    text = "Private Finder Reference (Hidden from Claimant): ${foundReport.hiddenIdentifyingDetails}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DeepNavy,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        if (currentConv.verificationPrompt.isNotBlank()) {
                            Text(
                                text = "Question: ${currentConv.verificationPrompt}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = DeepNavy
                            )
                        }

                        if (currentConv.verificationAnswer.isNotBlank()) {
                            Text(
                                text = "Claimant Proof: ${currentConv.verificationAnswer}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateText
                            )
                        }

                        // Owner submits proof
                        if (isOwner && (currentConv.verificationStatus == VerificationStatus.REQUESTED || currentConv.verificationStatus == VerificationStatus.REJECTED)) {
                            OutlinedTextField(
                                value = verificationAnswerInput,
                                onValueChange = { verificationAnswerInput = it },
                                label = { Text("Enter private proof of ownership") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("chat_verification_answer_input")
                            )
                            Button(
                                onClick = {
                                    viewModel.submitVerificationAnswer(currentConv.id, currentUser.id, verificationAnswerInput)
                                    verificationAnswerInput = ""
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("chat_submit_proof_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                            ) {
                                Text("Submit Proof Privately")
                            }
                        }

                        // Finder requests or reviews proof
                        if (!isOwner) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (currentConv.verificationStatus == VerificationStatus.NONE || currentConv.verificationStatus == VerificationStatus.REJECTED) {
                                    OutlinedButton(
                                        onClick = { showRequestQuestionDialog = true },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Ask Verification Question")
                                    }
                                }
                                if (currentConv.verificationStatus == VerificationStatus.SUBMITTED) {
                                    Button(
                                        onClick = { viewModel.reviewVerification(currentConv.id, currentUser.id, true) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("chat_approve_proof_button"),
                                        colors = ButtonDefaults.buttonColors(containerColor = FoundEmerald)
                                    ) {
                                        Text("Approve Proof")
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.reviewVerification(currentConv.id, currentUser.id, false) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Reject", color = LostCrimson)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Safe Public Handover & Return Confirmation Card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PureWhite,
                    border = BorderStroke(1.dp, SlateBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Handshake, contentDescription = null, tint = DeepNavy, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Safe Public Handover & Return", style = MaterialTheme.typography.titleMedium, color = DeepNavy)
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(SafeHandoverSpots.recommendations) { spot ->
                                OutlinedButton(
                                    onClick = { viewModel.suggestHandoverSpot(currentConv.id, currentUser.id, spot) },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(spot.substringBefore(" ("), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        val ownerDone = returnConf?.ownerConfirmed == true
                        val finderDone = returnConf?.finderConfirmed == true
                        val userAlreadyConfirmed = if (isOwner) ownerDone else finderDone
                        if (ownerDone && finderDone) {
                            Surface(color = FoundEmeraldSoft, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = FoundEmerald)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Return completed at ${returnConf?.handoverSpot}", style = MaterialTheme.typography.bodySmall, color = DeepNavy)
                                }
                            }
                        } else {
                            Button(
                                onClick = { showReturnDialog = true },
                                enabled = !userAlreadyConfirmed,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("chat_confirm_return_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = DeepNavy)
                            ) {
                                Icon(Icons.Outlined.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (userAlreadyConfirmed) "You Confirmed • Waiting for Other Party"
                                    else "Confirm Safe Item Return"
                                )
                            }
                        }
                    }
                }
            }

            // Message Bubbles
            items(messages, key = { it.id }) { msg ->
                MessageBubble(
                    message = msg,
                    isMine = msg.senderId == currentUser.id
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    if (showRequestQuestionDialog) {
        AlertDialog(
            onDismissRequest = { showRequestQuestionDialog = false },
            title = { Text("Ask Verification Question") },
            text = {
                OutlinedTextField(
                    value = customQuestionInput,
                    onValueChange = { customQuestionInput = it },
                    label = { Text("Private detail only the owner knows") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.requestVerification(currentConv.id, currentUser.id, customQuestionInput)
                    customQuestionInput = ""
                    showRequestQuestionDialog = false
                }) { Text("Send Question") }
            },
            dismissButton = {
                TextButton(onClick = { showRequestQuestionDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showReturnDialog) {
        var selectedSpot by remember { mutableStateOf(SafeHandoverSpots.recommendations.first()) }
        AlertDialog(
            onDismissRequest = { showReturnDialog = false },
            title = { Text("Confirm Safe Handover") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select the public spot where this item was returned:", style = MaterialTheme.typography.bodySmall)
                    SafeHandoverSpots.recommendations.forEach { spot ->
                        val active = selectedSpot == spot
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSpot = spot },
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) ElectricBlueTint else SlateSurface,
                            border = BorderStroke(1.dp, if (active) ElectricBlue else SlateBorder)
                        ) {
                            Text(spot, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(10.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.confirmReturn(currentConv.id, currentUser.id, selectedSpot)
                    showReturnDialog = false
                }) { Text("Confirm Return") }
            },
            dismissButton = {
                TextButton(onClick = { showReturnDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showReportUserDialog) {
        SafetyReportDialog(
            targetLabel = otherUser?.fullName ?: "User",
            onDismiss = { showReportUserDialog = false },
            onSubmit = { reason, details ->
                viewModel.submitSafetyReport(currentUser.id, "USER", otherUserId, reason, details)
                showReportUserDialog = false
            }
        )
    }
}

@Composable
private fun MessageBubble(message: MessageEntity, isMine: Boolean) {
    val isSystem = message.messageType == MessageType.SYSTEM || message.messageType == MessageType.HANDOVER_SUGGESTION
    if (isSystem) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Surface(
                color = ElectricBlueTint,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SlateBorder)
            ) {
                Text(
                    text = message.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = DeepNavy,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 290.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMine) 16.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 16.dp
            ),
            color = if (isMine) DeepNavy else PureWhite,
            border = if (isMine) null else BorderStroke(1.dp, SlateBorder)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                if (message.messageType == MessageType.VERIFICATION_REQUEST || message.messageType == MessageType.VERIFICATION_ANSWER) {
                    Text(
                        text = if (message.messageType == MessageType.VERIFICATION_REQUEST) "VERIFICATION QUESTION" else "OWNERSHIP PROOF",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMine) ElectricBlueTint else ElectricBlue
                    )
                    Spacer(Modifier.height(3.dp))
                }
                Text(
                    text = message.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isMine) PureWhite else DeepNavy
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = formatRelativeDate(message.createdAt) + if (message.isFlagged) " • Flagged" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isMine) PureWhite.copy(alpha = 0.68f) else SlateMuted
                )
            }
        }
    }
}
