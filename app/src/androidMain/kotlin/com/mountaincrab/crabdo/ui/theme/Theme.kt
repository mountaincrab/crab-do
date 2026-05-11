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

// ── Brand-constant accent palette (used in gradients and theme defs) ─────────
val AccentBlue = Color(0xFF4F7CFF)
val AccentPurple = Color(0xFF8B5CF6)
val AccentCyan = Color(0xFF06B6D4)
val AccentRed = Color(0xFFEF4444)
val AccentGreen = Color(0xFF10B981)
val AccentAmber = Color(0xFFF59E0B)

data class AppPalette(
    val gradientStart: Color,
    val gradientEnd: Color,
    val cardBorder: Color,
    val alarmTint: Color,
    // Lighter accent shade for inline accent text / icon-on-tile.
    // Equivalent to --accent-text in the design system tokens.
    val accentText: Color,
    // Translucent accent fill for active-tab pills + colored leading tiles
    // (matches --accent-soft, ~18% accent).
    val accentSoft: Color,
    // Green used for "Snoozing until …" text (matches --success-text).
    val successText: Color,
    // Surface used for input fields and chips/pills (matches --surface-high).
    val surfaceHigh: Color,
)

val LocalAppPalette = compositionLocalOf {
    AppPalette(
        gradientStart = AccentPurple,
        gradientEnd = AccentBlue,
        cardBorder = Color(0x1AFFFFFF),
        alarmTint = AccentAmber,
        accentText = Color(0xFFA5B4FC),
        accentSoft = Color(0x2E4F7CFF),
        successText = Color(0xFF34D399),
        surfaceHigh = Color(0xFF2A3250),
    )
}

@Composable
fun accentGradient(): Brush {
    val p = LocalAppPalette.current
    return Brush.linearGradient(listOf(p.gradientStart, p.gradientEnd))
}

// ── Color schemes ────────────────────────────────────────────────────────────
//
// Each theme gets its own primary accent (per the design system).
// Deep Navy uses indigo; Charcoal uses cyan; Slate keeps indigo (legacy);
// Retro uses magenta.
private fun buildScheme(
    primary: Color,
    onPrimary: Color,
    secondary: Color,
    background: Color,
    surface: Color,
    surfaceVariant: Color,
    outline: Color,
) = darkColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primary.copy(alpha = 0.18f),
    onPrimaryContainer = Color.White,
    secondary = secondary,
    onSecondary = Color.White,
    secondaryContainer = secondary.copy(alpha = 0.18f),
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
    inversePrimary = primary,
)

private val DeepNavyScheme = buildScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    secondary = AccentPurple,
    background = Color(0xFF0A1020),
    surface = Color(0xFF131A2E),
    surfaceVariant = Color(0xFF1C2340),
    outline = Color(0xFF2A3250),
)

private val CharcoalScheme = buildScheme(
    primary = AccentCyan,
    onPrimary = Color.White,
    secondary = Color(0xFF3B82F6),
    background = Color(0xFF0A0A0A),
    surface = Color(0xFF141414),
    surfaceVariant = Color(0xFF1E1E1E),
    outline = Color(0xFF2A2A2A),
)

private val SlateScheme = buildScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    secondary = AccentPurple,
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
    surface = Color(0xFF15001E),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF1E0030),
    onSurfaceVariant = Color(0xFFC9A0E0),
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
        alarmTint = AccentAmber,
        accentText = Color(0xFFA5B4FC),
        accentSoft = Color(0x2E4F7CFF),
        successText = Color(0xFF34D399),
        surfaceHigh = Color(0xFF2A3250),
    )
    AppTheme.CHARCOAL -> AppPalette(
        gradientStart = AccentCyan,
        gradientEnd = Color(0xFF3B82F6),
        cardBorder = Color(0x40FFFFFF),
        alarmTint = AccentAmber,
        accentText = Color(0xFF67E8F9),
        accentSoft = Color(0x2E06B6D4),
        successText = Color(0xFF34D399),
        surfaceHigh = Color(0xFF2A2A2A),
    )
    AppTheme.SLATE -> AppPalette(
        gradientStart = AccentPurple,
        gradientEnd = AccentBlue,
        cardBorder = Color(0x40BFC7E0),
        alarmTint = AccentAmber,
        accentText = Color(0xFFA5B4FC),
        accentSoft = Color(0x2E4F7CFF),
        successText = Color(0xFF34D399),
        surfaceHigh = Color(0xFF2A2E3C),
    )
    AppTheme.RETRO -> AppPalette(
        gradientStart = RetroMagenta,
        gradientEnd = RetroCyan,
        cardBorder = Color(0x50FF00CC),
        alarmTint = RetroYellow,
        accentText = Color(0xFFFF66DD),
        accentSoft = Color(0x2EFF00CC),
        successText = Color(0xFF66FFF5),
        surfaceHigh = Color(0xFF2E0048),
    )
}

// ── Typography ───────────────────────────────────────────────────────────────
// Bolder than Material defaults — matches the design system's bigger weight
// scale. Display sizes are deliberately black + tracked tight.
private val CrabbanTypography = Typography(
    displayLarge = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.Black, letterSpacing = (-1.68).sp),
    displayMedium = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Black, letterSpacing = (-1.0).sp),
    displaySmall = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
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
