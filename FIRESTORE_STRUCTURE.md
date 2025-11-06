# Firestore 데이터베이스 구조

## Collections

### 1. `rooms` Collection
강의실 정보를 저장합니다.

**Document ID**: 강의실 고유 ID (예: "room301")

**Fields**:
```
{
  "id": String,              // 강의실 ID
  "building": String,        // 건물명 (예: "공학관")
  "name": String,            // 강의실명 (예: "공학관 301호")
  "capacity": Number,        // 수용 인원
  "floor": String,           // 층수 (예: "3층")
  "facilities": Array<String>, // 시설 목록 (예: ["프로젝터", "와이파이"])
  "status": String,          // 상태 ("AVAILABLE", "RESERVED", "MAINTENANCE")
  "createdAt": Timestamp,    // 생성 시간
  "updatedAt": Timestamp     // 수정 시간
}
```

### 2. `reservations` Collection
예약 정보를 저장합니다.

**Document ID**: Firestore 자동 생성 ID

**Fields**:
```
{
  "id": String,              // 예약 ID (RS-YYYYMMDD-XXX 형식)
  "roomId": String,          // 강의실 ID
  "roomName": String,        // 강의실명 (비정규화)
  "title": String,           // 예약 제목
  "owner": String,           // 예약자 이름
  "ownerEmail": String,      // 예약자 이메일
  "userId": String,          // 예약자 Firebase UID
  "date": String,            // 예약 날짜 (ISO 형식: "2025-09-30")
  "startTime": String,       // 시작 시간 ("14:30")
  "endTime": String,         // 종료 시간 ("16:00")
  "attendees": Number,       // 참석 인원
  "status": String,          // 상태 ("PENDING", "RESERVED", "CHECKED_IN", "CANCELLED", "COMPLETED")
  "note": String,            // 예약 목적/메모
  "createdAt": Timestamp,    // 생성 시간
  "updatedAt": Timestamp     // 수정 시간
}
```

## Indexes (필요시 Firebase Console에서 생성)

1. **reservations**:
   - `roomId` + `date` (복합 인덱스)
   - `userId` + `date` (복합 인덱스)
   - `status` + `date` (복합 인덱스)

## Security Rules (예시)

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // 모든 사용자가 rooms 읽기 가능
    match /rooms/{roomId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null; // 추후 관리자만 가능하도록 변경
    }

    // 예약은 본인 것만 수정/삭제 가능
    match /reservations/{reservationId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
      allow update, delete: if request.auth != null &&
        resource.data.userId == request.auth.uid;
    }
  }
}
```
