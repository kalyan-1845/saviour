package com.sarathi.emergency.ui.screens

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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarathi.emergency.data.models.DriverRegisterRequest
import com.sarathi.emergency.ui.components.GlowButton
import com.sarathi.emergency.ui.components.GlowVariant
import com.sarathi.emergency.ui.theme.*
import com.sarathi.emergency.ui.viewmodel.DriverUiState
import com.sarathi.emergency.ui.viewmodel.DriverViewModel

@Composable
fun DriverRegisterScreen(
    viewModel: DriverViewModel,
    onRegisterSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var step by remember { mutableIntStateOf(1) }
    
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var vehicleNumber by remember { mutableStateOf("") }
    var licenseNumber by remember { mutableStateOf("") }
    
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState) {
        if (uiState is DriverUiState.AuthSuccess) {
            onRegisterSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkNavy, Color(0xFF1E3A5F), DarkPurple)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Progress header
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepIndicator(1, step >= 1)
                Spacer(modifier = Modifier.width(12.dp))
                Divider(modifier = Modifier.width(40.dp).height(2.dp), color = if (step > 1) SuccessGreen else Color.White.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.width(12.dp))
                StepIndicator(2, step >= 2)
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(if (step == 1) "Driver Profile" else "Vehicle Details", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text(if (step == 1) "Begin your journey as a lifesaver" else "Register your rescue unit", color = TextBlue300, fontSize = 14.sp)

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    if (uiState is DriverUiState.Error) {
                        Text((uiState as DriverUiState.Error).message, color = EmergencyRed, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))
                    }

                    if (step == 1) {
                        OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                    } else {
                        OutlinedTextField(value = vehicleNumber, onValueChange = { vehicleNumber = it }, label = { Text("Vehicle Plate No") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(value = licenseNumber, onValueChange = { licenseNumber = it }, label = { Text("Driving License No") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    GlowButton(
                        text = if (step == 1) "CONTINUE →" else if (uiState is DriverUiState.Loading) "PROCESSING..." else "CREATE ACCOUNT",
                        onClick = {
                            if (step == 1) {
                                if (fullName.isNotBlank() && email.isNotBlank()) step = 2
                            } else {
                                viewModel.register(DriverRegisterRequest(fullName, email, phone, licenseNumber, vehicleNumber, password))
                            }
                        },
                        isLoading = uiState is DriverUiState.Loading,
                        modifier = Modifier.fillMaxWidth(),
                        variant = if (step == 1) GlowVariant.PRIMARY else GlowVariant.SUCCESS
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = { if (step == 2) step = 1 else onBack() }) {
                Text(if (step == 2) "Edit personal info" else "Already a driver? Sign In", color = TextBlue400, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StepIndicator(num: Int, active: Boolean) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (active) SuccessGreen else Color.White.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Text(num.toString(), color = if (active) Color.Black else Color.White, fontWeight = FontWeight.Bold)
    }
}
