package com.wificraft.sentinel.modules.reports;

import org.bukkit.Location;
import java.time.LocalDateTime;
import java.util.UUID;

public abstract class Evidence {
    protected final UUID id;
    protected final UUID reporterId;
    protected final LocalDateTime createdAt;
    protected String notes;
    protected EvidenceType type;

    protected Evidence(UUID reporterId, String notes, EvidenceType type) {
        this.id = UUID.randomUUID();
        this.reporterId = reporterId;
        this.createdAt = LocalDateTime.now();
        this.notes = notes;
        this.type = type;
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getReporterId() { return reporterId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getNotes() { return notes; }
    public EvidenceType getType() { return type; }

    // Setters
    public void setNotes(String notes) { this.notes = notes; }

    public enum EvidenceType {
        CHAT,
        SCREENSHOT,
        LOCATION,
        OBJECT,
        CUSTOM
    }
}
