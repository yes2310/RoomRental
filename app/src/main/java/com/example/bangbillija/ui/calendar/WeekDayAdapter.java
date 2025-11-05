package com.example.bangbillija.ui.calendar;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bangbillija.R;
import com.example.bangbillija.databinding.ItemWeekDayBinding;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

class WeekDayAdapter extends ListAdapter<LocalDate, WeekDayAdapter.WeekDayViewHolder> {

    interface DayClickListener {
        void onDayClicked(LocalDate date);
    }

    private final DayClickListener listener;
    private LocalDate selectedDate;
    private LocalDate today = LocalDate.now();

    WeekDayAdapter(DayClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        this.selectedDate = today;
    }

    void setSelectedDate(LocalDate date) {
        this.selectedDate = date;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public WeekDayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemWeekDayBinding binding = ItemWeekDayBinding.inflate(inflater, parent, false);
        return new WeekDayViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull WeekDayViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class WeekDayViewHolder extends RecyclerView.ViewHolder {

        private final ItemWeekDayBinding binding;

        WeekDayViewHolder(ItemWeekDayBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(LocalDate date) {
            binding.textDayName.setText(date.getDayOfWeek()
                    .getDisplayName(TextStyle.SHORT, Locale.KOREAN));
            binding.textDayNumber.setText(String.valueOf(date.getDayOfMonth()));

            boolean isSelected = date.equals(selectedDate);
            boolean isToday = date.equals(today);

            binding.getRoot().setStrokeColor(binding.getRoot().getContext().getColor(
                    isSelected ? R.color.primary : android.R.color.transparent));
            binding.getRoot().setCardBackgroundColor(binding.getRoot().getContext().getColor(
                    isSelected ? R.color.primary_container : R.color.surface));
            binding.textDayName.setTextColor(binding.getRoot().getContext().getColor(
                    isSelected ? R.color.on_primary_container : R.color.on_surface_variant));
            binding.textDayNumber.setTextColor(binding.getRoot().getContext().getColor(
                    isSelected ? R.color.on_surface : R.color.on_surface));

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDayClicked(date);
                }
            });

            binding.textDayName.setContentDescription(isToday ? "오늘" : "");
        }
    }

    private static final DiffUtil.ItemCallback<LocalDate> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<LocalDate>() {
                @Override
                public boolean areItemsTheSame(@NonNull LocalDate oldItem, @NonNull LocalDate newItem) {
                    return oldItem.equals(newItem);
                }

                @Override
                public boolean areContentsTheSame(@NonNull LocalDate oldItem, @NonNull LocalDate newItem) {
                    return oldItem.equals(newItem);
                }
            };
}
