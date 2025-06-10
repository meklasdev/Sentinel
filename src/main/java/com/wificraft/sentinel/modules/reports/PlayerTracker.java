package com.wificraft.sentinel.modules.reports;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerTracker implements Listener {
    private final ReportManager reportManager;
    private final Map<UUID, Location> lastLocations;
    private final Map<UUID, Long> lastMoveTimes;
    private final Map<UUID, Integer> teleportCount;
    
    public PlayerTracker(ReportManager reportManager) {
        this.reportManager = reportManager;
        this.lastLocations = new ConcurrentHashMap<>();
        this.lastMoveTimes = new ConcurrentHashMap<>();
        this.teleportCount = new ConcurrentHashMap<>();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        lastLocations.put(playerId, player.getLocation());
        lastMoveTimes.put(playerId, System.currentTimeMillis());
        teleportCount.put(playerId, 0);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        lastLocations.remove(playerId);
        lastMoveTimes.remove(playerId);
        teleportCount.remove(playerId);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        Location oldLoc = lastLocations.get(playerId);
        Location newLoc = player.getLocation();
        
        if (oldLoc != null) {
            // Check for suspicious movement
            double distance = oldLoc.distance(newLoc);
            long timeDiff = System.currentTimeMillis() - lastMoveTimes.get(playerId);
            double speed = distance / (timeDiff / 1000.0);
            
            if (speed > 10.0) { // More than 10 blocks per second
                reportManager.createReport(null, playerId, "Suspicious movement speed detected");
            }
        }
        
        lastLocations.put(playerId, newLoc);
        lastMoveTimes.put(playerId, System.currentTimeMillis());
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Track teleport frequency
        int count = teleportCount.getOrDefault(playerId, 0);
        teleportCount.put(playerId, count + 1);
        
        if (count >= 5) { // More than 5 teleports in quick succession
            reportManager.createReport(null, playerId, "Potential teleport spam detected");
        }
    }

    /**
     * Get player statistics
     * @param playerId Player's UUID
     * @return Map of statistics
     */
    public Map<String, Object> getPlayerStatistics(UUID playerId) {
        Map<String, Object> stats = new HashMap<>();
        
        Location currentLoc = lastLocations.get(playerId);
        if (currentLoc != null) {
            stats.put("last_location", currentLoc.toString());
            stats.put("world", currentLoc.getWorld().getName());
        }
        
        Long lastMove = lastMoveTimes.get(playerId);
        if (lastMove != null) {
            stats.put("last_move_time", lastMove);
        }
        
        Integer teleports = teleportCount.get(playerId);
        if (teleports != null) {
            stats.put("teleport_count", teleports);
        }
        
        return stats;
    }

    /**
     * Reset player tracking data
     * @param playerId Player's UUID
     */
    public void resetPlayerData(UUID playerId) {
        lastLocations.remove(playerId);
        lastMoveTimes.remove(playerId);
        teleportCount.remove(playerId);
    }
}
