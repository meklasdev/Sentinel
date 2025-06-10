package com.wificraft.sentinel.modules;

import com.wificraft.sentinel.SentinelPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.sql.Timestamp;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.Inventory;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import java.util.concurrent.TimeUnit;

public class InspectionModule implements Listener {
    private final SentinelPlugin plugin;
    private final ModeratorRanking moderatorRanking;
    private final Map<UUID, List<InspectionReport>> reports = new HashMap<>();
    private final Map<UUID, List<Evidence>> evidenceMap = new HashMap<>();
    private final Map<String, String> reportTemplates = new HashMap<>();
    private final File reportsFolder;
    private final Map<UUID, PlayerStats> playerStats = new HashMap<>();
    private Map<UUID, ModeratorStats> moderatorStats = new HashMap<>();

    public InspectionModule(SentinelPlugin plugin, ModeratorRanking moderatorRanking) {
        this.plugin = plugin;
        this.moderatorRanking = moderatorRanking;
        this.reportsFolder = new File(plugin.getDataFolder(), "reports");
        if (!reportsFolder.exists()) {
            reportsFolder.mkdirs();
        }
        loadReportTemplates();
    }

    public void cleanupOldInspections() {
        long retentionPeriod = TimeUnit.DAYS.toMillis(plugin.getConfig().getInt("inspection.retention-days", 30));
        long currentTime = System.currentTimeMillis();

        reports.values().forEach(reportList -> reportList.removeIf(report -> {
            if (currentTime - report.getTimestamp() > retentionPeriod) {
                File reportFile = new File(reportsFolder, report.getReportId() + ".yml");
                if (reportFile.exists()) {
                    reportFile.delete();
                }
                return true;
            }
            return false;
        }));

        plugin.getLogger().info("Cleanup of old inspection reports completed.");
    }

    private void loadReportTemplates() {
        File templatesFile = new File(plugin.getDataFolder(), "report_templates.yml");
        if (!templatesFile.exists()) {
            plugin.saveResource("report_templates.yml", false);
        }
        YamlConfiguration templates = YamlConfiguration.loadConfiguration(templatesFile);
        ConfigurationSection templatesSection = templates.getConfigurationSection("templates");
        if (templatesSection != null) {
            for (String templateName : templatesSection.getKeys(false)) {
                reportTemplates.put(templateName, templates.getString("templates." + templateName));
            }
        }
    }

    public void createReport(UUID inspectorId, UUID targetId, String reason, List<String> evidence, String templateName) {
        InspectionReport report = new InspectionReport(inspectorId, targetId, reason, evidence);
        reports.computeIfAbsent(inspectorId, k -> new ArrayList<>()).add(report);
        saveReport(report, templateName);
        moderatorRanking.updateModeratorStats(inspectorId, false);
        // No need to call updateLastInspection separately as it's handled in updateModeratorStats
        updatePlayerStats(targetId);
    }

    private void saveReport(InspectionReport report, String templateName) {
        File reportFile = new File(reportsFolder, report.getReportId() + ".yml");
        YamlConfiguration reportConfig = new YamlConfiguration();
        
        // Basic report data
        reportConfig.set("report_id", report.getReportId().toString());
        reportConfig.set("inspector_id", report.getInspectorId().toString());
        reportConfig.set("target_id", report.getTargetId().toString());
        reportConfig.set("reason", report.getReason());
        reportConfig.set("evidence", report.getEvidence());
        reportConfig.set("timestamp", report.getTimestamp());
        reportConfig.set("resolved", report.isResolved());
        reportConfig.set("template", templateName);

        // Save to file
        try {
            reportConfig.save(reportFile);
        } catch (IOException e) {
            this.plugin.getLogger().severe("Failed to save report: " + e.getMessage());
        }
    }

    public List<InspectionReport> getReports(UUID inspectorId) {
        return reports.getOrDefault(inspectorId, Collections.emptyList());
    }

    public String getReportTemplate(String templateName) {
        return reportTemplates.getOrDefault(templateName, "Default template not found");
    }

    public void setReportResolved(UUID reportId, boolean resolved) {
        for (List<InspectionReport> reportList : reports.values()) {
            for (InspectionReport report : reportList) {
                if (report.getReportId().equals(reportId)) {
                    report.setResolved(resolved);
                    saveReport(report, ""); // Save with empty template to keep existing template
                    break;
                }
            }
        }
    }

    public class InspectionReport {
        private final UUID reportId;
        private final UUID inspectorId;
        private final UUID targetId;
        private final String reason;
        private final List<String> evidence;
        private final long timestamp;
        private boolean isResolved;

        public InspectionReport(UUID inspectorId, UUID targetId, String reason, List<String> evidence) {
            this.reportId = UUID.randomUUID();
            this.inspectorId = inspectorId;
            this.targetId = targetId;
            this.reason = reason;
            this.evidence = evidence;
            this.timestamp = System.currentTimeMillis();
            this.isResolved = false;
        }

