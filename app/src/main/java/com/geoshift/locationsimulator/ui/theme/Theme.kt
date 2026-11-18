package com.geoshift.locationsimulator.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// 1. Modern Tactical (default) — dark premium graphite dengan aksen teal taktis & emerald
private val ModernTacticalColorScheme = darkColorScheme(
    primary = Color(0xFF2EE6C8),          // Tactical Teal
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF0B5248),
    onPrimaryContainer = Color(0xFFA6FFEC),
    secondary = Color(0xFF7CC4FF),        // Sky Signal Blue
    onSecondary = Color(0xFF00324F),
    secondaryContainer = Color(0xFF1B3A52),
    onSecondaryContainer = Color(0xFFCFE6FF),
    tertiary = Color(0xFF34D399),         // Emerald (status aktif)
    onTertiary = Color(0xFF003822),
    tertiaryContainer = Color(0xFF0F4D33),
    onTertiaryContainer = Color(0xFFB7F5D6),
    background = Color(0xFF0A0E12),
    onBackground = Color(0xFFE6EDF2),
    surface = Color(0xFF12181E),
    onSurface = Color(0xFFE6EDF2),
    surfaceVariant = Color(0xFF1C242C),
    onSurfaceVariant = Color(0xFF98A6B3),
    surfaceTint = Color(0xFF2EE6C8),
    surfaceContainerLowest = Color(0xFF070A0D),
    surfaceContainerLow = Color(0xFF0F1419),
    surfaceContainer = Color(0xFF151C22),
    surfaceContainerHigh = Color(0xFF1C242C),
    surfaceContainerHighest = Color(0xFF26303A),
    surfaceBright = Color(0xFF2C3640),
    surfaceDim = Color(0xFF0A0E12),
    outline = Color(0xFF4A5865),
    outlineVariant = Color(0xFF26313B),
    inverseSurface = Color(0xFFE6EDF2),
    inverseOnSurface = Color(0xFF12181E),
    inversePrimary = Color(0xFF006B5C),
    scrim = Color(0xFF000000),
    error = DestructiveRed,
    onError = Color.White,
    errorContainer = Color(0xFF5C1414),
    onErrorContainer = Color(0xFFFFDAD6)
)

// 2. Dark OLED (Gelap pekat hemat baterai - True Black & High Voltage Cyan)
private val DarkOledColorScheme = darkColorScheme(
    primary = Color(0xFF00E5FF),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF00838F),
    secondary = Color(0xFF00E676),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF00A844),
    tertiary = Color(0xFF1DE9B6),
    onTertiary = Color(0xFF000000),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF0A0A0A),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF161616),
    onSurfaceVariant = Color(0xFFA0A0A0),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF050505),
    surfaceContainerHighest = Color(0xFF222222),
    outline = Color(0xFF3A3A3A),
    outlineVariant = Color(0xFF1E1E1E),
    error = DestructiveRed,
    errorContainer = DestructiveRed
)

// 3. Glassmorphism 2.0 (Transparan, blur premium & Ice Cyan Glow)
private val GlassmorphismColorScheme = darkColorScheme(
    primary = Color(0xFF00F0FF),
    onPrimary = Color(0xFF05101E),
    primaryContainer = Color(0xFF007A8C),
    secondary = Color(0xFFA78BFA),
    onSecondary = Color(0xFF1E1035),
    secondaryContainer = Color(0xFF6D28D9),
    tertiary = Color(0xFF2DD4BF),
    onTertiary = Color(0xFF042F2E),
    background = Color(0xFF080D1E),
    onBackground = Color(0xFFF1F5FF),
    surface = Color(0xFF121B3A),
    onSurface = Color(0xFFF1F5FF),
    surfaceVariant = Color(0xFF1D2B5A),
    onSurfaceVariant = Color(0xFFA5B4FC),
    surfaceContainerLowest = Color(0xFF040712),
    surfaceContainerLow = Color(0xFF0A1024),
    surfaceContainerHighest = Color(0xFF2B3E7A),
    outline = Color(0xFF3B5299),
    outlineVariant = Color(0xFF202E5C),
    error = DestructiveRed,
    errorContainer = DestructiveRed
)

// 4. Aurora Gradient (Gradient kosmik lembut, neon fuchsia magenta & aurora purple)
private val AuroraGradientColorScheme = darkColorScheme(
    primary = Color(0xFFE879F9),
    onPrimary = Color(0xFF3B0764),
    primaryContainer = Color(0xFFA21CAF),
    secondary = Color(0xFFA855F7),
    onSecondary = Color(0xFF2E0854),
    secondaryContainer = Color(0xFF7E22CE),
    tertiary = Color(0xFF2DD4BF),
    onTertiary = Color(0xFF042F2E),
    background = Color(0xFF0E0720),
    onBackground = Color(0xFFFAF5FF),
    surface = Color(0xFF1B0F38),
    onSurface = Color(0xFFFAF5FF),
    surfaceVariant = Color(0xFF2F1B5C),
    onSurfaceVariant = Color(0xFFE9D5FF),
    surfaceContainerLowest = Color(0xFF070312),
    surfaceContainerLow = Color(0xFF120A28),
    surfaceContainerHighest = Color(0xFF3F267A),
    outline = Color(0xFF6B42B0),
    outlineVariant = Color(0xFF3D236E),
    error = DestructiveRed,
    errorContainer = DestructiveRed
)

