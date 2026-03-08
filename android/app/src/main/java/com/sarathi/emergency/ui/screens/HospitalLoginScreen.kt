package com.sarathi.emergency.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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

data class LoginHospital(
    val id: String,
    val name: String,
    val area: String,
    val address: String,
    val pincode: String,
    val phone: String,
    val type: String,
    val city: String = "Hyderabad",
    val state: String = "Telangana"
)

@Composable
fun HospitalLoginScreen(
    onLoginSuccess: (LoginHospital) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedHospital by remember { mutableStateOf<LoginHospital?>(null) }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val hospitals = remember {
        listOf(
            LoginHospital("1", "Apollo Hospitals", "Jubilee Hills",
                "Film Nagar, Jubilee Hills", "500033", "040-23607777", "Private"),
            LoginHospital("2", "NIMS Hospital", "Punjagutta",
                "Punjagutta, Beside Niloufer Hospital", "500082", "040-23390536", "Government"),
            LoginHospital("3", "KIMS Hospital", "Secunderabad",
                "1-8-31/1, Minister Road", "500003", "040-44885000", "Private"),
            LoginHospital("4", "Yashoda Hospitals", "Malakpet",
                "Nalgonda X Roads, Malakpet", "500036", "040-67777777", "Private"),
            LoginHospital("5", "Care Hospitals", "Banjara Hills",
                "Road No. 1, Banjara Hills", "500034", "040-30418888", "Private"),
            LoginHospital("6", "Continental Hospitals", "Gachibowli",
                "IT Park Road, Nanakramguda", "500032", "040-67111111", "Private"),
            LoginHospital("7", "Citizens Hospital", "Nallagandla",
                "Serilingampally, Nallagandla", "500019", "040-67111111", "Private"),
            LoginHospital("8", "Osmania General Hospital", "Afzalgunj",
                "Afzalgunj, Near High Court", "500012", "040-24600146", "Government"),
            LoginHospital("9", "Medicover Hospitals", "Madhapur",
                "Hitech City, Madhapur", "500081", "040-68334455", "Private"),
            LoginHospital("10", "Gandhi Hospital", "Musheerabad",
                "Padmarao Nagar, Musheerabad", "500003", "040-27505566", "Government"),
            LoginHospital("11", "AIG Hospitals", "Gachibowli",
                "Survey No. 136, Mindspace Road", "500032", "040-49191919", "Private"),
            LoginHospital("12", "Archana Hospital", "Miyapur",
                "Allwyn Colony, Miyapur", "500049", "040-23053555", "Private"),
            LoginHospital("13", "Himagiri Hospital", "Gachibowli",
                "Road No. 2, Gachibowli", "500032", "040-29881214", "Private"),
            LoginHospital("14", "National Institute of Mental Health", "Rajendranagar",
                "Rajendranagar", "500032", "040-24110024", "Government")
        )
    }

    val filteredHospitals = remember(searchQuery) {
        if (searchQuery.isBlank()) hospitals
        else hospitals.filter {
            it.name.contains(searchQuery, true) ||
            it.area.contains(searchQuery, true) ||
            it.phone.contains(searchQuery) ||
            it.pincode.contains(searchQuery)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkNavy, Color(0xFF0D1B2A), Color(0xFF1B2838))))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = TextWhite)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Hospital Login", color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Text("Live emergency case intake dashboard", color = SuccessGreen, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Logo
                Box(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                        .size(64.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(SuccessGreen, Color(0xFF059669)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.LocalHospital, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Hospital", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search hospital name, area, phone...", color = TextWhite50, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = TextWhite70, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, null, tint = TextWhite70, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite, unfocusedTextColor = TextWhite,
                        focusedBorderColor = SuccessGreen.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedContainerColor = Color(0xFF0D1929), unfocusedContainerColor = Color(0xFF0D1929),
                        cursorColor = SuccessGreen
                    ),
                    shape = RoundedCornerShape(12.dp), singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Hospital list
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredHospitals) { hospital ->
                    val isSelected = selectedHospital?.id == hospital.id
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { selectedHospital = hospital },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) SuccessGreen.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.04f)
                        ),
                        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(
                            width = 2.dp, brush = Brush.linearGradient(listOf(SuccessGreen, SuccessGreen))
                        ) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier.width(4.dp).height(40.dp)
                                        .clip(RoundedCornerShape(2.dp)).background(EmergencyOrange)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(hospital.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${hospital.area}, ${hospital.city}, ${hospital.state} ${hospital.pincode}",
                                    color = TextWhite50, fontSize = 11.sp
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "Hospital Phone (for verification): ${hospital.phone}",
                                        color = TextWhite70, fontSize = 10.sp
                                    )
                                }
                            }
                            Text(
                                hospital.type,
                                color = if (hospital.type == "Government") SuccessGreen else PrimaryPurple,
                                fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (hospital.type == "Government") SuccessGreen.copy(alpha = 0.15f)
                                        else PrimaryPurple.copy(alpha = 0.15f)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Bottom panel: phone + password + login
            if (selectedHospital != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF162032))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(selectedHospital!!.name, color = TextWhite, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Text("${selectedHospital!!.address}, ${selectedHospital!!.city} ${selectedHospital!!.pincode}",
                            color = TextWhite70, fontSize = 12.sp)

                        Spacer(modifier = Modifier.height(10.dp))

                        // Phone
                        Text("Hospital Phone", color = TextWhite70, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                            modifier = Modifier.clickable {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${selectedHospital!!.phone}"))
                                context.startActivity(intent)
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Phone, null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(selectedHospital!!.phone, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Access Code", color = TextWhite70, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = { Text("Enter access code", color = TextWhite50) },
                            visualTransformation = PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite, unfocusedTextColor = TextWhite,
                                focusedBorderColor = SuccessGreen, unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                                focusedContainerColor = Color(0xFF0D1929), unfocusedContainerColor = Color(0xFF0D1929),
                                cursorColor = SuccessGreen
                            ),
                            shape = RoundedCornerShape(8.dp), singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (error != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(error!!, color = TextRed300, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        GlowButton(
                            text = "Login to Hospital Dashboard",
                            onClick = {
                                if (password == PANEL_PASSWORD) {
                                    error = null
                                    onLoginSuccess(selectedHospital!!)
                                } else {
                                    error = "Invalid access code"
                                }
                            },
                            variant = GlowVariant.SUCCESS,
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
