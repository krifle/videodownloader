package com.example.videodownloader.data.extract

import com.example.videodownloader.domain.SnsPlatform
import com.example.videodownloader.domain.UrlValidationResult
import com.example.videodownloader.domain.UrlValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppExtractorTest {
    @Test
    fun `delegates to platform extractor`() {
        val supportedUrl =
            (UrlValidator.validate("https://www.instagram.com/reel/ABC123/") as UrlValidationResult.Supported).value
        val html = """<meta property="og:video" content="https://cdn.example.com/reel.mp4">"""

        val outcome = AppExtractor().extract(supportedUrl, html)

        assertTrue(outcome is ExtractionOutcome.Success)
        val result = (outcome as ExtractionOutcome.Success).result
        assertEquals(SnsPlatform.Instagram, result.platform)
        assertEquals("https://cdn.example.com/reel.mp4", result.videoUrl)
    }
}
