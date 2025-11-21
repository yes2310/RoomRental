package com.example.bangbillija.service;

import com.example.bangbillija.model.Reservation;
import com.example.bangbillija.model.ReservationStatus;
import com.example.bangbillija.model.Room;
import com.example.bangbillija.model.RoomStatus;
import com.example.bangbillija.model.TimetableEntry;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.time.DayOfWeek;
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
    private static final String COLLECTION_TIMETABLE = "timetable";

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

    public void updateRoom(Room room, FirestoreCallback<Void> callback) {
        Map<String, Object> data = roomToMap(room);
        db.collection(COLLECTION_ROOMS)
                .document(room.getId())
                .set(data)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void deleteRoom(String roomId, FirestoreCallback<Void> callback) {
        db.collection(COLLECTION_ROOMS)
                .document(roomId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void getRoom(String roomId, FirestoreCallback<Room> callback) {
        db.collection(COLLECTION_ROOMS)
                .document(roomId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Room room = documentToRoom(documentSnapshot);
                        if (room != null) {
                            callback.onSuccess(room);
                        } else {
                            callback.onFailure(new Exception("Failed to parse room document"));
                        }
                    } else {
                        callback.onFailure(new Exception("Room not found: " + roomId));
                    }
                })
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

    public void deleteReservation(String reservationId, FirestoreCallback<Void> callback) {
        db.collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("id", reservationId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        callback.onFailure(new Exception("예약을 찾을 수 없습니다"));
                        return;
                    }
                    String documentId = querySnapshot.getDocuments().get(0).getId();
                    db.collection(COLLECTION_RESERVATIONS)
                            .document(documentId)
                            .delete()
                            .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                            .addOnFailureListener(callback::onFailure);
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

    public void getAllReservations(FirestoreCallback<List<Reservation>> callback) {
        db.collection(COLLECTION_RESERVATIONS)
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

    public void getReservationsByRoomAndDate(String roomId, LocalDate date, FirestoreCallback<List<Reservation>> callback) {
        // Convert LocalDate to String for Firestore query (dates are stored as strings)
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

    // ==================== Timetable Operations ====================

    public void addTimetableEntry(TimetableEntry entry, FirestoreCallback<Void> callback) {
        Map<String, Object> data = timetableEntryToMap(entry);
        db.collection(COLLECTION_TIMETABLE)
                .document(entry.getId())
                .set(data)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void addTimetableEntries(List<TimetableEntry> entries, FirestoreCallback<Void> callback) {
        if (entries.isEmpty()) {
            callback.onSuccess(null);
            return;
        }

        // Firestore batch write (최대 500개)
        com.google.firebase.firestore.WriteBatch batch = db.batch();
        for (TimetableEntry entry : entries) {
            Map<String, Object> data = timetableEntryToMap(entry);
            batch.set(db.collection(COLLECTION_TIMETABLE).document(entry.getId()), data);
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void getAllTimetableEntries(FirestoreCallback<List<TimetableEntry>> callback) {
        db.collection(COLLECTION_TIMETABLE)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<TimetableEntry> entries = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        TimetableEntry entry = documentToTimetableEntry(doc);
                        if (entry != null) {
                            entries.add(entry);
                        }
                    }
                    callback.onSuccess(entries);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void getTimetableEntriesForRoom(String roomId, FirestoreCallback<List<TimetableEntry>> callback) {
        db.collection(COLLECTION_TIMETABLE)
                .whereEqualTo("roomId", roomId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<TimetableEntry> entries = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        TimetableEntry entry = documentToTimetableEntry(doc);
                        if (entry != null) {
                            entries.add(entry);
                        }
                    }
                    callback.onSuccess(entries);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void getTimetableEntriesForRoomAndDay(String roomId, DayOfWeek dayOfWeek, FirestoreCallback<List<TimetableEntry>> callback) {
        db.collection(COLLECTION_TIMETABLE)
                .whereEqualTo("roomId", roomId)
                .whereEqualTo("dayOfWeek", dayOfWeek.name())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<TimetableEntry> entries = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        TimetableEntry entry = documentToTimetableEntry(doc);
                        if (entry != null) {
                            entries.add(entry);
                        }
                    }
                    callback.onSuccess(entries);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void deleteTimetableEntry(String entryId, FirestoreCallback<Void> callback) {
        db.collection(COLLECTION_TIMETABLE)
                .document(entryId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void deleteAllTimetableEntries(FirestoreCallback<Void> callback) {
        db.collection(COLLECTION_TIMETABLE)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        batch.delete(doc.getReference());
                    }
                    batch.commit()
                            .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                            .addOnFailureListener(callback::onFailure);
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
            String ownerStudentId = doc.getString("ownerStudentId");  // 학번
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

            // 학번이 없는 경우 빈 문자열로 처리 (기존 예약 호환성)
            if (ownerStudentId == null) {
                ownerStudentId = "";
            }

            return new Reservation(id, roomId, roomName, title, owner, ownerStudentId, date, startTime, endTime, attendees, status, note);
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
        data.put("ownerStudentId", reservation.getOwnerStudentId());  // 학번
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

    /**
     * 사용자의 학번을 가져옵니다.
     */
    public void getUserStudentId(String userId, FirestoreCallback<String> callback) {
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String studentId = documentSnapshot.getString("studentId");
                    callback.onSuccess(studentId != null ? studentId : "");
                })
                .addOnFailureListener(callback::onFailure);
    }

    private TimetableEntry documentToTimetableEntry(DocumentSnapshot doc) {
        try {
            String id = doc.getString("id");
            String courseName = doc.getString("courseName");
            String roomId = doc.getString("roomId");
            String roomName = doc.getString("roomName");
            String dayOfWeekStr = doc.getString("dayOfWeek");
            String startTimeStr = doc.getString("startTime");
            String endTimeStr = doc.getString("endTime");
            Long attendeesLong = doc.getLong("attendees");
            int attendees = attendeesLong != null ? attendeesLong.intValue() : 0;
            String professor = doc.getString("professor");
            String note = doc.getString("note");

            DayOfWeek dayOfWeek = DayOfWeek.valueOf(dayOfWeekStr);
            LocalTime startTime = LocalTime.parse(startTimeStr);
            LocalTime endTime = LocalTime.parse(endTimeStr);

            return new TimetableEntry(id, courseName, roomId, roomName, dayOfWeek, startTime, endTime, attendees, professor, note);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Map<String, Object> timetableEntryToMap(TimetableEntry entry) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", entry.getId());
        data.put("courseName", entry.getCourseName());
        data.put("roomId", entry.getRoomId());
        data.put("roomName", entry.getRoomName());
        data.put("dayOfWeek", entry.getDayOfWeek().name());
        data.put("startTime", entry.getStartTime().toString());
        data.put("endTime", entry.getEndTime().toString());
        data.put("attendees", entry.getAttendees());
        data.put("professor", entry.getProfessor() != null ? entry.getProfessor() : "");
        data.put("note", entry.getNote() != null ? entry.getNote() : "");
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
