package com.example.bangbillija.ui.calendar;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bangbillija.R;
import com.example.bangbillija.databinding.ItemTimeSlotBinding;
import com.example.bangbillija.model.Reservation;
import com.example.bangbillija.model.ReservationStatus;
import com.example.bangbillija.model.TimeSlot;

class TimeSlotAdapter extends ListAdapter<TimeSlot, TimeSlotAdapter.TimeSlotViewHolder> {

    interface SlotClickListener {
        void onReservationSelected(Reservation reservation);
    }

    private final SlotClickListener listener;

    TimeSlotAdapter(SlotClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public TimeSlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemTimeSlotBinding binding = ItemTimeSlotBinding.inflate(inflater, parent, false);
        return new TimeSlotViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TimeSlotViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class TimeSlotViewHolder extends RecyclerView.ViewHolder {

        private final ItemTimeSlotBinding binding;

        TimeSlotViewHolder(ItemTimeSlotBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(TimeSlot slot) {
            binding.textTime.setText(slot.getDisplayTime());
            binding.textTitle.setText(slot.getDisplayLabel());
            binding.textMeta.setText(slot.getMetaInfo());

            ReservationStatus status = slot.getStatus();
            int colorRes = R.color.outline_variant;
            binding.textMeta.setVisibility(View.VISIBLE);
            if (status == ReservationStatus.AVAILABLE) {
                colorRes = R.color.status_available;
                binding.textMeta.setVisibility(View.GONE);
            } else if (status == ReservationStatus.RESERVED) {
                binding.textMeta.setVisibility(View.VISIBLE);
                colorRes = R.color.status_reserved;
            } else if (status == ReservationStatus.PENDING) {
                binding.textMeta.setVisibility(View.VISIBLE);
                colorRes = R.color.status_pending;
            } else if (status == ReservationStatus.CHECKED_IN) {
                binding.textMeta.setVisibility(View.VISIBLE);
                colorRes = R.color.status_checked_in;
            } else {
                binding.textMeta.setVisibility(View.VISIBLE);
            }
            binding.viewStatus.setBackgroundColor(ContextCompat.getColor(binding.getRoot().getContext(), colorRes));

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null && slot.getReservation() != null) {
                    listener.onReservationSelected(slot.getReservation());
                }
            });
        }
    }

    private static final DiffUtil.ItemCallback<TimeSlot> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<TimeSlot>() {
                @Override
                public boolean areItemsTheSame(@NonNull TimeSlot oldItem, @NonNull TimeSlot newItem) {
                    return oldItem.getDate().equals(newItem.getDate())
                            && oldItem.getStart().equals(newItem.getStart());
                }

                @Override
                public boolean areContentsTheSame(@NonNull TimeSlot oldItem, @NonNull TimeSlot newItem) {
                    return oldItem.getStatus() == newItem.getStatus()
                            && equalsReservation(oldItem.getReservation(), newItem.getReservation());
                }

                private boolean equalsReservation(Reservation a, Reservation b) {
                    if (a == b) {
                        return true;
                    }
                    if (a == null || b == null) {
                        return false;
                    }
                    return a.getId().equals(b.getId());
                }
            };
}
