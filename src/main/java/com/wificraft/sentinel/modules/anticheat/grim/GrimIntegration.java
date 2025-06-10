package com.wificraft.sentinel.modules.anticheat.grim;

import ac.grim.grimac.api.GrimAbstractAPI;
import ac.grim.grimac.api.events.FlagEvent;
import ac.grim.grimac.api.platform.GrimUser;
import ac.grim.grimac.api.check.AbstractCheck;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.HandlerList;
import java.util.Map;
import org.bukkit.plugin.Plugin;

import com.wificraft.sentinel.modules.anticheat.AntiCheatIntegration;
import com.wificraft.sentinel.SentinelPlugin;

public class GrimIntegration implements Listener {
    private final SentinelPlugin plugin;
    private final AntiCheatIntegration antiCheat;
    private final Map<String, Integer> violationLevels;

    public GrimIntegration(SentinelPlugin plugin) {
        this.plugin = plugin;
        this.antiCheat = plugin.getAntiCheatIntegration();
        this.violationLevels = new java.util.HashMap<>();
        initializeViolationLevels();
    }

    private void initializeViolationLevels() {
        violationLevels.put("SPEED", 2);
        violationLevels.put("REACH", 2);
        violationLevels.put("FLY", 3);
        violationLevels.put("WALLCLIMB", 3);
        violationLevels.put("FASTPLACE", 2);
        violationLevels.put("FASTBREAK", 2);
        violationLevels.put("NOSLOWDOWN", 2);
    }

    @EventHandler
    public void onGrimFlag(FlagEvent event) {
        GrimUser user = event.getPlayer();
        Player player = Bukkit.getPlayer(user.getUniqueId());

        if (player == null) return;

        AbstractCheck check = event.getCheck();
        String checkName = check.getCheckName().toUpperCase();

        // Convert Grim violation type to our format
        String violationType = "GRIM_" + checkName;
        
        // Get violation level from configuration
        int level = violationLevels.getOrDefault(checkName, 1);
        
        // Process violation
        antiCheat.processViolation(player, violationType, level);
        
        // Log violation
        plugin.getLogger().info("Grim violation detected: " + 
            player.getName() + " - " + checkName);
    }

    public void register() {
        if (Bukkit.getPluginManager().isPluginEnabled("GrimAC")) {
            RegisteredServiceProvider<GrimAbstractAPI> provider = Bukkit.getServicesManager().getRegistration(GrimAbstractAPI.class);
            if (provider != null) {
                GrimAbstractAPI api = provider.getProvider();
                Bukkit.getPluginManager().registerEvents(this, plugin);
                plugin.getLogger().info("Successfully registered GrimAPI integration");
            } else {
                plugin.getLogger().warning("GrimAPI provider not found!");
            }
        } else {
            plugin.getLogger().warning("GrimAC plugin not found!");
        }
    }

    public void unregister() {
        HandlerList.unregisterAll(this);
        plugin.getLogger().info("Unregistered GrimAPI integration");
    }
}
