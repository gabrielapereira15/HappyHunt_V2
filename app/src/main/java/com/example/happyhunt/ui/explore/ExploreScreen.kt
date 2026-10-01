package com.example.happyhunt.ui.explore

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.happyhunt.R
import com.example.happyhunt.data.Locator
import com.example.happyhunt.data.Settings
import com.example.happyhunt.domain.Category
import com.example.happyhunt.domain.Distances
import com.example.happyhunt.domain.GeoPoint
import com.example.happyhunt.ui.components.HeartButton
import com.example.happyhunt.ui.components.KindTile
import com.example.happyhunt.ui.components.PlaceRow
import com.example.happyhunt.ui.components.rememberLocationRequest
import com.example.happyhunt.ui.components.StatusText
import com.example.happyhunt.ui.inSentence
import com.example.happyhunt.ui.label
import com.example.happyhunt.ui.line
import com.example.happyhunt.ui.map.CameraSpot
import com.example.happyhunt.ui.map.HuntMap
import com.example.happyhunt.ui.map.MapStyle
import com.example.happyhunt.ui.map.SelectedPin
import com.example.happyhunt.ui.subtitle
import com.example.happyhunt.ui.theme.Hunt
import com.example.happyhunt.ui.theme.HuntIcons
import com.example.happyhunt.ui.title
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val PEEK = 212.dp

