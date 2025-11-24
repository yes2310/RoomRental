package com.example.bangbillija.ui.calendar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.bangbillija.core.SharedReservationViewModel;
import com.example.bangbillija.databinding.FragmentCalendarBinding;
import com.example.bangbillija.model.Reservation;
import com.example.bangbillija.model.Room;
import com.example.bangbillija.service.AuthManager;
import com.example.bangbillija.service.FirestoreManager;
import com.example.bangbillija.ui.Navigator;
import com.example.bangbillija.ui.reservations.MyReservationsAdapter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CalendarFragment extends Fragment {

    private FragmentCalendarBinding binding;
    private SharedReservationViewModel viewModel;
    private AuthManager authManager;
    private MyReservationsAdapter reservationAdapter;
    private List<Reservation> allReservations = new ArrayList<>();
    private LocalDate selectedDate = null;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)", Locale.KOREAN);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCalendarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(SharedReservationViewModel.class);
        authManager = AuthManager.getInstance();

        // Rooms 데이터 로드를 위해 observe
        viewModel.getRooms().observe(getViewLifecycleOwner(), rooms -> {
            // Rooms 데이터가 로드되면 자동으로 업데이트됨
        });

        setupRecyclerView();
        setupCalendar();
        loadReservations();
    }

    private void setupRecyclerView() {
        reservationAdapter = new MyReservationsAdapter(new MyReservationsAdapter.ReservationClickListener() {
            @Override
            public void onPrimaryAction(Reservation reservation) {
                viewModel.focusReservation(reservation);

                // 상세보기 화면을 위해 Room 정보도 설정
                List<Room> rooms = viewModel.getRooms().getValue();
                if (rooms != null) {
                    Room matchingRoom = rooms.stream()
                            .filter(r -> r.getId().equals(reservation.getRoomId()))
                            .findFirst()
                            .orElse(null);

                    if (matchingRoom != null) {
                        viewModel.selectRoom(matchingRoom);
                    }
                }

                if (getActivity() instanceof Navigator) {
                    ((Navigator) getActivity()).openReservationDetail();
                }
            }

            @Override
            public void onSecondaryAction(Reservation reservation) {
                // Handle secondary action if needed
            }
        });

        binding.recyclerReservations.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerReservations.setAdapter(reservationAdapter);
        binding.recyclerReservations.setVisibility(View.GONE);
    }

    private void setupCalendar() {
        // 오늘 날짜로 초기화
        selectedDate = LocalDate.now();
        updateSelectedDateInfo();

        binding.calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                selectedDate = LocalDate.of(year, month + 1, dayOfMonth);
                updateSelectedDateInfo();
                showReservationsForDate(selectedDate);
            }
        });
    }

    private void loadReservations() {
        if (authManager.currentUser() == null) {
            return;
        }

        String userId = authManager.currentUser().getUid();
        FirestoreManager firestoreManager = FirestoreManager.getInstance();

        firestoreManager.getReservationsByUser(userId, new FirestoreManager.FirestoreCallback<>() {
            @Override
            public void onSuccess(List<Reservation> reservations) {
                allReservations = reservations;
                // 현재 선택된 날짜의 예약 표시
                if (selectedDate != null) {
                    showReservationsForDate(selectedDate);
                }
            }

            @Override
            public void onFailure(Exception e) {
                // Handle error silently
            }
        });
    }

    private void updateSelectedDateInfo() {
        if (selectedDate != null) {
            binding.textSelectedDate.setText(selectedDate.format(dateFormatter));
        }
    }

    private void showReservationsForDate(LocalDate date) {
        List<Reservation> dayReservations = new ArrayList<>();

        for (Reservation reservation : allReservations) {
            if (reservation.getDate().equals(date)) {
                dayReservations.add(reservation);
            }
        }

        if (dayReservations.isEmpty()) {
            binding.recyclerReservations.setVisibility(View.GONE);
            binding.textEmpty.setVisibility(View.VISIBLE);
            binding.textEmpty.setText("선택한 날짜에 예약이 없습니다");
            binding.textReservationCount.setVisibility(View.GONE);
        } else {
            binding.recyclerReservations.setVisibility(View.VISIBLE);
            binding.textEmpty.setVisibility(View.GONE);
            binding.textReservationCount.setVisibility(View.VISIBLE);
            binding.textReservationCount.setText(dayReservations.size() + "개 예약");
            reservationAdapter.submitList(dayReservations);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // 화면이 다시 보일 때 예약 목록 새로고침
        loadReservations();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
