package com.wificraft.sentinel.modules.security;

import org.bukkit.configuration.file.FileConfiguration;
import java.util.HashSet;
import java.util.Set;

public class Configuration {
    private final Set<String> suspiciousCountries;
    private final Set<String> vpnNetworks;
    private final Set<String> proxyNetworks;
    private final double locationThreshold;
    private final String serverTimezone;
    private final int maxTimezoneDifference;
    
    public Configuration(FileConfiguration config) {
        this.suspiciousCountries = new HashSet<>(config.getStringList("security.suspicious-countries"));
        this.vpnNetworks = new HashSet<>(config.getStringList("security.vpn-networks"));
        this.proxyNetworks = new HashSet<>(config.getStringList("security.proxy-networks"));
        this.locationThreshold = config.getDouble("security.location-threshold", 0.7);
        this.serverTimezone = config.getString("security.server-timezone", "UTC");
        this.maxTimezoneDifference = config.getInt("security.max-timezone-difference", 3);
    }

    public Set<String> getSuspiciousCountries() {
        return suspiciousCountries;
    }

    public Set<String> getVpnNetworks() {
        return vpnNetworks;
    }

    public Set<String> getProxyNetworks() {
        return proxyNetworks;
    }

    public double getLocationThreshold() {
        return locationThreshold;
    }

    public String getServerTimezone() {
        return serverTimezone;
    }

    public int getMaxTimezoneDifference() {
        return maxTimezoneDifference;
    }
}
