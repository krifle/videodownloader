package com.example.videodownloader.util

import com.example.videodownloader.data.extract.ExtractError
import com.example.videodownloader.domain.ValidationFailure
import org.junit.Assert.assertEquals
import org.junit.Test

class ErrorMessageMapperTest {
    @Test
    fun `maps validation failures to Korean messages`() {
        assertEquals("URL을 입력해주세요.", ErrorMessageMapper.fromValidationFailure(ValidationFailure.Empty))
        assertEquals("올바른 URL 형식이 아닙니다.", ErrorMessageMapper.fromValidationFailure(ValidationFailure.InvalidUrl))
        assertEquals("http 또는 https URL만 지원합니다.", ErrorMessageMapper.fromValidationFailure(ValidationFailure.UnsupportedScheme))
        assertEquals("Instagram 또는 Threads URL만 지원합니다.", ErrorMessageMapper.fromValidationFailure(ValidationFailure.UnsupportedHost))
        assertEquals("지원하지 않는 게시물 형식입니다.", ErrorMessageMapper.fromValidationFailure(ValidationFailure.UnsupportedPath))
    }

    @Test
    fun `maps extract errors to Korean messages`() {
        assertEquals("지원하지 않는 URL입니다.", ErrorMessageMapper.fromExtractError(ExtractError.UnsupportedUrl))
        assertEquals("게시물 페이지에 접근하지 못했습니다.", ErrorMessageMapper.fromExtractError(ExtractError.AccessBlocked))
        assertEquals("로그인이 필요한 게시물은 지원하지 않습니다.", ErrorMessageMapper.fromExtractError(ExtractError.LoginRequired))
        assertEquals("비공개이거나 접근할 수 없는 게시물입니다.", ErrorMessageMapper.fromExtractError(ExtractError.PrivateContent))
        assertEquals("영상 파일을 찾지 못했습니다.", ErrorMessageMapper.fromExtractError(ExtractError.MediaNotFound))
        assertEquals("영상 주소가 만료되었거나 접근할 수 없습니다. 다시 시도해주세요.", ErrorMessageMapper.fromExtractError(ExtractError.VideoUrlExpired))
        assertEquals("플랫폼 구조 변경으로 다운로드에 실패했을 수 있습니다.", ErrorMessageMapper.fromExtractError(ExtractError.PlatformChanged))
        assertEquals("네트워크 연결을 확인해주세요.", ErrorMessageMapper.fromExtractError(ExtractError.NetworkFailed))
        assertEquals("파일 다운로드에 실패했습니다.", ErrorMessageMapper.fromExtractError(ExtractError.DownloadFailed))
        assertEquals("갤러리 저장에 실패했습니다.", ErrorMessageMapper.fromExtractError(ExtractError.SaveFailed))
    }
}
