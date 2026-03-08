package com.sarathi.emergency.ui.screens

import android.content.Intent
import android.net.Uri
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
    onStartNavigation: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedHospitalId by remember { mutableStateOf<String?>(null) }
    var filterSpecialty by remember { mutableStateOf("") }

    // ── REAL Hyderabad Hospitals ──
    val hospitals = remember {
        listOf(
            RealHospital(
                id = "1", name = "Apollo Hospitals", area = "Jubilee Hills",
                address = "Film Nagar, Jubilee Hills", pincode = "500033",
                phone = "040-23607777", emergencyPhone = "040-23601066",
                specialties = listOf("Cardiac", "Trauma", "Neurology", "Oncology", "Pediatric"),
                bedsAvailable = 12, totalBeds = 550, rating = 4.8,
                type = "Private", ambulanceCount = 8, distance = 3.2
            ),
            RealHospital(
                id = "2", name = "NIMS Hospital", area = "Punjagutta",
                address = "Punjagutta, Beside Niloufer Hospital", pincode = "500082",
                phone = "040-23390536", emergencyPhone = "040-23390536",
                specialties = listOf("Trauma", "Burns", "Neurosurgery", "Cardiac", "General"),
                bedsAvailable = 45, totalBeds = 1500, rating = 4.5,
                type = "Government", ambulanceCount = 12, distance = 4.8
            ),
            RealHospital(
                id = "3", name = "KIMS Hospital", area = "Secunderabad",
                address = "1-8-31/1, Minister Road, Secunderabad", pincode = "500003",
                phone = "040-44885000", emergencyPhone = "040-44885100",
                specialties = listOf("Cardiac", "Stroke", "Neurology", "Orthopedics"),
                bedsAvailable = 8, totalBeds = 400, rating = 4.7,
                type = "Private", ambulanceCount = 6, distance = 5.1
            ),
            RealHospital(
                id = "4", name = "Yashoda Hospitals", area = "Malakpet",
                address = "Nalgonda X Roads, Malakpet", pincode = "500036",
                phone = "040-67777777", emergencyPhone = "040-67777000",
                specialties = listOf("Cardiac", "Trauma", "Nephrology", "Gastro"),
                bedsAvailable = 15, totalBeds = 500, rating = 4.6,
                type = "Private", ambulanceCount = 5, distance = 2.5
            ),
            RealHospital(
                id = "5", name = "Care Hospitals", area = "Banjara Hills",
                address = "Road No. 1, Banjara Hills", pincode = "500034",
                phone = "040-30418888", emergencyPhone = "040-30418800",
                specialties = listOf("Cardiac", "Neurology", "Pediatric", "Oncology"),
                bedsAvailable = 10, totalBeds = 435, rating = 4.5,
                type = "Private", ambulanceCount = 6, distance = 3.8
            ),
            RealHospital(
                id = "6", name = "Continental Hospitals", area = "Gachibowli",
                address = "IT Park Road, Nanakramguda, Gachibowli", pincode = "500032",
                phone = "040-67111111", emergencyPhone = "040-67111000",
                specialties = listOf("Trauma", "Stroke", "Burns", "Orthopedics", "Neuro"),
                bedsAvailable = 20, totalBeds = 750, rating = 4.4,
                type = "Private", ambulanceCount = 8, distance = 6.2
            ),
            RealHospital(
                id = "7", name = "Citizens Hospital", area = "Nallagandla",
                address = "Serilingampally, Nallagandla", pincode = "500019",
                phone = "040-67111111", emergencyPhone = "040-67111111",
                specialties = listOf("General", "Pediatric", "Orthopedics", "Cardiac"),
                bedsAvailable = 6, totalBeds = 200, rating = 4.3,
                type = "Private", ambulanceCount = 3, distance = 7.5
            ),
            RealHospital(
                id = "8", name = "Osmania General Hospital", area = "Afzalgunj",
                address = "Afzalgunj, Near High Court", pincode = "500012",
                phone = "040-24600146", emergencyPhone = "040-24600146",
                specialties = listOf("Trauma", "General", "Burns", "Orthopedics"),
                bedsAvailable = 55, totalBeds = 1168, rating = 4.0,
                type = "Government", ambulanceCount = 10, distance = 5.5
            ),
            RealHospital(
                id = "9", name = "Medicover Hospitals", area = "Madhapur",
                address = "Hitech City, Madhapur", pincode = "500081",
                phone = "040-68334455", emergencyPhone = "040-68334400",
                specialties = listOf("Cardiac", "Neurology", "Gastro", "Oncology"),
                bedsAvailable = 9, totalBeds = 350, rating = 4.4,
                type = "Private", ambulanceCount = 4, distance = 4.5
            ),
            RealHospital(
                id = "10", name = "Gandhi Hospital", area = "Musheerabad",
                address = "Padmarao Nagar, Musheerabad", pincode = "500003",
                phone = "040-27505566", emergencyPhone = "040-27505566",
                specialties = listOf("General", "Trauma", "Burns", "Pediatric"),
                bedsAvailable = 60, totalBeds = 1200, rating = 3.9,
                type = "Government", ambulanceCount = 8, distance = 6.0
            ),
            RealHospital(
                id = "11", name = "AIG Hospitals", area = "Gachibowli",
                address = "Survey No. 136, Gachibowli, Mindspace Road", pincode = "500032",
                phone = "040-49191919", emergencyPhone = "040-49191900",
                specialties = listOf("Gastro", "Hepatology", "Liver Transplant", "Oncology"),
                bedsAvailable = 7, totalBeds = 330, rating = 4.6,
                type = "Private", ambulanceCount = 4, distance = 5.8
            ),
            RealHospital(
                id = "12", name = "Sunshine Hospitals", area = "Secunderabad",
                address = "PG Road, Secunderabad", pincode = "500003",
                phone = "040-44550000", emergencyPhone = "040-44550100",
                specialties = listOf("Orthopedics", "Trauma", "Joint Replacement", "Spine"),
                bedsAvailable = 11, totalBeds = 280, rating = 4.5,
                type = "Private", ambulanceCount = 4, distance = 4.9
            ),
            RealHospital(
                id = "13", name = "Archana Hospital", area = "Miyapur",
                address = "Allwyn Colony, Miyapur", pincode = "500049",
                phone = "040-23053555", emergencyPhone = "040-23053555",
                specialties = listOf("General", "Pediatric", "Gynecology"),
                bedsAvailable = 5, totalBeds = 100, rating = 4.1,
                type = "Private", ambulanceCount = 2, distance = 8.2
            ),
            RealHospital(
                id = "14", name = "Himagiri Hospital", area = "Gachibowli",
                address = "Road No. 2, Gachibowli", pincode = "500032",
                phone = "040-29881214", emergencyPhone = "040-29881214",
                specialties = listOf("General", "Orthopedics", "ENT"),
                bedsAvailable = 4, totalBeds = 80, rating = 4.0,
                type = "Private", ambulanceCount = 1, distance = 6.0
            ),
            RealHospital(
                id = "15", name = "National Institute of Mental Health",
                area = "Rajendranagar",
                address = "Rajendranagar, Hyderabad", pincode = "500032",
                phone = "040-24110024", emergencyPhone = "040-24110024",
                specialties = listOf("Psychiatry", "Neurology", "Rehabilitation"),
                bedsAvailable = 30, totalBeds = 600, rating = 4.2,
                type = "Government", ambulanceCount = 3, distance = 7.0
            )
        )
    }

    val allSpecialties = remember {
        hospitals.flatMap { it.specialties }.distinct().sorted()
    }

    // Filter by search + specialty
    val filteredHospitals = remember(searchQuery, filterSpecialty) {
        hospitals.filter { h ->
            val matchesSearch = searchQuery.isBlank() ||
                    h.name.contains(searchQuery, ignoreCase = true) ||
                    h.area.contains(searchQuery, ignoreCase = true) ||
                    h.address.contains(searchQuery, ignoreCase = true) ||
                    h.phone.contains(searchQuery) ||
                    h.pincode.contains(searchQuery)
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
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${hospital.emergencyPhone.ifBlank { hospital.phone }}"))
                            context.startActivity(intent)
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
                                            val intent = Intent(
                                                Intent.ACTION_DIAL,
                                                Uri.parse("tel:${hospital.emergencyPhone.ifBlank { hospital.phone }}")
                                            )
                                            context.startActivity(intent)
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
