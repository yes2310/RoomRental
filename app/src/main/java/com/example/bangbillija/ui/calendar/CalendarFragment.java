package com.example.bangbillija.ui.calendar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.bangbillija.core.SharedReservationViewModel;
import com.example.bangbillija.databinding.FragmentCalendarBinding;
import com.example.bangbillija.model.Reservation;
import com.example.bangbillija.model.Room;
import com.example.bangbillija.ui.Navigator;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CalendarFragment extends Fragment implements WeekDayAdapter.DayClickListener, TimeSlotAdapter.SlotClickListener {

    private FragmentCalendarBinding binding;
    private SharedReservationViewModel viewModel;
    private WeekDayAdapter weekDayAdapter;
    private TimeSlotAdapter timeSlotAdapter;
    private LocalDate currentWeekStart;
    private final DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy년 M월");

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

        weekDayAdapter = new WeekDayAdapter(this);
        binding.recyclerWeek.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerWeek.setAdapter(weekDayAdapter);

        timeSlotAdapter = new TimeSlotAdapter(this);
        binding.recyclerTimeSlots.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerTimeSlots.setAdapter(timeSlotAdapter);

        binding.buttonPrevWeek.setOnClickListener(v -> shiftWeek(-1));
        binding.buttonNextWeek.setOnClickListener(v -> shiftWeek(1));
        binding.buttonToday.setOnClickListener(v -> viewModel.selectDate(LocalDate.now()));

        viewModel.getRooms().observe(getViewLifecycleOwner(), rooms -> ensureRoomSelection(rooms));
        viewModel.getSelectedDate().observe(getViewLifecycleOwner(), this::updateWeek);
        viewModel.getTimeSlots().observe(getViewLifecycleOwner(), timeSlotAdapter::submitList);
    }

    private void ensureRoomSelection(List<Room> rooms) {
        if (rooms == null || rooms.isEmpty()) {
            return;
        }
        Room selected = viewModel.getSelectedRoom().getValue();
        if (selected == null) {
            viewModel.selectRoom(rooms.get(0));
        }
    }

    private void updateWeek(LocalDate selectedDate) {
        if (selectedDate == null) {
            selectedDate = LocalDate.now();
        }
        currentWeekStart = selectedDate.with(DayOfWeek.MONDAY);
        binding.textMonth.setText(selectedDate.format(monthFormatter));

        List<LocalDate> week = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            week.add(currentWeekStart.plusDays(i));
        }
        weekDayAdapter.submitList(week);
        weekDayAdapter.setSelectedDate(selectedDate);
    }

    private void shiftWeek(int offset) {
        LocalDate selected = viewModel.getSelectedDate().getValue();
        if (selected == null) {
            selected = LocalDate.now();
        }
        viewModel.selectDate(selected.plusWeeks(offset));
    }

    @Override
    public void onDayClicked(LocalDate date) {
        viewModel.selectDate(date);
    }

    @Override
    public void onReservationSelected(Reservation reservation) {
        viewModel.focusReservation(reservation);
        if (getActivity() instanceof Navigator) {
            ((Navigator) getActivity()).openReservationDetail();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
