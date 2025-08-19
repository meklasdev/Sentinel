package com.wificraft.sentinel.modules.logging;

import com.wificraft.sentinel.modules.config.NotificationConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.wificraft.sentinel.WiFiCraftSentinel;
import com.wificraft.sentinel.alerts.DiscordAlertSystem;
import com.wificraft.sentinel.alerts.DiscordAlertSystem.SeverityLevel;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.TimeUnit;

public class HistoryLogger {
    private static final Logger logger = Logger.getLogger("SentinelLogger");
    private final File logFile;
    private final SimpleDateFormat dateFormat;
    private final SimpleDateFormat timeFormat;
    private final Map<String, Integer> activityCounters;
    private final NotificationConfig config;
    private final Map<String, List<String>> suspiciousPatterns;
    private final int logRetentionDays;
    private final File dataFolder;
    
    public HistoryLogger(File dataFolder, NotificationConfig config, int logRetentionDays) {
        this.dataFolder = dataFolder;
        this.config = config;
        this.logRetentionDays = logRetentionDays;
        this.logFile = new File(dataFolder, "sentinel.log");
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        this.timeFormat = new SimpleDateFormat("HH:mm:ss");
        this.activityCounters = new ConcurrentHashMap<>();
        this.suspiciousPatterns = new ConcurrentHashMap<>();
        
        loadSuspiciousPatterns();
        setupFileLogging();
        
        // Start periodic logging task
        new BukkitRunnable() {
            @Override
            public void run() {
                logActivityStats();
                // Check suspicious patterns for online players
                for (Player player : Bukkit.getOnlinePlayers()) {
                    checkSuspiciousPatterns(player, "");
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugins()[0], 20L * 60L, 20L * 60L); // Every minute

        // Schedule log cleanup task (e.g., daily)
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanupOldLogs();
            }
        }.runTaskTimerAsynchronously(Bukkit.getPluginManager().getPlugins()[0], 20L * 60L * 60L * 24L, 20L * 60L * 60L * 24L); // Every 24 hours
    }
    
    private void loadSuspiciousPatterns() {
        String[] patterns = config.getSuspiciousPatterns();
        for (String pattern : patterns) {
            suspiciousPatterns.put(pattern, new ArrayList<>());
        }
    }
    
    private void setupFileLogging() {
        try {
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            
            logger.addHandler(new java.util.logging.FileHandler(logFile.getAbsolutePath(), true));
            logger.setUseParentHandlers(false);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Cannot create log file", e);
        }
    }
    
    public void logInspection(Player target, Player moderator, String result, long duration) {
        String logMessage = String.format(
            "[INSPECTION] Target: %s, Moderator: %s, Result: %s, Duration: %d seconds",
            target.getName(), moderator.getName(), result, duration
        );
        
        logger.log(Level.INFO, logMessage);
        incrementCounter("inspections");
        
        // Check for suspicious patterns
        checkSuspiciousPatterns(target, logMessage);
    }
    
    public void logLogin(Player player) {
        String logMessage = String.format(
            "[LOGIN] Player: %s logged in",
            player.getName()
        );
        
        logger.log(Level.INFO, logMessage);
        incrementCounter("logins");
        
        // Check for suspicious patterns
        checkSuspiciousPatterns(player, logMessage);
    }
    
    public void logBan(Player player, Player moderator, String reason, long duration) {
        String logMessage = String.format(
            "[BAN] Player: %s, Moderator: %s, Reason: %s, Duration: %d seconds",
            player.getName(), moderator.getName(), reason, duration
        );
        
        logger.log(Level.WARNING, logMessage);
        incrementCounter("bans");
        
        // Check for suspicious patterns
        checkSuspiciousPatterns(player, logMessage);
    }
    
    public void logSuspiciousActivity(Player player, String activity, String details) {
        String logMessage = String.format("[%s] %s - %s: %s", 
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),
            player.getName(),
            activity,
            details
        );
        
        // Log to console
        logger.warning(logMessage);
        
        // Send to Discord
        WiFiCraftSentinel plugin = (WiFiCraftSentinel) Bukkit.getPluginManager().getPlugin("Sentinel");
        if (plugin != null) {
            DiscordAlertSystem alertSystem = new DiscordAlertSystem(plugin);
            alertSystem.sendAlert("Suspicious Activity", logMessage, SeverityLevel.HIGH);
        }
        
