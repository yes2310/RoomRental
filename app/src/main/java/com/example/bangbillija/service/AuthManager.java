package com.example.bangbillija.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AuthManager {

    private static AuthManager instance;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Fallback 관리자 이메일 목록 (Firestore 로드 실패 시 사용)
    private static final List<String> FALLBACK_ADMIN_EMAILS = Arrays.asList(
            "admin@bangbillija.com",
            "admin@example.com",
            "yes2310@naver.com"
    );

    // 캐시된 관리자 이메일 목록
    private final Set<String> adminEmails = new HashSet<>(FALLBACK_ADMIN_EMAILS);
    private boolean adminListLoaded = false;

    private AuthManager() {
        loadAdminList();
    }

    public static synchronized AuthManager getInstance() {
        if (instance == null) {
            instance = new AuthManager();
        }
        return instance;
    }

    /**
     * Firestore에서 관리자 목록을 로드합니다.
     * 'admins' 컬렉션의 모든 문서 ID를 관리자 이메일로 사용합니다.
     */
    private void loadAdminList() {
        db.collection("admins")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Set<String> newAdminEmails = new HashSet<>();
                    querySnapshot.getDocuments().forEach(doc -> {
                        String email = doc.getId(); // 문서 ID가 이메일
                        if (email != null && !email.isEmpty()) {
                            newAdminEmails.add(email.toLowerCase());
                        }
                    });

                    if (!newAdminEmails.isEmpty()) {
                        adminEmails.clear();
                        adminEmails.addAll(newAdminEmails);
                    }
                    adminListLoaded = true;
                    android.util.Log.d("AuthManager", "Admin list loaded: " + adminEmails.size() + " admins");
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("AuthManager", "Failed to load admin list, using fallback", e);
                    // fallback 목록 사용 (이미 초기화됨)
                    adminListLoaded = true;
                });
    }

    /**
     * 관리자 목록을 새로고침합니다.
     */
    public void refreshAdminList() {
        loadAdminList();
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

    /**
     * 회원가입 (학번과 이름 포함)
     * @param email 이메일
     * @param password 비밀번호
     * @param name 이름
     * @param studentId 학번
     * @param completion 완료 콜백
     */
    public void signUp(String email, String password, String name, String studentId, Completion completion) {
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

                    // FirebaseAuth의 displayName에 이름 저장
                    user.updateProfile(new UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build())
                            .addOnCompleteListener(updateTask -> {
                                if (!updateTask.isSuccessful()) {
                                    completion.onFailure(updateTask.getException());
                                    return;
                                }

                                // Firestore의 users 컬렉션에 사용자 정보 저장
                                saveUserToFirestore(user.getUid(), email, name, studentId, completion);
                            });
                });
    }

    /**
     * Firestore에 사용자 정보 저장
     */
    private void saveUserToFirestore(String userId, String email, String name, String studentId, Completion completion) {
        java.util.HashMap<String, Object> userMap = new java.util.HashMap<>();
        userMap.put("userId", userId);
        userMap.put("email", email);
        userMap.put("name", name);
        userMap.put("studentId", studentId);
        userMap.put("createdAt", System.currentTimeMillis());

        db.collection("users")
                .document(userId)
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("AuthManager", "User info saved to Firestore: " + userId);
                    completion.onSuccess();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("AuthManager", "Failed to save user info", e);
                    completion.onFailure(e);
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
        return adminEmails.contains(user.getEmail().toLowerCase());
    }

    public interface Completion {
        void onSuccess();

        void onFailure(Exception e);
    }
}
