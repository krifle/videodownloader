package com.example.videodownaloder.data.download

import com.example.videodownaloder.data.extract.ExtractError

sealed interface VideoDownloadResult {
    data class Success(val bytesWritten: Long) : VideoDownloadResult
    data class Failure(val error: ExtractError) : VideoDownloadResult
}
