package com.example.bangbillija.service;

import com.example.bangbillija.model.Reservation;
import com.example.bangbillija.model.ReservationStatus;
import com.example.bangbillija.model.Room;
import com.example.bangbillija.model.RoomStatus;
import com.example.bangbillija.model.TimetableEntry;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
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
    private static final String COLLECTION_NOTIFICATIONS = "notifications";

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

    /**
     * 강의실 실시간 리스너 (다른 사용자의 변경사항 즉시 반영)
     * @return ListenerRegistration (해제 시 remove() 호출)
     */
    public ListenerRegistration listenToRooms(FirestoreCallback<List<Room>> callback) {
        return db.collection(COLLECTION_ROOMS)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        callback.onFailure(error);
                        return;
                    }

                    if (querySnapshot != null) {
                        List<Room> rooms = new ArrayList<>();
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Room room = documentToRoom(doc);
                            if (room != null) {
                                rooms.add(room);
                            }
                        }
                        callback.onSuccess(rooms);
                    }
                });
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

    /**
     * 예약 실시간 리스너 (다른 사용자의 변경사항 즉시 반영)
     * @return ListenerRegistration (해제 시 remove() 호출)
     */
    public ListenerRegistration listenToReservations(FirestoreCallback<List<Reservation>> callback) {
        return db.collection(COLLECTION_RESERVATIONS)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        callback.onFailure(error);
                        return;
                    }

                    if (querySnapshot != null) {
                        List<Reservation> reservations = new ArrayList<>();
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Reservation reservation = documentToReservation(doc);
                            if (reservation != null) {
                                reservations.add(reservation);
                            }
                        }
                        callback.onSuccess(reservations);
                    }
                });
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

    /**
     * 시간표 실시간 리스너 (다른 사용자의 변경사항 즉시 반영)
     * @return ListenerRegistration (해제 시 remove() 호출)
     */
    public ListenerRegistration listenToTimetableEntries(FirestoreCallback<List<TimetableEntry>> callback) {
        return db.collection(COLLECTION_TIMETABLE)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        callback.onFailure(error);
                        return;
                    }

                    if (querySnapshot != null) {
                        List<TimetableEntry> entries = new ArrayList<>();
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            TimetableEntry entry = documentToTimetableEntry(doc);
                            if (entry != null) {
                                entries.add(entry);
                            }
                        }
                        callback.onSuccess(entries);
                    }
                });
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

    public void getTimetableEntriesBySemester(String semester, FirestoreCallback<List<TimetableEntry>> callback) {
        db.collection(COLLECTION_TIMETABLE)
                .whereEqualTo("semester", semester)
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

    /**
     * 학기별 시간표 실시간 리스너 (다른 사용자의 변경사항 즉시 반영)
     * @return ListenerRegistration (해제 시 remove() 호출)
     */
    public ListenerRegistration listenToTimetableEntriesBySemester(String semester, FirestoreCallback<List<TimetableEntry>> callback) {
        return db.collection(COLLECTION_TIMETABLE)
                .whereEqualTo("semester", semester)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        callback.onFailure(error);
                        return;
                    }

                    if (querySnapshot != null) {
                        List<TimetableEntry> entries = new ArrayList<>();
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            TimetableEntry entry = documentToTimetableEntry(doc);
                            if (entry != null) {
                                entries.add(entry);
                            }
                        }
                        callback.onSuccess(entries);
                    }
                });
    }

    public void deleteTimetableEntriesBySemester(String semester, FirestoreCallback<Void> callback) {
        // 1단계: 해당 학기의 시간표에 사용된 강의실 ID 목록 추출
        db.collection(COLLECTION_TIMETABLE)
                .whereEqualTo("semester", semester)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // 시간표에 사용된 강의실 ID 수집
                    java.util.Set<String> roomIdsToCheck = new java.util.HashSet<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String roomId = doc.getString("roomId");
                        if (roomId != null && !roomId.isEmpty()) {
                            roomIdsToCheck.add(roomId);
                        }
                    }

                    android.util.Log.d("FirestoreManager",
                        "Deleting semester " + semester + ", found " + querySnapshot.size() +
                        " timetable entries with rooms: " + roomIdsToCheck);

                    // 2단계: 시간표 삭제
                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        batch.delete(doc.getReference());
                    }
                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                android.util.Log.d("FirestoreManager",
                                    "Successfully deleted " + querySnapshot.size() + " timetable entries for semester " + semester);
                                // 3단계: 다른 학기에서 사용되지 않는 강의실 삭제
                                deleteUnusedRooms(roomIdsToCheck, semester, callback);
                            })
                            .addOnFailureListener(e -> {
                                android.util.Log.e("FirestoreManager",
                                    "Failed to delete timetable entries for semester " + semester, e);
                                callback.onFailure(e);
                            });
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("FirestoreManager",
                        "Failed to query timetable entries for semester " + semester, e);
                    callback.onFailure(e);
                });
    }

    /**
     * 특정 학기 삭제 후 다른 학기의 시간표에서 사용되지 않는 강의실 삭제
     */
    private void deleteUnusedRooms(java.util.Set<String> roomIds, String deletedSemester, FirestoreCallback<Void> callback) {
        if (roomIds.isEmpty()) {
            android.util.Log.d("FirestoreManager",
                "No rooms to check for deletion (semester " + deletedSemester + " had no rooms)");
            callback.onSuccess(null);
            return;
        }

        android.util.Log.d("FirestoreManager",
            "Checking if rooms can be deleted: " + roomIds);

        // 모든 시간표 조회 (삭제된 학기의 시간표는 이미 삭제됨)
        db.collection(COLLECTION_TIMETABLE)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    android.util.Log.d("FirestoreManager",
                        "Found " + querySnapshot.size() + " remaining timetable entries");

                    // 다른 학기에서 사용 중인 강의실 ID 수집
                    java.util.Set<String> usedRoomIds = new java.util.HashSet<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String roomId = doc.getString("roomId");
                        String semester = doc.getString("semester");
                        if (roomId != null && !roomId.isEmpty()) {
                            usedRoomIds.add(roomId);
                            android.util.Log.d("FirestoreManager",
                                "Room " + roomId + " is still used in semester " + semester);
                        }
                    }

                    android.util.Log.d("FirestoreManager",
                        "Rooms still in use: " + usedRoomIds);

                    // 다른 학기 시간표에서 사용되지 않는 강의실 찾기
                    List<String> roomsToDelete = new ArrayList<>();
                    for (String roomId : roomIds) {
                        if (!usedRoomIds.contains(roomId)) {
                            roomsToDelete.add(roomId);
                        }
                    }

                    // 강의실 삭제
                    if (roomsToDelete.isEmpty()) {
                        android.util.Log.d("FirestoreManager",
                            "No unused rooms to delete (all rooms are used in other semesters)");
                        callback.onSuccess(null);
                        return;
                    }

                    android.util.Log.d("FirestoreManager",
                        "Will delete " + roomsToDelete.size() + " unused rooms: " + roomsToDelete);

                    com.google.firebase.firestore.WriteBatch deleteBatch = db.batch();
                    for (String roomId : roomsToDelete) {
                        deleteBatch.delete(db.collection(COLLECTION_ROOMS).document(roomId));
                    }
                    deleteBatch.commit()
                            .addOnSuccessListener(aVoid -> {
                                android.util.Log.d("FirestoreManager",
                                    "Successfully deleted " + roomsToDelete.size() + " unused rooms: " + roomsToDelete);
                                callback.onSuccess(null);
                            })
                            .addOnFailureListener(e -> {
                                android.util.Log.e("FirestoreManager",
                                    "Failed to delete unused rooms", e);
                                callback.onFailure(e);
                            });
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("FirestoreManager",
                        "Failed to query remaining timetable entries", e);
                    callback.onFailure(e);
                });
    }

    public void getAllSemesters(FirestoreCallback<List<String>> callback) {
        db.collection(COLLECTION_TIMETABLE)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    java.util.Set<String> semestersSet = new java.util.HashSet<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String semester = doc.getString("semester");
                        if (semester != null && !semester.isEmpty()) {
                            semestersSet.add(semester);
                        }
                    }
                    List<String> semesters = new ArrayList<>(semestersSet);
                    java.util.Collections.sort(semesters, java.util.Collections.reverseOrder());
                    callback.onSuccess(semesters);
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
            String semester = doc.getString("semester");

            DayOfWeek dayOfWeek = DayOfWeek.valueOf(dayOfWeekStr);
            LocalTime startTime = LocalTime.parse(startTimeStr);
            LocalTime endTime = LocalTime.parse(endTimeStr);

            return new TimetableEntry(id, courseName, roomId, roomName, dayOfWeek, startTime, endTime, attendees, professor, note, semester);
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
        data.put("semester", entry.getSemester());
        data.put("createdAt", Timestamp.now());
        data.put("updatedAt", Timestamp.now());
        return data;
    }

    // ==================== Notification Management ====================

    /**
     * 관리자용 알림 생성
     */
    public void createNotification(String title, String message, String type, String relatedId, FirestoreCallback<Void> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("message", message);
        data.put("type", type); // "reservation", "room", "timetable"
        data.put("targetUserId", "admin");
        data.put("timestamp", Timestamp.now());
        data.put("read", false);
        data.put("relatedId", relatedId);

        db.collection(COLLECTION_NOTIFICATIONS)
                .add(data)
                .addOnSuccessListener(documentReference -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 관리자용 알림 목록 조회 (실시간 리스너)
     */
    public ListenerRegistration listenToAdminNotifications(FirestoreCallback<List<com.example.bangbillija.model.Notification>> callback) {
        return db.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("targetUserId", "admin")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        callback.onFailure(error);
                        return;
                    }

                    if (querySnapshot != null) {
                        List<com.example.bangbillija.model.Notification> notifications = new ArrayList<>();
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            com.example.bangbillija.model.Notification notification = mapToNotification(doc);
                            if (notification != null) {
                                notifications.add(notification);
                            }
                        }
                        callback.onSuccess(notifications);
                    }
                });
    }

    /**
     * 알림 읽음 처리
     */
    public void markNotificationAsRead(String notificationId, FirestoreCallback<Void> callback) {
        db.collection(COLLECTION_NOTIFICATIONS)
                .document(notificationId)
                .update("read", true)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * DocumentSnapshot을 Notification 객체로 변환
     */
    private com.example.bangbillija.model.Notification mapToNotification(DocumentSnapshot doc) {
        try {
            String id = doc.getId();
            String title = doc.getString("title");
            String message = doc.getString("message");
            String type = doc.getString("type");
            String targetUserId = doc.getString("targetUserId");
            Timestamp timestamp = doc.getTimestamp("timestamp");
            Boolean read = doc.getBoolean("read");
            String relatedId = doc.getString("relatedId");

            if (title == null || message == null || type == null || targetUserId == null || timestamp == null || read == null) {
                return null;
            }

            java.time.LocalDateTime localDateTime = Instant.ofEpochMilli(timestamp.toDate().getTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            return new com.example.bangbillija.model.Notification(
                    id,
                    title,
                    message,
                    type,
                    targetUserId,
                    localDateTime,
                    read,
                    relatedId
            );
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== Callback Interface ====================

    public interface FirestoreCallback<T> {
        void onSuccess(T result);
        void onFailure(Exception e);
    }
}
