package com.wificraft.sentinel.modules.security;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import java.util.*;
import com.wificraft.sentinel.modules.security.IPUtils;

public class ClientFingerprint {
    private static final IPUtils IP_UTILS = new IPUtils();

    private final UUID playerId;
    private final String username;
    private final String ip;
    private final String userAgent;
    private final String clientVersion;
    private final Map<String, String> hardwareInfo;
    private final List<ItemStack> inventory;
    private final List<PotionEffect> activeEffects;
    private final Map<String, Object> customData;
    private final long timestamp;
    
    public ClientFingerprint(Player player) {
        this.playerId = player.getUniqueId();
        this.username = player.getName();
        this.ip = player.getAddress().getAddress().getHostAddress();
        this.userAgent = "Unknown"; // TODO: Implement user agent detection
        this.clientVersion = player.getClientViewDistance() + 
                           "x" + player.getClientViewDistance();
        this.hardwareInfo = collectHardwareInfo(player);
        this.inventory = new ArrayList<>(player.getInventory().getContents());
        this.activeEffects = new ArrayList<>(player.getActivePotionEffects());
        this.customData = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }

    private Map<String, String> collectHardwareInfo(Player player) {
        Map<String, String> info = new HashMap<>();
        
        // TODO: Implement hardware detection
        // This will require native code or plugin communication
        info.put("os", "Unknown");
        info.put("cpu", "Unknown");
        info.put("gpu", "Unknown");
        info.put("ram", "Unknown");
        
        return info;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getUsername() {
        return username;
    }

    public String getIp() {
        return ip;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getClientVersion() {
        return clientVersion;
    }

    public Map<String, String> getHardwareInfo() {
        return hardwareInfo;
    }

    public List<ItemStack> getInventory() {
        return inventory;
    }

    public List<PotionEffect> getActiveEffects() {
        return activeEffects;
    }

    public Map<String, Object> getCustomData() {
        return customData;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double calculateSimilarityScore(ClientFingerprint other) {
        double score = 0.0;
        
        // IP Analysis (20% weight)
        score += analyzeIP(other) * 0.2;
        
        // Hardware Analysis (30% weight)
        score += analyzeHardware(other) * 0.3;
        
        // Inventory Analysis (20% weight)
        score += analyzeInventory(other) * 0.2;
        
        // Effects Analysis (15% weight)
        score += analyzeEffects(other) * 0.15;
        
        // Version Analysis (10% weight)
        score += analyzeVersion(other) * 0.1;
        
        // Behavior Analysis (5% weight)
        score += analyzeBehavior(other) * 0.05;
        
        // Add new analysis methods
        score += analyzeMovementPatterns(other) * 0.1; // Movement patterns (10%)
        score += analyzeChatBehavior(other) * 0.1; // Chat behavior (10%)
        score += analyzeConnectionPatterns(other) * 0.1; // Connection patterns (10%)
        
        return Math.min(score, 1.0); // Cap at 100%
    }

    private double analyzeMovementPatterns(ClientFingerprint other) {
        // TODO: Implement movement pattern analysis when PlayerTracker is available
        // This will require integration with PlayerTracker
        return 0.0;
    }

    private double analyzeChatBehavior(ClientFingerprint other) {
        // TODO: Implement chat behavior analysis when ChatMonitor is available
        // This will require integration with ChatMonitor
        return 0.0;
    }

    private double analyzeConnectionPatterns(ClientFingerprint other) {
        // Analyze connection patterns
        long timeDiff = Math.abs(this.timestamp - other.timestamp);
        
        // Consider connection time patterns
        if (timeDiff < 60000) { // Less than 1 minute
            return 0.9; // Very suspicious if connecting within 1 minute
        } else if (timeDiff < 3600000) { // Less than 1 hour
            return 0.7; // Suspicious if connecting within 1 hour
        } else if (timeDiff < 86400000) { // Less than 1 day
            return 0.4; // Moderately suspicious if connecting within 1 day
        } else {
            return 0.1; // Less suspicious if connecting after 1 day
        }
    }

    private double analyzeIP(ClientFingerprint other) {
        // Basic IP match
        if (this.ip.equals(other.ip)) {
            return 1.0;
        }
        
        // Check for similar IP ranges
        String[] thisParts = this.ip.split("\\.");
        String[] otherParts = other.ip.split("\\.");
        
        if (thisParts.length == 4 && otherParts.length == 4) {
            double score = 0.0;
            
            // Check octet similarity
            for (int i = 0; i < 4; i++) {
                try {
                    int thisOctet = Integer.parseInt(thisParts[i]);
                    int otherOctet = Integer.parseInt(otherParts[i]);
                    
                    // Calculate octet difference
                    int diff = Math.abs(thisOctet - otherOctet);
                    
                    // Score based on difference
                    if (diff == 0) {
                        score += 0.3; // Perfect match
                    } else if (diff <= 10) {
                        score += 0.2; // Similar
                    } else if (diff <= 25) {
                        score += 0.1; // Somewhat similar
                    }
                } catch (NumberFormatException e) {
                    // Invalid IP format
                    return 0.0;
                }
            }
            
            // Check for common VPN/proxy patterns
            score += checkVPNPatterns(this.ip, other.ip) * 0.2;
            
            // Check for common botnet patterns
            score += checkBotnetPatterns(this.ip, other.ip) * 0.1;
            
            // Check for common proxy patterns
            score += checkProxyPatterns(this.ip, other.ip) * 0.1;
            
            return score;
        }
        
        return 0.0;
    }

    private double checkVPNPatterns(String ip1, String ip2) {
        // Common VPN IP patterns
        String[] vpnRanges = {
            "103.10.0.0/16", "103.20.0.0/16", "103.30.0.0/16", // Common VPN ranges
            "104.0.0.0/16", "105.0.0.0/16", "106.0.0.0/16",
            "107.0.0.0/16", "108.0.0.0/16", "109.0.0.0/16"
        };
        
        for (String range : vpnRanges) {
            if (isIPInRange(ip1, range) && isIPInRange(ip2, range)) {
                return 0.9; // Very suspicious if both IPs are in VPN ranges
            }
        }
        
        return 0.0;
    }

    private double checkBotnetPatterns(String ip1, String ip2) {
        // Common botnet IP patterns
        String[] botnetRanges = {
            "172.16.0.0/12", "192.168.0.0/16", "10.0.0.0/8", // Private ranges
            "100.64.0.0/10", "169.254.0.0/16", "192.0.2.0/24"
        };
        
        for (String range : botnetRanges) {
            if (isIPInRange(ip1, range) && isIPInRange(ip2, range)) {
                return 0.8; // Suspicious if both IPs are in botnet ranges
            }
        }
        
        return 0.0;
    }

    private double checkProxyPatterns(String ip1, String ip2) {
        // Common proxy IP patterns
        String[] proxyRanges = {
            "192.168.0.0/16", "10.0.0.0/8", "172.16.0.0/12", // Private ranges
            "100.64.0.0/10", "169.254.0.0/16", "198.18.0.0/15"
        };
        
        for (String range : proxyRanges) {
            if (isIPInRange(ip1, range) && isIPInRange(ip2, range)) {
                return 0.7; // Suspicious if both IPs are in proxy ranges
            }
        }
        
        return 0.0;
    }

    private boolean isIPInRange(String ip, String range) {
        return IPUtils.isIPInRange(ip, range);
    }

    private double analyzeHardware(ClientFingerprint other) {
        double score = 0.0;
        
        // Compare hardware info
        int matches = 0;
        int total = Math.max(this.hardwareInfo.size(), other.hardwareInfo.size());
        
        for (Map.Entry<String, String> entry : this.hardwareInfo.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            if (other.hardwareInfo.containsKey(key)) {
                String otherValue = other.hardwareInfo.get(key);
                
                // Use fuzzy matching for hardware values
                if (value.equals(otherValue)) {
                    matches++;
                } else if (value != null && otherValue != null && 
                          value.toLowerCase().contains(otherValue.toLowerCase()) || 
                          otherValue.toLowerCase().contains(value.toLowerCase())) {
                    matches += 0.5; // Partial match
                }
            }
        }
        
        return matches / total;
    }

    private double analyzeInventory(ClientFingerprint other) {
        double score = 0.0;
        int matches = 0;
        int total = Math.max(this.inventory.size(), other.inventory.size());
        
        // Compare item types
        for (ItemStack item : this.inventory) {
            if (item != null) {
                for (ItemStack otherItem : other.inventory) {
                    if (otherItem != null && 
                        item.getType() == otherItem.getType() && 
                        Math.abs(item.getAmount() - otherItem.getAmount()) <= 2) {
                        matches++;
                        break;
                    }
                }
            }
        }
        
        // Check for similar inventory patterns
        if (matches > 0) {
            score += matches / total;
            
            // Check for similar inventory positioning
            for (int i = 0; i < Math.min(this.inventory.size(), other.inventory.size()); i++) {
                if (this.inventory.get(i) != null && other.inventory.get(i) != null && 
                    this.inventory.get(i).getType() == other.inventory.get(i).getType()) {
                    score += 0.1;
                }
            }
        }
        
        return Math.min(score, 1.0);
    }

    private double analyzeEffects(ClientFingerprint other) {
        double score = 0.0;
        int matches = 0;
        int total = Math.max(this.activeEffects.size(), other.activeEffects.size());
        
        // Compare effects
        for (PotionEffect effect : this.activeEffects) {
            for (PotionEffect otherEffect : other.activeEffects) {
                if (effect.getType() == otherEffect.getType() && 
                    Math.abs(effect.getDuration() - otherEffect.getDuration()) <= 100 && 
                    Math.abs(effect.getAmplifier() - otherEffect.getAmplifier()) <= 1) {
                    matches++;
                    break;
                }
            }
        }
        
        return matches / (double) total;
    }

    private double analyzeVersion(ClientFingerprint other) {
        // Compare client versions
        if (this.clientVersion.equals(other.clientVersion)) {
            return 1.0;
        }
        
        // Check for similar version numbers
        String[] thisParts = this.clientVersion.split("\\.");
        String[] otherParts = other.clientVersion.split("\\.");
        
        int matches = 0;
        int total = Math.min(thisParts.length, otherParts.length);
        
        for (int i = 0; i < total; i++) {
            if (thisParts[i].equals(otherParts[i])) {
                matches++;
            }
        }
        
        return matches / (double) total;
    }

    private double analyzeBehavior(ClientFingerprint other) {
        // Calculate time difference
        long timeDiff = Math.abs(this.timestamp - other.timestamp);
        
        // Consider recent connections more suspicious
        if (timeDiff < 3600000) { // Less than 1 hour
            return 0.8;
        } else if (timeDiff < 86400000) { // Less than 1 day
            return 0.5;
        } else {
            return 0.2;
        }
    }

    public boolean isSuspicious(ClientFingerprint other) {
        double similarity = calculateSimilarityScore(other);
        
        // Base suspicion threshold
        double threshold = 0.7;
        
        // Adjust threshold based on various factors
        threshold += adjustThresholdForIP(other);
        threshold += adjustThresholdForHardware(other);
        threshold += adjustThresholdForBehavior(other);
        
        // Cap threshold at 1.0
        threshold = Math.min(threshold, 1.0);
        
        // Return true if similarity is above adjusted threshold
        return similarity > threshold;
    }

    private double adjustThresholdForIP(ClientFingerprint other) {
        // Increase threshold if IPs are in different ranges
        if (!this.ip.startsWith(other.ip)) {
            return 0.1;
        }
        
        // Check for suspicious IP patterns
        if (isSuspiciousIPRange(this.ip) || isSuspiciousIPRange(other.ip)) {
            return 0.2;
        }
        
        return 0.0;
    }

    private double adjustThresholdForHardware(ClientFingerprint other) {
        // Increase threshold if hardware info differs significantly
        if (!this.hardwareInfo.equals(other.hardwareInfo)) {
            double diff = calculateHardwareDifference(other);
            if (diff > 0.5) {
                return 0.15;
            } else if (diff > 0.3) {
                return 0.1;
            }
        }
        
        return 0.0;
    }

    private double adjustThresholdForBehavior(ClientFingerprint other) {
        // Increase threshold based on behavior patterns
        if (isSuspiciousBehavior(other)) {
            return 0.2;
        }
        
        return 0.0;
    }

    private boolean isSuspiciousBehavior(ClientFingerprint other) {
        // TODO: Implement behavior analysis
        // This will require integration with behavior tracking systems
        return false;
    }

    private double calculateHardwareDifference(ClientFingerprint other) {
        double totalDiff = 0.0;
        int count = 0;
        
        for (Map.Entry<String, String> entry : this.hardwareInfo.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            if (other.hardwareInfo.containsKey(key)) {
                String otherValue = other.hardwareInfo.get(key);
                if (value != null && otherValue != null) {
                    double diff = calculateStringDifference(value, otherValue);
                    totalDiff += diff;
                    count++;
                }
            }
        }
        
        return count > 0 ? totalDiff / count : 0.0;
    }

    private double calculateStringDifference(String a, String b) {
        // Simple string difference calculation
        if (a.equals(b)) return 0.0;
        
        int len1 = a.length();
        int len2 = b.length();
        int maxLen = Math.max(len1, len2);
        
        // Calculate Levenshtein distance
        int[][] dp = new int[len1 + 1][len2 + 1];
        
        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        
        return (double) dp[len1][len2] / maxLen;
    }

    private boolean isSuspiciousIPRange(String ip) {
        // TODO: Implement IP range analysis
        // This will require integration with IP analysis system
        return false;
    }
}
