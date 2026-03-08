package com.sarathi.emergency.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarathi.emergency.ui.components.GlowButton
import com.sarathi.emergency.ui.components.GlowVariant
import com.sarathi.emergency.ui.theme.*

private const val PANEL_PASSWORD = "63050"

data class PoliceStation(
    val id: String,
    val name: String,
    val area: String,
    val jurisdiction: String,
    val address: String,
    val phone: String,
    val zone: String,
    val city: String = "Hyderabad"
)

@Composable
fun PoliceLoginScreen(
    onLoginSuccess: (PoliceStation) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedStation by remember { mutableStateOf<PoliceStation?>(null) }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val stations = remember {
        listOf(
            PoliceStation("1", "Shamirpet Police Station", "Shamirpet Area", "Shamirpet",
                "Shamirpet, Medchal-Malkajgiri", "040-27972254", "North"),
            PoliceStation("2", "Financial District Police Station", "Financial District", "Gachibowli",
                "Financial District, Gachibowli", "040-23601111", "West"),
            PoliceStation("3", "Rajendranagar Police Station", "Rajendranagar Area", "Rajendranagar",
                "Rajendranagar", "040-24015854", "South"),
            PoliceStation("4", "Gachibowli Police Station", "Gachibowli Area", "Gachibowli",
                "Gachibowli", "040-23561188", "West"),
            PoliceStation("5", "Madhapur Police Station", "Madhapur Area", "Madhapur",
                "Madhapur, Cyberabad", "040-23541232", "West"),
            PoliceStation("6", "Jubilee Hills Police Station", "Jubilee Hills Area", "Jubilee Hills",
                "Jubilee Hills, Road No 36", "040-23540585", "West"),
            PoliceStation("7", "Banjara Hills Police Station", "Banjara Hills Area", "Banjara Hills",
                "Banjara Hills, Road No 12", "040-23546254", "Central"),
            PoliceStation("8", "Begumpet Police Station", "Begumpet Area", "Begumpet",
                "Begumpet, Near Airport", "040-27853553", "North"),
            PoliceStation("9", "Kukatpally Police Station", "Kukatpally Area", "Kukatpally",
                "Kukatpally Housing Board", "040-23072630", "West"),
            PoliceStation("10", "Miyapur Police Station", "Miyapur Area", "Miyapur",
                "Miyapur, Allwyn Colony", "040-23055430", "West"),
            PoliceStation("11", "SR Nagar Police Station", "SR Nagar Area", "SR Nagar",
                "SR Nagar, Sanjeev Reddy Nagar", "040-23813775", "Central"),
            PoliceStation("12", "Secunderabad Police Station", "Secunderabad Area", "Secunderabad",
                "RP Road, Secunderabad", "040-27800256", "North"),
            PoliceStation("13", "Charminar Police Station", "Charminar Area", "Charminar",
                "Near Charminar, Old City", "040-24522286", "South"),
            PoliceStation("14", "Afzalgunj Police Station", "Afzalgunj Area", "Afzalgunj",
                "Afzalgunj, Near High Court", "040-24522293", "South"),
            PoliceStation("15", "Nampally Police Station", "Nampally Area", "Nampally",
                "Nampally, Near Railway Station", "040-24616024", "Central")
        )
    }

    val filteredStations = remember(searchQuery) {
        if (searchQuery.isBlank()) stations
        else stations.filter {
            it.name.contains(searchQuery, true) ||
            it.area.contains(searchQuery, true) ||
            it.jurisdiction.contains(searchQuery, true) ||
            it.zone.contains(searchQuery, true) ||
            it.phone.contains(searchQuery)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkNavy, Color(0xFF0D1B2A), Color(0xFF1B2838))))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header ──
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = TextWhite)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Police Login", color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Text("Green corridor alert dashboard", color = TextBlue300, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Logo
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(PrimaryBlue, Color(0xFF1D4ED8)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Shield, "Police", tint = Color.White, modifier = Modifier.size(32.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search
                Text("Police Station", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search station/jurisdiction/zone...", color = TextWhite50, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = TextWhite70, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, "Clear", tint = TextWhite70, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite, unfocusedTextColor = TextWhite,
                        focusedBorderColor = PrimaryBlue.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedContainerColor = Color(0xFF0D1929), unfocusedContainerColor = Color(0xFF0D1929),
                        cursorColor = PrimaryBlue
                    ),
                    shape = RoundedCornerShape(12.dp), singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ── Station List ──
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredStations) { station ->
                    val isSelected = selectedStation?.id == station.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedStation = station },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) PrimaryBlue.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f)
                        ),
                        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(
                            width = 2.dp, brush = Brush.linearGradient(listOf(PrimaryBlue, PrimaryBlue))
                        ) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Selection indicator
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(EmergencyOrange)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    station.name, color = TextWhite,
                                    fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${station.area} | ${station.jurisdiction}, ${station.city}",
                                    color = TextWhite50, fontSize = 11.sp
                                )
                            }
                            // Zone badge
                            Text(
                                station.zone, color = TextBlue300, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(PrimaryBlue.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // ── Selected station details + password + login ──
            if (selectedStation != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF162032))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Station info
                        Text(selectedStation!!.name, color = TextWhite, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Text(
                            "${selectedStation!!.address}, ${selectedStation!!.city}",
                            color = TextWhite70, fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Phone
                        Text("Station Phone", color = TextWhite70, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                            modifier = Modifier.clickable {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${selectedStation!!.phone}"))
                                context.startActivity(intent)
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Phone, null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(selectedStation!!.phone, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Password
                        Text("Access Code", color = TextWhite70, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = { Text("Enter access code", color = TextWhite50) },
                            visualTransformation = PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite, unfocusedTextColor = TextWhite,
                                focusedBorderColor = PrimaryBlue, unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                                focusedContainerColor = Color(0xFF0D1929), unfocusedContainerColor = Color(0xFF0D1929),
                                cursorColor = PrimaryBlue
                            ),
                            shape = RoundedCornerShape(8.dp), singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (error != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(error!!, color = TextRed300, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Login button
                        GlowButton(
                            text = "Login to Police Dashboard",
                            onClick = {
                                if (password == PANEL_PASSWORD) {
                                    error = null
                                    onLoginSuccess(selectedStation!!)
                                } else {
                                    error = "Invalid access code"
                                }
                            },
                            variant = GlowVariant.PRIMARY,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text("Back to Home", color = TextBlue400, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
