package com.wificraft.sentinel.modules.geolocation;

import org.bukkit.Location;

public class LocationData {
    private final String country;
    private final String region;
    private final String city;
    private final String timezone;
    private final String isp;
    private final String ip;
    private final double latitude;
    private final double longitude;
    private final Location bukkitLocation;

    public LocationData(String country, String region, String city, String timezone, 
                       String isp, String ip, double latitude, double longitude, Location bukkitLocation) {
        this.country = country;
        this.region = region;
        this.city = city;
        this.timezone = timezone;
        this.isp = isp;
        this.ip = ip;
        this.latitude = latitude;
        this.longitude = longitude;
        this.bukkitLocation = bukkitLocation;
    }

    // Getters
    public String getCountry() { return country; }
    public String getRegion() { return region; }
    public String getCity() { return city; }
    public String getTimezone() { return timezone; }
    public String getIsp() { return isp; }
    public String getIp() { return ip; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public Location getBukkitLocation() { return bukkitLocation; }

    @Override
    public String toString() {
        return String.format(
            "Country: %s, Region: %s, City: %s, Timezone: %s, ISP: %s, IP: %s, Coords: %.4f, %.4f",
            country, region, city, timezone, isp, ip, latitude, longitude
        );
    }
}
