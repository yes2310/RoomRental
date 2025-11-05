package com.example.bangbillija.ui.rooms;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bangbillija.R;
import com.example.bangbillija.databinding.ItemRoomBinding;
import com.example.bangbillija.model.Room;
import com.example.bangbillija.model.RoomStatus;

import java.util.Locale;
import java.util.stream.Collectors;

public class RoomListAdapter extends ListAdapter<Room, RoomListAdapter.RoomViewHolder> {

    interface RoomClickListener {
        void onRoomClicked(Room room);
    }

    private final RoomClickListener listener;

    RoomListAdapter(RoomClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemRoomBinding binding = ItemRoomBinding.inflate(inflater, parent, false);
        return new RoomViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class RoomViewHolder extends RecyclerView.ViewHolder {
        private final ItemRoomBinding binding;

        RoomViewHolder(ItemRoomBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Room room) {
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRoomClicked(room);
                }
            });

            binding.textRoomName.setText(room.getName());
            binding.textRoomNumber.setText(extractRoomNumber(room.getName()));
            binding.textRoomMeta.setText(String.format(Locale.getDefault(),
                    "👥 %d명 • 📍 %s", room.getCapacity(), room.getFloor()));

            String facilities = room.getFacilities().stream()
                    .collect(Collectors.joining("  •  "));
            binding.textFacilities.setText(facilities);

            updateStatusChip(room);
        }

        private void updateStatusChip(Room room) {
            RoomStatus status = room.getStatus();
            if (status == RoomStatus.AVAILABLE) {
                binding.textStatus.setBackgroundResource(R.drawable.bg_status_available);
                binding.textStatus.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_available));
                binding.textStatus.setText("예약 가능");
            } else if (status == RoomStatus.RESERVED) {
                binding.textStatus.setBackgroundResource(R.drawable.bg_status_reserved);
                binding.textStatus.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_reserved));
                binding.textStatus.setText("예약됨");
            } else {
                binding.textStatus.setBackgroundResource(R.drawable.bg_status_pending);
                binding.textStatus.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_pending));
                binding.textStatus.setText("대기중");
            }
        }

        private String extractRoomNumber(String roomName) {
            String[] parts = roomName.split(" ");
            return parts.length > 1 ? parts[1] : roomName;
        }
    }

    private static final DiffUtil.ItemCallback<Room> DIFF_CALLBACK = new DiffUtil.ItemCallback<Room>() {
        @Override
        public boolean areItemsTheSame(@NonNull Room oldItem, @NonNull Room newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Room oldItem, @NonNull Room newItem) {
            return oldItem.getName().equals(newItem.getName())
                    && oldItem.getCapacity() == newItem.getCapacity()
                    && oldItem.getFloor().equals(newItem.getFloor())
                    && oldItem.getFacilities().equals(newItem.getFacilities())
                    && oldItem.getStatus() == newItem.getStatus();
        }
    };
}
