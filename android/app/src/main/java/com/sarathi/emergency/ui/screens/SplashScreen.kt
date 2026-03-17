package com.sarathi.emergency.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarathi.emergency.ui.components.GlowButton
import com.sarathi.emergency.ui.components.GlowVariant
import com.sarathi.emergency.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    isLoggedIn: Boolean,
    isPoliceLoggedIn: Boolean = false,
    isHospitalLoggedIn: Boolean = false,
    onDriverLogin: () -> Unit,
    onPublicSOS: () -> Unit,
    onGoToDashboard: () -> Unit,
    onPoliceDashboard: () -> Unit = {},
    onHospitalDashboard: () -> Unit = {},
    onPoliceLogin: () -> Unit = {},
    onHospitalLogin: () -> Unit = {}
) {
    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(150)
        showContent = true
    }

    LaunchedEffect(isLoggedIn, isPoliceLoggedIn, isHospitalLoggedIn) {
        if (isLoggedIn || isPoliceLoggedIn || isHospitalLoggedIn) {
            delay(800)
            when {
                isLoggedIn -> onGoToDashboard()
                isPoliceLoggedIn -> onPoliceDashboard()
                isHospitalLoggedIn -> onHospitalDashboard()
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "pulseScale"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = tween(800, easing = EaseOut), label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkNavy, DarkPurple, Color(0xFF1E1338)))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .alpha(contentAlpha)
                .verticalScroll(rememberScrollState())
                .padding(28.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Logo with pulse
            Box(
                modifier = Modifier
                    .scale(pulseScale)
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

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "SARATHI",
                color = TextWhite,
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 6.sp
            )
            Text(
                text = "EMERGENCY VEHICLE NETWORK",
                color = TextBlue300,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )

            if (!isLoggedIn && !isPoliceLoggedIn && !isHospitalLoggedIn) {
                Spacer(modifier = Modifier.height(40.dp))

                // ── Primary Actions ──
                // SOS
                GlowButton(
                    text = "🆘  Public SOS",
                    onClick = onPublicSOS,
                    variant = GlowVariant.DANGER,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Driver
                GlowButton(
                    text = "🚑  Driver Portal",
                    onClick = onDriverLogin,
                    variant = GlowVariant.PRIMARY,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // ── Authority Panels ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Police Panel
                    Button(
                        onClick = onPoliceLogin,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.06f)
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            width = 1.dp,
                            brush = Brush.linearGradient(listOf(PrimaryBlue.copy(alpha = 0.4f), PrimaryBlue.copy(alpha = 0.2f)))
                        )
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Shield, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                            Text("Police Panel", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Hospital Panel
                    Button(
                        onClick = onHospitalLogin,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.06f)
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            width = 1.dp,
                            brush = Brush.linearGradient(listOf(SuccessGreen.copy(alpha = 0.4f), SuccessGreen.copy(alpha = 0.2f)))
                        )
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.LocalHospital, null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                            Text("Hospital Panel", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Emergency hotline
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.horizontalGradient(listOf(EmergencyRed, EmergencyOrange)))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Emergency Hotline", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                        Text("112", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(32.dp))
                val roleName = when {
                    isLoggedIn -> "Driver"
                    isPoliceLoggedIn -> "Police"
                    isHospitalLoggedIn -> "Hospital"
                    else -> ""
                }
                Text("Welcome back, $roleName!", color = SuccessGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Securely launching dashboard...", color = TextWhite70, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
