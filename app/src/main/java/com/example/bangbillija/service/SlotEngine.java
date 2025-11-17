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

    /**
     * 실제 예약과 수업 시간을 기반으로 TimeSlot 목록을 생성합니다.
     * 30분 단위 예약을 지원하며, 빈 시간은 자동으로 "예약 가능" 슬롯으로 채워집니다.
     */
    public static List<TimeSlot> calculateDailySlots(LocalDate date, List<Reservation> reservations, List<TimetableEntry> allTimetableEntries) {
        List<TimeSlot> slots = new ArrayList<>();

        // 해당 날짜의 요일 추출
        DayOfWeek dayOfWeek = date.getDayOfWeek();

        // 해당 요일의 시간표만 필터링
        List<TimetableEntry> todayTimetable = allTimetableEntries.stream()
                .filter(entry -> entry.getDayOfWeek() == dayOfWeek)
                .collect(Collectors.toList());

        // 예약과 시간표를 합쳐서 "사용 중인 시간 블록"으로 처리
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

        // 시간 순으로 정렬
        blockedTimes.sort(Comparator.comparing(TimeBlock::getStart));

        // 빈 시간을 찾아서 "예약 가능" 슬롯으로 채우기
        LocalTime currentTime = DAY_START;

        for (TimeBlock block : blockedTimes) {
            // 현재 시간과 블록 시작 사이에 빈 시간이 있으면 예약 가능 슬롯 추가
            if (currentTime.isBefore(block.getStart())) {
                slots.add(TimeSlot.available(date, currentTime, block.getStart()));
            }

            // 블록된 시간 추가 (예약 또는 수업)
            slots.add(block.getSlot());

            // 현재 시간을 블록 끝으로 이동
            currentTime = block.getEnd().isAfter(currentTime) ? block.getEnd() : currentTime;
        }

        // 마지막 블록 이후부터 운영 종료 시간까지 빈 시간이 있으면 추가
        if (currentTime.isBefore(DAY_END)) {
            slots.add(TimeSlot.available(date, currentTime, DAY_END));
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
