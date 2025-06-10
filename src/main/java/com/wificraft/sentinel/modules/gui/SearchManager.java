package com.wificraft.sentinel.modules.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;
import java.util.stream.Collectors;

public class SearchManager {
    private final Map<UUID, String> searchQueries = new HashMap<>();
    private final Map<UUID, Long> lastSearchTime = new HashMap<>();
    private final long searchCooldown = 5000; // 5 seconds
    
    public boolean canSearch(Player player) {
        long currentTime = System.currentTimeMillis();
        long lastSearch = lastSearchTime.getOrDefault(player.getUniqueId(), 0L);
        return currentTime - lastSearch > searchCooldown;
    }
    
    public void setSearchQuery(Player player, String query) {
        searchQueries.put(player.getUniqueId(), query);
        lastSearchTime.put(player.getUniqueId(), System.currentTimeMillis());
    }
    
    public String getSearchQuery(Player player) {
        return searchQueries.getOrDefault(player.getUniqueId(), "");
    }
    
    public List<ItemStack> searchHistoryItems(Player target, String query) {
        List<ItemStack> allItems = getHistoryItems(target);
        
        if (query == null || query.isEmpty()) {
            return allItems;
        }
        
        return allItems.stream()
            .filter(item -> {
                ItemMeta meta = item.getItemMeta();
                if (meta == null || meta.getLore() == null) return false;
                
                String lore = String.join(" ", meta.getLore());
                return lore.toLowerCase().contains(query.toLowerCase());
            })
            .collect(Collectors.toList());
    }
    
    private List<ItemStack> getHistoryItems(Player target) {
        // This would be implemented by HistoryGUI
        return new ArrayList<>();
    }
    
    public ItemStack createSearchItem() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§eSzukaj");
        meta.setLore(Arrays.asList(
            "§7Kliknij, aby wyszukać w historii",
            "§7Wpisz tekst do wyszukania",
            "§7Czas cooldownu: " + searchCooldown/1000 + " sekund"
        ));
        item.setItemMeta(meta);
        return item;
    }
}
