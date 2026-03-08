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
import com.sarathi.emergency.data.SessionManager
import com.sarathi.emergency.data.api.SarathiApi
import com.sarathi.emergency.ui.components.GlowButton
import com.sarathi.emergency.ui.components.GlowVariant
import com.sarathi.emergency.ui.components.MapMarker
import com.sarathi.emergency.ui.components.MarkerColor
import com.sarathi.emergency.ui.components.OfflineMapView
import com.sarathi.emergency.ui.theme.*
import com.sarathi.emergency.util.LocationHelper
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

data class IncomingCase(
    val id: String,
    val patientPhone: String,
    val emergencyType: String,
    val eta: Int,
    val driverName: String,
    val vehicleNumber: String,
    val status: String,
    val priority: String,
    val time: String,
    val fromArea: String,
    val bloodGroup: String = "Unknown",
    val mapUrl: String = "https://maps.google.com/?q=17.4426,78.5006",
    val ambulanceLat: Double = 17.4426,
    val ambulanceLng: Double = 78.5006
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HospitalDashboardScreen(
    hospitalName: String,
    hospitalArea: String,
    sessionManager: SessionManager,
    api: SarathiApi,
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

    var caseList by remember {
        mutableStateOf(
            listOf(
                IncomingCase("EC-001", "+91 98765 XXXXX", "Cardiac Arrest", 5,
                    "Kalyan", "TS-09-AB-1234", "Incoming", "Critical", "08:15 AM",
                    "Jubilee Hills", "B+", ambulanceLat = 17.4310, ambulanceLng = 78.4070),
                IncomingCase("EC-002", "+91 87654 XXXXX", "Road Accident", 8,
                    "Raju K", "TS-09-CD-5678", "Incoming", "High", "08:22 AM",
                    "Gachibowli", "O+", ambulanceLat = 17.4400, ambulanceLng = 78.3481)
            )
        )
    }

    // POLL FOR LIVE DRIVER UPDATES
    LaunchedEffect(Unit) {
        while(true) {
            val simId = sessionManager.getSimulatedSOS()
            if (simId != null) {
                val simType = sessionManager.getSimulatedSOSType()
                val simStatus = sessionManager.getSimulatedSOSStatus()
                
                // CHECK IF CASE ALREADY EXISTS
                val exists = caseList.any { it.id == simId }
                if (!exists) {
                    val newCase = IncomingCase(
                        id = simId,
                        patientName = "S.O.S ($simType)",
                        emergencyType = simType,
                        status = simStatus,
                        eta = sessionManager.getSimulatedETA(),
                        distance = 2.4,
                        isPriority = true
                    )
                    caseList = listOf(newCase) + caseList
                } else {
                    // Update existing simulation state
                    caseList = caseList.map { 
                        if (it.id == simId) it.copy(status = simStatus) else it 
                    }
                }
            }
            delay(5000)
        }
    }

    val cases = caseList

    val incomingCount = cases.count { it.status == "Incoming" }
    val enRouteCount = cases.count { it.status == "En Route" }
    val arrivedCount = cases.count { it.status == "Arrived" }
    val criticalCount = cases.count { it.priority == "Critical" }

    // Build map markers — hospital + all incoming ambulances
    val mapMarkers = remember(cases, latitude, longitude) {
        buildList {
            // Hospital itself
            add(MapMarker(latitude, longitude, "🏥 $hospitalName", "Your Hospital", MarkerColor.GREEN))
            // All incoming/en route ambulances
            cases.filter { it.status != "Arrived" }.forEach { case ->
                val color = when (case.priority) {
                    "Critical" -> MarkerColor.RED
                    "High" -> MarkerColor.ORANGE
                    else -> MarkerColor.BLUE
                }
                add(MapMarker(
                    case.ambulanceLat, case.ambulanceLng,
                    "🚑 ${case.id} — ${case.driverName}",
                    "${case.emergencyType} • ETA ${case.eta} min",
                    color
                ))
            }
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
                                    .background(Brush.linearGradient(listOf(SuccessGreen, Color(0xFF059669)))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.LocalHospital, null, tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Hospital Dashboard", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Black)
                                Text(hospitalName, color = SuccessGreen, fontSize = 12.sp)
                            }
                        }
                        IconButton(onClick = onLogout) {
                            Icon(Icons.Default.Logout, "Logout", tint = TextRed400)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Stats
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        HospDashStat("🚨", "$incomingCount", "Incoming", EmergencyOrange)
                        HospDashStat("🚑", "$enRouteCount", "En Route", PrimaryBlue)
                        HospDashStat("✅", "$arrivedCount", "Arrived", SuccessGreen)
                        HospDashStat("🔴", "$criticalCount", "Critical", EmergencyRed)
                        HospDashStat("📋", "${cases.size}", "Total", TextWhite)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Last updated: $currentTime", color = TextWhite50, fontSize = 10.sp,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ═══════════════════════════════════════════
            //  LIVE MAP — AMBULANCE TRACKING
            // ═══════════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(SuccessGreen.copy(alpha = 0.3f), Color.Transparent))
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Map, null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ambulance Tracker", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
                        showMyLocation = false,
                        showControls = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Section header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Emergency Case Intake", color = TextWhite, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SuccessGreen))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("LIVE", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            // ── Case List ──
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(cases) { case ->
                    CaseCard(case)
                }
            }
        }
    }
}

