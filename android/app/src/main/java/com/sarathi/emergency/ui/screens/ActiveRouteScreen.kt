package com.sarathi.emergency.ui.screens

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.sarathi.emergency.ui.components.*
import com.sarathi.emergency.ui.theme.*
import com.sarathi.emergency.ui.viewmodel.ActiveRouteUiState
import com.sarathi.emergency.ui.viewmodel.ActiveRouteViewModel
import com.sarathi.emergency.util.ExternalActionHandler
import com.sarathi.emergency.util.LocationHelper

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ActiveRouteScreen(
    viewModel: ActiveRouteViewModel,
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
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

    LaunchedEffect(latitude, longitude) {
        if (hasLocation) {
            viewModel.analyzeRoute(latitude, longitude)
        }
    }

    val mapMarkers = remember(latitude, longitude, hasLocation, uiState.destinationLat, uiState.destinationLng) {
        buildList {
            if (hasLocation) {
                add(MapMarker(latitude, longitude, "Ambulance", "Live Tracking", MarkerColor.BLUE))
            }
            add(MapMarker(uiState.destinationLat, uiState.destinationLng, uiState.destinationName, "Emergency Location", MarkerColor.RED))
        }
    }

    val mapRoutes = remember(latitude, longitude, uiState.destinationLat, uiState.destinationLng) {
        listOf(MapRoute(points = listOf(latitude to longitude, uiState.destinationLat to uiState.destinationLng)))
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkNavy)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) }
                    Column {
                        Text("Live Mission", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(uiState.status, color = SuccessGreen, fontSize = 12.sp)
                    }
                }
                IconButton(onClick = { 
                    ExternalActionHandler.sendSms(context, "SARATHI Update: Ambulance is en route. ETA ${uiState.etaMinutes} min.")
                }) {
                    Icon(Icons.Default.Textsms, "SMS", tint = PrimaryBlue)
                }
            }

            // Map Area
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                OfflineMapView(
                    modifier = Modifier.fillMaxSize(),
                    centerLatitude = latitude,
                    centerLongitude = longitude,
                    markers = mapMarkers,
                    routes = mapRoutes,
                    showMyLocation = hasLocation,
                    myLatitude = latitude,
                    myLongitude = longitude
                )

                // ETA Overlay
                Card(
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkNavy.copy(alpha = 0.8f))
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, null, tint = EmergencyOrange, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ETA: ${uiState.etaMinutes} MIN", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Action Panel
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131A2F))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GlowButton(
                            text = "NOTIFY OPS",
                            onClick = { viewModel.notifyAuthorities(latitude, longitude) },
                            isLoading = uiState.notifyLoading,
                            variant = GlowVariant.PURPLE,
                            modifier = Modifier.weight(1f),
                            icon = { Icon(Icons.Default.NotificationsActive, null, tint = Color.White) }
                        )

                        GlowButton(
                            text = "NAVIGATE",
                            onClick = { ExternalActionHandler.openNavigation(context, uiState.destinationLat, uiState.destinationLng) },
                            variant = GlowVariant.DANGER,
                            modifier = Modifier.weight(1f),
                            icon = { Icon(Icons.Default.Navigation, null, tint = Color.White) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    GlowButton(
                        text = "MISSION COMPLETE",
                        onClick = onComplete,
                        variant = GlowVariant.SUCCESS,
                        modifier = Modifier.fillMaxWidth(),
                        icon = { Icon(Icons.Default.CheckCircle, null, tint = Color.White) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // AI Analysis
                    if (uiState.isAnalyzing || uiState.aiAnalysis != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.AutoAwesome, null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = uiState.aiAnalysis ?: "Analyzing route dynamics...",
                                    color = TextWhite70,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
