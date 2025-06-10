package com.wificraft.sentinel.modules.security;

import org.bukkit.entity.Player;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class DataCenterDetector {
    private static final String DC_API_URL = "https://api.db-ip.com/v2/free";
    private final Map<String, DataCenterInfo> cachedDataCenters;
    private final Map<String, Long> lastLookupTime;
    private final int cacheDurationMinutes;
    private List<String> knownDataCenters = new ArrayList<>();
    private Map<String, String> dcRanges = new HashMap<>();
    private Map<String, String> suspiciousDcs = new HashMap<>();

    public DataCenterDetector(int cacheDurationMinutes) {
        this.cachedDataCenters = new ConcurrentHashMap<>();
        this.lastLookupTime = new ConcurrentHashMap<>();
        this.cacheDurationMinutes = cacheDurationMinutes;
        initializeKnownDataCenters();
        initializeDcRanges();
        initializeSuspiciousDcs();
    }

    private void initializeKnownDataCenters() {
        knownDataCenters.addAll(Arrays.asList(
            "Equinix", "CyrusOne", "Digital Realty",
            "CoreSite", "Interxion", "Telehouse",
            "Telecity", "ColoCrossing", "Cyxtera",
            "Switch", "Iron Mountain"
        ));
    }

    private void initializeDcRanges() {
        // Equinix ranges
        dcRanges.put("184.106.0.0/16", "Equinix");
        dcRanges.put("184.107.0.0/16", "Equinix");
        // Digital Realty ranges
        dcRanges.put("184.105.0.0/16", "Digital Realty");
        // Add more ranges as needed
    }

    private void initializeSuspiciousDcs() {
        suspiciousDcs.put("Equinix", "High traffic, potential botnet origin");
        suspiciousDcs.put("Cyxtera", "Known for hosting malicious services");
        suspiciousDcs.put("Digital Realty", "Large data center, potential for botnets");
    }

    public DataCenterInfo detectDataCenter(Player player) {
        String ipAddress = player.getAddress().getAddress().getHostAddress();
        return detectDataCenter(ipAddress);
    }

    private DataCenterInfo detectDataCenter(String ipAddress) {
        if (isCached(ipAddress)) {
            return cachedDataCenters.get(ipAddress);
        }

        DataCenterInfo info = new DataCenterInfo();
        
        try {
            // Check against known ranges first
            for (Map.Entry<String, String> entry : dcRanges.entrySet()) {
                if (isIpInRange(ipAddress, entry.getKey())) {
                    info.setDataCenter(entry.getValue());
                    info.setSuspicious(suspiciousDcs.containsKey(entry.getValue()));
                    info.setReason(suspiciousDcs.getOrDefault(entry.getValue(), "Known data center"));
                    break;
                }
            }

            // If not in known ranges, check API
            if (info.getDataCenter() == null) {
                String apiUrl = DC_API_URL + "?ipAddress=" + ipAddress;
                HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
                
                if (connection.getResponseCode() == 200) {
                    String response = new java.util.Scanner(connection.getInputStream()).useDelimiter("\\A").next();
                    
                    // Parse response and check for data center
                    if (response.contains("datacenter")) {
                        String dcName = extractDataCenterName(response);
                        info.setDataCenter(dcName);
                        info.setSuspicious(suspiciousDcs.containsKey(dcName));
                        info.setReason(suspiciousDcs.getOrDefault(dcName, "Known data center"));
                    }
                }
            }
        } catch (IOException e) {
            // Log error but return cached data if available
            if (isCached(ipAddress)) {
                return cachedDataCenters.get(ipAddress);
            }
        }

        cacheDataCenter(ipAddress, info);
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

    private void cacheDataCenter(String ipAddress, DataCenterInfo info) {
        cachedDataCenters.put(ipAddress, info);
        lastLookupTime.put(ipAddress, System.currentTimeMillis());
    }

    private boolean isIpInRange(String ipAddress, String cidrRange) {
        // Simplified CIDR checking
        String[] parts = ipAddress.split("\\.");
        String[] rangeParts = cidrRange.split("\\.");
        
        for (int i = 0; i < 4; i++) {
            if (!parts[i].equals(rangeParts[i])) {
                return false;
            }
        }
        return true;
    }

    private String extractDataCenterName(String response) {
        // Simplified response parsing
        return response.contains("Equinix") ? "Equinix" :
               response.contains("Digital Realty") ? "Digital Realty" :
               response.contains("Cyxtera") ? "Cyxtera" :
               "Unknown";
    }

    public String getDataCenterAnalysis(Player player) {
        DataCenterInfo info = detectDataCenter(player);
        StringBuilder analysis = new StringBuilder();
        
        analysis.append("Analiza centrum danych:").append("\n");
        analysis.append("Centrum danych: ").append(info.getDataCenter() != null ? info.getDataCenter() : "Nieznane").append("\n");
        analysis.append("Status: ").append(info.isSuspicious() ? "Podejrzane" : "Niepodejrzane").append("\n");
        analysis.append("Powód: ").append(info.getReason() != null ? info.getReason() : "Brak informacji").append("\n");
        
        if (info.isSuspicious()) {
            analysis.append("\n\nOSTRZEŻENIE: Wykryto podejrzane centrum danych!");
            analysis.append("\nCentrum danych może być używane do:");
            analysis.append("\n- Hostinga botnetów");
            analysis.append("\n- DDoS");
            analysis.append("\n- Hostinga serwerów gier");
        }
        
        return analysis.toString();
    }

    public static class DataCenterInfo {
        private String dataCenter;
        private boolean suspicious;
        private String reason;

        public String getDataCenter() {
            return dataCenter;
        }

        public void setDataCenter(String dataCenter) {
            this.dataCenter = dataCenter;
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
