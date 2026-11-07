package com.senior.kbju.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AccentGreen = Color(0xFF2E7D32)
val AccentBlue = Color(0xFF1565C0)
val AccentOrange = Color(0xFFE65100)
val ErrorRed = Color(0xFFB71C1C)
val SoftGray = Color(0xFFF2F2F2)
val BorderGray = Color(0xFF757575)

// Крупная типографика для пожилых: минимум 18sp, заголовки 24sp и больше.
private val LargeTypography = Typography(
    displayLarge = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Black, color = Color.Black),
    headlineMedium = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.Black),
    titleLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black),
    bodyLarge = TextStyle(fontSize = 20.sp, color = Color.Black),
    bodyMedium = TextStyle(fontSize = 18.sp, color = Color.Black)
)

@Composable
fun SeniorKbjuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = AccentGreen,
            onPrimary = Color.White,
            secondary = AccentBlue,
            onSecondary = Color.White,
            tertiary = AccentOrange,
            background = Color.White,
            onBackground = Color.Black,
            surface = Color.White,
            onSurface = Color.Black,
            surfaceVariant = SoftGray,
            onSurfaceVariant = Color.Black,
            error = ErrorRed,
            outline = BorderGray
        ),
        typography = LargeTypography,
        content = content
    )
}
