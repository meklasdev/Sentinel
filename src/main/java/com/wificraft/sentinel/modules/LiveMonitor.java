package com.wificraft.sentinel.modules;

import com.wificraft.sentinel.WiFiCraftSentinel;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LiveMonitor {
    private final WiFiCraftSentinel plugin;
    private BukkitTask refreshTask;
    private final List<Player> viewingPlayers;

    public LiveMonitor(WiFiCraftSentinel plugin) {
        this.plugin = plugin;
        this.viewingPlayers = new ArrayList<>();
    }

    public void initialize() {
        if (!plugin.getConfig().getBoolean("livemonitor.enabled")) {
            return;
        }

        // Start refresh task
        refreshTask = new RefreshTask().runTaskTimer(
            plugin,
            0L,
            plugin.getConfig().getInt("livemonitor.refresh-seconds") * 20L
        );
    }

    public void disable() {
        if (refreshTask != null) {
            refreshTask.cancel();
        }
    }
    
    public void refreshMonitoredPlayers() {
        // Implementation for refreshing monitored players
        // This is a placeholder - implement actual refresh logic here
    }
    
    public int getActiveMonitorCount() {
        return viewingPlayers.size();
    }

    public void openLiveMonitor(Player viewer) {
        viewingPlayers.add(viewer);
        refreshGUI(viewer);
    }

    private void refreshGUI(Player viewer) {
        Inventory gui = Bukkit.createInventory(null, 54, plugin.getConfig().getString("livemonitor.gui-title"));

        // Add server stats
        gui.setItem(0, createStatItem(Material.COMPASS, "§6TPS", String.format("§e%.2f", Bukkit.getServer().getTPS()[0])));
        gui.setItem(1, createStatItem(Material.DIAMOND, "§6RAM", formatRamUsage()));
        gui.setItem(2, createStatItem(Material.CLOCK, "§6CPU", formatCpuUsage()));

        // Add player list
        int slot = 10;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (slot >= 54) {
                break;
            }

            gui.setItem(slot, createPlayerItem(player));
            slot += 2;
        }

        viewer.openInventory(gui);
    }

    private ItemStack createStatItem(Material material, String name, String value) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of(value));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPlayerItem(Player player) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        if (item.getItemMeta() == null) {
            return item;
        }

        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(player);
        meta.setDisplayName("§e" + player.getName());
        
        // Add comprehensive player info
        List<String> lore = new ArrayList<>();
        lore.add("§7Ping: " + player.getPing() + "ms");
        lore.add("§7World: " + player.getWorld().getName());
        lore.add("§7Location: " + String.format("%.1f, %.1f, %.1f", 
            player.getLocation().getX(), 
            player.getLocation().getY(), 
            player.getLocation().getZ()));
        
        // Add inspection status
        int inspections = plugin.getInspectionModule().getPlayerInspectionCount(player.getUniqueId());
        lore.add("§7Inspections: " + inspections);
        
        // Add behavior score if available
        //if (plugin.getBehaviorAnalyzer() != null) {
        //    double score = plugin.getBehaviorAnalyzer().getPlayerScore(player.getUniqueId());
        //    lore.add("§7Behavior Score: " + String.format("%.2f", score));
        //}
        
        // Add IP info if available
        if (plugin.getIpAnalyzer() != null) {
            String ip = player.getAddress().getHostString();
            lore.add("§7IP: " + ip);
            double locationScore = plugin.getIpAnalyzer().getLocationScore(ip);
            lore.add("§7Location Score: " + String.format("%.2f", locationScore));
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String formatRamUsage() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long allocatedMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        
        double usedPercentage = ((allocatedMemory - freeMemory) / (double) maxMemory) * 100;
        return String.format("§e%.2f%% (Used: %.2f/%.2f MB)", 
            usedPercentage,
            (allocatedMemory - freeMemory) / (1024.0 * 1024.0),
            maxMemory / (1024.0 * 1024.0));
    }

    private String formatCpuUsage() {
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        double systemLoad = osBean.getSystemLoadAverage() * 100;
        double processLoad = osBean.getProcessCpuLoad() * 100;
        
        return String.format("§eSystem: %.1f%%, Process: %.1f%%", 
            systemLoad, processLoad);
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("livemonitor.enabled", false);
    }
    
    public void refreshCache() {
        // Clear any cached data if needed
        // This is a placeholder for actual cache refresh logic
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
        // Re-initialize the refresh task
        initialize();
    }
    
    private class RefreshTask extends BukkitRunnable {
        private final Set<Player> playersToRemove = new HashSet<>();
        
        @Override
        public void run() {
            // First mark players to remove
            for (Player viewer : viewingPlayers) {
                if (!viewer.isOnline()) {
                    playersToRemove.add(viewer);
                }
            }
            
            // Then remove them outside the loop
            viewingPlayers.removeAll(playersToRemove);
            playersToRemove.clear();
            
            // Refresh GUI for remaining players
            for (Player viewer : viewingPlayers) {
                refreshGUI(viewer);
            }
        }
    }
}
