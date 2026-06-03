package com.example.videodownaloder.domain

data class SupportedUrl(
    val originalUrl: String,
    val normalizedUrl: String,
    val platform: SnsPlatform,
    val contentType: ContentType,
)
