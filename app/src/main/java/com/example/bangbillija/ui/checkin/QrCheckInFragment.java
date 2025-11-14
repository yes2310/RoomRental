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
import com.example.bangbillija.service.AuthManager;
import com.example.bangbillija.service.FirestoreManager;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseUser;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanIntentResult;
import com.journeyapps.barcodescanner.ScanOptions;

import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;

public class QrCheckInFragment extends Fragment {

    private FragmentQrCheckinBinding binding;
    private SharedReservationViewModel viewModel;
    private ReservationRepository reservationRepository;
    private AuthManager authManager;
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
        authManager = AuthManager.getInstance();

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
            // Get current user
            FirebaseUser user = authManager.currentUser();
            if (user == null) {
                Snackbar.make(binding.getRoot(), "로그인이 필요합니다", Snackbar.LENGTH_SHORT).show();
                return;
            }

            // QR 코드 내용 파싱 (간단한 JSON 형식 - roomId만 포함)
            JSONObject json = new JSONObject(qrContent);
            String roomId = json.getString("roomId");
            String roomName = json.optString("roomName", "강의실");

            android.util.Log.d("QrCheckIn", "Scanned room: " + roomId + " (" + roomName + ")");

            // 현재 시간과 날짜
            LocalTime now = LocalTime.now();
            LocalDate today = LocalDate.now();

            // 사용자의 예약 목록 조회
            FirestoreManager firestoreManager = FirestoreManager.getInstance();
            firestoreManager.getReservationsByUser(user.getUid(), new FirestoreManager.FirestoreCallback<List<Reservation>>() {
                @Override
                public void onSuccess(List<Reservation> reservations) {
                    // 오늘 날짜, 해당 강의실의 예약 찾기
                    Reservation matchingReservation = null;

                    for (Reservation reservation : reservations) {
                        // 1. 오늘, 해당 강의실인지 확인
                        if (!reservation.getDate().equals(today) || !reservation.getRoomId().equals(roomId)) {
                            continue;
                        }

                        // 2. PENDING 상태 체크 (승인 대기 중)
                        if (reservation.getStatus() == ReservationStatus.PENDING) {
                            Snackbar.make(binding.getRoot(),
                                    "이 예약은 아직 관리자 승인 대기 중입니다\n승인 후에 체크인할 수 있습니다",
                                    Snackbar.LENGTH_LONG).show();
                            return;
                        }

                        // 3. RESERVED 상태만 체크인 가능
                        if (reservation.getStatus() != ReservationStatus.RESERVED) {
                            continue;
                        }

                        // 4. 체크인 가능 시간 확인: 예약 시작 10분 전부터 예약 종료 시간까지
                        LocalTime checkInStart = reservation.getStartTime().minusMinutes(10);
                        LocalTime checkInEnd = reservation.getEndTime();

                        if (now.isBefore(checkInStart)) {
                            long minutesLeft = java.time.Duration.between(now, reservation.getStartTime()).toMinutes();
                            Snackbar.make(binding.getRoot(),
                                    "체크인은 예약 시간 10분 전부터 가능합니다\n" +
                                    "(예약 시간: " + reservation.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")) +
                                    ", " + minutesLeft + "분 후 체크인 가능)",
                                    Snackbar.LENGTH_LONG).show();
                            return;
                        }

                        if (now.isAfter(checkInEnd)) {
                            Snackbar.make(binding.getRoot(),
                                    "예약 시간이 이미 종료되었습니다\n(종료 시간: " +
                                    checkInEnd.format(DateTimeFormatter.ofPattern("HH:mm")) + ")",
                                    Snackbar.LENGTH_LONG).show();
                            return;
                        }

                        // 체크인 가능한 예약 발견
                        matchingReservation = reservation;
                        break;
                    }

                    if (matchingReservation == null) {
                        Snackbar.make(binding.getRoot(),
                                roomName + "에 대한 활성 예약이 없습니다\n(현재 시간: " + now.format(DateTimeFormatter.ofPattern("HH:mm")) + ")",
                                Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    // 체크인 처리
                    checkInReservation(matchingReservation.getId(), matchingReservation.getTitle(), roomName);
                }

                @Override
                public void onFailure(Exception e) {
                    android.util.Log.e("QrCheckIn", "예약 조회 실패", e);
                    Snackbar.make(binding.getRoot(), "예약 정보를 가져올 수 없습니다: " + e.getMessage(),
                            Snackbar.LENGTH_LONG).show();
                }
            });

        } catch (Exception e) {
            android.util.Log.e("QrCheckIn", "QR 코드 처리 실패", e);
            Snackbar.make(binding.getRoot(), "유효하지 않은 QR 코드입니다\n형식: {\"roomId\":\"ROOM_ID\"}", Snackbar.LENGTH_LONG).show();
        }
    }

    private void checkInReservation(String reservationId, String reservationTitle, String roomName) {
        HashMap<String, Object> updates = new HashMap<>();
        updates.put("status", ReservationStatus.CHECKED_IN.name());

        reservationRepository.updateReservationByReservationId(reservationId, updates,
                new FirestoreManager.FirestoreCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Snackbar.make(binding.getRoot(),
                                "체크인 완료!\n" + roomName + " - " + reservationTitle,
                                Snackbar.LENGTH_LONG).show();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Snackbar.make(binding.getRoot(), "체크인 실패: " + e.getMessage(),
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
