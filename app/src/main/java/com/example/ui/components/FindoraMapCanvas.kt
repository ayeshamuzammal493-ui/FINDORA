package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.db.FoundReportEntity
import com.example.data.db.LostReportEntity
import com.example.data.db.OrganizationEntity
import com.example.data.prefs.ApproxLocation
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.FoundEmerald
import com.example.ui.theme.LostCrimson
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.SlateSurface
import kotlin.math.hypot

sealed interface MapPinItem {
    val id: String
    val label: String
    val latitude: Double
    val longitude: Double

    data class LostPin(val report: LostReportEntity) : MapPinItem {
        override val id: String = report.id
        override val label: String = report.title
        override val latitude: Double = report.latitude
        override val longitude: Double = report.longitude
    }

    data class FoundPin(val report: FoundReportEntity) : MapPinItem {
        override val id: String = report.id
        override val label: String = report.title
        override val latitude: Double = report.latitude
        override val longitude: Double = report.longitude
    }

    data class OrgPin(val org: OrganizationEntity) : MapPinItem {
        override val id: String = org.id
        override val label: String = org.name
        override val latitude: Double = org.latitude
        override val longitude: Double = org.longitude
    }
}

/**
 * Interactive privacy-preserving vector map canvas.
 * Never reveals exact street pins publicly: renders approximate zone halos (~500m radius).
 */