// 5. Neo-Minimal (Minimal elegan, monokrom Swiss)
private val NeoMinimalColorScheme = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFFE2E8F0),
    secondary = Color(0xFFCBD5E1),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF64748B),
    tertiary = Color(0xFF94A3B8),
    onTertiary = Color(0xFF000000),
    background = Color(0xFF080808),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF141414),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF222222),
    onSurfaceVariant = Color(0xFFCBD5E1),
    surfaceContainerLowest = Color(0xFF040404),
    surfaceContainerLow = Color(0xFF0E0E0E),
    surfaceContainerHighest = Color(0xFF303030),
    outline = Color(0xFF4B4B4B),
    outlineVariant = Color(0xFF2B2B2B),
    error = DestructiveRed,
    errorContainer = DestructiveRed
)

// 6. Soft Neumorphism (Lembut, matte hangat)
private val SoftNeumorphismColorScheme = darkColorScheme(
    primary = Color(0xFFF1F5F9),
    onPrimary = Color(0xFF181A1D),
    primaryContainer = Color(0xFF94A3B8),
    secondary = Color(0xFFCBD5E1),
    onSecondary = Color(0xFF181A1D),
    secondaryContainer = Color(0xFF475569),
    tertiary = Color(0xFF86EFAC),
    onTertiary = Color(0xFF062E18),
    background = Color(0xFF1C1E23),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF24272D),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF30343C),
    onSurfaceVariant = Color(0xFFCBD5E1),
    surfaceContainerLowest = Color(0xFF141519),
    surfaceContainerLow = Color(0xFF1E2025),
    surfaceContainerHighest = Color(0xFF3E434D),
    outline = Color(0xFF525966),
    outlineVariant = Color(0xFF383C45),
    error = DestructiveRed,
    errorContainer = DestructiveRed
)

// 7. CyberTech (Matrix Neon Green & Cyber Gold)
private val CyberTechColorScheme = darkColorScheme(
    primary = Color(0xFF00FF66),
    onPrimary = Color(0xFF00290A),
    primaryContainer = Color(0xFF008020),
    secondary = Color(0xFFFFD700),
    onSecondary = Color(0xFF332B00),
    secondaryContainer = Color(0xFF806C00),
    tertiary = Color(0xFF00F5FF),
    onTertiary = Color(0xFF002F33),
    background = Color(0xFF020904),
    onBackground = Color(0xFFF0FDF4),
    surface = Color(0xFF06180B),
    onSurface = Color(0xFFF0FDF4),
    surfaceVariant = Color(0xFF0F3017),
    onSurfaceVariant = Color(0xFF86EFAC),
    surfaceContainerLowest = Color(0xFF010402),
    surfaceContainerLow = Color(0xFF041007),
    surfaceContainerHighest = Color(0xFF1A4724),
    outline = Color(0xFF22C55E),
    outlineVariant = Color(0xFF155E2B),
    error = DestructiveRed,
    errorContainer = DestructiveRed
)

private fun contentOn(c: Color): Color = if (c.luminance() > 0.5f) Color(0xFF0B0F12) else Color.White

/**
 * Skema lama tidak mengisi surfaceContainer/High/onXContainer, sehingga komponen M3
 * (sheet, slider, switch, menu) jatuh ke warna baseline ungu. Turunkan dari surface yang ada.
 */
private fun ColorScheme.withDerivedContainers(): ColorScheme {
    val tertContainer = lerp(background, tertiary, 0.25f)
    return copy(
        surfaceContainer = lerp(surfaceContainerLow, surfaceVariant, 0.5f),
        surfaceContainerHigh = surfaceVariant,
        surfaceBright = lerp(surfaceVariant, surfaceContainerHighest, 0.5f),
        surfaceDim = background,
        surfaceTint = primary,
        onPrimaryContainer = contentOn(primaryContainer),
        onSecondaryContainer = contentOn(secondaryContainer),
        tertiaryContainer = tertContainer,
        onTertiaryContainer = contentOn(tertContainer),
        onError = Color.White,
        onErrorContainer = Color.White,
        inverseSurface = onSurface,
        inverseOnSurface = surface,
        inversePrimary = lerp(primary, Color.Black, 0.45f)
    )
}

val GeoShiftShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun GeoShiftTheme(
    uiTheme: String = DEFAULT_UI_THEME,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = when {
        uiTheme.startsWith("8") || uiTheme.contains("Dynamic", ignoreCase = true) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dynamicDarkColorScheme(context)
            } else {
                ModernTacticalColorScheme
            }
        }
        uiTheme.startsWith("2") || uiTheme.contains("Dark OLED", ignoreCase = true) -> DarkOledColorScheme.withDerivedContainers()
        uiTheme.startsWith("3") || uiTheme.contains("Glassmorphism", ignoreCase = true) -> GlassmorphismColorScheme.withDerivedContainers()
        uiTheme.startsWith("4") || uiTheme.contains("Aurora", ignoreCase = true) -> AuroraGradientColorScheme.withDerivedContainers()
        uiTheme.startsWith("5") || uiTheme.contains("Neo-Minimal", ignoreCase = true) -> NeoMinimalColorScheme.withDerivedContainers()
        uiTheme.startsWith("6") || uiTheme.contains("Neumorphism", ignoreCase = true) -> SoftNeumorphismColorScheme.withDerivedContainers()
        uiTheme.startsWith("7") || uiTheme.contains("CyberTech", ignoreCase = true) || uiTheme.contains("Tactical", ignoreCase = true) -> CyberTechColorScheme.withDerivedContainers()
        else -> ModernTacticalColorScheme
    }

    // API MaterialExpressiveTheme masih internal di material3 1.4.0 stabil; gaya expressive
    // (spring, bentuk pill, sudut besar) diterapkan langsung di komponen.
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = GeoShiftShapes,
        content = content
    )
}

const val DEFAULT_UI_THEME = "1. Modern Material 3"
