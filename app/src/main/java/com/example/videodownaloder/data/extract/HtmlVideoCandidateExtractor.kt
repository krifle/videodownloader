package com.example.videodownaloder.data.extract

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object HtmlVideoCandidateExtractor {
    private val directMp4Regex = Regex("""https?:\\?/\\?/[^"'<>\s)]+?\.mp4(?:\?[^"'<>\s)]*)?""")
    private val namedVideoFieldRegex = Regex(
        """"(?:contentUrl|embedUrl|video_url|playable_url|media_url|videoUrl)"\s*:\s*"([^"]+)"""",
        setOf(RegexOption.IGNORE_CASE),
    )

    fun extract(html: String): List<VideoCandidate> {
        val document = Jsoup.parse(html)
        val candidates = linkedSetOf<VideoCandidate>()

        candidates += extractFromOpenGraph(document)
        candidates += extractFromJsonLd(document)
        candidates += extractFromScripts(document)

        return candidates
            .filter { it.url.startsWith("http://") || it.url.startsWith("https://") }
            .filter { it.url.contains(".mp4", ignoreCase = true) }
            .distinctBy { it.url }
    }

    fun title(html: String): String? {
        val document = Jsoup.parse(html)
        return document.selectFirst("""meta[property=og:title]""")
            ?.attr("content")
            ?.takeIf { it.isNotBlank() }
            ?: document.title().takeIf { it.isNotBlank() }
    }

    fun thumbnailUrl(html: String): String? {
        val document = Jsoup.parse(html)
        return document.selectFirst("""meta[property=og:image], meta[name=twitter:image]""")
            ?.attr("content")
            ?.takeIf { it.isNotBlank() }
    }

    private fun extractFromOpenGraph(document: Document): Set<VideoCandidate> {
        return document
            .select(
                """
                meta[property=og:video],
                meta[property=og:video:url],
                meta[property=og:video:secure_url],
                meta[name=twitter:player:stream]
                """.trimIndent(),
            )
            .mapNotNull { it.attr("content").toCandidate(CandidateSource.OpenGraph) }
            .toSet()
    }

    private fun extractFromJsonLd(document: Document): Set<VideoCandidate> {
        return document
            .select("""script[type=application/ld+json]""")
            .flatMap { extractFromText(it.data().ifBlank { it.html() }, CandidateSource.JsonLd) }
            .toSet()
    }

    private fun extractFromScripts(document: Document): Set<VideoCandidate> {
        return document
            .select("script")
            .flatMap { extractFromText(it.data().ifBlank { it.html() }, CandidateSource.Script) }
            .toSet()
    }

    private fun extractFromText(text: String, source: CandidateSource): Set<VideoCandidate> {
        val namedFieldCandidates = namedVideoFieldRegex
            .findAll(text)
            .mapNotNull { match -> match.groupValues.getOrNull(1)?.toCandidate(source) }

        val directUrlCandidates = directMp4Regex
            .findAll(text)
            .mapNotNull { match -> match.value.toCandidate(source) }

        return (namedFieldCandidates + directUrlCandidates).toSet()
    }

    private fun String.toCandidate(source: CandidateSource): VideoCandidate? {
        val normalized = normalizeCandidate()
        if (normalized.isBlank()) return null
        return VideoCandidate(normalized, source)
    }

    private fun String.normalizeCandidate(): String {
        return trim()
            .replace("\\/", "/")
            .replace("\\u0026", "&")
            .replace("\\u003d", "=")
            .replace("\\u003f", "?")
            .replace("\\u0025", "%")
            .replace("&amp;", "&")
            .trim('"', '\'', ',', ';')
    }
}
