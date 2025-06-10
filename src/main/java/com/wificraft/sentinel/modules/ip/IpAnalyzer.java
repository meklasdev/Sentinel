package com.wificraft.sentinel.modules.ip;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.wificraft.sentinel.WiFiCraftSentinel;
import com.wificraft.sentinel.config.SecurityConfig;
// import com.wificraft.sentinel.modules.security.AlertManager;
// import com.wificraft.sentinel.modules.alerts.AlertSeverity;

public class IpAnalyzer implements Runnable {
    private final WiFiCraftSentinel plugin;
    // private final AlertManager alertManager;
    // private final SecurityConfig config;
    private final Map<UUID, IpAnalysis> playerAnalyses;
    private final Map<String, List<UUID>> ipToPlayers;
    private final Map<String, Long> lastCheckTimes;
    private final Map<String, Integer> suspiciousCountries;
    private final Map<String, Integer> suspiciousTimezones;
    private final Map<String, Integer> suspiciousRanges;

    public IpAnalyzer(WiFiCraftSentinel plugin) {
        this.plugin = plugin;
        // this.alertManager = plugin.getAlertManager();
        // this.config = plugin.getSecurityConfig();
        this.playerAnalyses = new ConcurrentHashMap<>();
        this.ipToPlayers = new ConcurrentHashMap<>();
        this.lastCheckTimes = new ConcurrentHashMap<>();
        this.suspiciousCountries = new HashMap<>();
        this.suspiciousTimezones = new HashMap<>();
        this.suspiciousRanges = new HashMap<>();
        // initializeSuspiciousLists();
    }

    private void initializeSuspiciousLists() {
        /*
        // Load suspicious countries from config
        config.getGeolocationConfig().getSuspiciousCountries().forEach(country ->
            suspiciousCountries.put(country, 3));
        
        // Load suspicious timezones from config
        config.getGeolocationConfig().getSuspiciousTimezones().forEach(tz ->
            suspiciousTimezones.put(tz, 2));
        
        // Load suspicious IP ranges from config
        config.getDataCenterConfig().getSuspiciousDataCenters().forEach(dc ->
            suspiciousRanges.put(dc, 3));
        */
    }

    public void analyzePlayer(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Get or create player analysis
        IpAnalysis analysis = playerAnalyses.computeIfAbsent(playerId, k -> new IpAnalysis(player));
        
        // Update last seen time
        analysis.updateLastSeen();
        
        // Check for IP changes
        String currentIp = player.getAddress().getAddress().getHostAddress();
        if (!currentIp.equals(analysis.getIp())) {
            analysis.setIp(currentIp);
            checkIpSuspiciousness(analysis);
        }
        
        // Check for VPN/Proxy
        analysis.checkVpnProxy();
        
        // Check for suspicious patterns
        checkSuspiciousPatterns(analysis);
    }

    private void checkIpSuspiciousness(IpAnalysis analysis) {
        /*
        String ip = analysis.getIp();
        
        // Check if IP is in suspicious ranges
        if (suspiciousRanges.containsKey(ip)) {
            analysis.setSuspicious(true);
            alertManager.createAlert(
                analysis.getPlayer(),
                "Suspicious IP Address",
                "Player is using suspicious IP address: " + ip,
                AlertSeverity.HIGH
            );
        }
        
        // Check if IP is shared
        List<UUID> playersWithIp = ipToPlayers.computeIfAbsent(ip, k -> new ArrayList<>());
        playersWithIp.add(analysis.getPlayer().getUniqueId());
        
        if (playersWithIp.size() > 1) {
            analysis.setSuspicious(true);
            alertManager.createAlert(
                analysis.getPlayer(),
                "Shared IP Address",
                "Player is sharing IP address with " + (playersWithIp.size() - 1) + " other players",
                AlertSeverity.MEDIUM
            );
        }
        */
    }

    private void checkSuspiciousPatterns(IpAnalysis analysis) {
        /*
        // Check IP change frequency
        if (analysis.getIpChanges() > 3 && 
            analysis.getTimeSinceLastIpChange().toMinutes() < 10) {
            analysis.setSuspicious(true);
            alertManager.createAlert(
                analysis.getPlayer(),
                "Frequent IP Changes",
                "Player is changing IP addresses too frequently",
                AlertSeverity.MEDIUM
            );
        }
        
        // Check connection time patterns
        if (TimeUnit.MILLISECONDS.toMinutes(analysis.getAverageConnectionTime()) < 5) {
            analysis.setSuspicious(true);
            alertManager.createAlert(
                analysis.getPlayer(),
                "Short Connection Times",
                "Player has unusually short connection times",
                AlertSeverity.LOW
            );
        }
        */
    }

