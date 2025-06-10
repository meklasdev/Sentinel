package com.wificraft.sentinel.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.wificraft.sentinel.WiFiCraftSentinel;
import com.wificraft.sentinel.modules.security.ClientSecurity;
import com.wificraft.sentinel.modules.security.HardwareFingerprint;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ClientInspectorGUI implements Listener {
    private static final Map<UUID, Inspection> activeInspections = new HashMap<>();
    private final WiFiCraftSentinel plugin;
    private final ClientSecurity clientSecurity;
    
    /**
     * Get all active inspections
     * @return Map of active inspections with moderator UUID as key
     */
    public static Map<UUID, Inspection> getActiveInspections() {
        return new HashMap<>(activeInspections);
    }

    public ClientInspectorGUI(WiFiCraftSentinel plugin) {
        this.plugin = plugin;
        this.clientSecurity = plugin.getSecurityModule().getClientSecurity();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openFor(Player player, String targetPlayer) {
        Inventory inventory = Bukkit.createInventory(null, 54, "§6Inspector Klienta: " + targetPlayer);

        // Add hardware info
        Player target = Bukkit.getPlayer(targetPlayer);
        if (target == null) {
            player.sendMessage("§cGracz nie jest online!");
            return;
        }
        
        // Get fingerprint asynchronously
        CompletableFuture.supplyAsync(() -> clientSecurity.getFingerprint(target))
            .thenAccept(fingerprint -> {
                if (fingerprint != null) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        addHardwareInfo(inventory, fingerprint);
                        player.openInventory(inventory);
                    });
                } else {
                    player.sendMessage("§cNie udało się pobrać informacji o sprzęcie gracza");
                }
            });

        // Security info will be added after hardware info is loaded
    }

    private void addHardwareInfo(Inventory inventory, HardwareFingerprint fingerprint) {
        int slot = 0;
        
        // CPU Info
        ItemStack cpuItem = createItem(Material.BOOK, "§6Informacje o procesorze",
            "§7Model: " + fingerprint.getCpuModel(),
            "§7Rdzenie: " + fingerprint.getCpuCores(),
            "§7Taktowanie: " + fingerprint.getCpuFrequency() + " MHz"
        );
        inventory.setItem(slot++, cpuItem);
        
        // RAM Info
        ItemStack ramItem = createItem(Material.BOOK, "§6Informacje o pamięci RAM",
            "§7Całkowita: " + fingerprint.getTotalRam() + " MB",
            "§7Dostępna: " + fingerprint.getAvailableRam() + " MB"
        );
        inventory.setItem(slot++, ramItem);
        
        // GPU Info
        ItemStack gpuItem = createItem(Material.BOOK, "§6Informacje o karcie graficznej",
            "§7Model: " + fingerprint.getGpuModel(),
            "§7Pamięć: " + fingerprint.getGpuMemory() + " MB"
        );
        inventory.setItem(slot, gpuItem);
        
        // Add security info if player is online
        // Since we might not have direct player reference, we'll skip this for now
        // You can implement player lookup by UUID or name if needed
        // Example:
        // Player target = Bukkit.getPlayer(fingerprint.getPlayerId());
        // if (target != null && target.isOnline()) {
        //     addSecurityInfo(inventory, target);
        // }
    }

    private void addSecurityInfo(Inventory inventory, Player targetPlayer) {
        int slot = 36; // Starting slot for security info
        
        // Client version
        String clientVersion = clientSecurity.getClientVersion(targetPlayer);
        boolean isVerified = clientSecurity.isClientVerified(targetPlayer);
        
        // Client Info
        ItemStack clientItem = createItem(Material.BOOK, "§6Informacje o kliencie",
            "§7Wersja: " + clientVersion,
            "§7Status: " + (isVerified ? "§aZweryfikowany" : "§cNiezidentyfikowany")
        );
        inventory.setItem(slot++, clientItem);

        // Security Status
        ItemStack securityItem = createItem(Material.SHIELD, "§6Status Bezpieczeństwa",
            "§7VPN: " + (clientSecurity.isUsingVPN(targetPlayer) ? "§cTak" : "§aNie"),
            "§7Proxy: " + (clientSecurity.isUsingProxy(targetPlayer) ? "§cTak" : "§aNie"),
            "§7Mod: " + (clientSecurity.isUsingModdedClient(targetPlayer) ? "§cTak" : "§aNie")
        );
        inventory.setItem(slot, securityItem);
    }

    private ItemStack createItem(Material material, String displayName, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            if (lore != null && lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().startsWith("§6Inspector Klienta")) {
            return;
        }
        event.setCancelled(true);
    }
}
