package com.wificraft.sentinel.modules.data;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ActivityAnalyzer {
    private final Map<UUID, Long> lastActivityTimes;
    private final Map<UUID, Integer> loginCounts;
    private final Map<UUID, Long> sessionDurations;
    private final Map<UUID, Integer> inspectionCounts;
    private final Map<UUID, Integer> banCounts;
    private final Map<UUID, Long> idleTimes;
    
    public ActivityAnalyzer() {
        this.lastActivityTimes = new ConcurrentHashMap<>();
        this.loginCounts = new ConcurrentHashMap<>();
        this.sessionDurations = new ConcurrentHashMap<>();
        this.inspectionCounts = new ConcurrentHashMap<>();
        this.banCounts = new ConcurrentHashMap<>();
        this.idleTimes = new ConcurrentHashMap<>();
        
        // Start monitoring tasks
        startMonitoringTasks();
    }
    
    private void startMonitoringTasks() {
        // Check activity every minute
        new BukkitRunnable() {
            @Override
            public void run() {
                checkActivity();
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("SentinelPlugin"), 20 * 60, 20 * 60);
    }
    
    public void onPlayerLogin(Player player) {
        UUID uuid = player.getUniqueId();
        lastActivityTimes.put(uuid, System.currentTimeMillis());
        loginCounts.compute(uuid, (k, v) -> v == null ? 1 : v + 1);
        sessionDurations.put(uuid, 0L);
    }
    
    public void onPlayerLogout(Player player) {
        UUID uuid = player.getUniqueId();
        long duration = System.currentTimeMillis() - lastActivityTimes.getOrDefault(uuid, System.currentTimeMillis());
        sessionDurations.compute(uuid, (k, v) -> v + duration);
    }
    
    public void onPlayerMovement(Player player) {
        lastActivityTimes.put(player.getUniqueId(), System.currentTimeMillis());
        idleTimes.put(player.getUniqueId(), 0L);
    }
    
    public void onInspectionStart(Player player) {
        inspectionCounts.compute(player.getUniqueId(), (k, v) -> v == null ? 1 : v + 1);
    }
    
    public void onBan(Player player) {
        banCounts.compute(player.getUniqueId(), (k, v) -> v == null ? 1 : v + 1);
    }
    
    private void checkActivity() {
        long currentTime = System.currentTimeMillis();
        
        // Check all online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            
            // Check idle time
            long lastActivity = lastActivityTimes.getOrDefault(uuid, currentTime);
            long idleTime = currentTime - lastActivity;
            idleTimes.compute(uuid, (k, v) -> v == null ? idleTime : v + idleTime);
            
            // Check login frequency
            int logins = loginCounts.getOrDefault(uuid, 0);
            long sessionDuration = sessionDurations.getOrDefault(uuid, 0L);
            
            // Check inspection frequency
            int inspections = inspectionCounts.getOrDefault(uuid, 0);
            
            // Check ban frequency
            int bans = banCounts.getOrDefault(uuid, 0);
            
            // Send warning if suspicious activity is detected
            if (isSuspiciousActivity(uuid, logins, sessionDuration, idleTime, inspections, bans)) {
                sendWarning(player);
            }
        }
    }
    
    private boolean isSuspiciousActivity(UUID uuid, int logins, long sessionDuration, 
                                        long idleTime, int inspections, int bans) {
        // Check login frequency
        if (logins > 3) return true;
        
        // Check session duration
        if (sessionDuration > TimeUnit.HOURS.toMillis(12)) return true;
        
        // Check idle time
        if (idleTime > TimeUnit.MINUTES.toMillis(30)) return true;
        
        // Check inspection frequency
        if (inspections > 2) return true;
        
        // Check ban frequency
        if (bans > 1) return true;
        
        return false;
    }
    
    private void sendWarning(Player player) {
        // This would be implemented by DiscordIntegration
        // For now, just log the warning
        Bukkit.getLogger().warning("Suspicious activity detected for player: " + player.getName());
    }
    
    public void checkActivity() {
        // This method is now implemented in DiscordIntegration
    }
    
    public Map<String, Object> getActivityStats(Player player) {
        UUID uuid = player.getUniqueId();
        return Map.of(
            "last_activity", lastActivityTimes.getOrDefault(uuid, 0L),
            "login_count", loginCounts.getOrDefault(uuid, 0),
            "session_duration", sessionDurations.getOrDefault(uuid, 0L),
            "idle_time", idleTimes.getOrDefault(uuid, 0L),
            "inspection_count", inspectionCounts.getOrDefault(uuid, 0),
            "ban_count", banCounts.getOrDefault(uuid, 0)
        );
    }
    
    public int getLoginCount(UUID playerId) {
        return loginCounts.getOrDefault(playerId, 0);
    }
    
    public long getSessionDuration(UUID playerId) {
        return sessionDurations.getOrDefault(playerId, 0L);
    }
    
    public int getInspectionCount(UUID playerId) {
        return inspectionCounts.getOrDefault(playerId, 0);
    }
    
    public int getBanCount(UUID playerId) {
        return banCounts.getOrDefault(playerId, 0);
    }
    
    public long getIdleTime(UUID playerId) {
        return idleTimes.getOrDefault(playerId, 0L);
    }
    
    public void updateActivity(Player player) {
        lastActivityTimes.put(player.getUniqueId(), System.currentTimeMillis());
    }
    
    public void recordInspection(Player player) {
        inspectionCounts.compute(player.getUniqueId(), (k, v) -> v == null ? 1 : v + 1);
    }
    
    public void shutdown() {
        // Clear all maps
        lastActivityTimes.clear();
        loginCounts.clear();
        sessionDurations.clear();
        inspectionCounts.clear();
        banCounts.clear();
        idleTimes.clear();
    }
}
