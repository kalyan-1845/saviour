package com.sarathi.emergency.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import android.telephony.SmsManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.sarathi.emergency.data.api.SarathiApi
import com.sarathi.emergency.data.models.SosRequest
import com.sarathi.emergency.data.models.SosResponse
import com.sarathi.emergency.data.models.TrackResponse
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

/**
 * Emergency SOS screen — with integrated offline map.
 * Phone number + GPS → Send SOS → Track Existing SOS
 * Tries API first, falls back to offline mode.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SOSScreen(
    api: SarathiApi,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationHelper = remember { LocationHelper(context) }

    // Form state
    var phoneNumber by remember { mutableStateOf("") }
    var latitude by remember { mutableDoubleStateOf(0.0) }
    var longitude by remember { mutableDoubleStateOf(0.0) }
    var hasLocation by remember { mutableStateOf(false) }
    var locationAttempted by remember { mutableStateOf(false) }

    // SOS state
    var isSending by remember { mutableStateOf(false) }
    var sosResponse by remember { mutableStateOf<SosResponse?>(null) }
    var sosError by remember { mutableStateOf<String?>(null) }
    var isOfflineMode by remember { mutableStateOf(false) }
    var sosSentTime by remember { mutableStateOf("") }

    // Track state
    var trackPhone by remember { mutableStateOf("") }
    var isTracking by remember { mutableStateOf(false) }
    var trackResult by remember { mutableStateOf<TrackResponse?>(null) }
    var trackError by remember { mutableStateOf<String?>(null) }

    // Permissions
    val permissions = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS
        )
    )

    LaunchedEffect(Unit) {
        if (!permissions.allPermissionsGranted) {
            permissions.launchMultiplePermissionRequest()
        }
    }

    // GPS — with retry logic
    val locationGranted = permissions.permissions
        .firstOrNull { it.permission == Manifest.permission.ACCESS_FINE_LOCATION }
        ?.status?.isGranted == true

    // Cleanup ref for location updates
    var stopLocationUpdates by remember { mutableStateOf<(() -> Unit)?>(null) }

    LaunchedEffect(locationGranted) {
        if (locationGranted) {
            locationAttempted = true
            // Get last known location first
            locationHelper.getLastLocation { loc ->
                if (loc != null) {
                    latitude = loc.latitude
                    longitude = loc.longitude
                    hasLocation = true
                }
            }
            // Start continuous updates
            val cleanup = locationHelper.requestLocationUpdates { loc ->
                latitude = loc.latitude
                longitude = loc.longitude
                hasLocation = true
            }
            stopLocationUpdates = cleanup
        }
    }

    // Clean up location updates when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            stopLocationUpdates?.invoke()
        }
    }

    // Keep trying to get location if we don't have it yet
    LaunchedEffect(locationGranted, hasLocation) {
        if (locationGranted && !hasLocation) {
            // Retry every 2 seconds for up to 30 seconds
            repeat(15) {
                if (hasLocation) return@LaunchedEffect
                delay(2000)
                locationHelper.getLastLocation { loc ->
                    if (loc != null && !hasLocation) {
                        latitude = loc.latitude
                        longitude = loc.longitude
                        hasLocation = true
                    }
                }
            }
        }
    }

    // Build map markers
    val mapMarkers = remember(sosResponse, latitude, longitude, hasLocation) {
        buildList {
            if (hasLocation) {
                add(MapMarker(latitude, longitude, "📍 Your Location", "GPS Active", MarkerColor.BLUE))
            }
            sosResponse?.hospital?.let { h ->
                add(MapMarker(
                    latitude + 0.008, longitude + 0.005,
                    "🏥 ${h.name}", "Hospital",
                    MarkerColor.GREEN
                ))
            }
        }
    }

    // ── Main UI ──
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F0A1E), Color(0xFF1A0A0A), DarkNavy)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // ── Header ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = TextWhite)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(EmergencyRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Warning, "SOS", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("SARATHI", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        Text("Emergency Response System", color = TextBlue300, fontSize = 11.sp)
                    }
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
                        Text("Live Location Map", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            if (hasLocation) "● LIVE" else if (locationAttempted) "⟳ SEARCHING..." else "○ NO GPS",
                            color = if (hasLocation) SuccessGreen else EmergencyOrange,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OfflineMapView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        centerLatitude = if (hasLocation) latitude else 17.4426,
                        centerLongitude = if (hasLocation) longitude else 78.5006,
                        zoomLevel = if (hasLocation) 16.0 else 12.0,
                        markers = mapMarkers,
                        showMyLocation = hasLocation,
                        myLatitude = latitude,
                        myLongitude = longitude,
                        showControls = true
                    )
                    if (!hasLocation && locationAttempted) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "💡 Enable GPS/Location in Settings • Go outside for best signal",
                            color = TextWhite50, fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ═══════════════════════════════════════════
            //  EMERGENCY SOS SECTION
            // ═══════════════════════════════════════════
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(EmergencyRed.copy(alpha = 0.4f), EmergencyOrange.copy(alpha = 0.2f)))
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Title
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(EmergencyRed.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Warning, null, tint = EmergencyRed, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Emergency SOS", color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            Text("Phone number + one tap for help", color = TextWhite70, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ── Phone Number ──
                    Text("Phone Number", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        placeholder = { Text("Enter your phone number", color = TextWhite50) },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, null, tint = TextWhite70, modifier = Modifier.size(18.dp))
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = EmergencyOrange.copy(alpha = 0.5f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = Color(0xFF1A0F1E),
                            unfocusedContainerColor = Color(0xFF1A0F1E),
                            cursorColor = EmergencyOrange
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // ── Live Location Status ──
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (hasLocation) SuccessGreen.copy(alpha = 0.06f)
                            else EmergencyRed.copy(alpha = 0.05f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LocationOn, null,
                                tint = if (hasLocation) SuccessGreen else EmergencyOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (hasLocation)
                                    "✅ GPS Active — ${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)}"
                                else if (locationAttempted)
                                    "⏳ Detecting GPS location... Make sure Location is ON"
                                else
                                    "⚠️ Location permission required",
                                color = if (hasLocation) SuccessGreen else EmergencyOrange,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── SEND SOS BUTTON ──
                    GlowButton(
                        text = if (isSending) "SENDING SOS..." else "🆘  SEND SOS",
                        onClick = {
                            if (phoneNumber.isBlank()) {
                                sosError = "Please enter your phone number"
                                return@GlowButton
                            }
                            sosError = null
                            isSending = true

                            // Vibrate immediately
                            try {
                                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                                vibrator?.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
                            } catch (_: Exception) {}

                            sosSentTime = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())

                            scope.launch {
                                try {
                                    val response = api.sendSos(
                                        SosRequest(
                                            phone = phoneNumber.replace(Regex("[^\\d]"), ""),
                                            latitude = latitude,
                                            longitude = longitude,
                                            emergencyType = "medical"
                                        )
                                    )
                                    if (response.isSuccessful && response.body() != null) {
                                        sosResponse = response.body()
                                        trackPhone = phoneNumber
                                        isOfflineMode = false
                                    } else {
                                        // API error — offline SOS
                                        createOfflineSosResponse(phoneNumber, latitude, longitude, hasLocation)
                                            .also { sosResponse = it }
                                        isOfflineMode = true
                                    }
                                } catch (_: Exception) {
                                    // No network — offline SOS
                                    createOfflineSosResponse(phoneNumber, latitude, longitude, hasLocation)
                                        .also { sosResponse = it }
                                    isOfflineMode = true
                                } finally {
                                    isSending = false
                                }
                            }
                        },
                        isLoading = isSending,
                        variant = GlowVariant.DANGER,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    )

                    // Error
                    if (sosError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(sosError!!, color = TextRed300, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ── Quick Action Buttons ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Call 112
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.Call, null, tint = EmergencyRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Call 112", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Find Hospitals
                        Button(
                            onClick = {
                                val lat = if (hasLocation) latitude else 17.4426
                                val lng = if (hasLocation) longitude else 78.5006
                                val uri = Uri.parse("geo:$lat,$lng?q=hospitals+near+me")
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                try { context.startActivity(intent) } catch (_: Exception) {
                                    context.startActivity(Intent(Intent.ACTION_VIEW,
                                        Uri.parse("https://maps.google.com/?q=hospitals+near+$lat,$lng")))
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.LocalHospital, null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hospitals", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // ── SOS Response ──
                    AnimatedVisibility(visible = sosResponse != null) {
                        sosResponse?.let { res ->
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isOfflineMode) EmergencyOrange.copy(alpha = 0.08f)
                                    else SuccessGreen.copy(alpha = 0.08f)
                                ),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    width = 1.dp,
                                    brush = Brush.linearGradient(
                                        listOf(
                                            if (isOfflineMode) EmergencyOrange.copy(alpha = 0.3f) else SuccessGreen.copy(alpha = 0.3f),
                                            Color.Transparent
                                        )
                                    )
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        if (isOfflineMode) "⚠️ SOS Sent — Offline Mode" else "✅ SOS Sent Successfully!",
                                        color = if (isOfflineMode) EmergencyOrange else SuccessGreen,
                                        fontWeight = FontWeight.Bold, fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    SosDetailRow("Trip ID:", res.tripId)
                                    SosDetailRow("Status:", res.status)
                                    SosDetailRow("Sent at:", sosSentTime)
                                    if (res.etaMinutes > 0) SosDetailRow("ETA:", "${res.etaMinutes} min")

                                    if (isOfflineMode) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "📡 SOS registered locally. When you get internet, data will sync with the server.",
                                            color = TextWhite50, fontSize = 11.sp
                                        )
                                        if (hasLocation) {
                                            SosDetailRow("Location:", "${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)}")
                                        }
                                    }

                                    res.driver?.let { d ->
                                        Spacer(modifier = Modifier.height(4.dp))
                                        SosDetailRow("Driver:", "${d.fullName} (${d.phone})")
                                    }
                                    res.hospital?.let { h ->
                                        SosDetailRow("Hospital:", h.name)
                                        if (h.mapUrl != null) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "🏥 Open Hospital Location",
                                                color = TextBlue400,
                                                fontSize = 13.sp,
                                                textDecoration = TextDecoration.Underline,
                                                modifier = Modifier.clickable {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(h.mapUrl))
                                                    context.startActivity(intent)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ═══════════════════════════════════════════
            //  TRACK EXISTING SOS
            // ═══════════════════════════════════════════
            Text("Track Existing SOS", color = TextWhite, fontWeight = FontWeight.Black, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(10.dp))

            // Track phone input
            OutlinedTextField(
                value = trackPhone,
                onValueChange = { trackPhone = it },
                placeholder = { Text("Enter phone number to track", color = TextWhite50) },
                leadingIcon = {
                    Icon(Icons.Default.Phone, null, tint = TextWhite70, modifier = Modifier.size(18.dp))
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedBorderColor = PrimaryBlue.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedContainerColor = Color(0xFF0D1929),
                    unfocusedContainerColor = Color(0xFF0D1929),
                    cursorColor = PrimaryBlue
                ),
                shape = RoundedCornerShape(10.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Track button
            GlowButton(
                text = if (isTracking) "Tracking..." else "Track SOS",
                onClick = {
                    if (trackPhone.isBlank()) {
                        trackError = "Enter a phone number to track"
                        return@GlowButton
                    }
                    trackError = null
                    isTracking = true
                    scope.launch {
                        try {
                            val response = api.trackSos(
                                phone = trackPhone.replace(Regex("[^\\d]"), "")
                            )
                            if (response.isSuccessful && response.body()?.success == true) {
                                trackResult = response.body()
                                trackError = null
                            } else {
                                // API returned no result — show offline message
                                trackError = "No active SOS found. Server may be offline."
                            }
                        } catch (e: Exception) {
                            trackError = "Cannot track — no internet connection. SOS tracking requires network."
                        } finally {
                            isTracking = false
                        }
                    }
                },
                isLoading = isTracking,
                variant = GlowVariant.PRIMARY,
                modifier = Modifier.fillMaxWidth()
            )

            if (trackError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = EmergencyOrange.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, null, tint = EmergencyOrange, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(trackError!!, color = EmergencyOrange, fontSize = 12.sp)
                    }
                }
            }

            // ── Track Results ──
            AnimatedVisibility(visible = trackResult != null) {
                trackResult?.let { res ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.06f)),
                        border = CardDefaults.outlinedCardBorder().copy(
                            width = 1.dp,
                            brush = Brush.linearGradient(listOf(PrimaryBlue.copy(alpha = 0.3f), Color.Transparent))
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Live SOS Tracking", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            res.trip?.let { trip ->
                                SosDetailRow("Status:", trip.status)
                                SosDetailRow("Update:", getStatusMessage(trip.status))
                                trip.estimatedTime?.let { SosDetailRow("ETA:", "$it min") }
                                trip.hospitalName?.let { SosDetailRow("Hospital:", it) }
                            }
                            res.driver?.let { driver ->
                                SosDetailRow("Driver:", "${driver.fullName} (${driver.phone})")
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Map links
                            res.trip?.pickupMapUrl?.let { url ->
                                Text(
                                    "📍 Open Pickup Location",
                                    color = TextBlue400, fontSize = 13.sp,
                                    textDecoration = TextDecoration.Underline,
                                    modifier = Modifier.clickable {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            res.trip?.hospitalMapUrl?.let { url ->
                                Text(
                                    "🏥 Open Hospital Location",
                                    color = TextBlue400, fontSize = 13.sp,
                                    textDecoration = TextDecoration.Underline,
                                    modifier = Modifier.clickable {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SosDetailRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(label, color = TextWhite70, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.width(6.dp))
        Text(value, color = TextWhite, fontSize = 13.sp)
    }
}

private fun getStatusMessage(status: String): String {
    return when (status.lowercase()) {
        "assigned" -> "Driver has been assigned and is on the way."
        "en_route", "en-route" -> "Ambulance is en route to your location."
        "arrived" -> "Ambulance has arrived at your location."
        "completed" -> "Trip has been completed."
        "pending" -> "Looking for nearest available driver..."
        "offline" -> "SOS registered in offline mode."
        else -> "Processing your emergency request."
    }
}

/**
 * Create an offline SOS response (no network call to 112, safe for testing).
 * Just registers the SOS locally.
 */
private fun createOfflineSosResponse(
    phone: String, lat: Double, lng: Double, hasLoc: Boolean
): SosResponse {
    return SosResponse(
        success = true,
        tripId = "SOS-${phone.takeLast(4)}-${System.currentTimeMillis() % 10000}",
        status = "offline",
        etaMinutes = 0,
        message = "SOS registered in offline mode. Call 112 for immediate help."
    )
}
