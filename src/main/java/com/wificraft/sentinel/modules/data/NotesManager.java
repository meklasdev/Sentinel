package com.wificraft.sentinel.modules.data;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class NotesManager {
    private final JavaPlugin plugin;
    private final File notesFile;
    private final YamlConfiguration notesConfig;
    private final Map<UUID, InspectionNotes> activeNotes;
    
    public NotesManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.notesFile = new File(plugin.getDataFolder(), "inspection_notes.yml");
        this.notesConfig = YamlConfiguration.loadConfiguration(notesFile);
        this.activeNotes = new HashMap<>();
        
        // Load existing notes
        loadNotes();
    }
    
    private void loadNotes() {
        for (String uuid : notesConfig.getConfigurationSection("notes").getKeys(false)) {
            UUID playerUuid = UUID.fromString(uuid);
            InspectionNotes notes = InspectionNotes.loadFromConfig(playerUuid, notesConfig);
            activeNotes.put(playerUuid, notes);
        }
    }
    
    public void addNote(Player player, String type, String content, UUID moderatorUuid, long duration) {
        UUID uuid = player.getUniqueId();
        InspectionNotes notes = activeNotes.computeIfAbsent(uuid, k -> new InspectionNotes(uuid));
        notes.addNote(type, content, moderatorUuid, duration);
        
        // Save to config
        notes.saveToConfig(notesConfig);
        try {
            notesConfig.save(notesFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Error saving inspection notes: " + e.getMessage());
        }
    }
    
    public List<InspectionNotes.Note> getNotes(Player player) {
        InspectionNotes notes = activeNotes.get(player.getUniqueId());
        return notes != null ? notes.getNotes() : Collections.emptyList();
    }
    
    public Map<String, Integer> getNoteCounts(Player player) {
        InspectionNotes notes = activeNotes.get(player.getUniqueId());
        return notes != null ? notes.getNoteCounts() : Collections.emptyMap();
    }
    
    public Map<String, Long> getNoteDurations(Player player) {
        InspectionNotes notes = activeNotes.get(player.getUniqueId());
        return notes != null ? notes.getNoteDurations() : Collections.emptyMap();
    }
    
    public String getStatistics(Player player) {
        InspectionNotes notes = activeNotes.get(player.getUniqueId());
        return notes != null ? notes.getStatistics() : "§7Brak statystyk inspekcji";
    }
    
    public void shutdown() {
        try {
            notesConfig.save(notesFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Error saving notes on shutdown: " + e.getMessage());
        }
    }
}
