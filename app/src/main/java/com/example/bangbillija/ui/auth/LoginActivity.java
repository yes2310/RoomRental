package com.example.bangbillija.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bangbillija.R;
import com.example.bangbillija.databinding.ActivityLoginBinding;
import com.example.bangbillija.service.AuthManager;
import com.example.bangbillija.ui.MainActivity;
import com.google.android.material.snackbar.Snackbar;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AuthManager authManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authManager = AuthManager.getInstance();

        if (authManager.currentUser() != null) {
            navigateToMain();
            return;
        }

        binding.buttonLogin.setOnClickListener(v -> attemptLogin());
        binding.buttonRegister.setOnClickListener(v -> attemptRegister());
    }

    private void attemptLogin() {
        clearErrors();
        String email = getTrimmed(binding.inputEmail.getText());
        String password = getTrimmed(binding.inputPassword.getText());

        if (!validateCredentials(email, password)) {
            return;
        }

        setLoading(true);
        authManager.signIn(email, password, new AuthManager.Completion() {
            @Override
            public void onSuccess() {
                setLoading(false);
                Snackbar.make(binding.getRoot(), R.string.message_login_success, Snackbar.LENGTH_SHORT).show();
                navigateToMain();
            }

            @Override
            public void onFailure(Exception e) {
                setLoading(false);
                showError(e);
            }
        });
    }

    private void attemptRegister() {
        clearErrors();
        String email = getTrimmed(binding.inputEmail.getText());
        String password = getTrimmed(binding.inputPassword.getText());
        String displayName = getTrimmed(binding.inputDisplayName.getText());

        if (!validateCredentials(email, password)) {
            return;
        }

        setLoading(true);
        authManager.signUp(email, password, displayName, new AuthManager.Completion() {
            @Override
            public void onSuccess() {
                setLoading(false);
                Snackbar.make(binding.getRoot(), R.string.message_register_success, Snackbar.LENGTH_SHORT).show();
                navigateToMain();
            }

            @Override
            public void onFailure(Exception e) {
                setLoading(false);
                showError(e);
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.buttonLogin.setEnabled(!loading);
        binding.buttonRegister.setEnabled(!loading);
        binding.inputEmailLayout.setEnabled(!loading);
        binding.inputPasswordLayout.setEnabled(!loading);
        binding.inputNameLayout.setEnabled(!loading);
    }

    private void showError(Exception e) {
        String message = e != null && !TextUtils.isEmpty(e.getMessage())
                ? e.getMessage()
                : getString(R.string.error_generic);
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
    }

    private void clearErrors() {
        binding.inputEmailLayout.setError(null);
        binding.inputPasswordLayout.setError(null);
    }

    private boolean validateCredentials(String email, String password) {
        boolean valid = true;
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.inputEmailLayout.setError(getString(R.string.error_invalid_email));
            valid = false;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            binding.inputPasswordLayout.setError(getString(R.string.error_password_short));
            valid = false;
        }

        return valid;
    }

    private String getTrimmed(@Nullable CharSequence text) {
        return text == null ? "" : text.toString().trim();
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
