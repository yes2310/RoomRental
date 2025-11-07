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
    private boolean useFakeData = false; // Firebase Firestore 사용

    private ReservationRepository() {
        loadReservations();
    }

    public static synchronized ReservationRepository getInstance() {
        if (instance == null) {
            instance = new ReservationRepository();
        }
        return instance;
    }

    public void setUseFakeData(boolean useFake) {
        this.useFakeData = useFake;
        loadReservations();
    }

    public void buildSlotsFor(String roomId, LocalDate date, FirestoreManager.FirestoreCallback<List<TimeSlot>> callback) {
        if (useFakeData) {
            // Fake data mode
            List<Reservation> reservations = FakeDataSource.getReservationsForRoom(roomId)
                    .stream()
                    .filter(reservation -> reservation.getDate().equals(date))
                    .collect(Collectors.toList());
            callback.onSuccess(SlotEngine.calculateDailySlots(date, reservations));
        } else {
            // Firestore mode: 예약 + 시간표 모두 가져오기
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

    public void loadReservations() {
        if (useFakeData) {
            // Fake data mode
            upcomingReservations.setValue(FakeDataSource.getReservationsAfter(LocalDate.now()));
            pastReservations.setValue(FakeDataSource.getReservationsBefore(LocalDate.now()));
            cancelledReservations.setValue(FakeDataSource.getReservationsByStatus(ReservationStatus.CANCELLED));
        } else {
            // Firestore mode
            FirebaseUser user = authManager.currentUser();
            if (user == null) {
                return;
            }

            String userId = user.getUid();

            // Load all reservations and filter by date
            firestoreManager.getReservationsByUser(userId, new FirestoreManager.FirestoreCallback<List<Reservation>>() {
                @Override
                public void onSuccess(List<Reservation> allReservations) {
                    LocalDate today = LocalDate.now();

                    List<Reservation> upcoming = allReservations.stream()
                            .filter(r -> !r.getDate().isBefore(today) && r.getStatus() != ReservationStatus.CANCELLED)
                            .collect(Collectors.toList());

                    List<Reservation> past = allReservations.stream()
                            .filter(r -> r.getDate().isBefore(today) && r.getStatus() != ReservationStatus.CANCELLED)
                            .collect(Collectors.toList());

                    List<Reservation> cancelled = allReservations.stream()
                            .filter(r -> r.getStatus() == ReservationStatus.CANCELLED)
                            .collect(Collectors.toList());

                    upcomingReservations.setValue(upcoming);
                    pastReservations.setValue(past);
                    cancelledReservations.setValue(cancelled);
                }

                @Override
                public void onFailure(Exception e) {
                    error.setValue(e.getMessage());
                }
            });
        }
    }

    public void refresh() {
        loadReservations();
    }

    public void createReservation(Reservation reservation, String userId, String userEmail, FirestoreManager.FirestoreCallback<String> callback) {
        firestoreManager.createReservation(reservation, userId, userEmail, new FirestoreManager.FirestoreCallback<String>() {
            @Override
            public void onSuccess(String documentId) {
                loadReservations(); // Refresh after creation
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
                loadReservations(); // Refresh after update
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
                loadReservations(); // Refresh after cancellation
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
                loadReservations(); // Refresh after cancellation
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
                loadReservations(); // Refresh after update
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
