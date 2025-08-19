package com.wificraft.sentinel.modules.security;

import com.google.common.util.concurrent.RateLimiter;
import com.google.gson.Gson;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import com.wificraft.sentinel.alerts.AlertManager;
import com.wificraft.sentinel.alerts.SeverityLevel;
import com.wificraft.sentinel.logging.SecurityLogger;
import com.wificraft.sentinel.modules.security.Configuration;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Arrays;

public class IPAnalyzer {
    private final AlertManager alertManager;
    private final Configuration config;
    private final ConcurrentHashMap<String, GeolocationCacheEntry> geolocationCache;
    private final Semaphore concurrentRequestSemaphore;
    private final RateLimiter geolocationRateLimiter;
    private final long cacheTtl;
    private final JavaPlugin plugin;
    private final Gson gson;
    private final CloseableHttpClient httpClient;
    private final String geolocationApiKey;

    public IPAnalyzer(AlertManager alertManager, Configuration config, JavaPlugin plugin) {
        this.alertManager = alertManager;
        this.config = config;
        this.plugin = plugin;
        this.geolocationCache = new ConcurrentHashMap<>();
        this.concurrentRequestSemaphore = new Semaphore(5); // Max 5 concurrent requests
        this.cacheTtl = TimeUnit.HOURS.toMillis(1); // 1 hour cache TTL
        this.geolocationRateLimiter = RateLimiter.create(60.0); // 60 requests per minute
        this.gson = new Gson();
        this.httpClient = HttpClients.createDefault();
        this.geolocationApiKey = plugin.getConfig().getString("security.geolocation-api-key", "");
    }

    private static class IpApiResponse {
        private String status;
        private String message;
        private String country;
        private String city;
        private String regionName;
        private String isp;
        private String org;
        private String timezone;
        private String zip;
        
        // Getters
        public String getStatus() { return status; }
        public String getMessage() { return message; }
        public String getCountry() { return country; }
        public String getCity() { return city; }
        public String getRegionName() { return regionName; }
        public String getIsp() { return isp; }
        public String getOrg() { return org; }
        public String getTimezone() { return timezone; }
        public String getZip() { return zip; }
    }

    private static class GeolocationCacheEntry {
        private final IpApiResponse data;
        private final long timestamp;
        private final long cacheTtl;
        
        public GeolocationCacheEntry(IpApiResponse data, long timestamp, long cacheTtl) {
            this.data = data;
            this.timestamp = timestamp;
            this.cacheTtl = cacheTtl;
        }
        
