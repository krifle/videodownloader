package com.example.videodownaloder.util

import com.example.videodownaloder.domain.ContentType
import com.example.videodownaloder.domain.SnsPlatform
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object FileNameSanitizer {
    private val timestampFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.US)
    private val invalidCharacters = Regex("""[\\/:*?"<>|#%{}~&]""")
    private val whitespace = Regex("""\s+""")
    private val repeatedUnderscores = Regex("""_+""")

    fun defaultVideoName(
        platform: SnsPlatform,
        contentType: ContentType,
        now: LocalDateTime,
    ): String {
        return "${platform.id}_${contentType.id}_${now.format(timestampFormatter)}.mp4"
    }

    fun sanitizeVideoFileName(rawName: String, fallbackName: String = "sns_video.mp4"): String {
        val base = rawName
            .trim()
            .replace(whitespace, "_")
            .replace(invalidCharacters, "")
            .replace(repeatedUnderscores, "_")
            .trim('_', '.')

        val safeBase = base.ifBlank { fallbackName }
        return if (safeBase.endsWith(".mp4", ignoreCase = true)) {
            safeBase
        } else {
            "$safeBase.mp4"
        }
    }
}
