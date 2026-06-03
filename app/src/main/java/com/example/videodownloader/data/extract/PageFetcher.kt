package com.example.videodownloader.data.extract

import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request

class PageFetcher(
    private val client: OkHttpClient = defaultClient(),
) {
    fun fetch(url: String): PageFetchResult {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                when {
                    response.code == 401 || response.code == 403 -> {
                        PageFetchResult.Failure(ExtractError.AccessBlocked)
                    }
                    response.code in 300..399 -> {
                        PageFetchResult.Failure(ExtractError.AccessBlocked)
                    }
                    !response.isSuccessful -> {
                        PageFetchResult.Failure(ExtractError.NetworkFailed)
                    }
                    else -> {
                        val body = response.body.string()
                        if (body.isBlank()) {
                            PageFetchResult.Failure(ExtractError.MediaNotFound)
                        } else {
                            PageFetchResult.Success(body)
                        }
                    }
                }
            }
        } catch (_: IOException) {
            PageFetchResult.Failure(ExtractError.NetworkFailed)
        }
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124 Mobile Safari/537.36"

        fun defaultClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .callTimeout(45, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
        }
    }
}

sealed interface PageFetchResult {
    data class Success(val html: String) : PageFetchResult
    data class Failure(val error: ExtractError) : PageFetchResult
}
