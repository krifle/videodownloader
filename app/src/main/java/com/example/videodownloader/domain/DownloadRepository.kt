package com.example.videodownloader.domain

import com.example.videodownloader.data.download.DownloadProgress
import com.example.videodownloader.data.download.VideoDownloadResult
import com.example.videodownloader.data.download.VideoDownloader
import com.example.videodownloader.data.download.VideoUrlVerifier
import com.example.videodownloader.data.download.VideoVerificationResult
import com.example.videodownloader.data.extract.AppExtractor
import com.example.videodownloader.data.extract.ExtractError
import com.example.videodownloader.data.extract.ExtractionOutcome
import com.example.videodownloader.data.extract.PageFetchResult
import com.example.videodownloader.data.extract.PageFetcher
import com.example.videodownloader.data.media.MediaSaveResult
import com.example.videodownloader.data.media.MediaSaver
import com.example.videodownloader.data.media.SaveVideoRequest
import com.example.videodownloader.data.media.WriteResult
import com.example.videodownloader.util.FileNameSanitizer
import java.time.Clock
import java.time.LocalDateTime

class DownloadRepository(
    private val pageFetcher: PageFetcher,
    private val appExtractor: AppExtractor,
    private val videoUrlVerifier: VideoUrlVerifier,
    private val videoDownloader: VideoDownloader,
    private val mediaSaver: MediaSaver,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    fun download(
        supportedUrl: SupportedUrl,
        onProgress: (DownloadProgress) -> Unit = {},
    ): DownloadRepositoryResult {
        val html = when (val fetchResult = pageFetcher.fetch(supportedUrl.normalizedUrl)) {
            is PageFetchResult.Success -> fetchResult.html
            is PageFetchResult.Failure -> return DownloadRepositoryResult.Failure(fetchResult.error)
        }

        val extractResult = when (val outcome = appExtractor.extract(supportedUrl, html)) {
            is ExtractionOutcome.Success -> outcome.result
            is ExtractionOutcome.Failure -> return DownloadRepositoryResult.Failure(outcome.error)
        }

        val verifiedUrl = when (val verification = videoUrlVerifier.verify(extractResult.videoUrl)) {
            is VideoVerificationResult.Success -> verification.verifiedVideoUrl
            is VideoVerificationResult.Failure -> return DownloadRepositoryResult.Failure(verification.error)
        }

        val fileName = FileNameSanitizer.sanitizeVideoFileName(
            rawName = extractResult.title ?: FileNameSanitizer.defaultVideoName(
                platform = extractResult.platform,
                contentType = extractResult.contentType,
                now = LocalDateTime.now(clock),
            ),
        )

        return when (
            val saveResult = mediaSaver.saveVideo(
                request = SaveVideoRequest(fileName = fileName),
                writer = { outputStream ->
                    when (
                        val downloadResult = videoDownloader.download(
                            url = verifiedUrl.url,
                            outputStream = outputStream,
                            onProgress = onProgress,
                        )
                    ) {
                        is VideoDownloadResult.Success -> WriteResult.Success(downloadResult.bytesWritten)
                        is VideoDownloadResult.Failure -> WriteResult.Failure(downloadResult.error)
                    }
                },
            )
        ) {
            is MediaSaveResult.Success -> DownloadRepositoryResult.Success(
                fileName = saveResult.fileName,
                bytesWritten = saveResult.bytesWritten,
            )
            is MediaSaveResult.Failure -> DownloadRepositoryResult.Failure(saveResult.error)
        }
    }
}

sealed interface DownloadRepositoryResult {
    data class Success(
        val fileName: String,
        val bytesWritten: Long,
    ) : DownloadRepositoryResult

    data class Failure(val error: ExtractError) : DownloadRepositoryResult
}
