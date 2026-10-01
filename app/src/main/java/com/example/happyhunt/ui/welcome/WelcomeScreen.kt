package com.example.happyhunt.ui.welcome

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.happyhunt.AppContainer
import com.example.happyhunt.R
import com.example.happyhunt.data.Locator
import com.example.happyhunt.domain.Category
import com.example.happyhunt.ui.components.WhiteStatusBarIcons
import com.example.happyhunt.ui.components.rememberLocationRequest
import com.example.happyhunt.ui.theme.Hunt
import com.example.happyhunt.ui.theme.HuntIcons
import kotlinx.coroutines.launch

/**
 * The first screen, once: what the app does, and where to start looking.
 * There is nothing to sign up for, so the very next tap is the map.
 */
@Composable
fun WelcomeScreen(container: AppContainer, onReady: () -> Unit, onChooseArea: () -> Unit) {
    WhiteStatusBarIcons()
    val scope = rememberCoroutineScope()
    var locating by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Int?>(null) }

    val useLocation = rememberLocationRequest(
        onGranted = {
            locating = true
            message = null
            scope.launch {
                val result = container.origins.useMyLocation()
                locating = false
                when (result) {
                    is Locator.Result.Found -> onReady()
                    Locator.Result.LocationOff -> message = R.string.location_off
                    Locator.Result.NoPermission -> message = R.string.location_denied
                    Locator.Result.NotFound -> message = R.string.location_not_found
                }
            }
        },
        onDenied = { message = R.string.location_denied },
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0E7A6E), Color(0xFF13968A)))),
    ) {
        Illustration(Modifier.weight(1f).fillMaxWidth().statusBarsPadding())
        Surface(
            shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
            color = Hunt.colors.background,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier.navigationBarsPadding().padding(start = 28.dp, end = 28.dp, top = 32.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(R.string.welcome_title),
                    style = MaterialTheme.typography.displaySmall,
                    color = Hunt.colors.ink,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.welcome_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Hunt.colors.inkMuted,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(28.dp))
                Button(
                    onClick = { if (!locating) useLocation() },
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    if (locating) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.5.dp, color = Hunt.colors.onPrimary)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.welcome_locating), style = MaterialTheme.typography.titleMedium)
                    } else {
                        Icon(HuntIcons.Locate, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.welcome_use_location), style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onChooseArea,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Hunt.colors.primary),
                ) {
                    Icon(HuntIcons.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.welcome_choose_area), style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(18.dp))
                val note = message
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        if (note == null) HuntIcons.Shield else HuntIcons.Info,
                        contentDescription = null,
                        tint = if (note == null) Hunt.colors.primary else Hunt.colors.soon,
                        modifier = Modifier.size(18.dp).padding(top = 1.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(note ?: R.string.welcome_privacy),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (note == null) Hunt.colors.inkMuted else Hunt.colors.ink,
                        fontWeight = if (note == null) FontWeight.Medium else FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/** The logo pin, bobbing gently, with the six kinds of outing floating around it. */
@Composable
private fun Illustration(modifier: Modifier) {
    val bob by rememberInfiniteTransition(label = "bob").animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "bob",
    )
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val w = maxWidth
        val h = maxHeight
        // Where each bubble sits, as fractions of the space, and how it leans.
        val spots = listOf(
            Triple(Category.EAT, -0.32f to -0.30f, -10f),
            Triple(Category.PARKS, 0.30f to -0.33f, 8f),
            Triple(Category.TREATS, -0.38f to 0.04f, 6f),
            Triple(Category.CULTURE, 0.37f to 0.02f, -8f),
            Triple(Category.PLAYGROUNDS, -0.22f to 0.32f, -6f),
            Triple(Category.ATTRACTIONS, 0.24f to 0.31f, 10f),
        )
        spots.forEachIndexed { index, (category, position, tilt) ->
            val colors = Hunt.colors.category(category)
            val drift = if (index % 2 == 0) bob else -bob
            Box(
                Modifier
                    .offset(x = w * position.first, y = h * position.second)
                    .offset { IntOffset(0, (drift * 0.6f).dp.roundToPx()) }
                    .rotate(tilt)
                    .shadow(10.dp, RoundedCornerShape(22.dp))
                    .size(64.dp)
                    .background(Color.White, RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(HuntIcons.forCategory(category), contentDescription = null, tint = colors.pin, modifier = Modifier.size(30.dp))
            }
        }
        Box(Modifier.size(170.dp).background(Color.White.copy(alpha = 0.12f), CircleShape))
        Box(Modifier.size(120.dp).background(Color.White.copy(alpha = 0.12f), CircleShape))
        Image(
            painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.offset { IntOffset(0, bob.dp.roundToPx()) }.size(210.dp),
        )
    }
}
