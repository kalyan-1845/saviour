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

private const val PANEL_PASSWORD = "4455"

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
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    
    val stations = remember {
        listOf(
            PoliceStation("ps1", "Abids Police Station", "Abids", "Central Zone", "Tilak Rd", "040-27853401", "West"),
            PoliceStation("ps2", "Banjara Hills PS", "Banjara Hills", "West Zone", "Road No 12", "040-27852482", "West"),
            PoliceStation("ps3", "Jubilee Hills PS", "Jubilee Hills", "West Zone", "Road No 36", "040-27852483", "West")
        )
    }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(DarkNavy, Color(0xFF0D1B2A)))
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))
            
            Box(
                modifier = Modifier.size(80.dp).clip(CircleShape).background(PrimaryBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Security, null, tint = PrimaryBlue, modifier = Modifier.size(40.dp))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("POLICE PANEL", color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text("Secure Access for Authorities", color = TextBlue400, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(40.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; error = null },
                label = { Text("Enter Station Pin", color = TextWhite70) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = PrimaryBlue)
            )

            if (error != null) {
                Text(error!!, color = EmergencyRed, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Select Jurisdiction", color = TextWhite70, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(stations) { station ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (password == PANEL_PASSWORD) {
                                onLoginSuccess(station)
                            } else {
                                error = "Invalid Access Pin"
                            }
                        },
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(station.name, color = TextWhite, fontWeight = FontWeight.Bold)
                                Text("${station.area} | ${station.jurisdiction}", color = TextWhite70, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onBack) { Text("Back to Home", color = TextWhite70) }
        }
    }
}
