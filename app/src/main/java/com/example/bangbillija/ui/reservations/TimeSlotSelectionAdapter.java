package com.example.bangbillija.ui.reservations;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bangbillija.R;
import com.example.bangbillija.model.ReservationStatus;
import com.example.bangbillija.model.TimeSlot;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class TimeSlotSelectionAdapter extends RecyclerView.Adapter<TimeSlotSelectionAdapter.ViewHolder> {

    private List<TimeSlot> slots = new ArrayList<>();
    private TimeSlot selectedSlot = null;
    private OnSlotSelectedListener listener;

    public interface OnSlotSelectedListener {
        void onSlotSelected(TimeSlot slot);
    }

    public void setOnSlotSelectedListener(OnSlotSelectedListener listener) {
        this.listener = listener;
    }

    public void setSlots(List<TimeSlot> slots) {
        this.slots = slots != null ? slots : new ArrayList<>();
        notifyDataSetChanged();
    }

    public TimeSlot getSelectedSlot() {
        return selectedSlot;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_time_slot, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TimeSlot slot = slots.get(position);
        holder.bind(slot);
    }

    @Override
    public int getItemCount() {
        return slots.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final View statusBar;
        private final TextView textTime;
        private final TextView textTitle;
        private final TextView textMeta;

        ViewHolder(View view) {
            super(view);
            cardView = (MaterialCardView) view;
            statusBar = view.findViewById(R.id.viewStatus);
            textTime = view.findViewById(R.id.textTime);
            textTitle = view.findViewById(R.id.textTitle);
            textMeta = view.findViewById(R.id.textMeta);
        }

        void bind(TimeSlot slot) {
            textTime.setText(slot.getDisplayTime());
            textTitle.setText(slot.getDisplayLabel());

            String metaInfo = slot.getMetaInfo();
            if (metaInfo != null && !metaInfo.isEmpty()) {
                textMeta.setText(metaInfo);
                textMeta.setVisibility(View.VISIBLE);
            } else {
                textMeta.setVisibility(View.GONE);
            }

            // Color coding
            int statusColor;
            int strokeColor;
            boolean isClickable;

            if (slot.getStatus() == ReservationStatus.AVAILABLE) {
                // Available slot - green
                statusColor = itemView.getContext().getColor(android.R.color.holo_green_dark);
                strokeColor = itemView.getContext().getColor(android.R.color.transparent);
                isClickable = true;

                // Highlight if selected
                if (slot == selectedSlot) {
                    strokeColor = itemView.getContext().getColor(android.R.color.holo_blue_dark);
                    cardView.setStrokeWidth(4);
                } else {
                    cardView.setStrokeWidth(0);
                }
            } else {
                // Blocked slot (reservation or class)
                if (slot.getTimetableEntry() != null) {
                    // Class time - orange
                    statusColor = itemView.getContext().getColor(android.R.color.holo_orange_dark);
                } else {
                    // Reserved - red
                    statusColor = itemView.getContext().getColor(android.R.color.holo_red_dark);
                }
                strokeColor = itemView.getContext().getColor(android.R.color.transparent);
                isClickable = false;
                cardView.setStrokeWidth(0);
            }

            statusBar.setBackgroundColor(statusColor);
            cardView.setStrokeColor(strokeColor);
            cardView.setClickable(isClickable);
            cardView.setAlpha(isClickable ? 1.0f : 0.6f);

            if (isClickable) {
                cardView.setOnClickListener(v -> {
                    TimeSlot previouslySelected = selectedSlot;
                    selectedSlot = slot;

                    // Refresh both items
                    if (previouslySelected != null) {
                        int prevIndex = slots.indexOf(previouslySelected);
                        if (prevIndex >= 0) {
                            notifyItemChanged(prevIndex);
                        }
                    }
                    notifyItemChanged(getAdapterPosition());

                    if (listener != null) {
                        listener.onSlotSelected(slot);
                    }
                });
            } else {
                cardView.setOnClickListener(null);
            }
        }
    }
}
