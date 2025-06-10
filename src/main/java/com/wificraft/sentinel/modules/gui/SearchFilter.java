package com.wificraft.sentinel.modules.gui;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class SearchFilter {
    private final Map<String, String> filters = new HashMap<>();
    private final Map<String, LocalDateTime> dateFilters = new HashMap<>();
    private final Map<String, Integer> numericFilters = new HashMap<>();
    private final Map<String, List<String>> listFilters = new HashMap<>();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public static ItemStack createFilterButton() {
        ItemStack item = new ItemStack(Material.HOPPER); // Using HOPPER as a filter icon
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§eFiltry");
        meta.setLore(Arrays.asList(
            "§7Kliknij, aby dodać filtry:",
            "§8• §eData: yyyy-MM-dd HH:mm:ss",
            "§8• §eTyp: inspekcja/ban",
            "§8• §eModerator: nazwa",
            "§8• §eWynik: wynik",
            "§8• §eCzas: sekundy"
        ));
        item.setItemMeta(meta);
        return item;
    }
    
    public void addStringFilter(String key, String value) {
        filters.put(key, value.toLowerCase());
    }
    
    public void addDateFilter(String key, String value) {
        try {
            dateFilters.put(key, LocalDateTime.parse(value, formatter));
        } catch (Exception e) {
            System.err.println("Nieprawidłowy format daty: " + value);
        }
    }
    
    public void addNumericFilter(String key, int value) {
        numericFilters.put(key, value);
    }
    
    public void addListFilter(String key, String... values) {
        listFilters.put(key, Arrays.asList(values));
    }
    
    public boolean matches(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.getLore() == null) return true;
        
        String displayName = meta.getDisplayName().replace("§e", "").replace("§c", "").toLowerCase();
        
        // Check string filters
        for (Map.Entry<String, String> entry : filters.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            boolean found = false;
            for (String line : meta.getLore()) {
                if (line.toLowerCase().contains(value)) {
                    found = true;
                    break;
                }
            }
            
            if (!found) return false;
        }
        
        // Check date filters
        for (Map.Entry<String, LocalDateTime> entry : dateFilters.entrySet()) {
            String key = entry.getKey();
            LocalDateTime value = entry.getValue();
            
            for (String line : meta.getLore()) {
                if (line.startsWith("§7Data:")) {
                    String dateStr = line.replace("§7Data: ", "").trim();
                    try {
                        LocalDateTime date = LocalDateTime.parse(dateStr, formatter);
                        if (key.equals("after") && date.isBefore(value)) return false;
                        if (key.equals("before") && date.isAfter(value)) return false;
                    } catch (Exception e) {
                        continue;
                    }
                }
            }
        }
        
        // Check numeric filters
        for (Map.Entry<String, Integer> entry : numericFilters.entrySet()) {
            String key = entry.getKey();
            int value = entry.getValue();
            
            for (String line : meta.getLore()) {
                if (line.startsWith("§7Czas:")) {
                    String timeStr = line.replace("§7Czas: ", "").trim();
                    try {
                        int time = Integer.parseInt(timeStr);
                        if (key.equals("min") && time < value) return false;
                        if (key.equals("max") && time > value) return false;
                    } catch (Exception e) {
                        continue;
                    }
                }
            }
        }
        
        // Check list filters
        for (Map.Entry<String, List<String>> entry : listFilters.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();
            
            boolean found = false;
            for (String line : meta.getLore()) {
                for (String value : values) {
                    if (line.toLowerCase().contains(value.toLowerCase())) {
                        found = true;
                        break;
                    }
                }
                if (found) break;
            }
            
            if (!found) return false;
        }
        
        return true;
    }
    
    public void clear() {
        filters.clear();
        dateFilters.clear();
        numericFilters.clear();
        listFilters.clear();
    }
    
    public String getFilterString() {
        StringBuilder sb = new StringBuilder();
        
        if (!filters.isEmpty()) {
            sb.append("§eFiltrowane po tekście:");
            for (String value : filters.values()) {
                sb.append(" §8• §e").append(value);
            }
        }
        
        if (!dateFilters.isEmpty()) {
            sb.append("\n§eFiltrowane po dacie:");
            for (Map.Entry<String, LocalDateTime> entry : dateFilters.entrySet()) {
                sb.append(" §8• §e").append(entry.getKey()).append(": ").append(entry.getValue().format(formatter));
            }
        }
        
        if (!numericFilters.isEmpty()) {
            sb.append("\n§eFiltrowane po czasie:");
            for (Map.Entry<String, Integer> entry : numericFilters.entrySet()) {
                sb.append(" §8• §e").append(entry.getKey()).append(": ").append(entry.getValue()).append("s");
            }
        }
        
        if (!listFilters.isEmpty()) {
            sb.append("\n§eFiltrowane po liście:");
            for (List<String> values : listFilters.values()) {
                sb.append(" §8• §e").append(String.join(", ", values));
            }
        }
        
        return sb.toString();
    }
}
