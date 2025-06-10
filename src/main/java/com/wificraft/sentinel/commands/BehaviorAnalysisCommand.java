package com.wificraft.sentinel.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.wificraft.sentinel.SentinelPlugin;
import com.wificraft.sentinel.modules.behavior.BehaviorLearningSystem;

public class BehaviorAnalysisCommand implements CommandExecutor {
    private final SentinelPlugin plugin;
    private final BehaviorLearningSystem learningSystem;

    public BehaviorAnalysisCommand(SentinelPlugin plugin) {
        this.plugin = plugin;
        this.learningSystem = new BehaviorLearningSystem(30, 1.5); // 30 minutes learning period, 1.5 confidence threshold
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cTa komenda może być użyta tylko przez graczy!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("sentinel.behavior.analyze")) {
            player.sendMessage("§cNie masz uprawnień do użycia tej komendy!");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cUżycie: /analyze <gracz>");
            return true;
        }

        String targetPlayer = args[0];
        Player target = Bukkit.getPlayer(targetPlayer);
        
        if (target == null) {
            player.sendMessage("§cNie znaleziono gracza: " + targetPlayer);
            return true;
        }
        
        // Get behavior analysis
        String analysis = learningSystem.getBehaviorAnalysis(target);
        
        // Send analysis to player
        player.sendMessage("§6Analiza zachowania gracza " + target.getName() + ":");
        player.sendMessage("§7" + analysis);

        return true;
    }
}
