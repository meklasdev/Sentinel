package com.wificraft.sentinel.gui;

import org.bukkit.entity.Player;
import java.util.UUID;

/**
 * Represents an active player inspection
 */
public class Inspection {
    private final UUID moderatorId;
    private final UUID targetId;
    private final long startTime;
    private final String targetName;

    public Inspection(UUID moderatorId, UUID targetId, String targetName) {
        this.moderatorId = moderatorId;
        this.targetId = targetId;
        this.targetName = targetName;
        this.startTime = System.currentTimeMillis();
    }

    public UUID getModeratorId() {
        return moderatorId;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public String getTargetName() {
        return targetName;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getDuration() {
        return System.currentTimeMillis() - startTime;
    }
}
