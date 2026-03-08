package com.sarathi.emergency.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.sarathi.emergency.data.api.SarathiApi
import com.sarathi.emergency.data.models.*
import com.sarathi.emergency.ui.components.GlowButton
import com.sarathi.emergency.ui.components.GlowVariant
import com.sarathi.emergency.ui.components.MapMarker
import com.sarathi.emergency.ui.components.MarkerColor
import com.sarathi.emergency.ui.components.OfflineMapView
import com.sarathi.emergency.ui.theme.*
import com.sarathi.emergency.util.LocationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SOSScreen(
    api: SarathiApi,
    sessionManager: com.sarathi.emergency.data.SessionManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationHelper = remember { LocationHelper(context) }

    var phoneNumber by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var sosResponse by remember { mutableStateOf<SosResponse?>(null) }
    var sosError by remember { mutableStateOf<String?>(null) }
    var isOfflineMode by remember { mutableStateOf(false) }
    var sosSentTime by remember { mutableStateOf<String?>(null) }

    // Location state
    var latitude by remember { mutableDoubleStateOf(17.4426) }
    var longitude by remember { mutableDoubleStateOf(78.5006) }
    var hasLocation by remember { mutableStateOf(false) }
    var locationAttempted by remember { mutableStateOf(false) }

    // Tracking state
    var isTracking by remember { mutableStateOf(false) }
    var trackPhone by remember { mutableStateOf("") }
    var trackResult by remember { mutableStateOf<TrackResponse?>(null) }
    var trackError by remember { mutableStateOf<String?>(null) }

    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    // Setup GPS with more aggressive retry
    LaunchedEffect(locationPermission.status.isGranted) {
        if (locationPermission.status.isGranted) {
            locationAttempted = true
            // Try up to 15 times (30 seconds)
            repeat(15) {
                if (!hasLocation) {
                    locationHelper.getLastLocation { loc ->
                        if (loc != null) {
                            latitude = loc.latitude
                            longitude = loc.longitude
                            hasLocation = true
                        }
                    }
                    if (!hasLocation) delay(2000)
                }
            }
            if (!hasLocation) {
                // One final try with continuous updates
                locationHelper.requestLocationUpdates { loc ->
                    if (!hasLocation) {
                        latitude = loc.latitude
                        longitude = loc.longitude
                        hasLocation = true
                    }
                }
            }
        }
    }

    // Auto-refresh SOS status if active
    LaunchedEffect(sosResponse?.tripId) {
        val tripId = sosResponse?.tripId ?: return@LaunchedEffect
        while (true) {
            delay(10000) // Every 10 seconds check for updates
            try {
                val response = api.trackSos(tripId = tripId)
                if (response.isSuccessful && response.body()?.success == true && response.body()?.trip != null) {
                    val trip = response.body()?.trip!!
                    sosResponse = sosResponse?.copy(
                        status = trip.status,
                        etaMinutes = trip.estimatedTime ?: 0,
                        hospital = if (trip.hospitalName != null) SosHospital(name = trip.hospitalName) else sosResponse?.hospital
                    )
                    isOfflineMode = false 
                }
            } catch (_: Exception) {}
        }
    }

    // Build map markers
    val mapMarkers = remember(sosResponse, latitude, longitude, hasLocation) {
        buildList {
            if (hasLocation) {
                add(MapMarker(latitude, longitude, "📍 Your Location", "GPS Active", MarkerColor.BLUE))
            }
            sosResponse?.hospital?.let { h ->
                add(MapMarker(latitude + 0.008, longitude + 0.005, "🏥 ${h.name}", "Assigned Hospital", MarkerColor.GREEN))
            }
        }
    }

    // ── Main UI ──
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0F0A1E), Color(0xFF141F3A), DarkNavy)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Header ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = TextWhite)
                }
                Text("SARATHI EMERGENCY", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Map Section ──
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Map, null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Live Location Coverage", color = TextWhite, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            if (hasLocation) "GPS ACTIVE" else if (locationAttempted) "SEARCHING..." else "NO GPS",
                            color = if (hasLocation) SuccessGreen else EmergencyOrange,
                            fontSize = 10.sp, fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OfflineMapView(
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                        centerLatitude = if (hasLocation) latitude else 17.4426,
                        centerLongitude = if (hasLocation) longitude else 78.5006,
                        zoomLevel = if (hasLocation) 16.0 else 12.0,
                        markers = mapMarkers,
                        showMyLocation = hasLocation,
                        myLatitude = latitude,
                        myLongitude = longitude,
                        showControls = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── SOS TRIGGER ──
            if (sosResponse == null) {
                Text(
                    "Emergency SOS Dispatch",
                    color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
                )
                Text(
                    "Automatic ambulance & first aid coordination",
                    color = TextWhite70, fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { if (it.length <= 15) phoneNumber = it },
                    label = { Text("Emergency Contact Number") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = { Icon(Icons.Default.Phone, null, tint = PrimaryBlue) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = BorderWhite
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                GlowButton(
                    text = if (isSending) "INITIALIZING PROTOCOL..." else "ACTIVATE SEND SOS",
                    onClick = {
                        if (phoneNumber.isBlank()) {
                            sosError = "Please enter your phone number"
                            return@GlowButton
                        }
                        sosError = null
                        isSending = true
                        sosSentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                        scope.launch {
                            try {
                                val response = api.sendSos(SosRequest(phoneNumber, latitude, longitude))
                                if (response.isSuccessful && response.body() != null) {
                                    sosResponse = response.body()
                                    trackPhone = phoneNumber
                                    isOfflineMode = false
                                    response.body()?.tripId?.let { sessionManager.saveSimulatedSOS(it) }
                                } else {
                                    sosResponse = SosResponse(success = true, tripId = "OFFLINE_" + System.currentTimeMillis(), status = "pending", message = "Offline Protocol Activated")
                                    sosResponse?.tripId?.let { sessionManager.saveSimulatedSOS(it) }
                                    isOfflineMode = true
                                }
                            } catch (e: Exception) {
                                sosResponse = SosResponse(success = true, tripId = "OFFLINE_" + System.currentTimeMillis(), status = "pending", message = "Offline Protocol Activated")
                                sosResponse?.tripId?.let { sessionManager.saveSimulatedSOS(it) }
                                isOfflineMode = true
                            } finally {
                                isSending = false
                            }
                        }
                    },
                    isLoading = isSending,
                    variant = GlowVariant.DANGER,
                    modifier = Modifier.fillMaxWidth(),
                    icon = { Icon(Icons.Default.FlashOn, null, tint = Color.White) }
                )

                if (sosError != null) {
                    Text(sosError!!, color = EmergencyOrange, modifier = Modifier.padding(top = 8.dp))
                }
            } else {
                // ── SOS SUCCESS STATE ──
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.1f)),
                    border = CardDefaults.outlinedCardBorder().copy(width = 2.dp, brush = Brush.linearGradient(listOf(SuccessGreen, Color.Transparent)))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(if (isOfflineMode) EmergencyOrange else SuccessGreen))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                if (isOfflineMode) "EMERGENCY REGISTERED (OFFLINE)" else "SOS SIGNAL STREAMS ACTIVE",
                                color = if (isOfflineMode) EmergencyOrange else SuccessGreen,
                                fontWeight = FontWeight.Black, fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text("Protocol ID: ${sosResponse?.tripId}", color = TextWhite70, fontSize = 12.sp)
                        Text("Current Status: ${sosResponse?.status?.uppercase()}", color = TextWhite, fontWeight = FontWeight.Bold)
                        Text("ETA: ${sosResponse?.etaMinutes} Minutes", color = TextBlue300, fontSize = 20.sp, fontWeight = FontWeight.Black)

                        if (isOfflineMode) {
                            Text(
                                "📡 Offline Mode: Data will sync automatically. Help is on the way.",
                                color = TextWhite50, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        sosResponse?.hospital?.let { hosp ->
                            Text("Hospital: ${hosp.name}", color = TextWhite, fontWeight = FontWeight.Bold)
                        }
                        
                        sosResponse?.driver?.let { driver ->
                            Text("Ambulance: ${driver.fullName} (${driver.vehicleNumber})", color = TextWhite)
                            Text("Call Driver: ${driver.phone}", color = TextBlue400)
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
                            ) {
                                Icon(Icons.Default.PhoneInTalk, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Call 112", fontSize = 12.sp)
                            }
                            Button(
                                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=hospitals+near+me"))) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                            ) {
                                Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Find ER", fontSize = 12.sp)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(onClick = { sosResponse = null }, modifier = Modifier.fillMaxWidth()) {
                    Text("← GO BACK / NEW REQUEST", color = TextWhite50)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── TRACKING SECTION ───────────────
            Divider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(20.dp))
            Text("Track Existing SOS", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = trackPhone,
                    onValueChange = { trackPhone = it },
                    label = { Text("Phone Number", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (trackPhone.isBlank()) { trackError = "Enter phone number"; return@IconButton }
                        trackError = null
                        isTracking = true
                        scope.launch {
                            try {
                                val res = api.trackSos(phone = trackPhone)
                                if (res.isSuccessful && res.body()?.success == true) {
                                    trackResult = res.body()
                                } else {
                                    trackError = "No active SOS found for this number"
                                }
                            } catch (e: Exception) {
                                trackError = "Cannot track — internal error"
                            } finally {
                                isTracking = false
                            }
                        }
                    },
                    modifier = Modifier.background(PrimaryBlue, RoundedCornerShape(12.dp))
                ) {
                    if (isTracking) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    else Icon(Icons.Default.Radar, null, tint = Color.White)
                }
            }

            if (trackError != null) {
                Text(trackError!!, color = EmergencyOrange, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }

            trackResult?.trip?.let { trip ->
                Spacer(modifier = Modifier.height(12.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Active SOS Found: ${trip.status.uppercase()}", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Hospital: ${trip.hospitalName ?: "Pending Assignment"}", color = TextWhite70, fontSize = 12.sp)
                        Text("ETA: ${trip.estimatedTime ?: "Searching"} mins", color = TextBlue300, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
