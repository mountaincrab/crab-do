package com.mountaincrab.crabdo.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// ── Theme options ────────────────────────────────────────────────────────────
enum class AppTheme(val displayName: String) {
    DEEP_NAVY("Deep Navy"),
    CHARCOAL("Charcoal"),
    SLATE("Slate"),
    RETRO("Retro");

    companion object {
        fun fromName(name: String?): AppTheme =
            entries.firstOrNull { it.name == name } ?: DEEP_NAVY
    }
}

// ── Shared accents ───────────────────────────────────────────────────────────
val AccentBlue = Color(0xFF4F7CFF)
val AccentPurple = Color(0xFF8B5CF6)
val AccentRed = Color(0xFFEF4444)
val AccentGreen = Color(0xFF10B981)

data class AppPalette(
    val gradientStart: Color,
    val gradientEnd: Color,
    val cardBorder: Color,
    val alarmTint: Color,
)

val LocalAppPalette = compositionLocalOf {
    AppPalette(
        gradientStart = AccentPurple,
        gradientEnd = AccentBlue,
        cardBorder = Color(0x1AFFFFFF),
        alarmTint = Color(0xFFF59E0B),
    )
}

@Composable
fun accentGradient(): Brush {
    val p = LocalAppPalette.current
    return Brush.linearGradient(listOf(p.gradientStart, p.gradientEnd))
}

// ── Color schemes ────────────────────────────────────────────────────────────
private fun buildScheme(
    background: Color,
    surface: Color,
    surfaceVariant: Color,
    outline: Color,
) = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    primaryContainer = AccentBlue.copy(alpha = 0.18f),
    onPrimaryContainer = Color.White,
    secondary = AccentPurple,
    onSecondary = Color.White,
    secondaryContainer = AccentPurple.copy(alpha = 0.18f),
    onSecondaryContainer = Color.White,
    tertiary = AccentGreen,
    onTertiary = Color.White,
    error = AccentRed,
    onError = Color.White,
    errorContainer = AccentRed.copy(alpha = 0.18f),
    onErrorContainer = Color(0xFFFCA5A5),
    background = background,
    onBackground = Color(0xFFE5E7EB),
    surface = surface,
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = surfaceVariant,
    onSurfaceVariant = Color(0xFF9CA3AF),
    outline = outline,
    outlineVariant = outline.copy(alpha = 0.4f),
    inverseSurface = Color(0xFFF3F4F6),
    inverseOnSurface = background,
    inversePrimary = AccentBlue,
)

private val DeepNavyScheme = buildScheme(
    background = Color(0xFF0A1020),
    surface = Color(0xFF131A2E),
    surfaceVariant = Color(0xFF1C2340),
    outline = Color(0xFF2A3250),
)

private val CharcoalScheme = buildScheme(
    background = Color(0xFF0A0A0A),
    surface = Color(0xFF141414),
    surfaceVariant = Color(0xFF1E1E1E),
    outline = Color(0xFF2A2A2A),
)

private val SlateScheme = buildScheme(
    background = Color(0xFF161820),
    surface = Color(0xFF20232E),
    surfaceVariant = Color(0xFF2A2E3C),
    outline = Color(0xFF353949),
)

// ── Retro theme ──────────────────────────────────────────────────────────────
private val RetroMagenta = Color(0xFFFF00CC)
private val RetroCyan = Color(0xFF00FFEE)
private val RetroYellow = Color(0xFFFFEE00)

private val RetroScheme = darkColorScheme(
    primary = RetroMagenta,
    onPrimary = Color.Black,
    primaryContainer = RetroMagenta.copy(alpha = 0.20f),
    onPrimaryContainer = Color.White,
    secondary = RetroCyan,
    onSecondary = Color.Black,
    secondaryContainer = RetroCyan.copy(alpha = 0.18f),
    onSecondaryContainer = Color.White,
    tertiary = RetroYellow,
    onTertiary = Color.Black,
    error = Color(0xFFFF4400),
    onError = Color.White,
    errorContainer = Color(0xFFFF4400).copy(alpha = 0.18f),
    onErrorContainer = Color(0xFFFCA5A5),
    background = Color(0xFF0D0015),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF1A0028),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF2D0050),
    onSurfaceVariant = Color(0xFFCC99FF),
    outline = Color(0xFF4A0080),
    outlineVariant = Color(0xFF4A0080).copy(alpha = 0.4f),
    inverseSurface = Color(0xFFFFFFFF),
    inverseOnSurface = Color(0xFF0D0015),
    inversePrimary = RetroMagenta,
)

private fun paletteFor(theme: AppTheme): AppPalette = when (theme) {
    AppTheme.DEEP_NAVY -> AppPalette(
        gradientStart = AccentPurple,
        gradientEnd = AccentBlue,
        cardBorder = Color(0x408B9CFF),
        alarmTint = Color(0xFFF59E0B),
    )
    AppTheme.CHARCOAL -> AppPalette(
        gradientStart = AccentPurple,
        gradientEnd = AccentBlue,
        cardBorder = Color(0x40FFFFFF),
        alarmTint = Color(0xFFF59E0B),
    )
    AppTheme.SLATE -> AppPalette(
        gradientStart = AccentPurple,
        gradientEnd = AccentBlue,
        cardBorder = Color(0x40BFC7E0),
        alarmTint = Color(0xFFF59E0B),
    )
    AppTheme.RETRO -> AppPalette(
        gradientStart = RetroMagenta,
        gradientEnd = RetroCyan,
        cardBorder = Color(0x50FF00CC),
        alarmTint = RetroYellow,
    )
}

// ── Typography ───────────────────────────────────────────────────────────────
private val CrabbanTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
    displayMedium = TextStyle(fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
    displaySmall = TextStyle(fontWeight = FontWeight.ExtraBold),
    headlineLarge = TextStyle(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 1.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 1.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 1.2.sp),
)

@Composable
fun CrabbanTheme(
    appTheme: AppTheme = AppTheme.DEEP_NAVY,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppTheme.DEEP_NAVY -> DeepNavyScheme
        AppTheme.CHARCOAL -> CharcoalScheme
        AppTheme.SLATE -> SlateScheme
        AppTheme.RETRO -> RetroScheme
    }
    val palette = paletteFor(appTheme)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    CompositionLocalProvider(LocalAppPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CrabbanTypography,
            content = content
        )
    }
}
