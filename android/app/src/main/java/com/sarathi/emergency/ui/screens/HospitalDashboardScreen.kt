package com.sarathi.emergency.ui.screens

import android.Manifest
import android.util.Log
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.sarathi.emergency.data.SessionManager
import com.sarathi.emergency.data.api.SarathiApi
import com.sarathi.emergency.data.models.HospitalCase
import com.sarathi.emergency.data.repository.SarathiRepository
import com.sarathi.emergency.ui.components.MapMarker
import com.sarathi.emergency.ui.components.MarkerColor
import com.sarathi.emergency.ui.components.OfflineMapView
import com.sarathi.emergency.ui.theme.DarkNavy
import com.sarathi.emergency.ui.theme.EmergencyOrange
import com.sarathi.emergency.ui.theme.EmergencyRed
import com.sarathi.emergency.ui.theme.PrimaryBlue
import com.sarathi.emergency.ui.theme.SuccessGreen
import com.sarathi.emergency.ui.theme.TextWhite
import com.sarathi.emergency.ui.theme.TextWhite30
import com.sarathi.emergency.ui.theme.TextWhite50
import com.sarathi.emergency.ui.theme.TextWhite70
import com.sarathi.emergency.ui.viewmodel.HospitalDashboardViewModel
import com.sarathi.emergency.ui.viewmodel.HospitalDashboardViewModelFactory
import com.sarathi.emergency.util.ExternalActionHandler
import com.sarathi.emergency.util.LocationHelper
import androidx.lifecycle.viewmodel.compose.viewModel

