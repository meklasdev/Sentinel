package com.wificraft.sentinel.modules.security;

import org.bukkit.entity.Player;

public class BehaviorEvent {
    private final String type;
    private final long timestamp;
    private final double x;
    private final double y;
    private final double z;
    private final int distance;
    private final String playerName;
    private final String message; // For chat events

    public BehaviorEvent(String type, long timestamp, double x, double y, double z, int distance, Player player) {
        this.type = type;
        this.timestamp = timestamp;
        this.x = x;
        this.y = y;
        this.z = z;
        this.distance = distance;
        this.playerName = player != null ? player.getName() : "unknown";
        this.message = null; // Null for non-chat events
    }

    public BehaviorEvent(String type, long timestamp, String message, Player player) {
        this.type = type;
        this.timestamp = timestamp;
        this.x = 0;
        this.y = 0;
        this.z = 0;
        this.distance = 0;
        this.playerName = player != null ? player.getName() : "unknown";
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public int getDistance() {
        return distance;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getMessage() {
        return message;
    }
}
