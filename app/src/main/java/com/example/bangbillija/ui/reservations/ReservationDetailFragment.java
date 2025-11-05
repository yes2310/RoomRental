package com.example.bangbillija.ui.reservations;

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
import com.example.bangbillija.databinding.FragmentReservationDetailBinding;
import com.example.bangbillija.databinding.ItemDetailRowBinding;
import com.example.bangbillija.model.Reservation;
import com.example.bangbillija.model.ReservationStatus;
import com.example.bangbillija.model.Room;
import com.example.bangbillija.ui.Navigator;
import com.google.android.material.snackbar.Snackbar;

import androidx.core.content.ContextCompat;

import java.time.Duration;
import java.time.format.DateTimeFormatter;

public class ReservationDetailFragment extends Fragment {

    private FragmentReservationDetailBinding binding;
    private SharedReservationViewModel viewModel;
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

        binding.buttonEdit.setOnClickListener(v -> Snackbar.make(v, "수정 기능은 곧 제공될 예정입니다", Snackbar.LENGTH_SHORT).show());
        binding.buttonCancel.setOnClickListener(v -> Snackbar.make(v, "취소 요청이 전송되었습니다", Snackbar.LENGTH_SHORT).show());
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
