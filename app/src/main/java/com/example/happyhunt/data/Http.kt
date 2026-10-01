package com.example.happyhunt.data

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Cache
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object Http {
    /**
     * The open map services ask every app to say who it is, so they can reach
     * the developer instead of blocking everyone when something misbehaves.
     */
    const val USER_AGENT = "HappyHunt/2.0 (Android; +https://github.com/gabrielapereira15/HappyHunt_V2)"

    /** One client for the whole app; its small disk cache holds the answers from Wikidata. */
    fun client(cacheDir: File): OkHttpClient = OkHttpClient.Builder()
        .cache(Cache(File(cacheDir, "http"), 16L * 1024 * 1024))
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("User-Agent", USER_AGENT).build())
        }
        .build()
}

/** Runs the call without blocking a thread, and cancels it if the coroutine is cancelled. */
suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) =
            continuation.resume(response) { _, unused, _ -> unused.close() }
        override fun onFailure(call: Call, e: IOException) = continuation.resumeWithException(e)
    })
}

/** A failure that says there is no connection at all, as opposed to a server that is busy. */
val IOException.isOffline: Boolean get() = this is UnknownHostException || this is ConnectException
