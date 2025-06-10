package com.wificraft.sentinel.alerts;

import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

public class AlertManager {
    private final List<Alert> alerts = new ArrayList<>();

    public AlertManager() {
    }

    public void createAlert(String title, String description, SeverityLevel severity) {
        // Create alert object
        Alert alert = new Alert("console", title, description, severity);
        createAlert(alert);
    }

    public void createAlert(Alert alert) {
        alerts.add(alert);

        // Log to console
        String message = String.format("[ALERT] %s: %s", alert.getTitle(), alert.getDescription());
        switch (alert.getSeverity()) {
            case YELLOW:
                Bukkit.getLogger().warning(message);
                break;
            case ORANGE:
                Bukkit.getLogger().severe(message);
                break;
            case RED:
                Bukkit.getLogger().severe("[RED] " + message);
                break;
            default:
                Bukkit.getLogger().info(message);
                break;
        }
    }

    public String listAlerts() {
        if (alerts.isEmpty()) {
            return "No alerts found.";
        }

        StringBuilder sb = new StringBuilder("Current alerts:\n");
        for (Alert alert : alerts) {
            sb.append(String.format("[%s] %s - %s\n", 
                alert.getSeverity().getEmoji(), 
                alert.getTitle(), 
                alert.getDescription()));
        }
        return sb.toString();
    }

    public void clearAlerts() {
        alerts.clear();
    }
    
    public boolean isEnabled() {
        return true;
    }
}
