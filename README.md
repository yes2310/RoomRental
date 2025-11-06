<div align="center">

# 🏫 방빌리자 (BangBilliJa)

### 대학교 강의실 스마트 예약 시스템

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)
[![Java](https://img.shields.io/badge/Language-Java%2011-orange.svg)](https://www.oracle.com/java/)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-yellow.svg)](https://firebase.google.com/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**언제 어디서나 간편하게 강의실을 예약하고 관리하세요** 📱

[Features](#-주요-기능) • [Screenshots](#-스크린샷) • [Tech Stack](#-기술-스택) • [Installation](#-설치-방법) • [Architecture](#-아키텍처)

---

</div>

## 📖 소개

**방빌리자**는 대학교 강의실 예약 및 관리를 위한 안드로이드 애플리케이션입니다.
기존의 불편한 오프라인 예약 방식을 개선하여 모바일에서 실시간으로 강의실을 조회하고 예약할 수 있습니다.

### 💡 개발 배경

- 🏃 행정실 방문 없이 강의실 예약 가능
- 👀 실시간 강의실 사용 현황 조회
- 📷 QR 체크인으로 no-show 방지 (10분 규칙)
- ☁️ Firebase 기반 실시간 데이터 동기화

---

## ✨ 주요 기능

### 🔐 인증 시스템
- Firebase Authentication 기반 로그인/회원가입
- 이메일 기반 관리자 권한 시스템
- 자동 로그인 유지

### 🏢 강의실 관리
- 📋 전체 강의실 목록 조회
- 🔍 검색 및 필터링 (건물명, 강의실명, 상태)
- 👨‍💼 관리자 전용 강의실 등록 기능
- 🏷️ 시설 정보 표시 (프로젝터, 와이파이 등)

### 📅 예약 시스템
- 📆 주간 캘린더 UI로 날짜 선택
- ⏰ 30분 단위 타임슬롯 자동 계산
- 📝 예약 생성/수정/취소
- 📊 내 예약 목록 (예정/과거/취소됨)

### 📷 QR 체크인
- 🎫 예약별 고유 QR 코드 자동 생성
- 📸 카메라 스캔으로 간편 체크인
- ⏱️ **10분 규칙**: 예약 시간 10분 전부터 체크인 가능
- 🚫 10분 지각 시 자동 취소

### 👤 사용자 프로필
- 계정 정보 조회
- 관리자 배지 표시
- 로그아웃 기능

---

## 📱 스크린샷

<div align="center">

### 로그인 & 강의실 목록
<img src="docs/screenshots/login.png" width="250" alt="로그인"> <img src="docs/screenshots/room_list.png" width="250" alt="강의실 목록"> <img src="docs/screenshots/room_filter.png" width="250" alt="필터링">

### 캘린더 & 예약 생성
<img src="docs/screenshots/calendar.png" width="250" alt="캘린더"> <img src="docs/screenshots/create_reservation.png" width="250" alt="예약 생성"> <img src="docs/screenshots/reservation_detail.png" width="250" alt="예약 상세">

### QR 체크인 & 프로필
<img src="docs/screenshots/qr_code.png" width="250" alt="QR 코드"> <img src="docs/screenshots/qr_scan.png" width="250" alt="QR 스캔"> <img src="docs/screenshots/profile.png" width="250" alt="프로필">

</div>

> 📸 스크린샷은 실제 앱 화면을 캡처하여 `docs/screenshots/` 폴더에 추가해주세요.

---

## 🛠 기술 스택

### Frontend
- **Language**: Java 11
- **UI Framework**: Material Components 3
- **View System**: View Binding
- **Architecture**: MVVM + Repository Pattern
- **Navigation**: Single Activity + Fragments
- **Async**: LiveData, ViewModel

### Backend
- **Authentication**: Firebase Authentication
- **Database**: Firebase Firestore
- **Analytics**: Firebase Analytics

### Libraries
| Library | Purpose | Version |
|---------|---------|---------|
| [ZXing](https://github.com/journeyapps/zxing-android-embedded) | QR 코드 생성/스캔 | 4.3.0 |
| AndroidX Lifecycle | ViewModel, LiveData | latest |
| Material Components | UI Components | latest |
| Firebase BOM | Firebase Services | 33.2.0 |

---

## 📦 설치 방법

### 사전 요구사항
- Android Studio Hedgehog (2023.1.1) 이상
- JDK 11 이상
- Android SDK (API 24 이상)
- Firebase 프로젝트 설정

### 설치 단계

1. **저장소 클론**
```bash
git clone https://github.com/yourusername/RoomRental.git
cd RoomRental
```

2. **Firebase 설정**
   - [Firebase Console](https://console.firebase.google.com/)에서 프로젝트 생성
   - Android 앱 등록 (패키지명: `com.example.bangbillija`)
   - `google-services.json` 다운로드 후 `app/` 디렉토리에 추가

3. **Firestore 데이터베이스 설정**
   - Firestore Database 생성 (테스트 모드)
   - Security Rules 설정 ([FIRESTORE_STRUCTURE.md](FIRESTORE_STRUCTURE.md) 참고)

4. **프로젝트 빌드**
```bash
./gradlew assembleDebug
```

5. **앱 실행**
   - Android Studio에서 `Run` 버튼 클릭
   - 또는 명령어 실행:
```bash
./gradlew installDebug
```

---

## 🏗 아키텍처

### MVVM + Repository Pattern

```
┌─────────────────────────────────────────────────┐
│                   UI Layer                       │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐      │
│  │Fragment 1│  │Fragment 2│  │Fragment 3│      │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘      │
│       └──────────────┴─────────────┘            │
│                      │                           │
│         ┌────────────▼────────────┐              │
│         │   SharedViewModel       │              │
│         └────────────┬────────────┘              │
└──────────────────────┼──────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────┐
│               Repository Layer                   │
│  ┌─────────────────┐  ┌──────────────────┐     │
│  │ RoomRepository  │  │ReservationRepo   │     │
│  └────────┬────────┘  └────────┬─────────┘     │
└───────────┼─────────────────────┼───────────────┘
            │                     │
┌───────────▼─────────────────────▼───────────────┐
│                Data Layer                        │
│  ┌─────────────────┐  ┌──────────────────┐     │
│  │ FirestoreManager│  │  AuthManager     │     │
│  └────────┬────────┘  └────────┬─────────┘     │
└───────────┼─────────────────────┼───────────────┘
            │                     │
            └──────────┬──────────┘
                       │
              ┌────────▼────────┐
              │    Firebase     │
              │   (Firestore)   │
              └─────────────────┘
```

### 프로젝트 구조

```
app/src/main/java/com/example/bangbillija/
├── core/                    # 공유 ViewModels
│   └── SharedReservationViewModel.java
├── data/                    # Repository Layer
│   ├── RoomRepository.java
│   ├── ReservationRepository.java
│   └── FakeDataSource.java
├── model/                   # 데이터 모델
│   ├── Room.java
│   ├── Reservation.java
│   ├── TimeSlot.java
│   └── *Status.java
├── service/                 # 비즈니스 로직
│   ├── AuthManager.java
│   ├── FirestoreManager.java
│   ├── SlotEngine.java
│   └── QrManager.java
├── ui/                      # UI Layer
│   ├── MainActivity.java
│   ├── auth/               # 인증 화면
│   ├── rooms/              # 강의실 목록
│   ├── calendar/           # 캘린더
│   ├── reservations/       # 예약 관리
│   └── checkin/            # QR 체크인
└── util/                    # 유틸리티
    ├── QRCodeUtil.java
    └── SimpleTextWatcher.java
```

---

## 🔥 Firebase 설정

### Firestore 컬렉션 구조

#### `rooms` Collection
```json
{
  "id": "room301",
  "building": "공학관",
  "name": "공학관 301호",
  "capacity": 50,
  "floor": "3층",
  "facilities": ["프로젝터", "와이파이"],
  "status": "AVAILABLE",
  "createdAt": "2025-01-09T10:00:00Z"
}
```

#### `reservations` Collection
```json
{
  "id": "RS-20250115-ABC",
  "roomId": "room301",
  "roomName": "공학관 301호",
  "title": "알고리즘 스터디",
  "owner": "홍길동",
  "userId": "firebase_uid",
  "date": "2025-01-15",
  "startTime": "14:00",
  "endTime": "16:00",
  "attendees": 30,
  "status": "RESERVED",
  "note": "중간고사 대비 스터디"
}
```

자세한 내용은 [FIRESTORE_STRUCTURE.md](FIRESTORE_STRUCTURE.md) 참고

---

## 👨‍💼 관리자 기능

### 관리자 계정 설정

`service/AuthManager.java`에서 관리자 이메일 목록을 설정할 수 있습니다:

```java
private static final List<String> ADMIN_EMAILS = Arrays.asList(
    "admin@bangbillija.com",
    "admin@example.com",
    "admin@admin.com"
);
```

### 관리자 전용 기능
- ➕ 강의실 등록
- ✏️ 강의실 정보 수정
- 🗑️ 강의실 삭제
- 📊 예약 통계 조회 (개발 예정)

---

## 🎯 주요 알고리즘

### 1. 타임슬롯 계산 엔진 (SlotEngine)

```java
// 운영 시간: 09:00 - 21:00
// 기존 예약과 빈 시간대를 병합하여 완전한 타임슬롯 생성
public List<TimeSlot> computeSlots(Room room, LocalDate date, List<Reservation> reservations)
```

### 2. QR 체크인 검증

```java
// 체크인 가능 시간: 예약 시간 10분 전 ~ 10분 후
long minutesDiff = Duration.between(reservationStartTime, now).toMinutes();

if (minutesDiff < -10) {
    // 너무 이름: 체크인 불가
} else if (minutesDiff > 10) {
    // 10분 초과: 자동 취소
} else {
    // 체크인 성공
}
```

---

## 🧪 테스트

### Unit Tests
```bash
./gradlew test
```

### Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

---

## 📝 커밋 컨벤션

```
feat: 새로운 기능 추가
fix: 버그 수정
docs: 문서 수정
style: 코드 포맷팅
refactor: 코드 리팩토링
test: 테스트 코드
chore: 빌드 설정, 패키지 매니저 등
```

---

## 🤝 기여하기

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'feat: Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 라이선스

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details

---

## 👥 팀원

| 이름 | 역할 | GitHub |
|------|------|--------|
| 팀원1 | Project Manager | [@github](https://github.com) |
| 팀원2 | Frontend Developer | [@github](https://github.com) |
| 팀원3 | Backend Developer | [@github](https://github.com) |

---

## 📞 연락처

프로젝트 관련 문의: [이메일 주소]

프로젝트 링크: [https://github.com/yourusername/RoomRental](https://github.com/yourusername/RoomRental)

---

## 🙏 Acknowledgments

- [Material Design](https://material.io/) - UI/UX Design
- [Firebase](https://firebase.google.com/) - Backend Services
- [ZXing](https://github.com/zxing/zxing) - QR Code Library
- [Android Developers](https://developer.android.com/) - Documentation

---

<div align="center">

**Made with ❤️ by 방빌리자 Team**

⭐ 이 프로젝트가 도움이 되었다면 Star를 눌러주세요!

</div>
