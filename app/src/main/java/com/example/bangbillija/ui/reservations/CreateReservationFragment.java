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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.bangbillija.R;
import com.example.bangbillija.core.SharedReservationViewModel;
import com.example.bangbillija.data.ReservationRepository;
import com.example.bangbillija.databinding.FragmentCreateReservationBinding;
import com.example.bangbillija.model.Reservation;
import com.example.bangbillija.model.ReservationStatus;
import com.example.bangbillija.model.Room;
import com.example.bangbillija.model.TimeSlot;
import com.example.bangbillija.service.AuthManager;
import com.example.bangbillija.service.FirestoreManager;
import com.example.bangbillija.ui.Navigator;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseUser;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public class CreateReservationFragment extends Fragment {

    private FragmentCreateReservationBinding binding;
    private SharedReservationViewModel viewModel;
    private ReservationRepository reservationRepository;
    private AuthManager authManager;
    private TimeSlotSelectionAdapter slotAdapter;

    private Room selectedRoom;
    private LocalDate selectedDate;
    private TimeSlot selectedTimeSlot;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)");

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

        setupRecyclerView();
        observeViewModel();

        binding.buttonSelectDate.setOnClickListener(v -> showDatePicker());
        binding.buttonCreate.setOnClickListener(v -> createReservation());
        binding.buttonCancel.setOnClickListener(v -> {
            if (getActivity() instanceof Navigator) {
                requireActivity().onBackPressed();
            }
        });
    }

    private void setupRecyclerView() {
        slotAdapter = new TimeSlotSelectionAdapter();
        slotAdapter.setOnSlotSelectedListener(slot -> {
            selectedTimeSlot = slot;
            binding.textSlotHint.setText("선택된 시간: " + slot.getDisplayTime());
        });

        binding.recyclerTimeSlots.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerTimeSlots.setAdapter(slotAdapter);
    }

    private void showDatePicker() {
        LocalDate today = LocalDate.now();
        LocalDate initialDate = selectedDate != null ? selectedDate : today;

        DatePickerDialog datePicker = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedDate = LocalDate.of(year, month + 1, dayOfMonth);
                    binding.textDateInfo.setText("날짜: " + selectedDate.format(dateFormatter));
                    loadAvailableSlots();
                },
                initialDate.getYear(),
                initialDate.getMonthValue() - 1,
                initialDate.getDayOfMonth()
        );

        // Prevent selecting past dates
        datePicker.getDatePicker().setMinDate(today.toEpochDay() * 24 * 60 * 60 * 1000);
        datePicker.show();
    }

    private void observeViewModel() {
        viewModel.getSelectedRoom().observe(getViewLifecycleOwner(), room -> {
            selectedRoom = room;
            if (room != null) {
                binding.textRoomInfo.setText("강의실: " + room.getName());
                loadAvailableSlots();
            }
        });

        // Initialize date from ViewModel if already selected
        LocalDate date = viewModel.getSelectedDate().getValue();
        if (date != null) {
            selectedDate = date;
            binding.textDateInfo.setText("날짜: " + date.format(dateFormatter));
        }
    }

    private void loadAvailableSlots() {
        if (selectedRoom == null || selectedDate == null) {
            binding.textSlotHint.setText("강의실과 날짜를 선택하면 예약 가능한 시간대가 표시됩니다");
            slotAdapter.setSlots(null);
            return;
        }

        // Show loading
        binding.textSlotHint.setText("시간대를 불러오는 중...");

        // Fetch slots from repository
        reservationRepository.buildSlotsFor(selectedRoom.getId(), selectedDate, new FirestoreManager.FirestoreCallback<List<TimeSlot>>() {
            @Override
            public void onSuccess(List<TimeSlot> slots) {
                slotAdapter.setSlots(slots);
                binding.textSlotHint.setText("예약 가능한 시간대를 선택하세요 (2시간 단위)");
            }

            @Override
            public void onFailure(Exception e) {
                binding.textSlotHint.setText("시간대 로드 실패: " + e.getMessage());
                Snackbar.make(binding.getRoot(), "시간대를 불러올 수 없습니다", Snackbar.LENGTH_SHORT).show();
            }
        });
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

        if (selectedTimeSlot == null) {
            Snackbar.make(binding.getRoot(), "시간대를 선택하세요", Snackbar.LENGTH_SHORT).show();
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

        // Create reservation ID
        String reservationId = generateReservationId();
        String ownerName = user.getDisplayName() != null ? user.getDisplayName() : user.getEmail();

        // 강의 시간과 겹치는지 확인
        boolean isClassTime = selectedTimeSlot.getTimetableEntry() != null;
        ReservationStatus initialStatus = isClassTime ? ReservationStatus.PENDING : ReservationStatus.RESERVED;

        // Use selected time slot's start and end times
        Reservation reservation = new Reservation(
                reservationId,
                selectedRoom.getId(),
                selectedRoom.getName(),
                title,
                ownerName,
                selectedDate,
                selectedTimeSlot.getStart(),
                selectedTimeSlot.getEnd(),
                attendees,
                initialStatus,
                note
        );

        // Show loading
        setLoading(true);

        // Create in repository
        String userId = user.getUid();
        String userEmail = user.getEmail();

        reservationRepository.createReservation(reservation, userId, userEmail, new FirestoreManager.FirestoreCallback<String>() {
            @Override
            public void onSuccess(String documentId) {
                setLoading(false);

                // 상태에 따라 다른 메시지 표시
                String message;
                if (isClassTime) {
                    message = "예약이 생성되었습니다\n강의 시간과 겹치므로 관리자 승인을 기다리는 중입니다";
                } else {
                    message = "예약이 확정되었습니다!\n10분 이내에 QR 체크인하지 않으면 자동 취소됩니다";
                }

                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
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
        binding.inputTitle.setEnabled(!loading);
        binding.inputAttendees.setEnabled(!loading);
        binding.inputNote.setEnabled(!loading);
        binding.recyclerTimeSlots.setEnabled(!loading);
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
