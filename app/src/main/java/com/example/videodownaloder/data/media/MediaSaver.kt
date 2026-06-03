package com.example.videodownaloder.data.media

import com.example.videodownaloder.data.extract.ExtractError
import java.io.OutputStream

interface MediaSaver {
    fun saveVideo(
        request: SaveVideoRequest,
        writer: (OutputStream) -> WriteResult,
    ): MediaSaveResult
}

data class SaveVideoRequest(
    val fileName: String,
    val mimeType: String = "video/mp4",
    val relativePath: String = "Movies/SNSDownloader",
)

sealed interface WriteResult {
    data class Success(val bytesWritten: Long) : WriteResult
    data class Failure(val error: ExtractError) : WriteResult
}

sealed interface MediaSaveResult {
    data class Success(
        val fileName: String,
        val bytesWritten: Long,
    ) : MediaSaveResult

    data class Failure(val error: ExtractError) : MediaSaveResult
}
