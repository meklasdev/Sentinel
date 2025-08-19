package com.wificraft.sentinel.modules.config;

import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.concurrent.TimeUnit;

public class InspectionConfig {
    private final File configFile;
    private final YamlConfiguration config;
    
    public InspectionConfig(File pluginFolder) {
        this.configFile = new File(pluginFolder, "inspection_config.yml");
        this.config = YamlConfiguration.loadConfiguration(configFile);
        saveDefaultConfig();
    }
    
    private void saveDefaultConfig() {
        if (!configFile.exists()) {
            config.set("thresholds.login_frequency", 3); // Max logins per hour before warning
            config.set("thresholds.play_time", 10); // Hours before warning about excessive play time
            config.set("thresholds.inspection_frequency", 2); // Max inspections per week before warning
            config.set("thresholds.ban_frequency", 1); // Max bans per month before warning
            config.set("thresholds.session_duration", 12); // Hours before warning about long session
            config.set("thresholds.idle_time", 30); // Minutes before warning about idle time
            
            config.set("notifications.discord.enabled", true);
            config.set("notifications.discord.channel_id", "");
            config.set("notifications.discord.call_channel_id", "");
            config.set("notifications.discord.inspection_channel_id", "");
            config.set("notifications.discord.warning_channel_id", "");
            
            config.set("inspection.reminder_time", 5); // Minutes before reminder
            config.set("inspection.max_duration", 30); // Minutes before warning about long inspection
            
            try {
                config.save(configFile);
            } catch (Exception e) {
                throw new RuntimeException("Failed to save default config: " + e.getMessage());
            }
        }
    }
    
    public int getMaxLoginsPerHour() {
        return config.getInt("thresholds.login_frequency");
    }
    
    public int getMaxPlayTimeHours() {
        return config.getInt("thresholds.play_time");
    }
    
    public int getMaxInspectionsPerWeek() {
        return config.getInt("thresholds.inspection_frequency");
    }
    
    public int getMaxBansPerMonth() {
        return config.getInt("thresholds.ban_frequency");
    }
    
    public int getMaxSessionDurationHours() {
        return config.getInt("thresholds.session_duration");
    }
    
    public int getMaxIdleTimeMinutes() {
        return config.getInt("thresholds.idle_time");
    }
    
    public boolean isDiscordNotificationsEnabled() {
        return config.getBoolean("notifications.discord.enabled");
    }
    
    public String getDiscordChannelId() {
        return config.getString("notifications.discord.channel_id");
    }
    
    public String getDiscordCallChannelId() {
        return config.getString("notifications.discord.call_channel_id");
    }
    
    public String getDiscordInspectionChannelId() {
        return config.getString("notifications.discord.inspection_channel_id");
    }
    
    public String getDiscordWarningChannelId() {
        return config.getString("notifications.discord.warning_channel_id");
    }
    
    public int getInspectionReminderTime() {
        return config.getInt("inspection.reminder_time");
    }
    
    public int getMaxInspectionDurationMinutes() {
        return config.getInt("inspection.max_duration");
    }
    
    public String formatDuration(long duration) {
        long hours = TimeUnit.MILLISECONDS.toHours(duration);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) % 60;
        
        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("s");
        
        return sb.toString().trim();
    }
    
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save config: " + e.getMessage());
        }
    }
}
