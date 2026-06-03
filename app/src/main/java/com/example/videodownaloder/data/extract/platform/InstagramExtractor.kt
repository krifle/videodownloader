package com.example.videodownaloder.data.extract.platform

import com.example.videodownaloder.data.extract.ExtractError
import com.example.videodownaloder.data.extract.ExtractResult
import com.example.videodownaloder.data.extract.ExtractionOutcome
import com.example.videodownaloder.data.extract.HtmlVideoCandidateExtractor
import com.example.videodownaloder.domain.SnsPlatform
import com.example.videodownaloder.domain.SupportedUrl

class InstagramExtractor : PlatformExtractor {
    override fun extract(supportedUrl: SupportedUrl, html: String): ExtractionOutcome {
        detectFailurePage(html)?.let { return ExtractionOutcome.Failure(it) }

        val candidate = HtmlVideoCandidateExtractor.extract(html).firstOrNull()
            ?: return ExtractionOutcome.Failure(ExtractError.MediaNotFound)

        return ExtractionOutcome.Success(
            ExtractResult(
                platform = SnsPlatform.Instagram,
                contentType = supportedUrl.contentType,
                sourceUrl = supportedUrl.normalizedUrl,
                videoUrl = candidate.url,
                title = HtmlVideoCandidateExtractor.title(html),
                thumbnailUrl = HtmlVideoCandidateExtractor.thumbnailUrl(html),
            ),
        )
    }

    private fun detectFailurePage(html: String): ExtractError? {
        val lower = html.lowercase()
        return when {
            "log in to instagram" in lower || "/accounts/login" in lower -> ExtractError.LoginRequired
            "private account" in lower || "비공개" in lower -> ExtractError.PrivateContent
            "temporarily blocked" in lower || "misusing this feature" in lower -> ExtractError.AccessBlocked
            else -> null
        }
    }
}
