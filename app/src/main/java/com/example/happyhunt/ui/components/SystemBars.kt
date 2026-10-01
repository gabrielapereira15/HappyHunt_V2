package com.example.happyhunt.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * White status bar icons while this screen shows, for screens that draw
 * something dark or colourful behind the status bar (the welcome screen, a
 * place photo). The theme's choice comes back when the screen leaves.
 */
@Composable
fun WhiteStatusBarIcons(enabled: Boolean = true) {
    val view = LocalView.current
    val activity = view.context.findActivity() ?: return
    DisposableEffect(enabled) {
        val controller = WindowCompat.getInsetsController(activity.window, view)
        val before = controller.isAppearanceLightStatusBars
        if (enabled) controller.isAppearanceLightStatusBars = false
        onDispose { controller.isAppearanceLightStatusBars = before }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
