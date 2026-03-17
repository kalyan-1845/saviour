package com.sarathi.emergency.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
    val type: String,         // Government / Private / Semi-Government
    val isEmergencyAvailable: Boolean = true,
    val ambulanceCount: Int = 2,
    val distance: Double,     // km
    val isOpen24x7: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalSelectionScreen(
    api: com.sarathi.emergency.data.api.SarathiApi,
    onStartNavigation: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedHospitalId by remember { mutableStateOf<String?>(null) }
    var filterSpecialty by remember { mutableStateOf("") }
    var hospitalList by remember { mutableStateOf(emptyList<com.sarathi.emergency.data.models.Hospital>()) }
    var isLoading by remember { mutableStateOf(false) }

    // FETCH HOSPITALS FROM BACKEND
    LaunchedEffect(Unit) {
        try {
            isLoading = true
            val response = api.getHospitals(latitude = 17.4426, longitude = 78.5006) // Dummy coords for now
            if (response.isSuccessful && response.body()?.success == true) {
                hospitalList = response.body()?.hospitals ?: emptyList()
            }
        } catch (_: Exception) {
        } finally {
            isLoading = false
        }
    }

    // Map backend model to UI model
    val hospitals = remember(hospitalList) {
        hospitalList.map { h ->
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

    // Filter by search + specialty
    val filteredHospitals = remember(searchQuery, filterSpecialty, hospitals) {
        hospitals.filter { h ->
            val matchesSearch = searchQuery.isBlank() ||
                    h.name.contains(searchQuery, ignoreCase = true) ||
                    h.area.contains(searchQuery, ignoreCase = true) ||
                    h.address.contains(searchQuery, ignoreCase = true)
            val matchesSpecialty = filterSpecialty.isBlank() || filterSpecialty in h.specialties
            matchesSearch && matchesSpecialty
        }.sortedBy { it.distance }
    }

    val selectedHospital = hospitals.find { it.id == selectedHospitalId }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(DarkNavy, Color(0xFF0D1B2A), Color(0xFF1B2838))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header ──
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = TextWhite)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text("Select Hospital", color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        Text(
                            "${filteredHospitals.size} hospitals found${if (filterSpecialty.isNotBlank()) " • $filterSpecialty" else ""}",
                            color = SuccessGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── Search Bar ──
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search hospital name, area, phone...", color = TextWhite50, fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, null, tint = TextWhite70, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, "Clear", tint = TextWhite70, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = PrimaryBlue.copy(alpha = 0.6f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedContainerColor = Color(0xFF0D1929),
                        unfocusedContainerColor = Color(0xFF0D1929),
                        cursorColor = PrimaryBlue
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // ── Specialty Filter Chips ──
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = filterSpecialty.isEmpty(),
                            onClick = { filterSpecialty = "" },
                            label = { Text("All", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White.copy(alpha = 0.06f),
                                labelColor = TextWhite70
                            )
                        )
                    }
                    items(allSpecialties) { specialty ->
                        FilterChip(
                            selected = filterSpecialty == specialty,
                            onClick = { filterSpecialty = if (filterSpecialty == specialty) "" else specialty },
                            label = { Text(specialty, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White.copy(alpha = 0.06f),
                                labelColor = TextWhite70
                            )
                        )
                    }
                }
            }

            // ── Hospital List ──
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = if (selectedHospital != null) 220.dp else 20.dp)
            ) {
                items(filteredHospitals) { hospital ->
                    RealHospitalCard(
                        hospital = hospital,
                        isSelected = selectedHospitalId == hospital.id,
                        onClick = { selectedHospitalId = hospital.id },
                        onCall = {
                            ExternalActionHandler.dial(
                                context = context,
                                phone = hospital.emergencyPhone.ifBlank { hospital.phone },
                                blockedMessage = "External dialer disabled in app mode"
                            )
                        }
                    )
                }

                if (filteredHospitals.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.SearchOff, null, tint = TextWhite50, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("No hospitals found", color = TextWhite70, fontSize = 16.sp)
                                Text("Try a different search or filter", color = TextWhite50, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // ── Bottom Action Bar (selected hospital) ──
            AnimatedVisibility(visible = selectedHospital != null) {
                selectedHospital?.let { hospital ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF162032))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            // Hospital name + type badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(hospital.name, color = TextWhite, fontWeight = FontWeight.Black, fontSize = 17.sp)
                                    Text(
                                        "${hospital.area}, ${hospital.city}, ${hospital.state} ${hospital.pincode}",
                                        color = TextWhite70, fontSize = 12.sp
                                    )
                                }
                                TextButton(onClick = { selectedHospitalId = null }) {
                                    Text("✕", color = TextRed400, fontSize = 16.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Stats row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatChip("📍", "${String.format("%.1f", hospital.distance)} km", SuccessGreen)
                                StatChip("🛏", "${hospital.bedsAvailable}/${hospital.totalBeds}", TextBlue400)
                                StatChip("⏱", "${(hospital.distance * 2.5).toInt()} min", EmergencyOrange)
                                StatChip("⭐", "${hospital.rating}", TextYellow400)
                                StatChip("🚑", "${hospital.ambulanceCount}", PrimaryPurple)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Phone
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            ExternalActionHandler.dial(
                                                context = context,
                                                phone = hospital.emergencyPhone.ifBlank { hospital.phone },
                                                blockedMessage = "External dialer disabled in app mode"
                                            )
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Phone, null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Emergency Phone", color = TextWhite70, fontSize = 11.sp)
                                        Text(
                                            hospital.emergencyPhone.ifBlank { hospital.phone },
                                            color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Navigation button
                            GlowButton(
                                text = "🚑 Start Navigation to ${hospital.name.take(15)}...",
                                onClick = onStartNavigation,
                                variant = GlowVariant.PRIMARY,
                                modifier = Modifier.fillMaxWidth(),
                                icon = {
                                    Icon(Icons.Default.Navigation, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}


// ═══════════════════════════════════════
//  Real Hospital Card
// ═══════════════════════════════════════

@Composable
private fun RealHospitalCard(
    hospital: RealHospital,
    isSelected: Boolean,
    onClick: () -> Unit,
    onCall: () -> Unit
) {
    val borderColor = if (isSelected) PrimaryBlue else Color.White.copy(alpha = 0.08f)
    val bgColor = if (isSelected) PrimaryBlue.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.04f)
    val typeBadgeColor = when (hospital.type) {
        "Government" -> SuccessGreen
        "Semi-Government" -> PrimaryBlue
        else -> PrimaryPurple
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = CardDefaults.outlinedCardBorder().copy(
            width = if (isSelected) 2.dp else 1.dp,
            brush = Brush.linearGradient(listOf(borderColor, borderColor))
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Row 1: Name + type + rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        hospital.name,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = TextBlue400, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            "${hospital.area}, ${hospital.city} ${hospital.pincode}",
                            color = TextWhite50, fontSize = 11.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    // Rating
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = TextYellow400, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("${hospital.rating}", color = TextYellow400, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    // Type badge
                    Text(
                        hospital.type,
                        color = typeBadgeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(typeBadgeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Distance
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Navigation, null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("${String.format("%.1f", hospital.distance)} km", color = SuccessGreen, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
                // Beds
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Hotel, null, tint = TextBlue400, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        "${hospital.bedsAvailable} beds",
                        color = if (hospital.bedsAvailable > 10) TextBlue400 else EmergencyOrange,
                        fontWeight = FontWeight.SemiBold, fontSize = 12.sp
                    )
                }
                // ETA
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, null, tint = TextOrange400, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("${(hospital.distance * 2.5).toInt()} min", color = TextOrange400, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
                // Ambulances
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🚑", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("${hospital.ambulanceCount}", color = TextWhite70, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 3: Specialties + call button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    hospital.specialties.take(3).forEach { spec ->
                        Text(
                            spec, color = TextWhite50, fontSize = 9.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (hospital.specialties.size > 3) {
                        Text(
                            "+${hospital.specialties.size - 3}", color = TextWhite50, fontSize = 9.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                // Call icon
                IconButton(
                    onClick = onCall,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SuccessGreen.copy(alpha = 0.15f))
                ) {
                    Icon(Icons.Default.Phone, "Call", tint = SuccessGreen, modifier = Modifier.size(16.dp))
                }
            }

            // 24x7 + Emergency badge
            if (hospital.isOpen24x7 || hospital.isEmergencyAvailable) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (hospital.isOpen24x7) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(SuccessGreen)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("24×7 Open", color = SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (hospital.isEmergencyAvailable) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(EmergencyRed)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Emergency Active", color = EmergencyOrange, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(icon: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}
