package com.wificraft.sentinel.modules.data;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerStatsManager implements Listener {
    private final JavaPlugin plugin;
    private final Map<UUID, PlayerStats> playerStats;
    private final Map<UUID, Long> sessionStartTimes;
    
    public PlayerStatsManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.playerStats = new HashMap<>();
        this.sessionStartTimes = new HashMap<>();
        
        // Load existing stats
        loadStats();
        
        // Register events
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    private void loadStats() {
        File statsFile = new File(plugin.getDataFolder(), "player_stats.yml");
        if (!statsFile.exists()) return;
        
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(statsFile);
            if (config.contains("stats")) {
                for (String uuidStr : config.getConfigurationSection("stats").getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                        if (offlinePlayer.hasPlayedBefore()) {
                            PlayerStats stats = new PlayerStats(offlinePlayer);
                            // Initialize stats with default values
                            stats.setLastLoginTime(config.getLong("stats." + uuidStr + ".lastLogin", System.currentTimeMillis()));
                            stats.setLoginCount(config.getInt("stats." + uuidStr + ".loginCount", 1));
                            stats.setInspectionCount(config.getInt("stats." + uuidStr + ".inspectionCount", 0));
                            stats.setBanCount(config.getInt("stats." + uuidStr + ".banCount", 0));
                            stats.setTotalPlayTime(config.getLong("stats." + uuidStr + ".totalPlayTime", 0));
                            stats.setLongestPlaySession(config.getLong("stats." + uuidStr + ".longestPlaySession", 0));
                            playerStats.put(uuid, stats);
                        }
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in player stats: " + uuidStr);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading player stats: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void saveStats() {
        try {
            YamlConfiguration config = new YamlConfiguration();
            
            for (Map.Entry<UUID, PlayerStats> entry : playerStats.entrySet()) {
                String uuid = entry.getKey().toString();
                PlayerStats stats = entry.getValue();
                
                config.set("stats." + uuid + ".lastLogin", stats.getLastLoginTime());
                config.set("stats." + uuid + ".loginCount", stats.getLoginCount());
                config.set("stats." + uuid + ".inspectionCount", stats.getInspectionCount());
                config.set("stats." + uuid + ".banCount", stats.getBanCount());
                config.set("stats." + uuid + ".totalPlayTime", stats.getTotalPlayTime());
                config.set("stats." + uuid + ".longestPlaySession", stats.getLongestPlaySession());
            }
            
            // Ensure data folder exists
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            
            // Save to file
            File statsFile = new File(plugin.getDataFolder(), "player_stats.yml");
            config.save(statsFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Error saving player stats: " + e.getMessage());
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        if (!playerStats.containsKey(uuid)) {
            playerStats.put(uuid, new PlayerStats(player));
        }
        
        PlayerStats stats = playerStats.get(uuid);
        stats.updateLastLogin();
        stats.incrementLoginCount();
        sessionStartTimes.put(uuid, System.currentTimeMillis());
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        if (playerStats.containsKey(uuid) && sessionStartTimes.containsKey(uuid)) {
            long sessionDuration = System.currentTimeMillis() - sessionStartTimes.get(uuid);
            PlayerStats stats = playerStats.get(uuid);
            stats.addPlayTime(sessionDuration);
            saveStats();
        }
    }
    
    public PlayerStats getPlayerStats(Player player) {
        return playerStats.getOrDefault(player.getUniqueId(), new PlayerStats(player));
    }
    
    public void incrementInspectionCount(Player player) {
        if (playerStats.containsKey(player.getUniqueId())) {
            PlayerStats stats = playerStats.get(player.getUniqueId());
            stats.incrementInspectionCount();
            saveStats();
        }
    }
    
    public void incrementBanCount(Player player) {
        if (playerStats.containsKey(player.getUniqueId())) {
            PlayerStats stats = playerStats.get(player.getUniqueId());
            stats.incrementBanCount();
            saveStats();
        }
    }
    
    public void shutdown() {
        saveStats();
    }
}
