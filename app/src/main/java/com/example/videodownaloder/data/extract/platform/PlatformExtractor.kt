package com.example.videodownaloder.data.extract.platform

import com.example.videodownaloder.data.extract.ExtractionOutcome
import com.example.videodownaloder.domain.SupportedUrl

interface PlatformExtractor {
    fun extract(supportedUrl: SupportedUrl, html: String): ExtractionOutcome
}
