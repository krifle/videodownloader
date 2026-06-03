package com.example.videodownloader.data.download

import com.example.videodownloader.data.extract.ExtractError

sealed interface VideoDownloadResult {
    data class Success(val bytesWritten: Long) : VideoDownloadResult
    data class Failure(val error: ExtractError) : VideoDownloadResult
}
