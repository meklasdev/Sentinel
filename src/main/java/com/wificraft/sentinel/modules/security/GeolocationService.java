package com.wificraft.sentinel.modules.security;

import org.bukkit.entity.Player;
import org.json.JSONObject;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class GeolocationService {
    private static final String GEO_API_URL = "https://api.ipgeolocation.io/ipgeo";
    private static final String API_KEY = "YOUR_API_KEY"; // Will be configured in config.yml
    private final Map<String, LocationData> cachedLocations;
    private final Map<String, Long> lastLookupTime;
    private final int cacheDurationMinutes;
    private List<String> suspiciousCountries = new ArrayList<>();
    private List<String> suspiciousTimezones = new ArrayList<>();

    public GeolocationService(int cacheDurationMinutes) {
        this.cachedLocations = new ConcurrentHashMap<>();
        this.lastLookupTime = new ConcurrentHashMap<>();
        this.cacheDurationMinutes = cacheDurationMinutes;
        initializeSuspiciousLists();
    }

    private void initializeSuspiciousLists() {
        // Countries with high risk of suspicious activity
        suspiciousCountries.addAll(Arrays.asList(
            "RU", "CN", "IR", "KP", "VN", "TH", "ID"
        ));
        
        // Timezones that are often suspicious
        suspiciousTimezones.addAll(Arrays.asList(
            "Asia/Shanghai", "Asia/Ho_Chi_Minh", "Asia/Bangkok"
        ));
    }

    public LocationData getLocationData(Player player) {
        String ipAddress = player.getAddress().getAddress().getHostAddress();
        return getLocationData(ipAddress);
    }

    private LocationData getLocationData(String ipAddress) {
        if (isCached(ipAddress)) {
            return cachedLocations.get(ipAddress);
        }

        try {
            String apiUrl = GEO_API_URL + "?apiKey=" + API_KEY + "&ip=" + ipAddress;
            HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            
            if (connection.getResponseCode() == 200) {
                String response = new java.util.Scanner(connection.getInputStream()).useDelimiter("\\A").next();
                JSONObject json = new JSONObject(response);
                
                LocationData location = new LocationData(
                    json.getString("country_code2"),
                    json.getString("country_name"),
                    json.getString("city"),
                    json.getString("timezone"),
                    json.getDouble("latitude"),
                    json.getDouble("longitude")
                );
                
                cacheLocation(ipAddress, location);
                return location;
            }
        } catch (IOException e) {
            // Log error but return cached data if available
            if (isCached(ipAddress)) {
                return cachedLocations.get(ipAddress);
            }
        }
        
        return new LocationData("Unknown", "Unknown", "Unknown", "Unknown", 0.0, 0.0);
    }

    private boolean isCached(String ipAddress) {
        Long lastLookup = lastLookupTime.get(ipAddress);
        if (lastLookup == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        return now - lastLookup < TimeUnit.MINUTES.toMillis(cacheDurationMinutes);
    }

    private void cacheLocation(String ipAddress, LocationData location) {
        cachedLocations.put(ipAddress, location);
        lastLookupTime.put(ipAddress, System.currentTimeMillis());
    }

    public boolean isSuspiciousLocation(LocationData location) {
        return suspiciousCountries.contains(location.getCountryCode()) ||
               suspiciousTimezones.contains(location.getTimezone());
    }

    public String getLocationAnalysis(Player player) {
        LocationData location = getLocationData(player);
        StringBuilder analysis = new StringBuilder();
        
        analysis.append("Analiza lokalizacji:").append("\n");
        analysis.append("Kraj: ").append(location.getCountry()).append("\n");
        analysis.append("Miasto: ").append(location.getCity()).append("\n");
        analysis.append("Strefa czasowa: ").append(location.getTimezone()).append("\n");
        analysis.append("Szerokość: ").append(location.getLatitude()).append("\n");
        analysis.append("Długość: ").append(location.getLongitude()).append("\n");
        
        if (isSuspiciousLocation(location)) {
            analysis.append("\n\nOSTRZEŻENIE: Lokalizacja może być podejrzana!");
            if (suspiciousCountries.contains(location.getCountryCode())) {
                analysis.append("\nKraj jest na liście podejrzanych.");
            }
            if (suspiciousTimezones.contains(location.getTimezone())) {
                analysis.append("\nStrefa czasowa jest na liście podejrzanych.");
            }
        }
        
        return analysis.toString();
    }

    public static class LocationData {
        private final String countryCode;
        private final String country;
        private final String city;
        private final String timezone;
        private final double latitude;
        private final double longitude;

        public LocationData(String countryCode, String country, String city, String timezone,
                          double latitude, double longitude) {
            this.countryCode = countryCode;
            this.country = country;
            this.city = city;
            this.timezone = timezone;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public String getCountryCode() {
            return countryCode;
        }

        public String getCountry() {
            return country;
        }

        public String getCity() {
            return city;
        }

        public String getTimezone() {
            return timezone;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }
    }
}
