package com.example.videodownaloder.ui.download

enum class DownloadStatus {
    Idle,
    InvalidUrl,
    FetchingPage,
    Extracting,
    VerifyingVideo,
    Downloading,
    Saving,
    Completed,
    Failed,
}

data class DownloadUiState(
    val url: String = "",
    val status: DownloadStatus = DownloadStatus.Idle,
    val progress: Float? = null,
    val message: String? = null,
    val savedFileName: String? = null,
)
