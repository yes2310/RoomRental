package com.example.bangbillija.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.example.bangbillija.R;
import com.example.bangbillija.core.SharedReservationViewModel;
import com.example.bangbillija.service.AuthManager;
import com.example.bangbillija.ui.calendar.CalendarFragment;
import com.example.bangbillija.ui.checkin.QrCheckInFragment;
import com.example.bangbillija.ui.reservations.CreateReservationFragment;
import com.example.bangbillija.ui.reservations.MyReservationsFragment;
import com.example.bangbillija.ui.reservations.ProfileFragment;
import com.example.bangbillija.ui.reservations.ReservationDetailFragment;
import com.example.bangbillija.ui.rooms.AddRoomFragment;
import com.example.bangbillija.ui.rooms.RoomListFragment;
import com.example.bangbillija.ui.auth.LoginActivity;
import com.example.bangbillija.ui.timetable.TimetableFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements Navigator {

    private TextView toolbarTitle;
    private BottomNavigationView bottomNavigationView;
    private SharedReservationViewModel viewModel;
    private AuthManager authManager;

    private final Fragment roomsFragment = new RoomListFragment();
    private final Fragment calendarFragment = new CalendarFragment();
    private final Fragment timetableFragment = new TimetableFragment();
    private final Fragment qrFragment = new QrCheckInFragment();
    private final Fragment myReservationsFragment = new MyReservationsFragment();
    private final Fragment detailFragment = new ReservationDetailFragment();
    private final Fragment profileFragment = new ProfileFragment();
    private final Fragment createReservationFragment = new CreateReservationFragment();
    private final Fragment addRoomFragment = new AddRoomFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        authManager = AuthManager.getInstance();
        if (authManager.currentUser() == null) {
            navigateToLogin();
            return;
        }

        toolbarTitle = findViewById(R.id.toolbarTitle);
        bottomNavigationView = findViewById(R.id.bottomNav);

        // 관리자가 아니면 시간표 탭 숨기기
        if (!authManager.isAdmin()) {
            bottomNavigationView.getMenu().findItem(R.id.menu_timetable).setVisible(false);
        }

        viewModel = new ViewModelProvider(this).get(SharedReservationViewModel.class);
        viewModel.getToolbarTitle().observe(this, title -> {
            if (title != null) {
                toolbarTitle.setText(title);
            }
        });

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_rooms) {
                switchTo(roomsFragment, getString(R.string.title_rooms), false);
                return true;
            } else if (itemId == R.id.menu_calendar) {
                switchTo(calendarFragment, getString(R.string.menu_calendar), false);
                return true;
            } else if (itemId == R.id.menu_timetable) {
                switchTo(timetableFragment, getString(R.string.title_timetable), false);
                return true;
            } else if (itemId == R.id.menu_qr) {
                switchTo(qrFragment, getString(R.string.title_qr), false);
                return true;
            } else if (itemId == R.id.menu_my) {
                switchTo(myReservationsFragment, getString(R.string.title_my_reservations), false);
                return true;
            }
            return false;
        });

        getSupportFragmentManager().addOnBackStackChangedListener(this::syncToolbarWithBackStack);

        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.menu_rooms);

            // 회원가입 후 환영 메시지 표시
            checkAndShowWelcomeMessage();
        }
    }

    private void switchTo(@NonNull Fragment fragment, @NonNull String title, boolean addToBackStack) {
        android.util.Log.d("MainActivity", "switchTo called: " + fragment.getClass().getSimpleName() + ", title: " + title + ", addToBackStack: " + addToBackStack);

        try {
            if (addToBackStack) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.fragment_fade_in, R.anim.fragment_fade_out,
                                R.anim.fragment_fade_in, R.anim.fragment_fade_out)
                        .replace(R.id.fragmentContainer, fragment)
                        .addToBackStack(fragment.getClass().getSimpleName())
                        .commit();
            } else {
                getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.fragment_fade_in, R.anim.fragment_fade_out)
                        .replace(R.id.fragmentContainer, fragment)
                        .commit();
            }
            viewModel.updateToolbarTitle(title);
            android.util.Log.d("MainActivity", "Fragment switch successful");
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Fragment switch failed", e);
            android.widget.Toast.makeText(this, "화면 전환 실패: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void openReservationDetail() {
        switchTo(detailFragment, getString(R.string.title_reservation_detail), true);
    }

    @Override
    public void navigateBackToRooms() {
        getSupportFragmentManager().popBackStack();
        bottomNavigationView.setSelectedItemId(R.id.menu_rooms);
    }

    @Override
    public void openQrScanner() {
        bottomNavigationView.setSelectedItemId(R.id.menu_qr);
    }

    @Override
    public void openCreateReservation() {
        // Fragment를 새로 생성하여 사용
        Fragment newCreateReservationFragment = new CreateReservationFragment();
        switchTo(newCreateReservationFragment, getString(R.string.title_create_reservation), true);
    }

    @Override
    public void openAddRoom() {
        android.util.Log.d("MainActivity", "========== openAddRoom called ==========");

        // Fragment를 새로 생성하여 사용
        Fragment newAddRoomFragment = new AddRoomFragment();
        android.util.Log.d("MainActivity", "AddRoomFragment created: " + newAddRoomFragment);

        // 애니메이션 없이 직접 전환 시도
        try {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, newAddRoomFragment)
                    .addToBackStack("AddRoomFragment")
                    .commitAllowingStateLoss();

            viewModel.updateToolbarTitle("강의실 등록");
            android.util.Log.d("MainActivity", "Fragment replacement committed successfully");
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Fragment replacement failed", e);
            android.widget.Toast.makeText(this, "전환 실패: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void openEditRoom(com.example.bangbillija.model.Room room) {
        android.util.Log.d("MainActivity", "========== openEditRoom called ==========");

        // Bundle로 Room 정보 전달
        Fragment editRoomFragment = new AddRoomFragment();
        Bundle args = new Bundle();
        args.putString("room_id", room.getId());
        args.putString("room_name", room.getName());
        args.putString("building", room.getBuilding());
        args.putInt("capacity", room.getCapacity());
        args.putString("floor", room.getFloor());
        args.putStringArrayList("facilities", new java.util.ArrayList<>(room.getFacilities()));
        args.putString("status", room.getStatus().name());
        args.putBoolean("edit_mode", true);
        editRoomFragment.setArguments(args);

        try {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, editRoomFragment)
                    .addToBackStack("EditRoomFragment")
                    .commitAllowingStateLoss();

            viewModel.updateToolbarTitle("강의실 수정");
            android.util.Log.d("MainActivity", "Edit room fragment committed successfully");
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Edit room fragment failed", e);
            android.widget.Toast.makeText(this, "전환 실패: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
        }
    }

    private void syncToolbarWithBackStack() {
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragmentContainer);
        if (fragment == null) return;

        android.util.Log.d("MainActivity", "syncToolbarWithBackStack: " + fragment.getClass().getSimpleName());

        if (fragment instanceof ReservationDetailFragment) {
            viewModel.updateToolbarTitle(getString(R.string.title_reservation_detail));
        } else if (fragment instanceof AddRoomFragment) {
            // AddRoomFragment - 이미 타이틀이 설정되어 있고, bottom nav는 변경하지 않음
            android.util.Log.d("MainActivity", "AddRoomFragment detected, skipping bottom nav update");
        } else if (fragment instanceof CreateReservationFragment) {
            // CreateReservationFragment - 이미 타이틀이 설정되어 있고, bottom nav는 변경하지 않음
            android.util.Log.d("MainActivity", "CreateReservationFragment detected, skipping bottom nav update");
        } else if (fragment instanceof TimetableFragment) {
            viewModel.updateToolbarTitle(getString(R.string.title_timetable));
            bottomNavigationView.setSelectedItemId(R.id.menu_timetable);
        } else if (fragment instanceof QrCheckInFragment) {
            viewModel.updateToolbarTitle(getString(R.string.title_qr));
            bottomNavigationView.setSelectedItemId(R.id.menu_qr);
        } else if (fragment instanceof MyReservationsFragment) {
            viewModel.updateToolbarTitle(getString(R.string.title_my_reservations));
            bottomNavigationView.setSelectedItemId(R.id.menu_my);
        } else if (fragment instanceof CalendarFragment) {
            viewModel.updateToolbarTitle(getString(R.string.menu_calendar));
            bottomNavigationView.setSelectedItemId(R.id.menu_calendar);
        } else {
            viewModel.updateToolbarTitle(getString(R.string.title_rooms));
            bottomNavigationView.setSelectedItemId(R.id.menu_rooms);
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void checkAndShowWelcomeMessage() {
        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra("new_user", false)) {
            String userName = intent.getStringExtra("user_name");
            if (userName != null && !userName.isEmpty()) {
                // UI가 완전히 로드된 후 환영 메시지 표시
                findViewById(android.R.id.content).post(() -> {
                    Snackbar.make(findViewById(R.id.fragmentContainer),
                            userName + "님, 방빌려에 오신 것을 환영합니다!",
                            Snackbar.LENGTH_LONG).show();
                });
            }
        }
    }
}
