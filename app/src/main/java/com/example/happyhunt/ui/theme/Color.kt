package com.example.happyhunt.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.example.happyhunt.domain.Category

/**
 * The Happy Hunt palette: lagoon teal and a sunny yellow on warm cream, with
 * one bright, friendly colour per kind of outing so the map reads at a glance.
 *
 * Material's colour scheme covers what its own components draw. Everything
 * else (open/closed, the hearts, the category pins) lives here, so a screen
 * never reaches for a raw hex value.
 */
@Immutable
data class HuntColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val surfaceSunken: Color,
    val line: Color,
    val ink: Color,
    val inkSoft: Color,
    val inkMuted: Color,
    val primary: Color,
    val onPrimary: Color,
    val primarySoft: Color,
    val onPrimarySoft: Color,
    val sunny: Color,
    val onSunny: Color,
    val heart: Color,
    val open: Color,
    val openSoft: Color,
    val soon: Color,
    val soonSoft: Color,
    val closed: Color,
    val closedSoft: Color,
    val scrim: Color,
    val categories: Map<Category, CategoryColors>,
) {
    fun category(category: Category): CategoryColors = categories.getValue(category)
}

/** [pin] is the marker and icon colour; [soft] its gentle background; [onSoft] text on that background. */
@Immutable
data class CategoryColors(val pin: Color, val soft: Color, val onSoft: Color)

val LightHuntColors = HuntColors(
    isDark = false,
    background = Color(0xFFFBF7F2),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFFFFFFF),
    surfaceSunken = Color(0xFFF3EDE5),
    line = Color(0xFFEAE2D8),
    ink = Color(0xFF1E2422),
    inkSoft = Color(0xFF3E4744),
    inkMuted = Color(0xFF68716D),
    primary = Color(0xFF0E7A6E),
    onPrimary = Color(0xFFFFFFFF),
    primarySoft = Color(0xFFD9F0EB),
    onPrimarySoft = Color(0xFF09584F),
    sunny = Color(0xFFFFB423),
    onSunny = Color(0xFF3B2A00),
    heart = Color(0xFFE5484D),
    open = Color(0xFF1C7F45),
    openSoft = Color(0xFFDDF3E4),
    soon = Color(0xFFA35F00),
    soonSoft = Color(0xFFFDEED3),
    closed = Color(0xFFBF3A2B),
    closedSoft = Color(0xFFFBE4DF),
    scrim = Color(0x66101615),
    categories = mapOf(
        Category.EAT to CategoryColors(Color(0xFFE5533C), Color(0xFFFDE6E1), Color(0xFFA2321F)),
        Category.TREATS to CategoryColors(Color(0xFFD6407A), Color(0xFFFBE3EC), Color(0xFF9C2457)),
        Category.PARKS to CategoryColors(Color(0xFF2F9E5F), Color(0xFFDDF2E5), Color(0xFF1C6E40)),
        Category.PLAYGROUNDS to CategoryColors(Color(0xFFD48A00), Color(0xFFFCEFD2), Color(0xFF8A5A00)),
        Category.ATTRACTIONS to CategoryColors(Color(0xFF7B5CE6), Color(0xFFEAE4FC), Color(0xFF5237B5)),
        Category.CULTURE to CategoryColors(Color(0xFF2F6FDB), Color(0xFFE0EAFB), Color(0xFF1C4FA6)),
    ),
)