        // Log to file
        logToFile(logMessage);
    }
    
    public void viewHistory(Player viewer, Player target) {
        // Open a GUI or send chat messages with the target's history
        // This is a placeholder - implement actual history viewing logic here
        viewer.sendMessage("§7Wyświetlanie historii gracza " + target.getName());
        
        // Example of how you might implement this:
        // 1. Get the log file for the target player
        // 2. Read the last N entries
        // 3. Display them in a GUI or send as chat messages
        
        // For now, just send a message
        viewer.sendMessage("§7Funkcja podglądu historii jest obecnie w budowie.");
    }
    
    private void incrementCounter(String activity) {
        activityCounters.merge(activity, 1, Integer::sum);
        
        // Check threshold and send notification if exceeded
        int count = activityCounters.get(activity);
        int threshold = config.getActivityThreshold(activity);
        
        if (count >= threshold) {
            String alertMessage = String.format(
                "Threshold exceeded for %s: %d/%d",
                activity, count, threshold
            );
            logger.log(Level.WARNING, alertMessage);
            // Get plugin instance and send alert
            WiFiCraftSentinel plugin = (WiFiCraftSentinel) Bukkit.getPluginManager().getPlugin("Sentinel");
            if (plugin != null) {
                DiscordAlertSystem alertSystem = new DiscordAlertSystem(plugin);
                alertSystem.sendAlert("Threshold Alert", alertMessage, SeverityLevel.WARNING);
            }
            activityCounters.put(activity, 0); // Reset counter
        }
    }
    
    private void checkSuspiciousPatterns(Player player, String message) {
        for (Map.Entry<String, List<String>> entry : suspiciousPatterns.entrySet()) {
            String pattern = entry.getKey();
            if (message.toLowerCase().contains(pattern.toLowerCase())) {
                entry.getValue().add(message);
                
                // Check if pattern count exceeds threshold
                int count = entry.getValue().size();
                int threshold = config.getSuspiciousThreshold(pattern);
                
                if (count >= threshold) {
                    String alertMessage = String.format(
                        "Suspicious pattern detected: %s\n" +
                        "Pattern: %s\n" +
                        "Occurrences: %d/%d\n" +
                        "Last occurrences: %s",
                        player.getName(),
                        pattern,
                        count,
                        threshold,
                        String.join("\n", entry.getValue())
                    );
                    
                    logger.log(Level.SEVERE, alertMessage);
                    // Get plugin instance and send alert
                    WiFiCraftSentinel plugin = (WiFiCraftSentinel) Bukkit.getPluginManager().getPlugin("Sentinel");
                    if (plugin != null) {
                        DiscordAlertSystem alertSystem = new DiscordAlertSystem(plugin);
                        alertSystem.sendAlert("Suspicious Pattern Detected", alertMessage, SeverityLevel.HIGH);
                    }
                    entry.getValue().clear(); // Reset pattern counter
                }
            }
        }
    }
    
    private void logActivityStats() {
        StringBuilder stats = new StringBuilder("[ACTIVITY_STATS] ");
        stats.append("Date: ").append(dateFormat.format(new Date())).append(" ");
        stats.append("Time: ").append(timeFormat.format(new Date())).append("\n");
        
        for (Map.Entry<String, Integer> entry : activityCounters.entrySet()) {
            stats.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        
        logger.log(Level.INFO, stats.toString());
    }
    
    public void cleanupOldLogs() {
        if (logRetentionDays <= 0) {
            return; // Retention disabled
        }

        File[] logFiles = dataFolder.listFiles((dir, name) -> name.startsWith("sentinel.log"));

        if (logFiles == null) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        long retentionMillis = TimeUnit.DAYS.toMillis(logRetentionDays);

        for (File file : logFiles) {
            try {
                BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                long fileTime = attrs.lastModifiedTime().toMillis();
                if ((currentTime - fileTime) > retentionMillis) {
                    if (file.delete()) {
                        logger.info("Deleted old log file: " + file.getName());
                    } else {
                        logger.warning("Failed to delete old log file: " + file.getName());
                    }
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error checking or deleting old log file: " + file.getName(), e);
            }
        }
    }

    public int getActivityCount(String activity) {
        return activityCounters.getOrDefault(activity, 0);
    }

    public void applyFilter(Player player, String option, String value) {
        // TODO: Implement actual log filtering logic based on option and value
        logger.info(String.format("[HistoryLogger] Apply filter called by %s: Option=%s, Value=%s. (Not yet implemented)", player.getName(), option, value));
        // For now, just sends a message to the player or logs it
        if (player != null) {
            player.sendMessage("§e[Sentinel] Log filtering for '" + option + "' with value '" + value + "' is not fully implemented yet.");
        }
    }

    public void exportHistory(Player player, String format) {
        // TODO: Implement actual log exporting logic based on format (e.g., TXT, CSV)
        logger.info(String.format("[HistoryLogger] Export history called by %s: Format=%s. (Not yet implemented)", player.getName(), format));
        // For now, just sends a message to the player or logs it
        if (player != null) {
            player.sendMessage("§e[Sentinel] Log exporting in '" + format + "' format is not fully implemented yet.");
        }
    }

    public void shutdown() {
        logger.info("Shutting down logging system");
        activityCounters.clear();
        suspiciousPatterns.clear();
    }
}
