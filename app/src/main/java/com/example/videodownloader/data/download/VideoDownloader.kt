package com.example.videodownloader.data.download

import com.example.videodownloader.data.extract.ExtractError
import java.io.IOException
import java.io.OutputStream
import okhttp3.OkHttpClient
import okhttp3.Request

class VideoDownloader(
    private val client: OkHttpClient,
) {
    fun download(
        url: String,
        outputStream: OutputStream,
        onProgress: (DownloadProgress) -> Unit = {},
    ): VideoDownloadResult {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return VideoDownloadResult.Failure(ExtractError.DownloadFailed)
                }

                val body = response.body
                val totalBytes = body.contentLength().takeIf { it >= 0L }
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var bytesWritten = 0L

                body.byteStream().use { input ->
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        outputStream.write(buffer, 0, read)
                        bytesWritten += read
                        onProgress(DownloadProgress(bytesWritten, totalBytes))
                    }
                }
                outputStream.flush()

                VideoDownloadResult.Success(bytesWritten)
            }
        } catch (_: IOException) {
            VideoDownloadResult.Failure(ExtractError.DownloadFailed)
        }
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124 Mobile Safari/537.36"
        private const val DEFAULT_BUFFER_SIZE = 8 * 1024
    }
}
