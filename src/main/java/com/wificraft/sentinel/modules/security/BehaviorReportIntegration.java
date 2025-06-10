package com.wificraft.sentinel.modules.security;

import com.wificraft.sentinel.WiFiCraftSentinel;
import com.wificraft.sentinel.alerts.AlertManager;
import com.wificraft.sentinel.alerts.SeverityLevel;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.*;

public class BehaviorReportIntegration {
    private final AlertManager alertManager;
    private final BehaviorAnalyzer behaviorAnalyzer;
    private final WiFiCraftSentinel plugin;
    
    public BehaviorReportIntegration(WiFiCraftSentinel plugin, BehaviorAnalyzer behaviorAnalyzer) {
        this.plugin = plugin;
        this.alertManager = new AlertManager();
        this.behaviorAnalyzer = behaviorAnalyzer;
    }
    
    public void handleAlert(String type, String description, int severity, UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            plugin.getLogger().warning("Cannot handle alert for offline player: " + playerId);
            return;
        }
        
        // Create alert title and message
        String title = String.format("Suspicious Behavior - %s", type);
        String message = String.format("Player %s triggered behavior alert:\nType: %s\nDescription: %s",
            player.getName(), type, description);
        
        // Map numeric severity to SeverityLevel
        SeverityLevel severityLevel = mapSeverity(severity);
        
        // Create and send alert
        alertManager.createAlert(title, message, severityLevel);
        
        // Log to console
        plugin.getLogger().info(String.format("[ALERT] %s - %s: %s", severityLevel, title, message));
    }
    
    private SeverityLevel mapSeverity(int severity) {
        if (severity >= 8) return SeverityLevel.RED;
        if (severity >= 5) return SeverityLevel.ORANGE;
        if (severity >= 3) return SeverityLevel.YELLOW;
        return SeverityLevel.GREEN;
    }
}
