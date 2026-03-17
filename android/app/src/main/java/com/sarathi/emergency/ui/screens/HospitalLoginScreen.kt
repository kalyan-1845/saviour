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

private const val PANEL_PASSWORD = "4455"

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
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    
    val hospitals = remember {
        listOf(
            LoginHospital("h1", "Apollo Hospitals", "Jubilee Hills", "Road No 72", "500033", "040-23607777", "Multispeciality"),
            LoginHospital("h2", "AIG Hospitals", "Gachibowli", "Mindspace Rd", "500032", "040-42444222", "Super-Speciality"),
            LoginHospital("h3", "Care Hospitals", "Banjara Hills", "Road No 1", "500034", "040-61656565", "Super-Speciality"),
            LoginHospital("h4", "NIMS", "Punjagutta", "Main Rd", "500082", "040-23489000", "Government")
        )
    }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(DarkNavy, Color(0xFF00211D)))
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))
            
            Box(
                modifier = Modifier.size(80.dp).clip(CircleShape).background(SuccessGreen.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.LocalHospital, null, tint = SuccessGreen, modifier = Modifier.size(40.dp))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("HOSPITAL PANEL", color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text("Emergency Reception & Trauma Care", color = SuccessGreen, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(40.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; error = null },
                label = { Text("Enter Reception Pin", color = TextWhite70) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = SuccessGreen)
            )

            if (error != null) {
                Text(error!!, color = EmergencyRed, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Select Hospital Registry", color = TextWhite70, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(hospitals) { hospital ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (password == PANEL_PASSWORD) {
                                onLoginSuccess(hospital)
                            } else {
                                error = "Invalid Access Pin"
                            }
                        },
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MedicalServices, null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(hospital.name, color = TextWhite, fontWeight = FontWeight.Bold)
                                Text("${hospital.area} | ${hospital.type}", color = TextWhite70, fontSize = 12.sp)
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
