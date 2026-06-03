package com.example.videodownaloder.data.extract

import com.example.videodownaloder.domain.ContentType
import com.example.videodownaloder.domain.SnsPlatform

data class ExtractResult(
    val platform: SnsPlatform,
    val contentType: ContentType,
    val sourceUrl: String,
    val videoUrl: String,
    val title: String? = null,
    val thumbnailUrl: String? = null,
    val extension: String = "mp4",
)

sealed interface ExtractionOutcome {
    data class Success(val result: ExtractResult) : ExtractionOutcome
    data class Failure(val error: ExtractError) : ExtractionOutcome
}
