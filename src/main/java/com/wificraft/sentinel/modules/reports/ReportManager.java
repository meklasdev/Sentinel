package com.wificraft.sentinel.modules.reports;

import com.wificraft.sentinel.modules.reports.evidence.ChatEvidence;
import com.wificraft.sentinel.modules.reports.evidence.LocationEvidence;
import com.wificraft.sentinel.modules.reports.evidence.ObjectEvidence;
import com.wificraft.sentinel.modules.reports.evidence.ScreenshotEvidence;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class ReportManager {
    private final JavaPlugin plugin;
    private final Map<UUID, Report> reports;
    private final Map<UUID, List<UUID>> playerReports; // playerId -> list of report IDs
    private final File reportsFile;
    private final DateTimeFormatter dateTimeFormatter;

    public ReportManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.reports = new ConcurrentHashMap<>();
        this.playerReports = new ConcurrentHashMap<>();
        this.reportsFile = new File(plugin.getDataFolder(), "reports.yml");
        this.dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        loadReports();
    }

    // Create a new report
    public Report createReport(UUID reporterId, UUID reportedPlayerId, String reason) {
        Report report = new Report(reporterId, reportedPlayerId, reason);
        reports.put(report.getId(), report);
        
        // Track reports by player
        playerReports.computeIfAbsent(reportedPlayerId, k -> new ArrayList<>()).add(report.getId());
        
        saveReports();
        return report;
    }

    /**
     * Adds a note to a report
     * @param reportId The ID of the report
     * @param note The note to add
     * @return true if the note was added, false if the report doesn't exist
     */
    public boolean addNote(UUID reportId, String note) {
        Report report = reports.get(reportId);
        if (report != null) {
            report.addNote(note);
            saveReports();
            return true;
        }
        return false;
    }
    
    // Get a report by ID
    public Optional<Report> getReport(UUID reportId) {
        return Optional.ofNullable(reports.get(reportId));
    }

    // Get all reports for a player
    public List<Report> getPlayerReports(UUID playerId) {
        return playerReports.getOrDefault(playerId, Collections.emptyList()).stream()
                .map(this::getReport)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    // Get all open reports
    public List<Report> getOpenReports() {
        return reports.values().stream()
                .filter(r -> r.getStatus() == Report.ReportStatus.OPEN)
                .collect(Collectors.toList());
    }
    
    // Get all reports
    public List<Report> getAllReports() {
        return new ArrayList<>(reports.values());
    }
    
    /**
     * Search reports by various criteria
     * @param query Search query string
     * @param reporterId Filter by reporter UUID (can be null)
     * @param targetId Filter by target player UUID (can be null)
     * @param status Filter by report status (can be null)
     * @return List of matching reports
     */
    /**
     * Search reports by various criteria
     * @param query Search query string
     * @param reporterId Filter by reporter UUID (can be null)
     * @param targetId Filter by target player UUID (can be null)
     * @param status Filter by report status (can be null)
     * @return List of matching reports
     */
    public List<Report> searchReports(String query, UUID reporterId, UUID targetId, Report.ReportStatus status) {
        return reports.values().stream()
                .filter(report -> {
                    // Filter by search query
                    if (query != null && !query.isEmpty()) {
                        String queryLower = query.toLowerCase();
                        boolean matches = report.getReason().toLowerCase().contains(queryLower) ||
                                report.getNotes().stream()
                                        .anyMatch(note -> note.toLowerCase().contains(queryLower));
                        if (!matches) return false;
                    }
                    
                    // Filter by reporter
                    if (reporterId != null && !report.getReporterId().equals(reporterId)) {
                        return false;
                    }
                    
                    // Filter by target
                    if (targetId != null && !report.getReportedPlayerId().equals(targetId)) {
                        return false;
                    }
                    
                    // Filter by status
                    if (status != null && report.getStatus() != status) {
                        return false;
                    }
                    
                    return true;
                })
                .sorted(Comparator.comparing(Report::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    // Assign a report to a moderator
    public boolean assignReport(UUID reportId, String moderatorId) {
        return getReport(reportId).map(report -> {
            report.assignToModerator(moderatorId);
            saveReports();
            return true;
        }).orElse(false);
    }

    // Resolve a report
    public boolean resolveReport(UUID reportId, String resolutionNotes) {
        return getReport(reportId).map(report -> {
            report.resolve(resolutionNotes);
            saveReports();
            return true;
        }).orElse(false);
    }

    // Close a report
    public boolean closeReport(UUID reportId, String reason) {
        return getReport(reportId).map(report -> {
            report.close(reason);
            saveReports();
            return true;
        }).orElse(false);
    }

    // Add evidence to a report
    public boolean addEvidence(UUID reportId, com.wificraft.sentinel.modules.reports.Evidence evidence) {
        return getReport(reportId).map(report -> {
            report.addEvidence(evidence);
            saveReports();
            return true;
        }).orElse(false);
    }

    // Remove evidence from a report
    public boolean removeEvidence(UUID reportId, UUID evidenceId) {
        return getReport(reportId).map(report -> {
            report.removeEvidence(evidenceId);
            saveReports();
            return true;
        }).orElse(false);
    }

    // Get player name by UUID
    public String getPlayerName(UUID uuid) {
        if (uuid == null) return "Unknown";
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return player.getName() != null ? player.getName() : "Unknown";
    }

    // Save reports to file
    /**
     * Saves all reports to disk
     */
    public void saveReports() {
        try {
            YamlConfiguration config = new YamlConfiguration();
            
            // Save each report
            for (Report report : reports.values()) {
                String reportPath = "reports." + report.getId().toString() + ".";
                config.set(reportPath + "reporter", report.getReporterId().toString());
                config.set(reportPath + "reported", report.getReportedPlayerId().toString());
                config.set(reportPath + "reason", report.getReason());
                config.set(reportPath + "createdAt", report.getCreatedAt().format(dateTimeFormatter));
                config.set(reportPath + "status", report.getStatus().name());
                
                if (report.getAssignedModeratorId() != null) {
                    config.set(reportPath + "assignedModerator", report.getAssignedModeratorId());
                }
                if (report.getResolutionNotes() != null) {
                    config.set(reportPath + "resolutionNotes", report.getResolutionNotes());
                }
                if (report.getResolvedAt() != null) {
                    config.set(reportPath + "resolvedAt", report.getResolvedAt().format(dateTimeFormatter));
                }
                
                // Save evidence
                int evidenceIndex = 0;
                for (Evidence evidence : report.getEvidence()) {
                    String evidencePath = reportPath + "evidence." + evidenceIndex + ".";
                    config.set(evidencePath + "id", evidence.getId().toString());
                    config.set(evidencePath + "type", evidence.getType().name());
                    config.set(evidencePath + "notes", evidence.getNotes());
                    config.set(evidencePath + "reporter", evidence.getReporterId().toString());
                    config.set(evidencePath + "createdAt", evidence.getCreatedAt().format(dateTimeFormatter));
                    
                    // Save specific evidence type data
                    if (evidence instanceof ScreenshotEvidence) {
                        ScreenshotEvidence se = (ScreenshotEvidence) evidence;
                        config.set(evidencePath + "screenshot.url", se.getImageUrl());
                        config.set(evidencePath + "screenshot.fileName", se.getFileName());
                    } else if (evidence instanceof ChatEvidence) {
                        ChatEvidence ce = (ChatEvidence) evidence;
                        config.set(evidencePath + "chat.message", ce.getMessage());
                        config.set(evidencePath + "chat.channel", ce.getChannel());
                    } else if (evidence instanceof LocationEvidence) {
                        LocationEvidence le = (LocationEvidence) evidence;
                        config.set(evidencePath + "location.world", le.getWorldName());
                        config.set(evidencePath + "location.x", le.getLocation().getX());
                        config.set(evidencePath + "location.y", le.getLocation().getY());
                        config.set(evidencePath + "location.z", le.getLocation().getZ());
                    } else if (evidence instanceof ObjectEvidence) {
                        ObjectEvidence oe = (ObjectEvidence) evidence;
                        config.set(evidencePath + "object.type", oe.getMaterialName());
                        config.set(evidencePath + "object.amount", oe.getAmount());
                        config.set(evidencePath + "object.displayName", oe.getDisplayName());
                    }
                    
                    evidenceIndex++;
                }
                config.set(reportPath + "evidenceCount", evidenceIndex);
            }
            
            // Save notes for each report
            for (Map.Entry<UUID, Report> entry : reports.entrySet()) {
                String reportPath = "reports." + entry.getKey().toString() + ".";
                List<String> notes = entry.getValue().getNotes();
                
                // Save each note with an index
                int noteIndex = 0;
                for (String note : notes) {
                    config.set(reportPath + "notes." + noteIndex, note);
                    noteIndex++;
                }
                config.set(reportPath + "notesCount", noteIndex);
            }
            
            // Save player reports mapping
            for (Map.Entry<UUID, List<UUID>> entry : playerReports.entrySet()) {
                config.set("player_reports." + entry.getKey().toString(), 
                         entry.getValue().stream().map(UUID::toString).collect(Collectors.toList()));
            }
            
            config.save(reportsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save reports to " + reportsFile, e);
        }
    }

    // Load reports from file
    private void loadReports() {
        if (!reportsFile.exists()) return;
        
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(reportsFile);
            
            // Clear existing data
            reports.clear();
            playerReports.clear();
            
            // Load reports
            if (config.isConfigurationSection("reports")) {
                for (String reportId : config.getConfigurationSection("reports").getKeys(false)) {
                    try {
                        String path = "reports." + reportId + ".";
                        UUID reporterId = UUID.fromString(Objects.requireNonNull(config.getString(path + "reporter")));
                        UUID reportedId = UUID.fromString(Objects.requireNonNull(config.getString(path + "reported")));
                        String reason = config.getString(path + "reason", "No reason provided");
                        
                        Report report = new Report(reporterId, reportedId, reason);
                        
                        // Set report status
                        String statusStr = config.getString(path + "status");
                        if (statusStr != null) {
                            report.setStatus(Report.ReportStatus.valueOf(statusStr));
                        }
                        
                        // Set assigned moderator if exists
                        String assignedModerator = config.getString(path + "assignedModerator");
                        if (assignedModerator != null) {
                            report.setAssignedModerator(assignedModerator);
                        }
                        
                        // Set resolution notes if exists
                        String resolutionNotes = config.getString(path + "resolutionNotes");
                        if (resolutionNotes != null) {
                            report.setResolutionNotes(resolutionNotes);
                        }
                        
                        // Set resolved at if exists
                        String resolvedAtStr = config.getString(path + "resolvedAt");
                        if (resolvedAtStr != null) {
                            report.setResolvedAt(LocalDateTime.parse(resolvedAtStr, dateTimeFormatter));
                        }
                        
                        // Load notes
                        int notesCount = config.getInt(path + "notesCount", 0);
                        for (int i = 0; i < notesCount; i++) {
                            String note = config.getString(path + "notes." + i);
                            if (note != null) {
                                // Add the note directly without the timestamp
                                // (the addNote method will add a new timestamp)
                                report.getNotes().add(note);
                            }
                        }
                        
                        // Load evidence
                        int evidenceCount = config.getInt(path + "evidenceCount", 0);
                        for (int i = 0; i < evidenceCount; i++) {
                            String evidencePath = path + "evidence." + i + ".";
                            String typeStr = config.getString(evidencePath + "type");
                            if (typeStr == null) continue;
                            
                            UUID evidenceReporterId = UUID.fromString(Objects.requireNonNull(
                                config.getString(evidencePath + "reporter")));
                            String notes = config.getString(evidencePath + "notes", "");
                            
                            try {
                                Evidence.EvidenceType type = Evidence.EvidenceType.valueOf(typeStr);
                                Evidence evidence = null;
                                
                                switch (type) {
                                    case CHAT:
                                        String message = config.getString(evidencePath + "chat.message");
                                        String channel = config.getString(evidencePath + "chat.channel", "GLOBAL");
                                        if (message != null) {
                                            evidence = new ChatEvidence(evidenceReporterId, message, channel, notes);
                                        }
                                        break;
                                        
                                    case SCREENSHOT:
                                        String url = config.getString(evidencePath + "screenshot.url");
                                        String fileName = config.getString(evidencePath + "screenshot.fileName");
                                        if (url != null && fileName != null) {
                                            // For now, we'll just create a basic screenshot evidence
                                            // In a real implementation, you'd want to load the actual image data
                                            evidence = new ScreenshotEvidence(evidenceReporterId, url, fileName, 
                                                                         0, "image/png", null, notes);
                                        }
                                        break;
                                        
                                    case LOCATION:
                                        String world = config.getString(evidencePath + "location.world");
                                        double x = config.getDouble(evidencePath + "location.x");
                                        double y = config.getDouble(evidencePath + "location.y");
                                        double z = config.getDouble(evidencePath + "location.z");
                                        // In a real implementation, you'd want to get the world and create a Location
                                        evidence = new LocationEvidence(evidenceReporterId, null, null, notes);
                                        break;
                                        
                                    case OBJECT:
                                        String matName = config.getString(evidencePath + "object.type");
                                        // In a real implementation, you'd want to create the actual ItemStack
                                        evidence = new ObjectEvidence(evidenceReporterId, null, notes);
                                        break;
                                }
                                
                                if (evidence != null) {
                                    report.addEvidence(evidence);
                                }
                            } catch (IllegalArgumentException e) {
                                plugin.getLogger().warning("Invalid evidence type: " + typeStr);
                            }
                        }
                        
                        // Add to reports map
                        reports.put(report.getId(), report);
                        
                        // Add to player reports mapping
                        playerReports.computeIfAbsent(reportedId, k -> new ArrayList<>()).add(report.getId());
                        
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "Error loading report " + reportId, e);
                    }
                }
            }
            
            // Load player reports mapping
            if (config.isConfigurationSection("player_reports")) {
                for (String playerIdStr : config.getConfigurationSection("player_reports").getKeys(false)) {
                    try {
                        UUID playerId = UUID.fromString(playerIdStr);
                        List<String> reportIds = config.getStringList("player_reports." + playerIdStr);
                        
                        List<UUID> reportUuids = reportIds.stream()
                                .map(UUID::fromString)
                                .collect(Collectors.toList());
                                
                        playerReports.put(playerId, reportUuids);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in player_reports: " + playerIdStr);
                    }
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading reports from " + reportsFile, e);
        }
    }
}
