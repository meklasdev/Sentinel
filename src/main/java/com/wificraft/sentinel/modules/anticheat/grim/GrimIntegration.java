package com.wificraft.sentinel.modules.anticheat.grim;

import com.wificraft.sentinel.WiFiCraftSentinel;
import com.wificraft.sentinel.modules.anticheat.AntiCheatIntegration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * GrimAC integration using reflection to avoid direct dependency.
 * This allows the plugin to work without GrimAC being present at compile time.
 */
public class GrimIntegration implements Listener {
    private final WiFiCraftSentinel plugin;
    private final AntiCheatIntegration antiCheat;
    private final Map<String, Integer> violationLevels;
    private Object grimACListener;
    private boolean registered = false;

    public GrimIntegration(WiFiCraftSentinel plugin) {
        this.plugin = plugin;
        this.antiCheat = plugin.getAntiCheatIntegration();
        
        // Initialize violation levels for different checks
        this.violationLevels = new HashMap<>();
        violationLevels.put("SPEED", 3);
        violationLevels.put("REACH", 3);
        violationLevels.put("FLY", 5);
        violationLevels.put("WALLCLIMB", 4);
        violationLevels.put("FASTPLACE", 2);
        violationLevels.put("FASTBREAK", 2);
        violationLevels.put("NOSLOWDOWN", 2);
    }

    public void register() {
        if (registered) {
            return;
        }

        try {
            // Check if GrimAC is loaded
            Plugin grimPlugin = plugin.getServer().getPluginManager().getPlugin("GrimAC");
            if (grimPlugin == null) {
                plugin.getLogger().warning("GrimAC plugin not found. Grim integration will be disabled.");
                return;
            }

            // Get GrimAC classes using reflection
            Class<?> flagEventClass = Class.forName("ac.grim.grimac.events.FlagEvent");
            Class<?> grimUserClass = Class.forName("ac.grim.grimac.player.GrimUser");
            Class<?> abstractCheckClass = Class.forName("ac.grim.grimac.checks.type.AbstractCheck");

            // Create a dynamic proxy to handle the event
            grimACListener = java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] { Listener.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("onGrimFlag") && args.length == 1) {
                        handleGrimFlag(args[0]);
                        return null;
                    }
                    return method.invoke(this, args);
                }
            );

            // Register the event using reflection
            Object pluginManager = plugin.getServer().getPluginManager();
            Method registerEventMethod = pluginManager.getClass().getMethod(
                "registerEvent",
                Class.class,
                Listener.class,
                org.bukkit.event.EventPriority.class,
                org.bukkit.plugin.EventExecutor.class,
                Plugin.class,
                boolean.class
            );

            // Create an event executor that will call our handler
            Object executor = java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] { org.bukkit.plugin.EventExecutor.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("execute")) {
                        Object event = args[1];
                        if (flagEventClass.isInstance(event)) {
                            handleGrimFlag(event);
                        }
                    }
                    return null;
                }
            );

            // Register the event
            registerEventMethod.invoke(
                pluginManager,
                flagEventClass,
                this,
                org.bukkit.event.EventPriority.NORMAL,
                executor,
                plugin,
                false
            );

            registered = true;
            plugin.getLogger().info("GrimAC integration enabled successfully!");

        } catch (ClassNotFoundException e) {
            plugin.getLogger().warning("GrimAC classes not found. Make sure GrimAC is installed on the server.");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to initialize GrimAC integration: " + e.getMessage());
            if (plugin.getConfig().getBoolean("debug", false)) {
                e.printStackTrace();
            }
        }
    }

    private void handleGrimFlag(Object flagEvent) {
        try {
            // Get the player from the event using reflection
            Method getPlayerMethod = flagEvent.getClass().getMethod("getPlayer");
            Object grimUser = getPlayerMethod.invoke(flagEvent);
            
            // Get the Bukkit player
            Method getBukkitPlayerMethod = grimUser.getClass().getMethod("getPlayer");
            Object bukkitPlayer = getBukkitPlayerMethod.invoke(grimUser);
            
            if (!(bukkitPlayer instanceof Player)) {
                return;
            }
            
            Player player = (Player) bukkitPlayer;
            
            // Get the check from the event
            Method getCheckMethod = flagEvent.getClass().getMethod("getCheck");
            Object check = getCheckMethod.invoke(flagEvent);
            
            // Get the check name
            Method getCheckNameMethod = check.getClass().getMethod("getCheckName");
            String checkName = ((String) getCheckNameMethod.invoke(check)).toUpperCase();

            // Convert Grim violation type to our format
            String violationType = "GRIM_" + checkName;
            
            // Get violation level from configuration
            int level = violationLevels.getOrDefault(checkName, 1);
            
            // Process violation
            antiCheat.processViolation(player, violationType, level);
            
            // Log violation if debug is enabled
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("Grim violation detected: " + player.getName() + " - " + checkName);
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error handling GrimAC flag: " + e.getMessage());
            if (plugin.getConfig().getBoolean("debug", false)) {
                e.printStackTrace();
            }
        }
    }

    public void unregister() {
        if (registered) {
            try {
                // Unregister all listeners for this class
                HandlerList.unregisterAll(this);
                registered = false;
                plugin.getLogger().info("GrimAC integration disabled");
            } catch (Exception e) {
                plugin.getLogger().warning("Error unregistering GrimAC integration: " + e.getMessage());
            }
        }
    }
}
