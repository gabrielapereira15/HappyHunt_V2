package com.example.happyhunt.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.happyhunt.AppContainer
import com.example.happyhunt.R
import com.example.happyhunt.data.Settings
import com.example.happyhunt.data.ThemeMode
import com.example.happyhunt.domain.Distances
import com.example.happyhunt.domain.Geo
import com.example.happyhunt.domain.Units
import com.example.happyhunt.ui.Intents
import com.example.happyhunt.ui.theme.Hunt
import com.example.happyhunt.ui.theme.HuntIcons
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    val settings: StateFlow<Settings?> = container.settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setRadius(meters: Int) = viewModelScope.launch { container.settings.setRadius(meters) }

    fun setUnits(units: Units?) = viewModelScope.launch { container.settings.setUnits(units) }

    fun setTheme(theme: ThemeMode) = viewModelScope.launch { container.settings.setTheme(theme) }

    fun clearSaved(then: () -> Unit) = viewModelScope.launch {
        container.saved.clear()
        then()
    }

    fun clearRecent(then: () -> Unit) = viewModelScope.launch {
        container.forgetRecentSearches()
        then()
    }
}

private data class Credit(val name: Int, val what: Int, val url: String)

private val credits = listOf(
    Credit(R.string.credit_osm, R.string.credit_osm_what, "https://www.openstreetmap.org/copyright"),
    Credit(R.string.credit_tiles, R.string.credit_tiles_what, "https://openfreemap.org"),
    Credit(R.string.credit_overpass, R.string.credit_overpass_what, "https://overpass-api.de"),
    Credit(R.string.credit_photon, R.string.credit_photon_what, "https://photon.komoot.io"),
    Credit(R.string.credit_wikimedia, R.string.credit_wikimedia_what, "https://commons.wikimedia.org"),
    Credit(R.string.credit_maplibre, R.string.credit_maplibre_what, "https://maplibre.org"),
    Credit(R.string.credit_design, R.string.credit_design_what, "https://lucide.dev"),
)

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val current = settings ?: return
    val context = LocalContext.current
    val resources = LocalResources.current
    val locale = LocalLocale.current.platformLocale
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmClear by remember { mutableStateOf(false) }
    val units = current.units ?: Geo.unitsFor(locale)
    val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() }
    val tell: (Int) -> Unit = { message -> scope.launch { snackbar.showSnackbar(resources.getString(message)) } }

    Box(Modifier.fillMaxSize().background(Hunt.colors.background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Text(
                stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineLarge,
                color = Hunt.colors.ink,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 12.dp),
            )

            Card(stringResource(R.string.settings_radius), HuntIcons.Compass) {
                Segmented(
                    options = Settings.RADII.map { it to Distances.format(it.toDouble(), units, locale) },
                    selected = current.radiusMeters,
                    onSelect = { viewModel.setRadius(it) },
                )
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.settings_radius_hint), style = MaterialTheme.typography.bodySmall, color = Hunt.colors.inkMuted)
            }
            Card(stringResource(R.string.settings_units), HuntIcons.Ruler) {
                Segmented(
                    options = listOf(
                        null to stringResource(R.string.units_auto),
                        Units.METRIC to stringResource(R.string.units_metric),
                        Units.IMPERIAL to stringResource(R.string.units_imperial),
                    ),
                    selected = current.units,
                    onSelect = { viewModel.setUnits(it) },
                )
            }
            Card(stringResource(R.string.settings_theme), HuntIcons.SunMoon) {
                Segmented(
                    options = listOf(
                        ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                        ThemeMode.LIGHT to stringResource(R.string.theme_light),
                        ThemeMode.DARK to stringResource(R.string.theme_dark),
                    ),
                    selected = current.theme,
                    onSelect = { viewModel.setTheme(it) },
                )
            }
            Card(stringResource(R.string.settings_data), HuntIcons.Shield) {
                Text(stringResource(R.string.settings_privacy_body), style = MaterialTheme.typography.bodyMedium, color = Hunt.colors.inkSoft)
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Hunt.colors.line)
                ActionRow(HuntIcons.Trash, stringResource(R.string.settings_clear_saved), stringResource(R.string.settings_clear_saved_hint), danger = true) {
                    confirmClear = true
                }
                HorizontalDivider(color = Hunt.colors.line)
                ActionRow(HuntIcons.History, stringResource(R.string.settings_clear_recent), stringResource(R.string.settings_clear_recent_hint)) {
                    viewModel.clearRecent { tell(R.string.settings_cleared_recent) }
                }
            }
            Card(stringResource(R.string.settings_credits), HuntIcons.Heart) {
                credits.forEachIndexed { index, credit ->
                    if (index > 0) HorizontalDivider(color = Hunt.colors.line)
                    ActionRow(null, stringResource(credit.name), stringResource(credit.what), trailing = HuntIcons.External) {
                        if (!Intents.web(context, credit.url)) tell(R.string.no_app)
                    }
                }
            }
            version?.let {
                Text(
                    stringResource(R.string.settings_version, it),
                    style = MaterialTheme.typography.labelMedium,
                    color = Hunt.colors.inkMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                )
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            icon = { Icon(HuntIcons.Trash, contentDescription = null, tint = Hunt.colors.heart) },
            title = { Text(stringResource(R.string.settings_clear_saved_confirm_title)) },
            text = { Text(stringResource(R.string.settings_clear_saved_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    viewModel.clearSaved { tell(R.string.settings_cleared_saved) }
                }) { Text(stringResource(R.string.settings_clear_saved_confirm), color = Hunt.colors.heart, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.cancel)) } },
            containerColor = Hunt.colors.surface,
        )
    }
}

@Composable
private fun Card(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Hunt.colors.surface,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).background(Hunt.colors.primarySoft, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = Hunt.colors.primary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, color = Hunt.colors.ink)
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

/** A row of choices in a soft track; the chosen one lifts out of it. */
@Composable
private fun <T> Segmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Hunt.colors.surfaceSunken).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (value, label) ->
            val chosen = value == selected
            Surface(
                onClick = { onSelect(value) },
                shape = RoundedCornerShape(12.dp),
                color = if (chosen) Hunt.colors.surfaceRaised else Hunt.colors.surfaceSunken,
                shadowElevation = if (chosen) 2.dp else 0.dp,
                modifier = Modifier.weight(1f).height(42.dp).semantics {
                    this.selected = chosen
                    role = Role.RadioButton
                },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (chosen) Hunt.colors.primary else Hunt.colors.inkSoft,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector?,
    title: String,
    detail: String?,
    danger: Boolean = false,
    trailing: ImageVector = HuntIcons.ChevronRight,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (danger) Hunt.colors.heart else Hunt.colors.inkSoft, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = if (danger) Hunt.colors.heart else Hunt.colors.ink)
            if (detail != null) Text(detail, style = MaterialTheme.typography.bodySmall, color = Hunt.colors.inkMuted)
        }
        Icon(trailing, contentDescription = null, tint = Hunt.colors.inkMuted, modifier = Modifier.size(18.dp))
    }
}