val DarkHuntColors = HuntColors(
    isDark = true,
    background = Color(0xFF101615),
    surface = Color(0xFF18201F),
    surfaceRaised = Color(0xFF212B29),
    surfaceSunken = Color(0xFF0B100F),
    line = Color(0xFF2B3634),
    ink = Color(0xFFEEF2F0),
    inkSoft = Color(0xFFC6CFCB),
    inkMuted = Color(0xFF909B97),
    primary = Color(0xFF52C8B7),
    onPrimary = Color(0xFF00332D),
    primarySoft = Color(0xFF15403A),
    onPrimarySoft = Color(0xFFA3E8DD),
    sunny = Color(0xFFFFC34F),
    onSunny = Color(0xFF3B2A00),
    heart = Color(0xFFFF6B70),
    open = Color(0xFF63D493),
    openSoft = Color(0xFF173A26),
    soon = Color(0xFFFFC266),
    soonSoft = Color(0xFF3D2C10),
    closed = Color(0xFFFF8A7A),
    closedSoft = Color(0xFF43201B),
    scrim = Color(0x99000000),
    categories = mapOf(
        Category.EAT to CategoryColors(Color(0xFFFF7A63), Color(0xFF45231D), Color(0xFFFFB4A6)),
        Category.TREATS to CategoryColors(Color(0xFFF06BA0), Color(0xFF441E2E), Color(0xFFFFADCB)),
        Category.PARKS to CategoryColors(Color(0xFF5CC487), Color(0xFF173A27), Color(0xFF9EE3B9)),
        Category.PLAYGROUNDS to CategoryColors(Color(0xFFF2B33D), Color(0xFF3F3010), Color(0xFFFFD98A)),
        Category.ATTRACTIONS to CategoryColors(Color(0xFFA08BFF), Color(0xFF2C2550), Color(0xFFCFC4FF)),
        Category.CULTURE to CategoryColors(Color(0xFF6B9BFF), Color(0xFF1B2B4D), Color(0xFFB4CCFF)),
    ),
)

val LightScheme = lightColorScheme(
    primary = LightHuntColors.primary,
    onPrimary = LightHuntColors.onPrimary,
    primaryContainer = LightHuntColors.primarySoft,
    onPrimaryContainer = LightHuntColors.onPrimarySoft,
    secondary = LightHuntColors.sunny,
    onSecondary = LightHuntColors.onSunny,
    secondaryContainer = LightHuntColors.primarySoft,
    onSecondaryContainer = LightHuntColors.onPrimarySoft,
    background = LightHuntColors.background,
    onBackground = LightHuntColors.ink,
    surface = LightHuntColors.surface,
    onSurface = LightHuntColors.ink,
    surfaceVariant = LightHuntColors.surfaceSunken,
    onSurfaceVariant = LightHuntColors.inkMuted,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFCF8),
    surfaceContainer = LightHuntColors.surface,
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = LightHuntColors.surfaceSunken,
    outline = Color(0xFFCFC6BB),
    outlineVariant = LightHuntColors.line,
    error = LightHuntColors.closed,
    inverseSurface = Color(0xFF26302E),
    inverseOnSurface = Color(0xFFF2EEE8),
    inversePrimary = Color(0xFF7FDACB),
    scrim = Color(0xFF101615),
)

val DarkScheme = darkColorScheme(
    primary = DarkHuntColors.primary,
    onPrimary = DarkHuntColors.onPrimary,
    primaryContainer = DarkHuntColors.primarySoft,
    onPrimaryContainer = DarkHuntColors.onPrimarySoft,
    secondary = DarkHuntColors.sunny,
    onSecondary = DarkHuntColors.onSunny,
    secondaryContainer = DarkHuntColors.primarySoft,
    onSecondaryContainer = DarkHuntColors.onPrimarySoft,
    background = DarkHuntColors.background,
    onBackground = DarkHuntColors.ink,
    surface = DarkHuntColors.surface,
    onSurface = DarkHuntColors.ink,
    surfaceVariant = DarkHuntColors.surfaceRaised,
    onSurfaceVariant = DarkHuntColors.inkMuted,
    surfaceContainerLowest = DarkHuntColors.surfaceSunken,
    surfaceContainerLow = Color(0xFF141B1A),
    surfaceContainer = DarkHuntColors.surface,
    surfaceContainerHigh = DarkHuntColors.surfaceRaised,
    surfaceContainerHighest = Color(0xFF2A3533),
    outline = Color(0xFF46524F),
    outlineVariant = DarkHuntColors.line,
    error = DarkHuntColors.closed,
    inverseSurface = Color(0xFFE6ECE9),
    inverseOnSurface = Color(0xFF1E2422),
    inversePrimary = LightHuntColors.primary,
    scrim = Color(0xFF000000),
)
