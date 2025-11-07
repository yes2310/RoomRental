package com.example.bangbillija.data;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.bangbillija.model.TimetableEntry;
import com.example.bangbillija.service.FirestoreManager;

import java.time.DayOfWeek;
import java.util.List;

public class TimetableRepository {

    private static TimetableRepository instance;
    private final FirestoreManager firestoreManager = FirestoreManager.getInstance();
    private final MutableLiveData<List<TimetableEntry>> timetableEntries = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private TimetableRepository() {
        loadTimetable();
    }

    public static synchronized TimetableRepository getInstance() {
        if (instance == null) {
            instance = new TimetableRepository();
        }
        return instance;
    }

    public LiveData<List<TimetableEntry>> getTimetableEntries() {
        return timetableEntries;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void loadTimetable() {
        firestoreManager.getAllTimetableEntries(new FirestoreManager.FirestoreCallback<List<TimetableEntry>>() {
            @Override
            public void onSuccess(List<TimetableEntry> result) {
                timetableEntries.setValue(result);
            }

            @Override
            public void onFailure(Exception e) {
                error.setValue(e.getMessage());
            }
        });
    }

    public void addEntry(TimetableEntry entry, FirestoreManager.FirestoreCallback<Void> callback) {
        firestoreManager.addTimetableEntry(entry, new FirestoreManager.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                loadTimetable(); // Refresh list after adding
                callback.onSuccess(result);
            }

            @Override
            public void onFailure(Exception e) {
                error.setValue(e.getMessage());
                callback.onFailure(e);
            }
        });
    }

    public void addEntries(List<TimetableEntry> entries, FirestoreManager.FirestoreCallback<Void> callback) {
        firestoreManager.addTimetableEntries(entries, new FirestoreManager.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                loadTimetable(); // Refresh list after adding
                callback.onSuccess(result);
            }

            @Override
            public void onFailure(Exception e) {
                error.setValue(e.getMessage());
                callback.onFailure(e);
            }
        });
    }

    public void deleteEntry(String entryId, FirestoreManager.FirestoreCallback<Void> callback) {
        firestoreManager.deleteTimetableEntry(entryId, new FirestoreManager.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                loadTimetable(); // Refresh list after deleting
                callback.onSuccess(result);
            }

            @Override
            public void onFailure(Exception e) {
                error.setValue(e.getMessage());
                callback.onFailure(e);
            }
        });
    }

    public void deleteAllEntries(FirestoreManager.FirestoreCallback<Void> callback) {
        firestoreManager.deleteAllTimetableEntries(new FirestoreManager.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                loadTimetable(); // Refresh list after deleting all
                callback.onSuccess(result);
            }

            @Override
            public void onFailure(Exception e) {
                error.setValue(e.getMessage());
                callback.onFailure(e);
            }
        });
    }

    public void getTimetableForRoomAndDay(String roomId, DayOfWeek dayOfWeek, FirestoreManager.FirestoreCallback<List<TimetableEntry>> callback) {
        firestoreManager.getTimetableEntriesForRoomAndDay(roomId, dayOfWeek, callback);
    }

    public void refresh() {
        loadTimetable();
    }
}
