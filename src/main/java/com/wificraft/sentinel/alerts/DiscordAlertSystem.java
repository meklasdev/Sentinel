package com.wificraft.sentinel.alerts;

import com.wiscraft.sentinel.modules.security.AlertManager.SeverityLevel;
import com.wificraft.sentinel.WiFiCraftSentinel;
import com.wificraft.sentinel.config.SecurityConfig;
import okhttp3.*;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.WebhookMessage;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageAction;


public class DiscordAlertSystem {
    private final WiFiCraftSentinel plugin;
    private final SecurityConfig config;
    private final OkHttpClient httpClient;
    private String webhookUrl;

    public DiscordAlertSystem(WiFiCraftSentinel plugin) {
        this.plugin = plugin;
        this.config = new SecurityConfig(plugin.getConfig());
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
        initialize();
    }

    private void initialize() {
        this.webhookUrl = config.getString("alerts.discord.webhook-url");
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            plugin.getLogger().warning("Brak webhook URL dla Discord - system alertów Discord nie będzie działał!");
        } else {
            plugin.getLogger().info("Zainicjowano system alertów Discord");
        }
    }

    public void sendAlert(String title, String description, SeverityLevel severity) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            return;
        }
        
        int color;
        switch (severity) {
            case CRITICAL: color = 0xff0000; break; // Red
            case HIGH: color = 0xff4500; break; // OrangeRed
            case MEDIUM: color = 0xffa500; break; // Orange
            case LOW: color = 0xffff00; break; // Yellow
            case INFO: color = 0x00ff00; break; // Green
            default: color = 0x3498db; // Blue
        }
        
        try {
            // Create JSON payload
            JSONObject json = new JSONObject();
            JSONObject embed = new JSONObject();
            JSONObject footer = new JSONObject();
            
            // Set embed properties
            embed.put("title", title);
            embed.put("description", description);
            embed.put("color", color);
            
            // Set footer with timestamp
            footer.put("text", "Sentinel Alert System • " + System.currentTimeMillis());
            embed.put("footer", footer);
            
            // Add embed to the main JSON
            json.put("embeds", new org.json.JSONArray().put(embed));
            
            // Set username and avatar
            json.put("username", "Sentinel Alert System");
            json.put("avatar_url", "https://i.imgur.com/4M34hi2.png");

            // Create request
            RequestBody body = RequestBody.create(
                json.toString(),
                okhttp3.MediaType.parse("application/json; charset=utf-8")
            );
            
            Request request = new Request.Builder()
                .url(webhookUrl)
                .post(body)
                .build();

            // Send request asynchronously
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    plugin.getLogger().warning("Nie udało się wysłać alertu Discord: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try (ResponseBody responseBody = response.body()) {
                        if (!response.isSuccessful()) {
                            plugin.getLogger().warning("Błąd odpowiedzi z webhook Discord: " + 
                                response.code() + " - " + (responseBody != null ? responseBody.string() : ""));
                        } else {
                            plugin.getLogger().info("Wysłano alert Discord: " + title);
                        }
                    }
                }
            });
        } catch (Exception e) {
            plugin.getLogger().warning("Błąd podczas tworzenia powiadomienia Discord: " + e.getMessage());
        }
    }

    
    public void shutdown() {
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
            try {
                if (!httpClient.cache().isClosed()) {
                    httpClient.cache().close();
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Błąd podczas zamykania cache HTTP: " + e.getMessage());
            }
        }
    }

    public enum AlertSeverity {
        LOW(0x00FF00),  // Green
        MEDIUM(0xFFFF00), // Yellow
        HIGH(0xFFA500),  // Orange
        CRITICAL(0xFF0000); // Red

        private final int color;

        AlertSeverity(int color) {
            this.color = color;
        }

        public int getColor() {
            return color;
        }
    }
}
