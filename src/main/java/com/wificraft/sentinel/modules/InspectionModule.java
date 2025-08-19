package com.wificraft.sentinel.modules;

import com.wificraft.sentinel.WiFiCraftSentinel;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Handles player inspections, reports, and evidence collection
 */
public class InspectionModule implements Listener {
    
    private final WiFiCraftSentinel plugin;
    private final Object moderatorRanking;
    private final Map<UUID, PlayerStats> playerStats = new ConcurrentHashMap<>();
    private final Map<UUID, List<InspectionReport>> reports = new HashMap<>();
    private final Map<UUID, List<Evidence>> evidenceMap = new HashMap<>();
    private final File dataFolder;
    private final File reportsFile;
    private final File evidenceFolder;
    private final File playerStatsFile;
    private final File reportsFolder;
    private final Map<String, Map<String, Object>> reportTemplates = new HashMap<>();
    private final Map<UUID, PlayerStats> moderatorStats = new HashMap<>();

    public InspectionModule(WiFiCraftSentinel plugin, Object moderatorRanking) {
        this.plugin = plugin;
        this.moderatorRanking = moderatorRanking;
        this.dataFolder = plugin.getDataFolder();
        this.reportsFile = new File(dataFolder, "reports.yml");
        this.evidenceFolder = new File(dataFolder, "evidence");
        this.playerStatsFile = new File(dataFolder, "player_stats.yml");
        this.reportsFolder = new File(dataFolder, "reports");
        
        // Ensure data folders exist
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        if (!evidenceFolder.exists()) {
            evidenceFolder.mkdirs();
        }
        if (!reportsFolder.exists()) {
            reportsFolder.mkdirs();
        }
        
        // Load existing data
        loadPlayerStats();
        loadReports();
    }

    /**
     * Possible statuses for an inspection report
     */
    public enum ReportStatus {
        OPEN,
        IN_PROGRESS,
        RESOLVED,
        CLOSED,
        REJECTED,
        REOPENED
    }

    /**
     * Represents an inspection report for a player
     */
    public static class InspectionReport {
        private final UUID reportId;
        private final UUID reporterId;
        private final UUID targetId;
        private final String reason;
        private final long timestamp;
        private ReportStatus status;
        private boolean resolved;
        private UUID resolvedBy;
        private long resolvedAt;
        private final List<UUID> evidenceIds = new ArrayList<>();
        private final List<String> evidence;
        private String notes;
        
        public InspectionReport(UUID reporterId, UUID targetId, String reason, List<String> evidence) {
            this(UUID.randomUUID(), reporterId, targetId, reason, System.currentTimeMillis(), evidence);
        }
        
        public InspectionReport(UUID reportId, UUID reporterId, UUID targetId, String reason, long timestamp, List<String> evidence) {
            this.reportId = reportId != null ? reportId : UUID.randomUUID();
            this.reporterId = reporterId;
            this.targetId = targetId;
            this.reason = reason != null ? reason : "";
            this.timestamp = timestamp > 0 ? timestamp : System.currentTimeMillis();
            this.status = ReportStatus.OPEN;
            this.resolved = false;
            this.evidence = evidence != null ? new ArrayList<>(evidence) : new ArrayList<>();
            this.notes = "";
        }
        
        // Getters and setters
        public UUID getReportId() { return reportId; }
        public UUID getReporterId() { return reporterId; }
        public UUID getInspectorId() { return reporterId; }
        public UUID getTargetId() { return targetId; }
        public String getReason() { return reason; }
        public long getTimestamp() { return timestamp; }
        public ReportStatus getStatus() { return status; }
        public void setStatus(ReportStatus status) { 
            if (status != null) {
                this.status = status; 
            }
        }
        public boolean isResolved() { return resolved; }
        public void setResolved(boolean resolved) { this.resolved = resolved; }
        public UUID getResolvedBy() { return resolvedBy; }
        public void setResolvedBy(UUID resolvedBy) { this.resolvedBy = resolvedBy; }
        public long getResolvedAt() { return resolvedAt; }
        public void setResolvedAt(long resolvedAt) { this.resolvedAt = resolvedAt; }
        public List<UUID> getEvidenceIds() { return new ArrayList<>(evidenceIds); }
        public List<String> getEvidence() { return new ArrayList<>(evidence); }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes != null ? notes : ""; }
        
