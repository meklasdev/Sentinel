package com.wificraft.sentinel.modules;

import com.wificraft.sentinel.WiFiCraftSentinel;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.logging.Level;
import java.util.Map;
import java.util.HashMap;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;
import org.bukkit.Chunk;

public class PerformanceModule {
    private final WiFiCraftSentinel plugin;
    private BukkitTask monitoringTask;
    private long lastOptimizationTime;

    public PerformanceModule(WiFiCraftSentinel plugin) {
        this.plugin = plugin;
        this.lastOptimizationTime = System.currentTimeMillis();
    }

    public void initialize() {
        if (!plugin.getConfig().getBoolean("performance.enabled")) {
            return;
        }

        // Start performance monitoring
        monitoringTask = new PerformanceMonitor(plugin).runTaskTimer(
            plugin,
            plugin.getConfig().getInt("performance.check-interval-ticks"),
            plugin.getConfig().getInt("performance.check-interval-ticks")
        );
    }

    public void disable() {
        if (monitoringTask != null) {
            monitoringTask.cancel();
        }
    }
    
    public double getTps() {
        // Get the TPS from the server (20.0 is the maximum TPS for a healthy server)
        try {
            return Bukkit.getServer().getTPS()[0]; // Get average TPS from last 1 minute
        } catch (Exception e) {
            return 20.0; // Default to perfect TPS if there's an error
        }
    }

    public void optimizeServer() {
        // Clear chunk cache
        Bukkit.getServer().getWorlds().forEach(world -> {
            for (Chunk chunk : world.getLoadedChunks()) {
                if (!chunk.isForceLoaded()) {
                    world.unloadChunk(chunk.getX(), chunk.getZ(), false);
                }
            }
        });

        // Force garbage collection
        System.gc();

        // Reset scheduled tasks
        Bukkit.getScheduler().cancelTasks(plugin);

        lastOptimizationTime = System.currentTimeMillis();
        plugin.getLogger().info("Serwer zoptymalizowany!");
    }

    private class PerformanceMonitor extends BukkitRunnable {
        private long lastOptimizationTime = 0;
        private final WiFiCraftSentinel plugin;

        public PerformanceMonitor(WiFiCraftSentinel plugin) {
            this.plugin = plugin;
        }

        @Override
        public void run() {
            double tps = getTPS();
            long ramUsage = getRAMUsage();
            long ramThreshold = plugin.getConfig().getLong("performance.ram-threshold-mb") * 1024 * 1024;

            // Check TPS
            if (tps < plugin.getConfig().getDouble("performance.tps-threshold")) {
                plugin.getLogger().warning("TPS spadł poniżej progów: " + tps);
                if (plugin.getConfig().getBoolean("performance.optimize-on-low-tps")) {
                    optimizeServer();
                }
            }

            // Check RAM usage
            if (ramUsage > ramThreshold) {
                plugin.getLogger().warning("Zużycie RAM przekroczyło prog: " + (ramUsage / (1024 * 1024)) + "MB");
                optimizeServer();
            }
        }

        private double getTPS() {
            return Bukkit.getServer().getTPS()[0];
        }

        private long getRAMUsage() {
            return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        }
    }

    private void startMonitoring() {
        PerformanceMonitor monitor = new PerformanceMonitor(this.plugin);
        monitor.runTaskTimer(this.plugin, 0, 20); // Run every second
    }
    
    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("performance.enabled", false);
    }
    
    public Map<String, Long> getStats() {
        Map<String, Long> stats = new HashMap<>();
        Runtime runtime = Runtime.getRuntime();
        
        // Add CPU usage
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        stats.put("cpu", (long) (osBean.getProcessCpuLoad() * 100));
        
        // Add memory usage
        stats.put("memory", (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));
        stats.put("maxMemory", runtime.maxMemory() / (1024 * 1024));
        
        // Add TPS (converted to ms)
        double tps = Bukkit.getServer().getTPS()[0];
        stats.put("tps", (long) (1000.0 / Math.max(tps, 1.0)));
        stats.put("lastCpu", (long) (osBean.getProcessCpuLoad() * 100));
        
        return stats;
    }
}
