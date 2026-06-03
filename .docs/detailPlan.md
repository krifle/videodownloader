# SNS 영상 다운로드 앱 상세 기획서

## 1. 문서 목적

이 문서는 `.docs/masterPlan.md`의 초기 기획을 **외부 백엔드 서버 없이 Android 앱 단독으로 처리하는 구조**에 맞춰 수정한 상세 기획서다.

기존 기획은 Android 앱이 URL 입력과 파일 저장을 담당하고, 별도 백엔드 서버가 Instagram/Threads URL 분석과 MP4 URL 추출을 담당하는 구조였다. 하지만 개인 프로젝트에서 서버를 유지하기 어렵기 때문에, MVP는 모바일 앱 내부에 URL 분석, 공개 페이지 조회, 영상 URL 후보 추출, 다운로드, MediaStore 저장까지 모두 넣는 방향으로 변경한다.

## 2. 결론

서버 없이 바로 다운로드하는 방식은 **부분적으로 가능하지만 안정적이지 않다.**

공식 API만으로는 사용자가 붙여넣은 임의의 공개 Instagram/Threads URL에서 MP4 원본 URL을 안정적으로 가져오는 기능을 제공한다고 보기 어렵다. Instagram oEmbed는 게시물 임베드와 기본 메타데이터 표시 목적이며, 해당 엔드포인트의 콘텐츠와 메타데이터를 추출하거나 저장하는 목적의 사용은 제한된다. Threads API 역시 사용자 인증과 권한을 전제로 게시물 생성, 관리, 조회 같은 기능을 제공하며, 임의의 공개 URL을 입력받아 다운로드 가능한 MP4 URL을 돌려주는 공개 다운로드 API로 보기 어렵다.

따라서 MVP의 현실적인 방향은 다음과 같다.

* 앱 내부에서 공개 게시물 페이지를 직접 요청한다.
* HTML, JSON-LD, script 데이터, meta 태그에서 영상 URL 후보를 찾는다.
* `.mp4` 또는 영상 CDN URL을 검증한다.
* 검증된 URL을 앱이 직접 다운로드한다.
* 실패하면 사용자에게 플랫폼 구조 변경, 로그인 필요, 접근 제한 가능성을 안내한다.

이 방식은 서버 유지 비용이 없다는 장점이 있지만, 플랫폼 구조 변경에 취약하고 앱 업데이트 없이는 추출 로직을 고치기 어렵다.

## 3. 현재 프로젝트 파악

### 3.1 프로젝트 상태

* 프로젝트명: `videodownaloder`
* Android 모듈: `:app`
* 패키지명: `com.example.videodownaloder`
* UI: Jetpack Compose 기본 템플릿
* 현재 화면: `Hello Android!` 텍스트만 표시
* Git 상태: 현재 작업 디렉터리는 Git 저장소로 인식되지 않음

### 3.2 주요 기술 전제

현재 Gradle 설정 기준:

* Kotlin: `2.0.21`
* Android Gradle Plugin: `9.0.1`
* Compose BOM: `2024.09.00`
* compileSdk: `36.1`
* minSdk: `35`
* targetSdk: `36`

MVP 우선 기술:

* Jetpack Compose
* Kotlin Coroutines
* OkHttp
* kotlinx.serialization 또는 Moshi
* Jsoup
* MediaStore

후순위 기술:

* WorkManager
* Room Database
* DataStore

## 4. 공식 API 검토 결과

### 4.1 Instagram oEmbed

Instagram oEmbed는 게시물의 embed HTML과 기본 메타데이터를 가져와 앱이나 웹사이트에 표시하는 목적의 API다. 공식 문서상 photo, video, Reel 게시물 임베드를 지원하지만, 해당 endpoint는 임베드 표시 용도이며 콘텐츠나 메타데이터를 다른 목적으로 추출, 조작, 저장하는 사용은 제한된다.

