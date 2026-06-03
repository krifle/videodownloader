package com.example.videodownloader.util

import com.example.videodownloader.data.extract.ExtractError
import com.example.videodownloader.domain.ValidationFailure

object ErrorMessageMapper {
    fun fromValidationFailure(reason: ValidationFailure): String {
        return when (reason) {
            ValidationFailure.Empty -> "URL을 입력해주세요."
            ValidationFailure.InvalidUrl -> "올바른 URL 형식이 아닙니다."
            ValidationFailure.UnsupportedScheme -> "http 또는 https URL만 지원합니다."
            ValidationFailure.UnsupportedHost -> "Instagram 또는 Threads URL만 지원합니다."
            ValidationFailure.UnsupportedPath -> "지원하지 않는 게시물 형식입니다."
        }
    }

    fun fromExtractError(error: ExtractError): String {
        return when (error) {
            ExtractError.UnsupportedUrl -> "지원하지 않는 URL입니다."
            ExtractError.AccessBlocked -> "게시물 페이지에 접근하지 못했습니다."
            ExtractError.LoginRequired -> "로그인이 필요한 게시물은 지원하지 않습니다."
            ExtractError.PrivateContent -> "비공개이거나 접근할 수 없는 게시물입니다."
            ExtractError.MediaNotFound -> "영상 파일을 찾지 못했습니다."
            ExtractError.VideoUrlExpired -> "영상 주소가 만료되었거나 접근할 수 없습니다. 다시 시도해주세요."
            ExtractError.PlatformChanged -> "플랫폼 구조 변경으로 다운로드에 실패했을 수 있습니다."
            ExtractError.NetworkFailed -> "네트워크 연결을 확인해주세요."
            ExtractError.DownloadFailed -> "파일 다운로드에 실패했습니다."
            ExtractError.SaveFailed -> "갤러리 저장에 실패했습니다."
        }
    }
}
