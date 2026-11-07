package com.senior.kbju.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Современная, но контрастная и приятная палитра для старшего поколения
val AccentGreen = Color(0xFF1E7E45)       // Свежий глубокий изумрудный
val AccentGreenLight = Color(0xFFEAF7EE)  // Нежно-мятный пастельный фон
val AccentGreenBorder = Color(0xFFA7E0B8) // Мягкая рамка карточек

val AccentBlue = Color(0xFF1565C0)        // Благородный сапфировый
val AccentBlueLight = Color(0xFFEBF5FE)   // Нежно-голубой пастельный фон
val AccentBlueBorder = Color(0xFFBADDFD)

val AccentOrange = Color(0xFFE65100)      // Тёплый сочный янтарно-оранжевый
val AccentOrangeLight = Color(0xFFFFF3E5) // Нежно-персиковый фон
val AccentOrangeBorder = Color(0xFFFFCC99)

val ErrorRed = Color(0xFFC62828)
val ErrorRedLight = Color(0xFFFFEBEE)

val SurfaceCanvas = Color(0xFFF8FAFC)     // Чистый современный холст экрана (Slate 50)
val SurfaceWhite = Color(0xFFFFFFFF)      // Белоснежные плашки-карточки
val SoftGray = Color(0xFFF1F5F9)          // Мягкий фон вторичных элементов (Slate 100)
val CardBorder = Color(0xFFE2E8F0)        // Тонкая аккуратная граница (Slate 200)
val BorderGray = Color(0xFF94A3B8)
val TextDark = Color(0xFF0F172A)          // Глубокий Slate 900 для безупречной читаемости
val TextMuted = Color(0xFF475569)         // Мягкий вторичный текст (Slate 600)

// Крупная типографика для пожилых: минимум 18sp, заголовки 24sp и больше.
private val LargeTypography = Typography(
    displayLarge = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Black, color = TextDark),
    headlineMedium = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, color = TextDark),
    titleLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextDark),
    bodyLarge = TextStyle(fontSize = 20.sp, color = TextDark),
    bodyMedium = TextStyle(fontSize = 18.sp, color = TextDark)
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
            background = SurfaceCanvas,
            onBackground = TextDark,
            surface = SurfaceWhite,
            onSurface = TextDark,
            surfaceVariant = SoftGray,
            onSurfaceVariant = TextDark,
            error = ErrorRed,
            outline = BorderGray
        ),
        typography = LargeTypography,
        content = content
    )
}
