# Firebase Cloud Functions 설정 가이드

이 가이드는 방빌리자 앱에서 관리자에게 푸시 알림을 보내기 위한 Firebase Cloud Functions 설정 방법을 안내합니다.

## 필수 요구사항

- Node.js 설치 (v14 이상 권장)
- Firebase CLI 설치
- Firebase 프로젝트 (이미 android 앱에 설정됨)

## 1. Firebase CLI 설치

```bash
npm install -g firebase-tools
```

## 2. Firebase 로그인

```bash
firebase login
```

## 3. Firebase 프로젝트 초기화

프로젝트 루트 디렉토리에서 실행:

```bash
firebase init functions
```

다음 옵션을 선택:
- **Use an existing project** → 기존 Firebase 프로젝트 선택
- **Language**: JavaScript
- **ESLint**: No (선택사항)
- **Install dependencies now**: Yes

## 4. Blaze 요금제 업그레이드

Cloud Functions는 Blaze(종량제) 요금제가 필요합니다.
- Firebase Console → 프로젝트 설정 → 요금제
- **무료 티어 포함** (월 2백만 함수 호출 무료)

## 5. Cloud Functions 코드 작성

`functions/index.js` 파일을 다음 내용으로 작성:

```javascript
const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();

/**
 * 예약 생성 시 관리자에게 푸시 알림 전송
 */
exports.notifyAdminOnNewReservation = functions.firestore
    .document('reservations/{reservationId}')
    .onCreate(async (snapshot, context) => {
        const reservation = snapshot.data();

        // 관리자 FCM 토큰 조회
        const usersSnapshot = await admin.firestore()
            .collection('users')
            .where('isAdmin', '==', true)
            .get();

        const tokens = [];
        usersSnapshot.forEach(doc => {
            const token = doc.data().fcmToken;
            if (token) {
                tokens.push(token);
            }
        });

        if (tokens.length === 0) {
            console.log('No admin tokens found');
            return null;
        }

        // 푸시 알림 메시지 구성
        const message = {
            data: {
                title: '새 예약 알림',
                body: `${reservation.roomName} - ${reservation.date} ${reservation.startTime}`,
                type: 'reservation'
            },
            tokens: tokens
        };

        // 멀티캐스트 메시지 전송
        const response = await admin.messaging().sendMulticast(message);
        console.log('Successfully sent messages:', response.successCount);

        return null;
    });

/**
 * 시간표 추가 시 관리자에게 푸시 알림 전송
 */
exports.notifyAdminOnNewTimetable = functions.firestore
    .document('timetable/{entryId}')
    .onCreate(async (snapshot, context) => {
        const entry = snapshot.data();

        // 관리자 FCM 토큰 조회
        const usersSnapshot = await admin.firestore()
            .collection('users')
            .where('isAdmin', '==', true)
            .get();

        const tokens = [];
        usersSnapshot.forEach(doc => {
            const token = doc.data().fcmToken;
            if (token) {
                tokens.push(token);
            }
        });

        if (tokens.length === 0) {
            return null;
        }

        const message = {
            data: {
                title: '새 시간표 등록',
                body: `${entry.courseName} - ${entry.roomName}`,
                type: 'timetable'
            },
            tokens: tokens
        };

        const response = await admin.messaging().sendMulticast(message);
        console.log('Successfully sent timetable notification:', response.successCount);

        return null;
    });

/**
 * 강의실 추가 시 관리자에게 푸시 알림 전송
 */
exports.notifyAdminOnNewRoom = functions.firestore
    .document('rooms/{roomId}')
    .onCreate(async (snapshot, context) => {
        const room = snapshot.data();

        // 관리자 FCM 토큰 조회
        const usersSnapshot = await admin.firestore()
            .collection('users')
            .where('isAdmin', '==', true)
            .get();

        const tokens = [];
        usersSnapshot.forEach(doc => {
            const token = doc.data().fcmToken;
            if (token) {
                tokens.push(token);
            }
        });

        if (tokens.length === 0) {
            return null;
        }

        const message = {
            data: {
                title: '새 강의실 등록',
                body: `${room.name} - ${room.building}`,
                type: 'room'
            },
            tokens: tokens
        };

        const response = await admin.messaging().sendMulticast(message);
        console.log('Successfully sent room notification:', response.successCount);

        return null;
    });
```

## 6. 의존성 확인

`functions/package.json`에 다음이 포함되어 있는지 확인:

```json
{
  "dependencies": {
    "firebase-admin": "^11.8.0",
    "firebase-functions": "^4.3.1"
  }
}
```

## 7. Cloud Functions 배포

```bash
cd functions
npm install
cd ..
firebase deploy --only functions
```

## 8. 배포 확인

Firebase Console → Functions 탭에서 배포된 함수 확인:
- `notifyAdminOnNewReservation`
- `notifyAdminOnNewTimetable`
- `notifyAdminOnNewRoom`

## 9. 테스트

1. 안드로이드 앱 실행
2. 새 예약 생성
3. 관리자 계정으로 로그인한 다른 디바이스에서 푸시 알림 수신 확인

## 비용

**무료 티어 (월간 한도):**
- 함수 호출: 2백만 회
- GB-초 메모리: 40만
- CPU-초: 20만
- 네트워크: 5GB

→ 소규모 앱은 **완전 무료**로 사용 가능합니다!

## 문제 해결

### 알림이 오지 않는 경우:

1. **Firebase Console → Functions → Logs** 확인
2. Firestore의 `users` 컬렉션에 관리자 FCM 토큰 저장 확인
3. Android 앱에서 FCM 토큰이 정상 저장되는지 Logcat 확인

### 함수 실행 실패:

```bash
firebase functions:log
```

로그를 확인하여 에러 메시지 분석

## 추가 기능 (선택사항)

### 예약 취소 알림 추가:

```javascript
exports.notifyAdminOnCanceledReservation = functions.firestore
    .document('reservations/{reservationId}')
    .onUpdate(async (change, context) => {
        const before = change.before.data();
        const after = change.after.data();

        // 상태가 CANCELLED로 변경된 경우
        if (before.status !== 'CANCELLED' && after.status === 'CANCELLED') {
            // 관리자에게 알림 전송 로직
            // ... (위와 동일한 패턴)
        }

        return null;
    });
```

## 참고 자료

- [Firebase Cloud Functions 공식 문서](https://firebase.google.com/docs/functions)
- [FCM 서버 API 문서](https://firebase.google.com/docs/cloud-messaging/server)
