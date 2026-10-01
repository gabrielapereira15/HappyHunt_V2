package com.example.happyhunt.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.happyhunt.domain.Area
import com.example.happyhunt.domain.GeoPoint
import com.example.happyhunt.domain.Units
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Where the hunt starts from: the phone's position, or an area picked by name. */
@Serializable
data class Origin(val point: GeoPoint, val label: String?, val mine: Boolean)

data class Settings(
    val radiusMeters: Int = DEFAULT_RADIUS,
    /** Null follows the phone's region. */
    val units: Units? = null,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    /** Null until the welcome screen has done its job, and again on a new phone restored from a backup. */
    val origin: Origin? = null,
    val recentAreas: List<Area> = emptyList(),
) {
    companion object {
        const val DEFAULT_RADIUS = 1000

        /** Tested in downtown Toronto: 5 km is about 4,600 places and still answers in under 20 seconds. */
        val RADII = listOf(500, 1000, 2000, 5000)
    }
}

/**
 * Choices live in [store], which is backed up. Where the person searched from
 * lives apart in [where], which is not: a location stays on the phone.
 */
class SettingsRepository(private val store: DataStore<Preferences>, private val where: DataStore<Preferences>) {
    private val json = Json { ignoreUnknownKeys = true }

    val settings: Flow<Settings> = combine(store.data, where.data) { prefs, places ->
        Settings(
            radiusMeters = prefs[RADIUS]?.takeIf { it in Settings.RADII } ?: Settings.DEFAULT_RADIUS,
            units = prefs[UNITS]?.let { name -> Units.entries.firstOrNull { it.name == name } },
            theme = prefs[THEME]?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } } ?: ThemeMode.SYSTEM,
            origin = places[ORIGIN]?.let { runCatching { json.decodeFromString<Origin>(it) }.getOrNull() },
            recentAreas = places[RECENT]?.let { runCatching { json.decodeFromString<List<Area>>(it) }.getOrNull() }.orEmpty(),
        )
    }

    suspend fun setRadius(meters: Int) {
        store.edit { it[RADIUS] = meters }
    }

    suspend fun setUnits(units: Units?) {
        store.edit { if (units == null) it.remove(UNITS) else it[UNITS] = units.name }
    }

    suspend fun setTheme(theme: ThemeMode) {
        store.edit { it[THEME] = theme.name }
    }

    suspend fun setOrigin(origin: Origin) {
        where.edit { it[ORIGIN] = json.encodeToString(origin) }
    }

    /** Remembers an area picked from search, most recent first, five at most. */
    suspend fun addRecent(area: Area) {
        where.edit { prefs ->
            val current = prefs[RECENT]?.let { runCatching { json.decodeFromString<List<Area>>(it) }.getOrNull() }.orEmpty()
            val updated = (listOf(area) + current.filterNot { it.name == area.name && it.detail == area.detail }).take(5)
            prefs[RECENT] = json.encodeToString(updated)
        }
    }

    suspend fun clearRecent() {
        where.edit { it.remove(RECENT) }
    }

    private companion object {
        val RADIUS = intPreferencesKey("radius_m")
        val UNITS = stringPreferencesKey("units")
        val THEME = stringPreferencesKey("theme")
        val ORIGIN = stringPreferencesKey("origin")
        val RECENT = stringPreferencesKey("recent_areas")
    }
}
