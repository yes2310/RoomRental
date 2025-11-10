package com.example.bangbillija.ui.timetable;

import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bangbillija.R;
import com.example.bangbillija.model.Room;
import com.example.bangbillija.model.TimetableEntry;
import com.example.bangbillija.data.RoomRepository;
import com.example.bangbillija.data.TimetableRepository;
import com.example.bangbillija.service.FirestoreManager;
import com.example.bangbillija.util.TimetableCSVParser;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TimetableFragment extends Fragment {

    private MaterialButton buttonDownloadTemplate;
    private MaterialButton buttonUploadCSV;
    private MaterialButton buttonAddCourse;
    private TextInputEditText inputCourseName;
    private TextInputEditText inputRoom;
    private TextInputEditText inputDay;
    private TextInputEditText inputStartTime;
    private TextInputEditText inputEndTime;
    private TextInputEditText inputAttendees;
    private TextInputEditText inputProfessor;
    private TextInputEditText inputNote;
    private RecyclerView recyclerTimetable;
    private View textEmptyTimetable;

    private TimetableAdapter adapter;
    private RoomRepository roomRepository;
    private TimetableRepository timetableRepository;
    private List<Room> availableRooms = new ArrayList<>();
    private Room selectedRoom;
    private DayOfWeek selectedDay;
    private LocalTime selectedStartTime;
    private LocalTime selectedEndTime;

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        parseCSVFile(uri);
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_timetable, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        roomRepository = RoomRepository.getInstance();
        timetableRepository = TimetableRepository.getInstance();

        initViews(view);
        setupRecyclerView();
        setupListeners();
        loadRooms();
        loadTimetable();
        updateEmptyState();
    }

    private void initViews(View view) {
        buttonDownloadTemplate = view.findViewById(R.id.buttonDownloadTemplate);
        buttonUploadCSV = view.findViewById(R.id.buttonUploadCSV);
        buttonAddCourse = view.findViewById(R.id.buttonAddCourse);
        inputCourseName = view.findViewById(R.id.inputCourseName);
        inputRoom = view.findViewById(R.id.inputRoom);
        inputDay = view.findViewById(R.id.inputDay);
        inputStartTime = view.findViewById(R.id.inputStartTime);
        inputEndTime = view.findViewById(R.id.inputEndTime);
        inputAttendees = view.findViewById(R.id.inputAttendees);
        inputProfessor = view.findViewById(R.id.inputProfessor);
        inputNote = view.findViewById(R.id.inputNote);
        recyclerTimetable = view.findViewById(R.id.recyclerTimetable);
        textEmptyTimetable = view.findViewById(R.id.textEmptyTimetable);
    }

    private void setupRecyclerView() {
        adapter = new TimetableAdapter();
        adapter.setOnItemClickListener(this::handleDeleteEntry);
        recyclerTimetable.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerTimetable.setAdapter(adapter);
    }

    private void setupListeners() {
        buttonDownloadTemplate.setOnClickListener(v -> downloadTemplate());
        buttonUploadCSV.setOnClickListener(v -> openFilePicker());
        buttonAddCourse.setOnClickListener(v -> handleAddCourse());

        inputRoom.setOnClickListener(v -> showRoomPicker());
        inputDay.setOnClickListener(v -> showDayPicker());
        inputStartTime.setOnClickListener(v -> showTimePicker(true));
        inputEndTime.setOnClickListener(v -> showTimePicker(false));
    }

    private void loadRooms() {
        roomRepository.getRooms().observe(getViewLifecycleOwner(), rooms -> {
            if (rooms != null) {
                availableRooms = rooms;
            }
        });
    }

    private void loadTimetable() {
        timetableRepository.getTimetableEntries().observe(getViewLifecycleOwner(), entries -> {
            if (entries != null) {
                adapter.setEntries(entries);
                updateEmptyState();
            }
        });
    }

    private void downloadTemplate() {
        try {
            InputStream inputStream = requireContext().getAssets().open("timetable_template.csv");

            String fileName = "timetable_template.csv";
            OutputStream outputStream;
            String savePath;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ (API 29+): Use MediaStore API
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = requireContext().getContentResolver().insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

                if (uri == null) {
                    throw new Exception("URI 생성 실패");
                }

                outputStream = requireContext().getContentResolver().openOutputStream(uri);
                savePath = "다운로드 폴더";
            } else {
                // Android 9 and below: Use legacy approach
                File downloadsDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS);
                File outputFile = new File(downloadsDir, fileName);
                outputStream = new FileOutputStream(outputFile);
                savePath = outputFile.getAbsolutePath();
            }

            if (outputStream == null) {
                throw new Exception("파일 스트림 생성 실패");
            }

            // Copy data
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            inputStream.close();
            outputStream.close();

            Toast.makeText(requireContext(),
                    "템플릿이 " + savePath + "에 저장되었습니다",
                    Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    "템플릿 다운로드 실패: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(Intent.createChooser(intent, "CSV 파일 선택"));
    }

    private void parseCSVFile(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            List<TimetableEntry> entries = TimetableCSVParser.parseCSV(inputStream);

            // Firestore에 저장
            timetableRepository.addEntries(entries, new FirestoreManager.FirestoreCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Toast.makeText(requireContext(),
                            entries.size() + "개의 수업이 저장되었습니다",
                            Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(requireContext(),
                            "저장 실패: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            });

        } catch (TimetableCSVParser.ParseException e) {
            Toast.makeText(requireContext(),
                    "CSV 파싱 오류 (Line " + e.getLineNumber() + "): " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    "파일 읽기 실패: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void showRoomPicker() {
        if (availableRooms.isEmpty()) {
            Toast.makeText(requireContext(), "등록된 강의실이 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] roomNames = availableRooms.stream()
                .map(Room::getName)
                .toArray(String[]::new);

        new AlertDialog.Builder(requireContext())
                .setTitle("강의실 선택")
                .setItems(roomNames, (dialog, which) -> {
                    selectedRoom = availableRooms.get(which);
                    inputRoom.setText(selectedRoom.getName());
                })
                .show();
    }

    private void showDayPicker() {
        String[] days = {"월요일", "화요일", "수요일", "목요일", "금요일", "토요일", "일요일"};
        DayOfWeek[] dayValues = {
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
        };

        new AlertDialog.Builder(requireContext())
                .setTitle("요일 선택")
                .setItems(days, (dialog, which) -> {
                    selectedDay = dayValues[which];
                    inputDay.setText(days[which]);
                })
                .show();
    }

    private void showTimePicker(boolean isStartTime) {
        LocalTime currentTime = isStartTime ?
                (selectedStartTime != null ? selectedStartTime : LocalTime.of(9, 0)) :
                (selectedEndTime != null ? selectedEndTime : LocalTime.of(10, 30));

        new TimePickerDialog(requireContext(),
                (view, hourOfDay, minute) -> {
                    LocalTime time = LocalTime.of(hourOfDay, minute);
                    if (isStartTime) {
                        selectedStartTime = time;
                        inputStartTime.setText(time.toString());
                    } else {
                        selectedEndTime = time;
                        inputEndTime.setText(time.toString());
                    }
                },
                currentTime.getHour(),
                currentTime.getMinute(),
                true
        ).show();
    }

    private void handleAddCourse() {
        String courseName = inputCourseName.getText().toString().trim();
        String attendeesStr = inputAttendees.getText().toString().trim();
        String professor = inputProfessor.getText().toString().trim();
        String note = inputNote.getText().toString().trim();

        // Validation
        if (courseName.isEmpty()) {
            Toast.makeText(requireContext(), "과목명을 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedRoom == null) {
            Toast.makeText(requireContext(), "강의실을 선택하세요", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedDay == null) {
            Toast.makeText(requireContext(), "요일을 선택하세요", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedStartTime == null || selectedEndTime == null) {
            Toast.makeText(requireContext(), "시작/종료 시간을 선택하세요", Toast.LENGTH_SHORT).show();
            return;
        }
        if (attendeesStr.isEmpty()) {
            Toast.makeText(requireContext(), "수강인원을 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        int attendees;
        try {
            attendees = Integer.parseInt(attendeesStr);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "올바른 수강인원을 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedStartTime.isAfter(selectedEndTime)) {
            Toast.makeText(requireContext(), "시작시간이 종료시간보다 늦습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create entry
        TimetableEntry entry = new TimetableEntry(
                UUID.randomUUID().toString(),
                courseName,
                selectedRoom.getId(),
                selectedRoom.getName(),
                selectedDay,
                selectedStartTime,
                selectedEndTime,
                attendees,
                professor,
                note
        );

        // Firestore에 저장
        timetableRepository.addEntry(entry, new FirestoreManager.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                clearInputs();
                Toast.makeText(requireContext(), "수업이 추가되었습니다", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "추가 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleDeleteEntry(TimetableEntry entry) {
        new AlertDialog.Builder(requireContext())
                .setTitle("수업 삭제")
                .setMessage(entry.getCourseName() + " 수업을 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> {
                    // Firestore에서 삭제
                    timetableRepository.deleteEntry(entry.getId(), new FirestoreManager.FirestoreCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Toast.makeText(requireContext(), "삭제되었습니다", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(requireContext(), "삭제 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void clearInputs() {
        inputCourseName.setText("");
        inputRoom.setText("");
        inputDay.setText("");
        inputStartTime.setText("");
        inputEndTime.setText("");
        inputAttendees.setText("");
        inputProfessor.setText("");
        inputNote.setText("");
        selectedRoom = null;
        selectedDay = null;
        selectedStartTime = null;
        selectedEndTime = null;
    }

    private void updateEmptyState() {
        boolean isEmpty = adapter.getItemCount() == 0;
        textEmptyTimetable.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerTimetable.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    public List<TimetableEntry> getTimetableEntries() {
        return adapter.getEntries();
    }
}
