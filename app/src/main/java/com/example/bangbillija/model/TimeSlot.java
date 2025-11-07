package com.example.bangbillija.model;

import java.time.LocalDate;
import java.time.LocalTime;

public class TimeSlot {

    private final LocalDate date;
    private final LocalTime start;
    private final LocalTime end;
    private final ReservationStatus status;
    private final Reservation reservation;
    private final TimetableEntry timetableEntry;

    private TimeSlot(LocalDate date, LocalTime start, LocalTime end,
                     ReservationStatus status, Reservation reservation, TimetableEntry timetableEntry) {
        this.date = date;
        this.start = start;
        this.end = end;
        this.status = status;
        this.reservation = reservation;
        this.timetableEntry = timetableEntry;
    }

    public static TimeSlot available(LocalDate date, LocalTime start, LocalTime end) {
        return new TimeSlot(date, start, end, ReservationStatus.AVAILABLE, null, null);
    }

    public static TimeSlot fromReservation(Reservation reservation) {
        return new TimeSlot(reservation.getDate(), reservation.getStartTime(),
                reservation.getEndTime(), reservation.getStatus(), reservation, null);
    }

    public static TimeSlot forCourse(LocalDate date, TimetableEntry entry) {
        return new TimeSlot(date, entry.getStartTime(), entry.getEndTime(),
                ReservationStatus.RESERVED, null, entry);
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getStart() {
        return start;
    }

    public LocalTime getEnd() {
        return end;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public TimetableEntry getTimetableEntry() {
        return timetableEntry;
    }

    public String getDisplayTime() {
        return String.format("%s - %s", start, end);
    }

    public String getDisplayLabel() {
        if (status == ReservationStatus.AVAILABLE) {
            return "예약 가능";
        }

        // 시간표 (수업 시간)
        if (timetableEntry != null) {
            return timetableEntry.getCourseName() + " (수업)";
        }

        // 예약
        if (reservation == null) {
            return status.name();
        }
        switch (status) {
            case RESERVED:
                return reservation.getTitle();
            case PENDING:
                return reservation.getTitle() + " (승인 대기)";
            case CHECKED_IN:
                return reservation.getTitle() + " (체크인 완료)";
            case CANCELLED:
                return reservation.getTitle() + " (취소됨)";
            default:
                return reservation.getTitle();
        }
    }

    public String getMetaInfo() {
        // 시간표 (수업 시간)
        if (timetableEntry != null) {
            String info = "";
            if (timetableEntry.getProfessor() != null && !timetableEntry.getProfessor().isEmpty()) {
                info = timetableEntry.getProfessor();
            }
            info += (info.isEmpty() ? "" : " • ") + timetableEntry.getAttendees() + "명";
            return info;
        }

        // 예약
        if (reservation == null) {
            return "";
        }
        return reservation.getOwner() + " • " + reservation.getAttendees() + "명";
    }
}
