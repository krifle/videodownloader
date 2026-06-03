package com.example.videodownloader.domain

import com.example.videodownloader.data.download.DownloadProgress
import com.example.videodownloader.data.download.VideoDownloader
import com.example.videodownloader.data.download.VideoUrlVerifier
import com.example.videodownloader.data.extract.AppExtractor
import com.example.videodownloader.data.extract.ExtractError
import com.example.videodownloader.data.extract.PageFetcher
import com.example.videodownloader.data.media.MediaSaveResult
import com.example.videodownloader.data.media.MediaSaver
import com.example.videodownloader.data.media.SaveVideoRequest
import com.example.videodownloader.data.media.WriteResult
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DownloadRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var mediaSaver: FakeMediaSaver
    private lateinit var repository: DownloadRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val client = OkHttpClient()
        mediaSaver = FakeMediaSaver()
        repository = DownloadRepository(
            pageFetcher = PageFetcher(client),
            appExtractor = AppExtractor(),
            videoUrlVerifier = VideoUrlVerifier(client),
            videoDownloader = VideoDownloader(client),
            mediaSaver = mediaSaver,
            clock = Clock.fixed(Instant.parse("2026-06-03T03:10:00Z"), ZoneId.of("Asia/Seoul")),
        )
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `downloads extracted video and saves it through media saver`() {
        val videoUrl = server.url("/video.mp4").toString()
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .body("""<meta property="og:video" content="$videoUrl">""")
                .build(),
        )
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "video/mp4")
                .addHeader("Content-Length", "11")
                .build(),
        )
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "video/mp4")
                .addHeader("Content-Length", "11")
                .body("video-bytes")
                .build(),
        )
        val progress = mutableListOf<DownloadProgress>()

        val result = repository.download(instagramUrl(server.url("/reel/ABC123/").toString())) {
            progress += it
        }

        assertEquals(DownloadRepositoryResult.Success("instagram_reel_20260603_121000.mp4", 11L), result)
        assertEquals("video-bytes", mediaSaver.savedBytes.toString())
        assertEquals("instagram_reel_20260603_121000.mp4", mediaSaver.lastRequest?.fileName)
        assertTrue(progress.isNotEmpty())
        assertEquals("GET", server.takeRequest().method)
        assertEquals("HEAD", server.takeRequest().method)
        assertEquals("GET", server.takeRequest().method)
    }

    @Test
    fun `stops when page fetch fails`() {
        server.enqueue(MockResponse.Builder().code(403).body("blocked").build())

        val result = repository.download(instagramUrl(server.url("/reel/ABC123/").toString()))

        assertEquals(DownloadRepositoryResult.Failure(ExtractError.AccessBlocked), result)
        assertEquals(0, mediaSaver.saveCount)
    }

    @Test
    fun `returns save failed when media saver fails`() {
        mediaSaver.resultOverride = MediaSaveResult.Failure(ExtractError.SaveFailed)
        val videoUrl = server.url("/video.mp4").toString()
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .body("""<meta property="og:video" content="$videoUrl">""")
                .build(),
        )
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "video/mp4")
                .build(),
        )

        val result = repository.download(instagramUrl(server.url("/reel/ABC123/").toString()))

        assertEquals(DownloadRepositoryResult.Failure(ExtractError.SaveFailed), result)
    }

    private fun instagramUrl(url: String): SupportedUrl {
        val parsed = UrlValidator.validate(url.replace(server.hostName, "www.instagram.com"))
            as UrlValidationResult.Supported
        return parsed.value.copy(normalizedUrl = url)
    }

    private class FakeMediaSaver : MediaSaver {
        var lastRequest: SaveVideoRequest? = null
        var saveCount = 0
        var resultOverride: MediaSaveResult? = null
        val savedBytes = ByteArrayOutputStream()

        override fun saveVideo(
            request: SaveVideoRequest,
            writer: (OutputStream) -> WriteResult,
        ): MediaSaveResult {
            saveCount += 1
            lastRequest = request
            resultOverride?.let { return it }

            return when (val writeResult = writer(savedBytes)) {
                is WriteResult.Success -> MediaSaveResult.Success(request.fileName, writeResult.bytesWritten)
                is WriteResult.Failure -> MediaSaveResult.Failure(writeResult.error)
            }
        }
    }
}
