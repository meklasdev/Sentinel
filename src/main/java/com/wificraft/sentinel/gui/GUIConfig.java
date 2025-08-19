package com.wificraft.sentinel.gui;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class GUIConfig {
    private final String id;
    private final String title;
    private final Map<Integer, GUIItem> items;

    public GUIConfig(String id, String title) {
        this.id = id;
        this.title = title;
        this.items = new HashMap<>();
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title != null ? title : "";
    }
    
    public Map<Integer, GUIItem> getItems() {
        return Collections.unmodifiableMap(items);
    }
    
    public void addItem(int slot, GUIItem item) {
        if (item != null) {
            items.put(slot, item);
        }
    }
    
    public void addItem(int slot, Material material, String displayName, String... lore) {
        GUIItem item = new GUIItem.Builder(material)
            .displayName(displayName)
            .lore(lore)
            .build();
        items.put(slot, item);
    }
    
    public GUIItem getItem(int slot) {
        return items.get(slot);
    }
    
    public ItemStack createItemStack(Material material, String displayName, String... lore) {
        return new GUIItem.Builder(material)
            .displayName(displayName)
            .lore(lore)
            .build()
            .createItemStack();
    }
    
    /**
     * Fills an inventory with the configured items
     * @param inventory The inventory to fill
     */
    public void fillInventory(org.bukkit.inventory.Inventory inventory) {
        for (Map.Entry<Integer, GUIItem> entry : items.entrySet()) {
            if (entry != null && entry.getKey() != null && entry.getKey() >= 0 && 
                entry.getKey() < inventory.getSize() && entry.getValue() != null) {
                inventory.setItem(entry.getKey(), entry.getValue().createItemStack());
            }
        }
    }
}
