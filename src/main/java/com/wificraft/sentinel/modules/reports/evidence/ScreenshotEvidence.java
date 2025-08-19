package com.wificraft.sentinel.modules.reports.evidence;

import com.wificraft.sentinel.modules.reports.Evidence;
import org.bukkit.Location;
import java.util.UUID;

public class ScreenshotEvidence extends Evidence {
    private final String imageUrl;
    private final String fileName;
    private final long fileSize;
    private final String mimeType;
    private final Location location;

    public ScreenshotEvidence(UUID reporterId, String imageUrl, String fileName, 
                            long fileSize, String mimeType, Location location, String notes) {
        super(reporterId, notes, Evidence.EvidenceType.SCREENSHOT);
        this.imageUrl = imageUrl;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.location = location;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Location getLocation() {
        return location;
    }
}
