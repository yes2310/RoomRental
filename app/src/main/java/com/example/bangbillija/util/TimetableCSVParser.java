package com.example.bangbillija.util;

import com.example.bangbillija.model.TimetableEntry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TimetableCSVParser {

    /**
     * CSV 파일에서 시간표 파싱
     * CSV 형식: 과목명,강의실ID,강의실명,요일,시작시간,종료시간,수강인원,교수명,비고
     * 예시: 알고리즘,room301,공학관 301호,월,09:00,10:30,40,김교수,중간고사 주의
     */
    public static List<TimetableEntry> parseCSV(InputStream inputStream, String semester) throws IOException, ParseException {
        List<TimetableEntry> entries = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        String line;
        int lineNumber = 0;
        boolean isFirstLine = true;

        while ((line = reader.readLine()) != null) {
            lineNumber++;

            // 첫 줄은 헤더이므로 건너뜀
            if (isFirstLine) {
                isFirstLine = false;
                continue;
            }

            // 빈 줄 건너뜀
            if (line.trim().isEmpty()) {
                continue;
            }

            try {
                TimetableEntry entry = parseLine(line, semester);
                entries.add(entry);
            } catch (Exception e) {
                throw new ParseException("Line " + lineNumber + ": " + e.getMessage(), lineNumber);
            }
        }

        reader.close();
        return entries;
    }

    /**
     * CSV 필드에서 앞뒤 공백과 모든 따옴표 제거
     */
    private static String cleanField(String field) {
        if (field == null) {
            return "";
        }
        // 앞뒤 공백 제거
        field = field.trim();
        // 모든 큰따옴표 제거
        field = field.replace("\"", "");
        // 모든 작은따옴표 제거
        field = field.replace("'", "");
        // 다시 trim
        return field.trim();
    }

    /**
     * 시간 문자열을 정규화 (9:00 -> 09:00)
     */
    private static String normalizeTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return timeStr;
        }
        // HH:mm 형식이 아니면 변환
        String[] parts = timeStr.split(":");
        if (parts.length == 2) {
            String hour = parts[0].length() == 1 ? "0" + parts[0] : parts[0];
            String minute = parts[1].length() == 1 ? "0" + parts[1] : parts[1];
            return hour + ":" + minute;
        }
        return timeStr;
    }

    private static TimetableEntry parseLine(String line, String semester) throws Exception {
        String[] parts = line.split(",");

        if (parts.length < 8) {
            throw new Exception("필드가 부족합니다. 최소 8개 필드 필요 (현재: " + parts.length + "개)");
        }

        // 각 필드에서 따옴표 제거 및 trim
        String courseName = cleanField(parts[0]);
        String roomId = cleanField(parts[1]);
        String roomName = cleanField(parts[2]);
        String dayStr = cleanField(parts[3]);
        String startTimeStr = cleanField(parts[4]);
        String endTimeStr = cleanField(parts[5]);
        String attendeesStr = cleanField(parts[6]);
        String professor = cleanField(parts[7]);
        String note = parts.length > 8 ? cleanField(parts[8]) : "";

        // 요일 파싱
        DayOfWeek dayOfWeek = parseDayOfWeek(dayStr);

        // 시간 파싱 (정규화 후)
        startTimeStr = normalizeTime(startTimeStr);
        endTimeStr = normalizeTime(endTimeStr);
        LocalTime startTime = LocalTime.parse(startTimeStr);
        LocalTime endTime = LocalTime.parse(endTimeStr);

        // 수강인원 파싱
        int attendees;
        try {
            attendees = Integer.parseInt(attendeesStr);
        } catch (NumberFormatException e) {
            throw new Exception("수강인원이 올바르지 않습니다: " + attendeesStr);
        }

        // 유효성 검사
        if (courseName.isEmpty()) {
            throw new Exception("과목명이 비어있습니다");
        }
        if (roomId.isEmpty()) {
            throw new Exception("강의실ID가 비어있습니다");
        }
        if (startTime.isAfter(endTime)) {
            throw new Exception("시작시간이 종료시간보다 늦습니다");
        }
        if (attendees <= 0) {
            throw new Exception("수강인원은 1명 이상이어야 합니다");
        }

        String id = UUID.randomUUID().toString();
        return new TimetableEntry(id, courseName, roomId, roomName, dayOfWeek,
                startTime, endTime, attendees, professor, note, semester);
    }

    private static DayOfWeek parseDayOfWeek(String dayStr) throws Exception {
        switch (dayStr) {
            case "월":
            case "MON":
            case "MONDAY":
                return DayOfWeek.MONDAY;
            case "화":
            case "TUE":
            case "TUESDAY":
                return DayOfWeek.TUESDAY;
            case "수":
            case "WED":
            case "WEDNESDAY":
                return DayOfWeek.WEDNESDAY;
            case "목":
            case "THU":
            case "THURSDAY":
                return DayOfWeek.THURSDAY;
            case "금":
            case "FRI":
            case "FRIDAY":
                return DayOfWeek.FRIDAY;
            case "토":
            case "SAT":
            case "SATURDAY":
                return DayOfWeek.SATURDAY;
            case "일":
            case "SUN":
            case "SUNDAY":
                return DayOfWeek.SUNDAY;
            default:
                throw new Exception("올바르지 않은 요일 형식: " + dayStr + " (월, 화, 수, 목, 금, 토, 일 중 입력)");
        }
    }

    public static class ParseException extends Exception {
        private final int lineNumber;

        public ParseException(String message, int lineNumber) {
            super(message);
            this.lineNumber = lineNumber;
        }

        public int getLineNumber() {
            return lineNumber;
        }
    }
}
