package com.wificraft.sentinel.modules.security;

import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import java.util.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class PlayerBehaviorTracker {
    private final UUID playerId;
    private final Map<String, BehaviorPattern> behaviorPatterns;
    private final Map<String, BehaviorEvent> recentEvents;
    private final long maxHistoryMs;
    
    public PlayerBehaviorTracker(UUID playerId, long maxHistoryMs) {
        this.playerId = playerId;
        this.behaviorPatterns = new HashMap<>();
        this.recentEvents = new LinkedHashMap<>();
        this.maxHistoryMs = maxHistoryMs;
        
        // Initialize default behavior patterns
        initializePatterns();
    }
    
    private void initializePatterns() {
        // Movement patterns
        addPattern("fast_movement", new BehaviorPattern()
            .setThreshold(10, ChronoUnit.SECONDS)
            .setDescription("Rapid movement detected")
            .setSeverity(2));
            
        // Block interaction patterns
        addPattern("block_break_spam", new BehaviorPattern()
            .setThreshold(5, ChronoUnit.SECONDS)
            .setDescription("Excessive block breaking")
            .setSeverity(3));
            
        // Chat patterns
        addPattern("spam_chat", new BehaviorPattern()
            .setThreshold(3, ChronoUnit.SECONDS)
            .setDescription("Excessive chat messages")
            .setSeverity(2));
    }
    
    public void trackMovement(Player player, Location from, Location to) {
        double distance = from.distance(to);
        long time = System.currentTimeMillis();
        
        BehaviorEvent event = new BehaviorEvent()
            .setType("movement")
            .setDescription("Moved from " + from.toString() + " to " + to.toString())
            .setDistance(distance)
            .setTimestamp(time);
            
        recordEvent("movement", event);
        analyzeMovementPattern(event);
    }
    
    public void trackBlockInteraction(Player player, Block block, BlockFace face) {
        BehaviorEvent event = new BehaviorEvent()
            .setType("block_interaction")
            .setDescription("Interacted with " + block.getType() + " at " + block.getLocation())
            .setBlockType(block.getType())
            .setTimestamp(System.currentTimeMillis());
            
        recordEvent("block_interaction", event);
        analyzeBlockInteractionPattern(event);
    }
    
    public void trackChat(Player player, String message) {
        BehaviorEvent event = new BehaviorEvent()
            .setType("chat")
            .setDescription("Sent message: " + message)
            .setTimestamp(System.currentTimeMillis());
            
        recordEvent("chat", event);
        analyzeChatPattern(event);
    }
    
    private void recordEvent(String type, BehaviorEvent event) {
        recentEvents.put(type + event.getTimestamp(), event);
        
        // Remove old events
        Iterator<Map.Entry<String, BehaviorEvent>> iterator = recentEvents.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, BehaviorEvent> entry = iterator.next();
            if (System.currentTimeMillis() - entry.getValue().getTimestamp() > maxHistoryMs) {
                iterator.remove();
            }
        }
    }
    
    private void analyzeMovementPattern(BehaviorEvent event) {
        BehaviorPattern pattern = behaviorPatterns.get("fast_movement");
        if (pattern == null) return;
        
        if (event.getDistance() > 10) { // More than 10 blocks in one movement
            pattern.recordEvent(event);
            if (pattern.isSuspicious()) {
                triggerAlert(pattern);
            }
        }
    }
    
    private void analyzeBlockInteractionPattern(BehaviorEvent event) {
        BehaviorPattern pattern = behaviorPatterns.get("block_break_spam");
        if (pattern == null) return;
        
        if (event.getBlockType() != null && event.getBlockType() != Material.AIR) {
            pattern.recordEvent(event);
            if (pattern.isSuspicious()) {
                triggerAlert(pattern);
            }
        }
    }
    
    private void analyzeChatPattern(BehaviorEvent event) {
        BehaviorPattern pattern = behaviorPatterns.get("spam_chat");
        if (pattern == null) return;
        
        pattern.recordEvent(event);
        if (pattern.isSuspicious()) {
            triggerAlert(pattern);
        }
    }
    
    private void triggerAlert(BehaviorPattern pattern) {
        // TODO: Implement alert system integration
        System.out.println("ALERT: Suspicious behavior detected - " + pattern.getDescription());
    }
    
    public static class BehaviorPattern {
        private String description;
        private int threshold;
        private ChronoUnit unit;
        private int severity;
        private List<BehaviorEvent> events;
        
        public BehaviorPattern() {
            this.events = new ArrayList<>();
        }
        
        public BehaviorPattern setThreshold(int threshold, ChronoUnit unit) {
            this.threshold = threshold;
            this.unit = unit;
            return this;
        }
        
        public BehaviorPattern setDescription(String description) {
            this.description = description;
            return this;
        }
        
        public BehaviorPattern setSeverity(int severity) {
            this.severity = severity;
            return this;
        }
        
        public void recordEvent(BehaviorEvent event) {
            events.add(event);
        }
        
        public boolean isSuspicious() {
            if (events.isEmpty()) return false;
            
            long now = System.currentTimeMillis();
            long timeWindow = unit.getDuration().toMillis() * threshold;
            
            // Check if there are too many events in the time window
            long eventsInWindow = events.stream()
                .filter(e -> now - e.getTimestamp() <= timeWindow)
                .count();
                
            return eventsInWindow >= threshold;
        }
    }
    
    public static class BehaviorEvent {
        private String type;
        private String description;
        private double distance;
        private Material blockType;
        private long timestamp;
        
        public BehaviorEvent setType(String type) {
            this.type = type;
            return this;
        }
        
        public BehaviorEvent setDescription(String description) {
            this.description = description;
            return this;
        }
        
        public BehaviorEvent setDistance(double distance) {
            this.distance = distance;
            return this;
        }
        
        public BehaviorEvent setBlockType(Material blockType) {
            this.blockType = blockType;
            return this;
        }
        
        public BehaviorEvent setTimestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public String getType() {
            return type;
        }
        
        public String getDescription() {
            return description;
        }
        
        public double getDistance() {
            return distance;
        }
        
        public Material getBlockType() {
            return blockType;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
    }
    
    private void addPattern(String id, BehaviorPattern pattern) {
        behaviorPatterns.put(id, pattern);
    }
}
