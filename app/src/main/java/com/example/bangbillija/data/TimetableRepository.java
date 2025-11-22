package com.example.bangbillija.data;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.bangbillija.model.TimetableEntry;
import com.example.bangbillija.service.FirestoreManager;
import com.google.firebase.firestore.ListenerRegistration;

import java.time.DayOfWeek;
import java.util.List;

public class TimetableRepository {

    private static TimetableRepository instance;
    private final FirestoreManager firestoreManager = FirestoreManager.getInstance();
    private final MutableLiveData<List<TimetableEntry>> timetableEntries = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private ListenerRegistration timetableListener;
    private String currentListeningSemester = null;

    private TimetableRepository() {
        // 초기에는 리스너를 시작하지 않음 (학기가 선택되면 시작)
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

    /**
     * 리스너 해제 (필요 시 호출)
     */
    public void stopListening() {
        if (timetableListener != null) {
            timetableListener.remove();
            timetableListener = null;
            currentListeningSemester = null;
        }
    }

    public void loadTimetable() {
        // 전체 시간표 실시간 리스너
        stopListening();
        currentListeningSemester = null;

        timetableListener = firestoreManager.listenToTimetableEntries(new FirestoreManager.FirestoreCallback<List<TimetableEntry>>() {
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

    public void loadTimetableBySemester(String semester) {
        // 이미 같은 학기를 듣고 있으면 리스너를 재시작하지 않음
        if (semester.equals(currentListeningSemester) && timetableListener != null) {
            return;
        }

        // 학기별 시간표 실시간 리스너
        stopListening();
        currentListeningSemester = semester;

        timetableListener = firestoreManager.listenToTimetableEntriesBySemester(semester, new FirestoreManager.FirestoreCallback<List<TimetableEntry>>() {
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
                // 관리자에게 알림 생성
                String title = "새로운 시간표 등록";
                String message = String.format("%s - %s(%s) %s %s~%s",
                        entry.getRoomName(),
                        entry.getCourseName(),
                        entry.getCourseCode(),
                        entry.getDayOfWeek(),
                        entry.getStartTime(),
                        entry.getEndTime());

                firestoreManager.createNotification(title, message, "timetable", entry.getEntryId(), new FirestoreManager.FirestoreCallback<Void>() {
                    @Override
                    public void onSuccess(Void notifResult) {
                        // 알림 생성 성공 (무시 가능)
                    }

                    @Override
                    public void onFailure(Exception e) {
                        // 알림 생성 실패해도 시간표는 성공했으므로 로그만 남김
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

    public void addEntries(List<TimetableEntry> entries, FirestoreManager.FirestoreCallback<Void> callback) {
        firestoreManager.addTimetableEntries(entries, new FirestoreManager.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // 관리자에게 알림 생성
                String title = "시간표 일괄 등록";
                String message = String.format("%d개의 시간표 항목이 등록되었습니다.", entries.size());

                firestoreManager.createNotification(title, message, "timetable", "", new FirestoreManager.FirestoreCallback<Void>() {
                    @Override
                    public void onSuccess(Void notifResult) {
                        // 알림 생성 성공 (무시 가능)
                    }

                    @Override
                    public void onFailure(Exception e) {
                        // 알림 생성 실패해도 시간표는 성공했으므로 로그만 남김
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

    public void deleteEntry(String entryId, FirestoreManager.FirestoreCallback<Void> callback) {
        firestoreManager.deleteTimetableEntry(entryId, new FirestoreManager.FirestoreCallback<Void>() {
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

    public void deleteAllEntries(FirestoreManager.FirestoreCallback<Void> callback) {
        firestoreManager.deleteAllTimetableEntries(new FirestoreManager.FirestoreCallback<Void>() {
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

    public void getTimetableForRoomAndDay(String roomId, DayOfWeek dayOfWeek, FirestoreManager.FirestoreCallback<List<TimetableEntry>> callback) {
        firestoreManager.getTimetableEntriesForRoomAndDay(roomId, dayOfWeek, callback);
    }

    public void deleteTimetableBySemester(String semester, FirestoreManager.FirestoreCallback<Void> callback) {
        firestoreManager.deleteTimetableEntriesBySemester(semester, new FirestoreManager.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // Fragment will handle reloading the timetable by semester
                callback.onSuccess(result);
            }

            @Override
            public void onFailure(Exception e) {
                error.setValue(e.getMessage());
                callback.onFailure(e);
            }
        });
    }

    public void getAllSemesters(FirestoreManager.FirestoreCallback<List<String>> callback) {
        firestoreManager.getAllSemesters(callback);
    }

    public void refresh() {
        // 실시간 리스너가 자동으로 업데이트하므로 별도 리프레시 불필요
        // 필요 시 리스너 재시작
        if (timetableListener == null) {
            loadTimetable();
        }
    }
}
