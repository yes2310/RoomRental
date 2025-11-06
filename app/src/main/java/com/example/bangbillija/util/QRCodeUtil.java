package com.example.bangbillija.util;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.HashMap;
import java.util.Map;

public class QRCodeUtil {

    /**
     * QR 코드 생성
     * @param content QR 코드에 담을 내용
     * @param width QR 코드 가로 크기 (픽셀)
     * @param height QR 코드 세로 크기 (픽셀)
     * @return QR 코드 Bitmap
     */
    public static Bitmap generateQRCode(String content, int width, int height) throws WriterException {
        QRCodeWriter writer = new QRCodeWriter();

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }

        return bitmap;
    }

    /**
     * 예약 정보를 QR 코드 문자열로 변환
     * @param reservationId 예약 ID
     * @param roomId 강의실 ID
     * @param date 예약 날짜 (yyyy-MM-dd)
     * @param startTime 시작 시간 (HH:mm)
     * @return QR 코드 문자열 (JSON 형식)
     */
    public static String createReservationQRContent(String reservationId, String roomId,
                                                    String date, String startTime) {
        // JSON 형식으로 예약 정보를 인코딩
        return String.format("{\"reservationId\":\"%s\",\"roomId\":\"%s\",\"date\":\"%s\",\"startTime\":\"%s\"}",
                reservationId, roomId, date, startTime);
    }
}
