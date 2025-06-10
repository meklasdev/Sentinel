package com.wificraft.sentinel.modules.data;

import org.bukkit.entity.Player;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class InspectionNotes {
    private final UUID playerUuid;
    private final List<Note> notes;
    private final Map<String, Integer> noteCounts;
    private final Map<String, Long> noteDurations;
    
    public static class Note {
        private final String type;
        private final String content;
        private final long timestamp;
        private final UUID moderatorUuid;
        private final long duration;
        
        public Note(String type, String content, UUID moderatorUuid, long duration) {
            this.type = type;
            this.content = content;
            this.timestamp = System.currentTimeMillis();
            this.moderatorUuid = moderatorUuid;
            this.duration = duration;
        }
        
        public String getType() {
            return type;
        }
        
        public String getContent() {
            return content;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public UUID getModeratorUuid() {
            return moderatorUuid;
        }
        
        public long getDuration() {
            return duration;
        }
        
        public String getFormattedDuration() {
            long hours = TimeUnit.MILLISECONDS.toHours(duration);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(duration) % 60;
            long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) % 60;
            
            StringBuilder sb = new StringBuilder();
            if (hours > 0) sb.append(hours).append("h ");
            if (minutes > 0) sb.append(minutes).append("m ");
            if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("s");
            
            return sb.toString().trim();
        }
        
        public String getFormattedTimestamp() {
            return new Date(timestamp).toString();
        }
    }
    
    public InspectionNotes(UUID playerUuid) {
        this.playerUuid = playerUuid;
        this.notes = new ArrayList<>();
        this.noteCounts = new HashMap<>();
        this.noteDurations = new HashMap<>();
    }
    
    public void addNote(String type, String content, UUID moderatorUuid, long duration) {
        Note note = new Note(type, content, moderatorUuid, duration);
        notes.add(note);
        
        // Update statistics
        noteCounts.merge(type, 1, Integer::sum);
        noteDurations.merge(type, duration, Long::sum);
    }
    
    public List<Note> getNotes() {
        return Collections.unmodifiableList(notes);
    }
    
    public Map<String, Integer> getNoteCounts() {
        return Collections.unmodifiableMap(noteCounts);
    }
    
    public Map<String, Long> getNoteDurations() {
        return Collections.unmodifiableMap(noteDurations);
    }
    
    public String getStatistics() {
        StringBuilder sb = new StringBuilder();
        sb.append("§7Statystyki inspekcji:\n");
        
        // Add note counts
        for (Map.Entry<String, Integer> entry : noteCounts.entrySet()) {
            sb.append("§7").append(entry.getKey()).append(" notes: §e").append(entry.getValue()).append("\n");
        }
        
        // Add average durations
        for (Map.Entry<String, Long> entry : noteDurations.entrySet()) {
            String type = entry.getKey();
            long totalDuration = entry.getValue();
            int count = noteCounts.getOrDefault(type, 0);
            if (count > 0) {
                long avgDuration = totalDuration / count;
                sb.append("§7Średni czas ").append(type).append(" inspekcji: §e").append(formatDuration(avgDuration)).append("\n");
            }
        }
        
        return sb.toString();
    }
    
    private String formatDuration(long duration) {
        long hours = TimeUnit.MILLISECONDS.toHours(duration);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) % 60;
        
        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("s");
        
        return sb.toString().trim();
    }
    
    public void saveToConfig(YamlConfiguration config) {
        List<Map<String, Object>> notesList = new ArrayList<>();
        for (Note note : notes) {
            Map<String, Object> noteMap = new HashMap<>();
            noteMap.put("type", note.getType());
            noteMap.put("content", note.getContent());
            noteMap.put("timestamp", note.getTimestamp());
            noteMap.put("moderator", note.getModeratorUuid().toString());
            noteMap.put("duration", note.getDuration());
            notesList.add(noteMap);
        }
        config.set("notes." + playerUuid.toString(), notesList);
    }
    
    public static InspectionNotes loadFromConfig(UUID playerUuid, YamlConfiguration config) {
        InspectionNotes notes = new InspectionNotes(playerUuid);
        List<Map<String, Object>> notesList = config.getMapList("notes." + playerUuid.toString());
        
        if (notesList != null) {
            for (Map<String, Object> noteMap : notesList) {
                String type = (String) noteMap.get("type");
                String content = (String) noteMap.get("content");
                long timestamp = (long) noteMap.get("timestamp");
                UUID moderator = UUID.fromString((String) noteMap.get("moderator"));
                long duration = (long) noteMap.get("duration");
                
                Note note = new Note(type, content, moderator, duration);
                notes.notes.add(note);
                notes.noteCounts.merge(type, 1, Integer::sum);
                notes.noteDurations.merge(type, duration, Long::sum);
            }
        }
        
        return notes;
    }
}
