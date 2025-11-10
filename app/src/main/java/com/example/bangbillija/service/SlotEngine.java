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
    private static final int SLOT_DURATION_HOURS = 2;

    // Fixed 2-hour time slots
    private static final List<TimeSlotTemplate> FIXED_SLOTS = List.of(
            new TimeSlotTemplate(LocalTime.of(9, 0), LocalTime.of(11, 0)),
            new TimeSlotTemplate(LocalTime.of(11, 0), LocalTime.of(13, 0)),
            new TimeSlotTemplate(LocalTime.of(13, 0), LocalTime.of(15, 0)),
            new TimeSlotTemplate(LocalTime.of(15, 0), LocalTime.of(17, 0)),
            new TimeSlotTemplate(LocalTime.of(17, 0), LocalTime.of(19, 0)),
            new TimeSlotTemplate(LocalTime.of(19, 0), LocalTime.of(21, 0))
    );

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

        // 고정된 2시간 슬롯에 대해 확인
        for (TimeSlotTemplate template : FIXED_SLOTS) {
            // 해당 슬롯과 겹치는 블록 찾기
            TimeBlock overlappingBlock = findOverlappingBlock(template, blockedTimes);

            if (overlappingBlock != null) {
                // 겹치는 예약/수업이 있으면 해당 슬롯 추가
                slots.add(overlappingBlock.getSlot());
            } else {
                // 비어있으면 예약 가능 슬롯 추가
                slots.add(TimeSlot.available(date, template.start, template.end));
            }
        }

        return slots;
    }

    private static TimeBlock findOverlappingBlock(TimeSlotTemplate template, List<TimeBlock> blockedTimes) {
        for (TimeBlock block : blockedTimes) {
            // 겹치는지 확인: 블록의 시작이 슬롯 끝 전이고, 블록의 끝이 슬롯 시작 후
            if (block.getStart().isBefore(template.end) && block.getEnd().isAfter(template.start)) {
                return block;
            }
        }
        return null;
    }

    // 내부 헬퍼 클래스: 고정 시간 슬롯 템플릿
    private static class TimeSlotTemplate {
        private final LocalTime start;
        private final LocalTime end;

        TimeSlotTemplate(LocalTime start, LocalTime end) {
            this.start = start;
            this.end = end;
        }
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
