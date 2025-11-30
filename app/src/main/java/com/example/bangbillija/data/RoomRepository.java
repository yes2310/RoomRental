package com.example.bangbillija.data;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.bangbillija.model.Room;
import com.example.bangbillija.service.FirestoreManager;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class RoomRepository {

    private static RoomRepository instance;
    private final FirestoreManager firestoreManager = FirestoreManager.getInstance();
    private final MutableLiveData<List<Room>> rooms = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private ListenerRegistration roomsListener;

    private RoomRepository() {
        startListening();
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

    /**
     * 실시간 리스너 시작 (다른 사용자의 변경사항 자동 반영)
     */
    private void startListening() {
        if (roomsListener != null) {
            roomsListener.remove();
        }

        roomsListener = firestoreManager.listenToRooms(new FirestoreManager.FirestoreCallback<List<Room>>() {
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

    /**
     * 리스너 해제 (필요 시 호출)
     */
    public void stopListening() {
        if (roomsListener != null) {
            roomsListener.remove();
            roomsListener = null;
        }
    }

    public void loadRooms() {
        // 실시간 리스너가 이미 실행 중이므로 별도 조회 불필요
        // 필요 시 리스너 재시작
        if (roomsListener == null) {
            startListening();
        }
    }

    public void refresh() {
        // 실시간 리스너가 자동으로 최신 데이터 반영
        // 필요 시 리스너 재시작
        stopListening();
        startListening();
    }

    public void addRoom(Room room, FirestoreManager.FirestoreCallback<Void> callback) {
        firestoreManager.addRoom(room, new FirestoreManager.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // 관리자에게 알림 생성
                String title = "새로운 강의실 등록";
                String message = String.format("%s (수용인원: %d명)",
                        room.getName(),
                        room.getCapacity());

                firestoreManager.createNotification(title, message, "room", room.getRoomId(), new FirestoreManager.FirestoreCallback<Void>() {
                    @Override
                    public void onSuccess(Void notifResult) {
                        // 알림 생성 성공 (무시 가능)
                    }

                    @Override
                    public void onFailure(Exception e) {
                        // 알림 생성 실패해도 강의실 추가는 성공했으므로 로그만 남김
                        error.setValue("알림 생성 실패: " + e.getMessage());
                    }
                });

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

    public void updateRoom(Room room, FirestoreManager.FirestoreCallback<Void> callback) {
        firestoreManager.updateRoom(room, new FirestoreManager.FirestoreCallback<Void>() {
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

    public void deleteRoom(String roomId, FirestoreManager.FirestoreCallback<Void> callback) {
        firestoreManager.deleteRoom(roomId, new FirestoreManager.FirestoreCallback<Void>() {
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
