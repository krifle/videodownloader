package com.example.videodownaloder.data.extract

import com.example.videodownaloder.data.extract.platform.InstagramExtractor
import com.example.videodownaloder.data.extract.platform.PlatformExtractor
import com.example.videodownaloder.data.extract.platform.ThreadsExtractor
import com.example.videodownaloder.domain.SnsPlatform
import com.example.videodownaloder.domain.SupportedUrl

class AppExtractor(
    private val extractors: Map<SnsPlatform, PlatformExtractor> = mapOf(
        SnsPlatform.Instagram to InstagramExtractor(),
        SnsPlatform.Threads to ThreadsExtractor(),
    ),
) {
    fun extract(supportedUrl: SupportedUrl, html: String): ExtractionOutcome {
        val extractor = extractors[supportedUrl.platform]
            ?: return ExtractionOutcome.Failure(ExtractError.UnsupportedUrl)

        return extractor.extract(supportedUrl, html)
    }
}
