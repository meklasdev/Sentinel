package com.wificraft.sentinel.modules.reports.evidence;

import com.wificraft.sentinel.modules.reports.Evidence;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import java.util.UUID;

public class LocationEvidence extends Evidence {
    private final Location location;
    private final Material blockType;
    private final String worldName;

    public LocationEvidence(UUID reporterId, Location location, Material blockType, String notes) {
        super(reporterId, notes, Evidence.EvidenceType.LOCATION);
        this.location = location;
        this.blockType = blockType != null ? blockType : Material.AIR;
        this.worldName = location != null && location.getWorld() != null ? 
                         location.getWorld().getName() : "unknown";
    }

    public Location getLocation() {
        return location;
    }

    public Material getBlockType() {
        return blockType;
    }

    public String getWorldName() {
        return worldName;
    }

    public String getCoordinates() {
        return location != null ? 
               String.format("X: %d, Y: %d, Z: %d", 
                   location.getBlockX(), 
                   location.getBlockY(), 
                   location.getBlockZ()) : "Unknown location";
    }
}
