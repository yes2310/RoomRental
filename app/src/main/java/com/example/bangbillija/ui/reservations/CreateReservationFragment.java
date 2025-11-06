package com.example.bangbillija.ui.reservations;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.bangbillija.R;
import com.example.bangbillija.core.SharedReservationViewModel;
import com.example.bangbillija.data.ReservationRepository;
import com.example.bangbillija.databinding.FragmentCreateReservationBinding;
import com.example.bangbillija.model.Reservation;
import com.example.bangbillija.model.ReservationStatus;
import com.example.bangbillija.model.Room;
import com.example.bangbillija.service.AuthManager;
import com.example.bangbillija.service.FirestoreManager;
import com.example.bangbillija.ui.Navigator;
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
    private LocalTime startTime;
    private LocalTime endTime;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

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

        setupTimeDropdowns();
        observeViewModel();

        binding.buttonCreate.setOnClickListener(v -> createReservation());
        binding.buttonCancel.setOnClickListener(v -> {
            if (getActivity() instanceof Navigator) {
                requireActivity().onBackPressed();
            }
        });
    }

    private void setupTimeDropdowns() {
        List<String> timeSlots = generateTimeSlots();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, timeSlots);

        binding.inputStartTime.setAdapter(adapter);
        binding.inputEndTime.setAdapter(adapter);

        binding.inputStartTime.setOnItemClickListener((parent, v, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            startTime = LocalTime.parse(selected, timeFormatter);
            validateTimes();
        });

        binding.inputEndTime.setOnItemClickListener((parent, v, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            endTime = LocalTime.parse(selected, timeFormatter);
            validateTimes();
        });
    }

    private List<String> generateTimeSlots() {
        List<String> slots = new ArrayList<>();
        LocalTime current = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(21, 0);

        while (!current.isAfter(end)) {
            slots.add(current.format(timeFormatter));
            current = current.plusMinutes(30);
        }
        return slots;
    }

    private void observeViewModel() {
        viewModel.getSelectedRoom().observe(getViewLifecycleOwner(), room -> {
            selectedRoom = room;
            if (room != null) {
                binding.textRoomInfo.setText("강의실: " + room.getName());
            }
        });

        viewModel.getSelectedDate().observe(getViewLifecycleOwner(), date -> {
            selectedDate = date;
            if (date != null) {
                binding.textDateInfo.setText("날짜: " + date.format(dateFormatter));
            }
        });
    }

    private void validateTimes() {
        if (startTime != null && endTime != null) {
            if (!startTime.isBefore(endTime)) {
                binding.inputEndTimeLayout.setError("종료 시간은 시작 시간 이후여야 합니다");
            } else {
                binding.inputEndTimeLayout.setError(null);
            }
        }
    }

    private void createReservation() {
        // Clear errors
        binding.inputTitleLayout.setError(null);
        binding.inputAttendeesLayout.setError(null);
        binding.inputStartTimeLayout.setError(null);
        binding.inputEndTimeLayout.setError(null);

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

        if (startTime == null) {
            binding.inputStartTimeLayout.setError("시작 시간을 선택하세요");
            valid = false;
        }

        if (endTime == null) {
            binding.inputEndTimeLayout.setError("종료 시간을 선택하세요");
            valid = false;
        }

        if (startTime != null && endTime != null && !startTime.isBefore(endTime)) {
            binding.inputEndTimeLayout.setError("종료 시간은 시작 시간 이후여야 합니다");
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

        Reservation reservation = new Reservation(
                reservationId,
                selectedRoom.getId(),
                selectedRoom.getName(),
                title,
                ownerName,
                selectedDate,
                startTime,
                endTime,
                attendees,
                ReservationStatus.PENDING,
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
                Snackbar.make(binding.getRoot(), "예약이 생성되었습니다", Snackbar.LENGTH_SHORT).show();
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
        binding.inputTitle.setEnabled(!loading);
        binding.inputStartTime.setEnabled(!loading);
        binding.inputEndTime.setEnabled(!loading);
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
