package com.example.happyhunt

import android.content.Context
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.happyhunt.data.AreasRepository
import com.example.happyhunt.data.Http
import com.example.happyhunt.data.Locator
import com.example.happyhunt.data.Origins
import com.example.happyhunt.data.PhotosRepository
import com.example.happyhunt.data.PlacesRepository
import com.example.happyhunt.data.SavedDatabase
import com.example.happyhunt.data.SavedPlacesRepository
import com.example.happyhunt.data.SettingsRepository
import coil3.SingletonImageLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext

/** Everything the screens need, made once for the whole app. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val http = Http.client()
    val places = PlacesRepository(http, context.cacheDir)
    val areas = AreasRepository(http)
    val photos = PhotosRepository(http)
    val saved = SavedPlacesRepository(SavedDatabase.open(context).places())
    val settings = SettingsRepository(
        store = PreferenceDataStoreFactory.create(scope = scope) { context.preferencesDataStoreFile("settings") },
        where = PreferenceDataStoreFactory.create(scope = scope) { context.preferencesDataStoreFile("places") },
    )
    val locator = Locator(context)
    val origins = Origins(locator, areas, settings, scope)

    /**
     * "Clear recent searches", from Settings or the search screen: the list of
     * recent areas, the earlier results kept for them, the area names looked up
     * and the photos of places found. The area being searched now stays, so the
     * app opens where it was left. It finishes even if the screen is left.
     */
    suspend fun forgetRecentSearches() = withContext(NonCancellable + Dispatchers.IO) {
        settings.clearRecent()
        places.clear()
        areas.forget()
        SingletonImageLoader.get(appContext).run {
            memoryCache?.clear()
            diskCache?.clear()
        }
    }
}
