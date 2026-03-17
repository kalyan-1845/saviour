package com.sarathi.emergency.ui.screens

import android.Manifest
import android.util.Log
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.sarathi.emergency.data.SessionManager
import com.sarathi.emergency.data.api.SarathiApi
import com.sarathi.emergency.data.models.PoliceAlert
import com.sarathi.emergency.data.repository.SarathiRepository
import com.sarathi.emergency.ui.components.MapMarker
import com.sarathi.emergency.ui.components.MapRoute
import com.sarathi.emergency.ui.components.MarkerColor
import com.sarathi.emergency.ui.components.OfflineMapView
import com.sarathi.emergency.ui.theme.DarkNavy
import com.sarathi.emergency.ui.theme.EmergencyOrange
import com.sarathi.emergency.ui.theme.EmergencyRed
import com.sarathi.emergency.ui.theme.PrimaryBlue
import com.sarathi.emergency.ui.theme.SuccessGreen
import com.sarathi.emergency.ui.theme.TextBlue300
import com.sarathi.emergency.ui.theme.TextWhite
import com.sarathi.emergency.ui.theme.TextWhite50
import com.sarathi.emergency.ui.theme.TextWhite70
import com.sarathi.emergency.ui.viewmodel.PoliceDashboardViewModel
import com.sarathi.emergency.ui.viewmodel.PoliceDashboardViewModelFactory
import com.sarathi.emergency.util.ExternalActionHandler
import com.sarathi.emergency.util.LocationHelper
import androidx.lifecycle.viewmodel.compose.viewModel

private const val TAG = "PoliceDashboard"

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PoliceDashboardScreen(
    stationId: String? = null,
    stationName: String,
    stationArea: String,
    api: SarathiApi,
    sessionManager: SessionManager,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val locationHelper = remember { LocationHelper(context) }
    val repository = remember(api) { SarathiRepository(api) }
    val viewModel: PoliceDashboardViewModel = viewModel(
        factory = remember(repository) { PoliceDashboardViewModelFactory(repository) }
    )
    val uiState by viewModel.uiState.collectAsState()

    var latitude by remember { mutableDoubleStateOf(17.4426) }
    var longitude by remember { mutableDoubleStateOf(78.5006) }
    var hasLocation by remember { mutableStateOf(false) }
    val alerts: List<PoliceAlert> = uiState.alerts
    val loading = uiState.loading
    val lastRefreshLabel = uiState.lastRefreshLabel

    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val activeStationId = remember(stationId) {
        stationId?.takeIf { it.isNotBlank() }
            ?: sessionManager.getPoliceStationId().takeIf { it.isNotBlank() }
    }
    val activeStationName = remember(stationName) { stationName.takeIf { it.isNotBlank() } }

    LaunchedEffect(locationPermission.status.isGranted) {
        if (!locationPermission.status.isGranted) {
            locationPermission.launchPermissionRequest()
            return@LaunchedEffect
        }
        locationHelper.getLastLocation { loc ->
            if (loc != null) {
                latitude = loc.latitude
                longitude = loc.longitude
                hasLocation = true
            }
        }
    }

    LaunchedEffect(activeStationId, activeStationName) {
        viewModel.startPolling(
            stationId = activeStationId,
            stationName = activeStationName
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopPolling()
        }
    }

    val mapMarkers = remember(alerts, latitude, longitude) {
        buildList {
            if (hasLocation) {
                add(MapMarker(latitude, longitude, stationName, stationArea, MarkerColor.BLUE))
            }
            alerts.filter { it.status != "completed" }.forEach { alert ->
                val latLng = parseMapUrl(alert.driver?.liveMapUrl)
                val lat = latLng?.first ?: latitude
                val lng = latLng?.second ?: longitude
                add(
                    MapMarker(
                        latitude = lat,
                        longitude = lng,
                        title = "Alert ${alert.id.takeLast(6)}",
                        snippet = "${alert.emergencyType} • ETA ${alert.etaMinutes ?: "?"}m",
                        markerColor = if (alert.emergencyType.equals("cardiac", true)) MarkerColor.RED else MarkerColor.ORANGE
                    )
                )
            }
        }
    }

    val mapRoutes = remember(alerts, latitude, longitude) {
        alerts.filter { it.status != "completed" }.map { alert ->
            val latLng = parseMapUrl(alert.driver?.liveMapUrl)
            val lat = latLng?.first ?: latitude
            val lng = latLng?.second ?: longitude
            MapRoute(
                points = listOf(lat to lng, latitude to longitude),
                color = if (alert.emergencyType.equals("cardiac", true)) android.graphics.Color.parseColor("#DC2626") else android.graphics.Color.parseColor("#4F46E5"),
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
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Shield, null, tint = PrimaryBlue)
                            }
                            Spacer(modifier = Modifier.size(10.dp))
                            Column {
                                Text("Police Dashboard", color = TextWhite, fontWeight = FontWeight.Black)
                                Text("$stationName • $stationArea", color = TextBlue300, fontSize = 12.sp)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (loading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = PrimaryBlue)
                                Spacer(modifier = Modifier.size(10.dp))
                            }
                            IconButton(onClick = onLogout) {
                                Icon(Icons.Default.Logout, "Logout", tint = EmergencyRed)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        DashStatCard("Active", alerts.count { it.status != "completed" }.toString(), PrimaryBlue)
                        DashStatCard("Critical", alerts.count { it.emergencyType.equals("cardiac", true) }.toString(), EmergencyRed)
                        DashStatCard("Completed", alerts.count { it.status == "completed" }.toString(), SuccessGreen)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Refresh: $lastRefreshLabel", color = TextWhite50, fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Map, null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Green Corridor Map", color = TextWhite, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OfflineMapView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        centerLatitude = if (hasLocation) latitude else 17.4426,
                        centerLongitude = if (hasLocation) longitude else 78.5006,
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Route Alerts", color = TextWhite, fontWeight = FontWeight.Black, fontSize = 18.sp)
                OutlinedButton(onClick = {
                    viewModel.refresh(
                        stationId = activeStationId,
                        stationName = activeStationName
                    )
                }) {
                    Text("Refresh", color = TextWhite)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(alerts, key = { it.id }) { alert ->
                    AlertCard(alert)
                }
            }
        }
    }
}

