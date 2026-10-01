package com.example.happyhunt.data

import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.PreferencesSerializer
import com.example.happyhunt.domain.Area
import com.example.happyhunt.domain.GeoPoint
import com.example.happyhunt.domain.Units
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toPath
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SettingsRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var scope: CoroutineScope
    private lateinit var settings: SettingsRepository

    @Before
    fun setUp() {
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        settings = SettingsRepository(store("settings"), store("places"))
    }

    // Okio storage: DataStore's plain File storage cannot replace its file on Windows, where these tests also run.
    private fun store(name: String) = PreferenceDataStoreFactory.create(
        storage = OkioStorage(
            fileSystem = FileSystem.SYSTEM,
            serializer = PreferencesSerializer,
            producePath = { File(folder.root, "$name.preferences_pb").absolutePath.toPath() },
        ),
        scope = scope,
    )

    @After
    fun tearDown() {
        scope.cancel()
    }

    private fun current() = runBlocking { settings.settings.first() }

    @Test
    fun defaults() {
        val current = current()
        assertEquals(1000, current.radiusMeters)
        assertNull(current.units)
        assertEquals(ThemeMode.SYSTEM, current.theme)
        assertNull(current.origin)
        assertTrue(current.recentAreas.isEmpty())
    }

    @Test
    fun `choices are kept`() = runBlocking {
        settings.setRadius(5000)
        settings.setUnits(Units.IMPERIAL)
        settings.setTheme(ThemeMode.DARK)
        assertEquals(5000, current().radiusMeters)
        assertEquals(Units.IMPERIAL, current().units)
        assertEquals(ThemeMode.DARK, current().theme)
        settings.setUnits(null)
        assertNull(current().units)
    }

    @Test
    fun `a radius that is no longer offered falls back to the default`() = runBlocking {
        settings.setRadius(1234)
        assertEquals(Settings.DEFAULT_RADIUS, current().radiusMeters)
    }

    @Test
    fun `where to search from is kept apart from the choices, which are backed up`() = runBlocking {
        val origin = Origin(GeoPoint(43.6465, -79.4197), "Trinity-Bellwoods, Toronto", mine = true)
        settings.setTheme(ThemeMode.DARK)
        settings.setOrigin(origin)
        assertEquals(origin, current().origin)
        assertTrue(File(folder.root, "places.preferences_pb").readBytes().decodeToString().contains("Trinity-Bellwoods"))
        assertFalse(File(folder.root, "settings.preferences_pb").readBytes().decodeToString().contains("Trinity"))
    }

    @Test
    fun `recent areas, newest first, without repeats, five at most`() = runBlocking {
        val areas = (1..6).map { Area("Area $it", "Toronto", GeoPoint(43.0 + it, -79.0)) }
        areas.forEach { settings.addRecent(it) }
        settings.addRecent(areas[2])
        assertEquals(listOf("Area 3", "Area 6", "Area 5", "Area 4", "Area 2"), current().recentAreas.map { it.name })
        settings.clearRecent()
        assertTrue(current().recentAreas.isEmpty())
    }
}
