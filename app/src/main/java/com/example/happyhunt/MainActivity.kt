package com.example.happyhunt

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.happyhunt.data.Settings
import com.example.happyhunt.data.ThemeMode
import com.example.happyhunt.ui.HappyHuntRoot
import com.example.happyhunt.ui.theme.HappyHuntTheme

class MainActivity : ComponentActivity() {
    private val container get() = (application as HappyHuntApp).container
    private var settingsLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // The splash stays until the settings are read, so a returning visitor never glimpses the welcome screen.
        splash.setKeepOnScreenCondition { !settingsLoaded }

        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle<Settings?>(initialValue = null)
            val current = settings ?: return@setContent
            SideEffect { settingsLoaded = true }

            val dark = when (current.theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            DisposableEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                // The window behind Compose follows the app's theme too, not only the phone's,
                // so a theme chosen in Settings never flashes the other colour.
                window.decorView.setBackgroundColor(if (dark) NIGHT else CREAM)
                onDispose { }
            }
            HappyHuntTheme(dark = dark) {
                HappyHuntRoot(container = container, settings = current)
            }
        }
    }

    private companion object {
        const val CREAM = 0xFFFBF7F2.toInt()
        const val NIGHT = 0xFF101615.toInt()
    }
}
