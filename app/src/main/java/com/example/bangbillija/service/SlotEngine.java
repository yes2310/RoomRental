package com.example.bangbillija.service;

import com.example.bangbillija.model.Reservation;
import com.example.bangbillija.model.TimeSlot;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SlotEngine {

    private static final LocalTime DAY_START = LocalTime.of(9, 0);
    private static final LocalTime DAY_END = LocalTime.of(21, 0);

    private SlotEngine() {
    }

    public static List<TimeSlot> calculateDailySlots(LocalDate date, List<Reservation> reservations) {
        List<TimeSlot> slots = new ArrayList<>();
        reservations.sort(Comparator.comparing(Reservation::getStartTime));

        LocalTime cursor = DAY_START;
        for (Reservation reservation : reservations) {
            if (cursor.isBefore(reservation.getStartTime())) {
                slots.add(TimeSlot.available(date, cursor, reservation.getStartTime()));
            }
            slots.add(TimeSlot.fromReservation(reservation));
            cursor = reservation.getEndTime();
        }

        if (cursor.isBefore(DAY_END)) {
            slots.add(TimeSlot.available(date, cursor, DAY_END));
        }

        if (slots.isEmpty()) {
            slots.add(TimeSlot.available(date, DAY_START, DAY_END));
        }

        return slots;
    }
}
