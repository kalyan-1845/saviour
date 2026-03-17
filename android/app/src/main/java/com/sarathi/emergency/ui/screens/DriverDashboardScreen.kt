package com.sarathi.emergency.ui.screens

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
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
import com.sarathi.emergency.data.models.AssignedTrip
import com.sarathi.emergency.data.models.TripLocation
import com.sarathi.emergency.ui.components.*
import com.sarathi.emergency.ui.theme.*
import com.sarathi.emergency.ui.viewmodel.DriverUiState
import com.sarathi.emergency.ui.viewmodel.DriverViewModel
import com.sarathi.emergency.util.LocationHelper

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DriverDashboardScreen(
    viewModel: DriverViewModel,
    onNavigateToHospitalSelection: () -> Unit,
    onNavigateToActiveRoute: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val locationHelper = remember { LocationHelper(context) }

    var latitude by remember { mutableDoubleStateOf(17.4426) }
    var longitude by remember { mutableDoubleStateOf(78.5006) }
    var hasLocation by remember { mutableStateOf(false) }

    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    LaunchedEffect(locationPermission.status.isGranted) {
        if (!locationPermission.status.isGranted) {
            locationPermission.launchPermissionRequest()
        } else {
            locationHelper.getLastLocation { loc ->
                if (loc != null) {
                    latitude = loc.latitude
                    longitude = loc.longitude
                    hasLocation = true
                }
            }
        }
    }

    DisposableEffect(locationPermission.status.isGranted) {
        if (locationPermission.status.isGranted) {
            val stopUpdates = locationHelper.requestLocationUpdates { loc ->
                latitude = loc.latitude
                longitude = loc.longitude
                hasLocation = true
            }
            onDispose { stopUpdates() }
        } else {
            onDispose { }
        }
    }

    val activeTrip = (uiState as? DriverUiState.Assigned)?.trip

    val mapMarkers = remember(latitude, longitude, activeTrip) {
        buildList {
            add(MapMarker(latitude, longitude, "🚑 My Ambulance", "Online", MarkerColor.BLUE))
            activeTrip?.pickupLocation?.let { loc ->
                add(MapMarker(loc.latitude, loc.longitude, "📍 SOS PICKUP", "Active Patient", MarkerColor.RED))
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(DarkNavy, Color(0xFF131A2F), DarkPurple)))) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Driver Dashboard", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text("Active Mission Control", color = TextBlue300, fontSize = 13.sp)
                }
                IconButton(onClick = { 
                    viewModel.logout()
                    onLogout()
                }) { 
                    Icon(Icons.Default.PowerSettingsNew, "Logout", tint = EmergencyRed) 
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Status Bar
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f))) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = if (hasLocation) SuccessGreen else EmergencyOrange
                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(statusColor))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(if (hasLocation) "Online • Ready" else "Wait • Finding GPS", color = statusColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    if (uiState is DriverUiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PrimaryBlue)
                    } else {
                        Icon(Icons.Default.Sync, null, tint = TextWhite50, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Active Trip Alert
            AnimatedVisibility(visible = activeTrip != null) {
                activeTrip?.let { trip ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = EmergencyRed.copy(alpha = 0.15f)),
                        border = BorderStroke(2.dp, Brush.linearGradient(listOf(EmergencyRed, EmergencyOrange)))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(EmergencyRed))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("🚨 ACTIVE ASSIGNMENT", color = Color.White, fontWeight = FontWeight.Black)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Type: ${trip.emergencyType.uppercase()}", color = TextWhite70, fontSize = 12.sp)
                            Text("ID: ${trip.id}", color = Color.White, fontWeight = FontWeight.Bold)
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            GlowButton(
                                text = "NAVIGATE TO PATIENT",
                                onClick = onNavigateToActiveRoute,
                                variant = GlowVariant.DANGER,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Map Card
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f))) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Service Area", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    OfflineMapView(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        centerLatitude = latitude,
                        centerLongitude = longitude,
                        zoomLevel = 14.0,
                        markers = mapMarkers,
                        showMyLocation = true,
                        myLatitude = latitude,
                        myLongitude = longitude
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
            
            if (activeTrip == null) {
                Text(
                    "Waiting for emergency dispatch signals...",
                    color = TextWhite50,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp
                )
            }
        }
    }
}
