package com.sarathi.emergency.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var licenseNumber by remember { mutableStateOf("") }
    var vehicleNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
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
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextWhite
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Driver Registration",
                        color = TextWhite,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Join SARATHI Emergency Network",
                        color = TextBlue300,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.08f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Error
                    if (error != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = EmergencyRed.copy(alpha = 0.15f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Error, null,
                                    tint = TextRed400,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(error ?: "", color = TextRed300, fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Fields helper
                    @Composable
                    fun FormField(
                        label: String,
                        value: String,
                        onValueChange: (String) -> Unit,
                        icon: @Composable () -> Unit,
                        keyboardType: KeyboardType = KeyboardType.Text,
                        isPassword: Boolean = false
                    ) {
                        Text(label, color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = value,
                            onValueChange = onValueChange,
                            leadingIcon = icon,
                            trailingIcon = if (isPassword) {
                                {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            if (showPassword) Icons.Default.VisibilityOff
                                            else Icons.Default.Visibility,
                                            null, tint = TextBlue400,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            } else null,
                            visualTransformation = if (isPassword && !showPassword)
                                PasswordVisualTransformation() else VisualTransformation.None,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = PrimaryBlue.copy(alpha = 0.3f),
                                focusedContainerColor = Color(0xFF0C1929).copy(alpha = 0.5f),
                                unfocusedContainerColor = Color(0xFF0C1929).copy(alpha = 0.5f),
                                cursorColor = TextBlue400
                            ),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = keyboardType,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    FormField("Full Name", fullName, { fullName = it },
                        { Icon(Icons.Default.Person, null, tint = TextBlue400, modifier = Modifier.size(18.dp)) })
                    FormField("Email", email, { email = it },
                        { Icon(Icons.Default.Email, null, tint = TextBlue400, modifier = Modifier.size(18.dp)) },
                        keyboardType = KeyboardType.Email)
                    FormField("Phone Number", phone, { phone = it },
                        { Icon(Icons.Default.Phone, null, tint = TextBlue400, modifier = Modifier.size(18.dp)) },
                        keyboardType = KeyboardType.Phone)
                    FormField("License Number", licenseNumber, { licenseNumber = it },
                        { Icon(Icons.Default.Badge, null, tint = TextBlue400, modifier = Modifier.size(18.dp)) })
                    FormField("Vehicle Number", vehicleNumber, { vehicleNumber = it },
                        { Icon(Icons.Default.DirectionsCar, null, tint = TextBlue400, modifier = Modifier.size(18.dp)) })
                    FormField("Password", password, { password = it },
                        { Icon(Icons.Default.Lock, null, tint = TextBlue400, modifier = Modifier.size(18.dp)) },
                        keyboardType = KeyboardType.Password, isPassword = true)
                    FormField("Confirm Password", confirmPassword, { confirmPassword = it },
                        { Icon(Icons.Default.Lock, null, tint = TextBlue400, modifier = Modifier.size(18.dp)) },
                        keyboardType = KeyboardType.Password, isPassword = true)

                    Spacer(modifier = Modifier.height(8.dp))

                    GlowButton(
                        text = if (isLoading) "Registering..." else "Register as Driver",
                        onClick = {
                            error = null
                            if (fullName.isBlank() || email.isBlank() || phone.isBlank() ||
                                licenseNumber.isBlank() || vehicleNumber.isBlank() || password.isBlank()
                            ) {
                                error = "Please fill all fields"
                                return@GlowButton
                            }
                            if (password != confirmPassword) {
                                error = "Passwords do not match"
                                return@GlowButton
                            }
                            if (password.length < 6) {
                                error = "Password must be at least 6 characters"
                                return@GlowButton
                            }
                            isLoading = true
                            scope.launch {
                                try {
                                    val response = api.driverRegister(
                                        RegisterRequest(
                                            fullName.trim(), email.trim(), phone.trim(),
                                            licenseNumber.trim(), vehicleNumber.trim(), password
                                        )
                                    )
                                    if (response.isSuccessful) {
                                        val body = response.body()
                                        if (body?.success == true && body.driver != null) {
                                            sessionManager.saveDriverSession(body.driver!!)
                                            onRegisterSuccess()
                                        } else {
                                            error = body?.error ?: "Registration failed"
                                        }
                                    } else {
                                        error = "Registration failed. Try again."
                                    }
                                } catch (e: Exception) {
                                    // Offline fallback — create local session
                                    val offlineDriver = com.sarathi.emergency.data.models.Driver(
                                        _id = "offline-${System.currentTimeMillis()}",
                                        fullName = fullName.trim(),
                                        email = email.trim(),
                                        phone = phone.trim(),
                                        licenseNumber = licenseNumber.trim(),
                                        vehicleNumber = vehicleNumber.trim(),
                                        isAvailable = true
                                    )
                                    sessionManager.saveDriverSession(offlineDriver)
                                    onRegisterSuccess()
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        isLoading = isLoading,
                        variant = GlowVariant.SUCCESS,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onBack) {
                Text(
                    text = "Already have an account? Login",
                    color = TextBlue400,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
