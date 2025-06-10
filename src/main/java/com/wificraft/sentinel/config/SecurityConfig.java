package com.wificraft.sentinel.config;

import org.bukkit.configuration.file.FileConfiguration;
import java.util.HashMap;
import java.util.Map;

public class SecurityConfig {
    // Behavior tracking settings
    public static final String BEHAVIOR_TRACKING_ENABLED = "behavior-tracking.enabled";
    public static final String BEHAVIOR_TRACKING_HISTORY = "behavior-tracking.history-duration";
    public static final String BEHAVIOR_TRACKING_ALERT_THRESHOLD = "behavior-tracking.alert-threshold";
    public static final String BEHAVIOR_TRACKING_MOVEMENT_SPEED_THRESHOLD = "behavior-tracking.movement-speed-threshold";
    public static final String BEHAVIOR_TRACKING_MOVEMENT_DISTANCE_THRESHOLD = "behavior-tracking.movement-distance-threshold";
    public static final String BEHAVIOR_TRACKING_TELEPORT_THRESHOLD = "behavior-tracking.teleport-threshold";
    public static final String BEHAVIOR_TRACKING_BLOCK_SPAM_THRESHOLD = "behavior-tracking.block-spam-threshold";
    public static final String BEHAVIOR_TRACKING_BLOCK_BREAK_THRESHOLD = "behavior-tracking.block-break-threshold";
    public static final String BEHAVIOR_TRACKING_BLOCK_PLACE_THRESHOLD = "behavior-tracking.block-place-threshold";
    public static final String BEHAVIOR_TRACKING_CHAT_SPAM_THRESHOLD = "behavior-tracking.chat-spam-threshold";
    public static final String BEHAVIOR_TRACKING_ALL_CAPS_THRESHOLD = "behavior-tracking.all-caps-threshold";
    public static final String BEHAVIOR_TRACKING_CHAT_FREQUENCY_THRESHOLD = "behavior-tracking.chat-frequency-threshold";
    public static final String BEHAVIOR_TRACKING_TELEPORT_FREQUENCY_THRESHOLD = "behavior-tracking.teleport-frequency-threshold";
    public static final String BEHAVIOR_TRACKING_MOVEMENT_FREQUENCY_THRESHOLD = "behavior-tracking.movement-frequency-threshold";
    
    // Alert system settings
    public static final String ALERTS_ENABLED = "alerts.enabled";
    public static final String ALERTS_SEVERITY_THRESHOLD = "alerts.severity-threshold";
    public static final String ALERTS_DISCORD_WEBHOOK = "alerts.discord-webhook";
    
    // IP analysis settings
    public static final String IP_ANALYSIS_ENABLED = "ip-analysis.enabled";
    public static final String IP_ANALYSIS_VPN_CHECK = "ip-analysis.vpn-check";
    public static final String IP_ANALYSIS_PROXY_CHECK = "ip-analysis.proxy-check";
    
    // Hardware fingerprinting settings
    public static final String HARDWARE_FINGERPRINTING_ENABLED = "hardware-fingerprinting.enabled";
    public static final String HARDWARE_FINGERPRINTING_CACHE_DURATION = "hardware-fingerprinting.cache-duration";
    
    private final FileConfiguration config;
    private final Map<String, Object> defaults;
    
    /**
     * Gets the underlying FileConfiguration
     * @return The FileConfiguration instance
     */
    public FileConfiguration getConfig() {
        return config;
    }
    
    public SecurityConfig(FileConfiguration config) {
        this.config = config;
        this.defaults = new HashMap<>();
        initializeDefaults();
    }
    
    private void initializeDefaults() {
        // Behavior tracking defaults
        defaults.put(BEHAVIOR_TRACKING_ENABLED, true);
        defaults.put(BEHAVIOR_TRACKING_HISTORY, "1h");
        defaults.put(BEHAVIOR_TRACKING_ALERT_THRESHOLD, 3);
        defaults.put(BEHAVIOR_TRACKING_MOVEMENT_SPEED_THRESHOLD, 2.0);
        defaults.put(BEHAVIOR_TRACKING_MOVEMENT_DISTANCE_THRESHOLD, 50.0);
        defaults.put(BEHAVIOR_TRACKING_TELEPORT_THRESHOLD, 1000);
        defaults.put(BEHAVIOR_TRACKING_BLOCK_SPAM_THRESHOLD, 100);
        defaults.put(BEHAVIOR_TRACKING_BLOCK_BREAK_THRESHOLD, 50);
        defaults.put(BEHAVIOR_TRACKING_BLOCK_PLACE_THRESHOLD, 50);
        defaults.put(BEHAVIOR_TRACKING_CHAT_SPAM_THRESHOLD, 10);
        defaults.put(BEHAVIOR_TRACKING_ALL_CAPS_THRESHOLD, 0.8);
        defaults.put(BEHAVIOR_TRACKING_CHAT_FREQUENCY_THRESHOLD, 5);
        defaults.put(BEHAVIOR_TRACKING_TELEPORT_FREQUENCY_THRESHOLD, 3);
        defaults.put(BEHAVIOR_TRACKING_MOVEMENT_FREQUENCY_THRESHOLD, 20);
        
        // Alert system defaults
        defaults.put(ALERTS_ENABLED, true);
        defaults.put(ALERTS_SEVERITY_THRESHOLD, 2);
        
        // IP analysis defaults
        defaults.put(IP_ANALYSIS_ENABLED, true);
        defaults.put(IP_ANALYSIS_VPN_CHECK, true);
        defaults.put(IP_ANALYSIS_PROXY_CHECK, true);
        
        // Hardware fingerprinting defaults
        defaults.put(HARDWARE_FINGERPRINTING_ENABLED, true);
        defaults.put(HARDWARE_FINGERPRINTING_CACHE_DURATION, "24h");
    }
    
    public boolean getBoolean(String path) {
        return config.getBoolean(path, (boolean) defaults.getOrDefault(path, false));
    }
    
    public String getString(String path) {
        return config.getString(path, (String) defaults.getOrDefault(path, ""));
    }
    
    public int getInt(String path) {
        return config.getInt(path, (int) defaults.getOrDefault(path, 0));
    }
    
    public void set(String path, Object value) {
        config.set(path, value);
    }
    
    public void saveDefaults() {
        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            if (config.get(entry.getKey()) == null) {
                config.set(entry.getKey(), entry.getValue());
            }
        }
    }
}