private const val TAG = "HospitalDashboard"

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HospitalDashboardScreen(
    hospitalId: String? = null,
    hospitalName: String,
    hospitalArea: String,
    viewModel: HospitalDashboardViewModel,
    sessionManager: SessionManager,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val locationHelper = remember { LocationHelper(context) }
    val uiState by viewModel.uiState.collectAsState()

    var latitude by remember { mutableDoubleStateOf(17.4426) }
    var longitude by remember { mutableDoubleStateOf(78.5006) }
    var hasLocation by remember { mutableStateOf(false) }
    val hospitalCases: List<HospitalCase> = uiState.cases
    val loading = uiState.loading
    val lastRefreshLabel = uiState.lastRefreshLabel

    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

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

    val activeHospitalId = remember(hospitalId) {
        hospitalId?.takeIf { it.isNotBlank() }
            ?: sessionManager.getHospitalId().takeIf { it.isNotBlank() }
    }
    val activeHospitalName = remember(hospitalName) {
        hospitalName.takeIf { it.isNotBlank() }
    }

    LaunchedEffect(activeHospitalId, activeHospitalName) {
        viewModel.startPolling(
            hospitalId = activeHospitalId,
            hospitalName = activeHospitalName
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopPolling()
        }
    }

    val mapMarkers = remember(hospitalCases, latitude, longitude, hasLocation) {
        buildList {
            add(MapMarker(latitude, longitude, hospitalName, "Hospital", MarkerColor.GREEN))
            hospitalCases.filter { it.status != "completed" }.forEach { case ->
                val latLng = parseMapUrl(case.driver?.liveMapUrl)
                val lat = latLng?.first ?: latitude
                val lng = latLng?.second ?: longitude
                add(
                    MapMarker(
                        latitude = lat,
                        longitude = lng,
                        title = "Case ${case.id.takeLast(6)}",
                        snippet = "${case.emergencyType} • ETA ${case.etaMinutes ?: "?"}m",
                        markerColor = if (case.emergencyType.contains("cardiac", true)) MarkerColor.RED else MarkerColor.BLUE
                    )
                )
            }
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
                                    .background(SuccessGreen.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.LocalHospital, null, tint = SuccessGreen)
                            }
                            Spacer(modifier = Modifier.size(10.dp))
                            Column {
                                Text("Hospital Dashboard", color = TextWhite, fontWeight = FontWeight.Black)
                                Text("$hospitalName • $hospitalArea", color = SuccessGreen, fontSize = 12.sp)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (loading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = SuccessGreen)
                                Spacer(modifier = Modifier.size(10.dp))
                            }
                            IconButton(onClick = onLogout) {
                                Icon(Icons.Default.Logout, "Logout", tint = EmergencyRed)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        HospDashStat("Assigned", hospitalCases.count { it.status == "assigned" }.toString(), EmergencyOrange)
                        HospDashStat("En Route", hospitalCases.count { it.status == "in-progress" }.toString(), PrimaryBlue)
                        HospDashStat("Arrived", hospitalCases.count { it.status == "completed" }.toString(), SuccessGreen)
                        HospDashStat("Critical", hospitalCases.count { it.emergencyType.contains("cardiac", true) }.toString(), EmergencyRed)
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
                        Icon(Icons.Default.Map, null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Live Ambulance Tracker", color = TextWhite, fontWeight = FontWeight.Bold)
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
                        showMyLocation = false,
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
                Text("Incoming Cases", color = TextWhite, fontWeight = FontWeight.Black, fontSize = 18.sp)
                OutlinedButton(onClick = {
                    viewModel.refresh(
                        hospitalId = activeHospitalId,
                        hospitalName = activeHospitalName
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
                items(hospitalCases, key = { it.id }) { hospitalCase ->
                    HospitalCaseCard(
                        case = hospitalCase,
                        onUpdateStatus = { newStatus ->
                            viewModel.updateCaseStatus(hospitalCase.id, newStatus)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HospitalCaseCard(
    case: HospitalCase,
    onUpdateStatus: (String) -> Unit
) {
    val statusColor = when (case.hospitalCaseStatus.lowercase()) {
        "pending" -> EmergencyOrange
        "registered" -> PrimaryBlue
        "ready" -> SuccessGreen
        else -> TextWhite70
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = Brush.linearGradient(listOf(statusColor.copy(alpha = 0.4f), Color.Transparent)))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("ID ${case.id.takeLast(6)}", color = TextWhite, fontWeight = FontWeight.Bold)
                Text(case.hospitalCaseStatus.uppercase(), color = statusColor, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(case.emergencyType, color = TextWhite, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, null, tint = TextWhite70, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.size(4.dp))
                Text(case.user?.fullName ?: "Unknown Patient", color = TextWhite70, fontSize = 12.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Phone, null, tint = TextWhite70, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.size(4.dp))
                Text(case.user?.phone ?: "N/A", color = TextWhite50, fontSize = 12.sp)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DirectionsCar, null, tint = TextWhite70, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(case.driver?.vehicleNumber ?: "N/A", color = TextWhite50, fontSize = 11.sp)
                }
                if (case.etaMinutes != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, null, tint = EmergencyOrange, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("ETA ${case.etaMinutes}m", color = EmergencyOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (case.hospitalCaseStatus == "pending") {
                    Button(
                        onClick = { onUpdateStatus("registered") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("Register", fontSize = 12.sp)
                    }
                }
                if (case.hospitalCaseStatus == "registered") {
                    Button(
                        onClick = { onUpdateStatus("ready") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                    ) {
                        Text("Mark Ready", fontSize = 12.sp)
                    }
                }
                if (!case.driver?.liveMapUrl.isNullOrBlank()) {
                    val context = LocalContext.current
                    OutlinedButton(
                        onClick = {
                            case.driver?.liveMapUrl?.let { url ->
                                ExternalActionHandler.openUrl(
                                    context = context,
                                    url = url,
                                    blockedMessage = "External tracking map disabled in app mode"
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = Brush.linearGradient(listOf(TextWhite30, TextWhite30)))
                    ) {
                        Text("Track", color = TextWhite, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun HospDashStat(label: String, value: String, color: Color) {
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
    val coordPart = mapUrl.substring(index + marker.length)
    val split = coordPart.split(",")
    if (split.size != 2) return null
    val lat = split[0].toDoubleOrNull()
    val lng = split[1].toDoubleOrNull()
    return if (lat != null && lng != null) lat to lng else null
}
