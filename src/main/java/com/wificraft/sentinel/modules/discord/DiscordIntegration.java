package com.wificraft.sentinel.modules.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import com.wificraft.sentinel.gui.Inspection;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.bukkit.configuration.file.YamlConfiguration;
import com.wificraft.sentinel.WiFiCraftSentinel;
import com.wificraft.sentinel.gui.ClientInspectorGUI;
import com.wificraft.sentinel.modules.config.InspectionConfig;
import com.wificraft.sentinel.modules.data.ActivityAnalyzer;
import java.util.concurrent.ConcurrentHashMap;

public class DiscordIntegration {
    private final JavaPlugin plugin;
    private JDA jda;
    private TextChannel helpChannel;
    private final Map<String, String> playerDiscordLinks;
    private TextChannel generalChannel;
    private TextChannel callChannel;
    private TextChannel inspectionChannel;
    private TextChannel warningChannel;
    private final ActivityAnalyzer activityAnalyzer;
    private final Map<UUID, Long> lastActivityTimes = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> loginCounts = new ConcurrentHashMap<>();
    private final Map<UUID, Long> sessionDurations = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> inspectionCounts = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> banCounts = new ConcurrentHashMap<>();
    private final Map<UUID, Long> idleTimes = new ConcurrentHashMap<>();
    private InspectionConfig inspectionConfig;
    
    public DiscordIntegration(JavaPlugin plugin) {
        this.plugin = plugin;
        this.playerDiscordLinks = new HashMap<>();
        this.activityAnalyzer = new ActivityAnalyzer();
        this.inspectionConfig = new InspectionConfig(plugin.getDataFolder());
        loadDiscordLinks();
        initializeJDA();
    }
    
    private void loadDiscordLinks() {
        File discordLinksFile = new File(plugin.getDataFolder(), "player_discord.cfg");
        if (!discordLinksFile.exists()) return;
        
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(discordLinksFile);
            for (String uuid : config.getConfigurationSection("players").getKeys(false)) {
                String discordId = config.getString("players." + uuid);
                if (discordId != null) {
                    playerDiscordLinks.put(uuid, discordId);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading Discord links: " + e.getMessage());
        }
    }
    
    private void initializeJDA() {
        String token = plugin.getConfig().getString("discord.token");
        
        if (token == null) {
            plugin.getLogger().warning("Discord token not configured!");
            return;
        }
        
        try {
            jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.GUILD_MESSAGES)
                .build()
                .awaitReady();
            
            // Load channels from config
            InspectionConfig inspectionConfig = new InspectionConfig(plugin.getDataFolder());
            
            // Get channel IDs from config
            String generalChannelId = inspectionConfig.getDiscordChannelId();
            String callChannelId = inspectionConfig.getDiscordCallChannelId();
            String inspectionChannelId = inspectionConfig.getDiscordInspectionChannelId();
            String warningChannelId = inspectionConfig.getDiscordWarningChannelId();
            
            // Initialize channels if IDs are provided
            generalChannel = generalChannelId != null && !generalChannelId.isEmpty() ? 
                jda.getTextChannelById(generalChannelId) : null;
                
            callChannel = callChannelId != null && !callChannelId.isEmpty() ? 
                jda.getTextChannelById(callChannelId) : null;
                
            inspectionChannel = inspectionChannelId != null && !inspectionChannelId.isEmpty() ? 
                jda.getTextChannelById(inspectionChannelId) : null;
                
            warningChannel = warningChannelId != null && !warningChannelId.isEmpty() ? 
                jda.getTextChannelById(warningChannelId) : null;
            
            if (generalChannel == null) {
                plugin.getLogger().warning("Could not find general channel");
            }
            if (callChannel == null) {
                plugin.getLogger().warning("Could not find call channel");
            }
            if (inspectionChannel == null) {
                plugin.getLogger().warning("Could not find inspection channel");
            }
            if (warningChannel == null) {
                plugin.getLogger().warning("Could not find warning channel");
            }
            
            // Start activity monitoring
            Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                checkActivity();
            }, 20 * 60, 20 * 60);
            
