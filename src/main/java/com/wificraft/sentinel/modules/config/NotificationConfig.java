package com.wificraft.sentinel.modules.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class NotificationConfig {
    private final FileConfiguration config;
    private final File configFile;
    private final Map<UUID, List<Map<String, Object>>> playerNotifications = new HashMap<>();
    private final Map<UUID, PlayerNotificationSettings> playerSettings = new HashMap<>();

    public static class PlayerNotificationSettings {
        private boolean soundEnabled = true;
        private boolean chatEnabled = true;

        public boolean isSoundEnabled() { return soundEnabled; }
        public void setSoundEnabled(boolean soundEnabled) { this.soundEnabled = soundEnabled; }
        public boolean isChatEnabled() { return chatEnabled; }
        public void setChatEnabled(boolean chatEnabled) { this.chatEnabled = chatEnabled; }
    }

    public NotificationConfig(File dataFolder) {
        this.configFile = new File(dataFolder, "notifications.yml");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);

        // Default values
        config.addDefault("thresholds.inspections.daily", 5);
        config.addDefault("thresholds.inspections.hourly", 3);
        config.addDefault("thresholds.activity.idle", 1800); // 30 minutes
        config.addDefault("thresholds.activity.logins.hourly", 5);
        config.addDefault("thresholds.activity.logins.daily", 20);
        config.addDefault("patterns.suspicious", Arrays.asList("^.*hack.*$", "^.*cheat.*$", "^.*exploit.*$", "^.*bot.*$"));
        config.options().copyDefaults(true);
        
        saveConfig();
        loadPlayerSettings();
    }

    public void addNotification(UUID playerId, String message) {
        List<Map<String, Object>> notifications = playerNotifications.computeIfAbsent(playerId, k -> new ArrayList<>());
        Map<String, Object> notification = new HashMap<>();
        notification.put("id", notifications.size() + 1);
        notification.put("message", message);
        notification.put("read", false);
        notification.put("timestamp", System.currentTimeMillis());
        notifications.add(notification);
        savePlayerSettings();
    }

    public List<Map<String, Object>> getNotifications(UUID playerId) {
        return playerNotifications.getOrDefault(playerId, Collections.emptyList());
    }

    public Map<String, Object> getNotification(UUID playerId, int notificationId) {
        return getNotifications(playerId).stream()
                .filter(n -> n.get("id") instanceof Integer && (Integer) n.get("id") == notificationId)
                .findFirst()
                .orElse(null);
    }

    public void clearNotifications(UUID playerId) {
        playerNotifications.remove(playerId);
        savePlayerSettings();
    }
    
    public void setSoundEnabled(UUID playerId, boolean enabled) {
        playerSettings.computeIfAbsent(playerId, k -> new PlayerNotificationSettings()).setSoundEnabled(enabled);
        savePlayerSettings();
    }

    public void setChatEnabled(UUID playerId, boolean enabled) {
        playerSettings.computeIfAbsent(playerId, k -> new PlayerNotificationSettings()).setChatEnabled(enabled);
        savePlayerSettings();
    }

    public int getInspectionThreshold(String type) {
        return config.getInt("thresholds.inspections." + type);
    }

    public int getActivityThreshold(String type) {
        return config.getInt("thresholds.activity." + type);
    }

    public List<String> getPatterns() {
        return config.getStringList("patterns.suspicious");
    }

    public String[] getSuspiciousPatterns() {
        return getPatterns().toArray(new String[0]);
    }

    public void setThreshold(String type, int value) {
        if (config.isSet("thresholds.inspections." + type)) {
            config.set("thresholds.inspections." + type, value);
        } else if (config.isSet("thresholds.activity." + type)) {
            config.set("thresholds.activity." + type, value);
        } else {
            // Default or log warning
            config.set("thresholds.activity." + type, value);
        }
        saveConfig();
    }

    public void addPattern(String pattern) {
        List<String> patterns = new ArrayList<>(getPatterns());
        if (!patterns.contains(pattern)) {
            patterns.add(pattern);
            config.set("patterns.suspicious", patterns);
            saveConfig();
        }
    }

    public void removePattern(String pattern) {
        List<String> patterns = new ArrayList<>(getPatterns());
        if (patterns.remove(pattern)) {
            config.set("patterns.suspicious", patterns);
            saveConfig();
        }
    }

    public Map<String, Object> getThresholds() {
        Map<String, Object> allThresholds = new HashMap<>();
        if (config.getConfigurationSection("thresholds") != null) {
            config.getConfigurationSection("thresholds").getValues(true).forEach(allThresholds::put);
        }
        return allThresholds;
    }

    private void loadPlayerSettings() {
        if (config.isConfigurationSection("player-settings")) {
            config.getConfigurationSection("player-settings").getKeys(false).forEach(uuidString -> {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    PlayerNotificationSettings settings = new PlayerNotificationSettings();
                    settings.setSoundEnabled(config.getBoolean("player-settings." + uuidString + ".sound", true));
                    settings.setChatEnabled(config.getBoolean("player-settings." + uuidString + ".chat", true));
                    playerSettings.put(uuid, settings);
                } catch (IllegalArgumentException e) {
                    System.err.println("Skipping invalid UUID in notifications.yml: " + uuidString);
                }
            });
        }
        if (config.isConfigurationSection("player-notifications")) {
            config.getConfigurationSection("player-notifications").getKeys(false).forEach(uuidString -> {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    List<Map<?, ?>> notifications = config.getMapList("player-notifications." + uuidString);
                    List<Map<String, Object>> typedNotifications = new ArrayList<>();
                    notifications.forEach(map -> {
                        Map<String, Object> typedMap = new HashMap<>();
                        map.forEach((key, value) -> typedMap.put(key.toString(), value));
                        typedNotifications.add(typedMap);
                    });
                    playerNotifications.put(uuid, typedNotifications);
                } catch (IllegalArgumentException e) {
                    System.err.println("Skipping invalid UUID in notifications.yml: " + uuidString);
                }
            });
        }
    }

    private void savePlayerSettings() {
        playerSettings.forEach((uuid, settings) -> {
            config.set("player-settings." + uuid.toString() + ".sound", settings.isSoundEnabled());
            config.set("player-settings." + uuid.toString() + ".chat", settings.isChatEnabled());
        });
        playerNotifications.forEach((uuid, notifications) -> {
            config.set("player-notifications." + uuid.toString(), notifications);
        });
        saveConfig();
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            System.err.println("Błąd podczas zapisywania konfiguracji powiadomień: " + e.getMessage());
        }
    }

    public void reloadConfig() {
        try {
            config.load(configFile);
            loadPlayerSettings();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
