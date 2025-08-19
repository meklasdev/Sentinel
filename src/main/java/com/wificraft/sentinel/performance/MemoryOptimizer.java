package com.wificraft.sentinel.performance;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

public class MemoryOptimizer {
    private final long cleanupInterval;
    private final long maxHistoryMs;
    private final Map<UUID, Long> lastEventTimes;
    
    public MemoryOptimizer(long cleanupInterval, long maxHistoryMs) {
        this.cleanupInterval = cleanupInterval;
        this.maxHistoryMs = maxHistoryMs;
        this.lastEventTimes = new ConcurrentHashMap<>();
    }
    
    public void startOptimization() {
        new BukkitRunnable() {
            @Override
            public void run() {
                optimizeMemory();
            }
        }.runTaskTimerAsynchronously(Bukkit.getPluginManager().getPlugin("Sentinel"), 
            0, cleanupInterval);
    }
    
    public void trackEvent(UUID playerId, long timestamp) {
        lastEventTimes.put(playerId, timestamp);
    }
    
    private void optimizeMemory() {
        // Clean up old player data
        long currentTime = System.currentTimeMillis();
        
        lastEventTimes.entrySet().removeIf(entry -> {
            long lastEventTime = entry.getValue();
            return (currentTime - lastEventTime) > maxHistoryMs;
        });
        
        // Force garbage collection if needed
        if (Runtime.getRuntime().freeMemory() < 100_000_000) { // 100MB threshold
            System.gc();
        }
    }
}
