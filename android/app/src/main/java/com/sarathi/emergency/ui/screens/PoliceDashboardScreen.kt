package com.sarathi.emergency.ui.screens

import android.Manifest
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.sarathi.emergency.ui.components.GlowButton
import com.sarathi.emergency.ui.components.GlowVariant
import com.sarathi.emergency.ui.components.MapMarker
import com.sarathi.emergency.ui.components.MarkerColor
import com.sarathi.emergency.ui.components.MapRoute
import com.sarathi.emergency.ui.components.OfflineMapView
import com.sarathi.emergency.ui.theme.*
import com.sarathi.emergency.util.LocationHelper
import java.text.SimpleDateFormat
import java.util.*

data class GreenCorridorAlert(
    val id: String,
    val type: String,
    val from: String,
    val to: String,
    val status: String,
    val eta: Int,
    val priority: String,
    val time: String,
    val driverName: String = "Assigned Driver",
    val vehicleNumber: String = "TS-09-XX-1234",
    val mapUrl: String = "https://maps.google.com/?q=17.4426,78.5006",
    val fromLat: Double = 17.4426,
    val fromLng: Double = 78.5006,
    val toLat: Double = 17.4500,
    val toLng: Double = 78.5100
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PoliceDashboardScreen(
    stationName: String,
    stationArea: String,
    sessionManager: SessionManager,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val locationHelper = remember { LocationHelper(context) }

    var latitude by remember { mutableDoubleStateOf(17.4426) }
    var longitude by remember { mutableDoubleStateOf(78.5006) }
    var hasLocation by remember { mutableStateOf(false) }

    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    LaunchedEffect(locationPermission.status.isGranted) {
        if (locationPermission.status.isGranted) {
            locationHelper.getLastLocation { loc ->
                if (loc != null) {
                    latitude = loc.latitude
                    longitude = loc.longitude
                    hasLocation = true
                }
            }
        } else {
            locationPermission.launchPermissionRequest()
        }
    }

    val currentTime = remember {
        SimpleDateFormat("hh:mm a, dd MMM yyyy", Locale.getDefault()).format(Date())
    }

    // Simulated live alerts with coordinates
    var alertList by remember {
        mutableStateOf(
            listOf(
                GreenCorridorAlert("GC-001", "Cardiac", "Jubilee Hills", "Apollo Hospital",
                    "Active", 5, "Critical", "08:15 AM", "Kalyan", "TS-09-AB-1234",
                    fromLat = 17.4310, fromLng = 78.4070, toLat = 17.4426, toLng = 78.4470),
                GreenCorridorAlert("GC-002", "Trauma", "Gachibowli", "NIMS Hospital",
                    "Active", 8, "High", "08:22 AM", "Raju K", "TS-09-CD-5678",
                    fromLat = 17.4400, fromLng = 78.3481, toLat = 17.3950, toLng = 78.4946)
            )
        )
    }

    // POLL FOR LIVE DRIVER UPDATES (SIMULATION)
    LaunchedEffect(Unit) {
        while(true) {
            val simId = sessionManager.getSimulatedSOS()
            if (simId != null) {
                // UPDATE RELEVANT GC STATUS IF FOUND
                alertList = alertList.map { 
                    if (it.id == "GC-001") it.copy(status = "Transporting", eta = 4) else it 
                }
            }
            delay(5000)
        }
    }

    val alerts = alertList

    val activeCount = alerts.count { it.status != "Completed" }
    val criticalCount = alerts.count { it.priority == "Critical" }

    // Build map markers from alerts
    val mapMarkers = remember(alerts) {
        buildList {
            alerts.forEach { alert ->
                if (alert.status != "Completed") {
                    add(MapMarker(alert.fromLat, alert.fromLng, "🚑 ${alert.id} Pickup", alert.from, MarkerColor.RED))
                    add(MapMarker(alert.toLat, alert.toLng, "🏥 ${alert.to}", "Hospital", MarkerColor.GREEN))
                }
            }
        }
    }

    // Build routes from active alerts
    val mapRoutes = remember(alerts) {
        alerts.filter { it.status != "Completed" }.map { alert ->
            val color = when (alert.priority) {
                "Critical" -> android.graphics.Color.parseColor("#DC2626")
                "High" -> android.graphics.Color.parseColor("#EA580C")
                else -> android.graphics.Color.parseColor("#4F46E5")
            }
            MapRoute(
                points = listOf(
                    alert.fromLat to alert.fromLng,
                    alert.toLat to alert.toLng
                ),
                color = color,
                width = 4f
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkNavy, Color(0xFF0D1B2A), Color(0xFF1B2838))))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF162032))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(PrimaryBlue, Color(0xFF1D4ED8)))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Shield, null, tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Police Dashboard", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Black)
                                Text(stationName, color = TextBlue300, fontSize = 12.sp)
                            }
                        }
                        IconButton(onClick = onLogout) {
                            Icon(Icons.Default.Logout, "Logout", tint = TextRed400)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stats row
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        DashStatCard("🚨", "$activeCount", "Active Alerts", PrimaryBlue)
                        DashStatCard("🔴", "$criticalCount", "Critical", EmergencyRed)
                        DashStatCard("✅", "${alerts.size - activeCount}", "Completed", SuccessGreen)
                        DashStatCard("🛣", "$activeCount", "Green Corridors", EmergencyOrange)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Last updated: $currentTime", color = TextWhite50, fontSize = 10.sp,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ═══════════════════════════════════════════
            //  LIVE MAP — GREEN CORRIDOR TRACKING
            // ═══════════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(PrimaryBlue.copy(alpha = 0.3f), Color.Transparent))
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Map, null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Green Corridor Map", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier.size(8.dp).clip(CircleShape).background(SuccessGreen)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("LIVE", color = SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OfflineMapView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        centerLatitude = if (hasLocation) latitude else 17.4426,
                        centerLongitude = if (hasLocation) longitude else 78.4500,
                        zoomLevel = 12.0,
                        markers = mapMarkers,
                        routes = mapRoutes,
                        showMyLocation = hasLocation,
                        myLatitude = latitude,
                        myLongitude = longitude,
                        showControls = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Section Header ──
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Green Corridor Alerts", color = TextWhite, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(8.dp).clip(CircleShape).background(SuccessGreen)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("LIVE", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            // ── Alert List ──
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(alerts) { alert ->
                    AlertCard(alert)
                }
            }
        }
    }
}

