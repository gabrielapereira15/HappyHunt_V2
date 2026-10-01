package com.example.happyhunt.data

import com.example.happyhunt.domain.Area
import com.example.happyhunt.domain.GeoPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Sets where the hunt starts from, the same way from every screen: the
 * phone's position, a searched area, or the middle of the map. A name for a
 * position is looked up afterwards, without making anyone wait for it.
 */
class Origins(
    private val locator: Locator,
    private val areas: AreasRepository,
    private val settings: SettingsRepository,
    private val scope: CoroutineScope,
) {
    suspend fun useMyLocation(): Locator.Result {
        val result = locator.locate()
        if (result is Locator.Result.Found) usePoint(result.point, mine = true)
        return result
    }

    suspend fun useArea(area: Area) {
        settings.addRecent(area)
        settings.setOrigin(Origin(area.point, area.name, mine = false))
    }

    suspend fun usePoint(point: GeoPoint, mine: Boolean) {
        val origin = Origin(point, label = null, mine = mine)
        settings.setOrigin(origin)
        scope.launch {
            val label = areas.label(point) ?: return@launch
            // Only if nothing else was picked in the meantime.
            if (settings.settings.first().origin == origin) settings.setOrigin(origin.copy(label = label))
        }
    }
}
