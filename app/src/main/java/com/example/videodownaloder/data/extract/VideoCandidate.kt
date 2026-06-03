package com.example.videodownaloder.data.extract

data class VideoCandidate(
    val url: String,
    val source: CandidateSource,
)

enum class CandidateSource {
    OpenGraph,
    JsonLd,
    Script,
}
