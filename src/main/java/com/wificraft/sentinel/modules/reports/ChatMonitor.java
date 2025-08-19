package com.wificraft.sentinel.modules.reports;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChatTabCompleteEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatMonitor implements Listener {
    private final ReportManager reportManager;
    private final Map<UUID, Long> lastMessageTimes;
    private final Map<UUID, Integer> messageCount;
    private final Map<UUID, Long> lastCapsTimes;
    private final Map<UUID, Integer> capsCount;
    
    public ChatMonitor(ReportManager reportManager) {
        this.reportManager = reportManager;
        this.lastMessageTimes = new HashMap<>();
        this.messageCount = new HashMap<>();
        this.lastCapsTimes = new HashMap<>();
        this.capsCount = new HashMap<>();
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Track message frequency
        trackMessageFrequency(playerId);
        
        // Check for excessive caps
        checkForCaps(event, player);
        
        // Check for banned words
        checkForBannedWords(event, player);
        
        // Check for spam
        checkForSpam(event, player);
    }

    private void trackMessageFrequency(UUID playerId) {
        long currentTime = System.currentTimeMillis();
        long lastTime = lastMessageTimes.getOrDefault(playerId, 0L);
        int count = messageCount.getOrDefault(playerId, 0);
        
        if (currentTime - lastTime < 2000) { // Less than 2 seconds between messages
            messageCount.put(playerId, count + 1);
            if (count >= 5) { // More than 5 messages in quick succession
                reportManager.createReport(null, playerId, "Potential spam detected");
            }
        } else {
            messageCount.put(playerId, 1);
        }
        lastMessageTimes.put(playerId, currentTime);
    }

    private void checkForCaps(AsyncPlayerChatEvent event, Player player) {
        String message = event.getMessage();
        int capsCharCount = (int) message.chars()
                .filter(Character::isUpperCase)
                .count();
        
        double capsRatio = (double) capsCharCount / message.length();
        
        if (capsRatio > 0.7) { // More than 70% of message is caps
            long currentTime = System.currentTimeMillis();
            long lastTime = lastCapsTimes.getOrDefault(player.getUniqueId(), 0L);
            int count = this.capsCount.getOrDefault(player.getUniqueId(), 0);
            
            if (currentTime - lastTime < 60000) { // Less than 1 minute between caps
                this.capsCount.put(player.getUniqueId(), count + 1);
                if (count >= 3) { // More than 3 all-caps messages in a minute
                    reportManager.createReport(null, player.getUniqueId(), "Excessive use of all-caps");
                }
            } else {
                this.capsCount.put(player.getUniqueId(), 1);
            }
            lastCapsTimes.put(player.getUniqueId(), currentTime);
        }
    }

    private void checkForBannedWords(AsyncPlayerChatEvent event, Player player) {
        String message = event.getMessage().toLowerCase();
        String[] bannedWords = {
            "hack", "cheat", "exploit", "bug", "admin", "mod", "staff",
            "pay", "buy", "sell", "trade", "ip", "server", "cracked"
        };
        
        for (String word : bannedWords) {
            if (message.contains(word)) {
                reportManager.createReport(null, player.getUniqueId(), 
                    "Suspicious chat containing potential rule violation");
                break;
            }
        }
    }

    private void checkForSpam(AsyncPlayerChatEvent event, Player player) {
        String message = event.getMessage();
        if (message.trim().length() < 3) { // Very short messages
            reportManager.createReport(null, player.getUniqueId(), "Potential spam (very short message)");
        }
        
        if (message.matches(".*[0-9]+.*")) { // Messages containing numbers
            reportManager.createReport(null, player.getUniqueId(), "Potential spam (contains numbers)");
        }
    }

    @EventHandler
    public void onTabComplete(PlayerChatTabCompleteEvent event) {
        String[] tokens = event.getBuffer().toLowerCase().split(" ");
        if (tokens.length == 0) {
            return;
        }
        
        String lastToken = tokens[tokens.length - 1];
        
        // Check for suspicious tab completions
        if (lastToken.length() > 15) {
            reportManager.createReport(null, event.getPlayer().getUniqueId(), 
                "Suspiciously long tab completion");
        }
    }
}
