package com.example.videodownloader.data.extract

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlVideoCandidateExtractorTest {
    @Test
    fun `extracts open graph video urls first`() {
        val html = """
            <html>
              <head>
                <meta property="og:video:secure_url" content="https://cdn.example.com/open-graph.mp4?token=abc&amp;dl=1">
              </head>
            </html>
        """.trimIndent()

        val candidates = HtmlVideoCandidateExtractor.extract(html)

        assertEquals(
            VideoCandidate(
                url = "https://cdn.example.com/open-graph.mp4?token=abc&dl=1",
                source = CandidateSource.OpenGraph,
            ),
            candidates.first(),
        )
    }

    @Test
    fun `extracts json ld content url`() {
        val html = """
            <script type="application/ld+json">
            {
              "@type": "VideoObject",
              "contentUrl": "https://cdn.example.com/jsonld-video.mp4?x=1\u0026y=2"
            }
            </script>
        """.trimIndent()

        val candidates = HtmlVideoCandidateExtractor.extract(html)

        assertTrue(candidates.any { it.url == "https://cdn.example.com/jsonld-video.mp4?x=1&y=2" })
    }

    @Test
    fun `extracts escaped mp4 urls from script payloads`() {
        val html = """
            <script>
              window.__data = {"video_url":"https:\/\/scontent.cdninstagram.com\/o1\/v\/clip.mp4?efg=abc\u0026_nc_ht=scontent.cdninstagram.com"};
            </script>
        """.trimIndent()

        val candidates = HtmlVideoCandidateExtractor.extract(html)

        assertTrue(
            candidates.any {
                it.url == "https://scontent.cdninstagram.com/o1/v/clip.mp4?efg=abc&_nc_ht=scontent.cdninstagram.com"
            },
        )
    }

    @Test
    fun `extracts title and thumbnail metadata`() {
        val html = """
            <html>
              <head>
                <title>Fallback title</title>
                <meta property="og:title" content="Public Reel">
                <meta property="og:image" content="https://cdn.example.com/thumb.jpg">
              </head>
            </html>
        """.trimIndent()

        assertEquals("Public Reel", HtmlVideoCandidateExtractor.title(html))
        assertEquals("https://cdn.example.com/thumb.jpg", HtmlVideoCandidateExtractor.thumbnailUrl(html))
    }
}
