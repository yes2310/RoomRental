package com.example.bangbillija.model;

import java.util.List;
import java.util.Objects;

public class Room {
    private final String id;
    private final String building;
    private final String name;
    private final int capacity;
    private final String floor;
    private final List<String> facilities;
    private final RoomStatus status;

    public Room(String id, String building, String name, int capacity, String floor,
                List<String> facilities, RoomStatus status) {
        this.id = id;
        this.building = building;
        this.name = name;
        this.capacity = capacity;
        this.floor = floor;
        this.facilities = facilities;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getBuilding() {
        return building;
    }

    public String getName() {
        return name;
    }

    public int getCapacity() {
        return capacity;
    }

    public String getFloor() {
        return floor;
    }

    public List<String> getFacilities() {
        return facilities;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public boolean isAvailable() {
        return status == RoomStatus.AVAILABLE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Room)) return false;
        Room room = (Room) o;
        return Objects.equals(id, room.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