            // Start warning monitoring
            Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                checkWarnings();
            }, 20 * 60, 20 * 60);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize Discord: " + e.getMessage());
        }
    }
    
    public void callToHelp(Player target, String reason) {
        if (callChannel == null) return;
        
        String discordId = playerDiscordLinks.get(target.getUniqueId().toString());
        if (discordId == null) {
            plugin.getLogger().info("No Discord link found for player: " + target.getName());
            return;
        }
        
        User user = jda.getUserById(discordId);
        if (user == null) {
            plugin.getLogger().info("Could not find Discord user: " + discordId);
            return;
        }
        
        // Track activity
        activityAnalyzer.onPlayerMovement(target);
        
        callChannel.sendMessage("**[UWAGA: WEZWANIE DO KANALU POMOCY]**\n" +
            "Gracz: **" + target.getName() + "**\n" +
            "Powód: **" + reason + "**\n" +
            "Proszę odwiedzić serwer i pomóc moderować!\n" +
            "<@" + discordId + ">")
            .queue();
    }
    
    public void sendInspectionNotification(Player target, Player moderator, String result) {
        if (inspectionChannel == null) return;
        
        activityAnalyzer.onInspectionStart(target);
        
        // Get activity stats
        Map<String, Object> stats = activityAnalyzer.getActivityStats(target);
        int loginCount = (int) stats.get("login_count");
        long sessionDuration = (long) stats.get("session_duration");
        int inspectionCount = (int) stats.get("inspection_count");
        int banCount = (int) stats.get("ban_count");
        long idleTime = (long) stats.get("idle_time");
        
        stats.put("loginCount", loginCount);
        stats.put("sessionDuration", sessionDuration);
        stats.put("inspectionCount", inspectionCount);
        stats.put("banCount", banCount);
        stats.put("idleTime", idleTime);
        
        inspectionChannel.sendMessage("**[ZAKOŃCZONO INSPEKCJĘ]**\n" +
            "Gracz: **" + target.getName() + "**\n" +
            "Moderator: **" + moderator.getName() + "**\n" +
            "Wynik: **" + result + "**\n" +
            "Czas: **" + formatDuration(System.currentTimeMillis() - target.getFirstPlayed()) + "**\n" +
            "Statystyki: \n" +
            "Logowania: **" + stats.get("loginCount") + "**\n" +
            "Inspekcje: **" + stats.get("inspectionCount") + "**\n" +
            "Banów: **" + stats.get("banCount") + "**")
            .queue();
    }
    
    private String formatDuration(long duration) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) % 60;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration) % 60;
        long hours = TimeUnit.MILLISECONDS.toHours(duration);
        
        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("s");
        
        return sb.toString().trim();
    }
    
    private void checkWarnings() {
        // Check for long-running inspections
        for (Map.Entry<UUID, Inspection> entry : ClientInspectorGUI.getActiveInspections().entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                long duration = entry.getValue().getDuration();
                if (duration > inspectionConfig.getMaxInspectionDurationMinutes() * 60 * 1000) {
                    sendWarning(player, "Inspekcja trwa już " + formatDuration(duration) + 
                        " - dłużej niż maksymalny czas " + 
                        inspectionConfig.getMaxInspectionDurationMinutes() + " minutes");
                }
            }
        }
        
        // Check for suspicious login patterns
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            
            // Check login frequency
            int logins = loginCounts.getOrDefault(uuid, 0);
            
            if (logins > inspectionConfig.getMaxLoginsPerHour()) {
                sendWarning(player, "Zbyt wiele logowań w ciągu ostatniej godziny");
            }
        }
    }
    
    private void sendWarning(Player player, String reason) {
        if (warningChannel == null) return;
        
        String discordId = playerDiscordLinks.get(player.getUniqueId().toString());
        if (discordId == null) {
            plugin.getLogger().info("No Discord link found for player: " + player.getName());
            return;
        }
        
        User user = jda.getUserById(discordId);
        if (user == null) {
            plugin.getLogger().info("Could not find Discord user: " + discordId);
            return;
        }
        
        // Get activity stats
        Map<String, Object> stats = activityAnalyzer.getActivityStats(player);
        int loginCount = (int) stats.get("login_count");
        long sessionDuration = (long) stats.get("session_duration");
        int inspectionCount = (int) stats.get("inspection_count");
        int banCount = (int) stats.get("ban_count");
        long idleTime = (long) stats.get("idle_time");
        
        stats.put("loginCount", loginCount);
        stats.put("sessionDuration", sessionDuration);
        stats.put("inspectionCount", inspectionCount);
        stats.put("banCount", banCount);
        stats.put("idleTime", idleTime);
        
        warningChannel.sendMessage("**[OSTRZEŻENIE: SUSPECTED ACTIVITY]**\n" +
            "Gracz: **" + player.getName() + "**\n" +
            "Powód: **" + reason + "**\n" +
            "Statystyki:\n" +
            "Logowania: **" + stats.get("loginCount") + "**\n" +
            "Inspekcje: **" + stats.get("inspectionCount") + "**\n" +
            "Banów: **" + stats.get("banCount") + "**\n" +
            "Czas nieaktywności: **" + formatDuration((long) stats.get("idleTime")) + "**\n" +
            "Sesja: **" + formatDuration((long) stats.get("sessionDuration")) + "**\n" +
            "<@" + discordId + ">")
            .queue();
    }
    
    private void checkActivity() {
        long currentTime = System.currentTimeMillis();
        
        // Check all online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            
            // Check idle time
            long lastActivity = lastActivityTimes.getOrDefault(uuid, currentTime);
            long idleTime = currentTime - lastActivity;
            idleTimes.compute(uuid, (k, v) -> v == null ? idleTime : v + idleTime);
            
            // Check login frequency
            int logins = loginCounts.getOrDefault(uuid, 0);
            long sessionDuration = sessionDurations.getOrDefault(uuid, 0L);
            
            // Check inspection frequency
            int inspections = inspectionCounts.getOrDefault(uuid, 0);
            
            // Check ban frequency
            int bans = banCounts.getOrDefault(uuid, 0);
            
            // Track activity
            activityAnalyzer.onPlayerMovement(player);
            
            // Check for warnings
            if (isSuspiciousActivity(uuid, logins, sessionDuration, idleTime, inspections, bans)) {
                sendWarning(player, "Suspicious activity detected: Multiple logins or long session");
            }
        }
    }
    
    private boolean isSuspiciousActivity(UUID playerId, int logins, long sessionDuration, 
                                         long idleTime, int inspections, int bans) {
        // Check login frequency (more than 5 logins per hour)
        if (logins > 5) {
            return true;
        }
        
        // Check session duration (more than 12 hours)
        if (sessionDuration > TimeUnit.HOURS.toMillis(12)) {
            return true;
        }
        
        // Check idle time (more than 30 minutes)
        if (idleTime > TimeUnit.MINUTES.toMillis(30)) {
            return true;
        }
        
        // Check inspection count (more than 5 inspections)
        if (inspections > 5) {
            return true;
        }
        
        // Check ban count (more than 3 bans)
        if (bans > 3) {
            return true;
        }
        
        return false;
    }
    
    public void shutdown() {
        if (jda != null) {
            jda.shutdown();
        }
        
        // Clear activity data
        activityAnalyzer.shutdown();
    }
    
    public void onPlayerMovement(Player player) {
        // Update last activity time in activity analyzer
        activityAnalyzer.updateActivity(player);
        lastActivityTimes.put(player.getUniqueId(), System.currentTimeMillis());
    }
    
    public void logInspectionStart(Player target, Player moderator, String reason) {
        if (inspectionChannel == null) return;
        
        String message = String.format("**Inspection Started**\n" +
            "**Player:** %s\n" +
            "**Moderator:** %s\n" +
            "**Reason:** %s\n" +
            "**Time:** %s",
            target.getName(),
            moderator.getName(),
            reason,
            new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())
        );
        
        inspectionChannel.sendMessage(message).queue();
    }
    
    public void logInspectionComplete(Player target, Player moderator, String result, long duration) {
        if (inspectionChannel == null) return;
        
        String message = String.format("**Inspection Completed**\n" +
            "**Player:** %s\n" +
            "**Moderator:** %s\n" +
            "**Result:** %s\n" +
            "**Duration:** %s\n" +
            "**Time:** %s",
            target.getName(),
            moderator != null ? moderator.getName() : "Unknown",
            result,
            formatDuration(duration),
            new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())
        );
        
        inspectionChannel.sendMessage(message).queue();
    }
    
    public void logSuspiciousActivity(Player player, String reason) {
        if (warningChannel == null) return;
        
        String message = String.format("**Suspicious Activity Detected**\n" +
            "**Player:** %s\n" +
            "**Reason:** %s\n" +
            "**Time:** %s\n" +
            "**Location:** %s, %s, %s",
            player.getName(),
            reason,
            new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()),
            player.getLocation().getWorld().getName(),
            player.getLocation().getBlockX(),
            player.getLocation().getBlockZ()
        );
        
        warningChannel.sendMessage(message).queue();
    }
    
    public void sendWarningNotification(Player target, Player moderator, String warning) {
        if (warningChannel == null) return;
        
        String message = String.format("**Warning Issued**\n" +
            "**Player:** %s\n" +
            "**Moderator:** %s\n" +
            "**Warning:** %s\n" +
            "**Time:** %s",
            target.getName(),
            moderator != null ? moderator.getName() : "Console",
            warning,
            new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())
        );
        
        warningChannel.sendMessage(message).queue();
        
        // Also send a message to the player if they're online
        if (target.isOnline()) {
            target.sendMessage("§c[OSTRZEŻENIE] " + warning);
        }
    }
}
