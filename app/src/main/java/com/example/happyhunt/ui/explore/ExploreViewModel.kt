package com.example.happyhunt.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.happyhunt.AppContainer
import com.example.happyhunt.data.Locator
import com.example.happyhunt.data.Origin
import com.example.happyhunt.data.PlacesUnavailable
import com.example.happyhunt.domain.Category
import com.example.happyhunt.domain.Geo
import com.example.happyhunt.domain.GeoPoint
import com.example.happyhunt.domain.Kind
import com.example.happyhunt.domain.OpeningHours
import com.example.happyhunt.domain.Place
import com.example.happyhunt.domain.Units
import com.example.happyhunt.ui.map.CameraGoal
import com.example.happyhunt.ui.map.CameraSpot
import com.example.happyhunt.ui.map.MapStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.maplibre.geojson.FeatureCollection
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

/** A place in the list, with what is worked out once per search: how far it is and its parsed hours. */
data class NearbyPlace(val place: Place, val distanceMeters: Double, val hours: OpeningHours?) {
    /** Open at [now]; parks and the like with no hours listed count as open, as they almost always are. */
    fun isOpenAt(now: LocalDateTime): Boolean = when (val status = hours?.statusAt(now)) {
        null -> place.kind in ALWAYS_OUT
        is OpeningHours.Status.Closed -> false
        else -> true
    }

    private companion object {
        val ALWAYS_OUT = setOf(Kind.PARK, Kind.PLAYGROUND, Kind.GARDEN, Kind.NATURE_RESERVE, Kind.VIEWPOINT)
    }
}

sealed interface Load {
    data object Loading : Load
    data object Ready : Load
    data class Failed(val offline: Boolean) : Load
}

data class ExploreState(
    val origin: Origin? = null,
    val radiusMeters: Int = 1000,
    val units: Units = Units.METRIC,
    val load: Load = Load.Loading,
    val stale: Boolean = false,
    val category: Category? = null,
    val openNow: Boolean = false,
    /** Everything found, before the filters. */
    val total: Int = 0,
    val places: List<NearbyPlace> = emptyList(),
    val pins: FeatureCollection = FeatureCollection.fromFeatures(emptyList()),
    val selected: NearbyPlace? = null,
    val saved: Set<String> = emptySet(),
    val now: LocalDateTime = LocalDateTime.now(),
    /** False when the area is far enough away that the phone's clock would give the wrong "open now". */
    val sameClock: Boolean = true,
    val searchHere: Boolean = false,
    val locating: Boolean = false,
)

sealed interface ExploreEvent {
    data class LocationProblem(val result: Locator.Result) : ExploreEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class ExploreViewModel(private val container: AppContainer) : ViewModel() {
    private val found = MutableStateFlow<List<NearbyPlace>>(emptyList())
    private val load = MutableStateFlow<Load>(Load.Loading)
    private val stale = MutableStateFlow(false)
    private val category = MutableStateFlow<Category?>(null)
    private val openNow = MutableStateFlow(false)
    private val selectedId = MutableStateFlow<String?>(null)
    private val searchHere = MutableStateFlow(false)
    private val locating = MutableStateFlow(false)
    private val retries = MutableStateFlow(0)
    private val events = Channel<ExploreEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    private val _goal = MutableStateFlow<CameraGoal?>(null)
    val goal: StateFlow<CameraGoal?> = _goal
    private var goals = 0

    /** Where the map was left, so coming back to Explore puts it back there. */
    var camera: CameraSpot? = null
        private set

    private val settings = container.settings.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** The clock, ticking once a minute, for "open now". */
    private val minute = flow {
        while (true) {
            val now = LocalDateTime.now()
            emit(now.withSecond(0).withNano(0))
            delay((60 - now.second) * 1000L)
        }
    }

    private data class Filtered(
        val total: Int,
        val places: List<NearbyPlace>,
        val pins: FeatureCollection,
        val now: LocalDateTime,
        val sameClock: Boolean,
    )

    private val filtered = combine(found, category, openNow, minute, settings) { all, category, openNow, now, settings ->
        val sameClock = settings?.origin?.point?.let { point ->
            val offset = ZoneId.systemDefault().rules.getOffset(java.time.Instant.now()).totalSeconds / 60
            Geo.likelySameClock(point, offset)
        } ?: true
        val places = all.filter { item ->
            (category == null || item.place.category == category) && (!openNow || !sameClock || item.isOpenAt(now))
        }
        Filtered(
            total = all.size,
            places = places,
            pins = MapStyle.pins(places.map { MapStyle.Pin(it.place.id, it.place.point, it.place.kind, it.place.name) }),
            now = now,
            sameClock = sameClock,
        )
    }.flowOn(Dispatchers.Default)

    private data class Flags(val load: Load, val stale: Boolean, val searchHere: Boolean, val locating: Boolean)