또한 모바일 앱에서 직접 호출하려면 Client Access Token을 사용해야 하고 rate limit도 서버용 App Access Token보다 낮다. App Access Token은 client-side에 두면 안 되므로, 서버 없는 앱 구조에서는 안전하게 쓰기 어렵다.

결론:

* 다운로드용 API로 채택하지 않는다.
* 앱 내부 추출기의 보조 메타데이터 소스로도 MVP에서는 사용하지 않는다.

근거:

* Instagram oEmbed 문서: <https://developers.facebook.com/docs/instagram-platform/oembed/>

### 4.2 Instagram Graph API

Instagram Graph API의 media 관련 문서는 주로 앱 사용자의 Instagram 계정, 미디어 생성, 게시, 조회 흐름을 다룬다. media 생성 시 `video_url`을 넘겨 Instagram이 외부 영상을 가져가게 하는 기능은 있지만, 이것은 사용자가 입력한 Instagram 게시물에서 원본 MP4 URL을 받아오는 기능이 아니다.

결론:

* 임의의 공개 게시물 URL 다운로드 용도로 적합하지 않다.
* Instagram 계정 로그인, 권한, 앱 리뷰까지 포함하면 개인용 MVP 범위를 넘어선다.

근거:

* Instagram Graph API Media 문서: <https://developers.facebook.com/docs/instagram-platform/instagram-graph-api/reference/ig-user/media>

### 4.3 Threads API

Threads API는 OAuth 인증과 권한을 전제로 Threads 콘텐츠를 생성, 관리, 게시하는 API다. Meta의 Postman 문서 기준으로도 access token과 `threads_basic`, `threads_content_publish` 등 권한 확인이 필요하다. 영상 게시용 `video_url` 파라미터는 Threads에 영상을 게시할 때 사용하는 외부 영상 URL이지, Threads 게시물 URL에서 다운로드 URL을 추출하는 기능이 아니다.

결론:

* 임의의 Threads 공개 URL에서 MP4 다운로드 URL을 가져오는 MVP 기능으로는 채택하지 않는다.
* Threads는 앱 내부 공개 페이지 추출기로만 시도한다.

근거:

* Meta Threads API Postman 문서: <https://www.postman.com/meta/threads/documentation/dht3nzz/threads-api>

## 5. 제품 목표

### 5.1 핵심 목표

사용자가 Instagram 또는 Threads의 공개 영상 URL을 입력하면, Android 앱이 직접 URL을 분석하고 MP4 다운로드 URL을 찾아 `Movies/SNSDownloader`에 저장한다.

### 5.2 사용자 가치

* 별도 서버 운영 없이 앱만으로 사용 가능
* URL 붙여넣기 후 다운로드까지 한 화면에서 처리
* 저장 완료 후 갤러리에서 바로 확인 가능
* 실패 시 사용자가 이해할 수 있는 한국어 메시지 제공

### 5.3 비목표

MVP 단계에서는 다음을 구현하지 않는다.

* 외부 백엔드 서버
* Instagram/Threads 로그인 기능
* 비공개 계정 게시물 다운로드
* 스토리, 라이브 영상 다운로드
* DRM 또는 접근 제한 콘텐츠 우회
* 캐러셀 전체 다운로드
* 타인 콘텐츠 재배포 기능
* Google Play 배포를 전제로 한 상용 앱 운영

## 6. MVP 범위

### 6.1 포함 기능

* URL 직접 입력
* Instagram Reel URL 1차 검증
* Instagram 공개 동영상 게시물 URL 1차 검증
* Threads 공개 동영상 URL 1차 검증
* 앱 내부 공개 페이지 조회
* HTML/script/meta 데이터 기반 영상 URL 후보 추출
* MP4 URL 검증
* MP4 다운로드
* MediaStore를 통한 갤러리 저장
* 다운로드 진행 상태 표시
* 성공/실패 메시지 표시
* 이용 책임 안내 표시

### 6.2 후순위 기능

