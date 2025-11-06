package com.example.bangbillija.ui.reservations;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.bangbillija.databinding.FragmentProfileBinding;
import com.example.bangbillija.service.AuthManager;
import com.example.bangbillija.ui.auth.LoginActivity;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseUser;

import java.text.DateFormat;
import java.util.Date;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private final AuthManager authManager = AuthManager.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.buttonLogout.setOnClickListener(v -> logout());
        bindUser();
    }

    private void bindUser() {
        FirebaseUser user = authManager.currentUser();
        if (user == null) {
            binding.textDisplayName.setText("-");
            binding.textEmail.setText("-");
            binding.textLastLogin.setText("");
            binding.chipAdminBadge.setVisibility(View.GONE);
            return;
        }

        String name = user.getDisplayName();
        binding.textDisplayName.setText(name == null || name.trim().isEmpty() ? "사용자" : name);
        binding.textEmail.setText(user.getEmail());

        // 관리자 배지 표시
        if (authManager.isAdmin()) {
            binding.chipAdminBadge.setVisibility(View.VISIBLE);
        } else {
            binding.chipAdminBadge.setVisibility(View.GONE);
        }

        if (user.getMetadata() != null && user.getMetadata().getLastSignInTimestamp() > 0) {
            String formatted = DateFormat.getDateTimeInstance().format(
                    new Date(user.getMetadata().getLastSignInTimestamp()));
            binding.textLastLogin.setText(getString(com.example.bangbillija.R.string.profile_last_login) + ": " + formatted);
        } else {
            binding.textLastLogin.setText("");
        }
    }

    private void logout() {
        authManager.signOut();
        if (getActivity() != null) {
            Snackbar.make(binding.getRoot(), com.example.bangbillija.R.string.profile_logout, Snackbar.LENGTH_SHORT).show();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            requireActivity().finish();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
