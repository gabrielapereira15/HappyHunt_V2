package com.example.happyhunt.ui.place

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.happyhunt.AppContainer
import com.example.happyhunt.data.Photo
import com.example.happyhunt.data.PlacesUnavailable
import com.example.happyhunt.domain.Geo
import com.example.happyhunt.domain.OpeningHours
import com.example.happyhunt.domain.Place
import com.example.happyhunt.domain.Units
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

data class PlaceState(
    val loading: Boolean = true,
    val failed: Boolean = false,
    val place: Place? = null,
    val hours: OpeningHours? = null,
    val photo: Photo? = null,
    val saved: Boolean = false,
    val distanceMeters: Double? = null,
    val units: Units = Units.METRIC,
    val sameClock: Boolean = true,
    val now: LocalDateTime = LocalDateTime.now(),
)

class PlaceViewModel(private val container: AppContainer, private val id: String) : ViewModel() {
    private data class Loaded(val loading: Boolean, val failed: Boolean, val place: Place?, val photo: Photo?)

    private val loaded = MutableStateFlow(Loaded(loading = true, failed = false, place = null, photo = null))

    private val minute = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(60_000L - System.currentTimeMillis() % 60_000L)
        }
    }

    val state: StateFlow<PlaceState> = combine(
        loaded,
        container.saved.ids.map { id in it },
        container.settings.settings,
        minute,
    ) { loaded, saved, settings, now ->
        val place = loaded.place
        val offset = ZoneId.systemDefault().rules.getOffset(Instant.now()).totalSeconds / 60
        PlaceState(
            loading = loaded.loading,
            failed = loaded.failed,
            place = place,
            hours = place?.let { OpeningHours.parse(it.openingHours) },
            photo = loaded.photo,
            saved = saved,
            distanceMeters = place?.let { p -> settings.origin?.point?.let { Geo.distanceMeters(it, p.point) } },
            units = settings.units ?: Geo.unitsFor(Locale.getDefault()),
            sameClock = place?.let { Geo.likelySameClock(it.point, offset) } ?: true,
            now = now,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaceState())

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            loaded.value = loaded.value.copy(loading = true, failed = false)
            // From this session's search first, then the saved copy, then the map servers.
            val fresh = container.places.known(id)
            val place = fresh
                ?: container.saved.find(id)
                ?: try {
                    container.places.place(id)
                } catch (_: PlacesUnavailable) {
                    null
                }
            loaded.value = Loaded(loading = false, failed = place == null, place = place, photo = null)
            if (place == null) return@launch
            // A saved place keeps the newest details seen.
            if (fresh != null && container.saved.ids.first().contains(id)) container.saved.save(fresh)
            place.wikidata?.let { item ->
                container.photos.photo(item)?.let { photo -> loaded.value = loaded.value.copy(photo = photo) }
            }
        }
    }

    fun toggleSaved() {
        val place = loaded.value.place ?: return
        viewModelScope.launch {
            if (state.value.saved) container.saved.remove(place.id) else container.saved.save(place)
        }
    }
}