* 클립보드 URL 자동 감지
* Android 공유 메뉴에서 URL 받기
* 다운로드 기록 저장
* WorkManager 기반 장기 백그라운드 다운로드
* 썸네일 미리보기
* 플랫폼별 저장 폴더 분리
* 앱 내 추출 규칙 버전 표시
* 사용자가 실패 URL을 복사해 리포트할 수 있는 기능

## 7. 서버리스 앱 구조

### 7.1 전체 구조

```text
사용자 URL 입력
    ↓
UrlValidator
    ↓
AppExtractor
    ↓
PageFetcher
    ↓
PlatformExtractor
    ├── InstagramExtractor
    └── ThreadsExtractor
    ↓
VideoUrlVerifier
    ↓
VideoDownloader
    ↓
MediaStoreSaver
    ↓
갤러리 저장 완료
```

### 7.2 기존 서버 구조와의 차이

| 항목 | 기존 서버 구조 | 수정된 앱 단독 구조 |
| --- | --- | --- |
| URL 분석 | 백엔드 서버 | Android 앱 내부 |
| HTML 조회 | 백엔드 서버 | Android 앱 내부 |
| MP4 URL 추출 | 백엔드 서버 | Android 앱 내부 extractor |
| 서버 운영 | 필요 | 불필요 |
| 플랫폼 변경 대응 | 서버 코드 수정 | 앱 업데이트 필요 |
| 보안 토큰 관리 | 서버에서 가능 | 앱에서는 제한적 |
| 안정성 | 상대적으로 높음 | 낮음 |

## 8. 사용자 흐름

### 8.1 기본 다운로드 흐름

1. 사용자가 Instagram 또는 Threads에서 게시물 링크를 복사한다.
2. 앱을 실행한다.
3. URL 입력창에 링크를 붙여넣는다.
4. 앱이 URL 형식을 1차 검증한다.
5. 사용자가 다운로드 버튼을 누른다.
6. 앱이 공개 게시물 페이지를 요청한다.
7. 앱이 HTML, JSON-LD, script, meta 태그에서 영상 URL 후보를 찾는다.
8. 앱이 후보 URL에 HEAD 또는 제한된 GET 요청을 보내 영상 응답인지 확인한다.
9. 앱이 MP4 파일을 다운로드한다.
10. 앱이 MediaStore로 `Movies/SNSDownloader`에 파일을 저장한다.
11. 앱이 저장 성공 메시지를 표시한다.

### 8.2 실패 흐름

1. 사용자가 지원하지 않는 URL을 입력한다.
2. 앱이 다운로드 버튼을 비활성화하거나 오류 메시지를 표시한다.
3. 페이지 요청이 차단되면 `ACCESS_BLOCKED`로 처리한다.
4. 로그인 페이지나 로그인 유도 응답이면 `LOGIN_REQUIRED`로 처리한다.
5. 영상 URL 후보를 찾지 못하면 `MEDIA_NOT_FOUND`로 처리한다.
6. 후보 URL 검증에 실패하면 `VIDEO_URL_EXPIRED` 또는 `MEDIA_NOT_FOUND`로 처리한다.
7. 다운로드 중 실패하면 `DOWNLOAD_FAILED`로 처리한다.

## 9. 화면 상세

### 9.1 메인 화면

MVP에서는 단일 화면으로 구성한다.

필수 구성 요소:

* 상단 앱 이름
* URL 입력 필드
* URL 초기화 버튼
* 다운로드 버튼
* 다운로드 상태 영역
* 진행률 표시 영역
* 실패 시 원본 URL 열기 버튼
* 정책 안내 문구

### 9.2 상태별 UI

#### 초기 상태

* URL 입력 필드가 비어 있음
* 다운로드 버튼 비활성화
* 안내 문구 표시

#### URL 입력 상태

* 지원 가능 도메인이면 다운로드 버튼 활성화
* 지원하지 않는 도메인이면 오류 메시지 표시

#### 페이지 확인 상태

* 다운로드 버튼 비활성화
* `게시물 페이지를 확인하는 중입니다.` 메시지 표시

#### 영상 추출 상태

* `영상 주소를 찾는 중입니다.` 메시지 표시

#### 다운로드 중 상태

