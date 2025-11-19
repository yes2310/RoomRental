package com.example.bangbillija.model;

/**
 * 사용자 정보 모델
 * Firestore의 users 컬렉션에 저장됩니다.
 */
public class User {
    private final String userId;        // Firebase Auth UID
    private final String email;         // 이메일
    private final String name;          // 이름
    private final String studentId;     // 학번
    private final long createdAt;       // 가입 시간 (timestamp)

    public User(String userId, String email, String name, String studentId, long createdAt) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.studentId = studentId;
        this.createdAt = createdAt;
    }

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getStudentId() {
        return studentId;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
