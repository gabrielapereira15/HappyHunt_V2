package com.example.happyhunt.ui.place

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.happyhunt.R
import com.example.happyhunt.domain.Distances
import com.example.happyhunt.domain.Geo
import com.example.happyhunt.domain.GeoPoint
import com.example.happyhunt.domain.OpeningHours
import com.example.happyhunt.domain.Place
import com.example.happyhunt.ui.Intents
import com.example.happyhunt.ui.clock
import com.example.happyhunt.ui.components.HeartButton
import com.example.happyhunt.ui.components.MessageState
import com.example.happyhunt.ui.components.StatusPill
import com.example.happyhunt.ui.components.WhiteStatusBarIcons
import com.example.happyhunt.ui.fullDay
import com.example.happyhunt.ui.label
import com.example.happyhunt.ui.line
import com.example.happyhunt.ui.map.CameraSpot
import com.example.happyhunt.ui.map.HuntMap
import com.example.happyhunt.ui.map.MapStyle
import com.example.happyhunt.ui.map.SelectedPin
import com.example.happyhunt.ui.kindLine
import com.example.happyhunt.ui.theme.Hunt
import com.example.happyhunt.ui.theme.HuntIcons
import com.example.happyhunt.ui.title
import kotlinx.coroutines.launch
import org.maplibre.geojson.FeatureCollection
import java.time.DayOfWeek
import java.time.LocalDateTime

private val HERO = 300.dp

@Composable
fun PlaceScreen(viewModel: PlaceViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val resources = LocalResources.current
    val place = state.place
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    // Once the hero has scrolled away, a plain bar with the name takes over the top.
    val collapsed by remember { derivedStateOf { scroll.value > with(density) { (HERO - 96.dp).toPx() } } }
    WhiteStatusBarIcons(enabled = place != null && !collapsed)

    val noApp: (Boolean) -> Unit = { opened ->
        if (!opened) scope.launch { snackbar.showSnackbar(resources.getString(R.string.no_app)) }
    }

    Box(Modifier.fillMaxSize().background(Hunt.colors.background)) {
        when {
            place != null -> {
                val name = place.title()
                Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
                    Hero(place, state, onCredit = { state.photo?.let { noApp(Intents.web(context, it.creditUrl)) } })
                    Column(
                        Modifier
                            .offset(y = (-28).dp)
                            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                            .background(Hunt.colors.background)
                            .padding(top = 24.dp),
                    ) {
                        Header(place, state)
                        Spacer(Modifier.height(20.dp))
                        Actions(
                            place = place,
                            onDirections = { noApp(Intents.directions(context, place, name)) },
                            onCall = { place.phone?.let { noApp(Intents.call(context, it)) } },
                            onWebsite = { place.website?.let { noApp(Intents.web(context, it)) } },
                            onShare = { noApp(Intents.share(context, place, name)) },
                        )
                        Spacer(Modifier.height(20.dp))
                        Hours(place, state)
                        GoodToKnow(place)
                        Info(place, onCall = { place.phone?.let { noApp(Intents.call(context, it)) } }, onWebsite = { place.website?.let { noApp(Intents.web(context, it)) } })
                        MiniMap(place, onClick = { noApp(Intents.directions(context, place, name)) })
                        EditOnOsm(onClick = { noApp(Intents.web(context, place.osmUrl)) })
                        Spacer(Modifier.navigationBarsPadding().height(8.dp))
                    }
                }
                TopBar(title = name, collapsed = collapsed, saved = state.saved, onBack = onBack, onToggleSaved = viewModel::toggleSaved)
            }
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Hunt.colors.primary)
            }
            else -> Column(Modifier.fillMaxSize().statusBarsPadding()) {
                RoundButton(HuntIcons.Back, stringResource(R.string.back), onBack, Modifier.padding(12.dp))
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    MessageState(
                        icon = HuntIcons.Offline,
                        title = stringResource(R.string.explore_offline_title),
                        body = stringResource(R.string.place_not_found),
                        primaryAction = stringResource(R.string.try_again) to viewModel::load,
                    )
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
    }
}

