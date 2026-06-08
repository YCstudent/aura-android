package com.edistrive.aura.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import com.edistrive.aura.R

val AuraFontFamily: FontFamily = FontFamily(
    Font(R.font.harmonyos_sans_regular, FontWeight.Normal),
    Font(R.font.harmonyos_sans_medium, FontWeight.Medium),
    Font(R.font.harmonyos_sans_bold, FontWeight.SemiBold),
    Font(R.font.harmonyos_sans_bold, FontWeight.Bold)
)

private val AuraTypography: Typography = run {
    val base = Typography()
    fun TextStyle.withFamily() = copy(fontFamily = AuraFontFamily)
    Typography(
        displayLarge = base.displayLarge.withFamily(),
        displayMedium = base.displayMedium.withFamily(),
        displaySmall = base.displaySmall.withFamily(),
        headlineLarge = base.headlineLarge.withFamily(),
        headlineMedium = base.headlineMedium.withFamily(),
        headlineSmall = base.headlineSmall.withFamily(),
        titleLarge = base.titleLarge.withFamily(),
        titleMedium = base.titleMedium.withFamily(),
        titleSmall = base.titleSmall.withFamily(),
        bodyLarge = base.bodyLarge.withFamily(),
        bodyMedium = base.bodyMedium.withFamily(),
        bodySmall = base.bodySmall.withFamily(),
        labelLarge = base.labelLarge.withFamily(),
        labelMedium = base.labelMedium.withFamily(),
        labelSmall = base.labelSmall.withFamily()
    )
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF1A8080),
    onPrimary = Color.White,
    secondary = Color(0xFF2D9C9C),
    onSecondary = Color.White,
    background = Color(0xFFF7F8FA),
    onBackground = Color(0xFF111111),
    surface = Color.White,
    onSurface = Color(0xFF111111)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8AB4FF),
    onPrimary = Color(0xFF0B0F1A),
    secondary = Color(0xFFB0BEC5),
    onSecondary = Color(0xFF0B0F1A),
    background = Color(0xFF0B0F1A),
    onBackground = Color(0xFFEAEAEA),
    surface = Color(0xFF121826),
    onSurface = Color(0xFFEAEAEA)
)

@Composable
fun AuraTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    // 限制字体缩放倍率，防止部分机型系统大字体导致界面臃肿
    // 保留一定弹性（0.9x - 1.25x），不彻底禁用用户偏好
    val currentDensity = LocalDensity.current
    val limitedDensity = if (currentDensity.fontScale !in 0.9f..1.25f) {
        Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale.coerceIn(0.9f, 1.25f)
        )
    } else {
        currentDensity
    }

    CompositionLocalProvider(LocalDensity provides limitedDensity) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AuraTypography,
            content = content
        )
    }
}
