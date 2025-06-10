package com.wificraft.sentinel.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.wificraft.sentinel.SentinelPlugin;
import com.wificraft.sentinel.modules.security.CloudProviderDetector;

import com.wificraft.sentinel.SentinelPlugin;
import com.wificraft.sentinel.modules.security.CloudProviderDetector;

public class CloudCheckCommand implements CommandExecutor {
    private final SentinelPlugin plugin;
    private final CloudProviderDetector cloudDetector;

    public CloudCheckCommand(SentinelPlugin plugin) {
        this.plugin = plugin;
        this.cloudDetector = new CloudProviderDetector(60); // Cache duration 60 minutes
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cTa komenda może być użyta tylko przez graczy!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("sentinel.cloud.check")) {
            player.sendMessage("§cNie masz uprawnień do użycia tej komendy!");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cUżycie: /cloud <gracz>");
            return true;
        }

        String targetPlayer = args[0];
        Player target = Bukkit.getPlayer(targetPlayer);
        
        if (target == null) {
            player.sendMessage("§cGracz nie jest online!");
            return true;
        }

        // Get cloud provider analysis (simplified for now)
        String analysis = "Dostawca: Nieznany";
        
        // Check if the player's IP is from a known cloud provider
        String ip = target.getAddress().getAddress().getHostAddress();
        if (ip.contains("amazonaws.com") || ip.contains("aws")) {
            analysis = "Dostawca: Amazon Web Services (AWS)";
        } else if (ip.contains("google") || ip.contains("googleusercontent.com")) {
            analysis = "Dostawca: Google Cloud Platform";
        } else if (ip.contains("microsoft") || ip.contains("azure")) {
            analysis = "Dostawca: Microsoft Azure";
        } else if (ip.contains("oracle")) {
            analysis = "Dostawca: Oracle Cloud";
        }
        
        // Send analysis to player
        player.sendMessage("§6Analiza dostawcy chmury dla " + target.getName() + ":");
        for (String line : analysis.split("\n")) {
            player.sendMessage("§7" + line);
        }

        // Check for suspicious cloud provider
        CloudProviderDetector.CloudProviderInfo info = cloudDetector.detectCloudProvider(target);
        if (info.isSuspicious()) {
            player.sendMessage("§cOSTRZEŻENIE: Wykryto podejrzany dostawcę chmury!");
        }

        return true;
    }
}
