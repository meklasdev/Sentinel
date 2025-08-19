package com.wificraft.sentinel.modules.security.advanced;

import com.wificraft.sentinel.config.SecurityConfig;
import org.bukkit.configuration.file.FileConfiguration;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class IPAnalysis {
    private static final Pattern CIDR_PATTERN = Pattern.compile("^(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})/(\\d{1,2})$");
    private final Map<String, IPRange> cloudProviderRanges;
    private final Map<String, IPRange> dataCenterRanges;
    private final Map<String, List<String>> suspiciousPatterns;
    private final SecurityConfig config;
    
    public IPAnalysis(SecurityConfig config) {
        this.cloudProviderRanges = new ConcurrentHashMap<>();
        this.dataCenterRanges = new ConcurrentHashMap<>();
        this.suspiciousPatterns = new ConcurrentHashMap<>();
        this.config = config;
        
        initializeRanges();
        initializePatterns();
    }
    
    private void initializeRanges() {
        // Example cloud provider ranges (should be loaded from config)
        addRange("AWS", "52.0.0.0/8");
        addRange("AWS", "54.0.0.0/8");
        addRange("AWS", "34.0.0.0/8");
        
        // Example data center ranges
        addRange("OVH", "149.202.0.0/16");
        addRange("OVH", "185.10.0.0/16");
        
        // Load additional ranges from config if available
        try {
            if (config != null && config.getConfig() != null) {
                FileConfiguration fileConfig = config.getConfig();
                if (fileConfig.contains("security.ip-analysis.cloud-provider-ranges")) {
                    for (String provider : fileConfig.getConfigurationSection("security.ip-analysis.cloud-provider-ranges").getKeys(false)) {
                        List<String> ranges = fileConfig.getStringList("security.ip-analysis.cloud-provider-ranges." + provider);
                        for (String range : ranges) {
                            addRange(provider, range);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading IP ranges from config: " + e.getMessage());
        }
    }
    
    private void initializePatterns() {
        // Common suspicious patterns
        addPattern("Proxy", "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
        addPattern("Tunnel", "10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
        addPattern("VPN", "192\\.168\\.\\d{1,3}\\.\\d{1,3}");
        
        // Load additional patterns from config if available
        try {
            if (config != null && config.getConfig() != null) {
                FileConfiguration fileConfig = config.getConfig();
                if (fileConfig.contains("security.ip-analysis.suspicious-patterns")) {
                    for (String pattern : fileConfig.getConfigurationSection("security.ip-analysis.suspicious-patterns").getKeys(false)) {
                        String value = fileConfig.getString("security.ip-analysis.suspicious-patterns." + pattern);
                        if (value != null) {
                            addPattern(pattern, value);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading suspicious patterns from config: " + e.getMessage());
        }
    }
    
    private void addRange(String provider, String cidr) {
        try {
            IPRange range = new IPRange(cidr);
            if (provider.startsWith("AWS")) {
                cloudProviderRanges.put(cidr, range);
            } else {
                dataCenterRanges.put(cidr, range);
            }
        } catch (IllegalArgumentException e) {
            // Invalid CIDR format
            System.err.println("Invalid CIDR range: " + cidr);
        }
    }
    
    private void addPattern(String name, String pattern) {
        suspiciousPatterns.putIfAbsent(name, new ArrayList<>());
        suspiciousPatterns.get(name).add(pattern);
    }
    
    public AnalysisResult analyzeIP(String ip) {
        AnalysisResult result = new AnalysisResult(ip);
        
        try {
            InetAddress address = InetAddress.getByName(ip);
            
            // Check cloud provider ranges
            for (Map.Entry<String, IPRange> entry : cloudProviderRanges.entrySet()) {
                if (entry.getValue().contains(address)) {
                    result.addFlag("CLOUD_PROVIDER", "IP belongs to " + entry.getKey());
                    break;
                }
            }
            
            // Check data center ranges
            for (Map.Entry<String, IPRange> entry : dataCenterRanges.entrySet()) {
                if (entry.getValue().contains(address)) {
                    result.addFlag("DATA_CENTER", "IP belongs to " + entry.getKey());
                    break;
                }
            }
            
            // Check suspicious patterns
            for (Map.Entry<String, List<String>> entry : suspiciousPatterns.entrySet()) {
                for (String pattern : entry.getValue()) {
                    if (ip.matches(pattern)) {
                        result.addFlag("SUSPICIOUS_PATTERN", "Matches " + entry.getKey() + " pattern");
                        break;
                    }
                }
            }
            
        } catch (UnknownHostException e) {
            result.addError("Invalid IP address");
        }
        
        return result;
    }
    
    public static class AnalysisResult {
        private final String ip;
        private final List<String> flags;
        private final List<String> errors;
        private final Map<String, String> details;
        
        public AnalysisResult(String ip) {
            this.ip = ip;
            this.flags = new ArrayList<>();
            this.errors = new ArrayList<>();
            this.details = new HashMap<>();
        }
        
        public void addFlag(String type, String description) {
            flags.add(type + ": " + description);
        }
        
        public void addError(String error) {
            errors.add(error);
        }
        
        public void addDetail(String key, String value) {
            details.put(key, value);
        }
        
        public boolean hasFlags() {
            return !flags.isEmpty();
        }
        
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
        
        public List<String> getFlags() {
            return Collections.unmodifiableList(flags);
        }
        
        public List<String> getErrors() {
            return Collections.unmodifiableList(errors);
        }
        
        public Map<String, String> getDetails() {
            return Collections.unmodifiableMap(details);
        }
    }
    
    public static class IPRange {
        private final long start;
        private final long end;
        private final String cidr;
        
        public IPRange(String cidr) {
            this.cidr = cidr;
            String[] parts = cidr.split("/");
            String ip = parts[0];
            int prefix = Integer.parseInt(parts[1]);
            
            long ipLong = ipToLong(ip);
            long mask = -1L << (32 - prefix);
            
            this.start = ipLong & mask;
            this.end = start | ~mask;
        }
        
        public boolean contains(InetAddress address) {
            long ipLong = ipToLong(address.getHostAddress());
            return ipLong >= start && ipLong <= end;
        }
        
        private long ipToLong(String ip) {
            String[] octets = ip.split("\\.");
            long result = 0;
            for (String octet : octets) {
                result = (result << 8) | Integer.parseInt(octet);
            }
            return result;
        }
    }
}
