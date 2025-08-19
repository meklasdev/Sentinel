package com.wificraft.sentinel.gui;

import com.wificraft.sentinel.WiFiCraftSentinel;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GUIManager implements Listener {
    private static final int GUI_SIZE = 54;
    private final Map<UUID, GUIState> openGuis = new ConcurrentHashMap<>();
    private final Map<String, GUIConfig> guiConfigs = new HashMap<>();
    private final WiFiCraftSentinel plugin;

    public GUIManager(WiFiCraftSentinel plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        // Initialize GUI configurations
        initializeGUIConfigs();
    }
    
    private void initializeGUIConfigs() {
        // Player Info GUI
        GUIConfig playerInfoConfig = new GUIConfig("player_info", "§8Informacje o graczu");
        playerInfoConfig.addItem(10, Material.matchMaterial("PLAYER_HEAD"), "§aGracz", 
            "§7Kliknij, aby zobaczyć informacje o graczu");
            
        playerInfoConfig.addItem(12, Material.matchMaterial("COMPASS"), "§aLokalizacja", 
            "§7Kliknij, aby zobaczyć lokalizację gracza");
            
        playerInfoConfig.addItem(14, Material.matchMaterial("IRON_PICKAXE"), "§aSprzęt", 
            "§7Kliknij, aby zobaczyć sprzęt gracza");
            
        playerInfoConfig.addItem(20, Material.matchMaterial("WHITE_WOOL"), "§aChmura", 
            "§7Kliknij, aby zobaczyć dostawcę chmury");
            
        playerInfoConfig.addItem(22, Material.matchMaterial("OBSERVER"), "§aCentrum danych", 
            "§7Kliknij, aby zobaczyć centrum danych");
            
        guiConfigs.put("player_info", playerInfoConfig);
        
        // Security Dashboard GUI
        GUIConfig securityConfig = new GUIConfig("security_dashboard", "§8Zarządzanie bezpieczeństwem");
        securityConfig.addItem(10, Material.matchMaterial("SHIELD"), "§aOstrzeżenia", 
            "§7Kliknij, aby zobaczyć ostrzeżenia");
            
        securityConfig.addItem(12, Material.matchMaterial("BELL"), "§aAlerty", 
            "§7Kliknij, aby zobaczyć alerty");
            
        securityConfig.addItem(14, Material.matchMaterial("MAP"), "§aLokalizacje", 
            "§7Kliknij, aby zobaczyć lokalizacje");
            
        securityConfig.addItem(20, Material.matchMaterial("BOOK"), "§aAnaliza", 
            "§7Kliknij, aby zobaczyć analizę zachowania");
            
        guiConfigs.put("security_dashboard", securityConfig);
    }

    /**
     * Open a GUI for a player
     * @param player The player to open the GUI for
     * @param guiType The type of GUI to open
     */
    public void openGUI(Player player, String guiType) {
        if (player == null || guiType == null) {
            return;
        }

        // Close any existing GUI for this player
        closeGUI(player);

        GUIConfig config = guiConfigs.get(guiType);
        if (config == null) {
            System.out.println("No GUI config found for type: " + guiType);
            return;
        }

        try {
            Inventory inventory = createGUI(config);
            player.openInventory(inventory);
            openGuis.put(player.getUniqueId(), new GUIState(guiType, inventory));
        } catch (Exception e) {
            System.out.println("Error opening GUI for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Create a GUI inventory from a GUIConfig
     * @param config The GUI configuration
     * @return The created inventory
     */
    public Inventory createGUI(GUIConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("GUIConfig cannot be null");
        }
        
        String title = config.getTitle();
        int size = 54; // Default size for 6 rows (54 slots)
        Inventory inventory = Bukkit.createInventory(null, size, title);
        
        // Fill the inventory using the config
        fillInventory(inventory, config, plugin);
        
        return inventory;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        GUIState guiState = openGuis.get(player.getUniqueId());

        
        if (guiState == null) return;
        
        event.setCancelled(true);
        
        int slot = event.getSlot();
        ItemStack item = event.getCurrentItem();
        
        if (item == null || item.getType() == Material.AIR) return;

        // Handle item clicks based on GUI type
        handleItemClick(guiState.getType(), slot, player);
    }

    private void handleItemClick(String guiType, int slot, Player player) {
        switch (guiType) {
            case "player_info":
                handlePlayerInfoClick(slot, player);
                break;
            case "security_dashboard":
                handleSecurityDashboardClick(slot, player);
                break;
        }
    }

    private void handlePlayerInfoClick(int slot, Player player) {
        switch (slot) {
            case 10: // Player info
                player.sendMessage("§6Informacje o graczu:");
                player.sendMessage("§7- Nick: " + player.getName());
                player.sendMessage("§7- UUID: " + player.getUniqueId());
                break;
            case 12: // Location
                player.performCommand("location " + player.getName());
                break;
            case 14: // Hardware
                player.performCommand("hardware " + player.getName());
                break;
            case 20: // Cloud
                player.performCommand("cloud " + player.getName());
                break;
            case 22: // Data Center
                player.performCommand("dc " + player.getName());
                break;
        }
    }

    private void handleSecurityDashboardClick(int slot, Player player) {
        switch (slot) {
            case 10: // Alerts
                player.performCommand("alerts");
                break;
            case 12: // Warnings
                player.performCommand("warnings");
                break;
            case 14: // Locations
                player.performCommand("locations");
                break;
            case 20: // Behavior
                player.performCommand("behavior " + player.getName());
                break;
        }
    }

    public void closeGUI(Player player) {
        openGuis.remove(player.getUniqueId());
    }

    public static class GUIConfig {
        private final String type;
        private final String title;
        private final Map<Integer, GUIItem> items;

        public GUIConfig(String type, String title) {
            this.type = type;
            this.title = title;
            this.items = new ConcurrentHashMap<>();
        }

        public void addItem(int slot, Material material, String displayName, String... lore) {
            items.put(slot, new GUIItem(material, displayName, lore));
        }

        /**
     * Fills an inventory with items from the config and fills empty slots with a filler item
     * @param inventory The inventory to fill
     * @param config The GUIConfig containing the items to place in the inventory
     * @param plugin The plugin instance for logging
     */
    public static void fillInventory(Inventory inventory, GUIConfig config, WiFiCraftSentinel plugin) {
            try {
            // Create a filler item (gray stained glass pane)
            ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = filler.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(" ");
                filler.setItemMeta(meta);
            }
            
            // Fill with config items if available
            if (config != null && config.getItems() != null) {
                for (Map.Entry<Integer, GUIItem> entry : config.getItems().entrySet()) {
                    int slot = entry.getKey();
                    if (slot >= 0 && slot < inventory.getSize()) {
                        try {
                            ItemStack item = entry.getValue().createItemStack();
                            if (item != null) {
                                inventory.setItem(slot, item);
                            }
                        } catch (Exception e) {
                            if (plugin != null) {
                                plugin.getLogger().warning("Error creating GUI item at slot " + slot + ": " + e.getMessage());
                            } else {
                                System.err.println("Error creating GUI item at slot " + slot + ": " + e.getMessage());
                            }
                        }
                    }
                }
            }
            
            // Fill remaining slots with the filler item
            for (int i = 0; i < inventory.getSize(); i++) {
                if (inventory.getItem(i) == null) {
                    inventory.setItem(i, filler.clone());
                }
            }
        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().severe("Error filling inventory: " + e.getMessage());
            } else {
                System.err.println("Error filling inventory: " + e.getMessage());
            }
        }
            }

        public String getTitle() {
            return title;
        }

        public Map<Integer, GUIItem> getItems() {
            return items;
        }
    }

    public static class GUIItem {
        private final Material material;
        private final String displayName;
        private final String[] lore;

        public GUIItem(Material material, String displayName, String... lore) {
            this.material = material;
            this.displayName = displayName;
            this.lore = lore;
        }

        public ItemStack createItemStack() {
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(displayName);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
            return item;
        }
    }

    public static class GUIState {
        private final String type;
        private final Inventory inventory;

        public GUIState(String type, Inventory inventory) {
            this.type = type;
            this.inventory = inventory;
        }

        public String getType() {
            return type;
        }

        public Inventory getInventory() {
            return inventory;
        }
    }
}
