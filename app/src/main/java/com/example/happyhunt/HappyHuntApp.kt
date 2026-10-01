package com.example.happyhunt

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Dispatcher
import org.maplibre.android.MapLibre
import org.maplibre.android.module.http.HttpRequestUtil

class HappyHuntApp : Application(), SingletonImageLoader.Factory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        MapLibre.getInstance(this)
        // Map tiles have their own cache in MapLibre; they only borrow the client to say who is asking.
        // A map screen asks for a dozen tiles at once, so allow as many parallel requests as MapLibre's own client.
        val tiles = Dispatcher().apply { maxRequestsPerHost = 20 }
        HttpRequestUtil.setOkHttpClient(container.http.newBuilder().cache(null).dispatcher(tiles).build())
        container.scope.launch(Dispatchers.IO) {
            // Version 1 kept accounts, with passwords in plain text, in this database. There are no accounts any more.
            deleteDatabase("HappyHunt.dp")
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context)
        .components { add(OkHttpNetworkFetcherFactory(callFactory = { container.http })) }
        .crossfade(true)
        .build()
}