@Composable
private fun AlertCard(alert: PoliceAlert) {
    val statusColor = when (alert.status.lowercase()) {
        "pending", "assigned" -> EmergencyOrange
        "in-progress", "accepted" -> PrimaryBlue
        "completed" -> SuccessGreen
        else -> TextWhite70
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = Brush.linearGradient(listOf(statusColor.copy(alpha = 0.4f), Color.Transparent))
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("ID ${alert.id.takeLast(6)}", color = TextWhite, fontWeight = FontWeight.Bold)
                Text(alert.status.uppercase(), color = statusColor, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Emergency, null, tint = EmergencyRed, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text(alert.emergencyType.uppercase(), color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            if (!alert.policeAlertMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(alert.policeAlertMessage, color = TextWhite70, fontSize = 12.sp, maxLines = 2)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, tint = TextWhite70, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(alert.driver?.fullName ?: "Driver pending", color = TextWhite70, fontSize = 11.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DirectionsCar, null, tint = TextWhite70, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(alert.driver?.vehicleNumber ?: "N/A", color = TextWhite50, fontSize = 11.sp)
                }
                if (alert.etaMinutes != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, null, tint = EmergencyOrange, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("${alert.etaMinutes}m", color = EmergencyOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (!alert.driver?.liveMapUrl.isNullOrBlank()) {
                val context = LocalContext.current
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Track driver live",
                    color = TextBlue300,
                    fontSize = 12.sp,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable {
                        alert.driver?.liveMapUrl?.let { url ->
                            ExternalActionHandler.openUrl(
                                context = context,
                                url = url,
                                blockedMessage = "External tracking map disabled in app mode"
                            )
                        }
                    }
                )
            }
            if (!alert.hospitalName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalHospital, null, tint = SuccessGreen, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(alert.hospitalName, color = TextWhite70, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun DashStatCard(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text(label, color = TextWhite50, fontSize = 10.sp)
    }
}

private fun parseMapUrl(mapUrl: String?): Pair<Double, Double>? {
    if (mapUrl.isNullOrBlank()) return null
    val marker = "q="
    val index = mapUrl.indexOf(marker)
    if (index == -1) return null
    val coords = mapUrl.substring(index + marker.length).split(",")
    if (coords.size != 2) return null
    val lat = coords[0].toDoubleOrNull()
    val lng = coords[1].toDoubleOrNull()
    return if (lat != null && lng != null) lat to lng else null
}
