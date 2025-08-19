package com.wificraft.sentinel.modules.anticheat;

import com.wificraft.sentinel.WiFiCraftSentinel;
import com.wificraft.sentinel.modules.anticheat.grim.GrimIntegration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import org.bukkit.entity.Player;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AntiCheatIntegration {
    private final Map<String, AntiCheatSystem> activeSystems;
    private final Map<UUID, Map<String, ViolationData>> playerViolations;
    private final Map<String, String> bannedCheats;
    private final Map<String, String> suspiciousCheats;
    private final WiFiCraftSentinel plugin;
    private GrimIntegration grimIntegration;

    public AntiCheatIntegration(WiFiCraftSentinel plugin) {
        this.plugin = plugin;
        this.activeSystems = new ConcurrentHashMap<>();
        this.playerViolations = new ConcurrentHashMap<>();
        this.bannedCheats = new HashMap<>();
        this.suspiciousCheats = new HashMap<>();
        this.grimIntegration = null;

        initializeBannedCheats();
        initializeSuspiciousCheats();
    }

    private void initializeBannedCheats() {

        // Grim banned cheats
        bannedCheats.put("GRIM_SPEED", "Speed hack");
        bannedCheats.put("GRIM_REACH", "Reach hack");
        bannedCheats.put("GRIM_FLY", "Fly hack");
        bannedCheats.put("GRIM_WALLCLIMB", "Wall climb");
    }

    private void initializeSuspiciousCheats() {

        // Grim suspicious cheats
        suspiciousCheats.put("GRIM_SPEED", "Speed hack");
        suspiciousCheats.put("GRIM_REACH", "Reach hack");
        suspiciousCheats.put("GRIM_FLY", "Fly hack");
        suspiciousCheats.put("GRIM_WALLCLIMB", "Wall climb");
    }

    public void registerAntiCheat(AntiCheatSystem system) {
        activeSystems.put(system.getName(), system);
    }

    public void registerGrimIntegration() {
        if (grimIntegration != null) {
            grimIntegration.unregister();
            grimIntegration = null;
        }

        try {
            // Try to load GrimIntegration class using reflection
            Class<?> grimIntegrationClass = Class.forName("com.wificraft.sentinel.modules.anticheat.grim.GrimIntegration");
            grimIntegration = (GrimIntegration) grimIntegrationClass.getConstructor(WiFiCraftSentinel.class).newInstance(plugin);
            
            // Call register method using reflection
            grimIntegrationClass.getMethod("register").invoke(grimIntegration);
            
            // Register Grim as an active anti-cheat system
            registerAntiCheat(new AntiCheatSystem("GrimAC", "2.0.0", "Grim Team"));
            plugin.getLogger().info("Successfully registered GrimAC integration");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().warning("GrimAC integration classes not found. Make sure GrimAC is installed on the server.");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to initialize GrimAC integration: " + e.getMessage());
            if (plugin.getConfig().getBoolean("debug", false)) {
                e.printStackTrace();
            }
        }
    }

    public void unregisterGrimIntegration() {
        if (grimIntegration != null) {
            grimIntegration.unregister();
            grimIntegration = null;
        }
    }



    public void unregisterAntiCheat(String systemName) {
        activeSystems.remove(systemName);
    }

    public void processViolation(Player player, String cheatType, int violationLevel) {
        UUID playerId = player.getUniqueId();
        Map<String, ViolationData> violations = playerViolations.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        
        ViolationData data = violations.get(cheatType);
        if (data == null) {
            data = new ViolationData();
            violations.put(cheatType, data);
        }
        
        data.incrementViolation(violationLevel);
        
        // Check if violation is banned
        if (bannedCheats.containsKey(cheatType)) {
            if (data.getViolationLevel() >= 5) {
                banPlayer(player, cheatType);
            } else if (data.getViolationLevel() >= 3) {
                sendWarning(player, cheatType);
            }
        }
    }

    private void banPlayer(Player player, String cheatType) {
        String reason = bannedCheats.get(cheatType);
        player.kickPlayer("§cZostałeś zbanowany za użycie: " + reason);
        // Add to ban list
        // Log the ban
    }

    private void sendWarning(Player player, String cheatType) {
        String reason = suspiciousCheats.get(cheatType);
        player.sendMessage("§cOSTRZEŻENIE: Wykryto podejrzane zachowanie!");
        player.sendMessage("§7Przyczyna: " + reason);
        player.sendMessage("§7Jeśli to nie jest prawda, skontaktuj się z administracją.");
    }

    public String getViolationAnalysis(Player player) {
        UUID playerId = player.getUniqueId();
        Map<String, ViolationData> violations = playerViolations.get(playerId);
        
        if (violations == null || violations.isEmpty()) {
            return "§aBrak wykrytych naruszeń!";
        }

        StringBuilder analysis = new StringBuilder();
        analysis.append("§6Analiza naruszeń:");
        
        for (Map.Entry<String, ViolationData> entry : violations.entrySet()) {
            String cheatType = entry.getKey();
            ViolationData data = entry.getValue();
            
            String reason = bannedCheats.getOrDefault(cheatType, 
                suspiciousCheats.getOrDefault(cheatType, "Nieznane cheat"));
                
            analysis.append("\n§7").append(reason)
                   .append(" (Poziom: ").append(data.getViolationLevel())
                   .append("/5)");
        }
        
        return analysis.toString();
    }

    public static class AntiCheatSystem {
        private final String name;
        private final String version;
        private final String author;

        public AntiCheatSystem(String name, String version, String author) {
            this.name = name;
            this.version = version;
            this.author = author;
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }

        public String getAuthor() {
            return author;
        }
    }

    public static class ViolationData {
        private int violationLevel;
        private long lastViolation;
        private int violationCount;

        public void incrementViolation(int level) {
            violationLevel += level;
            if (violationLevel > 5) violationLevel = 5;
            lastViolation = System.currentTimeMillis();
            violationCount++;
        }

        public int getViolationLevel() {
            return violationLevel;
        }

        public long getLastViolation() {
            return lastViolation;
        }

        public int getViolationCount() {
            return violationCount;
        }
    }
}
