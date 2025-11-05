package com.example.bangbillija.service;

import android.content.Context;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Stubbed QR 매니저. 실제 프로젝트에서는 CameraX + ML Kit를 연동해 QR 코드 분석을 처리하세요.
 */
public class QrManager {

    private final Context context;

    public QrManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public void simulateScan(Consumer<String> onResult) {
        // 임시 구현: 실제 스캔 대신 더미 성공 메시지를 반환합니다.
        String token = UUID.randomUUID().toString().substring(0, 8);
        if (onResult != null) {
            onResult.accept("체크인 토큰 확인 완료: " + token);
        }
    }
}
