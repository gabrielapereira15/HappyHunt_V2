package com.example.happyhunt.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

private val LOCATION = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

fun Context.hasLocationPermission(): Boolean =
    LOCATION.any { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

/**
 * Returns an action that asks for location only when it is needed, at the
 * moment the person taps "use my location". Approximate location is enough.
 */
@Composable
fun rememberLocationRequest(onGranted: () -> Unit, onDenied: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val granted by rememberUpdatedState(onGranted)
    val denied by rememberUpdatedState(onDenied)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) granted() else denied()
    }
    return remember(context, launcher) {
        { if (context.hasLocationPermission()) granted() else launcher.launch(LOCATION) }
    }
}
