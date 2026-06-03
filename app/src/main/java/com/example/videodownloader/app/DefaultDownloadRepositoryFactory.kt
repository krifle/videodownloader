package com.example.videodownloader.app

import android.content.Context
import com.example.videodownloader.data.download.VideoDownloader
import com.example.videodownloader.data.download.VideoUrlVerifier
import com.example.videodownloader.data.extract.AppExtractor
import com.example.videodownloader.data.extract.PageFetcher
import com.example.videodownloader.data.media.AndroidMediaStoreSaver
import com.example.videodownloader.domain.DownloadRepository

object DefaultDownloadRepositoryFactory {
    fun create(context: Context): DownloadRepository {
        val client = PageFetcher.defaultClient()
        return DownloadRepository(
            pageFetcher = PageFetcher(client),
            appExtractor = AppExtractor(),
            videoUrlVerifier = VideoUrlVerifier(client),
            videoDownloader = VideoDownloader(client),
            mediaSaver = AndroidMediaStoreSaver(context),
        )
    }
}
