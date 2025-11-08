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

    public TimetableEntry(String id, String courseName, String roomId, String roomName,
                          DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime,
                          int attendees, String professor, String note) {
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
