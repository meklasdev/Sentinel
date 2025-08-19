package com.wificraft.sentinel.modules;

import java.util.UUID;

public class ModeratorStats {
    private final UUID uuid;
    private int inspections;
    private int bans;
    private long lastInspection;

    public ModeratorStats(UUID uuid) {
        this.uuid = uuid;
        this.inspections = 0;
        this.bans = 0;
        this.lastInspection = 0;
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getInspections() {
        return inspections;
    }

    public void incrementInspections() {
        this.inspections++;
        this.lastInspection = System.currentTimeMillis();
    }

    public int getBans() {
        return bans;
    }

    public void incrementBans() {
        this.bans++;
    }

    public double getEffectiveness() {
        if (inspections == 0) return 0;
        return ((double) bans / inspections) * 100;
    }

    public long getLastInspection() {
        return lastInspection;
    }

    public void updateLastInspection() {
        this.lastInspection = System.currentTimeMillis();
    }
}
