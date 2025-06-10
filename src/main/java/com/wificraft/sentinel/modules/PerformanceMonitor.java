package com.wificraft.sentinel.modules;

import com.wificraft.sentinel.WiFiCraftSentinel;
import com.wificraft.sentinel.alerts.Alert;
import com.wificraft.sentinel.alerts.SeverityLevel;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

public class PerformanceMonitor extends BukkitRunnable {
    private final WiFiCraftSentinel plugin;

    public PerformanceMonitor(WiFiCraftSentinel plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        // Check TPS
        double tps = Bukkit.getServer().getTPS()[0];
        if (tps < plugin.getConfig().getDouble("performance.tps-threshold")) {
            plugin.getAlertManager().createAlert(
                "Low TPS Detected",
                String.format("Server TPS is below threshold: %.2f", tps),
                SeverityLevel.ORANGE
            );
        }

        // Check memory usage
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        double memoryUsage = (double) usedMemory / maxMemory;

        if (memoryUsage > plugin.getConfig().getDouble("performance.memory-threshold")) {
            plugin.getAlertManager().createAlert(
                "High Memory Usage",
                String.format("Memory usage is %.2f%% of maximum", memoryUsage * 100),
                SeverityLevel.YELLOW
            );
        }

        // Check player count
        int playerCount = Bukkit.getOnlinePlayers().size();
        int maxPlayers = plugin.getConfig().getInt("performance.max-players");
        
        if (playerCount > maxPlayers) {
            plugin.getAlertManager().createAlert(
                "High Player Count",
                String.format("%d players online (max: %d)", playerCount, maxPlayers),
                SeverityLevel.YELLOW
            );
        }
    }
}