        public IpApiResponse getData() { return data; }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > cacheTtl;
        }
    }

    private boolean isPrivateIP(InetAddress address) {
        return address.isSiteLocalAddress() || 
               address.isLoopbackAddress() || 
               address.isLinkLocalAddress() || 
               address.isAnyLocalAddress();
    }

    private double analyzeVPNScore(String ip) {
        int matches = 0;
        for (String network : config.getVpnNetworks()) {
            if (isIpInRange(ip, network)) {
                matches++;
            }
        }
        return matches > 0 ? 1.0 : 0.0;
    }

    private double analyzeProxyScore(String ip) {
        int matches = 0;
        for (String network : config.getProxyNetworks()) {
            if (isIpInRange(ip, network)) {
                matches++;
            }
        }
        return matches > 0 ? 1.0 : 0.0;
    }

    private boolean isIpInRange(String ip, String cidr) {
        if (ip == null || cidr == null) {
            return false;
        }
        String[] parts = cidr.split("/");
        if (parts.length != 2) {
            return false;
        }

        String networkIp = parts[0];
        int prefix = Integer.parseInt(parts[1]);
        long ipNum = ipToLong(ip);
        long networkNum = ipToLong(networkIp);
        long mask = -1 << (32 - prefix);
        return (ipNum & mask) == (networkNum & mask);
    }

    private long ipToLong(String ipAddress) {
        String[] ipAddressInArray = ipAddress.split("\\.");
        long result = 0;
        for (int i = 0; i < ipAddressInArray.length; i++) {
            int power = 3 - i;
            int ip = Integer.parseInt(ipAddressInArray[i]);
            result += ip * Math.pow(256, power);
        }
        return result;
    }

    private void handleCachedGeolocation(Player player, IpApiResponse data) {
        String country = data.getCountry();
        String city = data.getCity();
        String region = data.getRegionName();
        String isp = data.getIsp();
        String org = data.getOrg();
        String timezone = data.getTimezone();
        String zip = data.getZip();
        
        // Check if country is suspicious
        if (config.getSuspiciousCountries().contains(country)) {
            String title = String.format("Suspicious Location - %s", player.getName());
            String description = String.format("Player from suspicious country: %s (%s, %s)", 
                country, city, region);
            alertManager.createAlert(title, description, SeverityLevel.ORANGE);
        }
        
        // Check for suspicious timezone
        if (isSuspiciousTimezone(timezone)) {
            String title = String.format("Suspicious Timezone - %s", player.getName());
            String description = String.format("Player using suspicious timezone: %s", timezone);
            alertManager.createAlert(title, description, SeverityLevel.YELLOW);
        }
        
        // Check for suspicious ISP patterns
        if (isSuspiciousISP(isp)) {
            String title = String.format("Suspicious ISP - %s", player.getName());
            String description = String.format("Player using suspicious ISP: %s", isp);
            alertManager.createAlert(title, description, SeverityLevel.YELLOW);
        }
        
        // Calculate comprehensive location score
        double locationScore = calculateLocationScore(player.getAddress().getAddress().getHostAddress(), 
            isp, org, timezone, zip);
        
        if (locationScore >= config.getLocationThreshold()) {
            String title = String.format("Suspicious Location Pattern - %s", player.getName());
            String description = String.format("Player shows suspicious location pattern (Score: %.2f%%)", 
                locationScore * 100);
            alertManager.createAlert(title, description, SeverityLevel.YELLOW);
        }
        
        // Log detailed location data
        SecurityLogger.getInstance(plugin).info(String.format("Player %s location data (cached): Country=%s, City=%s, Region=%s, ISP=%s, Timezone=%s, ZIP=%s", 
            player.getName(), country, city, region, isp, timezone, zip));
    }

    private boolean isSuspiciousTimezone(String timezone) {
        // List of suspicious timezones (e.g., data center timezones)
        String[] suspiciousTimezones = {
            "UTC", "Etc/UTC", "Etc/GMT", "Etc/GMT+0", "Etc/GMT-0",
            "Etc/GMT+1", "Etc/GMT-1", "Etc/GMT+2", "Etc/GMT-2"
        };

        for (String suspicious : suspiciousTimezones) {
            if (timezone != null && timezone.equalsIgnoreCase(suspicious)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSuspiciousISP(String isp) {
        if (isp == null) return false;

        // List of suspicious ISP patterns
        String[] suspiciousPatterns = {
            "cloudflare", "digitalocean", "amazon", "google", "microsoft",
            "heroku", "linode", "hetzner", "vultr", "ovh",
            "hosting", "datacenter", "server", "proxy", "vpn"
        };

        isp = isp.toLowerCase();
        for (String pattern : suspiciousPatterns) {
            if (isp.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private double calculateLocationScore(String ip, String isp, String org, String timezone, String zip) {
        double score = 0.0;
        
        // VPN/Proxy score
        score += analyzeVPNScore(ip) * 0.3;
        score += analyzeProxyScore(ip) * 0.3;
        
        // ISP score
        if (isSuspiciousISP(isp)) {
            score += 0.2;
        }
        
        // Timezone score
        if (isSuspiciousTimezone(timezone)) {
            score += 0.1;
        }
        
        // ZIP code score (if available)
        if (zip != null && !zip.isEmpty()) {
            score += 0.1;
        }
        
        return Math.min(score, 1.0);
    }

    public void analyzeIP(Player player, InetAddress address) {
        if (isPrivateIP(address)) {
            SecurityLogger.getInstance(plugin).info(String.format("Player %s using private IP address: %s", 
                player.getName(), address.getHostAddress()));
            return;
        }
        
        String ip = address.getHostAddress();
        
        // Calculate scores
        double vpnScore = analyzeVPNScore(ip);
        double proxyScore = analyzeProxyScore(ip);
        
        if (vpnScore > 0 || proxyScore > 0) {
            String title = String.format("Suspicious IP - %s", player.getName());
            String description = String.format(
                "IP Address: %s\n" +
                "VPN Score: %.2f\n" +
                "Proxy Score: %.2f",
                ip,
                vpnScore,
                proxyScore
            );
            alertManager.createAlert(title, description, SeverityLevel.YELLOW);
        }
        
        SecurityLogger.getInstance(plugin).info(String.format("Player %s IP analysis: VPN=%.2f, Proxy=%.2f", 
            player.getName(), vpnScore, proxyScore));
        
        analyzeGeolocation(player, address);
    }

    public void analyzeGeolocation(Player player, InetAddress address) {
        if (player == null || !player.isOnline()) {
            SecurityLogger.getInstance(plugin).warning("Cannot analyze geolocation for offline player");
            return;
        }
        
        if (address == null) {
            SecurityLogger.getInstance(plugin).warning(String.format("Cannot analyze geolocation for player %s (no IP address)", player.getName()));
            return;
        }
        
        String ip = address.getHostAddress();
        
        // Check cache first
        GeolocationCacheEntry cacheEntry = geolocationCache.get(ip);
        if (cacheEntry != null && !cacheEntry.isExpired()) {
            handleCachedGeolocation(player, cacheEntry.getData());
            return;
        }
        
        // Rate limiting
        if (!geolocationRateLimiter.tryAcquire()) {
            SecurityLogger.getInstance(plugin).info(String.format("Rate limit hit for IP geolocation (player=%s)", player.getName()));
            return;
        }
        
        // Acquire semaphore
        if (!concurrentRequestSemaphore.tryAcquire()) {
            SecurityLogger.getInstance(plugin).info(String.format("Too many concurrent geolocation requests (player=%s)", player.getName()));
            return;
        }
        
        try {
            // Make geolocation request
            String url = String.format("http://ip-api.com/json/%s?fields=status,message,country,city,regionName,isp,org,timezone,zip&lang=pl", ip);
            HttpGet request = new HttpGet(url);
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    SecurityLogger.getInstance(plugin).warning(String.format("Failed to get geolocation data for %s (status=%d)", 
                        ip, response.getStatusLine().getStatusCode()));
                    return;
                }
                
                String json = EntityUtils.toString(response.getEntity());
                IpApiResponse data = gson.fromJson(json, IpApiResponse.class);
                
                if (data.getStatus() != null && data.getStatus().equalsIgnoreCase("success")) {
                    // Cache the result
                    GeolocationCacheEntry cacheEntry = new GeolocationCacheEntry(data, System.currentTimeMillis(), cacheTtl);
                    geolocationCache.put(ip, cacheEntry);
                    
                    // Process the data
                    handleCachedGeolocation(player, data);
                } else {
                    SecurityLogger.getInstance(plugin).warning(String.format("Failed to get geolocation data for %s: %s", 
                        ip, data.getMessage()));
                }
            }
        } catch (Exception e) {
            SecurityLogger.getInstance(plugin).severe(String.format("Error getting geolocation data for %s: %s", ip, e.getMessage()));
        } finally {
            concurrentRequestSemaphore.release();
        }
    }

    public static void loadConfiguration(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        
        // IP Analysis
        config.addDefault("security.ip-analysis.enabled", true);
        config.addDefault("security.ip-analysis.cache-ttl", 3600000); // 1 hour
        config.addDefault("security.ip-analysis.rate-limit", 60); // requests per minute
        config.addDefault("security.ip-analysis.concurrent-requests", 5);
        
        // Suspicious Timezones
        config.addDefault("security.ip-analysis.suspicious-timezones", Arrays.asList(
            "UTC", "Etc/UTC", "Etc/GMT", "Etc/GMT+0", "Etc/GMT-0",
            "Etc/GMT+1", "Etc/GMT-1", "Etc/GMT+2", "Etc/GMT-2"
        ));
        
        // Suspicious Countries
        config.addDefault("security.ip-analysis.suspicious-countries", Arrays.asList(
            "RU", "CN", "IR", "KP", "SY", "LY", "SD"
        ));
        
        // Suspicious ISPs
        config.addDefault("security.ip-analysis.suspicious-isps", Arrays.asList(
            "vpn", "proxy", "tor", "data center", "hosting"
        ));
        
        // Location Score Threshold
        config.addDefault("security.ip-analysis.location-threshold", 0.5);
        
        // VPN Networks
        config.addDefault("security.ip-analysis.vpn-networks", Arrays.asList(
            "10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16"
        ));
        
        // Proxy Networks
        config.addDefault("security.ip-analysis.proxy-networks", Arrays.asList(
            "10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16"
        ));
        
        config.options().copyDefaults(true);
        plugin.saveConfig();
    }

    public void analyzeIP(Player player) {
        if (player == null || !player.isOnline()) {
            SecurityLogger.getInstance(plugin).warning("Cannot analyze IP for offline player");
            return;
        }
        
        InetAddress address = player.getAddress().getAddress();
        if (address == null) {
            SecurityLogger.getInstance(plugin).warning(String.format("Cannot get IP address for player %s", player.getName()));
            return;
        }
        
        analyzeIP(player, address);
    }
}
