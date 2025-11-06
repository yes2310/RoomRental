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
import com.example.bangbillija.data.ReservationRepository;
import com.example.bangbillija.databinding.FragmentQrCheckinBinding;
import com.example.bangbillija.model.Reservation;
import com.example.bangbillija.model.ReservationStatus;
import com.example.bangbillija.service.FirestoreManager;
import com.google.android.material.snackbar.Snackbar;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanIntentResult;
import com.journeyapps.barcodescanner.ScanOptions;

import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

public class QrCheckInFragment extends Fragment {

    private FragmentQrCheckinBinding binding;
    private SharedReservationViewModel viewModel;
    private ReservationRepository reservationRepository;
    private ActivityResultLauncher<String> permissionLauncher;
    private ActivityResultLauncher<ScanOptions> barcodeLauncher;

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
        reservationRepository = ReservationRepository.getInstance();

        // QR 스캐너 런처 등록
        barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
            if (result.getContents() != null) {
                processQRCode(result.getContents());
            }
        });

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
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt("QR 코드를 스캔하세요");
        options.setCameraId(0);
        options.setBeepEnabled(true);
        options.setBarcodeImageEnabled(false);
        options.setOrientationLocked(true);

        barcodeLauncher.launch(options);
    }

    private void processQRCode(String qrContent) {
        try {
            // QR 코드 내용 파싱 (JSON 형식)
            JSONObject json = new JSONObject(qrContent);
            String reservationId = json.getString("reservationId");
            String roomId = json.getString("roomId");
            String dateStr = json.getString("date");
            String startTimeStr = json.getString("startTime");

            // 현재 시간
            LocalTime now = LocalTime.now();
            LocalDate today = LocalDate.now();

            // 예약 시간 파싱
            LocalDate reservationDate = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            LocalTime reservationStartTime = LocalTime.parse(startTimeStr, DateTimeFormatter.ofPattern("HH:mm"));

            // 날짜 확인
            if (!today.equals(reservationDate)) {
                Snackbar.make(binding.getRoot(), "오늘 예약이 아닙니다", Snackbar.LENGTH_LONG).show();
                return;
            }

            // 체크인 가능 시간 확인 (예약 시간 10분 이내)
            long minutesDiff = java.time.Duration.between(reservationStartTime, now).toMinutes();

            if (minutesDiff < -10) {
                Snackbar.make(binding.getRoot(),
                        "체크인은 예약 시간 10분 전부터 가능합니다\n(예약 시간: " + startTimeStr + ")",
                        Snackbar.LENGTH_LONG).show();
                return;
            }

            if (minutesDiff > 10) {
                // 10분 초과 - 예약 자동 취소
                cancelReservationAutomatically(reservationId);
                return;
            }

            // 체크인 처리
            checkInReservation(reservationId);

        } catch (Exception e) {
            android.util.Log.e("QrCheckIn", "QR 코드 처리 실패", e);
            Snackbar.make(binding.getRoot(), "유효하지 않은 QR 코드입니다", Snackbar.LENGTH_LONG).show();
        }
    }

    private void checkInReservation(String reservationId) {
        HashMap<String, Object> updates = new HashMap<>();
        updates.put("status", ReservationStatus.CHECKED_IN.name());

        reservationRepository.updateReservationByReservationId(reservationId, updates,
                new FirestoreManager.FirestoreCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Snackbar.make(binding.getRoot(), "체크인이 완료되었습니다!", Snackbar.LENGTH_LONG).show();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Snackbar.make(binding.getRoot(), "체크인 실패: " + e.getMessage(),
                                Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private void cancelReservationAutomatically(String reservationId) {
        reservationRepository.cancelReservationByReservationId(reservationId,
                new FirestoreManager.FirestoreCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Snackbar.make(binding.getRoot(),
                                "예약 시간으로부터 10분이 지나 자동으로 취소되었습니다",
                                Snackbar.LENGTH_LONG).show();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Snackbar.make(binding.getRoot(), "예약 취소 실패: " + e.getMessage(),
                                Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private void updateReservationInfo(Reservation reservation) {
        if (reservation == null) {
            binding.textReservationInfo.setText("QR 코드를 스캔하여 체크인하세요");
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
