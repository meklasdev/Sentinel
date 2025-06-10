package com.wificraft.sentinel.modules.security;

import org.bukkit.entity.Player;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class CloudProviderDetector {
    private static final String CLOUD_API_URL = "https://api.ipinfo.io/json";
    private final Map<String, CloudProviderInfo> cachedProviders;
    private final Map<String, Long> lastLookupTime;
    private final int cacheDurationMinutes;
    private Set<String> knownProviders = new HashSet<>();
    private Map<String, String> providerRanges = new HashMap<>();

    public CloudProviderDetector(int cacheDurationMinutes) {
        this.cachedProviders = new ConcurrentHashMap<>();
        this.lastLookupTime = new ConcurrentHashMap<>();
        this.cacheDurationMinutes = cacheDurationMinutes;
        initializeKnownProviders();
        initializeProviderRanges();
    }

    private void initializeKnownProviders() {
        knownProviders.addAll(Arrays.asList(
            "AWS", "Amazon", "Google Cloud", "Microsoft Azure",
            "DigitalOcean", "Heroku", "OVH", "Linode",
            "Oracle Cloud", "IBM Cloud", "Rackspace"
        ));
    }

    private void initializeProviderRanges() {
        // AWS ranges
        providerRanges.put("52.0.0.0/8", "AWS");
        providerRanges.put("54.0.0.0/8", "AWS");
        // Google Cloud ranges
        providerRanges.put("35.191.0.0/16", "Google Cloud");
        providerRanges.put("104.154.0.0/15", "Google Cloud");
        // Azure ranges
        providerRanges.put("13.64.0.0/14", "Azure");
        providerRanges.put("13.68.0.0/14", "Azure");
        // Add more ranges as needed
    }

    public CloudProviderInfo detectCloudProvider(Player player) {
        String ipAddress = player.getAddress().getAddress().getHostAddress();
        return detectCloudProvider(ipAddress);
    }

    private CloudProviderInfo detectCloudProvider(String ipAddress) {
        if (isCached(ipAddress)) {
            return cachedProviders.get(ipAddress);
        }

        CloudProviderInfo info = new CloudProviderInfo();
        
        try {
            // Check against known IP ranges
            for (Map.Entry<String, String> entry : providerRanges.entrySet()) {
                if (isIpInRange(ipAddress, entry.getKey())) {
                    info.setProvider(entry.getValue());
                    info.setSuspicious(true);
                    info.setReason("Known cloud provider IP range");
                    break;
                }
            }

            // If not in known ranges, check API
            if (info.getProvider() == null) {
                String apiUrl = CLOUD_API_URL + "?ip=" + ipAddress;
                HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
                
                if (connection.getResponseCode() == 200) {
                    String response = new java.util.Scanner(connection.getInputStream()).useDelimiter("\\A").next();
                    
                    // Parse response and check for cloud provider
                    if (response.contains("Amazon") || response.contains("AWS")) {
                        info.setProvider("AWS");
                        info.setSuspicious(true);
                        info.setReason("Amazon/AWS detected");
                    } else if (response.contains("Google Cloud")) {
                        info.setProvider("Google Cloud");
                        info.setSuspicious(true);
                        info.setReason("Google Cloud detected");
                    } else if (response.contains("Microsoft") || response.contains("Azure")) {
                        info.setProvider("Azure");
                        info.setSuspicious(true);
                        info.setReason("Microsoft Azure detected");
                    }
                }
            }
        } catch (IOException e) {
            // Log error but return cached data if available
            if (isCached(ipAddress)) {
                return cachedProviders.get(ipAddress);
            }
        }

        cacheProvider(ipAddress, info);
        return info;
    }

    private boolean isCached(String ipAddress) {
        Long lastLookup = lastLookupTime.get(ipAddress);
        if (lastLookup == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        return now - lastLookup < TimeUnit.MINUTES.toMillis(cacheDurationMinutes);
    }

    private void cacheProvider(String ipAddress, CloudProviderInfo info) {
        cachedProviders.put(ipAddress, info);
        lastLookupTime.put(ipAddress, System.currentTimeMillis());
    }

    private boolean isIpInRange(String ipAddress, String cidrRange) {
        // Implementation of CIDR range checking
        // This is a simplified version - in production use a proper CIDR library
        String[] parts = ipAddress.split("\\.");
        String[] rangeParts = cidrRange.split("\\.");
        
        for (int i = 0; i < 4; i++) {
            if (!parts[i].equals(rangeParts[i])) {
                return false;
            }
        }
        return true;
    }

    public String getCloudProviderAnalysis(Player player) {
        CloudProviderInfo info = detectCloudProvider(player);
        StringBuilder analysis = new StringBuilder();
        
        analysis.append("Analiza dostawcy chmury:").append("\n");
        analysis.append("Dostawca: ").append(info.getProvider() != null ? info.getProvider() : "Nieznany").append("\n");
        analysis.append("Status: ").append(info.isSuspicious() ? "Podejrzany" : "Niepodejrzany").append("\n");
        analysis.append("Powód: ").append(info.getReason() != null ? info.getReason() : "Brak informacji").append("\n");
        
        if (info.isSuspicious()) {
            analysis.append("\n\nOSTRZEŻENIE: Wykryto podejrzany dostawcę chmury!");
            analysis.append("\nIP może być używane do:");
            analysis.append("\n- Botnetów");
            analysis.append("\n- DDoS");
            analysis.append("\n- Ataków z wielu IP");
        }
        
        return analysis.toString();
    }

    public static class CloudProviderInfo {
        private String provider;
        private boolean suspicious;
        private String reason;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public boolean isSuspicious() {
            return suspicious;
        }

        public void setSuspicious(boolean suspicious) {
            this.suspicious = suspicious;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
