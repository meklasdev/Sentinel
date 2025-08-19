package com.wificraft.sentinel.modules.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class ExportManager {
    private final File exportDir;
    private final SimpleDateFormat dateFormat;
    
    public ExportManager(File dataFolder) {
        this.exportDir = new File(dataFolder, "exports");
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }
    }
    
    public ItemStack createExportItem() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§eEksport");
        meta.setLore(Arrays.asList(
            "§7Kliknij, aby eksportować historię",
            "§7Formaty: TXT, CSV, JSON",
            "§7Pliki zapisywane w folderze exports"
        ));
        item.setItemMeta(meta);
        return item;
    }
    
    public void exportHistory(Player target, List<ItemStack> historyItems, String format) throws IOException {
        String fileName = target.getName() + "_history_" + dateFormat.format(new Date()) + "." + format;
        File exportFile = new File(exportDir, fileName);
        
        switch (format.toLowerCase()) {
            case "txt":
                exportToText(exportFile, historyItems);
                break;
            case "csv":
                exportToCSV(exportFile, historyItems);
                break;
            case "json":
                exportToJSON(exportFile, historyItems);
                break;
            default:
                throw new IllegalArgumentException("Nieobsługiwany format eksportu: " + format);
        }
        
        target.sendMessage("§aHistoria została wyeksportowana do: " + exportFile.getAbsolutePath());
    }
    
    private void exportToText(File file, List<ItemStack> historyItems) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("=== HISTORIA GRACZA: " + historyItems.get(0).getItemMeta().getDisplayName() + " ===\n\n");
            
            for (ItemStack item : historyItems) {
                ItemMeta meta = item.getItemMeta();
                if (meta == null || meta.getLore() == null) continue;
                
                writer.write(meta.getDisplayName() + "\n");
                for (String line : meta.getLore()) {
                    writer.write(line + "\n");
                }
                writer.write("\n");
            }
        }
    }
    
    private void exportToCSV(File file, List<ItemStack> historyItems) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("Typ,Data,Moderator,Wynik/Reason,Czas/Duration\n");
            
            for (ItemStack item : historyItems) {
                ItemMeta meta = item.getItemMeta();
                if (meta == null || meta.getLore() == null) continue;
                
                String type = meta.getDisplayName().replace("§e", "").replace("§c", "");
                String data = "";
                String moderator = "";
                String result = "";
                String duration = "";
                
                for (String line : meta.getLore()) {
                    if (line.startsWith("§7Data:")) {
                        data = line.replace("§7Data: ", "");
                    } else if (line.startsWith("§7Moderator:")) {
                        moderator = line.replace("§7Moderator: ", "");
                    } else if (line.startsWith("§7Wynik:")) {
                        result = line.replace("§7Wynik: ", "");
                    } else if (line.startsWith("§7Powód:")) {
                        result = line.replace("§7Powód: ", "");
                    } else if (line.startsWith("§7Czas:")) {
                        duration = line.replace("§7Czas: ", "");
                    }
                }
                
                writer.write(String.format("%s,%s,%s,%s,%s\n", 
                    type, data, moderator, result, duration));
            }
        }
    }
    
    private void exportToJSON(File file, List<ItemStack> historyItems) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("[\n");
            
            for (int i = 0; i < historyItems.size(); i++) {
                ItemStack item = historyItems.get(i);
                ItemMeta meta = item.getItemMeta();
                if (meta == null || meta.getLore() == null) continue;
                
                String type = meta.getDisplayName().replace("§e", "").replace("§c", "");
                Map<String, String> data = new HashMap<>();
                
                for (String line : meta.getLore()) {
                    if (line.startsWith("§7")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length == 2) {
                            String key = parts[0].replace("§7", "").trim();
                            String value = parts[1].trim();
                            data.put(key, value);
                        }
                    }
                }
                
                writer.write(String.format("{\"type\":\"%s\",\"data\":%s}", 
                    type, data.toString()));
                
                if (i < historyItems.size() - 1) {
                    writer.write(",\n");
                }
            }
            
            writer.write("\n]");
        }
    }
    
    public List<String> getAvailableFormats() {
        return Arrays.asList("txt", "csv", "json");
    }
    
    public void cleanOldExports(int daysToKeep) {
        try {
            long cutoff = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000);
            Files.walk(Paths.get(exportDir.getAbsolutePath()))
                .filter(path -> Files.isRegularFile(path) && 
                    Files.getLastModifiedTime(path).toMillis() < cutoff)
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        System.err.println("Nie można usunąć pliku: " + path);
                    }
                });
        } catch (IOException e) {
            System.err.println("Błąd podczas czyszczenia starych eksportów: " + e.getMessage());
        }
    }
}
