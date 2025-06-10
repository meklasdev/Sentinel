package com.wificraft.sentinel.events;

import com.wiscraft.sentinel.modules.security.AlertManager;
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
        String behaviorType = event.getBehaviorType(); // Assuming this is a String now
        double score = event.getScore();
        String reason = event.getReason();

        // Generate alert based on behavior score
        if (score >= 70) {
            alertManager.createAlert(
                player,
                "Suspicious Behavior Detected",
                "Player " + player.getName() + " showed suspicious behavior: " + behaviorType + 
                "\nScore: " + score + 
                "\nReason: " + reason,
                AlertManager.SeverityLevel.HIGH
            );
        } else if (score >= 50) {
            alertManager.createAlert(
                player,
                "Potential Suspicious Behavior",
                "Player " + player.getName() + " showed potential suspicious behavior: " + behaviorType + 
                "\nScore: " + score + 
                "\nReason: " + reason,
                AlertManager.SeverityLevel.MEDIUM
            );
        }

        // Log behavior event
        plugin.getLogger().info("Behavior event detected for " + player.getName() + 
                              ": " + behaviorType + " (Score: " + score + ")");
    }

    @EventHandler
    public void onPlayerBehavior(PlayerBehaviorEvent event) {
        onEvent(event);
    }
}
