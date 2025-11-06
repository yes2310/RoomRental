package com.example.bangbillija.ui.rooms;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.bangbillija.core.SharedReservationViewModel;
import com.example.bangbillija.databinding.FragmentRoomListBinding;
import com.example.bangbillija.model.Room;
import com.example.bangbillija.model.RoomStatus;
import com.example.bangbillija.service.AuthManager;
import com.example.bangbillija.ui.Navigator;
import com.example.bangbillija.util.SimpleTextWatcher;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class RoomListFragment extends Fragment implements RoomListAdapter.RoomClickListener {

    private FragmentRoomListBinding binding;
    private SharedReservationViewModel viewModel;
    private RoomListAdapter adapter;
    private AuthManager authManager;
    private List<Room> currentRooms = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRoomListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(SharedReservationViewModel.class);
        authManager = AuthManager.getInstance();

        adapter = new RoomListAdapter(this);
        binding.recyclerRooms.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerRooms.setAdapter(adapter);

        binding.inputSearch.addTextChangedListener(new SimpleTextWatcher(text -> applyFilter()));
        binding.chipGroup.setOnCheckedStateChangeListener((group, checkedId) -> applyFilter());

        // 관리자인 경우 FAB 표시
        boolean isAdmin = authManager.isAdmin();
        android.util.Log.d("RoomListFragment", "=== Is Admin: " + isAdmin + " ===");

        if (isAdmin) {
            android.util.Log.d("RoomListFragment", "Setting up FAB for admin");

            binding.fabAddRoom.setVisibility(View.VISIBLE);
            binding.fabAddRoom.setClickable(true);
            binding.fabAddRoom.setEnabled(true);

            // 버튼이 제대로 설정되었는지 확인
            binding.fabAddRoom.post(() -> {
                android.util.Log.d("RoomListFragment", "FAB Visibility: " + binding.fabAddRoom.getVisibility());
                android.util.Log.d("RoomListFragment", "FAB Clickable: " + binding.fabAddRoom.isClickable());
                android.util.Log.d("RoomListFragment", "FAB Enabled: " + binding.fabAddRoom.isEnabled());
            });

            binding.fabAddRoom.setOnClickListener(v -> {
                android.util.Log.d("RoomListFragment", "========== FAB CLICKED! ==========");

                if (getActivity() instanceof Navigator) {
                    android.util.Log.d("RoomListFragment", "Opening add room screen");
                    ((Navigator) getActivity()).openAddRoom();
                } else {
                    android.util.Log.e("RoomListFragment", "Activity is not Navigator!");
                    Snackbar.make(binding.getRoot(), "Navigation 오류", Snackbar.LENGTH_LONG).show();
                }
            });

            android.util.Log.d("RoomListFragment", "FAB setup complete");
        } else {
            android.util.Log.d("RoomListFragment", "User is not admin, hiding FAB");
            binding.fabAddRoom.setVisibility(View.GONE);
        }

        viewModel.getRooms().observe(getViewLifecycleOwner(), rooms -> {
            currentRooms = rooms;
            adapter.submitList(new ArrayList<>(rooms));
        });
    }

    private void applyFilter() {
        if (currentRooms == null) {
            return;
        }
        String keyword = binding.inputSearch.getText() == null
                ? ""
                : binding.inputSearch.getText().toString().toLowerCase(Locale.getDefault());

        int checkedId = binding.chipGroup.getCheckedChipId();
        List<Room> filtered = currentRooms.stream()
                .filter(room -> matchRoom(room, keyword, checkedId))
                .collect(Collectors.toList());
        adapter.submitList(new ArrayList<>(filtered));
    }

    private boolean matchRoom(Room room, String keyword, int chipId) {
        boolean matchesKeyword = TextUtils.isEmpty(keyword)
                || room.getName().toLowerCase(Locale.getDefault()).contains(keyword)
                || room.getBuilding().toLowerCase(Locale.getDefault()).contains(keyword);

        if (!matchesKeyword) {
            return false;
        }

        if (chipId == binding.chipAvailable.getId()) {
            return room.getStatus() == RoomStatus.AVAILABLE;
        } else if (chipId == binding.chipReserved.getId()) {
            return room.getStatus() == RoomStatus.RESERVED;
        } else if (chipId == binding.chipLarge.getId()) {
            return room.getCapacity() >= 40;
        }

        return true;
    }

    @Override
    public void onRoomClicked(Room room) {
        viewModel.selectRoom(room);
        viewModel.focusReservation(null);
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
