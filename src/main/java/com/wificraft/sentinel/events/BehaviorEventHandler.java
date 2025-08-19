package com.wificraft.sentinel.events;

import com.wificraft.sentinel.alerts.AlertManager;
import com.wificraft.sentinel.alerts.SeverityLevel;
import com.wificraft.sentinel.WiFiCraftSentinel;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;

import java.util.logging.Level;

public class BehaviorEventHandler implements Listener {
    private final WiFiCraftSentinel plugin;
    private final AlertManager alertManager;

    public BehaviorEventHandler(WiFiCraftSentinel plugin) {
        this.plugin = plugin;
        this.alertManager = plugin.getAlertManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerBehavior(PlayerBehaviorEvent event) {
        if (event == null || event.getPlayer() == null) {
            return;
        }
        
        try {
            handleBehaviorEvent(event);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error handling player behavior event", e);
        }
    }

    private void handleBehaviorEvent(PlayerBehaviorEvent event) {
        Player player = event.getPlayer();
        PlayerBehaviorEvent.BehaviorType behaviorType = event.getBehaviorType();
        String behaviorTypeStr = behaviorType != null ? behaviorType.name() : "UNKNOWN";
        double score = event.getScore();
        String reason = event.getReason();

        // Map score to severity level
        SeverityLevel severity;
        if (score >= 70) {
            severity = SeverityLevel.RED;
        } else if (score >= 50) {
            severity = SeverityLevel.ORANGE;
        } else if (score >= 30) {
            severity = SeverityLevel.YELLOW;
        } else {
            severity = SeverityLevel.GREEN;
        }

        // Create alert
        alertManager.createAlert(
            player,
            String.format("%s Behavior Detected", 
                score >= 70 ? "Suspicious" : "Potential Suspicious"),
            String.format("Player %s showed %s behavior: %s\nScore: %.1f\nReason: %s",
                player.getName(),
                score >= 70 ? "suspicious" : "potential suspicious",
                behaviorTypeStr,
                score,
                reason
            ),
            severity
        );

        // Log behavior event
        plugin.getLogger().info(String.format(
            "Behavior event detected for %s: %s (Score: %.1f, Severity: %s)",
            player.getName(),
            behaviorTypeStr,
            score,
            severity.name()
        ));
    }
}
