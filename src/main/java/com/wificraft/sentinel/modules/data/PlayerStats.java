package com.wificraft.sentinel.modules.data;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import java.util.UUID;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class PlayerStats {
    private final UUID uuid;
    private long lastLoginTime;
    private long lastActivityTime;
    private int loginCount;
    private int inspectionCount;
    private int banCount;
    private long totalPlayTime;
    private long longestPlaySession;
    private long sessionStartTime;
    
    public PlayerStats(OfflinePlayer player) {
        this.uuid = player.getUniqueId();
        this.lastLoginTime = System.currentTimeMillis();
        this.loginCount = 1;
        this.inspectionCount = 0;
        this.banCount = 0;
        this.totalPlayTime = 0;
        this.longestPlaySession = 0;
        this.sessionStartTime = System.currentTimeMillis();
    }
    
    public PlayerStats(Player player) {
        this((OfflinePlayer) player);
    }
    
    public UUID getUuid() {
        return uuid;
    }
    
    public long getLastLoginTime() {
        return lastLoginTime;
    }
    
    public long getLastActivityTime() {
        return lastActivityTime;
    }
    
    public void updateActivity() {
        this.lastActivityTime = System.currentTimeMillis();
    }
    
    public void setLastLoginTime(long lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }
    
    public void setLoginCount(int loginCount) {
        this.loginCount = loginCount;
    }
    
    public void setInspectionCount(int inspectionCount) {
        this.inspectionCount = inspectionCount;
    }
    
    public void setBanCount(int banCount) {
        this.banCount = banCount;
    }
    
    public void setTotalPlayTime(long totalPlayTime) {
        this.totalPlayTime = totalPlayTime;
    }
    
    public void setLongestPlaySession(long longestPlaySession) {
        this.longestPlaySession = longestPlaySession;
    }
    
    public void updateLastLogin() {
        this.lastLoginTime = System.currentTimeMillis();
    }
    
    public int getLoginCount() {
        return loginCount;
    }
    
    public void incrementLoginCount() {
        this.loginCount++;
    }
    
    public int getInspectionCount() {
        return inspectionCount;
    }
    
    public void incrementInspectionCount() {
        this.inspectionCount++;
    }
    
    public int getBanCount() {
        return banCount;
    }
    
    public void incrementBanCount() {
        this.banCount++;
    }
    
    public long getTotalPlayTime() {
        return totalPlayTime;
    }
    
    public void addPlayTime(long duration) {
        this.totalPlayTime += duration;
        if (duration > longestPlaySession) {
            this.longestPlaySession = duration;
        }
    }
    
    public long getLongestPlaySession() {
        return longestPlaySession;
    }
    
    public String formatLastLogin() {
        long diff = System.currentTimeMillis() - lastLoginTime;
        long days = TimeUnit.MILLISECONDS.toDays(diff);
        long hours = TimeUnit.MILLISECONDS.toHours(diff) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60;
        
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0 || sb.length() == 0) sb.append(minutes).append("m");
        
        return sb.toString().trim();
    }
    
    public String formatPlayTime(long time) {
        long hours = TimeUnit.MILLISECONDS.toHours(time);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(time) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(time) % 60;
        
        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("s");
        
        return sb.toString().trim();
    }
    
    public String getStatsString() {
        return "§7Statystyki gracza:\n" +
               "§7Ostatnie logowanie: §e" + formatLastLogin() + " temu\n" +
               "§7Liczba logowań: §e" + loginCount + "\n" +
               "§7Czas gry: §e" + formatPlayTime(totalPlayTime) + "\n" +
               "§7Najdłuższa sesja: §e" + formatPlayTime(longestPlaySession) + "\n" +
               "§7Liczba inspekcji: §e" + inspectionCount + "\n" +
               "§7Liczba banów: §e" + banCount;
    }
    
    public void endSession() {
        long sessionDuration = System.currentTimeMillis() - sessionStartTime;
        this.totalPlayTime += sessionDuration;
        this.longestPlaySession = Math.max(this.longestPlaySession, sessionDuration);
        this.lastActivityTime = System.currentTimeMillis();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerStats that = (PlayerStats) o;
        return Objects.equals(uuid, that.uuid);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
