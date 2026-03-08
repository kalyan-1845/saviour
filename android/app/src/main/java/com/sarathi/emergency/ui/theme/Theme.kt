package com.sarathi.emergency.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SarathiColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    secondary = PrimaryPurple,
    tertiary = SuccessGreen,
    background = DarkNavy,
    surface = DarkBlueGray,
    error = EmergencyRed,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onError = Color.White,
    surfaceVariant = SurfaceCard,
    outline = BorderBlue,
)

@Composable
fun SarathiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SarathiColorScheme,
        typography = SarathiTypography,
        content = content
    )
}