/** On a short screen (a phone on its side) the sheet peeks less, so the map keeps some room. */
private val PEEK_SHORT = 120.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    viewModel: ExploreViewModel,
    onOpenPlace: (String) -> Unit,
    onChangeArea: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val goal by viewModel.goal.collectAsStateWithLifecycle()
    val sheetState = rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded)
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    val density = LocalDensity.current
    val listState = rememberLazyListState()

    val locate = rememberLocationRequest(
        onGranted = viewModel::locate,
        onDenied = { scope.launch { snackbar.showSnackbar(resources.getString(R.string.location_denied)) } },
    )

    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is ExploreEvent.LocationProblem -> {
                    val message = when (event.result) {
                        Locator.Result.LocationOff -> R.string.location_off
                        Locator.Result.NoPermission -> R.string.location_denied
                        else -> R.string.location_not_found
                    }
                    snackbar.showSnackbar(resources.getString(message))
                }
            }
        }
    }
    // A new search starts the list from the top.
    // A new search or filter starts the list from the top; coming back to the screen keeps the place.
    val listKey = "${state.load}|${state.category}|${state.openNow}|${state.origin?.point}|${state.radiusMeters}"
    var handledListKey by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(listKey) {
        if (handledListKey != null && handledListKey != listKey && state.places.isNotEmpty()) listState.scrollToItem(0)
        handledListKey = listKey
    }

    BackHandler(enabled = state.selected != null) { viewModel.select(null) }
    BackHandler(enabled = state.selected == null && sheetState.currentValue == SheetValue.Expanded) {
        scope.launch { sheetState.partialExpand() }
    }

    var topChrome by remember { mutableStateOf(0.dp) }
    val initialCamera = remember {
        viewModel.camera ?: CameraSpot(state.origin?.point ?: GeoPoint(43.6532, -79.3832), if (state.origin != null) 14.0 else 3.0)
    }
    val windowHeight = with(density) { LocalWindowInfo.current.containerSize.height.toDp() }
    val peek = if (windowHeight < 560.dp) PEEK_SHORT else PEEK
    val mapBottom = peek + if (state.selected != null) 220.dp else 64.dp

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = peek,
        sheetContainerColor = Hunt.colors.surface,
        sheetShadowElevation = 16.dp,
        sheetContent = {
            ExploreSheet(
                state = state,
                listState = listState,
                onOpen = onOpenPlace,
                onToggleSaved = viewModel::toggleSaved,
                onRetry = viewModel::retry,
                onOpenNow = viewModel::setOpenNow,
                onShowAll = { viewModel.setCategory(null) },
                onLookFurther = viewModel::lookFurther,
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) {
        Box(Modifier.fillMaxSize()) {
            HuntMap(
                pins = state.pins,
                selected = state.selected?.let { SelectedPin(it.place.id, it.place.point, it.place.kind) },
                origin = state.origin?.point,
                radiusMeters = state.radiusMeters,
                dark = Hunt.colors.isDark,
                initialCamera = initialCamera,
                goal = goal,
                padding = PaddingValues(start = 24.dp, end = 24.dp, top = topChrome + 12.dp, bottom = mapBottom),
                modifier = Modifier.fillMaxSize(),
                onCameraIdle = viewModel::onCameraIdle,
                onUserMovedMap = viewModel::onUserMovedMap,
                onPinClick = { id -> viewModel.select(id) },
                onMapClick = { viewModel.select(null) },
                onGoalHandled = viewModel::goalHandled,
            )
            TopChrome(
                state = state,
                onChangeArea = onChangeArea,
                onCategory = viewModel::setCategory,
                onSearchHere = viewModel::searchThisArea,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .onSizeChanged { topChrome = with(density) { it.height.toDp() } },
            )
            AboveSheet(sheetState = sheetState, fallback = peek) {
                AnimatedVisibility(
                    visible = state.selected != null,
                    enter = slideInVertically { it / 2 } + fadeIn(),
                    exit = slideOutVertically { it / 2 } + fadeOut(),
                ) {
                    state.selected?.let { item ->
                        PreviewCard(
                            item = item,
                            state = state,
                            onOpen = { onOpenPlace(item.place.id) },
                            onToggleSaved = { viewModel.toggleSaved(item.place) },
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        MapStyle.ATTRIBUTION,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                        color = Hunt.colors.inkMuted,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .background(Hunt.colors.surface.copy(alpha = 0.82f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                    RoundMapButton(
                        icon = HuntIcons.Locate,
                        description = stringResource(R.string.explore_locate),
                        busy = state.locating,
                        onClick = locate,
                    )
                }
            }
        }
    }
}

/** Places its content just above the top edge of the bottom sheet, and fades it away as the sheet comes up. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboveSheet(sheetState: SheetState, fallback: Dp, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    Column(
        Modifier
            .fillMaxWidth()
            // Outside the layout below, so its size is the whole screen's height.
            .graphicsLayer {
                val sheetTop = runCatching { sheetState.requireOffset() }.getOrNull() ?: Float.MAX_VALUE
                alpha = ((sheetTop / size.height - 0.45f) / 0.2f).coerceIn(0f, 1f)
            }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(minHeight = 0))
                layout(placeable.width, constraints.maxHeight) {
                    val sheetTop = runCatching { sheetState.requireOffset() }.getOrNull()
                        ?: (constraints.maxHeight - with(density) { fallback.toPx() })
                    placeable.place(0, (sheetTop - placeable.height).roundToInt())
                }
            },
    ) { content() }
}

@Composable
private fun TopChrome(
    state: ExploreState,
    onChangeArea: () -> Unit,
    onCategory: (Category?) -> Unit,
    onSearchHere: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AreaPill(state, onChangeArea, Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            item {
                CategoryChip(
                    label = stringResource(R.string.category_all),
                    icon = HuntIcons.Sparkles,
                    color = Hunt.colors.primary,
                    selected = state.category == null,
                    onClick = { onCategory(null) },
                )
            }
            items(Category.entries) { category ->
                CategoryChip(
                    label = stringResource(category.label()),
                    icon = HuntIcons.forCategory(category),
                    color = Hunt.colors.category(category).pin,
                    selected = state.category == category,
                    onClick = { onCategory(if (state.category == category) null else category) },
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        AnimatedVisibility(
            visible = state.load == Load.Loading,
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f),
        ) {
            FloatingPill(color = Hunt.colors.surface) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Hunt.colors.primary)
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.explore_looking), style = MaterialTheme.typography.labelLarge, color = Hunt.colors.ink)
            }
        }
        AnimatedVisibility(
            visible = state.searchHere && state.load != Load.Loading,
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f),
        ) {
            FloatingPill(color = Hunt.colors.primary, onClick = onSearchHere) {
                Icon(HuntIcons.Refresh, contentDescription = null, tint = Hunt.colors.onPrimary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.explore_search_here), style = MaterialTheme.typography.labelLarge, color = Hunt.colors.onPrimary)
            }
        }
    }
}

@Composable
private fun AreaPill(state: ExploreState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val origin = state.origin
    val label = origin?.label ?: stringResource(if (origin?.mine == true) R.string.explore_near_you else R.string.explore_this_area)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = Hunt.colors.surface,
        shadowElevation = 6.dp,
        modifier = modifier.fillMaxWidth().height(56.dp),
    ) {
        Row(Modifier.padding(start = 8.dp, end = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).background(Hunt.colors.primarySoft, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (origin?.mine == true) HuntIcons.Locate else HuntIcons.Pin,
                    contentDescription = null,
                    tint = Hunt.colors.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleSmall, color = Hunt.colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(R.string.explore_within, Distances.format(state.radiusMeters.toDouble(), state.units, LocalLocale.current.platformLocale)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Hunt.colors.inkMuted,
                    maxLines = 1,
                )
            }
            Icon(HuntIcons.Search, contentDescription = stringResource(R.string.explore_change_area), tint = Hunt.colors.inkSoft, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun CategoryChip(label: String, icon: ImageVector, color: Color, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) color else Hunt.colors.surface,
        shadowElevation = if (selected) 4.dp else 3.dp,
        modifier = Modifier.height(40.dp).semantics { this.selected = selected },
    ) {
        Row(Modifier.padding(start = 12.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = if (selected) Color.White else color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) Color.White else Hunt.colors.ink)
        }
    }
}

@Composable
private fun FloatingPill(color: Color, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val inner: @Composable () -> Unit = {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) { content() }
    }
    if (onClick != null) {
        Surface(onClick = onClick, shape = CircleShape, color = color, shadowElevation = 6.dp, content = inner)
    } else {
        Surface(shape = CircleShape, color = color, shadowElevation = 6.dp, content = inner)
    }
}

@Composable
private fun RoundMapButton(icon: ImageVector, description: String, busy: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = Hunt.colors.surface, shadowElevation = 6.dp, modifier = Modifier.size(52.dp)) {
        Box(contentAlignment = Alignment.Center) {
            if (busy) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp, color = Hunt.colors.primary)
            } else {
                Icon(icon, contentDescription = description, tint = Hunt.colors.primary, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun PreviewCard(item: NearbyPlace, state: ExploreState, onOpen: () -> Unit, onToggleSaved: () -> Unit) {
    val place = item.place
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(26.dp),
        color = Hunt.colors.surface,
        shadowElevation = 10.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                KindTile(place.kind, size = 56.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(place.title(), style = MaterialTheme.typography.titleLarge, color = Hunt.colors.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    val subtitle = place.subtitle()
                    if (subtitle.isNotEmpty()) {
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Hunt.colors.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    val status = item.hours?.takeIf { state.sameClock }?.statusAt(state.now)
                    if (status != null) {
                        Spacer(Modifier.height(4.dp))
                        StatusText(status.line())
                    }
                }
                HeartButton(saved = place.id in state.saved, onClick = onToggleSaved)
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(HuntIcons.Walk, contentDescription = null, tint = Hunt.colors.inkMuted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    Distances.format(item.distanceMeters, state.units, LocalLocale.current.platformLocale) + " · " +
                        stringResource(R.string.walk_minutes, com.example.happyhunt.domain.Geo.walkingMinutes(item.distanceMeters)),
                    style = MaterialTheme.typography.labelLarge,
                    color = Hunt.colors.inkSoft,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = onOpen, contentPadding = PaddingValues(start = 18.dp, end = 12.dp)) {
                    Text(stringResource(R.string.explore_details), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(4.dp))
                    Icon(HuntIcons.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun ExploreSheet(
    state: ExploreState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onOpen: (String) -> Unit,
    onToggleSaved: (com.example.happyhunt.domain.Place) -> Unit,
    onRetry: () -> Unit,
    onOpenNow: (Boolean) -> Unit,
    onShowAll: () -> Unit,
    onLookFurther: (Int) -> Unit,
) {
    val locale = LocalLocale.current.platformLocale
    val radius = Distances.format(state.radiusMeters.toDouble(), state.units, locale)
    val load = state.load
    val further = Settings.RADII.firstOrNull { it > state.radiusMeters }
    val furtherAction = further?.let {
        stringResource(R.string.explore_look_further, Distances.format(it.toDouble(), state.units, locale)) to { onLookFurther(it) }
    }
    // When there is nothing to list, the header says why and the actions sit right under it,
    // so they fit in the sheet as it first peeks up.
    val title = when {
        load == Load.Loading -> stringResource(R.string.explore_looking)
        load is Load.Failed -> stringResource(if (load.offline) R.string.explore_offline_title else R.string.explore_busy_title)
        state.places.isEmpty() -> stringResource(R.string.explore_empty_title)
        else -> pluralStringResource(R.plurals.explore_count, state.places.size, state.places.size)
    }
    Column(Modifier.fillMaxWidth().fillMaxHeight(0.92f)) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Hunt.colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(stringResource(R.string.explore_within, radius), state.origin?.label).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = Hunt.colors.inkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (state.sameClock && load == Load.Ready && state.total > 0) {
                OpenNowToggle(selected = state.openNow, onClick = { onOpenNow(!state.openNow) })
            }
        }
        if (state.stale && load == Load.Ready) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .background(Hunt.colors.soonSoft, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(HuntIcons.Offline, contentDescription = null, tint = Hunt.colors.soon, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.explore_stale), style = MaterialTheme.typography.bodySmall, color = Hunt.colors.ink)
            }
        }
        when (load) {
            Load.Loading -> SkeletonRows()
            is Load.Failed -> InlineMessage(
                body = stringResource(if (load.offline) R.string.explore_offline_body else R.string.explore_busy_body),
                primary = stringResource(R.string.try_again) to onRetry,
            )
            Load.Ready -> if (state.places.isEmpty()) {
                when {
                    state.total == 0 -> InlineMessage(
                        body = stringResource(R.string.explore_empty_body, radius),
                        primary = furtherAction,
                    )
                    state.openNow -> InlineMessage(
                        body = stringResource(R.string.explore_empty_open),
                        primary = stringResource(R.string.explore_show_closed) to { onOpenNow(false) },
                    )
                    state.category != null -> InlineMessage(
                        body = stringResource(R.string.explore_empty_category, stringResource(state.category.inSentence()), radius),
                        primary = stringResource(R.string.explore_show_all) to onShowAll,
                        secondary = furtherAction,
                    )
                    else -> InlineMessage(
                        body = stringResource(R.string.explore_empty_body, radius),
                        primary = furtherAction,
                    )
                }
            } else {
                LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(state.places, key = { it.place.id }) { item ->
                        PlaceRow(
                            place = item.place,
                            status = item.hours?.takeIf { state.sameClock }?.statusAt(state.now),
                            distanceMeters = item.distanceMeters,
                            units = state.units,
                            saved = item.place.id in state.saved,
                            onClick = { onOpen(item.place.id) },
                            onToggleSaved = { onToggleSaved(item.place) },
                        )
                    }
                }
            }
        }
    }
}

/** Why the list is empty, with what to do about it, compact enough for the peeking sheet. */
@Composable
private fun InlineMessage(body: String, primary: Pair<String, () -> Unit>?, secondary: Pair<String, () -> Unit>? = null) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Text(body, style = MaterialTheme.typography.bodyMedium, color = Hunt.colors.inkMuted)
        if (primary != null || secondary != null) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                primary?.let { (label, action) -> Button(onClick = action) { Text(label, fontWeight = FontWeight.Bold) } }
                secondary?.let { (label, action) -> OutlinedButton(onClick = action) { Text(label, fontWeight = FontWeight.Bold) } }
            }
        }
    }
}

@Composable
private fun OpenNowToggle(selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) Hunt.colors.open else Hunt.colors.surfaceSunken,
        modifier = Modifier.height(36.dp).semantics { this.selected = selected },
    ) {
        Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(HuntIcons.Clock, contentDescription = null, tint = if (selected) Color.White else Hunt.colors.inkSoft, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.explore_open_now),
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) Color.White else Hunt.colors.inkSoft,
            )
        }
    }
}

@Composable
private fun SkeletonRows() {
    Column(Modifier.padding(top = 8.dp)) {
        repeat(4) { index ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).alpha(1f - index * 0.2f), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).background(Hunt.colors.surfaceSunken, RoundedCornerShape(16.dp)))
                Spacer(Modifier.width(14.dp))
                Column {
                    Box(Modifier.width(160.dp).height(14.dp).background(Hunt.colors.surfaceSunken, CircleShape))
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.width(110.dp).height(10.dp).background(Hunt.colors.surfaceSunken, CircleShape))
                }
            }
        }
    }
}
