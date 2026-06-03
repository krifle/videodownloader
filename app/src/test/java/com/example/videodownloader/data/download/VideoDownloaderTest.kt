package com.example.videodownloader.data.download

import com.example.videodownloader.data.extract.ExtractError
import java.io.ByteArrayOutputStream
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VideoDownloaderTest {
    private lateinit var server: MockWebServer
    private lateinit var downloader: VideoDownloader

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        downloader = VideoDownloader(OkHttpClient())
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `downloads video bytes into output stream`() {
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "video/mp4")
                .addHeader("Content-Length", "11")
                .body("video-bytes")
                .build(),
        )
        val output = ByteArrayOutputStream()
        val progressEvents = mutableListOf<DownloadProgress>()

        val result = downloader.download(server.url("/video.mp4").toString(), output) {
            progressEvents += it
        }

        assertEquals(VideoDownloadResult.Success(11L), result)
        assertEquals("video-bytes", output.toString())
        assertTrue(progressEvents.isNotEmpty())
        assertEquals(11L, progressEvents.last().bytesRead)
        assertEquals(11L, progressEvents.last().totalBytes)
        assertEquals(1f, progressEvents.last().fraction)
    }

    @Test
    fun `maps failed responses to download failed`() {
        server.enqueue(MockResponse.Builder().code(500).body("nope").build())

        val result = downloader.download(server.url("/video.mp4").toString(), ByteArrayOutputStream())

        assertEquals(VideoDownloadResult.Failure(ExtractError.DownloadFailed), result)
    }
}
