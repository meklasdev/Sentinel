package com.wificraft.sentinel.modules.gui;

import com.wificraft.sentinel.modules.config.NotificationConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
import java.util.regex.Pattern;

public class NotificationManager {
    private final NotificationConfig config;
    private final Map<UUID, Long> lastInspection = new HashMap<>();
    private final Map<UUID, Integer> dailyInspections = new HashMap<>();
    private final Map<UUID, Integer> hourlyInspections = new HashMap<>();
    private final Map<UUID, Long> lastLogin = new HashMap<>();
    private final Map<UUID, Integer> hourlyLogins = new HashMap<>();
    private final Map<UUID, Integer> dailyLogins = new HashMap<>();
    private final Map<UUID, Long> lastActivity = new HashMap<>();
    private final Set<Pattern> suspiciousPatterns;
    
    public NotificationManager(NotificationConfig config) {
        this.config = config;
        this.suspiciousPatterns = new HashSet<>();
        
        // Load suspicious patterns
        String[] patterns = config.getSuspiciousPatterns();
        for (String pattern : patterns) {
            suspiciousPatterns.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
        }
        
        // Start periodic checks
        startPeriodicChecks();
    }
    
    private void startPeriodicChecks() {
        new BukkitRunnable() {
            @Override
            public void run() {
                checkInspectionThresholds();
                checkActivityThresholds();
                checkSuspiciousPatterns();
                
                // Reset counters
                resetHourlyCounters();
                resetActivityTracking();
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugins()[0], 20 * 60, 20 * 60); // Every minute
    }
    
    public void recordInspection(Player target, Player moderator) {
        UUID uuid = target.getUniqueId();
        
        // Update inspection counters
        lastInspection.put(uuid, System.currentTimeMillis());
        hourlyInspections.merge(uuid, 1, Integer::sum);
        dailyInspections.merge(uuid, 1, Integer::sum);
        
        // Check thresholds immediately
        checkInspectionThresholds(uuid);
    }
    
    private void checkInspectionThresholds() {
        Map<String, Integer> thresholds = getInspectionThresholds();
        
        for (Map.Entry<UUID, Integer> entry : hourlyInspections.entrySet()) {
            if (entry.getValue() >= thresholds.get("hourly")) {
                sendNotification(entry.getKey(), "hourly", "inspections");
            }
        }
        
        for (Map.Entry<UUID, Integer> entry : dailyInspections.entrySet()) {
            if (entry.getValue() >= thresholds.get("daily")) {
                sendNotification(entry.getKey(), "daily", "inspections");
            }
        }
    }
    
    private void checkInspectionThresholds(UUID uuid) {
        Map<String, Integer> thresholds = getInspectionThresholds();
        
        if (hourlyInspections.getOrDefault(uuid, 0) >= thresholds.get("hourly")) {
            sendNotification(uuid, "hourly", "inspections");
        }
        
        if (dailyInspections.getOrDefault(uuid, 0) >= thresholds.get("daily")) {
            sendNotification(uuid, "daily", "inspections");
        }
    }
    
    public void recordLogin(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Update login counters
        lastLogin.put(uuid, System.currentTimeMillis());
        hourlyLogins.merge(uuid, 1, Integer::sum);
        dailyLogins.merge(uuid, 1, Integer::sum);
        
        // Reset activity timer on login
        lastActivity.put(uuid, System.currentTimeMillis());
        
        // Check thresholds immediately
        checkActivityThresholds(uuid);
    }
    
    private void checkActivityThresholds() {
        Map<String, Integer> thresholds = getActivityThresholds();
        
        for (Map.Entry<UUID, Integer> entry : hourlyLogins.entrySet()) {
            if (entry.getValue() >= thresholds.get("logins.hourly")) {
                sendNotification(entry.getKey(), "hourly", "logins");
            }
        }
        
        for (Map.Entry<UUID, Integer> entry : dailyLogins.entrySet()) {
            if (entry.getValue() >= thresholds.get("logins.daily")) {
                sendNotification(entry.getKey(), "daily", "logins");
            }
        }
        
        // Check idle time
        long currentTime = System.currentTimeMillis();
        for (Map.Entry<UUID, Long> entry : lastActivity.entrySet()) {
            if (currentTime - entry.getValue() >= thresholds.get("idle") * 1000) {
                sendNotification(entry.getKey(), "idle", "activity");
            }
        }
    }
    
    private void checkActivityThresholds(UUID uuid) {
        Map<String, Integer> thresholds = getActivityThresholds();
        
        if (hourlyLogins.getOrDefault(uuid, 0) >= thresholds.get("logins.hourly")) {
            sendNotification(uuid, "hourly", "logins");
        }
        
        if (dailyLogins.getOrDefault(uuid, 0) >= thresholds.get("logins.daily")) {
            sendNotification(uuid, "daily", "logins");
        }
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastActivity.getOrDefault(uuid, currentTime) >= thresholds.get("idle") * 1000) {
            sendNotification(uuid, "idle", "activity");
        }
    }
    
    private void checkSuspiciousPatterns() {
        // This feature is temporarily disabled
        // It will be reimplemented with a proper chat event listener
    }
    
    private void sendNotification(UUID uuid, String type, String category) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;
        
        String message = String.format(
            "§cALERT: %s przekroczył próg %s dla %s!",
            player.getName(),
            type,
            category
        );
        
        // Send to all online moderators
        for (Player moderator : Bukkit.getOnlinePlayers()) {
            if (moderator.hasPermission("sentinel.moderator")) {
                moderator.sendMessage(message);
            }
        }
    }
    
    private void resetHourlyCounters() {
        hourlyInspections.clear();
        hourlyLogins.clear();
    }
    
    private void resetActivityTracking() {
        lastActivity.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            lastActivity.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }
    
    public Map<String, Integer> getInspectionThresholds() {
        Map<String, Integer> thresholds = new HashMap<>();
        thresholds.put("hourly", 3);  // Default values
        thresholds.put("daily", 10);
        return thresholds;
    }
    
    public Map<String, Integer> getActivityThresholds() {
        Map<String, Integer> thresholds = new HashMap<>();
        thresholds.put("idle", 1800);  // 30 minutes
        thresholds.put("logins.hourly", 5);
        thresholds.put("logins.daily", 20);
        return thresholds;
    }
    
    public String[] getSuspiciousPatterns() {
        return new String[0];  // Empty array for now
    }
    
    public void addSuspiciousPattern(String pattern) {
        suspiciousPatterns.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
    }
    
    public void removeSuspiciousPattern(String pattern) {
        suspiciousPatterns.remove(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
    }
}
