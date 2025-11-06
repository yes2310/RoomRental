package com.example.bangbillija.service;

import com.example.bangbillija.model.Reservation;
import com.example.bangbillija.model.ReservationStatus;
import com.example.bangbillija.model.Room;
import com.example.bangbillija.model.RoomStatus;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreManager {

    private static FirestoreManager instance;
    private final FirebaseFirestore db;

    private static final String COLLECTION_ROOMS = "rooms";
    private static final String COLLECTION_RESERVATIONS = "reservations";

    private FirestoreManager() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized FirestoreManager getInstance() {
        if (instance == null) {
            instance = new FirestoreManager();
        }
        return instance;
    }

    // ==================== Room Operations ====================

    public void getAllRooms(FirestoreCallback<List<Room>> callback) {
        db.collection(COLLECTION_ROOMS)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Room> rooms = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Room room = documentToRoom(doc);
                        if (room != null) {
                            rooms.add(room);
                        }
                    }
                    callback.onSuccess(rooms);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void addRoom(Room room, FirestoreCallback<Void> callback) {
        Map<String, Object> data = roomToMap(room);
        db.collection(COLLECTION_ROOMS)
                .document(room.getId())
                .set(data)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    // ==================== Reservation Operations ====================

    public void createReservation(Reservation reservation, String userId, String userEmail, FirestoreCallback<String> callback) {
        Map<String, Object> data = reservationToMap(reservation);
        data.put("userId", userId);
        data.put("ownerEmail", userEmail);
        db.collection(COLLECTION_RESERVATIONS)
                .add(data)
                .addOnSuccessListener(documentReference -> callback.onSuccess(documentReference.getId()))
                .addOnFailureListener(callback::onFailure);
    }

    public void updateReservation(String documentId, Map<String, Object> updates, FirestoreCallback<Void> callback) {
        updates.put("updatedAt", Timestamp.now());
        db.collection(COLLECTION_RESERVATIONS)
                .document(documentId)
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void cancelReservation(String documentId, FirestoreCallback<Void> callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", ReservationStatus.CANCELLED.name());
        updates.put("updatedAt", Timestamp.now());

        db.collection(COLLECTION_RESERVATIONS)
                .document(documentId)
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void cancelReservationByReservationId(String reservationId, FirestoreCallback<Void> callback) {
        db.collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("id", reservationId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        callback.onFailure(new Exception("예약을 찾을 수 없습니다"));
                        return;
                    }
                    String documentId = querySnapshot.getDocuments().get(0).getId();
                    cancelReservation(documentId, callback);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void updateReservationByReservationId(String reservationId, Map<String, Object> updates, FirestoreCallback<Void> callback) {
        db.collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("id", reservationId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        callback.onFailure(new Exception("예약을 찾을 수 없습니다"));
                        return;
                    }
                    String documentId = querySnapshot.getDocuments().get(0).getId();
                    updateReservation(documentId, updates, callback);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void getReservationsForRoom(String roomId, LocalDate date, FirestoreCallback<List<Reservation>> callback) {
        String dateStr = date.toString();
        db.collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("roomId", roomId)
                .whereEqualTo("date", dateStr)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Reservation> reservations = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Reservation reservation = documentToReservation(doc);
                        if (reservation != null && reservation.getStatus() != ReservationStatus.CANCELLED) {
                            reservations.add(reservation);
                        }
                    }
                    callback.onSuccess(reservations);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void getReservationsByUser(String userId, FirestoreCallback<List<Reservation>> callback) {
        db.collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("userId", userId)
                .orderBy("date", Query.Direction.DESCENDING)
                .orderBy("startTime", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Reservation> reservations = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Reservation reservation = documentToReservation(doc);
                        if (reservation != null) {
                            reservations.add(reservation);
                        }
                    }
                    callback.onSuccess(reservations);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void getReservationsByStatus(String userId, ReservationStatus status, FirestoreCallback<List<Reservation>> callback) {
        db.collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", status.name())
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Reservation> reservations = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Reservation reservation = documentToReservation(doc);
                        if (reservation != null) {
                            reservations.add(reservation);
                        }
                    }
                    callback.onSuccess(reservations);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // ==================== Conversion Utilities ====================

    private Room documentToRoom(DocumentSnapshot doc) {
        try {
            String id = doc.getString("id");
            String building = doc.getString("building");
            String name = doc.getString("name");
            Long capacityLong = doc.getLong("capacity");
            int capacity = capacityLong != null ? capacityLong.intValue() : 0;
            String floor = doc.getString("floor");
            List<String> facilities = (List<String>) doc.get("facilities");
            String statusStr = doc.getString("status");
            RoomStatus status = statusStr != null ? RoomStatus.valueOf(statusStr) : RoomStatus.AVAILABLE;

            return new Room(id, building, name, capacity, floor, facilities != null ? facilities : new ArrayList<>(), status);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Reservation documentToReservation(DocumentSnapshot doc) {
        try {
            String id = doc.getString("id");
            String roomId = doc.getString("roomId");
            String roomName = doc.getString("roomName");
            String title = doc.getString("title");
            String owner = doc.getString("owner");
            String dateStr = doc.getString("date");
            String startTimeStr = doc.getString("startTime");
            String endTimeStr = doc.getString("endTime");
            Long attendeesLong = doc.getLong("attendees");
            int attendees = attendeesLong != null ? attendeesLong.intValue() : 0;
            String statusStr = doc.getString("status");
            String note = doc.getString("note");

            LocalDate date = LocalDate.parse(dateStr);
            LocalTime startTime = LocalTime.parse(startTimeStr);
            LocalTime endTime = LocalTime.parse(endTimeStr);
            ReservationStatus status = statusStr != null ? ReservationStatus.valueOf(statusStr) : ReservationStatus.PENDING;

            return new Reservation(id, roomId, roomName, title, owner, date, startTime, endTime, attendees, status, note);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Map<String, Object> roomToMap(Room room) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", room.getId());
        data.put("building", room.getBuilding());
        data.put("name", room.getName());
        data.put("capacity", room.getCapacity());
        data.put("floor", room.getFloor());
        data.put("facilities", room.getFacilities());
        data.put("status", room.getStatus().name());
        data.put("createdAt", Timestamp.now());
        data.put("updatedAt", Timestamp.now());
        return data;
    }

    private Map<String, Object> reservationToMap(Reservation reservation) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", reservation.getId());
        data.put("roomId", reservation.getRoomId());
        data.put("roomName", reservation.getRoomName());
        data.put("title", reservation.getTitle());
        data.put("owner", reservation.getOwner());
        data.put("date", reservation.getDate().toString());
        data.put("startTime", reservation.getStartTime().toString());
        data.put("endTime", reservation.getEndTime().toString());
        data.put("attendees", reservation.getAttendees());
        data.put("status", reservation.getStatus().name());
        data.put("note", reservation.getNote() != null ? reservation.getNote() : "");
        data.put("createdAt", Timestamp.now());
        data.put("updatedAt", Timestamp.now());
        return data;
    }

    // ==================== Callback Interface ====================

    public interface FirestoreCallback<T> {
        void onSuccess(T result);
        void onFailure(Exception e);
    }
}