@Composable
fun FindoraMapCanvas(
    center: ApproxLocation,
    radiusKm: Double?,
    lostReports: List<LostReportEntity>,
    foundReports: List<FoundReportEntity>,
    organizations: List<OrganizationEntity>,
    onPinSelected: (MapPinItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var zoomLevel by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val pins = remember(lostReports, foundReports, organizations) {
        buildList {
            organizations.forEach { add(MapPinItem.OrgPin(it)) }
            lostReports.forEach { add(MapPinItem.LostPin(it)) }
            foundReports.forEach { add(MapPinItem.FoundPin(it)) }
        }
    }

    // Deterministic slight privacy jitter (+/- ~180m) so public coordinates are never pinpoint exact
    fun projectToScreen(lat: Double, lng: Double, width: Float, height: Float, seed: String): Offset {
        val jitterLat = ((seed.hashCode() % 11) - 5) * 0.0004
        val jitterLng = (((seed.hashCode() / 13) % 11) - 5) * 0.0004
        val dLat = (lat + jitterLat) - center.latitude
        val dLng = (lng + jitterLng) - center.longitude
        val scale = (minOf(width, height) * 1.4f) * zoomLevel
        val x = width / 2f + (dLng * scale * 2.1).toFloat() + panOffset.x
        val y = height / 2f - (dLat * scale * 2.7).toFloat() + panOffset.y
        return Offset(x, y)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(340.dp),
        shape = RoundedCornerShape(20.dp),
        color = SlateSurface,
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("findora_map_canvas")
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            panOffset += dragAmount
                        }
                    }
                    .pointerInput(pins, center, zoomLevel, panOffset) {
                        detectTapGestures { tapOffset ->
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            val hit = pins.reversed().firstOrNull { pin ->
                                val pos = projectToScreen(pin.latitude, pin.longitude, w, h, pin.id)
                                hypot(tapOffset.x - pos.x, tapOffset.y - pos.y) <= 44f
                            }
                            if (hit != null) onPinSelected(hit)
                        }
                    }
            ) {
                val w = size.width
                val h = size.height

                // Soft cartographic backdrop & street grid
                drawRect(color = Color(0xFFF3F7FC))
                val gridStep = 48f * zoomLevel.coerceIn(0.7f, 1.8f)
                val gridColor = Color(0xFFE1EAF5)
                var gx = (panOffset.x % gridStep)
                while (gx < w) {
                    drawLine(gridColor, Offset(gx, 0f), Offset(gx, h), strokeWidth = 1.2f)
                    gx += gridStep
                }
                var gy = (panOffset.y % gridStep)
                while (gy < h) {
                    drawLine(gridColor, Offset(0f, gy), Offset(w, gy), strokeWidth = 1.2f)
                    gy += gridStep
                }

                // Stylized river / avenue diagonal contour
                drawLine(
                    color = Color(0xFFDCE8FA),
                    start = Offset(0f, h * 0.78f + panOffset.y * 0.3f),
                    end = Offset(w, h * 0.28f + panOffset.y * 0.3f),
                    strokeWidth = 26f
                )

                // Active search area radius circle
                val centerPos = Offset(w / 2f + panOffset.x, h / 2f + panOffset.y)
                val radiusPx = ((radiusKm ?: 50.0) * 3.8 * zoomLevel).toFloat().coerceIn(40f, w * 0.9f)
                drawCircle(
                    color = ElectricBlue.copy(alpha = 0.06f),
                    radius = radiusPx,
                    center = centerPos
                )
                drawCircle(
                    color = ElectricBlue.copy(alpha = 0.45f),
                    radius = radiusPx,
                    center = centerPos,
                    style = Stroke(
                        width = 2.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
                    )
                )

                // Center user search anchor
                drawCircle(color = PureWhite, radius = 12f, center = centerPos)
                drawCircle(color = ElectricBlue, radius = 8f, center = centerPos)

                // Render approximate privacy-zone pins
                pins.forEach { pin ->
                    val pos = projectToScreen(pin.latitude, pin.longitude, w, h, pin.id)
                    when (pin) {
                        is MapPinItem.LostPin -> {
                            drawCircle(
                                color = LostCrimson.copy(alpha = 0.16f),
                                radius = 34f,
                                center = pos
                            )
                            drawCircle(
                                color = LostCrimson.copy(alpha = 0.4f),
                                radius = 34f,
                                center = pos,
                                style = Stroke(width = 1.5f)
                            )
                            drawCircle(color = PureWhite, radius = 13f, center = pos)
                            drawCircle(color = LostCrimson, radius = 9.5f, center = pos)
                        }
                        is MapPinItem.FoundPin -> {
                            drawCircle(
                                color = FoundEmerald.copy(alpha = 0.16f),
                                radius = 34f,
                                center = pos
                            )
                            drawCircle(
                                color = FoundEmerald.copy(alpha = 0.4f),
                                radius = 34f,
                                center = pos,
                                style = Stroke(width = 1.5f)
                            )
                            drawCircle(color = PureWhite, radius = 13f, center = pos)
                            drawCircle(color = FoundEmerald, radius = 9.5f, center = pos)
                        }
                        is MapPinItem.OrgPin -> {
                            drawCircle(color = PureWhite, radius = 15f, center = pos)
                            drawCircle(color = DeepNavy, radius = 11.5f, center = pos)
                        }
                    }
                }
            }

            // Legend Pill Top-Left
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
                shape = RoundedCornerShape(12.dp),
                color = PureWhite.copy(alpha = 0.94f),
                border = BorderStroke(1.dp, SlateBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendDot("Lost", LostCrimson)
                    LegendDot("Found", FoundEmerald)
                    LegendDot("Hub", DeepNavy)
                }
            }

            // Zoom Controls Top-Right
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilledIconButton(
                    onClick = { zoomLevel = (zoomLevel * 1.35f).coerceAtMost(4.5f) },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = PureWhite)
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Zoom in", tint = DeepNavy, modifier = Modifier.size(18.dp))
                }
                FilledIconButton(
                    onClick = { zoomLevel = (zoomLevel / 1.35f).coerceAtLeast(0.45f) },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = PureWhite)
                ) {
                    Icon(Icons.Outlined.Remove, contentDescription = "Zoom out", tint = DeepNavy, modifier = Modifier.size(18.dp))
                }
                FilledIconButton(
                    onClick = {
                        zoomLevel = 1.0f
                        panOffset = Offset.Zero
                    },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = PureWhite)
                ) {
                    Icon(Icons.Outlined.MyLocation, contentDescription = "Recenter map", tint = ElectricBlue, modifier = Modifier.size(18.dp))
                }
            }

            // Privacy Footer
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(10.dp),
                shape = RoundedCornerShape(10.dp),
                color = DeepNavy.copy(alpha = 0.88f)
            ) {
                Text(
                    text = "Approximate zones only • Tap any pin to inspect • Drag to pan",
                    style = MaterialTheme.typography.labelSmall,
                    color = PureWhite,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = SlateMuted)
    }
}
