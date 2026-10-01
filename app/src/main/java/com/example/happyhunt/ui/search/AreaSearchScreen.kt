package com.example.happyhunt.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.happyhunt.AppContainer
import com.example.happyhunt.R
import com.example.happyhunt.data.Locator
import com.example.happyhunt.domain.Area
import com.example.happyhunt.ui.components.rememberLocationRequest
import com.example.happyhunt.ui.theme.Hunt
import com.example.happyhunt.ui.theme.HuntIcons
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AreaSearchState(
    val query: String = "",
    val searching: Boolean = false,
    /** Null until a search has finished; empty when it found nothing. */
    val results: List<Area>? = null,
    val failed: Boolean = false,
    val recent: List<Area> = emptyList(),
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class AreaSearchViewModel(private val container: AppContainer) : ViewModel() {
    private val query = MutableStateFlow("")

    private data class Outcome(val query: String, val results: List<Area>?, val searching: Boolean, val failed: Boolean)

    private val outcome = query
        .map { it.trim() }
        .distinctUntilChanged()
        .debounce { if (it.length < 2) 0L else 350L }
        .mapLatest { text ->
            if (text.length < 2) {
                Outcome(text, null, searching = false, failed = false)
            } else {
                val near = container.settings.settings.first().origin?.point
                val found = container.areas.search(text, near)
                Outcome(text, found.orEmpty(), searching = false, failed = found == null)
            }
        }
        .onStart { emit(Outcome("", null, searching = false, failed = false)) }

    val state: StateFlow<AreaSearchState> = combine(query, outcome, container.settings.settings) { typed, outcome, settings ->
        val pending = typed.trim().length >= 2 && typed.trim() != outcome.query
        AreaSearchState(
            query = typed,
            searching = pending,
            results = outcome.results.takeIf { !pending },
            failed = outcome.failed && !pending,
            recent = settings.recentAreas,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AreaSearchState())

    fun type(text: String) {
        query.value = text
    }

    fun pick(area: Area, then: () -> Unit) {
        viewModelScope.launch {
            container.origins.useArea(area)
            then()
        }
    }

    fun clearRecent() {
        viewModelScope.launch { container.settings.clearRecent() }
    }
}

@Composable
fun AreaSearchScreen(
    viewModel: AreaSearchViewModel,
    container: AppContainer,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    var locating by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<Int?>(null) }

    val useLocation = rememberLocationRequest(
        onGranted = {
            locating = true
            problem = null
            scope.launch {
                val result = container.origins.useMyLocation()
                locating = false
                when (result) {
                    is Locator.Result.Found -> onDone()
                    Locator.Result.LocationOff -> problem = R.string.location_off
                    Locator.Result.NoPermission -> problem = R.string.location_denied
                    Locator.Result.NotFound -> problem = R.string.location_not_found
                }
            }
        },
        onDenied = { problem = R.string.location_denied },
    )

    LaunchedEffect(Unit) {
        delay(150)
        focus.requestFocus()
    }

    Column(Modifier.fillMaxSize().background(Hunt.colors.background).statusBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(HuntIcons.Back, contentDescription = stringResource(R.string.back), tint = Hunt.colors.ink)
            }
            Text(stringResource(R.string.search_title), style = MaterialTheme.typography.headlineSmall, color = Hunt.colors.ink)
        }
        TextField(
            value = state.query,
            onValueChange = viewModel::type,
            placeholder = { Text(stringResource(R.string.search_hint), color = Hunt.colors.inkMuted) },
            leadingIcon = { Icon(HuntIcons.Search, contentDescription = null, tint = Hunt.colors.primary) },
            trailingIcon = {
                when {
                    state.searching -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    state.query.isNotEmpty() -> IconButton(onClick = { viewModel.type("") }) {
                        Icon(HuntIcons.Close, contentDescription = stringResource(R.string.search_clear), tint = Hunt.colors.inkMuted)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Search,
            ),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Hunt.colors.surface,
                unfocusedContainerColor = Hunt.colors.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Hunt.colors.primary,
            ),
            textStyle = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .focusRequester(focus),
        )

        LazyColumn(Modifier.fillMaxSize()) {
            item {
                AreaRow(
                    icon = HuntIcons.Locate,
                    title = stringResource(R.string.search_use_location),
                    detail = problem?.let { stringResource(it) } ?: stringResource(R.string.search_use_location_hint),
                    highlight = true,
                    busy = locating,
                    onClick = { if (!locating) useLocation() },
                )
            }
            val results = state.results
            when {
                results == null && state.recent.isNotEmpty() && state.query.isBlank() -> {
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(R.string.search_recent),
                                style = MaterialTheme.typography.titleSmall,
                                color = Hunt.colors.inkMuted,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = viewModel::clearRecent) { Text(stringResource(R.string.search_clear_recent)) }
                        }
                    }
                    items(state.recent, key = { "recent-${it.name}-${it.detail}" }) { area ->
                        AreaRow(HuntIcons.History, area.name, area.detail, onClick = { viewModel.pick(area, onDone) })
                    }
                }
                results != null && results.isEmpty() -> item {
                    Message(
                        if (state.failed) stringResource(R.string.search_failed)
                        else stringResource(R.string.search_no_results, state.query.trim()),
                    )
                }
                results != null -> items(results, key = { "result-${it.name}-${it.detail}-${it.point}" }) { area ->
                    AreaRow(HuntIcons.Pin, area.name, area.detail, onClick = {
                        keyboard?.hide()
                        viewModel.pick(area, onDone)
                    })
                }
            }
        }
    }
}

@Composable
private fun AreaRow(
    icon: ImageVector,
    title: String,
    detail: String?,
    highlight: Boolean = false,
    busy: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .background(if (highlight) Hunt.colors.primarySoft else Hunt.colors.surfaceSunken, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (busy) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(icon, contentDescription = null, tint = if (highlight) Hunt.colors.primary else Hunt.colors.inkSoft, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = if (highlight) Hunt.colors.primary else Hunt.colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.bodySmall, color = Hunt.colors.inkMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun Message(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = Hunt.colors.inkMuted,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
    )
}
