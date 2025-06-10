package com.wificraft.sentinel.modules.discord;

import com.wificraft.sentinel.modules.config.NotificationConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageUpdateAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageRetrieveAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageDeleteAction;
import net.dv8tion.jda.api.entities.WebhookClient;
import net.dv8tion.jda.api.entities.WebhookClientBuilder;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.awt.Color;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.regex.Pattern;

class WebhookConfig {
    private final String webhookUrl;
    private final int severityLevel;
    private final boolean enabled;
    private final Color color;

    public WebhookConfig(String webhookUrl, int severityLevel, boolean enabled, Color color) {
        this.webhookUrl = webhookUrl;
        this.severityLevel = severityLevel;
        this.enabled = enabled;
        this.color = color;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public int getSeverityLevel() {
        return severityLevel;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Color getColor() {
        return color;
    }
}

public class DiscordAlertManager {
    private final JDA jda;
    private final Map<String, TextChannel> channels;
    private final NotificationConfig config;
    private final Map<String, Pattern> alertPatterns;
    private final Map<String, Integer> alertCounters;
    private final Map<String, WebhookConfig> webhooks;
    private final Map<String, Color> colorMap;
    
    public DiscordAlertManager(String token, NotificationConfig config) {
        this.config = config;
        this.channels = new ConcurrentHashMap<>();
        this.alertPatterns = new ConcurrentHashMap<>();
        this.alertCounters = new ConcurrentHashMap<>();
        this.webhooks = new ConcurrentHashMap<>();
        this.colorMap = Map.of(
            "RED", Color.RED,
            "ORANGE", Color.ORANGE,
            "YELLOW", Color.YELLOW,
            "GREEN", Color.GREEN,
            "BLUE", Color.BLUE
        );

        // Initialize webhooks from configuration
        // Note: Webhook configuration is now managed through the NotificationConfig directly
        // You can add webhooks programmatically or load them from a different source
        // Example:
        // webhooks.put("default", new WebhookConfig("your-webhook-url", 2, true, Color.RED));

        try {
            this.jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .enableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE)
                .build();
            
            loadChannels();
            loadAlertPatterns();
        } catch (Exception e) {
            System.err.println("[DiscordAlertManager] Nie można połączyć z Discord: " + e.getMessage());
        }
    }
    
    private void loadChannels() {
        if (jda != null) {
            jda.awaitReady();
            jda.getTextChannels().forEach(channel -> {
                String name = channel.getName();
                if (name.equals("alerts") || name.equals("inspections") || name.equals("activity")) {
                    channels.put(name, channel);
                    System.out.println("[DiscordAlertManager] Registered channel: " + name);
                }
            });
        }
    }
    
    private void loadAlertPatterns() {
        String[] patterns = config.getSuspiciousPatterns();
        for (String pattern : patterns) {
            alertPatterns.put(pattern, Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
        }
    }
    
    public void sendAlert(String alertType, Player player, String message, int count) {
        // Get webhook configuration for this alert type
        WebhookConfig webhookConfig = webhooks.get(alertType);
        if (webhookConfig == null) return;
        
        // Format message
        String formattedMessage = String.format("[%s] %s: %s (x%d)", 
            alertType, 
            player != null ? player.getName() : "Console", 
            message, 
            count);
            
        // Send to all webhooks using JDA
        for (TextChannel channel : channels.values()) {
            channel.sendMessage(formattedMessage).queue();
        }
    }
    
    private void sendToWebhook(String webhookUrl, String message, Color color) {
        try {
            // Create HTTP client
            HttpClient client = HttpClient.newHttpClient();
            
            // Create JSON payload
            JSONObject embed = new JSONObject()
                .put("description", message)
                .put("color", color.getRGB() & 0xFFFFFF);
                
            JSONObject payload = new JSONObject()
                .put("content", "")
                .put("embeds", new JSONArray().put(embed))
                .put("username", "Sentinel Alert System")
                .put("avatar_url", "https://i.imgur.com/4M34hi2.png");
            
            // Create request
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();
            
            // Send request asynchronously
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        System.err.println("[DiscordAlertManager] Błąd podczas wysyłania webhooka: " + response.statusCode() + " - " + response.body());
                    }
                })
                .exceptionally(error -> {
                    System.err.println("[DiscordAlertManager] Błąd podczas wysyłania webhooka: " + error.getMessage());
                    return null;
                });
        } catch (Exception e) {
            System.err.println("[DiscordAlertManager] Błąd podczas wysyłania wiadomości do webhooka: " + e.getMessage());
        }
    }
    
    public void sendInspectionAlert(Player target, Player moderator, String result, long duration) {
        String message = String.format(
            "**Inspekcja: **%s\n" +
            "**Moderator: **%s\n" +
            "**Wynik: **%s\n" +
            "**Czas: **%d sekund",
            target.getName(),
            moderator.getName(),
            result,
            duration
        );
        sendAlert("security", null, message, 1); // Wykorzystanie webhooka dla alertów bezpieczeństwa
    }
    
    public void sendActivityAlert(Player player, String type, String details) {
        String message = String.format(
            "**Aktywność: **%s\n" +
            "**Gracz: **%s\n" +
            "**Szczegóły: **%s",
            type,
            player.getName(),
            details
        );
        sendAlert("activity", player, message, 1); // Wykorzystanie webhooka dla aktywności
    }
    
    public void checkForAlerts(Player player, String message) {
        // Check if message matches any alert pattern
        for (Map.Entry<String, Pattern> entry : alertPatterns.entrySet()) {
            if (entry.getValue().matcher(message).find()) {
                // Increment alert counter
                int count = alertCounters.getOrDefault(entry.getKey(), 0) + 1;
                alertCounters.put(entry.getKey(), count);
                
                // Send alert to Discord
                sendAlert(entry.getKey(), player, message, count);
                break;
            }
        }
    }
    
    public void shutdown() {
        jda.shutdown();
        alertCounters.clear();
    }
    
    public void setActivity(String activity) {
        jda.getPresence().setActivity(Activity.playing(activity));
    }
    
    public void updateAlertPatterns() {
        alertPatterns.clear();
        loadAlertPatterns();
    }
}
