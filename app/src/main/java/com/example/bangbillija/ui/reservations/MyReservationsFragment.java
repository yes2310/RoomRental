package com.example.bangbillija.ui.reservations;

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
import com.example.bangbillija.databinding.FragmentMyReservationsBinding;
import com.example.bangbillija.model.Reservation;
import com.example.bangbillija.model.ReservationStatus;
import com.example.bangbillija.ui.Navigator;
import com.google.android.material.snackbar.Snackbar;

import java.util.Collections;
import java.util.List;

public class MyReservationsFragment extends Fragment implements MyReservationsAdapter.ReservationClickListener {

    private FragmentMyReservationsBinding binding;
    private SharedReservationViewModel viewModel;
    private MyReservationsAdapter adapter;
    private List<Reservation> upcoming = Collections.emptyList();
    private List<Reservation> past = Collections.emptyList();
    private List<Reservation> cancelled = Collections.emptyList();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMyReservationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(SharedReservationViewModel.class);

        adapter = new MyReservationsAdapter(this);
        binding.recyclerReservations.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerReservations.setAdapter(adapter);

        binding.chipGroupFilters.setOnCheckedStateChangeListener((group, ids) -> refreshList());
        binding.buttonCreateReservation.setOnClickListener(v ->
                Snackbar.make(v, "새 예약 기능은 아직 구현되지 않았습니다", Snackbar.LENGTH_SHORT).show());

        viewModel.getUpcomingReservations().observe(getViewLifecycleOwner(), reservations -> {
            upcoming = reservations;
            refreshList();
        });
        viewModel.getPastReservations().observe(getViewLifecycleOwner(), reservations -> {
            past = reservations;
            refreshList();
        });
        viewModel.getCancelledReservations().observe(getViewLifecycleOwner(), reservations -> {
            cancelled = reservations;
            refreshList();
        });
    }

    private void refreshList() {
        if (binding == null) {
            return;
        }
        int checkedId = binding.chipGroupFilters.getCheckedChipId();
        if (checkedId == binding.chipPast.getId()) {
            adapter.submitList(past == null ? Collections.emptyList() : new java.util.ArrayList<>(past));
        } else if (checkedId == binding.chipCancelled.getId()) {
            adapter.submitList(cancelled == null ? Collections.emptyList() : new java.util.ArrayList<>(cancelled));
        } else {
            adapter.submitList(upcoming == null ? Collections.emptyList() : new java.util.ArrayList<>(upcoming));
        }
    }

    @Override
    public void onPrimaryAction(Reservation reservation) {
        handleReservationAction(reservation, true);
    }

    @Override
    public void onSecondaryAction(Reservation reservation) {
        handleReservationAction(reservation, false);
    }

    private void handleReservationAction(Reservation reservation, boolean primary) {
        viewModel.focusReservation(reservation);
        if (primary && reservation.getStatus() == ReservationStatus.RESERVED) {
            if (getActivity() instanceof Navigator) {
                ((Navigator) getActivity()).openQrScanner();
            }
        } else {
            if (getActivity() instanceof Navigator) {
                ((Navigator) getActivity()).openReservationDetail();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
