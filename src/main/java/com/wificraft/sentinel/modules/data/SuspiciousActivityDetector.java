package com.wificraft.sentinel.modules.data;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import com.wificraft.sentinel.modules.config.InspectionConfig;

public class SuspiciousActivityDetector {
    private final InspectionConfig config;
    private final Map<UUID, Long> lastLoginTimes;
    private final Map<UUID, Integer> loginCounts;
    private final Map<UUID, Long> lastMovementTimes;
    private final Map<UUID, Long> inspectionStartTimes;
    
    public SuspiciousActivityDetector(InspectionConfig config) {
        this.config = config;
        this.lastLoginTimes = new HashMap<>();
        this.loginCounts = new HashMap<>();
        this.lastMovementTimes = new HashMap<>();
        this.inspectionStartTimes = new HashMap<>();
        
        // Start monitoring tasks
        startMonitoringTasks();
    }
    
    private void startMonitoringTasks() {
        // Check for suspicious activity every minute
        new BukkitRunnable() {
            @Override
            public void run() {
                checkForSuspiciousActivity();
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("SentinelPlugin"), 20 * 60, 20 * 60);
    }
    
    public void onPlayerLogin(Player player) {
        UUID uuid = player.getUniqueId();
        lastLoginTimes.put(uuid, System.currentTimeMillis());
        
        // Reset login count for this hour
        if (!loginCounts.containsKey(uuid)) {
            loginCounts.put(uuid, 1);
        } else {
            loginCounts.put(uuid, loginCounts.get(uuid) + 1);
        }
    }
    
    public void onPlayerLogout(Player player) {
        UUID uuid = player.getUniqueId();
        lastLoginTimes.put(uuid, System.currentTimeMillis());
        loginCounts.remove(uuid);
    }
    
    public void onPlayerMovement(Player player) {
        lastMovementTimes.put(player.getUniqueId(), System.currentTimeMillis());
    }
    
    public void onInspectionStart(Player player) {
        inspectionStartTimes.put(player.getUniqueId(), System.currentTimeMillis());
    }
    
    public void onInspectionEnd(Player player) {
        inspectionStartTimes.remove(player.getUniqueId());
    }
    
    public boolean isSuspiciousActivity(UUID playerId, int logins, long sessionDuration, 
                                      long idleTime, int inspections, int bans) {
        // Check login frequency (more than 5 logins per hour)
        if (logins > 5) {
            return true;
        }
        
        // Check session duration (more than 12 hours)
        if (sessionDuration > TimeUnit.HOURS.toMillis(12)) {
            return true;
        }
        
        // Check idle time (more than 30 minutes)
        if (idleTime > TimeUnit.MINUTES.toMillis(30)) {
            return true;
        }
        
        // Check inspection count (more than 5 inspections)
        if (inspections > 5) {
            return true;
        }
        
        // Check ban count (more than 3 bans)
        if (bans > 3) {
            return true;
        }
        
        return false;
    }
    
    private void checkForSuspiciousActivity() {
        long currentTime = System.currentTimeMillis();
        
        // Check all online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            
            // Check login frequency
            if (loginCounts.containsKey(uuid)) {
                int loginCount = loginCounts.get(uuid);
                long lastLogin = lastLoginTimes.get(uuid);
                long timeSinceLogin = currentTime - lastLogin;
                
                if (loginCount > config.getMaxLoginsPerHour() && 
                    timeSinceLogin < TimeUnit.HOURS.toMillis(1)) {
                    warnPlayer(player, "Zbyt częste logowanie!");
                }
            }
            
            // Check idle time
            if (lastMovementTimes.containsKey(uuid)) {
                long idleTime = currentTime - lastMovementTimes.get(uuid);
                if (idleTime > TimeUnit.MINUTES.toMillis(config.getMaxIdleTimeMinutes())) {
                    warnPlayer(player, "Długa nieaktywność!");
                }
            }
            
            // Check inspection duration
            if (inspectionStartTimes.containsKey(uuid)) {
                long inspectionTime = currentTime - inspectionStartTimes.get(uuid);
                if (inspectionTime > TimeUnit.MINUTES.toMillis(config.getMaxInspectionDurationMinutes())) {
                    warnPlayer(player, "Długa inspekcja!");
                }
            }
        }
    }
    
    private void warnPlayer(Player player, String reason) {
        player.sendMessage("§c[OSTRZEŻENIE] " + reason);
        
        // Send to Discord if enabled
        if (config.isDiscordNotificationsEnabled()) {
            String channelId = config.getDiscordChannelId();
            if (!channelId.isEmpty()) {
                // TODO: Implement Discord notification
                // This would use the DiscordIntegration class
            }
        }
    }
    
    public void shutdown() {
        // Clear all maps
        lastLoginTimes.clear();
        loginCounts.clear();
        lastMovementTimes.clear();
        inspectionStartTimes.clear();
    }
}