* 진행률 표시
* `다운로드 중입니다.` 메시지 표시

#### 저장 완료 상태

* `갤러리에 저장되었습니다.` 메시지 표시
* 저장 파일명 또는 폴더명 표시

#### 실패 상태

* 실패 원인을 한국어로 표시
* URL 입력값은 유지
* 필요 시 `원본 게시물 열기` 버튼 표시

## 10. Android 앱 구조

### 10.1 권장 패키지 구조

```text
com.example.videodownaloder
├── MainActivity.kt
├── app
│   └── SnsDownloaderApp.kt
├── data
│   ├── extract
│   │   ├── AppExtractor.kt
│   │   ├── ExtractResult.kt
│   │   ├── PageFetcher.kt
│   │   ├── VideoUrlVerifier.kt
│   │   └── platform
│   │       ├── InstagramExtractor.kt
│   │       ├── PlatformExtractor.kt
│   │       └── ThreadsExtractor.kt
│   ├── download
│   │   ├── DownloadProgress.kt
│   │   └── VideoDownloader.kt
│   └── media
│       └── MediaStoreSaver.kt
├── domain
│   ├── DownloadRepository.kt
│   ├── SnsPlatform.kt
│   ├── SupportedUrl.kt
│   └── UrlValidator.kt
├── ui
│   ├── download
│   │   ├── DownloadScreen.kt
│   │   ├── DownloadUiState.kt
│   │   └── DownloadViewModel.kt
│   └── theme
└── util
    ├── ErrorMessageMapper.kt
    └── FileNameSanitizer.kt
```

### 10.2 역할 분리

* `DownloadScreen`: Compose UI와 사용자 입력 처리
* `DownloadViewModel`: 화면 상태 관리, 다운로드 요청 시작
* `DownloadRepository`: 추출, 다운로드, 저장 흐름 조율
* `UrlValidator`: Instagram/Threads URL 1차 검증
* `PageFetcher`: 공개 게시물 HTML 요청
* `AppExtractor`: 플랫폼별 extractor 선택
* `InstagramExtractor`: Instagram HTML/script/meta 분석
* `ThreadsExtractor`: Threads HTML/script/meta 분석
* `VideoUrlVerifier`: 후보 URL이 실제 영상 응답인지 확인
* `VideoDownloader`: MP4 스트림 다운로드
* `MediaStoreSaver`: 갤러리 저장 처리
* `ErrorMessageMapper`: 오류 코드를 사용자 메시지로 변환

## 11. 추출 전략

### 11.1 공통 원칙

앱 내부 추출기는 다음 순서로 영상 URL을 찾는다.

1. URL 정규화
2. 공개 페이지 HTML 요청
3. 로그인 또는 차단 페이지 여부 확인
4. JSON-LD script 검색
5. Next.js 또는 Relay 계열 script 데이터 검색
6. Open Graph meta 태그 검색
7. `.mp4`, `video_url`, `playable_url`, `video_versions` 같은 키워드 기반 후보 수집
8. 후보 URL 디코딩
9. 후보 URL 검증
10. 가장 적합한 MP4 URL 선택

### 11.2 Instagram 추출 후보

Instagram 공개 페이지에서 시도할 수 있는 후보:

* `application/ld+json`
* `og:video`
* `og:video:secure_url`
* script 내부 JSON payload
* `.mp4` CDN URL 패턴
* `video_url` 유사 필드

주의:

* Instagram은 로그인 유도, bot 탐지, HTML 구조 변경 가능성이 높다.
* 일부 공개 게시물도 앱 내부 HTTP 요청에서는 접근이 차단될 수 있다.
* 캐러셀 게시물은 MVP에서 첫 번째 영상 후보만 처리한다.

### 11.3 Threads 추출 후보

Threads 공개 페이지에서 시도할 수 있는 후보:

* `application/ld+json`
* Open Graph meta 태그
* script 내부 JSON payload
* `.mp4` CDN URL 패턴
* `video_url`, `media_url` 유사 필드

주의:

