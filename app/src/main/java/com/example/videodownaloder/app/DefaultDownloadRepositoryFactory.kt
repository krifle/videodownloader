package com.example.videodownaloder.app

import android.content.Context
import com.example.videodownaloder.data.download.VideoDownloader
import com.example.videodownaloder.data.download.VideoUrlVerifier
import com.example.videodownaloder.data.extract.AppExtractor
import com.example.videodownaloder.data.extract.PageFetcher
import com.example.videodownaloder.data.media.AndroidMediaStoreSaver
import com.example.videodownaloder.domain.DownloadRepository

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
