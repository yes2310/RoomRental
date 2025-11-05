package com.example.bangbillija.data;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.bangbillija.model.Room;

import java.util.List;

public class RoomRepository {

    private static RoomRepository instance;
    private final MutableLiveData<List<Room>> rooms = new MutableLiveData<>(FakeDataSource.getRooms());

    private RoomRepository() {
    }

    public static synchronized RoomRepository getInstance() {
        if (instance == null) {
            instance = new RoomRepository();
        }
        return instance;
    }

    public LiveData<List<Room>> getRooms() {
        return rooms;
    }

    public void refresh() {
        rooms.setValue(FakeDataSource.getRooms());
    }
}
