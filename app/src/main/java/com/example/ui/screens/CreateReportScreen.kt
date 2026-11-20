package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.db.FoundReportEntity
import com.example.data.db.GeoHubPresets
import com.example.data.db.ItemCategories
import com.example.data.db.LostReportEntity
import com.example.data.db.UserEntity
import com.example.ui.components.ItemPhoto
import com.example.ui.components.SafetyBanner
import com.example.ui.components.formatFullDate
import com.example.ui.state.FindoraViewModel
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.FoundEmerald
import com.example.ui.theme.LostCrimson
import com.example.ui.theme.LostCrimsonSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.SlateSurface
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReportScreen(
    isLostMode: Boolean,
    currentUser: UserEntity,
    editingLost: LostReportEntity? = null,
    editingFound: FoundReportEntity? = null,
    viewModel: FindoraViewModel,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf(editingLost?.title ?: editingFound?.title ?: "") }
    var category by remember {
        mutableStateOf(editingLost?.category ?: editingFound?.category ?: ItemCategories.all.first())
    }
    var description by remember { mutableStateOf(editingLost?.description ?: editingFound?.description ?: "") }
    var colorBrand by remember { mutableStateOf(editingLost?.colorBrand ?: "") }
    var dateMillis by remember {
        mutableLongStateOf(editingLost?.dateLostMillis ?: editingFound?.dateFoundMillis ?: System.currentTimeMillis())
    }
    var approxLocationName by remember {
        mutableStateOf(
            editingLost?.approxLocationName
                ?: editingFound?.approxLocationName
                ?: GeoHubPresets.hubs.first().name
        )
    }
    var city by remember { mutableStateOf(editingLost?.city ?: editingFound?.city ?: currentUser.city) }
    var latitude by remember {
        mutableStateOf(editingLost?.latitude ?: editingFound?.latitude ?: GeoHubPresets.hubs.first().latitude)
    }
    var longitude by remember {
        mutableStateOf(editingLost?.longitude ?: editingFound?.longitude ?: GeoHubPresets.hubs.first().longitude)
    }
    var photoUri by remember { mutableStateOf(editingLost?.photoUri ?: editingFound?.photoUri) }
    var contactPref by remember { mutableStateOf(editingLost?.contactPreference ?: "IN_APP_ONLY") }
    var rewardOffered by remember { mutableStateOf(editingLost?.rewardOffered ?: "") }
    var hiddenDetails by remember { mutableStateOf(editingFound?.hiddenIdentifyingDetails ?: "") }
    var verificationQuestion by remember { mutableStateOf(editingFound?.verificationQuestion ?: "") }

    var formError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var duplicateNotice by remember { mutableStateOf<String?>(null) }

    // Zero-permission Android Photo Picker (Google Play compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val localPath = viewModel.copyPickedPhoto(uri)
                if (localPath != null) {
                    photoUri = localPath
                } else {
                    formError = "Could not load selected image."
                }
            }
        }
    }

    fun performPublish() {
        formError = null
        if (isLostMode) {
            viewModel.saveLostReport(
                existingId = editingLost?.id,
                userId = currentUser.id,
                title = title,
                category = category,
                description = description,
                colorBrand = colorBrand,
                dateLostMillis = dateMillis,
                approxLocationName = approxLocationName,
                city = city,
                latitude = latitude,
                longitude = longitude,
                photoUri = photoUri,
                contactPreference = contactPref,
                rewardOffered = rewardOffered,
                onSuccess = onClose,
                onError = { formError = it }
            )
        } else {
            viewModel.saveFoundReport(
                existingId = editingFound?.id,
                userId = currentUser.id,
                organizationId = currentUser.organizationId,
                title = title,
                category = category,
                description = description,
                dateFoundMillis = dateMillis,
                approxLocationName = approxLocationName,
                city = city,
                latitude = latitude,
                longitude = longitude,
                photoUri = photoUri,
                hiddenDetails = hiddenDetails,
                verificationQuestion = verificationQuestion,
                onSuccess = onClose,
                onError = { formError = it }
            )
        }
    }

    Scaffold(
        containerColor = PureWhite,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            editingLost != null -> "Edit Lost Report"
                            editingFound != null -> "Edit Found Report"
                            isLostMode -> "Report Lost Item"
                            else -> "Report Found Item"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = DeepNavy
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = DeepNavy)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PureWhite)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SafetyBanner(
                text = if (isLostMode) {
                    "Never include your home address or phone number in public descriptions."
                } else {
                    "Keep unique serial numbers or inside contents in Hidden Identifying Details so only the true owner can verify."
                }
            )

            if (!formError.isNullOrBlank()) {
                Surface(
                    color = LostCrimsonSoft,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = formError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = LostCrimson,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Photo Picker Section
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SlateSurface,
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ItemPhoto(
                        photoUri = photoUri,
                        contentDescription = "Selected item photo",
                        modifier = Modifier.size(84.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Item Photograph", style = MaterialTheme.typography.titleMedium, color = DeepNavy)
                        Text(
                            text = if (isLostMode) "Upload a reference photo if available." else "Avoid photographing serial numbers publicly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateMuted
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.testTag("report_pick_photo_button")
                            ) {
                                Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Choose Photo")
                            }
                            if (photoUri != null) {
                                TextButton(onClick = { photoUri = null }) {
                                    Text("Remove", color = LostCrimson)
                                }
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Item Name *") },
                placeholder = { Text("e.g. Navy Commuter Backpack") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("report_title_input")
            )

            Text("Category *", style = MaterialTheme.typography.labelLarge, color = DeepNavy)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ItemCategories.all) { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat) }
                    )
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(if (isLostMode) "Description & Distinct Features *" else "Public Description (Exclude secret details) *") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("report_description_input")
            )

            if (isLostMode) {
                OutlinedTextField(
                    value = colorBrand,
                    onValueChange = { colorBrand = it },
                    label = { Text("Color / Brand (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Date Picker Trigger
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("${if (isLostMode) "Date Lost" else "Date Found"}: ${formatFullDate(dateMillis)}")
            }

            // Approximate Location & Hub Selector
            Text("Approximate Zone / Hub *", style = MaterialTheme.typography.labelLarge, color = DeepNavy)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(GeoHubPresets.hubs, key = { it.id }) { hub ->
                    FilterChip(
                        selected = approxLocationName == hub.name,
                        onClick = {
                            approxLocationName = hub.name
                            city = hub.city
                            latitude = hub.latitude
                            longitude = hub.longitude
                        },
                        label = { Text("${hub.city} • ${hub.name}") }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = approxLocationName,
                    onValueChange = { approxLocationName = it },
                    label = { Text("Approx. Area / Venue *") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag("report_location_input")
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("City *") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            if (isLostMode) {
                Text("Contact Preference", style = MaterialTheme.typography.labelLarge, color = DeepNavy)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "IN_APP_ONLY" to "In-App Chat",
                        "EMAIL_RELAY" to "Masked Email",
                        "ORG_DESK" to "Partner Desk"
                    ).forEach { (code, label) ->
                        FilterChip(
                            selected = contactPref == code,
                            onClick = { contactPref = code },
                            label = { Text(label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = rewardOffered,
                    onValueChange = { rewardOffered = it },
                    label = { Text("Optional Thank-You Reward (e.g. $25)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Found Item Protected Ownership Verification Fields
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SlateSurface,
                    border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Lock, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Private Ownership Verification (Never Public)",
                                style = MaterialTheme.typography.titleMedium,
                                color = DeepNavy
                            )
                        }
                        OutlinedTextField(
                            value = hiddenDetails,
                            onValueChange = { hiddenDetails = it },
                            label = { Text("Hidden Identifying Details *") },
                            placeholder = { Text("e.g. Initials on inside pocket, lockscreen wallpaper, last 4 of serial") },
                            minLines = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("report_hidden_details_input")
                        )
                        OutlinedTextField(
                            value = verificationQuestion,
                            onValueChange = { verificationQuestion = it },
                            label = { Text("Question to Ask Claimant *") },
                            placeholder = { Text("e.g. What custom engraving or item is inside the front pocket?") },
                            minLines = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("report_verification_question_input")
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Button(
                onClick = {
                    if (editingLost == null && editingFound == null) {
                        scope.launch {
                            val dupTitle = if (isLostMode) {
                                viewModel.checkDuplicateLost(currentUser.id, title, category)?.title
                            } else {
                                viewModel.checkDuplicateFound(currentUser.id, title, category)?.title
                            }
                            if (dupTitle != null) {
                                duplicateNotice = dupTitle
                            } else {
                                performPublish()
                            }
                        }
                    } else {
                        performPublish()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("report_publish_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLostMode) DeepNavy else FoundEmerald
                )
            ) {
                Text(
                    text = if (editingLost != null || editingFound != null) "Save Changes" else "Publish Report",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { dateMillis = it }
                    showDatePicker = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (duplicateNotice != null) {
        AlertDialog(
            onDismissRequest = { duplicateNotice = null },
            title = { Text("Possible Duplicate Report", color = DeepNavy) },
            text = {
                Text(
                    "You already have an active listing titled '$duplicateNotice' in $category. Publishing duplicate listings reduces matching quality. Publish anyway?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(onClick = {
                    duplicateNotice = null
                    performPublish()
                }) { Text("Publish Anyway") }
            },
            dismissButton = {
                TextButton(onClick = { duplicateNotice = null }) { Text("Review") }
            }
        )
    }
}
