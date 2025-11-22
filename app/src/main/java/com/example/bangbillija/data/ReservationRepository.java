package com.example.bangbillija.data;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.bangbillija.model.Reservation;
import com.example.bangbillija.model.ReservationStatus;
import com.example.bangbillija.model.TimeSlot;
import com.example.bangbillija.model.TimetableEntry;
import com.example.bangbillija.service.AuthManager;
import com.example.bangbillija.service.FirestoreManager;
import com.example.bangbillija.service.SlotEngine;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReservationRepository {

    private static ReservationRepository instance;
    private final FirestoreManager firestoreManager = FirestoreManager.getInstance();
    private final AuthManager authManager = AuthManager.getInstance();
    private final TimetableRepository timetableRepository = TimetableRepository.getInstance();

    private final MutableLiveData<List<Reservation>> upcomingReservations = new MutableLiveData<>();
    private final MutableLiveData<List<Reservation>> pastReservations = new MutableLiveData<>();
    private final MutableLiveData<List<Reservation>> cancelledReservations = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private ListenerRegistration reservationsListener;

    private ReservationRepository() {
        startListening();
    }

    public static synchronized ReservationRepository getInstance() {
        if (instance == null) {
            instance = new ReservationRepository();
        }
        return instance;
    }

    public void buildSlotsFor(String roomId, LocalDate date, FirestoreManager.FirestoreCallback<List<TimeSlot>> callback) {
        // 예약 + 시간표 모두 가져오기
        firestoreManager.getReservationsForRoom(roomId, date, new FirestoreManager.FirestoreCallback<List<Reservation>>() {
            @Override
            public void onSuccess(List<Reservation> reservations) {
                // 시간표도 함께 가져오기
                firestoreManager.getTimetableEntriesForRoom(roomId, new FirestoreManager.FirestoreCallback<List<TimetableEntry>>() {
                    @Override
                    public void onSuccess(List<TimetableEntry> timetableEntries) {
                        // 예약 + 시간표를 고려하여 슬롯 계산
                        callback.onSuccess(SlotEngine.calculateDailySlots(date, reservations, timetableEntries));
                    }

                    @Override
                    public void onFailure(Exception e) {
                        // 시간표 로드 실패 시 예약만으로 계산
                        error.setValue("시간표 로드 실패: " + e.getMessage());
                        callback.onSuccess(SlotEngine.calculateDailySlots(date, reservations));
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                error.setValue(e.getMessage());
                callback.onFailure(e);
            }
        });
    }

    public void getReservationsByRoomAndDate(String roomId, LocalDate date, FirestoreManager.FirestoreCallback<List<Reservation>> callback) {
        firestoreManager.getReservationsByRoomAndDate(roomId, date, callback);
    }

    public LiveData<List<Reservation>> getUpcomingReservations() {
        return upcomingReservations;
    }

    public LiveData<List<Reservation>> getPastReservations() {
        return pastReservations;
    }

    public LiveData<List<Reservation>> getCancelledReservations() {
        return cancelledReservations;
    }

    public LiveData<String> getError() {
        return error;
    }

    /**
     * 실시간 리스너 시작 (다른 사용자의 변경사항 자동 반영)
     */
    private void startListening() {
        if (reservationsListener != null) {
            reservationsListener.remove();
        }

        reservationsListener = firestoreManager.listenToReservations(new FirestoreManager.FirestoreCallback<List<Reservation>>() {
            @Override
            public void onSuccess(List<Reservation> allReservations) {
                FirebaseUser user = authManager.currentUser();
                if (user == null) {
                    return;
                }

                // 관리자는 모든 예약, 일반 사용자는 자신의 예약만 필터링
                List<Reservation> filteredReservations;
                if (authManager.isAdmin()) {
                    filteredReservations = allReservations;
                } else {
                    filteredReservations = allReservations.stream()
                            .filter(r -> r.getOwner().equals(user.getUid()))
                            .collect(Collectors.toList());
                }

                processAndSetReservations(filteredReservations);
            }

            @Override
            public void onFailure(Exception e) {
                error.setValue(e.getMessage());
            }
        });
    }

    /**
     * 리스너 해제 (필요 시 호출)
     */
    public void stopListening() {
        if (reservationsListener != null) {
            reservationsListener.remove();
            reservationsListener = null;
        }
    }

    public void loadReservations() {
        // 실시간 리스너가 이미 실행 중이므로 별도 조회 불필요
        if (reservationsListener == null) {
            startListening();
        }
    }

    /**
     * 관리자용: 모든 사용자의 예약을 로드합니다.
     */
    private void loadAllReservations() {
        // 실시간 리스너가 자동으로 처리
    }

    /**
     * 일반 사용자용: 자신의 예약만 로드합니다.
     */
    private void loadUserReservations(String userId) {
        firestoreManager.getReservationsByUser(userId, new FirestoreManager.FirestoreCallback<List<Reservation>>() {
            @Override
            public void onSuccess(List<Reservation> allReservations) {
                processAndSetReservations(allReservations);
            }

            @Override
            public void onFailure(Exception e) {
                error.setValue(e.getMessage());
            }
        });
    }

    /**
     * 예약 목록을 날짜와 상태에 따라 분류합니다.
     */
    private void processAndSetReservations(List<Reservation> allReservations) {
        LocalDate today = LocalDate.now();

        List<Reservation> upcoming = allReservations.stream()
                .filter(r -> !r.getDate().isBefore(today) && r.getStatus() != ReservationStatus.CANCELLED)
                .sorted((r1, r2) -> {
                    int dateCompare = r2.getDate().compareTo(r1.getDate());
                    if (dateCompare != 0) return dateCompare;
                    return r2.getStartTime().compareTo(r1.getStartTime());
                })
                .collect(Collectors.toList());

        List<Reservation> past = allReservations.stream()
                .filter(r -> r.getDate().isBefore(today) && r.getStatus() != ReservationStatus.CANCELLED)
                .sorted((r1, r2) -> {
                    int dateCompare = r2.getDate().compareTo(r1.getDate());
                    if (dateCompare != 0) return dateCompare;
                    return r2.getStartTime().compareTo(r1.getStartTime());
                })
                .collect(Collectors.toList());

        List<Reservation> cancelled = allReservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.CANCELLED)
                .sorted((r1, r2) -> {
                    int dateCompare = r2.getDate().compareTo(r1.getDate());
                    if (dateCompare != 0) return dateCompare;
                    return r2.getStartTime().compareTo(r1.getStartTime());
                })
                .collect(Collectors.toList());

        upcomingReservations.setValue(upcoming);
        pastReservations.setValue(past);
        cancelledReservations.setValue(cancelled);
    }

    public void refresh() {
        // 실시간 리스너가 자동으로 업데이트하므로 별도 리프레시 불필요
        // 필요 시 리스너 재시작
        if (reservationsListener == null) {
            startListening();
        }
    }

    public void createReservation(Reservation reservation, String userId, String userEmail, FirestoreManager.FirestoreCallback<String> callback) {
        firestoreManager.createReservation(reservation, userId, userEmail, new FirestoreManager.FirestoreCallback<String>() {
            @Override
            public void onSuccess(String documentId) {
                // 관리자에게 알림 생성
                String title = "새로운 예약";
                String message = String.format("%s님이 %s %s~%s 예약을 생성했습니다.",
                        userEmail,
                        reservation.getDate(),
                        reservation.getStartTime(),
                        reservation.getEndTime());

                firestoreManager.createNotification(title, message, "reservation", documentId, new FirestoreManager.FirestoreCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        // 알림 생성 성공 (무시 가능)
                    }

                    @Override
                    public void onFailure(Exception e) {
                        // 알림 생성 실패해도 예약은 성공했으므로 로그만 남김
                        error.setValue("알림 생성 실패: " + e.getMessage());
                    }
                });

                // 실시간 리스너가 자동으로 업데이트
                callback.onSuccess(documentId);
            }

            @Override
            public void onFailure(Exception e) {
                error.setValue(e.getMessage());
                callback.onFailure(e);
            }
        });
    }

    public void updateReservation(String documentId, Map<String, Object> updates, FirestoreManager.FirestoreCallback<Void> callback) {
        firestoreManager.updateReservation(documentId, updates, new FirestoreManager.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // 실시간 리스너가 자동으로 업데이트
                callback.onSuccess(result);
            }

            @Override
            public void onFailure(Exception e) {
                error.setValue(e.getMessage());
                callback.onFailure(e);
            }
        });
    }

    public void cancelReservation(String documentId, FirestoreManager.FirestoreCallback<Void> callback) {
        firestoreManager.cancelReservation(documentId, new FirestoreManager.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // 실시간 리스너가 자동으로 업데이트
                callback.onSuccess(result);
            }

            @Override
            public void onFailure(Exception e) {
                error.setValue(e.getMessage());
                callback.onFailure(e);
            }
        });
    }

    public void cancelReservationByReservationId(String reservationId, FirestoreManager.FirestoreCallback<Void> callback) {
        firestoreManager.cancelReservationByReservationId(reservationId, new FirestoreManager.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // 실시간 리스너가 자동으로 업데이트
                callback.onSuccess(result);
            }

            @Override
            public void onFailure(Exception e) {
                error.setValue(e.getMessage());
                callback.onFailure(e);
            }
        });
    }

    public void updateReservationByReservationId(String reservationId, Map<String, Object> updates, FirestoreManager.FirestoreCallback<Void> callback) {
        firestoreManager.updateReservationByReservationId(reservationId, updates, new FirestoreManager.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // 실시간 리스너가 자동으로 업데이트
                callback.onSuccess(result);
            }

            @Override
            public void onFailure(Exception e) {
                error.setValue(e.getMessage());
                callback.onFailure(e);
            }
        });
    }
}