    @Override
    public void run() {
        // Clean up old analyses
        long currentTime = System.currentTimeMillis();
        playerAnalyses.entrySet().removeIf(entry -> {
            IpAnalysis analysis = entry.getValue();
            long lastSeen = analysis.getLastSeen();
            return currentTime - lastSeen > TimeUnit.HOURS.toMillis(24);
        });
        
        // Check for shared IPs
        ipToPlayers.forEach((ip, players) -> {
            if (players.size() > 1) {
                for (UUID playerId : players) {
                    Player player = plugin.getServer().getPlayer(playerId);
                    if (player != null) {
                        IpAnalysis analysis = playerAnalyses.get(playerId);
                        if (analysis != null) {
                            analysis.setSuspicious(true);
                        }
                    }
                }
            }
        });
        
        // Clean up old shared IP lists
        ipToPlayers.entrySet().removeIf(entry -> {
            List<UUID> players = entry.getValue();
            return players.isEmpty();
        });
    }

    public void shutdown() {
        // Cancel any running tasks
        if (task != null) {
            task.cancel();
        }
    }
    
    /**
     * Gets the IP analysis for a specific player.
     * @param playerId The UUID of the player
     * @return The IpAnalysis for the player, or null if not found
     */
    public IpAnalysis getPlayerAnalysis(UUID playerId) {
        return playerAnalyses.get(playerId);
    }

    private BukkitRunnable task;

    public void start() {
        task = new BukkitRunnable() {
            @Override
            public void run() {
                IpAnalyzer.this.run();
            }
        };
        task.runTaskTimer(plugin, 0L, 20L * 60L);
    }
}

public class IpAnalysis {
    private final Player player;
    private final List<String> ips;
    private final List<Long> connectionTimes;
    private boolean suspicious;
    private long lastSeen;
    private String currentIp;
    private int ipChanges;
    private long lastIpChange;
    private boolean vpnDetected;
    private boolean proxyDetected;

    public IpAnalysis(Player player) {
        this.player = player;
        this.ips = new ArrayList<>();
        this.connectionTimes = new ArrayList<>();
        this.suspicious = false;
        this.lastSeen = System.currentTimeMillis();
    }

    public void updateLastSeen() {
        this.lastSeen = System.currentTimeMillis();
    }

    public void setIp(String ip) {
        if (!ips.contains(ip)) {
            ips.add(ip);
            ipChanges++;
            lastIpChange = System.currentTimeMillis();
        }
        this.currentIp = ip;
    }

    public void checkVpnProxy() {
        try {
            InetAddress address = InetAddress.getByName(currentIp);
            vpnDetected = isVpnAddress(address);
            proxyDetected = isProxyAddress(address);
        } catch (UnknownHostException e) {
            player.sendMessage("§cNie udało się sprawdzić adresu IP!");
        }
    }

    private boolean isVpnAddress(InetAddress address) {
        // Implement VPN detection logic
        return false; // Placeholder
    }

    private boolean isProxyAddress(InetAddress address) {
        // Implement proxy detection logic
        return false; // Placeholder
    }

    public Player getPlayer() {
        return player;
    }

    public String getIp() {
        return currentIp;
    }

    public boolean isSuspicious() {
        return suspicious;
    }

    public void setSuspicious(boolean suspicious) {
        this.suspicious = suspicious;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public int getIpChanges() {
        return ipChanges;
    }

    public long getTimeSinceLastIpChange() {
        return System.currentTimeMillis() - lastIpChange;
    }

    public long getAverageConnectionTime() {
        if (connectionTimes.isEmpty()) return 0;
        long total = 0;
        for (long time : connectionTimes) {
            total += time;
        }
        return total / connectionTimes.size();
    }
    
    public boolean getVpnDetected() {
        return vpnDetected;
    }
    
    public boolean getProxyDetected() {
        return proxyDetected;
    }
    
    public List<String> getIps() {
        return new ArrayList<>(ips);
    }
}
