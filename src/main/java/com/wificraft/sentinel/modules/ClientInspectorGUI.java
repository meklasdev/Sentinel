package com.wificraft.sentinel.modules;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;
import com.wificraft.sentinel.modules.data.Inspection;
import com.wificraft.sentinel.modules.data.PlayerStats;
import com.wificraft.sentinel.modules.data.PlayerStatsManager;
import com.wificraft.sentinel.modules.data.SuspiciousActivityDetector;
import com.wificraft.sentinel.modules.data.NotesManager;
import com.wificraft.sentinel.modules.config.InspectionConfig;
import com.wificraft.sentinel.modules.discord.DiscordIntegration;
import com.wificraft.sentinel.SentinelPlugin;
import org.bukkit.ChatColor;

public class ClientInspectorGUI {
    private final JavaPlugin plugin;
    private final File inspectionsFile;
    private final File banHistoryFile;
    private final File discordLinksFile;
    private final File notesFile;
    private final YamlConfiguration inspections;
    private final YamlConfiguration banHistory;
    private final YamlConfiguration discordLinks;
    private static final Map<UUID, Inspection> activeInspections = new HashMap<>();
    
    /**
     * Gets a map of all active inspections
     * @return A new HashMap containing all active inspections
     */
    public static Map<UUID, Inspection> getActiveInspections() {
        return new HashMap<>(activeInspections);
    }
    private final DiscordIntegration discordIntegration;
    private final PlayerStatsManager statsManager;
    private final SuspiciousActivityDetector suspiciousActivityDetector;
    private final InspectionConfig config;
    private final NotesManager notesManager;
    
