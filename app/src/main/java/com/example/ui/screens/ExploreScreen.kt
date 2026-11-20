package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.db.FoundReportEntity
import com.example.data.db.GeoHubPresets
import com.example.data.db.ItemCategories
import com.example.data.db.LostReportEntity
import com.example.data.repo.MatchingEngine
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FindoraMapCanvas
import com.example.ui.components.FoundReportCard
import com.example.ui.components.LostReportCard
import com.example.ui.components.MapPinItem
import com.example.ui.components.SafetyBanner
import com.example.ui.state.DateWindowFilter
import com.example.ui.state.FindoraDashboardData
import com.example.ui.state.ReportTypeFilter
import com.example.ui.state.SearchFilters
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueTint
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateText

@Composable
fun ExploreScreen(
    data: FindoraDashboardData,
    filters: SearchFilters,
    onUpdateFilters: ((SearchFilters) -> SearchFilters) -> Unit,
    onSetApproxLocation: (label: String, lat: Double, lng: Double) -> Unit,
    onSelectLost: (LostReportEntity) -> Unit,
    onSelectFound: (FoundReportEntity) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var showMapView by remember { mutableStateOf(false) }
    var showHubSelector by remember { mutableStateOf(false) }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val coords = readCoarseDeviceLocation(context)
            if (coords != null) {
                onSetApproxLocation("Current Approx. Area", coords.first, coords.second)
            } else {
                onShowMessage("Location unavailable on device; select a city hub below.")
            }
        } else {
            onShowMessage("Location permission denied. Choose a city hub manually.")
        }
    }

    val now = System.currentTimeMillis()
    val filteredLost = remember(data.publicLost, filters, data.approxLocation) {
        if (filters.reportType == ReportTypeFilter.FOUND) emptyList()
        else data.publicLost.mapNotNull { r ->
            val dist = MatchingEngine.haversineKm(
                data.approxLocation.latitude, data.approxLocation.longitude,
                r.latitude, r.longitude
            )
            val matchesQuery = filters.query.isBlank() ||
                r.title.contains(filters.query, true) ||
                r.description.contains(filters.query, true) ||
                r.approxLocationName.contains(filters.query, true) ||
                r.city.contains(filters.query, true)
            val matchesCat = filters.category == "All" || r.category.equals(filters.category, true)
            val matchesDist = filters.maxDistanceKm == null || dist <= filters.maxDistanceKm
            val matchesDate = filters.dateWindow.days == null ||
                (now - r.dateLostMillis) <= filters.dateWindow.days * 86_400_000L
            if (matchesQuery && matchesCat && matchesDist && matchesDate) r to dist else null
        }.sortedBy { it.second }
    }

    val filteredFound = remember(data.publicFound, filters, data.approxLocation) {
        if (filters.reportType == ReportTypeFilter.LOST) emptyList()
        else data.publicFound.mapNotNull { r ->
            val dist = MatchingEngine.haversineKm(
                data.approxLocation.latitude, data.approxLocation.longitude,
                r.latitude, r.longitude
            )
            val matchesQuery = filters.query.isBlank() ||
                r.title.contains(filters.query, true) ||
                r.description.contains(filters.query, true) ||
                r.approxLocationName.contains(filters.query, true) ||
                r.city.contains(filters.query, true)
            val matchesCat = filters.category == "All" || r.category.equals(filters.category, true)
            val matchesDist = filters.maxDistanceKm == null || dist <= filters.maxDistanceKm
            val matchesDate = filters.dateWindow.days == null ||
                (now - r.dateFoundMillis) <= filters.dateWindow.days * 86_400_000L
            if (matchesQuery && matchesCat && matchesDist && matchesDate) r to dist else null
        }.sortedBy { it.second }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header + List/Map Switcher
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Explore & Map", style = MaterialTheme.typography.headlineMedium, color = DeepNavy)
                    Text(
                        text = "${filteredLost.size + filteredFound.size} reports within ${filters.maxDistanceKm?.toInt()?.toString() ?: "All"} km",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateMuted
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SlateSurface,
                    border = BorderStroke(1.dp, SlateBorder)
                ) {
                    Row(modifier = Modifier.padding(3.dp)) {
                        Surface(
                            modifier = Modifier
                                .testTag("explore_mode_list")
                                .clickable { showMapView = false },
                            shape = RoundedCornerShape(9.dp),
                            color = if (!showMapView) DeepNavy else PureWhite.copy(alpha = 0f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.FormatListBulleted,
                                    contentDescription = "List view",
                                    tint = if (!showMapView) PureWhite else SlateText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "List",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (!showMapView) PureWhite else SlateText
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier
                                .testTag("explore_mode_map")
                                .clickable { showMapView = true },
                            shape = RoundedCornerShape(9.dp),
                            color = if (showMapView) DeepNavy else PureWhite.copy(alpha = 0f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.Map,
                                    contentDescription = "Map view",
                                    tint = if (showMapView) PureWhite else SlateText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Map",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (showMapView) PureWhite else SlateText
                                )
                            }
                        }
                    }
                }
            }
        }

        // Keyword Search Input
        item {
            OutlinedTextField(
                value = filters.query,
                onValueChange = { q -> onUpdateFilters { it.copy(query = q) } },
                placeholder = { Text("Keyword, brand, station, city...") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = ElectricBlue) },
                trailingIcon = {
                    if (filters.query.isNotEmpty()) {
                        IconButton(onClick = { onUpdateFilters { it.copy(query = "") } }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("explore_search_input")
            )
        }

        // Location Anchor Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showHubSelector = !showHubSelector },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(data.approxLocation.label, maxLines = 1)
                }
                OutlinedButton(
                    onClick = { locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("explore_use_location_button")
                ) {
                    Icon(Icons.Outlined.MyLocation, contentDescription = "Use approximate device location", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("My Area")
                }
            }
        }

        if (showHubSelector) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(GeoHubPresets.hubs, key = { it.id }) { hub ->
                        FilterChip(
                            selected = data.approxLocation.label == hub.name,
                            onClick = {
                                onSetApproxLocation(hub.name, hub.latitude, hub.longitude)
                                showHubSelector = false
                            },
                            label = { Text("${hub.city}: ${hub.name}") }
                        )
                    }
                }
            }
        }

        // Report Type + Radius + Date Filters
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ReportTypeFilter.entries) { type ->
                    FilterChip(
                        selected = filters.reportType == type,
                        onClick = { onUpdateFilters { it.copy(reportType = type) } },
                        label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DeepNavy,
                            selectedLabelColor = PureWhite
                        )
                    )
                }
                val distances = listOf(5.0 to "5 km", 15.0 to "15 km", 50.0 to "50 km", null to "Global")
                items(distances) { (km, label) ->
                    FilterChip(
                        selected = filters.maxDistanceKm == km,
                        onClick = { onUpdateFilters { it.copy(maxDistanceKm = km) } },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricBlueTint,
                            selectedLabelColor = ElectricBlue
                        )
                    )
                }
                items(DateWindowFilter.entries) { dw ->
                    FilterChip(
                        selected = filters.dateWindow == dw,
                        onClick = { onUpdateFilters { it.copy(dateWindow = dw) } },
                        label = { Text(dw.label) }
                    )
                }
            }
        }

        // Category Chips
        item {
            val categories = listOf("All") + ItemCategories.all
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = filters.category == cat,
                        onClick = { onUpdateFilters { it.copy(category = cat) } },
                        label = { Text(cat) }
                    )
                }
            }
        }

        // Map View
        if (showMapView) {
            item {
                FindoraMapCanvas(
                    center = data.approxLocation,
                    radiusKm = filters.maxDistanceKm,
                    lostReports = filteredLost.map { it.first },
                    foundReports = filteredFound.map { it.first },
                    organizations = data.organizations,
                    onPinSelected = { pin ->
                        when (pin) {
                            is MapPinItem.LostPin -> onSelectLost(pin.report)
                            is MapPinItem.FoundPin -> onSelectFound(pin.report)
                            is MapPinItem.OrgPin -> {
                                onSetApproxLocation(pin.org.name, pin.org.latitude, pin.org.longitude)
                            }
                        }
                    }
                )
            }
            item {
                SafetyBanner(
                    text = "Privacy-first map: exact street addresses are never displayed publicly. Pins show approximate zones only."
                )
            }
        }

        // Results List
        if (filteredLost.isEmpty() && filteredFound.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No matching items found",
                    subtitle = "Try expanding your distance radius to Global or clearing keyword filters."
                )
            }
        } else {
            items(filteredLost, key = { "exp_lost_${it.first.id}" }) { (report, dist) ->
                LostReportCard(
                    report = report,
                    distanceKm = dist,
                    onClick = { onSelectLost(report) }
                )
            }
            items(filteredFound, key = { "exp_found_${it.first.id}" }) { (report, dist) ->
                FoundReportCard(
                    report = report,
                    distanceKm = dist,
                    onClick = { onSelectFound(report) }
                )
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@SuppressLint("MissingPermission")
private fun readCoarseDeviceLocation(context: Context): Pair<Double, Double>? {
    return runCatching {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            ?: lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
        loc?.let { it.latitude to it.longitude }
    }.getOrNull()
}
