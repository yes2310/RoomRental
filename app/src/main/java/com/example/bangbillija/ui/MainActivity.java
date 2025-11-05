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
import com.example.bangbillija.ui.reservations.MyReservationsFragment;
import com.example.bangbillija.ui.reservations.ProfileFragment;
import com.example.bangbillija.ui.reservations.ReservationDetailFragment;
import com.example.bangbillija.ui.rooms.RoomListFragment;
import com.example.bangbillija.ui.auth.LoginActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity implements Navigator {

    private MaterialToolbar topAppBar;
    private BottomNavigationView bottomNavigationView;
    private SharedReservationViewModel viewModel;
    private AuthManager authManager;

    private final Fragment roomsFragment = new RoomListFragment();
    private final Fragment calendarFragment = new CalendarFragment();
    private final Fragment qrFragment = new QrCheckInFragment();
    private final Fragment myReservationsFragment = new MyReservationsFragment();
    private final Fragment detailFragment = new ReservationDetailFragment();
    private final Fragment profileFragment = new ProfileFragment();

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

        topAppBar = findViewById(R.id.topAppBar);
        bottomNavigationView = findViewById(R.id.bottomNav);

        setSupportActionBar(topAppBar);

        viewModel = new ViewModelProvider(this).get(SharedReservationViewModel.class);
        viewModel.getToolbarTitle().observe(this, title -> {
            if (title != null) {
                topAppBar.setTitle(title);
            }
        });

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_rooms) {
                switchTo(roomsFragment, getString(R.string.title_rooms), false);
                return true;
            } else if (itemId == R.id.menu_calendar) {
                switchTo(calendarFragment, getString(R.string.title_calendar), false);
                return true;
            } else if (itemId == R.id.menu_qr) {
                switchTo(qrFragment, getString(R.string.title_qr), false);
                return true;
            } else if (itemId == R.id.menu_my) {
                switchTo(myReservationsFragment, getString(R.string.title_my_reservations), false);
                return true;
            } else if (itemId == R.id.menu_profile) {
                switchTo(profileFragment, getString(R.string.menu_profile), false);
                return true;
            }
            return false;
        });

        getSupportFragmentManager().addOnBackStackChangedListener(this::syncToolbarWithBackStack);

        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.menu_rooms);
        }
    }

    private void switchTo(@NonNull Fragment fragment, @NonNull String title, boolean addToBackStack) {
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

    private void syncToolbarWithBackStack() {
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragmentContainer);
        if (fragment == null) return;

        if (fragment instanceof ReservationDetailFragment) {
            viewModel.updateToolbarTitle(getString(R.string.title_reservation_detail));
        } else if (fragment instanceof CalendarFragment) {
            viewModel.updateToolbarTitle(getString(R.string.title_calendar));
            bottomNavigationView.setSelectedItemId(R.id.menu_calendar);
        } else if (fragment instanceof QrCheckInFragment) {
            viewModel.updateToolbarTitle(getString(R.string.title_qr));
            bottomNavigationView.setSelectedItemId(R.id.menu_qr);
        } else if (fragment instanceof MyReservationsFragment) {
            viewModel.updateToolbarTitle(getString(R.string.title_my_reservations));
            bottomNavigationView.setSelectedItemId(R.id.menu_my);
        } else if (fragment instanceof ProfileFragment) {
            viewModel.updateToolbarTitle(getString(R.string.menu_profile));
            bottomNavigationView.setSelectedItemId(R.id.menu_profile);
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
}
