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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Everything the screens need, made once for the whole app. */
class AppContainer(context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val http = Http.client(context.cacheDir)
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
}
