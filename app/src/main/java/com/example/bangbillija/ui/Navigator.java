package com.example.bangbillija.ui;

import com.example.bangbillija.model.Room;

public interface Navigator {
    void openReservationDetail();
    void navigateBackToRooms();
    void openQrScanner();
    void openCreateReservation();
    void openAddRoom();
    void openEditRoom(Room room);
}
