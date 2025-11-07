package com.example.bangbillija.service;

import com.example.bangbillija.model.Reservation;
import com.example.bangbillija.model.TimeSlot;
import com.example.bangbillija.model.TimetableEntry;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class SlotEngine {

    private static final LocalTime DAY_START = LocalTime.of(9, 0);
    private static final LocalTime DAY_END = LocalTime.of(21, 0);

    private SlotEngine() {
    }

    public static List<TimeSlot> calculateDailySlots(LocalDate date, List<Reservation> reservations) {
        return calculateDailySlots(date, reservations, new ArrayList<>());
    }

    public static List<TimeSlot> calculateDailySlots(LocalDate date, List<Reservation> reservations, List<TimetableEntry> allTimetableEntries) {
        List<TimeSlot> slots = new ArrayList<>();

        // 해당 날짜의 요일 추출
        DayOfWeek dayOfWeek = date.getDayOfWeek();

        // 해당 요일의 시간표만 필터링
        List<TimetableEntry> todayTimetable = allTimetableEntries.stream()
                .filter(entry -> entry.getDayOfWeek() == dayOfWeek)
                .collect(Collectors.toList());

        // 예약과 시간표를 합쳐서 "사용 중인 시간"으로 처리
        List<TimeBlock> blockedTimes = new ArrayList<>();

        // 예약 추가
        for (Reservation reservation : reservations) {
            blockedTimes.add(new TimeBlock(
                    reservation.getStartTime(),
                    reservation.getEndTime(),
                    TimeSlot.fromReservation(reservation)
            ));
        }

        // 시간표 추가 (수업 시간)
        for (TimetableEntry entry : todayTimetable) {
            blockedTimes.add(new TimeBlock(
                    entry.getStartTime(),
                    entry.getEndTime(),
                    TimeSlot.forCourse(date, entry)
            ));
        }

        // 시작 시간 순으로 정렬
        blockedTimes.sort(Comparator.comparing(TimeBlock::getStart));

        // 빈 시간과 사용 중인 시간을 계산
        LocalTime cursor = DAY_START;
        for (TimeBlock block : blockedTimes) {
            if (cursor.isBefore(block.getStart())) {
                // 빈 시간 추가
                slots.add(TimeSlot.available(date, cursor, block.getStart()));
            }
            // 사용 중인 시간 추가
            slots.add(block.getSlot());
            cursor = block.getEnd();
        }

        // 마지막 블록 이후 남은 시간
        if (cursor.isBefore(DAY_END)) {
            slots.add(TimeSlot.available(date, cursor, DAY_END));
        }

        // 비어있으면 전체 시간을 사용 가능으로 추가
        if (slots.isEmpty()) {
            slots.add(TimeSlot.available(date, DAY_START, DAY_END));
        }

        return slots;
    }

    // 내부 헬퍼 클래스: 시간 블록 (예약 또는 시간표)
    private static class TimeBlock {
        private final LocalTime start;
        private final LocalTime end;
        private final TimeSlot slot;

        TimeBlock(LocalTime start, LocalTime end, TimeSlot slot) {
            this.start = start;
            this.end = end;
            this.slot = slot;
        }

        LocalTime getStart() {
            return start;
        }

        LocalTime getEnd() {
            return end;
        }

        TimeSlot getSlot() {
            return slot;
        }
    }
}
