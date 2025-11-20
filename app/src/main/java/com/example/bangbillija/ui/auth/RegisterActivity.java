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
import com.example.bangbillija.databinding.ActivityRegisterBinding;
import com.example.bangbillija.service.AuthManager;
import com.example.bangbillija.ui.MainActivity;
import com.google.android.material.snackbar.Snackbar;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private AuthManager authManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authManager = AuthManager.getInstance();

        // 이미 로그인되어 있으면 메인으로 이동
        if (authManager.currentUser() != null) {
            navigateToMain();
            return;
        }

        binding.buttonRegister.setOnClickListener(v -> attemptRegister());
        binding.buttonGoToLogin.setOnClickListener(v -> navigateToLogin());
    }

    private void attemptRegister() {
        clearErrors();

        String email = getTrimmed(binding.inputEmail.getText());
        String password = getTrimmed(binding.inputPassword.getText());
        String passwordConfirm = getTrimmed(binding.inputPasswordConfirm.getText());
        String studentId = getTrimmed(binding.inputStudentId.getText());
        String name = getTrimmed(binding.inputName.getText());

        if (!validateInputs(email, password, passwordConfirm, studentId, name)) {
            return;
        }

        setLoading(true);
        authManager.signUp(email, password, name, studentId, new AuthManager.Completion() {
            @Override
            public void onSuccess() {
                setLoading(false);
                // 회원가입 성공 후 MainActivity에 환영 메시지 표시를 위한 플래그와 함께 이동
                navigateToMainWithWelcome(name);
            }

            @Override
            public void onFailure(Exception e) {
                setLoading(false);
                showError(e);
            }
        });
    }

    private boolean validateInputs(String email, String password, String passwordConfirm,
                                    String studentId, String name) {
        boolean valid = true;

        // 이메일 검증
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.inputEmailLayout.setError(getString(R.string.error_invalid_email));
            valid = false;
        }

        // 비밀번호 검증
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            binding.inputPasswordLayout.setError(getString(R.string.error_password_short));
            valid = false;
        }

        // 비밀번호 확인 검증
        if (TextUtils.isEmpty(passwordConfirm)) {
            binding.inputPasswordConfirmLayout.setError(getString(R.string.error_password_confirm_empty));
            valid = false;
        } else if (!password.equals(passwordConfirm)) {
            binding.inputPasswordConfirmLayout.setError(getString(R.string.error_password_mismatch));
            valid = false;
        }

        // 학번 검증
        if (TextUtils.isEmpty(studentId)) {
            binding.inputStudentIdLayout.setError(getString(R.string.error_student_id_empty));
            valid = false;
        }

        // 이름 검증
        if (TextUtils.isEmpty(name)) {
            binding.inputNameLayout.setError(getString(R.string.error_name_empty));
            valid = false;
        }

        return valid;
    }

    private void clearErrors() {
        binding.inputEmailLayout.setError(null);
        binding.inputPasswordLayout.setError(null);
        binding.inputPasswordConfirmLayout.setError(null);
        binding.inputStudentIdLayout.setError(null);
        binding.inputNameLayout.setError(null);
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.buttonRegister.setEnabled(!loading);
        binding.buttonGoToLogin.setEnabled(!loading);
        binding.inputEmailLayout.setEnabled(!loading);
        binding.inputPasswordLayout.setEnabled(!loading);
        binding.inputPasswordConfirmLayout.setEnabled(!loading);
        binding.inputStudentIdLayout.setEnabled(!loading);
        binding.inputNameLayout.setEnabled(!loading);
    }

    private void showError(Exception e) {
        String message = e != null && !TextUtils.isEmpty(e.getMessage())
                ? e.getMessage()
                : getString(R.string.error_generic);
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
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

    private void navigateToMainWithWelcome(String userName) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("new_user", true);
        intent.putExtra("user_name", userName);
        startActivity(intent);
        finish();
    }

    private void navigateToLogin() {
        finish(); // 현재 액티비티 종료하면 LoginActivity로 돌아감
    }
}
