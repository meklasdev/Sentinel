package com.wificraft.sentinel.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.wificraft.sentinel.WiFiCraftSentinel;
import com.wificraft.sentinel.modules.anticheat.AntiCheatIntegration;

public class ViolationCheckCommand implements CommandExecutor {
    private final WiFiCraftSentinel plugin;
    private final AntiCheatIntegration antiCheat;

    public ViolationCheckCommand(WiFiCraftSentinel plugin) {
        this.plugin = plugin;
        this.antiCheat = plugin.getAntiCheatIntegration();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cTa komenda może być użyta tylko przez graczy!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("sentinel.violation.check")) {
            player.sendMessage("§cNie masz uprawnień do użycia tej komendy!");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cUżycie: /violation <gracz>");
            return true;
        }

        String targetPlayer = args[0];
        Player target = Bukkit.getPlayer(targetPlayer);
        
        if (target == null) {
            player.sendMessage("§cGracz nie jest online!");
            return true;
        }

        // Get violation analysis
        String analysis = antiCheat.getViolationAnalysis(target);
        
        // Send analysis to player
        player.sendMessage("§6Analiza naruszeń:");
        for (String line : analysis.split("\n")) {
            player.sendMessage(line);
        }

        return true;
    }
}
