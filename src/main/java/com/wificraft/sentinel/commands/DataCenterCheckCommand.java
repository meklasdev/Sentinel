package com.wificraft.sentinel.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.wificraft.sentinel.WiFiCraftSentinel;
import com.wificraft.sentinel.modules.security.DataCenterDetector;

import com.wificraft.sentinel.WiFiCraftSentinel;
import com.wificraft.sentinel.modules.security.DataCenterDetector;

public class DataCenterCheckCommand implements CommandExecutor {
    private final WiFiCraftSentinel plugin;
    private final DataCenterDetector dcDetector;

    public DataCenterCheckCommand(WiFiCraftSentinel plugin) {
        this.plugin = plugin;
        this.dcDetector = new DataCenterDetector(60); // Cache duration 60 minutes
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cTa komenda może być użyta tylko przez graczy!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("sentinel.dc.check")) {
            player.sendMessage("§cNie masz uprawnień do użycia tej komendy!");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cUżycie: /dc <gracz>");
            return true;
        }

        String targetPlayer = args[0];
        Player target = Bukkit.getPlayer(targetPlayer);
        
        if (target == null) {
            player.sendMessage("§cGracz nie jest online!");
            return true;
        }

        // Get data center analysis (simplified for now)
        String analysis = "Centrum danych: Nieznane";
        
        // Check if the player's IP is from a known data center
        String ip = target.getAddress().getAddress().getHostAddress();
        if (ip.matches("^(\\d{1,3}\\.){3}\\d{1,3}$")) {
            // Simple check based on IP ranges (this is just an example)
            String[] parts = ip.split("\\.");
            int firstOctet = Integer.parseInt(parts[0]);
            
            if (firstOctet == 10 || (firstOctet == 172 && Integer.parseInt(parts[1]) >= 16 && Integer.parseInt(parts[1]) <= 31) || 
                (firstOctet == 192 && Integer.parseInt(parts[1]) == 168)) {
                analysis = "Lokalna sieć prywatna (LAN)";
            } else if (firstOctet == 100 && Integer.parseInt(parts[1]) >= 64 && Integer.parseInt(parts[1]) <= 127) {
                analysis = "Shared Address Space (CGNAT)";
            } else {
                analysis = "Publiczne IP: " + ip;
            }
        }
        
        // Send analysis to player
        player.sendMessage("§6Analiza centrum danych dla " + target.getName() + ":");
        for (String line : analysis.split("\n")) {
            player.sendMessage("§7" + line);
        }

        // Check for suspicious data center
        DataCenterDetector.DataCenterInfo info = dcDetector.detectDataCenter(target);
        if (info.isSuspicious()) {
            player.sendMessage("§cOSTRZEŻENIE: Wykryto podejrzane centrum danych!");
        }

        return true;
    }
}
