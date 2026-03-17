package com.sarathi.emergency.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarathi.emergency.data.SessionManager
import com.sarathi.emergency.data.api.SarathiApi
import com.sarathi.emergency.data.models.RegisterRequest
import com.sarathi.emergency.ui.components.GlowButton
import com.sarathi.emergency.ui.components.GlowVariant
import com.sarathi.emergency.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun DriverRegisterScreen(
    api: SarathiApi,
    sessionManager: SessionManager,
    onRegisterSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) } // 1: Basic, 2: Vehicle details
    
    // Step 1 fields
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    // Step 2 fields
    var vehicleType by remember { mutableStateOf("ambulance") } // ambulance, police, fire
    var vehicleNumber by remember { mutableStateOf("") }
    var licenseNumber by remember { mutableStateOf("") }
    
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(DarkNavy, Color(0xFF1E3A5F), DarkPurple)
                )
            )
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
                Spacer(modifier = Modifier.width(8.dp))
                Divider(modifier = Modifier.width(40.dp).height(2.dp), color = if (step > 1) SuccessGreen else Color.White.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.width(8.dp))
                StepIndicator(2, step >= 2)
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = if (step == 1) "Create Account" else "Vehicle Details",
                color = TextWhite,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = if (step == 1) "Join the SARATHI emergency network" else "Register your emergency vehicle",
                color = TextBlue300,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    if (error != null) {
                        Text(error ?: "", color = EmergencyRed, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))
                    }

                    if (step == 1) {
                        RegisterStepOne(
                            fullName, { fullName = it },
                            email, { email = it },
                            phone, { phone = it },
                            password, { password = it },
                            focusManager
                        )
                    } else {
                        RegisterStepTwo(
                            vehicleType, { vehicleType = it },
                            vehicleNumber, { vehicleNumber = it },
                            licenseNumber, { licenseNumber = it },
                            focusManager
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    GlowButton(
                        text = if (step == 1) "NEXT" else if (isLoading) "REGISTERING..." else "COMPLETE SIGNUP",
                        onClick = {
                            if (step == 1) {
                                if (fullName.isBlank() || email.isBlank() || phone.isBlank() || password.isBlank()) {
                                    error = "Please fill all fields"
                                } else {
                                    error = null
                                    step = 2
                                }
                            } else {
                                if (vehicleNumber.isBlank() || licenseNumber.isBlank()) {
                                    error = "Please fill vehicle details"
                                } else {
                                    error = null
                                    isLoading = true
                                    scope.launch {
                                        try {
                                            val response = api.driverRegister(
                                                RegisterRequest(fullName, email, phone, licenseNumber, vehicleNumber, password)
                                            )
                                            if (response.isSuccessful && response.body()?.success == true) {
                                                response.body()?.driver?.let { sessionManager.saveDriverSession(it) }
                                                onRegisterSuccess()
                                            } else {
                                                // Offline mock
                                                saveOfflineSession(fullName, email, phone, licenseNumber, vehicleNumber, sessionManager)
                                                onRegisterSuccess()
                                            }
                                        } catch (e: Exception) {
                                            saveOfflineSession(fullName, email, phone, licenseNumber, vehicleNumber, sessionManager)
                                            onRegisterSuccess()
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                }
                            }
                        },
                        isLoading = isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        variant = if (step == 1) GlowVariant.PRIMARY else GlowVariant.SUCCESS
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = { if (step == 2) step = 1 else onBack() }) {
                Text(if (step == 2) "Back to basic info" else "Already have an account? Login", color = TextBlue400, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StepIndicator(num: Int, active: Boolean) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(if (active) SuccessGreen else Color.White.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Text(num.toString(), color = if (active) Color.Black else TextWhite, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RegisterStepOne(
    name: String, onNameChange: (String) -> Unit,
    email: String, onEmailChange: (String) -> Unit,
    phone: String, onPhoneChange: (String) -> Unit,
    pass: String, onPassChange: (String) -> Unit,
    focusManager: FocusManager
) {
    Column {
        OutlinedTextField(
            value = name, onValueChange = onNameChange,
            label = { Text("Full Name", color = TextWhite70) },
            leadingIcon = { Icon(Icons.Default.Person, null, tint = TextBlue400) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = PrimaryBlue)
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = email, onValueChange = onEmailChange,
            label = { Text("Email", color = TextWhite70) },
            leadingIcon = { Icon(Icons.Default.Email, null, tint = TextBlue400) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = PrimaryBlue)
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = phone, onValueChange = onPhoneChange,
            label = { Text("Phone Number", color = TextWhite70) },
            leadingIcon = { Icon(Icons.Default.Phone, null, tint = TextBlue400) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = PrimaryBlue)
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = pass, onValueChange = onPassChange,
            label = { Text("Password", color = TextWhite70) },
            leadingIcon = { Icon(Icons.Default.Lock, null, tint = TextBlue400) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = PrimaryBlue),
            visualTransformation = PasswordVisualTransformation()
        )
    }
}

@Composable
private fun RegisterStepTwo(
    type: String, onTypeChange: (String) -> Unit,
    vNum: String, onVNumChange: (String) -> Unit,
    lNum: String, onLNumChange: (String) -> Unit,
    focusManager: FocusManager
) {
    Column {
        Text("Vehicle Type", color = TextWhite70, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VehicleTypeChip("ambulance", Icons.Default.MedicalServices, type == "ambulance") { onTypeChange("ambulance") }
            VehicleTypeChip("police", Icons.Default.Security, type == "police") { onTypeChange("police") }
            VehicleTypeChip("fire", Icons.Default.LocalFireDepartment, type == "fire") { onTypeChange("fire") }
        }
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedTextField(
            value = vNum, onValueChange = onVNumChange,
            label = { Text("Vehicle Number", color = TextWhite70) },
            leadingIcon = { Icon(Icons.Default.DirectionsCar, null, tint = TextBlue400) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = PrimaryBlue)
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = lNum, onValueChange = onLNumChange,
            label = { Text("License Number", color = TextWhite70) },
            leadingIcon = { Icon(Icons.Default.Badge, null, tint = TextBlue400) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = PrimaryBlue)
        )
    }
}

@Composable
private fun RowScope.VehicleTypeChip(id: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.weight(1f).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = if (selected) PrimaryBlue.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)),
        border = androidx.compose.foundation.BorderStroke(2.dp, if (selected) PrimaryBlue else Color.Transparent),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = if (selected) PrimaryBlue else TextWhite70, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(id.uppercase(), color = if (selected) PrimaryBlue else TextWhite70, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun saveOfflineSession(name: String, email: String, phone: String, lNum: String, vNum: String, sessionManager: SessionManager) {
    sessionManager.saveDriverSession(com.sarathi.emergency.data.models.Driver(
        _id = "offline-${System.currentTimeMillis()}",
        fullName = name, email = email, phone = phone, licenseNumber = lNum, vehicleNumber = vNum, isAvailable = true
    ))
}
