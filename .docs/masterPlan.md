# URL 기반 SNS 영상 다운로드 앱 기획서

## 1. 프로젝트 개요

인스타그램과 Threads의 공개 게시물 URL을 입력하면, 해당 게시물의 동영상을 MP4 파일로 다운로드하여 안드로이드 기기 갤러리에 저장하는 개인용 앱을 만든다.

앱의 목표는 복잡한 조작 없이 URL 붙여넣기 → 다운로드 → 저장 완료까지 한 번에 처리하는 것이다.

## 2. 지원 범위

### 1차 지원

* Instagram Reels
* Instagram 공개 동영상 게시물
* Threads 공개 동영상 게시물

### 1차 제외

* 비공개 계정 게시물
* 로그인이 필요한 게시물
* 스토리
* 라이브 영상
* 여러 미디어가 섞인 캐러셀 전체 다운로드
* 저작권 보호 또는 접근 제한 콘텐츠
* 타인 콘텐츠 재배포 기능

## 3. 핵심 사용자 시나리오

사용자는 Instagram 또는 Threads 앱에서 게시물 링크를 복사한다.

앱을 실행한다.

URL 입력창에 링크를 붙여넣는다.

“다운로드” 버튼을 누른다.

앱이 서버에 URL을 전달한다.

서버가 다운로드 가능한 MP4 URL을 추출한다.

앱이 파일을 다운로드한다.

다운로드된 영상은 `Movies/SNSDownloader` 폴더에 저장된다.

완료 후 “갤러리에 저장됨” 메시지를 표시한다.

## 4. 주요 기능

### URL 입력 기능

* URL 직접 입력
* 클립보드 URL 자동 감지
* Android 공유 메뉴에서 URL 받기

### URL 판별 기능

* Instagram URL 여부 확인
* Threads URL 여부 확인
* 지원하지 않는 URL이면 오류 메시지 표시

### 영상 추출 기능

* 서버에서 URL 분석
* 공개 게시물 HTML 또는 메타데이터 조회
* MP4 원본 URL 추출
* 실패 시 원인 코드 반환

### 다운로드 기능

* 백그라운드 다운로드
* 다운로드 진행률 표시
* 중복 파일명 자동 처리
* MediaStore를 이용한 갤러리 저장

### 다운로드 기록

* 다운로드한 URL
* 저장 파일명
* 저장 시각
* 성공/실패 여부
* 플랫폼 구분

## 5. 시스템 구조

Android 앱은 UI와 파일 저장을 담당한다.

백엔드 서버는 URL 분석과 영상 URL 추출을 담당한다.

앱에서 Instagram/Threads를 직접 파싱하지 않고 서버를 두는 이유는, 플랫폼 구조가 바뀌었을 때 앱 업데이트 없이 서버 코드만 수정하기 위해서다.

## 6. 기술 스택

### Android

* Kotlin
* Jetpack Compose
* Retrofit 또는 OkHttp
* WorkManager
* MediaStore
* Room Database

### Backend

* Python
* FastAPI
* httpx
* BeautifulSoup 또는 selectolax
* Docker

### 배포

* 개인 서버
* Oracle Cloud Free Tier
* Fly.io
* Render
* Cloudflare Tunnel

## 7. API 설계

### POST /extract

요청:

```json
{
  "url": "https://www.instagram.com/reel/xxxxx/"
}
```

응답 성공:

```json
{
  "success": true,
  "platform": "instagram",
  "type": "reel",
  "title": "instagram_reel_xxxxx",
  "video_url": "https://...",
  "extension": "mp4"
}
```

응답 실패:

```json
{
  "success": false,
  "error_code": "UNSUPPORTED_URL",
  "message": "지원하지 않는 URL입니다."
}
```

## 8. 오류 코드

* `UNSUPPORTED_URL`: 지원하지 않는 URL
* `PRIVATE_CONTENT`: 비공개 또는 접근 불가 게시물
* `LOGIN_REQUIRED`: 로그인 필요
* `MEDIA_NOT_FOUND`: 영상 URL 추출 실패
* `RATE_LIMITED`: 요청 제한
* `PLATFORM_CHANGED`: 플랫폼 구조 변경 가능성
* `DOWNLOAD_FAILED`: 파일 다운로드 실패

## 9. MVP 범위

MVP에서는 기능을 최대한 줄인다.

* URL 붙여넣기
* Instagram 공개 Reel 다운로드
* Threads 공개 동영상 다운로드
* MP4 저장
* 다운로드 성공/실패 표시

공유 인텐트, 기록 관리, 여러 파일 다운로드, 캐시, 로그인 지원은 후순위로 둔다.

## 10. 개발 단계

### 1단계: 백엔드 프로토타입

* FastAPI 서버 생성
* `/extract` API 구현
* Instagram Reel URL 입력 테스트
* Threads URL 입력 테스트
* MP4 URL 추출 성공 여부 확인

### 2단계: Android MVP

* URL 입력 화면
* 다운로드 버튼
* API 호출
* MP4 다운로드
* MediaStore 저장

### 3단계: 사용성 개선

* 클립보드 URL 자동 감지
* 공유 메뉴 연동
* 다운로드 진행률 표시
* 다운로드 기록 저장

### 4단계: 안정화

* 실패 케이스별 메시지 정리
* 서버 로그 추가
* User-Agent 설정
* 재시도 처리
* 플랫폼 구조 변경 대응

## 11. 운영 리스크

Instagram과 Threads는 Meta 서비스이므로 자동 수집, 스크래핑, 비공식 접근이 제한될 수 있다.

따라서 이 프로젝트는 개인 학습 및 개인 사용 목적에 맞춘다.

배포 앱으로 확장할 경우 다음 리스크가 있다.

* 플랫폼 약관 위반 가능성
* IP 차단
* 봇 탐지
* HTML 구조 변경
* 다운로드 실패 증가
* 저작권 문제
* Google Play 심사 거절 가능성

## 12. 권장 정책

앱 내에 다음 안내를 표시한다.

“이 앱은 개인 보관 용도로만 사용해야 하며, 사용자는 다운로드하는 콘텐츠에 대한 권리와 책임을 직접 확인해야 합니다.”

“비공개 콘텐츠, 타인의 저작물을 무단 저장·재배포하는 용도로 사용하지 마세요.”

## 13. 향후 확장 기능

* 공유 메뉴에서 바로 다운로드
* 여러 URL 일괄 다운로드
* 썸네일 미리보기
* 파일명 자동 생성
* 플랫폼별 폴더 분리
* 다운로드 실패 시 재시도
* PC용 웹 UI
* Telegram bot 연동
* yt-dlp 기반 백엔드 모드 추가

## 14. 결론

이 프로젝트는 개인용 Android 앱으로 시작하기에 적합하다. 핵심은 Android 앱보다 백엔드 추출 로직이며, 플랫폼 구조 변경에 자주 대응해야 한다.

초기 목표는 “공개 URL 하나를 넣으면 MP4 하나가 저장된다”로 제한하는 것이 가장 현실적이다.
