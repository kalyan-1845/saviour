package com.sarathi.emergency.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarathi.emergency.ui.components.GlowButton
import com.sarathi.emergency.ui.components.GlowVariant
import com.sarathi.emergency.ui.theme.*
import com.sarathi.emergency.ui.viewmodel.HospitalSelectionViewModel
import com.sarathi.emergency.util.ExternalActionHandler

data class RealHospital(
    val id: String,
    val name: String,
    val address: String,
    val area: String,
    val city: String = "Hyderabad",
    val pincode: String,
    val state: String = "Telangana",
    val phone: String,
    val emergencyPhone: String = "",
    val specialties: List<String>,
    val bedsAvailable: Int,
    val totalBeds: Int,
    val rating: Double,
    val type: String,
    val isEmergencyAvailable: Boolean = true,
    val ambulanceCount: Int = 2,
    val distance: Double,
    val isOpen24x7: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalSelectionScreen(
    viewModel: HospitalSelectionViewModel,
    onStartNavigation: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Map backend model to UI model
    val hospitals = remember(uiState.hospitals) {
        uiState.hospitals.map { h ->
            RealHospital(
                id = h._id,
                name = h.name,
                address = h.address,
                area = h.city,
                city = h.city,
                pincode = "N/A",
                phone = h.phone,
                emergencyPhone = h.phone,
                specialties = h.specialties,
                bedsAvailable = h.bedsAvailable,
                totalBeds = h.totalBeds,
                rating = h.rating ?: 4.0,
                type = h.type.replaceFirstChar { it.uppercase() },
                distance = h.distance ?: 0.0,
                ambulanceCount = h.ambulanceCount,
                isEmergencyAvailable = h.isEmergencyAvailable
            )
        }
    }

    val allSpecialties = remember(hospitals) {
        hospitals.flatMap { it.specialties }.distinct().sorted()
    }

    val filteredHospitals = remember(uiState.searchQuery, uiState.filterSpecialty, hospitals) {
        hospitals.filter { h ->
            val matchesSearch = uiState.searchQuery.isBlank() ||
                    h.name.contains(uiState.searchQuery, ignoreCase = true) ||
                    h.area.contains(uiState.searchQuery, ignoreCase = true) ||
                    h.address.contains(uiState.searchQuery, ignoreCase = true)
            val matchesSpecialty = uiState.filterSpecialty.isBlank() || uiState.filterSpecialty in h.specialties
            matchesSearch && matchesSpecialty
        }.sortedBy { it.distance }
    }

    val selectedHospital = hospitals.find { it.id == uiState.selectedHospitalId }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkNavy, Color(0xFF0D1B2A))))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = TextWhite)
                    }
                    Column {
                        Text("Select Hospital", color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        Text(
                            "${filteredHospitals.size} hospitals found",
                            color = SuccessGreen, fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("Search hospitals...", color = TextWhite50) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = TextWhite70) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Specialties
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = uiState.filterSpecialty.isEmpty(),
                            onClick = { viewModel.updateSpecialtyFilter("") },
                            label = { Text("All") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryBlue)
                        )
                    }
                    items(allSpecialties) { specialty ->
                        FilterChip(
                            selected = uiState.filterSpecialty == specialty,
                            onClick = { viewModel.updateSpecialtyFilter(if (uiState.filterSpecialty == specialty) "" else specialty) },
                            label = { Text(specialty) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryBlue)
                        )
                    }
                }
            }

            // Hospital List
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = if (selectedHospital != null) 220.dp else 20.dp)
            ) {
                if (uiState.isLoading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = SuccessGreen)
                        }
                    }
                } else {
                    items(filteredHospitals) { hospital ->
                        RealHospitalCard(
                            hospital = hospital,
                            isSelected = uiState.selectedHospitalId == hospital.id,
                            onClick = { viewModel.selectHospital(hospital.id) },
                            onCall = { ExternalActionHandler.dial(context, hospital.emergencyPhone.ifBlank { hospital.phone }) }
                        )
                    }
                }
            }

            // Bottom Action Bar
            AnimatedVisibility(visible = selectedHospital != null) {
                selectedHospital?.let { hospital ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF162032))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text(hospital.name, color = TextWhite, fontWeight = FontWeight.Black, fontSize = 17.sp)
                                    Text(hospital.area, color = TextWhite70, fontSize = 12.sp)
                                }
                                TextButton(onClick = { viewModel.selectHospital(null) }) {
                                    Icon(Icons.Default.Close, null, tint = EmergencyRed)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            GlowButton(
                                text = "START NAVIGATION",
                                onClick = onStartNavigation,
                                variant = GlowVariant.PRIMARY,
                                modifier = Modifier.fillMaxWidth(),
                                icon = { Icon(Icons.Default.Navigation, null, tint = Color.White) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RealHospitalCard(
    hospital: RealHospital,
    isSelected: Boolean,
    onClick: () -> Unit,
    onCall: () -> Unit
) {
    val borderColor = if (isSelected) PrimaryBlue else Color.White.copy(alpha = 0.08f)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(hospital.name, color = TextWhite, fontWeight = FontWeight.Bold)
                Text("${hospital.distance} km | ${hospital.type}", color = TextWhite70, fontSize = 12.sp)
            }
            IconButton(onClick = onCall) { Icon(Icons.Default.Phone, null, tint = SuccessGreen) }
        }
    }
}
