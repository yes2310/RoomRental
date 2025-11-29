package com.example.bangbillija.ui.reservations;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.bangbillija.core.SharedReservationViewModel;
import com.example.bangbillija.data.ReservationRepository;
import com.example.bangbillija.databinding.FragmentCreateReservationBinding;
import com.example.bangbillija.model.Reservation;
import com.example.bangbillija.model.ReservationStatus;
import com.example.bangbillija.model.Room;
import com.example.bangbillija.service.AuthManager;
import com.example.bangbillija.service.FirestoreManager;
import com.example.bangbillija.ui.Navigator;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseUser;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CreateReservationFragment extends Fragment {

    private FragmentCreateReservationBinding binding;
    private SharedReservationViewModel viewModel;
    private ReservationRepository reservationRepository;
    private AuthManager authManager;

    private Room selectedRoom;
    private LocalDate selectedDate;
    private LocalTime selectedStartTime;
    private LocalTime selectedEndTime;
    private List<Reservation> existingReservations = new ArrayList<>();
    private List<com.example.bangbillija.model.TimetableEntry> existingTimetable = new ArrayList<>();

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    // 30분 단위 시간 슬롯 (9:00 ~ 21:00)
    private static final List<LocalTime> TIME_SLOTS = generateTimeSlots();

    private static List<LocalTime> generateTimeSlots() {
        List<LocalTime> slots = new ArrayList<>();
        LocalTime time = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(21, 0);

        while (!time.isAfter(endTime)) {
            slots.add(time);
            time = time.plusMinutes(30);
        }
        return slots;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCreateReservationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(SharedReservationViewModel.class);
        reservationRepository = ReservationRepository.getInstance();
        authManager = AuthManager.getInstance();

        observeViewModel();
        setupButtons();
    }

    private void setupButtons() {
        binding.buttonSelectDate.setOnClickListener(v -> showDatePicker());
        binding.buttonSelectStartTime.setOnClickListener(v -> showStartTimePicker());
        binding.buttonSelectEndTime.setOnClickListener(v -> showEndTimePicker());
        binding.buttonCreate.setOnClickListener(v -> createReservation());
        binding.buttonCancel.setOnClickListener(v -> {
            if (getActivity() instanceof Navigator) {
                requireActivity().onBackPressed();
            }
        });
    }

    private void showDatePicker() {
        LocalDate today = LocalDate.now();
        LocalDate initialDate = selectedDate != null ? selectedDate : today;

        DatePickerDialog datePicker = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedDate = LocalDate.of(year, month + 1, dayOfMonth);
                    binding.textDateInfo.setText("날짜: " + selectedDate.format(dateFormatter));

                    // 날짜 변경 시 시간 선택 초기화
                    resetTimeSelection();

                    // 해당 날짜의 기존 예약 로드
                    loadExistingReservations();
                },
                initialDate.getYear(),
                initialDate.getMonthValue() - 1,
                initialDate.getDayOfMonth()
        );

        datePicker.getDatePicker().setMinDate(today.toEpochDay() * 24 * 60 * 60 * 1000);
        datePicker.show();
    }

    private void showStartTimePicker() {
        if (selectedRoom == null || selectedDate == null) {
            Snackbar.make(binding.getRoot(), "먼저 강의실과 날짜를 선택하세요", Snackbar.LENGTH_SHORT).show();
            return;
        }

        List<LocalTime> validStartTimes = TIME_SLOTS.stream()
                .filter(time -> time.isBefore(LocalTime.of(21, 0))) // 마지막 시작 시간은 20:30
                .collect(java.util.stream.Collectors.toList());

        String[] timeStrings = validStartTimes.stream()
                .map(time -> {
                    // 해당 시간이 예약 가능한지 확인
                    boolean isBlocked = isTimeBlocked(time);
                    String timeStr = time.format(timeFormatter);

                    if (isBlocked) {
                        // 예약 불가능한 시간에 표시 추가
                        return timeStr + " (예약불가)";
                    }
                    return timeStr;
                })
                .toArray(String[]::new);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("시작 시간 선택")
                .setItems(timeStrings, (dialog, which) -> {
                    LocalTime selectedTime = validStartTimes.get(which);

                    // 예약 불가능한 시간 선택 시 경고
                    if (isTimeBlocked(selectedTime)) {
                        Snackbar.make(binding.getRoot(),
                                "해당 시간은 이미 예약되어 있거나 수업이 있습니다",
                                Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    selectedStartTime = selectedTime;
                    selectedEndTime = null; // 종료 시간 초기화

                    binding.buttonSelectStartTime.setText("시작: " + selectedStartTime.format(timeFormatter));
                    binding.buttonSelectEndTime.setEnabled(true);
                    binding.buttonSelectEndTime.setText("종료 시간");
                    updateSelectedTimeDisplay();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    /**
     * 특정 시작 시간이 차단되어 있는지 확인
     */
    private boolean isTimeBlocked(LocalTime startTime) {
        if (selectedDate == null || selectedRoom == null) {
            return false;
        }

        // 최소 1시간 예약이므로 종료 시간 계산
        LocalTime endTime = startTime.plusHours(1);

        // 기존 예약과 충돌 확인
        for (Reservation reservation : existingReservations) {
            if (timesOverlap(startTime, endTime, reservation.getStartTime(), reservation.getEndTime())) {
                return true;
            }
        }

        // 시간표와 충돌 확인
        java.time.DayOfWeek dayOfWeek = selectedDate.getDayOfWeek();
        for (com.example.bangbillija.model.TimetableEntry entry : existingTimetable) {
            if (entry.getDayOfWeek() == dayOfWeek) {
                if (timesOverlap(startTime, endTime, entry.getStartTime(), entry.getEndTime())) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 두 시간 범위가 겹치는지 확인
     */
    private boolean timesOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }

    private void showEndTimePicker() {
        if (selectedStartTime == null) {
            Snackbar.make(binding.getRoot(), "먼저 시작 시간을 선택하세요", Snackbar.LENGTH_SHORT).show();
            return;
        }

        // 최소 1시간 이후부터 선택 가능
        LocalTime minEndTime = selectedStartTime.plusHours(1);
        LocalTime maxEndTime = LocalTime.of(21, 0);

        List<LocalTime> availableEndTimes = TIME_SLOTS.stream()
                .filter(time -> !time.isBefore(minEndTime) && !time.isAfter(maxEndTime))
                .collect(java.util.stream.Collectors.toList());

        if (availableEndTimes.isEmpty()) {
            Snackbar.make(binding.getRoot(), "선택 가능한 종료 시간이 없습니다", Snackbar.LENGTH_SHORT).show();
            return;
        }

        String[] timeStrings = availableEndTimes.stream()
                .map(time -> {
                    // 해당 종료 시간이 예약 가능한지 확인
                    boolean isBlocked = isEndTimeBlocked(time);
                    String timeStr = time.format(timeFormatter);

                    if (isBlocked) {
                        return timeStr + " (예약불가)";
                    }
                    return timeStr;
                })
                .toArray(String[]::new);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("종료 시간 선택")
                .setItems(timeStrings, (dialog, which) -> {
                    LocalTime selectedTime = availableEndTimes.get(which);

                    // 예약 불가능한 시간 선택 시 경고
                    if (isEndTimeBlocked(selectedTime)) {
                        Snackbar.make(binding.getRoot(),
                                "선택한 시간에 이미 예약이 있거나 수업이 있습니다. 다른 시간을 선택하세요",
                                Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    selectedEndTime = selectedTime;
                    binding.buttonSelectEndTime.setText("종료: " + selectedEndTime.format(timeFormatter));
                    updateSelectedTimeDisplay();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    /**
     * 선택한 시작 시간부터 종료 시간까지 예약 가능한지 확인
     */
    private boolean isEndTimeBlocked(LocalTime endTime) {
        if (selectedDate == null || selectedRoom == null || selectedStartTime == null) {
            return false;
        }

        // 기존 예약과 충돌 확인
        for (Reservation reservation : existingReservations) {
            if (timesOverlap(selectedStartTime, endTime, reservation.getStartTime(), reservation.getEndTime())) {
                return true;
            }
        }

        // 시간표와 충돌 확인
        java.time.DayOfWeek dayOfWeek = selectedDate.getDayOfWeek();
        for (com.example.bangbillija.model.TimetableEntry entry : existingTimetable) {
            if (entry.getDayOfWeek() == dayOfWeek) {
                if (timesOverlap(selectedStartTime, endTime, entry.getStartTime(), entry.getEndTime())) {
                    return true;
                }
            }
        }

        return false;
    }

    private void resetTimeSelection() {
        selectedStartTime = null;
        selectedEndTime = null;
        binding.buttonSelectStartTime.setText("시작 시간");
        binding.buttonSelectEndTime.setText("종료 시간");
        binding.buttonSelectEndTime.setEnabled(false);
        updateSelectedTimeDisplay();
    }

    private void updateSelectedTimeDisplay() {
        if (selectedStartTime == null) {
            binding.textSelectedTime.setText("선택된 시간: -");
        } else if (selectedEndTime == null) {
            binding.textSelectedTime.setText("시작: " + selectedStartTime.format(timeFormatter) + " (종료 시간을 선택하세요)");
        } else {
            long hours = java.time.Duration.between(selectedStartTime, selectedEndTime).toHours();
            long minutes = java.time.Duration.between(selectedStartTime, selectedEndTime).toMinutes() % 60;
            String duration = hours + "시간" + (minutes > 0 ? " " + minutes + "분" : "");
            binding.textSelectedTime.setText(
                    selectedStartTime.format(timeFormatter) + " ~ " +
                    selectedEndTime.format(timeFormatter) + " (" + duration + ")");
        }
    }

    private void loadExistingReservations() {
        if (selectedRoom == null || selectedDate == null) {
            return;
        }

        // 예약 로드
        reservationRepository.getReservationsByRoomAndDate(
                selectedRoom.getId(),
                selectedDate,
                new FirestoreManager.FirestoreCallback<List<Reservation>>() {
                    @Override
                    public void onSuccess(List<Reservation> reservations) {
                        // CANCELLED 상태가 아닌 예약만 필터링
                        existingReservations = reservations.stream()
                                .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
                                .collect(java.util.stream.Collectors.toList());

                        android.util.Log.d("CreateReservation", "Loaded " + existingReservations.size() + " existing reservations");
                    }

                    @Override
                    public void onFailure(Exception e) {
                        android.util.Log.e("CreateReservation", "Failed to load reservations", e);
                        existingReservations.clear();
                    }
                });

        // 시간표 로드 (수업 시간과 충돌 방지)
        FirestoreManager.getInstance()
                .getTimetableEntriesForRoomAndDay(
                        selectedRoom.getId(),
                        selectedDate.getDayOfWeek(),
                        new FirestoreManager.FirestoreCallback<List<com.example.bangbillija.model.TimetableEntry>>() {
                            @Override
                            public void onSuccess(List<com.example.bangbillija.model.TimetableEntry> timetable) {
                                existingTimetable = timetable;
                                android.util.Log.d("CreateReservation", "Loaded " + existingTimetable.size() + " timetable entries");
                            }

                            @Override
                            public void onFailure(Exception e) {
                                android.util.Log.e("CreateReservation", "Failed to load timetable", e);
                                existingTimetable.clear();
                            }
                        });
    }

    private boolean hasTimeConflict() {
        if (selectedStartTime == null || selectedEndTime == null) {
            return false;
        }

        // 기존 예약과의 충돌 검사
        for (Reservation reservation : existingReservations) {
            // 겹치는지 확인: 새 예약의 시작이 기존 예약 끝 전이고, 새 예약의 끝이 기존 예약 시작 후
            boolean overlaps = selectedStartTime.isBefore(reservation.getEndTime()) &&
                              selectedEndTime.isAfter(reservation.getStartTime());

            if (overlaps) {
                android.util.Log.d("CreateReservation", "Time conflict with reservation: " +
                        reservation.getStartTime() + " - " + reservation.getEndTime());
                return true;
            }
        }

        // 시간표(수업 시간)와의 충돌 검사
        for (com.example.bangbillija.model.TimetableEntry timetableEntry : existingTimetable) {
            boolean overlaps = selectedStartTime.isBefore(timetableEntry.getEndTime()) &&
                              selectedEndTime.isAfter(timetableEntry.getStartTime());

            if (overlaps) {
                android.util.Log.d("CreateReservation", "Time conflict with class: " +
                        timetableEntry.getCourseName() + " (" +
                        timetableEntry.getStartTime() + " - " + timetableEntry.getEndTime() + ")");
                return true;
            }
        }

        return false;
    }

    private void observeViewModel() {
        viewModel.getSelectedRoom().observe(getViewLifecycleOwner(), room -> {
            selectedRoom = room;
            if (room != null) {
                binding.textRoomInfo.setText("강의실: " + room.getName());
                if (selectedDate != null) {
                    loadExistingReservations();
                }
            }
        });

        // Initialize date from ViewModel if already selected
        LocalDate date = viewModel.getSelectedDate().getValue();
        if (date != null) {
            selectedDate = date;
            binding.textDateInfo.setText("날짜: " + date.format(dateFormatter));
            if (selectedRoom != null) {
                loadExistingReservations();
            }
        }
    }

    private void createReservation() {
        // Clear errors
        binding.inputTitleLayout.setError(null);
        binding.inputAttendeesLayout.setError(null);

        // Validate inputs
        String title = getTrimmed(binding.inputTitle.getText());
        String attendeesStr = getTrimmed(binding.inputAttendees.getText());
        String note = getTrimmed(binding.inputNote.getText());

        boolean valid = true;

        if (TextUtils.isEmpty(title)) {
            binding.inputTitleLayout.setError("제목을 입력하세요");
            valid = false;
        }

        if (TextUtils.isEmpty(attendeesStr)) {
            binding.inputAttendeesLayout.setError("참석 인원을 입력하세요");
            valid = false;
        }

        if (selectedStartTime == null || selectedEndTime == null) {
            Snackbar.make(binding.getRoot(), "시작 시간과 종료 시간을 선택하세요", Snackbar.LENGTH_SHORT).show();
            valid = false;
        }

        if (selectedRoom == null) {
            Snackbar.make(binding.getRoot(), "강의실을 선택하세요", Snackbar.LENGTH_SHORT).show();
            valid = false;
        }

        if (selectedDate == null) {
            Snackbar.make(binding.getRoot(), "날짜를 선택하세요", Snackbar.LENGTH_SHORT).show();
            valid = false;
        }

        if (!valid) {
            return;
        }

        // 시간 충돌 재확인
        if (hasTimeConflict()) {
            Snackbar.make(binding.getRoot(),
                    "선택한 시간에 이미 예약이 있습니다. 다른 시간을 선택하세요",
                    Snackbar.LENGTH_LONG).show();
            return;
        }

        int attendees;
        try {
            attendees = Integer.parseInt(attendeesStr);
            if (attendees <= 0) {
                binding.inputAttendeesLayout.setError("참석 인원은 1명 이상이어야 합니다");
                return;
            }
            if (selectedRoom != null && attendees > selectedRoom.getCapacity()) {
                binding.inputAttendeesLayout.setError("강의실 수용 인원(" + selectedRoom.getCapacity() + "명)을 초과했습니다");
                return;
            }
        } catch (NumberFormatException e) {
            binding.inputAttendeesLayout.setError("올바른 숫자를 입력하세요");
            return;
        }

        // Get current user
        FirebaseUser user = authManager.currentUser();
        if (user == null) {
            Snackbar.make(binding.getRoot(), "로그인이 필요합니다", Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Show loading
        setLoading(true);

        // Create reservation ID
        String reservationId = generateReservationId();
        String ownerName = user.getDisplayName() != null ? user.getDisplayName() : user.getEmail();
        String userId = user.getUid();
        String userEmail = user.getEmail();

        // 사용자의 학번 가져오기
        FirestoreManager.getInstance().getUserStudentId(userId, new FirestoreManager.FirestoreCallback<String>() {
            @Override
            public void onSuccess(String studentId) {
                // 관리자 승인 없이 바로 RESERVED 상태로 생성
                Reservation reservation = new Reservation(
                        reservationId,
                        selectedRoom.getId(),
                        selectedRoom.getName(),
                        title,
                        userId,  // owner 필드는 userId로 설정 (필터링에 사용)
                        studentId,  // 학번 추가
                        selectedDate,
                        selectedStartTime,
                        selectedEndTime,
                        attendees,
                        ReservationStatus.RESERVED,
                        note
                );

                // Create in repository
                reservationRepository.createReservation(reservation, userId, userEmail, new FirestoreManager.FirestoreCallback<String>() {
                    @Override
                    public void onSuccess(String documentId) {
                        setLoading(false);
                        Snackbar.make(binding.getRoot(), "예약이 확정되었습니다!", Snackbar.LENGTH_LONG).show();
                        if (getActivity() != null) {
                            requireActivity().onBackPressed();
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        setLoading(false);
                        Snackbar.make(binding.getRoot(), "예약 생성 실패: " + e.getMessage(),
                                Snackbar.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                setLoading(false);
                Snackbar.make(binding.getRoot(), "학번 조회 실패: " + e.getMessage(),
                        Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private String generateReservationId() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String datePart = selectedDate.format(formatter);
        String uniquePart = UUID.randomUUID().toString().substring(0, 3).toUpperCase();
        return "RS-" + datePart + "-" + uniquePart;
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.buttonCreate.setEnabled(!loading);
        binding.buttonCancel.setEnabled(!loading);
        binding.buttonSelectDate.setEnabled(!loading);
        binding.buttonSelectStartTime.setEnabled(!loading);
        binding.buttonSelectEndTime.setEnabled(!loading && selectedStartTime != null);
        binding.inputTitle.setEnabled(!loading);
        binding.inputAttendees.setEnabled(!loading);
        binding.inputNote.setEnabled(!loading);
    }

    private String getTrimmed(@Nullable CharSequence text) {
        return text == null ? "" : text.toString().trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
