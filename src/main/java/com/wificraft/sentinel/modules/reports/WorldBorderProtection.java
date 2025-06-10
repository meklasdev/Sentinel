package com.wificraft.sentinel.modules.reports;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

public class WorldBorderProtection implements Listener {
    private final ReportManager reportManager;
    private final Map<String, Location> worldBorders;
    private final Map<String, Integer> borderWarningCount;
    
    public WorldBorderProtection(ReportManager reportManager, FileConfiguration config) {
        this.reportManager = reportManager;
        this.worldBorders = new HashMap<>();
        this.borderWarningCount = new HashMap<>();
        
        // Load world borders from config
        for (String worldName : config.getConfigurationSection("world-borders").getKeys(false)) {
            Location border = new Location(
                Bukkit.getWorld(worldName),
                config.getDouble("world-borders." + worldName + ".x"),
                config.getDouble("world-borders." + worldName + ".y"),
                config.getDouble("world-borders." + worldName + ".z")
            );
            worldBorders.put(worldName, border);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        checkBorder(event.getPlayer(), event.getTo());
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        checkBorder(event.getPlayer(), event.getTo());
    }

    private void checkBorder(Player player, Location location) {
        if (location == null) return;
        
        String worldName = location.getWorld().getName();
        Location border = worldBorders.get(worldName);
        
        if (border != null) {
            double distance = location.distance(border);
            
            if (distance > 2000) { // Player is too far from border
                Integer count = borderWarningCount.getOrDefault(worldName, 0);
                borderWarningCount.put(worldName, count + 1);
                
                if (count >= 3) { // More than 3 warnings in quick succession
                    reportManager.createReport(null, player.getUniqueId(), 
                        "Player approaching world border in " + worldName);
                }
                
                // Teleport player back if they're too far
                if (distance > 3000) {
                    player.teleport(border);
                }
            }
        }
    }

    /**
     * Get border information for a world
     * @param worldName Name of the world
     * @return Border location or null if not set
     */
    public Location getBorderLocation(String worldName) {
        return worldBorders.get(worldName);
    }

    /**
     * Set border for a world
     * @param worldName Name of the world
     * @param location Border location
     */
    public void setBorderLocation(String worldName, Location location) {
        worldBorders.put(worldName, location);
        borderWarningCount.put(worldName, 0);
    }

    /**
     * Remove border protection for a world
     * @param worldName Name of the world
     */
    public void removeBorder(String worldName) {
        worldBorders.remove(worldName);
        borderWarningCount.remove(worldName);
    }
}
