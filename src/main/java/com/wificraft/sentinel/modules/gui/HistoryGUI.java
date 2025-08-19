package com.wificraft.sentinel.modules.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.wificraft.sentinel.modules.data.PlayerStatsManager;

import java.util.*;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class HistoryGUI implements Listener {
    private final YamlConfiguration inspections;
    private final YamlConfiguration banHistory;
    private final Map<UUID, Integer> currentPage = new HashMap<>();
    private final int itemsPerPage = 27;
    private final PlayerStatsManager statsManager;
    private final JavaPlugin plugin;
    
    public HistoryGUI(YamlConfiguration inspections, YamlConfiguration banHistory, PlayerStatsManager statsManager) {
        this.inspections = inspections;
        this.banHistory = banHistory;
        this.statsManager = statsManager;
        this.plugin = (JavaPlugin) Bukkit.getPluginManager().getPlugin("Sentinel");
        
        if (this.plugin != null) {
            this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
        }
    }
    
    public void openHistoryGUI(Player moderator, Player target) {
        currentPage.put(moderator.getUniqueId(), 0);
        updateHistoryGUI(moderator, target, 0);
    }
    
    private void updateHistoryGUI(Player moderator, Player target, int page) {
        Inventory gui = Bukkit.createInventory(null, 54, "§6Historia gracza: " + target.getName() + " (Strona " + (page + 1) + ")");
        
        // Add navigation buttons
        gui.setItem(0, createItem(Material.ARROW, "§cPowrót", 
            Collections.singletonList("§7Kliknij, aby wrócić do głównego menu")));
        
        gui.setItem(1, createItem(Material.PAPER, "§ePoprzednia strona", 
            Collections.singletonList("§7Kliknij, aby zobaczyć poprzednią stronę")));
        
        gui.setItem(2, createItem(Material.PAPER, "§eNastępna strona", 
            Collections.singletonList("§7Kliknij, aby zobaczyć następną stronę")));
        
        // Add action buttons
        gui.setItem(3, createItem(Material.COMPASS, "§eWyszukaj", 
            Collections.singletonList("§7Kliknij, aby wyszukać w historii")));
        
        gui.setItem(4, createItem(Material.BOOK, "§eEksportuj", 
            Collections.singletonList("§7Kliknij, aby wyeksportować historię")));
        
        gui.setItem(5, createItem(Material.BELL, "§ePowiadomienia",
            Arrays.asList("§7Kliknij, aby:", "§8• §eZarządzać ustawieniami powiadomień")));
        
        // Add stats display
        gui.setItem(8, createItem(Material.PLAYER_HEAD, "§6Statystyki gracza",
            Collections.singletonList("§7Statystyki są tymczasowo niedostępne")));
        
        // Add items to the inventory based on the page
        List<ItemStack> items = getHistoryItems(target);
        int start = page * itemsPerPage;
        int end = Math.min(start + itemsPerPage, items.size());
        
        for (int i = start; i < end && i < items.size(); i++) {
            gui.setItem(9 + (i - start), items.get(i));
        }
        
        currentPage.put(moderator.getUniqueId(), page);
        moderator.openInventory(gui);
        
        // Get all history items
        List<ItemStack> historyItems = getHistoryItems(target);
        
        // Calculate pagination
        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, historyItems.size());
        
        // Add history items
        int slot = 9;
        for (int i = startIndex; i < endIndex; i++) {
            if (slot < 54) {
                gui.setItem(slot, historyItems.get(i));
                slot += 2;
            }
        }
        
        moderator.openInventory(gui);
    }
    
    private List<ItemStack> getHistoryItems(Player target) {
        List<ItemStack> items = new ArrayList<>();
        
        // Add inspection history
        List<?> playerInspections = inspections.getList("inspections." + target.getUniqueId(), new ArrayList<>());
        for (Object inspectionObj : playerInspections) {
            if (inspectionObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> inspection = (Map<String, Object>) inspectionObj;
                
                // Get inspection notes
                // Temporarily disabled inspection notes
                // List<InspectionNotes> notes = notesManager.getNotesForInspection(target, inspection.get("timestamp"));
                // StringBuilder notesText = new StringBuilder();
                // for (InspectionNotes note : notes) {
                //     notesText.append("\n§7Notatki:\n");
                //     notesText.append("§8• §e").append(note.getContent()).append("\n");
                // }
                
                ItemStack inspectionItem = createItem(
                    Material.COMPASS,
                    "§6Inspekcja " + formatTimestamp((Long) inspection.get("timestamp")),
                    Arrays.asList(
                        "§7Data: " + formatTimestamp((Long) inspection.get("timestamp")),
                        "§7Moderator: " + getModeratorName((String) inspection.get("moderator")),
                        "§7Wynik: " + inspection.get("result"),
                        "§7Czas: " + formatDuration((Long) inspection.get("duration"))
                    )
                );
                
                items.add(inspectionItem);
            }
        }
        
        // Add ban history
        List<?> playerBans = banHistory.getList("bans", new ArrayList<>());
        for (Object banObj : playerBans) {
            if (banObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> ban = (Map<String, Object>) banObj;
                if (ban.get("uuid").equals(target.getUniqueId().toString())) {
                    // Get ban notes
                    // Temporarily disabled ban notes
                    // List<InspectionNotes> notes = notesManager.getNotesForBan(target, ban.get("timestamp"));
                    // StringBuilder notesText = new StringBuilder();
                    // for (InspectionNotes note : notes) {
                    //     notesText.append("\n§7Notatki:\n");
                    //     notesText.append("§8• §e").append(note.getContent()).append("\n");
                    // }
                    
                    ItemStack banItem = createItem(
                        Material.BARRIER,
                        "§cBan " + formatTimestamp((Long) ban.get("timestamp")),
                        Arrays.asList(
                            "§7Data: " + formatTimestamp((Long) ban.get("timestamp")),
                            "§7Moderator: " + getModeratorName((String) ban.get("moderator")),
                            "§7Powód: " + ban.get("reason"),
                            "§7Czas: " + formatBanDuration((Long) ban.get("duration")),
                            "§7Status: " + (isBanActive((Long) ban.get("timestamp"), (Long) ban.get("duration")) ? "§cAktywny" : "§aZakończony")
                        )
                    );
                    
                    items.add(banItem);
                }
            }
        }
        
        // Sort by timestamp (newest first)
        items.sort((item1, item2) -> {
            ItemMeta meta1 = item1.getItemMeta();
            ItemMeta meta2 = item2.getItemMeta();
            if (meta1 == null || meta2 == null) return 0;
            
            String name1 = meta1.getDisplayName();
            String name2 = meta2.getDisplayName();
            long time1 = extractTimestamp(name1);
            long time2 = extractTimestamp(name2);
            return Long.compare(time2, time1);
        });
        
        return items;
    }

    /**
     * Extracts timestamp from item display name
     */
    private long extractTimestamp(String displayName) {
        try {
            // Extract timestamp from display name (format: "§6Inspekcja [timestamp]" or "§cBan [timestamp]")
            String[] parts = displayName.split(" ");
            if (parts.length > 1) {
                return Long.parseLong(parts[parts.length - 1]);
            }
        } catch (Exception e) {
            // Ignore and return current time as fallback
        }
        return System.currentTimeMillis();
    }

    /**
     * Creates a GUI item with the given material, display name, and lore
     */
    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        if (!title.startsWith("§6Historia gracza: ")) return;
        
        event.setCancelled(true);
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        
        String displayName = clicked.getItemMeta().getDisplayName();
        String targetName = title.replace("§6Historia gracza: ", "").split(" ")[0];
        Player target = Bukkit.getPlayer(targetName);
        
        if (target == null) {
            player.sendMessage("§cNie można znaleźć gracza!");
            return;
        }
        
        int currentPage = this.currentPage.getOrDefault(player.getUniqueId(), 0);
        
        // Handle navigation buttons
        if (clicked.getType() == Material.ARROW && displayName.equals("§cPowrót")) {
            player.closeInventory();
        } 
        // Handle pagination
        else if (clicked.getType() == Material.PAPER) {
            if (displayName.equals("§ePoprzednia strona") && currentPage > 0) {
                updateHistoryGUI(player, target, currentPage - 1);
            } else if (displayName.equals("§eNastępna strona")) {
                updateHistoryGUI(player, target, currentPage + 1);
            }
        } 
        // Handle action buttons
        else if (clicked.getType() == Material.COMPASS) {
            player.sendMessage("§eWprowadź wiadomość, aby wyszukać w historii:");
            player.sendMessage("§eFunkcjonalność wyszukiwania będzie dostępna wkrótce!");
        }
        // Handle export button
        else if (clicked.getType() == Material.BOOK) {
            player.sendMessage("§eEksportowanie historii...");
            player.sendMessage("§eFunkcjonalność eksportu będzie dostępna wkrótce!");
        }
        // Handle notification settings
        else if (clicked.getType() == Material.BELL) {
            player.sendMessage("§eOtwieranie ustawień powiadomień...");
            player.sendMessage("§eFunkcjonalność powiadomień będzie dostępna wkrótce!");
        }
    }

    /**
     * Gets the moderator's name from their UUID
     */
    private String getModeratorName(String uuid) {
        try {
            return Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getName();
        } catch (Exception e) {
            return "Nieznany";
        }
    }
    
    /**
     * Formats a timestamp into a readable date string
     */
    private String formatTimestamp(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
    }
    
    /**
     * Formats a duration in milliseconds into a readable string
     */
    private String formatDuration(long duration) {
        Duration d = Duration.ofMillis(duration);
        long days = d.toDays();
        long hours = d.toHours() % 24;
        long minutes = d.toMinutes() % 60;
        
        List<String> parts = new ArrayList<>();
        if (days > 0) parts.add(days + "d");
        if (hours > 0) parts.add(hours + "h");
        if (minutes > 0) parts.add(minutes + "m");
        
        return parts.isEmpty() ? "0m" : String.join(" ", parts);
    }
    
    /**
     * Formats a ban duration in milliseconds into a readable string
     */
    private String formatBanDuration(long duration) {
        if (duration <= 0) return "Permanentny";
        return formatDuration(duration);
    }
    
    /**
     * Checks if a ban is currently active
     */
    private boolean isBanActive(long banTime, long banDuration) {
        if (banDuration <= 0) return true; // Permanent ban
        return (System.currentTimeMillis() - banTime) < banDuration;
    }
}
