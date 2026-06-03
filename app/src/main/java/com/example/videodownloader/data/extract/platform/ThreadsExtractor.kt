package com.example.videodownloader.data.extract.platform

import com.example.videodownloader.data.extract.ExtractError
import com.example.videodownloader.data.extract.ExtractResult
import com.example.videodownloader.data.extract.ExtractionOutcome
import com.example.videodownloader.data.extract.HtmlVideoCandidateExtractor
import com.example.videodownloader.domain.SnsPlatform
import com.example.videodownloader.domain.SupportedUrl

class ThreadsExtractor : PlatformExtractor {
    override fun extract(supportedUrl: SupportedUrl, html: String): ExtractionOutcome {
        detectFailurePage(html)?.let { return ExtractionOutcome.Failure(it) }

        val candidate = HtmlVideoCandidateExtractor.extract(html).firstOrNull()
            ?: return ExtractionOutcome.Failure(ExtractError.MediaNotFound)

        return ExtractionOutcome.Success(
            ExtractResult(
                platform = SnsPlatform.Threads,
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
            "log in to threads" in lower || "login to threads" in lower -> ExtractError.LoginRequired
            "private profile" in lower || "비공개" in lower -> ExtractError.PrivateContent
            "temporarily blocked" in lower || "misusing this feature" in lower -> ExtractError.AccessBlocked
            else -> null
        }
    }
}