        public UUID getReportId() { return reportId; }
        public UUID getInspectorId() { return inspectorId; }
        public UUID getTargetId() { return targetId; }
        public String getReason() { return reason; }
        public List<String> getEvidence() { return Collections.unmodifiableList(evidence); }
        public long getTimestamp() { return timestamp; }
        public boolean isResolved() { return isResolved; }
        public void setResolved(boolean resolved) { this.isResolved = resolved; }
    }

    public class Evidence {
        private final UUID playerId;
        private final String type;
        private final String description;
        private final long timestamp;

        public Evidence(UUID playerId, String type, String description) {
            this.playerId = playerId;
            this.type = type;
            this.description = description;
            this.timestamp = System.currentTimeMillis();
        }

        public UUID getPlayerId() { return playerId; }
        public String getType() { return type; }
        public String getDescription() { return description; }

        public long getTimestamp() { return timestamp; }
    }

    public void updatePlayerStats(UUID playerId) {
        PlayerStats stats = playerStats.computeIfAbsent(playerId, uuid -> new PlayerStats(uuid));
        stats.incrementInspections();
    }

    public int getPlayerInspectionCount(UUID playerId) {
        PlayerStats stats = playerStats.get(playerId);
        return (stats != null) ? stats.getInspections() : 0;
    }

    public class PlayerStats {
        private final UUID uuid;
        private int inspections;
        private int bans;
        private final List<ChatLogEntry> chatLog = new ArrayList<>();

        public int getInspections() {
            return inspections;
        }

        public PlayerStats(UUID uuid) {
            this.uuid = uuid;
            this.inspections = 0;
            this.bans = 0;
        }

        public PlayerStats(UUID uuid, int inspections, int bans) {
            this.uuid = uuid;
            this.inspections = inspections;
            this.bans = bans;
        }

        public UUID getUuid() {
            return uuid;
        }

        public int getInspections() {
            return inspections;
        }

        public void setInspections(int inspections) {
            this.inspections = inspections;
        }

        public void incrementInspections() {
            inspections++;
        }

        public int getBans() {
            return bans;
        }

        public void setBans(int bans) {
            this.bans = bans;
        }

        public void incrementBans() {
            bans++;
        }

        public List<ChatLogEntry> getChatLog() {
            return chatLog;
        }

        public void addChatEntry(String message, long timestamp) {
            chatLog.add(new ChatLogEntry(message, timestamp));
        }

        public static class ChatLogEntry {
            private final String message;
            private final long timestamp;

            public ChatLogEntry(String message, long timestamp) {
                this.message = message;
                this.timestamp = timestamp;
            }

            public String getMessage() {
                return message;
            }

            public long getTimestamp() {
                return timestamp;
            }
        }
    }

    public void initialize() {
        initializePlayerStats();
        initializeModeratorRanking();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void disable() {
        // Save all reports and cleanup
        saveAllReports();
    }
    
    public int getActiveInspectionCount() {
        // Return the number of active inspections
        // This is a simplified implementation
        return (int) evidenceMap.values().stream()
            .flatMap(List::stream)
            .filter(evidence -> !evidence.isResolved())
            .count();
    }
    
    public void startInspection(Player inspector, Player target) {
        // Start an inspection
        // This is a placeholder - implement actual inspection logic here
        inspector.sendMessage("§7Rozpoczęto inspekcję gracza " + target.getName());
    }
    
    public void addInspectionNote(Player inspector, Player target, String note) {
        // Add a note to an inspection
        // This is a placeholder - implement actual note adding logic here
        inspector.sendMessage("§7Dodano notatkę do inspekcji gracza " + target.getName() + ": §f" + note);
    }
    
    public void endInspection(Player inspector, Player target, String reason) {
        // End an inspection
        // This is a placeholder - implement actual inspection ending logic here
        inspector.sendMessage("§7Zakończono inspekcję gracza " + target.getName() + " z powodem: §f" + reason);
    }
    
    public void openInspectionGUI(Player inspector, Player target) {
        // Open the inspection GUI
        // This is a placeholder - implement actual GUI opening logic here
        inspector.sendMessage("§7Otwieranie interfejsu inspekcji dla gracza " + target.getName());
    }
    
    public void openModeratorStatsGUI(Player player) {
        // Open moderator stats GUI
        // This is a placeholder - implement actual stats GUI logic here
        player.sendMessage("§7Otwieranie statystyk moderatora");
    }

    public void onEnable() {
        initialize();
    }

    public void onDisable() {
        disable();
    }

    private void initializeModeratorRanking() {
        if (this.moderatorRanking != null) {
            this.moderatorStats = moderatorRanking.getModeratorStats();
        } else {
            this.moderatorStats = new HashMap<>();
        }
    }

    private void initializePlayerStats() {
        File playerStatsFile = new File(this.plugin.getDataFolder(), "player_stats.yml");
        if (playerStatsFile.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(playerStatsFile);
            for (String uuidStr : config.getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                PlayerStats stats = new PlayerStats(uuid, 
                    config.getInt(uuidStr + ".inspections", 0),
                    config.getInt(uuidStr + ".bans", 0)
                );
                this.playerStats.put(uuid, stats);
            }
        }
    }

    private void savePlayerStats() {
        File playerStatsFile = new File(this.plugin.getDataFolder(), "player_stats.yml");
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerStats> entry : this.playerStats.entrySet()) {
            UUID uuid = entry.getKey();
            PlayerStats stats = entry.getValue();
            config.set(uuid.toString() + ".inspections", stats.getInspections());
            config.set(uuid.toString() + ".bans", stats.getBans());
        }
        try {
            config.save(playerStatsFile);
        } catch (IOException e) {
            this.plugin.getLogger().severe("Failed to save player stats: " + e.getMessage());
        }
    }

