package com.wificraft.sentinel.modules.data;

import org.bukkit.entity.Player;
import java.util.UUID;
import java.util.Date;

public class Inspection {
    private final UUID playerUUID;
    private final UUID moderatorUUID;
    private final Date startTime;
    private Date endTime;
    private String result;
    private String notes;
    
    public Inspection(Player player, Player moderator) {
        this.playerUUID = player.getUniqueId();
        this.moderatorUUID = moderator.getUniqueId();
        this.startTime = new Date();
        this.endTime = null;
        this.result = "W toku";
        this.notes = "";
    }
    
    public UUID getPlayerUUID() {
        return playerUUID;
    }
    
    public UUID getModeratorUUID() {
        return moderatorUUID;
    }
    
    public Date getStartTime() {
        return startTime;
    }
    
    public Date getEndTime() {
        return endTime;
    }
    
    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }
    
    public String getResult() {
        return result;
    }
    
    public void setResult(String result) {
        this.result = result;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public long getDuration() {
        if (endTime == null) return 0;
        return endTime.getTime() - startTime.getTime();
    }
    
    public boolean isCompleted() {
        return endTime != null;
    }
    
    public void completeInspection(String result, String notes) {
        this.endTime = new Date();
        this.result = result;
        this.notes = notes;
    }
}