* Threads URL 구조와 페이지 렌더링 방식은 변경될 수 있다.
* 공식 API는 인증 기반이므로 임의 URL 다운로드 대체 수단으로 사용하지 않는다.

### 11.4 WebView 방식 보류

WebView에서 페이지를 렌더링한 뒤 DOM 또는 네트워크 요청을 분석하는 방식은 MVP에서 보류한다.

보류 이유:

* 구현 복잡도가 높음
* 로그인 쿠키와 사용자 계정 처리 문제가 생김
* 플랫폼 정책 리스크가 커짐
* 앱이 무거워짐

단, OkHttp 기반 추출 성공률이 너무 낮으면 후순위 실험 항목으로 검토한다.

## 12. 다운로드와 저장

### 12.1 다운로드 처리

MVP에서는 ViewModel의 coroutine에서 다운로드를 수행한다.
대용량 파일과 앱 종료 이후 작업 보장이 필요해지는 단계에서 WorkManager로 이관한다.

다운로드 처리 요구사항:

* HTTP 상태 코드 확인
* `Content-Type`이 `video/*` 또는 바이너리인지 확인
* `Content-Length`가 있으면 진행률 계산
* CDN URL 만료 또는 403 응답 처리
* 다운로드 실패 시 MediaStore pending 항목 정리

### 12.2 MediaStore 저장

Android 10 이상에서는 공용 미디어 컬렉션 저장에 `MediaStore`를 사용한다.
현재 minSdk가 35이므로 구버전 외부 저장소 권한 분기는 MVP에서 필요하지 않다.

저장 위치:

```text
Movies/SNSDownloader
```

저장 메타데이터:

* `DISPLAY_NAME`: 앱 생성 파일명
* `MIME_TYPE`: `video/mp4`
* `RELATIVE_PATH`: `Movies/SNSDownloader`
* `IS_PENDING`: 저장 중 `1`, 완료 후 `0`

### 12.3 권한

다운로드와 페이지 조회를 위해 다음 권한이 필요하다.

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

MVP 저장 동작에는 별도 저장소 쓰기 권한을 요청하지 않는다.
Android 13 이상에서 저장된 영상을 앱 내부에서 다시 읽거나 미리보기를 구현할 경우 `READ_MEDIA_VIDEO`를 검토한다.

## 13. 데이터 모델

### 13.1 ExtractResult

```kotlin
data class ExtractResult(
    val platform: SnsPlatform,
    val contentType: ContentType,
    val sourceUrl: String,
    val videoUrl: String,
    val title: String? = null,
    val thumbnailUrl: String? = null,
    val extension: String = "mp4"
)
```

### 13.2 ExtractError

```kotlin
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
    SaveFailed
}
```

### 13.3 DownloadUiState

```kotlin
data class DownloadUiState(
    val url: String = "",
    val status: DownloadStatus = DownloadStatus.Idle,
    val progress: Float? = null,
    val message: String? = null,
    val savedFileName: String? = null
)
```

```kotlin
enum class DownloadStatus {
    Idle,
    InvalidUrl,
    FetchingPage,
    Extracting,
    VerifyingVideo,
    Downloading,
    Saving,
    Completed,
    Failed
}
```

## 14. 오류 코드와 메시지

| 코드 | 의미 | 앱 표시 메시지 |
| --- | --- | --- |
| `UNSUPPORTED_URL` | 지원하지 않는 URL | 지원하지 않는 URL입니다. |
| `ACCESS_BLOCKED` | 페이지 요청 차단 | 게시물 페이지에 접근하지 못했습니다. |
| `LOGIN_REQUIRED` | 로그인 필요 | 로그인이 필요한 게시물은 지원하지 않습니다. |
| `PRIVATE_CONTENT` | 비공개 또는 접근 불가 | 비공개이거나 접근할 수 없는 게시물입니다. |
| `MEDIA_NOT_FOUND` | 영상 URL 추출 실패 | 영상 파일을 찾지 못했습니다. |
| `VIDEO_URL_EXPIRED` | CDN URL 만료 또는 접근 실패 | 영상 주소가 만료되었거나 접근할 수 없습니다. 다시 시도해주세요. |
| `PLATFORM_CHANGED` | 플랫폼 구조 변경 가능성 | 플랫폼 구조 변경으로 다운로드에 실패했을 수 있습니다. |
| `NETWORK_FAILED` | 네트워크 오류 | 네트워크 연결을 확인해주세요. |
| `DOWNLOAD_FAILED` | 파일 다운로드 실패 | 파일 다운로드에 실패했습니다. |
| `SAVE_FAILED` | 갤러리 저장 실패 | 갤러리 저장에 실패했습니다. |