    private void saveModeratorStats() {
        File moderatorStatsFile = new File(this.plugin.getDataFolder(), "moderator_stats.yml");
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, ModeratorStats> entry : this.moderatorStats.entrySet()) {
            UUID uuid = entry.getKey();
            ModeratorStats stats = entry.getValue();
            config.set(uuid.toString() + ".inspections", stats.getInspections());
            config.set(uuid.toString() + ".bans", stats.getBans());
            config.set(uuid.toString() + ".lastInspection", stats.getLastInspection());
        }
        try {
            config.save(moderatorStatsFile);
        } catch (IOException e) {
            this.plugin.getLogger().severe("Failed to save moderator stats: " + e.getMessage());
        }
    }

    private ItemStack createStatItem(Material material, String name, String value) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            meta.lore(List.of(Component.text(value)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createButton(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name).color(NamedTextColor.YELLOW));
            meta.lore(List.of(Component.text(lore).color(NamedTextColor.GRAY)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void updateModeratorStats(UUID moderatorId, boolean isBan) {
        ModeratorStats stats = moderatorStats.computeIfAbsent(moderatorId, 
            uuid -> new ModeratorStats(moderatorId));
        
        if (isBan) {
            stats.incrementBans();
        } else {
            stats.incrementInspections();
        }
        
        saveInspectionData();
    }

    private void loadInspectionData() {
        try {
            File dataFile = new File(plugin.getDataFolder(), "inspections.yml");
            if (!dataFile.exists()) {
                dataFile.createNewFile();
                return;
            }

            YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
            ConfigurationSection playersSection = data.getConfigurationSection("players");
            ConfigurationSection moderatorsSection = data.getConfigurationSection("moderators");
            
            if (playersSection != null) {
                for (String uuid : playersSection.getKeys(false)) {
                    try {
                        UUID playerId = UUID.fromString(uuid);
                        int inspections = playersSection.getInt(uuid + ".inspections");
                        int bans = playersSection.getInt(uuid + ".bans");
                        playerStats.put(playerId, new PlayerStats(playerId, inspections, bans));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Nieprawidłowy UUID w danych inspekcji: " + uuid);
                    }
                }
            }

            if (moderatorsSection != null) {
                for (String uuid : moderatorsSection.getKeys(false)) {
                    try {
                        UUID moderatorId = UUID.fromString(uuid);
                        int inspections = moderatorsSection.getInt(uuid + ".inspections");
                        int bans = moderatorsSection.getInt(uuid + ".bans");
                        moderatorStats.put(moderatorId, new ModeratorStats(moderatorId, inspections, bans));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Nieprawidłowy UUID w danych moderatorów: " + uuid);
                    }
                }
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Błąd podczas wczytywania danych inspekcji: " + e.getMessage());
        }
    }

    private void saveInspectionData() {
        try {
            File dataFile = new File(plugin.getDataFolder(), "inspections.yml");
            YamlConfiguration data = new YamlConfiguration();

            for (PlayerStats stats : playerStats.values()) {
                data.set("players." + stats.getUuid() + ".inspections", stats.getInspections());
                data.set("players." + stats.getUuid() + ".bans", stats.getBans());
            }

            for (ModeratorStats stats : moderatorStats.values()) {
                data.set("moderators." + stats.getUuid() + ".inspections", stats.getInspections());
                data.set("moderators." + stats.getUuid() + ".bans", stats.getBans());
            }

            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Błąd podczas zapisu danych inspekcji: " + e.getMessage());
        }
    }

    public class ModeratorStats {
        private final UUID uuid;
        private int inspections;
        private int bans;
        private long lastInspection;

        public ModeratorStats(UUID uuid) {
            this.uuid = uuid;
            this.inspections = 0;
            this.bans = 0;
            this.lastInspection = 0;
        }

        public ModeratorStats(UUID uuid, int inspections, int bans) {
            this.uuid = uuid;
            this.inspections = inspections;
            this.bans = bans;
            this.lastInspection = 0;
        }

        public UUID getUuid() {
            return uuid;
        }

        public int getInspections() {
            return inspections;
        }

        public void setInspections(int inspections) {
            this.inspections = inspections;
        }

        public void incrementInspections() {
            inspections++;
        }

        public int getBans() {
            return bans;
        }

        public void setBans(int bans) {
            this.bans = bans;
        }

        public void incrementBans() {
            bans++;
        }

        public double getEffectiveness() {
            if (inspections == 0) return 0;
            return (double) bans / inspections;
        }

        public long getLastInspection() {
            return lastInspection;
        }

        public void updateLastInspection() {
            this.lastInspection = System.currentTimeMillis();
        }
    }
}
