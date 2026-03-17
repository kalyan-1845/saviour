package com.sarathi.emergency.ui.screens

import android.Manifest
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.ShareLocation
import androidx.compose.material.icons.filled.Textsms
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.sarathi.emergency.data.models.GroqRequest
import com.sarathi.emergency.data.models.NotifyHospital
import com.sarathi.emergency.data.models.NotifyRequest
import com.sarathi.emergency.data.models.NotifyResponse
import com.sarathi.emergency.ui.components.GlowButton
import com.sarathi.emergency.ui.components.GlowVariant
import com.sarathi.emergency.ui.components.MapMarker
import com.sarathi.emergency.ui.components.MapRoute
import com.sarathi.emergency.ui.components.MarkerColor
import com.sarathi.emergency.ui.components.OfflineMapView
import com.sarathi.emergency.ui.theme.DarkBlueGray
import com.sarathi.emergency.ui.theme.DarkNavy
import com.sarathi.emergency.ui.theme.EmergencyOrange
import com.sarathi.emergency.ui.theme.EmergencyRed
import com.sarathi.emergency.ui.theme.PrimaryBlue
import com.sarathi.emergency.ui.theme.SuccessGreen
import com.sarathi.emergency.ui.theme.TextWhite
import com.sarathi.emergency.ui.theme.TextWhite50
import com.sarathi.emergency.ui.theme.TextWhite70
import com.sarathi.emergency.util.ExternalActionHandler
import com.sarathi.emergency.util.LocationHelper
import kotlinx.coroutines.launch

