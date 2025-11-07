package com.example.bangbillija.ui.timetable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bangbillija.R;
import com.example.bangbillija.model.TimetableEntry;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class TimetableAdapter extends RecyclerView.Adapter<TimetableAdapter.ViewHolder> {

    private List<TimetableEntry> entries = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onDeleteClick(TimetableEntry entry);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setEntries(List<TimetableEntry> entries) {
        this.entries = entries != null ? entries : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<TimetableEntry> getEntries() {
        return new ArrayList<>(entries);
    }

    public void addEntry(TimetableEntry entry) {
        entries.add(entry);
        notifyItemInserted(entries.size() - 1);
    }

    public void removeEntry(TimetableEntry entry) {
        int position = entries.indexOf(entry);
        if (position != -1) {
            entries.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void clearAll() {
        int size = entries.size();
        entries.clear();
        notifyItemRangeRemoved(0, size);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_timetable, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TimetableEntry entry = entries.get(position);
        holder.bind(entry);
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textDay;
        private final TextView textCourseName;
        private final TextView textRoom;
        private final TextView textTime;
        private final TextView textInfo;
        private final MaterialButton buttonDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textDay = itemView.findViewById(R.id.textDay);
            textCourseName = itemView.findViewById(R.id.textCourseName);
            textRoom = itemView.findViewById(R.id.textRoom);
            textTime = itemView.findViewById(R.id.textTime);
            textInfo = itemView.findViewById(R.id.textInfo);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }

        public void bind(TimetableEntry entry) {
            textDay.setText(entry.getDayOfWeekKorean());
            textCourseName.setText(entry.getCourseName());
            textRoom.setText(entry.getRoomName());
            textTime.setText(String.format("🕐 %s - %s",
                    entry.getStartTime().toString(),
                    entry.getEndTime().toString()));
            textInfo.setText(String.format("👥 %d명  •  %s",
                    entry.getAttendees(),
                    entry.getProfessor()));

            buttonDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(entry);
                }
            });
        }
    }
}
