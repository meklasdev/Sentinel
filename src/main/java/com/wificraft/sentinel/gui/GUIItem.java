package com.wificraft.sentinel.gui;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.Arrays;
import java.util.stream.Collectors;

public class GUIItem {
    private final Material material;
    private final String displayName;
    private final List<String> lore;
    private final int amount;
    private final Map<Enchantment, Integer> enchantments;
    private final Set<ItemFlag> itemFlags;
    private final boolean unbreakable;

    private GUIItem(Builder builder) {
        this.material = builder.material;
        this.displayName = builder.displayName;
        this.lore = builder.lore;
        this.amount = builder.amount;
        this.enchantments = builder.enchantments;
        this.itemFlags = builder.itemFlags;
        this.unbreakable = builder.unbreakable;
    }

    public ItemStack createItemStack() {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            if (displayName != null) {
                meta.setDisplayName(displayName);
            }
            
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(new ArrayList<>(lore));
            }
            
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                meta.addEnchant(entry.getKey(), entry.getValue(), true);
            }
            
            for (ItemFlag flag : itemFlags) {
                meta.addItemFlags(flag);
            }
            
            meta.setUnbreakable(unbreakable);
            item.setItemMeta(meta);
        }
        
        return item;
    }

    public static class Builder {
        private final Material material;
        private String displayName;
        private List<String> lore = new ArrayList<>();
        private int amount = 1;
        private Map<Enchantment, Integer> enchantments = new HashMap<>();
        private Set<ItemFlag> itemFlags = new HashSet<>();
        private boolean unbreakable = false;

        public Builder(Material material) {
            this.material = material;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder lore(String... lore) {
            this.lore = Arrays.asList(lore);
            return this;
        }

        public Builder lore(List<String> lore) {
            this.lore = new ArrayList<>(lore);
            return this;
        }

        public Builder amount(int amount) {
            this.amount = Math.max(1, amount);
            return this;
        }

        public Builder enchant(Enchantment enchantment, int level) {
            this.enchantments.put(enchantment, level);
            return this;
        }

        public Builder flags(ItemFlag... flags) {
            this.itemFlags.addAll(Arrays.asList(flags));
            return this;
        }

        public Builder unbreakable(boolean unbreakable) {
            this.unbreakable = unbreakable;
            return this;
        }

        public GUIItem build() {
            return new GUIItem(this);
        }
    }
}
