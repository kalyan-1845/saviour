package com.sarathi.emergency.ui.screens

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.sarathi.emergency.data.SessionManager
import com.sarathi.emergency.data.api.SarathiApi
import com.sarathi.emergency.data.models.AssignedTrip
import com.sarathi.emergency.data.models.EmergencySelectRequest
import com.sarathi.emergency.ui.components.EmergencyCard
import com.sarathi.emergency.ui.components.GlowButton
import com.sarathi.emergency.ui.components.GlowVariant
import com.sarathi.emergency.ui.components.MapMarker
import com.sarathi.emergency.ui.components.MarkerColor
import com.sarathi.emergency.ui.components.OfflineMapView
import com.sarathi.emergency.ui.theme.*
import com.sarathi.emergency.util.LocationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DriverDashboardScreen(
    api: SarathiApi,
    sessionManager: SessionManager,
    onNavigateToHospitalSelection: () -> Unit,
    onNavigateToActiveRoute: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val locationHelper = remember { LocationHelper(context) }
    val scope = rememberCoroutineScope()
    val driver = remember { sessionManager.getDriver() }

    var selectedEmergency by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var latitude by remember { mutableDoubleStateOf(17.4426) }
    var longitude by remember { mutableDoubleStateOf(78.5006) }
    var hasLocation by remember { mutableStateOf(false) }
    var locationAttempted by remember { mutableStateOf(false) }

    // Active trip detection
    var activeTrip by remember { mutableStateOf<AssignedTrip?>(null) }
    var isCheckingTrip by remember { mutableStateOf(false) }

    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    // Setup GPS
    LaunchedEffect(locationPermission.status.isGranted) {
        if (locationPermission.status.isGranted) {
            locationAttempted = true
            locationHelper.getLastLocation { loc ->
                if (loc != null) {
                    latitude = loc.latitude
                    longitude = loc.longitude
                    hasLocation = true
                }
            }
            locationHelper.requestLocationUpdates { loc ->
                latitude = loc.latitude
                longitude = loc.longitude
                hasLocation = true
            }
        }
    }

    // Auto-poll for assigned SOS trips (Requirement: "automatically detects and track sos")
    LaunchedEffect(driver?._id) {
        val driverId = driver?._id ?: return@LaunchedEffect
        while (true) {
            try {
                isCheckingTrip = true
                val response = api.getAssignedTrip(driverId)
                if (response.isSuccessful && response.body()?.success == true && response.body()?.trip != null) {
                    activeTrip = response.body()?.trip
                } else {
                    // Check for SIMULATED SOS on the same device (offline testing)
                    val simulatedId = sessionManager.getSimulatedSOS()
                    if (simulatedId != null) {
                        activeTrip = AssignedTrip(
                            id = simulatedId,
                            status = "assigned",
                            emergencyType = "medical",
                            phone = "9988776655",
                            pickupLocation = com.sarathi.emergency.data.models.TripLocation(latitude + 0.002, longitude + 0.003)
                        )
                    } else {
                        activeTrip = null
                    }
                }
            } catch (_: Exception) {
                // Ignore API failures in background poll
            } finally {
                isCheckingTrip = false
            }
            delay(10000) // Poll every 10 seconds
        }
    }

    val emergencies = remember {
        listOf(
            EmergencyType("cardiac", "Cardiac", "❤️", "Heart attack / Cardiac arrest",
                "heart_attack", listOf("CPR if needed", "Keep patient calm", "Aspirin if available")),
            EmergencyType("trauma", "Trauma", "🚑", "Severe injury / Road accident",
                "accident", listOf("Stop bleeding", "Don't move patient", "Call for help")),
            EmergencyType("general", "Medical", "🏥", "General medical emergency",
                "medical", listOf("Assess vitals", "Provide first aid", "Transport safely")),
            EmergencyType("pediatric", "Pediatric", "👶", "Child emergency",
                "pediatric", listOf("Stay calm", "Check airways", "Keep child warm")),
            EmergencyType("stroke", "Stroke", "🧠", "Brain stroke / Neurological",
                "stroke", listOf("FAST test", "Note symptom time", "Keep head elevated")),
            EmergencyType("burn", "Burn", "🔥", "Burn / Fire injury",
                "burn", listOf("Cool with water", "Do not apply ice", "Cover with clean cloth"))
        )
    }

    // Map markers
    val mapMarkers = remember(latitude, longitude, hasLocation, activeTrip) {
        buildList {
            if (hasLocation) {
                add(MapMarker(latitude, longitude, "🚑 My Ambulance", "Ready for assistance", MarkerColor.BLUE))
                
                // If there's an active SOS trip, show pickup marker
                activeTrip?.pickupLocation?.let { loc ->
                    add(MapMarker(loc.latitude, loc.longitude, "📍 SOS PICKUP", "Active Emergency Patient", MarkerColor.RED))
                }

                // Nearby hospitals
                add(MapMarker(latitude + 0.012, longitude + 0.008, "🏥 Sunshine Hospital", "24/7 ER", MarkerColor.GREEN))
                add(MapMarker(latitude - 0.008, longitude + 0.015, "🏥 KIMS Gachibowli", "Trauma Care", MarkerColor.GREEN))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkNavy, Color(0xFF131A2F), Color(0xFF1E1338))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ── Header ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hello, ${driver?.fullName ?: "Driver"}",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Driver Monitoring Hub",
                        color = TextBlue300,
                        fontSize = 13.sp
                    )
                }
                IconButton(onClick = onLogout) {
                    Icon(Icons.Default.PowerSettingsNew, "Logout", tint = EmergencyRed)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Active Trip Alert ────────────
            AnimatedVisibility(visible = activeTrip != null) {
                activeTrip?.let { trip ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = EmergencyRed.copy(alpha = 0.15f)),
                        border = CardDefaults.outlinedCardBorder().copy(
                            width = 2.dp,
                            brush = Brush.linearGradient(listOf(EmergencyRed, EmergencyOrange))
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(EmergencyRed)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("🚨 ACTIVE EMERGENCY ASSIGNED", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Type: ${trip.emergencyType.uppercase()}", color = TextWhite70, fontSize = 13.sp)
                            Text("Phone: ${trip.phone ?: "N/A"}", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            GlowButton(
                                text = "OPEN GPS NAVIGATION",
                                onClick = onNavigateToActiveRoute,
                                variant = GlowVariant.DANGER,
                                modifier = Modifier.fillMaxWidth(),
                                icon = { Icon(Icons.Default.Route, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                            )
                        }
                    }
                }
            }

            // ── Status Bar ──
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusColor = if (hasLocation) SuccessGreen else EmergencyOrange
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (hasLocation) "Online • Receiving SOS Alerts" else "Offline • Checking Location...",
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (isCheckingTrip) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PrimaryBlue)
                    } else {
                        Icon(Icons.Default.Sync, null, tint = TextWhite50, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Area Map ──
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Area Awareness Map", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OfflineMapView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        centerLatitude = latitude,
                        centerLongitude = longitude,
                        zoomLevel = 15.0,
                        markers = mapMarkers,
                        showMyLocation = hasLocation,
                        myLatitude = latitude,
                        myLongitude = longitude,
                        showControls = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Start New Emergency ──
            Text(
                text = "Dispatch New Emergency",
                color = TextWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "If you are with a patient, start here",
                color = TextWhite70,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            val chunked = emergencies.chunked(2)
            chunked.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { emergency ->
                        EmergencyCard(
                            icon = emergency.icon,
                            title = emergency.name,
                            description = emergency.description,
                            isSelected = selectedEmergency == emergency.id,
                            onClick = { 
                                selectedEmergency = emergency.id
                                error = null
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (selectedEmergency != null) {
                val selected = emergencies.find { it.id == selectedEmergency }!!
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Ready to start: ${selected.name}", color = TextWhite, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(10.dp))
                        GlowButton(
                            text = if (isLoading) "Initializing..." else "Start Response Hub →",
                            onClick = {
                                if (!hasLocation) {
                                    error = "Location detection in progress..."
                                    return@GlowButton
                                }
                                isLoading = true
                                scope.launch {
                                    try {
                                        api.selectEmergency(
                                            EmergencySelectRequest(
                                                driverId = sessionManager.getDriverId(),
                                                emergencyType = selected.apiType,
                                                latitude = latitude,
                                                longitude = longitude
                                            )
                                        )
                                        onNavigateToHospitalSelection()
                                    } catch (_: Exception) {
                                        onNavigateToHospitalSelection()
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            isLoading = isLoading,
                            variant = GlowVariant.PRIMARY,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(error!!, color = EmergencyOrange, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
