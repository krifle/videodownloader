package com.example.videodownaloder.domain

import java.net.URI
import java.net.URISyntaxException
import java.util.Locale

object UrlValidator {
    private val instagramHosts = setOf(
        "instagram.com",
        "www.instagram.com",
    )
    private val threadsHosts = setOf(
        "threads.net",
        "www.threads.net",
        "threads.com",
        "www.threads.com",
    )

    fun validate(input: String): UrlValidationResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return UrlValidationResult.Unsupported(ValidationFailure.Empty)
        }

        val uri = parseUri(trimmed)
            ?: return UrlValidationResult.Unsupported(ValidationFailure.InvalidUrl)

        val scheme = uri.scheme?.lowercase(Locale.US)
            ?: return UrlValidationResult.Unsupported(ValidationFailure.InvalidUrl)
        if (scheme != "https" && scheme != "http") {
            return UrlValidationResult.Unsupported(ValidationFailure.UnsupportedScheme)
        }

        val host = uri.host?.lowercase(Locale.US)
            ?: return UrlValidationResult.Unsupported(ValidationFailure.InvalidUrl)

        val pathSegments = uri.path
            ?.split("/")
            ?.filter { it.isNotBlank() }
            ?: emptyList()

        val supported = when {
            host in instagramHosts -> parseInstagram(trimmed, uri, pathSegments)
            host in threadsHosts -> parseThreads(trimmed, uri, pathSegments)
            else -> null
        }

        return if (supported != null) {
            UrlValidationResult.Supported(supported)
        } else {
            val failure = if (host in instagramHosts || host in threadsHosts) {
                ValidationFailure.UnsupportedPath
            } else {
                ValidationFailure.UnsupportedHost
            }
            UrlValidationResult.Unsupported(failure)
        }
    }

    private fun parseUri(input: String): URI? {
        return try {
            URI(input)
        } catch (_: URISyntaxException) {
            null
        }
    }

    private fun parseInstagram(
        originalUrl: String,
        uri: URI,
        segments: List<String>,
    ): SupportedUrl? {
        if (segments.size < 2) return null

        val contentType = when (segments[0].lowercase(Locale.US)) {
            "reel", "reels" -> ContentType.Reel
            "p" -> ContentType.Post
            "tv" -> ContentType.Video
            else -> return null
        }

        return SupportedUrl(
            originalUrl = originalUrl,
            normalizedUrl = normalize(uri),
            platform = SnsPlatform.Instagram,
            contentType = contentType,
        )
    }

    private fun parseThreads(
        originalUrl: String,
        uri: URI,
        segments: List<String>,
    ): SupportedUrl? {
        if (segments.size < 3) return null
        val isPostUrl = segments[0].startsWith("@") && segments[1].equals("post", ignoreCase = true)
        if (!isPostUrl) return null

        return SupportedUrl(
            originalUrl = originalUrl,
            normalizedUrl = normalize(uri),
            platform = SnsPlatform.Threads,
            contentType = ContentType.Post,
        )
    }

    private fun normalize(uri: URI): String {
        val scheme = uri.scheme.lowercase(Locale.US)
        val host = uri.host.lowercase(Locale.US)
        val rawPath = uri.rawPath?.takeIf { it.isNotBlank() } ?: "/"
        return URI(scheme, host, rawPath, null).toASCIIString()
    }
}

sealed interface UrlValidationResult {
    data class Supported(val value: SupportedUrl) : UrlValidationResult
    data class Unsupported(val reason: ValidationFailure) : UrlValidationResult
}

enum class ValidationFailure {
    Empty,
    InvalidUrl,
    UnsupportedScheme,
    UnsupportedHost,
    UnsupportedPath,
}
