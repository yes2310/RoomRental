package com.example.bangbillija.core;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.bangbillija.data.ReservationRepository;
import com.example.bangbillija.data.RoomRepository;
import com.example.bangbillija.model.Reservation;
import com.example.bangbillija.model.Room;
import com.example.bangbillija.model.TimeSlot;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public class SharedReservationViewModel extends ViewModel {

    private final RoomRepository roomRepository = RoomRepository.getInstance();
    private final ReservationRepository reservationRepository = ReservationRepository.getInstance();

    private final MutableLiveData<String> toolbarTitle = new MutableLiveData<>();
    private final MutableLiveData<Room> selectedRoom = new MutableLiveData<>();
    private final MutableLiveData<LocalDate> selectedDate = new MutableLiveData<>(LocalDate.now());
    private final MediatorLiveData<List<TimeSlot>> timeSlots = new MediatorLiveData<>();
    private final MutableLiveData<Reservation> focusedReservation = new MutableLiveData<>();

    public SharedReservationViewModel() {
        timeSlots.addSource(selectedRoom, room -> refreshSlots());
        timeSlots.addSource(selectedDate, date -> refreshSlots());
    }

    public LiveData<List<Room>> getRooms() {
        return roomRepository.getRooms();
    }

    public LiveData<List<TimeSlot>> getTimeSlots() {
        return timeSlots;
    }

    public LiveData<Reservation> getFocusedReservation() {
        return focusedReservation;
    }

    public LiveData<String> getToolbarTitle() {
        return toolbarTitle;
    }

    public LiveData<List<Reservation>> getUpcomingReservations() {
        return reservationRepository.getUpcomingReservations();
    }

    public LiveData<List<Reservation>> getPastReservations() {
        return reservationRepository.getPastReservations();
    }

    public LiveData<List<Reservation>> getCancelledReservations() {
        return reservationRepository.getCancelledReservations();
    }

    public LiveData<Room> getSelectedRoom() {
        return selectedRoom;
    }

    public LiveData<LocalDate> getSelectedDate() {
        return selectedDate;
    }

    public void selectRoom(Room room) {
        selectedRoom.setValue(room);
    }

    public void selectDate(LocalDate date) {
        selectedDate.setValue(date);
    }

    public void focusReservation(Reservation reservation) {
        focusedReservation.setValue(reservation);
    }

    public void updateToolbarTitle(String title) {
        toolbarTitle.setValue(title);
    }

    private void refreshSlots() {
        Room room = selectedRoom.getValue();
        LocalDate date = selectedDate.getValue();
        if (room == null || date == null) {
            timeSlots.setValue(Collections.emptyList());
            return;
        }
        reservationRepository.buildSlotsFor(room.getId(), date, new com.example.bangbillija.service.FirestoreManager.FirestoreCallback<List<TimeSlot>>() {
            @Override
            public void onSuccess(List<TimeSlot> result) {
                timeSlots.setValue(result);
            }

            @Override
            public void onFailure(Exception e) {
                timeSlots.setValue(Collections.emptyList());
            }
        });
    }
}
