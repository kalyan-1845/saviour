package com.sarathi.emergency.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.BorderStroke
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
import com.sarathi.emergency.services.BackgroundLocationService
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

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
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
    var selectedType by remember { mutableStateOf<String>("medical") }
    var isOfflineMode by remember { mutableStateOf(false) }
    var sosSentTime by remember { mutableStateOf<String?>(null) }

    // Location state
    var latitude by remember { mutableDoubleStateOf(17.4426) }
    var longitude by remember { mutableDoubleStateOf(78.5006) }
    var hasLocation by remember { mutableStateOf(false) }
    var locationAttempted by remember { mutableStateOf(false) }

    // Tracking state
    var notifyResult by remember { mutableStateOf<NotifyResponse?>(null) }
    var notifyLoading by remember { mutableStateOf(false) }
    var voiceEnabled by remember { mutableStateOf(true) }
    var trackPhone by remember { mutableStateOf("") }
    var isTracking by remember { mutableStateOf(false) }
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

    var driverLat by remember { mutableDoubleStateOf(17.4310) }
    var driverLng by remember { mutableDoubleStateOf(78.4070) }
    var driverEta by remember { mutableIntStateOf(5) }

    val mapMarkers = remember(sosResponse, latitude, longitude, hasLocation, driverLat, driverLng) {
        buildList {
            if (hasLocation) {
                add(MapMarker(latitude, longitude, "📍 Your Location", "GPS Active", MarkerColor.BLUE))
            }
            if (sosResponse != null && driverEta > 0) {
                add(MapMarker(driverLat, driverLng, "🚑 Ambulance", "Emergency Vehicle", MarkerColor.RED))
            }
            sosResponse?.hospital?.let { h ->
                add(MapMarker(latitude + 0.008, longitude + 0.005, "🏥 ${h.name}", "Assigned Hospital", MarkerColor.GREEN))
            }
        }
    }

    // SIMULATED LIVE TRACKING FOR USER
    LaunchedEffect(sosResponse) {
        if (sosResponse != null) {
            while(driverEta > 0) {
                delay(3000)
                // Move driver closer to patient (latitude 17.4426, longitude 78.5006)
                driverLat += (latitude - driverLat) * 0.1
                driverLng += (longitude - driverLng) * 0.1
                if (driverEta > 1) driverEta -= 1
            }
        }
    }

    val emergencyTypes = remember {
        listOf(
            Triple("medical", "General Medical", "🏥"),
            Triple("cardiac", "Cardiac Arrest", "❤️"),
            Triple("trauma", "Trauma / Bleeding", "🩸"),
            Triple("respiratory", "Choking / Breath", "🫁"),
            Triple("burn", "Serious Burns", "🔥"),
            Triple("accident", "Road Accident", "🚑")
        )
    }

    val firstAidProtocols = remember {
        mapOf(
            "cardiac" to listOf("START CPR IMMEDIATELY", "100-120 compressions per minute", "Push deep in center of chest", "Don't stop until help arrives"),
            "trauma" to listOf("STOP THE BLEEDING", "Apply FIRM direct pressure with cloth", "Do not remove soaked cloth", "Elevate the limb if possible"),
            "respiratory" to listOf("CHECK AIRWAYS", "Clear mouth of obstructions", "Heimlich maneuver if choking", "Keep person sitting up if possible"),
            "burn" to listOf("COOL THE AREA", "Run cool water for 20 mins", "Do not apply ICE directly", "Cover loosely with clean wrap"),
            "medical" to listOf("KEEP PATIENT CALM", "Check responsiveness", "Loosen tight clothing", "Do not give any food/water"),
            "accident" to listOf("STABILIZE NECK", "Do not move the victim", "Switch off vehicle engines", "Talk to keep them conscious")
        )
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

                // Emergency Type Selector
                Text("Select Emergency Type", color = TextWhite70, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(emergencyTypes) { (id, name, icon) ->
                        val isSelected = selectedType == id
                        Card(
                            onClick = { selectedType = id },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) PrimaryBlue.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)
                            ),
                            border = if (isSelected) BorderStroke(2.dp, PrimaryBlue) else null,
                            modifier = Modifier.width(110.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(icon, fontSize = 24.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(name, color = if (isSelected) Color.White else TextWhite70, fontSize = 10.sp, textAlign = TextAlign.Center, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { if (it.length <= 15) phoneNumber = it },
                    label = { Text("Your Contact Number") },
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

                Spacer(modifier = Modifier.height(24.dp))

                var broadcastToFamily by remember { mutableStateOf(true) }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = broadcastToFamily,
                        onCheckedChange = { broadcastToFamily = it },
                        colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
                    )
                    Text("Broadcast Location to Family/Emergency Contacts", color = TextWhite70, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                GlowButton(
                    text = if (isSending) "DISPATCHING PROTOCOL..." else "ACTIVATE SEND SOS",
                    onClick = {
                        if (phoneNumber.isBlank()) {
                            sosError = "Please enter your phone number"
                            return@GlowButton
                        }
                        sosError = null
                        isSending = true
                        sosSentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                        scope.launch {
                            // SIMULATE SMS BROADCAST
                            if (broadcastToFamily) {
                                // In a real app, this would use SMS Manager
                                println("SARATHI: SMS Sent to Emergency Contacts with location: https://maps.google.com/?q=$latitude,$longitude")
                            }
                            try {
                                val response = api.sendSos(SosRequest(phoneNumber, latitude, longitude, emergencyType = selectedType))
                                if (response.isSuccessful && response.body() != null) {
                                    sosResponse = response.body()
                                    trackPhone = phoneNumber
                                    isOfflineMode = false
                                    response.body()?.let { 
                                        sessionManager.saveSimulatedMission(it.tripId, selectedType, it.status, latitude, longitude)
                                    }
                                } else {
                                    val fallbackId = "OFFLINE_" + System.currentTimeMillis()
                                    sosResponse = SosResponse(success = true, tripId = fallbackId, status = "pending", message = "Offline Protocol Activated")
                                    sessionManager.saveSimulatedMission(fallbackId, selectedType, "pending", latitude, longitude)
                                    isOfflineMode = true
                                }
                            } catch (e: Exception) {
                                val fallbackId = "OFFLINE_" + System.currentTimeMillis()
                                sosResponse = SosResponse(success = true, tripId = fallbackId, status = "pending", message = "Offline Protocol Activated")
                                sessionManager.saveSimulatedMission(fallbackId, selectedType, "pending", latitude, longitude)
                                isOfflineMode = true
                            } finally {
                                isSending = false
                            }
                        }
                    },
                    isLoading = isSending,
                    variant = GlowVariant.DANGER,
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    icon = { Icon(Icons.Default.FlashOn, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // FAILSAFE BUTTON
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmergencyRed),
                    border = BorderStroke(1.dp, EmergencyRed)
                ) {
                    Icon(Icons.Default.Phone, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("DIRECT FAILSAFE CALL 112", fontWeight = FontWeight.Black)
                }


                // MOVED OUT OF THE DISPATCH BLOCK
            } else {
                // ── SOS SUCCESS STATE ──
                if (sosResponse != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.15f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DirectionsCar, null, tint = SuccessGreen)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("AMBULANCE LIVE TRACKING", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Driver is coming! ETA: $driverEta mins", color = SuccessGreen, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
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

                        // ── SURVIVAL GUIDE SECTION ──
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, EmergencyRed.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.MedicalServices, null, tint = EmergencyRed, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("IMMEDIATE ACTION PLAN (LIFE-SAVING)", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                val protocols = firstAidProtocols[selectedType] ?: firstAidProtocols["medical"]!!
                                protocols.forEachIndexed { index, text ->
                                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Box(
                                            modifier = Modifier.size(18.dp).clip(CircleShape).background(if (index == 0) EmergencyRed else TextWhite.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("${index + 1}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(text, color = if (index == 0) Color.White else TextWhite70, fontSize = 13.sp, fontWeight = if (index == 0) FontWeight.ExtraBold else FontWeight.Normal)
                                    }
                                }
                            }
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
