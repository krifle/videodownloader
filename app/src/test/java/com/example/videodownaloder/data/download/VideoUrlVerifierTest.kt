package com.example.videodownaloder.data.download

import com.example.videodownaloder.data.extract.ExtractError
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VideoUrlVerifierTest {
    private lateinit var server: MockWebServer
    private lateinit var verifier: VideoUrlVerifier

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        verifier = VideoUrlVerifier(OkHttpClient())
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `accepts video content from head responses`() {
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "video/mp4")
                .addHeader("Content-Length", "1234")
                .build(),
        )

        val result = verifier.verify(server.url("/video.mp4").toString())

        assertTrue(result is VideoVerificationResult.Success)
        val verified = (result as VideoVerificationResult.Success).verifiedVideoUrl
        assertEquals("video/mp4", verified.contentType)
        assertEquals(1234L, verified.contentLength)
        assertEquals("HEAD", server.takeRequest().method)
    }

    @Test
    fun `falls back to ranged get when head is method not allowed`() {
        server.enqueue(MockResponse.Builder().code(405).build())
        server.enqueue(
            MockResponse.Builder()
                .code(206)
                .addHeader("Content-Type", "video/mp4")
                .addHeader("Content-Length", "1")
                .body("x")
                .build(),
        )

        val result = verifier.verify(server.url("/video.mp4").toString())

        assertTrue(result is VideoVerificationResult.Success)
        assertEquals("HEAD", server.takeRequest().method)
        val fallbackRequest = server.takeRequest()
        assertEquals("GET", fallbackRequest.method)
        assertEquals("bytes=0-0", fallbackRequest.headers["Range"])
    }

    @Test
    fun `rejects non video content`() {
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "text/html")
                .body("<html></html>")
                .build(),
        )

        val result = verifier.verify(server.url("/page").toString())

        assertEquals(VideoVerificationResult.Failure(ExtractError.MediaNotFound), result)
    }

    @Test
    fun `maps forbidden urls to expired video urls`() {
        server.enqueue(MockResponse.Builder().code(403).build())

        val result = verifier.verify(server.url("/expired.mp4").toString())

        assertEquals(VideoVerificationResult.Failure(ExtractError.VideoUrlExpired), result)
    }
}
