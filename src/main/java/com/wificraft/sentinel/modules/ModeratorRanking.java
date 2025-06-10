package com.wificraft.sentinel.modules;

import com.wificraft.sentinel.WiFiCraftSentinel;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ModeratorRanking {
    private final WiFiCraftSentinel plugin;
    private final Map<UUID, ModeratorStats> moderatorStats;
    private final Map<YearMonth, Map<UUID, MonthlyStats>> monthlyStats;
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final String REWARDS_PERMISSION = "sentinel.ranking.rewards";

    public Map<UUID, ModeratorStats> getModeratorStats() {
        return moderatorStats;
    }
    
    public int getRankedModeratorCount() {
        return moderatorStats.size();
    }

    public ModeratorRanking(WiFiCraftSentinel plugin) {
        this.plugin = plugin;
        this.moderatorStats = new HashMap<>();
        this.monthlyStats = new HashMap<>();
    }

    public void initialize() {
        if (!plugin.getConfig().getBoolean("ranking.enabled")) {
            return;
        }

        // Load ranking data
        loadRankingData();
        loadMonthlyStats();
        
        // Schedule monthly reset
        scheduleMonthlyReset();
        
        // Schedule reward distribution
        if (plugin.getConfig().getBoolean("ranking.rewards.enabled", true)) {
            scheduleRewardDistribution();
        }
    }

    public void disable() {
        // Save all data
        saveRankingData();
        saveMonthlyStats();
    }

    public void refreshCache() {
        // Clear existing data to ensure a fresh load
        moderatorStats.clear();
        monthlyStats.clear();

        // Reload all ranking data from files
        loadRankingData();
        loadMonthlyStats();

        plugin.getLogger().info("Moderator ranking cache refreshed.");
    }

    public void displayRanking(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cTa komenda jest dostępna tylko dla graczy!");
            return;
        }
        openRankingGUI((Player) sender);
    }

    public void openRankingGUI(Player viewer) {
        Inventory gui = Bukkit.createInventory(null, 54, "§6Ranking moderatorów");
        
        // Add time period selector
        ItemStack currentMonth = createTimePeriodItem("§aBieżący miesiąc", "month", "§7Kliknij, aby zobaczyć ranking z bieżącego miesiąca");
        ItemStack allTime = createTimePeriodItem("§eCałkowity", "all", "§7Kliknij, aby zobaczyć ranking ogólny");
        
        gui.setItem(45, currentMonth);
        gui.setItem(53, allTime);
        
        // Show monthly ranking by default
        showMonthlyRanking(gui, viewer, YearMonth.now());
        
        viewer.openInventory(gui);
    }
    
    private void showMonthlyRanking(Inventory gui, Player viewer, YearMonth month) {
        // Clear previous items
        for (int i = 0; i < 45; i++) {
            gui.setItem(i, null);
        }
        
        // Get monthly stats
        Map<UUID, MonthlyStats> monthlyRanking = monthlyStats.getOrDefault(month, new HashMap<>());
        
        // Sort by points
        List<Map.Entry<UUID, MonthlyStats>> sorted = monthlyRanking.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue().getPoints(), a.getValue().getPoints()))
            .collect(Collectors.toList());
            
        // Add ranking items
        int slot = 0;
        for (int i = 0; i < sorted.size() && slot < 45; i++) {
            Map.Entry<UUID, MonthlyStats> entry = sorted.get(i);
            MonthlyStats stats = entry.getValue();
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry.getKey());
            
            if (offlinePlayer.hasPlayedBefore()) {
                gui.setItem(slot, createMonthlyModeratorItem(offlinePlayer, stats, i + 1));
                slot += 2;
            }
        }
        
        // Update viewer's inventory
        if (viewer != null && viewer.getOpenInventory() != null) {
            viewer.updateInventory();
        }
    }
    
    private ItemStack createTimePeriodItem(String name, String period, String... lore) {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createModeratorItem(Player moderator, ModeratorStats stats, int rank) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        if (item.getItemMeta() == null) {
            return item;
        }

        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(moderator);
        meta.setDisplayName("§e" + moderator.getName() + " §7(#" + rank + ")");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Statystyki ogólne:");
        lore.add("§eLiczba inspekcji: §f" + stats.getInspections());
        lore.add("§eLiczba banów: §f" + stats.getBans());
        lore.add("§eSkuteczność: §f" + String.format("%.2f%%", stats.getEffectiveness()));
        
        // Add monthly stats if available
        MonthlyStats monthly = getCurrentMonthStats(moderator.getUniqueId());
        if (monthly != null) {
            lore.add("");
            lore.add("§6Statystyki z bieżącego miesiąca:");
            lore.add("§ePunkty: §f" + monthly.getPoints());
            lore.add("§eAktywność: §f" + monthly.getActivityScore() + " pkt");
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createMonthlyModeratorItem(OfflinePlayer player, MonthlyStats stats, int rank) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        if (item.getItemMeta() == null) {
            return item;
        }

        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(player);
        meta.setDisplayName("§e" + player.getName() + " §7(#" + rank + ")");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Statystyki miesięczne:");
        lore.add("§ePunkty: §f" + stats.getPoints());
        lore.add("§eAktywność: §f" + stats.getActivityScore() + " pkt");
        lore.add("§eDni aktywne: §f" + stats.getActiveDays().size() + "/30");
        
        // Add reward info if applicable
        if (rank <= 3) {
            lore.add("");
            lore.add("§6Nagroda za miejsce #" + rank + ":");
            lore.add("§7- " + getRewardForRank(rank));
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public void updateModeratorStats(UUID moderatorId, boolean isBan) {
        // Update overall stats
        ModeratorStats stats = moderatorStats.computeIfAbsent(moderatorId, 
            uuid -> new ModeratorStats(uuid));
        
        if (isBan) {
            stats.incrementBans();
        } else {
            stats.incrementInspections();
        }
        
        // Update monthly stats
        updateMonthlyStats(moderatorId, isBan);
        
        saveRankingData();
    }
    
    private void updateMonthlyStats(UUID moderatorId, boolean isBan) {
        YearMonth currentMonth = YearMonth.now();
        MonthlyStats monthly = getOrCreateMonthlyStats(moderatorId, currentMonth);
        
        // Update activity for today
        LocalDate today = LocalDate.now();
        monthly.getActiveDays().add(today);
        
        // Update points
        if (isBan) {
            monthly.incrementBans();
        } else {
            monthly.incrementInspections();
        }
        
        // Recalculate activity score
        monthly.calculateActivityScore();
        
        // Save to monthly stats
        monthlyStats.computeIfAbsent(currentMonth, k -> new HashMap<>()).put(moderatorId, monthly);
    }
    
    private MonthlyStats getOrCreateMonthlyStats(UUID moderatorId, YearMonth month) {
        return monthlyStats.computeIfAbsent(month, k -> new HashMap<>())
                         .computeIfAbsent(moderatorId, k -> new MonthlyStats(moderatorId));
    }
    
    private MonthlyStats getCurrentMonthStats(UUID moderatorId) {
        return monthlyStats.getOrDefault(YearMonth.now(), new HashMap<>())
                         .get(moderatorId);
    }

    private void loadRankingData() {
        try {
            File rankingFile = new File(plugin.getDataFolder(), "rankmod_stats.yml");
            if (!rankingFile.exists()) {
                rankingFile.createNewFile();
                return;
            }

            YamlConfiguration ranking = YamlConfiguration.loadConfiguration(rankingFile);
            ConfigurationSection moderatorsSection = ranking.getConfigurationSection("moderators");
            
            if (moderatorsSection != null) {
                for (String uuid : moderatorsSection.getKeys(false)) {
                    try {
                        UUID moderatorUuid = UUID.fromString(uuid);
                        int inspections = moderatorsSection.getInt(uuid + ".inspections");
                        int bans = moderatorsSection.getInt(uuid + ".bans");
                        moderatorStats.put(moderatorUuid, new ModeratorStats(moderatorUuid, inspections, bans));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Nieprawidłowy UUID w rankingu: " + uuid);
                    }
                }
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Błąd podczas wczytywania rankingu: " + e.getMessage());
        }
    }
    
    private void loadMonthlyStats() {
        File monthlyFile = new File(plugin.getDataFolder(), "monthly_stats.yml");
        if (!monthlyFile.exists()) {
            try {
                monthlyFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Nie udało się utworzyć pliku monthly_stats.yml: " + e.getMessage());
            }
            return;
        }
        
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(monthlyFile);
        for (String monthKey : yaml.getKeys(false)) {
            try {
                YearMonth month = YearMonth.parse(monthKey, MONTH_FORMAT);
                ConfigurationSection monthSection = yaml.getConfigurationSection(monthKey);
                if (monthSection == null) continue;
                
                Map<UUID, MonthlyStats> monthStats = new HashMap<>();
                
                for (String uuidStr : monthSection.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        ConfigurationSection statsSection = monthSection.getConfigurationSection(uuidStr);
                        if (statsSection == null) continue;
                        
                        MonthlyStats stats = new MonthlyStats(uuid);
                        stats.setInspections(statsSection.getInt("inspections", 0));
                        stats.setBans(statsSection.getInt("bans", 0));
                        stats.setPoints(statsSection.getInt("points", 0));
                        stats.setActivityScore(statsSection.getInt("activityScore", 0));
                        
                        // Load active days
                        List<String> activeDays = statsSection.getStringList("activeDays");
                        Set<LocalDate> days = activeDays.stream()
                            .map(LocalDate::parse)
                            .collect(Collectors.toSet());
                        stats.setActiveDays(days);
                        
                        monthStats.put(uuid, stats);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Nieprawidłowy UUID w statystykach miesięcznych: " + uuidStr);
                    }
                }
                
                monthlyStats.put(month, monthStats);
            } catch (Exception e) {
                plugin.getLogger().warning("Błąd podczas wczytywania statystyk dla miesiąca " + monthKey + ": " + e.getMessage());
            }
        }
    }

    private void saveRankingData() {
        try {
            File rankingFile = new File(plugin.getDataFolder(), "rankmod_stats.yml");
            YamlConfiguration ranking = new YamlConfiguration();

            for (ModeratorStats stats : moderatorStats.values()) {
                ranking.set("moderators." + stats.getUuid().toString() + ".inspections", stats.getInspections());
                ranking.set("moderators." + stats.getUuid().toString() + ".bans", stats.getBans());
            }

            ranking.save(rankingFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Błąd podczas zapisywania rankingu: " + e.getMessage());
        }
    }
    
    private void saveMonthlyStats() {
        try {
            File monthlyFile = new File(plugin.getDataFolder(), "monthly_stats.yml");
            YamlConfiguration yaml = new YamlConfiguration();
            
            for (Map.Entry<YearMonth, Map<UUID, MonthlyStats>> entry : monthlyStats.entrySet()) {
                String monthKey = entry.getKey().format(MONTH_FORMAT);
                
                for (Map.Entry<UUID, MonthlyStats> statsEntry : entry.getValue().entrySet()) {
                    MonthlyStats stats = statsEntry.getValue();
                    String basePath = monthKey + "." + stats.getUuid().toString() + ".";
                    
                    yaml.set(basePath + "inspections", stats.getInspections());
                    yaml.set(basePath + "bans", stats.getBans());
                    yaml.set(basePath + "points", stats.getPoints());
                    yaml.set(basePath + "activityScore", stats.getActivityScore());
                    
                    // Save active days as string list
                    List<String> activeDays = stats.getActiveDays().stream()
                        .map(LocalDate::toString)
                        .collect(Collectors.toList());
                    yaml.set(basePath + "activeDays", activeDays);
                }
            }
            
            yaml.save(monthlyFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Błąd podczas zapisywania statystyk miesięcznych: " + e.getMessage());
        }
    }

    private class ModeratorStats {
        private final UUID uuid;
        private int inspections;
        private int bans;

        public ModeratorStats(UUID uuid) {
            this.uuid = uuid;
            this.inspections = 0;
            this.bans = 0;
        }

        public ModeratorStats(UUID uuid, int inspections, int bans) {
            this.uuid = uuid;
            this.inspections = inspections;
            this.bans = bans;
        }

        public UUID getUuid() {
            return uuid;
        }

        public int getInspections() {
            return inspections;
        }

        public int getBans() {
            return bans;
        }

        public double getEffectiveness() {
            if (inspections == 0) return 0;
            return (double) bans / inspections * 100;
        }

        public void incrementInspections() {
            inspections++;
        }

        public void incrementBans() {
            bans++;
        }
    }
    
    private class MonthlyStats {
        private final UUID uuid;
        private int inspections;
        private int bans;
        private int points;
        private int activityScore;
        private Set<LocalDate> activeDays;
        
        public MonthlyStats(UUID uuid) {
            this.uuid = uuid;
            this.activeDays = new HashSet<>();
        }
        
        public void incrementInspections() {
            inspections++;
            calculatePoints();
        }
        
        public void incrementBans() {
            bans++;
            calculatePoints();
        }
        
        private void calculatePoints() {
            // Base points
            points = (inspections * 1) + (bans * 3);
        }
        
        public void calculateActivityScore() {
            // Activity score based on number of active days and recent activity
            activityScore = activeDays.size() * 2;
            
            // Bonus for recent activity
            if (activeDays.contains(LocalDate.now())) {
                activityScore += 5;
            }
            if (activeDays.contains(LocalDate.now().minusDays(1))) {
                activityScore += 3;
            }
            
            // Add to total points
            points += activityScore;
        }
        
        // Getters and setters
        public UUID getUuid() { return uuid; }
        public int getInspections() { return inspections; }
        public void setInspections(int inspections) { this.inspections = inspections; }
        public int getBans() { return bans; }
        public void setBans(int bans) { this.bans = bans; }
        public int getPoints() { return points; }
        public void setPoints(int points) { this.points = points; }
        public int getActivityScore() { return activityScore; }
        public void setActivityScore(int activityScore) { this.activityScore = activityScore; }
        public Set<LocalDate> getActiveDays() { return activeDays; }
        public void setActiveDays(Set<LocalDate> activeDays) { this.activeDays = activeDays; }
    }
    
    private void scheduleMonthlyReset() {
        // Run at midnight on the first day of each month
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, 1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        
        long delay = calendar.getTimeInMillis() - System.currentTimeMillis();
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Save current month's data
            saveMonthlyStats();
            
            // Clear current month's data
            monthlyStats.remove(YearMonth.now().minusMonths(1));
            
            // Schedule next reset
            scheduleMonthlyReset();
        }, delay / 50); // Convert to ticks
    }
    
    private void scheduleRewardDistribution() {
        // Run at 1 AM on the first day of each month
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, 1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 1);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        
        long delay = calendar.getTimeInMillis() - System.currentTimeMillis();
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            distributeMonthlyRewards();
            scheduleRewardDistribution();
        }, delay / 50); // Convert to ticks
    }
    
    private void distributeMonthlyRewards() {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        Map<UUID, MonthlyStats> lastMonthStats = monthlyStats.getOrDefault(lastMonth, new HashMap<>());
        
        if (lastMonthStats.isEmpty()) {
            return;
        }
        
        // Sort moderators by points
        List<Map.Entry<UUID, MonthlyStats>> sorted = lastMonthStats.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue().getPoints(), a.getValue().getPoints()))
            .collect(Collectors.toList());
        
        // Distribute rewards to top 3
        for (int i = 0; i < Math.min(3, sorted.size()); i++) {
            Map.Entry<UUID, MonthlyStats> entry = sorted.get(i);
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.hasPermission(REWARDS_PERMISSION)) {
                String reward = getRewardForRank(i + 1);
                // Here you would implement the actual reward distribution
                player.sendMessage("§aGratulacje! Zajęłeś/aś " + (i + 1) + " miejsce w rankingu moderatorów!");
                player.sendMessage("§aTwoja nagroda: " + reward);
            }
        }
    }
    
    private String getRewardForRank(int rank) {
        // Get rewards from config or use defaults
        List<String> rewards = plugin.getConfig().getStringList("ranking.rewards.rank" + rank);
        if (rewards == null || rewards.isEmpty()) {
            // Default rewards if not configured
            switch (rank) {
                case 1: return "§6500 monet, §6Legenda serwera (30d), §6Skrzynka VIP";
                case 2: return "§e300 monet, §eZłota skrzynka";
                case 3: return "§f100 monet, §fSrebrna skrzynka";
                default: return "Brak nagrody";
            }
        }
        return String.join(", ", rewards);
    }
}
