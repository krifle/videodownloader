package com.example.videodownloader.data.download

import com.example.videodownloader.data.extract.ExtractError
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

class VideoUrlVerifier(
    private val client: OkHttpClient,
) {
    fun verify(url: String): VideoVerificationResult {
        val headRequest = Request.Builder()
            .url(url)
            .head()
            .build()

        return try {
            client.newCall(headRequest).execute().use { response ->
                if (response.code == 405) {
                    return execute(
                        Request.Builder()
                            .url(url)
                            .header("Range", "bytes=0-0")
                            .get()
                            .build(),
                    )
                }

                response.toVerificationResult(headRequest.url.toString())
            }
        } catch (_: IOException) {
            VideoVerificationResult.Failure(ExtractError.NetworkFailed)
        }
    }

    private fun execute(request: Request): VideoVerificationResult {
        return try {
            client.newCall(request).execute().use { response ->
                response.toVerificationResult(request.url.toString())
            }
        } catch (_: IOException) {
            VideoVerificationResult.Failure(ExtractError.NetworkFailed)
        }
    }

    private fun Response.toVerificationResult(url: String): VideoVerificationResult {
        return when {
            code == 401 || code == 403 || code == 404 -> {
                VideoVerificationResult.Failure(ExtractError.VideoUrlExpired)
            }
            !isSuccessful && code != 206 -> {
                VideoVerificationResult.Failure(ExtractError.MediaNotFound)
            }
            else -> {
                val contentType = header("Content-Type")
                val contentLength = header("Content-Length")?.toLongOrNull()
                if (contentType.isVideoLike()) {
                    VideoVerificationResult.Success(
                        VerifiedVideoUrl(
                            url = url,
                            contentType = contentType,
                            contentLength = contentLength,
                        ),
                    )
                } else {
                    VideoVerificationResult.Failure(ExtractError.MediaNotFound)
                }
            }
        }
    }

    private fun String?.isVideoLike(): Boolean {
        if (this == null) return false
        val lower = lowercase()
        return lower.startsWith("video/") || lower == "application/octet-stream"
    }
}

data class VerifiedVideoUrl(
    val url: String,
    val contentType: String?,
    val contentLength: Long?,
)

sealed interface VideoVerificationResult {
    data class Success(val verifiedVideoUrl: VerifiedVideoUrl) : VideoVerificationResult
    data class Failure(val error: ExtractError) : VideoVerificationResult
}
