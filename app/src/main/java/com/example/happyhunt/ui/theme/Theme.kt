package com.example.happyhunt.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

val LocalHuntColors = staticCompositionLocalOf { LightHuntColors }

/** Soft, generous corners: the app should feel friendly, like a pebble in the hand. */
val HuntShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun HappyHuntTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = HuntTypography,
        shapes = HuntShapes,
    ) {
        CompositionLocalProvider(LocalHuntColors provides if (dark) DarkHuntColors else LightHuntColors, content = content)
    }
}

/** The Happy Hunt colours for the current theme: `Hunt.colors.primary`. */
object Hunt {
    val colors: HuntColors
        @Composable
        @ReadOnlyComposable
        get() = LocalHuntColors.current
}
