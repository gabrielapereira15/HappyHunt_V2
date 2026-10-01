package com.example.happyhunt.data

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Response
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

    /**
     * One client for the whole app. It has no disk cache on purpose: area
     * lookups carry coordinates, and the answers worth keeping (searches,
     * photos) are kept by the code that uses them.
     */
    fun client(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("User-Agent", USER_AGENT).build())
        }
        .build()
}

/**
 * Runs the call and reads the whole body, or null for an error status, without
 * blocking the caller's thread. The body is read on OkHttp's thread before the
 * coroutine resumes, so cancelling the coroutine cancels the call at any point,
 * including halfway through a long download.
 */
suspend fun Call.awaitBody(): String? = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            val body = try {
                response.use { if (it.isSuccessful) it.body.string() else null }
            } catch (e: IOException) {
                continuation.resumeWithException(e)
                return
            }
            continuation.resume(body)
        }

        override fun onFailure(call: Call, e: IOException) = continuation.resumeWithException(e)
    })
}

/** A failure that says there is no connection at all, as opposed to a server that is busy. */
val IOException.isOffline: Boolean get() = this is UnknownHostException || this is ConnectException
