package com.example.bangbillija.ui.reservations;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.bangbillija.R;
import com.example.bangbillija.core.SharedReservationViewModel;
import com.example.bangbillija.data.ReservationRepository;
import com.example.bangbillija.databinding.FragmentReservationDetailBinding;
import com.example.bangbillija.databinding.ItemDetailRowBinding;
import com.example.bangbillija.model.Reservation;
import com.example.bangbillija.model.ReservationStatus;
import com.example.bangbillija.model.Room;
import com.example.bangbillija.service.FirestoreManager;
import com.example.bangbillija.ui.Navigator;
import com.example.bangbillija.util.QRCodeUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.WriterException;

import androidx.core.content.ContextCompat;

import java.time.Duration;
import java.time.format.DateTimeFormatter;

public class ReservationDetailFragment extends Fragment {

    private FragmentReservationDetailBinding binding;
    private SharedReservationViewModel viewModel;
    private ReservationRepository reservationRepository;
    private Room currentRoom;
    private Reservation currentReservation;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentReservationDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(SharedReservationViewModel.class);
        reservationRepository = ReservationRepository.getInstance();

        viewModel.getSelectedRoom().observe(getViewLifecycleOwner(), room -> {
            currentRoom = room;
            render();
        });

        viewModel.getFocusedReservation().observe(getViewLifecycleOwner(), reservation -> {
            currentReservation = reservation;
            render();
        });

        binding.buttonCheckIn.setOnClickListener(v -> {
            if (currentReservation == null) {
                Snackbar.make(v, "예약을 먼저 선택해 주세요", Snackbar.LENGTH_SHORT).show();
                return;
            }
            if (getActivity() instanceof Navigator) {
                ((Navigator) getActivity()).openQrScanner();
            }
        });

