package com.example.videodownaloder.data.extract.platform

import com.example.videodownaloder.data.extract.ExtractError
import com.example.videodownaloder.data.extract.ExtractionOutcome
import com.example.videodownaloder.domain.SnsPlatform
import com.example.videodownaloder.domain.UrlValidationResult
import com.example.videodownaloder.domain.UrlValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InstagramExtractorTest {
    private val extractor = InstagramExtractor()

    @Test
    fun `extracts instagram video from open graph metadata`() {
        val html = """
            <html>
              <head>
                <meta property="og:title" content="Instagram reel by creator">
                <meta property="og:image" content="https://cdn.example.com/reel.jpg">
                <meta property="og:video" content="https://cdn.example.com/reel.mp4?token=abc&amp;expires=1">
              </head>
            </html>
        """.trimIndent()

        val outcome = extractor.extract(instagramUrl(), html)

        assertTrue(outcome is ExtractionOutcome.Success)
        val result = (outcome as ExtractionOutcome.Success).result
        assertEquals(SnsPlatform.Instagram, result.platform)
        assertEquals("https://cdn.example.com/reel.mp4?token=abc&expires=1", result.videoUrl)
        assertEquals("Instagram reel by creator", result.title)
        assertEquals("https://cdn.example.com/reel.jpg", result.thumbnailUrl)
    }

    @Test
    fun `returns login required for instagram login pages`() {
        val html = """<html><title>Log in to Instagram</title><a href="/accounts/login/">login</a></html>"""

        val outcome = extractor.extract(instagramUrl(), html)

        assertEquals(ExtractionOutcome.Failure(ExtractError.LoginRequired), outcome)
    }

    @Test
    fun `returns media not found when no video candidate exists`() {
        val html = """<html><head><meta property="og:title" content="Photo post"></head></html>"""

        val outcome = extractor.extract(instagramUrl(), html)

        assertEquals(ExtractionOutcome.Failure(ExtractError.MediaNotFound), outcome)
    }

    private fun instagramUrl() =
        (UrlValidator.validate("https://www.instagram.com/reel/ABC123/") as UrlValidationResult.Supported).value
}
