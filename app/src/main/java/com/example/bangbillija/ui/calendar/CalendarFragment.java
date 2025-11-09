package com.example.bangbillija.ui.calendar;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.bangbillija.R;
import com.example.bangbillija.data.ReservationRepository;
import com.example.bangbillija.databinding.FragmentCalendarBinding;
import com.example.bangbillija.model.Reservation;
import com.example.bangbillija.service.AuthManager;
import com.example.bangbillija.service.FirestoreManager;
import com.example.bangbillija.ui.Navigator;
import com.example.bangbillija.ui.reservations.MyReservationsAdapter;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CalendarFragment extends Fragment {

    private FragmentCalendarBinding binding;
    private ReservationRepository reservationRepository;
    private AuthManager authManager;
    private MyReservationsAdapter reservationAdapter;
    private List<Reservation> allReservations = new ArrayList<>();
    private LocalDate selectedDate = null;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)");

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

        reservationRepository = ReservationRepository.getInstance();
        authManager = AuthManager.getInstance();

        setupRecyclerView();
        setupCalendar();
        loadReservations();
    }

    private void setupRecyclerView() {
        reservationAdapter = new MyReservationsAdapter();
        reservationAdapter.setOnReservationClickListener(reservation -> {
            if (getActivity() instanceof Navigator) {
                ((Navigator) getActivity()).openReservationDetail();
            }
        });

        binding.recyclerReservations.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerReservations.setAdapter(reservationAdapter);
        binding.recyclerReservations.setVisibility(View.GONE);
    }

    private void setupCalendar() {
        binding.calendarView.setSelectedDate(CalendarDay.today());

        binding.calendarView.setOnDateChangedListener((widget, date, selected) -> {
            selectedDate = LocalDate.of(date.getYear(), date.getMonth(), date.getDay());
            updateSelectedDateInfo();
            showReservationsForDate(selectedDate);
        });
    }

    private void loadReservations() {
        if (authManager.currentUser() == null) {
            return;
        }

        String userId = authManager.currentUser().getUid();
        FirestoreManager firestoreManager = FirestoreManager.getInstance();

        firestoreManager.getReservationsByUser(userId, new FirestoreManager.FirestoreCallback<List<Reservation>>() {
            @Override
            public void onSuccess(List<Reservation> reservations) {
                allReservations = reservations;
                updateCalendarDots();
            }

            @Override
            public void onFailure(Exception e) {
                // Handle error silently
            }
        });
    }

    private void updateCalendarDots() {
        // 예약이 있는 날짜들을 수집
        Set<CalendarDay> datesWithReservations = new HashSet<>();

        for (Reservation reservation : allReservations) {
            LocalDate date = reservation.getDate();
            datesWithReservations.add(CalendarDay.from(
                    date.getYear(),
                    date.getMonthValue(),
                    date.getDayOfMonth()
            ));
        }

        // 점 데코레이터 추가
        binding.calendarView.removeDecorators();
        binding.calendarView.addDecorator(new DotDecorator(Color.parseColor("#FF6200EE"), datesWithReservations));
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
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // 점 데코레이터 클래스
    private static class DotDecorator implements DayViewDecorator {
        private final int color;
        private final Set<CalendarDay> dates;

        DotDecorator(int color, Set<CalendarDay> dates) {
            this.color = color;
            this.dates = dates;
        }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return dates.contains(day);
        }

        @Override
        public void decorate(DayViewFacade view) {
            view.addSpan(new DotSpan(5, color));
        }
    }
}
