package com.example.bangbillija.data;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.bangbillija.model.Room;
import com.example.bangbillija.service.FirestoreManager;

import java.util.List;

public class RoomRepository {

    private static RoomRepository instance;
    private final FirestoreManager firestoreManager = FirestoreManager.getInstance();
    private final MutableLiveData<List<Room>> rooms = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private boolean useFakeData = true; // 개발 중 전환 가능

    private RoomRepository() {
        loadRooms();
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

    public LiveData<String> getError() {
        return error;
    }

    public void setUseFakeData(boolean useFake) {
        this.useFakeData = useFake;
        loadRooms();
    }

    public void loadRooms() {
        if (useFakeData) {
            // 개발/테스트 모드: FakeDataSource 사용
            rooms.setValue(FakeDataSource.getRooms());
        } else {
            // 프로덕션 모드: Firestore 사용
            firestoreManager.getAllRooms(new FirestoreManager.FirestoreCallback<List<Room>>() {
                @Override
                public void onSuccess(List<Room> result) {
                    rooms.setValue(result);
                }

                @Override
                public void onFailure(Exception e) {
                    error.setValue(e.getMessage());
                    // Fallback to fake data on error
                    rooms.setValue(FakeDataSource.getRooms());
                }
            });
        }
    }

    public void refresh() {
        loadRooms();
    }

    public void addRoom(Room room, FirestoreManager.FirestoreCallback<Void> callback) {
        firestoreManager.addRoom(room, new FirestoreManager.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                loadRooms(); // Refresh list after adding
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
