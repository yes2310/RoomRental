package com.example.bangbillija.ui.checkin;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.bangbillija.core.SharedReservationViewModel;
import com.example.bangbillija.databinding.FragmentQrCheckinBinding;
import com.example.bangbillija.model.Reservation;
import com.example.bangbillija.service.QrManager;
import com.google.android.material.snackbar.Snackbar;

public class QrCheckInFragment extends Fragment {

    private FragmentQrCheckinBinding binding;
    private SharedReservationViewModel viewModel;
    private QrManager qrManager;
    private ActivityResultLauncher<String> permissionLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentQrCheckinBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(SharedReservationViewModel.class);
        qrManager = new QrManager(requireContext());

        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) {
                startScan();
            } else {
                Snackbar.make(requireView(), "카메라 권한이 필요합니다", Snackbar.LENGTH_SHORT).show();
            }
        });

        binding.buttonStartScan.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                startScan();
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        binding.buttonManual.setOnClickListener(v -> Snackbar.make(v, "수동 입력 화면은 준비 중입니다", Snackbar.LENGTH_SHORT).show());

        viewModel.getFocusedReservation().observe(getViewLifecycleOwner(), this::updateReservationInfo);
    }

    private void startScan() {
        Reservation reservation = viewModel.getFocusedReservation().getValue();
        if (reservation == null) {
            Snackbar.make(requireView(), "체크인할 예약을 먼저 선택하세요", Snackbar.LENGTH_SHORT).show();
            return;
        }
        qrManager.simulateScan(result -> Snackbar.make(requireView(), result, Snackbar.LENGTH_SHORT).show());
    }

    private void updateReservationInfo(Reservation reservation) {
        if (reservation == null) {
            binding.textReservationInfo.setText("체크인할 예약이 없습니다");
            return;
        }
        String info = reservation.getTitle() + " • " + reservation.getDate()
                + " • " + reservation.getStartTime() + " - " + reservation.getEndTime();
        binding.textReservationInfo.setText(info);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
