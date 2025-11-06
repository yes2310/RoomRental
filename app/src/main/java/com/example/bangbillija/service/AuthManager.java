package com.example.bangbillija.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.Arrays;
import java.util.List;

public class AuthManager {

    private static AuthManager instance;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    // 관리자 이메일 목록 (하드코딩)
    private static final List<String> ADMIN_EMAILS = Arrays.asList(
            "admin@bangbillija.com",
            "admin@example.com",
            "admin@admin.com"
    );

    public static synchronized AuthManager getInstance() {
        if (instance == null) {
            instance = new AuthManager();
        }
        return instance;
    }

    public FirebaseUser currentUser() {
        return auth.getCurrentUser();
    }

    public void signIn(String email, String password, Completion completion) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        completion.onSuccess();
                    } else {
                        completion.onFailure(task.getException());
                    }
                });
    }

    public void signUp(String email, String password, String displayName, Completion completion) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        completion.onFailure(task.getException());
                        return;
                    }

                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null) {
                        completion.onFailure(new IllegalStateException("사용자 정보를 불러오지 못했습니다."));
                        return;
                    }

                    String safeName = (displayName == null || displayName.trim().isEmpty())
                            ? user.getEmail()
                            : displayName.trim();

                    user.updateProfile(new UserProfileChangeRequest.Builder()
                            .setDisplayName(safeName)
                            .build())
                            .addOnCompleteListener(updateTask -> {
                                if (updateTask.isSuccessful()) {
                                    completion.onSuccess();
                                } else {
                                    completion.onFailure(updateTask.getException());
                                }
                            });
                });
    }

    public void signOut() {
        auth.signOut();
    }

    public boolean isAdmin() {
        FirebaseUser user = currentUser();
        if (user == null || user.getEmail() == null) {
            return false;
        }
        return ADMIN_EMAILS.contains(user.getEmail().toLowerCase());
    }

    public interface Completion {
        void onSuccess();

        void onFailure(Exception e);
    }
}
