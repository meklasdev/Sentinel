package com.wificraft.sentinel.modules.security;

import com.wificraft.sentinel.WiFiCraftSentinel;
import com.wificraft.sentinel.alerts.AlertManager;
import com.wificraft.sentinel.alerts.SeverityLevel;
import com.wificraft.sentinel.config.SecurityConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BehaviorAnalyzer implements Listener, IAnalyzer {
    private final AlertManager alertManager;
    private final Map<UUID, List<BehaviorEvent>> playerEvents;
    private final long maxHistoryTime;
    private final double movementSpeedThreshold;
    private final double movementDistanceThreshold;
    private final int movementFrequencyThreshold;
    private final int blockBreakThreshold;
    private final int blockPlaceThreshold;

    public BehaviorAnalyzer(WiFiCraftSentinel plugin) {
        this.alertManager = new AlertManager();
        this.playerEvents = new HashMap<>();
        FileConfiguration config = plugin.getConfig();
        
        // Convert 1h to milliseconds
        String historyDuration = config.getString(SecurityConfig.BEHAVIOR_TRACKING_HISTORY, "1h");
        this.maxHistoryTime = parseDuration(historyDuration);
        
        this.movementSpeedThreshold = config.getDouble(SecurityConfig.BEHAVIOR_TRACKING_MOVEMENT_SPEED_THRESHOLD, 2.0);
        this.movementDistanceThreshold = config.getDouble(SecurityConfig.BEHAVIOR_TRACKING_MOVEMENT_DISTANCE_THRESHOLD, 10.0);
        this.movementFrequencyThreshold = config.getInt(SecurityConfig.BEHAVIOR_TRACKING_MOVEMENT_FREQUENCY_THRESHOLD, 10);
        this.blockBreakThreshold = config.getInt(SecurityConfig.BEHAVIOR_TRACKING_BLOCK_BREAK_THRESHOLD, 10);
        this.blockPlaceThreshold = config.getInt(SecurityConfig.BEHAVIOR_TRACKING_BLOCK_PLACE_THRESHOLD, 10);
    }

    private long parseDuration(String duration) {
        if (duration == null || duration.isEmpty()) {
            return 3600000; // 1 hour in milliseconds
        }
        
        char unit = duration.charAt(duration.length() - 1);
        int value = Integer.parseInt(duration.substring(0, duration.length() - 1));
        
        switch (unit) {
            case 's': return value * 1000;
            case 'm': return value * 60000;
            case 'h': return value * 3600000;
            case 'd': return value * 86400000;
            default: return 3600000; // Default to 1 hour
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Get or create player's event list
        List<BehaviorEvent> events = playerEvents.computeIfAbsent(playerId, k -> new ArrayList<>());
        
        // Add new movement event
        BehaviorEvent newEvent = new BehaviorEvent(
            "movement",
            System.currentTimeMillis(),
            event.getTo().getX() - event.getFrom().getX(),
            event.getTo().getY() - event.getFrom().getY(),
            event.getTo().getZ() - event.getFrom().getZ(),
            0, // Distance will be calculated in analyzeMovementPatterns
            player
        );
        events.add(newEvent);
        
        // Clean up old events
        cleanOldEvents(events);
        
        // Analyze movement patterns if we have enough data
        if (events.size() >= 10) {
            analyzeMovementPatterns(events);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        List<BehaviorEvent> events = playerEvents.computeIfAbsent(playerId, k -> new ArrayList<>());
        
        // Add new event
        events.add(new BehaviorEvent(
            "block_break",
            System.currentTimeMillis(),
            event.getBlock().getLocation().getX(),
            event.getBlock().getLocation().getY(),
            event.getBlock().getLocation().getZ(),
            1, // Distance is 1 for block interactions
            player
        ));
        
        cleanOldEvents(events);
        analyzeBlockPatterns(events);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        List<BehaviorEvent> events = playerEvents.computeIfAbsent(playerId, k -> new ArrayList<>());
        
        // Add new event
        events.add(new BehaviorEvent(
            "block_place",
            System.currentTimeMillis(),
            event.getBlock().getLocation().getX(),
            event.getBlock().getLocation().getY(),
            event.getBlock().getLocation().getZ(),
            1, // Distance is 1 for block interactions
            player
        ));
        
        cleanOldEvents(events);
        analyzeBlockPatterns(events);
    }

    private void cleanOldEvents(List<BehaviorEvent> events) {
        long currentTime = System.currentTimeMillis();
        
        // Remove events older than maxHistoryTime
        events.removeIf(event -> (currentTime - event.getTimestamp()) > maxHistoryTime);
    }

    private void analyzeMovementPatterns(List<BehaviorEvent> events) {
        double movementScore = 0;
        double maxSpeed = 0;
        double maxDistance = 0;
        double avgSpeed = 0;
        int movementCount = 0;
        int rapidDirectionChanges = 0;
        long currentTime = System.currentTimeMillis();
        
        List<BehaviorEvent> recentEvents = events.stream()
            .filter(event -> event.getType().equals("movement") && 
                        (currentTime - event.getTimestamp()) <= 10000)
            .toList();
        
        if (recentEvents.size() < 3) return; // Need at least 3 points for direction analysis
        
        // Calculate movement statistics
        for (int i = 0; i < recentEvents.size() - 1; i++) {
            BehaviorEvent current = recentEvents.get(i);
            BehaviorEvent next = recentEvents.get(i + 1);
            
            if (current.getType().equals("movement")) {
                movementCount++;
                double deltaTime = (next.getTimestamp() - current.getTimestamp()) / 1000.0;
                double speed = Math.sqrt(Math.pow(next.getX() - current.getX(), 2) + 
                                     Math.pow(next.getY() - current.getY(), 2) + 
                                     Math.pow(next.getZ() - current.getZ(), 2)) / deltaTime;
                double distance = Math.sqrt(Math.pow(next.getX() - current.getX(), 2) + 
                                        Math.pow(next.getY() - current.getY(), 2) + 
                                        Math.pow(next.getZ() - current.getZ(), 2));
                
                maxSpeed = Math.max(maxSpeed, speed);
                maxDistance = Math.max(maxDistance, distance);
                avgSpeed += speed;
                
                if (speed > movementSpeedThreshold) {
                    movementScore += 2;
                }
                if (distance > movementDistanceThreshold) {
                    movementScore += 1;
                }
                
                // Check for rapid direction changes
                if (i > 0) {
                    BehaviorEvent prev = recentEvents.get(i - 1);
                    double angleChange = calculateAngleChange(prev, current, next);
                    if (angleChange > 90) { // More than 90 degree turn
                        rapidDirectionChanges++;
                        movementScore += 1;
                    }
                }
            }
        }
        
        if (movementCount > 0) {
            avgSpeed /= movementCount;
        }
        
        if (movementCount > movementFrequencyThreshold) {
            movementScore += 3;
        }
        
        if (rapidDirectionChanges > 2) { // More than 2 rapid direction changes
            movementScore += 4;
        }
        
        movementScore = Math.min(100, movementScore);
        
        if (movementScore >= 70) {
            alertManager.createAlert(
                "Suspicious Movement Pattern",
                String.format("Movement score: %.2f%%, Max Speed: %.2f, Avg Speed: %.2f, Max Distance: %.2f, Movements: %d, Rapid Direction Changes: %d", 
                    movementScore, maxSpeed, avgSpeed, maxDistance, movementCount, rapidDirectionChanges),
                SeverityLevel.YELLOW
            );
        }
    }

    private double calculateAngleChange(BehaviorEvent prev, BehaviorEvent current, BehaviorEvent next) {
        double[] v1 = {
            current.getX() - prev.getX(),
            current.getY() - prev.getY(),
            current.getZ() - prev.getZ()
        };
        double[] v2 = {
            next.getX() - current.getX(),
            next.getY() - current.getY(),
            next.getZ() - current.getZ()
        };
        
        double dotProduct = v1[0] * v2[0] + v1[1] * v2[1] + v1[2] * v2[2];
        double mag1 = Math.sqrt(v1[0] * v1[0] + v1[1] * v1[1] + v1[2] * v1[2]);
        double mag2 = Math.sqrt(v2[0] * v2[0] + v2[1] * v2[1] + v2[2] * v2[2]);
        
        if (mag1 == 0 || mag2 == 0) return 0;
        
        double cosTheta = dotProduct / (mag1 * mag2);
        return Math.toDegrees(Math.acos(Math.min(1, Math.max(-1, cosTheta))));
    }
    @EventHandler
    public void onPlayerChat(org.bukkit.event.player.AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Get or create player's event list
        List<BehaviorEvent> events = playerEvents.computeIfAbsent(playerId, k -> new ArrayList<>());
        
        // Add new chat event
        String message = event.getMessage();
        BehaviorEvent newEvent = new BehaviorEvent("chat", System.currentTimeMillis(), message, player);
        events.add(newEvent);
        
        // Clean up old events
        cleanOldEvents(events);
        
        // Analyze chat patterns
        analyzeChatPatterns(events);
    }

    private void analyzeChatPatterns(List<BehaviorEvent> events) {
        int messageCount = 0;
        int rapidMessages = 0;
        int suspiciousWords = 0;
        long currentTime = System.currentTimeMillis();
        
        List<BehaviorEvent> recentEvents = events.stream()
            .filter(event -> event.getType().equals("chat") && 
                        (currentTime - event.getTimestamp()) <= 10000)
            .toList();

        if (recentEvents.isEmpty()) return;

        // Track suspicious words
        String[] suspiciousWordsList = {
            "hack", "cheat", "bot", "exploit", "admin", "op", "mod", "staff",
            "bug", "glitch", "crash", "lag", "laggy", "slow", "performance",
            "ban", "kick", "mute", "unfair", "complain", "complaint"
        };

        // Analyze chat patterns
        for (int i = 0; i < recentEvents.size(); i++) {
            BehaviorEvent event = recentEvents.get(i);
            messageCount++;
            
            // Check for rapid messages
            if (i > 0) {
                long timeDiff = event.getTimestamp() - recentEvents.get(i - 1).getTimestamp();
                if (timeDiff < 500) { // Less than 500ms between messages
                    rapidMessages++;
                }
            }
            
            // Check for suspicious words
            String message = event.getMessage().toLowerCase();
            for (String word : suspiciousWordsList) {
                if (message.contains(word)) {
                    suspiciousWords++;
                    break;
                }
            }
        }

        // Calculate chat score
        double chatScore = 0;
        if (messageCount > 10) {
            chatScore += 2;
        }
        if (rapidMessages > 2) {
            chatScore += 3;
        }
        if (suspiciousWords > 1) {
            chatScore += 4;
        }

        if (chatScore >= 5) {
            alertManager.createAlert(
                "Suspicious Chat Behavior",
                String.format("Messages: %d, Rapid messages: %d, Suspicious words: %d",
                    messageCount, rapidMessages, suspiciousWords),
                SeverityLevel.YELLOW
            );
        }
    }



    @Override
    public String getPlayerInfo(Player player) {
        Map<String, Object> info = getPlayerInfoInternal(player);
        StringBuilder sb = new StringBuilder();
        
        sb.append("Behavior Analysis for ").append(player.getName()).append("\n\n");
        
        // Movement stats
        @SuppressWarnings("unchecked")
        Map<String, Object> movementStats = (Map<String, Object>) info.get("movement");
        if (movementStats != null) {
            sb.append("Movement Stats:\n");
            sb.append("  - Average Speed: ").append(String.format("%.2f", movementStats.get("avgSpeed"))).append(" blocks/s\n");
            sb.append("  - Average Distance: ").append(String.format("%.2f", movementStats.get("avgDistance"))).append(" blocks\n");
            sb.append("  - Rapid Direction Changes: ").append(movementStats.get("rapidDirectionChanges")).append("\n");
            sb.append("  - Total Movements: ").append(movementStats.get("totalMovements")).append("\n\n");
        }
        
        // Block stats
        @SuppressWarnings("unchecked")
        Map<String, Object> blockStats = (Map<String, Object>) info.get("block");
        if (blockStats != null) {
            sb.append("Block Interaction Stats:\n");
            sb.append("  - Blocks Broken: ").append(blockStats.get("blocksBroken")).append("\n");
            sb.append("  - Blocks Placed: ").append(blockStats.get("blocksPlaced")).append("\n");
            sb.append("  - Rapid Block Actions: ").append(blockStats.get("rapidBlockActions")).append("\n");
            sb.append("  - Avg Break Distance: ").append(String.format("%.2f", blockStats.get("avgBreakDistance"))).append(" blocks\n");
            sb.append("  - Avg Place Distance: ").append(String.format("%.2f", blockStats.get("avgPlaceDistance"))).append(" blocks\n\n");
        }
        
        // Chat stats
        @SuppressWarnings("unchecked")
        Map<String, Object> chatStats = (Map<String, Object>) info.get("chat");
        if (chatStats != null) {
            sb.append("Chat Stats:\n");
            sb.append("  - Message Count: ").append(chatStats.get("messageCount")).append("\n");
            sb.append("  - Rapid Messages: ").append(chatStats.get("rapidMessages")).append("\n");
            sb.append("  - Suspicious Words: ").append(chatStats.get("suspiciousWords")).append("\n\n");
        }
        
        // Overall behavior score
        sb.append("Overall Behavior Score: ").append(String.format("%.2f", info.get("behaviorScore")).toString()).append("\n");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sb.append("Last Updated: ").append(sdf.format(new Date((long) info.get("lastUpdated"))));
        
        return sb.toString();
    }

    private Map<String, Object> getPlayerInfoInternal(Player player) {
        UUID playerId = player.getUniqueId();
        List<BehaviorEvent> events = playerEvents.getOrDefault(playerId, Collections.emptyList());
        
        Map<String, Object> info = new HashMap<>();
        
        // Movement stats
        Map<String, Object> movementStats = new HashMap<>();
        double avgSpeed = 0;
        double avgDistance = 0;
        int rapidDirectionChanges = 0;
        
        List<BehaviorEvent> movementEvents = events.stream()
            .filter(event -> event.getType().equals("movement"))
            .toList();
            
        for (int i = 0; i < movementEvents.size() - 1; i++) {
            BehaviorEvent current = movementEvents.get(i);
            BehaviorEvent next = movementEvents.get(i + 1);
            
            double deltaTime = (next.getTimestamp() - current.getTimestamp()) / 1000.0;
            double speed = Math.sqrt(Math.pow(next.getX() - current.getX(), 2) + 
                                 Math.pow(next.getY() - current.getY(), 2) + 
                                 Math.pow(next.getZ() - current.getZ(), 2)) / deltaTime;
            double distance = Math.sqrt(Math.pow(next.getX() - current.getX(), 2) + 
                                    Math.pow(next.getY() - current.getY(), 2) + 
                                    Math.pow(next.getZ() - current.getZ(), 2));
            
            avgSpeed += speed;
            avgDistance += distance;
            
            if (i > 0) {
                BehaviorEvent prev = movementEvents.get(i - 1);
                double angleChange = calculateAngleChange(prev, current, next);
                if (angleChange > 90) {
                    rapidDirectionChanges++;
                }
            }
        }
        
        if (!movementEvents.isEmpty()) {
            avgSpeed /= movementEvents.size();
            avgDistance /= movementEvents.size();
        }
        
        movementStats.put("avgSpeed", avgSpeed);
        movementStats.put("avgDistance", avgDistance);
        movementStats.put("rapidDirectionChanges", rapidDirectionChanges);
        movementStats.put("totalMovements", movementEvents.size());
        info.put("movement", movementStats);

        // Block interaction stats
        Map<String, Object> blockStats = new HashMap<>();
        int blocksBroken = 0;
        int blocksPlaced = 0;
        int rapidBlockActions = 0;
        double avgBreakDistance = 0;
        double avgPlaceDistance = 0;
        
        List<BehaviorEvent> blockEvents = events.stream()
            .filter(event -> event.getType().equals("block_break") || event.getType().equals("block_place"))
            .toList();
            
        Map<String, BehaviorEvent> lastPositions = new HashMap<>();
        
        for (int i = 0; i < blockEvents.size(); i++) {
            BehaviorEvent event = blockEvents.get(i);
            if (event.getType().equals("block_break")) {
                blocksBroken++;
                if (lastPositions.containsKey("break")) {
                    BehaviorEvent lastBreak = lastPositions.get("break");
                    double distance = Math.sqrt(
                        Math.pow(event.getX() - lastBreak.getX(), 2) +
                        Math.pow(event.getY() - lastBreak.getY(), 2) +
                        Math.pow(event.getZ() - lastBreak.getZ(), 2)
                    );
                    avgBreakDistance += distance;
                }
            } else if (event.getType().equals("block_place")) {
                blocksPlaced++;
                if (lastPositions.containsKey("place")) {
                    BehaviorEvent lastPlace = lastPositions.get("place");
                    double distance = Math.sqrt(
                        Math.pow(event.getX() - lastPlace.getX(), 2) +
                        Math.pow(event.getY() - lastPlace.getY(), 2) +
                        Math.pow(event.getZ() - lastPlace.getZ(), 2)
                    );
                    avgPlaceDistance += distance;
                }
            }
            
            // Check for rapid block actions
            if (i > 0 && blockEvents.get(i - 1).getType().equals(event.getType())) {
                long timeDiff = event.getTimestamp() - blockEvents.get(i - 1).getTimestamp();
                if (timeDiff < 100) {
                    rapidBlockActions++;
                }
            }
            
            lastPositions.put(event.getType(), event);
        }
        
        if (blocksBroken > 0) {
            avgBreakDistance /= blocksBroken;
        }
        if (blocksPlaced > 0) {
            avgPlaceDistance /= blocksPlaced;
        }
        
        blockStats.put("blocksBroken", blocksBroken);
        blockStats.put("blocksPlaced", blocksPlaced);
        blockStats.put("rapidBlockActions", rapidBlockActions);
        blockStats.put("avgBreakDistance", avgBreakDistance);
        blockStats.put("avgPlaceDistance", avgPlaceDistance);
        info.put("block", blockStats);

        // Chat stats
        Map<String, Object> chatStats = new HashMap<>();
        int messageCount = 0;
        int rapidMessages = 0;
        int suspiciousWords = 0;
        
        List<BehaviorEvent> chatEvents = events.stream()
            .filter(event -> event.getType().equals("chat"))
            .toList();
            
        String[] suspiciousWordsList = {
            "hack", "cheat", "bot", "exploit", "admin", "op", "mod", "staff",
            "bug", "glitch", "crash", "lag", "laggy", "slow", "performance",
            "ban", "kick", "mute", "unfair", "complain", "complaint"
        };
        
        for (int i = 0; i < chatEvents.size(); i++) {
            messageCount++;
            
            // Check for rapid messages
            if (i > 0) {
                long timeDiff = chatEvents.get(i).getTimestamp() - chatEvents.get(i - 1).getTimestamp();
                if (timeDiff < 500) {
                    rapidMessages++;
                }
            }
            
            // Check for suspicious words
            String message = chatEvents.get(i).getMessage().toLowerCase();
            for (String word : suspiciousWordsList) {
                if (message.contains(word)) {
                    suspiciousWords++;
                    break;
                }
            }
        }
        
        chatStats.put("messageCount", messageCount);
        chatStats.put("rapidMessages", rapidMessages);
        chatStats.put("suspiciousWords", suspiciousWords);
        info.put("chat", chatStats);

        // Overall behavior score
        double behaviorScore = 0;
        if (rapidDirectionChanges > 2) behaviorScore += 2;
        if (avgSpeed > movementSpeedThreshold) behaviorScore += 2;
        if (rapidBlockActions > 2) behaviorScore += 2;
        if (suspiciousWords > 0) behaviorScore += 3;
        if (rapidMessages > 2) behaviorScore += 2;
        
        info.put("behaviorScore", behaviorScore);
        info.put("lastUpdated", System.currentTimeMillis());
        
        return info;
    }

    private void analyzeBlockPatterns(List<BehaviorEvent> events) {
        int blocksBroken = 0;
        int blocksPlaced = 0;
        int rapidBlockChanges = 0;
        double avgBreakDistance = 0;
        double avgPlaceDistance = 0;
        long currentTime = System.currentTimeMillis();
        List<BehaviorEvent> recentEvents = events.stream()
            .filter(event -> event.getType().equals("block_break") || event.getType().equals("block_place") && 
                        (currentTime - event.getTimestamp()) <= 10000)
            .toList();

        if (recentEvents.isEmpty()) return;

        // Track previous block positions
        Map<String, BehaviorEvent> lastBlockPositions = new HashMap<>();

        for (int i = 0; i < recentEvents.size(); i++) {
            BehaviorEvent event = recentEvents.get(i);
            
            if (event.getType().equals("block_break")) {
                blocksBroken++;
                
                // Calculate distance from last break
                if (lastBlockPositions.containsKey("break")) {
                    BehaviorEvent lastBreak = lastBlockPositions.get("break");
                    double distance = Math.sqrt(
                        Math.pow(event.getX() - lastBreak.getX(), 2) +
                        Math.pow(event.getY() - lastBreak.getY(), 2) +
                        Math.pow(event.getZ() - lastBreak.getZ(), 2)
                    );
                    avgBreakDistance += distance;
                    
                    // Check for rapid block breaking
                    if (i > 0 && recentEvents.get(i - 1).getType().equals("block_break")) {
                        long timeDiff = event.getTimestamp() - recentEvents.get(i - 1).getTimestamp();
                        if (timeDiff < 100) { // Less than 100ms between breaks
                            rapidBlockChanges++;
                        }
                    }
                }
                lastBlockPositions.put("break", event);
            } else if (event.getType().equals("block_place")) {
                blocksPlaced++;
                
                // Calculate distance from last place
                if (lastBlockPositions.containsKey("place")) {
                    BehaviorEvent lastPlace = lastBlockPositions.get("place");
                    double distance = Math.sqrt(
                        Math.pow(event.getX() - lastPlace.getX(), 2) +
                        Math.pow(event.getY() - lastPlace.getY(), 2) +
                        Math.pow(event.getZ() - lastPlace.getZ(), 2)
                    );
                    avgPlaceDistance += distance;
                    
                    // Check for rapid block placing
                    if (i > 0 && recentEvents.get(i - 1).getType().equals("block_place")) {
                        long timeDiff = event.getTimestamp() - recentEvents.get(i - 1).getTimestamp();
                        if (timeDiff < 100) { // Less than 100ms between places
                            rapidBlockChanges++;
                        }
                    }
                }
                lastBlockPositions.put("place", event);
            }
        }

        // Calculate averages
        if (blocksBroken > 0) {
            avgBreakDistance /= blocksBroken;
        }
        if (blocksPlaced > 0) {
            avgPlaceDistance /= blocksPlaced;
        }

        // Calculate block activity score
        int blockScore = 0;
        if (blocksBroken > blockBreakThreshold) {
            blockScore += 3;
        }
        if (blocksPlaced > blockPlaceThreshold) {
            blockScore += 3;
        }
        if (rapidBlockChanges > 2) {
            blockScore += 4;
        }
        if (avgBreakDistance > 10 || avgPlaceDistance > 10) { // Large distances between blocks
            blockScore += 2;
        }

        if (blockScore >= 7) {
            alertManager.createAlert(
                "Suspicious Block Activity",
                String.format("Blocks broken: %d, Blocks placed: %d, Rapid changes: %d, Avg break distance: %.2f, Avg place distance: %.2f",
                    blocksBroken, blocksPlaced, rapidBlockChanges, avgBreakDistance, avgPlaceDistance),
                SeverityLevel.YELLOW
            );
        }
    }
}
