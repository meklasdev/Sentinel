package com.wificraft.sentinel.modules.behavior;

import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class BehaviorAnalyzer implements Listener {
    private final Map<String, BehaviorPattern> patterns;
    private final Map<UUID, PlayerBehavior> playerBehaviors;
    private final Map<UUID, List<String>> chatHistory;
    private final Map<UUID, Integer> blockInteractions;
    private final Map<UUID, Long> lastMoveTime;
    private final Map<UUID, Location> lastLocation;

    public BehaviorAnalyzer() {
        patterns = new HashMap<>();
        playerBehaviors = new ConcurrentHashMap<>();
        chatHistory = new ConcurrentHashMap<>();
        blockInteractions = new ConcurrentHashMap<>();
        lastMoveTime = new ConcurrentHashMap<>();
        lastLocation = new ConcurrentHashMap<>();

        initializePatterns();
    }

    private void initializePatterns() {
        // Initialize default patterns
        patterns.put("Movement", new MovementPattern(this, 2.0, 50.0));
        patterns.put("BlockInteraction", new BlockInteractionPattern(this, 50));
        patterns.put("ChatSpam", new ChatSpamPattern(this, 10, 0.8));
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        Location from = event.getFrom();
        Location to = event.getTo();

        lastLocation.put(playerId, to);
        lastMoveTime.put(playerId, System.currentTimeMillis());

        // Calculate speed and distance
        double distance = from.distance(to);
        long timeDiff = System.currentTimeMillis() - lastMoveTime.getOrDefault(playerId, System.currentTimeMillis());
        double speed = distance / (timeDiff / 1000.0);

        // Store behavior data
        PlayerBehavior behavior = playerBehaviors.computeIfAbsent(playerId, k -> new PlayerBehavior());
        behavior.addMovementData(speed, distance);

        // Check patterns
        checkPatterns(player);
    }

    @EventHandler
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        String message = event.getMessage();

        // Update chat history
        List<String> history = chatHistory.computeIfAbsent(playerId, k -> new ArrayList<>());
        history.add(message);
        if (history.size() > 100) {
            history.remove(0);
        }

        // Calculate caps ratio
        int capsCount = message.replaceAll("[^A-Z]", "").length();
        double capsRatio = (double) capsCount / message.length();

        // Store behavior data
        PlayerBehavior behavior = playerBehaviors.computeIfAbsent(playerId, k -> new PlayerBehavior());
        behavior.addChatData(message, capsRatio);

        // Check patterns
        checkPatterns(player);
    }

    @EventHandler
    public void onBlockInteraction(BlockBreakEvent event) {
        handleBlockInteraction(event.getPlayer());
    }

    @EventHandler
    public void onBlockInteraction(BlockPlaceEvent event) {
        handleBlockInteraction(event.getPlayer());
    }

    private void handleBlockInteraction(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Update block interaction count
        blockInteractions.merge(playerId, 1, Integer::sum);
        
        // Store behavior data
        PlayerBehavior behavior = playerBehaviors.computeIfAbsent(playerId, k -> new PlayerBehavior());
        behavior.addBlockInteraction();

        // Check patterns
        checkPatterns(player);
    }

    private void checkPatterns(Player player) {
        for (BehaviorPattern pattern : patterns.values()) {
            if (pattern.matches(player)) {
                // Generate alert if pattern matches
                String alertMessage = pattern.getAlertMessage();
                int severity = pattern.getSeverity();
                
                // Send alert through the alert system
                sendAlert(player, alertMessage, severity);
            }
        }
    }

    private void sendAlert(Player player, String message, int severity) {
        // Implementation of alert sending would go here
        // This would typically use the AlertManager
    }

    public double getMovementSpeed(Player player) {
        PlayerBehavior behavior = playerBehaviors.get(player.getUniqueId());
        return behavior != null ? behavior.getAverageSpeed() : 0.0;
    }

    public double getMovementDistance(Player player) {
        PlayerBehavior behavior = playerBehaviors.get(player.getUniqueId());
        return behavior != null ? behavior.getTotalDistance() : 0.0;
    }

    public int getBlockInteractionRate(Player player) {
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        
        // Calculate interactions per second
        int interactions = blockInteractions.getOrDefault(playerId, 0);
        long timeDiff = now - lastMoveTime.getOrDefault(playerId, now);
        
        return (int) (interactions / (timeDiff / 1000.0));
    }

    public double getChatCapsRatio(Player player) {
        PlayerBehavior behavior = playerBehaviors.get(player.getUniqueId());
        return behavior != null ? behavior.getAverageCapsRatio() : 0.0;
    }

    public int getChatMessageRate(Player player) {
        PlayerBehavior behavior = playerBehaviors.get(player.getUniqueId());
        return behavior != null ? behavior.getMessagesPerMinute() : 0;
    }

    private class PlayerBehavior {
        private final List<Double> speeds;
        private final List<Double> distances;
        private final List<Double> capsRatios;
        private int totalInteractions;
        private long lastInteractionTime;
        private int messageCount;
        private long lastMessageTime;

        public PlayerBehavior() {
            speeds = new ArrayList<>();
            distances = new ArrayList<>();
            capsRatios = new ArrayList<>();
            lastInteractionTime = System.currentTimeMillis();
            lastMessageTime = System.currentTimeMillis();
        }

        public void addMovementData(double speed, double distance) {
            speeds.add(speed);
            distances.add(distance);
            cleanOldData();
        }

        public void addChatData(String message, double capsRatio) {
            capsRatios.add(capsRatio);
            messageCount++;
            lastMessageTime = System.currentTimeMillis();
            cleanOldData();
        }

        public void addBlockInteraction() {
            totalInteractions++;
            lastInteractionTime = System.currentTimeMillis();
            cleanOldData();
        }

        private void cleanOldData() {
            long now = System.currentTimeMillis();
            
            // Remove old speed/distance data (keep last 10 seconds)
            speeds.removeIf(s -> now - lastInteractionTime > 10000);
            distances.removeIf(d -> now - lastInteractionTime > 10000);
            
            // Remove old caps ratio data (keep last minute)
            capsRatios.removeIf(r -> now - lastMessageTime > 60000);
        }

        public double getAverageSpeed() {
            return speeds.isEmpty() ? 0.0 : speeds.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        }

        public double getTotalDistance() {
            return distances.isEmpty() ? 0.0 : distances.stream().mapToDouble(Double::doubleValue).sum();
        }

        public double getAverageCapsRatio() {
            return capsRatios.isEmpty() ? 0.0 : capsRatios.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        }

        public int getMessagesPerMinute() {
            long timeDiff = System.currentTimeMillis() - lastMessageTime;
            return (int) (messageCount / (timeDiff / 60000.0));
        }
    }
}
