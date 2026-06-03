package com.example.videodownloader.data.extract

data class VideoCandidate(
    val url: String,
    val source: CandidateSource,
)

enum class CandidateSource {
    OpenGraph,
    JsonLd,
    Script,
}
