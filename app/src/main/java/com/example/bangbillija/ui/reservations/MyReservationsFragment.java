package com.example.bangbillija.ui.reservations;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.bangbillija.core.SharedReservationViewModel;
import com.example.bangbillija.databinding.FragmentMyReservationsBinding;
import com.example.bangbillija.model.Reservation;
import com.example.bangbillija.model.ReservationStatus;
import com.example.bangbillija.service.AuthManager;
import com.example.bangbillija.ui.Navigator;
import com.example.bangbillija.ui.auth.LoginActivity;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseUser;

import java.util.Collections;
import java.util.List;

public class MyReservationsFragment extends Fragment implements MyReservationsAdapter.ReservationClickListener {

    private FragmentMyReservationsBinding binding;
    private SharedReservationViewModel viewModel;
    private MyReservationsAdapter adapter;
    private AuthManager authManager;
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
        authManager = AuthManager.getInstance();

        // 프로필 정보 설정
        setupProfileSection();

        adapter = new MyReservationsAdapter(this);
        binding.recyclerReservations.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerReservations.setAdapter(adapter);

        binding.chipGroupFilters.setOnCheckedStateChangeListener((group, ids) -> refreshList());

        // 로그아웃 버튼
        binding.buttonLogout.setOnClickListener(v -> showLogoutDialog());

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

    @Override
    public void onResume() {
        super.onResume();
        // 화면이 다시 보일 때 데이터 새로고침
        com.example.bangbillija.data.ReservationRepository.getInstance().refresh();
    }

    private void setupProfileSection() {
        FirebaseUser user = authManager.currentUser();
        if (user != null) {
            // 사용자 이름 설정
            String displayName = user.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                binding.textUserName.setText(displayName);
            } else {
                binding.textUserName.setText("사용자");
            }

            // 이메일 설정
            binding.textUserEmail.setText(user.getEmail());

            // 관리자 배지 표시
            if (authManager.isAdmin()) {
                binding.textAdminBadge.setVisibility(View.VISIBLE);
            } else {
                binding.textAdminBadge.setVisibility(View.GONE);
            }
        }
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("로그아웃")
                .setMessage("정말 로그아웃 하시겠습니까?")
                .setPositiveButton("로그아웃", (dialog, which) -> performLogout())
                .setNegativeButton("취소", null)
                .show();
    }

    private void performLogout() {
        authManager.signOut();

        // LoginActivity로 이동
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
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
