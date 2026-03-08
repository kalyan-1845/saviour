package com.sarathi.emergency.ui.screens

import androidx.compose.foundation.background
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
import kotlinx.coroutines.launch

@Composable
fun DriverLoginScreen(
    api: SarathiApi,
    sessionManager: SessionManager,
    onLoginSuccess: () -> Unit,
    onRegister: () -> Unit,
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isOfflineLogin by remember { mutableStateOf(false) }
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
            Spacer(modifier = Modifier.height(48.dp))

            // Logo
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(PrimaryBlue, PrimaryPurple))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "S",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "SARATHI",
                color = TextWhite,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Driver Portal Login",
                color = TextBlue300,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.08f)
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        listOf(PrimaryBlue.copy(alpha = 0.5f), PrimaryPurple.copy(alpha = 0.3f))
                    )
                )
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
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
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Error",
                                    tint = TextRed400,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = error ?: "",
                                    color = TextRed300,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Offline Login notice
                    if (isOfflineLogin) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = EmergencyOrange.copy(alpha = 0.15f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WifiOff,
                                    contentDescription = null,
                                    tint = EmergencyOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Logged in offline — some features limited",
                                    color = EmergencyOrange,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Email
                    Text(
                        text = "Email Address",
                        color = TextWhite,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text("prsnlkalyan@gmail.com", color = TextWhite50) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = TextBlue400,
                                modifier = Modifier.size(20.dp)
                            )
                        },
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
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password
                    Text(
                        text = "Password",
                        color = TextWhite,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("••••••••", color = TextWhite50) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = TextBlue400,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    contentDescription = "Toggle password",
                                    tint = TextBlue400,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None
                        else PasswordVisualTransformation(),
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
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Login Button
                    GlowButton(
                        text = if (isLoading) "Signing in..." else "Sign In",
                        onClick = {
                            error = null
                            if (email.isBlank() || password.isBlank()) {
                                error = "Please fill all fields"
                                return@GlowButton
                            }
                            isLoading = true
                            scope.launch {
                                try {
                                    val response = api.driverLogin(
                                        LoginRequest(email.trim(), password)
                                    )
                                    if (response.isSuccessful) {
                                        val body = response.body()
                                        if (body?.success == true && body.driver != null) {
                                            sessionManager.saveDriverSession(body.driver!!)
                                            isOfflineLogin = false
                                            onLoginSuccess()
                                        } else {
                                            error = body?.error ?: body?.message ?: "Login failed"
                                        }
                                    } else {
                                        // Server responded with error — try offline
                                        performOfflineLogin(email, password, sessionManager)
                                        isOfflineLogin = true
                                        onLoginSuccess()
                                    }
                                } catch (e: Exception) {
                                    // Network error — do offline login
                                    performOfflineLogin(email, password, sessionManager)
                                    isOfflineLogin = true
                                    onLoginSuccess()
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        isLoading = isLoading,
                        variant = GlowVariant.PRIMARY,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Register link
            TextButton(onClick = onRegister) {
                Text(
                    text = "Don't have an account? Register here",
                    color = TextBlue400,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Emergency help card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFCA8A04).copy(alpha = 0.15f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Need Help?",
                        color = TextYellow300,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Emergency: 112",
                        color = TextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Offline login — creates a local driver session so the app works
 * without a backend server.
 */
private fun performOfflineLogin(email: String, password: String, sessionManager: SessionManager) {
    val name = email.substringBefore("@")
        .replaceFirstChar { it.uppercase() }
        .replace(".", " ")

    val offlineDriver = Driver(
        _id = "offline-${System.currentTimeMillis()}",
        fullName = name,
        email = email,
        phone = "",
        licenseNumber = "OFFLINE",
        vehicleNumber = "SA-00-OFF-0000",
        isAvailable = true
    )
    sessionManager.saveDriverSession(offlineDriver)
}
