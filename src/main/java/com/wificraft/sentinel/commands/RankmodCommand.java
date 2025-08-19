package com.wificraft.sentinel.commands;

import com.wificraft.sentinel.WiFiCraftSentinel;
import com.wificraft.sentinel.modules.ModeratorRanking;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class RankmodCommand implements CommandExecutor {
    private final WiFiCraftSentinel plugin;

    public RankmodCommand(WiFiCraftSentinel plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("sentinel.rankmod")) {
            sender.sendMessage("§cNie masz uprawnień do użycia tej komendy!");
            return false;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cTa komenda może być użyta tylko przez graczy!");
            return false;
        }

        plugin.getModeratorRanking().openRankingGUI(player);
        return true;
    }
}
