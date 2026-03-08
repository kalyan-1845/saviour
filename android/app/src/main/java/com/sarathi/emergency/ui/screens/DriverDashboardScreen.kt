package com.sarathi.emergency.ui.screens

import android.Manifest
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
import com.sarathi.emergency.data.models.EmergencySelectRequest
import com.sarathi.emergency.ui.components.EmergencyCard
import com.sarathi.emergency.ui.components.GlowButton
import com.sarathi.emergency.ui.components.GlowVariant
import com.sarathi.emergency.ui.components.MapMarker
import com.sarathi.emergency.ui.components.MarkerColor
import com.sarathi.emergency.ui.components.OfflineMapView
import com.sarathi.emergency.ui.theme.*
import com.sarathi.emergency.util.LocationHelper
import kotlinx.coroutines.launch

data class EmergencyType(
    val id: String,
    val name: String,
    val icon: String,
    val description: String,
    val apiType: String,
    val protocols: List<String>
)

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
    var latitude by remember { mutableDoubleStateOf(0.0) }
    var longitude by remember { mutableDoubleStateOf(0.0) }
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
            locationHelper.requestLocationUpdates { loc ->
                latitude = loc.latitude
                longitude = loc.longitude
                hasLocation = true
            }
        } else {
            locationPermission.launchPermissionRequest()
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

    // Map markers — show nearby simulated emergency hotspots
    val mapMarkers = remember(latitude, longitude, hasLocation) {
        buildList {
            if (hasLocation) {
                add(MapMarker(latitude, longitude, "🚑 You (Driver)", "Your current location", MarkerColor.BLUE))
                // Simulated nearby hospitals
                add(MapMarker(latitude + 0.006, longitude + 0.004, "🏥 Apollo Hospital", "2.1 km away", MarkerColor.GREEN))
                add(MapMarker(latitude - 0.004, longitude + 0.007, "🏥 NIMS Hospital", "3.5 km away", MarkerColor.GREEN))
                add(MapMarker(latitude + 0.008, longitude - 0.003, "🏥 KIMS Hospital", "4.2 km away", MarkerColor.GREEN))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(DarkNavy, Color(0xFF1E2A4A), DarkPurple)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header with logout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Welcome, ${driver?.fullName ?: "Driver"}",
                        color = TextWhite,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "SARATHI Emergency Dashboard",
                        color = TextBlue300,
                        fontSize = 13.sp
                    )
                }
                IconButton(onClick = {
                    sessionManager.logout()
                    onLogout()
                }) {
                    Icon(Icons.Default.Logout, "Logout", tint = TextRed400)
                }
            }

            // Status indicator
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SuccessGreen.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(SuccessGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hasLocation) "Online — GPS Active" else "Online — Waiting for GPS",
                        color = SuccessGreen,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ═══════════════════════════════════════════
            //  LIVE MAP — STREET VIEW
            // ═══════════════════════════════════════════
            Card(
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
                        Text("Area Map — Nearby Hospitals", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier.size(8.dp).clip(CircleShape)
                                .background(if (hasLocation) SuccessGreen else EmergencyOrange)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (hasLocation) "LIVE" else "NO GPS",
                            color = if (hasLocation) SuccessGreen else EmergencyOrange,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OfflineMapView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        centerLatitude = if (hasLocation) latitude else 17.4426,
                        centerLongitude = if (hasLocation) longitude else 78.5006,
                        zoomLevel = if (hasLocation) 14.0 else 12.0,
                        markers = mapMarkers,
                        showMyLocation = hasLocation,
                        myLatitude = latitude,
                        myLongitude = longitude,
                        showControls = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Select Emergency Type",
                color = TextWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Choose the emergency type to start response",
                color = TextWhite70,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Emergency cards — using Column with Rows for 2-column grid in scroll
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
                            onClick = { selectedEmergency = emergency.id },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Protocols display
            selectedEmergency?.let { sel ->
                val selected = emergencies.find { it.id == sel }
                if (selected != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = PrimaryBlue.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "${selected.icon} ${selected.name} Protocols",
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            selected.protocols.forEach { protocol ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle, null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(protocol, color = TextWhite70, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            if (error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(error ?: "", color = TextRed300, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Continue button
            GlowButton(
                text = if (isLoading) "Starting Emergency..." else "Continue →",
                onClick = {
                    if (selectedEmergency == null) {
                        error = "Please select an emergency type"
                        return@GlowButton
                    }
                    if (!hasLocation) {
                        error = "GPS location required"
                        return@GlowButton
                    }
                    error = null
                    isLoading = true
                    val selected = emergencies.find { it.id == selectedEmergency }!!
                    scope.launch {
                        try {
                            val response = api.selectEmergency(
                                EmergencySelectRequest(
                                    driverId = sessionManager.getDriverId(),
                                    emergencyType = selected.apiType,
                                    latitude = latitude,
                                    longitude = longitude
                                )
                            )
                            if (response.isSuccessful && response.body()?.success == true) {
                                onNavigateToHospitalSelection()
                            } else {
                                // Even if API fails, navigate — hospitals page has mock data
                                onNavigateToHospitalSelection()
                            }
                        } catch (e: Exception) {
                            // Navigate anyway with mock data
                            onNavigateToHospitalSelection()
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = selectedEmergency != null,
                isLoading = isLoading,
                variant = GlowVariant.PRIMARY,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
