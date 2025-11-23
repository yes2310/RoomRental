package com.example.bangbillija.model;

import java.time.DayOfWeek;
import java.time.LocalTime;

public class TimetableEntry {
    private final String id;
    private final String courseName;
    private final String roomId;
    private final String roomName;
    private final DayOfWeek dayOfWeek;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final int attendees;
    private final String professor;
    private final String note;
    private final String semester; // 학기 정보 (예: "2024-1", "2024-2")

    public TimetableEntry(String id, String courseName, String roomId, String roomName,
                          DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime,
                          int attendees, String professor, String note, String semester) {
        this.id = id;
        this.courseName = courseName;
        this.roomId = roomId;
        this.roomName = roomName;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.attendees = attendees;
        this.professor = professor;
        this.note = note;
        this.semester = semester != null ? semester : getCurrentSemester();
    }

    // 현재 학기 자동 계산 (3-8월: 1학기, 9-2월: 2학기)
    private static String getCurrentSemester() {
        java.time.LocalDate now = java.time.LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        if (month >= 3 && month <= 8) {
            return year + "-1";
        } else {
            return year + "-2";
        }
    }

    public String getId() {
        return id;
    }

    public String getCourseName() {
        return courseName;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public int getAttendees() {
        return attendees;
    }

    public String getProfessor() {
        return professor;
    }

    public String getNote() {
        return note;
    }

    public String getSemester() {
        return semester;
    }

    public String getDayOfWeekKorean() {
        switch (dayOfWeek) {
            case MONDAY: return "월";
            case TUESDAY: return "화";
            case WEDNESDAY: return "수";
            case THURSDAY: return "목";
            case FRIDAY: return "금";
            case SATURDAY: return "토";
            case SUNDAY: return "일";
            default: return "";
        }
    }
}
