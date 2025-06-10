package com.wificraft.sentinel.commands;

import com.wificraft.sentinel.SentinelPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class SprawdzCommand implements CommandExecutor, TabCompleter {
    private final SentinelPlugin plugin;

    public SprawdzCommand(SentinelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sendMessage(sender, "&cTa komenda może być używana tylko przez graczy!");
            return true;
        }

        if (!player.hasPermission("sentinel.sprawdz")) {
            sendMessage(player, "&cNie masz uprawnień do użycia tej komendy!");
            return true;
        }

        if (args.length == 0) {
            // Pokaż statystyki moderatora
            plugin.getInspectionModule().openModeratorStatsGUI(player);
            return true;
        }

        // Obsługa podkomend
        String subCommand = args[0].toLowerCase();
        
        try {
            switch (subCommand) {
                case "strefa":
                    return handleZoneCommand(player, args);
                case "pomoc":
                    return showHelp(player);
                case "reload":
                    return handleReload(player);
                default:
                    // Sprawdzanie gracza
                    return handlePlayerInspection(player, args[0]);
            }
        } catch (Exception e) {
            sendMessage(player, "&cWystąpił błąd podczas wykonywania komendy: &e" + e.getMessage());
            plugin.getLogger().warning("Błąd w komendzie /sprawdz: " + e.getMessage());
            e.printStackTrace();
            return true;
        }
    }

    private boolean handleZoneCommand(Player player, String[] args) {
        if (!player.hasPermission("sentinel.sprawdz.strefa")) {
            sendMessage(player, "&cNie masz uprawnień do zarządzania strefą sprawdzania!");
            return true;
        }

        if (args.length < 2) {
            sendMessage(player, "&eUżycie: &6/sprawdz strefa <ustaw|usun|info> [promień]");
            sendMessage(player, "&7- &eustaw &7- Ustawia strefę wokół gracza");
            sendMessage(player, "&7- &eusun &7- Wyłącza strefę sprawdzania");
            sendMessage(player, "&7- &einfo &7- Pokazuje informacje o obecnej strefie");
            return true;
        }

        String action = args[1].toLowerCase();
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection zoneConfig = config.getConfigurationSection("inspection.check-zone");
        
        if (zoneConfig == null) {
            zoneConfig = config.createSection("inspection.check-zone");
        }
        
        switch (action) {
            case "ustaw":
                int radius = 5; // Domyślny promień
                if (args.length > 2) {
                    try {
                        radius = Integer.parseInt(args[2]);
                        if (radius < 1 || radius > 50) {
                            sendMessage(player, "&cPromień musi być między 1 a 50!");
                            return true;
                        }
                    } catch (NumberFormatException e) {
                        sendMessage(player, "&cNieprawidłowy promień! Użyj liczby całkowitej.");
                        return true;
                    }
                }
                
                Location loc = player.getLocation();
                String worldName = loc.getWorld().getName();
                
                zoneConfig.set("enabled", true);
                zoneConfig.set("world", worldName);
                
                ConfigurationSection pos1 = zoneConfig.createSection("pos1");
                pos1.set("x", loc.getBlockX() - radius);
                pos1.set("y", Math.max(0, loc.getBlockY() - radius));
                pos1.set("z", loc.getBlockZ() - radius);
                
                ConfigurationSection pos2 = zoneConfig.createSection("pos2");
                pos2.set("x", loc.getBlockX() + radius);
                pos2.set("y", Math.min(255, loc.getBlockY() + radius));
                pos2.set("z", loc.getBlockZ() + radius);
                
                plugin.saveConfig();
                sendMessage(player, "&aStrefa sprawdzania została ustawiona wokół Ciebie!");
                sendMessage(player, "&7Świat: &f" + worldName);
                sendMessage(player, "&7Promień: &f" + radius + " kratek");
                return true;
                
            case "usun":
                zoneConfig.set("enabled", false);
                plugin.saveConfig();
                sendMessage(player, "&aStrefa sprawdzania została wyłączona!");
                return true;
                
            case "info":
                if (!zoneConfig.getBoolean("enabled", false)) {
                    sendMessage(player, "&7Strefa sprawdzania jest &cwyłączona");
                    return true;
                }
                
                ConfigurationSection pos1Info = zoneConfig.getConfigurationSection("pos1");
                ConfigurationSection pos2Info = zoneConfig.getConfigurationSection("pos2");
                
                if (pos1Info == null || pos2Info == null) {
                    sendMessage(player, "&cBłąd konfiguracji strefy!");
                    return true;
                }
                
                sendMessage(player, "&6=== Informacje o strefie sprawdzania ===");
                sendMessage(player, "&7Status: &aAktywna");
                sendMessage(player, "&7Świat: &f" + zoneConfig.getString("world", "nieznany"));
                sendMessage(player, "&7Pozycja 1: &fX: " + pos1Info.getInt("x") + " Y: " + pos1Info.getInt("y") + " Z: " + pos1Info.getInt("z"));
                sendMessage(player, "&7Pozycja 2: &fX: " + pos2Info.getInt("x") + " Y: " + pos2Info.getInt("y") + " Z: " + pos2Info.getInt("z"));
                return true;
                
            default:
                sendMessage(player, "&cNieznana akcja: " + args[1]);
                return true;
        }
    }
    
    private boolean handlePlayerInspection(Player moderator, String targetName) {
        Player target = plugin.getServer().getPlayer(targetName);
        if (target == null) {
            sendMessage(moderator, "&cNie znaleziono gracza: &e" + targetName);
            return true;
        }
        
        if (moderator.equals(target)) {
            sendMessage(moderator, "&cNie możesz sprawdzić samego siebie!");
            return true;
        }
        
        // Sprawdź czy gracz jest w strefie sprawdzania (jeśli włączona)
        if (isInCheckZoneRequired()) {
            if (!isInCheckZone(target)) {
                sendMessage(moderator, "&cGracz &e" + target.getName() + " &cmusi znajdować się w strefie sprawdzania!");
                return true;
            }
            
            if (!isInCheckZone(moderator)) {
                sendMessage(moderator, "&cMusisz znajdować się w strefie sprawdzania!");
                return true;
            }
        }
        
        // Otwórz GUI inspekcji
        plugin.getInspectionModule().openInspectionGUI(moderator, target);
        sendMessage(moderator, "&7Rozpoczęto sprawdzanie gracza &e" + target.getName());
        return true;
    }
    
    private boolean isInCheckZone(Player player) {
        ConfigurationSection zone = plugin.getConfig().getConfigurationSection("inspection.check-zone");
        if (zone == null || !zone.getBoolean("enabled", false)) {
            return true; // Jeśli strefa nie jest włączona, zawsze zwracaj true
        }
        
        String worldName = zone.getString("world");
        if (worldName == null || worldName.isEmpty()) {
            return true; // Jeśli świat nie jest ustawiony, pozwól na sprawdzanie
        }
        
        World world = Bukkit.getWorld(worldName);
        if (world == null || !player.getWorld().equals(world)) {
            return false;
        }
        
        Location loc = player.getLocation();
        ConfigurationSection pos1 = zone.getConfigurationSection("pos1");
        ConfigurationSection pos2 = zone.getConfigurationSection("pos2");
        
        if (pos1 == null || pos2 == null) {
            return true; 
        }
        
        int minX = Math.min(pos1.getInt("x"), pos2.getInt("x"));
        int minY = Math.min(pos1.getInt("y"), pos2.getInt("y"));
        int minZ = Math.min(pos1.getInt("z"), pos2.getInt("z"));
        int maxX = Math.max(pos1.getInt("x"), pos2.getInt("x"));
        int maxY = Math.max(pos1.getInt("y"), pos2.getInt("y"));
        int maxZ = Math.max(pos1.getInt("z"), pos2.getInt("z"));
        
        return loc.getX() >= minX && loc.getX() <= maxX &&
               loc.getY() >= minY && loc.getY() <= maxY &&
               loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }
    
    private boolean isInCheckZoneRequired() {
        return plugin.getConfig().getBoolean("inspection.check-zone.enabled", false);
    }
    
    private boolean showHelp(Player player) {
        sendMessage(player, "&6===== &ePomoc - /sprawdz &6=====");
        sendMessage(player, "&7Komenda do sprawdzania graczy i zarządzania strefą sprawdzania");
        sendMessage(player, "");
        
        sendMessage(player, "&ePodstawowe komendy:");
        sendMessage(player, "&7• &e/sprawdz &7- Otwórz panel moderatora");
        sendMessage(player, "&7• &e/sprawdz <gracz> &7- Sprawdź wybranego gracza");
        sendMessage(player, "&7• &e/sprawdz pomoc &7- Wyświetl tę pomoc");
        
        if (player.hasPermission("sentinel.sprawdz.strefa")) {
            sendMessage(player, "\n&eZarządzanie strefą sprawdzania:");
            sendMessage(player, "&7• &e/sprawdz strefa ustaw [promień] &7- Ustaw strefę wokół siebie");
            sendMessage(player, "&7• &e/sprawdz strefa usun &7- Wyłącz strefę sprawdzania");
            sendMessage(player, "&7• &e/sprawdz strefa info &7- Pokaż informacje o strefie");
        }
        
        if (player.hasPermission("sentinel.admin")) {
            sendMessage(player, "\n&eAdministracyjne:");
            sendMessage(player, "&7• &e/sprawdz reload &7- Przeładuj konfigurację");
        }
        
        return true;
    }
    
    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, 
                                             @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Podpowiedzi dla pierwszego argumentu
            completions.add("pomoc");
            
            if (sender.hasPermission("sentinel.sprawdz.strefa")) {
                completions.add("strefa");
            }
            
            if (sender.hasPermission("sentinel.admin")) {
                completions.add("reload");
            }
            
            // Dodaj online graczy
            String currentArg = args[0].toLowerCase();
            completions.addAll(Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(currentArg))
                .collect(Collectors.toList()));
                
        } else if (args.length == 2 && args[0].equalsIgnoreCase("strefa")) {
            // Podpowiedzi dla drugiego argumentu komendy strefa
            completions.addAll(Arrays.asList("ustaw", "usun", "info"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("strefa") && args[1].equalsIgnoreCase("ustaw")) {
            // Sugeruj promień tylko przy ustawianiu strefy
            completions.addAll(Arrays.asList("5", "10", "15", "20"));
        }
        
        // Filtruj wyniki na podstawie tego, co użytkownik już wpisał
        String currentArg = args[args.length - 1].toLowerCase();
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(currentArg))
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .collect(Collectors.toList());
    }
    
    private boolean handleReload(Player player) {
        if (!player.hasPermission("sentinel.admin")) {
            sendMessage(player, "&cNie masz uprawnień do przeładowania konfiguracji!");
            return true;
        }
        
        try {
            plugin.reloadConfig();
            sendMessage(player, "&aKonfiguracja została przeładowana!");
            return true;
        } catch (Exception e) {
            sendMessage(player, "&cWystąpił błąd podczas przeładowywania konfiguracji!");
            plugin.getLogger().severe("Błąd podczas przeładowywania konfiguracji: " + e.getMessage());
            e.printStackTrace();
            return true;
        }
    }
    
    private void sendMessage(CommandSender sender, String message) {
        // Zamienia & na kolorowe formatowanie
        Component component = LegacyComponentSerializer.legacy('&')
            .deserialize(message);
        sender.sendMessage(component);
    }
    
    private void sendMessage(Player player, String message) {
        sendMessage((CommandSender) player, message);
    }
}
