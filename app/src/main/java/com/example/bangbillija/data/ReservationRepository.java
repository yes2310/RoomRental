package com.example.bangbillija.data;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.bangbillija.model.Reservation;
import com.example.bangbillija.model.ReservationStatus;
import com.example.bangbillija.model.TimeSlot;
import com.example.bangbillija.service.SlotEngine;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class ReservationRepository {

    private static ReservationRepository instance;

    private final MutableLiveData<List<Reservation>> upcomingReservations =
            new MutableLiveData<>(FakeDataSource.getReservationsAfter(LocalDate.now()));
    private final MutableLiveData<List<Reservation>> pastReservations =
            new MutableLiveData<>(FakeDataSource.getReservationsBefore(LocalDate.now()));
    private final MutableLiveData<List<Reservation>> cancelledReservations =
            new MutableLiveData<>(FakeDataSource.getReservationsByStatus(ReservationStatus.CANCELLED));

    private ReservationRepository() {
    }

    public static synchronized ReservationRepository getInstance() {
        if (instance == null) {
            instance = new ReservationRepository();
        }
        return instance;
    }

    public List<TimeSlot> buildSlotsFor(String roomId, LocalDate date) {
        List<Reservation> reservations = FakeDataSource.getReservationsForRoom(roomId)
                .stream()
                .filter(reservation -> reservation.getDate().equals(date))
                .collect(Collectors.toList());
        return SlotEngine.calculateDailySlots(date, reservations);
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

    public void refresh() {
        upcomingReservations.setValue(FakeDataSource.getReservationsAfter(LocalDate.now()));
        pastReservations.setValue(FakeDataSource.getReservationsBefore(LocalDate.now()));
        cancelledReservations.setValue(FakeDataSource.getReservationsByStatus(ReservationStatus.CANCELLED));
    }
}
