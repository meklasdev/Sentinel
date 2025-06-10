package com.wificraft.sentinel.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.wificraft.sentinel.modules.ip.IpAnalysis;
import java.util.concurrent.TimeUnit;

import com.wificraft.sentinel.SentinelPlugin;
import com.wificraft.sentinel.modules.ip.IpAnalyzer;
import com.wificraft.sentinel.modules.ip.IpAnalysis;

public class IpAnalysisCommand implements CommandExecutor {
    private final SentinelPlugin plugin;
    private final IpAnalyzer ipAnalyzer;

    public IpAnalysisCommand(SentinelPlugin plugin) {
        this.plugin = plugin;
        this.ipAnalyzer = plugin.getIpAnalyzer();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cTa komenda może być użyta tylko przez graczy!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("sentinel.ip.analyze")) {
            player.sendMessage("§cNie masz uprawnień do użycia tej komendy!");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cUżycie: /ipanalyze <gracz>");
            return true;
        }

        String targetPlayer = args[0];
        Player target = Bukkit.getPlayer(targetPlayer);
        
        if (target == null) {
            player.sendMessage("§cGracz nie jest online!");
            return true;
        }

        IpAnalysis analysis = ipAnalyzer.getPlayerAnalysis(target.getUniqueId());
        if (analysis == null) {
            player.sendMessage("§cBrak danych analizy IP dla tego gracza!");
            return true;
        }

        // Build analysis message
        StringBuilder message = new StringBuilder("§6Analiza IP:");
        message.append("\n§7IP: §a").append(analysis.getIp());
        message.append("\n§7Zmiany IP: §a").append(analysis.getIpChanges());
        message.append("\n§7Ostatnia zmiana: §a").append(formatTime(analysis.getTimeSinceLastIpChange()));
        message.append("\n§7Czas połączenia: §a").append(formatTime(analysis.getAverageConnectionTime()));
        message.append("\n§7VPN wykryty: §a").append(analysis.getVpnDetected() ? "Tak" : "Nie");
        message.append("\n§7Proxy wykryty: §a").append(analysis.getProxyDetected() ? "Tak" : "Nie");
        message.append("\n§7Podejrzany: §a").append(analysis.isSuspicious() ? "Tak" : "Nie");

        player.sendMessage(message.toString());
        return true;
    }

    private String formatTime(long milliseconds) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
