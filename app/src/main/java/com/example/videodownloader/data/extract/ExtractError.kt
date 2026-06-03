package com.example.videodownloader.data.extract

enum class ExtractError {
    UnsupportedUrl,
    AccessBlocked,
    LoginRequired,
    PrivateContent,
    MediaNotFound,
    VideoUrlExpired,
    PlatformChanged,
    NetworkFailed,
    DownloadFailed,
    SaveFailed,
}