        public void addEvidence(Evidence evidence) { 
            if (evidence != null && evidence.getEvidenceId() != null) {
                evidenceIds.add(evidence.getEvidenceId()); 
            }
        }
        
        public void resolve(UUID resolvedBy) {
            this.resolved = true;
            this.resolvedBy = resolvedBy;
            this.resolvedAt = System.currentTimeMillis();
            this.status = ReportStatus.RESOLVED;
        }
        
        public void reopen() {
            this.resolved = false;
            this.resolvedBy = null;
            this.resolvedAt = 0;
            this.status = ReportStatus.REOPENED;
        }
    }

    /**
     * Represents evidence for an inspection report
     */
    public static class Evidence {
        private final UUID id;
        private final EvidenceType type;
        private final Map<String, Object> data;
        private final UUID collectedBy;
        private final long collectedAt;
        private boolean resolved;

        public enum EvidenceType {
            CHAT, INVENTORY, LOCATION, SCREENSHOT, OTHER;
            
            public static EvidenceType fromString(String name) {
                if (name == null) return OTHER;
                try {
                    return EvidenceType.valueOf(name.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return OTHER;
                }
            }
        }

        public Evidence(EvidenceType type, Map<String, Object> data, UUID collectedBy) {
            this(UUID.randomUUID(), type, data, collectedBy, System.currentTimeMillis(), false);
        }
        
        public Evidence(UUID id, EvidenceType type, Map<String, Object> data, 
                       UUID collectedBy, long collectedAt, boolean resolved) {
            this.id = id != null ? id : UUID.randomUUID();
            this.type = type != null ? type : EvidenceType.OTHER;
            this.data = data != null ? new HashMap<>(data) : new HashMap<>();
            this.collectedBy = collectedBy;
            this.collectedAt = collectedAt > 0 ? collectedAt : System.currentTimeMillis();
            this.resolved = resolved;
        }

        public UUID getId() { return id; }
        public UUID getEvidenceId() { return id; }
        public EvidenceType getType() { return type != null ? type : EvidenceType.OTHER; }
        public Map<String, Object> getData() { return new HashMap<>(data); }
        public UUID getCollectedBy() { return collectedBy; }
        public long getCollectedAt() { return collectedAt; }
        public boolean isResolved() { return resolved; }
        public void setResolved(boolean resolved) { this.resolved = resolved; }
    }

    /**
     * Represents statistics for a player in the inspection system
     */
    public static class PlayerStats {
        private static final int MAX_CHAT_LOG_ENTRIES = 100;
        private static final long CHAT_LOG_MAX_AGE = TimeUnit.DAYS.toMillis(7);
        
        private final UUID playerId;
        private final List<ChatMessage> chatLog = new CopyOnWriteArrayList<>();
        private int inspectionCount = 0;
        private int banCount = 0;
        private long lastInspectionTime = 0;
        private long lastBanTime = 0;
        private final Map<String, Object> metadata = new ConcurrentHashMap<>();
        
        public PlayerStats(UUID playerId) {
            this.playerId = playerId != null ? playerId : UUID.randomUUID();
        }
        
        public PlayerStats(UUID playerId, int inspectionCount, int banCount) {
            this(playerId);
            this.inspectionCount = Math.max(0, inspectionCount);
            this.banCount = Math.max(0, banCount);
        }
        
        // Getters
        public UUID getPlayerId() { return playerId; }
        public UUID getUuid() { return playerId; }
        public int getInspectionCount() { return inspectionCount; }
        public int getInspections() { return inspectionCount; }
        public int getBanCount() { return banCount; }
        public int getBans() { return banCount; }
        public long getLastInspectionTime() { return lastInspectionTime; }
        public long getLastBanTime() { return lastBanTime; }
        public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }
        
        // Setters
        public void setInspectionCount(int count) { this.inspectionCount = Math.max(0, count); }
        public void setBanCount(int count) { this.banCount = Math.max(0, count); }
        public void setLastInspectionTime(long time) { this.lastInspectionTime = Math.max(0, time); }
        public void setLastBanTime(long time) { this.lastBanTime = Math.max(0, time); }
        
        public void incrementInspections() {
            inspectionCount++;
            lastInspectionTime = System.currentTimeMillis();
        }
        
        public void incrementBans() {
            banCount++;
            lastBanTime = System.currentTimeMillis();
        }
        
        public void addChatMessage(String message) {
            if (message != null && !message.trim().isEmpty()) {
                addChatMessage(new ChatMessage(message.trim()));
            }
        }
        
        public void addChatMessage(ChatMessage chatMessage) {
            if (chatMessage != null) {
                chatLog.add(chatMessage);
                cleanupChatLog();
            }
        }
        
        private void cleanupChatLog() {
            long cutoff = System.currentTimeMillis() - CHAT_LOG_MAX_AGE;
            chatLog.removeIf(msg -> msg.getTimestamp() < cutoff);
            
            while (chatLog.size() > MAX_CHAT_LOG_ENTRIES) {
                chatLog.remove(0);
            }
        }
        
        public List<ChatMessage> getChatLogWithTimestamps() { 
            cleanupChatLog();
            return new ArrayList<>(chatLog); 
        }
        
        public List<String> getChatLog() {
            cleanupChatLog();
            return chatLog.stream()
                .map(ChatMessage::getMessage)
                .collect(Collectors.toList());
        }
        
        public void setMetadata(String key, Object value) {
            if (key != null) {
                if (value != null) {
                    metadata.put(key, value);
                } else {
                    metadata.remove(key);
                }
            }
        }
        
        public Object getMetadata(String key) {
            return key != null ? metadata.get(key) : null;
        }

        public static class ChatMessage {
            private final String message;
            private final long timestamp;
            
            public ChatMessage(String message) {
                this(message, System.currentTimeMillis());
            }
            
            public ChatMessage(String message, long timestamp) {
                this.message = message != null ? message : "";
                this.timestamp = timestamp > 0 ? timestamp : System.currentTimeMillis();
            }
            
            public String getMessage() { return message; }
            public long getTimestamp() { return timestamp; }
        }
    }

    /**
     * Loads all reports from the reports file and individual report files
     */
    private void loadReports() {
        reports.clear();
        
        if (reportsFile != null && reportsFile.exists()) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(reportsFile);
                for (String playerIdStr : config.getKeys(false)) {
                    try {
                        UUID playerId = UUID.fromString(playerIdStr);
                        List<InspectionReport> playerReports = new ArrayList<>();
                        ConfigurationSection reportsSection = config.getConfigurationSection(playerIdStr);
                        
                        if (reportsSection != null) {
                            for (String reportIdStr : reportsSection.getKeys(false)) {
                                try {
                                    ConfigurationSection reportSection = reportsSection.getConfigurationSection(reportIdStr);
                                    if (reportSection != null) {
                                        UUID reportId = UUID.fromString(reportIdStr);
                                        UUID reporterId = UUID.fromString(reportSection.getString("reporter"));
                                        UUID targetId = UUID.fromString(reportSection.getString("target"));
                                        String reason = reportSection.getString("reason", "No reason provided");
                                        long timestamp = reportSection.getLong("timestamp", System.currentTimeMillis());
                                        
                                        InspectionReport report = new InspectionReport(reportId, reporterId, targetId, reason, timestamp, new ArrayList<>());
                                        
                                        playerReports.add(report);
                                    }
                                } catch (Exception e) {
                                    plugin.getLogger().warning("Failed to load report " + reportIdStr + ": " + e.getMessage());
                                }
                            }
                            
                            if (!playerReports.isEmpty()) {
                                reports.put(playerId, playerReports);
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in reports: " + playerIdStr);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load reports: " + e.getMessage());
            }
        }
        
        loadReportTemplates();
    }
    
    private void loadReportTemplates() {
        reportTemplates.clear();
        
        Map<String, Object> defaultTemplate = new HashMap<>();
        defaultTemplate.put("name", "Default Report");
        defaultTemplate.put("reason", "Suspicious activity detected");
        reportTemplates.put("default", defaultTemplate);
    }
    
    private void loadPlayerStats() {
        if (playerStatsFile == null || !playerStatsFile.exists()) {
            return;
        }
        
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(playerStatsFile);
            ConfigurationSection playersSection = config.getConfigurationSection("players");
            if (playersSection == null) return;
            
            for (String playerIdStr : playersSection.getKeys(false)) {
                try {
                    UUID playerId = UUID.fromString(playerIdStr);
                    ConfigurationSection playerSection = playersSection.getConfigurationSection(playerIdStr);
                    if (playerSection != null) {
                        PlayerStats stats = new PlayerStats(playerId);
                        stats.setInspectionCount(playerSection.getInt("inspectionCount", 0));
                        stats.setBanCount(playerSection.getInt("banCount", 0));
                        stats.setLastInspectionTime(playerSection.getLong("lastInspectionTime", 0));
                        stats.setLastBanTime(playerSection.getLong("lastBanTime", 0));
                        
                        playerStats.put(playerId, stats);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in player stats: " + playerIdStr);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load player stats: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveReport(InspectionReport report, String templateName) {
        File reportFile = new File(reportsFolder, report.getReportId() + ".yml");
        YamlConfiguration reportConfig = new YamlConfiguration();
        
        reportConfig.set("report_id", report.getReportId().toString());
        reportConfig.set("inspector_id", report.getInspectorId().toString());
        reportConfig.set("target_id", report.getTargetId().toString());
        reportConfig.set("reason", report.getReason());
        reportConfig.set("evidence", report.getEvidence());
        reportConfig.set("evidence_ids", report.getEvidenceIds().stream().map(UUID::toString).toList());
        reportConfig.set("timestamp", report.getTimestamp());
        reportConfig.set("status", report.getStatus().name());
        reportConfig.set("resolved", report.isResolved());
        if (report.getResolvedBy() != null) {
            reportConfig.set("resolved_by", report.getResolvedBy().toString());
        }
        if (report.getResolvedAt() > 0) {
            reportConfig.set("resolved_at", report.getResolvedAt());
        }
        if (templateName != null && !templateName.isEmpty()) {
            reportConfig.set("template", templateName);
        }

        try {
            reportConfig.save(reportFile);
        } catch (IOException e) {
            this.plugin.getLogger().severe("Failed to save report: " + e.getMessage());
        }
    }

    public List<InspectionReport> getReports(UUID inspectorId) {
        return reports.getOrDefault(inspectorId, new ArrayList<>());
    }

    public String getReportTemplate(String templateName) {
        Map<String, Object> template = reportTemplates.get(templateName);
        if (template != null) {
            return template.toString();
        }
        return "Default template not found";
    }

    public void setReportResolved(UUID reportId, boolean resolved) {
        for (List<InspectionReport> reportList : reports.values()) {
            for (InspectionReport report : reportList) {
                if (report.getReportId().equals(reportId)) {
                    report.setResolved(resolved);
                    if (resolved) {
                        report.setResolvedBy(null);
                    }
                    saveReport(report, null);
                    break;
                }
            }
        }
    }

    private void updateModeratorStats(UUID moderatorId, boolean isBan) {
        if (moderatorId == null) return;
        
        PlayerStats stats = moderatorStats.computeIfAbsent(moderatorId, PlayerStats::new);
        stats.incrementInspections();
        
        if (isBan) {
            stats.incrementBans();
        }
        
        savePlayerStats();
    }
    
    private void updatePlayerStats(UUID playerId) {
        if (playerId == null) return;
        
        PlayerStats stats = playerStats.computeIfAbsent(playerId, PlayerStats::new);
        stats.incrementInspections();
        savePlayerStats();
    }

    private void savePlayerStats() {
        try {
            File dataFile = new File(plugin.getDataFolder(), "inspections.yml");
            YamlConfiguration data = new YamlConfiguration();

            for (PlayerStats stats : playerStats.values()) {
                data.set("players." + stats.getUuid() + ".inspections", stats.getInspections());
                data.set("players." + stats.getUuid() + ".bans", stats.getBans());
            }

            for (PlayerStats stats : moderatorStats.values()) {
                data.set("moderators." + stats.getUuid() + ".inspections", stats.getInspections());
                data.set("moderators." + stats.getUuid() + ".bans", stats.getBans());
            }

            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save inspection data: " + e.getMessage());
        }
    }
}