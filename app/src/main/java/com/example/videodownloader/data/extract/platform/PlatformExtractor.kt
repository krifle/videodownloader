package com.example.videodownloader.data.extract.platform

import com.example.videodownloader.data.extract.ExtractionOutcome
import com.example.videodownloader.domain.SupportedUrl

interface PlatformExtractor {
    fun extract(supportedUrl: SupportedUrl, html: String): ExtractionOutcome
}
