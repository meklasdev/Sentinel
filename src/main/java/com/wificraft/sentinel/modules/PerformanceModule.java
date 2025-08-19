package com.wificraft.sentinel.modules;

import com.wificraft.sentinel.WiFiCraftSentinel;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Level;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;
import java.util.logging.Logger;

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
        long startTime = System.currentTimeMillis();
        plugin.getLogger().info("Starting server optimization...");
        
        try {
            int unloadedChunks = 0;
            // Clear chunk cache
            for (org.bukkit.World world : Bukkit.getServer().getWorlds()) {
                plugin.getLogger().info("Optimizing world: " + world.getName());
                for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
                    if (!chunk.isForceLoaded()) {
                        if (world.unloadChunk(chunk.getX(), chunk.getZ(), false)) {
                            unloadedChunks++;
                        }
                    }
                }
            }
            
            plugin.getLogger().info("Unloaded " + unloadedChunks + " chunks");
            
            // Clear entity and tile entity caches
            int removedEntities = 0;
            for (org.bukkit.entity.Entity entity : Bukkit.getWorlds().stream()
                    .flatMap(w -> w.getEntities().stream())
                    .filter(e -> !(e instanceof org.bukkit.entity.Player) && !e.isCustomNameVisible())
                    .toArray(org.bukkit.entity.Entity[]::new)) {
                entity.remove();
                removedEntities++;
            }
            
            plugin.getLogger().info("Removed " + removedEntities + " entities");
            
            // Force garbage collection
            long beforeGC = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            System.gc();
            long afterGC = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            
            plugin.getLogger().info(String.format("GC freed %d MB", (beforeGC - afterGC) / (1024 * 1024)));
            
            // Reset scheduled tasks if configured to do so
            if (plugin.getConfig().getBoolean("performance.reset-scheduled-tasks", false)) {
                int taskCount = Bukkit.getScheduler().getPendingTasks().size();
                Bukkit.getScheduler().cancelTasks(plugin);
                plugin.getLogger().info("Cancelled " + taskCount + " scheduled tasks");
            }
            
            lastOptimizationTime = System.currentTimeMillis();
            long duration = System.currentTimeMillis() - startTime;
            
            plugin.getLogger().info(String.format("Server optimization completed in %d ms. Memory before: %d MB, after: %d MB",
                duration,
                beforeGC / (1024 * 1024),
                afterGC / (1024 * 1024)));
                
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Error during server optimization: " + e.getMessage(), e);
        }
    }

    private PerformanceMonitor performanceMonitor;

    private void startMonitoring() {
        if (performanceMonitor == null) {
            performanceMonitor = new PerformanceMonitor(plugin);
            performanceMonitor.runTaskTimer(plugin, 0, 20); // Run every second
        }
    }
    
    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("performance.enabled", false);
    }
    
    public Map<String, Long> getStats() {
        Map<String, Long> stats = new HashMap<>();
        try {
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
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get performance stats: " + e.getMessage());
        }
        return stats;
    }
}
