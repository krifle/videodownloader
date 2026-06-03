package com.example.videodownloader.data.extract.platform

import com.example.videodownloader.data.extract.ExtractError
import com.example.videodownloader.data.extract.ExtractionOutcome
import com.example.videodownloader.domain.SnsPlatform
import com.example.videodownloader.domain.UrlValidationResult
import com.example.videodownloader.domain.UrlValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadsExtractorTest {
    private val extractor = ThreadsExtractor()

    @Test
    fun `extracts threads video from script payload`() {
        val html = """
            <html>
              <head>
                <meta property="og:title" content="Threads video post">
              </head>
              <body>
                <script>
                  require("__payload").handle({"media_url":"https:\/\/cdn.threads.example\/video.mp4?x=1\u0026y=2"});
                </script>
              </body>
            </html>
        """.trimIndent()

        val outcome = extractor.extract(threadsUrl(), html)

        assertTrue(outcome is ExtractionOutcome.Success)
        val result = (outcome as ExtractionOutcome.Success).result
        assertEquals(SnsPlatform.Threads, result.platform)
        assertEquals("https://cdn.threads.example/video.mp4?x=1&y=2", result.videoUrl)
        assertEquals("Threads video post", result.title)
    }

    @Test
    fun `returns login required for threads login pages`() {
        val html = """<html><title>Log in to Threads</title></html>"""

        val outcome = extractor.extract(threadsUrl(), html)

        assertEquals(ExtractionOutcome.Failure(ExtractError.LoginRequired), outcome)
    }

    @Test
    fun `returns media not found when no threads video candidate exists`() {
        val html = """<html><head><meta property="og:title" content="Text post"></head></html>"""

        val outcome = extractor.extract(threadsUrl(), html)

        assertEquals(ExtractionOutcome.Failure(ExtractError.MediaNotFound), outcome)
    }

    private fun threadsUrl() =
        (UrlValidator.validate("https://www.threads.net/@openai/post/ABC123") as UrlValidationResult.Supported).value
}
