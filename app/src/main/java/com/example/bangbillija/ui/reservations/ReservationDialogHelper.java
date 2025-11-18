package com.example.bangbillija.ui.reservations;

import android.content.Context;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.example.bangbillija.data.ReservationRepository;
import com.example.bangbillija.model.Reservation;
import com.example.bangbillija.service.FirestoreManager;
import com.google.android.material.snackbar.Snackbar;

/**
 * 예약 관련 다이얼로그를 표시하는 헬퍼 클래스
 * CalendarFragment와 MyReservationsFragment에서 중복 코드를 제거하기 위해 생성
 */
public class ReservationDialogHelper {

    /**
     * 예약 취소 확인 다이얼로그를 표시합니다.
     *
     * @param context           Context (Fragment의 requireContext())
     * @param reservation       취소할 예약
     * @param rootView          Snackbar를 표시할 루트 View (binding.getRoot())
     * @param onSuccessCallback 취소 성공 후 실행할 콜백
     */
    public static void showCancelConfirmDialog(
            Context context,
            Reservation reservation,
            View rootView,
            Runnable onSuccessCallback) {

        new AlertDialog.Builder(context)
                .setTitle("예약 취소")
                .setMessage("'" + reservation.getTitle() + "' 예약을 취소하시겠습니까?\n\n" +
                        "날짜: " + reservation.getDate() + "\n" +
                        "시간: " + reservation.getStartTime() + " - " + reservation.getEndTime())
                .setPositiveButton("취소하기", (dialog, which) -> {
                    ReservationRepository.getInstance()
                            .cancelReservationByReservationId(reservation.getId(),
                                    new FirestoreManager.FirestoreCallback<Void>() {
                                        @Override
                                        public void onSuccess(Void result) {
                                            Snackbar.make(rootView, "예약이 취소되었습니다", Snackbar.LENGTH_SHORT).show();
                                            if (onSuccessCallback != null) {
                                                onSuccessCallback.run();
                                            }
                                        }

                                        @Override
                                        public void onFailure(Exception e) {
                                            Snackbar.make(rootView, "취소 실패: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                                        }
                                    });
                })
                .setNegativeButton("돌아가기", null)
                .show();
    }

    /**
     * 예약 삭제 확인 다이얼로그를 표시합니다.
     *
     * @param context           Context (Fragment의 requireContext())
     * @param reservation       삭제할 예약
     * @param rootView          Snackbar를 표시할 루트 View (binding.getRoot())
     * @param onSuccessCallback 삭제 성공 후 실행할 콜백
     */
    public static void showDeleteConfirmDialog(
            Context context,
            Reservation reservation,
            View rootView,
            Runnable onSuccessCallback) {

        new AlertDialog.Builder(context)
                .setTitle("예약 삭제")
                .setMessage("'" + reservation.getTitle() + "' 예약 기록을 완전히 삭제하시겠습니까?\n\n" +
                        "이 작업은 되돌릴 수 없습니다.")
                .setPositiveButton("삭제", (dialog, which) -> {
                    FirestoreManager.getInstance()
                            .deleteReservation(reservation.getId(),
                                    new FirestoreManager.FirestoreCallback<Void>() {
                                        @Override
                                        public void onSuccess(Void result) {
                                            Snackbar.make(rootView, "예약이 삭제되었습니다", Snackbar.LENGTH_SHORT).show();
                                            if (onSuccessCallback != null) {
                                                onSuccessCallback.run();
                                            }
                                        }

                                        @Override
                                        public void onFailure(Exception e) {
                                            Snackbar.make(rootView, "삭제 실패: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                                        }
                                    });
                })
                .setNegativeButton("취소", null)
                .show();
    }
}
