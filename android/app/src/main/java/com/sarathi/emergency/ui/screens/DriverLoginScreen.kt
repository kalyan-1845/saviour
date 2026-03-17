package com.sarathi.emergency.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarathi.emergency.data.SessionManager
import com.sarathi.emergency.data.api.SarathiApi
import com.sarathi.emergency.data.models.Driver
import com.sarathi.emergency.data.models.LoginRequest
import com.sarathi.emergency.ui.components.GlowButton
import com.sarathi.emergency.ui.components.GlowVariant
import com.sarathi.emergency.ui.theme.*
import android.util.Log
import kotlinx.coroutines.launch

@Composable
fun DriverLoginScreen(
    api: SarathiApi,
    sessionManager: SessionManager,
    onLoginSuccess: () -> Unit,
    onRegister: () -> Unit,
    onBack: () -> Unit
) {
    val tag = "DriverLoginScreen"
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Logo Branding
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
                    .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Emergency,
                    contentDescription = "Logo",
                    tint = EmergencyRed,
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "SARATHI",
                color = TextWhite,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Text(
                text = "Emergency Vehicle Network",
                color = TextBlue300,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Login Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.08f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Login to Account",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    if (error != null) {
                        Text(
                            text = error ?: "",
                            color = EmergencyRed,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email", color = TextWhite70) },
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = TextBlue400) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password", color = TextWhite70) },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = TextBlue400) },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    null, tint = TextBlue400
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    GlowButton(
                        text = if (isLoading) "Signing in..." else "LOGIN",
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                error = "Please fill all fields"
                                return@GlowButton
                            }
                            isLoading = true
                            scope.launch {
                                try {
                                    val response = api.driverLogin(LoginRequest(email.trim(), password))
                                    if (response.isSuccessful && response.body()?.success == true) {
                                        response.body()?.driver?.let { sessionManager.saveDriverSession(it) }
                                        sessionManager.saveAuthToken(response.body()?.token)
                                        onLoginSuccess()
                                    } else {
                                        // Offline fallback
                                        Log.w(tag, "Online login failed with code ${response.code()}, using offline fallback")
                                        performOfflineLogin(email, password, sessionManager)
                                        onLoginSuccess()
                                    }
                                } catch (e: Exception) {
                                    Log.e(tag, "Login exception, using offline fallback", e)
                                    performOfflineLogin(email, password, sessionManager)
                                    onLoginSuccess()
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        isLoading = isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        variant = GlowVariant.PRIMARY
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onRegister) {
                Text("Don't have an account? Sign Up", color = TextBlue400, fontWeight = FontWeight.Bold)
            }

            TextButton(onClick = onBack) {
                Text("Back to Splash", color = TextWhite70)
            }
        }
    }
}

private fun performOfflineLogin(email: String, password: String, sessionManager: SessionManager) {
    val mockDriver = com.sarathi.emergency.data.models.Driver(
        _id = "offline-driver-1",
        fullName = "Offline Driver",
        email = email,
        phone = "9876543210",
        licenseNumber = "OFFLINE-123",
        vehicleNumber = "TS-01-EM-0001",
        isAvailable = true
    )
    sessionManager.saveDriverSession(mockDriver)
}
