package com.example.bangbillija.data;

import com.example.bangbillija.model.Reservation;
import com.example.bangbillija.model.ReservationStatus;
import com.example.bangbillija.model.Room;
import com.example.bangbillija.model.RoomStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class FakeDataSource {

    private static final List<Room> ROOMS = Arrays.asList(
            new Room(
                    "room301",
                    "공학관",
                    "공학관 301호",
                    40,
                    "3층",
                    Arrays.asList("프로젝터", "와이파이", "화이트보드"),
                    RoomStatus.AVAILABLE
            ),
            new Room(
                    "room205",
                    "공학관",
                    "공학관 205호",
                    30,
                    "2층",
                    Arrays.asList("프로젝터", "화이트보드"),
                    RoomStatus.RESERVED
            ),
            new Room(
                    "room501",
                    "공학관",
                    "공학관 501호",
                    50,
                    "5층",
                    Arrays.asList("프로젝터", "와이파이", "스피커", "화이트보드"),
                    RoomStatus.AVAILABLE
            ),
            new Room(
                    "roomB102",
                    "공학관",
                    "공학관 B102호",
                    25,
                    "지하 1층",
                    Arrays.asList("컴퓨터", "와이파이"),
                    RoomStatus.AVAILABLE
            )
    );

    private static final List<Reservation> RESERVATIONS = Arrays.asList(
            new Reservation(
                    "RS-2025-0929-001",
                    "room301",
                    "공학관 301호",
                    "알고리즘 스터디",
                    "김민준",
                    LocalDate.of(2025, 9, 30),
                    LocalTime.of(10, 30),
                    LocalTime.of(12, 0),
                    15,
                    ReservationStatus.RESERVED,
                    "알고리즘 문제 풀이 및 코딩테스트 준비 스터디"
            ),
            new Reservation(
                    "RS-2025-0929-002",
                    "room301",
                    "공학관 301호",
                    "졸업 프로젝트 회의",
                    "박서연",
                    LocalDate.of(2025, 9, 30),
                    LocalTime.of(14, 30),
                    LocalTime.of(16, 0),
                    8,
                    ReservationStatus.RESERVED,
                    "졸업 프로젝트 팀 미팅"
            ),
            new Reservation(
                    "RS-2025-0929-003",
                    "room301",
                    "공학관 301호",
                    "AI 세미나 준비",
                    "이동현",
                    LocalDate.of(2025, 9, 30),
                    LocalTime.of(16, 0),
                    LocalTime.of(17, 30),
                    20,
                    ReservationStatus.PENDING,
                    "세미나 발표 준비"
            ),
            new Reservation(
                    "RS-2025-1002-001",
                    "room205",
                    "공학관 205호",
                    "졸업 프로젝트 발표 연습",
                    "최지훈",
                    LocalDate.of(2025, 10, 2),
                    LocalTime.of(14, 0),
                    LocalTime.of(16, 0),
                    6,
                    ReservationStatus.RESERVED,
                    "발표 리허설"
            ),
            new Reservation(
                    "RS-2025-1005-001",
                    "room501",
                    "공학관 501호",
                    "머신러닝 세미나",
                    "윤수연",
                    LocalDate.of(2025, 10, 5),
                    LocalTime.of(13, 0),
                    LocalTime.of(17, 0),
                    30,
                    ReservationStatus.PENDING,
                    "외부 초청 세미나"
            ),
            new Reservation(
                    "RS-2025-0925-001",
                    "room301",
                    "공학관 301호",
                    "네트워크 스터디",
                    "정유나",
                    LocalDate.of(2025, 9, 25),
                    LocalTime.of(18, 0),
                    LocalTime.of(19, 30),
                    12,
                    ReservationStatus.CANCELLED,
                    "노쇼로 인해 취소"
            )
    );

    private FakeDataSource() {
    }

    public static List<Room> getRooms() {
        return ROOMS;
    }

    public static List<Reservation> getReservations() {
        return RESERVATIONS;
    }

    public static List<Reservation> getReservationsForRoom(String roomId) {
        return RESERVATIONS.stream()
                .filter(reservation -> reservation.getRoomId().equals(roomId))
                .collect(Collectors.toList());
    }

    public static List<Reservation> getReservationsByStatus(ReservationStatus status) {
        return RESERVATIONS.stream()
                .filter(reservation -> reservation.getStatus() == status)
                .collect(Collectors.toList());
    }

    public static List<Reservation> getReservationsAfter(LocalDate date) {
        return RESERVATIONS.stream()
                .filter(reservation -> !reservation.getDate().isBefore(date))
                .collect(Collectors.toList());
    }

    public static List<Reservation> getReservationsBefore(LocalDate date) {
        return RESERVATIONS.stream()
                .filter(reservation -> reservation.getDate().isBefore(date))
                .collect(Collectors.toList());
    }
}
