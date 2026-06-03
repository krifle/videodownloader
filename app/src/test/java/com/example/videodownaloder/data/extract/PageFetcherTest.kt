package com.example.videodownaloder.data.extract

import java.util.concurrent.TimeUnit
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PageFetcherTest {
    private lateinit var server: MockWebServer
    private lateinit var fetcher: PageFetcher

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        fetcher = PageFetcher(
            OkHttpClient.Builder()
                .callTimeout(5, TimeUnit.SECONDS)
                .build(),
        )
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `fetches successful html pages`() {
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .body("<html>ok</html>")
                .build(),
        )

        val result = fetcher.fetch(server.url("/post").toString())

        assertEquals(PageFetchResult.Success("<html>ok</html>"), result)
        val request = server.takeRequest()
        assertTrue(request.headers["User-Agent"]?.contains("Android") == true)
    }

    @Test
    fun `maps forbidden pages to access blocked`() {
        server.enqueue(MockResponse.Builder().code(403).body("blocked").build())

        val result = fetcher.fetch(server.url("/blocked").toString())

        assertEquals(PageFetchResult.Failure(ExtractError.AccessBlocked), result)
    }

    @Test
    fun `maps empty pages to media not found`() {
        server.enqueue(MockResponse.Builder().code(200).body("   ").build())

        val result = fetcher.fetch(server.url("/empty").toString())

        assertEquals(PageFetchResult.Failure(ExtractError.MediaNotFound), result)
    }
}
