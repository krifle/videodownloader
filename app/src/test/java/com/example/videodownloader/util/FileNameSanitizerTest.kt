package com.example.videodownloader.util

import com.example.videodownloader.domain.ContentType
import com.example.videodownloader.domain.SnsPlatform
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class FileNameSanitizerTest {
    @Test
    fun `creates deterministic default video names`() {
        val now = LocalDateTime.of(2026, 6, 3, 12, 10, 0)

        val name = FileNameSanitizer.defaultVideoName(
            platform = SnsPlatform.Instagram,
            contentType = ContentType.Reel,
            now = now,
        )

        assertEquals("instagram_reel_20260603_121000.mp4", name)
    }

    @Test
    fun `sanitizes invalid file name characters and appends mp4 extension`() {
        val name = FileNameSanitizer.sanitizeVideoFileName(" my reel: launch? #1 ")

        assertEquals("my_reel_launch_1.mp4", name)
    }

    @Test
    fun `keeps existing mp4 extension case insensitively`() {
        val name = FileNameSanitizer.sanitizeVideoFileName("Threads Clip.MP4")

        assertEquals("Threads_Clip.MP4", name)
    }

    @Test
    fun `uses fallback when sanitized name is blank`() {
        val name = FileNameSanitizer.sanitizeVideoFileName(" :/*? ")

        assertEquals("sns_video.mp4", name)
    }
}
