package com.example.videodownloader.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlValidatorTest {
    @Test
    fun `supports instagram reel urls`() {
        val result = UrlValidator.validate("https://www.instagram.com/reel/ABC123/?igsh=abc")

        assertTrue(result is UrlValidationResult.Supported)
        val supported = (result as UrlValidationResult.Supported).value
        assertEquals(SnsPlatform.Instagram, supported.platform)
        assertEquals(ContentType.Reel, supported.contentType)
        assertEquals("https://www.instagram.com/reel/ABC123/", supported.normalizedUrl)
    }

    @Test
    fun `supports instagram post urls`() {
        val result = UrlValidator.validate("https://instagram.com/p/POST123/")

        assertTrue(result is UrlValidationResult.Supported)
        val supported = (result as UrlValidationResult.Supported).value
        assertEquals(SnsPlatform.Instagram, supported.platform)
        assertEquals(ContentType.Post, supported.contentType)
    }

    @Test
    fun `supports threads net post urls`() {
        val result = UrlValidator.validate("https://www.threads.net/@openai/post/THREAD123")

        assertTrue(result is UrlValidationResult.Supported)
        val supported = (result as UrlValidationResult.Supported).value
        assertEquals(SnsPlatform.Threads, supported.platform)
        assertEquals(ContentType.Post, supported.contentType)
    }

    @Test
    fun `supports threads com post urls`() {
        val result = UrlValidator.validate("https://www.threads.com/@openai/post/THREAD123")

        assertTrue(result is UrlValidationResult.Supported)
        val supported = (result as UrlValidationResult.Supported).value
        assertEquals(SnsPlatform.Threads, supported.platform)
        assertEquals(ContentType.Post, supported.contentType)
    }

    @Test
    fun `rejects unsupported hosts`() {
        val result = UrlValidator.validate("https://example.com/reel/ABC123/")

        assertEquals(
            UrlValidationResult.Unsupported(ValidationFailure.UnsupportedHost),
            result,
        )
    }

    @Test
    fun `rejects unsupported instagram paths`() {
        val result = UrlValidator.validate("https://www.instagram.com/stories/openai/123")

        assertEquals(
            UrlValidationResult.Unsupported(ValidationFailure.UnsupportedPath),
            result,
        )
    }

    @Test
    fun `rejects unsupported schemes`() {
        val result = UrlValidator.validate("ftp://www.instagram.com/reel/ABC123/")

        assertEquals(
            UrlValidationResult.Unsupported(ValidationFailure.UnsupportedScheme),
            result,
        )
    }

    @Test
    fun `rejects blank input`() {
        val result = UrlValidator.validate("   ")

        assertEquals(
            UrlValidationResult.Unsupported(ValidationFailure.Empty),
            result,
        )
    }
}
