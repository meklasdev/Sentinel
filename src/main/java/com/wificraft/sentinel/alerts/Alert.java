package com.wificraft.sentinel.alerts;

import java.util.UUID;

public class Alert {
    private final UUID id;
    private final String player;
    private final String title;
    private final String description;
    private final SeverityLevel severity;
    private final long timestamp;

    public Alert(String player, String title, String description, SeverityLevel severity) {
        this.id = UUID.randomUUID();
        this.player = player;
        this.title = title;
        this.description = description;
        this.severity = severity;
        this.timestamp = System.currentTimeMillis();
    }

    public UUID getId() {
        return id;
    }

    public String getPlayer() {
        return player;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public SeverityLevel getSeverity() {
        return severity;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getEmoji() {
        return severity.getEmoji();
    }

    @Override
    public String toString() {
        return String.format("[%s] %s: %s", severity.getEmoji(), title, description);
    }
}
