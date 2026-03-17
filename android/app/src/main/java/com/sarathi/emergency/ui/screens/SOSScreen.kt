@file:OptIn(ExperimentalMaterial3Api::class)
package com.sarathi.emergency.ui.screens

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.sarathi.emergency.data.SessionManager
import com.sarathi.emergency.data.models.*
import com.sarathi.emergency.ui.components.GlowButton
import com.sarathi.emergency.ui.components.GlowVariant
import com.sarathi.emergency.ui.components.MapMarker
import com.sarathi.emergency.ui.components.MarkerColor
import com.sarathi.emergency.ui.components.OfflineMapView
import com.sarathi.emergency.ui.theme.*
import com.sarathi.emergency.ui.viewmodel.SOSViewModel
import com.sarathi.emergency.ui.viewmodel.SosUiState
import com.sarathi.emergency.util.ExternalActionHandler

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SOSScreen(
    viewModel: SOSViewModel,
    sessionManager: SessionManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()

    var phoneNumber by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("medical") }
    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    LaunchedEffect(Unit) {
        if (!locationPermission.status.isGranted) {
            locationPermission.launchPermissionRequest()
        }
    }

    val mapMarkers = remember(currentLocation, uiState) {
        buildList {
            currentLocation?.let {
                add(MapMarker(it.latitude, it.longitude, "📍 Your Location", "GPS Active", MarkerColor.BLUE))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavy)
    ) {
        // Map Background
        OfflineMapView(
            centerLatitude = currentLocation?.latitude ?: 12.9716,
            centerLongitude = currentLocation?.longitude ?: 77.5946,
            markers = mapMarkers,
            modifier = Modifier.fillMaxSize()
        )

        // Overlay with Blur/Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            DarkNavy.copy(alpha = 0.5f),
                            DarkNavy
                        )
                    )
                )
        )

        // Header
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                }

                Surface(
                    color = Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (currentLocation != null) SuccessGreen else EmergencyRed))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (currentLocation != null) "GPS ACTIVE" else "SEARCHING GPS",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Main Content Area
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkNavy.copy(alpha = 0.95f),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier
                        .padding(28.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (val state = uiState) {
                        is SosUiState.Idle -> {
                            SOSTriggerContent(
                                phoneNumber = phoneNumber,
                                onPhoneChange = { phoneNumber = it },
                                selectedType = selectedType,
                                onTypeSelect = { selectedType = it },
                                isSending = false,
                                errorMsg = null,
                                onTrigger = {
                                    viewModel.triggerSos(phoneNumber, selectedType)
                                }
                            )
                        }
                        is SosUiState.Loading -> {
                            CircularProgressIndicator(color = EmergencyRed, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Activating Emergency Protocol...", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        is SosUiState.Active -> {
                            SOSTrackingContent(
                                sosResponse = state.response,
                                trackResponse = null,
                                onReset = { viewModel.reset() }
                            )
                        }
                        is SosUiState.Tracking -> {
                            SOSTrackingContent(
                                sosResponse = null,
                                trackResponse = state.response,
                                onReset = { viewModel.reset() }
                            )
                        }
                        is SosUiState.Error -> {
                            SOSTriggerContent(
                                phoneNumber = phoneNumber,
                                onPhoneChange = { phoneNumber = it },
                                selectedType = selectedType,
                                onTypeSelect = { selectedType = it },
                                isSending = false,
                                errorMsg = state.message,
                                onTrigger = {
                                    viewModel.triggerSos(phoneNumber, selectedType)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SOSTriggerContent(
    phoneNumber: String,
    onPhoneChange: (String) -> Unit,
    selectedType: String,
    onTypeSelect: (String) -> Unit,
    isSending: Boolean,
    errorMsg: String?,
    onTrigger: () -> Unit
) {
    val emergencyTypes = listOf(
        Triple("medical", "Medical", "🏥"),
        Triple("cardiac", "Cardiac", "❤️"),
        Triple("accident", "Accident", "🚑"),
        Triple("fire", "Fire", "🔥")
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Emergency Type", color = TextWhite70, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(emergencyTypes) { (id, name, icon) ->
                val isSelected = selectedType == id
                Card(
                    onClick = { onTypeSelect(id) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isSelected) PrimaryBlue else Color.White.copy(alpha = 0.05f)),
                    modifier = Modifier.width(90.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(icon, fontSize = 24.sp)
                        Text(name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = onPhoneChange,
            label = { Text("Contact Number") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(12.dp)
        )

        if (errorMsg != null) {
            Text(errorMsg, color = EmergencyRed, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        GlowButton(
            text = if (isSending) "ACTIVATING..." else "TRIGGER SOS",
            onClick = onTrigger,
            isLoading = isSending,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            variant = GlowVariant.DANGER
        )
    }
}

@Composable
fun SOSTrackingContent(
    sosResponse: SosResponse?,
    trackResponse: TrackResponse?,
    onReset: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(EmergencyRed.copy(alpha = 0.2f))
                .border(2.dp, EmergencyRed, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Emergency, null, tint = EmergencyRed, modifier = Modifier.size(40.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("SOS ACTIVE", color = EmergencyRed, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text("Help is on the way!", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            color = Color.White.copy(alpha = 0.05f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Emergency Details", color = TextWhite70, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                
                if (sosResponse != null) {
                    Text("Case ID: ${sosResponse.tripId}", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Status: ${sosResponse.status.uppercase()}", color = SuccessGreen, fontWeight = FontWeight.Bold)
                } else if (trackResponse != null) {
                    Text("Case ID: ${trackResponse.trip?.id ?: "N/A"}", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Status: ${trackResponse.trip?.status?.uppercase() ?: "ACTIVE"}", color = SuccessGreen, fontWeight = FontWeight.Bold)
                    
                    trackResponse.driver?.let { driver ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Driver: ${driver.fullName}", color = Color.White)
                        Text("Vehicle: ${driver.vehicleNumber}", color = Color.White)
                    }
                } else {
                    Text("Provisioning details...", color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        GlowButton(
            text = "CANCEL EMERGENCY",
            onClick = onReset,
            modifier = Modifier.fillMaxWidth(),
            variant = GlowVariant.PRIMARY
        )
    }
}
