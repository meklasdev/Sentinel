package com.wificraft.sentinel.modules;

import com.wificraft.sentinel.WiFiCraftSentinel;
import com.wificraft.sentinel.alerts.SeverityLevel;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.logging.Level;

public class PerformanceMonitor extends BukkitRunnable {
    private final WiFiCraftSentinel plugin;

    public PerformanceMonitor(WiFiCraftSentinel plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        try {
            // Check TPS
            double tps = Bukkit.getServer().getTPS()[0];
            double tpsThreshold = plugin.getConfig().getDouble("performance.tps-threshold", 15.0);
            if (tps < tpsThreshold) {
                String message = String.format("Server TPS is below threshold: %.2f (Threshold: %.2f)", tps, tpsThreshold);
                plugin.getLogger().warning(message);
                if (plugin.getAlertManager() != null) {
                    plugin.getAlertManager().createAlert(
                        "Low TPS Detected",
                        message,
                        SeverityLevel.ORANGE
                    );
                }
            }


            // Check memory usage
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
            long maxMemory = runtime.maxMemory() / (1024 * 1024);
            double memoryUsage = (double) usedMemory / maxMemory;
            double memoryThreshold = plugin.getConfig().getDouble("performance.memory-threshold", 0.85);

            if (memoryUsage > memoryThreshold) {
                String message = String.format("Memory usage is %.2f%% of maximum (Threshold: %.0f%%)", 
                    memoryUsage * 100, memoryThreshold * 100);
                plugin.getLogger().warning(message);
                if (plugin.getAlertManager() != null) {
                    plugin.getAlertManager().createAlert(
                        "High Memory Usage",
                        message,
                        SeverityLevel.YELLOW
                    );
                }
                
                // Trigger memory optimization if enabled
                if (plugin.getPerformanceModule() != null && 
                    plugin.getConfig().getBoolean("performance.optimize-on-high-memory", true)) {
                    plugin.getPerformanceModule().optimizeServer();
                }
            }

            // Check player count
            int playerCount = Bukkit.getOnlinePlayers().size();
            int maxPlayers = plugin.getConfig().getInt("performance.max-players", 50);
            
            if (playerCount > maxPlayers) {
                String message = String.format("%d players online (max: %d)", playerCount, maxPlayers);
                plugin.getLogger().warning("High player count: " + message);
                if (plugin.getAlertManager() != null) {
                    plugin.getAlertManager().createAlert(
                        "High Player Count",
                        message,
                        SeverityLevel.YELLOW
                    );
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error in PerformanceMonitor: " + e.getMessage(), e);
        }
    }
}