/** A photo when Wikimedia has one, otherwise a bright banner in the place's colours. */
@Composable
private fun Hero(place: Place, state: PlaceState, onCredit: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(HERO)) {
        Banner(place)
        state.photo?.let { photo ->
            AsyncImage(
                model = photo.url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // Darkened at the top for the status bar and buttons, at the bottom for the credit.
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.45f), 0.35f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.25f)),
                ),
            )
            Text(
                stringResource(R.string.place_photo_credit),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(onClick = onCredit)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
private fun Banner(place: Place) {
    val colors = Hunt.colors.category(place.category)
    val deep = lerp(colors.pin, Color.Black, 0.18f)
    BoxWithConstraints(
        Modifier.fillMaxSize().background(Brush.linearGradient(listOf(colors.pin, deep))),
        contentAlignment = Alignment.Center,
    ) {
        val w = maxWidth
        val h = maxHeight
        val icon = HuntIcons.forKind(place.kind)
        // A scatter of the same icon, faint, like a patterned wrapping paper.
        val scatter = listOf(
            Triple(-0.38f to -0.22f, 34, -14f), Triple(0.36f to -0.28f, 28, 12f), Triple(-0.30f to 0.22f, 26, 10f),
            Triple(0.40f to 0.16f, 38, -8f), Triple(-0.05f to -0.36f, 22, 6f), Triple(0.12f to 0.30f, 24, -12f),
        )
        scatter.forEach { (position, size, tilt) ->
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.16f),
                modifier = Modifier.offset(x = w * position.first, y = h * position.second).rotate(tilt).size(size.dp),
            )
        }
        Box(Modifier.size(112.dp).background(Color.White.copy(alpha = 0.18f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(56.dp))
        }
    }
}

@Composable
private fun TopBar(title: String, collapsed: Boolean, saved: Boolean, onBack: () -> Unit, onToggleSaved: () -> Unit) {
    Box {
        AnimatedVisibility(visible = collapsed, enter = fadeIn(), exit = fadeOut()) {
            Surface(color = Hunt.colors.background, shadowElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(HuntIcons.Back, contentDescription = stringResource(R.string.back), tint = Hunt.colors.ink)
                    }
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Hunt.colors.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    HeartButton(saved = saved, onClick = onToggleSaved, tint = Hunt.colors.ink)
                }
            }
        }
        AnimatedVisibility(visible = !collapsed, enter = fadeIn(), exit = fadeOut()) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundButton(HuntIcons.Back, stringResource(R.string.back), onBack)
                Spacer(Modifier.weight(1f))
                Surface(shape = CircleShape, color = Hunt.colors.surface, shadowElevation = 4.dp, modifier = Modifier.size(44.dp)) {
                    Box(contentAlignment = Alignment.Center) { HeartButton(saved = saved, onClick = onToggleSaved, tint = Hunt.colors.ink) }
                }
            }
        }
    }
}

