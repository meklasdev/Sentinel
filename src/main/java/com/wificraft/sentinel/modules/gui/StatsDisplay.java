package com.wificraft.sentinel.modules.gui;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class StatsDisplay {
    private final Map<String, Integer> stats;
    private final Map<String, List<String>> notes;
    private final DecimalFormat decimalFormat;
    
    public StatsDisplay() {
        this.stats = new HashMap<>();
        this.notes = new HashMap<>();
        this.decimalFormat = new DecimalFormat("0.00");
    }
    
    public void addStat(String key, int value) {
        stats.put(key, value);
    }
    
    public void addNote(String category, String note) {
        notes.computeIfAbsent(category, k -> new ArrayList<>()).add(note);
    }
    
    public ItemStack createStatsItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6Statystyki Gracza");
        
        List<String> lore = new ArrayList<>();
        
        // Add stats
        lore.add("§7Statystyki:");
        stats.forEach((key, value) -> {
            lore.add("§8• §7" + key + ": §e" + value);
        });
        
        // Add notes
        if (!notes.isEmpty()) {
            lore.add("§7" + "-".repeat(20));
            lore.add("§7Notatki:");
            notes.forEach((category, notesList) -> {
                lore.add("§8• §7" + category + ":");
                notesList.forEach(note -> lore.add("§8  • §e" + note));
            });
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    public void calculateStats(int inspections, int bans, int logins, long totalDurationMillis) {
        // Calculate inspection success rates
        int clean = 0;
        int suspicious = 0;
        int cheater = 0;
        
        // Add basic stats
        addStat("Liczba inspekcji", inspections);
        addStat("Liczba banów", bans);
        addStat("Liczba logowań", logins);
        
        // Calculate percentages
        if (inspections > 0) {
            addStat("% czystych", (int)calculatePercentage(clean, inspections));
            addStat("% podejrzanych", (int)calculatePercentage(suspicious, inspections));
            addStat("% cheaterów", (int)calculatePercentage(cheater, inspections));
        }
        
        // Add formatted duration
        addStat("Czas gry", (int)(totalDurationMillis / 1000)); // Store in seconds
    }
    
    private double calculatePercentage(int value, int total) {
        if (total == 0) return 0.0;
        return ((double) value / total) * 100;
    }
    
    private String formatDuration(long durationSeconds) {
        Duration duration = Duration.ofSeconds(durationSeconds);
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        
        List<String> parts = new ArrayList<>();
        if (days > 0) parts.add(days + " dni");
        if (hours > 0) parts.add(hours + " godzin");
        if (minutes > 0) parts.add(minutes + " minut");
        
        return parts.isEmpty() ? "0 minut" : String.join(" ", parts);
    }
}