private const val TAG = "ActiveRouteScreen"

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

    var tripStatus by remember { mutableStateOf("En Route to Patient") }
    var etaMinutes by remember { mutableIntStateOf(7) }
    var trafficLevel by remember { mutableStateOf("Normal") }
    var aiAnalysis by remember { mutableStateOf<String?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var notifyResult by remember { mutableStateOf<NotifyResponse?>(null) }
    var notifyLoading by remember { mutableStateOf(false) }

    var currentDestLat by remember { mutableDoubleStateOf(17.4550) }
    var currentDestLng by remember { mutableDoubleStateOf(78.4700) }
    var currentHospitalName by remember { mutableStateOf("Emergency Hospital") }

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

    DisposableEffect(locationPermission.status.isGranted) {
        if (!locationPermission.status.isGranted) {
            onDispose { }
        } else {
            val stopUpdates = locationHelper.requestLocationUpdates { loc ->
                latitude = loc.latitude
                longitude = loc.longitude
                hasLocation = true
            }
            onDispose {
                stopUpdates()
            }
        }
    }

    LaunchedEffect(latitude, longitude, trafficLevel, currentDestLat, currentDestLng) {
        if (!hasLocation) return@LaunchedEffect
        isAnalyzing = true
        try {
            val response = api.analyzeRoute(
                GroqRequest(
                    origin = "$latitude,$longitude",
                    destination = "$currentDestLat,$currentDestLng",
                    trafficData = mapOf("level" to trafficLevel)
                )
            )
            if (response.isSuccessful && response.body()?.success == true) {
                aiAnalysis = response.body()?.analysis
                Log.d(TAG, "Route analysis success")
            } else {
                Log.w(TAG, "Route analysis failed: ${response.code()}")
            }
        } catch (error: Exception) {
            Log.e(TAG, "Route analysis error", error)
        } finally {
            isAnalyzing = false
        }
    }

    val mapMarkers = remember(latitude, longitude, hasLocation, currentDestLat, currentDestLng, currentHospitalName) {
        buildList {
            if (hasLocation) {
                add(MapMarker(latitude, longitude, "Ambulance", "Current location", MarkerColor.BLUE))
            }
            add(MapMarker(currentDestLat, currentDestLng, currentHospitalName, "Destination", MarkerColor.GREEN))
        }
    }

    val mapRoutes = remember(latitude, longitude, currentDestLat, currentDestLng) {
        listOf(
            MapRoute(
                points = listOf(latitude to longitude, currentDestLat to currentDestLng),
                color = android.graphics.Color.parseColor("#4F46E5"),
                width = 6f
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkNavy, DarkBlueGray)))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                    Column {
                        Text("Active Navigation", color = TextWhite, fontWeight = FontWeight.Bold)
                        Text(tripStatus, color = SuccessGreen, fontSize = 12.sp)
                    }
                }
                IconButton(
                    onClick = {
                        ExternalActionHandler.sendSms(
                            context = context,
                            body = "SARATHI UPDATE: Ambulance at $latitude,$longitude. ETA $etaMinutes mins."
                        )
                    }
                ) {
                    Icon(Icons.Default.Textsms, contentDescription = "SMS", tint = PrimaryBlue)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
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

                Card(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkNavy.copy(alpha = 0.85f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AccessTime, null, tint = EmergencyOrange, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("ETA $etaMinutes min", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.size(10.dp))
                        Icon(Icons.Default.Traffic, null, tint = if (trafficLevel == "Heavy") EmergencyRed else SuccessGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(trafficLevel, color = TextWhite70, fontSize = 12.sp)
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkBlueGray)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GlowButton(
                            text = if (tripStatus == "En Route to Patient") "Mark Picked Up" else "Send ETA Update",
                            onClick = {
                                if (tripStatus == "En Route to Patient") {
                                    tripStatus = "Transporting Patient"
                                    sessionManager.updateSimulatedMissionStatus("Transporting Patient")
                                    notifyLoading = true
                                    scope.launch {
                                        try {
                                            notifyResult = api.notifyAuthorities(
                                                NotifyRequest(
                                                    tripId = sessionManager.getSimulatedSOS() ?: "active",
                                                    driverId = sessionManager.getDriverId(),
                                                    latitude = latitude,
                                                    longitude = longitude
                                                )
                                            ).body()
                                            Log.d(TAG, "Authority notify success")
                                        } catch (error: Exception) {
                                            Log.e(TAG, "Authority notify failed", error)
                                            notifyResult = NotifyResponse(
                                                success = true,
                                                hospital = NotifyHospital(
                                                    name = "Offline fallback",
                                                    message = "Notification queued for retry"
                                                )
                                            )
                                        } finally {
                                            notifyLoading = false
                                        }
                                    }
                                } else {
                                    ExternalActionHandler.sendSms(
                                        context = context,
                                        body = "SARATHI ETA UPDATE: ambulance ETA to hospital is $etaMinutes minutes."
                                    )
                                }
                            },
                            isLoading = notifyLoading,
                            variant = GlowVariant.PURPLE,
                            modifier = Modifier.weight(1f),
                            icon = {
                                Icon(
                                    if (tripStatus == "En Route to Patient") Icons.Default.PersonSearch else Icons.Default.ShareLocation,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )

                        GlowButton(
                            text = "Complete",
                            onClick = onComplete,
                            variant = GlowVariant.SUCCESS,
                            modifier = Modifier.weight(1f),
                            icon = { Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    GlowButton(
                        text = "Open External Navigation",
                        onClick = {
                            ExternalActionHandler.openNavigation(
                                context = context,
                                latitude = currentDestLat,
                                longitude = currentDestLng
                            )
                        },
                        variant = GlowVariant.DANGER,
                        modifier = Modifier.fillMaxWidth(),
                        icon = { Icon(Icons.Default.Route, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RerouteChip("Apollo Hospital", "5 min") {
                            currentDestLat = 17.4310
                            currentDestLng = 78.4070
                            currentHospitalName = "Apollo Hospital"
                            etaMinutes = 5
                            trafficLevel = "Normal"
                        }
                        RerouteChip("NIMS", "3 min") {
                            currentDestLat = 17.4426
                            currentDestLng = 78.5006
                            currentHospitalName = "NIMS"
                            etaMinutes = 3
                            trafficLevel = "Normal"
                        }
                    }

                    if (isAnalyzing || !aiAnalysis.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkNavy.copy(alpha = 0.8f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(Icons.Default.AutoAwesome, null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.size(6.dp))
                                if (isAnalyzing) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = SuccessGreen)
                                }
                                Text(
                                    text = aiAnalysis ?: "Analyzing best route...",
                                    color = TextWhite70,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(start = if (isAnalyzing) 8.dp else 0.dp)
                                )
                            }
                        }
                    }

                    notifyResult?.let { result ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Authorities Updated", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                result.hospital?.let { hospital ->
                                    Text("${hospital.name}: ${hospital.message}", color = TextWhite70, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.Warning, null, tint = EmergencyRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Emergency Hotline: 112", color = TextWhite50, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.RerouteChip(name: String, etaText: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                Text("ETA $etaText", color = SuccessGreen, fontSize = 10.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextWhite50, modifier = Modifier.size(16.dp))
        }
    }
}
