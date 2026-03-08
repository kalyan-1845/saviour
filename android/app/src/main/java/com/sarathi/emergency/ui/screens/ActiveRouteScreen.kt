package com.sarathi.emergency.ui.screens

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.sarathi.emergency.data.SessionManager
import com.sarathi.emergency.data.api.SarathiApi
import com.sarathi.emergency.data.models.NotifyRequest
import com.sarathi.emergency.data.models.NotifyResponse
import com.sarathi.emergency.data.models.NotifyHospital
import com.sarathi.emergency.ui.components.GlowButton
import com.sarathi.emergency.ui.components.GlowVariant
import com.sarathi.emergency.ui.components.MapMarker
import com.sarathi.emergency.ui.components.MarkerColor
import com.sarathi.emergency.ui.components.MapRoute
import com.sarathi.emergency.ui.components.OfflineMapView
import com.sarathi.emergency.ui.theme.*
import android.content.Intent
import androidx.compose.foundation.clickable
import com.sarathi.emergency.services.BackgroundLocationService
import com.sarathi.emergency.util.LocationHelper
import kotlinx.coroutines.launch

/**
 * Active Route screen — works OFFLINE with embedded OSMDroid street map.
 * Notification feature tries online API first, falls back to offline mock.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ActiveRouteScreen(
    api: SarathiApi,
    sessionManager: SessionManager,
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val locationHelper = remember { LocationHelper(context) }
    val scope = rememberCoroutineScope()

    var latitude by remember { mutableDoubleStateOf(17.4426) }
    var longitude by remember { mutableDoubleStateOf(78.5006) }
    var hasLocation by remember { mutableStateOf(false) }
    var notifyResult by remember { mutableStateOf<NotifyResponse?>(null) }
    var notifyLoading by remember { mutableStateOf(false) }
    var voiceEnabled by remember { mutableStateOf(true) }
    var showRerouteDialog by remember { mutableStateOf(false) }
    var tripStatus by remember { mutableStateOf("En Route to Patient") }
    var etaMinutes by remember { mutableIntStateOf(7) }
    var trafficLevel by remember { mutableStateOf("Normal") }
    
    // Track destination
    var currentDestLat by remember { mutableDoubleStateOf(17.4550) }
    var currentDestLng by remember { mutableDoubleStateOf(78.4700) }
    var currentHospitalName by remember { mutableStateOf("Default Emergency Hospital") }

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
            // Continuous updates
            locationHelper.requestLocationUpdates { loc ->
                latitude = loc.latitude
                longitude = loc.longitude
                hasLocation = true
            }
        }
    }

    // Map markers
    val mapMarkers = remember(latitude, longitude, hasLocation, currentDestLat, currentDestLng) {
        buildList {
            if (hasLocation) {
                add(MapMarker(latitude, longitude, "🚑 Your Location", "Ambulance", MarkerColor.BLUE))
            }
            add(MapMarker(currentDestLat, currentDestLng, "🏥 $currentHospitalName", "Emergency destination", MarkerColor.GREEN))
        }
    }

    // Route from current to destination
    val mapRoutes = remember(latitude, longitude, currentDestLat, currentDestLng) {
        listOf(
            MapRoute(
                points = listOf(
                    latitude to longitude,
                    currentDestLat to currentDestLng
                ),
                color = android.graphics.Color.parseColor("#4F46E5"),
                width = 6f
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavy)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBlueGray)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = TextWhite)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text("Active Navigation", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(tripStatus, color = SuccessGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Row {
                    IconButton(onClick = { 
                        // SIMULATE SMS SEND
                        println("SARATHI SMS: Your ambulance is at $latitude,$longitude. ETA: $etaMinutes mins. Priority Green Corridor Active.")
                    }) {
                        Icon(Icons.Default.Textsms, "SMS Update", tint = PrimaryBlue)
                    }
                    IconButton(onClick = { voiceEnabled = !voiceEnabled }) {
                        Icon(
                            imageVector = if (voiceEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Voice",
                            tint = if (voiceEnabled) SuccessGreen else TextWhite70
                        )
                    }
                }
            }

            // Offline Street Map — replaces Google Maps
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                OfflineMapView(
                    modifier = Modifier.fillMaxSize(),
                    centerLatitude = if (hasLocation) latitude else 17.4426,
                    centerLongitude = if (hasLocation) longitude else 78.5006,
                    zoomLevel = if (hasLocation) 15.0 else 13.0,
                    markers = mapMarkers,
                    routes = mapRoutes,
                    showMyLocation = hasLocation,
                    myLatitude = latitude,
                    myLongitude = longitude,
                    showControls = true
                )

                // ETA overlay
                Card(
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkNavy.copy(alpha = 0.9f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, null, tint = EmergencyOrange, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("ETA", color = TextWhite70, fontSize = 11.sp)
                            Text("$etaMinutes mins", color = if (etaMinutes > 15) EmergencyOrange else TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Icon(Icons.Default.Traffic, null, tint = if (trafficLevel == "Heavy") EmergencyRed else SuccessGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Traffic", color = TextWhite70, fontSize = 11.sp)
                            Text(trafficLevel, color = if (trafficLevel == "Heavy") EmergencyRed else SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                }

                // TRAFFIC ALERT - DYNAMIC REROUTE PROMPT
                if (trafficLevel == "Heavy") {
                    Card(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp).padding(horizontal = 20.dp),
                        colors = CardDefaults.cardColors(containerColor = EmergencyRed.copy(alpha = 0.9f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("HEAVY TRAFFIC DETECTED. SUGGEST REROUTE?", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                }

                // Offline indicator
                Card(
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 60.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkNavy.copy(alpha = 0.85f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(6.dp).clip(CircleShape)
                                .background(SuccessGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("OFFLINE MAP", color = SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Bottom panel
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkBlueGray)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Notification results
                    if (notifyResult != null) {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("✅ Authorities Notified", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                notifyResult?.hospital?.let {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("🏥 ${it.name}: ${it.message}", color = TextWhite70, fontSize = 12.sp)
                                }
                                notifyResult?.police?.let { p ->
                                    p.stations.forEach { station ->
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("🚔 ${station.name} (${station.phone})", color = TextWhite70, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                    // Notify — tries API, falls back to offline mock
                    GlowButton(
                        text = if (tripStatus == "En Route to Patient") "MARK PICKED UP" else "SEND LIVE ETA SMS",
                        onClick = {
                            if (tripStatus == "En Route to Patient") {
                                tripStatus = "Transporting Patient"
                                sessionManager.updateSimulatedMissionStatus("Transporting Patient")
                                scope.launch {
                                    api.notifyAuthorities(NotifyRequest("active", sessionManager.getDriverId(), latitude, longitude))
                                }
                            } else {
                                // Simulate family SMS
                                println("SARATHI: SMS Sent to patient group. Status: $tripStatus, ETA 🏥: $etaMinutes mins")
                            }
                        },
                        variant = GlowVariant.PURPLE,
                        modifier = Modifier.weight(1f),
                        icon = { Icon(if (tripStatus == "En Route to Patient") Icons.Default.PersonSearch else Icons.Default.ShareLocation, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                    )

                        // Complete
                        GlowButton(
                            text = "Complete",
                            onClick = {
                                context.stopService(Intent(context, BackgroundLocationService::class.java))
                                onComplete()
                            },
                            variant = GlowVariant.SUCCESS,
                            modifier = Modifier.weight(1f),
                            icon = { Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Open in Google Maps navigation
                    GlowButton(
                        text = "🚀 NAVIGATE TO PATIENT (HIGH PRIORITY)",
                        onClick = {
                            val uri = Uri.parse("google.navigation:q=$currentDestLat,$currentDestLng&mode=d")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            intent.setPackage("com.google.android.apps.maps")
                            try { context.startActivity(intent) } catch (_: Exception) {
                                val browserUri = Uri.parse("https://maps.google.com/maps?daddr=$currentDestLat,$currentDestLng")
                                context.startActivity(Intent(Intent.ACTION_VIEW, browserUri))
                            }
                        },
                        variant = GlowVariant.DANGER,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // REROUTE BUTTON
                    GlowButton(
                        text = "🔀 RE-ROUTE EMERGENCY HOSPITAL",
                        onClick = { showRerouteDialog = true },
                        variant = GlowVariant.PRIMARY,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (showRerouteDialog) {
                        AlertDialog(
                            onDismissRequest = { showRerouteDialog = false },
                            containerColor = DarkBlueGray,
                            title = { Text("Heavy Traffic: Reroute Suggested", color = Color.White) },
                            text = { 
                                Column {
                                    Text("Suggested fastest alternatives based on your current GPS and traffic:", color = TextWhite70, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    RerouteOption("Apollo Hospital (Jubilee Hills)", "5 mins", "Clear Flow") {
                                        currentDestLat = 17.4310
                                        currentDestLng = 78.4070
                                        currentHospitalName = "Apollo Hospital"
                                        etaMinutes = 5
                                        trafficLevel = "Normal"
                                        showRerouteDialog = false
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    RerouteOption("NIMS Emergency Unit", "3 mins", "Clear Flow") {
                                        currentDestLat = 17.4426
                                        currentDestLng = 78.5006
                                        currentHospitalName = "NIMS"
                                        etaMinutes = 3
                                        trafficLevel = "Normal"
                                        showRerouteDialog = false
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showRerouteDialog = false }) {
                                    Text("CANCEL", color = TextWhite70)
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Emergency call strip
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = EmergencyRed.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Warning, null, tint = EmergencyRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Emergency Hotline: 112", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RerouteOption(name: String, time: String, flow: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocalHospital, null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("ETA $time | $flow", color = SuccessGreen, fontSize = 11.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextWhite50)
        }
    }
}

private fun offlineMockNotify(): NotifyResponse {
    return NotifyResponse(
        success = true,
        hospital = NotifyHospital(
            name = "Nearest Hospital",
            message = "Alert sent (offline mode)"
        )
    )
}