@Composable
private fun RoundButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, shape = CircleShape, color = Hunt.colors.surface, shadowElevation = 4.dp, modifier = modifier.size(44.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, tint = Hunt.colors.ink, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun Header(place: Place, state: PlaceState) {
    val colors = Hunt.colors.category(place.category)
    Column(Modifier.padding(horizontal = 24.dp)) {
        Text(
            place.kindLine(),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSoft,
            modifier = Modifier.clip(CircleShape).background(colors.soft).padding(horizontal = 12.dp, vertical = 5.dp),
        )
        Spacer(Modifier.height(10.dp))
        Text(place.title(), style = MaterialTheme.typography.headlineMedium, color = Hunt.colors.ink)
        place.address?.let {
            Spacer(Modifier.height(2.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = Hunt.colors.inkMuted)
        }
        Spacer(Modifier.height(12.dp))
        FlowRowCompat {
            val status = state.hours?.takeIf { state.sameClock }?.statusAt(state.now)
            if (status != null) StatusPill(status.line())
            state.distanceMeters?.let { meters ->
                Row(
                    Modifier.clip(CircleShape).background(Hunt.colors.surfaceSunken).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(HuntIcons.Walk, contentDescription = null, tint = Hunt.colors.inkSoft, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.place_away, Distances.format(meters, state.units, LocalLocale.current.platformLocale)) +
                            " · " + stringResource(R.string.walk_minutes, Geo.walkingMinutes(meters)),
                        style = MaterialTheme.typography.labelLarge,
                        color = Hunt.colors.inkSoft,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowCompat(content: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

@Composable
private fun Actions(place: Place, onDirections: () -> Unit, onCall: () -> Unit, onWebsite: () -> Unit, onShare: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        ActionButton(HuntIcons.Directions, stringResource(R.string.place_directions), primary = true, onClick = onDirections)
        if (place.phone != null) ActionButton(HuntIcons.Phone, stringResource(R.string.place_call), onClick = onCall)
        if (place.website != null) ActionButton(HuntIcons.Globe, stringResource(R.string.place_website), onClick = onWebsite)
        ActionButton(HuntIcons.Share, stringResource(R.string.place_share), onClick = onShare)
    }
}

@Composable
private fun ActionButton(icon: ImageVector, label: String, onClick: () -> Unit, primary: Boolean = false) {
    Column(
        Modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(56.dp)
                .background(if (primary) Hunt.colors.primary else Hunt.colors.primarySoft, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = if (primary) Hunt.colors.onPrimary else Hunt.colors.primary, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = Hunt.colors.ink)
    }
}

@Composable
private fun Section(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Hunt.colors.surface,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = Hunt.colors.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, color = Hunt.colors.ink)
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun Hours(place: Place, state: PlaceState) {
    Section(stringResource(R.string.place_hours), HuntIcons.Clock) {
        val hours = state.hours
        when {
            hours == null && place.openingHours == null -> Muted(stringResource(R.string.place_hours_unknown))
            hours == null -> Muted(stringResource(R.string.place_hours_as_written, place.openingHours.orEmpty()))
            hours.alwaysOpen -> Text(stringResource(R.string.status_open_24), style = MaterialTheme.typography.bodyLarge, color = Hunt.colors.open, fontWeight = FontWeight.Bold)
            else -> Week(hours, today = state.now)
        }
        if (hours != null) {
            Spacer(Modifier.height(12.dp))
            Muted(stringResource(if (state.sameClock) R.string.place_hours_note else R.string.place_hours_elsewhere))
        }
    }
}

@Composable
private fun Week(hours: OpeningHours, today: LocalDateTime) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DayOfWeek.entries.forEach { day ->
            val isToday = day == today.dayOfWeek
            val ranges = hours.rangesOn(day)
            val text = if (ranges.isEmpty()) {
                stringResource(R.string.place_day_closed)
            } else {
                ranges.map { (start, end) ->
                    if (end == null) stringResource(R.string.place_open_end, clock(start)) else "${clock(start)} – ${clock(end)}"
                }.joinToString(", ")
            }
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.padding(top = 7.dp).size(6.dp).background(if (isToday) Hunt.colors.primary else Color.Transparent, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(
                    fullDay(day),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (isToday) Hunt.colors.ink else Hunt.colors.inkSoft,
                    modifier = Modifier.width(112.dp),
                )
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (ranges.isEmpty()) Hunt.colors.inkMuted else if (isToday) Hunt.colors.ink else Hunt.colors.inkSoft,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoodToKnow(place: Place) {
    val highlights = place.highlights
    val ages = place.ages
    if (highlights.isEmpty() && ages == null) return
    Section(stringResource(R.string.place_good_to_know), HuntIcons.Sparkles) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            highlights.forEach { highlight -> Chip(HuntIcons.forHighlight(highlight), stringResource(highlight.label())) }
            ages?.let {
                val text = if (it.last >= 99) pluralStringResource(R.plurals.place_ages_from, it.first, it.first) else stringResource(R.string.place_ages, it.first, it.last)
                Chip(HuntIcons.Baby, text)
            }
        }
    }
}

@Composable
private fun Chip(icon: ImageVector, label: String) {
    Row(
        Modifier.clip(CircleShape).background(Hunt.colors.primarySoft).padding(start = 10.dp, end = 14.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Hunt.colors.onPrimarySoft, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = Hunt.colors.onPrimarySoft)
    }
}

@Composable
private fun Info(place: Place, onCall: () -> Unit, onWebsite: () -> Unit) {
    val rows = buildList<@Composable () -> Unit> {
        place.address?.let { add { InfoRow(HuntIcons.Pin, stringResource(R.string.place_address), it, null) } }
        place.phone?.let { add { InfoRow(HuntIcons.Phone, stringResource(R.string.place_phone), it, onCall) } }
        place.website?.let { url ->
            val host = url.substringAfter("://").removePrefix("www.").trimEnd('/')
            add { InfoRow(HuntIcons.Globe, stringResource(R.string.place_website), host, onWebsite) }
        }
    }
    val description = place.tags["description"]
    if (rows.isEmpty() && description == null) return
    Section(stringResource(R.string.place_info), HuntIcons.Info) {
        description?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = Hunt.colors.inkSoft)
            if (rows.isNotEmpty()) Spacer(Modifier.height(12.dp))
        }
        rows.forEachIndexed { index, row ->
            if (index > 0) HorizontalDivider(Modifier.padding(vertical = 4.dp), color = Hunt.colors.line)
            row()
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String, onClick: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Hunt.colors.inkMuted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = Hunt.colors.inkMuted)
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge,
                color = if (onClick != null) Hunt.colors.primary else Hunt.colors.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onClick != null) Icon(HuntIcons.ChevronRight, contentDescription = null, tint = Hunt.colors.inkMuted, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun MiniMap(place: Place, onClick: () -> Unit) {
    Section(stringResource(R.string.place_on_the_map), HuntIcons.Explore) {
        Box(Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(18.dp))) {
            HuntMap(
                pins = remember { FeatureCollection.fromFeatures(emptyList()) },
                selected = SelectedPin(place.id, place.point, place.kind),
                origin = null,
                radiusMeters = null,
                dark = Hunt.colors.isDark,
                initialCamera = remember(place.id) { CameraSpot(GeoPoint(place.point.lat, place.point.lon), 15.5) },
                goal = null,
                padding = PaddingValues(top = 40.dp),
                interactive = false,
                modifier = Modifier.fillMaxSize(),
            )
            // The map itself does not take touches here; the whole card opens directions.
            Box(Modifier.fillMaxSize().clickable(onClick = onClick))
        }
        Spacer(Modifier.height(8.dp))
        Text(MapStyle.ATTRIBUTION, style = MaterialTheme.typography.labelSmall, color = Hunt.colors.inkMuted)
    }
}

@Composable
private fun EditOnOsm(onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 18.dp)) {
        Text(stringResource(R.string.place_osm_title), style = MaterialTheme.typography.titleSmall, color = Hunt.colors.ink)
        Spacer(Modifier.height(2.dp))
        Text(stringResource(R.string.place_osm_body), style = MaterialTheme.typography.bodySmall, color = Hunt.colors.inkMuted)
        TextButton(onClick = onClick, contentPadding = PaddingValues(0.dp)) {
            Text(stringResource(R.string.place_osm_link), fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(4.dp))
            Icon(HuntIcons.External, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun Muted(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = Hunt.colors.inkMuted)
}