## 15. 파일명 정책

### 15.1 기본 파일명

앱에서 다음 형식으로 생성한다.

```text
{platform}_{type}_{yyyyMMdd_HHmmss}.mp4
```

예시:

```text
instagram_reel_20260603_121000.mp4
threads_post_20260603_121500.mp4
```

### 15.2 파일명 정리

파일명에는 다음 처리를 적용한다.

* 공백은 `_`로 치환
* 파일 시스템에 부적합한 문자는 제거
* 확장자가 없으면 `.mp4` 추가
* 같은 이름이 있으면 `_1`, `_2` suffix 추가

## 16. 정책 안내 문구

앱 화면 하단 또는 다운로드 버튼 주변에 다음 문구를 표시한다.

```text
이 앱은 개인 보관 용도로만 사용해야 하며, 다운로드하는 콘텐츠에 대한 권리와 책임은 사용자에게 있습니다.
비공개 콘텐츠, 접근 제한 콘텐츠 또는 타인의 저작물을 무단 저장하거나 재배포하는 용도로 사용하지 마세요.
플랫폼 구조 변경이나 접근 제한으로 일부 공개 게시물도 다운로드되지 않을 수 있습니다.
```

## 17. 개발 단계

### 17.1 1단계: Android 화면 뼈대

작업:

* `Greeting` 기본 화면 제거
* 단일 다운로드 화면 생성
* URL 입력 필드 구현
* 다운로드 버튼 상태 처리
* 정책 안내 문구 표시

완료 기준:

* 앱 실행 시 다운로드 화면이 표시됨
* URL 입력에 따라 버튼 활성화 상태가 바뀜

### 17.2 2단계: URL 검증과 상태 모델

작업:

* `UrlValidator` 구현
* `DownloadUiState` 구현
* `ExtractResult`, `ExtractError` 모델 생성
* 오류 메시지 매핑 구현

완료 기준:

* 지원 도메인을 판별할 수 있음
* 실패 메시지가 한국어로 표시됨

### 17.3 3단계: 앱 내부 추출기 프로토타입

작업:

* `PageFetcher` 구현
* `InstagramExtractor` 구현
* `ThreadsExtractor` 구현
* 영상 URL 후보 수집 로직 구현
* 후보 URL 검증 구현

완료 기준:

* 테스트용 공개 게시물 URL에서 MP4 후보를 찾거나, 실패 원인을 명확히 반환함

### 17.4 4단계: MP4 다운로드와 저장

작업:

* 후보 URL에서 MP4 스트림 다운로드
* 진행률 계산
* MediaStore 저장 구현
* 저장 완료 메시지 표시

완료 기준:

* 실제 MP4 파일이 `Movies/SNSDownloader`에 저장됨
* 갤러리 앱에서 저장 파일 확인 가능

### 17.5 5단계: 안정화

작업:

* 네트워크 오류 처리
* 다운로드 중복 클릭 방지
* 파일명 충돌 처리
* 실패 URL 원본 열기 기능
* 기본 단위 테스트 추가

완료 기준:

* 주요 실패 케이스에서 앱이 종료되지 않음
* 사용자가 다음 행동을 이해할 수 있는 메시지를 받음

## 18. 테스트 계획

### 18.1 단위 테스트

대상:

* `UrlValidator`
* `FileNameSanitizer`
* `ErrorMessageMapper`
* `InstagramExtractor`
* `ThreadsExtractor`

검증:

* Instagram Reel URL 통과
* Instagram Post URL 통과
* Threads Post URL 통과
* 지원하지 않는 URL 실패
* HTML fixture에서 MP4 후보 추출
* 로그인 페이지 fixture 감지
* 차단 페이지 fixture 감지
* 오류 코드별 한국어 메시지 반환

### 18.2 통합 테스트

대상:

* 공개 페이지 요청 성공/실패
* 영상 URL 후보 검증
* 다운로드 성공 흐름
* 다운로드 실패 흐름
* MediaStore 저장 흐름

### 18.3 수동 테스트

체크리스트:

* 앱 최초 실행 화면 확인
* URL 붙여넣기
* 지원하지 않는 URL 입력
* Instagram 공개 Reel 다운로드 시도
* Instagram 공개 동영상 게시물 다운로드 시도
* Threads 공개 동영상 게시물 다운로드 시도
* 로그인 필요 URL 실패 메시지 확인
* 정상 MP4 다운로드
* 갤러리 저장 확인
* 앱 재실행 후 화면 상태 확인

## 19. 리스크와 대응

| 리스크 | 영향 | 대응 |
| --- | --- | --- |
| HTML 구조 변경 | 추출 실패 | extractor를 작게 분리하고 fixture 테스트 추가 |
| 로그인 유도 증가 | 공개 URL도 실패 가능 | `LOGIN_REQUIRED` 메시지 제공 |
| CDN URL 만료 | 다운로드 실패 | 추출 직후 즉시 다운로드, 실패 시 재시도 |
| Bot 탐지 또는 차단 | 페이지 접근 실패 | 요청 횟수 제한, 과도한 자동 반복 방지 |
| 앱 업데이트 필요 | 유지보수 부담 | 추출 로직을 모듈화 |
| 약관/저작권 리스크 | 배포 제한 | 개인 보관 목적 안내, 재배포 기능 제외 |

## 20. 구현 우선순위

| 우선순위 | 항목 | 이유 |
| --- | --- | --- |
| P0 | URL 입력 화면 | 모든 기능의 시작점 |
| P0 | URL 검증 | 빠른 사용자 피드백 |
| P0 | PageFetcher | 서버리스 구조의 핵심 |
| P0 | PlatformExtractor | MP4 URL 추출의 핵심 |
| P0 | MP4 다운로드 | 앱의 핵심 결과물 |
| P0 | MediaStore 저장 | 갤러리 저장 목표 달성 |
| P1 | 오류 메시지 정리 | 실패 상황에서 사용자 혼란 감소 |
| P1 | 진행률 표시 | 다운로드 중 상태 인지 |
| P1 | HTML fixture 테스트 | 플랫폼 변경 감지 |
| P2 | 클립보드 감지 | 사용성 개선 |
| P2 | 공유 인텐트 | SNS 앱에서 바로 사용 가능 |
| P2 | 다운로드 기록 | 반복 사용 편의 |

## 21. 다음 구현 권장 순서

1. `MainActivity`의 기본 `Greeting` UI를 다운로드 화면으로 교체한다.
2. URL 검증과 화면 상태 모델을 만든다.
3. mock extractor로 성공/실패 상태를 먼저 연결한다.
4. `PageFetcher`와 `InstagramExtractor` 프로토타입을 만든다.
5. `ThreadsExtractor` 프로토타입을 만든다.
6. MP4 다운로드와 MediaStore 저장을 붙인다.
7. 실제 공개 URL 테스트 결과에 따라 extractor 규칙을 보강한다.

이 순서로 진행하면 서버 없이 Android 앱만으로 MVP 가능성을 빠르게 검증할 수 있다. 단, 성공률은 플랫폼 상태에 크게 좌우되므로 첫 구현 목표는 “모든 공개 URL 다운로드”가 아니라 “일부 공개 영상 URL에서 성공하고, 실패 사유를 명확히 표시하는 것”으로 잡는다.