@Composable
private fun CaseCard(case: IncomingCase) {
    val statusColor = when (case.status) {
        "Incoming" -> EmergencyOrange
        "En Route" -> PrimaryBlue
        "Arrived" -> SuccessGreen
        else -> TextWhite70
    }
    val priorityColor = when (case.priority) {
        "Critical" -> EmergencyRed
        "High" -> EmergencyOrange
        else -> TextYellow400
    }

    val typeIcon = when {
        case.emergencyType.contains("Cardiac", true) -> "❤️"
        case.emergencyType.contains("Accident", true) -> "🚗"
        case.emergencyType.contains("Burn", true) -> "🔥"
        case.emergencyType.contains("Stroke", true) -> "🧠"
        case.emergencyType.contains("Fracture", true) -> "🦴"
        case.emergencyType.contains("Pediatric", true) -> "👶"
        else -> "⚠️"
    }

    val pulseAnim = rememberInfiniteTransition(label = "case_${case.id}")
    val pulseAlpha by pulseAnim.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "alpha"
    )

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(
                    statusColor.copy(alpha = if (case.status != "Arrived") 0.4f else 0.1f),
                    Color.Transparent
                )
            )
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Row 1: Case ID + Priority + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(case.id, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        case.priority, color = priorityColor, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(4.dp))
                            .background(priorityColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (case.status != "Arrived") {
                        Box(
                            modifier = Modifier.size(8.dp).clip(CircleShape)
                                .background(statusColor.copy(alpha = pulseAlpha))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(case.status, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Emergency type
            Text("$typeIcon ${case.emergencyType}", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)

            Spacer(modifier = Modifier.height(6.dp))

            // Patient + Blood Group
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, null, tint = TextBlue400, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(case.patientPhone, color = TextWhite70, fontSize = 12.sp)
                if (case.bloodGroup != "Unknown") {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "🩸 ${case.bloodGroup}", color = EmergencyRed, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(4.dp))
                            .background(EmergencyRed.copy(alpha = 0.1f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // From area
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, tint = EmergencyOrange, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Coming from: ${case.fromArea}", color = TextWhite50, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Details row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, tint = TextBlue400, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(case.driverName, color = TextWhite50, fontSize = 11.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DirectionsCar, null, tint = TextBlue400, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(case.vehicleNumber, color = TextWhite50, fontSize = 11.sp)
                }
                if (case.eta > 0 && case.status != "Arrived") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, null, tint = EmergencyOrange, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("ETA ${case.eta} min", color = EmergencyOrange, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
                Text(case.time, color = TextWhite50, fontSize = 11.sp)
            }

            if (case.status != "Arrived") {
                Spacer(modifier = Modifier.height(10.dp))
                val context = androidx.compose.ui.platform.LocalContext.current
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        "🗺️ Track Live Ambulance",
                        color = TextBlue300,
                        textDecoration = TextDecoration.Underline,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable {
                            val uri = android.net.Uri.parse(case.mapUrl)
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                        }
                    )
                }
            }

            // Prepare bed prompt for incoming
            if (case.status == "Incoming" && case.priority == "Critical") {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = EmergencyRed.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Warning, null, tint = EmergencyRed, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("⚡ PREPARE EMERGENCY BED", color = EmergencyRed,
                            fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun HospDashStat(icon: String, value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, color = color, fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text(label, color = TextWhite50, fontSize = 9.sp)
    }
}
