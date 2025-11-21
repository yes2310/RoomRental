package com.example.bangbillija.model;

import java.time.LocalDateTime;

public class Notification {
    private final String id;
    private final String title;
    private final String message;
    private final String type; // "reservation", "room", "timetable"
    private final String targetUserId; // 받을 사람 (관리자면 "admin")
    private final LocalDateTime timestamp;
    private final boolean read;
    private final String relatedId; // 예약ID, 강의실ID 등

    public Notification(String id, String title, String message, String type,
                        String targetUserId, LocalDateTime timestamp, boolean read, String relatedId) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.type = type;
        this.targetUserId = targetUserId;
        this.timestamp = timestamp;
        this.read = read;
        this.relatedId = relatedId;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getType() {
        return type;
    }

    public String getTargetUserId() {
        return targetUserId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public boolean isRead() {
        return read;
    }

    public String getRelatedId() {
        return relatedId;
    }
}
