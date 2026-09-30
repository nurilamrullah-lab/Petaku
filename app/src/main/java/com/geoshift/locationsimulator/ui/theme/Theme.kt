package com.geoshift.locationsimulator.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// 1. Modern Material 3 (Bersih, modern, standar Material You - Royal Blue & Slate)
private val ModernMaterial3ColorScheme = darkColorScheme(
    primary = Color(0xFF3B82F6),     // Material Royal Blue
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF1D4ED8),
    secondary = Color(0xFF60A5FA),   // Sky Light Blue
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF2563EB),
    tertiary = Color(0xFF10B981),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFF0B1120),  // Midnight Deep Slate
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF1E293B),     // Slate Container
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainerLowest = Color(0xFF020617),
    surfaceContainerLow = Color(0xFF0F172A),
    surfaceContainerHighest = Color(0xFF475569),
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFF334155),
    error = DestructiveRed,
    errorContainer = DestructiveRed
)

// 2. Dark OLED (Gelap pekat hemat baterai - True Black & High Voltage Cyan)
private val DarkOledColorScheme = darkColorScheme(
    primary = Color(0xFF00E5FF),     // Ultra-bright Electric Cyan
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF00838F),
    secondary = Color(0xFF00E676),   // Neon Mint Green
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF00A844),
    tertiary = Color(0xFF1DE9B6),
    onTertiary = Color(0xFF000000),
    background = Color(0xFF000000),  // True AMOLED Pitch Black
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF060606),     // Pitch Black Container
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF121212),
    onSurfaceVariant = Color(0xFFA0A0A0),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF030303),
    surfaceContainerHighest = Color(0xFF1A1A1A),
    outline = Color(0xFF2E2E2E),
    outlineVariant = Color(0xFF181818),
    error = DestructiveRed,
    errorContainer = DestructiveRed
)

// 3. Glassmorphism 2.0 (Transparan, blur premium & Ice Cyan Glow)
private val GlassmorphismColorScheme = darkColorScheme(
    primary = Color(0xFF00F0FF),     // Crystal Ice Cyan
    onPrimary = Color(0xFF05101E),
    primaryContainer = Color(0xFF007A8C),
    secondary = Color(0xFFA78BFA),   // Glass Lavender
    onSecondary = Color(0xFF1E1035),
    secondaryContainer = Color(0xFF6D28D9),
    tertiary = Color(0xFF2DD4BF),
    onTertiary = Color(0xFF042F2E),
    background = Color(0xFF080D1E),  // Deep Translucent Navy
    onBackground = Color(0xFFF1F5FF),
    surface = Color(0xFF121B3A),     // Frosted Glass Layer
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
    primary = Color(0xFFE879F9),     // Neon Fuchsia / Magenta
    onPrimary = Color(0xFF3B0764),
    primaryContainer = Color(0xFFA21CAF),
    secondary = Color(0xFFA855F7),   // Cosmic Purple
    onSecondary = Color(0xFF2E0854),
    secondaryContainer = Color(0xFF7E22CE),
    tertiary = Color(0xFF2DD4BF),    // Aurora Mint Teal
    onTertiary = Color(0xFF042F2E),
    background = Color(0xFF0E0720),  // Deep Cosmic Night
    onBackground = Color(0xFFFAF5FF),
    surface = Color(0xFF1B0F38),     // Aurora Nebula Surface
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

// 5. Neo-Minimal (Minimal elegan, monokrom Swiss, hitam pekat & putih salju)
private val NeoMinimalColorScheme = darkColorScheme(
    primary = Color(0xFFFFFFFF),     // Pure Snow White
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFFE2E8F0),
    secondary = Color(0xFFCBD5E1),   // Platinum Silver
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF64748B),
    tertiary = Color(0xFF94A3B8),    // Architectural Slate
    onTertiary = Color(0xFF000000),
    background = Color(0xFF080808),  // Pure Obsidian Carbon
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF141414),     // Matte Neutral Charcoal
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

// 6. Soft Neumorphism (Lembut, modern, ramah, matte debossed hangat)
private val SoftNeumorphismColorScheme = darkColorScheme(
    primary = Color(0xFFF1F5F9),     // Warm Bone Light
    onPrimary = Color(0xFF181A1D),
    primaryContainer = Color(0xFF94A3B8),
    secondary = Color(0xFFCBD5E1),   // Soft Clay
    onSecondary = Color(0xFF181A1D),
    secondaryContainer = Color(0xFF475569),
    tertiary = Color(0xFF86EFAC),    // Pastel Mint
    onTertiary = Color(0xFF062E18),
    background = Color(0xFF1C1E23),  // Warm Matte Charcoal
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF24272D),     // Soft Raised Panel
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

// 7. CyberTech (Teknologi futuristik, Matrix Neon Green & Cyber Gold)
private val CyberTechColorScheme = darkColorScheme(
    primary = Color(0xFF00FF66),     // Matrix Electric Green
    onPrimary = Color(0xFF00290A),
    primaryContainer = Color(0xFF008020),
    secondary = Color(0xFFFFD700),   // Cyber Gold
    onSecondary = Color(0xFF332B00),
    secondaryContainer = Color(0xFF806C00),
    tertiary = Color(0xFF00F5FF),    // Laser Cyan
    onTertiary = Color(0xFF002F33),
    background = Color(0xFF020904),  // Terminal Black
    onBackground = Color(0xFFF0FDF4),
    surface = Color(0xFF06180B),     // Cyber Console Dark
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

@Composable
fun GeoShiftTheme(
    uiTheme: String = "Modern Material 3",
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = when {
        uiTheme.startsWith("8") || uiTheme.contains("Dynamic", ignoreCase = true) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dynamicDarkColorScheme(context)
            } else {
                ModernMaterial3ColorScheme
            }
        }
        uiTheme.startsWith("2") || uiTheme.contains("Dark OLED", ignoreCase = true) || uiTheme.contains("Dark Navy", ignoreCase = true) -> DarkOledColorScheme
        uiTheme.startsWith("3") || uiTheme.contains("Glassmorphism", ignoreCase = true) -> GlassmorphismColorScheme
        uiTheme.startsWith("4") || uiTheme.contains("Aurora", ignoreCase = true) -> AuroraGradientColorScheme
        uiTheme.startsWith("5") || uiTheme.contains("Neo-Minimal", ignoreCase = true) -> NeoMinimalColorScheme
        uiTheme.startsWith("6") || uiTheme.contains("Neumorphism", ignoreCase = true) -> SoftNeumorphismColorScheme
        uiTheme.startsWith("7") || uiTheme.contains("CyberTech", ignoreCase = true) || uiTheme.contains("Tactical", ignoreCase = true) -> CyberTechColorScheme
        else -> ModernMaterial3ColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