        binding.buttonEdit.setOnClickListener(v -> showEditDialog());
        binding.buttonCancel.setOnClickListener(v -> showCancelDialog());
    }

    private void render() {
        if (currentRoom == null) {
            binding.cardDetail.setVisibility(View.GONE);
            binding.textEmpty.setVisibility(View.VISIBLE);
            return;
        }

        binding.cardDetail.setVisibility(View.VISIBLE);

        if (currentReservation == null) {
            binding.textReservationTitle.setText(currentRoom.getName());
            binding.textReservationSubtitle.setText("예약을 선택하여 상세 정보를 확인하세요");
            updateStatusBadge(ReservationStatus.AVAILABLE);
            binding.containerRows.removeAllViews();
            addRow("강의실", currentRoom.getName());
            addRow("위치", currentRoom.getBuilding() + " • " + currentRoom.getFloor());
            addRow("수용 인원", currentRoom.getCapacity() + "명");
            binding.textEmpty.setVisibility(View.VISIBLE);
            return;
        }

        binding.textEmpty.setVisibility(View.GONE);
        binding.textReservationTitle.setText(currentReservation.getTitle());
        binding.textReservationSubtitle.setText("예약 ID: " + currentReservation.getId());
        updateStatusBadge(currentReservation.getStatus());

        binding.containerRows.removeAllViews();
        addRow("강의실", currentRoom.getName());
        addRow("날짜", currentReservation.getDate().format(dateFormatter));

        Duration duration = Duration.between(currentReservation.getStartTime(), currentReservation.getEndTime());
        long minutes = duration.toMinutes();
        addRow("시간", currentReservation.getStartTime().format(timeFormatter) + " - "
                + currentReservation.getEndTime().format(timeFormatter) + " ("
                + (minutes / 60) + "시간 " + (minutes % 60) + "분)");

        addRow("참석 인원", currentReservation.getAttendees() + "명");
        addRow("예약자", currentReservation.getOwner());
        addRow("목적", currentReservation.getNote());

        // QR 코드 생성 및 표시 (PENDING 또는 RESERVED 상태일 때만)
        if (currentReservation.getStatus() == ReservationStatus.PENDING ||
                currentReservation.getStatus() == ReservationStatus.RESERVED) {
            generateAndDisplayQRCode();
        } else {
            binding.cardQrCode.setVisibility(View.GONE);
        }
    }

    private void generateAndDisplayQRCode() {
        try {
            String qrContent = QRCodeUtil.createReservationQRContent(
                    currentReservation.getId(),
                    currentRoom.getId(),
                    currentReservation.getDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                    currentReservation.getStartTime().format(timeFormatter)
            );

            Bitmap qrBitmap = QRCodeUtil.generateQRCode(qrContent, 500, 500);
            binding.imageQrCode.setImageBitmap(qrBitmap);
            binding.cardQrCode.setVisibility(View.VISIBLE);
        } catch (WriterException e) {
            android.util.Log.e("ReservationDetail", "QR 코드 생성 실패", e);
            binding.cardQrCode.setVisibility(View.GONE);
        }
    }

    private void addRow(String label, String value) {
        ItemDetailRowBinding rowBinding = ItemDetailRowBinding.inflate(getLayoutInflater(), binding.containerRows, false);
        rowBinding.textLabel.setText(label);
        rowBinding.textValue.setText(value);
        binding.containerRows.addView(rowBinding.getRoot());
    }

    private void updateStatusBadge(ReservationStatus status) {
        if (status == ReservationStatus.AVAILABLE) {
            binding.textReservationStatus.setText("예약 가능");
            binding.textReservationStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_available));
            binding.textReservationStatus.setBackgroundResource(R.drawable.bg_status_available);
        } else if (status == ReservationStatus.RESERVED) {
            binding.textReservationStatus.setText("예약 확정");
            binding.textReservationStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_reserved));
            binding.textReservationStatus.setBackgroundResource(R.drawable.bg_status_reserved);
        } else if (status == ReservationStatus.PENDING) {
            binding.textReservationStatus.setText("승인 대기중");
            binding.textReservationStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_pending));
            binding.textReservationStatus.setBackgroundResource(R.drawable.bg_status_pending);
        } else if (status == ReservationStatus.CHECKED_IN) {
            binding.textReservationStatus.setText("체크인 완료");
            binding.textReservationStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_checked_in));
            binding.textReservationStatus.setBackgroundResource(R.drawable.bg_status_available);
        } else {
            binding.textReservationStatus.setText("취소됨");
            binding.textReservationStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_reserved));
            binding.textReservationStatus.setBackgroundResource(R.drawable.bg_status_reserved);
        }
    }

    private void showEditDialog() {
        if (currentReservation == null) {
            Snackbar.make(binding.getRoot(), "예약을 선택하세요", Snackbar.LENGTH_SHORT).show();
            return;
        }

        // 간단한 수정: 참석 인원과 메모만 수정 가능
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_reservation, null);
        com.google.android.material.textfield.TextInputEditText inputAttendees =
                dialogView.findViewById(R.id.inputAttendeesEdit);
        com.google.android.material.textfield.TextInputEditText inputNote =
                dialogView.findViewById(R.id.inputNoteEdit);

        inputAttendees.setText(String.valueOf(currentReservation.getAttendees()));
        inputNote.setText(currentReservation.getNote());

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("예약 수정")
                .setView(dialogView)
                .setPositiveButton("수정", (dialog, which) -> {
                    String attendeesStr = inputAttendees.getText() != null ?
                            inputAttendees.getText().toString().trim() : "";
                    String note = inputNote.getText() != null ?
                            inputNote.getText().toString().trim() : "";

                    if (attendeesStr.isEmpty()) {
                        Snackbar.make(binding.getRoot(), "참석 인원을 입력하세요", Snackbar.LENGTH_SHORT).show();
                        return;
                    }

                    int attendees;
                    try {
                        attendees = Integer.parseInt(attendeesStr);
                        if (attendees <= 0) {
                            Snackbar.make(binding.getRoot(), "참석 인원은 1명 이상이어야 합니다", Snackbar.LENGTH_SHORT).show();
                            return;
                        }
                    } catch (NumberFormatException e) {
                        Snackbar.make(binding.getRoot(), "올바른 숫자를 입력하세요", Snackbar.LENGTH_SHORT).show();
                        return;
                    }

                    java.util.Map<String, Object> updates = new java.util.HashMap<>();
                    updates.put("attendees", attendees);
                    updates.put("note", note);

                    reservationRepository.updateReservationByReservationId(
                            currentReservation.getId(),
                            updates,
                            new FirestoreManager.FirestoreCallback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    Snackbar.make(binding.getRoot(), "예약이 수정되었습니다", Snackbar.LENGTH_SHORT).show();
                                    if (getActivity() != null) {
                                        requireActivity().onBackPressed();
                                    }
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    Snackbar.make(binding.getRoot(), "수정 실패: " + e.getMessage(),
                                            Snackbar.LENGTH_LONG).show();
                                }
                            }
                    );
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void showCancelDialog() {
        if (currentReservation == null) {
            Snackbar.make(binding.getRoot(), "예약을 선택하세요", Snackbar.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("예약 취소")
                .setMessage("정말 이 예약을 취소하시겠습니까?\n취소된 예약은 복구할 수 없습니다.")
                .setPositiveButton("취소하기", (dialog, which) -> {
                    reservationRepository.cancelReservationByReservationId(
                            currentReservation.getId(),
                            new FirestoreManager.FirestoreCallback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    Snackbar.make(binding.getRoot(), "예약이 취소되었습니다", Snackbar.LENGTH_SHORT).show();
                                    if (getActivity() != null) {
                                        requireActivity().onBackPressed();
                                    }
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    Snackbar.make(binding.getRoot(), "취소 실패: " + e.getMessage(),
                                            Snackbar.LENGTH_LONG).show();
                                }
                            }
                    );
                })
                .setNegativeButton("돌아가기", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
