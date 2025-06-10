package com.wificraft.sentinel.modules.security;

import org.bukkit.entity.Player;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class HardwareFingerprinter {
    private final Map<UUID, HardwareFingerprint> fingerprints;
    private final Map<UUID, Long> lastScanTime;
    private final int scanIntervalSeconds;
    private final int cacheDurationMinutes;

    public HardwareFingerprinter() {
        this(300, 5); // Default: 5 minutes scan interval, 5 minutes cache duration
    }
    
    public HardwareFingerprinter(int scanIntervalSeconds, int cacheDurationMinutes) {
        this.fingerprints = new ConcurrentHashMap<>();
        this.lastScanTime = new ConcurrentHashMap<>();
        this.scanIntervalSeconds = scanIntervalSeconds;
        this.cacheDurationMinutes = cacheDurationMinutes;
    }

    public String getFingerprint(Player player) {
        if (player == null) {
            return "";
        }
        
        // Generate a consistent fingerprint based on player's UUID and name
        String base = player.getUniqueId().toString() + player.getName();
        return Integer.toHexString(base.hashCode());
    }
    
    public HardwareFingerprint getHardwareFingerprint(Player player) {
        if (player == null) {
            return new HardwareFingerprint("");
        }
        
        // Check if we have a cached fingerprint
        UUID playerId = player.getUniqueId();
        HardwareFingerprint fingerprint = fingerprints.get(playerId);
        
        if (fingerprint == null || shouldUpdateFingerprint(playerId, System.currentTimeMillis())) {
            // Create a new fingerprint
            String hwId = getFingerprint(player);
            fingerprint = new HardwareFingerprint(hwId);
            fingerprints.put(playerId, fingerprint);
            lastScanTime.put(playerId, System.currentTimeMillis());
        }
        
        return fingerprint;
    }

    private boolean shouldUpdateFingerprint(UUID playerId, long now) {
        Long lastScan = lastScanTime.get(playerId);
        return lastScan == null || 
               now - lastScan > TimeUnit.SECONDS.toMillis(scanIntervalSeconds);
    }

    public boolean isSuspiciousHardware(Player player) {
        if (player == null) {
            return false;
        }
        
        // Simple check - consider hardware suspicious if the hash is even
        // In a real implementation, you'd want more sophisticated checks
        HardwareFingerprint fingerprint = getHardwareFingerprint(player);
        return Math.abs(fingerprint.getHardwareId().hashCode()) % 2 == 0;
    }
    
    public String getHardwareAnalysis(Player player) {
        if (player == null) {
            return "";
        }
        
        HardwareFingerprint fingerprint = getHardwareFingerprint(player);
        if (fingerprint == null) {
            return "Brak danych o sprzęcie";
        }
        
        return "Analiza sprzętu:\n" +
               "ID: " + fingerprint.getHardwareId() + "\n" +
               "Podsumowanie: " + fingerprint.getHardwareSummary();
    }
}