@Composable
private fun AlertCard(alert: GreenCorridorAlert) {
    val statusColor = when (alert.status) {
        "Active" -> EmergencyOrange
        "En Route" -> PrimaryBlue
        "Completed" -> SuccessGreen
        else -> TextWhite70
    }
    val priorityColor = when (alert.priority) {
        "Critical" -> EmergencyRed
        "High" -> EmergencyOrange
        else -> TextYellow400
    }

    val pulseAnim = rememberInfiniteTransition(label = "pulse_${alert.id}")
    val pulseAlpha by pulseAnim.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "alpha_${alert.id}"
    )

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(
                    statusColor.copy(alpha = if (alert.status != "Completed") 0.4f else 0.1f),
                    Color.Transparent
                )
            )
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Row 1: ID + Priority + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(alert.id, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    // Priority badge
                    Text(
                        alert.priority, color = priorityColor, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(priorityColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                // Status with pulse
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (alert.status != "Completed") {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor.copy(alpha = pulseAlpha))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(alert.status, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Emergency type
            Row(verticalAlignment = Alignment.CenterVertically) {
                val typeIcon = when (alert.type) {
                    "Cardiac" -> "❤️"
                    "Trauma" -> "🚑"
                    "Burn" -> "🔥"
                    "Stroke" -> "🧠"
                    "Accident" -> "🚗"
                    else -> "⚠️"
                }
                Text("$typeIcon ${alert.type} Emergency", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 3: Route
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, tint = EmergencyRed, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(alert.from, color = TextWhite70, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text("→", color = TextWhite50, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.LocalHospital, null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(alert.to, color = TextWhite70, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 4: Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, tint = TextBlue400, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(alert.driverName, color = TextWhite50, fontSize = 11.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DirectionsCar, null, tint = TextBlue400, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(alert.vehicleNumber, color = TextWhite50, fontSize = 11.sp)
                }
                if (alert.eta > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, null, tint = EmergencyOrange, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("${alert.eta} min", color = EmergencyOrange, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
                Text(alert.time, color = TextWhite50, fontSize = 11.sp)
            }

            if (alert.status != "Completed") {
                Spacer(modifier = Modifier.height(10.dp))
                val context = androidx.compose.ui.platform.LocalContext.current
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        "🗺️ Track Green Corridor",
                        color = TextBlue300,
                        textDecoration = TextDecoration.Underline,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable {
                            val uri = android.net.Uri.parse(alert.mapUrl)
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DashStatCard(icon: String, value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, color = color, fontWeight = FontWeight.Black, fontSize = 20.sp)
        Text(label, color = TextWhite50, fontSize = 9.sp)
    }
}
