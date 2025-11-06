package com.example.bangbillija.ui.rooms;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.bangbillija.data.RoomRepository;
import com.example.bangbillija.databinding.FragmentAddRoomBinding;
import com.example.bangbillija.model.Room;
import com.example.bangbillija.model.RoomStatus;
import com.example.bangbillija.service.AuthManager;
import com.example.bangbillija.service.FirestoreManager;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class AddRoomFragment extends Fragment {

    private FragmentAddRoomBinding binding;
    private RoomRepository roomRepository;
    private AuthManager authManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAddRoomBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        roomRepository = RoomRepository.getInstance();
        authManager = AuthManager.getInstance();

        // 관리자 체크
        if (!authManager.isAdmin()) {
            Snackbar.make(binding.getRoot(), "관리자 권한이 필요합니다", Snackbar.LENGTH_LONG).show();
            if (getActivity() != null) {
                requireActivity().onBackPressed();
            }
            return;
        }

        binding.buttonAddRoom.setOnClickListener(v -> addRoom());
        binding.buttonCancelAdd.setOnClickListener(v -> {
            if (getActivity() != null) {
                requireActivity().onBackPressed();
            }
        });
    }

    private void addRoom() {
        // Clear errors
        binding.inputRoomIdLayout.setError(null);
        binding.inputBuildingLayout.setError(null);
        binding.inputRoomNameLayout.setError(null);
        binding.inputCapacityLayout.setError(null);
        binding.inputFloorLayout.setError(null);

        // Get inputs
        String roomId = getTrimmed(binding.inputRoomId.getText());
        String building = getTrimmed(binding.inputBuilding.getText());
        String roomName = getTrimmed(binding.inputRoomName.getText());
        String capacityStr = getTrimmed(binding.inputCapacity.getText());
        String floor = getTrimmed(binding.inputFloor.getText());

        boolean valid = true;

        // Validate
        if (TextUtils.isEmpty(roomId)) {
            binding.inputRoomIdLayout.setError("강의실 ID를 입력하세요");
            valid = false;
        }

        if (TextUtils.isEmpty(building)) {
            binding.inputBuildingLayout.setError("건물명을 입력하세요");
            valid = false;
        }

        if (TextUtils.isEmpty(roomName)) {
            binding.inputRoomNameLayout.setError("강의실명을 입력하세요");
            valid = false;
        }

        if (TextUtils.isEmpty(capacityStr)) {
            binding.inputCapacityLayout.setError("수용 인원을 입력하세요");
            valid = false;
        }

        if (TextUtils.isEmpty(floor)) {
            binding.inputFloorLayout.setError("층수를 입력하세요");
            valid = false;
        }

        if (!valid) {
            return;
        }

        int capacity;
        try {
            capacity = Integer.parseInt(capacityStr);
            if (capacity <= 0) {
                binding.inputCapacityLayout.setError("수용 인원은 1명 이상이어야 합니다");
                return;
            }
        } catch (NumberFormatException e) {
            binding.inputCapacityLayout.setError("올바른 숫자를 입력하세요");
            return;
        }

        // Get facilities
        List<String> facilities = new ArrayList<>();
        if (binding.chipProjector.isChecked()) facilities.add("프로젝터");
        if (binding.chipWifi.isChecked()) facilities.add("와이파이");
        if (binding.chipWhiteboard.isChecked()) facilities.add("화이트보드");
        if (binding.chipComputer.isChecked()) facilities.add("컴퓨터");
        if (binding.chipSpeaker.isChecked()) facilities.add("스피커");

        // Get status
        RoomStatus status = binding.chipAvailableStatus.isChecked()
                ? RoomStatus.AVAILABLE
                : RoomStatus.MAINTENANCE;

        // Create room
        Room room = new Room(roomId, building, roomName, capacity, floor, facilities, status);

        // Show loading
        setLoading(true);

        // Add to repository
        roomRepository.addRoom(room, new FirestoreManager.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                setLoading(false);
                Snackbar.make(binding.getRoot(), "강의실이 등록되었습니다", Snackbar.LENGTH_SHORT).show();
                if (getActivity() != null) {
                    requireActivity().onBackPressed();
                }
            }

            @Override
            public void onFailure(Exception e) {
                setLoading(false);
                Snackbar.make(binding.getRoot(), "등록 실패: " + e.getMessage(),
                        Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.buttonAddRoom.setEnabled(!loading);
        binding.buttonCancelAdd.setEnabled(!loading);
        binding.inputRoomId.setEnabled(!loading);
        binding.inputBuilding.setEnabled(!loading);
        binding.inputRoomName.setEnabled(!loading);
        binding.inputCapacity.setEnabled(!loading);
        binding.inputFloor.setEnabled(!loading);
        binding.chipGroupFacilities.setEnabled(!loading);
        binding.chipGroupStatus.setEnabled(!loading);
    }

    private String getTrimmed(@Nullable CharSequence text) {
        return text == null ? "" : text.toString().trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
