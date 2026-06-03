package com.example.videodownaloder.data.download

data class DownloadProgress(
    val bytesRead: Long,
    val totalBytes: Long?,
) {
    val fraction: Float? =
        totalBytes?.takeIf { it > 0L }?.let { bytesRead.toFloat() / it.toFloat() }
}