    public ClientInspectorGUI(JavaPlugin plugin) {
        this.plugin = plugin;
        
        // Initialize config files
        inspectionsFile = new File(plugin.getDataFolder(), "inspections.yml");
        banHistoryFile = new File(plugin.getDataFolder(), "ban_history.yml");
        discordLinksFile = new File(plugin.getDataFolder(), "player_discord.cfg");
        notesFile = new File(plugin.getDataFolder(), "inspection_notes.yml");
        
        // Load configurations
        inspections = YamlConfiguration.loadConfiguration(inspectionsFile);
        banHistory = YamlConfiguration.loadConfiguration(banHistoryFile);
        discordLinks = YamlConfiguration.loadConfiguration(discordLinksFile);
        
        // Initialize Discord integration
        discordIntegration = new DiscordIntegration(plugin);
        
        // Initialize player stats
        statsManager = new PlayerStatsManager(plugin);
        
        // Initialize configuration
        config = new InspectionConfig(plugin.getDataFolder());
        
        // Initialize suspicious activity detector
        suspiciousActivityDetector = new SuspiciousActivityDetector(config);
        
        // Initialize notes manager
        notesManager = new NotesManager(plugin);
        
        // Register event listener
        plugin.getServer().getPluginManager().registerEvents(new GUIListener(), plugin);
        
        // Start activity monitoring
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            checkPlayerActivity();
        }, 20 * 60, 20 * 60);
    }
    
    private boolean isSuspiciousActivity(Player player) {
        UUID playerId = player.getUniqueId();
        PlayerStats stats = statsManager.getPlayerStats(player);
        int logins = stats.getLoginCount();
        long sessionDuration = System.currentTimeMillis() - stats.getLastLoginTime();
        long idleTime = System.currentTimeMillis() - stats.getLastActivityTime();
        int inspections = stats.getInspectionCount();
        int bans = stats.getBanCount();
        
        return suspiciousActivityDetector.isSuspiciousActivity(
            playerId, logins, sessionDuration, idleTime, inspections, bans
        );
    }
    
    public void openGUI(Player moderator, Player target) {
        Inventory gui = Bukkit.createInventory(null, 54, "§6Inspekcja gracza: " + target.getName());
        
        // Add player head
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        headMeta.setOwningPlayer(target);
        headMeta.setDisplayName("§e" + target.getName());
        
        // Add suspicious activity indicators
        List<String> indicators = new ArrayList<>();
        if (isSuspiciousActivity(target)) {
            indicators.add("§cOSTRZEŻENIE: Wykryto podejrzane działanie!");
        }
        
        // Add player info
        List<String> lore = new ArrayList<>();
        lore.add("§7Status: " + (target.isOnline() ? "§aOnline" : "§cOffline"));
        
        // Add stats from PlayerStatsManager
        PlayerStats stats = statsManager.getPlayerStats(target);
        String statsString = stats.getStatsString();
        lore.addAll(Arrays.asList(statsString.split("\n")));
        
        if (activeInspections.containsKey(target.getUniqueId())) {
            Inspection current = activeInspections.get(target.getUniqueId());
            lore.add("§7Aktualna inspekcja: §eW toku");
            lore.add("§7Czas trwania: §e" + formatDuration(current.getDuration()));
        }
        
        headMeta.setLore(lore);
        head.setItemMeta(headMeta);
        gui.setItem(10, head);
        
        // Add history button
        ItemStack history = new ItemStack(Material.BOOK);
        ItemMeta historyMeta = history.getItemMeta();
        historyMeta.setDisplayName("§eHistoria");
        historyMeta.setLore(Arrays.asList(
            "§7Kliknij, aby zobaczyć historię inspekcji i banów"
        ));
        history.setItemMeta(historyMeta);
        gui.setItem(12, history);
        
        // Add inspection buttons
        ItemStack startInspection = new ItemStack(Material.COMPASS);
        ItemMeta startMeta = startInspection.getItemMeta();
        startMeta.setDisplayName("§eRozpocznij inspekcję");
        startMeta.setLore(Arrays.asList(
            "§7Kliknij, aby rozpocząć inspekcję gracza",
            "§7Po rozpoczęciu, gracz zostanie wezwany na kanał pomocy Discord"
        ));
        startInspection.setItemMeta(startMeta);
        gui.setItem(14, startInspection);
        
        ItemStack endInspection = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta endMeta = endInspection.getItemMeta();
        endMeta.setDisplayName("§cZakończ inspekcję");
        endMeta.setLore(Arrays.asList(
            "§7Kliknij, aby zakończyć inspekcję",
            "§7Wpisz wynik: §e/czysty §c/ludzik §6/cheater",
            "§7Wynik zostanie wysłany na kanał pomocy Discord"
        ));
        endInspection.setItemMeta(endMeta);
        gui.setItem(16, endInspection);
        
        moderator.openInventory(gui);
    }
    
    private String getLastLogin(Player player) {
        PlayerStats stats = statsManager.getPlayerStats(player);
        return stats.formatLastLogin();
    }
    
    private String getDiscordStatus(Player player) {
        String discordId = discordLinks.getString("players." + player.getUniqueId().toString());
        return discordId != null ? "§aTAK (ID: " + discordId + ")" : "§cNIE";
    }
    
    private int getInspectionCount(Player player) {
        List<?> inspections = this.inspections.getList("inspections." + player.getUniqueId().toString());
        return inspections != null ? inspections.size() : 0;
    }
    
    private int getBanCount(Player player) {
        List<?> bans = this.banHistory.getList("bans");
        if (bans == null) return 0;
        
        return (int) bans.stream()
            .map(ban -> (Map<String, Object>) ban)
            .filter(ban -> ban.get("uuid").equals(player.getUniqueId().toString()))
            .count();
    }
    
    public void addInspection(Player player, Player moderator, String result) {
        if (player == null || moderator == null) {
            plugin.getLogger().warning("Cannot add inspection: player or moderator is null");
            return;
        }
        
        // Update stats
        statsManager.getPlayerStats(player).incrementInspectionCount();
        
        // Create and store inspection
        Inspection inspection = new Inspection(player, moderator);
        inspection.setResult(result);
        activeInspections.put(player.getUniqueId(), inspection);
        
        // Add inspection note to notes manager
        notesManager.addNote(player, result, "Inspection started by " + moderator.getName(), 
            moderator.getUniqueId(), 0);
            
        // Log to Discord if available
        if (discordIntegration != null) {
            discordIntegration.logInspectionStart(player, moderator, result);
        }
        
        // Save inspection to config
        String inspectionId = "inspection_" + System.currentTimeMillis();
        String path = "inspections." + inspectionId;
        
        inspections.set(path + ".targetUuid", player.getUniqueId().toString());
        inspections.set(path + ".moderatorUuid", moderator.getUniqueId().toString());
        inspections.set(path + ".startTime", System.currentTimeMillis());
        inspections.set(path + ".result", result);
        inspections.set(path + ".status", "started");
        
        try {
            inspections.save(inspectionsFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save inspection: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void completeInspection(Player player, String result, String notes) {
        if (player == null) {
            plugin.getLogger().warning("Cannot complete inspection: player is null");
            return;
        }
        
        Inspection inspection = activeInspections.get(player.getUniqueId());
        if (inspection == null) {
            plugin.getLogger().warning("No active inspection found for player: " + player.getName());
            return;
        }
        
        // Complete the inspection
        inspection.completeInspection(result, notes);
        activeInspections.remove(player.getUniqueId());
        
        // Calculate duration
        long duration = inspection.getDuration();
        
        // Update stats
        statsManager.getPlayerStats(player).incrementInspectionCount();
        
        // Add completion note
        Player moderator = Bukkit.getPlayer(inspection.getModeratorUUID());
        String moderatorName = moderator != null ? moderator.getName() : "Unknown";
        notesManager.addNote(player, result, "Inspection completed by " + moderatorName, 
            inspection.getModeratorUUID(), duration);
        
        // Update inspection in config
        if (inspections.contains("inspections")) {
            for (String key : inspections.getConfigurationSection("inspections").getKeys(false)) {
                String path = "inspections." + key;
                String targetUuid = inspections.getString(path + ".targetUuid");
                
                if (targetUuid != null && targetUuid.equals(player.getUniqueId().toString()) && 
                    !inspections.getBoolean(path + ".completed", false)) {
                    
                    inspections.set(path + ".endTime", System.currentTimeMillis());
                    inspections.set(path + ".result", result);
                    inspections.set(path + ".notes", notes);
                    inspections.set(path + ".status", "completed");
                    inspections.set(path + ".duration", duration);
                    
                    try {
                        inspections.save(inspectionsFile);
                    } catch (Exception e) {
                        plugin.getLogger().severe("Failed to update inspection: " + e.getMessage());
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
        
        // Log to Discord if available
        if (discordIntegration != null) {
            discordIntegration.logInspectionComplete(player, moderator, result, duration);
        }
    }
    
    private List<Inspection> loadInspections() {
        List<Inspection> loadedInspections = new ArrayList<>();
        
        if (inspections.contains("inspections")) {
            for (String key : inspections.getConfigurationSection("inspections").getKeys(false)) {
                String path = "inspections." + key;
                try {
                    // Get player UUIDs from config
                    String targetUuidStr = inspections.getString(path + ".targetUuid");
                    String moderatorUuidStr = inspections.getString(path + ".moderatorUuid");
                    
                    if (targetUuidStr != null && moderatorUuidStr != null) {
                        UUID targetUuid = UUID.fromString(targetUuidStr);
                        UUID moderatorUuid = UUID.fromString(moderatorUuidStr);
                        
                        // Create offline players for the inspection
                        Player target = Bukkit.getPlayer(targetUuid);
                        Player moderator = Bukkit.getPlayer(moderatorUuid);
                        
                        if (target != null && moderator != null) {
                            Inspection inspection = new Inspection(target, moderator);
                            inspection.setResult(inspections.getString(path + ".result", ""));
                            inspection.setNotes(inspections.getString(path + ".notes", ""));
                            
                            // Set end time if inspection is completed
                            if (inspections.contains(path + ".endTime")) {
                                long endTime = inspections.getLong(path + ".endTime");
                                inspection.setEndTime(new Date(endTime));
                            }
                            
                            loadedInspections.add(inspection);
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load inspection: " + key + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        
        return loadedInspections;
    }
    
    public void addBan(Player player, Player moderator, String reason, long duration) {
        // Update stats
        statsManager.incrementBanCount(player);
        
        // Load ban history
        List<?> rawBans = this.banHistory.getList("bans", new ArrayList<>());
        List<String> result = new ArrayList<>();
        
        for (Object obj : rawBans) {
            if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> ban = (Map<String, Object>) obj;
                result.add((String) ban.get("player"));
            }
        }
        
        // Save ban
        Map<String, Object> newBan = new HashMap<>();
        newBan.put("player", player.getUniqueId().toString());
        newBan.put("reason", reason);
        newBan.put("moderator", moderator.getUniqueId().toString());
        newBan.put("timestamp", System.currentTimeMillis());
        newBan.put("duration", duration);
        
        result.add(player.getUniqueId().toString());
        
        this.banHistory.set("bans", result);
        saveConfig(banHistoryFile, banHistory);
    }
    
    private void saveConfig(File file, YamlConfiguration config) {
        try {
            config.save(file);
        } catch (Exception e) {
            plugin.getLogger().severe("Error saving config: " + e.getMessage());
        }
    }
    
    private String formatDuration(long duration) {
        if (duration <= 0) return "0 sekund";
        
        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) % 60;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration) % 60;
        long hours = TimeUnit.MILLISECONDS.toHours(duration);
        
        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("s");
        
        return sb.toString().trim();
    }
    
    private void checkPlayerActivity() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                if (isSuspiciousActivity(player)) {
                    // Notify staff about suspicious activity
                    String message = "§c[!] Podejrzana aktywność gracza: " + player.getName();
                    Bukkit.broadcastMessage(message);
                    
                    // Log to Discord if integration is available
                    if (discordIntegration != null) {
                        discordIntegration.logSuspiciousActivity(player, "Podejrzana aktywność");
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error checking player activity for " + player.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private class GUIListener implements Listener {
        @EventHandler
        private void onInventoryClick(InventoryClickEvent event) {
            if (event.getClickedInventory() == null || event.getCurrentItem() == null) return;
            
            Player player = (Player) event.getWhoClicked();
            Inventory inventory = event.getClickedInventory();
            ItemStack item = event.getCurrentItem();
            // Get the title from the inventory view
            String title = event.getView().getTitle();
            Material type = item.getType();
            
            event.setCancelled(true);
            
            // Update suspicious activity detector
            if (type == Material.COMPASS) {
                Player target = Bukkit.getPlayer(title.replace("§6Inspekcja gracza: ", ""));
                if (target != null) {
                    suspiciousActivityDetector.onInspectionStart(target);
                    // Notify Discord integration about inspection start
                    discordIntegration.logInspectionStart(target, player, "Rozpoczęcie inspekcji");
                }
            } else if (type == Material.RED_STAINED_GLASS_PANE) {
                Player target = Bukkit.getPlayer(title.replace("§6Inspekcja gracza: ", ""));
                if (target != null) {
                    suspiciousActivityDetector.onInspectionEnd(target);
                }
            }
            
            if (title.startsWith("§6Historia gracza")) {
                // Handle history GUI clicks
                if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.ARROW) {
                    event.setCancelled(true);
                    player.closeInventory();
                }
                return;
            }
            
            if (title.startsWith("§6Inspekcja gracza")) {
                event.setCancelled(true);
                
                if (event.getCurrentItem() != null) {
                    ItemStack clickedItem = event.getCurrentItem();
                    Material itemType = clickedItem.getType();
                    
                    if (itemType == Material.BOOK) {
                        // Open history GUI
                        String targetName = title.replace("§6Inspekcja gracza: ", "");
                        Player target = Bukkit.getPlayer(targetName);
                        if (target != null) {
                            // historyGUI.openHistoryGUI(player, target);
                            player.sendMessage("§eOtwieranie historii nie jest jeszcze dostępne");
                        }
                    } else if (itemType == Material.COMPASS) {
                        // Start inspection
                        Player target = Bukkit.getPlayer(title.replace("§6Inspekcja gracza: ", ""));
                        if (target != null) {
                            if (activeInspections.containsKey(target.getUniqueId())) {
                                player.sendMessage("§cGracz jest już w trakcie inspekcji!");
                                return;
                            }
                            
                            Inspection inspection = new Inspection(target, player);
                            activeInspections.put(target.getUniqueId(), inspection);
                            player.sendMessage("§aRozpoczęto inspekcję gracza " + target.getName());
                            
                            // Add inspection start note
                            notesManager.addNote(target, "start", "Inspekcja rozpoczęta", player.getUniqueId(), 0);
                            
                            // Send notification to inspection channel
                            String reason = "Moderator " + player.getName() + " rozpoczął inspekcję gracza " + target.getName();
                            if (discordIntegration != null) {
                                discordIntegration.logInspectionStart(target, player, reason);
                                // Call player to help channel
                                discordIntegration.callToHelp(target, reason);
                            }
                            
                            // Schedule a reminder after 5 minutes
                            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                if (activeInspections.containsKey(target.getUniqueId())) {
                                    // Get the last message from chat (this would need to be tracked separately)
                                    // For now, we'll use an empty string
                                    String query = "";
                                    player.sendMessage("§ePrzypomnienie: Inspekcja trwa już 5 minut. " +
                                        "Wpisz wynik: §e/czysty §c/ludzik §6/cheater");
                                }
                            }, 20 * 60 * 5);
                        }
                    } else if (type == Material.RED_STAINED_GLASS_PANE) {
                        // End inspection
                        Player target = Bukkit.getPlayer(title.replace("§6Inspekcja gracza: ", ""));
                        if (target != null) {
                            Inspection inspection = activeInspections.get(target.getUniqueId());
                            if (inspection != null) {
                                // Get the last message from chat (this would need to be tracked separately)
                                // For now, we'll use an empty string
                                String result = "";
                                if (result.equals("czysty") || result.equals("ludzik") || result.equals("cheater")) {
                                    // inspection.completeInspection(result, "");
                                    addInspection(target, player, result);
                                    activeInspections.remove(target.getUniqueId());
                                    
                                    // Send notification to Discord
                                    discordIntegration.sendInspectionNotification(target, player, result);
                                    
                                    // Send warning if inspection took too long
                                    long duration = inspection.getDuration();
                                    if (duration > config.getMaxInspectionDurationMinutes() * 60 * 1000) {
                                        String warning = "Inspekcja trwała " + formatDuration(duration) +
                                            " - dłużej niż maksymalny czas " +
                                            config.getMaxInspectionDurationMinutes() + " minut";
                                        discordIntegration.sendWarningNotification(target, player, warning);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static class CommandExecutor implements org.bukkit.command.CommandExecutor {
        private final ClientInspectorGUI gui;

        public CommandExecutor(ClientInspectorGUI gui) {
            this.gui = gui;
        }
        
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cTylko gracze mogą używać tej komendy!");
                return true;
            }
            
            Player moderator = (Player) sender;
            
            if (args.length == 0) {
                gui.openGUI(moderator, moderator);
            } else if (args.length == 1) {
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    moderator.sendMessage("§cNie znaleziono gracza o nazwie: " + args[0]);
                    return true;
                }
                gui.openGUI(moderator, target);
            } else {
                moderator.sendMessage("§cUżycie: /sprawdz lub /sprawdz <nick>");
            }
            
            return true;
        }
    }
}
