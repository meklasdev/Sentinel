package com.wificraft.sentinel.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.wificraft.sentinel.modules.security.GeolocationService;
import com.wificraft.sentinel.SentinelPlugin;
import com.wificraft.sentinel.modules.security.GeolocationService;

import com.wificraft.sentinel.SentinelPlugin;
import com.wificraft.sentinel.modules.security.GeolocationService;

public class LocationCheckCommand implements CommandExecutor {
    private final SentinelPlugin plugin;
    private final GeolocationService geolocationService;

    public LocationCheckCommand(SentinelPlugin plugin) {
        this.plugin = plugin;
        this.geolocationService = new GeolocationService(60); // Cache duration 60 minutes
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cTa komenda może być użyta tylko przez graczy!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("sentinel.location.check")) {
            player.sendMessage("§cNie masz uprawnień do użycia tej komendy!");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cUżycie: /location <gracz>");
            return true;
        }

        String targetPlayer = args[0];
        Player target = Bukkit.getPlayer(targetPlayer);
        
        if (target == null) {
            player.sendMessage("§cGracz nie jest online!");
            return true;
        }

        // Get location analysis
        String analysis = "Lokalizacja: " + target.getLocation().getWorld().getName() + 
                        ", X: " + (int)target.getLocation().getX() +
                        ", Y: " + (int)target.getLocation().getY() +
                        ", Z: " + (int)target.getLocation().getZ();
        
        // Send analysis to player
        player.sendMessage("§6Analiza lokalizacji gracza " + target.getName() + ":");
        for (String line : analysis.split("\n")) {
            player.sendMessage("§7" + line);
        }

        // Check for suspicious location
        GeolocationService.LocationData location = geolocationService.getLocationData(target);
        if (geolocationService.isSuspiciousLocation(location)) {
            player.sendMessage("§cOSTRZEŻENIE: Wykryto podejrzaną lokalizację!");
        }

        return true;
    }
}
