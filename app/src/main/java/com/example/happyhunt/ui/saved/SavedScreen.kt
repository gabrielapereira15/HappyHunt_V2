package com.example.happyhunt.ui.saved

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.happyhunt.AppContainer
import com.example.happyhunt.R
import com.example.happyhunt.data.SavedPlace
import com.example.happyhunt.domain.Category
import com.example.happyhunt.domain.Geo
import com.example.happyhunt.domain.OpeningHours
import com.example.happyhunt.domain.Units
import com.example.happyhunt.ui.components.MessageState
import com.example.happyhunt.ui.components.PlaceRow
import com.example.happyhunt.ui.label
import com.example.happyhunt.ui.theme.Hunt
import com.example.happyhunt.ui.theme.HuntIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

data class SavedItem(val saved: SavedPlace, val hours: OpeningHours?, val distanceMeters: Double?, val sameClock: Boolean)

data class SavedState(
    val loaded: Boolean = false,
    val items: List<SavedItem> = emptyList(),
    val categories: List<Category> = emptyList(),
    val category: Category? = null,
    val units: Units = Units.METRIC,
    val now: LocalDateTime = LocalDateTime.now(),
)

class SavedViewModel(private val container: AppContainer) : ViewModel() {
    private val category = MutableStateFlow<Category?>(null)

    private val minute = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(60_000L - System.currentTimeMillis() % 60_000L)
        }
    }

    val state: StateFlow<SavedState> = combine(container.saved.places, container.settings.settings, category, minute) { saved, settings, category, now ->
        val origin = settings.origin?.point
        val offset = ZoneId.systemDefault().rules.getOffset(Instant.now()).totalSeconds / 60
        val all = saved.map { entry ->
            SavedItem(
                saved = entry,
                hours = OpeningHours.parse(entry.place.openingHours),
                distanceMeters = origin?.let { Geo.distanceMeters(it, entry.place.point) },
                sameClock = Geo.likelySameClock(entry.place.point, offset),
            )
        }
        val categories = all.map { it.saved.place.category }.distinct().sortedBy { it.ordinal }
        val chosen = category?.takeIf { it in categories }
        SavedState(
            loaded = true,
            items = if (chosen == null) all else all.filter { it.saved.place.category == chosen },
            categories = categories,
            category = chosen,
            units = settings.units ?: Geo.unitsFor(Locale.getDefault()),
            now = now,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SavedState())

    fun setCategory(value: Category?) {
        category.value = value
    }

    fun remove(item: SavedPlace) {
        viewModelScope.launch { container.saved.remove(item.place.id) }
    }

    /** Puts a removed place back exactly where it was in the list. */
    fun restore(item: SavedPlace) {
        viewModelScope.launch { container.saved.save(item.place, savedAt = item.savedAt) }
    }
}

@Composable
fun SavedScreen(viewModel: SavedViewModel, onOpenPlace: (String) -> Unit, onExplore: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current

    fun remove(item: SavedPlace, title: String) {
        viewModel.remove(item)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(
                message = resources.getString(R.string.saved_removed, title),
                actionLabel = resources.getString(R.string.undo),
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.restore(item)
        }
    }

    Box(Modifier.fillMaxSize().background(Hunt.colors.background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp)) {
                Text(stringResource(R.string.saved_title), style = MaterialTheme.typography.headlineLarge, color = Hunt.colors.ink)
                if (state.categories.isNotEmpty()) {
                    val total = state.items.size
                    Text(
                        pluralStringResource(R.plurals.saved_count, total, total),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Hunt.colors.inkMuted,
                    )
                }
            }
            if (state.categories.size > 1) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item { FilterPill(stringResource(R.string.category_all), Hunt.colors.primary, state.category == null) { viewModel.setCategory(null) } }
                    items(state.categories) { category ->
                        FilterPill(stringResource(category.label()), Hunt.colors.category(category).pin, state.category == category) {
                            viewModel.setCategory(if (state.category == category) null else category)
                        }
                    }
                }
            }
            when {
                !state.loaded -> Unit
                state.items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    MessageState(
                        icon = HuntIcons.Heart,
                        title = stringResource(R.string.saved_empty_title),
                        body = stringResource(R.string.saved_empty_body),
                        tint = Hunt.colors.heart,
                        tileColor = Hunt.colors.closedSoft,
                        primaryAction = stringResource(R.string.saved_empty_action) to onExplore,
                        modifier = Modifier.padding(bottom = 48.dp),
                    )
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.items, key = { it.saved.place.id }) { item ->
                        val title = item.saved.place.name ?: stringResource(item.saved.place.kind.label())
                        SavedCard(
                            item = item,
                            state = state,
                            onOpen = { onOpenPlace(item.saved.place.id) },
                            onRemove = { remove(item.saved, title) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun SavedCard(item: SavedItem, state: SavedState, onOpen: () -> Unit, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    val dismiss = rememberSwipeToDismissBoxState()
    // The list keeps each card's swipe state by place, so it is put back at once: otherwise a place
    // brought back with Undo would come back swiped away, and be removed again.
    LaunchedEffect(dismiss.currentValue) {
        if (dismiss.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onRemove()
            dismiss.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }
    SwipeToDismissBox(
        state = dismiss,
        enableDismissFromStartToEnd = false,
        modifier = modifier,
        backgroundContent = {
            val color by animateColorAsState(
                if (dismiss.targetValue == SwipeToDismissBoxValue.EndToStart) Hunt.colors.heart else Hunt.colors.closedSoft,
                label = "swipe",
            )
            Box(
                Modifier.fillMaxSize().background(color, RoundedCornerShape(24.dp)).padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(HuntIcons.Trash, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        },
    ) {
        Surface(shape = RoundedCornerShape(24.dp), color = Hunt.colors.surface, shadowElevation = 1.dp) {
            PlaceRow(
                place = item.saved.place,
                status = item.hours?.takeIf { item.sameClock }?.statusAt(state.now),
                distanceMeters = item.distanceMeters,
                units = state.units,
                saved = true,
                onClick = onOpen,
                onToggleSaved = onRemove,
                modifier = Modifier.padding(vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun FilterPill(label: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) color else Hunt.colors.surface,
        border = if (selected) null else BorderStroke(1.dp, Hunt.colors.line),
        modifier = Modifier.height(36.dp).semantics { this.selected = selected },
    ) {
        Box(Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) Color.White else Hunt.colors.inkSoft)
        }
    }
}
