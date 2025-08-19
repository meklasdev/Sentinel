package com.wificraft.sentinel.modules.security;

import com.wificraft.sentinel.modules.reports.ReportManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClientFingerprintManager implements Listener {
    private final ReportManager reportManager;
    private final Map<UUID, ClientFingerprint> fingerprints;
    private final Map<UUID, Long> lastFingerprintTimes;
    private final Map<UUID, Integer> suspiciousAttempts;
    
    public ClientFingerprintManager(ReportManager reportManager) {
        this.reportManager = reportManager;
        this.fingerprints = new ConcurrentHashMap<>();
        this.lastFingerprintTimes = new ConcurrentHashMap<>();
        this.suspiciousAttempts = new ConcurrentHashMap<>();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Create new fingerprint
        ClientFingerprint fingerprint = new ClientFingerprint(player);
        fingerprints.put(playerId, fingerprint);
        lastFingerprintTimes.put(playerId, System.currentTimeMillis());
        
        // Check for suspicious behavior
        checkForSuspiciousBehavior(playerId);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        fingerprints.remove(playerId);
        lastFingerprintTimes.remove(playerId);
        suspiciousAttempts.remove(playerId);
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        checkForSuspiciousTeleport(playerId);
    }

    private void checkForSuspiciousBehavior(UUID playerId) {
        ClientFingerprint current = fingerprints.get(playerId);
        
        // Check against existing fingerprints
        for (Map.Entry<UUID, ClientFingerprint> entry : fingerprints.entrySet()) {
            if (!entry.getKey().equals(playerId)) {
                ClientFingerprint other = entry.getValue();
                if (current.isSuspicious(other)) {
                    handleSuspiciousActivity(playerId);
                    break;
                }
            }
        }
    }

    private void checkForSuspiciousTeleport(UUID playerId) {
        ClientFingerprint current = fingerprints.get(playerId);
        if (current == null) return;
        
        // Check teleport frequency
        long currentTime = System.currentTimeMillis();
        long lastTime = lastFingerprintTimes.getOrDefault(playerId, 0L);
        
        if (currentTime - lastTime < 1000) { // Less than 1 second between teleports
            Integer attempts = suspiciousAttempts.getOrDefault(playerId, 0);
            suspiciousAttempts.put(playerId, attempts + 1);
            
            if (attempts >= 3) { // More than 3 suspicious teleports in quick succession
                handleSuspiciousActivity(playerId);
            }
        }
        
        lastFingerprintTimes.put(playerId, currentTime);
    }

    private void handleSuspiciousActivity(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            reportManager.createReport(null, playerId, 
                "Suspicious client behavior detected - potential account sharing or cheating");
            
            // Add more actions as needed
            // For example: kick player, ban IP, etc.
        }
    }

    /**
     * Get fingerprint for a player
     * @param playerId Player's UUID
     * @return ClientFingerprint or null if not found
     */
    public ClientFingerprint getFingerprint(UUID playerId) {
        return fingerprints.get(playerId);
    }

    /**
     * Remove fingerprint data for a player
     * @param playerId Player's UUID
     */
    public void removeFingerprint(UUID playerId) {
        fingerprints.remove(playerId);
        lastFingerprintTimes.remove(playerId);
        suspiciousAttempts.remove(playerId);
    }

    /**
     * Get all fingerprints
     * @return Map of all fingerprints
     */
    public Map<UUID, ClientFingerprint> getAllFingerprints() {
        return new HashMap<>(fingerprints);
    }
}