    private val flags = combine(load, stale, searchHere, locating) { load, stale, here, locating -> Flags(load, stale, here, locating) }

    private data class Choices(val category: Category?, val openNow: Boolean, val selectedId: String?)

    private val choices = combine(category, openNow, selectedId) { category, openNow, selected -> Choices(category, openNow, selected) }

    val state: StateFlow<ExploreState> = combine(
        settings,
        filtered,
        flags,
        choices,
        container.saved.ids,
    ) { settings, filtered, flags, choices, saved ->
        ExploreState(
            origin = settings?.origin,
            radiusMeters = settings?.radiusMeters ?: 1000,
            units = settings?.units ?: Geo.unitsFor(Locale.getDefault()),
            load = flags.load,
            stale = flags.stale,
            category = choices.category,
            openNow = choices.openNow && filtered.sameClock,
            total = filtered.total,
            places = filtered.places,
            pins = filtered.pins,
            selected = choices.selectedId?.let { id -> found.value.firstOrNull { it.place.id == id } },
            saved = saved,
            now = filtered.now,
            sameClock = filtered.sameClock,
            searchHere = flags.searchHere,
            locating = flags.locating,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExploreState())

    init {
        // A new centre or radius means a new search; a newer one cancels an older one still running.
        viewModelScope.launch {
            combine(
                settings.map { it?.origin?.point to it?.radiusMeters }.distinctUntilChanged(),
                retries,
            ) { key, _ -> key }
                .mapLatest { (point, radius) ->
                    if (point != null && radius != null) search(point, radius)
                }
                .collect { }
        }
    }

    private suspend fun search(center: GeoPoint, radius: Int) {
        load.value = Load.Loading
        searchHere.value = false
        movedByHand = false
        selectedId.value = null
        _goal.value = CameraGoal(++goals, center, radiusMeters = radius)
        try {
            val nearby = container.places.around(center, radius)
            found.value = kotlinx.coroutines.withContext(Dispatchers.Default) {
                nearby.places
                    .map { NearbyPlace(it, Geo.distanceMeters(center, it.point), OpeningHours.parse(it.openingHours)) }
                    .sortedBy { it.distanceMeters }
            }
            stale.value = nearby.stale
            load.value = Load.Ready
        } catch (e: PlacesUnavailable) {
            found.value = emptyList()
            load.value = Load.Failed(e.offline)
        }
    }

    fun retry() = retries.update { it + 1 }

    /** The map has moved there; coming back to Explore must not fly there again. */
    fun goalHandled(key: Int) {
        _goal.update { if (it?.key == key) null else it }
    }

    fun setCategory(value: Category?) {
        category.value = value
        selectedId.value = null
    }

    fun setOpenNow(value: Boolean) {
        openNow.value = value
    }

    fun select(id: String?) {
        selectedId.value = id
        val item = id?.let { found.value.firstOrNull { it.place.id == id } } ?: return
        val zoom = maxOf(camera?.zoom ?: 15.0, 15.5)
        _goal.value = CameraGoal(++goals, item.place.point, zoom = zoom)
    }

    /** Only a map the person moved themselves offers to search where it now is. */
    private var movedByHand = false

    fun onUserMovedMap() {
        movedByHand = true
    }

    fun onCameraIdle(spot: CameraSpot) {
        camera = spot
        val origin = settings.value?.origin ?: return
        val radius = settings.value?.radiusMeters ?: return
        if (load.value == Load.Loading || !movedByHand) return
        searchHere.value = Geo.distanceMeters(spot.center, origin.point) > radius * 0.45
    }

    /** Searches around the middle of the map, named after the neighbourhood once that is known. */
    fun searchThisArea() {
        val center = camera?.center ?: return
        viewModelScope.launch { container.origins.usePoint(center, mine = false) }
    }

    fun locate() {
        if (locating.value) return
        viewModelScope.launch {
            locating.value = true
            val current = settings.value
            when (val result = container.locator.locate()) {
                is Locator.Result.Found -> {
                    val here = current?.origin?.let { it.mine && Geo.distanceMeters(it.point, result.point) < 50 } == true
                    if (here) {
                        // Already searching here: just bring the map back.
                        _goal.value = CameraGoal(++goals, result.point, radiusMeters = current?.radiusMeters)
                    } else {
                        container.origins.usePoint(result.point, mine = true)
                    }
                }
                else -> events.send(ExploreEvent.LocationProblem(result))
            }
            locating.value = false
        }
    }

    fun lookFurther(radius: Int) {
        viewModelScope.launch { container.settings.setRadius(radius) }
    }

    fun toggleSaved(place: Place) {
        viewModelScope.launch {
            if (place.id in state.value.saved) container.saved.remove(place.id) else container.saved.save(place)
        }
    }
}
