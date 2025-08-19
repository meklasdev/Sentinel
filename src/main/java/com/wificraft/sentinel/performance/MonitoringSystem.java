package com.wificraft.sentinel.performance;

import com.wificraft.sentinel.alerts.AlertManager;
import com.wificraft.sentinel.alerts.SeverityLevel;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

public class MonitoringSystem {
    private final AlertManager alertManager;
    private final long checkInterval;
    private final long warningThreshold;
    private final long criticalThreshold;

    public MonitoringSystem(AlertManager alertManager, long checkInterval, 
                          long warningThreshold, long criticalThreshold) {
        this.alertManager = alertManager;
        this.checkInterval = checkInterval;
        this.warningThreshold = warningThreshold;
        this.criticalThreshold = criticalThreshold;
    }

    public void startMonitoring() {
        new BukkitRunnable() {
            @Override
            public void run() {
                checkPerformance();
            }
        }.runTaskTimerAsynchronously(Bukkit.getPluginManager().getPlugin("Sentinel"), 
            0, checkInterval);
    }

    private void checkPerformance() {
        // Check CPU usage
        double cpuUsage = getCpuUsage();
        if (cpuUsage > criticalThreshold) {
            alertManager.createAlert(
                "High CPU Usage",
                "CPU usage is at " + cpuUsage + "%",
                SeverityLevel.RED
            );
        } else if (cpuUsage > warningThreshold) {
            alertManager.createAlert(
                "Warning: CPU Usage",
                "CPU usage is at " + cpuUsage + "%",
                SeverityLevel.YELLOW
            );
        }

        // Check memory usage
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        double memoryUsage = (usedMemory / (double) maxMemory) * 100;

        if (memoryUsage > 90) {
            alertManager.createAlert(
                "High Memory Usage",
                "Memory usage is at " + memoryUsage + "%",
                SeverityLevel.RED
            );
        } else if (memoryUsage > 80) {
            alertManager.createAlert(
                "Warning: Memory Usage",
                "Memory usage is at " + memoryUsage + "%",
                SeverityLevel.YELLOW
            );
        }
    }

    private double getCpuUsage() {
        // Implementation of CPU usage calculation
        // This should be replaced with actual CPU monitoring
        return 0.0;
    }
}
