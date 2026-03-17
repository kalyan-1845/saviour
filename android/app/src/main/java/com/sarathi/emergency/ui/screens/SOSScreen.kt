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
            (uiState as? SosUiState.Tracking)?.response?.let { res ->
                res.driver?.currentLocation?.let { dloc ->
                    val lat = dloc.latitude ?: 0.0
                    val lng = dloc.longitude ?: 0.0
                    add(MapMarker(lat, lng, "🚑 Ambulance", "Emergency Vehicle", MarkerColor.RED))
                }
                res.trip?.hospitalName?.let { hName ->
                    // Approximate location for visual feedback if hospital coords aren't in response
                    add(MapMarker((currentLocation?.latitude ?: 17.44) + 0.005, (currentLocation?.longitude ?: 78.50) + 0.005, "🏥 $hName", "Assigned Hospital", MarkerColor.GREEN))
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("SARATHI SOS", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = DarkNavy
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF0F0A1E), Color(0xFF141F3A), DarkNavy)))
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Map Section
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Map, null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Live Coordination Map", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OfflineMapView(
                            modifier = Modifier.fillMaxWidth().height(220.dp),
                            centerLatitude = currentLocation?.latitude ?: 17.4426,
                            centerLongitude = currentLocation?.longitude ?: 78.5006,
                            zoomLevel = 15.0,
                            markers = mapMarkers,
                            showMyLocation = true,
                            myLatitude = currentLocation?.latitude ?: 0.0,
                            myLongitude = currentLocation?.longitude ?: 0.0
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                when (val state = uiState) {
                    is SosUiState.Idle, is SosUiState.Error, is SosUiState.Loading -> {
                        SOSDispatchContent(
                            phoneNumber = phoneNumber,
                            onPhoneChange = { phoneNumber = it },
                            selectedType = selectedType,
                            onTypeSelect = { selectedType = it },
                            isSending = state is SosUiState.Loading,
                            error = (state as? SosUiState.Error)?.message,
                            onTrigger = { viewModel.triggerSos(phoneNumber, selectedType) }
                        )
                    }
                    is SosUiState.Active, is SosUiState.Tracking -> {
                        val response: SosResponse? = if (state is SosUiState.Active) state.response else (state as SosUiState.Tracking).response as? SosResponse
                        SOSTrackingContent(
                            response = response,
                            onReset = { viewModel.reset() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SOSDispatchContent(
    phoneNumber: String,
    onPhoneChange: (String) -> Unit,
    selectedType: String,
    onTypeSelect: (String) -> Unit,
    isSending: Boolean,
    error: String?,
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
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        if (error != null) {
            Text(error, color = EmergencyRed, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

        GlowButton(
            text = if (isSending) "DISPATCHING..." else "ACTIVATE SOS",
            onClick = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onTrigger()
            },
            isLoading = isSending,
            variant = GlowVariant.DANGER,
            modifier = Modifier.fillMaxWidth().height(64.dp)
        )
    }
}

@Composable
fun SOSTrackingContent(
    response: SosResponse? = null,
    trackResponse: TrackResponse? = null, // Can accept either
    onReset: () -> Unit
) {
    val trip = trackResponse?.trip
    val driver = trackResponse?.driver

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, SuccessGreen)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("SOS SIGNAL ACTIVE", color = SuccessGreen, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Status: ${trip?.status?.uppercase() ?: "ASSIGNING..."}", color = Color.White, fontWeight = FontWeight.Bold)
            Text("Assigned Hospital: ${trip?.hospitalName ?: "Searching..."}", color = TextWhite70)
            
            if (trip?.estimatedTime != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("ETA: ${trip.estimatedTime} Minutes", color = PrimaryBlue, fontSize = 24.sp, fontWeight = FontWeight.Black)
            }

            Spacer(modifier = Modifier.height(20.dp))
            
            Button(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))
            ) {
                Text("CANCEL / NEW REQUEST")
            }
        }
    }
}
