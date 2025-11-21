package com.example.bangbillija.ui.rooms;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

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
import com.example.bangbillija.util.QRCodeUtil;
import com.example.bangbillija.util.SimpleTextWatcher;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.WriterException;

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
        if (getActivity() instanceof Navigator) {
            // 관리자는 수정 화면으로, 일반 사용자는 예약 화면으로 이동
            if (authManager.isAdmin()) {
                ((Navigator) getActivity()).openEditRoom(room);
            } else {
                viewModel.selectRoom(room);
                viewModel.focusReservation(null);
                ((Navigator) getActivity()).openCreateReservation();
            }
        }
    }

    @Override
    public void onRoomLongClicked(Room room) {
        // 관리자만 메뉴 표시
        if (!authManager.isAdmin()) {
            return;
        }

        showAdminMenuDialog(room);
    }

    private void showAdminMenuDialog(Room room) {
        String[] options = {"QR 코드 생성", "강의실 삭제"};

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(room.getName() + " 관리")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // QR 코드 생성
                        showRoomQRCodeDialog(room);
                    } else if (which == 1) {
                        // 강의실 삭제
                        showDeleteRoomConfirmDialog(room);
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void showDeleteRoomConfirmDialog(Room room) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("강의실 삭제")
                .setMessage("'" + room.getName() + "' 강의실을 삭제하시겠습니까?\n\n" +
                        "위치: " + room.getBuilding() + " " + room.getFloor() + "\n" +
                        "수용인원: " + room.getCapacity() + "명\n\n" +
                        "⚠️ 이 강의실과 관련된 모든 예약도 함께 삭제됩니다.\n" +
                        "이 작업은 되돌릴 수 없습니다.")
                .setPositiveButton("삭제", (dialog, which) -> {
                    com.example.bangbillija.data.RoomRepository.getInstance()
                            .deleteRoom(room.getId(), new com.example.bangbillija.service.FirestoreManager.FirestoreCallback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    if (binding != null) {
                                        Snackbar.make(binding.getRoot(),
                                                "강의실이 삭제되었습니다",
                                                Snackbar.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    if (binding != null) {
                                        Snackbar.make(binding.getRoot(),
                                                "삭제 실패: " + e.getMessage(),
                                                Snackbar.LENGTH_LONG).show();
                                    }
                                }
                            });
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void showRoomQRCodeDialog(Room room) {
        try {
            // QR 코드 생성
            String qrContent = QRCodeUtil.createRoomQRContent(room.getId(), room.getName());
            Bitmap qrBitmap = QRCodeUtil.generateQRCode(qrContent, 500, 500);

            // 다이얼로그에 표시할 ImageView 생성
            ImageView imageView = new ImageView(requireContext());
            imageView.setImageBitmap(qrBitmap);
            imageView.setPadding(32, 32, 32, 32);

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(room.getName() + " QR 코드")
                    .setMessage("사용자가 이 QR 코드를 스캔하여 체크인할 수 있습니다.\n\n" +
                            "QR 코드를 출력하거나 화면에 표시하여 공유하세요.")
                    .setView(imageView)
                    .setPositiveButton("확인", null)
                    .show();

            Snackbar.make(binding.getRoot(), "QR 코드가 생성되었습니다", Snackbar.LENGTH_SHORT).show();

        } catch (WriterException e) {
            android.util.Log.e("RoomListFragment", "QR 코드 생성 실패", e);
            Snackbar.make(binding.getRoot(), "QR 코드 생성 실패: " + e.getMessage(),
                    Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
