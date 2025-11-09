package com.example.bangbillija.ui.reservations;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bangbillija.R;
import com.example.bangbillija.databinding.ItemMyReservationBinding;
import com.example.bangbillija.model.Reservation;
import com.example.bangbillija.model.ReservationStatus;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class MyReservationsAdapter extends ListAdapter<Reservation, MyReservationsAdapter.ViewHolder> {

    public interface ReservationClickListener {
        void onPrimaryAction(Reservation reservation);

        void onSecondaryAction(Reservation reservation);
    }

    private final ReservationClickListener listener;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("M월 d일 (E)");

    public MyReservationsAdapter(ReservationClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemMyReservationBinding binding = ItemMyReservationBinding.inflate(inflater, parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemMyReservationBinding binding;

        ViewHolder(ItemMyReservationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Reservation reservation) {
            binding.textTitle.setText(reservation.getTitle());
            binding.textRoom.setText(reservation.getRoomName());

            String meta = "📅 " + reservation.getDate().format(dateFormatter)
                    + "  •  🕐 " + reservation.getStartTime() + " - " + reservation.getEndTime()
                    + "  •  👥 " + reservation.getAttendees() + "명";
            binding.textMeta.setText(meta);

            updateBadge(reservation);
            updateButtons(reservation);

            binding.buttonPrimary.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPrimaryAction(reservation);
                }
            });
            binding.buttonSecondary.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSecondaryAction(reservation);
                }
            });
        }

        private void updateBadge(Reservation reservation) {
            ReservationStatus status = reservation.getStatus();
            boolean isToday = reservation.getDate().isEqual(LocalDate.now());
            binding.textBadge.setVisibility(View.VISIBLE);
            if (isToday && status == ReservationStatus.RESERVED) {
                binding.textBadge.setText("오늘");
                binding.textBadge.setTextColor(binding.getRoot().getContext().getColor(R.color.tertiary));
                binding.textBadge.setBackgroundResource(R.drawable.bg_status_pending);
            } else if (status == ReservationStatus.RESERVED) {
                binding.textBadge.setText("확정");
                binding.textBadge.setTextColor(binding.getRoot().getContext().getColor(R.color.status_available));
                binding.textBadge.setBackgroundResource(R.drawable.bg_status_available);
            } else if (status == ReservationStatus.PENDING) {
                binding.textBadge.setText("대기중");
                binding.textBadge.setTextColor(binding.getRoot().getContext().getColor(R.color.status_pending));
                binding.textBadge.setBackgroundResource(R.drawable.bg_status_pending);
            } else if (status == ReservationStatus.CANCELLED) {
                binding.textBadge.setText("취소됨");
                binding.textBadge.setTextColor(binding.getRoot().getContext().getColor(R.color.status_reserved));
                binding.textBadge.setBackgroundResource(R.drawable.bg_status_reserved);
            } else {
                binding.textBadge.setVisibility(View.GONE);
            }
        }

        private void updateButtons(Reservation reservation) {
            if (reservation.getStatus() == ReservationStatus.RESERVED) {
                binding.buttonPrimary.setText("체크인");
                binding.buttonSecondary.setText("상세보기");
            } else if (reservation.getStatus() == ReservationStatus.PENDING) {
                binding.buttonPrimary.setText("상세보기");
                binding.buttonSecondary.setText("취소");
            } else {
                binding.buttonPrimary.setText("상세보기");
                binding.buttonSecondary.setText("삭제");
            }
        }
    }

    private static final DiffUtil.ItemCallback<Reservation> DIFF_CALLBACK = new DiffUtil.ItemCallback<Reservation>() {
        @Override
        public boolean areItemsTheSame(@NonNull Reservation oldItem, @NonNull Reservation newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Reservation oldItem, @NonNull Reservation newItem) {
            return oldItem.equals(newItem);
        }
    };
}
