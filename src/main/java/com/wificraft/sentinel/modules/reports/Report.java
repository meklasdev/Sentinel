package com.wificraft.sentinel.modules.reports;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.time.LocalDateTime;
import java.util.*;

// Import evidence types
import com.wificraft.sentinel.modules.reports.evidence.*;

public class Report {
    private final UUID id;
    private final UUID reporterId;
    private final UUID reportedPlayerId;
    private final String reason;
    private final LocalDateTime createdAt;
    private ReportStatus status;
    private final List<com.wificraft.sentinel.modules.reports.Evidence> evidenceList;
    private final List<String> notes;
    private String assignedModeratorId;
    private String resolutionNotes;
    private LocalDateTime resolvedAt;

    public Report(UUID reporterId, UUID reportedPlayerId, String reason) {
        this.id = UUID.randomUUID();
        this.reporterId = reporterId;
        this.reportedPlayerId = reportedPlayerId;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
        this.status = ReportStatus.OPEN;
        this.evidenceList = new ArrayList<>();
        this.notes = new ArrayList<>();
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getReporterId() { return reporterId; }
    public UUID getReportedPlayerId() { return reportedPlayerId; }
    public String getReason() { return reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public ReportStatus getStatus() { return status; }
    public List<com.wificraft.sentinel.modules.reports.Evidence> getEvidence() { return new ArrayList<>(evidenceList); }
    public String getAssignedModeratorId() { return assignedModeratorId; }
    public String getResolutionNotes() { return resolutionNotes; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public List<String> getNotes() { return new ArrayList<>(notes); }

    // Setters
    public void setStatus(ReportStatus status) { this.status = status; }
    public void setAssignedModerator(String moderatorId) { this.assignedModeratorId = moderatorId; }
    public void setResolutionNotes(String notes) { this.resolutionNotes = notes; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    // Note management
    public void addNote(String note) {
        if (note != null && !note.trim().isEmpty()) {
            notes.add("[" + LocalDateTime.now().toString() + "] " + note);
        }
    }
    
    // Evidence management
    public void addEvidence(com.wificraft.sentinel.modules.reports.Evidence evidence) {
        if (evidence != null) {
            evidenceList.add(evidence);
        }
    }

    public void removeEvidence(UUID evidenceId) {
        evidenceList.removeIf(e -> e.getId().equals(evidenceId));
    }

    // Status management
    public void assignToModerator(String moderatorId) {
        this.assignedModeratorId = moderatorId;
        this.status = ReportStatus.IN_PROGRESS;
    }

    public void resolve(String resolutionNotes) {
        this.status = ReportStatus.RESOLVED;
        this.resolutionNotes = resolutionNotes;
        this.resolvedAt = LocalDateTime.now();
    }

    public void close(String reason) {
        this.status = ReportStatus.CLOSED;
        this.resolutionNotes = reason;
        this.resolvedAt = LocalDateTime.now();
    }

    public enum ReportStatus {
        OPEN,
        IN_PROGRESS,
        RESOLVED,
        CLOSED,
        REJECTED
    }
}
