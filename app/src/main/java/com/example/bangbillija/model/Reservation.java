package com.example.bangbillija.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

public class Reservation {

    private final String id;
    private final String roomId;
    private final String roomName;
    private final String title;
    private final String owner;
    private final LocalDate date;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final int attendees;
    private final ReservationStatus status;
    private final String note;

    public Reservation(String id, String roomId, String roomName, String title, String owner,
                       LocalDate date, LocalTime startTime, LocalTime endTime,
                       int attendees, ReservationStatus status, String note) {
        this.id = id;
        this.roomId = roomId;
        this.roomName = roomName;
        this.title = title;
        this.owner = owner;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.attendees = attendees;
        this.status = status;
        this.note = note;
    }

    public String getId() {
        return id;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getTitle() {
        return title;
    }

    public String getOwner() {
        return owner;
    }

    public LocalDate getDate() {
        return date;
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

    public ReservationStatus getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }

    public String getFormattedTime() {
        return String.format("%s - %s", startTime, endTime);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Reservation)) return false;
        Reservation that = (Reservation) o;
        return attendees == that.attendees
                && Objects.equals(id, that.id)
                && Objects.equals(roomId, that.roomId)
                && Objects.equals(roomName, that.roomName)
                && Objects.equals(title, that.title)
                && Objects.equals(owner, that.owner)
                && Objects.equals(date, that.date)
                && Objects.equals(startTime, that.startTime)
                && Objects.equals(endTime, that.endTime)
                && status == that.status
                && Objects.equals(note, that.note);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, roomId, roomName, title, owner, date, startTime, endTime, attendees, status, note);
    }
}
